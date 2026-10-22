package gestion.repository;

import java.nio.file.Path;
import java.time.*;
import java.util.*;
import gestion.model.*;
import gestion.util.DomainValidation;

public final class ReplacementCsvRepository {

	private final Path path;

	public ReplacementCsvRepository(Path directory) {
		path = CsvStore.directory(directory).resolve("replacements.csv");
	}

	public Path getDirectory() {
		return path.getParent();
	}

	public ArrayList<Replacement> load() {

		Set<String> absenceIds = new HashSet<>();

		return CsvFormat.readUnchecked(path, cells -> {
			CsvFormat.columns(cells, 10, 11);
			Employee original = new Employee(cells[0], cells[1], Double.parseDouble(cells[2]), "Opérationnel", 0, 0);
			Employee substitute = null;
			double substituteHours = Double.parseDouble(cells[5]);

			if (!(cells[3].isBlank() && cells[4].isBlank())) {
				if (cells[3].isBlank() || cells[4].isBlank())
					throw new IllegalArgumentException("Référence incomplète.");
				substitute = new Employee(cells[3], cells[4], substituteHours, "Opérationnel", 0, 0);
			}

			Replacement replacement = new Replacement(original, original.getContractHours(), substitute,
					substituteHours, cells[6], cells[7], cells[8], cells[9]);

			if (cells.length == 11 && !cells[10].isBlank()) {
				replacement.setAbsenceId(cells[10]);
				if (!absenceIds.add(cells[10]))
					throw new IllegalArgumentException("Lien d'absence dupliqué.");
			}

			DomainValidation.replacement(replacement);
			return replacement;
		});
	}

	public void save(List<Replacement> replacements) {

		StringBuilder content = new StringBuilder();
		Set<String> absenceIds = new HashSet<>();

		for (Replacement replacement : replacements) {
			DomainValidation.replacement(replacement);

			if (replacement.getAbsenceId() != null && !absenceIds.add(replacement.getAbsenceId()))
				throw new IllegalArgumentException("Lien d'absence dupliqué.");
			Employee employee = replacement.getReplacementEmployee();
			content.append(CsvFormat.row(replacement.getEmployeeToReplace().getFirstName(),
					replacement.getEmployeeToReplace().getLastName(), replacement.getContractHoursEmployeeToReplace(),
					employee == null ? "" : employee.getFirstName(), employee == null ? "" : employee.getLastName(),
					employee == null ? 0 : replacement.getContractHoursReplacementEmployee(),
					replacement.getStartDate(), replacement.getStartTime(), replacement.getEndDate(),
					replacement.getEndTime(), replacement.getAbsenceId()));
		}

		CsvStore.write(path, content.toString());
	}
}
