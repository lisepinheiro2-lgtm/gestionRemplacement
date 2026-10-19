package gestionRemplacement.controller;

import gestionRemplacement.model.Employee;
import gestionRemplacement.model.EmployeeAbsence;
import gestionRemplacement.model.EmployeeStatus;
import gestionRemplacement.model.EmployeeSummary;
import gestionRemplacement.service.AbsenceService;
import gestionRemplacement.service.PersonnelService;
import gestionRemplacement.service.WorkHoursService;
import gestionRemplacement.util.FormatUtils;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


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

    public List<EmployeeSummary> getRows(LocalDate assignedDate, boolean assignedMonthly, LocalDate remainingDate,
            boolean remainingMonthly) {
        List<EmployeeSummary> rows = new ArrayList<>();
        for (Employee employee : personnel.getEmployees()) {
            double assigned = assignedMonthly ? hours.getMonthlyHours(employee, assignedDate)
                    : hours.getWeeklyHours(employee, assignedDate);
            double assignedInRemainingPeriod = remainingMonthly ? hours.getMonthlyHours(employee, remainingDate)
                    : hours.getWeeklyHours(employee, remainingDate);
            double expected = remainingMonthly ? employee.getContractHours() * 52 / 12 : employee.getContractHours();
            rows.add(new EmployeeSummary(employee, absences.getStatusAt(employee, LocalDate.now()), assigned,
                    expected - assignedInRemainingPeriod, hours.getHoursAlert(employee, assignedDate, assignedMonthly),
                    hours.getHoursAlert(employee, remainingDate, remainingMonthly)));
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
