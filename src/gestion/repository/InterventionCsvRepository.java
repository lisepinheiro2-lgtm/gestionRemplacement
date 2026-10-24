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

		ArrayList<Intervention> checked = new ArrayList<>();
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
			validateOverlap(intervention, checked);
			checked.add(intervention);

			return intervention;
		});
	}

	private static void validateOverlap(Intervention candidate, List<Intervention> checked) {

		DateTimeRange range = DomainValidation.intervention(candidate);

		for (Intervention other : checked)
			if (candidate.getEmployee() != null && other.getEmployee() != null
					&& candidate.getEmployee().getEmployeeID().equals(other.getEmployee().getEmployeeID())
					&& gestion.util.PeriodUtils.hasDateTimeOverlap(range, DomainValidation.intervention(other)))
				throw new IllegalArgumentException("Un salarié ne peut effectuer deux interventions simultanées.");
	}

	public void save(List<Intervention> interventions) {
		CsvIdentityMigration.requireCurrentFormat(getDirectory());
		Set<UUID> savedIds = new HashSet<>();

		StringBuilder content = new StringBuilder();
		ArrayList<Intervention> checked = new ArrayList<>();

		for (Intervention intervention : interventions) {

			DomainValidation.intervention(intervention);

			if (!savedIds.add(intervention.getInterventionID()))
				throw new IllegalArgumentException("UUID intervention dupliqué : " + intervention.getInterventionID());

			validateOverlap(intervention, checked);
			checked.add(intervention);
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
