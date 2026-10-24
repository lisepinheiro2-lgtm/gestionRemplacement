package gestion.repository;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import gestion.repository.CsvIdentityMigration.Row;
import gestion.service.AbsenceService;
import gestion.service.ClientService;
import gestion.service.PersonnelService;
import gestion.service.PlanningService;
import gestion.service.RecurringInterventionService;
import gestion.service.ReplacementService;
import gestion.util.DomainValidation;

final class CsvIdentityValidation {

	private CsvIdentityValidation() {
	}

	static void validate(Path root, Map<String, String> content) throws IOException {
		Map<Path, String> snapshot = new HashMap<>();
		content.forEach((name, text) -> snapshot.put(root.resolve(name), text));
		CsvFormat.withSnapshot(snapshot, () -> validateGraph(root));
	}

	static void validateReferenceIds(Map<String, List<Row>> input) {
		Map<UUID, String> kinds = new HashMap<>();
		Set<UUID> employees = owners(input.get("employees.csv"), 6, "salarié", kinds);
		Set<UUID> clients = owners(input.get("clients.csv"), 6, "client", kinds);
		owners(input.get("interventions.csv"), 10, "intervention", kinds);
		Set<UUID> series = owners(input.get("recurringInterventions.csv"), 0, "série", kinds);
		Set<UUID> absences = owners(input.get("employee_absences.csv"), 0, "absence", kinds);
		Map<UUID, UUID> absentEmployees = new HashMap<>();
		Set<UUID> requests = new HashSet<>();

		for (Row row : input.get("recurringInterventions.csv")) {
			requireReference(row, 10, clients, false);
			requireReference(row, 11, employees, true);
		}

		for (Row row : input.get("employee_absences.csv")) {
			requireReference(row, 8, employees, false);
			absentEmployees.put(row.identity(0), row.identity(8));
		}

		for (Row row : input.get("replacements.csv")) {
			UUID original = row.identity(11);
			register(kinds, original, "salarié");

			if (!row.cells()[12].isEmpty())
				register(kinds, row.identity(12), "salarié");

			requireReference(row, 10, absences, true);
			if (!row.cells()[10].isEmpty()) {
				UUID absence = row.identity(10);
				if (!requests.add(absence) || !original.equals(absentEmployees.get(absence)))
					throw row.failure("Lien d'absence dupliqué ou salarié absent incohérent : " + absence);
			}
		}

		for (Row row : input.get("interventions.csv")) {
			requireReference(row, 11, clients, false);
			requireReference(row, 12, employees, true);
			requireReference(row, 8, series, true);
			requireReference(row, 9, absences, true);

			if (!row.cells()[9].isEmpty() && !requests.contains(row.identity(9)))
				throw row.failure("Demande de remplacement liée introuvable pour l'absence " + row.cells()[9]);
		}
	}

	private static Set<UUID> owners(List<Row> rows, int column, String kind, Map<UUID, String> kinds) {
		Set<UUID> ids = new HashSet<>();
		for (Row row : rows) {
			UUID id = row.identity(column);
			if (!ids.add(id))
				throw row.failure("UUID " + kind + " dupliqué : " + id);
			register(kinds, id, kind);
		}
		return ids;
	}

	private static void requireReference(Row row, int column, Set<UUID> ids, boolean optional) {
		if (optional && row.cells()[column].isEmpty())
			return;

		UUID id = row.identity(column);
		if (!ids.contains(id))
			throw row.failure("UUID référencé introuvable en colonne " + (column + 1) + " : " + id);
	}

	private static void validateGraph(Path root) throws IOException {
		PersonnelService personnel = new PersonnelService(new EmployeeCsvRepository(root));
		ClientService clients = new ClientService(new ClientCsvRepository(root));
		PlanningService planning = new PlanningService(personnel, clients, new InterventionCsvRepository(root));
		RecurringInterventionService recurring = new RecurringInterventionService(clients, personnel,
				new RecurringInterventionCsvRepository(root));
		ReplacementService replacements = new ReplacementService(new ReplacementCsvRepository(root));
		AbsenceService absences = new AbsenceService(personnel, replacements, planning, new AbsenceCsvRepository(root));
		Map<UUID, String> kinds = new HashMap<>();

		personnel.getEmployees().forEach(e -> register(kinds, e.getEmployeeID(), "salarié"));
		clients.getClients().forEach(c -> register(kinds, c.getClientID(), "client"));
		recurring.getRecurringInterventions().forEach(r -> register(kinds, DomainValidation.uuid(r.getId()), "série"));
		absences.getAbsences().forEach(a -> register(kinds, DomainValidation.uuid(a.getId()), "absence"));

		for (var intervention : planning.getInterventions()) {
			register(kinds, intervention.getInterventionID(), "intervention");
			String seriesId = intervention.getRecurringId();

			if (seriesId != null && recurring.getRecurringInterventions().stream().noneMatch(r -> r.getId().equals(seriesId)))
				throw new IOException("interventions.csv : série introuvable " + seriesId
						+ " pour l'intervention " + intervention.getInterventionID());
		}

		for (var replacement : replacements.getReplacements()) {
			register(kinds, replacement.getEmployeeToReplace().getEmployeeID(), "salarié");

			if (replacement.getReplacementEmployee() != null)
				register(kinds, replacement.getReplacementEmployee().getEmployeeID(), "salarié");
		}
	}

	private static void register(Map<UUID, String> kinds, UUID id, String kind) {
		String previous = kinds.putIfAbsent(id, kind);

		if (previous != null && !previous.equals(kind))
			throw new IllegalArgumentException("UUID " + id + " partagé entre " + previous + " et " + kind);
	}
}
