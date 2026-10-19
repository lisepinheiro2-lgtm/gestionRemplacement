package gestionRemplacement.repository;

import gestionRemplacement.model.Client;
import gestionRemplacement.model.Employee;
import java.util.List;

public final class CsvReferences {
	private CsvReferences() {
	}

	public static Employee findEmployee(List<Employee> employees, String firstName, String lastName) {
		for (Employee employee : employees) {
			if (employee.getFirstName().equals(firstName) && employee.getLastName().equals(lastName))
				return employee;
		}
		return null;
	}

	public static Client findClient(List<Client> clients, String firstName, String lastName) {
		for (Client client : clients) {
			if (client.getFirstName().equals(firstName) && client.getLastName().equals(lastName))
				return client;
		}
		return null;
	}
}
