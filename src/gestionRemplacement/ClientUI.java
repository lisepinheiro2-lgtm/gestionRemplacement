package gestionRemplacement;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

public class ClientUI extends JPanel {

	private ClientList clients;
	private JTextField lastNameField;
	private JTextField firstNameField;
	private JTextField addressField;
	private JTextField totalHoursField;
	private JTextField startDateField;
	private JTextField endDateField;
	private DefaultTableModel tableModel;
	private JTable clientTable;

	public ClientUI(ClientList clients) {

		this.clients = clients;

		setLayout(new BorderLayout());

		createInputPanel();
		createTable();
		loadTable();
	}

	private void createInputPanel() {

		JPanel inputPanel = new JPanel(new GridLayout(7, 2));
		lastNameField = new JTextField();
		firstNameField = new JTextField();
		addressField = new JTextField();
		totalHoursField = new JTextField();
		startDateField = new JTextField();
		endDateField = new JTextField();
		JButton removeButton = new JButton("Supprimer le client");
		JButton addButton = new JButton("Ajouter le client");

		inputPanel.add(new JLabel("Nom :"));
		inputPanel.add(lastNameField);
		inputPanel.add(new JLabel("Prénom :"));
		inputPanel.add(firstNameField);
		inputPanel.add(new JLabel("Adresse :"));
		inputPanel.add(addressField);
		inputPanel.add(new JLabel("Heures prévues au contrat :"));
		inputPanel.add(totalHoursField);
		inputPanel.add(new JLabel("Début du contrat :"));
		inputPanel.add(startDateField);
		inputPanel.add(new JLabel("Fin du contrat :"));
		inputPanel.add(endDateField);
		inputPanel.add(removeButton);
		inputPanel.add(addButton);

		addPlaceholder(totalHoursField, "Ex : 120 - 120,5 - 120h30");
		addPlaceholder(startDateField, "jj/MM/aaaa");
		addPlaceholder(endDateField, "jj/MM/aaaa");

		addButton.addActionListener(event -> addClient());
		removeButton.addActionListener(event -> removeClient());

		add(inputPanel, BorderLayout.NORTH);
	}

	private void createTable() {

		String[] columns = { "Nom", "Prénom", "Adresse", "Heures contrat", "Début contrat", "Fin contrat" };

		tableModel = new DefaultTableModel(columns, 0);
		clientTable = new JTable(tableModel);

		JScrollPane scrollPane = new JScrollPane(clientTable);
		add(scrollPane, BorderLayout.CENTER);
	}

	private void loadTable() {

		for (Client client : clients.getClients()) {
			String startDate = "";
			String endDate = "";

			if (client.getContractStartDate() != null) {
				startDate = client.getContractStartDate().toString();
			}
			if (client.getContractEndDate() != null) {
				endDate = client.getContractEndDate().toString();
			}

			Object[] row = { client.getLastName(), client.getFirstName(), client.getAddress(),
					client.getTotalHoursContract(), startDate, endDate };

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

	private void addClient() {

		String lastName = lastNameField.getText();
		String firstName = firstNameField.getText();
		String address = addressField.getText();
		String totalHoursText = totalHoursField.getText();
		String startDateText = startDateField.getText();
		String endDateText = endDateField.getText();

		if (lastName.isBlank() || firstName.isBlank() || address.isBlank() || totalHoursText.isBlank()) {

			JOptionPane.showMessageDialog(this,
					"Veuillez renseigner le nom, le prénom, l'adresse et les heures du contrat.");
			return;
		}

		try {

			double totalHoursContract = parseContractHours(totalHoursText);
			LocalDate contractStartDate = null;
			LocalDate contractEndDate = null;

			if (!startDateText.isBlank() && !startDateText.equals("jj/MM/aaaa")) {
				contractStartDate = parseDate(startDateText);
			}
			if (!endDateText.isBlank() && !endDateText.equals("jj/MM/aaaa")) {
				contractEndDate = parseDate(endDateText);
			}

			clients.addClient(firstName, lastName, address, totalHoursContract, contractStartDate, contractEndDate);

			refreshTable();

			lastNameField.setText("");
			firstNameField.setText("");
			addressField.setText("");
			totalHoursField.setText("");
			startDateField.setText("");
			endDateField.setText("");

		} catch (NumberFormatException | DateTimeParseException e) {
			JOptionPane.showMessageDialog(this, "Format invalide. Heures : 120 ou 120,5. Date : 05/03/2027.");
		}
	}

	private void removeClient() {

		int index = clientTable.getSelectedRow();

		if (index == -1) {
			JOptionPane.showMessageDialog(this, "Veuillez sélectionner un client.");
			return;
		}

		clients.removeClient(index);
		refreshTable();
	}

	private void refreshTable() {
		tableModel.setRowCount(0);
		loadTable();
	}

	private LocalDate parseDate(String text) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

		return LocalDate.parse(text.trim(), formatter);
	}
}
