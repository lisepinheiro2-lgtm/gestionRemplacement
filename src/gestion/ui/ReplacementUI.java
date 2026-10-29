package gestion.ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import gestion.model.Employee;
import gestion.model.Intervention;
import gestion.model.Replacement;
import gestion.service.PersonnelService;
import gestion.service.PlanningService;
import gestion.service.ReplacementService;
import gestion.ui.ActionDialogs;
import gestion.util.FormatUtils;

public class ReplacementUI extends JPanel {

	private ReplacementService replacementList;
	private PersonnelService personnel;
	private PlanningService planning;

	private JComboBox<Employee> employeeToReplaceBox;
	private JComboBox<Employee> replacementEmployeeBox;

	private JCheckBox assignAllCheckBox;

	private JTextField startDateField;
	private JTextField startTimeField;
	private JTextField endTimeField;
	private JTextField endDateField;

	private DefaultTableModel tableModel;
	private JTable replacementTable;

	private final List<Integer> displayedReplacementIndexes = new ArrayList<>();
	private final List<Replacement> displayedReplacements = new ArrayList<>();
	private final List<String> replacementTooltips = new ArrayList<>();

	public ReplacementUI(ReplacementService replacementList, PersonnelService personnel, PlanningService planning) {

		this.replacementList = replacementList;
		this.personnel = personnel;
		this.planning = planning;

		replacementList.bindPersonnel(personnel);
		setLayout(new BorderLayout(0, 14));
		setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 18, 16, 18));

		createInputPanel();
		createTable();
		loadTable();
	}

	private void createInputPanel() {
		JPanel inputPanel = new JPanel(new BorderLayout(0, 12));
		inputPanel.putClientProperty("gestion.panelTitle", "Rechercher / ajouter un remplacement");
		JPanel fields = new JPanel(new GridLayout(3, 2, 18, 8));
		employeeToReplaceBox = new JComboBox<>();
		replacementEmployeeBox = new JComboBox<>();
		replacementEmployeeBox.addItem(null);
		replacementEmployeeBox.setRenderer(new javax.swing.DefaultListCellRenderer() {
			@Override
			public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
					int index, boolean selected, boolean focused) {
				super.getListCellRendererComponent(list, value, index, selected, focused);
				if (value == null)
					setText("Aucun remplaçant désigné");
				return this;
			}
		});
		startDateField = new JTextField();
		startTimeField = new JTextField();
		endDateField = new JTextField();
		endTimeField = new JTextField();
		UIUtils.addPlaceholder(startDateField, "jj/MM/aaaa");
		UIUtils.addPlaceholder(startTimeField, "__ h __");
		UIUtils.addPlaceholder(endDateField, "jj/MM/aaaa");
		UIUtils.addPlaceholder(endTimeField, "__ h __");
		for (Employee employee : personnel.getEmployees()) {
			employeeToReplaceBox.addItem(employee);
			replacementEmployeeBox.addItem(employee);
		}
		fields.add(fieldBlock("Employé à remplacer", employeeToReplaceBox));
		fields.add(fieldBlock("Employé remplaçant (facultatif)", replacementEmployeeBox));
		fields.add(fieldBlock("Date de début", startDateField));
		fields.add(fieldBlock("Date de fin", endDateField));
		fields.add(fieldBlock("Heure de début", startTimeField));
		fields.add(fieldBlock("Heure de fin", endTimeField));
		inputPanel.add(fields, BorderLayout.NORTH);
		assignAllCheckBox = new JCheckBox("Attribuer toutes les interventions au remplaçant");
		assignAllCheckBox.setEnabled(false);
		replacementEmployeeBox.setToolTipText("Pour appliquer ce remplaçant lors de l'ajout, cochez l'attribution de toutes les interventions.");
		replacementEmployeeBox.addActionListener(event -> {
			boolean available = replacementEmployeeBox.getSelectedItem() != null;
			assignAllCheckBox.setEnabled(available);
			if (!available)
				assignAllCheckBox.setSelected(false);
		});
		inputPanel.add(assignAllCheckBox, BorderLayout.CENTER);
		JPanel actions = new JPanel(new GridLayout(1, 3, 10, 0));
		JButton addButton = new ReplacementActionButton("Ajouter un remplacement", "primary");
		JButton removeButton = new ReplacementActionButton("Supprimer le remplacement", "danger");
		JButton assignGroup = new ReplacementActionButton("Attribuer les non attribuées", "primary");
		assignGroup.getAccessibleContext().setAccessibleName("Attribuer les interventions non attribuées de la demande");
		assignGroup.putClientProperty("gestion.icon", NavigationIcon.Kind.REPLACEMENTS);
		assignGroup.setToolTipText("Attribuer les interventions non attribuées de la demande liée à une absence : uniquement les interventions futures encore libres.");
		((ReplacementActionButton) assignGroup).applyAppTheme(UIUtils.isDarkMode());
		actions.add(removeButton);
		actions.add(addButton);
		actions.add(assignGroup);

		inputPanel.add(actions, BorderLayout.SOUTH);
		addButton.addActionListener(event -> addReplacement());
		removeButton.addActionListener(event -> removeReplacement());
		assignGroup.addActionListener(event -> assignSelectedGroup());
		add(inputPanel, BorderLayout.NORTH);
	}

	private static final class ReplacementActionButton extends JButton implements AppThemeAware {
		private final String tone;

		ReplacementActionButton(String text, String tone) {
			super(text);
			this.tone = tone;
			setToolTipText(text);
			applyAppTheme(UIUtils.isDarkMode());
		}

		@Override
		public java.awt.Dimension getPreferredSize() {
			return new java.awt.Dimension(super.getPreferredSize().width, 32);
		}

		@Override
		public void applyAppTheme(boolean dark) {
			UIUtils.styleActionButton(this, tone);
			setMargin(new java.awt.Insets(3, 8, 3, 8));
		}
	}

	private JPanel fieldBlock(String title, javax.swing.JComponent field) {
		JPanel panel = new JPanel(new BorderLayout(0, 4));
		field.putClientProperty("JComponent.roundRect", true);
		field.setPreferredSize(new java.awt.Dimension(160, 32));
		JLabel label = new JLabel(title);
		label.setLabelFor(field);
		label.setPreferredSize(new java.awt.Dimension(185, 32));
		panel.add(label, BorderLayout.WEST);
		panel.add(field, BorderLayout.CENTER);
		return panel;
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
		Employee replacementEmployee = assignAllCheckBox.isSelected()
				? (Employee) replacementEmployeeBox.getSelectedItem()
				: null;

		try {
			personnel.requireKnown(employeeToReplace);

			if (assignAllCheckBox.isSelected())
				personnel.requireKnown(replacementEmployee);

			if (start.isBefore(LocalDateTime.now()) && !ActionDialogs.confirm(this,
					"Cette demande commence dans le passé et peut modifier des attributions passées. Voulez-vous continuer ?"))
				return;

			replacementList.createWithPlanning(planning, employeeToReplace, replacementEmployee, startDate, startTime,
					endDate, endTime);

		} catch (UncheckedIOException | IllegalArgumentException e) {
			ActionDialogs.error(this, e);
			return;
		}

		refreshTable();

		startDateField.setText("");
		endDateField.setText("");
		startTimeField.setText("");
		endTimeField.setText("");
	}

	private void removeReplacement() {

		int index = getSelectedReplacementIndex();

		if (index < 0) {
			JOptionPane.showMessageDialog(this, "Veuillez sélectionner un remplacement.");
			return;
		}

		try {
			if (replacementList.getReplacements().get(index).getAbsenceId() == null
					&& !ActionDialogs.confirm(this, "Supprimer cette demande de remplacement ? Cette action ne rétablit pas les anciennes attributions."))
				return;
			replacementList.removeReplacement(index);
			refreshTable();

		} catch (UncheckedIOException | IllegalArgumentException e) {
			ActionDialogs.error(this, e);
		}
	}

	private void createTable() {
		String[] columns = { "Employé à remplacer", "Attribution future", "À attribuer (futur) / total",
				"Heures total / restantes", "Début", "Fin" };

		tableModel = new DefaultTableModel(columns, 0) {
			@Override
			public boolean isCellEditable(int row, int column) {
				return false;
			}
		};
		replacementTable = new JTable(tableModel) {
			@Override
			public String getToolTipText(java.awt.event.MouseEvent event) {
				int row = rowAtPoint(event.getPoint());
				if (row < 0)
					return null;
				return replacementTooltips.get(convertRowIndexToModel(row));
			}
		};
		DefaultTableCellRenderer centeredText = new DefaultTableCellRenderer();
		centeredText.setHorizontalAlignment(JLabel.CENTER);
		replacementTable.setDefaultRenderer(Object.class, centeredText);
		replacementTable.getColumnModel().getColumn(1).setCellRenderer(new BadgeRenderer() {
			@Override
			protected java.awt.Color colorFor(Object value) {
				return "Suivi individuel".equals(value) || "Sans occurrence future".equals(value)
						? UIUtils.getPlaceholderColor()
						: String.valueOf(value).startsWith("Attribué") ? UIUtils.getTurquoiseColor() : UIUtils.getVioletColor();
			}
		});
		int[] widths = { 195, 190, 200, 180, 170, 170 };
		for (int column = 0; column < widths.length; column++) {
			replacementTable.getColumnModel().getColumn(column).setPreferredWidth(widths[column]);
			replacementTable.getColumnModel().getColumn(column).setMinWidth(widths[column] - 15);
		}
		replacementTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		JScrollPane scrollPane = new JScrollPane(replacementTable);
		UIUtils.configureTableViewport(replacementTable, scrollPane);
		add(scrollPane, BorderLayout.CENTER);
	}

	private void loadTable() {
		int selectedRow = replacementTable.getSelectedRow();
		Replacement selected = selectedRow < 0 ? null
				: displayedReplacements.get(replacementTable.convertRowIndexToModel(selectedRow));
		List<Object[]> rows = new ArrayList<>();
		List<Replacement> previousRows = new ArrayList<>(displayedReplacements);
		displayedReplacementIndexes.clear();
		displayedReplacements.clear();
		replacementTooltips.clear();
		java.util.Map<String, List<Intervention>> groups = new java.util.HashMap<>();
		for (Intervention intervention : planning.getInterventions())
			if (intervention.getSourceAbsenceId() != null)
				groups.computeIfAbsent(intervention.getSourceAbsenceId(), ignored -> new ArrayList<>()).add(intervention);
		List<Replacement> allReplacements = replacementList.getReplacements();
		LocalDateTime now = LocalDateTime.now();
		for (int i = 0; i < allReplacements.size(); i++) {
			Replacement replacement = allReplacements.get(i);
			boolean linkedToAbsence = replacement.getAbsenceId() != null;
			List<Intervention> group = linkedToAbsence
					? groups.getOrDefault(replacement.getAbsenceId(), List.of()) : List.of();
			if (linkedToAbsence && group.isEmpty())
				continue;
			String status = "Suivi individuel";
			String count = "—";
			String hours = "—";

			if (linkedToAbsence) {
				List<Intervention> future = group.stream().filter(intervention ->
						!LocalDateTime.of(intervention.getStartDate(), intervention.getStartTime()).isBefore(now)).toList();
				List<Intervention> remaining = future.stream().filter(intervention -> intervention.getEmployee() == null).toList();
				long assigned = future.size() - remaining.size();
				status = future.isEmpty() ? "Sans occurrence future"
						: remaining.isEmpty() ? "Attribué (futur)"
						: assigned > 0 ? "Partiellement attribué" : "Non attribué";
				count = remaining.size() + " / " + group.size();
				double totalHours = group.stream().mapToDouble(Intervention::getDurationHours).sum();
				double remainingHours = remaining.stream().mapToDouble(Intervention::getDurationHours).sum();
				hours = FormatUtils.formatContractHours(totalHours) + " / " + FormatUtils.formatContractHours(remainingHours);

			}
			rows.add(new Object[] { replacement.getEmployeeToReplace().toString(), status, count, hours,
					formatBoundary(replacement, false), formatBoundary(replacement, true) });
			displayedReplacementIndexes.add(i);
			displayedReplacements.add(replacement);
			String employees = !linkedToAbsence
					? replacement.getReplacementEmployee() == null ? "Aucun remplaçant désigné"
							: replacement.getReplacementEmployee().toString()
					: group.stream().filter(intervention -> intervention.getEmployee() != null)
							.map(intervention -> intervention.getEmployee().toString()).distinct()
							.collect(java.util.stream.Collectors.joining(", "));
			replacementTooltips.add(!linkedToAbsence
					? "Suivi individuel — " + employees + ". Détail des interventions indisponible."
					: "Reste : futures non attribuées ; total : toutes les occurrences. Fin à 24h00 : fin de la journée indiquée. Intervenants : "
							+ (employees.isEmpty() ? "aucun" : employees));
		}

		boolean changed = UIUtils.updateTableRows(tableModel, rows);
		if (!changed && !previousRows.equals(displayedReplacements)) {
			tableModel.fireTableDataChanged();
			changed = true;
		}
		if (changed && selected != null) {
			int index = displayedReplacements.indexOf(selected);
			if (index >= 0) {
				int row = replacementTable.convertRowIndexToView(index);
				if (row >= 0)
					replacementTable.setRowSelectionInterval(row, row);
			}
		}
	}

	private String formatBoundary(Replacement replacement, boolean end) {
		LocalDate date = FormatUtils.parseDate(end ? replacement.getEndDate() : replacement.getStartDate());
		LocalTime time = FormatUtils.parseTime(end ? replacement.getEndTime() : replacement.getStartTime());
		if (end && replacement.getAbsenceId() != null && time.equals(LocalTime.MIDNIGHT))
			return FormatUtils.formatDate(date.minusDays(1)) + " 24h00";
		return FormatUtils.formatDate(date) + " " + FormatUtils.formatTime(time);
	}

	private void refreshEmployeeChoices() {
		List<Employee> employees = personnel.getEmployees();
		boolean same = employeeToReplaceBox.getItemCount() == employees.size()
				&& replacementEmployeeBox.getItemCount() == employees.size() + 1;
		for (int index = 0; same && index < employees.size(); index++)
			same = employeeToReplaceBox.getItemAt(index) == employees.get(index)
					&& replacementEmployeeBox.getItemAt(index + 1) == employees.get(index);
		if (same) {
			employeeToReplaceBox.repaint();
			replacementEmployeeBox.repaint();
			return;
		}

		boolean assignAll = assignAllCheckBox.isSelected();
		Employee original = (Employee) employeeToReplaceBox.getSelectedItem();
		Employee substitute = (Employee) replacementEmployeeBox.getSelectedItem();
		employeeToReplaceBox.setModel(new javax.swing.DefaultComboBoxModel<>(employees.toArray(Employee[]::new)));
		List<Employee> substitutes = new ArrayList<>();
		substitutes.add(null);
		substitutes.addAll(employees);
		replacementEmployeeBox.setModel(new javax.swing.DefaultComboBoxModel<>(substitutes.toArray(Employee[]::new)));

		if (personnel.getEmployees().contains(original))
			employeeToReplaceBox.setSelectedItem(original);
		if (personnel.getEmployees().contains(substitute))
			replacementEmployeeBox.setSelectedItem(substitute);
		assignAllCheckBox.setSelected(assignAll && replacementEmployeeBox.getSelectedItem() != null);
	}

	private void assignSelectedGroup() {

		try {

			int index = getSelectedReplacementIndex();

			if (index < 0) {
				throw new IllegalArgumentException("Sélectionnez une demande de remplacement.");
			}

			Employee employee = (Employee) replacementEmployeeBox.getSelectedItem();

			personnel.requireKnown(employee);
			replacementList.assignUnassignedGroup(planning, index, employee);

			refreshTable();

		} catch (UncheckedIOException | IllegalArgumentException e) {
			ActionDialogs.error(this, e);
		}
	}

	private int getSelectedReplacementIndex() {

		int selectedRow = replacementTable.getSelectedRow();

		if (selectedRow < 0) {
			return -1;
		}

		int modelRow = replacementTable.convertRowIndexToModel(selectedRow);

		return replacementList.getReplacements().indexOf(displayedReplacements.get(modelRow));
	}

	public void refreshTable() {
		refreshEmployeeChoices();
		loadTable();
	}
}
