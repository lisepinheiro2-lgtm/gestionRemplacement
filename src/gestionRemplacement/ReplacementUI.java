package gestionRemplacement;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class ReplacementUI {

	private ReplacementList replacementList;
	private PersonnelList personnel;
	private Planning planning;

	private JFrame frame;

	private JComboBox<Employee> employeeToReplaceBox;
	private JComboBox<Employee> replacementEmployeeBox;

	private JTextField dateField;
	private JTextField startTimeField;
	private JTextField endTimeField;

	private DefaultTableModel tableModel;
	private JTable replacementTable;

	public ReplacementUI(ReplacementList replacementList, PersonnelList personnel, Planning planning) {

		this.replacementList = replacementList;
		this.personnel = personnel;
		this.planning = planning;

		createWindow();
		loadTable();
	}

	private void createWindow() {

		frame = new JFrame("Remplacements");
		frame.setSize(1000, 800);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		createInputPanel();
		createTable();

		frame.setVisible(true);
	}

	private void createInputPanel() {

		JPanel inputPanel = new JPanel(new GridLayout(6, 2));

		employeeToReplaceBox = new JComboBox<>();
		replacementEmployeeBox = new JComboBox<>();

		dateField = new JTextField();
		startTimeField = new JTextField();
		endTimeField = new JTextField();
		JButton addButton = new JButton("Ajouter un remplacement");

		for (Employee employee : personnel.getEmployees()) {

			employeeToReplaceBox.addItem(employee);
			replacementEmployeeBox.addItem(employee);
		}

		inputPanel.add(new JLabel("Employé à remplacer :"));
		inputPanel.add(employeeToReplaceBox);

		inputPanel.add(new JLabel("Employé remplaçant :"));
		inputPanel.add(replacementEmployeeBox);

		inputPanel.add(new JLabel("Date :"));
		inputPanel.add(dateField);

		inputPanel.add(new JLabel("Heure de début :"));
		inputPanel.add(startTimeField);

		inputPanel.add(new JLabel("Heure de fin :"));
		inputPanel.add(endTimeField);

		inputPanel.add(new JLabel(""));
		inputPanel.add(addButton);

		addButton.addActionListener(event -> addReplacement());

		frame.add(inputPanel, BorderLayout.NORTH);
	}

	private void addReplacement() {

		String date = dateField.getText();
		String startTime = startTimeField.getText();
		String endTime = endTimeField.getText();

		if (date.isBlank() || startTime.isBlank() || endTime.isBlank()) {
			JOptionPane.showMessageDialog(frame,
					("Veuillez renseigner la date, l'heure de début et l'heure de fin, sinon le remplacement ne sera pas sauvegardé."));
			return;
		}

		Employee employeeToReplace = (Employee) employeeToReplaceBox.getSelectedItem();
		Employee replacementEmployee = (Employee) replacementEmployeeBox.getSelectedItem();

		double contractHoursEmployeeToReplace = employeeToReplace.getContractHours();
		double contractHoursReplacementEmployee = replacementEmployee.getContractHours();

		if (employeeToReplace == replacementEmployee) {
			JOptionPane.showMessageDialog(frame, "L'employé à remplacer et le remplaçant doivent être différents.");
			return;
		}

		replacementList.addReplacement(employeeToReplace, contractHoursEmployeeToReplace, replacementEmployee,
				contractHoursReplacementEmployee, date, startTime, endTime);

		Object[] row = { employeeToReplace, contractHoursEmployeeToReplace, replacementEmployee,
				contractHoursReplacementEmployee, date, startTime, endTime };

		tableModel.addRow(row);

		dateField.setText("");
		startTimeField.setText("");
		endTimeField.setText("");
	}

	private Employee findEmployee(Employee employeeToFind) {

		for (Employee employee : personnel.getEmployees()) {

			if (employee.getFirstName().equals(employeeToFind.getFirstName())
					&& employee.getLastName().equals(employeeToFind.getLastName())) {

				return employee;
			}
		}

		return null;
	}

	private void createTable() {

		String[] columns = { "Employé à remplacer", "Heures contrat", "Remplaçant", "Heures contrat", "Date", "Début",
				"Fin" };

		tableModel = new DefaultTableModel(columns, 0);
		replacementTable = new JTable(tableModel);

		JScrollPane scrollPane = new JScrollPane(replacementTable);
		frame.add(scrollPane, BorderLayout.CENTER);
	}

	private void loadTable() {

		for (Replacement replacement : replacementList.getReplacements()) {

			Employee employeeToReplace = findEmployee(replacement.getEmployeeToReplace());

			Employee replacementEmployee = findEmployee(replacement.getReplacementEmployee());

			Object[] row = { replacement.getEmployeeToReplace(), employeeToReplace.getContractHours(),
					replacement.getReplacementEmployee(), replacementEmployee.getContractHours(), replacement.getDate(),
					replacement.getStartTime(), replacement.getEndTime() };

			tableModel.addRow(row);
		}
	}
}
