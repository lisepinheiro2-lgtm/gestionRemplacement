package gestion.ui.personnel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import gestion.model.DateRange;
import gestion.model.Employee;
import gestion.model.HoursAlert;
import gestion.model.Intervention;
import gestion.service.WorkHoursService;
import gestion.ui.UIUtils;
import gestion.util.FormatUtils;

final class AssignmentImpactPanel extends JPanel {

	record Impact(Employee employee, LocalDate date, String period, double before, double after, double contract,
			HoursAlert alert) {
	}

	private final WorkHoursService hours;
	private final boolean gauges;
	private final AssignmentImpactGroups groups;
	private final List<Impact> rows = new ArrayList<>();
	private final JLabel title = new JLabel("Impact prévisionnel — occurrences sélectionnées");
	private final JPanel compactHeading = new JPanel(new BorderLayout());
	private final AbstractTableModel model = new AbstractTableModel() {
		private final String[] compactColumns = { "Salarié", "Période", "Avant → Après / Contrat", "Variation" };

		@Override
		public int getRowCount() {
			return rows.size();
		}

		@Override
		public int getColumnCount() {
			return compactColumns.length;
		}

		@Override
		public String getColumnName(int column) {
			return compactColumns[column];
		}

		@Override
		public Object getValueAt(int row, int column) {
			Impact impact = rows.get(row);
			return switch (column) {
			case 0 -> impact.employee().toString();
			case 1 -> impact.period();
			case 2 -> FormatUtils.formatContractHours(impact.before()) + " → "
					+ FormatUtils.formatContractHours(impact.after()) + " / "
					+ FormatUtils.formatContractHours(impact.contract());
			case 3 -> variation(impact.after() - impact.before());
			default -> variation(impact.after() - impact.before());
			};
		}
	};

	AssignmentImpactPanel(WorkHoursService hours, int height) {
		this(hours, height, false);
	}

	AssignmentImpactPanel(WorkHoursService hours, int height, boolean gauges) {
		super(new BorderLayout(0, 8));
		this.hours = hours;
		this.gauges = gauges;
		setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

		groups = gauges ? new AssignmentImpactGroups(height, title) : null;

		if (gauges) {
			title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
			add(groups, BorderLayout.CENTER);
		} else {
			compactHeading.add(title, BorderLayout.WEST);
			add(compactHeading, BorderLayout.NORTH);
			createCompactTable(height);
		}
	}

	void addHeaderAccessory(JComponent accessory) {
		JPanel centered = new JPanel(new GridBagLayout());
		centered.add(accessory);
		compactHeading.add(centered, BorderLayout.CENTER);
	}

	private void createCompactTable(int height) {
		JTable table = new JTable(model);
		table.setRowHeight(25);
		table.setFocusable(false);
		table.setRowSelectionAllowed(false);
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setResizingAllowed(false);
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
					boolean focused, int row, int column) {

				super.getTableCellRendererComponent(source, value, false, false, row, column);
				Impact impact = rows.get(row);
				setForeground(UIUtils.getTextColor());
				setHorizontalAlignment(column == 0 ? JLabel.LEFT : JLabel.CENTER);
				setToolTipText(column == 2 ? UIUtils.getHoursLegend() : null);

				if (column == 2)
					setForeground(UIUtils.getHoursColor(impact.alert()));

				if (column == 3 && Math.round((impact.after() - impact.before()) * 60) != 0)
					setForeground(
							impact.after() > impact.before() ? UIUtils.getTurquoiseColor() : UIUtils.getVioletColor());

				return this;
			}
		});

		table.getColumnModel().getColumn(2).setPreferredWidth(260);

		JScrollPane scroll = new JScrollPane(table);
		scroll.setColumnHeaderView(table.getTableHeader());
		scroll.setPreferredSize(new Dimension(0, height));
		add(scroll, BorderLayout.CENTER);
	}

	void showImpact(List<Intervention> targets, Employee destination, DateRange context, String scope) {
		showImpact(targets, destination, List.of(), context, scope);
	}

	void showImpact(List<Intervention> targets, Employee destination, List<Intervention> releases,
			DateRange context, String scope) {
		rows.clear();
		title.setText(
				"Impact prévisionnel — " + scope + (targets.isEmpty() ? " : sélectionnez des interventions" : ""));
		Set<Employee> employees = new LinkedHashSet<>();

		if (destination != null && !targets.isEmpty())
			employees.add(destination);

		Set<Intervention> touched = new LinkedHashSet<>();

		for (Intervention target : targets)
			if (target.getEmployee() != destination)
				touched.add(target);

		touched.addAll(releases);

		for (Intervention intervention : touched)
			if (intervention.getEmployee() != null && intervention.getEmployee() != destination)
				employees.add(intervention.getEmployee());

		boolean monthly = context.start().getDayOfMonth() == 1
				&& context.end().equals(context.start().plusMonths(1).minusDays(1));
		DateTimeFormatter format = DateTimeFormatter.ofPattern("dd/MM/uuuu");

		for (Employee employee : employees) {
			Set<LocalDate> weeks = new TreeSet<>();

			for (Intervention intervention : touched) {
				if (destination != employee && intervention.getEmployee() != employee)
					continue;

				LocalDate monday = intervention.getStartDate().with(DayOfWeek.MONDAY);
				LocalDateTime end = LocalDateTime.of(intervention.getEndDate(), intervention.getEndTime());

				while (monday.atStartOfDay().isBefore(end)) {
					weeks.add(monday);
					monday = monday.plusWeeks(1);
				}
			}

			if (monthly && !gauges)
				addImpact(employee, context.start(), true,
						"Mois " + context.start().format(DateTimeFormatter.ofPattern("MM/uuuu")), targets, destination, releases);

			for (LocalDate monday : weeks)
				addImpact(employee, monday, false, "Semaine du " + monday.format(format), targets, destination, releases);
		}

		model.fireTableDataChanged();
		if (gauges) {
			title.setText("Prévisionnel · " + (targets.isEmpty() ? "aucune occurrence" : scope));
			groups.showImpact(rows, destination);
		}
	}

	private void addImpact(Employee employee, LocalDate date, boolean monthly, String label,
			List<Intervention> targets, Employee destination, List<Intervention> releases) {

		double before = monthly ? hours.getMonthlyHours(employee, date) : hours.getWeeklyHours(employee, date);
		double after = hours.getProjectedHours(employee, date, monthly, targets, destination, releases);
		double contract = employee.getContractHours() * (monthly ? 52.0 / 12 : 1);
		rows.add(new Impact(employee, date, label, before, after, contract,
				hours.getHoursAlert(employee, date, monthly, after)));
	}

	static String variation(double hours) {
		return (Math.round(hours * 60) > 0 ? "+" : "") + FormatUtils.formatContractHours(hours);
	}

}
