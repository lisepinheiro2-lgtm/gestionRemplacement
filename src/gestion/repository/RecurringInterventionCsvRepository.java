package gestion.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import gestion.model.Client;
import gestion.model.Employee;
import gestion.model.RecurringIntervention;

public final class RecurringInterventionCsvRepository {
	private final Path path;

	public RecurringInterventionCsvRepository(Path directory) {
		path = directory.resolve("recurringInterventions.csv");
	}

	public ArrayList<RecurringIntervention> load(List<Employee> employees, List<Client> clients) {
		ArrayList<RecurringIntervention> recurringInterventions = new ArrayList<>();

		File file = path.toFile();

		if (!file.exists()) {
			return recurringInterventions;
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

			String line;

			while ((line = reader.readLine()) != null) {

				String[] data = line.split(";", -1);

				if (data.length == 10) {

					String id = data[0];
					Client client = CsvReferences.findClient(clients, data[1], data[2]);
					Employee employee = null;

					if (!data[3].isBlank() && !data[4].isBlank()) {
						employee = CsvReferences.findEmployee(employees, data[3], data[4]);
					}

					DayOfWeek dayOfWeek = DayOfWeek.valueOf(data[5]);
					LocalTime startTime = LocalTime.parse(data[6]);
					LocalTime endTime = LocalTime.parse(data[7]);
					LocalDate startDate = LocalDate.parse(data[8]);
					LocalDate endDate = LocalDate.parse(data[9]);

					if (client != null) {
						RecurringIntervention recurring = new RecurringIntervention(client, employee, dayOfWeek,
								startTime, endTime, startDate, endDate);
						recurring.setId(id);

						recurringInterventions.add(recurring);
					}
				}
			}

		} catch (IOException e) {
			System.out.println("Erreur au chargement des interventions récurrentes.");
			e.printStackTrace();
		}
		return recurringInterventions;
	}

	public void save(List<RecurringIntervention> recurringInterventions) {

		try (FileWriter writer = new FileWriter(path.toFile())) {

			for (RecurringIntervention recurring : recurringInterventions) {
				String employeeFirstName = "";
				String employeeLastName = "";

				if (recurring.getEmployee() != null) {
					employeeFirstName = recurring.getEmployee().getFirstName();
					employeeLastName = recurring.getEmployee().getLastName();
				}

				writer.write(recurring.getId() + ";" + recurring.getClient().getFirstName() + ";"
						+ recurring.getClient().getLastName() + ";" + employeeFirstName + ";" + employeeLastName + ";"
						+ recurring.getDayOfWeek() + ";" + recurring.getStartTime() + ";" + recurring.getEndTime() + ";"
						+ recurring.getStartDate() + ";" + recurring.getEndDate() + System.lineSeparator());
			}

		} catch (IOException e) {
			System.out.println("Erreur pendant la sauvegarde des interventions récurrentes.");
			e.printStackTrace();
		}
	}
}
