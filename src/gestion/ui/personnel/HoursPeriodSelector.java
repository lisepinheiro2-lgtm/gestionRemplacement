package gestion.ui.personnel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.formdev.flatlaf.ui.FlatComboBoxUI;

import gestion.model.DateRange;
import gestion.ui.AppThemeAware;
import gestion.ui.UIUtils;

public final class HoursPeriodSelector extends JPanel implements AppThemeAware {
	private final JComboBox<String> mode;
	private final JLabel label = new JLabel("", JLabel.CENTER);
	private final JPanel navigation = new JPanel(new BorderLayout());
	private LocalDate date = LocalDate.now();

	public HoursPeriodSelector(JPanel header, Runnable onChange) {
		
		super(new BorderLayout(0, 4));
		mode = new JComboBox<>(new String[] { "Semaine", "Mois" });
		mode.putClientProperty("JComponent.roundRect", true);
		mode.putClientProperty("FlatLaf.style", "buttonBackground:#1B2330;buttonFocusedBackground:#2D394A;"
				+ "focusedBackground:#1B2330;buttonArrowColor:#EEF3FB;buttonHoverArrowColor:#FFFFFF;"
				+ "buttonPressedArrowColor:#EEF3FB;[dark]borderColor:#3D4C61;[light]borderColor:#1B2330");
		DefaultListCellRenderer renderer = new DefaultListCellRenderer();

		JButton previous = createNavigationButton("◀");
		JButton next = createNavigationButton("▶");
		
		previous.setToolTipText("Période précédente");
		next.setToolTipText("Période suivante");
		
		navigation.add(previous, BorderLayout.WEST);
		navigation.add(label, BorderLayout.CENTER);
		navigation.add(next, BorderLayout.EAST);
		add(mode, BorderLayout.CENTER);
		add(navigation, BorderLayout.SOUTH);
		
		UIUtils.configureResponsiveComboBox(mode, header);
		renderer.setHorizontalAlignment(JLabel.CENTER);
		label.setFont(label.getFont().deriveFont(label.getFont().getSize2D() + 1f));
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
		applyAppTheme(UIUtils.isDarkMode());
	}

	private JButton createNavigationButton(String text) {
		JButton button = new PersonnelPanel.PersonnelActionButton(text);
		button.putClientProperty("JButton.buttonType", "roundRect");
		button.setMargin(new Insets(1, 5, 1, 5));
		return button;
	}

	@Override
	public void applyAppTheme(boolean dark) {
		setBackground(UIUtils.getBackgroundColor());

		if (!(mode.getUI() instanceof FlatComboBoxUI))
			mode.setUI(new FlatComboBoxUI());

		mode.setBackground(new Color(0x1B2330));
		mode.setForeground(new Color(0xEEF3FB));
		UIUtils.applyTheme(navigation);
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
