package gestion.repository;

import java.nio.file.Path;
import java.time.*;
import java.util.*;
import gestion.model.*;
import gestion.util.DomainValidation;

import java.io.IOException;

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
			CsvFormat.columns(cells, 6, 7, 8);
			Employee employee = CsvReferences.optionalEmployee(employees, cells[2], cells[1]);
			EmployeeAbsence absence = new EmployeeAbsence(employee, EmployeeStatus.valueOf(cells[3]),
					LocalDate.parse(cells[4]), LocalDate.parse(cells[5]),
					cells.length >= 7 ? LocalTime.parse(cells[6]) : LocalTime.MIDNIGHT,
					cells.length == 8 ? LocalTime.parse(cells[7]) : null);
			absence.setId(cells[0]);
			validate(absence, checked);
			checked.add(absence);
			return absence;
		});
	}

	private static void validate(EmployeeAbsence absence, List<EmployeeAbsence> checked) {

		DomainValidation.absence(absence);

		for (EmployeeAbsence other : checked) {
			if (absence.getId().equals(other.getId()))
				throw new IllegalArgumentException("Identifiant d'absence dupliqué.");
			if (absence.getEmployee() == other.getEmployee() && gestion.util.PeriodUtils.hasDateTimeOverlap(
					DomainValidation.absencePeriod(absence), DomainValidation.absencePeriod(other)))
				throw new IllegalArgumentException("Absences superposées.");
		}
	}

	public void save(List<EmployeeAbsence> absences) throws IOException {

		StringBuilder content = new StringBuilder();
		List<EmployeeAbsence> checked = new ArrayList<>();

		for (EmployeeAbsence absence : absences) {
			validate(absence, checked);
			checked.add(absence);
			Object[] cells = { absence.getId(), absence.getEmployee().getLastName(),
					absence.getEmployee().getFirstName(), absence.getStatus().name(), absence.getStartDate(),
					absence.getEndDate() };

			if (absence.getStartTime().equals(LocalTime.MIDNIGHT) && absence.getEndTime() == null) {
				content.append(CsvFormat.row(cells));
			} else {
				Object[] timedCells = Arrays.copyOf(cells, absence.getEndTime() == null ? 7 : 8);
				timedCells[6] = absence.getStartTime();

				if (absence.getEndTime() != null)
					timedCells[7] = absence.getEndTime();

				content.append(CsvFormat.row(timedCells));
			}
		}

		CsvStore.writeChecked(path, content.toString());
	}
}
