package gestion.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import gestion.model.Employee;
import gestion.model.EmployeeAbsence;
import gestion.model.EmployeeStatus;
import gestion.model.EmployeeSummary;
import gestion.service.AbsenceService;
import gestion.service.PersonnelService;
import gestion.service.WorkHoursService;
import gestion.util.FormatUtils;

public final class PersonnelController {
	private final PersonnelService personnel;
	private final AbsenceService absences;
	private final WorkHoursService hours;

	public PersonnelController(PersonnelService personnel, AbsenceService absences, WorkHoursService hours) {
		this.personnel = personnel;
		this.absences = absences;
		this.hours = hours;
	}

	public void addEmployee(String firstName, String lastName, String contractHours) {
		double weeklyHours = validateEmployee(firstName, lastName, contractHours);
		personnel.addEmployee(firstName, lastName, weeklyHours, EmployeeStatus.OPERATIONAL.toString(), 0, 0);
	}

	public void editEmployee(Employee employee, String firstName, String lastName, String contractHours) {
		
		double weeklyHours = validateEmployee(firstName, lastName, contractHours);
		int index = personnel.getEmployees().indexOf(employee);
		
		if (index < 0)
			throw new IllegalArgumentException("Cet employé n’existe plus.");

		personnel.editEmployee(index, firstName, lastName, weeklyHours, employee.getStatut(),
				employee.getAssignedHours(), employee.getRemainingHours());
	}

	public void removeEmployee(Employee employee) {
		int index = personnel.getEmployees().indexOf(employee);
		if (index >= 0)
			personnel.removeEmployee(index);
	}

	public void changeStatus(Employee employee, EmployeeStatus status, String startText, String endText)
			throws IOException {

		if (status == EmployeeStatus.OPERATIONAL) {
			absences.resumeEmployee(employee, LocalDate.now());
			return;
		}

		LocalDate start = FormatUtils.parseDate(startText);
		LocalDate end = FormatUtils.parseDate(endText);

		if (end.isBefore(start))
			throw new IllegalArgumentException("La fin doit être le même jour ou après le début.");
		absences.addAbsence(new EmployeeAbsence(employee, status, start, end));
	}

	public List<EmployeeSummary> getRows(LocalDate assignedDate, boolean assignedMonthly) {

		List<EmployeeSummary> rows = new ArrayList<>();

		for (Employee employee : personnel.getEmployees()) {

			double expected = assignedMonthly ? employee.getContractHours() * 52 / 12 : employee.getContractHours();
			double assigned = assignedMonthly ? hours.getMonthlyHours(employee, assignedDate)
					: hours.getWeeklyHours(employee, assignedDate);
			double remaining = expected - assigned;

			rows.add(new EmployeeSummary(employee, absences.getStatusAt(employee, LocalDate.now()), assigned, remaining,
					hours.getHoursAlert(employee, assignedDate, assignedMonthly),
					hours.getHoursAlert(employee, assignedDate, assignedMonthly)));
		}
		return rows;
	}

	private static double validateEmployee(String firstName, String lastName, String contractHours) {
		
		if (firstName.isBlank() || lastName.isBlank() || contractHours.isBlank())
			throw new IllegalArgumentException(
					"Veuillez renseigner le nom, le prénom et les heures prévues au contrat.");
		try {
			return FormatUtils.parseContractHours(contractHours);
		
		} catch (NumberFormatException exception) {
			throw new IllegalArgumentException("Les heures prévues au contrat doivent être un nombre.", exception);
		}
	}
}
