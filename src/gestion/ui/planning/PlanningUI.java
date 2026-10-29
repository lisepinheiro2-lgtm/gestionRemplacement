package gestion.ui.planning;

import java.awt.BorderLayout;
import java.io.UncheckedIOException;
import gestion.ui.ActionDialogs;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import gestion.model.Intervention;
import gestion.model.DateRange;
import gestion.model.Client;
import gestion.model.Employee;
import gestion.service.ClientService;
import gestion.service.PersonnelService;
import gestion.service.PlanningService;
import gestion.service.RecurringInterventionService;
import gestion.ui.UIUtils;
import gestion.ui.AppThemeAware;
import gestion.ui.NavigationIcon;
import gestion.util.FormatUtils;

public class PlanningUI extends JPanel {

	private final PlanningInterventionDialogs dialogs;
	private final EmployeeAssignmentDialog assignmentDialog;
	private PlanningService planning;
	private ClientService clients;

	private LocalDate currentDate;
	private JPanel planningPanel;
	private JButton previousButton;
	private JButton todayButton;
	private JButton nextButton;
	private JComboBox<String> viewBox;
	private JLabel periodLabel;
	private LocalDate selectedDay = LocalDate.now();
	private int weekScrollPosition;
	private boolean weekViewportInitialized;
	private double weekDividerProportion = 0.76;
	private final javax.swing.JTextField search = new javax.swing.JTextField();
	private Map<LocalDate, List<Intervention>> visibleDays = Map.of();
	private final Map<ViewPeriod, CachedView> views = new LinkedHashMap<>(4, 0.75f, true);
	private ViewPeriod displayedPeriod;

	private record ViewPeriod(LocalDate start, boolean weekly, String search) {

		DateRange range() {
			if (weekly)
				return new DateRange(start, start.plusDays(6));

			LocalDate first = start.with(DayOfWeek.MONDAY);
			LocalDate last = start.plusMonths(1).minusDays(1).with(DayOfWeek.SUNDAY);
			return new DateRange(first, last);
		}
	}

	private record DisplayedIntervention(Intervention intervention, Client client, Employee employee,
			LocalDateTime start, LocalDateTime end, String clientName, String employeeName) {

		DisplayedIntervention(Intervention intervention) {
			this(intervention, intervention.getClient(), intervention.getEmployee(),
					LocalDateTime.of(intervention.getStartDate(), intervention.getStartTime()),
					LocalDateTime.of(intervention.getEndDate(), intervention.getEndTime()),
					intervention.getClient().toString(),
					intervention.getEmployee() == null ? "" : intervention.getEmployee().toString());
		}
	}

	private record CachedView(List<DisplayedIntervention> content, Component component,
			Map<LocalDate, List<Intervention>> days, boolean dark) {
	}

	private final class WeekView extends javax.swing.JSplitPane implements AppThemeAware {
		private final JPanel details = new JPanel(new BorderLayout());
		private final List<JPanel> dayColumns;
		private final Map<LocalDate, List<Intervention>> days;
		private final LocalDate monday;
		private LocalDate detailsDay;
		private javax.swing.LookAndFeel themedLookAndFeel;

		WeekView(JScrollPane scroll, List<JPanel> dayColumns, LocalDate monday) {
			super(javax.swing.JSplitPane.HORIZONTAL_SPLIT);
			this.dayColumns = List.copyOf(dayColumns);
			this.days = visibleDays;
			this.monday = monday;
			details.setMinimumSize(new Dimension(200, 0));
			details.setPreferredSize(new Dimension(280, 0));
			setLeftComponent(scroll);
			setRightComponent(details);
			setResizeWeight(0.76);
			setBorder(BorderFactory.createEmptyBorder());
			showDay(selectedDay);
		}

		void showDay(LocalDate day) {
			if (day.equals(detailsDay))
				return;

			JPanel next = createDayDetails(days.getOrDefault(day, List.of()));
			UIUtils.applyTheme(next);
			details.removeAll();
			details.add(next, BorderLayout.CENTER);
			details.revalidate();
			details.repaint();

			if (detailsDay != null)
				dayColumns.get((int) java.time.temporal.ChronoUnit.DAYS.between(monday, detailsDay)).repaint();
			dayColumns.get((int) java.time.temporal.ChronoUnit.DAYS.between(monday, day)).repaint();
			((JScrollPane) getLeftComponent()).getColumnHeader().repaint();
			detailsDay = day;
		}

		@Override
		public void applyAppTheme(boolean dark) {
			setBackground(UIUtils.getBackgroundColor());
			UIUtils.applyTheme(getLeftComponent());
			UIUtils.applyTheme(details);
			periodLabel.setIcon(new NavigationIcon(NavigationIcon.Kind.CALENDAR, 22, UIUtils.getTextColor()));
			themedLookAndFeel = javax.swing.UIManager.getLookAndFeel();
		}
	}

	public PlanningUI(PlanningService planning, PersonnelService personnel, ClientService clients,
			RecurringInterventionService recurringInterventionList) {

		this.planning = planning;
		this.clients = clients;

		currentDate = LocalDate.now();
		setLayout(new BorderLayout(0, 10));
		setBorder(BorderFactory.createEmptyBorder(10, 12, 12, 12));
		createNavigationPanel();
		planningPanel = new JPanel();
		add(planningPanel, BorderLayout.CENTER);

		assignmentDialog = new EmployeeAssignmentDialog(this, planning, personnel, recurringInterventionList,
				this::refreshPlanning);
		dialogs = new PlanningInterventionDialogs(this, planning, personnel, clients, recurringInterventionList,
				this::refreshPlanning);
	}

