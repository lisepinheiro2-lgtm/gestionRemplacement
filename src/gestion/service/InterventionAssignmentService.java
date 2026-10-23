package gestion.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import gestion.model.Client;
import gestion.model.DateTimeRange;
import gestion.model.Employee;
import gestion.model.Intervention;
import gestion.model.RecurringIntervention;
import gestion.repository.CsvStore;
import gestion.util.DomainValidation;
import gestion.util.PeriodUtils;

public final class InterventionAssignmentService {

	private final PlanningService planning;
	private final RecurringInterventionService recurring;

	public record State(Client client, Employee employee, DateTimeRange period, String recurringId,
			String sourceAbsenceId) {

		static State of(Intervention intervention) {
			return new State(intervention.getClient(), intervention.getEmployee(),
					DomainValidation.intervention(intervention), intervention.getRecurringId(),
					intervention.getSourceAbsenceId());
		}
	}

	public record SeriesState(RecurringIntervention series, Employee employee) {
	}

	public record Plan(List<Intervention> selected, List<Intervention> targets, Employee destination,
			boolean wholeSeries, Map<Intervention, State> before, Map<String, SeriesState> series,
			List<Intervention> releases) {

		public Plan {
			selected = List.copyOf(selected);
			targets = List.copyOf(targets);
			before = Map.copyOf(before);
			series = Map.copyOf(series);
			releases = List.copyOf(releases);
		}
	}

	public enum ConflictType {
		PLANNING, SELECTION
	}

	public record Conflict(Intervention intervention, Intervention source, ConflictType type) {

		public boolean canRelease() {
			return type == ConflictType.PLANNING;
		}

		public String reason() {
			return canRelease() ? "Planning existant" : "Entre interventions sélectionnées";
		}

		public DateTimeRange intersection() {
			DateTimeRange first = DomainValidation.intervention(intervention);
			DateTimeRange second = DomainValidation.intervention(source);
			return new DateTimeRange(first.start().isAfter(second.start()) ? first.start() : second.start(),
					first.end().isBefore(second.end()) ? first.end() : second.end());
		}
	}

	public InterventionAssignmentService(PlanningService planning, RecurringInterventionService recurring) {
		this.planning = Objects.requireNonNull(planning);
		this.recurring = recurring;
	}

	public static boolean isModifiable(Intervention intervention, LocalDateTime now) {
		return DomainValidation.intervention(intervention).start().isAfter(now);
	}

	public Plan preview(List<Intervention> selected, Employee destination, boolean wholeSeries) {
		if (selected == null || selected.isEmpty())
			throw new IllegalArgumentException("Veuillez sélectionner au moins une intervention.");

		Set<Intervention> targets = new LinkedHashSet<>();
		Map<String, SeriesState> series = new LinkedHashMap<>();
		LocalDateTime now = LocalDateTime.now();

		for (Intervention intervention : selected) {
			if (!planning.getInterventions().contains(intervention))
				throw new IllegalArgumentException("Une intervention sélectionnée n'existe plus.");

			String id = intervention.getRecurringId();

			if (wholeSeries && id != null) {
				if (recurring == null || !planning.getDirectory().equals(recurring.getDirectory()))
					throw new IllegalStateException("Le service des récurrences n'est pas disponible.");

				RecurringIntervention reference = recurring.getRecurringInterventions().stream()
						.filter(r -> id.equals(r.getId())).findFirst()
						.orElseThrow(() -> new IllegalArgumentException("Cette série n'existe plus."));
				List<Intervention> future = planning.getFutureInterventionsByRecurringId(id, now).stream()
						.filter(i -> isModifiable(i, now)).toList();

				if (!future.isEmpty()) {
					series.putIfAbsent(id, new SeriesState(reference, reference.getEmployee()));
					targets.addAll(future);
				}
			} else if (isModifiable(intervention, now)) {
				targets.add(intervention);
			}
		}

		Map<Intervention, State> before = new LinkedHashMap<>();
		targets.forEach(i -> before.put(i, State.of(i)));

		return new Plan(selected, new ArrayList<>(targets), destination, wholeSeries, before, series, List.of());
	}

