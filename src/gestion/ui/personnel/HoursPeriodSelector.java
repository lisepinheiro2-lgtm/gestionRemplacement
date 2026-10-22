package gestion.ui.personnel;

import java.awt.BorderLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import gestion.model.DateRange;
import gestion.ui.UIUtils;

public final class HoursPeriodSelector extends JPanel {
	private final JComboBox<String> mode;
	private final JLabel label = new JLabel("", JLabel.CENTER);
	private LocalDate date = LocalDate.now();

	public HoursPeriodSelector(JPanel header, Runnable onChange) {
		
		super(new BorderLayout());
		mode = new JComboBox<>(new String[] { "Semaine", "Mois" });
		DefaultListCellRenderer renderer = new DefaultListCellRenderer();

		JPanel navigation = new JPanel(new BorderLayout());
		JButton previous = new JButton("◀");
		JButton next = new JButton("▶");
		
		previous.setToolTipText("Période précédente");
		next.setToolTipText("Période suivante");
		
		navigation.add(previous, BorderLayout.WEST);
		navigation.add(label, BorderLayout.CENTER);
		navigation.add(next, BorderLayout.EAST);
		add(mode, BorderLayout.CENTER);
		add(navigation, BorderLayout.SOUTH);
		
		UIUtils.configureResponsiveComboBox(mode, header);
		renderer.setHorizontalAlignment(JLabel.CENTER);
		label.setFont(label.getFont().deriveFont(label.getFont().getSize2D() + 2f));
		mode.setRenderer(renderer);
		mode.setFont(mode.getFont().deriveFont(mode.getFont().getSize2D() + 2f));
		previous.addActionListener(event -> {
			move(-1);
			onChange.run();
		});
		
		next.addActionListener(event -> {
			move(1);
			onChange.run();
		});
		
		mode.addActionListener(event -> {
			updateLabel();
			onChange.run();
		});
		
		updateLabel();
	}

	public LocalDate getDate() {
		return date;
	}

	public boolean isMonthly() {
		return mode.getSelectedIndex() == 1;
	}

	private void move(int direction) {
		date = isMonthly() ? date.withDayOfMonth(1).plusMonths(direction) : date.plusWeeks(direction);
		updateLabel();
	}

	private void updateLabel() {

		DateRange period = getSelectedPeriod();
		LocalDate start = period.start();
		LocalDate end = period.end();
		DateTimeFormatter shortFormat = DateTimeFormatter.ofPattern("dd/MM");
		DateTimeFormatter fullFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu");
		label.setText(isMonthly() ? start.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.FRENCH))
				: start.format(shortFormat) + " – " + end.format(shortFormat));
		label.setToolTipText(start.format(fullFormat) + " – " + end.format(fullFormat));

	}

	public DateRange getSelectedPeriod() {

		LocalDate start = isMonthly() ? date.withDayOfMonth(1) : date.with(DayOfWeek.MONDAY);
		LocalDate end = isMonthly() ? start.plusMonths(1).minusDays(1) : start.plusDays(6);

		return new DateRange(start, end);
	}
}
