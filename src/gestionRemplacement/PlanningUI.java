
package gestionRemplacement;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
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

		JPanel navigationPanel = new JPanel(new BorderLayout());
		JPanel periodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel viewPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		previousButton = new JButton("<");
		todayButton = new JButton("Aujourd'hui");
		nextButton = new JButton(">");
		addInterventionButton = new JButton("Ajouter une intervention");
		JButton addRecurringButton = new JButton("Ajouter une intervention récurrente");
		String[] views = { "Mois", "Semaine" };
		viewBox = new JComboBox<>(views);

		periodPanel.add(previousButton);
		periodPanel.add(todayButton);
		periodPanel.add(nextButton);
		viewPanel.add(viewBox);
		actionPanel.add(addInterventionButton);
		actionPanel.add(addRecurringButton);

		navigationPanel.add(periodPanel, BorderLayout.WEST);
		navigationPanel.add(viewPanel, BorderLayout.CENTER);
		navigationPanel.add(actionPanel, BorderLayout.EAST);
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
			LocalTime startTime = FormatUtils.parseTime(startTimeField.getText());
			LocalTime endTime = FormatUtils.parseTime(endTimeField.getText());
			LocalDate startDate = FormatUtils.parseDate(startDateField.getText());
			LocalDate endDate = FormatUtils.parseDate(endDateField.getText());

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
		formPanel.add(new JLabel("Heure de début :"));
		formPanel.add(startTimeField);
		formPanel.add(new JLabel("Heure de fin :"));
		formPanel.add(endTimeField);
		formPanel.add(new JLabel("Date de début :"));
		formPanel.add(startDateField);
		formPanel.add(new JLabel("Date de fin :"));
		formPanel.add(endDateField);

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

			LocalDate startDate = FormatUtils.parseDate(startDateText);
			LocalDate endDate = FormatUtils.parseDate(endDateText);
			LocalTime startTime = FormatUtils.parseTime(startTimeText);
			LocalTime endTime = FormatUtils.parseTime(endTimeText);

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

			if (planning.hasOverlap(selectedClient, intervention)) {

				int confirmation = JOptionPane.showConfirmDialog(this,
						"Une intervention est déjà prévue pour ce client et chevauche ce créneau.\n"
								+ "Voulez-vous vraiment ajouter une deuxième intervention ?",
						"Chevauchement d'interventions", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

				if (confirmation != JOptionPane.YES_OPTION) {
					return;
				}
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

		planningPanel.setLayout(new BorderLayout());
		JPanel bodyPanel = new JPanel(new BorderLayout());
		JPanel daysPanel = new JPanel(new GridBagLayout());
		bodyPanel.add(daysPanel, BorderLayout.CENTER);
		JPanel weekHeaderPanel = new JPanel(new BorderLayout());
		JLabel hoursHeader = new JLabel();
		hoursHeader.setPreferredSize(new Dimension(80, 25));
		weekHeaderPanel.add(hoursHeader, BorderLayout.WEST);
		JPanel dayHeadersPanel = new JPanel(new GridBagLayout());
		weekHeaderPanel.add(dayHeadersPanel, BorderLayout.CENTER);
		JPanel hoursPanel = new JPanel(new GridLayout(24, 1));
		hoursPanel.setPreferredSize(new Dimension(80, 24 * 60));

		for (int hour = 0; hour < 24; hour++) {

			JLabel hourLabel = new JLabel(String.format("%02dh00", hour), JLabel.CENTER);
			hourLabel.setVerticalAlignment(JLabel.TOP);

			hoursPanel.add(hourLabel);
		}

		bodyPanel.add(hoursPanel, BorderLayout.WEST);

		for (int day = 0; day < 7; day++) {

			LocalDate currentDay = monday.plusDays(day);
			final int minimumInterventionWidth = 95;
			JPanel dayColumn = new JPanel(new BorderLayout());
			final int minimumDayWidth = 120;
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE dd/MM", Locale.FRENCH);
			JLabel dayLabel = new JLabel(currentDay.format(formatter), JLabel.CENTER);
			dayLabel.setPreferredSize(new Dimension(120, 25));

			JPanel dayPanel = new JPanel() {

				@Override
				protected void paintComponent(Graphics g) {
					super.paintComponent(g);

					g.setColor(Color.LIGHT_GRAY);

					for (int hour = 1; hour < 24; hour++) {
						int y = hour * 60;
						g.drawLine(0, y, getWidth(), y);
					}
				}
			};

			ArrayList<Integer> columnEndMinutes = new ArrayList<>();

			dayPanel.setLayout(null);
			dayPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
			dayPanel.setPreferredSize(new Dimension(120, 24 * 60));

			dayPanel.addComponentListener(new ComponentAdapter() {

				@Override
				public void componentResized(ComponentEvent e) {

					int columnCount = Math.max(1, columnEndMinutes.size());
					int columnWidth = Math.max(minimumInterventionWidth + 4, dayPanel.getWidth() / columnCount);

					for (Component component : dayPanel.getComponents()) {

						JButton button = (JButton) component;
						int columnIndex = (Integer) button.getClientProperty("columnIndex");
						int x = columnIndex * columnWidth + 2;
						int width = Math.max(1, columnWidth - 4);

						button.setBounds(x, button.getY(), width, button.getHeight());
					}
				}
			});

			dayColumn.add(dayPanel, BorderLayout.CENTER);

			ArrayList<Intervention> dayInterventions = planning.getInterventionsForDay(currentDay);

			dayInterventions
					.sort(Comparator.comparing(Intervention::getStartDate).thenComparing(Intervention::getStartTime));

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
				int columnIndex = 0;

				while (columnIndex < columnEndMinutes.size() && columnEndMinutes.get(columnIndex) > startMinutes) {
					columnIndex++;
				}

				if (columnIndex == columnEndMinutes.size()) {
					columnEndMinutes.add(endMinutes);
				} else {
					columnEndMinutes.set(columnIndex, endMinutes);
				}

				String employeeText;

				if (intervention.getEmployee() == null) {
					employeeText = "Non attribué";
				} else {
					employeeText = intervention.getEmployee().toString();
				}

				String startText = String.format("%02dh%02d", startMinutes / 60, startMinutes % 60);
				String endText = String.format("%02dh%02d", (endMinutes / 60) % 24, endMinutes % 60);
				String text = "<html><div style='text-align: center;'>" + intervention.getClient() + "<br>" + startText
						+ " → " + endText + "<br>" + employeeText + "</div></html>";
				JButton interventionButton = new JButton(text);
				interventionButton.setHorizontalAlignment(JLabel.CENTER);
				interventionButton.setVerticalAlignment(JLabel.CENTER);
				interventionButton.putClientProperty("columnIndex", columnIndex);
				UIUtils.styleInterventionButton(interventionButton, intervention.getEmployee() != null);

				interventionButton.addActionListener(event -> openEmployeeSelection(intervention));
				interventionButton.setBounds(2, y, 116, height);

				dayPanel.add(interventionButton);
			}

			int columnCount = columnEndMinutes.size();

			int dayWidth = Math.max(minimumDayWidth, columnCount * (minimumInterventionWidth + 4));

			dayPanel.setPreferredSize(new Dimension(dayWidth, 24 * 60));
			dayPanel.setMinimumSize(new Dimension(dayWidth, 24 * 60));

			dayLabel.setPreferredSize(new Dimension(dayWidth, 25));
			dayLabel.setMinimumSize(new Dimension(dayWidth, 25));

			GridBagConstraints constraints = new GridBagConstraints();
			constraints.gridx = day;
			constraints.gridy = 0;
			constraints.weightx = 1;
			constraints.weighty = 1;
			constraints.fill = GridBagConstraints.BOTH;

			daysPanel.add(dayColumn, constraints);
			dayHeadersPanel.add(dayLabel, constraints);
		}

		JScrollPane scrollPane = new JScrollPane(bodyPanel);
		scrollPane.setColumnHeaderView(weekHeaderPanel);
		scrollPane.getVerticalScrollBar().setUnitIncrement(25);
		
		planningPanel.add(scrollPane, BorderLayout.CENTER);
		UIUtils.applyTheme(planningPanel);
	}

	private void displayMonth() {

		LocalDate firstDay = currentDate.withDayOfMonth(1);

		int numberOfDays = currentDate.lengthOfMonth();
		int firstDayPosition = firstDay.getDayOfWeek().getValue();
		int numberOfWeeks = (int) Math.ceil((firstDayPosition - 1 + numberOfDays) / 7.0);

		JPanel monthPanel = new JPanel(new GridLayout(0, 7));
		LocalDate calendarStart = firstDay.minusDays(firstDayPosition - 1);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE dd/MM", Locale.FRENCH);

		for (int i = 0; i < firstDayPosition - 1; i++) {

			LocalDate day = calendarStart.plusDays(i);
			JPanel emptyDay = new JPanel(new BorderLayout());
			JLabel dayLabel = new JLabel(day.format(formatter), JLabel.CENTER);
			emptyDay.add(dayLabel, BorderLayout.NORTH);
			emptyDay.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

			monthPanel.add(emptyDay);
		}

		for (int i = 1; i <= numberOfDays; i++) {

			LocalDate day = firstDay.plusDays(i - 1);
			String dayText = day.format(formatter);
			JPanel dayPanel = new JPanel(new BorderLayout());
			dayPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

			JLabel dayLabel = new JLabel(dayText, JLabel.CENTER);
			dayLabel.setFont(new Font("Arial", Font.BOLD, 17));
			dayPanel.add(dayLabel, BorderLayout.NORTH);

			ArrayList<Intervention> dayInterventions = planning.getInterventionsForDay(day);
			dayInterventions
					.sort(Comparator.comparing(Intervention::getStartDate).thenComparing(Intervention::getStartTime));

			JPanel interventionsPanel = new JPanel();
			interventionsPanel.setLayout(new BoxLayout(interventionsPanel, BoxLayout.Y_AXIS));

			dayPanel.add(interventionsPanel, BorderLayout.CENTER);

			for (Intervention intervention : dayInterventions) {

				int startMinutes = 0;

				if (intervention.getStartDate().equals(day)) {
					startMinutes = intervention.getStartTime().getHour() * 60 + intervention.getStartTime().getMinute();
				}

				int endMinutes = 24 * 60;

				if (intervention.getEndDate().equals(day)) {
					endMinutes = intervention.getEndTime().getHour() * 60 + intervention.getEndTime().getMinute();
				}

				if (endMinutes <= startMinutes) {
					continue;
				}

				String employeeText;

				if (intervention.getEmployee() == null) {
					employeeText = "Non attribué";
				} else {
					employeeText = intervention.getEmployee().toString();
				}

				String startText = String.format("%02dh%02d", startMinutes / 60, startMinutes % 60);
				String endText = String.format("%02dh%02d", (endMinutes / 60) % 24, endMinutes % 60);
				String text = "<html><div style='text-align: center;'>" + intervention.getClient() + "<br>" + startText
						+ " → " + endText + "<br>" + employeeText + "</div></html>";

				JButton interventionButton = new JButton(text);
				interventionButton.setHorizontalAlignment(JLabel.CENTER);
				interventionButton.setVerticalAlignment(JLabel.CENTER);
				interventionButton.setFont(new Font("Arial", Font.PLAIN, 16));
				interventionButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
				interventionButton.addActionListener(event -> openEmployeeSelection(intervention));
				UIUtils.styleInterventionButton(interventionButton, intervention.getEmployee() != null);

				interventionsPanel.add(interventionButton);
			}

			monthPanel.add(dayPanel);
		}

		int usedCells = (firstDayPosition - 1) + numberOfDays;
		int remainingCells = numberOfWeeks * 7 - usedCells;
		LocalDate firstDayNextMonth = firstDay.plusMonths(1);

		for (int i = 0; i < remainingCells; i++) {
			LocalDate day = firstDayNextMonth.plusDays(i);
			JPanel nextMonthDay = new JPanel(new BorderLayout());
			JLabel dayLabel = new JLabel(day.format(formatter), JLabel.CENTER);

			nextMonthDay.add(dayLabel, BorderLayout.NORTH);
			nextMonthDay.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
			monthPanel.add(nextMonthDay);
		}

		planningPanel.setLayout(new BorderLayout());
		JScrollPane scrollPane = new JScrollPane(monthPanel);
		scrollPane.getVerticalScrollBar().setUnitIncrement(25);

		planningPanel.add(scrollPane, BorderLayout.CENTER);
		UIUtils.applyTheme(planningPanel);
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

	public void refreshPlanning() {

		planningPanel.removeAll();

		if (viewBox.getSelectedItem().equals("Semaine")) {
			displayWeek();
		} else {
			displayMonth();
		}
		
		UIUtils.applyTheme(planningPanel);
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