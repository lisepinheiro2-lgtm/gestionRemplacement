package gestion.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import gestion.repository.CsvStore;
import gestion.util.DomainValidation;

import gestion.model.Employee;
import gestion.model.RecurringIntervention;
import gestion.repository.RecurringInterventionCsvRepository;

public class RecurringInterventionService {

	private ArrayList<RecurringIntervention> recurringInterventions = new ArrayList<>();

	private final RecurringInterventionCsvRepository repository;
	private final PersonnelService personnel;
	private final ClientService clients;

	public Path getDirectory() {
		return repository.getDirectory();
	}

	private Runnable snapshot() {
		ArrayList<RecurringIntervention> previous = new ArrayList<>(recurringInterventions);
		ArrayList<Employee> employees = new ArrayList<>();

		for (RecurringIntervention recurring : previous)
			employees.add(recurring.getEmployee());
		return () -> {
			recurringInterventions.clear();
			recurringInterventions.addAll(previous);

			for (int i = 0; i < previous.size(); i++)
				previous.get(i).setEmployee(employees.get(i));
		};
	}

	private void change(Runnable mutation) {
		ServiceChanges.apply(getDirectory(), mutation, this::saveRecurringInterventions, snapshot());
	}

	private RecurringIntervention findRequired(String id) {
		DomainValidation.text(id, true);

		return recurringInterventions.stream().filter(r -> id.equals(r.getId())).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Cette série n'existe plus."));
	}

	private void sameDirectory(PlanningService planning) {
		if (!getDirectory().equals(planning.getDirectory()))
			throw new IllegalArgumentException("Dossiers de données différents.");
	}

	public boolean createWithPlanning(PlanningService planning, RecurringIntervention recurring) {
		sameDirectory(planning);
		boolean[] generated = { false };
		CsvStore.transaction(getDirectory(), () -> {
			generated[0] = planning.generateRecurringInterventions(recurring);

			if (generated[0])
				addRecurringIntervention(recurring);
		});

		return generated[0];
	}

	public void assignFutureSeries(PlanningService planning, String id, Employee employee) {
		sameDirectory(planning);
		findRequired(id);
		List<gestion.model.Intervention> targets = planning.getFutureInterventionsByRecurringId(id,
				LocalDateTime.now());

		if (targets.isEmpty())
			throw new IllegalArgumentException("Aucune occurrence future à attribuer ; l'historique est conservé.");
		CsvStore.transaction(getDirectory(), () -> {
			planning.assignEmployee(targets, employee);
			assignEmployeeByRecurringId(id, employee);
		});
	}

	public void removeFutureSeries(PlanningService planning, String id) {
		sameDirectory(planning);
		findRequired(id);
		LocalDateTime now = LocalDateTime.now();

		if (planning.getFutureInterventionsByRecurringId(id, now).isEmpty())
			throw new IllegalArgumentException("Aucune occurrence future à supprimer ; l'historique est conservé.");

		CsvStore.transaction(getDirectory(), () -> {
			planning.removeFutureInterventionsByRecurringId(id, now);

			if (planning.getInterventionsByRecurringId(id).isEmpty())
				removeRecurringInterventionById(id);
		});
	}

	public RecurringInterventionService(ClientService clients, PersonnelService personnel) {
		this(clients, personnel, new RecurringInterventionCsvRepository(Path.of(".")));
	}

	public RecurringInterventionService(ClientService clients, PersonnelService personnel,
			RecurringInterventionCsvRepository repository) {
		this.repository = repository;
		this.clients = clients;
		this.personnel = personnel;

		recurringInterventions = repository.load(personnel.getEmployees(), clients.getClients());
		personnel.protectReferences(
				employee -> recurringInterventions.stream().anyMatch(r -> r.getEmployee() == employee));
		clients.protectReferences(client -> recurringInterventions.stream().anyMatch(r -> r.getClient() == client));
	}

	public void assignEmployeeByRecurringId(String recurringId, Employee employee) {
		RecurringIntervention recurring = findRequired(recurringId);

		if (employee != null)
			personnel.requireKnown(employee);
		change(() -> recurring.setEmployee(employee));
	}

	public void removeRecurringInterventionById(String recurringId) {
		RecurringIntervention recurring = findRequired(recurringId);
		change(() -> recurringInterventions.remove(recurring));
	}

	public void addRecurringIntervention(RecurringIntervention recurring) {
		DomainValidation.recurring(recurring);
		clients.requireKnown(recurring.getClient());

		if (recurring.getEmployee() != null)
			personnel.requireKnown(recurring.getEmployee());

		if (recurringInterventions.stream().anyMatch(r -> r.getId().equals(recurring.getId())))
			throw new IllegalArgumentException("Identifiant de série déjà présent.");

		change(() -> recurringInterventions.add(recurring));
	}

	public void removeRecurringIntervention(int index) {

		if (index < 0 || index >= recurringInterventions.size())
			throw new IllegalArgumentException("Sélectionnez une série.");
		removeRecurringInterventionById(recurringInterventions.get(index).getId());
	}

	public ArrayList<RecurringIntervention> getRecurringInterventions() {
		return recurringInterventions;
	}

	public boolean isEmpty() {
		return recurringInterventions.isEmpty();
	}

	private void saveRecurringInterventions() {
		repository.save(recurringInterventions);
	}
}
