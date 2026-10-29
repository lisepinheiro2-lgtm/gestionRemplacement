package gestion.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import gestion.ui.raven.RavenDarkLightSwitch;

public final class ApplicationHeader extends JPanel implements AppThemeAware {

	private final JLabel brand = new JLabel("Gestion +");
	private final JLabel description = new JLabel("Organisation & remplacements");
	private final JLabel page = new JLabel("Accueil", JLabel.CENTER);
	private final JButton home;
	private final JButton account;
	private final RavenDarkLightSwitch theme = new RavenDarkLightSwitch();
	private final JPanel navigation = new JPanel(new java.awt.GridLayout(1, 6, 6, 0));
	private final Map<String, JButton> destinations = new LinkedHashMap<>();

	public ApplicationHeader(Component parent, JButton home, Image image) {
		super(new BorderLayout(16, 8));
		this.home = home;

		setBorder(BorderFactory.createEmptyBorder(14, 32, 14, 24));
		JPanel identity = new JPanel(new BorderLayout(12, 0));
		identity.setOpaque(false);
		JLabel logo = new JLabel(new ImageIcon(image.getScaledInstance(36, 36, Image.SCALE_SMOOTH)));
		JPanel words = new JPanel();
		words.setOpaque(false);
		words.setLayout(new BoxLayout(words, BoxLayout.Y_AXIS));
		brand.setFont(new Font("Segoe UI", Font.BOLD, 18));
		description.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		words.add(brand);
		words.add(Box.createVerticalStrut(2));
		words.add(description);
		identity.add(logo, BorderLayout.WEST);
		identity.add(words, BorderLayout.CENTER);
		page.setFont(new Font("Segoe UI", Font.BOLD, 12));
		JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
		controls.setOpaque(false);
		home.setPreferredSize(new Dimension(108, 36));
		theme.setPreferredSize(new Dimension(70, 36));
		theme.setSelected(UIUtils.isDarkMode());
		theme.addActionListener(event -> UIUtils.setDarkMode(theme.isSelected()));
		account = new JButton();
		account.setPreferredSize(new Dimension(38, 36));
		account.setEnabled(false);
		account.setFocusable(false);
		account.setToolTipText("Compte utilisateur — bientôt disponible");
		account.getAccessibleContext().setAccessibleName("Compte utilisateur");

		controls.add(theme);
		controls.add(account);
		add(identity, BorderLayout.WEST);
		JPanel center = new JPanel(new BorderLayout(0, 6));
		center.setOpaque(false);
		navigation.setOpaque(false);
		center.add(page, BorderLayout.CENTER);
		add(navigation, BorderLayout.SOUTH);
		add(center, BorderLayout.CENTER);
		add(controls, BorderLayout.EAST);
		addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent event) {
				if (getWidth() >= 1160 && navigation.getParent() != center) {
					remove(navigation);
					center.remove(page);
					center.add(page, BorderLayout.NORTH);
					center.add(navigation, BorderLayout.CENTER);
					page.setVisible("Interventions".equals(page.getText()) || "Accueil".equals(page.getText()));
					revalidate();
				} else if (getWidth() < 1160 && navigation.getParent() == center) {
					center.remove(navigation);
					center.add(page, BorderLayout.CENTER);
					page.setVisible(true);
					add(navigation, BorderLayout.SOUTH);
					revalidate();
				}
			}
		});
	}

	public void configureNavigation(Runnable employees, Runnable clients, Runnable planning, Runnable replacements) {
		navigation.add(home);
		destinations.put("Accueil", home);
		addDestination("Salariés", employees);
		addDestination("Clients", clients);
		addDestination("Planning", planning);
		addDestination("Remplacements", replacements);
		JButton documents = addDestination("Documents", null);
		documents.setEnabled(false);
		documents.setToolTipText("Documents — À venir");
		documents.getAccessibleContext().setAccessibleDescription("À venir");
		navigation.setVisible(false);
		applyAppTheme(UIUtils.isDarkMode());
	}

	private JButton addDestination(String title, Runnable action) {
		JButton button = new JButton(title);
		if (action != null)
			button.addActionListener(event -> action.run());
		destinations.put(title, button);
		navigation.add(button);
		return button;
	}

	public void setPageTitle(String title) {
		page.setText(title);
		navigation.setVisible(!"Accueil".equals(title));
		page.setVisible(getWidth() < 1160 || "Interventions".equals(title) || "Accueil".equals(title));
		revalidate();
		applyAppTheme(UIUtils.isDarkMode());
	}

	@Override
	public void applyAppTheme(boolean dark) {
		Color background = dark ? new Color(27, 35, 48) : Color.WHITE;
		Color foreground = dark ? new Color(238, 243, 251) : new Color(28, 42, 63);
		Color muted = dark ? new Color(159, 174, 194) : new Color(102, 118, 139);
		Color border = dark ? new Color(45, 57, 74) : new Color(224, 231, 240);

		setBackground(background);
		brand.setForeground(foreground);
		description.setForeground(muted);
		page.setForeground(muted);
		setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, border),
				BorderFactory.createEmptyBorder(14, 32, 14, 24)));
		theme.setSelected(dark);
		for (var entry : destinations.entrySet()) {
			JButton button = entry.getValue();
			boolean active = entry.getKey().equals(page.getText())
					|| entry.getKey().equals("Salariés") && page.getText().equals("Personnel");
			UIUtils.styleActionButton(button, active ? "primary" : "neutral");
			button.setFont(button.getFont().deriveFont(active ? Font.BOLD : Font.PLAIN, 12f));
			button.setMargin(new java.awt.Insets(6, 8, 6, 8));
			NavigationIcon.Kind kind = switch (entry.getKey()) {
			case "Accueil" -> NavigationIcon.Kind.HOME;
			case "Salariés" -> NavigationIcon.Kind.PEOPLE;
			case "Clients" -> NavigationIcon.Kind.CLIENTS;
			case "Planning" -> NavigationIcon.Kind.CALENDAR;
			case "Remplacements" -> NavigationIcon.Kind.REPLACEMENTS;
			default -> NavigationIcon.Kind.DOCUMENTS;
			};
			NavigationIcon icon = new NavigationIcon(kind, 18,
					active ? UIUtils.getTurquoiseColor() : foreground);
			button.setIcon(icon);
			button.setDisabledIcon(new NavigationIcon(kind, 18, muted));
		}

		NavigationIcon accountIcon = new NavigationIcon(NavigationIcon.Kind.USER, 20, muted);
		account.setIcon(accountIcon);
		account.setDisabledIcon(accountIcon);

		String style = "arc:12;borderWidth:0;focusWidth:1;innerFocusWidth:0;background:" + hex(background)
				+ ";foreground:" + hex(foreground) + ";hoverBackground:" + hex(border);


		account.putClientProperty("FlatLaf.style", style);
	}

	private static String hex(Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}
}
