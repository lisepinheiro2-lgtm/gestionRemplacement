package gestion.model;

import java.time.LocalDate;
import java.util.UUID;
import java.util.Objects;

public class Client {

	private String firstName;
	private String lastName;
	private String address;
	private double totalHoursContract;
	private LocalDate contractStartDate;
	private LocalDate contractEndDate;
	private final UUID clientID;

	public Client(String firstName, String lastName, String address, double totalHoursContract,
			LocalDate contractStartDate, LocalDate contractEndDate, UUID clientID) {

		this.lastName = lastName;
		this.firstName = firstName;
		this.address = address;
		this.totalHoursContract = totalHoursContract;
		this.contractStartDate = contractStartDate;
		this.contractEndDate = contractEndDate;
		this.clientID = Objects.requireNonNull(clientID, "L'UUID du client ne peut pas être null");

	}

	public Client(String firstName, String lastName, String address, double totalHoursContract,
			LocalDate contractStartDate, LocalDate contractEndDate) {

		this(firstName, lastName, address, totalHoursContract, contractStartDate, contractEndDate, UUID.randomUUID());

	}

	@Override
	public String toString() {
		return lastName + " " + firstName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public double getTotalHoursContract() {
		return totalHoursContract;
	}

	public void setTotalHoursContract(double totalHoursContract) {
		this.totalHoursContract = totalHoursContract;
	}

	public LocalDate getContractStartDate() {
		return contractStartDate;
	}

	public void setContractStartDate(LocalDate contractStartDate) {
		this.contractStartDate = contractStartDate;
	}

	public LocalDate getContractEndDate() {
		return contractEndDate;
	}

	public void setContractEndDate(LocalDate contractEndDate) {
		this.contractEndDate = contractEndDate;
	}

	public UUID getClientID() {
		return clientID;
	}

}
