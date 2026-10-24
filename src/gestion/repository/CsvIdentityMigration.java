package gestion.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import gestion.util.DomainValidation;

public final class CsvIdentityMigration {

	private static final Map<String, Integer> WIDTHS = Map.of(
			"employees.csv", 7, "clients.csv", 7, "interventions.csv", 13,
			"recurringInterventions.csv", 12, "employee_absences.csv", 9, "replacements.csv", 13);
	private static final List<String> FILES = List.of("employees.csv", "clients.csv", "interventions.csv",
			"recurringInterventions.csv", "employee_absences.csv", "replacements.csv");

	public record Result(boolean migrationRequired, boolean migrated, Path backupDirectory, Map<String, Integer> rowCounts) {
	}

	record Row(String file, int line, String[] cells) {

		void columns(int... accepted) {
			for (int count : accepted)
				if (cells.length == count)
					return;

			throw failure("Nombre de colonnes non reconnu : " + cells.length);
		}

		String uuid(int index) {
			return identity(index).toString();
		}

		UUID identity(int index) {
			try {
				return DomainValidation.uuid(cells[index]);
			} catch (IllegalArgumentException failure) {
				throw failure("UUID invalide en colonne " + (index + 1) + " : " + cells[index]);
			}
		}

		String[] expanded(int width) {
			String[] result = Arrays.copyOf(cells, width);
			Arrays.setAll(result, index -> result[index] == null ? "" : result[index]);
			return result;
		}

		IllegalArgumentException failure(String message) {
			return new IllegalArgumentException(file + ", ligne " + line + " : " + message);
		}
	}

	private record Prepared(Map<String, String> content, Map<String, Integer> counts, boolean changed) {
	}

	private CsvIdentityMigration() {
	}

	public static Result migrate(Path directory) throws IOException {
		Path root = CsvStore.directory(directory);

		synchronized (CsvStore.class) {
			if (CsvStore.hasTransaction())
				throw new IOException("La migration UUID doit précéder toute autre transaction de l'application.");

			CsvStore.recover(root);
			Map<Path, byte[]> original = snapshot(root);
			Prepared prepared = prepare(root, original);

			if (!prepared.changed())
				return new Result(false, false, null, Map.copyOf(prepared.counts()));

			Path backup;
			try {
				backup = backup(root, original);
			} catch (IOException failure) {
				throw new IOException("Sauvegarde préalable UUID impossible ; aucun CSV migré. "
						+ failure.getMessage(), failure);
			}
			ensureUnchanged(original);

			try {
				CsvStore.transaction(root, () -> {
					CsvStore.expectUnchanged(root, original);
					prepared.content().forEach((name, content) -> CsvStore.write(root.resolve(name), content));
				});
			} catch (UncheckedIOException failure) {
				throw new IOException("Migration UUID non enregistrée ; sauvegarde conservée : " + backup
						+ ". " + failure.getMessage(), failure);
			}

			return new Result(false, true, backup, Map.copyOf(prepared.counts()));
		}
	}

	public static Result check(Path directory) throws IOException {
		Path root = CsvStore.directory(directory);

		if (Files.exists(root.resolve(".gestion-transaction"), LinkOption.NOFOLLOW_LINKS))
			throw new IOException("Journal CSV présent : récupération nécessaire avant la validation UUID.");

		Prepared prepared = prepare(root, snapshot(root));
		return new Result(prepared.changed(), false, null, Map.copyOf(prepared.counts()));
	}

	static void requireCurrentFormat(Path root) {
		try {
			Map<Path, byte[]> files = snapshot(root);

			for (String name : FILES)
				for (Row row : rows(name, files.get(root.resolve(name))))
					if (row.cells().length != WIDTHS.get(name))
						throw row.failure("Migration UUID requise avant toute sauvegarde ; aucun format mixte autorisé.");
		} catch (IOException failure) {
			throw new UncheckedIOException(failure);
		}
	}

	static void validatePending(Path root, Map<Path, byte[]> writes) throws IOException {
		Map<Path, byte[]> files = snapshot(root);
		files.putAll(writes);
		Map<String, List<Row>> input = new LinkedHashMap<>();

		for (String name : FILES) {
			List<Row> data = rows(name, files.get(root.resolve(name)));
			data.forEach(row -> row.columns(WIDTHS.get(name)));
			input.put(name, data);
		}

		CsvIdentityValidation.validateReferenceIds(input);
	}

