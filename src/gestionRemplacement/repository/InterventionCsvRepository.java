package gestionRemplacement.repository;

import gestionRemplacement.model.Client;
import gestionRemplacement.model.Employee;
import gestionRemplacement.model.Intervention;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class InterventionCsvRepository {
	private final Path path;

	public InterventionCsvRepository(Path directory) {
		path = directory.resolve("interventions.csv");
	}

	public ArrayList<Intervention> load(List<Employee> employees, List<Client> clients) {
		ArrayList<Intervention> interventions = new ArrayList<>();

		File file = path.toFile();

		if (!file.exists()) {
			return interventions;
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;

			while ((line = reader.readLine()) != null) {

				String[] data = line.split(";", -1);

				if (data.length == 9 || data.length == 8) {

					Client client = CsvReferences.findClient(clients, data[0], data[1]);
					Employee employee = null;

					if (!data[2].isBlank() && !data[3].isBlank()) {
						employee = CsvReferences.findEmployee(employees, data[2], data[3]);
					}

					LocalDate startDate = LocalDate.parse(data[4]);
					LocalTime startTime = LocalTime.parse(data[5]);
					LocalDate endDate = LocalDate.parse(data[6]);
					LocalTime endTime = LocalTime.parse(data[7]);

					if (client != null) {
						Intervention intervention = new Intervention(client, employee, startDate, startTime, endDate,
								endTime);

						if (data.length == 9 && !data[8].isBlank()) {
							intervention.setRecurringId(data[8]);
						}

						interventions.add(intervention);
					}
				}
			}

		} catch (IOException e) {
			System.out.println("Erreur au chargement du planning.");
			e.printStackTrace();
		}
		return interventions;
	}

	public void save(List<Intervention> interventions) {

		try (FileWriter writer = new FileWriter(path.toFile())) {

			for (Intervention intervention : interventions) {

				Client client = intervention.getClient();
				Employee employee = intervention.getEmployee();

				String employeeFirstName = "";
				String employeeLastName = "";

				if (employee != null) {
					employeeFirstName = employee.getFirstName();
					employeeLastName = employee.getLastName();
				}

				String recurringId = "";

				if (intervention.getRecurringId() != null) {
					recurringId = intervention.getRecurringId();
				}

				writer.write(client.getFirstName() + ";" + client.getLastName() + ";" + employeeFirstName + ";"
						+ employeeLastName + ";" + intervention.getStartDate() + ";" + intervention.getStartTime() + ";"
						+ intervention.getEndDate() + ";" + intervention.getEndTime() + ";" + recurringId
						+ System.lineSeparator());
			}

		} catch (IOException e) {
			System.out.println("Erreur pendant la sauvegarde du planning.");
			e.printStackTrace();
		}
	}
}
