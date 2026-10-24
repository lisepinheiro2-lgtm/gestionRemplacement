package gestion.repository;

import java.nio.file.Path;
import java.time.*;
import java.util.*;

import gestion.model.*;
import gestion.util.DomainValidation;

public final class InterventionCsvRepository {

	private final Path path;

	public InterventionCsvRepository(Path directory) {
		path = CsvStore.directory(directory).resolve("interventions.csv");
	}

	public Path getDirectory() {
		return path.getParent();
	}

	public ArrayList<Intervention> load(List<Employee> employees, List<Client> clients) {

		OverlapIndex overlaps = new OverlapIndex();
		Set<UUID> interventionIds = new HashSet<>();

		return CsvFormat.readUnchecked(path, cells -> {

			CsvFormat.columns(cells, 13);
			UUID interventionUuid = DomainValidation.uuid(cells[10]);
			Client client = CsvReferences.clientById(clients, cells[11]);
			Employee employee = CsvReferences.optionalEmployeeById(employees, cells[12], cells[2], cells[3]);

			Intervention intervention = new Intervention(client, employee, LocalDate.parse(cells[4]),
					LocalTime.parse(cells[5]), LocalDate.parse(cells[6]), LocalTime.parse(cells[7]), interventionUuid);

			if (!interventionIds.add(intervention.getInterventionID())) {
				throw new IllegalArgumentException("UUID intervention en double : " + intervention.getInterventionID());
			}

			if (!cells[8].isEmpty())
				intervention.setRecurringId(cells[8]);
			if (!cells[9].isEmpty())
				intervention.setSourceAbsenceId(cells[9]);
			overlaps.add(intervention);

			return intervention;
		});
	}

	private static final class OverlapIndex {

		private final Map<UUID, NavigableMap<LocalDateTime, DateTimeRange>> employees = new HashMap<>();

		void add(Intervention intervention) {
			DateTimeRange range = DomainValidation.intervention(intervention);
			Employee employee = intervention.getEmployee();

			if (employee == null)
				return;

			NavigableMap<LocalDateTime, DateTimeRange> periods = employees.computeIfAbsent(
					employee.getEmployeeID(), id -> new TreeMap<>());
			var previous = periods.floorEntry(range.start());
			var next = periods.ceilingEntry(range.start());

			if ((previous != null && previous.getValue().end().isAfter(range.start()))
					|| (next != null && next.getKey().isBefore(range.end())))
				throw new IllegalArgumentException("Un salarié ne peut effectuer deux interventions simultanées.");

			periods.put(range.start(), range);
		}
	}

	public void save(List<Intervention> interventions) {
		CsvIdentityMigration.requireCurrentFormat(getDirectory());
		Set<UUID> savedIds = new HashSet<>();

		StringBuilder content = new StringBuilder();
		OverlapIndex overlaps = new OverlapIndex();

		for (Intervention intervention : interventions) {

			overlaps.add(intervention);

			if (!savedIds.add(intervention.getInterventionID()))
				throw new IllegalArgumentException("UUID intervention dupliqué : " + intervention.getInterventionID());

			Employee employee = intervention.getEmployee();

			String employeeId = "";

			if (intervention.getEmployee() != null) {
				employeeId = intervention.getEmployee().getEmployeeID().toString();
			}

			content.append(CsvFormat.row(intervention.getClient().getFirstName(),
					intervention.getClient().getLastName(), employee == null ? "" : employee.getFirstName(),
					employee == null ? "" : employee.getLastName(), intervention.getStartDate(),
					intervention.getStartTime(), intervention.getEndDate(), intervention.getEndTime(),
					intervention.getRecurringId(), intervention.getSourceAbsenceId(),
					intervention.getInterventionID().toString(), intervention.getClient().getClientID().toString(),
					employeeId));
		}

		CsvStore.write(path, content.toString());
	}
}
