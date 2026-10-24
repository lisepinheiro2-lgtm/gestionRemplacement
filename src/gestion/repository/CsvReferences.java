package gestion.repository;

import java.util.List;
import java.util.UUID;

import gestion.model.*;
import gestion.util.DomainValidation;

public final class CsvReferences {

	private CsvReferences() {
	}

	static Employee employeeById(List<Employee> employees, String value, boolean optional) {
		if (optional && value.isEmpty())
			return null;

		UUID id = DomainValidation.uuid(value);
		List<Employee> matches = employees.stream().filter(e -> e.getEmployeeID().equals(id)).toList();

		if (matches.size() != 1)
			throw new IllegalArgumentException("UUID salarié introuvable ou dupliqué : " + id);

		return matches.getFirst();
	}

	static Employee optionalEmployeeById(List<Employee> employees, String value, String first, String last) {
		if (value.isEmpty() && (!first.isEmpty() || !last.isEmpty()))
			throw new IllegalArgumentException("Référence salarié sans UUID ; attribution non devinée.");

		return employeeById(employees, value, true);
	}

	static Client clientById(List<Client> clients, String value) {
		UUID id = DomainValidation.uuid(value);
		List<Client> matches = clients.stream().filter(c -> c.getClientID().equals(id)).toList();

		if (matches.size() != 1)
			throw new IllegalArgumentException("UUID client introuvable ou dupliqué : " + id);

		return matches.getFirst();
	}

	public static Employee findEmployee(List<Employee> employees, String firstName, String lastName) {
		Employee match = null;

		for (Employee employee : employees) {
			if (employee.getFirstName().equals(firstName) && employee.getLastName().equals(lastName)) {
				if (match != null)
					throw new IllegalArgumentException("Référence employé ambiguë.");
				match = employee;
			}
		}

		return match;
	}

	public static Client findClient(List<Client> clients, String firstName, String lastName) {
		Client match = null;

		for (Client client : clients) {
			if (client.getFirstName().equals(firstName) && client.getLastName().equals(lastName)) {
				if (match != null)
					throw new IllegalArgumentException("Référence client ambiguë.");
				match = client;
			}
		}

		return match;
	}

}
