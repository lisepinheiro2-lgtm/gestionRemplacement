package gestionRemplacement;

import java.io.IOException;

import javax.swing.JOptionPane;

public class Main {

	public static void main(String[] args) {

		PersonnelList personnel = new PersonnelList();
		ClientList clients = new ClientList();
		Planning planning = new Planning(personnel, clients);
		RecurringInterventionList recurringInterventionList = new RecurringInterventionList(clients, personnel);
		ReplacementList replacementList = new ReplacementList();
		EmployeeAbsenceList absenceList;

		try {
			absenceList = new EmployeeAbsenceList(personnel, replacementList);
		} catch (IOException exception) {
			JOptionPane.showMessageDialog(null, "Impossible de charger les absences : " + exception.getMessage(),
					"Erreur de lecture", JOptionPane.ERROR_MESSAGE);
			return;
		}

		new MainUI(personnel, clients, planning, replacementList, recurringInterventionList, absenceList);

	}
}