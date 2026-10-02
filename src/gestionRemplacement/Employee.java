package gestionRemplacement;

public class Employee {

	private String firstName;
	private String lastName;
	private double contractHours;
	private String statut;
	private double attribuateHours;
	private double nonAttribuateHours;

	public Employee(String firstName, String lastName, double contractHours, String statut, double attribuateHours,
			double nonAttribuateHours) {
		
		this.firstName = firstName;
		this.lastName = lastName;
		this.contractHours = contractHours;
		this.statut = statut;
		this.attribuateHours = attribuateHours;
		this.nonAttribuateHours = nonAttribuateHours;
	}
	

	public String toString() {
		return firstName + " " + lastName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public double getContractHours() {
		return contractHours;
	}

	public void setContractHours(double contractHours) {
		this.contractHours = contractHours;
	}

	public double getNonAttribuateHours() {
		return nonAttribuateHours;
	}

	public void setNonAttribuateHours(double nonAttribuateHours) {
		this.nonAttribuateHours = nonAttribuateHours;
	}

	public double getAttribuateHours() {
		return attribuateHours;
	}

	public void setAttribuateHours(double attribuateHours) {
		this.attribuateHours = attribuateHours;
	}

	public String getStatut() {
		return statut;
	}

	public void setStatut(String statut) {
		this.statut = statut;
	}

}