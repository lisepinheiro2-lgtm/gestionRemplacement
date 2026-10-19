package gestion.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;

import gestion.model.Employee;
import gestion.repository.EmployeeCsvRepository;

public class PersonnelService {

	private ArrayList<Employee> employees = new ArrayList<>();
	private final EmployeeCsvRepository repository;

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

		employees.add(new Employee(firstName, lastName, contractHours, statut, assignedHours, remainingHours));

		sortEmployees();
		saveEmployees();
	}

	public void editEmployee(int index, String firstName, String lastName, double contractHours, String statut,
			double assignedHours, double remainingHours) {

		Employee employee = employees.get(index);

		employee.setFirstName(firstName);
		employee.setLastName(lastName);
		employee.setContractHours(contractHours);
		employee.setStatut(statut);
		employee.setAssignedHours(assignedHours);
		employee.setRemainingHours(remainingHours);

		saveEmployees();
		sortEmployees();
	}

	public void removeEmployee(int index) {

		employees.remove(index);

		saveEmployees();
	}

	public ArrayList<Employee> getEmployees() {
		return employees;
	}

	public Employee findEmployee(Employee employeeToFind) {

		if (employeeToFind == null) {
			return null;
		}

		return findEmployee(employeeToFind.getFirstName(), employeeToFind.getLastName());
	}

	public Employee findEmployee(String firstName, String lastName) {

		for (Employee employee : employees) {

			if (employee.getFirstName().equals(firstName) && employee.getLastName().equals(lastName)) {

				return employee;
			}
		}

		return null;
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
