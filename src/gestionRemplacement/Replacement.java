package gestionRemplacement;

public class Replacement {

	private Employee employeeToReplace;
	private Employee replacementEmployee;

	private String startDate;
	private String endDate;
	private String startTime;
	private String endTime;
	private double contractHoursEmployeeToReplace;
	private double contractHoursReplacementEmployee;
	private String absenceId;

	public Replacement(Employee employeeToReplace, double contractHoursEmployeeToReplace, Employee replacementEmployee,
			double contractHoursReplacementEmployee, String startDate, String startTime, String endDate, String endTime) {

		this.setEmployeeToReplace(employeeToReplace);
		this.setReplacementEmployee(replacementEmployee);
		this.setStartDate(startDate);
		this.setEndDate(endDate);
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

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getAbsenceId() {
		return absenceId;
	}

	public void setAbsenceId(String absenceId) {
		this.absenceId = absenceId;
	}

}
