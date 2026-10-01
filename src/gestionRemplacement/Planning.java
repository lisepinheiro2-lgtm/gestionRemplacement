package gestionRemplacement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;

public class Planning {

	private ArrayList<Intervention> interventions = new ArrayList<>();

	private final String fileName = "interventions.csv";
	private PersonnelList personnel;
	private ClientList clients;

	public Planning(PersonnelList personnel, ClientList clients) {

		this.personnel = personnel;
		this.clients = clients;

		loadInterventions();
	}

	public void addIntervention(Intervention intervention) {
		interventions.add(intervention);
		saveInterventions();
	}

	public void editIntervention(int index, Client client, Employee employee, LocalDate startDate, LocalTime startTime,
			LocalDate endDate, LocalTime endTime) {

		Intervention intervention = interventions.get(index);

		intervention.setClient(client);
		intervention.setEmployee(employee);
		intervention.setStartDate(startDate);
		intervention.setStartTime(startTime);
		intervention.setEndDate(endDate);
		intervention.setEndTime(endTime);

		saveInterventions();
	}

	private Client findClient(String firstName, String lastName) {

		for (Client client : clients.getClients()) {

			if (client.getFirstName().equals(firstName) && client.getLastName().equals(lastName)) {
				return client;
			}
		}

		return null;
	}

