package gestion.model;

public record EmployeeSummary(Employee employee, EmployeeStatus status, double assignedHours, double remainingHours,
		HoursAlert assignedAlert, HoursAlert remainingAlert) {
}
