package gestionRemplacement;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;

public class InterventionUI extends JPanel {

	private Planning planning;
	private RecurringInterventionList recurringInterventionList;
	private ClientList clients;
	private PersonnelList personnel;
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
	private DefaultTableModel interventionTableModel;
	private JTable interventionTable;
	private JScrollPane interventionScrollPane;
	private String currentSearchText = "";
	private JLabel searchIcon;

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

		createInterventionTable();
	}

	private void createNavigationPanel() {

		JPanel navigationPanel = new JPanel(new BorderLayout());

		JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JPanel pagePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		addInterventionButton = new JButton("Ajouter une intervention");
		addRecurringButton = new JButton("Ajouter une intervention récurrente");
		removeInterventionButton = new JButton("Supprimer une intervention");
		removeRecurringInterventionButton = new JButton("Supprimer une intervention récurrente");
		searchIcon = new JLabel("🔍");
		searchButton = new JTextField(20);
		previousButton = new JButton("<");
		nextButton = new JButton(">");

		String[] views = { "Trier par...", "Dates", "Clients", "Intervenants" };
		viewBox = new JComboBox<>(views);

		actionsPanel.add(addInterventionButton);
		actionsPanel.add(addRecurringButton);
		actionsPanel.add(removeInterventionButton);
		actionsPanel.add(removeRecurringInterventionButton);
		actionsPanel.add(viewBox);

		searchPanel.add(searchIcon);
		searchPanel.add(searchButton);

		pagePanel.add(previousButton);
		pagePanel.add(nextButton);

		UIUtils.addTextChangeListener(searchButton, this::resetSearchIfEmpty);
		navigationPanel.add(actionsPanel, BorderLayout.WEST);
		navigationPanel.add(searchPanel, BorderLayout.CENTER);
		navigationPanel.add(pagePanel, BorderLayout.EAST);
		add(navigationPanel, BorderLayout.NORTH);

		previousButton.addActionListener(event -> previousPage());
		searchButton.addActionListener(event -> search());
		nextButton.addActionListener(event -> nextPage());
		viewBox.addActionListener(event -> view());
		addInterventionButton.addActionListener(event -> openAddIntervention());
		addRecurringButton.addActionListener(event -> openAddRecurringIntervention());
		removeInterventionButton.addActionListener(event -> removeIntervention());
		removeRecurringInterventionButton.addActionListener(event -> removeRecurringIntervention());

	}

	private void createInterventionTable() {

		String[] columns = { "Client", "Jour(s)", "Horaires", "Intervenant", "Type" };

		interventionTableModel = new DefaultTableModel(columns, 0);
		interventionTable = new JTable(interventionTableModel);

		interventionScrollPane = new JScrollPane(interventionTable);

		interventionPanel.setLayout(new BorderLayout());
		interventionPanel.add(interventionScrollPane, BorderLayout.CENTER);
	}

	private void search() {

		currentSearchText = searchButton.getText().trim().toLowerCase();
		currentPage = 0;

		ArrayList<Intervention> results = buildDisplayedInterventions();

		if (!currentSearchText.isBlank() && results.isEmpty()) {
			JOptionPane.showMessageDialog(this, "Aucune correspondance.");
		}

		refreshIntervention(results);
	}

	private ArrayList<Intervention> buildDisplayedInterventions() {

		ArrayList<Intervention> displayedInterventions = new ArrayList<>(planning.getInterventions());

		if (!currentSearchText.isBlank()) {
			boolean searchApplied = false;

			try {

				LocalDate searchedDate = FormatUtils.parseDate(currentSearchText);

				displayedInterventions.removeIf(intervention -> searchedDate.isBefore(intervention.getStartDate())
						|| searchedDate.isAfter(intervention.getEndDate()));

				searchApplied = true;

			} catch (DateTimeParseException e) {
			}

			if (!searchApplied) {

				try {

					LocalTime searchedTime = FormatUtils.parseTime(currentSearchText);

					displayedInterventions.removeIf(intervention -> !intervention.getStartTime().equals(searchedTime));
					searchApplied = true;

				} catch (DateTimeParseException e) {

				}
				if (!searchApplied) {
					displayedInterventions.removeIf(intervention -> !matchesName(intervention, currentSearchText));
				}
			}
		}

		String selectedView = (String) viewBox.getSelectedItem();

		switch (selectedView) {

		case "Dates":
			displayedInterventions
					.sort(Comparator.comparing(Intervention::getStartDate).thenComparing(Intervention::getStartTime));
			break;
		case "Clients":
			displayedInterventions
					.sort(Comparator.comparing((Intervention intervention) -> intervention.getClient().getLastName())
							.thenComparing(intervention -> intervention.getClient().getFirstName()));
			break;
		case "Intervenants":
			displayedInterventions.sort(Comparator.comparing(Intervention::getEmployee, Comparator
					.nullsLast(Comparator.comparing(Employee::getLastName).thenComparing(Employee::getFirstName))));
			break;
		default:
			break;
		}

		return displayedInterventions;
	}

	private void refreshIntervention(ArrayList<Intervention> displayedInterventions) {

		interventionTableModel.setRowCount(0);
		displayedInterventions = buildDisplayedInterventions();

		int interventionsPerPage = getInterventionsPerPage();
		int startIndex = currentPage * interventionsPerPage;
		int endIndex = startIndex + interventionsPerPage;

		endIndex = Math.min(endIndex, displayedInterventions.size());

		for (int i = startIndex; i < endIndex; i++) {

			Intervention intervention = displayedInterventions.get(i);

			String clientFirstName = intervention.getClient().getFirstName().toLowerCase();
			String clientLastName = intervention.getClient().getLastName().toLowerCase();
			String clientFullName = clientLastName + " " + clientFirstName;

			String employeeFullName = "Non attribué";

			if (intervention.getEmployee() != null) {

				String employeeFirstName = intervention.getEmployee().getFirstName().toLowerCase();
				String employeeLastName = intervention.getEmployee().getLastName().toLowerCase();
				employeeFullName = employeeLastName + " " + employeeFirstName;
			}

			DayOfWeek day = intervention.getStartDate().getDayOfWeek();
			LocalTime startHour = intervention.getStartTime();
			LocalTime endHour = intervention.getEndTime();

			String dayOfIntervention = FormatUtils.formatDay(day) + " "
					+ FormatUtils.formatDate(intervention.getStartDate());
			String hours = FormatUtils.formatTime(startHour) + " / " + FormatUtils.formatTime(endHour);
			String type = intervention.getRecurringId();

			if (type == null || type.isBlank()) {
				type = "Unique";

			} else {
				type = "Récurrent";
			}

			interventionTableModel
					.addRow(new Object[] { clientFullName, dayOfIntervention, hours, employeeFullName, type });
		}

		updatePageButtons(displayedInterventions);
		interventionPanel.revalidate();
		interventionPanel.repaint();
	}

	private boolean matchesName(Intervention intervention, String searchText) {

		Client client = intervention.getClient();
		Employee employee = intervention.getEmployee();

		return (client != null && matchesPerson(client.getFirstName(), client.getLastName(), searchText))
				|| (employee != null && matchesPerson(employee.getFirstName(), employee.getLastName(), searchText));
	}

	private boolean matchesPerson(String firstName, String lastName, String searchText) {

		firstName = firstName.toLowerCase();
		lastName = lastName.toLowerCase();
		searchText = searchText.toLowerCase();

		String fullName = firstName + " " + lastName;
		String fullNameReverse = lastName + " " + firstName;

		return firstName.contains(searchText) || lastName.contains(searchText) || fullName.contains(searchText)
				|| fullNameReverse.contains(searchText);
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

	private void removeIntervention() {

		Intervention interventionToRemove = getSelectedIntervention();

		if (interventionToRemove == null) {
			JOptionPane.showMessageDialog(this, "Veuillez sélectionner une intervention.");
			return;
		}

		planning.removeIntervention(interventionToRemove);

		adjustCurrentPage();
		refreshIntervention();
	}

	public void removeRecurringIntervention() {

		Intervention interventionToRemove = getSelectedIntervention();

		if (interventionToRemove == null) {
			JOptionPane.showMessageDialog(this, "Veuillez sélectionner une intervention récurrente.");
			return;
		}

		String recurringIdToRemove = interventionToRemove.getRecurringId();

		if (recurringIdToRemove == null || recurringIdToRemove.isBlank()) {
			JOptionPane.showMessageDialog(this, "Veuillez sélectionner une intervention récurrente.");
			return;
		}

		planning.removeInterventionsByRecurringId(recurringIdToRemove);
		recurringInterventionList.removeRecurringInterventionById(recurringIdToRemove);

		adjustCurrentPage();
		refreshIntervention();
	}

	private void adjustCurrentPage() {

		ArrayList<Intervention> displayedInterventions = buildDisplayedInterventions();

		if (displayedInterventions.isEmpty()) {
			currentPage = 0;
			return;
		}

		int interventionsPerPage = getInterventionsPerPage();
		int endPage = (displayedInterventions.size() - 1) / interventionsPerPage;

		if (currentPage > endPage) {
			currentPage = endPage;
		}
	}

	private Intervention getSelectedIntervention() {

		int selectedRow = interventionTable.getSelectedRow();

		if (selectedRow == -1) {
			return null;
		}

		ArrayList<Intervention> displayedInterventions = buildDisplayedInterventions();
		int currentFirstIndex = currentPage * getInterventionsPerPage();
		int index = currentFirstIndex + selectedRow;

		return displayedInterventions.get(index);
	}

	private int getInterventionsPerPage() {

		int size = interventionScrollPane.getViewport().getExtentSize().height;
		int rowSize = interventionTable.getRowHeight();
		int interventionsVisiblePerPage = 0;

		if (rowSize > 0) {
			interventionsVisiblePerPage = size / rowSize;
		}

		if (interventionsVisiblePerPage > 0) {
			return interventionsVisiblePerPage;
		} else {
			return 1;
		}
	}

	private void resetSearchIfEmpty() {

		if (searchButton.getText().isBlank()) {
			currentSearchText = "";
			currentPage = 0;
			refreshIntervention();
		}
	}

	private void previousPage() {

		if (currentPage > 0) {
			currentPage--;
		}

		refreshIntervention();
	}

	private void nextPage() {

		ArrayList<Intervention> displayedInterventions = buildDisplayedInterventions();
		int interventionsPerPage = getInterventionsPerPage();
		int endPage = (displayedInterventions.size() - 1) / interventionsPerPage;

		if (currentPage < endPage) {
			currentPage++;
		}

		refreshIntervention();
	}

	private void updatePageButtons(ArrayList<Intervention> displayedInterventions) {

		int interventionsPerPage = getInterventionsPerPage();
		int endPage = (displayedInterventions.size() - 1) / interventionsPerPage;

		previousButton.setEnabled(currentPage > 0);
		nextButton.setEnabled(currentPage < endPage);
	}

	private void view() {

		currentPage = 0;
		refreshIntervention();
	}

	public void refreshIntervention() {
		refreshIntervention(buildDisplayedInterventions());
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