package gestion.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.TreeSet;

import gestion.model.Employee;
import gestion.model.HoursAlert;
import gestion.model.Intervention;

public final class WorkHoursService {

	private final PlanningService planning;
	private final Clock clock;

	public WorkHoursService(PlanningService planning) {
		this(planning, Clock.systemDefaultZone());
	}

	public WorkHoursService(PlanningService planning, Clock clock) {
		this.planning = planning;
		this.clock = clock;
	}

	public double getTotalHours(Employee employee) {
		double totalHours = 0;

		for (Intervention intervention : planning.getInterventions()) {
			Employee assignedEmployee = intervention.getEmployee();

			if (assignedEmployee != null && assignedEmployee.getFirstName().equals(employee.getFirstName())
					&& assignedEmployee.getLastName().equals(employee.getLastName())) {

				totalHours += intervention.getDurationHours();
			}
		}

		return totalHours;
	}

	public double getWeeklyHours(Employee employee, LocalDate date) {
		LocalDate monday = date.with(DayOfWeek.MONDAY);
		LocalDateTime weekStart = monday.atStartOfDay();
		LocalDateTime weekEnd = monday.plusWeeks(1).atStartOfDay();
		double totalHours = 0;

		for (Intervention intervention : planning.getInterventions()) {
			Employee assignedEmployee = intervention.getEmployee();
			LocalDateTime interventionStart = LocalDateTime.of(intervention.getStartDate(),
					intervention.getStartTime());
			LocalDateTime interventionEnd = LocalDateTime.of(intervention.getEndDate(), intervention.getEndTime());

			if (assignedEmployee != null && assignedEmployee == employee && interventionEnd.isAfter(weekStart)
					&& interventionStart.isBefore(weekEnd)) {

				totalHours += getInterventionHoursInPeriod(intervention, weekStart, weekEnd);
			}
		}

		return totalHours;
	}

	public double getMonthlyHours(Employee employee, LocalDate date) {

		LocalDateTime monthStart = date.withDayOfMonth(1).atStartOfDay();
		LocalDateTime monthEnd = date.withDayOfMonth(1).plusMonths(1).atStartOfDay();
		double totalHours = 0;

		for (Intervention intervention : planning.getInterventions()) {
			Employee assignedEmployee = intervention.getEmployee();
			LocalDateTime interventionStart = LocalDateTime.of(intervention.getStartDate(),
					intervention.getStartTime());
			LocalDateTime interventionEnd = LocalDateTime.of(intervention.getEndDate(), intervention.getEndTime());

			if (assignedEmployee != null && assignedEmployee.getFirstName().equals(employee.getFirstName())
					&& assignedEmployee.getLastName().equals(employee.getLastName())
					&& interventionEnd.isAfter(monthStart) && interventionStart.isBefore(monthEnd)) {
				totalHours += getInterventionHoursInPeriod(intervention, monthStart, monthEnd);
			}
		}

		return totalHours;
	}

	public double getProjectedWeeklyOverrun(Employee employee, Intervention intervention) {

		LocalDate weekMonday = intervention.getStartDate().with(DayOfWeek.MONDAY);
		LocalDateTime interventionEnd = LocalDateTime.of(intervention.getEndDate(), intervention.getEndTime());
		double totalOverrun = 0.0;

		while (weekMonday.atStartOfDay().isBefore(interventionEnd)) {
			LocalDateTime weekStart = weekMonday.atStartOfDay();
			LocalDateTime weekEnd = weekMonday.plusWeeks(1).atStartOfDay();
			double currentHours = getWeeklyHours(employee, weekMonday);
			double projectedHours = currentHours;

			if (intervention.getEmployee() != employee) {
				projectedHours += getInterventionHoursInPeriod(intervention, weekStart, weekEnd);
			}

			totalOverrun += Math.max(0, projectedHours - employee.getContractHours());
			weekMonday = weekMonday.plusWeeks(1);
		}

		return totalOverrun;
	}

