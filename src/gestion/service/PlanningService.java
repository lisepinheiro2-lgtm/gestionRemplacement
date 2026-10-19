package gestion.service;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import gestion.model.Client;
import gestion.model.DateRange;
import gestion.model.DateTimeRange;
import gestion.model.Employee;
import gestion.model.Intervention;
import gestion.model.RecurringIntervention;
import gestion.repository.InterventionCsvRepository;
import gestion.util.PeriodUtils;

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

	public ArrayList<Intervention> getInterventionsForPeriod(DateRange period) {

		if (period == null) {
			throw new IllegalArgumentException("Veuillez renseigner une période");
		}

		ArrayList<Intervention> interventionsForPeriod = new ArrayList<Intervention>();

		for (Intervention intervention : interventions) {

			DateRange interventionPeriod = new DateRange(intervention.getStartDate(), intervention.getEndDate());

			if (PeriodUtils.hasDateOverlap(interventionPeriod, period)) {
				interventionsForPeriod.add(intervention);
			}
		}

		return interventionsForPeriod;
	}

	public ArrayList<Intervention> getUnassignedInterventionsForPeriod(DateRange period) {

		ArrayList<Intervention> unassignedInterventions = new ArrayList<Intervention>();

		for (Intervention intervention : getInterventionsForPeriod(period)) {

			if (intervention.getEmployee() == null) {
				unassignedInterventions.add(intervention);
			}
		}

		return unassignedInterventions;
	}

	public ArrayList<Intervention> getAssignedInterventionsForPeriod(DateRange period, Employee employee) {

		if (employee == null) {
			throw new IllegalArgumentException("Veuillez renseigner un employee");
		}

		ArrayList<Intervention> assignedInterventions = new ArrayList<Intervention>();

		for (Intervention intervention : getInterventionsForPeriod(period)) {

			if (intervention.getEmployee() == employee) {
				assignedInterventions.add(intervention);
			}
		}

		return assignedInterventions;
	}

	public ArrayList<Intervention> getOtherAssignedInterventionsForPeriod(DateRange period, Employee employee) {

		if (employee == null) {
			throw new IllegalArgumentException("Veuillez renseigner un employee");
		}

		ArrayList<Intervention> assignedAtOtherInterventions = new ArrayList<Intervention>();

		for (Intervention intervention : getInterventionsForPeriod(period)) {

			if (intervention.getEmployee() != null && intervention.getEmployee() != employee) {
				assignedAtOtherInterventions.add(intervention);
			}
		}

		return assignedAtOtherInterventions;
	}

	private boolean interventionsOverlap(Intervention first, Intervention second) {

		if (first == null || second == null) {
			throw new IllegalArgumentException("Intervention invalide");
		}

		LocalDateTime startFirst = LocalDateTime.of(first.getStartDate(), first.getStartTime());
		LocalDateTime endFirst = LocalDateTime.of(first.getEndDate(), first.getEndTime());
		DateTimeRange range1 = new DateTimeRange(startFirst, endFirst);

		LocalDateTime startSecond = LocalDateTime.of(second.getStartDate(), second.getStartTime());
		LocalDateTime endSecond = LocalDateTime.of(second.getEndDate(), second.getEndTime());
		DateTimeRange range2 = new DateTimeRange(startSecond, endSecond);

		return PeriodUtils.hasDateTimeOverlap(range1, range2);

	}

	public boolean hasOverlap(Client client, Intervention interventionToCheck, Intervention interventionToIgnore) {

		for (Intervention intervention : interventions) {

			if (intervention == interventionToCheck || intervention == interventionToIgnore) {
				continue;
			}

			Client interventionClient = intervention.getClient();

			if (interventionClient == null || !interventionClient.equals(client)) {
				continue;
			}

			if (interventionsOverlap(interventionToCheck, intervention)) {
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

			if (interventionsOverlap(interventionToCheck, intervention)) {
				return true;
			}
		}

		return false;
	}

	private void applyAssignments(List<Intervention> targets, Employee newEmployee) {

		ArrayList<Employee> rollBack = new ArrayList<>();

		try {

			for (Intervention current : targets) {

				rollBack.add(current.getEmployee());
				current.setEmployee(newEmployee);
			}

			saveInterventions();

		} catch (UncheckedIOException e) {

			for (int i = 0; i < targets.size(); i++) {
				targets.get(i).setEmployee(rollBack.get(i));
			}
			throw e;
		}
	}

	public void removeClientInterventions(Client client, LocalDate startDate, LocalDate endDate) {

		interventions.removeIf(intervention -> intervention.getClient() == client
				&& !intervention.getEndDate().isBefore(startDate) && !intervention.getStartDate().isAfter(endDate));

		saveInterventions();
	}

	public void unassignEmployeeInterventions(Employee employee, LocalDate startDate, LocalDate endDate) {

		if (employee == null) {
			throw new IllegalArgumentException("Veuillez sélectionner un employé.");
		}

		DateRange period = new DateRange(startDate, endDate);
		ArrayList<Intervention> targets = getAssignedInterventionsForPeriod(period, employee);

		if (targets.isEmpty()) {
			return;
		}

		applyAssignments(targets, null);

	}

	public boolean replaceEmployeeInterventions(Employee employeeToReplace, Employee replacementEmployee,
			LocalDate startDate, LocalDate endDate) {

		if (employeeToReplace == null || replacementEmployee == null) {
			throw new IllegalArgumentException("Veuillez sélectionner un employé.");
		}

		if (employeeToReplace == replacementEmployee) {
			return true;
		}

		DateRange period = new DateRange(startDate, endDate);
		ArrayList<Intervention> interventionsToAssign = getAssignedInterventionsForPeriod(period, employeeToReplace);

		if (interventionsToAssign.isEmpty()) {
			return true;
		}

		if (hasOverlap(replacementEmployee, interventionsToAssign)) {
			return false;
		}

		applyAssignments(interventionsToAssign, replacementEmployee);

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

		if (employee == null) {
			throw new IllegalArgumentException("Veuillez choisir un employé");
		}

		if (interventionsToAssign == null || interventionsToAssign.isEmpty()) {
			throw new IllegalArgumentException("Veuillez sélectionner au moins une intervention.");
		}

		HashSet<Intervention> seen = new HashSet<>();

		for (Intervention current : interventionsToAssign) {

			if (current == null) {
				throw new IllegalArgumentException("Attribution impossible : une des intervention est null");
			}

			if (!interventions.contains(current)) {
				throw new IllegalArgumentException("Attribution impossible : cette intervention n'existe pas");
			}

			if (!seen.add(current)) {
				throw new IllegalArgumentException("Intervention en double");
			}
		}

		if (hasOverlap(employee, interventionsToAssign)) {
			throw new IllegalArgumentException(
					"Attribution impossible : cet employé a déjà une intervention sur un des créneaux.");
		}

		applyAssignments(interventionsToAssign, employee);
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

		if (intervention == null) {
			throw new IllegalArgumentException("Veuillez choisir une intervention.");
		}

		List<Intervention> interventionToAssign = List.of(intervention);

		assignEmployee(interventionToAssign, employee);
	}

	public void unassignEmployee(Intervention intervention) {

		if (intervention == null) {
			throw new IllegalArgumentException("Veuillez choisir une intervention.");
		}

		if (!interventions.contains(intervention)) {
			throw new IllegalArgumentException("Cette intervention n'existe pas.");
		}

		List<Intervention> interventionToAssign = List.of(intervention);
		applyAssignments(interventionToAssign, null);
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
