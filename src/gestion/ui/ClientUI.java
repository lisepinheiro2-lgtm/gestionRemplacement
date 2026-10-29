package gestion.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.UncheckedIOException;
import gestion.ui.ActionDialogs;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import gestion.model.Client;
import gestion.service.ClientService;
import gestion.util.FormatUtils;

public class ClientUI extends JPanel {

	private ClientService clients;
	private JTextField lastNameField;
	private JTextField firstNameField;
	private JTextField addressField;
	private JTextField totalHoursField;
	private JTextField startDateField;
	private JTextField endDateField;
	private DefaultTableModel tableModel;
	private JTable clientTable;
	private final java.util.List<Client> displayedClients = new java.util.ArrayList<>();

	public ClientUI(ClientService clients) {

		this.clients = clients;

		setLayout(new BorderLayout(0, 14));
		setBorder(BorderFactory.createEmptyBorder(14, 18, 16, 18));

		createInputPanel();
		createTable();
		loadTable();
	}

	private void createInputPanel() {

		JPanel inputPanel = new JPanel(new BorderLayout(0, 12));
		inputPanel.putClientProperty("gestion.panelTitle", "Ajouter un client");
		JPanel fields = new JPanel(new GridLayout(2, 3, 0, 8));
		fields.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		lastNameField = new JTextField();
		firstNameField = new JTextField();
		addressField = new JTextField();
		totalHoursField = new JTextField();
		startDateField = new JTextField();
		endDateField = new JTextField();

		JButton addButton = new ClientActionButton("Ajouter le client");

		fields.add(createFieldBlock("Nom", lastNameField));
		fields.add(createFieldBlock("Prénom", firstNameField));
		fields.add(createFieldBlock("Adresse", addressField));
		fields.add(createFieldBlock("Heures au contrat", totalHoursField));
		fields.add(createFieldBlock("Début du contrat", startDateField));
		fields.add(createFieldBlock("Fin du contrat", endDateField));

		JPanel actions = new JPanel(new GridLayout(1, 3));
		actions.add(new JPanel());
		actions.add(createActionCell(addButton));
		actions.add(new JPanel());
		inputPanel.add(fields, BorderLayout.CENTER);
		inputPanel.add(actions, BorderLayout.SOUTH);

		UIUtils.addPlaceholder(totalHoursField, "Ex : 120 - 120,5 - 120h30");
		UIUtils.addPlaceholder(startDateField, "jj/MM/aaaa");
		UIUtils.addPlaceholder(endDateField, "jj/MM/aaaa");

		addButton.addActionListener(event -> addClient());


		add(inputPanel, BorderLayout.NORTH);
	}

