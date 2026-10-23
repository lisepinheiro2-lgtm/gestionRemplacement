package gestion.ui.personnel;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import gestion.ui.UIUtils;

final class SelectAllTableHeader extends JCheckBox implements TableCellRenderer {

	private final JTable table;
	private boolean dark = UIUtils.isDarkMode();

	SelectAllTableHeader(JTable table, int modelColumn, Runnable onToggle) {
		super(table.getModel().getColumnName(modelColumn));
		this.table = table;
		setHorizontalAlignment(CENTER);
		setFocusable(false);
		setOpaque(true);
		setBorderPainted(true);
		setIconTextGap(4);
		setMargin(new Insets(0, 0, 0, 0));
		getAccessibleContext().setAccessibleName("Tout sélectionner ou désélectionner dans la colonne " + getText());
		table.getColumnModel().getColumn(table.convertColumnIndexToView(modelColumn)).setHeaderRenderer(this);

		if (table.getRowSorter() instanceof TableRowSorter<?> sorter)
			sorter.setSortable(modelColumn, false);

		table.getTableHeader().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent event) {
				if (!SwingUtilities.isLeftMouseButton(event) || event.getClickCount() != 1 || !isEnabled())
					return;

				int viewColumn = table.getTableHeader().columnAtPoint(event.getPoint());

				if (viewColumn < 0 || table.convertColumnIndexToModel(viewColumn) != modelColumn)
					return;

				if (table.isEditing() && !table.getCellEditor().stopCellEditing())
					return;

				onToggle.run();
			}
		});
	}

	void updateSelection(int eligible, int selected) {
		boolean all = eligible > 0 && selected == eligible;
		setEnabled(eligible > 0);
		setSelected(all);
		putClientProperty("JButton.selectedState", selected > 0 && !all ? "indeterminate" : null);
		setToolTipText(eligible == 0 ? "Aucune intervention modifiable à sélectionner."
				: all ? "Tout désélectionner" : "Tout sélectionner — interventions non commencées uniquement");
		table.getTableHeader().repaint();
	}

	@Override
	public Component getTableCellRendererComponent(JTable source, Object value, boolean selected, boolean focused,
			int row, int column) {
		if (dark != UIUtils.isDarkMode()) {
			dark = UIUtils.isDarkMode();
			updateUI();
		}

		Component header = source.getTableHeader().getDefaultRenderer()
				.getTableCellRendererComponent(source, value, false, false, row, column);
		setFont(header.getFont());
		setForeground(header.getForeground());
		setBackground(header.getBackground());

		if (header instanceof JComponent component)
			setBorder(component.getBorder());

		return this;
	}

	@Override
	protected void paintBorder(Graphics graphics) {
		super.paintBorder(graphics);
		Graphics line = graphics.create();

		try {
			line.setColor(table.getGridColor());
			line.drawLine(0, getHeight() - 1, getWidth() - 1, getHeight() - 1);
		} finally {
			line.dispose();
		}
	}
}
