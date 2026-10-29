package gestion.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class HomePanel extends JPanel implements AppThemeAware {

	private static final BufferedImage BANNER_BACKGROUND = loadBannerBackground();

	private final JLabel title = new JLabel("<html>Votre <b>espace de gestion</b></html>");
	private final JLabel subtitle = new JLabel("Retrouvez vos équipes, vos clients et vos interventions.");
	private final JLabel date = new JLabel(
			LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)));
	private final JLabel greeting = new JLabel("Bonne journée !");
	private final JLabel calendar = new JLabel();
	private final JPanel dateCard = new DateCard();
	private final JPanel introduction = new WelcomeBanner();
	private final JPanel heading = new JPanel();
	private final JPanel cards = new JPanel(new GridLayout(3, 2, 16, 16));
	private final HomeCardButton[] destinations;
	private double contentScale = 1;

	public HomePanel(Runnable employees, Runnable clients, Runnable planning, Runnable replacements,
			Runnable interventions) {
		super(new BorderLayout(0, 26));
		setBorder(BorderFactory.createEmptyBorder(28, 32, 30, 32));
		heading.setLayout(new BorderLayout(0, 7));
		heading.setOpaque(false);
		title.setFont(new Font("Segoe UI", Font.PLAIN, 24));
		subtitle.setText("<html>Retrouvez vos équipes, vos clients et vos interventions.</html>");
		subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
		subtitle.setVerticalAlignment(JLabel.TOP);
		date.setFont(new Font("Segoe UI", Font.BOLD, 14));
		greeting.setFont(new Font("Segoe UI", Font.PLAIN, 11));
		calendar.setHorizontalAlignment(JLabel.CENTER);
		calendar.setPreferredSize(new Dimension(40, 40));
		heading.add(title, BorderLayout.NORTH);
		heading.add(subtitle, BorderLayout.CENTER);

		JPanel dateText = new JPanel();
		dateText.setOpaque(false);
		dateText.setLayout(new BoxLayout(dateText, BoxLayout.Y_AXIS));
		dateText.add(Box.createVerticalGlue());
		dateText.add(date);
		dateText.add(Box.createVerticalStrut(4));
		dateText.add(greeting);
		dateText.add(Box.createVerticalGlue());
		dateCard.setLayout(new BorderLayout(12, 0));
		dateCard.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 14));
		dateCard.setOpaque(false);
		dateCard.add(calendar, BorderLayout.WEST);
		dateCard.add(dateText, BorderLayout.CENTER);
		introduction.setOpaque(false);
		introduction.add(heading);
		introduction.add(dateCard);
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
		applyAppTheme(UIUtils.isDarkMode());
	}

	private static HomeCardButton card(String title, String subtitle, NavigationIcon.Kind icon, int color,
			Runnable action) {
		HomeCardButton button = new HomeCardButton(title, subtitle, icon, new Color(color), action != null);

		if (action != null)
			button.addActionListener(event -> action.run());
		return button;
	}

	@Override
	public void doLayout() {
		double scale = Math.max(1, Math.min(getWidth() / 1366d, getHeight() / 680d));
		if (Math.abs(contentScale - scale) >= .001) {
			contentScale = scale;
			title.setFont(title.getFont().deriveFont((float) (24 * contentScale)));
			subtitle.setFont(subtitle.getFont().deriveFont((float) (12 * contentScale)));
			date.setFont(date.getFont().deriveFont((float) (14 * contentScale)));
			greeting.setFont(greeting.getFont().deriveFont((float) (11 * contentScale)));
			((BoxLayout) date.getParent().getLayout()).invalidateLayout(date.getParent());
			calendar.setPreferredSize(new Dimension(scaled(40), scaled(40)));
			calendar.setIcon(new NavigationIcon(NavigationIcon.Kind.CALENDAR, scaled(24), new Color(105, 236, 243)));
			((BorderLayout) heading.getLayout()).setVgap(scaled(7));
			((BorderLayout) dateCard.getLayout()).setHgap(scaled(12));
			dateCard.setBorder(BorderFactory.createEmptyBorder(scaled(10), scaled(12), scaled(10), scaled(14)));
			for (HomeCardButton button : destinations)
				button.setContentScale(contentScale);
		}
		boolean shortWindow = getHeight() > 0 && getHeight() < 580;
		java.awt.Insets padding = shortWindow ? new java.awt.Insets(16, 24, 16, 24)
				: new java.awt.Insets(scaled(28), scaled(32), scaled(30), scaled(32));
		if (!getInsets().equals(padding))
			setBorder(BorderFactory.createEmptyBorder(padding.top, padding.left, padding.bottom, padding.right));
		((BorderLayout) getLayout()).setVgap(shortWindow ? 16 : scaled(26));
		((GridLayout) cards.getLayout()).setVgap(shortWindow ? 12 : scaled(16));
		((GridLayout) cards.getLayout()).setHgap(scaled(16));
		super.doLayout();
		// Bound extreme aspect ratios without stretching cards independently of their content.
		cards.setSize(cards.getWidth(), Math.max(0, Math.min(cards.getHeight(),
				3 * scaled(176) + 2 * ((GridLayout) cards.getLayout()).getVgap())));
	}

	private int scaled(int value) {
		return (int) Math.round(value * contentScale);
	}

	@Override
	public void applyAppTheme(boolean dark) {
		setBackground(dark ? new Color(23, 30, 42) : new Color(244, 247, 251));
		Color foreground = new Color(238, 243, 251);
		Color muted = new Color(178, 199, 221);
		title.setForeground(foreground);
		subtitle.setForeground(muted);
		date.setForeground(foreground);
		greeting.setForeground(muted);
		calendar.setIcon(new NavigationIcon(NavigationIcon.Kind.CALENDAR, scaled(24), new Color(105, 236, 243)));
		introduction.repaint();

		for (HomeCardButton button : destinations)
			button.applyAppTheme(dark);
	}

	private static BufferedImage loadBannerBackground() {
		try (var stream = HomePanel.class.getResourceAsStream("/icons/home-banner-background.png")) {
			return stream == null ? null : ImageIO.read(stream);
		} catch (IOException failure) {
			return null;
		}
	}

	private static Color transparent(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}

	private final class WelcomeBanner extends JPanel {

		WelcomeBanner() {
			super(null);
		}

		@Override
		public Dimension getPreferredSize() {
			int width = HomePanel.this.getWidth() - HomePanel.this.getInsets().left
					- HomePanel.this.getInsets().right;
			boolean shortWindow = HomePanel.this.getHeight() > 0 && HomePanel.this.getHeight() < 580;
			return new Dimension(0, width > 0 && width < scaled(650) ? scaled(152) : shortWindow ? 88 : scaled(104));
		}

		@Override
		public void doLayout() {
			int width = getWidth();
			int cardWidth = Math.min(Math.max(scaled(224), date.getPreferredSize().width + scaled(86)),
					Math.max(0, width - scaled(48)));
			boolean compact = width < scaled(650);
			int headingWidth = Math.max(0, compact ? width - scaled(48) : width - cardWidth - scaled(76));
			heading.setBounds(scaled(24),
					compact ? scaled(25) : Math.max(scaled(22), (getHeight() - scaled(58)) / 2 + scaled(3)),
					headingWidth, scaled(58));
			int cardHeight = compact ? scaled(60) : Math.min(scaled(68), getHeight() - scaled(20));
			dateCard.setBounds(compact ? scaled(24) : width - cardWidth - scaled(20),
					compact ? scaled(80) : (getHeight() - cardHeight) / 2, cardWidth, cardHeight);
		}

		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 16, 16);
			g.clip(shape);
			g.setColor(new Color(5, 23, 43));
			g.fill(shape);
			if (BANNER_BACKGROUND != null) {
				int sourceWidth = BANNER_BACKGROUND.getWidth();
				int sourceHeight = BANNER_BACKGROUND.getHeight();
				int sourceLeft = Math.round(sourceWidth * .35f);
				int sourceRight = Math.round(sourceWidth * .90f);
				int left = getWidth() / 2;
				int right = Math.min(getWidth(), left + Math.round((sourceRight - sourceLeft)
						* getHeight() / (float) sourceHeight * 1.4f));
				g.drawImage(BANNER_BACKGROUND, 0, 0, left, getHeight(), 0, 0, sourceLeft, sourceHeight, this);
				g.drawImage(BANNER_BACKGROUND, left, 0, right, getHeight(), sourceLeft, 0, sourceRight, sourceHeight, this);
				g.drawImage(BANNER_BACKGROUND, right, 0, getWidth(), getHeight(), sourceRight, 0, sourceWidth, sourceHeight, this);
			}
			g.setColor(new Color(23, 30, 42, 65));
			g.fill(shape);
			g.setColor(new Color(0, 217, 237));
			g.fillRoundRect(scaled(24), heading.getY() - scaled(12), scaled(24), scaled(4), scaled(4), scaled(4));
			g.dispose();
		}
	}

	private final class DateCard extends JPanel {

		@Override
		protected void paintComponent(Graphics graphics) {
			super.paintComponent(graphics);
			Graphics2D g = (Graphics2D) graphics.create();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			Color cyan = new Color(0, 217, 237);
			RoundRectangle2D shape = new RoundRectangle2D.Float(.5f, .5f, getWidth() - 1, getHeight() - 1, 14, 14);
			g.setPaint(new GradientPaint(0, 0, new Color(5, 35, 57, 220), getWidth(), getHeight(),
					new Color(9, 91, 113, 190)));
			g.fill(shape);
			g.setColor(transparent(cyan, 105));
			g.draw(shape);
			g.setColor(transparent(cyan, 35));
			g.fillRoundRect(scaled(12), (getHeight() - scaled(40)) / 2, scaled(40), scaled(40), scaled(10), scaled(10));
			g.dispose();
		}
	}
}