	private void loadInterventions() {

		File file = new File(fileName);

		if (!file.exists()) {
			return;
		}

		try {

			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;

			while ((line = reader.readLine()) != null) {

				String[] data = line.split(";", -1);

				if (data.length == 9 || data.length == 8) {

					Client client = findClient(data[0], data[1]);
					Employee employee = null;

					if (!data[2].isBlank() && !data[3].isBlank()) {
						employee = personnel.findEmployee(data[2], data[3]);
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

			reader.close();

		} catch (IOException e) {
			System.out.println("Erreur au chargement du planning.");
			e.printStackTrace();
		}
	}

	public double getTotalHours(Employee employee) {

		double totalHours = 0;

		for (Intervention intervention : interventions) {

			Employee assignedEmployee = intervention.getEmployee();

			if (assignedEmployee != null && assignedEmployee.getFirstName().equals(employee.getFirstName())
					&& assignedEmployee.getLastName().equals(employee.getLastName())) {

				totalHours += intervention.getDurationHours();
			}
		}

		return totalHours;
	}

	private void saveInterventions() {

		try {

			FileWriter writer = new FileWriter(fileName);

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

			writer.close();

		} catch (IOException e) {
			System.out.println("Erreur pendant la sauvegarde du planning.");
			e.printStackTrace();
		}
	}

	public ArrayList<Intervention> getInterventionsForDay(LocalDate day) {

		ArrayList<Intervention> interventionsForDay = new ArrayList<>();

		for (Intervention intervention : interventions) {

			if (!day.isBefore(intervention.getStartDate()) && !day.isAfter(intervention.getEndDate())) {
				interventionsForDay.add(intervention);
			}
		}

		return interventionsForDay;
	}

	public double getWeeklyHours(Employee employee, LocalDate date) {

		LocalDate monday = date.with(DayOfWeek.MONDAY);
		LocalDate sunday = monday.plusDays(6);
		double totalHours = 0;

		for (Intervention intervention : interventions) {

			Employee assignedEmployee = intervention.getEmployee();

			if (assignedEmployee != null && assignedEmployee.getFirstName().equals(employee.getFirstName())
					&& assignedEmployee.getLastName().equals(employee.getLastName())
					&& !intervention.getStartDate().isAfter(sunday) && !intervention.getEndDate().isBefore(monday)) {

				totalHours += intervention.getDurationHours();
			}
		}

		return totalHours;
	}

	public boolean hasOverlap(Client client, Intervention interventionToCheck) {

		LocalDateTime startIntervention;
		LocalDateTime endIntervention;
		LocalDateTime startToCheck = LocalDateTime.of(interventionToCheck.getStartDate(),
				interventionToCheck.getStartTime());
		LocalDateTime endToCheck = LocalDateTime.of(interventionToCheck.getEndDate(), interventionToCheck.getEndTime());

		for (Intervention intervention : interventions) {

			if (intervention == interventionToCheck) {
				continue;
			}

			Client interventionClient = intervention.getClient();

			if (interventionClient == null || !interventionClient.equals(client)) {
				continue;
			}

			startIntervention = LocalDateTime.of(intervention.getStartDate(), intervention.getStartTime());
			endIntervention = LocalDateTime.of(intervention.getEndDate(), intervention.getEndTime());

			if (startToCheck.isBefore(endIntervention) && endToCheck.isAfter(startIntervention)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasOverlap(Employee employee, Intervention interventionToCheck) {

		LocalDateTime startIntervention;
		LocalDateTime endIntervention;
		LocalDateTime startToCheck = LocalDateTime.of(interventionToCheck.getStartDate(),
				interventionToCheck.getStartTime());
		LocalDateTime endToCheck = LocalDateTime.of(interventionToCheck.getEndDate(), interventionToCheck.getEndTime());

		for (Intervention intervention : interventions) {

			if (intervention == interventionToCheck) {
				continue;
			}

			Employee assignedEmployee = intervention.getEmployee();

			if (assignedEmployee == null) {
				continue;
			}

			if (!assignedEmployee.getFirstName().equals(employee.getFirstName())
					|| !assignedEmployee.getLastName().equals(employee.getLastName())) {
				continue;
			}

			startIntervention = LocalDateTime.of(intervention.getStartDate(), intervention.getStartTime());
			endIntervention = LocalDateTime.of(intervention.getEndDate(), intervention.getEndTime());

			if (startToCheck.isBefore(endIntervention) && endToCheck.isAfter(startIntervention)) {
				return true;
			}
		}
		return false;
	}

	public void removeClientInterventions(Client client, LocalDate startDate, LocalDate endDate) {

		interventions.removeIf(intervention -> intervention.getClient() == client
				&& !intervention.getEndDate().isBefore(startDate) && !intervention.getStartDate().isAfter(endDate));

		saveInterventions();
	}

	public void unassignEmployeeInterventions(Employee employee, LocalDate startDate, LocalDate endDate) {

		for (Intervention intervention : interventions) {

			if (intervention.getEmployee() == employee && !intervention.getEndDate().isBefore(startDate)
					&& !intervention.getStartDate().isAfter(endDate)) {

				intervention.setEmployee(null);
			}
		}

		saveInterventions();
	}

	public boolean replaceEmployeeInterventions(Employee employeeToReplace, Employee replacementEmployee,
			LocalDate startDate, LocalDate endDate) {

		for (Intervention intervention : interventions) {

			if (intervention.getEmployee() == employeeToReplace && !intervention.getEndDate().isBefore(startDate)
					&& !intervention.getStartDate().isAfter(endDate)) {
				if (hasOverlap(replacementEmployee, intervention)) {
					return false;
				}
			}
		}

		for (Intervention intervention : interventions) {

			if (intervention.getEmployee() == employeeToReplace && !intervention.getEndDate().isBefore(startDate)
					&& !intervention.getStartDate().isAfter(endDate)) {

				intervention.setEmployee(replacementEmployee);
			}
		}

		saveInterventions();

		return true;
	}

	public boolean generateRecurringInterventions(RecurringIntervention recurring) {

		ArrayList<Intervention> generatedInterventions = new ArrayList<>();
		LocalDate date = recurring.getStartDate();

		while (!date.isAfter(recurring.getEndDate())) {

			if (date.getDayOfWeek() == recurring.getDayOfWeek()) {
				Intervention intervention = new Intervention(recurring.getClient(), recurring.getEmployee(), date,
						recurring.getStartTime(), date, recurring.getEndTime());
				intervention.setRecurringId(recurring.getId());

				if (recurring.getEmployee() != null && hasOverlap(recurring.getEmployee(), intervention)) {
					return false;
				}
				generatedInterventions.add(intervention);
			}
			date = date.plusDays(1);
		}

		interventions.addAll(generatedInterventions);
		saveInterventions();

		return true;
	}

	public void removeInterventionsByRecurringId(String recurringId) {

		for (int i = interventions.size() - 1; i >= 0; i--) {

			Intervention currentIntervention = interventions.get(i);
			String currentId = currentIntervention.getRecurringId();

			if (recurringId.equals(currentId)) {
				interventions.remove(i);
			}
		}

		saveInterventions();
	}

	public void assignEmployee(Intervention intervention, Employee employee) {
		intervention.setEmployee(employee);
		saveInterventions();
	}

	public void unassignEmployee(Intervention intervention) {
		intervention.setEmployee(null);
		saveInterventions();
	}

	public void removeIntervention(int index) {
		interventions.remove(index);
		saveInterventions();
	}

	public void removeIntervention(Intervention intervention) {
		interventions.remove(intervention);
		saveInterventions();
	}

	public ArrayList<Intervention> getInterventions() {
		return interventions;
	}

	public boolean isEmpty() {
		return interventions.isEmpty();
	}
}