package gestion.ui.personnel;

import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.Locale;
import java.util.function.Predicate;

import gestion.model.Intervention;
import gestion.util.DomainValidation;
import gestion.util.FormatUtils;

final class InterventionSearch {

	private InterventionSearch() {
	}

	static Predicate<Intervention> filter(String text) {
		String query = text.strip();

		if (query.isEmpty())
			return intervention -> true;

		String dateText = query.replace('-', '/');

		if (dateText.matches("\\d{2}/\\d{2}(?:/(?:\\d{2}|\\d{4}))?")) {
			try {
				if (dateText.length() == 5) {
					MonthDay day = MonthDay.from(FormatUtils.parseDate(dateText + "/2000"));
					return intervention -> occursOn(intervention, day);
				}

				LocalDate date = FormatUtils.parseDate(dateText);
				return intervention -> occursOn(intervention, date);
			} catch (DateTimeException exception) {
				return intervention -> false;
			}
		}

		String name = normalize(query);
		return intervention -> matchesName(intervention.getClient().getFirstName(),
				intervention.getClient().getLastName(), name)
				|| intervention.getEmployee() != null && matchesName(intervention.getEmployee().getFirstName(),
						intervention.getEmployee().getLastName(), name);
	}

	private static boolean matchesName(String firstName, String lastName, String query) {
		String first = normalize(firstName);
		String last = normalize(lastName);
		return (first + " " + last).contains(query) || (last + " " + first).contains(query);
	}

	private static String normalize(String value) {
		return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
				.toLowerCase(Locale.ROOT).strip().replaceAll("\\s+", " ");
	}

	private static boolean occursOn(Intervention intervention, MonthDay day) {
		for (int year = intervention.getStartDate().getYear(); year <= intervention.getEndDate().getYear(); year++) {
			if (day.isValidYear(year) && occursOn(intervention, day.atYear(year)))
				return true;
		}

		return false;
	}

	private static boolean occursOn(Intervention intervention, LocalDate date) {
		var range = DomainValidation.intervention(intervention);
		return range.start().isBefore(date.plusDays(1).atStartOfDay()) && range.end().isAfter(date.atStartOfDay());
	}
}
