package gestion.ui.personnel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import gestion.model.Employee;
import gestion.ui.AppThemeAware;
import gestion.ui.UIUtils;
import gestion.ui.personnel.AssignmentImpactPanel.Impact;
import gestion.util.FormatUtils;

final class AssignmentImpactCard extends JPanel implements AppThemeAware {

	private final JPanel heading = new JPanel(new BorderLayout(0, 1));
	private final JLabel role;
	private final JPanel rows = new JPanel();
	private final Map<LocalDate, Impact> impacts = new LinkedHashMap<>();
	private List<LocalDate> displayedWeeks;

	AssignmentImpactCard(Employee employee, List<Impact> weeks, boolean recipient) {

		super(new BorderLayout(0, 2));
		setOpaque(false);
		setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

		heading.setOpaque(false);

		JLabel name = new JLabel(employee.toString());
		name.setFont(name.getFont().deriveFont(Font.BOLD, 14f));
		name.setHorizontalAlignment(JLabel.RIGHT);
		name.setToolTipText(employee.toString());

		role = new JLabel(recipient ? "Destinataire · fixe" : "Salarié concerné");

		role.setFont(role.getFont().deriveFont(12f));
		role.setHorizontalAlignment(JLabel.RIGHT);

		heading.add(role, BorderLayout.NORTH);
		heading.add(name, BorderLayout.SOUTH);

		add(heading, BorderLayout.NORTH);

		weeks.forEach(impact -> impacts.put(impact.date(), impact));

		rows.setOpaque(false);
		rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));

		add(rows, BorderLayout.CENTER);
	}

	boolean hasChanges(List<LocalDate> weeks) {
		return weeks.stream().map(impacts::get).anyMatch(AssignmentImpactCard::changesHours);
	}

	private static boolean changesHours(Impact impact) {
		return impact != null && Math.round((impact.after() - impact.before()) * 60) != 0;
	}

	void showWeeks(List<LocalDate> weeks) {
		if (weeks.equals(displayedWeeks))
			return;
		displayedWeeks = List.copyOf(weeks);
		rows.removeAll();
		weeks.stream().map(impacts::get).filter(AssignmentImpactCard::changesHours)
				.forEach(impact -> rows.add(new WeekRow(impact)));
		UIUtils.applyTheme(rows);
		rows.revalidate();
		rows.repaint();
	}

	@Override
	public void applyAppTheme(boolean dark) {
		UIUtils.applyTheme(heading);
		role.setForeground(UIUtils.getPlaceholderColor());
		UIUtils.applyTheme(rows);
		repaint();
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics.create();

		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(UIUtils.getSurfaceColor());
			g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
			g.setColor(UIUtils.getBorderColor());
			g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
		} finally {
			g.dispose();
		}
		super.paintComponent(graphics);
	}

	private static String hex(Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	private static String formatWeek(LocalDate start) {
		LocalDate end = start.plusDays(6);
		DateTimeFormatter month = DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH);
		DateTimeFormatter year = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.FRENCH);

		if (start.getYear() != end.getYear())
			return start.format(year) + " – " + end.format(year);

		if (start.getMonth() != end.getMonth())
			return start.format(month) + " – " + end.format(month);

		return start.getDayOfMonth() + " – " + end.format(month);
	}

	private static final class WeekRow extends JPanel implements AppThemeAware {

		private final Impact impact;
		private final JPanel gaugeLine = new JPanel(new BorderLayout(8, 0));
		private final JLabel date;
		private final JLabel variation;
		private final JLabel hours = new JLabel();

		WeekRow(Impact impact) {
			super(new BorderLayout());
			this.impact = impact;

			setOpaque(false);
			setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));

			gaugeLine.setOpaque(false);

			DateTimeFormatter fullDate = DateTimeFormatter.ofPattern("dd/MM/uuuu");
			date = new JLabel(formatWeek(impact.date()));

			date.setFont(date.getFont().deriveFont(Font.BOLD, 12f));
			date.setToolTipText(impact.date().format(fullDate) + " – " + impact.date().plusDays(6).format(fullDate));

			variation = new JLabel(AssignmentImpactPanel.variation(impact.after() - impact.before()));
			variation.setFont(variation.getFont().deriveFont(Font.BOLD, 12f));
			variation.setVerticalAlignment(JLabel.CENTER);

			hours.setFont(hours.getFont().deriveFont(11.5f));
			hours.setToolTipText(UIUtils.getHoursLegend());

			gaugeLine.add(new HoursGauge(impact), BorderLayout.CENTER);
			gaugeLine.add(variation, BorderLayout.EAST);

			add(date, BorderLayout.NORTH);
			add(hours, BorderLayout.CENTER);
			add(gaugeLine, BorderLayout.SOUTH);

			applyAppTheme(UIUtils.isDarkMode());

			int height = date.getPreferredSize().height + hours.getFontMetrics(hours.getFont()).getHeight()
					+ gaugeLine.getPreferredSize().height + getInsets().top + getInsets().bottom;
			setPreferredSize(new Dimension(0, height));
			setMinimumSize(new Dimension(0, height));
			setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
		}

		@Override
		public void applyAppTheme(boolean dark) {
			UIUtils.applyTheme(date);
			UIUtils.applyTheme(gaugeLine);

			Color state = UIUtils.getHoursColor(impact.alert());
			Color readable = state;
			hours.setText("<html><font color='" + hex(UIUtils.getPlaceholderColor()) + "'>"
					+ FormatUtils.formatContractHours(impact.before()) + " → </font><b><font color='" + hex(readable)
					+ "'>" + FormatUtils.formatContractHours(impact.after()) + " / "
					+ FormatUtils.formatContractHours(impact.contract()) + "</font></b></html>");

			if (Math.round((impact.after() - impact.before()) * 60) != 0)
				variation.setForeground(impact.after() > impact.before() ? UIUtils.getTurquoiseColor() : UIUtils.getVioletColor());
		}
	}

	private static final class HoursGauge extends JComponent {

		private final Impact impact;

		HoursGauge(Impact impact) {
			this.impact = impact;
			
			setPreferredSize(new Dimension(0, 8));
			String percentage = impact.contract() > 0 ? Math.round(impact.after() * 100 / impact.contract()) + " %"
					: impact.after() > 0 ? "Hors contrat" : "0 %";
			setToolTipText(percentage + " · " + FormatUtils.formatContractHours(impact.after()) + " / "
					+ FormatUtils.formatContractHours(impact.contract())
					+ ". Le repère indique le contrat en dépassement.");
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			Graphics2D g = (Graphics2D) graphics.create();

			try {
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				int height = Math.min(7, getHeight());
				int availableWidth = getParent() == null ? getWidth() : getParent().getWidth();
				int width = Math.min(getWidth(), Math.min(200, (int) Math.round(availableWidth * 0.62)));
				int y = (getHeight() - height) / 2;
				RoundRectangle2D track = new RoundRectangle2D.Double(0, y, width, height, height, height);
				g.setColor(UIUtils.isDarkMode() ? new Color(68, 74, 84) : new Color(228, 233, 239));
				g.fill(track);
				double maximum = Math.max(impact.contract(), impact.after());

				if (maximum <= 0)
					return;

				g.clip(track);
				g.setColor(UIUtils.getHoursColor(impact.alert()));
				g.fillRoundRect(0, y, (int) Math.round(width * impact.after() / maximum), height, height, height);

				if (impact.after() > impact.contract() && impact.contract() > 0) {
					int limit = (int) Math.round(width * impact.contract() / maximum);
					g.setColor(UIUtils.isDarkMode() ? new Color(32, 35, 40) : Color.WHITE);
					g.fillRect(limit - 1, y, 3, height);
				}
			} finally {
				g.dispose();
			}
		}
	}
}
