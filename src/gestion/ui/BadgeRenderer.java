package gestion.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableCellRenderer;

public class BadgeRenderer extends DefaultTableCellRenderer {
	private Color accent;

	public BadgeRenderer() {
		setHorizontalAlignment(JLabel.CENTER);
		setOpaque(false);
	}

	protected Color colorFor(Object value) {
		String text = String.valueOf(value);
		return text.equals("Récurrent") || text.equals("Attribué") || text.equals("Opérationnel")
				? UIUtils.getTurquoiseColor() : UIUtils.getVioletColor();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		accent = colorFor(value);
		setForeground(accent);
		String text = value == null ? "" : value.toString();
		NavigationIcon.Kind kind = "Récurrent".equals(text) ? NavigationIcon.Kind.RECURRING
				: "Unique".equals(text) ? NavigationIcon.Kind.CALENDAR : null;
		setIcon(kind == null ? null : new NavigationIcon(kind, 16, accent));
		setIconTextGap(8);
		setOpaque(false);
		setToolTipText(value == null ? null : value.toString());
		return this;
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics.create();
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			// The label stays transparent so its background cannot cover the badge.
			// Paint the renderer's row color, including striping and selection, first.
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
			if (accent != null) {
				int width = Math.min(Math.max(0, getWidth() - 12), 260);
				int x = (getWidth() - width) / 2;
				g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
				g.fillRoundRect(x, 3, width, Math.max(0, getHeight() - 6), 12, 12);
				g.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 135));
				g.drawRoundRect(x, 3, Math.max(0, width - 1), Math.max(0, getHeight() - 7), 12, 12);
			}
		} finally {
			g.dispose();
		}
		super.paintComponent(graphics);
	}
}
