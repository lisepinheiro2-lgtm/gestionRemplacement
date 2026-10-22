package gestion.ui.personnel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import gestion.model.DateRange;
import gestion.model.Employee;
import gestion.model.Intervention;
import gestion.service.PlanningService;
import gestion.util.FormatUtils;

public class PersonnelInterventionsDialog {

	private final PlanningService planning;
	private final Runnable onChange;
	private JTable table1;
	private JTable table2;
	private JTable table3;
	private JDialog dialog;
	private List<Intervention> unassignedInterventions;
	private List<Intervention> assignedInterventions;
	private List<Intervention> otherAssignedInterventions;

	public PersonnelInterventionsDialog(PlanningService planning, Runnable onChange) {
		this.planning = planning;
		this.onChange = onChange;
	}

	private void fillTable(List<Intervention> interventions, boolean intervenantsColumn, DefaultTableModel model) {

		for (Intervention intervention : interventions) {

			String[] row;
			String clientFullName = intervention.getClient().getLastName() + " "
					+ intervention.getClient().getFirstName();
			String date = FormatUtils.formatDate(intervention.getStartDate());

			if (!intervention.getStartDate().equals(intervention.getEndDate())) {
				date = date + " - " + FormatUtils.formatDate(intervention.getEndDate());
			}

			String hours = intervention.getStartTime() + " - " + intervention.getEndTime();

			if (!intervenantsColumn) {
				row = new String[] { clientFullName, date, hours };
			} else {
				String employeeFullName = intervention.getEmployee().getLastName() + " "
						+ intervention.getEmployee().getFirstName();
				row = new String[] { clientFullName, employeeFullName, date, hours };
			}

			model.addRow(row);
		}
	}

	private JTable createInterventionsTable(List<Intervention> interventions, boolean intervenantsColumn) {

		DefaultTableModel interventionsTable = new DefaultTableModel() {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};

		interventionsTable.addColumn("Client");

		if (intervenantsColumn) {
			interventionsTable.addColumn("Intervenant");
		}

		interventionsTable.addColumn("Dates");
		interventionsTable.addColumn("Horaires");

		int dateColumnIndex = intervenantsColumn ? 2 : 1;
		fillTable(interventions, intervenantsColumn, interventionsTable);
		JTable table = new JTable(interventionsTable);
		DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
		centeredRenderer.setHorizontalAlignment(JLabel.CENTER);

		for (int i = 0; i < table.getColumnCount(); i++) {
			table.getColumnModel().getColumn(i).setCellRenderer(centeredRenderer);
		}

		table.getColumnModel().getColumn(dateColumnIndex).setPreferredWidth(240);

		return table;
	}

	private void refreshTables(Employee employee, DateRange period) {

		unassignedInterventions = planning.getUnassignedInterventionsForPeriod(period);
		assignedInterventions = planning.getAssignedInterventionsForPeriod(period, employee);
		otherAssignedInterventions = planning.getOtherAssignedInterventionsForPeriod(period, employee);

		DefaultTableModel model = (DefaultTableModel) table1.getModel();
		model.setRowCount(0);
		fillTable(unassignedInterventions, false, model);

		model = (DefaultTableModel) table2.getModel();
		model.setRowCount(0);
		fillTable(assignedInterventions, false, model);

		model = (DefaultTableModel) table3.getModel();
		model.setRowCount(0);
		fillTable(otherAssignedInterventions, true, model);

	}

