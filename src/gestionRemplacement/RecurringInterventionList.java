package gestionRemplacement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;

public class RecurringInterventionList {

	private ArrayList<RecurringIntervention> recurringInterventions = new ArrayList<>();

	private final String fileName = "recurringInterventions.csv";

	private ClientList clients;
	private PersonnelList personnel;

	public RecurringInterventionList(ClientList clients, PersonnelList personnel) {

		this.clients = clients;
		this.personnel = personnel;

		loadRecurringInterventions();
	}

	private Client findClient(String firstName, String lastName) {

		for (Client client : clients.getClients()) {

			if (client.getFirstName().equals(firstName) && client.getLastName().equals(lastName)) {
				return client;
			}
		}

		return null;
	}

	private Employee findEmployee(String firstName, String lastName) {

		for (Employee employee : personnel.getEmployees()) {

			if (employee.getFirstName().equals(firstName) && employee.getLastName().equals(lastName)) {
				return employee;
			}
		}

		return null;
	}

	private void loadRecurringInterventions() {

		File file = new File(fileName);

		if (!file.exists()) {
			return;
		}

		try {

			BufferedReader reader = new BufferedReader(new FileReader(file));

			String line;

			while ((line = reader.readLine()) != null) {

				String[] data = line.split(";", -1);

				if (data.length == 10) {

					String id = data[0];
					Client client = findClient(data[1], data[2]);
					Employee employee = null;

					if (!data[3].isBlank() && !data[4].isBlank()) {
						employee = findEmployee(data[3], data[4]);
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

			reader.close();

		} catch (IOException e) {
			System.out.println("Erreur au chargement des interventions récurrentes.");
			e.printStackTrace();
		}
	}

	private void saveRecurringInterventions() {

		try {

			FileWriter writer = new FileWriter(fileName);

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

			writer.close();

		} catch (IOException e) {
			System.out.println("Erreur pendant la sauvegarde des interventions récurrentes.");
			e.printStackTrace();
		}
	}

	public void addRecurringIntervention(RecurringIntervention recurringIntervention) {
		recurringInterventions.add(recurringIntervention);
		saveRecurringInterventions();
	}

	public void removeRecurringIntervention(int index) {
	    recurringInterventions.remove(index);
	    saveRecurringInterventions();
	}

	public ArrayList<RecurringIntervention> getRecurringInterventions() {
		return recurringInterventions;
	}

	public boolean isEmpty() {
		return recurringInterventions.isEmpty();
	}
}