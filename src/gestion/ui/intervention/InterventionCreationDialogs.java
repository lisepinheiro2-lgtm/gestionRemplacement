package gestion.ui.intervention;

import java.awt.Component;
import java.awt.GridLayout;
import java.io.UncheckedIOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import gestion.model.Client;
import gestion.model.Employee;
import gestion.model.Intervention;
import gestion.model.RecurringIntervention;
import gestion.service.ClientService;
import gestion.service.PersonnelService;
import gestion.service.PlanningService;
import gestion.service.RecurringInterventionService;
import gestion.util.FormatUtils;
import gestion.ui.ActionDialogs;
import java.util.List;

public final class InterventionCreationDialogs {

	private final Component parent;
	private final PlanningService planning;
	private final PersonnelService personnel;
	private final ClientService clients;
	private final RecurringInterventionService recurringInterventionList;
	private final Runnable onChange;

	public InterventionCreationDialogs(Component parent, PlanningService planning, PersonnelService personnel,
			ClientService clients, RecurringInterventionService recurringInterventionList, Runnable onChange) {
		this.parent = parent;
		this.planning = planning;
		this.personnel = personnel;
		this.clients = clients;
		this.recurringInterventionList = recurringInterventionList;
		this.onChange = onChange;
	}

	public void openAddRecurringIntervention() {

		JPanel formPanel = new JPanel(new GridLayout(7, 2));
		JComboBox<Client> clientBox = new JComboBox<>();

		for (Client client : clients.getClients()) {
			clientBox.addItem(client);
		}

		JComboBox<Object> employeeBox = new JComboBox<>();
		employeeBox.addItem("Non attribué");

		for (Employee employee : personnel.getEmployees()) {
			employeeBox.addItem(employee);
		}

		String[] days = { "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche" };
		JComboBox<String> dayBox = new JComboBox<>(days);
		JTextField startTimeField = new JTextField();
		JTextField endTimeField = new JTextField();
		JTextField startDateField = new JTextField();
		JTextField endDateField = new JTextField();
		endTimeField.setToolTipText("Si la fin précède le début, l'intervention se termine le lendemain.");
		endDateField.setToolTipText("Dernier jour de départ possible des occurrences, y compris celles finissant le lendemain.");

		formPanel.add(new JLabel("Client :"));
		formPanel.add(clientBox);
		formPanel.add(new JLabel("Intervenant :"));
		formPanel.add(employeeBox);
		formPanel.add(new JLabel("Jour :"));
		formPanel.add(dayBox);
		formPanel.add(new JLabel("Heure de début :"));
		formPanel.add(startTimeField);
		formPanel.add(new JLabel("Heure de fin :"));
		formPanel.add(endTimeField);
		formPanel.add(new JLabel("Date de début :"));
		formPanel.add(startDateField);
		formPanel.add(new JLabel("Date de fin :"));
		formPanel.add(endDateField);

		while (true) {
			int result = JOptionPane.showConfirmDialog(parent, formPanel, "Ajouter une intervention récurrente",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

			if (result != JOptionPane.OK_OPTION) {
				return;
			}

			try {

				Client selectedClient = (Client) clientBox.getSelectedItem();
				Employee selectedEmployee = null;
				Object selectedItem = employeeBox.getSelectedItem();

				if (selectedItem instanceof Employee) {
					selectedEmployee = (Employee) selectedItem;
				}

				DayOfWeek dayOfWeek = DayOfWeek.of(dayBox.getSelectedIndex() + 1);
				LocalTime startTime = FormatUtils.parseTime(startTimeField.getText());
				LocalTime endTime = FormatUtils.parseTime(endTimeField.getText());
				LocalDate startDate = FormatUtils.parseDate(startDateField.getText());
				LocalDate endDate = FormatUtils.parseDate(endDateField.getText());

				if (!endDate.isAfter(startDate) && !endDate.equals(startDate)) {
					JOptionPane.showMessageDialog(parent, "La date de fin doit être postérieure à la date de début.");
					continue;
				}

				if (endTime.equals(startTime)) {
					JOptionPane.showMessageDialog(parent, "Les heures de début et de fin doivent être différentes.");
					continue;
				}

				RecurringIntervention recurring = new RecurringIntervention(selectedClient, selectedEmployee, dayOfWeek,
						startTime, endTime, startDate, endDate);
				var preview = planning.previewRecurringInterventions(recurring);
				if (!ActionDialogs.confirmSaving(parent, planning, preview, null, false))
					return;
				boolean generated = recurringInterventionList.createWithPlanning(planning, recurring);

				if (!generated) {
					JOptionPane.showMessageDialog(parent, "Impossible de créer cette récurrence : "
							+ "l'intervenant est déjà occupé " + "sur au moins une des interventions.");
					continue;
				}

				onChange.run();
				return;

			} catch (DateTimeParseException e) {
				JOptionPane.showMessageDialog(parent,
						"Format invalide.\n" + "Date : 05/03/2027\n" + "Heure : 8h, 8h30 ou 14h15");
			} catch (UncheckedIOException | IllegalArgumentException e) {
				ActionDialogs.error(parent, e);
			}
		}
	}

	public void openAddIntervention() {
		openAddIntervention(null, null);
	}

	public void openAddIntervention(LocalDate initialDate, LocalTime initialTime) {
		JPanel formPanel = new JPanel(new GridLayout(6, 2));
		JComboBox<Client> clientBox = new JComboBox<>();

		for (Client client : clients.getClients()) {
			clientBox.addItem(client);
		}

		JComboBox<Object> employeeBox = new JComboBox<>();
		employeeBox.addItem("Non attribué");

		for (Employee employee : personnel.getEmployees()) {
			employeeBox.addItem(employee);
		}

		JTextField startDateField = new JTextField();
		JTextField startTimeField = new JTextField();
		JTextField endDateField = new JTextField();
		JTextField endTimeField = new JTextField();

		if (initialDate != null) {
			startDateField.setText(FormatUtils.formatDate(initialDate));
			endDateField.setText(FormatUtils.formatDate(initialDate));
		}

		if (initialTime != null) {
			startTimeField.setText(FormatUtils.formatTime(initialTime));
		}

		formPanel.add(new JLabel("Client :"));
		formPanel.add(clientBox);
		formPanel.add(new JLabel("Intervenant :"));
		formPanel.add(employeeBox);
		formPanel.add(new JLabel("Date de début :"));
		formPanel.add(startDateField);
		formPanel.add(new JLabel("Heure de début :"));
		formPanel.add(startTimeField);
		formPanel.add(new JLabel("Date de fin :"));
		formPanel.add(endDateField);
		formPanel.add(new JLabel("Heure de fin :"));
		formPanel.add(endTimeField);

		while (true) {
			int result = JOptionPane.showConfirmDialog(parent, formPanel, "Ajouter une intervention",
					JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

			if (result != JOptionPane.OK_OPTION) {
				return;
			}

			Client selectedClient = (Client) clientBox.getSelectedItem();
			Employee selectedEmployee = null;
			Object selectedItem = employeeBox.getSelectedItem();

			if (selectedItem instanceof Employee) {
				selectedEmployee = (Employee) selectedItem;
			}

			String startDateText = startDateField.getText();
			String startTimeText = startTimeField.getText();
			String endDateText = endDateField.getText();
			String endTimeText = endTimeField.getText();

			try {
				LocalDate startDate = FormatUtils.parseDate(startDateText);
				LocalDate endDate = FormatUtils.parseDate(endDateText);
				LocalTime startTime = FormatUtils.parseTime(startTimeText);
				LocalTime endTime = FormatUtils.parseTime(endTimeText);

				LocalDateTime start = LocalDateTime.of(startDate, startTime);
				LocalDateTime end = LocalDateTime.of(endDate, endTime);

				if (!end.isAfter(start)) {
					JOptionPane.showMessageDialog(parent, "La fin de l'intervention doit être postérieure au début.");
					continue;
				}

				Intervention intervention = new Intervention(selectedClient, selectedEmployee, startDate, startTime,
						endDate, endTime);

				if (selectedEmployee != null && planning.hasOverlap(selectedEmployee, intervention)) {
					JOptionPane.showMessageDialog(parent,
							"Cet intervenant possède déjà une intervention sur cette plage horaire.");
					continue;
				}

				if (!ActionDialogs.confirmSaving(parent, planning, List.of(intervention), null, false))
					return;
				planning.addIntervention(intervention);
				onChange.run();
				return;

			} catch (DateTimeParseException e) {
				JOptionPane.showMessageDialog(parent,
						"Format invalide. Date : 05/03/27 ou 05/03/2027. " + "Heure : 7h, 07h, 7h30 ou 18h30.");

			} catch (UncheckedIOException e) {
				ActionDialogs.error(parent, e);

			} catch (IllegalArgumentException e) {
				JOptionPane.showMessageDialog(parent, e.getMessage());
			}
		}
	}
}
