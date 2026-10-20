package gestion.ui.personnel;

import java.awt.BorderLayout;
import java.io.UncheckedIOException;
import gestion.ui.ActionDialogs;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

import gestion.controller.PersonnelController;
import gestion.model.Employee;
import gestion.model.EmployeeStatus;
import gestion.model.EmployeeSummary;
import gestion.ui.UIUtils;

public final class PersonnelPanel extends JPanel {
	private final PersonnelController controller;
	private final JTextField firstName = new JTextField();
	private final JTextField lastName = new JTextField();
	private final JTextField contractHours = new JTextField();
	private final PersonnelTableModel model;
	private final JTable table;
	private final HoursPeriodSelector assignedPeriod;
	private final HoursPeriodSelector remainingPeriod;

	public PersonnelPanel(PersonnelController controller) {
		super(new BorderLayout());
		this.controller = controller;
		model = new PersonnelTableModel(this::editEmployee);
		table = new JTable(model) {
			@Override
			protected void configureEnclosingScrollPane() {
			}
		};
		add(createInputPanel(), BorderLayout.NORTH);
		JPanel header = new JPanel(new GridLayout(1, 6));
		for (String title : new String[] { "Nom", "Prénom", "Heures prévues au contrat", "Statut" })
			header.add(new JLabel(title, JLabel.CENTER));
		assignedPeriod = new HoursPeriodSelector(true, header, this::refreshTable);
		remainingPeriod = new HoursPeriodSelector(false, header, this::refreshTable);
		header.add(assignedPeriod);
		header.add(remainingPeriod);
		header.setPreferredSize(new Dimension(0, header.getPreferredSize().height));
		configureTable();
		JScrollPane scroll = new JScrollPane(table);
		scroll.setColumnHeaderView(header);
		add(scroll, BorderLayout.CENTER);
		refreshTable();
	}

	private JPanel createInputPanel() {
		JPanel form = new JPanel(new GridLayout(4, 2));
		UIUtils.addPlaceholder(contractHours, "Ex : 35 - 24,5 - 24h30");
		form.add(new JLabel("Nom :"));
		form.add(lastName);
		form.add(new JLabel("Prénom :"));
		form.add(firstName);
		form.add(new JLabel("Heures hébdomadaires prévues au contrat :"));
		form.add(contractHours);
		JButton remove = new JButton("Supprimer un employé");
		JButton add = new JButton("Ajouter un employé");
		remove.addActionListener(event -> removeEmployee());
		add.addActionListener(event -> addEmployee());
		form.add(remove);
		form.add(add);
		return form;
	}

	private void configureTable() {
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (!SwingUtilities.isLeftMouseButton(event))
					return;
				int row = table.rowAtPoint(event.getPoint());
				int column = table.columnAtPoint(event.getPoint());
				if (row >= 0 && column >= 0 && table.convertColumnIndexToModel(column) == 3) {
					Employee employee = model.rowAt(table.convertRowIndexToModel(row)).employee();
					if (EmployeeStatusDialog.show(PersonnelPanel.this, controller, employee))
						refreshTable();
				}
			}
		});
		table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
					boolean focused, int row, int column) {
				super.getTableCellRendererComponent(table, value, selected, focused, row, column);
				setOpaque(true);
				setBackground(EmployeeStatus.OPERATIONAL.toString().equals(value) ? new Color(0x315C49)
						: new Color(0x6B3A42));
				setForeground(new Color(0xF1F4F6));
				return this;
			}
		});
		DefaultTableCellRenderer hoursRenderer = new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
					boolean focused, int row, int column) {
				super.getTableCellRendererComponent(table, value, selected, focused, row, column);
				EmployeeSummary values = model.rowAt(table.convertRowIndexToModel(row));
				setToolTipText(UIUtils.getHoursLegend());
				if (!selected)
					setForeground(
							UIUtils.getHoursColor(table.convertColumnIndexToModel(column) == 4 ? values.assignedAlert()
									: values.remainingAlert()));
				return this;
			}
		};
		table.getColumnModel().getColumn(4).setCellRenderer(hoursRenderer);
		table.getColumnModel().getColumn(5).setCellRenderer(hoursRenderer);
	}

	private void addEmployee() {
		try {
			controller.addEmployee(firstName.getText(), lastName.getText(), contractHours.getText());
			firstName.setText("");
			lastName.setText("");
			contractHours.setText("");
			refreshTable();
		} catch (IllegalArgumentException | UncheckedIOException exception) {
			ActionDialogs.error(this, exception);
		}
	}

	private void editEmployee(Employee employee, String firstName, String lastName, String contractHours) {
		try {
			controller.editEmployee(employee, firstName, lastName, contractHours);
		} catch (IllegalArgumentException | UncheckedIOException exception) {
			ActionDialogs.error(this, exception);
		} finally {
			refreshTable();
		}
	}

	private void removeEmployee() {
		int row = table.getSelectedRow();
		if (row < 0) {
			JOptionPane.showMessageDialog(this, "Veuillez sélectionner un employé.");
			return;
		}
        try {
            controller.removeEmployee(model.rowAt(table.convertRowIndexToModel(row)).employee());
            refreshTable();
        } catch (IllegalArgumentException | UncheckedIOException exception) { ActionDialogs.error(this, exception); }
	}

	public void refreshTable() {
		model.setRows(controller.getRows(assignedPeriod.getDate(), assignedPeriod.isMonthly(),
				remainingPeriod.getDate(), remainingPeriod.isMonthly()));
	}
}
