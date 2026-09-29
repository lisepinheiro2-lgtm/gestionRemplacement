package gestionRemplacement;

public class Main {

	public static void main(String[] args) {

		PersonnelList personnel = new PersonnelList();
		ClientList clients = new ClientList();
		Planning planning = new Planning(personnel, clients);
		RecurringInterventionList recurringInterventionList = new RecurringInterventionList(clients, personnel);
		ReplacementList replacementList = new ReplacementList();

		new MainUI(personnel, clients, planning, replacementList, recurringInterventionList);
	}
}