package gestion.ui.personnel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import com.formdev.flatlaf.extras.FlatSVGIcon;

import gestion.model.EmployeeStatus;
import gestion.ui.UIUtils;

public final class PersonnelStatusRenderer extends DefaultTableCellRenderer {

	private static final long serialVersionUID = 1L;
	private final PersonnelTableModel model;

	private final Icon walking = loadIcon("walking.svg");
	private final Icon vacation = loadIcon("vacation.svg");
	private final Icon sick = loadIcon("sick.svg");
	private final Icon other = loadIcon("other.svg");

	private Color glowColor;
	private Color selectionColor;
	private boolean selected;

	public PersonnelStatusRenderer(PersonnelTableModel model) {
		this.model = model;
		setHorizontalAlignment(JLabel.CENTER);
		setVerticalAlignment(JLabel.CENTER);
		setOpaque(false);
	}

	private static Icon loadIcon(String fileName) {
		return new FlatSVGIcon(PersonnelStatusRenderer.class.getResource("/icons/status/" + fileName)).derive(0.65f);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		EmployeeStatus status = model.rowAt(table.convertRowIndexToModel(row)).status();

		setText("");
		setIcon(switch (status) {
		case OPERATIONAL -> walking;
		case VACATION -> vacation;
		case SICK_LEAVE -> sick;
		case OTHER_ABSENCE -> other;
		});

		setToolTipText(status.toString());
		setOpaque(false);

		glowColor = UIUtils.getStatusGlowColor(status);
		selected = isSelected;
		selectionColor = table.getSelectionBackground();

		return this;
	}

	@Override
	protected void paintComponent(Graphics graphics) {

		Graphics2D g = (Graphics2D) graphics.create();

		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

			if (selected) {
				g.setColor(selectionColor);
				g.fillRect(0, 0, getWidth(), getHeight());
			}

			if (glowColor != null) {
				int x = 6;
				int y = 3;
				int width = getWidth() - 12;
				int height = getHeight() - 6;

				if (width > 8 && height > 8) {

					g.setColor(withAlpha(glowColor, 20));
					g.fillRoundRect(x, y, width, height, 16, 16);

					for (int i = 0; i < 5; i++) {
						g.setColor(withAlpha(glowColor, 65 - i * 12));
						g.drawRoundRect(x + i, y + i, width - 2 * i - 1, height - 2 * i - 1, 16, 16);
					}
				}
			}

		} finally {
			g.dispose();
		}

		super.paintComponent(graphics);
	}

	private static Color withAlpha(Color color, int alpha) {
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
	}
}