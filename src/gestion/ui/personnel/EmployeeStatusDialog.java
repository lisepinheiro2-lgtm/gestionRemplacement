package gestion.ui.personnel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.Window;
import java.io.IOException;
import java.io.UncheckedIOException;
import gestion.ui.ActionDialogs;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import gestion.controller.PersonnelController;
import gestion.model.Employee;
import gestion.model.EmployeeStatus;
import gestion.ui.UIUtils;

public final class EmployeeStatusDialog {
	private EmployeeStatusDialog() {
	}

	public static boolean show(Component parent, PersonnelController controller, Employee employee) {
		JComboBox<EmployeeStatus> statusBox = new JComboBox<>(EmployeeStatus.values());
		String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/uuuu"));
		JTextField start = new JTextField(today);
		JTextField end = new JTextField(today);
		JPanel form = new JPanel(new BorderLayout(0, 6));
		JPanel status = new JPanel(new GridLayout(1, 2, 6, 6));
		status.add(new JLabel("Statut :"));
		status.add(statusBox);
		JPanel dates = new JPanel(new GridLayout(2, 2, 6, 6));
		dates.add(new JLabel("Date de début :"));
		dates.add(start);
		dates.add(new JLabel("Date de fin :"));
		dates.add(end);
		form.add(status, BorderLayout.NORTH);
		form.add(dates, BorderLayout.CENTER);
		dates.setVisible(false);
		statusBox.addActionListener(event -> {
			dates.setVisible(statusBox.getSelectedItem() != EmployeeStatus.OPERATIONAL);
			Window dialog = SwingUtilities.getWindowAncestor(form);
			if (dialog != null)
				dialog.pack();
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
				controller.changeStatus(employee, (EmployeeStatus) statusBox.getSelectedItem(), start.getText(),
						end.getText());
				return true;
			} catch (DateTimeParseException exception) {
				JOptionPane.showMessageDialog(parent, "Saisis des dates valides au format jj/MM/aaaa.");
			} catch (IllegalArgumentException exception) {
				JOptionPane.showMessageDialog(parent, exception.getMessage());
			} catch (IOException | UncheckedIOException exception) {
				ActionDialogs.error(parent, exception);
			}
		}
	}
}
