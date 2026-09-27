package gestionRemplacement;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;

public class RecurringIntervention {

	private Client client;
	private Employee employee;
	private String id;

	private DayOfWeek dayOfWeek;

	private LocalTime startTime;
	private LocalTime endTime;

	private LocalDate startDate;
	private LocalDate endDate;

	public RecurringIntervention(Client client, Employee employee, DayOfWeek dayOfWeek, LocalTime startTime,
			LocalTime endTime, LocalDate startDate, LocalDate endDate) {

		this.client = client;
		this.employee = employee;
		this.dayOfWeek = dayOfWeek;
		this.startTime = startTime;
		this.endTime = endTime;
		this.startDate = startDate;
		this.endDate = endDate;
		this.setId(java.util.UUID.randomUUID().toString());
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

	public DayOfWeek getDayOfWeek() {
		return dayOfWeek;
	}

	public void setDayOfWeek(DayOfWeek dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
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

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}