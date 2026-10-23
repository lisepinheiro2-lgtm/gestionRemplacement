package gestion.ui.personnel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

import gestion.model.Employee;
import gestion.ui.UIUtils;
import gestion.ui.personnel.AssignmentImpactPanel.Impact;

final class AssignmentImpactGroups extends JPanel {

	private static final int WEEKS_PER_PAGE = 4;
	private static final int MAX_DISPLAYED_WEEKS = 8;

	private final JPanel cards = new JPanel();
	private final ForecastContent content = new ForecastContent();
	private final JScrollPane scroll = new JScrollPane(content, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
	private final JLabel weekPosition = new JLabel();
	private final JLabel weekRange = new JLabel();
	private final JButton previousWeeks = createArrow("‹", "Quatre semaines précédentes");
	private final JButton nextWeeks = createArrow("›", "Quatre semaines suivantes");
	private final JPanel weekNavigation = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
	private List<LocalDate> weeks = List.of();
	private LocalDate weekAnchor;
	private int weekPage;

	private final JPanel navigation = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
	private final JLabel count = new JLabel();
	private final JLabel position = new JLabel();
	private final JButton previous = createArrow("‹", "Salariés précédents");
	private final JButton next = createArrow("›", "Salariés suivants");
	private final List<AssignmentImpactCard> others = new ArrayList<>();
	private AssignmentImpactCard recipient;
	private int page;

	AssignmentImpactGroups(int height, JLabel title) {
		super(new BorderLayout());
		setPreferredSize(new Dimension(0, height));
		JPanel toolbar = new JPanel(new CenteredTitleLayout());
		count.setFont(count.getFont().deriveFont(12f));
		position.setFont(position.getFont().deriveFont(12f));
		weekPosition.setFont(weekPosition.getFont().deriveFont(12f));
		weekRange.setFont(weekRange.getFont().deriveFont(Font.BOLD, 13f));
		weekRange.setHorizontalAlignment(JLabel.CENTER);
		title.setHorizontalAlignment(JLabel.CENTER);
		count.setHorizontalAlignment(JLabel.RIGHT);

		weekNavigation.add(previousWeeks);
		weekNavigation.add(weekPosition);
		weekNavigation.add(nextWeeks);
		toolbar.add(weekNavigation, BorderLayout.WEST);
		toolbar.add(title, BorderLayout.CENTER);
		navigation.add(position);
		navigation.add(previous);
		navigation.add(next);
		toolbar.add(navigation, BorderLayout.EAST);

		JPanel details = new JPanel(new BorderLayout(8, 0));
		details.add(weekRange, BorderLayout.WEST);
		details.add(count, BorderLayout.EAST);
		JPanel header = new JPanel(new BorderLayout(0, 1));
		header.add(toolbar, BorderLayout.NORTH);
		header.add(details, BorderLayout.CENTER);
		content.block.add(header, BorderLayout.NORTH);
		content.block.add(cards, BorderLayout.CENTER);
		scroll.setBorder(null);
		scroll.getVerticalScrollBar().setUnitIncrement(28);
		add(scroll, BorderLayout.CENTER);

		previous.addActionListener(event -> changePage(-1));
		next.addActionListener(event -> changePage(1));
		previousWeeks.addActionListener(event -> changeWeeks(-1));
		nextWeeks.addActionListener(event -> changeWeeks(1));
	}

	void showImpact(List<Impact> impacts, Employee destination) {
		Map<Employee, List<Impact>> employees = new LinkedHashMap<>();
		impacts.forEach(
				impact -> employees.computeIfAbsent(impact.employee(), ignored -> new ArrayList<>()).add(impact));
		List<Impact> recipientWeeks = employees.remove(destination);
		recipient = recipientWeeks == null ? null : new AssignmentImpactCard(destination, recipientWeeks, true);
		others.clear();
		employees.forEach((employee, values) -> others.add(new AssignmentImpactCard(employee, values, false)));

		TreeSet<LocalDate> impactedWeeks = new TreeSet<>(impacts.stream().map(Impact::date).toList());
		weeks = impactedWeeks.stream().limit(MAX_DISPLAYED_WEEKS).toList();
		weekPosition.setToolTipText(impactedWeeks.size() > MAX_DISPLAYED_WEEKS
				? "Affichage limité aux huit premières semaines. L'opération conserve toutes les semaines concernées."
				: null);

		if (!impacts.isEmpty()) {
			page = Math.min(page, Math.max(0, (others.size() - 1) / 2));
			int index = 0;

			while (index + 1 < weeks.size() && weekAnchor != null && weeks.get(index).isBefore(weekAnchor))
				index++;

			weekPage = index / WEEKS_PER_PAGE;
			weekAnchor = weeks.get(weekPage * WEEKS_PER_PAGE);
		}

		showPage();
	}

	private JButton createArrow(String text, String description) {
		JButton button = new JButton(text);
		UIUtils.styleRoundedButton(button);
		button.setFont(button.getFont().deriveFont(Font.PLAIN, 20f));
		button.setMargin(new Insets(0, 0, 0, 0));
		button.setPreferredSize(new Dimension(28, 26));
		button.setToolTipText(description);
		button.getAccessibleContext().setAccessibleName(description);
		return button;
	}

	private void changePage(int direction) {
		int requested = page + direction;

		if (requested < 0 || requested * 2 >= relevantEmployees(visibleWeeks()).size())
			return;

		page = requested;
		showPage();
	}

	private void changeWeeks(int direction) {
		int requested = weekPage + direction;

		if (requested < 0 || requested * WEEKS_PER_PAGE >= weeks.size())
			return;

		weekPage = requested;
		weekAnchor = weeks.get(weekPage * WEEKS_PER_PAGE);
		showPage();
		scroll.getVerticalScrollBar().setValue(0);
	}

	private List<LocalDate> visibleWeeks() {
		int start = Math.min(weekPage * WEEKS_PER_PAGE, weeks.size());
		return weeks.subList(start, Math.min(start + WEEKS_PER_PAGE, weeks.size()));
	}

	private List<AssignmentImpactCard> relevantEmployees(List<LocalDate> visible) {
		return others.stream().filter(card -> card.hasChanges(visible)).toList();
	}

	private void addCard(AssignmentImpactCard card, List<LocalDate> visible) {
		card.showWeeks(visible);
		JPanel column = new JPanel(new BorderLayout());
		column.setOpaque(false);
		column.add(card, BorderLayout.NORTH);
		cards.add(column);
	}

	private void updateWeekNavigation(List<LocalDate> visible) {
		weekNavigation.setVisible(!visible.isEmpty());
		weekRange.setVisible(!visible.isEmpty());
		previousWeeks.setVisible(weeks.size() > WEEKS_PER_PAGE);
		nextWeeks.setVisible(weeks.size() > WEEKS_PER_PAGE);
		previousWeeks.setEnabled(weekPage > 0);
		nextWeeks.setEnabled((weekPage + 1) * WEEKS_PER_PAGE < weeks.size());

		if (visible.isEmpty())
			return;

		int start = weekPage * WEEKS_PER_PAGE;
		weekPosition.setText((start + visible.size()) + " " + (start + visible.size() == 1 ? "semaine" : "semaines")
				+ " sur " + weeks.size());
		LocalDate first = visible.getFirst();
		LocalDate last = visible.getLast();
		DateTimeFormatter format = DateTimeFormatter
				.ofPattern(first.getYear() == last.getYear() ? "dd/MM" : "dd/MM/uuuu");
		weekRange.setText(first.format(format) + (first.equals(last) ? "" : " – " + last.format(format)));
		weekRange.setPreferredSize(new Dimension(weekNavigation.getPreferredSize().width,
				weekRange.getFontMetrics(weekRange.getFont()).getHeight()));
		weekRange.setToolTipText(visible.stream().map(date -> date.format(DateTimeFormatter.ofPattern("dd/MM/uuuu")))
				.collect(java.util.stream.Collectors.joining(" · ")));
	}

	private void showPage() {
		cards.removeAll();
		List<LocalDate> visible = visibleWeeks();
		List<AssignmentImpactCard> relevant = relevantEmployees(visible);

		if (!visible.isEmpty())
			page = Math.min(page, Math.max(0, (relevant.size() - 1) / 2));

		int start = Math.min(page * 2, relevant.size());
		int end = Math.min(start + 2, relevant.size());
		int total = others.size() + (recipient == null ? 0 : 1);
		int columns = Math.max(1, Math.min(recipient == null ? 2 : 3, total));
		cards.setLayout(new GridLayout(1, columns, 8, 0));

		if (recipient != null)
			addCard(recipient, visible);

		for (int index = start; index < end; index++)
			addCard(relevant.get(index), visible);

		updateWeekNavigation(visible);

		while (cards.getComponentCount() < columns)
			cards.add(Box.createHorizontalStrut(0));

		int changed = relevant.size() + (recipient != null && recipient.hasChanges(visible) ? 1 : 0);
		count.setText(changed + " salarié(s) impacté(s)");
		count.setToolTipText(total + " salarié(s) concernés sur l'ensemble des semaines.");
		navigation.setVisible(relevant.size() > 2);
		position.setText("Autres salariés : " + end + " sur " + relevant.size());
		previous.setEnabled(start > 0);
		next.setEnabled(end < relevant.size());
		UIUtils.applyTheme(this);
		revalidate();
		repaint();
	}

	private static final class CenteredTitleLayout extends BorderLayout {

		CenteredTitleLayout() {
			super(8, 0);
		}

		@Override
		public void layoutContainer(Container parent) {
			super.layoutContainer(parent);
			Component title = getLayoutComponent(parent, BorderLayout.CENTER);

			if (title == null)
				return;

			Insets insets = parent.getInsets();
			int margin = Math.max(sideWidth(getLayoutComponent(parent, BorderLayout.WEST)),
					sideWidth(getLayoutComponent(parent, BorderLayout.EAST)));
			int width = Math.max(0, parent.getWidth() - insets.left - insets.right - 2 * margin);

			title.setBounds(insets.left + margin, title.getY(), width, title.getHeight());
		}

		private int sideWidth(Component component) {
			return component != null && component.isVisible() ? component.getWidth() + getHgap() : 0;
		}
	}

	private static final class ForecastContent extends JPanel implements Scrollable {

		private final JPanel block = new JPanel(new BorderLayout(0, 4));

		ForecastContent() {
			super(new BorderLayout());
			add(block, BorderLayout.CENTER);
		}

		@Override
		public void doLayout() {
			Insets insets = getInsets();
			int available = Math.max(0, getHeight() - insets.top - insets.bottom);
			int natural = Math.max(0, getPreferredSize().height - insets.top - insets.bottom);
			int height = Math.min(available, natural);
			int top = insets.top + (available - height) / 2;

			int width = Math.max(0, getWidth() - insets.left - insets.right);
			block.setBounds(insets.left, top, width, height);
		}

		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
			return 28;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
			return Math.max(28, visible.height - 28);
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return getParent() != null && getParent().getHeight() >= getPreferredSize().height;
		}
	}
}
