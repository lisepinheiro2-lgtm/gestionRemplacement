package gestion.repository;

import java.nio.file.Path;
import java.time.*;
import java.util.*;
import gestion.model.*;
import gestion.util.DomainValidation;

public final class EmployeeCsvRepository {

	private final Path path;

	public EmployeeCsvRepository(Path directory) {
		path = CsvStore.directory(directory).resolve("employees.csv");
	}

	public Path getDirectory() {
		return path.getParent();
	}

	public ArrayList<Employee> load() {

		Set<String> keys = new HashSet<>();

		return CsvFormat.readUnchecked(path, cells -> {
			CsvFormat.columns(cells, 3, 6);
			Employee employee = new Employee(cells[0], cells[1], Double.parseDouble(cells[2]),
					cells.length == 6 ? cells[3] : "Opérationnel", cells.length == 6 ? Double.parseDouble(cells[4]) : 0,
					cells.length == 6 ? Double.parseDouble(cells[5]) : 0);
			DomainValidation.employee(employee);

			if (!keys.add(cells[0] + ";" + cells[1]))
				throw new IllegalArgumentException("Identité ambiguë.");
			return employee;
		});
	}

	public void save(List<Employee> employees) {

		StringBuilder content = new StringBuilder();
		Set<String> keys = new HashSet<>();

		for (Employee employee : employees) {
			DomainValidation.employee(employee);

			if (!keys.add(employee.getFirstName() + ";" + employee.getLastName()))
				throw new IllegalArgumentException("Homonymes non pris en charge par ce format CSV.");
			content.append(CsvFormat.row(employee.getFirstName(), employee.getLastName(), employee.getContractHours(),
					employee.getStatut(), employee.getAssignedHours(), employee.getRemainingHours()));
		}

		CsvStore.write(path, content.toString());
	}
}
