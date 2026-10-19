package gestion.ui.personnel;

import java.util.List;
import javax.swing.table.AbstractTableModel;

import gestion.model.Employee;
import gestion.model.EmployeeSummary;
import gestion.util.FormatUtils;

public final class PersonnelTableModel extends AbstractTableModel {
	private static final String[] COLUMNS = { "Nom", "Prénom", "Heures prévues au contrat", "Statut",
			"Heures attribuées", "Heures restantes" };
	private List<EmployeeSummary> rows = List.of();
	private final EditHandler editHandler;

	@FunctionalInterface
	public interface EditHandler {
		void edit(Employee employee, String firstName, String lastName, String contractHours);
	}

	public PersonnelTableModel(EditHandler editHandler) {
		this.editHandler = editHandler;
	}

	public void setRows(List<EmployeeSummary> rows) {
		this.rows = List.copyOf(rows);
		fireTableDataChanged();
	}

	public EmployeeSummary rowAt(int row) {
		return rows.get(row);
	}

	@Override
	public int getRowCount() {
		return rows.size();
	}

	@Override
	public int getColumnCount() {
		return COLUMNS.length;
	}

	@Override
	public String getColumnName(int column) {
		return COLUMNS[column];
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return column < 3;
	}

	@Override
	public Object getValueAt(int row, int column) {
		EmployeeSummary values = rows.get(row);
		return switch (column) {
		case 0 -> values.employee().getLastName();
		case 1 -> values.employee().getFirstName();
		case 2 -> FormatUtils.formatContractHours(values.employee().getContractHours());
		case 3 -> values.status().toString();
		case 4 -> FormatUtils.formatContractHours(values.assignedHours());
		case 5 -> FormatUtils.formatContractHours(values.remainingHours());
		default -> throw new IndexOutOfBoundsException(column);
		};
	}

	@Override
	public void setValueAt(Object value, int row, int column) {
		if (!isCellEditable(row, column))
			return;
		Employee employee = rows.get(row).employee();
		String text = value == null ? "" : value.toString();
		editHandler.edit(employee, column == 1 ? text : employee.getFirstName(),
				column == 0 ? text : employee.getLastName(),
				column == 2 ? text : FormatUtils.formatContractHours(employee.getContractHours()));
	}
}
