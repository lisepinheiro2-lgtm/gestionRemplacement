package gestionRemplacement;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;

public class FormatUtils {

	public static LocalDate parseDate(String text) {

		DateTimeFormatter longYear = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
		DateTimeFormatter shortYear = new DateTimeFormatterBuilder().appendPattern("d/M/")
				.appendValueReduced(ChronoField.YEAR, 2, 2, 2000).toFormatter().withResolverStyle(ResolverStyle.STRICT);

		try {

			return LocalDate.parse(text.trim(), longYear);

		} catch (DateTimeParseException e) {

			return LocalDate.parse(text.trim(), shortYear);
		}
	}

	public static String formatDay(DayOfWeek day) {

		switch (day) {
		case MONDAY:
			return "Lundi";
		case TUESDAY:
			return "Mardi";
		case WEDNESDAY:
			return "Mercredi";
		case THURSDAY:
			return "Jeudi";
		case FRIDAY:
			return "Vendredi";
		case SATURDAY:
			return "Samedi";
		case SUNDAY:
			return "Dimanche";
		}

		return "";
	}

	public static String formatTime(LocalTime time) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH'h'mm");
		return time.format(formatter);
	}

	public static String formatDate(LocalDate date) {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu");

		return date.format(formatter);
	}

	public static LocalTime parseTime(String text) {

		DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendValue(ChronoField.HOUR_OF_DAY)
				.appendLiteral('h').optionalStart().appendValue(ChronoField.MINUTE_OF_HOUR, 2).optionalEnd()
				.parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0).toFormatter().withResolverStyle(ResolverStyle.STRICT);

		return LocalTime.parse(text.trim().toLowerCase(), formatter);
	}

	public static double parseContractHours(String text) {

		text = text.trim().toLowerCase().replace(",", ".");

		if (text.contains("h")) {
			String[] parts = text.split("h", -1);
			int hours = Integer.parseInt(parts[0]);
			int minutes = 0;

			if (!parts[1].isBlank()) {
				minutes = Integer.parseInt(parts[1]);
			}

			if (minutes < 0 || minutes > 59) {
				throw new NumberFormatException();
			}

			return hours + minutes / 60.0;
		}

		return Double.parseDouble(text);
	}

	public static String formatContractHours(double hours) {

		int totalMinutes = (int) Math.round(hours * 60);
		int hour = totalMinutes / 60;
		int minutes = totalMinutes % 60;

		return String.format("%dh%02d", hour, minutes);
	}

}
