package gestionRemplacement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

public class ReplacementList {

	private ArrayList<Replacement> replacements = new ArrayList<>();
	private final String fileName = "replacements.csv";

	public ReplacementList() {

		loadReplacements();
		sortReplacements();
	}

	private void loadReplacements() {

		File file = new File(fileName);
		if (!file.exists()) {
			return;
		}

		try {

			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;

			while ((line = reader.readLine()) != null) {
				String[] data = line.split(";", -1);

				if (data.length == 10 || data.length == 11) {

					double contractHoursEmployeeToReplace = Double.parseDouble(data[2]);
					Employee employeeToReplace = new Employee(data[0], data[1], contractHoursEmployeeToReplace, "satut",
							0.0, 0.0);

					Employee replacementEmployee = null;
					double contractHoursReplacementEmployee = 0;

					if (!data[3].isBlank() && !data[4].isBlank()) {

						contractHoursReplacementEmployee = Double.parseDouble(data[5]);
						replacementEmployee = new Employee(data[3], data[4], contractHoursReplacementEmployee, "satut",
								0.0, 0.0);
					}

					String startDate = data[6];
					String startTime = data[7];
					String endDate = data[8];
					String endTime = data[9];

					Replacement replacement = new Replacement(employeeToReplace, contractHoursEmployeeToReplace,
							replacementEmployee, contractHoursReplacementEmployee, startDate, startTime, endDate,
							endTime);

					if (data.length == 11 && !data[10].isBlank()) {
						replacement.setAbsenceId(data[10]);
					}

					replacements.add(replacement);
				}
			}

			reader.close();

		} catch (IOException e) {
			System.out.println("Erreur au chargement du fichier remplacements");
			e.printStackTrace();
		}
	}

	public void addReplacement(Employee employeeToReplace, double contractHoursEmployeeToReplace,
			Employee replacementEmployee, double contractHoursReplacementEmployee, String startDate, String startTime,
			String endDate, String endTime) {

		replacements.add(new Replacement(employeeToReplace, contractHoursEmployeeToReplace, replacementEmployee,
				contractHoursReplacementEmployee, startDate, startTime, endDate, endTime));

		sortReplacements();
		saveReplacements();
	}

	private void saveReplacements() {

		try {

			FileWriter writer = new FileWriter(fileName);

			for (Replacement replacement : replacements) {

				String replacementFirstName = "";
				String replacementLastName = "";
				double replacementContractHours = 0;

				if (replacement.getReplacementEmployee() != null) {
					replacementFirstName = replacement.getReplacementEmployee().getFirstName();
					replacementLastName = replacement.getReplacementEmployee().getLastName();
					replacementContractHours = replacement.getContractHoursReplacementEmployee();
				}

				writer.write(replacement.getEmployeeToReplace().getFirstName() + ";"
						+ replacement.getEmployeeToReplace().getLastName() + ";"
						+ replacement.getContractHoursEmployeeToReplace() + ";" + replacementFirstName + ";"
						+ replacementLastName + ";" + replacementContractHours + ";" + replacement.getStartDate() + ";"
						+ replacement.getStartTime() + ";" + replacement.getEndDate() + ";" + replacement.getEndTime()
						+ ";" + (replacement.getAbsenceId() == null ? "" : replacement.getAbsenceId())
						+ System.lineSeparator());
			}

			writer.close();

		} catch (IOException e) {
			System.out.println("Erreur pendant la sauvegarde du fichier remplacements");
			e.printStackTrace();
		}
	}

	public void editReplacements(int index, Employee employeeToReplace, double contractHoursEmployeeToReplace,
			Employee replacementEmployee, double contractHoursReplacementEmployee, String startDate, String startTime,
			String endDate, String endTime) {

		Replacement replacement = replacements.get(index);

		replacement.setEmployeeToReplace(employeeToReplace);
		replacement.setContractHoursEmployeeToReplace(contractHoursEmployeeToReplace);
		replacement.setReplacementEmployee(replacementEmployee);
		replacement.setContractHoursReplacementEmployee(contractHoursReplacementEmployee);
		replacement.setStartDate(startDate);
		replacement.setStartTime(startTime);
		replacement.setEndDate(endDate);
		replacement.setEndTime(endTime);

		sortReplacements();
		saveReplacements();
	}

	public void synchronizeAbsences(ArrayList<EmployeeAbsence> absences) {

		HashSet<String> absenceIds = new HashSet<>();

		for (EmployeeAbsence absence : absences) {
			absenceIds.add(absence.getId());
		}

		replacements.removeIf(
				replacement -> replacement.getAbsenceId() != null && !absenceIds.contains(replacement.getAbsenceId()));

		for (EmployeeAbsence absence : absences) {
			Replacement linkedReplacement = null;

			for (Replacement replacement : replacements) {
				if (absence.getId().equals(replacement.getAbsenceId())) {
					linkedReplacement = replacement;
					break;
				}
			}

			String startDate = FormatUtils.formatDate(absence.getStartDate());
			String endDate = FormatUtils.formatDate(absence.getEndDate().plusDays(1));

			if (linkedReplacement == null) {
				Employee employee = absence.getEmployee();

				linkedReplacement = new Replacement(employee, employee.getContractHours(), null, 0, startDate, "00h00",
						endDate, "00h00");

				linkedReplacement.setAbsenceId(absence.getId());
				replacements.add(linkedReplacement);
			} else {
				linkedReplacement.setStartDate(startDate);
				linkedReplacement.setStartTime("00h00");
				linkedReplacement.setEndDate(endDate);
				linkedReplacement.setEndTime("00h00");
			}
		}

		sortReplacements();
		saveReplacements();
	}

	public void sortReplacements() {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		replacements.sort(Comparator.comparing(replacement -> LocalDate.parse(replacement.getStartDate(), formatter)));
	}

	public void removeReplacement(int index) {
		replacements.remove(index);

		saveReplacements();
	}

	public ArrayList<Replacement> getReplacements() {
		return replacements;
	}

	public boolean isEmpty() {
		return replacements.isEmpty();
	}
}
