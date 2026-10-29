package gestion.ui.personnel;

import java.awt.Color;
import java.awt.Component;
import javax.swing.Icon;
import javax.swing.JTable;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import gestion.model.EmployeeStatus;
import gestion.ui.BadgeRenderer;
import gestion.ui.UIUtils;

public final class PersonnelStatusRenderer extends BadgeRenderer {
	private final PersonnelTableModel model;
	private final Icon walking = loadIcon("walking.svg");
	private final Icon vacation = loadIcon("vacation.svg");
	private final Icon sick = loadIcon("sick.svg");
	private final Icon other = loadIcon("other.svg");

	public PersonnelStatusRenderer(PersonnelTableModel model) {
		this.model = model;
	}

	private static Icon loadIcon(String fileName) {
		return new FlatSVGIcon(PersonnelStatusRenderer.class.getResource("/icons/status/" + fileName)).derive(0.65f);
	}

	@Override
	protected Color colorFor(Object value) {
		return "Opérationnel".equals(value) ? UIUtils.getTurquoiseColor()
				: "Maladie".equals(value) ? UIUtils.getVioletColor() : UIUtils.getChampagneColor();
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
			boolean hasFocus, int row, int column) {
		super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		EmployeeStatus status = model.rowAt(table.convertRowIndexToModel(row)).status();
		setText(status.toString());
		setIcon(switch (status) {
		case OPERATIONAL -> walking;
		case VACATION -> vacation;
		case SICK_LEAVE -> sick;
		case OTHER_ABSENCE -> other;
		});
		setIconTextGap(10);
		return this;
	}
}
