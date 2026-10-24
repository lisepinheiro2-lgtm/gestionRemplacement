package gestion.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

import gestion.model.Employee;
import gestion.repository.CsvReferences;
import gestion.repository.CsvStore;
import gestion.repository.EmployeeCsvRepository;
import gestion.util.DomainValidation;

public class PersonnelService {

	private ArrayList<Employee> employees = new ArrayList<>();
	private final EmployeeCsvRepository repository;
	private final List<Predicate<Employee>> referenceChecks = new ArrayList<>();

	public Path getDirectory() {
		return repository.getDirectory();
	}

	public void protectReferences(Predicate<Employee> check) {
		referenceChecks.add(check);
	}

	public void requireKnown(Employee person) {
		if (person == null || !employees.contains(person))
			throw new IllegalArgumentException("Veuillez choisir une personne de la liste actuelle.");
	}

	private boolean referenced(Employee person) {
		return referenceChecks.stream().anyMatch(check -> check.test(person));
	}

	private void validateIdentity(Employee candidate, Employee original) {
		DomainValidation.employee(candidate);

		for (Employee person : employees)
			if (person != original && person.getFirstName().equals(candidate.getFirstName())
					&& person.getLastName().equals(candidate.getLastName()))
				throw new IllegalArgumentException(
						"Ce prénom et ce nom existent déjà ; les homonymes ne sont pas encore pris en charge.");

	}

	private void changeList(Consumer<ArrayList<Employee>> mutation) {
		ArrayList<Employee> previous = new ArrayList<>(employees);
		ServiceChanges.apply(getDirectory(), () -> mutation.accept(employees), this::saveEmployees, () -> {
			employees.clear();
			employees.addAll(previous);
		});
	}

	public PersonnelService() {
		this(new EmployeeCsvRepository(Path.of(".")));
	}

	public PersonnelService(EmployeeCsvRepository repository) {
		this.repository = repository;

		employees = repository.load();
		sortEmployees();
	}

	public void addEmployee(String firstName, String lastName, double contractHours, String statut,
			double assignedHours, double remainingHours) {

		Employee candidate = new Employee(firstName, lastName, contractHours, statut, assignedHours, remainingHours);
		validateIdentity(candidate, null);
		changeList(list -> {
			list.add(candidate);
			sortEmployees();
		});
	}

	public void editEmployee(int index, String firstName, String lastName, double contractHours, String statut,
			double assignedHours, double remainingHours) {

		if (index < 0 || index >= employees.size())
			throw new IllegalArgumentException("Veuillez sélectionner un employé.");
		Employee employee = employees.get(index);
		Employee candidate = new Employee(firstName, lastName, contractHours, statut, assignedHours, remainingHours,
				employee.getEmployeeID());
		validateIdentity(candidate, employee);
		Employee previous = new Employee(employee.getFirstName(), employee.getLastName(), employee.getContractHours(),
				employee.getStatut(), employee.getAssignedHours(), employee.getRemainingHours(), employee.getEmployeeID());
		ArrayList<Employee> order = new ArrayList<>(employees);
		ServiceChanges.apply(getDirectory(), () -> {
			copyValues(candidate, employee);
			sortEmployees();
		}, this::saveEmployees, () -> {
			copyValues(previous, employee);
			employees.clear();
			employees.addAll(order);
		});
	}

	private static void copyValues(Employee source, Employee target) {

		target.setFirstName(source.getFirstName());
		target.setLastName(source.getLastName());
		target.setContractHours(source.getContractHours());
		target.setStatut(source.getStatut());
		target.setAssignedHours(source.getAssignedHours());
		target.setRemainingHours(source.getRemainingHours());
	}

	public void removeEmployee(int index) {
		if (index < 0 || index >= employees.size())

			throw new IllegalArgumentException("Veuillez sélectionner un employé.");
		if (referenced(employees.get(index)))
			throw new IllegalArgumentException(
					"Suppression refusée : cet employé est encore référencé par le planning, une série, une absence ou un remplacement.");

		changeList(list -> list.remove(index));
	}

	public ArrayList<Employee> getEmployees() {
		return employees;
	}

	public Employee findEmployee(Employee employeeToFind) {

		if (employeeToFind == null) {
			return null;
		}

		return findEmployee(employeeToFind.getEmployeeID());
	}

	public Employee findEmployee(UUID id) {
		Employee found = null;

		for (Employee employee : employees) {
			if (employee.getEmployeeID().equals(id)) {
				if (found != null)
					throw new IllegalArgumentException("UUID salarié dupliqué : " + id);
				found = employee;
			}
		}

		return found;
	}

	public Employee findEmployee(String firstName, String lastName) {
		return CsvReferences.findEmployee(employees, firstName, lastName);
	}

	public boolean isEmpty() {
		return employees.isEmpty();
	}

	public void sortEmployees() {
		employees.sort(Comparator.comparing(Employee::getLastName, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(Employee::getFirstName, String.CASE_INSENSITIVE_ORDER));
	}

	private void saveEmployees() {
		repository.save(employees);
	}
}
