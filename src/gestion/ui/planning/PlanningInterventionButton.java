package gestion.ui.planning;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import gestion.model.Intervention;
import gestion.ui.UIUtils;

final class PlanningInterventionButton extends JButton {

	private final Intervention intervention;
	private final List<String> lines;
	private List<String> wrappedLines = List.of();
	private int wrappedWidth = -1;
	private Font wrappedFont;

	PlanningInterventionButton(Intervention intervention, String start, String end) {
		this.intervention = intervention;
		String employee = intervention.getEmployee() == null ? "Non attribué" : intervention.getEmployee().toString();
		lines = List.of(intervention.getClient().toString(), start + " → " + end, employee);
		getAccessibleContext().setAccessibleName(String.join(", ", lines));
	}

	@Override
	public Dimension getPreferredSize() {
		if (lines == null)
			return super.getPreferredSize();

		FontMetrics metrics = getFontMetrics(getFont());
		Insets insets = getInsets();
		int width = lines.stream().mapToInt(metrics::stringWidth).max().orElse(0);

		return new Dimension(width + insets.left + insets.right,
				lines.size() * metrics.getHeight() + insets.top + insets.bottom);
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		super.paintComponent(graphics);
		Graphics2D text = (Graphics2D) graphics.create();

		try {
			Insets insets = getInsets();
			int width = Math.max(1, getWidth() - insets.left - insets.right);
			FontMetrics metrics = text.getFontMetrics(getFont());
			prepareLines(width, metrics);
			int height = wrappedLines.size() * metrics.getHeight();
			int y = (getHeight() - height) / 2 + metrics.getAscent();

			text.clipRect(insets.left, insets.top, width, Math.max(0, getHeight() - insets.top - insets.bottom));
			text.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			text.setFont(getFont());
			text.setColor(getForeground());

			for (String line : wrappedLines) {
				int x = insets.left + (width - metrics.stringWidth(line)) / 2;
				text.drawString(line, x, y);
				y += metrics.getHeight();
			}
		} finally {
			text.dispose();
		}

		LocalDateTime end = LocalDateTime.of(intervention.getEndDate(), intervention.getEndTime());

		if (!end.isAfter(LocalDateTime.now()))
			UIUtils.drawPastStripes(graphics, getWidth(), getHeight());
	}

	private void prepareLines(int width, FontMetrics metrics) {
		if (wrappedWidth == width && getFont().equals(wrappedFont))
			return;

		List<String> result = new ArrayList<>();

		for (String line : lines) {
			String current = "";

			for (String word : line.split(" ")) {
				String candidate = current.isEmpty() ? word : current + " " + word;

				if (!current.isEmpty() && metrics.stringWidth(candidate) > width) {
					result.add(current);
					current = word;
				} else {
					current = candidate;
				}
			}

			result.add(current);
		}

		wrappedLines = List.copyOf(result);
		wrappedWidth = width;
		wrappedFont = getFont();
	}
}
