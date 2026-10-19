package gestion.repository;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import gestion.model.Client;
import gestion.model.Employee;
import gestion.model.Intervention;

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

		Path temporaryFile = null;

		try {

			temporaryFile = Files.createTempFile(path.toAbsolutePath().getParent(), "interventions-", ".tmp");

			try (FileWriter writer = new FileWriter(temporaryFile.toFile())) {

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
							+ employeeLastName + ";" + intervention.getStartDate() + ";" + intervention.getStartTime()
							+ ";" + intervention.getEndDate() + ";" + intervention.getEndTime() + ";" + recurringId
							+ System.lineSeparator());
				}
			}

			Files.move(temporaryFile, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

		} catch (IOException e) {
			throw new UncheckedIOException("Impossible de sauvegarder les interventions", e);

		} finally {

			if (temporaryFile != null) {

				try {

					Files.deleteIfExists(temporaryFile);

				} catch (IOException e) {
					System.err.println("Impossible de supprimer le fichier temporaire : " + e.getMessage());
				}
			}
		}
	}
}
