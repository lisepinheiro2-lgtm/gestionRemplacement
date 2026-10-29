package gestion.ui.personnel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.EventObject;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import gestion.ui.UIUtils;

final class PersonnelTable extends JTable {

	private static final Border TEXT_PADDING = BorderFactory.createEmptyBorder(1, 8, 1, 8);
	private int hoveredRow = -1;
	private int hoveredColumn = -1;
	private int activeRow = -1;
	private int activeColumn = -1;
	private boolean effectsReady;
	private JTextField styledEditor;
	private Border originalEditorBorder;
	private Border editorPadding;

	PersonnelTable(PersonnelTableModel model) {
		super(model);
		MouseAdapter hover = new MouseAdapter() {
			@Override
			public void mouseMoved(MouseEvent event) {
				updateHover(event.getPoint());
			}

			@Override
			public void mouseDragged(MouseEvent event) {
				updateHover(event.getPoint());
			}

			@Override
			public void mouseExited(MouseEvent event) {
				clearHover();
			}
		};
		addMouseMotionListener(hover);
		addMouseListener(hover);
		getSelectionModel().addListSelectionListener(event -> refreshActiveCell());
		getColumnModel().getSelectionModel().addListSelectionListener(event -> refreshActiveCell());
		model.addTableModelListener(event -> clearHover());
		effectsReady = true;
	}

	@Override
	protected void configureEnclosingScrollPane() {
	}

	@Override
	protected boolean isPaintingOrigin() {
		return true;
	}

	@Override
	public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
		Component component = renderer.getTableCellRendererComponent(this, getValueAt(row, column),
				!isPaintingForPrint() && isCellSelected(row, column), false, row, column);

		if (convertColumnIndexToModel(column) < 3 && component instanceof JLabel label)
			label.setBorder(TEXT_PADDING);

