package gestion.repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;


final class CsvFormat {

	private static final ThreadLocal<Map<Path, String>> SNAPSHOT = new ThreadLocal<>();

	@FunctionalInterface
	interface CheckedAction {
		void run() throws IOException;
	}

	static void withSnapshot(Map<Path, String> content, CheckedAction action) throws IOException {
		if (SNAPSHOT.get() != null)
			throw new IllegalStateException("Validation CSV déjà en cours.");

		SNAPSHOT.set(Map.copyOf(content));
		try {
			action.run();
		} finally {
			SNAPSHOT.remove();
		}
	}

	private CsvFormat() {
	}

	@FunctionalInterface
	interface Parser<T> {
		T parse(String[] cells);
	}

	static <T> ArrayList<T> read(Path path, Parser<T> parser) throws IOException {

		Map<Path, String> snapshot = SNAPSHOT.get();

		if (snapshot == null)
			CsvStore.recover(path.toAbsolutePath().getParent());
		ArrayList<T> result = new ArrayList<>();

		if (snapshot == null && Files.notExists(path))
			return result;
		int lineNumber = 0;

		if (snapshot != null && !snapshot.containsKey(CsvStore.directory(path)))
			throw new IOException("Fichier absent de l'aperçu de migration : " + path.getFileName());

		try (var reader = snapshot == null ? Files.newBufferedReader(path, StandardCharsets.UTF_8)
				: new BufferedReader(new StringReader(snapshot.get(CsvStore.directory(path))))) {

			String line;

			while ((line = reader.readLine()) != null) {
				lineNumber++;
				if (line.isBlank())
					continue;
				try {
					result.add(parser.parse(line.split(";", -1)));
				} catch (RuntimeException invalid) {
					throw new IOException(path.getFileName() + ", ligne " + lineNumber + " : " + invalid.getMessage(), invalid);
				}
			}

		} catch (IOException e) {
			throw new IOException("Lecture complète impossible : " + path.getFileName() + ", ligne " + lineNumber
					+ ". Aucune ligne chargée. " + e.getMessage(), e);
		}

		return result;
	}

	static <T> ArrayList<T> readUnchecked(Path path, Parser<T> parser) {

		try {
			return read(path, parser);
		} catch (IOException e) {
			throw new UncheckedIOException(e.getMessage(), e);
		}
	}

	static void columns(String[] cells, int... accepted) {

		for (int count : accepted)
			if (cells.length == count)
				return;
		throw new IllegalArgumentException("Nombre de colonnes invalide.");
	}

	static String row(Object... cells) {

		String[] values = new String[cells.length];

		for (int i = 0; i < cells.length; i++) {
			values[i] = cells[i] == null ? "" : cells[i].toString();
			gestion.util.DomainValidation.text(values[i], false);
		}

		return String.join(";", values) + System.lineSeparator();
	}
}
