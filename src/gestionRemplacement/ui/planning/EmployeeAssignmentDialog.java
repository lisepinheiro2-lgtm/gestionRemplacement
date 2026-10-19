package gestionRemplacement.ui.planning;

import gestionRemplacement.model.Employee;
import gestionRemplacement.model.Intervention;
import gestionRemplacement.service.PersonnelService;
import gestionRemplacement.service.PlanningService;
import gestionRemplacement.service.RecurringInterventionService;
import gestionRemplacement.service.WorkHoursService;
import gestionRemplacement.util.FormatUtils;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public final class EmployeeAssignmentDialog {
    private final Component parent;
    private final PlanningService planning;
    private final WorkHoursService hours;
    private final PersonnelService personnel;
    private final RecurringInterventionService recurringInterventionList;
    private final Runnable onChange;

    public EmployeeAssignmentDialog(Component parent, PlanningService planning, PersonnelService personnel,
            RecurringInterventionService recurringInterventionList, Runnable onChange) {
        this.parent = parent;
        this.planning = planning;
        this.personnel = personnel;
        this.hours = new WorkHoursService(planning);
        this.recurringInterventionList = recurringInterventionList;
        this.onChange = onChange;
    }

    public void open(Intervention intervention) {

        LocalDateTime interventionEnd = LocalDateTime.of(intervention.getEndDate(), intervention.getEndTime());

        if (!interventionEnd.isAfter(LocalDateTime.now())) {
            int confirmation = JOptionPane.showConfirmDialog(parent,

                    "Cette intervention a déjà eu lieu, êtes-vous sûr de vouloir modifier son attribution ?",
                    "Modifier une attribution passée", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (confirmation != JOptionPane.YES_OPTION) {
                return;
            }
        }

        JPanel employeesPanel = new JPanel();
        employeesPanel.setLayout(new BoxLayout(employeesPanel, BoxLayout.Y_AXIS));
        LocalDate monday = intervention.getStartDate().with(DayOfWeek.MONDAY);
        LocalDateTime weekStart = monday.atStartOfDay();
        LocalDateTime weekEnd = monday.plusWeeks(1).atStartOfDay();
        String recurringId = intervention.getRecurringId();
        JCheckBox assignSeriesBox = new JCheckBox("Attribuer toutes les occurrences à cet employé");

        if (recurringId != null && !recurringId.isBlank()) {
            employeesPanel.add(assignSeriesBox);
        }

        for (Employee employee : personnel.getEmployees()) {

            double currentHours = hours.getWeeklyHours(employee, intervention.getStartDate());
            double currentMonthlyHours = hours.getMonthlyHours(employee, intervention.getStartDate());
            double estimatedMonthlyHours = (employee.getContractHours() * 52.0) / 12;
            double remainingMonthlyHours = estimatedMonthlyHours - currentMonthlyHours;

            double projectedHours = currentHours;
            double remainingWeeklyHours = employee.getContractHours() - currentHours;

            if (intervention.getEmployee() != employee) {
                projectedHours += WorkHoursService.getInterventionHoursInPeriod(intervention, weekStart, weekEnd);
            }

            String text = "<html><div style='text-align: center;'>" + employee + "<br>Semaine : "
                    + String.format("%.2f", remainingWeeklyHours) + "h restantes / "
                    + String.format("%.2f", employee.getContractHours()) + " h prévues" + "<br>Mois : "
                    + String.format("%.2f", remainingMonthlyHours) + " h restantes / "
                    + String.format("%.2f", estimatedMonthlyHours) + " h estimées" + "</div></html>";

            double overtimeHours = hours.getProjectedWeeklyOverrun(employee, intervention);

            JButton employeeButton = new JButton(text);
            employeeButton.setPreferredSize(new Dimension(550, 70));
            employeeButton.setMaximumSize(new Dimension(550, 70));

            if (planning.hasOverlap(employee, intervention)) {
                employeeButton.setEnabled(false);
            } else if (overtimeHours > 0) {
                employeeButton.setBackground(Color.RED);

            } else if (projectedHours < employee.getContractHours()) {
                employeeButton.setBackground(Color.GREEN);

            } else if (projectedHours == employee.getContractHours()) {
                employeeButton.setBackground(Color.YELLOW);

            } else {
                employeeButton.setBackground(Color.RED);
            }

            employeeButton.addActionListener(event -> {

                List<Intervention> interventionsToAssign = assignSeriesBox.isSelected()
                        ? planning.getInterventionsByRecurringId(recurringId)
                        : List.of(intervention);

                if (planning.hasOverlap(employee, interventionsToAssign)) {
                    JOptionPane.showMessageDialog(parent,
                            "Cet employé a un chevauchement sur au moins une des interventions.");
                    return;
                }

                double totalOverrun = hours.getProjectedWeeklyOverrun(employee, interventionsToAssign);

                if (totalOverrun > 0) {
                    int confirmation = JOptionPane.showConfirmDialog(parent,
                            "Cette attribution causerait dépassement des heures prévues sur les semaines concernées. "
                                    + FormatUtils.formatContractHours(totalOverrun) + "\nVoulez-vous continuer ?",
                            "dépassement des heures prévues sur les semaines concernées", JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);

                    if (confirmation != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                planning.assignEmployee(interventionsToAssign, employee);

                if (assignSeriesBox.isSelected()) {
                    recurringInterventionList.assignEmployeeByRecurringId(recurringId, employee);
                }

                onChange.run();

                Window window = SwingUtilities.getWindowAncestor(employeeButton);

                if (window != null) {
                    window.dispose();
                }
            });

            employeesPanel.add(employeeButton);
        }

        JOptionPane.showMessageDialog(parent, employeesPanel, "Attribuer l'intervention", JOptionPane.PLAIN_MESSAGE);
    }
}
