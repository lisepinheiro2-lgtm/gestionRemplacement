package gestionRemplacement;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class InterventionUI extends JPanel {

	private Planning planning;
	private RecurringInterventionList recurringInterventionList;
	private ClientList clients;
	private PersonnelList personnel;
	private DefaultTableModel interventionTableModel;
	private JPanel interventionPanel;
	private LocalDate currentDate;
	private JButton previousButton;
	private JButton nextButton;
	private JTextField searchButton;
	private JButton addInterventionButton;
	private JButton addRecurringButton;
	private JButton removeInterventionButton;
	private JButton removeRecurringInterventionButton;
	private JComboBox<String> viewBox;
	private int currentPage = 0;

	public InterventionUI(Planning planning, RecurringInterventionList recurringInterventionList, ClientList clients,
			PersonnelList personnel) {

		this.planning = planning;
		this.recurringInterventionList = recurringInterventionList;
		this.clients = clients;
		this.personnel = personnel;

		currentDate = LocalDate.now();
		setLayout(new BorderLayout());
		createNavigationPanel();
		interventionPanel = new JPanel();
		add(interventionPanel, BorderLayout.CENTER);
	}

	private void createNavigationPanel() {

		JPanel navigationPanel = new JPanel(new FlowLayout());

		previousButton = new JButton("<");
		searchButton = new JTextField();
		nextButton = new JButton(">");
		addInterventionButton = new JButton("Ajouter une intervention");
		addRecurringButton = new JButton("Ajouter une intervention récurrente");
		removeInterventionButton = new JButton("Supprimer une intervention");
		removeRecurringInterventionButton = new JButton("Supprimer une intervention récurrente");

		String[] views = { "Trier par...", "Dates", "Clients", "Intervenants" };
		viewBox = new JComboBox<>(views);

		JLabel clientLabel = new JLabel("Client");
		JLabel dayLabel = new JLabel("Jour(s)");
		JLabel hourLabel = new JLabel("Horaire(s)");
		JLabel intervenantLabel = new JLabel("Intervenant");
		JLabel typeLabel = new JLabel("Type");

		navigationPanel.add(previousButton);
		navigationPanel.add(searchButton);
		navigationPanel.add(nextButton);
		navigationPanel.add(addInterventionButton);
		navigationPanel.add(addRecurringButton);
		navigationPanel.add(removeInterventionButton);
		navigationPanel.add(removeRecurringInterventionButton);
		navigationPanel.add(viewBox);

		add(navigationPanel, BorderLayout.NORTH);

		previousButton.addActionListener(event -> previousPage());
		searchButton.addActionListener(event -> search());
		nextButton.addActionListener(event -> nextPage());
		viewBox.addActionListener(event -> view());
		addInterventionButton.addActionListener(event -> openAddIntervention());
		addRecurringButton.addActionListener(event -> openAddRecurringIntervention());
		removeInterventionButton.addActionListener(event -> removeIntervention());
		removeRecurringInterventionButton.addActionListener(event -> removeRecurringInterventionById());

	}

	private void view() {

		if (viewBox.getSelectedItem().equals("Trier par...")) {
			return;
		}
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
			refreshIntervention();

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

			planning.addIntervention(intervention);
			refreshIntervention();

		} catch (DateTimeParseException e) {
			JOptionPane.showMessageDialog(this,
					"Format invalide. Date : 05/03/27 ou 05/03/2027. " + "Heure : 7h, 07h, 7h30 ou 18h30.");
		}
	}

	public void removeRecurringInterventionById(String recurringId) {

		for (int i = recurringInterventionList.recurringInterventions.size() - 1; i >= 0; i--) {

			RecurringIntervention currentIntervention = recurringInterventionList.recurringInterventions.get(i);
			String currentId = currentIntervention.getId();

			if (recurringId.equals(currentId)) {
				recurringInterventionList.recurringInterventions.remove(i);
				break;
			}
		}

		recurringInterventionList.saveRecurringInterventions();
	}

	private void previousPage() {

		if (currentPage > 0) {
			currentPage--;
		}

		refreshIntervention();
	}

	private void nextPage() {

		int endPage = (planning.getInterventions().size() - 1) / 10;
		if (currentPage < endPage) {
			currentPage++;
		}

		refreshIntervention();
	}

	public void refreshIntervention() {

		interventionPanel.removeAll();

		if (viewBox.getSelectedItem().equals("Dates")) {

		}
		if (viewBox.getSelectedItem().equals("Clients")) {

		}
		if (viewBox.getSelectedItem().equals("Intervenants")) {

		}

		interventionPanel.revalidate();
		interventionPanel.repaint();
	}

	public void addRecurringIntervention(RecurringIntervention recurringIntervention) {
		recurringInterventionList.recurringInterventions.add(recurringIntervention);
		recurringInterventionList.saveRecurringInterventions();
	}

	public void removeRecurringIntervention(int index) {
		recurringInterventionList.recurringInterventions.remove(index);
		recurringInterventionList.saveRecurringInterventions();
	}

}