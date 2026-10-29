package gestion.ui.personnel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import gestion.model.DateRange;
import gestion.model.Employee;
import gestion.model.Intervention;
import gestion.service.InterventionAssignmentService;
import gestion.service.InterventionAssignmentService.Conflict;
import gestion.service.InterventionAssignmentService.Plan;
import gestion.service.WorkHoursService;
import gestion.ui.UIUtils;
import gestion.util.FormatUtils;

final class AssignmentReviewPanel extends JPanel {

	private final InterventionAssignmentService assignments;
	private final List<Intervention> selected;
	private final Employee destination;
	private final DateRange period;
	private final boolean recurring;
	private final JRadioButton occurrences = new JRadioButton("Occurrences sélectionnées uniquement");
	private final JRadioButton series = new JRadioButton("Toutes les occurrences futures des séries concernées");
	private final AssignmentImpactPanel forecast;
	private final List<Intervention> conflicts = new ArrayList<>();
	private final Set<Intervention> releases = new LinkedHashSet<>();
	private final JPanel conflictPanel = new JPanel(new BorderLayout(0, 6));
	private JScrollPane conflictScroll;
	private SelectAllTableHeader releaseHeader;
	private final JLabel selectionSummary = new JLabel();
	private LocalDateTime nextRefreshAt;
	
	private final Timer availability = new Timer(1000, event -> {
		if (nextRefreshAt != null && nextRefreshAt.isBefore(LocalDateTime.now()))
			recalculate();
	});
	
	private final JLabel conflictCount = new JLabel();
	