	public double getProjectedWeeklyOverrun(Employee employee, List<Intervention> interventionsToAssign) {
		TreeSet<LocalDate> weeks = new TreeSet<>();

		for (Intervention current : interventionsToAssign) {
			LocalDate monday = current.getStartDate().with(DayOfWeek.MONDAY);
			LocalDateTime end = LocalDateTime.of(current.getEndDate(), current.getEndTime());

			while (monday.atStartOfDay().isBefore(end)) {
				weeks.add(monday);
				monday = monday.plusWeeks(1);
			}
		}

		double totalOverrun = 0;

		for (LocalDate monday : weeks) {
			LocalDateTime weekStart = monday.atStartOfDay();
			LocalDateTime weekEnd = monday.plusWeeks(1).atStartOfDay();
			double projectedHours = getWeeklyHours(employee, monday);

			for (Intervention current : interventionsToAssign) {
				Employee assigned = current.getEmployee();
				boolean alreadyAssigned = assigned != null && assigned.getFirstName().equals(employee.getFirstName())
						&& assigned.getLastName().equals(employee.getLastName());

				if (!alreadyAssigned) {
					projectedHours += getInterventionHoursInPeriod(current, weekStart, weekEnd);
				}
			}

			totalOverrun += Math.max(0, projectedHours - employee.getContractHours());
		}

		return totalOverrun;
	}

	public static double getInterventionHoursInPeriod(Intervention intervention, LocalDateTime periodStart,
			LocalDateTime periodEnd) {

		LocalDateTime interventionStart = LocalDateTime.of(intervention.getStartDate(), intervention.getStartTime());
		LocalDateTime interventionEnd = LocalDateTime.of(intervention.getEndDate(), intervention.getEndTime());

		if (!interventionEnd.isAfter(periodStart) || !interventionStart.isBefore(periodEnd)) {
			return 0;
		}

		LocalDateTime countedStart = interventionStart;
		LocalDateTime countedEnd = interventionEnd;

		if (countedStart.isBefore(periodStart)) {
			countedStart = periodStart;
		}

		if (countedEnd.isAfter(periodEnd)) {
			countedEnd = periodEnd;
		}

		return Duration.between(countedStart, countedEnd).toMinutes() / 60.0;
	}

	public HoursAlert getHoursAlert(Employee employee, LocalDate date, boolean monthly) {
		double expectedHours = monthly ? employee.getContractHours() * 52 / 12 : employee.getContractHours();
		double assignedHours = monthly ? getMonthlyHours(employee, date) : getWeeklyHours(employee, date);
		long remainingMinutes = Math.round((expectedHours - assignedHours) * 60);

		if (remainingMinutes < 0) {
			return HoursAlert.ALERT;
		}

		if (remainingMinutes == 0 && expectedHours > 0) {
			return HoursAlert.TARGET_REACHED;
		}

		if (expectedHours <= 0) {
			return HoursAlert.IN_PROGRESS;
		}

		LocalDate periodStart = monthly ? date.withDayOfMonth(1)
				: date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate periodEnd = monthly ? periodStart.plusMonths(1) : periodStart.plusWeeks(1);
		long periodDays = ChronoUnit.DAYS.between(periodStart, periodEnd);
		long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(clock), periodEnd);
		remainingDays = Math.max(0, Math.min(periodDays, remainingDays));
		double remainingTimeRatio = remainingDays / (double) periodDays;
		double remainingHoursRatio = (remainingMinutes / 60.0) / expectedHours;
		boolean nearPeriodEnd = monthly ? remainingTimeRatio <= 0.20 : remainingDays <= 3;

		if (remainingDays == 0 || (nearPeriodEnd && remainingHoursRatio > 0.25)) {
			return HoursAlert.ALERT;
		}

		if (remainingHoursRatio > remainingTimeRatio + 0.15) {
			return HoursAlert.WATCH;
		}
		return HoursAlert.IN_PROGRESS;
	}
}
