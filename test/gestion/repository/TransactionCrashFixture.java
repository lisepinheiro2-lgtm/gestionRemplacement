package gestion.repository;

import java.nio.file.Path;

/** Processus enfant, exclusivement invoqué par les tests sur un @TempDir fictif. */
public final class TransactionCrashFixture {
    public static void main(String[] args) throws Exception {
        Path directory = Path.of(args[1]);
        if (args[0].equals("recover")) { CsvStore.recover(directory); return; }
        CsvStore.installHook = count -> { if (count == 1) Runtime.getRuntime().halt(23); };
        CsvStore.transaction(directory, () -> {
            CsvStore.write(directory.resolve("clients.csv"), "Nouveau;Client;Adresse fictive;0;;\n");
            CsvStore.write(directory.resolve("employees.csv"), "Nouveau;Employé;35;Opérationnel;0;0\n");
        });
    }
}
