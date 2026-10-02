package gestionRemplacement;

public enum EmployeeStatus {
	OPERATIONAL, VACATION, SICK_LEAVE, OTHER_ABSENCE;

	@Override
	public String toString() {
		return switch (this) {
		case OPERATIONAL -> "Opérationnel";
		case VACATION -> "Congés";
		case SICK_LEAVE -> "Maladie";
		case OTHER_ABSENCE -> "Autre absence";
		};
	}
}
