package gestionRemplacement.repository;

import gestionRemplacement.model.Employee;
import gestionRemplacement.model.Replacement;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ReplacementCsvRepository {
	private final Path path;

	public ReplacementCsvRepository(Path directory) {
		path = directory.resolve("replacements.csv");
	}

	public ArrayList<Replacement> load() {
		ArrayList<Replacement> replacements = new ArrayList<>();

		File file = path.toFile();
		if (!file.exists()) {
			return replacements;
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
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

		} catch (IOException e) {
			System.out.println("Erreur au chargement du fichier remplacements");
			e.printStackTrace();
		}
		return replacements;
	}

	public void save(List<Replacement> replacements) {

		try (FileWriter writer = new FileWriter(path.toFile())) {

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

		} catch (IOException e) {
			System.out.println("Erreur pendant la sauvegarde du fichier remplacements");
			e.printStackTrace();
		}
	}
}
