package gestionRemplacement;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class ReplacementUI extends JPanel {

	private ReplacementList replacementList;
	private PersonnelList personnel;
	private Planning planning;

	private JComboBox<Employee> employeeToReplaceBox;
	private JComboBox<Employee> replacementEmployeeBox;

	private JCheckBox assignAllCheckBox;

	private JTextField startDateField;
	private JTextField startTimeField;
	private JTextField endTimeField;
	private JTextField endDateField;

	private DefaultTableModel tableModel;
	private JTable replacementTable;

	public ReplacementUI(ReplacementList replacementList, PersonnelList personnel, Planning planning) {

		this.replacementList = replacementList;
		this.personnel = personnel;
		this.planning = planning;

		setLayout(new BorderLayout());

		createInputPanel();
		createTable();
		loadTable();
	}

	private void createInputPanel() {

		JPanel inputPanel = new JPanel(new GridLayout(8, 2));

		employeeToReplaceBox = new JComboBox<>();
		replacementEmployeeBox = new JComboBox<>();

		startDateField = new JTextField();
		startTimeField = new JTextField();
		endDateField = new JTextField();
		endTimeField = new JTextField();

		UIUtils.addPlaceholder(startDateField, "jj/MM/aaaa");
		UIUtils.addPlaceholder(startTimeField, "__ h __");
		UIUtils.addPlaceholder(endDateField, "jj/MM/aaaa");
		UIUtils.addPlaceholder(endTimeField, "__ h __");

		JButton addButton = new JButton("Ajouter un remplacement");
		JButton removeButton = new JButton("Supprimer le remplacement");

		for (Employee employee : personnel.getEmployees()) {

			employeeToReplaceBox.addItem(employee);
			replacementEmployeeBox.addItem(employee);
		}

		inputPanel.add(new JLabel("Employé à remplacer : "));
		inputPanel.add(employeeToReplaceBox);
		inputPanel.add(new JLabel("Employé remplaçant : "));
		inputPanel.add(replacementEmployeeBox);
		inputPanel.add(new JLabel("Date de début : "));
		inputPanel.add(startDateField);
		inputPanel.add(new JLabel("Heure de début : "));
		inputPanel.add(startTimeField);
		inputPanel.add(new JLabel("Date de fin : "));
		inputPanel.add(endDateField);
		inputPanel.add(new JLabel("Heure de fin : "));
		inputPanel.add(endTimeField);

		inputPanel.add(removeButton);
		inputPanel.add(addButton);

		assignAllCheckBox = new JCheckBox("Attribuer toutes les interventions au remplaçant");

		inputPanel.add(assignAllCheckBox);
		inputPanel.add(new JLabel(""));

		addButton.addActionListener(event -> addReplacement());
		removeButton.addActionListener(event -> removeReplacement());

		add(inputPanel, BorderLayout.NORTH);
	}

	private void addReplacement() {

		String startDate = startDateField.getText();
		String endDate = endDateField.getText();
		String startTime = startTimeField.getText();
		String endTime = endTimeField.getText();

		if (startDate.isBlank() || startTime.isBlank() || endTime.isBlank()) {
			JOptionPane.showMessageDialog(this, "Veuillez renseigner la date, l'heure de début et l'heure de fin.");
			return;
		}

		LocalDate parsedStartDate;
		LocalDate parsedEndDate;
		LocalTime parsedStartTime;
		LocalTime parsedEndTime;

		try {

			parsedStartTime = FormatUtils.parseTime(startTime);
			parsedEndTime = FormatUtils.parseTime(endTime);
			parsedStartDate = FormatUtils.parseDate(startDate);

			if (endDate.isBlank() || endDate.equals("jj/MM/aaaa")) {

				if (!parsedEndTime.isAfter(parsedStartTime)) {
					JOptionPane.showMessageDialog(this,
							"La date de fin est obligatoire lorsque le remplacement se termine le lendemain ou plus tard.");
					return;
				}

				parsedEndDate = parsedStartDate;

			} else {
				parsedEndDate = FormatUtils.parseDate(endDate);
			}

		} catch (DateTimeParseException e) {

			JOptionPane.showMessageDialog(this,
					"Format invalide. Date : 05/03/27 ou 05/03/2027. Heure : 7h, 07h, 7h30 ou 18h30.");
			return;
		}

		LocalDateTime start = LocalDateTime.of(parsedStartDate, parsedStartTime);
		LocalDateTime end = LocalDateTime.of(parsedEndDate, parsedEndTime);

		if (!end.isAfter(start)) {
			JOptionPane.showMessageDialog(this, "La fin du remplacement doit être postérieure au début.");
			return;
		}

		startDate = parsedStartDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		startTime = parsedStartTime.format(DateTimeFormatter.ofPattern("HH'h'mm"));
		endDate = parsedEndDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
		endTime = parsedEndTime.format(DateTimeFormatter.ofPattern("HH'h'mm"));

		Employee employeeToReplace = (Employee) employeeToReplaceBox.getSelectedItem();
		Employee replacementEmployee = null;
		double contractHoursEmployeeToReplace = employeeToReplace.getContractHours();
		double contractHoursReplacementEmployee = 0;

		if (assignAllCheckBox.isSelected()) {
			replacementEmployee = (Employee) replacementEmployeeBox.getSelectedItem();
			contractHoursReplacementEmployee = replacementEmployee.getContractHours();

			if (employeeToReplace == replacementEmployee) {
				JOptionPane.showMessageDialog(this, "L'employé à remplacer et le remplaçant doivent être différents.");
				return;
			}

			boolean replaced = planning.replaceEmployeeInterventions(employeeToReplace, replacementEmployee,
					parsedStartDate, parsedEndDate);

			if (!replaced) {
				JOptionPane.showMessageDialog(this, "Le remplacement global est impossible : "
						+ "le remplaçant a déjà une intervention sur l'une de ces plages.");
				return;
			}
		} else {
			planning.unassignEmployeeInterventions(employeeToReplace, parsedStartDate, parsedEndDate);
		}

		replacementList.addReplacement(employeeToReplace, contractHoursEmployeeToReplace, replacementEmployee,
				contractHoursReplacementEmployee, startDate, startTime, endDate, endTime);

		refreshTable();

		startDateField.setText("");
		endDateField.setText("");
		startTimeField.setText("");
		endTimeField.setText("");
	}

	private void removeReplacement() {

		int index = replacementTable.getSelectedRow();

		if (index == -1) {
			JOptionPane.showMessageDialog(this, "Veuillez sélectionner un remplacement.");
			return;
		}

		replacementList.removeReplacement(index);
		refreshTable();
	}

	private void createTable() {

		String[] columns = { "Employé à remplacer", "Heures contrat", "Remplaçant", "Heures contrat", "Date début",
				"Début", "Date fin", "Fin" };

		tableModel = new DefaultTableModel(columns, 0);
		replacementTable = new JTable(tableModel);

		JScrollPane scrollPane = new JScrollPane(replacementTable);
		add(scrollPane, BorderLayout.CENTER);
	}

	private void loadTable() {

		for (Replacement replacement : replacementList.getReplacements()) {

			Employee employeeToReplace = personnel.findEmployee(replacement.getEmployeeToReplace());
			Employee replacementEmployee = personnel.findEmployee(replacement.getReplacementEmployee());
			String replacementText = "Attribution individuelle";
			String replacementHours = "-";

			if (replacementEmployee != null) {
				replacementText = replacementEmployee.toString();
				replacementHours = FormatUtils.formatContractHours(replacementEmployee.getContractHours());
			}

			Object[] row = { replacement.getEmployeeToReplace(),
					FormatUtils.formatContractHours(employeeToReplace.getContractHours()), replacementText,
					replacementHours, replacement.getStartDate(), replacement.getStartTime(), replacement.getEndDate(),
					replacement.getEndTime() };

			tableModel.addRow(row);
		}
	}

	private void refreshTable() {

		tableModel.setRowCount(0);
		loadTable();
	}
}