	private final JLabel message = new JLabel() {
		@Override
		protected void paintComponent(Graphics graphics) {
			
			boolean blocked = Boolean.TRUE.equals(getClientProperty("blocked"));
			boolean dark = UIUtils.isDarkMode();
			Graphics2D g = (Graphics2D) graphics.create();

			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(blocked ? new Color(dark ? 0x41262B : 0xFFEDED) : new Color(dark ? 0x253141 : 0xEBF2FA));
				g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
			} finally {
				g.dispose();
			}

			setForeground(blocked ? new Color(dark ? 0xF2ACB2 : 0x9D292D) : UIUtils.getTextColor());
			super.paintComponent(graphics);
		}
	};
	
	private final JButton confirm = new JButton("Confirmer");
	private Plan preview;
	private final AbstractTableModel conflictModel = new AbstractTableModel() {
		private final String[] columns = { "Libérer", "Client", "Date(s)", "Horaires", "Durée" };

		@Override
		public int getRowCount() {
			return conflicts.size();
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
			
			return switch (column) {
			case 0 -> Boolean.class;
			case 2 -> LocalDateTime.class;
			case 4 -> Double.class;
			default -> String.class;
			};
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return column == 0 && InterventionAssignmentService.isModifiable(conflicts.get(row), LocalDateTime.now());
		}

		@Override
		public Object getValueAt(int row, int column) {

			Intervention intervention = conflicts.get(row);

			return switch (column) {
			case 0 -> releases.contains(intervention);
			case 1 -> intervention.getClient().getFirstName() + " " + intervention.getClient().getLastName();
			case 2 -> LocalDateTime.of(intervention.getStartDate(), intervention.getStartTime());
			case 3 -> FormatUtils.formatTime(intervention.getStartTime()) + "–"
					+ FormatUtils.formatTime(intervention.getEndTime());
			default -> intervention.getDurationHours();
			};
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
			if (!isCellEditable(row, column))
				return;

			Intervention intervention = conflicts.get(row);

			if (Boolean.TRUE.equals(value)) {
				releases.add(intervention);
			} else {
				releases.remove(intervention);
			}

			recalculate();
		}
	};

	AssignmentReviewPanel(InterventionAssignmentService assignments, WorkHoursService hours,
			List<Intervention> selected, Employee destination, DateRange period, Consumer<Plan> onConfirm,
			Runnable onCancel) {

		super(new BorderLayout(0, 6));
		this.assignments = assignments;
		this.selected = List.copyOf(selected);
		this.destination = destination;
		this.period = period;

		recurring = selected.stream().anyMatch(i -> i.getRecurringId() != null);
		setBorder(BorderFactory.createEmptyBorder(10, 14, 8, 14));
		add(createOptions(), BorderLayout.NORTH);

		forecast = new AssignmentImpactPanel(hours, 260, true);
		JPanel lists = new JPanel(new BorderLayout(0, 6)) {
			@Override
			public void doLayout() {
				updateConflictHeight(getHeight() - getInsets().top - getInsets().bottom);
				super.doLayout();
			}
		};

		lists.add(forecast, BorderLayout.CENTER);

		configureConflicts();

		lists.add(conflictPanel, BorderLayout.SOUTH);
		add(lists, BorderLayout.CENTER);
		add(createActions(onConfirm, onCancel), BorderLayout.SOUTH);

		occurrences.addItemListener(event -> {
			if (event.getStateChange() == java.awt.event.ItemEvent.SELECTED)
				recalculate();
		});
		series.addItemListener(event -> {
			if (event.getStateChange() == java.awt.event.ItemEvent.SELECTED)
				recalculate();
		});

		UIUtils.applyTheme(this);

		recalculate();
	}

	@Override
	public void addNotify() {
		super.addNotify();
		availability.start();
	}

	@Override
	public void removeNotify() {
		availability.stop();
		super.removeNotify();
	}

	private void updateSelectionSummary(LocalDateTime now) {

		long unavailable = selected.stream().filter(i -> !InterventionAssignmentService.isModifiable(i, now)).count();
		selectionSummary.setText(selected.size() + " occurrence(s) sélectionnée(s)"
				+ (unavailable > 0 ? ", dont " + unavailable + " passée(s) ou commencée(s)." : "."));

		long seriesCount = selected.stream().map(Intervention::getRecurringId).filter(id -> id != null).distinct()
				.count();
		selectionSummary
				.setToolTipText(seriesCount + " série(s) concernée(s). Les créneaux commencés sont toujours exclus.");
	}

	private void trackNextStart(List<Intervention> interventions, LocalDateTime now) {

		for (Intervention intervention : interventions) {
			LocalDateTime start = LocalDateTime.of(intervention.getStartDate(), intervention.getStartTime());

			if (!start.isBefore(now) && (nextRefreshAt == null || start.isBefore(nextRefreshAt)))
				nextRefreshAt = start;
		}
	}

	private JPanel createOptions() {

		JPanel options = new JPanel(new BorderLayout(0, 4));
		JPanel heading = new JPanel(new BorderLayout(12, 0));
		JLabel title = new JLabel(recurring ? "Portée de la modification" : "Vérifier l'attribution");

		title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
		selectionSummary.setFont(selectionSummary.getFont().deriveFont(12f));

		heading.add(title, BorderLayout.WEST);
		heading.add(selectionSummary, BorderLayout.EAST);

		options.add(heading, BorderLayout.NORTH);

		if (recurring) {
			ButtonGroup group = new ButtonGroup();

			group.add(occurrences);
			group.add(series);

			occurrences.setFont(occurrences.getFont().deriveFont(14f));
			series.setFont(series.getFont().deriveFont(14f));

			JPanel choices = createScopeChoices();

			choices.add(occurrences);
			choices.add(series);

			options.add(choices, BorderLayout.CENTER);
		}

		return options;
	}

	private JPanel createScopeChoices() {
		JPanel choices = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		choices.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				int required = occurrences.getPreferredSize().width + series.getPreferredSize().width + 24;
				boolean stacked = choices.getWidth() < required;

				if ((choices.getLayout() instanceof GridLayout) != stacked) {
					choices.setLayout(stacked ? new GridLayout(2, 1, 0, 2) : new FlowLayout(FlowLayout.LEFT, 8, 0));
					choices.revalidate();
				}
			}
		});
		return choices;
	}

	private void configureConflicts() {

		JPanel heading = new JPanel(new BorderLayout(12, 0));
		JLabel title = new JLabel("Interventions à libérer avant validation");

		title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));

		heading.add(title, BorderLayout.WEST);
		heading.add(conflictCount, BorderLayout.EAST);

		conflictPanel.add(heading, BorderLayout.NORTH);

		JTable table = new JTable(conflictModel) {
			@Override
			public java.awt.Component prepareRenderer(javax.swing.table.TableCellRenderer renderer, int row,
					int column) {
				java.awt.Component component = super.prepareRenderer(renderer, row, column);
				component.setEnabled(column != 0 || conflictModel.isCellEditable(convertRowIndexToModel(row), 0));
				return component;
			}
		};

		table.setRowHeight(28);
		table.setRowSelectionAllowed(false);
		table.setAutoCreateRowSorter(true);
		table.putClientProperty("terminateEditOnFocusLost", true);
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setResizingAllowed(false);

		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
			@Override
			public java.awt.Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
					boolean focused, int row, int column) {
				super.getTableCellRendererComponent(source, value, false, false, row, column);
				setHorizontalAlignment(JLabel.CENTER);
				Intervention intervention = conflicts.get(source.convertRowIndexToModel(row));
				int modelColumn = source.convertColumnIndexToModel(column);

				if (modelColumn == 2)
					setText(FormatUtils.formatDate(intervention.getStartDate())
							+ (intervention.getStartDate().equals(intervention.getEndDate()) ? ""
									: " – " + FormatUtils.formatDate(intervention.getEndDate())));

				if (modelColumn == 4)
					setText(FormatUtils.formatContractHours(intervention.getDurationHours()));

				return this;
			}
		};

		table.setDefaultRenderer(String.class, renderer);
		table.setDefaultRenderer(LocalDateTime.class, renderer);
		table.setDefaultRenderer(Double.class, renderer);
		table.getColumnModel().getColumn(0).setMinWidth(58);
		table.getColumnModel().getColumn(0).setMaxWidth(76);
		table.getColumnModel().getColumn(1).setPreferredWidth(250);
		table.getColumnModel().getColumn(2).setPreferredWidth(220);
		table.getColumnModel().getColumn(3).setPreferredWidth(170);
		table.getColumnModel().getColumn(4).setPreferredWidth(100);
		releaseHeader = new SelectAllTableHeader(table, 0, this::toggleAllReleases);

		JScrollPane scroll = new JScrollPane(table);
		conflictScroll = scroll;

		scroll.setColumnHeaderView(table.getTableHeader());
		scroll.setPreferredSize(new Dimension(0, 90));

		conflictPanel.add(scroll, BorderLayout.CENTER);
	}

	private List<Intervention> modifiableConflicts() {
		LocalDateTime now = LocalDateTime.now();
		return conflicts.stream().filter(i -> InterventionAssignmentService.isModifiable(i, now)).toList();
	}

	private void toggleAllReleases() {
		List<Intervention> eligible = modifiableConflicts();

		if (releases.containsAll(eligible)) {
			releases.removeAll(eligible);
		} else {
			releases.addAll(eligible);
		}

		recalculate();
	}

	private void updateReleaseHeader() {
		List<Intervention> eligible = modifiableConflicts();
		int checked = (int) eligible.stream().filter(releases::contains).count();
		releaseHeader.updateSelection(eligible.size(), checked);
	}

	private static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private void updateConflictHeight(int availableHeight) {
		if (conflictScroll == null || !conflictPanel.isVisible() || conflicts.isEmpty())
			return;

		JTable table = (JTable) conflictScroll.getViewport().getView();
		Insets insets = conflictScroll.getInsets();
		int header = table.getTableHeader().getPreferredSize().height + insets.top + insets.bottom;
		int rowHeight = table.getRowHeight();
		int overhead = conflictPanel.getPreferredSize().height - conflictScroll.getPreferredSize().height;
		int reserve = forecast.getMinimumSize().height + 6;
		int budget = Math.min(availableHeight * 2 / 5, Math.max(0, availableHeight - reserve));
		int capacity = Math.max(1, (budget - overhead - header) / rowHeight);
		int rows = Math.min(conflicts.size(), Math.min(5, capacity));
		int height = header + rows * rowHeight;

		if (conflictScroll.getPreferredSize().height != height)
			conflictScroll.setPreferredSize(new Dimension(0, height));
	}

	private void showMessage(String text, boolean blocked) {
		
		message.putClientProperty("blocked", blocked);
		message.setText(
				"<html><b>" + (blocked ? "Validation bloquée" : "À confirmer") + "</b> — " + escape(text) + "</html>");
		message.setToolTipText(text);
	}

	private JPanel createActions(Consumer<Plan> onConfirm, Runnable onCancel) {

		JPanel footer = new JPanel(new BorderLayout(0, 4));
		message.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
		message.setPreferredSize(new Dimension(0, 40));
		footer.add(message, BorderLayout.NORTH);
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 2));
		JButton cancel = new JButton("Annuler");
		UIUtils.styleRoundedButton(confirm);
		UIUtils.styleRoundedButton(cancel);

		confirm.addActionListener(event -> {
			if (preview != null && confirm.isEnabled())
				onConfirm.accept(preview);
		});

		cancel.addActionListener(event -> onCancel.run());

		actions.add(confirm);
		actions.add(cancel);
		footer.add(actions, BorderLayout.SOUTH);
		return footer;
	}

	private void recalculate() {

		preview = null;
		confirm.setEnabled(false);
		confirm.setText("Confirmer");
		LocalDateTime now = LocalDateTime.now();
		nextRefreshAt = null;
		updateSelectionSummary(now);
		trackNextStart(selected, now);
		forecast.showImpact(List.of(), destination, period, "calcul de la portée");
		conflicts.clear();
		conflictModel.fireTableDataChanged();
		conflictPanel.setVisible(false);

		try {

			Plan base = assignments.preview(selected, destination, series.isSelected());
			trackNextStart(base.targets(), now);
			List<Conflict> detected = assignments.conflicts(base);
			Set<Intervention> allowed = new LinkedHashSet<>();
			detected.stream().filter(Conflict::canRelease).forEach(c -> allowed.add(c.intervention()));

			conflicts.addAll(allowed);

			conflicts.sort(java.util.Comparator.comparing(i -> LocalDateTime.of(i.getStartDate(), i.getStartTime())));
			trackNextStart(conflicts, now);
			Set<Intervention> modifiable = new LinkedHashSet<>(allowed);
			modifiable.removeIf(i -> !InterventionAssignmentService.isModifiable(i, now));
			releases.retainAll(modifiable);
			conflictModel.fireTableDataChanged();
			conflictPanel.setVisible(!conflicts.isEmpty());
			conflictCount.setText(conflicts.size() + " intervention(s)");
			Plan plan = assignments.withReleases(base, new ArrayList<>(releases));
			forecast.showImpact(plan.targets(), destination, plan.releases(), period,

					series.isSelected() ? "récurrences futures, détail par semaine" : "occurrences sélectionnées");

			if (detected.stream().anyMatch(c -> !c.canRelease()))
				throw new IllegalArgumentException(
						"Des interventions sélectionnées se chevauchent entre elles. Revoyez la sélection initiale.");

			assignments.validate(plan);

			if (recurring && !occurrences.isSelected() && !series.isSelected()) {
				showMessage(
						"Choisissez la portée. Passées ou commencées exclues ; aucune modification avant confirmation.",
						false);
			} else {
				preview = plan;
				confirm.setText(releases.isEmpty() ? "Confirmer" : "Libérer et confirmer");
				confirm.setEnabled(true);
				showMessage(plan.targets().size() + " occurrence(s), " + releases.size()
						+ " libération(s). Aucune modification avant confirmation.", false);
			}
		} catch (IllegalArgumentException | IllegalStateException exception) {
			showMessage(exception.getMessage(), true);
		}

		updateReleaseHeader();
		revalidate();
		repaint();
	}
}
