package gestionRemplacement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ReplacementList {

	private ArrayList<Replacement> replacements = new ArrayList<>();
	private final String fileName = "replacements.csv";

	public ReplacementList() {
		loadReplacements();
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
				String[] data = line.split(";");

				if (data.length == 9) {

					double contractHoursEmployeeToReplace = Double.parseDouble(data[2]);
					Employee employeeToReplace = new Employee(data[0], data[1], contractHoursEmployeeToReplace);
					double contractHoursReplacementEmployee = Double.parseDouble(data[5]);
					Employee replacementEmployee = new Employee(data[3], data[4], contractHoursReplacementEmployee);
					String date = data[6];
					String startTime = data[7];
					String endTime = data[8];

					replacements.add(new Replacement(employeeToReplace, contractHoursEmployeeToReplace,
							replacementEmployee, contractHoursReplacementEmployee, date, startTime, endTime));

				}
			}

			reader.close();

		} catch (IOException e) {
			System.out.println("Erreur au chargement du fichier remplacements");
			e.printStackTrace();
		}
	}

	public void addReplacement(Employee employeeToReplace, double contractHoursEmployeeToReplace,
			Employee replacementEmployee, double contractHoursReplacementEmployee, String date, String startTime,
			String endTime) {

		replacements.add(new Replacement(employeeToReplace, contractHoursEmployeeToReplace, replacementEmployee,
				contractHoursReplacementEmployee, date, startTime, endTime));

		saveReplacements();
	}

	private void saveReplacements() {

		try {

			FileWriter writer = new FileWriter(fileName);

			for (Replacement replacement : replacements) {

				writer.write(replacement.getEmployeeToReplace().getFirstName() + ";"
						+ replacement.getEmployeeToReplace().getLastName() + ";"
						+ replacement.getContractHoursEmployeeToReplace() + ";"
						+ replacement.getReplacementEmployee().getFirstName() + ";"
						+ replacement.getReplacementEmployee().getLastName() + ";"
						+ replacement.getContractHoursReplacementEmployee() + ";" + replacement.getDate() + ";"
						+ replacement.getStartTime() + ";" + replacement.getEndTime() + System.lineSeparator());
			}

			writer.close();

		} catch (IOException e) {
			System.out.println("Erreur pendant la sauvegarde du fichier remplacements");
			e.printStackTrace();
		}
	}

	public void editReplacements(int index, Employee employeeToReplace, double contractHoursEmployeeToReplace,
			Employee replacementEmployee, double contractHoursReplacementEmployee, String date, String startTime,
			String endTime) {

		Replacement replacement = replacements.get(index);

		replacement.setEmployeeToReplace(employeeToReplace);
		replacement.setContractHoursEmployeeToReplace(contractHoursEmployeeToReplace);
		replacement.setReplacementEmployee(replacementEmployee);
		replacement.setContractHoursReplacementEmployee(contractHoursReplacementEmployee);
		replacement.setDate(date);
		replacement.setStartTime(startTime);
		replacement.setEndTime(endTime);

		saveReplacements();
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
