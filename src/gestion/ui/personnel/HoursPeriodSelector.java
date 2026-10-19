package gestion.ui.personnel;

import java.awt.BorderLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import gestion.ui.UIUtils;

public final class HoursPeriodSelector extends JPanel {
	private final JComboBox<String> mode;
	private final JLabel label = new JLabel("", JLabel.CENTER);
	private LocalDate date = LocalDate.now();

	public HoursPeriodSelector(boolean assigned, JPanel header, Runnable onChange) {
		super(new BorderLayout());
		String prefix = assigned ? "Heures actuellement attribuées" : "Heures restantes à attribuer";
		mode = new JComboBox<>(new String[] { prefix + " pour la semaine", prefix + " pour le mois" });
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
		LocalDate start = isMonthly() ? date.withDayOfMonth(1) : date.with(DayOfWeek.MONDAY);
		LocalDate end = isMonthly() ? start.plusMonths(1).minusDays(1) : start.plusDays(6);
		DateTimeFormatter shortFormat = DateTimeFormatter.ofPattern("dd/MM");
		DateTimeFormatter fullFormat = DateTimeFormatter.ofPattern("dd/MM/uuuu");
		label.setText(isMonthly() ? start.format(DateTimeFormatter.ofPattern("MMMM uuuu", Locale.FRENCH))
				: start.format(shortFormat) + " – " + end.format(shortFormat));
		label.setToolTipText(start.format(fullFormat) + " – " + end.format(fullFormat));
	}
}
