
package gestionRemplacement;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class PlanningUI extends JPanel {

	private Planning planning;
	private PersonnelList personnel;
	private ClientList clients;
	private RecurringInterventionList recurringInterventionList;

	private LocalDate currentDate;

	private JPanel planningPanel;

	private JButton previousButton;
	private JButton todayButton;
	private JButton nextButton;
	private JButton addInterventionButton;

	private JComboBox<String> viewBox;

	public PlanningUI(Planning planning, PersonnelList personnel, ClientList clients,
			RecurringInterventionList recurringInterventionList) {

		this.planning = planning;
		this.personnel = personnel;
		this.clients = clients;
		this.recurringInterventionList = recurringInterventionList;

		currentDate = LocalDate.now();
		setLayout(new BorderLayout());
		createNavigationPanel();
		planningPanel = new JPanel();
		add(planningPanel, BorderLayout.CENTER);
	}

	private void createNavigationPanel() {

		JPanel navigationPanel = new JPanel(new FlowLayout());

		previousButton = new JButton("<");
		todayButton = new JButton("Aujourd'hui");
		nextButton = new JButton(">");
		addInterventionButton = new JButton("Ajouter une intervention");
		JButton addRecurringButton = new JButton("Ajouter une intervention récurrente");

		String[] views = { "Semaine", "Mois" };
		viewBox = new JComboBox<>(views);

		navigationPanel.add(previousButton);
		navigationPanel.add(todayButton);
		navigationPanel.add(nextButton);
		navigationPanel.add(viewBox);
		navigationPanel.add(addInterventionButton);
		navigationPanel.add(addInterventionButton);
		navigationPanel.add(addRecurringButton);

		add(navigationPanel, BorderLayout.NORTH);

		previousButton.addActionListener(event -> previousPeriod());
		todayButton.addActionListener(event -> today());
		nextButton.addActionListener(event -> nextPeriod());
		viewBox.addActionListener(event -> refreshPlanning());
		addInterventionButton.addActionListener(event -> openAddIntervention());
		addRecurringButton.addActionListener(event -> openAddRecurringIntervention());
	}

	private void openAddRecurringIntervention() {

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

		int result = JOptionPane.showConfirmDialog(this, formPanel, "Ajouter une intervention récurrente",
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
			LocalTime startTime = parseTime(startTimeField.getText());
			LocalTime endTime = parseTime(endTimeField.getText());
			LocalDate startDate = parseDate(startDateField.getText());
			LocalDate endDate = parseDate(endDateField.getText());

			if (!endDate.isAfter(startDate) && !endDate.equals(startDate)) {
				JOptionPane.showMessageDialog(this, "La date de fin doit être postérieure à la date de début.");
				return;
			}

			if (!endTime.isAfter(startTime)) {
				JOptionPane.showMessageDialog(this, "L'heure de fin doit être postérieure à l'heure de début.");
				return;
			}
			RecurringIntervention recurring = new RecurringIntervention(selectedClient, selectedEmployee, dayOfWeek,
					startTime, endTime, startDate, endDate);
			boolean generated = planning.generateRecurringInterventions(recurring);

			if (!generated) {
				JOptionPane.showMessageDialog(this, "Impossible de créer cette récurrence : "
						+ "l'intervenant est déjà occupé " + "sur au moins une des interventions.");
				return;
			}

			recurringInterventionList.addRecurringIntervention(recurring);
			refreshPlanning();

		} catch (DateTimeParseException e) {
			JOptionPane.showMessageDialog(this,
					"Format invalide.\n" + "Date : 05/03/2027\n" + "Heure : 8h, 8h30 ou 14h15");
		}
	}

	private void openAddIntervention() {

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

		int result = JOptionPane.showConfirmDialog(this, formPanel, "Ajouter une intervention",
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

			LocalDate startDate = parseDate(startDateText);
			LocalDate endDate = parseDate(endDateText);
			LocalTime startTime = parseTime(startTimeText);
			LocalTime endTime = parseTime(endTimeText);

			LocalDateTime start = LocalDateTime.of(startDate, startTime);
			LocalDateTime end = LocalDateTime.of(endDate, endTime);

			if (!end.isAfter(start)) {
				JOptionPane.showMessageDialog(this, "La fin de l'intervention doit être postérieure au début.");
				return;
			}

			Intervention intervention = new Intervention(selectedClient, selectedEmployee, startDate, startTime,
					endDate, endTime);

			if (selectedEmployee != null && planning.hasOverlap(selectedEmployee, intervention)) {
				JOptionPane.showMessageDialog(this,
						"Cet intervenant possède déjà une intervention sur cette plage horaire.");
				return;
			}

			planning.addIntervention(intervention);
			refreshPlanning();

		} catch (DateTimeParseException e) {
			JOptionPane.showMessageDialog(this,
					"Format invalide. Date : 05/03/27 ou 05/03/2027. " + "Heure : 7h, 07h, 7h30 ou 18h30.");
		}
	}

	private void previousPeriod() {

		if (viewBox.getSelectedItem().equals("Semaine")) {
			currentDate = currentDate.minusWeeks(1);
		} else {
			currentDate = currentDate.minusMonths(1);
		}

		refreshPlanning();
	}

	private void nextPeriod() {

		if (viewBox.getSelectedItem().equals("Semaine")) {
			currentDate = currentDate.plusWeeks(1);
		} else {
			currentDate = currentDate.plusMonths(1);
		}

		refreshPlanning();
	}

	private void today() {
		currentDate = LocalDate.now();
		refreshPlanning();
	}

	private void displayWeek() {

		LocalDate monday = currentDate.with(DayOfWeek.MONDAY);
		JPanel weekPanel = new JPanel(new GridLayout(1, 8));
		weekPanel.add(new JLabel(""));

		for (int i = 0; i < 7; i++) {

			LocalDate day = monday.plusDays(i);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE dd/MM", Locale.FRENCH);
			String dayText = day.format(formatter);
			JLabel dayLabel = new JLabel(dayText, JLabel.CENTER);

			weekPanel.add(dayLabel);
		}

		planningPanel.setLayout(new BorderLayout());
		planningPanel.add(weekPanel, BorderLayout.NORTH);

		JPanel bodyPanel = new JPanel(new GridLayout(1, 8));
		JPanel hoursPanel = new JPanel(new GridLayout(24, 1));
		hoursPanel.setPreferredSize(new Dimension(80, 24 * 60));

		for (int hour = 0; hour < 24; hour++) {
			JLabel hourLabel = new JLabel(String.format("%02dh00", hour), JLabel.CENTER);

			hoursPanel.add(hourLabel);
		}

		bodyPanel.add(hoursPanel);

		for (int day = 0; day < 7; day++) {

			LocalDate currentDay = monday.plusDays(day);
			JPanel dayPanel = new JPanel();

			dayPanel.setLayout(null);
			dayPanel.setPreferredSize(new Dimension(120, 24 * 60));

			bodyPanel.add(dayPanel);

			ArrayList<Intervention> dayInterventions = planning.getInterventionsForDay(currentDay);

			for (Intervention intervention : dayInterventions) {

				int startMinutes;

				if (intervention.getStartDate().equals(currentDay)) {
					startMinutes = intervention.getStartTime().getHour() * 60 + intervention.getStartTime().getMinute();

				} else {
					startMinutes = 0;
				}

				int endMinutes;

				if (intervention.getEndDate().equals(currentDay)) {
					endMinutes = intervention.getEndTime().getHour() * 60 + intervention.getEndTime().getMinute();

				} else {
					endMinutes = 24 * 60;
				}

				int y = startMinutes;
				int height = endMinutes - startMinutes;

				String employeeText;

				if (intervention.getEmployee() == null) {
					employeeText = "Non attribué";
				} else {
					employeeText = intervention.getEmployee().toString();
				}

				String text = "<html>" + intervention.getClient() + "<br>" + intervention.getStartTime() + " → "
						+ intervention.getEndTime() + "<br>" + employeeText + "</html>";

				JButton interventionButton = new JButton(text);

				if (intervention.getEmployee() == null) {
					interventionButton.setBackground(Color.RED);
				} else {
					interventionButton.setBackground(Color.GREEN);
				}

				interventionButton.addActionListener(event -> openEmployeeSelection(intervention));
				interventionButton.setBounds(2, y, 116, height);

				dayPanel.add(interventionButton);
			}
		}

		JScrollPane scrollPane = new JScrollPane(bodyPanel);
		planningPanel.add(scrollPane, BorderLayout.CENTER);
	}

	private void displayMonth() {

		LocalDate firstDay = currentDate.withDayOfMonth(1);

		int numberOfDays = currentDate.lengthOfMonth();
		int firstDayPosition = firstDay.getDayOfWeek().getValue();
		int numberOfWeeks = (int) Math.ceil((firstDayPosition - 1 + numberOfDays) / 7.0);

		JPanel monthPanel = new JPanel(new GridLayout(0, 7));
		monthPanel.setPreferredSize(new Dimension(7 * 160, numberOfWeeks * 170));

		int dayWidth = 200;
		int dayHeight = 180;

		monthPanel.setPreferredSize(new Dimension(dayWidth * 7, dayHeight * numberOfWeeks));

		String[] dayNames = { "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche" };

		for (String dayName : dayNames) {

			JLabel dayNameLabel = new JLabel(dayName, JLabel.CENTER);
			dayNameLabel.setFont(new Font("Arial", Font.BOLD, 18));

			monthPanel.add(dayNameLabel);
		}

		for (int i = 0; i < firstDayPosition - 1; i++) {
			JPanel emptyDay = new JPanel();
			emptyDay.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

			monthPanel.add(emptyDay);
		}

		for (int i = 1; i <= numberOfDays; i++) {

			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE dd/MM", Locale.FRENCH);
			LocalDate day = firstDay.plusDays(i - 1);
			String dayText = day.format(formatter);
			JPanel dayPanel = new JPanel(new BorderLayout());
			dayPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

			JLabel dayLabel = new JLabel(dayText, JLabel.CENTER);
			dayLabel.setFont(new Font("Arial", Font.BOLD, 17));
			dayPanel.add(dayLabel, BorderLayout.NORTH);

			ArrayList<Intervention> dayInterventions = planning.getInterventionsForDay(day);
			JPanel interventionsPanel = new JPanel();
			interventionsPanel.setLayout(new BoxLayout(interventionsPanel, BoxLayout.Y_AXIS));

			dayPanel.add(interventionsPanel, BorderLayout.CENTER);

			for (Intervention intervention : dayInterventions) {

				String employeeText;

				if (intervention.getEmployee() == null) {
					employeeText = "Non attribué";
				} else {
					employeeText = intervention.getEmployee().toString();
				}

				String text = "<html>" + intervention.getClient() + "<br>" + intervention.getStartTime() + " → "
						+ intervention.getEndTime() + "<br>" + employeeText + "</html>";
				JButton interventionButton = new JButton(text);
				interventionButton.setFont(new Font("Arial", Font.PLAIN, 16));
				interventionButton.setPreferredSize(new Dimension(200, 75));
				interventionButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
				interventionButton.addActionListener(event -> openEmployeeSelection(intervention));

				if (intervention.getEmployee() == null) {
					interventionButton.setBackground(Color.RED);
				} else {
					interventionButton.setBackground(Color.GREEN);
				}

				interventionsPanel.add(interventionButton);
			}

			monthPanel.add(dayPanel);
		}

		planningPanel.setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane(monthPanel);

		planningPanel.add(scrollPane, BorderLayout.CENTER);

	}

	private void openEmployeeSelection(Intervention intervention) {

		JPanel employeesPanel = new JPanel();
		employeesPanel.setLayout(new BoxLayout(employeesPanel, BoxLayout.Y_AXIS));

		for (Employee employee : personnel.getEmployees()) {

			double currentHours = planning.getWeeklyHours(employee, intervention.getStartDate());
			double projectedHours = currentHours;

			if (intervention.getEmployee() != employee) {
				projectedHours += intervention.getDurationHours();
			}

			String text = employee + " - " + String.format("%.2f", projectedHours) + "h / "
					+ String.format("%.2f", employee.getContractHours()) + "h";

			JButton employeeButton = new JButton(text);

			if (planning.hasOverlap(employee, intervention)) {
				employeeButton.setEnabled(false);

			} else if (projectedHours < employee.getContractHours()) {
				employeeButton.setBackground(Color.GREEN);

			} else if (projectedHours == employee.getContractHours()) {
				employeeButton.setBackground(Color.YELLOW);

			} else {
				employeeButton.setBackground(Color.RED);
			}
			employeeButton.addActionListener(event -> {

				planning.assignEmployee(intervention, employee);

				refreshPlanning();

				Window window = SwingUtilities.getWindowAncestor(employeeButton);

				if (window != null) {
					window.dispose();
				}
			});

			employeesPanel.add(employeeButton);
		}

		JOptionPane.showMessageDialog(this, employeesPanel, "Attribuer l'intervention", JOptionPane.PLAIN_MESSAGE);
	}

	private LocalDate parseDate(String text) {

		DateTimeFormatter longYear = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
		DateTimeFormatter shortYear = new DateTimeFormatterBuilder().appendPattern("dd/MM/")
				.appendValueReduced(ChronoField.YEAR, 2, 2, 2000).toFormatter().withResolverStyle(ResolverStyle.STRICT);

		try {

			return LocalDate.parse(text.trim(), longYear);

		} catch (DateTimeParseException e) {
			return LocalDate.parse(text.trim(), shortYear);
		}
	}

	private LocalTime parseTime(String text) {

		DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendValue(ChronoField.HOUR_OF_DAY)
				.appendLiteral('h').optionalStart().appendValue(ChronoField.MINUTE_OF_HOUR, 2).optionalEnd()
				.parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0).toFormatter().withResolverStyle(ResolverStyle.STRICT);
		return LocalTime.parse(text.trim().toLowerCase(), formatter);
	}

	public void refreshPlanning() {

		planningPanel.removeAll();

		if (viewBox.getSelectedItem().equals("Semaine")) {
			displayWeek();
		} else {
			displayMonth();
		}

		planningPanel.revalidate();
		planningPanel.repaint();
	}

	public ClientList getClients() {
		return clients;
	}

	public void setClients(ClientList clients) {
		this.clients = clients;
	}
}