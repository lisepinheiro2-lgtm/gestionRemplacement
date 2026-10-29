package gestion.ui.personnel;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.KeyEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import gestion.model.Intervention;
import gestion.service.InterventionAssignmentService;
import gestion.service.PlanningService;
import gestion.service.WorkHoursService;
import gestion.ui.UIUtils;
import gestion.util.DomainValidation;
import gestion.util.FormatUtils;
import gestion.util.PeriodUtils;

final class PersonnelInterventionsTable extends JTable {

	private final List<Intervention> interventions = new ArrayList<>();
	private final PlanningService planning;
	private final boolean showEmployee;
	private final int dateColumn;
	private final boolean preventOverlaps;
	private final Map<Intervention, Intervention> blockers = new HashMap<>();
	private final Map<String, String> recurrenceTooltips = new HashMap<>();

	PersonnelInterventionsTable(PlanningService planning, boolean showEmployee) {
		this(planning, showEmployee, true);
	}

	PersonnelInterventionsTable(PlanningService planning, boolean showEmployee, boolean preventOverlaps) {
		this.planning = planning;
		this.showEmployee = showEmployee;
		this.preventOverlaps = preventOverlaps;
		dateColumn = showEmployee ? 4 : 3;

		setModel(new AbstractTableModel() {
			private final String[] columns = showEmployee
					? new String[] { "Sélection", "", "Client", "Intervenant", "Date(s)", "Horaires", "Durée" }
					: new String[] { "Sélection", "", "Client", "Date(s)", "Horaires", "Durée" };

			@Override
			public int getRowCount() {
				return interventions.size();
			}

			@Override
			public int getColumnCount() {
				return columns.length;
			}

			@Override
			public String getColumnName(int column) {
				return columns[column];
			}

			@Override
			public Class<?> getColumnClass(int column) {
				if (column == 0)
					return Boolean.class;
				if (column == dateColumn)
					return LocalDateTime.class;
				if (column == dateColumn + 2)
					return Double.class;
				return String.class;
			}

			@Override
			public Object getValueAt(int row, int column) {
				Intervention intervention = interventions.get(row);

				if (column == 0)
					return isRowSelected(convertRowIndexToView(row));
				if (column == 1)
					return intervention.getRecurringId() == null ? "" : "Récurrent";
				if (column == 2)
					return intervention.getClient().getFirstName() + " " + intervention.getClient().getLastName();
				if (showEmployee && column == 3)
					return intervention.getEmployee() == null ? "" : intervention.getEmployee().toString();
				if (column == dateColumn)
					return LocalDateTime.of(intervention.getStartDate(), intervention.getStartTime());
				if (column == dateColumn + 1)
					return FormatUtils.formatTime(intervention.getStartTime()) + " – "
							+ FormatUtils.formatTime(intervention.getEndTime());
				return intervention.getDurationHours();
			}
		});

		getColumnModel().getColumn(0).setHeaderRenderer(new javax.swing.table.DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
					boolean focused, int row, int column) {
				super.getTableCellRendererComponent(table, "", selected, focused, row, column);
				setHorizontalAlignment(CENTER);
				setBackground(table.getTableHeader().getBackground());
				setIcon(new gestion.ui.NavigationIcon(gestion.ui.NavigationIcon.Kind.CHECK, 16, UIUtils.getTextColor()));
				setToolTipText("Sélection des interventions");
				return this;
			}
		});
		getColumnModel().getColumn(1).setCellRenderer(new javax.swing.table.DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
					boolean focused, int row, int column) {
				super.getTableCellRendererComponent(table, "", selected, focused, row, column);
				setHorizontalAlignment(CENTER);
				setIcon(value == null || value.toString().isEmpty() ? null
						: new gestion.ui.NavigationIcon(gestion.ui.NavigationIcon.Kind.RECURRING, 16, UIUtils.getTurquoiseColor()));
				return this;
			}
		});

		TableRowSorter<?> sorter = new TableRowSorter<>(getModel());
		sorter.setSortable(0, false);
		sorter.setSortable(1, false);
		sorter.setSortKeys(List.of(new RowSorter.SortKey(dateColumn, SortOrder.ASCENDING)));
		setRowSorter(sorter);

		if (preventOverlaps)
			setSelectionModel(new CompatibleSelectionModel());

		setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		setRowHeight(28);
		setFillsViewportHeight(true);
		setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		getTableHeader().setReorderingAllowed(false);
		getTableHeader().setResizingAllowed(false);
		getSelectionModel().addListSelectionListener(event -> {
			if (!event.getValueIsAdjusting())
				updateBlockedRows();
		});

		for (int column = 0; column < getColumnCount(); column++) {
			if (column < 2) {
				getColumnModel().getColumn(column).setMinWidth(32);
				getColumnModel().getColumn(column).setMaxWidth(38);
			} else {
				getColumnModel().getColumn(column).setPreferredWidth(column == dateColumn ? 200 : 140);
			}
		}
	}

	@Override
	public void changeSelection(int row, int column, boolean toggle, boolean extend) {
		if (row < 0 || row >= getRowCount())
			return;

		Intervention requested = interventions.get(convertRowIndexToModel(row));
		hideStartedInterventions();
		int modelRow = interventions.indexOf(requested);

		if (modelRow < 0)
			return;

		int viewRow = convertRowIndexToView(modelRow);
		List<Intervention> selected = selectedInterventions();

		if (selectionBlocker(requested, selected) != null && EventQueue.getCurrentEvent() instanceof KeyEvent) {
			int direction = Integer.compare(viewRow, getSelectionModel().getLeadSelectionIndex());

			if (direction == 0)
				return;

			do {
				viewRow += direction;

				if (viewRow < 0 || viewRow >= getRowCount())
					return;

				requested = interventions.get(convertRowIndexToModel(viewRow));
			} while (selectionBlocker(requested, selected) != null);
		}

		if (InterventionAssignmentService.isModifiable(requested, LocalDateTime.now())
				&& selectionBlocker(requested, selected) == null)
			super.changeSelection(viewRow, column, column == 0 || toggle, extend);
	}

	@Override
	public void selectAll() {
		hideStartedInterventions();
		super.selectAll();
	}

	@Override
	public Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row, int column) {
		Component component = super.prepareRenderer(renderer, row, column);
		Intervention intervention = interventions.get(convertRowIndexToModel(row));
		Intervention blocker = blockers.get(intervention);
		component.setEnabled(blocker == null);
		component.setForeground(blocker != null ? UIUtils.getPlaceholderColor()
				: isRowSelected(row) ? getSelectionForeground() : getForeground());
		component.setBackground(blocker != null ? new Color(UIUtils.isDarkMode() ? 0x26303D : 0xE8EDF3)
				: isRowSelected(row) ? getSelectionBackground()
				: row % 2 == 0 ? getBackground() : new Color(UIUtils.isDarkMode() ? 0x1D2735 : 0xEDF3FA));

		if (component instanceof JComponent target)
			target.setToolTipText(null);

		if (component instanceof JLabel label) {
			label.setHorizontalAlignment(JLabel.CENTER);
			label.setToolTipText(null);

			if (column == dateColumn)
				label.setText(FormatUtils.formatDate(intervention.getStartDate())
						+ (intervention.getStartDate().equals(intervention.getEndDate()) ? ""
								: " – " + FormatUtils.formatDate(intervention.getEndDate())));

			if (column == dateColumn + 2)
				label.setText(FormatUtils.formatContractHours(intervention.getDurationHours()));

		}

		return component;
	}

	@Override
	public String getToolTipText(java.awt.event.MouseEvent event) {
		int row = rowAtPoint(event.getPoint());
		int column = columnAtPoint(event.getPoint());
		if (row < 0 || column < 0)
			return null;
		Intervention intervention = interventions.get(convertRowIndexToModel(row));
		Intervention blocker = blockers.get(intervention);
		if (blocker != null)
			return "Chevauche l'intervention sélectionnée : " + describe(blocker);

		int modelColumn = convertColumnIndexToModel(column);
		if (intervention.getRecurringId() == null || modelColumn != 1 && modelColumn != dateColumn + 2)
			return null;
		LocalDate monday = intervention.getStartDate().with(DayOfWeek.MONDAY);
		String key = intervention.getRecurringId() + ":" + monday;
		return recurrenceTooltips.computeIfAbsent(key, ignored -> {
			double weekly = planning.getInterventionsByRecurringId(intervention.getRecurringId()).stream()
					.mapToDouble(i -> WorkHoursService.getInterventionHoursInPeriod(i, monday.atStartOfDay(),
							monday.plusWeeks(1).atStartOfDay())).sum();
			return "Récurrence : " + FormatUtils.formatContractHours(weekly)
					+ " dans la semaine du " + FormatUtils.formatDate(monday)
					+ ". Durée affichée : cette occurrence uniquement.";
		});
	}

	private Intervention selectionBlocker(Intervention candidate, List<Intervention> selected) {
		if (!preventOverlaps)
			return null;

		for (Intervention other : selected) {
			if (other != candidate && PeriodUtils.hasDateTimeOverlap(DomainValidation.intervention(candidate),
					DomainValidation.intervention(other)))
				return other;
		}

		return null;
	}

	private void updateBlockedRows() {
		blockers.clear();
		List<Intervention> selected = selectedInterventions();

		for (Intervention intervention : interventions) {
			Intervention blocker = selectionBlocker(intervention, selected);

			if (blocker != null)
				blockers.put(intervention, blocker);
		}

		repaint();
	}

	private String describe(Intervention intervention) {
		return intervention.getClient() + " · " + FormatUtils.formatDate(intervention.getStartDate()) + " "
				+ FormatUtils.formatTime(intervention.getStartTime()) + " – "
				+ (intervention.getStartDate().equals(intervention.getEndDate()) ? ""
						: FormatUtils.formatDate(intervention.getEndDate()) + " ")
				+ FormatUtils.formatTime(intervention.getEndTime());
	}

	List<Intervention> selectedInterventions() {
		List<Intervention> selected = new ArrayList<>();

		LocalDateTime now = LocalDateTime.now();

		for (int row : getSelectedRows()) {
			if (row < 0 || row >= getRowCount())
				continue;

			Intervention intervention = interventions.get(convertRowIndexToModel(row));

			if (InterventionAssignmentService.isModifiable(intervention, now))
				selected.add(intervention);
		}

		return selected;
	}

	void showInterventions(List<Intervention> updated) {
		recurrenceTooltips.clear();
		
		List<Intervention> selected = selectedInterventions();
		clearSelection();
		interventions.clear();
		LocalDateTime now = LocalDateTime.now();
		updated.stream().filter(i -> InterventionAssignmentService.isModifiable(i, now)).forEach(interventions::add);
		((AbstractTableModel) getModel()).fireTableDataChanged();

		for (Intervention intervention : selected) {
			int modelRow = interventions.indexOf(intervention);

			if (modelRow >= 0) {
				int viewRow = convertRowIndexToView(modelRow);
				addRowSelectionInterval(viewRow, viewRow);
			}
		}

		updateBlockedRows();
	}

	boolean hideStartedInterventions() {
		LocalDateTime now = LocalDateTime.now();

		if (interventions.stream().noneMatch(i -> !InterventionAssignmentService.isModifiable(i, now)))
			return false;

		showInterventions(new ArrayList<>(interventions));
		return true;
	}

	private final class CompatibleSelectionModel extends DefaultListSelectionModel {

		@Override
		public void setSelectionInterval(int first, int last) {
			selectCompatibleInterval(first, last, true);
		}

		@Override
		public void addSelectionInterval(int first, int last) {
			selectCompatibleInterval(first, last, false);
		}

		private void selectCompatibleInterval(int first, int last, boolean replace) {
			if (first < 0 || last < 0 || first >= getRowCount() || last >= getRowCount())
				return;

			List<Intervention> compatible = selectedInterventions();
			List<Integer> accepted = new ArrayList<>();
			LocalDateTime now = LocalDateTime.now();
			int direction = first <= last ? 1 : -1;

			for (int row = first; ; row += direction) {
				Intervention candidate = interventions.get(convertRowIndexToModel(row));

				if (InterventionAssignmentService.isModifiable(candidate, now)
						&& selectionBlocker(candidate, compatible) == null) {
					accepted.add(row);
					compatible.add(candidate);
				}

				if (row == last)
					break;
			}

			if (accepted.isEmpty())
				return;

			boolean adjusting = getValueIsAdjusting();
			setValueIsAdjusting(true);

			try {
				if (replace)
					super.clearSelection();

				for (int row : accepted)
					super.addSelectionInterval(row, row);

				setAnchorSelectionIndex(first);
				moveLeadSelectionIndex(accepted.getLast());
			} finally {
				setValueIsAdjusting(adjusting);
			}
		}
	}
}
