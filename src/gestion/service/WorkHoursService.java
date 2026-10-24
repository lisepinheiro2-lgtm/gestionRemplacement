package gestion.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

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

			if (assignedEmployee != null && assignedEmployee.getEmployeeID().equals(employee.getEmployeeID())) {

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

			if (assignedEmployee == null || !assignedEmployee.getEmployeeID().equals(employee.getEmployeeID()))
				continue;

			LocalDateTime interventionStart = LocalDateTime.of(intervention.getStartDate(),
					intervention.getStartTime());
			LocalDateTime interventionEnd = LocalDateTime.of(intervention.getEndDate(), intervention.getEndTime());

			if (interventionEnd.isAfter(weekStart)
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

			if (assignedEmployee == null || !assignedEmployee.getEmployeeID().equals(employee.getEmployeeID()))
				continue;

			LocalDateTime interventionStart = LocalDateTime.of(intervention.getStartDate(),
					intervention.getStartTime());
			LocalDateTime interventionEnd = LocalDateTime.of(intervention.getEndDate(), intervention.getEndTime());

			if (interventionEnd.isAfter(monthStart) && interventionStart.isBefore(monthEnd)) {
				totalHours += getInterventionHoursInPeriod(intervention, monthStart, monthEnd);
			}
		}

		return totalHours;
	}

	public Map<UUID, Double> getHoursByEmployee(LocalDate date, boolean monthly) {
		LocalDate start = monthly ? date.withDayOfMonth(1) : date.with(DayOfWeek.MONDAY);
		LocalDateTime periodStart = start.atStartOfDay();
		LocalDateTime periodEnd = (monthly ? start.plusMonths(1) : start.plusWeeks(1)).atStartOfDay();
		Map<UUID, Double> hours = new HashMap<>();

		for (Intervention intervention : planning.getInterventions()) {
			Employee employee = intervention.getEmployee();

			if (employee == null)
				continue;

			double portion = getInterventionHoursInPeriod(intervention, periodStart, periodEnd);

			if (portion != 0)
				hours.merge(employee.getEmployeeID(), portion, Double::sum);
		}

		return Map.copyOf(hours);
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
				boolean alreadyAssigned = assigned != null && assigned.getEmployeeID().equals(employee.getEmployeeID());

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

	public double getProjectedHours(Employee employee, LocalDate date, boolean monthly,
			List<Intervention> targets, Employee destination) {
		return getProjectedHours(employee, date, monthly, targets, destination, List.of());
	}

	public double getProjectedHours(Employee employee, LocalDate date, boolean monthly,
			List<Intervention> targets, Employee destination, List<Intervention> releases) {

		LocalDate start = monthly ? date.withDayOfMonth(1) : date.with(DayOfWeek.MONDAY);
		LocalDate end = monthly ? start.plusMonths(1) : start.plusWeeks(1);
		double hours = monthly ? getMonthlyHours(employee, date) : getWeeklyHours(employee, date);

		for (Intervention intervention : new java.util.LinkedHashSet<>(targets)) {
			double portion = getInterventionHoursInPeriod(intervention, start.atStartOfDay(), end.atStartOfDay());

			if (intervention.getEmployee() == employee)
				hours -= portion;

			if (destination == employee)
				hours += portion;
		}

		for (Intervention release : new java.util.LinkedHashSet<>(releases)) {
			if (release.getEmployee() == employee && !targets.contains(release))
				hours -= getInterventionHoursInPeriod(release, start.atStartOfDay(), end.atStartOfDay());
		}

		return hours;
	}

	public HoursAlert getHoursAlert(Employee employee, LocalDate date, boolean monthly) {

		double assignedHours = monthly ? getMonthlyHours(employee, date) : getWeeklyHours(employee, date);
		return getHoursAlert(employee, date, monthly, assignedHours);
	}

	public HoursAlert getHoursAlert(Employee employee, LocalDate date, boolean monthly, double assignedHours) {

		double expectedHours = monthly ? employee.getContractHours() * 52 / 12 : employee.getContractHours();
		long remainingMinutes = Math.round((expectedHours - assignedHours) * 60);

		if (remainingMinutes < 0)
			return HoursAlert.ALERT;

		if (expectedHours <= 0)
			return HoursAlert.IN_PROGRESS;

		if (remainingMinutes == 0)
			return HoursAlert.TARGET_REACHED;

		double assignedRatio = assignedHours / expectedHours;

		if (assignedRatio < 0.40)
			return HoursAlert.ALERT;

		LocalDate periodStart = monthly ? date.withDayOfMonth(1)
				: date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
		LocalDate periodEnd = monthly ? periodStart.plusMonths(1) : periodStart.plusWeeks(1);

		long periodDays = ChronoUnit.DAYS.between(periodStart, periodEnd);
		long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(clock), periodEnd);
		remainingDays = Math.max(0, Math.min(periodDays, remainingDays));
		boolean nearPeriodEnd = monthly ? remainingDays <= periodDays * 0.20 : remainingDays <= 3;

		if (nearPeriodEnd)
			return HoursAlert.ALERT;

		long elapsedDays = periodDays - remainingDays;

		if (assignedRatio <= 0.60 || elapsedDays >= periodDays / 3)
			return HoursAlert.WATCH;
		return HoursAlert.IN_PROGRESS;
	}
}
