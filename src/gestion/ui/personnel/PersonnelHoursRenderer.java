package gestion.ui.personnel;

import java.awt.Component;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import gestion.model.EmployeeSummary;
import gestion.model.HoursAlert;
import gestion.ui.UIUtils;

public final class PersonnelHoursRenderer extends DefaultTableCellRenderer {
	private final PersonnelTableModel model;

	public PersonnelHoursRenderer(PersonnelTableModel model) {
		this.model = model;
		setHorizontalAlignment(JLabel.CENTER);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		EmployeeSummary summary = model.rowAt(table.convertRowIndexToModel(row));
		HoursAlert alert = table.convertColumnIndexToModel(column) == 4
				? summary.assignedAlert() : summary.remainingAlert();
		setForeground(UIUtils.getHoursColor(alert));
		setFont(table.getFont().deriveFont(Font.BOLD));
		setToolTipText(UIUtils.getHoursLegend());
		return this;
	}
}
