package gestion.util;

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

		if (text == null)
			throw new NumberFormatException("Heures manquantes.");
		text = text.trim().toLowerCase(java.util.Locale.ROOT).replace(',', '.');
		double value;

		if (text.matches("\\d+h(?:[0-5]?\\d)?")) {
			String[] parts = text.split("h", -1);
			value = Double.parseDouble(parts[0]) + (parts[1].isEmpty() ? 0 : Integer.parseInt(parts[1])) / 60.0;
		} else if (text.matches("(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:e[+-]?\\d+)?")) {
			value = Double.parseDouble(text);
		} else
			throw new NumberFormatException("Format d'heures invalide.");

		if (!Double.isFinite(value) || value < 0)
			throw new NumberFormatException("Heures finies et non négatives attendues.");
		return value;
	}

	public static String formatContractHours(double hours) {

		int totalMinutes = (int) Math.round(hours * 60);
		int absoluteMinutes = Math.abs(totalMinutes);
		String sign = totalMinutes < 0 ? "-" : "";
		int hour = absoluteMinutes / 60;
		int minutes = absoluteMinutes % 60;

		return sign + String.format("%dh%02d", hour, minutes);
	}

}
