package gestion.util;

import java.time.LocalDateTime;
import gestion.model.*;

/** Invariants communs aux entrées des services et aux lecteurs/écrivains CSV. */
public final class DomainValidation {
    private DomainValidation() { }
    public static void text(String value, boolean required) {
        if (value == null || (required && value.isBlank()))
            throw new IllegalArgumentException("Veuillez renseigner les champs obligatoires.");
        if (value.indexOf(';') >= 0 || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0)
            throw new IllegalArgumentException("Les champs texte ne peuvent contenir ni point-virgule ni saut de ligne.");
    }
    public static void hours(double value) {
        if (!Double.isFinite(value) || value < 0)
            throw new IllegalArgumentException("Les heures du contrat doivent être finies et positives ou nulles.");
    }
    public static void employee(Employee employee) {
        if (employee == null) throw new IllegalArgumentException("Veuillez sélectionner un employé.");
        text(employee.getFirstName(), true); text(employee.getLastName(), true); text(employee.getStatut(), true);
        hours(employee.getContractHours());
        if (!Double.isFinite(employee.getAssignedHours()) || !Double.isFinite(employee.getRemainingHours()))
            throw new IllegalArgumentException("Compteurs d'heures invalides.");
    }
    public static void client(Client client) {
        if (client == null) throw new IllegalArgumentException("Veuillez sélectionner un client.");
        text(client.getFirstName(), true); text(client.getLastName(), true); text(client.getAddress(), true);
        hours(client.getTotalHoursContract());
        if (client.getContractStartDate() != null && client.getContractEndDate() != null)
            new DateRange(client.getContractStartDate(), client.getContractEndDate());
    }
    public static DateTimeRange intervention(Intervention intervention) {
        if (intervention == null) throw new IllegalArgumentException("Veuillez sélectionner une intervention.");
        client(intervention.getClient());
        if (intervention.getEmployee() != null) employee(intervention.getEmployee());
        if (intervention.getStartDate() == null || intervention.getStartTime() == null
                || intervention.getEndDate() == null || intervention.getEndTime() == null)
            throw new IllegalArgumentException("Veuillez renseigner les dates et horaires.");
        if (intervention.getSourceAbsenceId() != null) text(intervention.getSourceAbsenceId(), true);
        if (intervention.getRecurringId() != null) text(intervention.getRecurringId(), true);
        return new DateTimeRange(LocalDateTime.of(intervention.getStartDate(), intervention.getStartTime()),
                LocalDateTime.of(intervention.getEndDate(), intervention.getEndTime()));
    }
    public static void recurring(RecurringIntervention recurring) {
        if (recurring == null) throw new IllegalArgumentException("Série absente.");
        client(recurring.getClient());
        if (recurring.getEmployee() != null) employee(recurring.getEmployee());
        text(recurring.getId(), true);
        new DateRange(recurring.getStartDate(), recurring.getEndDate());
        if (recurring.getDayOfWeek() == null || recurring.getStartTime() == null || recurring.getEndTime() == null
                || !recurring.getEndTime().isAfter(recurring.getStartTime()))
            throw new IllegalArgumentException("Jour ou horaires de la série invalides.");
    }
    public static void absence(EmployeeAbsence absence) {
        if (absence == null) throw new IllegalArgumentException("Absence manquante.");
        employee(absence.getEmployee()); text(absence.getId(), true);
        new DateRange(absence.getStartDate(), absence.getEndDate());
        if (absence.getEndDate().equals(java.time.LocalDate.MAX))
            throw new IllegalArgumentException("La date de fin ne permet pas de représenter le lendemain à minuit.");
        if (absence.getStatus() == null || absence.getStatus() == EmployeeStatus.OPERATIONAL)
            throw new IllegalArgumentException("Veuillez sélectionner un statut d'absence.");
    }
    public static DateTimeRange replacement(Replacement replacement) {
        if (replacement == null) throw new IllegalArgumentException("Remplacement manquant.");
        employee(replacement.getEmployeeToReplace());
        if (replacement.getReplacementEmployee() != null) employee(replacement.getReplacementEmployee());
        hours(replacement.getContractHoursEmployeeToReplace()); hours(replacement.getContractHoursReplacementEmployee());
        text(replacement.getStartDate(), true); text(replacement.getEndDate(), true);
        text(replacement.getStartTime(), true); text(replacement.getEndTime(), true);
        if (replacement.getAbsenceId() != null) text(replacement.getAbsenceId(), true);
        return new DateTimeRange(LocalDateTime.of(FormatUtils.parseDate(replacement.getStartDate()),
                FormatUtils.parseTime(replacement.getStartTime())),
                LocalDateTime.of(FormatUtils.parseDate(replacement.getEndDate()), FormatUtils.parseTime(replacement.getEndTime())));
    }
}
