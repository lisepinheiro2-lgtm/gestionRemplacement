package gestion.model;

/**
 * Valeurs calculées pour une ligne ; aucun compteur dérivé n’est enregistré
 * dans le CSV.
 */
public record EmployeeSummary(Employee employee, EmployeeStatus status, double assignedHours, double remainingHours,
        HoursAlert assignedAlert, HoursAlert remainingAlert) {
}
