package gestion.ui.planning;

import java.awt.Color;
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
	private final boolean weekly;
	private List<String> wrappedLines = List.of();
	private int wrappedWidth = -1;
	private Font wrappedFont;

	PlanningInterventionButton(Intervention intervention, String start, String end) {
		this(intervention, start, end, false);
	}

	PlanningInterventionButton(Intervention intervention, String start, String end, boolean weekly) {
		this.intervention = intervention;
		this.weekly = weekly;
		String employee = intervention.getEmployee() == null ? "Non attribué" : intervention.getEmployee().toString();
		if (weekly && intervention.getEmployee() != null)
			employee = intervention.getEmployee().getLastName() + " " + intervention.getEmployee().getFirstName();

		lines = weekly ? List.of(employee, intervention.getClient().getLastName() + " "
				+ intervention.getClient().getFirstName())
				: List.of(intervention.getClient().toString(), start + " → " + end, employee);

		if (weekly) {
			setMargin(new Insets(1, 3, 1, 3));
			setToolTipText("Intervenant : " + employee + " | Client : " + intervention.getClient()
					+ " | " + intervention.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu"))
					+ " " + intervention.getStartTime() + " → "
					+ intervention.getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/uuuu"))
					+ " " + intervention.getEndTime());
		}
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
			if (weekly) {
				paintWeeklyNames(text);
			} else {
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
			}
		} finally {
			text.dispose();
		}

		LocalDateTime end = LocalDateTime.of(intervention.getEndDate(), intervention.getEndTime());

		if (!end.isAfter(LocalDateTime.now())) {
			Graphics pastGraphics = graphics.create();

			try {
				pastGraphics.setColor(new Color(66, 70, 77, UIUtils.isDarkMode() ? 82 : 48));
				pastGraphics.fillRect(0, 0, getWidth(), getHeight());
			} finally {
				pastGraphics.dispose();
			}
		}
	}

	static Color nameColor(boolean employee, boolean dark) {
		return new Color(employee ? (dark ? 0xFFD36A : 0x805000) : (dark ? 0x9CCCFF : 0x174E99));
	}

	private void paintWeeklyNames(Graphics2D text) {
		Insets insets = getInsets();
		int width = Math.max(0, getWidth() - insets.left - insets.right);
		int height = Math.max(0, getHeight() - insets.top - insets.bottom);
		Font font = getFont().deriveFont(Font.PLAIN, 12f);
		FontMetrics metrics = text.getFontMetrics(font);
		if (height < metrics.getHeight())
			return;
		text.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		text.clipRect(insets.left, insets.top, width, height);
		text.setFont(font);
		text.setColor(UIUtils.getTextColor());
		int count = Math.min(2, height / metrics.getHeight());
		int y = insets.top + (height - count * metrics.getHeight()) / 2 + metrics.getAscent();
		String employee = lines.getFirst();
		text.drawString(fitName(employee, width, metrics), insets.left, y);
		if (count > 1) {
			text.setColor(UIUtils.getPlaceholderColor());
			text.drawString(fitName(lines.get(1), width, metrics), insets.left, y + metrics.getHeight());
		}
	}

	private static void drawName(Graphics2D text, String name, boolean employee, int left, int y, int width,
			FontMetrics metrics) {
		name = fitName(name, width, metrics);
		int x = left + (width - metrics.stringWidth(name)) / 2;
		text.setColor(UIUtils.isDarkMode() ? new Color(0x202833) : new Color(0xFFF9EF));
		text.fillRect(x, y, metrics.stringWidth(name), metrics.getHeight());
		text.setColor(nameColor(employee, UIUtils.isDarkMode()));
		text.drawString(name, x, y + metrics.getAscent());
	}

	private static String compactName(String name) {
		if ("Non attribué".equals(name))
			return name;

		StringBuilder initials = new StringBuilder();

		for (String word : name.split("\\s+")) {
			if (!word.isEmpty())
				initials.appendCodePoint(word.codePointAt(0)).append('.');
		}

		return initials.toString();
	}

	private static String fitName(String name, int width, FontMetrics metrics) {
		if (metrics.stringWidth(name) <= width)
			return name;
		if (metrics.stringWidth("…") > width)
			return "";

		while (!name.isEmpty() && metrics.stringWidth(name + "…") > width)
			name = name.substring(0, name.offsetByCodePoints(name.length(), -1));

		return name + "…";
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
