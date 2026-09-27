package gestionRemplacement;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Intervention {

	private Client client;
	private Employee employee;

	private LocalDate startDate;
	private LocalDate endDate;

	private LocalTime startTime;
	private LocalTime endTime;
	private String recurringId;

	public Intervention(Client client, Employee employee, LocalDate startDate, LocalTime startTime, LocalDate endDate,
			LocalTime endTime) {

		this.client = client;
		this.employee = employee;
		this.startDate = startDate;
		this.startTime = startTime;
		this.endDate = endDate;
		this.endTime = endTime;
	}

	public double getDurationHours() {

		LocalDateTime start = LocalDateTime.of(startDate, startTime);
		LocalDateTime end = LocalDateTime.of(endDate, endTime);

		long minutes = Duration.between(start, end).toMinutes();

		return minutes / 60.0;
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public LocalTime getStartTime() {
		return startTime;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public LocalTime getEndTime() {
		return endTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

	public String getRecurringId() {
		return recurringId;
	}

	public void setRecurringId(String recurringId) {
		this.recurringId = recurringId;
	}

}