		return component;
	}

	private boolean supportsHalo(int row, int column) {
		if (row < 0 || row >= getRowCount() || column < 0 || column >= getColumnCount())
			return false;

		int modelColumn = convertColumnIndexToModel(column);
		return isCellEditable(row, column) || modelColumn == 4 || modelColumn == 5;
	}

	private void updateHover(Point point) {
		int row = rowAtPoint(point);
		int column = columnAtPoint(point);

		if (!supportsHalo(row, column)) {
			row = -1;
			column = -1;
		}

		if (row == hoveredRow && column == hoveredColumn)
			return;

		repaintCell(hoveredRow, hoveredColumn);
		hoveredRow = row;
		hoveredColumn = column;
		repaintCell(hoveredRow, hoveredColumn);
	}

	private void clearHover() {
		repaintCell(hoveredRow, hoveredColumn);
		hoveredRow = -1;
		hoveredColumn = -1;
	}

	private void refreshActiveCell() {
		if (!effectsReady)
			return;

		int row = isEditing() ? getEditingRow() : getSelectionModel().getLeadSelectionIndex();
		int column = isEditing() ? getEditingColumn() : getColumnModel().getSelectionModel().getLeadSelectionIndex();

		if (!supportsHalo(row, column) || (!isEditing() && !isRowSelected(row))) {
			row = -1;
			column = -1;
		}

		if (row == activeRow && column == activeColumn)
			return;

		repaintCell(activeRow, activeColumn);
		activeRow = row;
		activeColumn = column;
		repaintCell(activeRow, activeColumn);
	}

	private void repaintCell(int row, int column) {
		if (!effectsReady || row < 0 || row >= getRowCount() || column < 0 || column >= getColumnCount())
			return;

		Rectangle bounds = getCellRect(row, column, true);
		bounds.grow(3, 3);
		repaint(bounds);
	}

	@Override
	public Component prepareEditor(TableCellEditor editor, int row, int column) {
		Component component = super.prepareEditor(editor, row, column);

		if (component instanceof JTextField field) {
			field.setHorizontalAlignment(JTextField.CENTER);
			styledEditor = field;
			originalEditorBorder = field.getBorder();
			Insets insets = field.getInsets();
			editorPadding = BorderFactory.createEmptyBorder(insets.top, 8, insets.bottom, 8);
			field.setBorder(editorPadding);
		}

		return component;
	}

	private void restoreEditorBorder() {
		if (styledEditor != null && styledEditor.getBorder() == editorPadding)
			styledEditor.setBorder(originalEditorBorder);

		styledEditor = null;
		originalEditorBorder = null;
		editorPadding = null;
	}

	@Override
	public boolean editCellAt(int row, int column, EventObject event) {
		boolean started = super.editCellAt(row, column, event);

		if (!started)
			restoreEditorBorder();

		refreshActiveCell();
		return started;
	}

	@Override
	public void removeEditor() {
		int row = getEditingRow();
		int column = getEditingColumn();
		super.removeEditor();
		restoreEditorBorder();
		repaintCell(row, column);
		refreshActiveCell();
	}

	private Rectangle rowBounds(int row) {
		return getCellRect(row, 0, true).union(getCellRect(row, getColumnCount() - 1, true));
	}

	private Shape rounded(Rectangle bounds, double inset) {
		return new RoundRectangle2D.Double(bounds.x + inset, bounds.y + inset,
				Math.max(0, bounds.width - inset * 2), Math.max(0, bounds.height - inset * 2), 10, 10);
	}

	private boolean wholeRowSelected(int row) {
		return getRowSelectionAllowed() && !getColumnSelectionAllowed() && isRowSelected(row);
	}

	private int firstVisibleRow(Rectangle clip) {
		return rowAtPoint(new Point(0, Math.max(0, clip.y)));
	}

	private int lastVisibleRow(Rectangle clip) {
		int row = rowAtPoint(new Point(0, clip.y + clip.height - 1));
		return row < 0 ? getRowCount() - 1 : row;
	}

	private void clipSelectedRows(Graphics2D graphics) {
		Rectangle clip = graphics.getClipBounds();
		int first = firstVisibleRow(clip);

		if (first < 0 || getColumnCount() == 0)
			return;

		Area visible = new Area(graphics.getClip());

		for (int row = first, last = lastVisibleRow(clip); row <= last; row++) {
			if (!wholeRowSelected(row))
				continue;

			Rectangle bounds = rowBounds(row);
			Area corners = new Area(bounds);
			corners.subtract(new Area(rounded(bounds, 1)));
			visible.subtract(corners);
		}

		graphics.setClip(visible);
	}

	@Override
	protected void paintComponent(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics.create();

		try {
			if (g.getClip() == null)
				g.setClip(0, 0, getWidth(), getHeight());

			if (isOpaque()) {
				g.setColor(getBackground());
				Rectangle clip = g.getClipBounds();
				g.fillRect(clip.x, clip.y, clip.width, clip.height);
			}

			clipSelectedRows(g);
			super.paintComponent(g);
		} finally {
			g.dispose();
		}
	}

	@Override
	protected void paintChildren(Graphics graphics) {
		Graphics2D g = (Graphics2D) graphics.create();

		try {
			if (g.getClip() == null)
				g.setClip(0, 0, getWidth(), getHeight());

			clipSelectedRows(g);

			if (isEditing() && styledEditor != null) {
				Rectangle bounds = styledEditor.getBounds();
				Area corners = new Area(bounds);
				corners.subtract(new Area(rounded(bounds, 0.5)));
				Area visible = new Area(g.getClip());
				visible.subtract(corners);
				g.setClip(visible);
			}

			super.paintChildren(g);
		} finally {
			g.dispose();
		}
	}

	@Override
	public void paint(Graphics graphics) {
		super.paint(graphics);
		Graphics2D g = (Graphics2D) graphics.create();

		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			Rectangle clip = g.getClipBounds();

			if (clip == null)
				clip = new Rectangle(0, 0, getWidth(), getHeight());

			int first = firstVisibleRow(clip);

			if (first >= 0 && getColumnCount() > 0) {
				for (int row = first, last = lastVisibleRow(clip); row <= last; row++)
					if (wholeRowSelected(row))
						paintHalo(g, rounded(rowBounds(row), 1.5), false);
			}

			if (supportsHalo(hoveredRow, hoveredColumn) && (hoveredRow != activeRow || hoveredColumn != activeColumn))
				paintHalo(g, rounded(getCellRect(hoveredRow, hoveredColumn, true), 2.5), false);

			if (supportsHalo(activeRow, activeColumn) && (isEditing() || isRowSelected(activeRow)))
				paintHalo(g, rounded(getCellRect(activeRow, activeColumn, true), isEditing() ? 1.5 : 2.5), true);
		} finally {
			g.dispose();
		}
	}

	private void paintHalo(Graphics2D graphics, Shape outline, boolean active) {
		Color blue = UIUtils.getTurquoiseColor();
		graphics.setStroke(new BasicStroke(active ? 1.2f : 0.8f));
		graphics.setColor(new Color(blue.getRed(), blue.getGreen(), blue.getBlue(), active ? 190 : 100));
		graphics.draw(outline);
	}
}
