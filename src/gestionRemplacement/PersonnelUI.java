package gestionRemplacement;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class PersonnelUI extends JPanel {

	private PersonnelList personnel;
	private Planning planning;
	private JTextField lastNameField;
	private JTextField firstNameField;
	private JTextField contractHoursField;
	private DefaultTableModel tableModel;
	private JTable employeeTable;
	private boolean updatingTable = false;
	private JComboBox<String> viewBox;
	private JComboBox<String> viewBoxx;
	private LocalDate assignedDate = LocalDate.now();
	private LocalDate remainingDate = LocalDate.now();
	private JLabel assignedPeriodLabel = new JLabel("", JLabel.CENTER);
	private JLabel remainingPeriodLabel = new JLabel("", JLabel.CENTER);
	private EmployeeAbsenceList absenceList;

	public PersonnelUI(PersonnelList personnel, Planning planning, EmployeeAbsenceList absenceList) {

		this.personnel = personnel;
		this.planning = planning;
		this.absenceList = absenceList;

		setLayout(new BorderLayout());

		createInputPanel();
		createTable();
		loadTable();
	}

	private void createInputPanel() {

		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new GridLayout(4, 2));

		JLabel lastNameLabel = new JLabel("Nom :");
		JLabel firstNameLabel = new JLabel("Prénom :");
		JLabel contractHoursLabel = new JLabel("Heures hébdomadaires prévues au contrat :");

		firstNameField = new JTextField();
		lastNameField = new JTextField();
		contractHoursField = new JTextField();

		UIUtils.addPlaceholder(contractHoursField, "Ex : 35 - 24,5 - 24h30");

		JButton addButton = new JButton("Ajouter un employé");
		JButton delButton = new JButton("Supprimer un employé");

		inputPanel.add(lastNameLabel);
		inputPanel.add(lastNameField);
		inputPanel.add(firstNameLabel);
		inputPanel.add(firstNameField);
		inputPanel.add(contractHoursLabel);
		inputPanel.add(contractHoursField);
		inputPanel.add(delButton);
		inputPanel.add(addButton);

		add(inputPanel, BorderLayout.NORTH);

		addButton.addActionListener(event -> addEmployee());
		delButton.addActionListener(event -> removeEmployee());
	}

	private void createTable() {

		JPanel titlePanel = new JPanel();
		titlePanel.setLayout(new GridLayout(1, 6));

		JLabel lastName = new JLabel("Nom");
		JLabel firstName = new JLabel("Prénom");
		JLabel contractHoursLabel = new JLabel("Heures prévues au contrat");
		JLabel statut = new JLabel("Statut");

		lastName.setHorizontalAlignment(JLabel.CENTER);
		firstName.setHorizontalAlignment(JLabel.CENTER);
		contractHoursLabel.setHorizontalAlignment(JLabel.CENTER);
		statut.setHorizontalAlignment(JLabel.CENTER);

		String[] viewsAttribuateHours = { "Heures actuellement attribuées pour la semaine",
				"Heures actuellement attribuées pour le mois" };
		String[] viewsNonAttribuateHours = { "Heures restantes à attribuer pour la semaine",
				"Heures restantes à attribuer<br>pour le mois" };
		viewBox = new JComboBox<>(viewsAttribuateHours);
		viewBoxx = new JComboBox<>(viewsNonAttribuateHours);

		titlePanel.add(lastName);
		titlePanel.add(firstName);
		titlePanel.add(contractHoursLabel);
		titlePanel.add(statut);
		titlePanel.add(createHoursHeader(viewBox, true));
		titlePanel.add(createHoursHeader(viewBoxx, false));

		updateHoursPeriodLabel(assignedPeriodLabel, assignedDate, viewBox.getSelectedIndex() == 1);
		updateHoursPeriodLabel(remainingPeriodLabel, remainingDate, viewBoxx.getSelectedIndex() == 1);

		UIUtils.configureResponsiveComboBox(viewBox, titlePanel);
		UIUtils.configureResponsiveComboBox(viewBoxx, titlePanel);

		int headerHeight = titlePanel.getPreferredSize().height;
		titlePanel.setPreferredSize(new Dimension(0, headerHeight));

		String[] columns = { lastName.getText(), firstName.getText(), contractHoursLabel.getText(), statut.getText(),
				viewBox.getSelectedItem().toString(), viewBoxx.getSelectedItem().toString() };

		tableModel = new DefaultTableModel(columns, 0) {

			@Override
			public boolean isCellEditable(int row, int column) {
				return column < 3;
			}
		};

		tableModel.addTableModelListener(event -> {

			if (updatingTable) {
				return;
			}

			int row = event.getFirstRow();
			int column = event.getColumn();

			if (row < 0 || column < 0) {
				return;
			}

			String lastNamet = tableModel.getValueAt(row, 0).toString();
			String firstNamet = tableModel.getValueAt(row, 1).toString();
			String contractHoursText = tableModel.getValueAt(row, 2).toString();
			String statutText = tableModel.getValueAt(row, 3).toString();
			String attribuateHoursText = tableModel.getValueAt(row, 4).toString();
			String nonAttribuateHoursText = tableModel.getValueAt(row, 5).toString();

			if (lastNamet.isBlank() || firstNamet.isBlank() || contractHoursText.isBlank()) {

				Employee employee = personnel.getEmployees().get(row);

				updatingTable = true;

				tableModel.setValueAt(employee.getLastName(), row, 0);
				tableModel.setValueAt(employee.getFirstName(), row, 1);
				tableModel.setValueAt(employee.getContractHours(), row, 2);

				updatingTable = false;

				JOptionPane.showMessageDialog(this,
						"Veuillez renseigner le nom, le prénom et les heures prévus au contrat");
				return;
			}

			try {

				double contractHours = FormatUtils.parseContractHours(contractHoursText);
				double attribuateHours = Double.parseDouble(attribuateHoursText);
				double nonAttribuateHours = Double.parseDouble(nonAttribuateHoursText);

				personnel.editEmployee(row, firstNamet, lastNamet, contractHours, statutText, attribuateHours,
						nonAttribuateHours);

			} catch (NumberFormatException e) {
				Employee employee = personnel.getEmployees().get(row);

				updatingTable = true;
				tableModel.setValueAt(FormatUtils.formatContractHours(employee.getContractHours()), row, 2);
				updatingTable = false;

				JOptionPane.showMessageDialog(this, "Les heures prévues au contrat doivent être un nombre.");
			}
		});

		employeeTable = new JTable(tableModel) {

			@Override
			protected void configureEnclosingScrollPane() {

			}
		};

		employeeTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (!SwingUtilities.isLeftMouseButton(event)) {
					return;
				}

				int row = employeeTable.rowAtPoint(event.getPoint());
				int column = employeeTable.columnAtPoint(event.getPoint());

				if (row < 0 || column < 0) {
					return;
				}

				if (employeeTable.convertColumnIndexToModel(column) == 3) {
					int employeeIndex = employeeTable.convertRowIndexToModel(row);
					openStatusDialog(personnel.getEmployees().get(employeeIndex));
				}
			}
		});

		DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {

				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				boolean operational = EmployeeStatus.OPERATIONAL.toString().equals(value);

				setOpaque(true);
				setBackground(operational ? new Color(0x315C49) : new Color(0x6B3A42));
				setForeground(new Color(0xF1F4F6));

				return this;
			}
		};

		employeeTable.getColumnModel().getColumn(3).setCellRenderer(statusRenderer);

		DefaultTableCellRenderer hoursRenderer = new DefaultTableCellRenderer() {

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

				setToolTipText(UIUtils.getHoursLegend());

				int employeeIndex = table.convertRowIndexToModel(row);
				Employee employee = personnel.getEmployees().get(employeeIndex);

				int modelColumn = table.convertColumnIndexToModel(column);
				boolean monthly = modelColumn == 4 ? viewBox.getSelectedIndex() == 1 : viewBoxx.getSelectedIndex() == 1;

				LocalDate referenceDate = modelColumn == 4 ? assignedDate : remainingDate;

				HoursAlert alert = planning.getHoursAlert(employee, referenceDate, monthly);
				if (!isSelected) {
					setForeground(UIUtils.getHoursColor(alert));
				}

				return this;
			}

		};

		employeeTable.getColumnModel().getColumn(4).setCellRenderer(hoursRenderer);
		employeeTable.getColumnModel().getColumn(5).setCellRenderer(hoursRenderer);
		employeeTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		JScrollPane scrollPane = new JScrollPane(employeeTable);
		scrollPane.setColumnHeaderView(titlePanel);
		viewBox.addActionListener(event -> refreshTable());
		viewBoxx.addActionListener(event -> refreshTable());
		add(scrollPane, BorderLayout.CENTER);
	}

	private void addEmployee() {

		String lastName = lastNameField.getText();
		String firstName = firstNameField.getText();
		String contractHoursText = contractHoursField.getText();
		String statut = "Opérationnel";
		String attribuateHoursText = "0";
		String nonAttribuateHoursText = "0";

		if (lastName.isBlank() || firstName.isBlank() || contractHoursText.isBlank()) {
			JOptionPane.showMessageDialog(this,
					"Veuillez remplir les cases nom, prénom et heures prévues au contrat, sinon l'employé ne sera pas sauvegardé.");
			return;
		}

		try {

			double attribuateHours = Double.parseDouble(attribuateHoursText);
			double nonAttribuateHours = Double.parseDouble(nonAttribuateHoursText);
			double contractHours = FormatUtils.parseContractHours(contractHoursText);

			personnel.addEmployee(firstName, lastName, contractHours, statut, attribuateHours, nonAttribuateHours);
			refreshTable();

			lastNameField.setText("");
			firstNameField.setText("");
			contractHoursField.setText("");

		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this, "Les heures prévues au contrat doivent être un nombre.");
		}
	}

	private void removeEmployee() {

		int index = employeeTable.getSelectedRow();

		if (index == -1) {
			JOptionPane.showMessageDialog(this, "Veuillez sélectionner un employé.");
			return;
		}

		personnel.removeEmployee(index);
		refreshTable();
	}

	private void openStatusDialog(Employee employee) {

		JComboBox<EmployeeStatus> statusBox = new JComboBox<>(EmployeeStatus.values());
		String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/uuuu"));
		JTextField startDateField = new JTextField(today);
		JTextField endDateField = new JTextField(today);

		JPanel form = new JPanel(new BorderLayout(0, 6));

		JPanel statusPanel = new JPanel(new GridLayout(1, 2, 6, 6));
		statusPanel.add(new JLabel("Statut :"));
		statusPanel.add(statusBox);

		JPanel datesPanel = new JPanel(new GridLayout(2, 2, 6, 6));
		datesPanel.add(new JLabel("Date de début :"));
		datesPanel.add(startDateField);
		datesPanel.add(new JLabel("Date de fin :"));
		datesPanel.add(endDateField);

		form.add(statusPanel, BorderLayout.NORTH);
		form.add(datesPanel, BorderLayout.CENTER);

		datesPanel.setVisible(statusBox.getSelectedItem() != EmployeeStatus.OPERATIONAL);

		statusBox.addActionListener(event -> {
			datesPanel.setVisible(statusBox.getSelectedItem() != EmployeeStatus.OPERATIONAL);

			Window dialog = SwingUtilities.getWindowAncestor(form);
			if (dialog != null) {
				dialog.pack();
			}
		});

		UIUtils.applyTheme(form);

		while (true) {
			JOptionPane pane = new JOptionPane(form, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);

			JDialog dialog = pane.createDialog(this, "Statut de " + employee);

			pane.setBackground(UIUtils.getBackgroundColor());
			UIUtils.applyTheme(dialog);

			dialog.pack();
			dialog.setLocationRelativeTo(this);
			dialog.setVisible(true);

			Object choice = pane.getValue();
			dialog.dispose();

			if (!Integer.valueOf(JOptionPane.OK_OPTION).equals(choice)) {
				return;
			}

			EmployeeStatus status = (EmployeeStatus) statusBox.getSelectedItem();

			try {

				if (status == EmployeeStatus.OPERATIONAL) {
					absenceList.resumeEmployee(employee, LocalDate.now());
					refreshTable();
					return;
				}

				LocalDate startDate = FormatUtils.parseDate(startDateField.getText());
				LocalDate endDate = FormatUtils.parseDate(endDateField.getText());

				if (endDate.isBefore(startDate)) {
					JOptionPane.showMessageDialog(this, "La fin doit être le même jour ou après le début.");
					continue;
				}

				EmployeeAbsence absence = new EmployeeAbsence(employee, status, startDate, endDate);
				absenceList.addAbsence(absence);
				refreshTable();
				break;
			} catch (DateTimeParseException exception) {
				JOptionPane.showMessageDialog(this, "Saisis des dates valides au format jj/MM/aaaa.");
			} catch (IllegalArgumentException exception) {
				JOptionPane.showMessageDialog(this, exception.getMessage());
			} catch (IOException exception) {
				JOptionPane.showMessageDialog(this, "Impossible de sauvegarder l'absence : " + exception.getMessage());
			}
		}
	}

	private void loadTable() {

		for (Employee employee : personnel.getEmployees()) {
			boolean assignedMonthly = viewBox.getSelectedIndex() == 1;
			boolean remainingMonthly = viewBoxx.getSelectedIndex() == 1;

			double assignedHoursToDisplay = assignedMonthly ? planning.getMonthyHours(employee, assignedDate)
					: planning.getWeeklyHours(employee, assignedDate);

			double hoursAssignedInRemainingPeriod = remainingMonthly ? planning.getMonthyHours(employee, remainingDate)
					: planning.getWeeklyHours(employee, remainingDate);

			double expectedHours = remainingMonthly ? employee.getContractHours() * 52 / 12
					: employee.getContractHours();

			double remainingHoursToDisplay = expectedHours - hoursAssignedInRemainingPeriod;

			Object[] row = { employee.getLastName(), employee.getFirstName(),
					FormatUtils.formatContractHours(employee.getContractHours()),
					absenceList.getStatusAt(employee, LocalDate.now()).toString(),
					FormatUtils.formatContractHours(assignedHoursToDisplay),
					FormatUtils.formatContractHours(remainingHoursToDisplay) };

			tableModel.addRow(row);
		}

	}

	private JPanel createHoursHeader(JComboBox<String> comboBox, boolean assigned) {

		JPanel header = new JPanel(new BorderLayout());
		JPanel navigation = new JPanel(new BorderLayout());

		JButton previous = new JButton("◀");
		JButton next = new JButton("▶");

		previous.setToolTipText("Période précédente");
		next.setToolTipText("Période suivante");

		navigation.add(previous, BorderLayout.WEST);
		navigation.add(assigned ? assignedPeriodLabel : remainingPeriodLabel, BorderLayout.CENTER);
		navigation.add(next, BorderLayout.EAST);

		header.add(comboBox, BorderLayout.CENTER);
		header.add(navigation, BorderLayout.SOUTH);

		previous.addActionListener(event -> moveHoursPeriod(assigned, -1));
		next.addActionListener(event -> moveHoursPeriod(assigned, 1));

		return header;
	}

	private void updateHoursPeriodLabel(JLabel label, LocalDate date, boolean monthly) {

		LocalDate start = monthly ? date.withDayOfMonth(1) : date.with(DayOfWeek.MONDAY);

		LocalDate end = monthly ? start.plusMonths(1).minusDays(1) : start.plusDays(6);

		DateTimeFormatter shortFormat = DateTimeFormatter.ofPattern("dd/MM");
		DateTimeFormatter fullFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu");

		if (monthly) {
			label.setText(start.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.FRENCH)));
		} else {
			label.setText(start.format(shortFormat) + " – " + end.format(shortFormat));
		}

		label.setToolTipText(start.format(fullFormat) + " – " + end.format(fullFormat));
	}

	private void moveHoursPeriod(boolean assigned, int direction) {

		JComboBox<String> comboBox = assigned ? viewBox : viewBoxx;
		LocalDate date = assigned ? assignedDate : remainingDate;

		date = comboBox.getSelectedIndex() == 1 ? date.withDayOfMonth(1).plusMonths(direction)
				: date.plusWeeks(direction);

		if (assigned) {
			assignedDate = date;
		} else {
			remainingDate = date;
		}

		refreshTable();
	}

	public void refreshTable() {

		updatingTable = true;

		tableModel.setRowCount(0);
		loadTable();

		updateHoursPeriodLabel(assignedPeriodLabel, assignedDate, viewBox.getSelectedIndex() == 1);
		updateHoursPeriodLabel(remainingPeriodLabel, remainingDate, viewBoxx.getSelectedIndex() == 1);

		updatingTable = false;
	}
}
