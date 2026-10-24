package gestion.repository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import gestion.model.Employee;
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

		Set<UUID> employeeIds = new HashSet<>();

		return CsvFormat.readUnchecked(path, cells -> {
			CsvFormat.columns(cells, 7);
			UUID employeeUuid = DomainValidation.uuid(cells[6]);

			Employee employee = new Employee(cells[0], cells[1], Double.parseDouble(cells[2]),
					cells[3], Double.parseDouble(cells[4]), Double.parseDouble(cells[5]), employeeUuid);

			if (!employeeIds.add(employee.getEmployeeID())) {
				throw new IllegalArgumentException("UUID salarié en double : " + employee.getEmployeeID());
			}

			DomainValidation.employee(employee);

			return employee;
		});
	}

	public void save(List<Employee> employees) {
		CsvIdentityMigration.requireCurrentFormat(getDirectory());

		StringBuilder content = new StringBuilder();
		Set<UUID> savedIds = new HashSet<>();

		for (Employee employee : employees) {
			DomainValidation.employee(employee);

			if (!savedIds.add(employee.getEmployeeID()))
				throw new IllegalArgumentException("UUID salarié dupliqué : " + employee.getEmployeeID());
			content.append(CsvFormat.row(employee.getFirstName(), employee.getLastName(), employee.getContractHours(),
					employee.getStatut(), employee.getAssignedHours(), employee.getRemainingHours(),
					employee.getEmployeeID().toString()));
		}

		CsvStore.write(path, content.toString());
	}
}
