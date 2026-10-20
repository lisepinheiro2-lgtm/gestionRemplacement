package gestion.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Écritures unitaires atomiques et journal de compensation pour les opérations composées.
 * Une seule instance de l'application doit utiliser un dossier de données à la fois.
 */
public final class CsvStore {
    private static final String JOURNAL = ".gestion-transaction";
    private static final Set<String> FILES = Set.of("employees.csv", "clients.csv", "interventions.csv",
            "recurringInterventions.csv", "replacements.csv", "employee_absences.csv");
    private static final ThreadLocal<Transaction> CURRENT = new ThreadLocal<>();
    private static final Set<Path> BLOCKED = new java.util.HashSet<>();

    private CsvStore() { }

    private static final class Transaction {
        final Path directory;
        final Map<Path, byte[]> writes = new LinkedHashMap<>();
        final List<Runnable> rollbacks = new ArrayList<>();
        Transaction(Path directory) { this.directory = directory; }
    }

    public static Path directory(Path path) { return path.toAbsolutePath().normalize(); }

    public static synchronized void recover(Path directory) throws IOException {
        Path root = directory(directory);
        if (BLOCKED.contains(root))
            throw new IOException("Récupération incomplète : fermer l'application et vérifier le journal local.");
        Path journal = root.resolve(JOURNAL);
        if (Files.notExists(journal)) return;
        if (!Files.isDirectory(journal, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Journal de récupération invalide.");
        Path prepared = journal.resolve("prepared");
        if (Files.exists(prepared) && !Files.exists(journal.resolve("committed"))) {
            List<String[]> entries = new ArrayList<>();
            Set<String> seen = new java.util.HashSet<>();
            for (String line : Files.readAllLines(prepared, StandardCharsets.UTF_8)) {
                String[] entry = line.split(";", -1);
                if (entry.length != 4 || !FILES.contains(entry[0]) || !seen.add(entry[0])
                        || !(entry[1].equals("true") || entry[1].equals("false"))
                        || !entry[2].matches("[0-9a-f]{64}") || !entry[3].matches("[0-9a-f]{64}"))
                    throw new IOException("Manifeste de récupération invalide.");
                Path target = root.resolve(entry[0]);
                boolean existed = Boolean.parseBoolean(entry[1]);
                if (existed && !hash(Files.readAllBytes(journal.resolve(entry[0] + ".old"))).equals(entry[2]))
                    throw new IOException("Copie de récupération invalide : " + entry[0]);
                if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
                    requireRegular(target);
                    String current = hash(Files.readAllBytes(target));
                    if (!current.equals(entry[2]) && !current.equals(entry[3]))
                        throw new IOException("Fichier modifié hors de l'opération : " + entry[0]
                                + ". Récupération manuelle nécessaire.");
                } else if (!Files.notExists(target)) {
                    throw new IOException("Fichier inaccessible : " + entry[0]);
                }
                entries.add(entry);
            }
            if (entries.isEmpty()) throw new IOException("Manifeste de récupération vide.");
            // Toutes les copies et cibles sont contrôlées avant la première compensation.
            for (String[] entry : entries) {
                Path target = root.resolve(entry[0]);
                if (Boolean.parseBoolean(entry[1]))
                    atomicWrite(target, Files.readAllBytes(journal.resolve(entry[0] + ".old")));
                else Files.deleteIfExists(target);
            }
        }
        cleanup(journal);
    }

    public static synchronized void write(Path path, String content) {
        try { writeChecked(path, content); }
        catch (IOException e) { throw new UncheckedIOException("Échec de sauvegarde : " + path.getFileName(), e); }
    }

    public static synchronized void writeChecked(Path path, String content) throws IOException {
        Path target = directory(path);
        if (!FILES.contains(target.getFileName().toString())) throw new IOException("Fichier CSV non pris en charge.");
        Path root = target.getParent();
        if (BLOCKED.contains(root)) throw new IOException("Récupération incomplète ; redémarrage requis.");
        Transaction transaction = CURRENT.get();
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (transaction != null) {
            if (!transaction.directory.equals(root)) throw new IOException("Les CSV doivent partager le même dossier.");
            transaction.writes.put(target, bytes);
        } else {
            recover(root);
            atomicWrite(target, bytes);
        }
    }

    public static synchronized void onRollback(Path directory, Runnable restore) {
        Transaction transaction = CURRENT.get();
        if (transaction != null) {
            if (!transaction.directory.equals(directory(directory)))
                throw new IllegalArgumentException("Les services doivent utiliser le même dossier de données.");
            transaction.rollbacks.add(restore);
        }
    }

    public static synchronized void transaction(Path directory, Runnable action) {
        Path root = directory(directory);
        if (CURRENT.get() != null) {
            if (!CURRENT.get().directory.equals(root)) throw new IllegalArgumentException("Dossiers différents.");
            action.run();
            return;
        }
        Transaction transaction = new Transaction(root);
        try {
            recover(root);
            CURRENT.set(transaction);
            action.run();
            commit(transaction);
        } catch (IOException | RuntimeException failure) {
            for (int i = transaction.rollbacks.size() - 1; i >= 0; i--) {
                try { transaction.rollbacks.get(i).run(); }
                catch (RuntimeException rollback) { failure.addSuppressed(rollback); }
            }
            if (failure instanceof IOException io)
                throw new UncheckedIOException("Opération non enregistrée. Vérifier le journal si la récupération échoue.", io);
            throw (RuntimeException) failure;
        } finally { CURRENT.remove(); }
    }

    private static void commit(Transaction transaction) throws IOException {
        if (transaction.writes.isEmpty()) return;
        Path journal = transaction.directory.resolve(JOURNAL);
        Files.createDirectory(journal);
        try {
            StringBuilder manifest = new StringBuilder();
            for (var entry : transaction.writes.entrySet()) {
                Path target = entry.getKey();
                boolean exists = Files.exists(target, LinkOption.NOFOLLOW_LINKS);
                if (exists) requireRegular(target);
                else if (!Files.notExists(target)) throw new IOException("Fichier inaccessible : " + target.getFileName());
                byte[] previous = exists ? Files.readAllBytes(target) : new byte[0];
                String name = target.getFileName().toString();
                if (exists) atomicWrite(journal.resolve(name + ".old"), previous);
                atomicWrite(journal.resolve(name + ".new"), entry.getValue());
                manifest.append(name).append(';').append(exists).append(';').append(hash(previous)).append(';')
                        .append(hash(entry.getValue())).append('\n');
            }
            atomicWrite(journal.resolve("prepared"), manifest.toString().getBytes(StandardCharsets.UTF_8));
            int installed = 0;
            for (Path target : transaction.writes.keySet()) {
                Files.move(journal.resolve(target.getFileName() + ".new"), target,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                afterInstall(++installed); // point d'injection package-private, réservé aux tests
            }
            atomicWrite(journal.resolve("committed"), new byte[0]);
        } catch (IOException | RuntimeException failure) {
            try { recover(transaction.directory); }
            catch (IOException recovery) {
                BLOCKED.add(transaction.directory);
                failure.addSuppressed(recovery);
                throw new IOException("Compensation incomplète : fermer l'application et conserver .gestion-transaction.", failure);
            }
            throw failure;
        }
        // Le marqueur committed rend l'opération définitive. Un échec de nettoyage ne l'annule pas.
        try { cleanup(journal); }
        catch (IOException ignored) { /* nettoyé avant la prochaine opération ou au redémarrage */ }
    }

    @FunctionalInterface interface InstallHook { void installed(int count) throws IOException; }
    static InstallHook installHook = count -> { };
    @FunctionalInterface interface WriteHook { void written(Path target) throws IOException; }
    static WriteHook writeHook = target -> { };
    private static void afterInstall(int count) throws IOException { installHook.installed(count); }

    private static void requireRegular(Path path) throws IOException {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
            throw new IOException("Fichier ordinaire attendu : " + path.getFileName());
    }

    private static void atomicWrite(Path path, byte[] bytes) throws IOException {
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) requireRegular(path);
        Path temp = Files.createTempFile(path.getParent(), ".gestion-", ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) channel.write(buffer);
                channel.force(true);
            }
            if (FILES.contains(path.getFileName().toString())) writeHook.written(path);
            Files.move(temp, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temp); }
    }

    private static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static void cleanup(Path journal) throws IOException {
        // N'effacer que les fichiers créés par ce protocole, jamais récursivement.
        try (var files = Files.list(journal)) {
            for (Path path : files.toList()) {
                String name = path.getFileName().toString();
                if (!(name.equals("prepared") || name.equals("committed") || name.startsWith(".gestion-")
                        || FILES.stream().anyMatch(file -> name.equals(file + ".old") || name.equals(file + ".new"))))
                    throw new IOException("Fichier inconnu dans le journal ; vérification manuelle requise.");
            }
        }
        // Garder les marqueurs jusqu'après la suppression des copies évite une fausse récupération.
        if (Files.exists(journal.resolve("prepared")) && !Files.exists(journal.resolve("committed")))
            atomicWrite(journal.resolve("committed"), new byte[0]);
        try (var files = Files.list(journal)) {
            for (Path path : files.toList())
                if (!path.getFileName().toString().equals("committed")) Files.delete(path);
        }
        Files.deleteIfExists(journal.resolve("committed"));
        Files.delete(journal);
    }
}
