package gestionRemplacement;

public class Replacement {

	private Employee employeeToReplace;
	private Employee replacementEmployee;

	private String date;
	private String startTime;
	private String endTime;
	private double contractHoursEmployeeToReplace;
	private double contractHoursReplacementEmployee;

	public Replacement(Employee employeeToReplace, double contractHoursEmployeeToReplace, Employee replacementEmployee,
			double contractHoursReplacementEmployee, String date, String startTime, String endTime) {

		this.setEmployeeToReplace(employeeToReplace);
		this.setReplacementEmployee(replacementEmployee);
		this.setDate(date);
		this.setStartTime(startTime);
		this.setEndTime(endTime);
		this.setContractHoursEmployeeToReplace(contractHoursEmployeeToReplace);
		this.setContractHoursReplacementEmployee(contractHoursReplacementEmployee);
	}

	public Employee getEmployeeToReplace() {
		return employeeToReplace;
	}

	public void setEmployeeToReplace(Employee employeeToReplace) {
		this.employeeToReplace = employeeToReplace;
	}

	public Employee getReplacementEmployee() {
		return replacementEmployee;
	}

	public void setReplacementEmployee(Employee replacementEmployee) {
		this.replacementEmployee = replacementEmployee;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public double getContractHoursEmployeeToReplace() {
		return contractHoursEmployeeToReplace;
	}

	public void setContractHoursEmployeeToReplace(double contractHoursEmployeeToReplace) {
		this.contractHoursEmployeeToReplace = contractHoursEmployeeToReplace;
	}

	public double getContractHoursReplacementEmployee() {
		return contractHoursReplacementEmployee;
	}

	public void setContractHoursReplacementEmployee(double contractHoursReplacementEmployee) {
		this.contractHoursReplacementEmployee = contractHoursReplacementEmployee;
	}

}
