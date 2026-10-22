package gestion.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

import gestion.model.DateRange;
import gestion.model.DateTimeRange;

public class PeriodUtils {

	public static boolean isValidPeriod(LocalDate startDate, LocalDate endDate) {

		if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
			return false;
		}

		return true;
	}

	public static boolean hasDateOverlap(DateRange period1, DateRange period2) {

		if (period1 == null || period2 == null) {
			throw new IllegalArgumentException("Période invalide");
		}

		if (period1.end().isBefore(period2.start()) || period2.end().isBefore(period1.start())) {
			return false;
		}

		return true;
	}

	public static boolean isValidDateTimeRange(LocalDateTime start, LocalDateTime end) {

		if (start == null || end == null || !end.isAfter(start)) {
			return false;
		}

		return true;
	}

	public static boolean hasDateTimeOverlap(DateTimeRange period1, DateTimeRange period2) {

		if (period1 == null || period2 == null) {
			throw new IllegalArgumentException("Période invalide");
		}

		if (period1.start().isBefore(period2.end()) && period1.end().isAfter(period2.start())) {
			return true;
		}

		return false;
	}
}
