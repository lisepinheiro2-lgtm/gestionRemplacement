package gestionRemplacement;

public class WorkShift {

	private Employee employee;
	private String date;
	private String startTime;
	private String endTime;

	public WorkShift(Employee employee, String date, String startTime, String endTime) {
		this.employee = employee;
		this.date = date;
		this.startTime = startTime;
		this.endTime = endTime;
	}

	public double getDurationHours() {

		String[] start = startTime.split("h");

		int startHour = Integer.parseInt(start[0]);
		int startMinute = Integer.parseInt(start[1]);
		int startTotalMinutes = startHour * 60 + startMinute;

		String[] end = endTime.split("h");

		int endtHour = Integer.parseInt(end[0]);
		int endMinute = Integer.parseInt(end[1]);
		int endTotalMinutes = endtHour * 60 + endMinute;

		int durationMinutes = endTotalMinutes - startTotalMinutes;

		return durationMinutes / 60;
	}

	public Employee getEmployee() {
		return employee;
	}

	public void setEmployee(Employee employee) {
		this.employee = employee;
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

}