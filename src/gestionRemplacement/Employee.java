package gestionRemplacement;

public class Employee {

	private String firstName;
	private String lastName;
	private double contractHours;

	public Employee(String firstName, String lastName, double contractHours) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.contractHours = contractHours;
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

}