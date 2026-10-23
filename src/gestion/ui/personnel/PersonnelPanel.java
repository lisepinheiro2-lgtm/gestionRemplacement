package gestion.ui.personnel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.UncheckedIOException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import gestion.controller.PersonnelController;
import gestion.model.DateRange;
import gestion.model.Employee;
import gestion.service.PlanningService;
import gestion.service.RecurringInterventionService;
import gestion.ui.ActionDialogs;
import gestion.ui.AppThemeAware;
import gestion.ui.UIUtils;

public final class PersonnelPanel extends JPanel {
	private final PersonnelController controller;
	private final JTextField firstName = new JTextField();
	private final JTextField lastName = new JTextField();
	private final JTextField contractHours = new JTextField();
	private final PersonnelTableModel model;
	private final JTable table;
	private final HoursPeriodSelector selectedPeriod;
	private final PlanningService planning;
	private final RecurringInterventionService recurring;

	public PersonnelPanel(PersonnelController controller, PlanningService planning) {
		this(controller, planning, null);
	}

	public PersonnelPanel(PersonnelController controller, PlanningService planning,
			RecurringInterventionService recurring) {
		super(new BorderLayout());
		this.controller = controller;
		this.planning = planning;
		this.recurring = recurring;

		model = new PersonnelTableModel(this::editEmployee);
		table = new PersonnelTable(model);

		JPanel hoursHeader = new JPanel(new GridLayout(1, 3));

		selectedPeriod = new HoursPeriodSelector(hoursHeader, this::refreshTable);

		hoursHeader.add(createActionCell("Supprimer", this::removeEmployee));
		hoursHeader.add(createActionCell("Ajouter un employé", this::addEmployee));
		hoursHeader.add(selectedPeriod);

		JPanel header = new JPanel(new BorderLayout(1, 6));
		header.add(createInputPanel(), BorderLayout.NORTH);
		header.add(hoursHeader, BorderLayout.CENTER);
		JPanel titles = new JPanel(new GridLayout(1, 6));
		String[] titleText = { "Nom", "Prénom", "Contrat hebdo", "Statut", "Heures attribuées", "Heures restantes" };

		for (String text : titleText) {
			JLabel title = new JLabel(text, JLabel.CENTER);
			title.setFont(title.getFont().deriveFont(title.getFont().getSize2D() + 2f));
			title.setBorder(BorderFactory.createLineBorder(Color.GRAY));
			titles.add(title);
		}

		header.add(titles, BorderLayout.SOUTH);

		header.setPreferredSize(new Dimension(0, header.getPreferredSize().height));
		configureTable();

		JScrollPane scroll = new JScrollPane(table);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.setColumnHeaderView(header);
		add(scroll, BorderLayout.CENTER);
		refreshTable();
	}

	private JPanel createInputPanel() {

		JPanel form = new JPanel(new GridLayout(1, 3));
		form.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

		UIUtils.addPlaceholder(contractHours, "Ex : 35 - 24,5 - 24h30");

		form.add(createFieldBlock("Nom", lastName));
		form.add(createFieldBlock("Prénom", firstName));
		form.add(createFieldBlock("Heures hebdomadaires au contrat", contractHours));

		return form;
	}

	private JPanel createActionCell(String label, Runnable action) {

		JPanel cell = new JPanel(new GridBagLayout());

		JButton button = new PersonnelActionButton(label);
		UIUtils.styleRoundedButton(button);
		button.setMargin(new Insets(5, 24, 5, 24));

		FontMetrics metrics = button.getFontMetrics(button.getFont());
		Insets margins = button.getInsets();

		int width = metrics.stringWidth("Ajouter un employé") + margins.left + margins.right + 40;
		int height = button.getPreferredSize().height;

		button.setPreferredSize(new Dimension(width, height));

		button.addActionListener(event -> action.run());
		cell.add(button);

		return cell;
	}

	private JPanel createFieldBlock(String title, JTextField field) {

		JPanel block = new JPanel(new BorderLayout(0, 4));

		if (field != contractHours)
			block.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 7));

		field.putClientProperty("JComponent.roundRect", true);
		field.setPreferredSize(new Dimension(160, 30));

		block.add(new JLabel(title), BorderLayout.NORTH);
		block.add(field, BorderLayout.CENTER);

		return block;
	}

	private void configureTable() {

		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (!SwingUtilities.isLeftMouseButton(event) || event.getClickCount() != 2)
					return;

				int row = table.rowAtPoint(event.getPoint());
				int column = table.columnAtPoint(event.getPoint());

				if (row < 0 || column < 0)
					return;

				Employee employee = model.rowAt(table.convertRowIndexToModel(row)).employee();
				int result = table.convertColumnIndexToModel(column);

				if (result == 3) {
					if (EmployeeStatusDialog.show(PersonnelPanel.this, controller, employee))
						refreshTable();
				} else if (result == 4 || result == 5) {
					boolean attribuateHours = result == 4;
					DateRange period = selectedPeriod.getSelectedPeriod();
					PersonnelInterventionsDialog interventionsDialog = new PersonnelInterventionsDialog(planning, recurring,
							PersonnelPanel.this::refreshTable);

					interventionsDialog.open(employee, period, attribuateHours, PersonnelPanel.this);
				}
			}
		});

		table.setRowHeight(25);
		table.getColumnModel().getColumn(3).setCellRenderer(new PersonnelStatusRenderer(model));

		PersonnelHoursRenderer hoursRenderer = new PersonnelHoursRenderer(model);

		table.getColumnModel().getColumn(4).setCellRenderer(hoursRenderer);
		table.getColumnModel().getColumn(5).setCellRenderer(hoursRenderer);
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

		} catch (IllegalArgumentException | UncheckedIOException exception) {
			ActionDialogs.error(this, exception);
		}
	}

	public void refreshTable() {
		model.setRows(controller.getRows(selectedPeriod.getDate(), selectedPeriod.isMonthly()));
	}

	static final class PersonnelActionButton extends JButton implements AppThemeAware {

		PersonnelActionButton(String text) {
			super(text);
			putClientProperty("FlatLaf.style", "background:#1B2330;foreground:#EEF3FB;"
					+ "hoverBackground:#2D394A;pressedBackground:#15202C;"
					+ "[dark]borderColor:#3D4C61;[light]borderColor:#1B2330");
		}

		@Override
		public void applyAppTheme(boolean dark) {
			setOpaque(false);
			setForeground(new Color(0xEEF3FB));
		}
	}
}
