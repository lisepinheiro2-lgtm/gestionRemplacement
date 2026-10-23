package gestion.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

public final class NavigationIcon implements Icon {
	public enum Kind {
		PEOPLE, CLIENTS, CALENDAR, REPLACEMENTS, INTERVENTIONS, ARCHIVE, SETTINGS, HOME, ARROW, USER
	}

	private final Kind kind;
	private final int size;
	private final Color color;

	public NavigationIcon(Kind kind, int size, Color color) {
		this.kind = kind;
		this.size = size;
		this.color = color;
	}

	public int getIconWidth() {
		return size;
	}

	public int getIconHeight() {
		return size;
	}

	public void paintIcon(Component component, Graphics graphics, int x, int y) {
		Graphics2D g = (Graphics2D) graphics.create();
		g.translate(x, y);
		g.scale(size / 24.0, size / 24.0);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g.setColor(color);
		g.setStroke(new BasicStroke(1.65f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		switch (kind) {

		case PEOPLE -> {
			g.draw(new Ellipse2D.Double(6, 3.5, 6, 6));
			g.draw(new Arc2D.Double(2.5, 12, 13, 13, 0, 180, Arc2D.OPEN));
			line(g, 2.5, 18.5, 2.5, 21);
			line(g, 15.5, 18.5, 15.5, 21);
			g.draw(new Arc2D.Double(12, 4.5, 6, 6, -65, 145, Arc2D.OPEN));
			g.draw(new Arc2D.Double(14, 13, 7, 10, 0, 85, Arc2D.OPEN));
			line(g, 21, 18, 21, 21);
		}

		case USER -> {
			g.draw(new Ellipse2D.Double(8, 3, 8, 8));
			g.draw(new Arc2D.Double(4, 13, 16, 14, 0, 180, Arc2D.OPEN));
			path(g, 4, 20, 4, 21, 20, 21, 20, 20);
		}

		case CLIENTS -> {
			g.draw(new RoundRectangle2D.Double(3, 8, 18, 13, 3, 3));
			g.draw(new RoundRectangle2D.Double(8, 3, 8, 5, 2, 2));
			line(g, 3, 13, 21, 13);
			line(g, 10, 13, 10, 15);
			line(g, 14, 13, 14, 15);
		}

		case CALENDAR -> {
			g.draw(new RoundRectangle2D.Double(3, 5, 18, 16, 3, 3));
			line(g, 7, 3, 7, 7);
			line(g, 17, 3, 17, 7);
			line(g, 3, 10, 21, 10);
			for (int row = 0; row < 2; row++)
				for (int col = 0; col < 3; col++)
					g.fill(new RoundRectangle2D.Double(6 + col * 5, 13 + row * 4, 2, 2, .7, .7));
		}

		case REPLACEMENTS -> {
			line(g, 4, 7, 20, 7);
			line(g, 16, 3, 20, 7);
			line(g, 16, 11, 20, 7);
			line(g, 20, 17, 4, 17);
			line(g, 8, 13, 4, 17);
			line(g, 8, 21, 4, 17);
		}

		case INTERVENTIONS -> {
			g.draw(new RoundRectangle2D.Double(4, 4, 16, 18, 3, 3));
			g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
			g.fill(new RoundRectangle2D.Double(8, 2, 8, 4, 2, 2));
			g.setColor(color);
			g.draw(new RoundRectangle2D.Double(8, 2, 8, 4, 2, 2));
			line(g, 8, 11, 9.5, 12.5);
			line(g, 9.5, 12.5, 12, 10);
			line(g, 14, 11, 17, 11);
			line(g, 8, 17, 17, 17);
		}

		case ARCHIVE -> {
			g.draw(new RoundRectangle2D.Double(3, 4, 18, 5, 2, 2));
			g.draw(new RoundRectangle2D.Double(5, 9, 14, 12, 2, 2));
			line(g, 10, 13, 14, 13);
		}

		case HOME -> {
			path(g, 3, 11, 12, 3, 21, 11);
			path(g, 6, 9, 6, 21, 10, 21, 10, 15, 14, 15, 14, 21, 18, 21, 18, 9);
		}

		case ARROW -> {
			line(g, 4, 12, 20, 12);
			path(g, 14, 6, 20, 12, 14, 18);
		}

		case SETTINGS -> {
			g.draw(new Ellipse2D.Double(5.5, 5.5, 13, 13));
			g.draw(new Ellipse2D.Double(9, 9, 6, 6));

			for (int i = 0; i < 8; i++) {
				double angle = i * Math.PI / 4;
				line(g, 12 + Math.cos(angle) * 7, 12 + Math.sin(angle) * 7, 12 + Math.cos(angle) * 9.5,
						12 + Math.sin(angle) * 9.5);
			}
		}
		}

		g.dispose();
	}

	private static void line(Graphics2D g, double x1, double y1, double x2, double y2) {
		g.draw(new Line2D.Double(x1, y1, x2, y2));
	}

	private static void path(Graphics2D g, double... coordinates) {
		Path2D path = new Path2D.Double();
		path.moveTo(coordinates[0], coordinates[1]);

		for (int i = 2; i < coordinates.length; i += 2)
			path.lineTo(coordinates[i], coordinates[i + 1]);
		g.draw(path);
	}
}