	private static Prepared prepare(Path root, Map<Path, byte[]> original) throws IOException {
		try {
			Map<String, List<Row>> input = new LinkedHashMap<>();
			for (String name : FILES)
				input.put(name, rows(name, original.get(root.resolve(name))));

			CsvLegacyIdentities identities = new CsvLegacyIdentities(input);
			Map<String, List<String[]>> output = new LinkedHashMap<>();
			output.put("employees.csv", identities.employees(input.get("employees.csv")));
			output.put("clients.csv", identities.clients(input.get("clients.csv")));
			output.put("interventions.csv", interventions(input.get("interventions.csv"), identities));
			output.put("recurringInterventions.csv", series(input.get("recurringInterventions.csv"), identities));
			output.put("employee_absences.csv", absences(input.get("employee_absences.csv"), identities));
			output.put("replacements.csv", replacements(input.get("replacements.csv"), identities,
					output.get("employee_absences.csv")));

			Map<String, String> content = new LinkedHashMap<>();
			Map<String, Integer> counts = new LinkedHashMap<>();
			boolean changed = false;

			for (String name : FILES) {
				StringBuilder text = new StringBuilder();
				List<String[]> converted = output.get(name);
				for (int index = 0; index < converted.size(); index++) {
					String[] row = converted.get(index);
					text.append(CsvFormat.row((Object[]) row));
					changed |= !Arrays.equals(row, input.get(name).get(index).cells());
				}
				content.put(name, text.toString());
				counts.put(name, converted.size());
			}

			CsvIdentityValidation.validate(root, content);
			return new Prepared(content, counts, changed);
		} catch (UncheckedIOException failure) {
			throw new IOException("Migration UUID interrompue : " + failure.getCause().getMessage(), failure);
		} catch (IllegalArgumentException failure) {
			throw new IOException("Migration UUID interrompue : " + failure.getMessage(), failure);
		}
	}

	private static List<String[]> interventions(List<Row> rows, CsvLegacyIdentities identities) {
		List<String[]> result = new ArrayList<>();
		for (Row row : rows) {
			row.columns(8, 9, 10, 13);
			String[] cells = row.expanded(13);
			boolean identified = row.cells().length == 13;
			cells[10] = identified ? row.uuid(10) : UUID.randomUUID().toString();
			cells[11] = identified ? row.uuid(11) : identities.client(row, cells[0], cells[1]);
			cells[12] = identified ? optionalUuid(row, 12)
					: identities.employee(row, cells[2], cells[3], true, false);
			canonicalLink(row, cells, 8);
			canonicalLink(row, cells, 9);
			result.add(cells);
		}
		return result;
	}

	private static List<String[]> series(List<Row> rows, CsvLegacyIdentities identities) {
		List<String[]> result = new ArrayList<>();
		for (Row row : rows) {
			row.columns(10, 12);
			String[] cells = row.expanded(12);
			cells[0] = row.uuid(0);
			cells[10] = row.cells().length == 12 ? row.uuid(10) : identities.client(row, cells[1], cells[2]);
			cells[11] = row.cells().length == 12 ? optionalUuid(row, 11)
					: identities.employee(row, cells[3], cells[4], true, false);
			result.add(cells);
		}
		return result;
	}

	private static List<String[]> absences(List<Row> rows, CsvLegacyIdentities identities) {
		List<String[]> result = new ArrayList<>();
		for (Row row : rows) {
			row.columns(6, 7, 8, 9);
			String[] cells = row.expanded(9);
			cells[0] = row.uuid(0);
			if (row.cells().length == 6)
				cells[6] = "00:00";
			cells[8] = row.cells().length == 9 ? row.uuid(8)
					: identities.employee(row, cells[2], cells[1], false, false);
			result.add(cells);
		}
		return result;
	}

