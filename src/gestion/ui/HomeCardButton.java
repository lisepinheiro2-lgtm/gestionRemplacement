package gestion.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import gestion.ui.raven.RavenStyledButton;
import gestion.ui.raven.RavenTone;

public final class HomeCardButton extends RavenStyledButton implements AppThemeAware {
	private final JLabel heading;
	private final JLabel description;
	private final JLabel status;
	private final JLabel arrow = new JLabel();
	private final JLabel illustration = new JLabel();
	private final NavigationIcon.Kind kind;
	private final Color accent;
	private final boolean available;

	public HomeCardButton(String title, String subtitle, NavigationIcon.Kind kind, Color accent, boolean available) {
		this.kind = kind;
		this.accent = accent;
		this.available = available;
		setTone(RavenTone.NEUTRAL);
		setText("");
		setLayout(new BorderLayout(18, 0));
		setMargin(new Insets(18, 22, 18, 22));
		setPreferredSize(new Dimension(400, 144));
		setMinimumSize(new Dimension(270, 110));
		setCursor(Cursor.getPredefinedCursor(available ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
		setEnabled(available);
		setFocusable(available);
		getAccessibleContext().setAccessibleName(title);
		getAccessibleContext().setAccessibleDescription(available ? subtitle : "Archives : bientôt disponible");
		setToolTipText(available ? subtitle : "L'écran des archives sera disponible prochainement.");

		heading = new JLabel(title);
		heading.setFont(new Font("Segoe UI", Font.BOLD, 18));
		description = new JLabel(subtitle);
		description.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		status = new JLabel("À venir");
		status.setFont(new Font("Segoe UI", Font.BOLD, 11));
		JPanel text = new JPanel();
		text.setOpaque(false);
		text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
		heading.setAlignmentX(LEFT_ALIGNMENT);
		description.setAlignmentX(LEFT_ALIGNMENT);
		status.setAlignmentX(LEFT_ALIGNMENT);
		text.add(Box.createVerticalGlue());
		text.add(heading);
		text.add(Box.createVerticalStrut(7));
		text.add(description);
		if (!available) {
			text.add(Box.createVerticalStrut(4));
			text.add(status);
		}
		text.add(Box.createVerticalGlue());
		illustration.setVerticalAlignment(JLabel.CENTER);
		arrow.setVerticalAlignment(JLabel.CENTER);
		add(illustration, BorderLayout.WEST);
		add(text, BorderLayout.CENTER);
		add(arrow, BorderLayout.EAST);
	}

	@Override
	public void applyAppTheme(boolean dark) {
		Color surface = dark ? new Color(33, 42, 56) : Color.WHITE;
		Color line = dark ? new Color(51, 63, 80) : new Color(222, 229, 238);
		Color foreground = dark ? new Color(237, 242, 249) : new Color(30, 43, 63);
		Color muted = dark ? new Color(159, 174, 194) : new Color(102, 118, 139);
		Color hover = dark ? new Color(40, 52, 70) : new Color(245, 249, 255);
		putClientProperty("FlatLaf.style",
				"arc:22;margin:18,22,18,22;borderWidth:1;focusWidth:2;innerFocusWidth:0;" + "background:" + hex(surface)
						+ ";borderColor:" + hex(line) + ";hoverBackground:" + hex(hover) + ";hoverBorderColor:"
						+ hex(accent) + ";focusedBorderColor:" + hex(accent) + ";focusColor:" + hex(accent)
						+ ";disabledBackground:" + hex(surface) + ";disabledBorderColor:" + hex(line));
		heading.setForeground(available ? foreground : muted);
		description.setForeground(muted);
		status.setForeground(muted);
		arrow.setIcon(available ? new NavigationIcon(NavigationIcon.Kind.ARROW, 18, muted) : null);
		illustration.setIcon(new Icon() {
			public int getIconWidth() {
				return 54;
			}

			public int getIconHeight() {
				return 54;
			}

			public void paintIcon(Component c, Graphics graphics, int x, int y) {
				Graphics2D g = (Graphics2D) graphics.create();
				g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				Color tint = available ? accent : muted;
				g.setColor(new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), dark ? 40 : 22));
				g.fill(new RoundRectangle2D.Double(x, y, 54, 54, 17, 17));
				new NavigationIcon(kind, 27, tint).paintIcon(c, g, x + 14, y + 13);
				g.dispose();
			}
		});
	}

	private static String hex(Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}
}
