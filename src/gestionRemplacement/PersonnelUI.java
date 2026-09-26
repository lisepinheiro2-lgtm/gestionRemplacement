package gestionRemplacement;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;

public class PersonnelUI {

	private PersonnelList personnel;
	private JFrame frame;
	private JTextField firstNameField;
	private JTextField lastNameField;
	private JTextField contractHoursField;
	private DefaultTableModel tableModel;
	private JTable employeeTable;
	private boolean updatingTable = false;

	public PersonnelUI(PersonnelList personnel) {

		this.personnel = personnel;
		createWindow();
		loadTable();
	}

	private void createWindow() {

		frame = new JFrame("Personnel Management");
		frame.setSize(1000, 800);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new java.awt.event.WindowAdapter() {

			public void windowClosing(java.awt.event.WindowEvent e) {

				if (employeeTable.isEditing()) {
					employeeTable.getCellEditor().stopCellEditing();
				}

				frame.dispose();
				System.exit(0);
			}
		});

		frame.setLayout(new BorderLayout());
		createInputPanel();
		createTable();
		frame.setVisible(true);
	}

	private void createInputPanel() {

		JPanel inputPanel = new JPanel();
		inputPanel.setLayout(new GridLayout(4, 2));

		JLabel firstNameLabel = new JLabel("Prénom :");
		JLabel lastNameLabel = new JLabel("Nom :");
		JLabel contractHoursLabel = new JLabel("Heures prévues au contrat :");
		firstNameField = new JTextField();
		lastNameField = new JTextField();
		contractHoursField = new JTextField();

		JButton addButton = new JButton("Ajouter un employé");
		JButton delButton = new JButton("Supprimer un employé");

		inputPanel.add(firstNameLabel);
		inputPanel.add(firstNameField);
		inputPanel.add(lastNameLabel);
		inputPanel.add(lastNameField);
		inputPanel.add(contractHoursLabel);
		inputPanel.add(contractHoursField);
		inputPanel.add(delButton);
		inputPanel.add(addButton);

		frame.add(inputPanel, BorderLayout.NORTH);

		addButton.addActionListener(event -> addEmployee());
		delButton.addActionListener(event -> removeEmployee());
	}

	private void createTable() {

		String[] columns = { "Prénom", "Nom", "Heures prévues au contrat" };
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

			String firstName = tableModel.getValueAt(row, 0).toString();
			String lastName = tableModel.getValueAt(row, 1).toString();
			String contractHoursText = tableModel.getValueAt(row, 2).toString();

			if (firstName.isBlank() || lastName.isBlank() || contractHoursText.isBlank()) {

				Employee employee = personnel.getEmployees().get(row);
				updatingTable = true;

				tableModel.setValueAt(employee.getFirstName(), row, 0);
				tableModel.setValueAt(employee.getLastName(), row, 1);
				tableModel.setValueAt(employee.getContractHours(), row, 2);
				updatingTable = false;

				JOptionPane.showMessageDialog(frame,
						"Veuillez renseigner le prénom, le nom et les heures prévus au contrat");
				return;
			}

			try {

				double contractHours = Double.parseDouble(contractHoursText);
				personnel.editEmployee(row, firstName, lastName, contractHours);

			} catch (NumberFormatException e) {
				Employee employee = personnel.getEmployees().get(row);

				updatingTable = true;
				tableModel.setValueAt(employee.getContractHours(), row, 2);
				updatingTable = false;

				JOptionPane.showMessageDialog(frame, "Les heures prévues au contrat doivent être un nombre.");
			}
		});

		employeeTable = new JTable(tableModel);
		JScrollPane scrollPane = new JScrollPane(employeeTable);
		frame.add(scrollPane, BorderLayout.CENTER);
	}

	private void addEmployee() {

		String firstName = firstNameField.getText();
		String lastName = lastNameField.getText();
		String contractHoursText = contractHoursField.getText();
		if (firstName.isBlank() || lastName.isBlank() || contractHoursText.isBlank()) {
			JOptionPane.showMessageDialog(frame,
					"Veuillez remplir les cases prénom, nom et heures prévues au contrat, sinon l'employé ne sera pas sauvegardé.");
			return;
		}

		try {

			double contractHours = Double.parseDouble(contractHoursText);

			personnel.addEmployee(firstName, lastName, contractHours);

			Object[] employee = { firstName, lastName, contractHours };
			tableModel.addRow(employee);

			firstNameField.setText("");
			lastNameField.setText("");
			contractHoursField.setText("");

		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(frame, "Les heures prévues au contrat doivent être un nombre.");
		}
	}

	private void removeEmployee() {

		int index = employeeTable.getSelectedRow();

		if (index == -1) {
			JOptionPane.showMessageDialog(frame, "Veuillez sélectionner un employé.");
			return;
		}

		personnel.removeEmployee(index);
		tableModel.removeRow(index);
	}

	private void loadTable() {

		for (Employee employee : personnel.getEmployees()) {

			Object[] row = { employee.getFirstName(), employee.getLastName(), employee.getContractHours() };

			tableModel.addRow(row);
		}
	}
}
