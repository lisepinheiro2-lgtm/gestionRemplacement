package gestion.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class HomePanel extends JPanel implements AppThemeAware {

	private final JLabel eyebrow = new JLabel("ESPACE DE TRAVAIL");
	private final JLabel title = new JLabel("Votre espace de gestion");
	private final JLabel subtitle = new JLabel("Retrouvez vos équipes, vos clients et vos interventions.");
	private final JLabel date = new JLabel(
			LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)));
	private final JPanel introduction = new JPanel(new BorderLayout(18, 0));
	private final JPanel heading = new JPanel();
	private final JPanel cards = new JPanel(new GridLayout(3, 2, 16, 16));
	private final HomeCardButton[] destinations;

	public HomePanel(Runnable employees, Runnable clients, Runnable planning, Runnable replacements,
			Runnable interventions) {
		super(new BorderLayout(0, 26));
		setBorder(BorderFactory.createEmptyBorder(28, 32, 30, 32));
		heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));
		heading.setOpaque(false);
		eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 11));
		title.setFont(new Font("Segoe UI", Font.BOLD, 29));
		subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		date.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		date.setVerticalAlignment(JLabel.TOP);
		heading.add(eyebrow);
		heading.add(Box.createVerticalStrut(9));
		heading.add(title);
		heading.add(Box.createVerticalStrut(8));
		heading.add(subtitle);
		introduction.setOpaque(false);
		introduction.add(heading, BorderLayout.CENTER);
		introduction.add(date, BorderLayout.EAST);
		cards.setOpaque(false);

		destinations = new HomeCardButton[] {
				card("Personnel", "Équipe, contrats et absences", NavigationIcon.Kind.PEOPLE, 0x5B94F3, employees),
				card("Clients", "Coordonnées et contrats", NavigationIcon.Kind.CLIENTS, 0x2BAAA0, clients),
				card("Planning", "Organisation des interventions", NavigationIcon.Kind.CALENDAR, 0x9A82E8, planning),
				card("Remplacements", "Demandes et attributions", NavigationIcon.Kind.REPLACEMENTS, 0xD99C49,
						replacements),
				card("Interventions", "Ponctuelles et récurrentes", NavigationIcon.Kind.INTERVENTIONS, 0x5D9EBE,
						interventions),
				card("Archives", "Historique des interventions", NavigationIcon.Kind.ARCHIVE, 0x91A0B6, null) };

		for (HomeCardButton button : destinations)
			cards.add(button);
		add(introduction, BorderLayout.NORTH);
		add(cards, BorderLayout.CENTER);
	}

	private static HomeCardButton card(String title, String subtitle, NavigationIcon.Kind icon, int color,
			Runnable action) {
		HomeCardButton button = new HomeCardButton(title, subtitle, icon, new Color(color), action != null);

		if (action != null)
			button.addActionListener(event -> action.run());
		return button;
	}

	@Override
	public void applyAppTheme(boolean dark) {
		setBackground(dark ? new Color(23, 30, 42) : new Color(244, 247, 251));
		title.setForeground(dark ? new Color(238, 243, 251) : new Color(28, 42, 63));
		eyebrow.setForeground(dark ? new Color(113, 167, 252) : new Color(55, 106, 192));
		Color muted = dark ? new Color(159, 174, 194) : new Color(102, 118, 139);
		subtitle.setForeground(muted);
		date.setForeground(muted);

		for (HomeCardButton button : destinations)
			button.applyAppTheme(dark);
	}
}