	private void createNavigationPanel() {
		JPanel navigationPanel = new JPanel(new BorderLayout(16, 8));
		JPanel periodPanel = new JPanel(new BorderLayout(8, 0));
		JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		previousButton = new PlanningActionButton("");
		previousButton.putClientProperty("gestion.icon", NavigationIcon.Kind.CHEVRON_LEFT);
		todayButton = new PlanningActionButton("Aujourd'hui");
		nextButton = new PlanningActionButton("");
		nextButton.putClientProperty("gestion.icon", NavigationIcon.Kind.CHEVRON_RIGHT);
		previousButton.setToolTipText("Période précédente");
		nextButton.setToolTipText("Période suivante");
		previousButton.getAccessibleContext().setAccessibleName("Période précédente");
		nextButton.getAccessibleContext().setAccessibleName("Période suivante");
		viewBox = new PlanningViewBox(new String[] { "Mois", "Semaine" });
		viewBox.setSelectedItem("Semaine");
		viewBox.getAccessibleContext().setAccessibleName("Vue du planning");
		periodLabel = new JLabel("", JLabel.CENTER);
		periodLabel.setFont(periodLabel.getFont().deriveFont(Font.BOLD, 15f));
		periodPanel.add(viewBox, BorderLayout.WEST);
		periodPanel.add(periodLabel, BorderLayout.CENTER);
		actionPanel.add(previousButton);
		actionPanel.add(nextButton);
		actionPanel.add(todayButton);
		periodPanel.add(actionPanel, BorderLayout.EAST);
		search.putClientProperty("JTextField.placeholderText", "Rechercher un salarié ou un client…");
		search.putClientProperty("JTextField.leadingIcon", new com.formdev.flatlaf.icons.FlatSearchIcon());
		search.putClientProperty("JTextField.showClearButton", true);
		search.getAccessibleContext().setAccessibleName("Filtrer le planning par salarié ou client");
		javax.swing.Timer delay = new javax.swing.Timer(150, event -> refreshPlanning());
		delay.setRepeats(false);
		UIUtils.addTextChangeListener(search, delay::restart);
		search.addActionListener(event -> {
			delay.stop();
			refreshPlanning();
		});
		navigationPanel.add(periodPanel, BorderLayout.NORTH);
		navigationPanel.add(search, BorderLayout.CENTER);
		search.setPreferredSize(new Dimension(260, 34));
		navigationPanel.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				if (navigationPanel.getWidth() >= 1240 && search.getParent() != actionPanel) {
					navigationPanel.remove(search);
					actionPanel.add(search, 0);
					navigationPanel.revalidate();
				} else if (navigationPanel.getWidth() < 1240 && search.getParent() == actionPanel) {
					actionPanel.remove(search);
					navigationPanel.add(search, BorderLayout.CENTER);
					navigationPanel.revalidate();
				}
			}
		});
		add(navigationPanel, BorderLayout.NORTH);
		previousButton.addActionListener(event -> previousPeriod());
		todayButton.addActionListener(event -> today());
		nextButton.addActionListener(event -> nextPeriod());
		viewBox.addActionListener(event -> refreshPlanning());
	}

	private static final class PlanningActionButton extends JButton implements AppThemeAware {

		PlanningActionButton(String text) {
			super(text);
			UIUtils.styleRoundedButton(this);
			setMargin(new Insets(5, 12, 5, 12));
			putClientProperty("FlatLaf.style", "background:#1B2330;foreground:#EEF3FB;"
					+ "hoverBackground:#2D394A;pressedBackground:#15202C;"
					+ "focusedBorderColor:#508FE6;"
					+ "[dark]borderColor:#3D4C61;[light]borderColor:#1B2330");
		}

		@Override
		public void applyAppTheme(boolean dark) {
			UIUtils.styleActionButton(this, "neutral");
		}
	}

	private static final class PlanningGroupButton extends JButton implements AppThemeAware {
		PlanningGroupButton(String text) {
			super(text);
		}

		@Override
		public void applyAppTheme(boolean dark) {
			UIUtils.styleActionButton(this, "neutral");
			setMargin(new Insets(0, 2, 0, 2));
			setFont(getFont().deriveFont(11f));
		}
	}

	private static final class PlanningViewBox extends JComboBox<String> implements AppThemeAware {

		PlanningViewBox(String[] views) {
			super(views);
			putClientProperty("JComponent.roundRect", true);
			setFont(getFont().deriveFont(15f));
			setPreferredSize(new Dimension(124, 32));
			DefaultListCellRenderer renderer = new DefaultListCellRenderer();
			renderer.setHorizontalAlignment(JLabel.CENTER);
			setRenderer(renderer);
			applyAppTheme(UIUtils.isDarkMode());
		}

		@Override
		public void applyAppTheme(boolean dark) {
			setBackground(UIUtils.getSurfaceColor());
			setForeground(UIUtils.getTextColor());
		}
	}

	private static void drawPastOverlay(Graphics g, int width, int height) {
		Graphics pastGraphics = g.create();
		pastGraphics.setColor(new Color(66, 70, 77, UIUtils.isDarkMode() ? 82 : 48));
		pastGraphics.fillRect(0, 0, width, height);
		pastGraphics.dispose();
	}

	private void openInterventionActions(Intervention intervention) {
		String[] options = { "Attribuer / Réattribuer", "Modifier", "Supprimer" };
		int choice = JOptionPane.showOptionDialog(this, "Que voulez-vous faire ?", "Choix de l'action",
				JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

		switch (choice) {

		case 0:
			assignmentDialog.open(intervention);
			break;

		case 1:
			dialogs.openEditIntervention(intervention);
			break;

		case 2:
			int confirmation = JOptionPane.showConfirmDialog(this, "Supprimer cette intervention ?",
					"Confirmer la suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

			if (confirmation == JOptionPane.YES_OPTION) {
				try {
					planning.removeIntervention(intervention);
					refreshPlanning();
				} catch (IllegalArgumentException | UncheckedIOException e) {
					ActionDialogs.error(this, e);
				}
			}
			break;

		default:
			break;
		}
	}

	private void previousPeriod() {

		if (viewBox.getSelectedItem().equals("Semaine")) {
			currentDate = currentDate.minusWeeks(1);
		} else {
			currentDate = currentDate.minusMonths(1);
		}

		refreshPlanning();
	}

	private void nextPeriod() {

		if (viewBox.getSelectedItem().equals("Semaine")) {
			currentDate = currentDate.plusWeeks(1);
		} else {
			currentDate = currentDate.plusMonths(1);
		}

		refreshPlanning();
	}

	private void today() {
		currentDate = LocalDate.now();
		selectedDay = currentDate;
		refreshPlanning();
	}

	private WeekView displayWeek() {
		if (!weekViewportInitialized) {
			weekScrollPosition = visibleDays.entrySet().stream()
					.flatMapToInt(entry -> entry.getValue().stream().mapToInt(i -> dayStart(i, entry.getKey())))
					.min().orElse(8 * 60);
			weekViewportInitialized = true;
		}
		LocalDate monday = currentDate.with(DayOfWeek.MONDAY);
		JPanel bodyPanel = new JPanel(new GridLayout(1, 7));
		bodyPanel.setPreferredSize(new Dimension(0, 24 * 60));
		JPanel hoursPanel = new JPanel(new GridLayout(24, 1));
		hoursPanel.setPreferredSize(new Dimension(56, 24 * 60));
		for (int hour = 0; hour < 24; hour++) {
			JLabel label = new JLabel(String.format("%02d:00", hour), JLabel.CENTER);
			label.setVerticalAlignment(JLabel.TOP);
			hoursPanel.add(label);
		}

		List<JPanel> dayColumns = new ArrayList<>();
		for (int day = 0; day < 7; day++) {
			LocalDate currentDay = monday.plusDays(day);
			List<Intervention> dayInterventions = visibleDays.getOrDefault(currentDay, List.of());
			JPanel dayPanel = new JPanel(null) {
				@Override
				public void doLayout() {
					layoutWeekDay(this, dayInterventions, currentDay);
				}

				@Override
				protected void paintComponent(Graphics graphics) {
					super.paintComponent(graphics);
					if (currentDay.equals(selectedDay)) {
						graphics.setColor(new Color(UIUtils.isDarkMode() ? 0x21374F : 0xE4F3FF));
						graphics.fillRect(0, 0, getWidth(), getHeight());
					}
					graphics.setColor(UIUtils.getBorderColor());
					graphics.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight());
					for (int hour = 1; hour < 24; hour++)
						graphics.drawLine(0, hour * 60, getWidth(), hour * 60);
					LocalDateTime now = LocalDateTime.now();
					if (currentDay.isBefore(now.toLocalDate())) {
						drawPastOverlay(graphics, getWidth(), getHeight());
					} else if (currentDay.equals(now.toLocalDate())) {
						Graphics past = graphics.create();
						past.clipRect(0, 0, getWidth(), now.getHour() * 60 + now.getMinute());
						drawPastOverlay(past, getWidth(), getHeight());
						past.dispose();
					}
				}
			};
			dayPanel.setPreferredSize(new Dimension(0, 24 * 60));
			dayPanel.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent event) {
					if (SwingUtilities.isLeftMouseButton(event)) {
						int minutes = Math.max(0, Math.min(1439, event.getY()));
						dialogs.openAddInterventionChoice(currentDay, LocalTime.of(minutes / 60, minutes % 60));
					}
				}
			});

			layoutWeekDay(dayPanel, dayInterventions, currentDay);
			bodyPanel.add(dayPanel);
			dayColumns.add(dayPanel);
		}

		JScrollPane scrollPane = new JScrollPane(bodyPanel);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setRowHeaderView(hoursPanel);
		scrollPane.setCorner(JScrollPane.UPPER_LEFT_CORNER, new JLabel());
		scrollPane.getVerticalScrollBar().setUnitIncrement(30);
		installDayStrip(scrollPane, bodyPanel, dayColumns, monday, false);

		scrollPane.setMinimumSize(new Dimension(280, 0));
		return new WeekView(scrollPane, dayColumns, monday);
	}

	private int dayStart(Intervention intervention, LocalDate day) {
		return intervention.getStartDate().equals(day)
				? intervention.getStartTime().getHour() * 60 + intervention.getStartTime().getMinute() : 0;
	}

	private int dayEnd(Intervention intervention, LocalDate day) {
		return intervention.getEndDate().equals(day)
				? intervention.getEndTime().getHour() * 60 + intervention.getEndTime().getMinute() : 24 * 60;
	}

	private record WeekSlot(int column, int count, List<Intervention> interventions) {
	}

	private record WeekBlock(int start, int end, WeekSlot slot) {
	}

	private record WeekLayout(int capacity, List<WeekBlock> blocks) {
	}

	private List<WeekBlock> weekBlocks(List<Intervention> interventions, LocalDate day, int capacity) {
		Map<Integer, List<Intervention>> starts = new java.util.TreeMap<>();
		Map<Integer, List<Intervention>> ends = new java.util.TreeMap<>();
		java.util.TreeSet<Integer> boundaries = new java.util.TreeSet<>();
		for (Intervention intervention : interventions) {
			int start = dayStart(intervention, day);
			int end = dayEnd(intervention, day);
			if (end <= start)
				continue;
			starts.computeIfAbsent(start, ignored -> new ArrayList<>()).add(intervention);
			ends.computeIfAbsent(end, ignored -> new ArrayList<>()).add(intervention);
			boundaries.add(start);
			boundaries.add(end);
		}
		List<Integer> times = new ArrayList<>(boundaries);
		java.util.LinkedHashSet<Intervention> active = new java.util.LinkedHashSet<>();
		List<WeekBlock> blocks = new ArrayList<>();
		Map<WeekSlot, Integer> last = new HashMap<>();
		for (int position = 0; position + 1 < times.size(); position++) {
			int start = times.get(position);
			int end = times.get(position + 1);
			active.removeAll(ends.getOrDefault(start, List.of()));
			active.addAll(starts.getOrDefault(start, List.of()));
			List<Intervention> current = new ArrayList<>(active);
			int count = Math.min(capacity, current.size());
			for (int column = 0; column < count; column++) {
				boolean grouped = current.size() > capacity && column == count - 1;
				List<Intervention> targets = grouped ? List.copyOf(current.subList(column, current.size()))
						: List.of(current.get(column));
				WeekSlot slot = new WeekSlot(column, count, targets);
				Integer previous = last.get(slot);
				if (previous != null && blocks.get(previous).end() == start) {
					WeekBlock block = blocks.get(previous);
					blocks.set(previous, new WeekBlock(block.start(), end, slot));
				} else {
					last.put(slot, blocks.size());
					blocks.add(new WeekBlock(start, end, slot));
				}
			}
		}
		return blocks;
	}

	private void layoutWeekDay(JPanel dayPanel, List<Intervention> interventions, LocalDate day) {
		int capacity = Math.max(2, Math.min(3, dayPanel.getWidth() / 90));
		WeekLayout layout = (WeekLayout) dayPanel.getClientProperty("planning.layout");
		boolean rebuild = layout == null || layout.capacity() != capacity;
		if (!rebuild && Integer.valueOf(dayPanel.getWidth()).equals(dayPanel.getClientProperty("planning.width")))
			return;

		dayPanel.putClientProperty("planning.width", dayPanel.getWidth());
		if (rebuild) {
			layout = new WeekLayout(capacity, weekBlocks(interventions, day, capacity));
			dayPanel.putClientProperty("planning.layout", layout);
			dayPanel.removeAll();

			for (WeekBlock block : layout.blocks()) {
				List<Intervention> targets = block.slot().interventions();
				JButton button;
				if (targets.size() == 1) {
					Intervention intervention = targets.getFirst();
					button = new PlanningInterventionButton(intervention, intervention.getStartTime().toString(),
							intervention.getEndTime().toString(), true);
					button.putClientProperty("planning.intervention", intervention);
					UIUtils.styleInterventionButton(button, intervention.getEmployee() != null);
					button.addActionListener(event -> {
						selectDay(day);
						openInterventionActions(intervention);
					});
				} else {
					button = new PlanningGroupButton("+" + targets.size());
					button.putClientProperty("planning.group", targets);
					String period = String.format("%02d:%02d – %02d:%02d", block.start() / 60, block.start() % 60,
							block.end() / 60, block.end() % 60);
					button.setToolTipText(targets.size() + " interventions simultanées — " + period);
					button.getAccessibleContext().setAccessibleName(button.getToolTipText());
					((PlanningGroupButton) button).applyAppTheme(UIUtils.isDarkMode());
					button.addActionListener(event -> openGroup(day, targets));
				}
				dayPanel.add(button);
			}
		}

		for (int index = 0; index < layout.blocks().size(); index++) {
			WeekBlock block = layout.blocks().get(index);
			int left = block.slot().column() * dayPanel.getWidth() / block.slot().count();
			int right = (block.slot().column() + 1) * dayPanel.getWidth() / block.slot().count();
			dayPanel.getComponent(index).setBounds(left + 3, block.start(), Math.max(1, right - left - 6),
					block.end() - block.start());
		}
	}

	private JPanel createDayDetails(List<Intervention> interventions) {
		JPanel panel = new JPanel(new BorderLayout(0, 12));
		panel.setMinimumSize(new Dimension(200, 0));
		panel.setPreferredSize(new Dimension(280, 0));
		panel.putClientProperty("gestion.panelTitle", "DÉTAILS DU JOUR");
		JPanel heading = new JPanel(new GridLayout(0, 1, 0, 5));
		JLabel day = new JLabel(selectedDay.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)));
		day.setFont(day.getFont().deriveFont(Font.BOLD, 17f));
		heading.add(day);
		heading.add(new JLabel(interventions.size() + " intervention(s)" + (search.getText().isBlank() ? "" : " — recherche")));
		panel.add(heading, BorderLayout.NORTH);
		javax.swing.JList<Intervention> list = new javax.swing.JList<>(interventions.toArray(Intervention[]::new)) {
			@Override
			public String getToolTipText(MouseEvent event) {
				int index = locationToIndex(event.getPoint());
				return index >= 0 && getCellBounds(index, index).contains(event.getPoint())
						? interventionDescription(getModel().getElementAt(index), false) : null;
			}
		};
		list.setFixedCellHeight(88);
		list.setFixedCellWidth(1);
		list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new DayDetailsRenderer());
		list.setToolTipText("");
		list.getAccessibleContext().setAccessibleName("Interventions du jour");
		list.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				int index = list.locationToIndex(event.getPoint());
				if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount() == 1 && index >= 0
						&& list.getCellBounds(index, index).contains(event.getPoint()))
					openInterventionActions(list.getModel().getElementAt(index));
			}
		});
		list.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "interventionActions");
		list.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("SPACE"), "interventionActions");
		list.getActionMap().put("interventionActions", new javax.swing.AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent event) {
				if (list.getSelectedValue() != null)
					openInterventionActions(list.getSelectedValue());
			}
		});
		JScrollPane scroll = new JScrollPane(list);
		if (interventions.isEmpty())
			scroll.setViewportView(new JLabel("Aucune intervention à afficher."));
		scroll.getVerticalScrollBar().setUnitIncrement(25);
		scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		panel.add(scroll, BorderLayout.CENTER);
		return panel;
	}

	private static String interventionDescription(Intervention intervention, boolean html) {
		String employee = intervention.getEmployee() == null ? "Intervention non attribuée"
				: intervention.getEmployee().getFirstName() + " " + intervention.getEmployee().getLastName();
		String period = intervention.getStartTime() + " – " + intervention.getEndTime();
		if (!intervention.getStartDate().equals(intervention.getEndDate()))
			period = intervention.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM")) + " "
					+ intervention.getStartTime() + " – "
					+ intervention.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM")) + " " + intervention.getEndTime();

		return html ? "<html>" + escape(period) + "<br><b>" + escape(employee) + "</b><br>Client : "
				+ escape(intervention.getClient().toString()) + "</html>"
				: employee + " | " + intervention.getClient() + " | " + period;
	}

	private static final class DayDetailsRenderer extends JPanel implements javax.swing.ListCellRenderer<Intervention> {
		private final JButton button = new JButton();
		private Boolean assigned;
		private javax.swing.LookAndFeel lookAndFeel;
		private Color background;

		DayDetailsRenderer() {
			super(new BorderLayout());
			setOpaque(false);
			setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
			button.setHorizontalAlignment(JLabel.LEFT);
			add(button, BorderLayout.CENTER);
		}

		@Override
		public Component getListCellRendererComponent(javax.swing.JList<? extends Intervention> list,
				Intervention value, int index, boolean selected, boolean focused) {
			boolean nextAssigned = value.getEmployee() != null;
			if (assigned == null || assigned != nextAssigned || lookAndFeel != javax.swing.UIManager.getLookAndFeel()) {
				UIUtils.styleInterventionButton(button, nextAssigned);
				background = button.getBackground();
				assigned = nextAssigned;
				lookAndFeel = javax.swing.UIManager.getLookAndFeel();
			}

			button.setFont(list.getFont());
			button.setText(interventionDescription(value, true));
			button.getAccessibleContext().setAccessibleName(interventionDescription(value, false));
			button.setBackground(selected ? list.getSelectionBackground() : background);
			button.setForeground(selected ? list.getSelectionForeground() : UIUtils.getTextColor());
			return this;
		}
	}

	private JPanel interventionList(List<Intervention> interventions) {
		JPanel list = new JPanel();
		list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
		for (Intervention intervention : interventions) {
			String employee = intervention.getEmployee() == null ? "Intervention non attribuée"
					: intervention.getEmployee().getFirstName() + " " + intervention.getEmployee().getLastName();
			String period = intervention.getStartTime() + " – " + intervention.getEndTime();
			if (!intervention.getStartDate().equals(intervention.getEndDate()))
				period = intervention.getStartDate().format(DateTimeFormatter.ofPattern("dd/MM")) + " "
						+ intervention.getStartTime() + " – "
						+ intervention.getEndDate().format(DateTimeFormatter.ofPattern("dd/MM")) + " " + intervention.getEndTime();
			JButton button = new JButton("<html>" + escape(period) + "<br><b>" + escape(employee)
					+ "</b><br>Client : " + escape(intervention.getClient().toString()) + "</html>");
			button.setHorizontalAlignment(JLabel.LEFT);
			button.setAlignmentX(Component.LEFT_ALIGNMENT);
			button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));
			button.setPreferredSize(new Dimension(230, 80));
			button.setToolTipText(employee + " | " + intervention.getClient() + " | " + period);
			button.getAccessibleContext().setAccessibleName(button.getToolTipText());
			UIUtils.styleInterventionButton(button, intervention.getEmployee() != null);
			button.addActionListener(event -> {
				if (Boolean.TRUE.equals(list.getClientProperty("gestion.group"))) {
					java.awt.Window window = SwingUtilities.getWindowAncestor(button);
					if (window instanceof javax.swing.JDialog)
						window.dispose();
				}
				openInterventionActions(intervention);
			});
			list.add(button);
			list.add(javax.swing.Box.createVerticalStrut(8));
		}
		return list;
	}

	private static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private void selectDay(LocalDate day) {
		if (day.equals(selectedDay))
			return;

		selectedDay = day;
		if (displayedPeriod != null && displayedPeriod.weekly()
				&& !day.isBefore(displayedPeriod.start()) && !day.isAfter(displayedPeriod.start().plusDays(6))
				&& displayedPeriod.search().equals(normalize(search.getText()))
				&& planningPanel.getComponentCount() > 0 && planningPanel.getComponent(0) instanceof WeekView week) {
			week.showDay(day);
			return;
		}

		refreshPlanning();
	}

	private void openGroup(LocalDate day, List<Intervention> interventions) {
		selectDay(day);
		JPanel list = interventionList(interventions);
		list.putClientProperty("gestion.group", true);
		JScrollPane scroll = new JScrollPane(list);
		scroll.setPreferredSize(new Dimension(420, Math.min(500, interventions.size() * 88)));
		UIUtils.applyTheme(scroll);
		JOptionPane.showMessageDialog(this, scroll, "Interventions superposées — " + day,
				JOptionPane.PLAIN_MESSAGE);
	}

	private JScrollPane displayMonth() {
		LocalDate firstDay = currentDate.withDayOfMonth(1);

		int numberOfDays = currentDate.lengthOfMonth();
		int firstDayPosition = firstDay.getDayOfWeek().getValue();
		int numberOfWeeks = (int) Math.ceil((firstDayPosition - 1 + numberOfDays) / 7.0);
		JPanel monthPanel = new JPanel(new GridLayout(0, 7)) {
			@Override
			public Dimension getPreferredSize() {
				return new Dimension(0, super.getPreferredSize().height);
			}
		};
		List<JPanel> dayColumns = new ArrayList<>();
		LocalDate calendarStart = firstDay.minusDays(firstDayPosition - 1);

		for (int i = 0; i < numberOfWeeks * 7; i++) {
			LocalDate day = calendarStart.plusDays(i);
			JPanel dayPanel = new JPanel(new BorderLayout()) {

				@Override
				public Dimension getPreferredSize() {
					Dimension size = super.getPreferredSize();
					return new Dimension(Math.max(120, size.width), Math.max(40, size.height));
				}

				@Override
				protected void paintComponent(Graphics g) {
					super.paintComponent(g);

					if (day.isBefore(LocalDate.now())) {
						drawPastOverlay(g, getWidth(), getHeight());
					}
				}
			};

			dayPanel.setBorder(BorderFactory.createLineBorder(UIUtils.getBorderColor()));
			dayPanel.putClientProperty("planning.day", day);

			List<Intervention> dayInterventions = visibleDays.getOrDefault(day, List.of());

			JPanel interventionsPanel = new JPanel();
			interventionsPanel.setOpaque(false);
			interventionsPanel.setLayout(new BoxLayout(interventionsPanel, BoxLayout.Y_AXIS));

			MouseAdapter addInterventionListener = new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent event) {
					if (SwingUtilities.isLeftMouseButton(event)) {
						dialogs.openAddInterventionChoice(day, null);
					}
				}
			};

			JLabel dateLabel = new JLabel(day.format(DateTimeFormatter.ofPattern("dd/MM")), JLabel.CENTER);
			dateLabel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
			dateLabel.setToolTipText(day.format(DateTimeFormatter.ofPattern("EEEE dd MMMM uuuu", Locale.FRENCH)));
			dateLabel.addMouseListener(addInterventionListener);
			dayPanel.add(dateLabel, BorderLayout.NORTH);
			dayPanel.add(interventionsPanel, BorderLayout.CENTER);
			dayPanel.addMouseListener(addInterventionListener);
			interventionsPanel.addMouseListener(addInterventionListener);

			for (Intervention intervention : dayInterventions.stream().limit(2).toList()) {
				int startMinutes = 0;

				if (intervention.getStartDate().equals(day)) {
					startMinutes = intervention.getStartTime().getHour() * 60 + intervention.getStartTime().getMinute();
				}

				int endMinutes = 24 * 60;

				if (intervention.getEndDate().equals(day)) {
					endMinutes = intervention.getEndTime().getHour() * 60 + intervention.getEndTime().getMinute();
				}

				if (endMinutes <= startMinutes) {
					continue;
				}

				String startText = String.format("%02dh%02d", startMinutes / 60, startMinutes % 60);
				String endText = String.format("%02dh%02d", (endMinutes / 60) % 24, endMinutes % 60);
				JButton interventionButton = new PlanningInterventionButton(intervention, startText, endText);

				interventionButton.setHorizontalAlignment(JLabel.CENTER);
				interventionButton.setVerticalAlignment(JLabel.CENTER);
				interventionButton.setFont(interventionButton.getFont().deriveFont(12f));
				interventionButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
				interventionButton.putClientProperty("planning.intervention", intervention);
				interventionButton.setToolTipText(interventionDescription(intervention, false));
				interventionButton.addActionListener(event -> openInterventionActions(intervention));
				UIUtils.styleInterventionButton(interventionButton, intervention.getEmployee() != null);

				interventionsPanel.add(interventionButton);
			}

			if (dayInterventions.size() > 2) {
				JButton more = new JButton("Voir les " + dayInterventions.size());
				more.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
				more.putClientProperty("planning.group", dayInterventions);
				more.setToolTipText("Afficher toutes les interventions du " + FormatUtils.formatDate(day));
				more.getAccessibleContext().setAccessibleName(more.getToolTipText());
				more.addActionListener(event -> openGroup(day, dayInterventions));
				interventionsPanel.add(more);
			}

			monthPanel.add(dayPanel);
			dayColumns.add(dayPanel);
		}

		JScrollPane scrollPane = new JScrollPane(monthPanel);
		scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		installDayStrip(scrollPane, monthPanel, dayColumns, calendarStart, true);
		scrollPane.getVerticalScrollBar().setUnitIncrement(25);
		LocalDate focus = selectedDay.getMonth().equals(firstDay.getMonth()) && selectedDay.getYear() == firstDay.getYear()
				? selectedDay : currentDate;
		JPanel selected = dayColumns.stream().filter(column -> focus.equals(column.getClientProperty("planning.day")))
				.findFirst().orElse(null);
		SwingUtilities.invokeLater(() -> {
			if (selected != null && scrollPane.getParent() == planningPanel)
				monthPanel.scrollRectToVisible(selected.getBounds());
		});

		return scrollPane;
	}

	private static final class PlanningNameLegend extends JLabel implements AppThemeAware {

		private final boolean employee;

		PlanningNameLegend(String text, boolean employee) {
			super(text);
			this.employee = employee;
			applyAppTheme(UIUtils.isDarkMode());
		}

		@Override
		public void applyAppTheme(boolean dark) {
			setForeground(PlanningInterventionButton.nameColor(employee, dark));
		}
	}

	private void installDayStrip(JScrollPane scrollPane, JPanel bodyPanel, List<JPanel> dayColumns,
			LocalDate firstDay, boolean monthly) {
		JPanel strip = new JPanel(null) {

			@Override
			public Dimension getPreferredSize() {
				return new Dimension(bodyPanel.getPreferredSize().width, 46);
			}

			@Override
			public void doLayout() {
				int first = 0;
				int top = scrollPane.getViewport().getViewPosition().y;

				if (monthly) {
					for (int row = 0; row < dayColumns.size(); row += 7) {
						if (dayColumns.get(row).getY() <= top)
							first = row;
					}
				}

				Rectangle visible = scrollPane.getViewport().getViewRect();

				for (int day = 0; day < 7; day++) {
					JPanel column = dayColumns.get(first + day);
					int x = SwingUtilities.convertPoint(column, 0, 0, bodyPanel).x;
					int left = Math.max(x, visible.x);
					int right = Math.min(x + column.getWidth(), visible.x + visible.width);
					JLabel label = (JLabel) getComponent(day);
					LocalDate date = firstDay.plusDays(first + day);
					if (!date.equals(label.getClientProperty("day"))) {
						label.setText("<html><center>" + date.format(DateTimeFormatter.ofPattern("EEEE", Locale.FRENCH))
								+ "<br>" + date.format(DateTimeFormatter.ofPattern("dd/MM")) + "</center></html>");
						label.setToolTipText(date.format(DateTimeFormatter.ofPattern("EEEE dd MMMM uuuu", Locale.FRENCH)));
						label.putClientProperty("day", date);
					}
					label.setBounds(left, 0, Math.max(0, right - left), getHeight());
				}
			}
		};

		for (int day = 0; day < 7; day++) {
			JLabel label = new JLabel("", JLabel.CENTER) {
				@Override
				public void updateUI() {
					super.updateUI();
					setBorder(BorderFactory.createLineBorder(UIUtils.getBorderColor()));
				}

				@Override
				protected void paintComponent(Graphics graphics) {
					boolean active = !monthly && selectedDay.equals(getClientProperty("day"));
					setOpaque(true);
					setBackground(active ? new Color(UIUtils.isDarkMode() ? 0x21374F : 0xE4F3FF)
							: UIUtils.getSurfaceColor());
					setForeground(active ? UIUtils.getTurquoiseColor() : UIUtils.getTextColor());
					super.paintComponent(graphics);
				}
			};
			label.setFont(label.getFont().deriveFont(Font.BOLD, 14f));
			label.setBorder(BorderFactory.createLineBorder(UIUtils.getBorderColor()));
			label.addMouseListener(new MouseAdapter() {

				@Override
				public void mouseClicked(MouseEvent event) {
					if (SwingUtilities.isLeftMouseButton(event)) {
						LocalDate day = (LocalDate) label.getClientProperty("day");
						if (monthly)
							dialogs.openAddInterventionChoice(day, null);
						else
							selectDay(day);
					}
				}
			});
			strip.add(label);
		}

		scrollPane.setColumnHeaderView(strip);
		scrollPane.getViewport().addChangeListener(event -> {
			strip.doLayout();
			strip.repaint();
		});
		bodyPanel.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent event) {
				strip.revalidate();
				strip.doLayout();
				strip.repaint();
			}
		});
	}

	public void refreshPlanning() {
		if (!SwingUtilities.isEventDispatchThread()) {
			SwingUtilities.invokeLater(this::refreshPlanning);
			return;
		}

		if (planningPanel.getComponentCount() > 0
				&& planningPanel.getComponent(0) instanceof javax.swing.JSplitPane split
				&& split.getLeftComponent() instanceof JScrollPane scroll) {
			weekScrollPosition = scroll.getVerticalScrollBar().getValue();
			int available = split.getWidth() - split.getDividerSize();
			if (available > 0)
				weekDividerProportion = Math.max(0.4, Math.min(0.9, (double) split.getDividerLocation() / available));
		}

		boolean weekly = "Semaine".equals(viewBox.getSelectedItem());
		LocalDate first = currentDate.with(DayOfWeek.MONDAY);
		if (weekly && (selectedDay.isBefore(first) || selectedDay.isAfter(first.plusDays(6))))
			selectedDay = currentDate;
		ViewPeriod period = new ViewPeriod(weekly ? first : currentDate.withDayOfMonth(1), weekly,
				normalize(search.getText()));
		DateTimeFormatter fullFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu");
		LocalDate start = period.start();
		LocalDate end = weekly ? start.plusDays(6) : start.plusMonths(1).minusDays(1);
		DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("dd MMMM", Locale.FRENCH);
		DateTimeFormatter yearFormat = DateTimeFormatter.ofPattern("dd MMMM uuuu", Locale.FRENCH);
		periodLabel.setText(weekly ? start.format(start.getYear() == end.getYear() ? dayFormat : yearFormat)
				+ " – " + end.format(yearFormat)
				: start.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.FRENCH)));
		periodLabel.setIcon(new NavigationIcon(NavigationIcon.Kind.CALENDAR, 22, UIUtils.getTextColor()));
		periodLabel.setToolTipText(start.format(fullFormat) + " – " + end.format(fullFormat));
		DateRange range = period.range();
		List<Intervention> interventions = planning.getInterventionsForPeriod(range);
		String query = normalize(search.getText());
		if (!query.isBlank())
			interventions.removeIf(i -> !normalize(i.getClient().getFirstName() + " " + i.getClient().getLastName()).contains(query)
					&& !normalize(i.getClient().getLastName() + " " + i.getClient().getFirstName()).contains(query)
					&& (i.getEmployee() == null
							|| !normalize(i.getEmployee().getFirstName() + " " + i.getEmployee().getLastName()).contains(query)
							&& !normalize(i.getEmployee().getLastName() + " " + i.getEmployee().getFirstName()).contains(query)));
		interventions.sort(Comparator.comparing(Intervention::getStartDate).thenComparing(Intervention::getStartTime));
		List<DisplayedIntervention> content = interventions.stream().map(DisplayedIntervention::new).toList();
		CachedView cached = views.get(period);
		Component component;

		if (cached != null && cached.content().equals(content)) {
			visibleDays = cached.days();
			component = cached.component();
			boolean attached = component.getParent() == planningPanel;
			boolean staleTheme = component instanceof WeekView week
					? week.themedLookAndFeel != javax.swing.UIManager.getLookAndFeel()
					: cached.dark() != UIUtils.isDarkMode();

			if (staleTheme) {
				if (!attached)
					SwingUtilities.updateComponentTreeUI(component);
				UIUtils.applyTheme(component);
			}

			if (component instanceof WeekView week)
				week.showDay(selectedDay);
		} else {
			visibleDays = groupByDay(content, range);
			component = weekly ? displayWeek() : displayMonth();
			UIUtils.applyTheme(component);
		}

		views.put(period, new CachedView(content, component, visibleDays, UIUtils.isDarkMode()));
		if (views.size() > 3)
			views.remove(views.keySet().iterator().next());

		displayedPeriod = period;
		boolean attaching = component.getParent() != planningPanel;
		if (attaching) {
			planningPanel.removeAll();
			planningPanel.setLayout(new BorderLayout());
			planningPanel.add(component, BorderLayout.CENTER);
			planningPanel.revalidate();
			planningPanel.repaint();
		}

		if (attaching && component instanceof WeekView split) {
			int position = weekScrollPosition;
			double proportion = weekDividerProportion;
			SwingUtilities.invokeLater(() -> {
				if (split.getParent() == planningPanel) {
					split.setDividerLocation(proportion);
					((JScrollPane) split.getLeftComponent()).getVerticalScrollBar().setValue(position);
				}
			});
		}
	}

	private Map<LocalDate, List<Intervention>> groupByDay(List<DisplayedIntervention> content, DateRange period) {
		Map<LocalDate, List<Intervention>> days = new HashMap<>();

		for (DisplayedIntervention displayed : content) {
			LocalDate first = displayed.start().toLocalDate();
			LocalDate last = displayed.end().minusNanos(1).toLocalDate();

			if (first.isBefore(period.start()))
				first = period.start();
			if (last.isAfter(period.end()))
				last = period.end();

			for (LocalDate day = first; !day.isAfter(last); day = day.plusDays(1))
				days.computeIfAbsent(day, ignored -> new ArrayList<>()).add(displayed.intervention());
		}

		return days;
	}

	private static String normalize(String text) {
		return java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFD)
				.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).strip().replaceAll("\\s+", " ");
	}

	public ClientService getClients() {
		return clients;
	}

	public void setClients(ClientService clients) {
		this.clients = clients;
	}
}