	public List<Conflict> conflicts(Plan plan) {
		
		if (plan.destination() == null)
			return List.of();

		List<Conflict> conflicts = new ArrayList<>();
		Set<Intervention> targets = new LinkedHashSet<>(plan.targets());

		for (Intervention target : plan.targets()) {
			for (Intervention other : planning.getInterventions()) {
				if (other != target && !targets.contains(other) && other.getEmployee() == plan.destination()
						&& overlap(target, other))
					conflicts.add(new Conflict(other, target, ConflictType.PLANNING));
			}
		}

		List<Intervention> selected = new ArrayList<>(targets);

		for (int first = 0; first < selected.size(); first++) {
			for (int second = first + 1; second < selected.size(); second++) {
				if (overlap(selected.get(first), selected.get(second)))
					conflicts.add(new Conflict(selected.get(second), selected.get(first), ConflictType.SELECTION));
			}
		}

		return conflicts.stream().sorted(java.util.Comparator
				.comparing((Conflict c) -> DomainValidation.intervention(c.source()).start())
				.thenComparing(c -> DomainValidation.intervention(c.intervention()).start())).toList();
	}

	private boolean overlap(Intervention first, Intervention second) {
		return PeriodUtils.hasDateTimeOverlap(DomainValidation.intervention(first),
				DomainValidation.intervention(second));
	}

	public Plan withReleases(Plan plan, List<Intervention> releases) {
		
		Set<Intervention> allowed = new LinkedHashSet<>();
		LocalDateTime now = LocalDateTime.now();
		conflicts(plan).stream().filter(Conflict::canRelease)
				.filter(c -> isModifiable(c.intervention(), now)).forEach(c -> allowed.add(c.intervention()));
		Set<Intervention> unique = new LinkedHashSet<>(releases);

		if (!allowed.containsAll(unique))
			throw new IllegalArgumentException("Une désattribution ne correspond plus à un conflit de cette attribution.");

		Map<Intervention, State> before = new LinkedHashMap<>(plan.before());
		unique.forEach(i -> before.put(i, State.of(i)));
		return new Plan(plan.selected(), plan.targets(), plan.destination(), plan.wholeSeries(), before,
				plan.series(), new ArrayList<>(unique));
	}

	public void validate(Plan plan) {
		
		if (plan.targets().isEmpty())
			throw new IllegalArgumentException("Aucune occurrence modifiable : les interventions passées ou commencées sont exclues.");

		LocalDateTime now = LocalDateTime.now();

		for (Intervention intervention : plan.before().keySet()) {
			if (!planning.getInterventions().contains(intervention)
					|| !State.of(intervention).equals(plan.before().get(intervention)))
				throw new IllegalArgumentException("Le planning a changé : veuillez recalculer le prévisionnel.");

			if (!isModifiable(intervention, now))
				throw new IllegalArgumentException("Une intervention a déjà commencé : actualisez la sélection ou la portée. Aucun changement enregistré.");
		}

		if (plan.destination() != null)
			planning.validateEmployeeAssignment(plan.targets(), plan.destination(), plan.releases());
	}

	public void apply(Plan plan) {
		
		CsvStore.transaction(planning.getDirectory(), () -> {
			validate(plan);

			if (plan.wholeSeries()) {
				Plan current = preview(plan.selected(), plan.destination(), true);

				if (!new LinkedHashSet<>(current.targets()).equals(new LinkedHashSet<>(plan.targets())))
					throw new IllegalArgumentException(
							"Une occurrence a commencé ou la série a changé : veuillez recalculer le prévisionnel.");

				for (var entry : plan.series().entrySet()) {
					SeriesState previous = entry.getValue();
					SeriesState actual = current.series().get(entry.getKey());

					if (actual == null || actual.series() != previous.series()
							|| actual.employee() != previous.employee())
						throw new IllegalArgumentException(
								"La récurrence a changé : veuillez recalculer le prévisionnel.");
				}
			}

			if (!plan.releases().isEmpty())
				planning.unassignEmployee(plan.releases());

			if (plan.destination() == null) {
				planning.unassignEmployee(plan.targets());
			} else {
				planning.assignEmployee(plan.targets(), plan.destination());
			}

			for (String id : plan.series().keySet())
				recurring.assignEmployeeByRecurringId(id, plan.destination());
		});
	}
}
