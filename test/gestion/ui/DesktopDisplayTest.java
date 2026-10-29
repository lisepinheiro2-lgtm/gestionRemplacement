package gestion.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import org.junit.jupiter.api.Test;

class DesktopDisplayTest {

	@Test
	void badgeBackgroundMatchesOtherCellsAcrossThemesAndSelection() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			boolean originalTheme = UIUtils.isDarkMode();
			try {
				for (boolean dark : new boolean[] { false, true }) {
					UIUtils.setDarkMode(dark);
					UIUtils.installLookAndFeel();
					JTable table = new JTable(new Object[][] { { "A" }, { "B" } }, new Object[] { "Statut" });
					UIUtils.applyTheme(table);
					BadgeRenderer badge = new BadgeRenderer();
					DefaultTableCellRenderer standard = new DefaultTableCellRenderer();
					for (boolean selected : new boolean[] { false, true, false }) {
						for (int row = 0; row < 2; row++) {
							Component reference = standard.getTableCellRendererComponent(table, "Texte", selected, false, row, 0);
							Component cell = badge.getTableCellRendererComponent(table, "Opérationnel", selected, false, row, 0);
							cell.setSize(500, 28);
							BufferedImage image = new BufferedImage(500, 28, BufferedImage.TYPE_INT_ARGB);
							Graphics2D graphics = image.createGraphics();
							cell.paint(graphics);
							graphics.dispose();
							assertEquals(reference.getBackground().getRGB(), image.getRGB(2, 2),
									"Badge background: dark=" + dark + ", row=" + row + ", selected=" + selected);
						}
					}
				}
			} finally {
				UIUtils.setDarkMode(originalTheme);
			}
		});
	}

	@Test
	void homeScalesItsContentWithTheWindowAndKeepsTwoColumnsAtTheTop() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			HomePanel home = new HomePanel(() -> {}, () -> {}, () -> {}, () -> {}, () -> {});
			for (Dimension size : new Dimension[] { new Dimension(760, 550), new Dimension(1366, 680),
					new Dimension(2554, 1250), new Dimension(2554, 600), new Dimension(3840, 2000), new Dimension(760, 550) }) {
				home.setSize(size);
				layoutTree(home);
				assertLabelsFit(home);
				JPanel cards = (JPanel) home.getComponent(1);
				int availableWidth = size.width - home.getInsets().left - home.getInsets().right;
				assertEquals(availableWidth, cards.getWidth());
				assertTrue(Math.abs(cards.getX() - (size.width - cards.getX() - cards.getWidth())) <= 1);
				assertTrue(cards.getY() >= home.getComponent(0).getY() + home.getComponent(0).getHeight());
				assertTrue(cards.getY() + cards.getHeight() <= size.height - home.getInsets().bottom);
				assertEquals(home.getInsets().top, home.getComponent(0).getY(), "No large empty space above the banner");
				assertEquals(2, ((GridLayout) cards.getLayout()).getColumns());
				assertEquals(3, ((GridLayout) cards.getLayout()).getRows());
				for (Component card : cards.getComponents()) {
					JLabel heading = findHeading((Container) card);
					assertTrue(card.getHeight() >= 110);
					assertTrue(card.getHeight() / heading.getFont().getSize2D() <= 11,
							"Card content must grow with the card instead of remaining tiny");
					if (size.width >= 2500 && size.height >= 1200)
						assertTrue(heading.getFont().getSize2D() >= 30);
					if (size.width == 760)
						assertEquals(18f, heading.getFont().getSize2D(), "Restore the small-window font after resizing down");
					assertTrue(card.getX() + card.getWidth() <= cards.getWidth());
					assertTrue(card.getY() + card.getHeight() <= cards.getHeight());
				}
			}
		});
	}

	private static JLabel findHeading(Container root) {
		for (Component child : root.getComponents()) {
			if (child instanceof JLabel label && label.getFont().isBold() && !label.getText().isEmpty())
				return label;
			if (child instanceof Container container) {
				JLabel found = findHeading(container);
				if (found != null)
					return found;
			}
		}
		return null;
	}

	private static void assertLabelsFit(Container root) {
		for (Component child : root.getComponents()) {
			if (child instanceof JLabel label && label.getText() != null && !label.getText().isEmpty()
					&& !label.getText().startsWith("<html>")) {
				assertTrue(label.getWidth() >= label.getPreferredSize().width,
						"Full label must remain visible after resizing: " + label.getText());
				assertTrue(label.getHeight() >= label.getPreferredSize().height,
						"Label height must adapt: " + label.getText());
			}
			if (child instanceof Container container)
				assertLabelsFit(container);
		}
	}

	private static void layoutTree(Container root) {
		root.doLayout();
		for (Component child : root.getComponents())
			if (child instanceof Container container)
				layoutTree(container);
	}
}