	private JPanel createFieldBlock(String title, JTextField field) {

		JPanel block = new JPanel(new BorderLayout(0, 4));
		block.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 7));
		field.putClientProperty("JComponent.roundRect", true);
		field.setPreferredSize(new Dimension(160, 30));
		block.add(new JLabel(title), BorderLayout.NORTH);
		block.add(field, BorderLayout.CENTER);
		return block;
	}

	private JPanel createActionCell(JButton button) {

		JPanel cell = new JPanel(new GridBagLayout());
		UIUtils.styleRoundedButton(button);
		button.setMargin(new Insets(5, 24, 5, 24));
		FontMetrics metrics = button.getFontMetrics(button.getFont());
		Insets margins = button.getInsets();
		int width = metrics.stringWidth("Ajouter un employé") + margins.left + margins.right + 40;
		button.setPreferredSize(new Dimension(width, button.getPreferredSize().height));
		cell.add(button);
		return cell;
	}

	private void createTable() {

		String[] columns = { "Nom", "Prénom", "Adresse", "Heures contrat", "Début contrat", "Fin contrat" };

		tableModel = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		clientTable = new JTable(tableModel) {
			@Override
			public String getToolTipText(java.awt.event.MouseEvent event) {
				int row = rowAtPoint(event.getPoint());
				int column = columnAtPoint(event.getPoint());
				if (row < 0 || column < 0)
					return null;
				java.awt.Rectangle cell = getCellRect(row, column, true);
				if (convertColumnIndexToModel(column) == 5 && event.getX() >= cell.x + cell.width - 30)
					return "Supprimer ce client — confirmation et protections conservées";
				Object value = getValueAt(row, column);
				return value == null ? null : value.toString();
			}

			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component component = super.prepareRenderer(renderer, row, column);

				if (component instanceof JLabel label && convertColumnIndexToModel(column) != 5)
					label.setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 8));

				return component;
			}
		};
		clientTable.setRowHeight(30);
		clientTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
					boolean focused, int row, int column) {
				super.getTableCellRendererComponent(table, value, selected, focused, row, column);
				setHorizontalAlignment(JLabel.CENTER);
				setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 32));
				return this;
			}

			@Override
			protected void paintComponent(java.awt.Graphics graphics) {
				super.paintComponent(graphics);
				new NavigationIcon(NavigationIcon.Kind.DELETE, 16, UIUtils.getVioletColor())
						.paintIcon(this, graphics, getWidth() - 25, (getHeight() - 16) / 2);
			}
		});
		clientTable.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent event) {
				int row = clientTable.rowAtPoint(event.getPoint());
				int column = clientTable.columnAtPoint(event.getPoint());
				if (!javax.swing.SwingUtilities.isLeftMouseButton(event) || event.getClickCount() != 1
						|| row < 0 || column < 0 || clientTable.convertColumnIndexToModel(column) != 5)
					return;
				java.awt.Rectangle cell = clientTable.getCellRect(row, column, true);
				if (event.getX() >= cell.x + cell.width - 30) {
					clientTable.setRowSelectionInterval(row, row);
					removeClient();
				}
			}
		});
		clientTable.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("DELETE"), "removeClient");
		clientTable.getActionMap().put("removeClient", new javax.swing.AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent event) {
				removeClient();
			}
		});
		clientTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		int[] widths = { 130, 130, 310, 115, 140, 160 };
		for (int column = 0; column < widths.length; column++) {
			clientTable.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
			clientTable.getColumnModel().getColumn(column).setMinWidth(column == 2 ? 190 : column >= 4 ? 130 : 95);
		}
		clientTable.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				super.getTableCellRendererComponent(table, value, false, false, row, column);
				setHorizontalAlignment(JLabel.CENTER);
				setFont(table.getTableHeader().getFont().deriveFont(table.getTableHeader().getFont().getSize2D() + 2f));
				setBackground(table.getTableHeader().getBackground());
				setForeground(table.getTableHeader().getForeground());
				setBorder(BorderFactory.createLineBorder(UIUtils.getBorderColor()));
				return this;
			}
		});

		JScrollPane scrollPane = new JScrollPane(clientTable);
		UIUtils.configureTableViewport(clientTable, scrollPane);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.CENTER);
	}

	private void loadTable() {
		int selectedRow = clientTable.getSelectedRow();
		Client selected = selectedRow < 0 ? null : displayedClients.get(clientTable.convertRowIndexToModel(selectedRow));
		java.util.List<Object[]> rows = new java.util.ArrayList<>();
		java.util.List<Client> previousRows = new java.util.ArrayList<>(displayedClients);
		displayedClients.clear();

		for (Client client : clients.getClients()) {
			String startDate = "";
			String endDate = "";

			if (client.getContractStartDate() != null) {
				startDate = FormatUtils.formatDate(client.getContractStartDate());
			}
			if (client.getContractEndDate() != null) {
				endDate = FormatUtils.formatDate(client.getContractEndDate());
			}

			Object[] row = { client.getLastName(), client.getFirstName(), client.getAddress(),
					client.getTotalHoursContract(), startDate, endDate };

			rows.add(row);
			displayedClients.add(client);
		}

		boolean changed = UIUtils.updateTableRows(tableModel, rows);
		if (!changed && !previousRows.equals(displayedClients)) {
			tableModel.fireTableDataChanged();
			changed = true;
		}
		if (changed && selected != null) {
			int index = displayedClients.indexOf(selected);
			if (index >= 0) {
				int row = clientTable.convertRowIndexToView(index);
				if (row >= 0)
					clientTable.setRowSelectionInterval(row, row);
			}
		}
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

			double totalHoursContract = FormatUtils.parseContractHours(totalHoursText);
			LocalDate contractStartDate = null;
			LocalDate contractEndDate = null;

			if (!startDateText.isBlank() && !startDateText.equals("jj/MM/aaaa")) {
				contractStartDate = FormatUtils.parseDate(startDateText);
			}
			if (!endDateText.isBlank() && !endDateText.equals("jj/MM/aaaa")) {
				contractEndDate = FormatUtils.parseDate(endDateText);
			}

			clients.addClient(firstName, lastName, address, totalHoursContract, contractStartDate, contractEndDate);

			refreshTable();

			lastNameField.setText("");
			firstNameField.setText("");
			addressField.setText("");
			totalHoursField.setText("");
			startDateField.setText("");
			endDateField.setText("");

		} catch (UncheckedIOException e) {
			ActionDialogs.error(this, e);
		} catch (IllegalArgumentException e) {
			ActionDialogs.error(this, e);
		} catch (DateTimeParseException e) {
			JOptionPane.showMessageDialog(this, "Format invalide. Heures : 120 ou 120,5. Date : 05/03/2027.");
		}
	}

	private void removeClient() {
		int index = clientTable.getSelectedRow();

		if (index == -1) {
			JOptionPane.showMessageDialog(this, "Veuillez sélectionner un client.");
			return;
		}

		try {
			if (!ActionDialogs.confirm(this, "Supprimer le client sélectionné ? Les clients encore référencés restent protégés."))
				return;
			Client client = displayedClients.get(clientTable.convertRowIndexToModel(index));
			int position = clients.getClients().indexOf(client);
			if (position < 0)
				throw new IllegalArgumentException("Ce client n'existe plus.");
			clients.removeClient(position);
			refreshTable();
		} catch (UncheckedIOException | IllegalArgumentException e) {
			ActionDialogs.error(this, e);
		}
	}

	public void refreshTable() {
		loadTable();
	}

	private static final class ClientActionButton extends JButton implements AppThemeAware {

		ClientActionButton(String text) {
			super(text);
			putClientProperty("FlatLaf.style", "background:#1B2330;foreground:#EEF3FB;"
					+ "hoverBackground:#2D394A;pressedBackground:#15202C;"
					+ "[dark]borderColor:#3D4C61;[light]borderColor:#1B2330");
		}

		@Override
		public void applyAppTheme(boolean dark) {
			UIUtils.styleActionButton(this, getText().startsWith("Supprimer") ? "danger" : "primary");
		}
	}

}
