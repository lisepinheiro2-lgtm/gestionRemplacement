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

	public void addEmployee(String firstName, String lastName, double contractHours) {

		employees.add(new Employee(firstName, lastName, contractHours));

		sortEmployees();
		saveEmployees();
	}

	public void editEmployee(int index, String firstName, String lastName, double contractHours) {

		Employee employee = employees.get(index);

		employee.setFirstName(firstName);
		employee.setLastName(lastName);
		employee.setContractHours(contractHours);

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
						+ System.lineSeparator());
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

				if (data.length == 3) {

					String firstName = data[0];
					String lastName = data[1];
					double contractHours = Double.parseDouble(data[2]);

					employees.add(new Employee(firstName, lastName, contractHours));

				} else if (data.length == 2) {

					String firstName = data[0];
					String lastName = data[1];

					employees.add(new Employee(firstName, lastName, 0));
				}
			}

			reader.close();

		} catch (IOException e) {
			System.out.println("Erreur à la fermeture du fichier employees.");
			e.printStackTrace();
		}
	}

	public boolean isEmpty() {

		return employees.isEmpty();
	}

	public void sortEmployees() {

		employees.sort(Comparator.comparing(Employee::getLastName, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(Employee::getFirstName, String.CASE_INSENSITIVE_ORDER));
	}
}