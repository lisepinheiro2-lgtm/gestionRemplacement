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

		Set<UUID> absenceIds = new HashSet<>();

		return CsvFormat.readUnchecked(path, cells -> {
			CsvFormat.columns(cells, 13);
			Employee original = new Employee(cells[0], cells[1], Double.parseDouble(cells[2]),
					"Opérationnel", 0, 0, DomainValidation.uuid(cells[11]));
			Employee substitute = null;
			double substituteHours = Double.parseDouble(cells[5]);

			if (!cells[12].isEmpty()) {
				substitute = new Employee(cells[3], cells[4], substituteHours, "Opérationnel", 0, 0,
						DomainValidation.uuid(cells[12]));
			} else if (!cells[3].isEmpty() || !cells[4].isEmpty() || substituteHours != 0) {
				throw new IllegalArgumentException("Remplaçant historique sans UUID.");
			}

			Replacement replacement = new Replacement(original, original.getContractHours(), substitute,
					substituteHours, cells[6], cells[7], cells[8], cells[9]);

			if (!cells[10].isEmpty()) {
				replacement.setAbsenceId(cells[10]);

				if (!absenceIds.add(DomainValidation.uuid(cells[10])))
					throw new IllegalArgumentException("Lien d'absence dupliqué.");
			}

			DomainValidation.replacement(replacement);
			return replacement;
		});
	}

	public void save(List<Replacement> replacements) {
		CsvIdentityMigration.requireCurrentFormat(getDirectory());

		StringBuilder content = new StringBuilder();
		Set<UUID> absenceIds = new HashSet<>();

		for (Replacement replacement : replacements) {
			DomainValidation.replacement(replacement);

			if (replacement.getAbsenceId() != null && !absenceIds.add(DomainValidation.uuid(replacement.getAbsenceId())))
				throw new IllegalArgumentException("Lien d'absence dupliqué.");
			Employee employee = replacement.getReplacementEmployee();
			content.append(CsvFormat.row(replacement.getEmployeeToReplace().getFirstName(),
					replacement.getEmployeeToReplace().getLastName(), replacement.getContractHoursEmployeeToReplace(),
					employee == null ? "" : employee.getFirstName(), employee == null ? "" : employee.getLastName(),
					employee == null ? 0 : replacement.getContractHoursReplacementEmployee(),
					replacement.getStartDate(), replacement.getStartTime(), replacement.getEndDate(),
					replacement.getEndTime(), replacement.getAbsenceId(),
					replacement.getEmployeeToReplace().getEmployeeID().toString(),
					employee == null ? "" : employee.getEmployeeID().toString()));
		}

		CsvStore.write(path, content.toString());
	}
}
