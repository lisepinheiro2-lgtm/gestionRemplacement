package gestion.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Image;
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

	public ApplicationHeader(Component parent, JButton home, Image image) {
		super(new BorderLayout(20, 0));
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
		controls.add(home);
		controls.add(theme);
		controls.add(account);
		add(identity, BorderLayout.WEST);
		add(page, BorderLayout.CENTER);
		add(controls, BorderLayout.EAST);
	}

	public void setPageTitle(String title) {
		page.setText(title);
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
		home.setIcon(new NavigationIcon(NavigationIcon.Kind.HOME, 16, foreground));
		NavigationIcon accountIcon = new NavigationIcon(NavigationIcon.Kind.USER, 20, muted);
		account.setIcon(accountIcon);
		account.setDisabledIcon(accountIcon);

		String style = "arc:12;borderWidth:0;focusWidth:1;innerFocusWidth:0;background:" + hex(background)
				+ ";foreground:" + hex(foreground) + ";hoverBackground:" + hex(border);

		home.putClientProperty("FlatLaf.style", style);
		account.putClientProperty("FlatLaf.style", style);
	}

	private static String hex(Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}
}
