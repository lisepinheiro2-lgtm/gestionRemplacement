package gestion.repository;

import java.nio.file.Path;
import java.time.*;
import java.util.*;

import gestion.model.*;
import gestion.util.DomainValidation;

public final class RecurringInterventionCsvRepository {

	private final Path path;

	public RecurringInterventionCsvRepository(Path directory) {
		path = CsvStore.directory(directory).resolve("recurringInterventions.csv");
	}

	public Path getDirectory() {
		return path.getParent();
	}

	public ArrayList<RecurringIntervention> load(List<Employee> employees, List<Client> clients) {

		Set<UUID> ids = new HashSet<>();

		return CsvFormat.readUnchecked(path, cells -> {
			CsvFormat.columns(cells, 12);
			Client client = CsvReferences.clientById(clients, cells[10]);
			Employee employee = CsvReferences.optionalEmployeeById(employees, cells[11], cells[3], cells[4]);

			RecurringIntervention recurring = new RecurringIntervention(client, employee, DayOfWeek.valueOf(cells[5]),
					LocalTime.parse(cells[6]), LocalTime.parse(cells[7]), LocalDate.parse(cells[8]),
					LocalDate.parse(cells[9]));

			recurring.setId(cells[0]);
			DomainValidation.recurring(recurring);

			if (!ids.add(DomainValidation.uuid(recurring.getId())))
				throw new IllegalArgumentException("Identifiant de série dupliqué.");
			return recurring;
		});
	}

	public void save(List<RecurringIntervention> recurringInterventions) {
		CsvIdentityMigration.requireCurrentFormat(getDirectory());

		StringBuilder content = new StringBuilder();
		Set<UUID> ids = new HashSet<>();

		for (RecurringIntervention recurring : recurringInterventions) {
			DomainValidation.recurring(recurring);

			if (!ids.add(DomainValidation.uuid(recurring.getId())))
				throw new IllegalArgumentException("Identifiant de série dupliqué.");
			Employee employee = recurring.getEmployee();
			content.append(CsvFormat.row(recurring.getId(), recurring.getClient().getFirstName(),
					recurring.getClient().getLastName(), employee == null ? "" : employee.getFirstName(),
					employee == null ? "" : employee.getLastName(), recurring.getDayOfWeek(), recurring.getStartTime(),
					recurring.getEndTime(), recurring.getStartDate(), recurring.getEndDate(),
					recurring.getClient().getClientID().toString(),
					employee == null ? "" : employee.getEmployeeID().toString()));
		}

		CsvStore.write(path, content.toString());
	}
}
