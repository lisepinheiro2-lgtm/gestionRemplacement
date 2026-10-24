package gestion.model;

import java.util.UUID;
import java.util.Objects;

public class Employee {

	private String firstName;
	private String lastName;
	private double contractHours;
	private String statut;
	private double assignedHours;
	private double remainingHours;
	private final UUID employeeID;

	public Employee(String firstName, String lastName, double contractHours, String statut, double assignedHours,
			double remainingHours, UUID employeeUuid) {

		this.firstName = firstName;
		this.lastName = lastName;
		this.contractHours = contractHours;
		this.statut = statut;
		this.assignedHours = assignedHours;
		this.remainingHours = remainingHours;
		this.employeeID = Objects.requireNonNull(employeeUuid, "L'UUID de l'employé ne peut pas être null");
	}

	public Employee(String firstName, String lastName, double contractHours, String statut, double assignedHours,
			double remainingHours) {

		this(firstName, lastName, contractHours, statut, assignedHours, remainingHours, UUID.randomUUID());
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

	public double getRemainingHours() {
		return remainingHours;
	}

	public void setRemainingHours(double remainingHours) {
		this.remainingHours = remainingHours;
	}

	public double getAssignedHours() {
		return assignedHours;
	}

	public void setAssignedHours(double assignedHours) {
		this.assignedHours = assignedHours;
	}

	public String getStatut() {
		return statut;
	}

	public void setStatut(String statut) {
		this.statut = statut;
	}

	public UUID getEmployeeID() {
		return employeeID;
	}
}
