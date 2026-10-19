package gestion.model;

import java.time.LocalDate;

import gestion.util.PeriodUtils;

public record DateRange(LocalDate start, LocalDate end) {

	public DateRange {

		if (!PeriodUtils.isValidPeriod(start, end)) {

			throw new IllegalArgumentException("Période invalide");
		}
	}
}
