package gestion.service;

import java.nio.file.Path;
import java.util.ArrayList;

import gestion.model.Employee;
import gestion.model.RecurringIntervention;
import gestion.repository.RecurringInterventionCsvRepository;

public class RecurringInterventionService {

	private ArrayList<RecurringIntervention> recurringInterventions = new ArrayList<>();

	private final RecurringInterventionCsvRepository repository;

	public RecurringInterventionService(ClientService clients, PersonnelService personnel) {
		this(clients, personnel, new RecurringInterventionCsvRepository(Path.of(".")));
	}

	public RecurringInterventionService(ClientService clients, PersonnelService personnel,
			RecurringInterventionCsvRepository repository) {
		this.repository = repository;

		recurringInterventions = repository.load(personnel.getEmployees(), clients.getClients());
	}

	public void assignEmployeeByRecurringId(String recurringId, Employee employee) {

		if (recurringId == null || recurringId.isBlank()) {
			return;
		}

		for (RecurringIntervention recurring : recurringInterventions) {
			if (recurringId.equals(recurring.getId())) {
				recurring.setEmployee(employee);
				saveRecurringInterventions();
				return;
			}
		}
	}

	public void removeRecurringInterventionById(String recurringId) {

		for (int i = recurringInterventions.size() - 1; i >= 0; i--) {

			RecurringIntervention currentIntervention = recurringInterventions.get(i);
			String currentId = currentIntervention.getId();

			if (recurringId.equals(currentId)) {
				recurringInterventions.remove(i);
				break;
			}
		}

		saveRecurringInterventions();
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

	private void saveRecurringInterventions() {
		repository.save(recurringInterventions);
	}
}
