package gestionRemplacement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;

public class PersonnelList {

	private ArrayList<Employee> employees = new ArrayList<>();
	private final String fileName = "employees.csv";

	public PersonnelList() {

		loadEmployees();
		sortEmployees();
	}

	public void addEmployee(String firstName, String lastName, double contractHours, String statut,
			double attribuateHours, double nonAttribuateHours) {

		employees.add(new Employee(firstName, lastName, contractHours, statut, attribuateHours, nonAttribuateHours));

		sortEmployees();
		saveEmployees();
	}

	public void editEmployee(int index, String firstName, String lastName, double contractHours, String statut,
			double attribuateHours, double nonAttribuateHours) {

		Employee employee = employees.get(index);

		employee.setFirstName(firstName);
		employee.setLastName(lastName);
		employee.setContractHours(contractHours);
		employee.setStatut(statut);
		employee.setAttribuateHours(attribuateHours);
		employee.setNonAttribuateHours(nonAttribuateHours);

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

	private void saveEmployees() {

		try {

			FileWriter writer = new FileWriter(fileName);

			for (Employee employee : employees) {
				writer.write(employee.getFirstName() + ";" + employee.getLastName() + ";" + employee.getContractHours()
						+ ";" + employee.getStatut() + ";" + employee.getAttribuateHours() + ";"
						+ employee.getNonAttribuateHours() + System.lineSeparator());
			}

			writer.close();

		} catch (IOException e) {
			System.out.println("Erreur à la sauvegarde du fichier employees");
			e.printStackTrace();
		}
	}

	private void loadEmployees() {

		File file = new File(fileName);

		if (!file.exists()) {
			return;
		}

		try {

			BufferedReader reader = new BufferedReader(new FileReader(file));
			String line;

			while ((line = reader.readLine()) != null) {
				String[] data = line.split(";");

				if (data.length == 6) {

					String firstName = data[0];
					String lastName = data[1];
					double contractHours = Double.parseDouble(data[2]);
					String statut = data[3];
					double attribuateHours = Double.parseDouble(data[4]);
					double nonAttribuateHours = Double.parseDouble(data[5]);

					employees.add(new Employee(firstName, lastName, contractHours, statut, attribuateHours,
							nonAttribuateHours));

				} else if (data.length == 3) {
					String firstName = data[0];
					String lastName = data[1];
					double contractHours = Double.parseDouble(data[2]);
					
					employees.add(new Employee(firstName, lastName, contractHours, "Opérationnel", 0, 0));
				}

			}
			reader.close();

		} catch (IOException e) {
			System.out.println("Erreur à la fermeture du fichier employees.");
			e.printStackTrace();

		}
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
}