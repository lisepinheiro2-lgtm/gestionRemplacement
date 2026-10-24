package gestion.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import gestion.repository.CsvIdentityMigration.Row;
import gestion.util.DomainValidation;

final class CsvLegacyIdentities {

	private final Map<String, Set<String>> employees = new HashMap<>();
	private final Map<String, Set<String>> clients = new HashMap<>();
	private final Map<String, Set<String>> historicalEmployees = new HashMap<>();
	private final Map<String, Set<String>> employeeReferences = new HashMap<>();
	private final Map<String, Set<String>> clientReferences = new HashMap<>();
	private final Set<UUID> employeeIds = new LinkedHashSet<>();
	private final Set<UUID> clientIds = new LinkedHashSet<>();

	CsvLegacyIdentities(Map<String, List<Row>> input) {
		indexHistory(input.get("replacements.csv"));

		for (Row row : input.get("interventions.csv")) {
			if (row.cells().length == 13) {
				add(clientReferences, row.cells()[0], row.cells()[1], row.uuid(11));
				indexEmployeeReference(row, 2, 3, 12);
			}
		}

		for (Row row : input.get("recurringInterventions.csv")) {
			if (row.cells().length == 12) {
				add(clientReferences, row.cells()[1], row.cells()[2], row.uuid(10));
				indexEmployeeReference(row, 3, 4, 11);
			}
		}

		Map<String, Row> absences = new HashMap<>();
		for (Row row : input.get("employee_absences.csv")) {
			row.columns(6, 7, 8, 9);
			absences.put(row.uuid(0), row);
			if (row.cells().length == 9)
				indexEmployeeReference(row, 2, 1, 8);
		}

		for (Row row : input.get("replacements.csv")) {
			if (row.cells().length == 13 && !row.cells()[10].isEmpty()) {
				Row absence = absences.get(row.uuid(10));
				if (absence != null)
					add(employeeReferences, absence.cells()[2], absence.cells()[1], row.uuid(11));
			}
		}
	}

	private void indexEmployeeReference(Row row, int first, int last, int id) {
		if (!row.cells()[id].isEmpty())
			add(employeeReferences, row.cells()[first], row.cells()[last], row.uuid(id));
	}

	private String recoverOrCreate(Row row, Map<String, Set<String>> references, boolean employee) {
		String key = row.cells()[0] + ";" + row.cells()[1];
		Set<String> candidates = references.getOrDefault(key, Set.of());

		if (candidates.size() > 1)
			throw row.failure("Plusieurs UUID existent pour cette identité : " + candidates);

		if (candidates.size() == 1)
			return candidates.iterator().next();

		if (employee && historicalEmployees.containsKey(key))
			throw row.failure("Salarié actif sans UUID, homonyme historique identifié " + historicalEmployees.get(key)
					+ " : identité commune impossible à prouver ; aucun nouvel UUID généré.");

		return UUID.randomUUID().toString();
	}

	List<String[]> employees(List<Row> rows) {
		List<String[]> result = new ArrayList<>();

		for (Row row : rows) {
			row.columns(3, 6, 7);
			String[] cells = row.expanded(7);

			if (row.cells().length == 3) {
				cells[3] = "Opérationnel";
				cells[4] = "0";
				cells[5] = "0";
			}

			cells[6] = row.cells().length == 7 ? row.uuid(6) : recoverOrCreate(row, employeeReferences, true);
			if (!employeeIds.add(DomainValidation.uuid(cells[6])))
				throw row.failure("UUID salarié dupliqué : " + cells[6]);

			add(employees, cells[0], cells[1], cells[6]);
			add(historicalEmployees, cells[0], cells[1], cells[6]);
			result.add(cells);
		}

		return result;
	}

	List<String[]> clients(List<Row> rows) {
		List<String[]> result = new ArrayList<>();

		for (Row row : rows) {
			row.columns(2, 6, 7);
			String[] cells = row.expanded(7);

			if (row.cells().length == 2) {
				cells[2] = "N/A";
				cells[3] = "0";
			}

			cells[6] = row.cells().length == 7 ? row.uuid(6) : recoverOrCreate(row, clientReferences, false);
			if (!clientIds.add(DomainValidation.uuid(cells[6])))
				throw row.failure("UUID client dupliqué : " + cells[6]);

			add(clients, cells[0], cells[1], cells[6]);
			result.add(cells);
		}

		return result;
	}

	void indexHistory(List<Row> rows) {
		for (Row row : rows) {
			row.columns(10, 11, 13);

			if (row.cells().length == 13) {
				add(historicalEmployees, row.cells()[0], row.cells()[1], row.uuid(11));

				if (!row.cells()[12].isEmpty())
					add(historicalEmployees, row.cells()[3], row.cells()[4], row.uuid(12));
			}
		}
	}

	String employee(Row row, String first, String last, boolean optional, boolean historical) {
		if (first.isEmpty() && last.isEmpty() && optional)
			return "";

		return resolve(row, historical ? historicalEmployees : employees, first, last, "salarié");
	}

	String client(Row row, String first, String last) {
		return resolve(row, clients, first, last, "client");
	}

	void checkHistoricalName(Row row, String first, String last, String authoritativeId) {
		Set<String> ids = historicalEmployees.getOrDefault(first + ";" + last, Set.of());

		if (!ids.isEmpty() && !ids.contains(authoritativeId))
			throw row.failure("Le salarié historique " + first + " " + last
					+ " ne correspond pas à l'UUID de l'absence : " + authoritativeId);
	}

	private static void add(Map<String, Set<String>> index, String first, String last, String id) {
		index.computeIfAbsent(first + ";" + last, ignored -> new LinkedHashSet<>()).add(id);
	}

	private static String resolve(Row row, Map<String, Set<String>> index, String first, String last, String kind) {
		Set<String> ids = index.getOrDefault(first + ";" + last, Set.of());

		if (first.isBlank() || last.isBlank() || ids.size() != 1)
			throw row.failure("Référence " + kind + " " + first + " " + last + " : "
					+ (ids.isEmpty() ? "introuvable ; aucune identité inventée." : "ambiguë, UUID possibles " + ids));

		return ids.iterator().next();
	}
}
