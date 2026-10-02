package gestionRemplacement;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

public class EmployeeAbsenceList {

	private final PersonnelList personnel;
	private final ArrayList<EmployeeAbsence> absences = new ArrayList<>();
	private final String fileName = "employee_absences.csv";
	private final ReplacementList replacementList;

	public EmployeeAbsenceList(PersonnelList personnel, ReplacementList replacementList) throws IOException {

		super();
		this.personnel = personnel;
		this.replacementList = replacementList;

		loadAbsences();
		replacementList.synchronizeAbsences(absences);
	}

	public EmployeeStatus getStatusAt(Employee employee, LocalDate date) {

		for (EmployeeAbsence absence : absences) {
			if (absence.getEmployee() == employee && !date.isBefore(absence.getStartDate())
					&& !date.isAfter(absence.getEndDate())) {

				return absence.getStatus();
			}
		}

		return EmployeeStatus.OPERATIONAL;
	}

	public void resumeEmployee(Employee employee, LocalDate date) throws IOException {

		for (int i = 0; i < absences.size(); i++) {
			EmployeeAbsence absence = absences.get(i);

			if (absence.getEmployee() != employee || date.isBefore(absence.getStartDate())
					|| date.isAfter(absence.getEndDate())) {
				continue;
			}

			LocalDate previousEnd = absence.getEndDate();
			boolean remove = absence.getStartDate().equals(date);

			if (remove) {
				absences.remove(i);
			} else {
				absence.setEndDate(date.minusDays(1));
			}

			try {
				saveAbsences();
			} catch (IOException exception) {
				if (remove) {
					absences.add(i, absence);
				} else {
					absence.setEndDate(previousEnd);
				}
				throw exception;
			}

			replacementList.synchronizeAbsences(absences);
			return;
		}
	}

	public void addAbsence(EmployeeAbsence absence) throws IOException {

		for (EmployeeAbsence existing : absences) {
			if (existing.getEmployee() == absence.getEmployee()
					&& !absence.getEndDate().isBefore(existing.getStartDate())
					&& !absence.getStartDate().isAfter(existing.getEndDate())) {

				throw new IllegalArgumentException("Une absence existe déjà pour cet employé sur cette période.");
			}
		}

		absences.add(absence);

		try {
			saveAbsences();
		} catch (IOException exception) {
			absences.remove(absence);
			throw exception;
		}
		replacementList.synchronizeAbsences(absences);
	}

	public void saveAbsences() throws IOException {

		try (BufferedWriter writer = Files.newBufferedWriter(Path.of(fileName), StandardCharsets.UTF_8)) {

			for (EmployeeAbsence absence : absences) {
				Employee employee = absence.getEmployee();

				writer.write(String.join(";", absence.getId(), employee.getLastName(), employee.getFirstName(),
						absence.getStatus().name(), absence.getStartDate().toString(),
						absence.getEndDate().toString()));

				writer.newLine();
			}
		}
	}

	private void loadAbsences() throws IOException {

		Path path = Path.of(fileName);

		if (Files.notExists(path)) {
			return;
		}

		ArrayList<EmployeeAbsence> loadedAbsences = new ArrayList<>();

		try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {

			String line;

			while ((line = reader.readLine()) != null) {

				if (line.isBlank()) {
					continue;
				}

				String[] data = line.split(";", -1);

				if (data.length != 6 || data[0].isBlank()) {
					throw new IOException("Ligne d'absence invalide : " + line);
				}

				Employee employee = personnel.findEmployee(data[2], data[1]);

				if (employee == null) {
					throw new IOException("Employé introuvable : " + data[2] + " " + data[1]);
				}

				try {
					EmployeeStatus status = EmployeeStatus.valueOf(data[3]);
					LocalDate startDate = LocalDate.parse(data[4]);
					LocalDate endDate = LocalDate.parse(data[5]);

					if (endDate.isBefore(startDate)) {
						throw new IOException("Dates d'absence inversées.");
					}

					EmployeeAbsence absence = new EmployeeAbsence(employee, status, startDate, endDate);

					absence.setId(data[0]);
					loadedAbsences.add(absence);

				} catch (IllegalArgumentException | DateTimeParseException exception) {
					throw new IOException("Absence invalide : " + line, exception);
				}
			}
		}

		absences.clear();
		absences.addAll(loadedAbsences);
	}

	public ArrayList<EmployeeAbsence> getAbsences() {
		return absences;
	}

}
