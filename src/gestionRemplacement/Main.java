package gestionRemplacement;

import java.time.LocalDate;
import java.time.LocalTime;

public class Main {

	public static void main(String[] args) {

		PersonnelList personnel = new PersonnelList();
		ClientList clients = new ClientList();
		Planning planning = new Planning(personnel, clients);

		Client testClient = clients.findClient("Jeanne", "Dupont");

		if (testClient == null) {

			clients.addClient("Jeanne", "Dupont", "Adresse test", 50, LocalDate.of(2026, 9, 1),
					LocalDate.of(2026, 12, 31));

			testClient = clients.findClient("Jeanne", "Dupont");
		}

		if (planning.isEmpty()) {

			Intervention testIntervention = new Intervention(testClient, null, LocalDate.of(2026, 9, 29),
					LocalTime.of(10, 0), LocalDate.of(2026, 9, 29), LocalTime.of(12, 30));

			planning.addIntervention(testIntervention);
		}

		RecurringInterventionList recurringInterventionList = new RecurringInterventionList(clients, personnel);

		ReplacementList replacementList = new ReplacementList();

		new MainUI(personnel, clients, planning, replacementList, recurringInterventionList);
	}
}