
package gestion.ui.personnel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import gestion.model.EmployeeSummary;
import gestion.model.HoursAlert;
import gestion.ui.UIUtils;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

public final class PersonnelHoursRenderer extends DefaultTableCellRenderer {

	private static final long serialVersionUID = 1L;

	private final PersonnelTableModel model;
	private Color glowColor;
	private Color selectionColor;
	private boolean selected;

	public PersonnelHoursRenderer(PersonnelTableModel model) {
		this.model = model;
		setHorizontalAlignment(JLabel.CENTER);
		setOpaque(false);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

		EmployeeSummary summary = model.rowAt(table.convertRowIndexToModel(row));
		int modelColumn = table.convertColumnIndexToModel(column);

		HoursAlert alert = modelColumn == 4 ? summary.assignedAlert() : summary.remainingAlert();

		glowColor = UIUtils.getHoursColor(alert);
		selected = isSelected;
		selectionColor = table.getSelectionBackground();

		setHorizontalAlignment(JLabel.CENTER);
		setOpaque(false);
		setForeground(isSelected ? table.getSelectionForeground() : UIUtils.getTextColor());

		setToolTipText(UIUtils.getHoursLegend());

		return this;
	}

	@Override
	protected void paintComponent(Graphics graphics) {

		Graphics2D g = (Graphics2D) graphics.create();

		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

			if (selected) {
				g.setColor(selectionColor);
				g.fillRect(0, 0, getWidth(), getHeight());
			}

			String text = getText();

			if (text == null || text.isBlank())
				return;

			boolean dark = UIUtils.isDarkMode();
			Font displayFont = getFont().deriveFont(Font.BOLD, 14f);
			GlyphVector vector = displayFont.createGlyphVector(g.getFontRenderContext(), text);
			Rectangle2D bounds = vector.getVisualBounds();

			float x = (float) ((getWidth() - bounds.getWidth()) / 2 - bounds.getX());
			float y = (float) ((getHeight() - bounds.getHeight()) / 2 - bounds.getY());

			Shape letters = vector.getOutline(x, y);

			if (glowColor != null) {
				float[] widths = dark ? new float[] { 5.5f, 3.2f, 1.6f } : new float[] { 3.8f, 2.2f, 1.1f };
				int[] alphas = dark ? new int[] { 20, 48, 120 } : new int[] { 8, 22, 65 };

				for (int i = 0; i < widths.length; i++) {
					g.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), alphas[i]));
					g.setStroke(new BasicStroke(widths[i], BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

					g.draw(letters);
				}
			}

			g.setColor(dark ? Color.WHITE : Color.DARK_GRAY);
			g.fill(letters);

		} finally {
			g.dispose();
		}
	}

}