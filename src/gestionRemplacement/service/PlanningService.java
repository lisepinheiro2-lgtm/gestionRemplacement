package gestionRemplacement.service;

import gestionRemplacement.model.Client;
import gestionRemplacement.model.Employee;
import gestionRemplacement.model.Intervention;
import gestionRemplacement.model.RecurringIntervention;
import gestionRemplacement.repository.InterventionCsvRepository;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PlanningService {

	private ArrayList<Intervention> interventions = new ArrayList<>();

	private final InterventionCsvRepository repository;

	public PlanningService(PersonnelService personnel, ClientService clients) {
		this(personnel, clients, new InterventionCsvRepository(Path.of(".")));
	}

	public PlanningService(PersonnelService personnel, ClientService clients, InterventionCsvRepository repository) {
		this.repository = repository;

		interventions = repository.load(personnel.getEmployees(), clients.getClients());
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

	public ArrayList<Intervention> getInterventionsForDay(LocalDate day) {

		ArrayList<Intervention> interventionsForDay = new ArrayList<>();

		for (Intervention intervention : interventions) {

			if (!day.isBefore(intervention.getStartDate()) && !day.isAfter(intervention.getEndDate())) {
				interventionsForDay.add(intervention);
			}
		}

		return interventionsForDay;
	}

	public boolean hasOverlap(Client client, Intervention interventionToCheck, Intervention interventionToIgnore) {

		LocalDateTime startIntervention;
		LocalDateTime endIntervention;
		LocalDateTime startToCheck = LocalDateTime.of(interventionToCheck.getStartDate(),
				interventionToCheck.getStartTime());
		LocalDateTime endToCheck = LocalDateTime.of(interventionToCheck.getEndDate(), interventionToCheck.getEndTime());

		for (Intervention intervention : interventions) {

			if (intervention == interventionToCheck || intervention == interventionToIgnore) {
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

	public boolean hasOverlap(Client client, Intervention interventionToCheck) {
		return hasOverlap(client, interventionToCheck, null);
	}

	public boolean hasOverlap(Employee employee, Intervention interventionToCheck) {
		return hasOverlap(employee, interventionToCheck, null);
	}

	public boolean hasOverlap(Employee employee, Intervention interventionToCheck, Intervention interventionToIgnore) {

		LocalDateTime startIntervention;
		LocalDateTime endIntervention;
		LocalDateTime startToCheck = LocalDateTime.of(interventionToCheck.getStartDate(),
				interventionToCheck.getStartTime());
		LocalDateTime endToCheck = LocalDateTime.of(interventionToCheck.getEndDate(), interventionToCheck.getEndTime());

		for (Intervention intervention : interventions) {

			if (intervention == interventionToCheck || intervention == interventionToIgnore) {
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

	public ArrayList<Intervention> getInterventionsByRecurringId(String recurringId) {

		ArrayList<Intervention> matches = new ArrayList<>();

		if (recurringId == null || recurringId.isBlank()) {
			return matches;
		}

		for (Intervention current : interventions) {
			if (recurringId.equals(current.getRecurringId())) {
				matches.add(current);
			}
		}

		return matches;
	}

	public void assignEmployee(List<Intervention> interventionsToAssign, Employee employee) {

		for (Intervention current : interventionsToAssign) {
			current.setEmployee(employee);
		}

		saveInterventions();
	}

	public boolean hasOverlap(Employee employee, List<Intervention> interventionsToAssign) {

		ArrayList<Intervention> ordered = new ArrayList<>(interventionsToAssign);

		ordered.sort(Comparator.comparing(current -> LocalDateTime.of(current.getStartDate(), current.getStartTime())));

		LocalDateTime previousEnd = null;

		for (Intervention current : ordered) {
			if (hasOverlap(employee, current)) {
				return true;
			}

			LocalDateTime start = LocalDateTime.of(current.getStartDate(), current.getStartTime());
			LocalDateTime end = LocalDateTime.of(current.getEndDate(), current.getEndTime());

			if (previousEnd != null && start.isBefore(previousEnd)) {
				return true;
			}

			previousEnd = end;
		}

		return false;
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

	private void saveInterventions() {
		repository.save(interventions);
	}
}
