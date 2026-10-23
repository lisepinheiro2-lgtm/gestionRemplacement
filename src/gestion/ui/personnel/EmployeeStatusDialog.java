package gestion.ui.personnel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import gestion.controller.PersonnelController;
import gestion.model.DateTimeRange;
import gestion.model.Employee;
import gestion.model.EmployeeAbsence;
import gestion.model.EmployeeStatus;
import gestion.service.PlanningService.AbsenceRelease;
import gestion.ui.ActionDialogs;
import gestion.ui.UIUtils;

public final class EmployeeStatusDialog {
	private EmployeeStatusDialog() {
	}

	public static boolean show(Component parent, PersonnelController controller, Employee employee) {
		JComboBox<EmployeeStatus> statusBox = new JComboBox<>(EmployeeStatus.values());
		String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/uuuu"));
		JTextField start = new JTextField(today);
		JTextField end = new JTextField(today);
		JCheckBox timed = new JCheckBox("Absence partielle ou signalée tardivement");
		JTextField startTime = new JTextField("00h00", 6);
		JTextField endTime = new JTextField("24h00", 6);
		startTime.putClientProperty("JComponent.roundRect", true);
		endTime.putClientProperty("JComponent.roundRect", true);
		startTime.setToolTipText("Heure réelle de début, par exemple 14h30 ou 14:30. La partie travaillée avant est conservée.");
		endTime.setToolTipText("Heure réelle de fin sur la date de fin. 24h00 signifie la fin de cette journée.");
		JPanel timing = new JPanel(new GridLayout(2, 2, 6, 6));
		timing.add(new JLabel("Heure réelle de début :"));
		timing.add(startTime);
		timing.add(new JLabel("Heure réelle de fin :"));
		timing.add(endTime);
		timing.setVisible(false);

		JPanel form = new JPanel(new BorderLayout(0, 6));
		JPanel status = new JPanel(new GridLayout(1, 2, 6, 6));
		status.add(new JLabel("Statut :"));
		status.add(statusBox);
		JPanel dates = new JPanel(new GridLayout(2, 2, 6, 6));
		dates.add(new JLabel("Date de début :"));
		dates.add(start);
		dates.add(new JLabel("Date de fin :"));
		dates.add(end);
		JPanel absenceFields = new JPanel(new BorderLayout(0, 6));
		JPanel timedFields = new JPanel(new BorderLayout(0, 4));
		timedFields.add(timed, BorderLayout.NORTH);
		timedFields.add(timing, BorderLayout.CENTER);
		absenceFields.add(dates, BorderLayout.NORTH);
		absenceFields.add(timedFields, BorderLayout.CENTER);
		form.add(status, BorderLayout.NORTH);
		form.add(absenceFields, BorderLayout.CENTER);
		absenceFields.setVisible(false);
		statusBox.addActionListener(event -> {
			absenceFields.setVisible(statusBox.getSelectedItem() != EmployeeStatus.OPERATIONAL);
			resizeDialog(form);
		});
		timed.addActionListener(event -> {
			timing.setVisible(timed.isSelected());
			resizeDialog(form);
		});

		UIUtils.applyTheme(form);

		while (true) {
			JOptionPane pane = new JOptionPane(form, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
			JDialog dialog = pane.createDialog(parent, "Statut de " + employee);
			pane.setBackground(UIUtils.getBackgroundColor());
			UIUtils.applyTheme(dialog);
			dialog.pack();
			dialog.setLocationRelativeTo(parent);
			dialog.setVisible(true);
			Object choice = pane.getValue();
			dialog.dispose();

			if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(choice))
				return false;

			try {
				EmployeeStatus selected = (EmployeeStatus) statusBox.getSelectedItem();

				if (timed.isSelected() && selected != EmployeeStatus.OPERATIONAL) {
					EmployeeAbsence absence = controller.prepareTimedAbsence(employee, selected, start.getText(),
							end.getText(), startTime.getText(), endTime.getText());
					List<AbsenceRelease> releases = controller.previewTimedAbsence(absence);

					if (!confirmTimedAbsence(parent, absence, releases))
						continue;

					controller.declareTimedAbsence(absence, releases);
				} else {
					controller.changeStatus(employee, selected, start.getText(), end.getText());
				}

				return true;

			} catch (DateTimeParseException exception) {
				JOptionPane.showMessageDialog(parent,
						"Saisis des dates valides au format jj/MM/aaaa et une heure au format 14h30 ou 14:30.");
			} catch (IllegalArgumentException exception) {
				JOptionPane.showMessageDialog(parent, exception.getMessage());
			} catch (IOException | UncheckedIOException exception) {
				ActionDialogs.error(parent, exception);
			}
		}
	}

	private static void resizeDialog(Component form) {
		Window dialog = SwingUtilities.getWindowAncestor(form);

		if (dialog != null)
			dialog.pack();
	}

	private static boolean confirmTimedAbsence(Component parent, EmployeeAbsence absence, List<AbsenceRelease> releases) {
		DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/uuuu HH'h'mm");
		String beginning = absence.getStartDate().atTime(absence.getStartTime()).format(format);
		JPanel content = new JPanel(new BorderLayout(0, 8));
		String ending = gestion.util.DomainValidation.absencePeriod(absence).end().format(format);
		content.add(new JLabel("Absence réelle : " + beginning + " – " + ending), BorderLayout.NORTH);

		if (releases.isEmpty()) {
			content.add(new JLabel("Aucune intervention à libérer sur cette période."), BorderLayout.CENTER);
		} else {
			DefaultTableModel model = new DefaultTableModel(
					new String[] { "Client", "Créneau initial", "Partie à libérer", "État" }, 0) {
				@Override
				public boolean isCellEditable(int row, int column) {
					return false;
				}
			};
			LocalDateTime now = LocalDateTime.now();

			releases.stream().sorted(Comparator.comparing(release -> release.originalPeriod().start()))
					.forEach(release -> model.addRow(new Object[] { release.client(),
							formatPeriod(release.originalPeriod(), format), formatPeriod(release.absentPeriod(), format),
							!release.originalPeriod().end().isAfter(now) ? "Terminée"
									: release.originalPeriod().start().isBefore(now) ? "En cours" : "À venir" }));

			JTable table = new JTable(model);
			table.setRowHeight(25);
			table.getTableHeader().setReorderingAllowed(false);
			table.getColumnModel().getColumn(0).setPreferredWidth(160);
			table.getColumnModel().getColumn(1).setPreferredWidth(300);
			table.getColumnModel().getColumn(2).setPreferredWidth(300);
			table.getColumnModel().getColumn(3).setPreferredWidth(100);
			JScrollPane scroll = new JScrollPane(table);
			scroll.setPreferredSize(new Dimension(880, Math.min(240, 30 + releases.size() * 25)));
			content.add(scroll, BorderLayout.CENTER);
		}

		content.add(new JLabel("Seules les parties indiquées seront désattribuées. Les heures hors de l'absence restent conservées."),
				BorderLayout.SOUTH);
		UIUtils.applyTheme(content);
		Object[] options = { "Enregistrer l'absence", "Annuler" };
		return JOptionPane.showOptionDialog(parent, content, "Confirmer l'absence partielle ou tardive",
				JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]) == 0;
	}

	private static String formatPeriod(DateTimeRange period, DateTimeFormatter format) {
		return period.start().format(format) + " – " + period.end().format(format);
	}
}
