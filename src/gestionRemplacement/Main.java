package gestionRemplacement;

public class Main {

	public static void main(String[] args) {

		PersonnelList personnel = new PersonnelList();
		new PersonnelUI(personnel);

		Planning planning = new Planning(personnel);

		ReplacementList replacementList = new ReplacementList();
		new ReplacementUI(replacementList, personnel, planning);
	}
}