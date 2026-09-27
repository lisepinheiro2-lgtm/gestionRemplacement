package gestionRemplacement;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;

public class PersonnelUI extends JPanel {

	private PersonnelList personnel;
	private JTextField lastNameField;
	private JTextField firstNameField;
	private JTextField contractHoursField;
	private DefaultTableModel tableModel;
	private JTable employeeTable;
	private boolean updatingTable = false;

	public PersonnelUI(PersonnelList personnel) {

		this.personnel = personnel;

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
		JLabel contractHoursLabel = new JLabel("Heures prévues au contrat :");
		firstNameField = new JTextField();
		lastNameField = new JTextField();
		contractHoursField = new JTextField();

		addPlaceholder(contractHoursField, "Ex : 35 - 24,5 - 24h30");

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

		String[] columns = { "Nom", "Prénom", "Heures prévues au contrat" };
		tableModel = new DefaultTableModel(columns, 0);
		tableModel.addTableModelListener(event -> {

			if (updatingTable) {
				return;
			}

			int row = event.getFirstRow();
			int column = event.getColumn();

			if (row < 0 || column < 0) {
				return;
			}

			String lastName = tableModel.getValueAt(row, 0).toString();
			String firstName = tableModel.getValueAt(row, 1).toString();
			String contractHoursText = tableModel.getValueAt(row, 2).toString();

			if (lastName.isBlank() || firstName.isBlank() || contractHoursText.isBlank()) {

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

				double contractHours = parseContractHours(contractHoursText);
				personnel.editEmployee(row, firstName, lastName, contractHours);

			} catch (NumberFormatException e) {
				Employee employee = personnel.getEmployees().get(row);

				updatingTable = true;
				tableModel.setValueAt(formatContractHours(employee.getContractHours()), row, 2);
				updatingTable = false;

				JOptionPane.showMessageDialog(this, "Les heures prévues au contrat doivent être un nombre.");
			}
		});

		employeeTable = new JTable(tableModel);
		JScrollPane scrollPane = new JScrollPane(employeeTable);
		add(new JScrollPane(employeeTable), BorderLayout.CENTER);
	}

	private void addEmployee() {

		String lastName = lastNameField.getText();
		String firstName = firstNameField.getText();
		String contractHoursText = contractHoursField.getText();

		if (lastName.isBlank() || firstName.isBlank() || contractHoursText.isBlank()) {
			JOptionPane.showMessageDialog(this,
					"Veuillez remplir les cases nom, prénom et heures prévues au contrat, sinon l'employé ne sera pas sauvegardé.");
			return;
		}

		try {

			double contractHours = parseContractHours(contractHoursText);

			personnel.addEmployee(firstName, lastName, contractHours);
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

	private void loadTable() {

		for (Employee employee : personnel.getEmployees()) {

			Object[] row = { employee.getLastName(), employee.getFirstName(),
					formatContractHours(employee.getContractHours()) };

			tableModel.addRow(row);
		}
	}

	private void addPlaceholder(JTextField field, String placeholder) {

		field.setText(placeholder);
		field.setForeground(Color.GRAY);

		field.addFocusListener(new FocusAdapter() {

			public void focusGained(FocusEvent e) {

				if (field.getText().equals(placeholder)) {
					field.setText("");
					field.setForeground(Color.BLACK);
				}
			}

			public void focusLost(FocusEvent e) {

				if (field.getText().isBlank()) {
					field.setText(placeholder);
					field.setForeground(Color.GRAY);
				}
			}
		});
	}

	private double parseContractHours(String text) {

		text = text.trim().toLowerCase().replace(",", ".");

		if (text.contains("h")) {

			String[] parts = text.split("h");
			int hours = Integer.parseInt(parts[0]);
			int minutes = Integer.parseInt(parts[1]);

			if (minutes < 0 || minutes > 59) {
				throw new NumberFormatException();
			}

			return hours + minutes / 60.0;
		}

		return Double.parseDouble(text);
	}

	private String formatContractHours(double hours) {

		int totalMinutes = (int) Math.round(hours * 60);

		int hour = totalMinutes / 60;
		int minutes = totalMinutes % 60;

		return String.format("%dh%02d", hour, minutes);
	}

	private void refreshTable() {

		updatingTable = true;

		tableModel.setRowCount(0);
		loadTable();

		updatingTable = false;
	}
}
