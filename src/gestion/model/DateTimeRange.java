package gestion.model;

import java.time.LocalDateTime;

import gestion.util.PeriodUtils;

public record DateTimeRange(LocalDateTime start, LocalDateTime end) {

	public DateTimeRange {

		if (!PeriodUtils.isValidDateTimeRange(start, end)) {

			throw new IllegalArgumentException("Période invalide");
		}
	}
}
