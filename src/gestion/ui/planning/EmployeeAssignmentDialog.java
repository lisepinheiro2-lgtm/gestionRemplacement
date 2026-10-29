package gestion.ui.planning;

import java.awt.Color;
import java.io.UncheckedIOException;
import gestion.ui.ActionDialogs;
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

import gestion.model.Employee;
import gestion.model.Intervention;
import gestion.service.PersonnelService;
import gestion.service.PlanningService;
import gestion.service.RecurringInterventionService;
import gestion.service.WorkHoursService;
import gestion.util.FormatUtils;
import gestion.ui.UIUtils;

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
		JCheckBox assignSeriesBox = new JCheckBox("Attribuer les occurrences à venir à cet employé");

		if (recurringId != null && !recurringId.isBlank()) {
			employeesPanel.add(assignSeriesBox);
		}

		var weeklyHours = hours.getHoursByEmployee(intervention.getStartDate(), false);
		var monthlyHours = hours.getHoursByEmployee(intervention.getStartDate(), true);
		java.util.Map<Employee, JButton> choices = new java.util.LinkedHashMap<>();
		for (Employee employee : personnel.getEmployees()) {
			double currentHours = weeklyHours.getOrDefault(employee.getEmployeeID(), 0.0);
			double currentMonthlyHours = monthlyHours.getOrDefault(employee.getEmployeeID(), 0.0);
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
			UIUtils.styleActionButton(employeeButton, "neutral");

			if (planning.hasOverlap(employee, intervention)) {
				employeeButton.setEnabled(false);
			} else if (overtimeHours > 0) {
				UIUtils.styleActionButton(employeeButton, "danger");

			} else if (projectedHours < employee.getContractHours()) {
				UIUtils.styleActionButton(employeeButton, "primary");

			} else if (projectedHours == employee.getContractHours()) {
				UIUtils.styleActionButton(employeeButton, "warning");

			} else {
				UIUtils.styleActionButton(employeeButton, "danger");
			}

			choices.put(employee, employeeButton);
			employeeButton.addActionListener(event -> {

				try {
					List<Intervention> interventionsToAssign = assignSeriesBox.isSelected()
							? planning.getFutureInterventionsByRecurringId(recurringId, LocalDateTime.now())
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

					if (assignSeriesBox.isSelected())
						recurringInterventionList.assignFutureSeries(planning, recurringId, employee);
					else
						planning.assignEmployee(interventionsToAssign, employee);

					onChange.run();

					Window window = SwingUtilities.getWindowAncestor(employeeButton);

					if (window != null) {
						window.dispose();
					}

				} catch (UncheckedIOException | IllegalArgumentException e) {
					ActionDialogs.error(parent, e);
				}
			});

			employeesPanel.add(employeeButton);
		}

		assignSeriesBox.addActionListener(event -> {
			List<Intervention> targets = assignSeriesBox.isSelected()
					? planning.getFutureInterventionsByRecurringId(recurringId, LocalDateTime.now()) : List.of(intervention);
			choices.forEach((employee, button) -> button.setEnabled(!planning.hasOverlap(employee, targets)));
		});

		javax.swing.JScrollPane scroll = new javax.swing.JScrollPane(employeesPanel);
		scroll.setPreferredSize(new Dimension(580, Math.min(520, Math.max(100, personnel.getEmployees().size() * 78))));
		scroll.getVerticalScrollBar().setUnitIncrement(25);
		UIUtils.applyTheme(scroll);
		JOptionPane.showMessageDialog(parent, scroll, "Attribuer l'intervention", JOptionPane.PLAIN_MESSAGE);
	}
}
