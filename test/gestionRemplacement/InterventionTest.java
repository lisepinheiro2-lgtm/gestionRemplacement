package gestionRemplacement;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class InterventionTest {

	@Test
	void getDurationHours() {

		Intervention intervention = new Intervention(null, null, LocalDate.of(2026, 10, 3), LocalTime.of(10, 00),
				LocalDate.of(2026, 10, 3), LocalTime.of(12, 30));

		double durationHours = intervention.getDurationHours();

		assertEquals(2.5, durationHours);
	}

	@Test
	void getDurationHoursAcrossMidnight() {

		Intervention intervention = new Intervention(null, null, LocalDate.of(2026, 10, 3), LocalTime.of(23, 00),
				LocalDate.of(2026, 10, 4), LocalTime.of(1, 30));

		double durationHours = intervention.getDurationHours();

		assertEquals(2.5, durationHours);
	}

	@Test
	void getInterventionHoursSplitAcrossDays() {

		Intervention intervention = new Intervention(null, null, LocalDate.of(2026, 10, 3), LocalTime.of(23, 00),
				LocalDate.of(2026, 10, 4), LocalTime.of(1, 30));

		LocalDateTime dayOneStart = LocalDateTime.of(2026, 10, 3, 0, 0);
		LocalDateTime dayOneEnd = LocalDateTime.of(2026, 10, 4, 0, 0);

		double dayOneHours = Planning.getInterventionHoursInPeriod(intervention, dayOneStart, dayOneEnd);
		assertEquals(1.0, dayOneHours);

		LocalDateTime dayTwoEnd = LocalDateTime.of(2026, 10, 5, 0, 0);

		double dayTwoHours = Planning.getInterventionHoursInPeriod(intervention, dayOneEnd, dayTwoEnd);
		assertEquals(1.5, dayTwoHours);
	}

	@Test
	void getDurationHoursThirtyMinutes() {

		Intervention intervention = new Intervention(null, null, LocalDate.of(2026, 10, 3), LocalTime.of(10, 0),
				LocalDate.of(2026, 10, 3), LocalTime.of(10, 30));

		double durationHours = intervention.getDurationHours();

		assertEquals(0.5, durationHours);
	}

	@Test
	void getDurationHoursSeveralHours() {

		Intervention intervention = new Intervention(null, null, LocalDate.of(2026, 10, 3), LocalTime.of(8, 15),
				LocalDate.of(2026, 10, 3), LocalTime.of(12, 0));

		double durationHours = intervention.getDurationHours();

		assertEquals(3.75, durationHours);
	}

	@Test
	void getDurationHoursExactlyOneDay() {

		Intervention intervention = new Intervention(null, null, LocalDate.of(2026, 10, 3), LocalTime.of(10, 0),
				LocalDate.of(2026, 10, 4), LocalTime.of(10, 0));

		double durationHours = intervention.getDurationHours();

		assertEquals(24.0, durationHours);
	}

	@Test
	void getDurationHoursSeveralDays() {

		Intervention intervention = new Intervention(null, null, LocalDate.of(2026, 10, 3), LocalTime.of(10, 0),
				LocalDate.of(2026, 10, 5), LocalTime.of(12, 30));

		double durationHours = intervention.getDurationHours();

		assertEquals(50.5, durationHours);
	}

	@Test
	void getDurationHoursZeroDuration() {

		Intervention intervention = new Intervention(null, null, LocalDate.of(2026, 10, 3), LocalTime.of(10, 0),
				LocalDate.of(2026, 10, 3), LocalTime.of(10, 0));

		double durationHours = intervention.getDurationHours();

		assertEquals(0.0, durationHours);
	}
}
