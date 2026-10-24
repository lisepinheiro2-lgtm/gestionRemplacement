package gestion.repository;

import java.io.IOException;
import java.nio.file.Path;
import java.time.*;
import java.util.*;

import gestion.model.*;
import gestion.util.DomainValidation;

public final class AbsenceCsvRepository {

	private final Path path;

	public AbsenceCsvRepository(Path directory) {
		path = CsvStore.directory(directory).resolve("employee_absences.csv");
	}

	public Path getDirectory() {
		return path.getParent();
	}

	public ArrayList<EmployeeAbsence> load(List<Employee> employees) throws IOException {

		List<EmployeeAbsence> checked = new ArrayList<>();

		return CsvFormat.read(path, cells -> {
			CsvFormat.columns(cells, 9);
			Employee employee = CsvReferences.employeeById(employees, cells[8], false);

			EmployeeAbsence absence = new EmployeeAbsence(employee, EmployeeStatus.valueOf(cells[3]),
					LocalDate.parse(cells[4]), LocalDate.parse(cells[5]),
					LocalTime.parse(cells[6]),
					!cells[7].isEmpty() ? LocalTime.parse(cells[7]) : null);

			absence.setId(cells[0]);
			validate(absence, checked);
			checked.add(absence);

			return absence;
		});
	}

	private static void validate(EmployeeAbsence absence, List<EmployeeAbsence> checked) {

		DomainValidation.absence(absence);

		for (EmployeeAbsence other : checked) {
			if (DomainValidation.uuid(absence.getId()).equals(DomainValidation.uuid(other.getId())))
				throw new IllegalArgumentException("Identifiant d'absence dupliqué.");
			if (absence.getEmployee().getEmployeeID().equals(other.getEmployee().getEmployeeID()) && gestion.util.PeriodUtils
					.hasDateTimeOverlap(DomainValidation.absencePeriod(absence), DomainValidation.absencePeriod(other)))
				throw new IllegalArgumentException("Absences superposées.");
		}
	}

	public void save(List<EmployeeAbsence> absences) throws IOException {
		CsvIdentityMigration.requireCurrentFormat(getDirectory());

		StringBuilder content = new StringBuilder();
		List<EmployeeAbsence> checked = new ArrayList<>();

		for (EmployeeAbsence absence : absences) {
			validate(absence, checked);
			checked.add(absence);
			content.append(CsvFormat.row(absence.getId(), absence.getEmployee().getLastName(),
					absence.getEmployee().getFirstName(), absence.getStatus().name(), absence.getStartDate(),
					absence.getEndDate(), absence.getStartTime(), absence.getEndTime(),
					absence.getEmployee().getEmployeeID()));
		}

		CsvStore.writeChecked(path, content.toString());
	}
}
