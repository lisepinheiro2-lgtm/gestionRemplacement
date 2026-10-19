package gestionRemplacement.repository;

import gestionRemplacement.model.Employee;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class EmployeeCsvRepository {
	private final Path path;

	public EmployeeCsvRepository(Path directory) {
		path = directory.resolve("employees.csv");
	}

	public ArrayList<Employee> load() {
		ArrayList<Employee> employees = new ArrayList<>();

		File file = path.toFile();

		if (!file.exists()) {
			return employees;
		}

		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;

			while ((line = reader.readLine()) != null) {
				String[] data = line.split(";");

				if (data.length == 6) {

					String firstName = data[0];
					String lastName = data[1];
					double contractHours = Double.parseDouble(data[2]);
					String statut = data[3];
					double assignedHours = Double.parseDouble(data[4]);
					double remainingHours = Double.parseDouble(data[5]);

					employees.add(
							new Employee(firstName, lastName, contractHours, statut, assignedHours, remainingHours));

				} else if (data.length == 3) {
					String firstName = data[0];
					String lastName = data[1];
					double contractHours = Double.parseDouble(data[2]);

					employees.add(new Employee(firstName, lastName, contractHours, "Opérationnel", 0, 0));
				}

			}

		} catch (IOException e) {
			System.out.println("Erreur à la fermeture du fichier employees.");
			e.printStackTrace();

		}
		return employees;
	}

	public void save(List<Employee> employees) {

		try (FileWriter writer = new FileWriter(path.toFile())) {

			for (Employee employee : employees) {
				writer.write(employee.getFirstName() + ";" + employee.getLastName() + ";" + employee.getContractHours()
						+ ";" + employee.getStatut() + ";" + employee.getAssignedHours() + ";"
						+ employee.getRemainingHours() + System.lineSeparator());
			}

		} catch (IOException e) {
			System.out.println("Erreur à la sauvegarde du fichier employees");
			e.printStackTrace();
		}
	}
}