	private static List<String[]> replacements(List<Row> rows, CsvLegacyIdentities identities, List<String[]> absences) {
		Map<String, String> absentEmployees = new LinkedHashMap<>();
		absences.forEach(cells -> absentEmployees.put(cells[0], cells[8]));
		List<String[]> result = new ArrayList<>();

		for (Row row : rows) {
			row.columns(10, 11, 13);
			String[] cells = row.expanded(13);
			canonicalLink(row, cells, 10);

			if (row.cells().length == 13) {
				cells[11] = row.uuid(11);
				cells[12] = optionalUuid(row, 12);
			} else {
				cells[11] = cells[10].isEmpty()
						? identities.employee(row, cells[0], cells[1], false, true) : absentEmployees.get(cells[10]);
				if (cells[11] == null)
					throw row.failure("Absence source introuvable : " + cells[10]);
				if (!cells[10].isEmpty())
					identities.checkHistoricalName(row, cells[0], cells[1], cells[11]);
				cells[12] = identities.employee(row, cells[3], cells[4], true, true);
			}
			result.add(cells);
		}
		return result;
	}

	private static String optionalUuid(Row row, int index) {
		return row.cells()[index].isEmpty() ? "" : row.uuid(index);
	}

	private static void canonicalLink(Row row, String[] cells, int index) {
		if (!cells[index].isEmpty())
			cells[index] = row.uuid(index);
	}

	private static Map<Path, byte[]> snapshot(Path root) throws IOException {
		if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS))
			throw new IOException("Dossier de données introuvable ou inaccessible : " + root);

		Map<Path, byte[]> result = new LinkedHashMap<>();
		for (String name : FILES) {
			Path path = root.resolve(name);
			if (Files.notExists(path)) {
				result.put(path, null);
			} else {
				if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
					throw new IOException("CSV inaccessible ou non régulier : " + path);
				result.put(path, Files.readAllBytes(path));
			}
		}
		return result;
	}

	private static List<Row> rows(String name, byte[] bytes) throws IOException {
		List<Row> result = new ArrayList<>();
		if (bytes == null)
			return result;

		String text;
		try {
			text = StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
		} catch (java.nio.charset.CharacterCodingException failure) {
			throw new IOException(name + " : encodage UTF-8 invalide.", failure);
		}
		List<String> lines = text.lines().toList();
		for (int index = 0; index < lines.size(); index++)
			if (!lines.get(index).isBlank())
				result.add(new Row(name, index + 1, lines.get(index).split(";", -1)));
		return result;
	}

	private static Path backup(Path root, Map<Path, byte[]> original) throws IOException {
		String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("uuuuMMdd-HHmmss"));
		Path directory = root.getParent().resolve(".gestion-uuid-backups")
				.resolve(root.getFileName() + "-" + time + "-" + UUID.randomUUID());
		Files.createDirectories(directory);

		for (var entry : original.entrySet()) {
			if (entry.getValue() == null)
				continue;
			Path target = directory.resolve(entry.getKey().getFileName());
			try (FileChannel channel = FileChannel.open(target, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
				ByteBuffer buffer = ByteBuffer.wrap(entry.getValue());
				while (buffer.hasRemaining())
					channel.write(buffer);
				channel.force(true);
			}
			if (!Arrays.equals(Files.readAllBytes(target), entry.getValue()))
				throw new IOException("Sauvegarde UUID non vérifiable : " + target);
		}
		return directory;
	}

	private static void ensureUnchanged(Map<Path, byte[]> original) throws IOException {
		for (var entry : original.entrySet()) {
			byte[] current = Files.notExists(entry.getKey()) ? null : Files.readAllBytes(entry.getKey());
			if (!Arrays.equals(current, entry.getValue()))
				throw new IOException("CSV modifié pendant la préparation : " + entry.getKey());
		}
	}

	public static void main(String[] args) {
		if (args.length != 2 || !(args[0].equals("--check") || args[0].equals("--migrate")))
			throw new IllegalArgumentException("Usage : CsvIdentityMigration --check|--migrate dossier");

		try {
			Result result = args[0].equals("--migrate") ? migrate(Path.of(args[1])) : check(Path.of(args[1]));
			System.out.println(result);
		} catch (IOException | RuntimeException failure) {
			System.err.println(failure.getMessage());
			System.exit(1);
		}
	}
}
