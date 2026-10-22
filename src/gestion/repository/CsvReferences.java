package gestion.repository;

import java.util.List;
import gestion.model.*;

public final class CsvReferences {

	private CsvReferences() {
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

	static Client requiredClient(List<Client> clients, String first, String last) {
		Client client = findClient(clients, first, last);

		if (client == null)
			throw new IllegalArgumentException("Référence client absente.");
		return client;
	}

	static Employee optionalEmployee(List<Employee> employees, String first, String last) {

		if (first.isBlank() && last.isBlank())
			return null;
		if (first.isBlank() || last.isBlank())
			throw new IllegalArgumentException("Référence employé incomplète.");
		Employee employee = findEmployee(employees, first, last);
		if (employee == null)
			throw new IllegalArgumentException("Référence employé absente.");
		return employee;
	}
}