	public void open(Employee employee, DateRange period, boolean attribuateHours, Component parent) {

		unassignedInterventions = planning.getUnassignedInterventionsForPeriod(period);
		table1 = createInterventionsTable(unassignedInterventions, false);

		JPanel freePanel = new JPanel(new BorderLayout());
		freePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JScrollPane freeScrollPane = new JScrollPane(table1);
		freePanel.add(freeScrollPane, BorderLayout.CENTER);

		JToggleButton freeButton = new JToggleButton("Libres");
		freeButton.putClientProperty("JButton.buttonType", "roundRect");
		freeButton.setFont(freeButton.getFont().deriveFont(18f));
		freeButton.setMargin(new Insets(10, 24, 10, 24));

		assignedInterventions = planning.getAssignedInterventionsForPeriod(period, employee);
		table2 = createInterventionsTable(assignedInterventions, false);

		JPanel attribuatePanel = new JPanel(new BorderLayout());
		attribuatePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JScrollPane attribuateScrollPane = new JScrollPane(table2);
		attribuatePanel.add(attribuateScrollPane, BorderLayout.CENTER);

		JToggleButton attribuateButton = new JToggleButton(
				"Attribuées à " + employee.getFirstName() + " " + employee.getLastName());
		attribuateButton.putClientProperty("JButton.buttonType", "roundRect");
		attribuateButton.setFont(freeButton.getFont().deriveFont(18f));
		attribuateButton.setMargin(new Insets(10, 24, 10, 24));

		otherAssignedInterventions = planning.getOtherAssignedInterventionsForPeriod(period, employee);
		table3 = createInterventionsTable(otherAssignedInterventions, true);

		JPanel otherEmployee = new JPanel(new BorderLayout());
		otherEmployee.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JScrollPane otherEmployeeScrollPane = new JScrollPane(table3);
		otherEmployee.add(otherEmployeeScrollPane, BorderLayout.CENTER);

		JToggleButton otherButton = new JToggleButton("Attribuées à d'autres Employés");
		otherButton.putClientProperty("JButton.buttonType", "roundRect");
		otherButton.setFont(freeButton.getFont().deriveFont(18f));
		otherButton.setMargin(new Insets(10, 24, 10, 24));

		ButtonGroup buttons = new ButtonGroup();
		buttons.add(freeButton);
		buttons.add(attribuateButton);
		buttons.add(otherButton);

		JPanel buttonsTitlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 15));
		buttonsTitlePanel.add(freeButton);
		buttonsTitlePanel.add(attribuateButton);
		buttonsTitlePanel.add(otherButton);

		CardLayout cardLayout = new CardLayout();
		JPanel cardsPanel = new JPanel(cardLayout);

		cardsPanel.add(freePanel, "FREE");
		cardsPanel.add(attribuatePanel, "ASSIGNED");
		cardsPanel.add(otherEmployee, "OTHERS");

		JDialog dialog = new JDialog();
		dialog.setTitle("Interventions : ");
		dialog.setModal(true);

		dialog.add(buttonsTitlePanel, BorderLayout.NORTH);
		dialog.add(cardsPanel, BorderLayout.CENTER);

		JPanel buttonPanel = new JPanel();
		JButton validate = new JButton(attribuateHours ? "Désattribuer" : "Attribuer");
		JButton cancel = new JButton("Annuler");

		buttonPanel.add(validate);
		buttonPanel.add(cancel);
		dialog.add(buttonPanel, BorderLayout.SOUTH);

		freeButton.addActionListener(e -> {
			cardLayout.show(cardsPanel, "FREE");
			validate.setText("Attribuer");
		});

		attribuateButton.addActionListener(e -> {
			cardLayout.show(cardsPanel, "ASSIGNED");
			validate.setText("Désattribuer");
		});

		otherButton.addActionListener(e -> {
			cardLayout.show(cardsPanel, "OTHERS");
			validate.setText("Réattribuer");
		});

		if (attribuateHours) {
			attribuateButton.setSelected(true);
			cardLayout.show(cardsPanel, "ASSIGNED");
		} else {
			freeButton.setSelected(true);
			cardLayout.show(cardsPanel, "FREE");
		}

		cancel.addActionListener(e -> dialog.dispose());
		validate.addActionListener(e -> {
			int selectedIndex;

			if (freeButton.isSelected()) {
				selectedIndex = 0;
			} else if (attribuateButton.isSelected()) {
				selectedIndex = 1;
			} else {
				selectedIndex = 2;
			}

			validate(selectedIndex, employee, period);
		});

		dialog.setPreferredSize(new Dimension(900, 600));
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		dialog.setVisible(true);
	}

	private List<Intervention> getSelectedInterventions(List<Intervention> interventions, JTable table) {

		int[] selectedIndexes = table.getSelectedRows();
		List<Intervention> selectedInterventions = new ArrayList<>();

		for (int selectedIndex : selectedIndexes) {
			selectedInterventions.add(interventions.get(table.convertRowIndexToModel(selectedIndex)));
		}

		return selectedInterventions;
	}

	private void validate(int index, Employee employee, DateRange period) {

		List<Intervention> selectedInterventions;
		boolean result = false;

		try {
			switch (index) {

			case 0:
				selectedInterventions = getSelectedInterventions(unassignedInterventions, table1);

				if (!selectedInterventions.isEmpty()) {
					planning.assignEmployee(selectedInterventions, employee);
					result = true;
				}

				break;

			case 1:
				selectedInterventions = getSelectedInterventions(assignedInterventions, table2);

				if (!selectedInterventions.isEmpty()) {
					planning.unassignEmployee(selectedInterventions);
					result = true;
				}

				break;

			case 2:
				selectedInterventions = getSelectedInterventions(otherAssignedInterventions, table3);

				if (!selectedInterventions.isEmpty()) {
					planning.assignEmployee(selectedInterventions, employee);
					result = true;
				}

				break;
			}

		} catch (IllegalArgumentException e) {
			JOptionPane.showMessageDialog(dialog, e.getMessage(), "Opération impossible", JOptionPane.ERROR_MESSAGE);
		} catch (UncheckedIOException e) {
			JOptionPane.showMessageDialog(dialog, "La sauvegarde a échoué. L'opération n'a pas pu être finalisée.",
					"Echec de l'opération", JOptionPane.ERROR_MESSAGE);
		}
		if (result) {
			onChange.run();
			refreshTables(employee, period);
		}
	}
}
