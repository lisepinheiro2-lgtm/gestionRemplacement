package gestionRemplacement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

public class PlanningTest {

	@Test
	void interventionCompletelyInsidePeriod() {

		Intervention intervention = new Intervention(null, null, LocalDate.of(2026, 10, 3), LocalTime.of(10, 0),
				LocalDate.of(2026, 10, 3), LocalTime.of(12, 0));

		LocalDateTime periodStart = LocalDateTime.of(2026, 10, 3, 0, 0);
		LocalDateTime periodEnd = LocalDateTime.of(2026, 10, 4, 0, 0);

		double hours = Planning.getInterventionHoursInPeriod(intervention, periodStart, periodEnd);

		assertEquals(2.0, hours);
	}

	@Test
	void periodCompletelyInsideIntervention() {

		Intervention intervention = new Intervention(null, null, LocalDate.of(2026, 10, 3), LocalTime.of(8, 0),
				LocalDate.of(2026, 10, 3), LocalTime.of(18, 0));

		LocalDateTime periodStart = LocalDateTime.of(2026, 10, 3, 10, 0);
		LocalDateTime periodEnd = LocalDateTime.of(2026, 10, 3, 12, 30);

		double hours = Planning.getInterventionHoursInPeriod(intervention, periodStart, periodEnd);

		assertEquals(2.5, hours);
	}

	@Test
	void interventionStartsBeforePeriod() {

		Intervention intervention = new Intervention(null, null, LocalDate.of(2026, 10, 3), LocalTime.of(8, 0),
				LocalDate.of(2026, 10, 3), LocalTime.of(11, 0));

		LocalDateTime periodStart = LocalDateTime.of(2026, 10, 3, 10, 0);
		LocalDateTime periodEnd = LocalDateTime.of(2026, 10, 3, 12, 0);

		double hours = Planning.getInterventionHoursInPeriod(intervention, periodStart, periodEnd);

		assertEquals(1.0, hours);
	}

	@Test
	void interventionEndsAfterPeriod() {

		Intervention intervention = new Intervention(null, null, LocalDate.of(2026, 10, 3), LocalTime.of(11, 0),
				LocalDate.of(2026, 10, 3), LocalTime.of(14, 0));

		LocalDateTime periodStart = LocalDateTime.of(2026, 10, 3, 10, 0);
		LocalDateTime periodEnd = LocalDateTime.of(2026, 10, 3, 12, 0);

		double hours = Planning.getInterventionHoursInPeriod(intervention, periodStart, periodEnd);

		assertEquals(1.0, hours);
	}

	@Test
	void interventionBeforePeriodDoesNotCount() {

		Intervention intervention = new Intervention(null, null, LocalDate.of(2026, 10, 3), LocalTime.of(8, 0),
				LocalDate.of(2026, 10, 3), LocalTime.of(10, 0));

		LocalDateTime periodStart = LocalDateTime.of(2026, 10, 3, 10, 0);
		LocalDateTime periodEnd = LocalDateTime.of(2026, 10, 3, 12, 0);

		double hours = Planning.getInterventionHoursInPeriod(intervention, periodStart, periodEnd);

		assertEquals(0.0, hours);
	}

	@Test
	void interventionAfterPeriodDoesNotCount() {

		Intervention intervention = new Intervention(null, null, LocalDate.of(2026, 10, 3), LocalTime.of(12, 0),
				LocalDate.of(2026, 10, 3), LocalTime.of(14, 0));

		LocalDateTime periodStart = LocalDateTime.of(2026, 10, 3, 10, 0);
		LocalDateTime periodEnd = LocalDateTime.of(2026, 10, 3, 12, 0);

		double hours = Planning.getInterventionHoursInPeriod(intervention, periodStart, periodEnd);

		assertEquals(0.0, hours);
	}

	@Test
	void interventionAcrossMonthBoundaryOctoberPart() {

		Intervention intervention = new Intervention(null, null, LocalDate.of(2026, 10, 31), LocalTime.of(22, 0),
				LocalDate.of(2026, 11, 1), LocalTime.of(3, 0));

		LocalDateTime periodStart = LocalDateTime.of(2026, 10, 1, 0, 0);
		LocalDateTime periodEnd = LocalDateTime.of(2026, 11, 1, 0, 0);

		double hours = Planning.getInterventionHoursInPeriod(intervention, periodStart, periodEnd);

		assertEquals(2.0, hours);
	}

	@Test
	void interventionAcrossMonthBoundaryNovemberPart() {

		Intervention intervention = new Intervention(null, null, LocalDate.of(2026, 10, 31), LocalTime.of(22, 0),
				LocalDate.of(2026, 11, 1), LocalTime.of(3, 0));

		LocalDateTime periodStart = LocalDateTime.of(2026, 11, 1, 0, 0);
		LocalDateTime periodEnd = LocalDateTime.of(2026, 12, 1, 0, 0);

		double hours = Planning.getInterventionHoursInPeriod(intervention, periodStart, periodEnd);

		assertEquals(3.0, hours);
	}
}
