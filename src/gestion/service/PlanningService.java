package gestion.service;

import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import gestion.model.Client;
import gestion.model.DateRange;
import gestion.model.DateTimeRange;
import gestion.model.Employee;
import gestion.model.Intervention;
import gestion.model.RecurringIntervention;
import gestion.repository.InterventionCsvRepository;
import gestion.util.PeriodUtils;
import gestion.util.DomainValidation;
import gestion.repository.CsvStore;
import java.util.function.Consumer;

public class PlanningService {

	private ArrayList<Intervention> interventions = new ArrayList<>();

	private final InterventionCsvRepository repository;
    private final PersonnelService personnel;
    private final ClientService clients;
    private java.util.function.BiPredicate<Employee, DateTimeRange> absenceCheck = (employee, period) -> false;
    public void setAbsenceCheck(java.util.function.BiPredicate<Employee, DateTimeRange> check) {
        absenceCheck = java.util.Objects.requireNonNull(check);
    }
    public Path getDirectory() { return repository.getDirectory(); }
    private void validateCandidate(Intervention candidate) {
        DomainValidation.intervention(candidate);
        clients.requireKnown(candidate.getClient());
        if (candidate.getEmployee() != null) personnel.requireKnown(candidate.getEmployee());
    }
    private void changeList(Consumer<ArrayList<Intervention>> mutation) {
        ArrayList<Intervention> previous = new ArrayList<>(interventions);
        ServiceChanges.apply(getDirectory(), () -> mutation.accept(interventions), this::saveInterventions,
                () -> { interventions.clear(); interventions.addAll(previous); });
    }

	public PlanningService(PersonnelService personnel, ClientService clients) {
		this(personnel, clients, new InterventionCsvRepository(Path.of(".")));
	}

	public PlanningService(PersonnelService personnel, ClientService clients, InterventionCsvRepository repository) {
		this.repository = repository;
        this.personnel = personnel;
        this.clients = clients;

		interventions = repository.load(personnel.getEmployees(), clients.getClients());
        personnel.protectReferences(employee -> interventions.stream().anyMatch(i -> i.getEmployee() == employee));
        clients.protectReferences(client -> interventions.stream().anyMatch(i -> i.getClient() == client));
	}

	public void addIntervention(Intervention intervention) {
        validateCandidate(intervention);
        if (interventions.contains(intervention)) throw new IllegalArgumentException("Cette intervention existe déjà.");
        if (intervention.getEmployee() != null && hasOverlap(intervention.getEmployee(), intervention))
            throw new IllegalArgumentException("Cet intervenant possède déjà une intervention sur ce créneau.");
        changeList(list -> list.add(intervention));
    }

	private void rollbackIntervention(Intervention intervention, DateTimeRange oldPeriod, Client oldClient,
			Employee oldEmployee) {

		LocalDate startDate = oldPeriod.start().toLocalDate();
		LocalTime startTime = oldPeriod.start().toLocalTime();

		LocalDate endDate = oldPeriod.end().toLocalDate();
		LocalTime endTime = oldPeriod.end().toLocalTime();

		intervention.setClient(oldClient);
		intervention.setEmployee(oldEmployee);
		intervention.setStartDate(startDate);
		intervention.setStartTime(startTime);
		intervention.setEndDate(endDate);
		intervention.setEndTime(endTime);
	}

	public void editIntervention(int index, Client client, Employee employee, LocalDate startDate, LocalTime startTime,
			LocalDate endDate, LocalTime endTime) {

		if (index < 0 || index >= interventions.size()) {
			throw new IllegalArgumentException("Veuillez selectionner une intervention.");
		}

		Intervention intervention = interventions.get(index);

		if (intervention == null) {
			throw new IllegalArgumentException("Cette intervention n'existe pas.");
		}

        validateCandidate(new Intervention(client, employee, startDate, startTime, endDate, endTime));
        if (intervention.getSourceAbsenceId() != null && (intervention.getClient() != client
                || !intervention.getStartDate().equals(startDate) || !intervention.getStartTime().equals(startTime)
                || !intervention.getEndDate().equals(endDate) || !intervention.getEndTime().equals(endTime)))
            throw new IllegalArgumentException("Cette intervention est liée à une absence : modifier ses dates ou son client demande une gestion du groupe qui n'est pas encore disponible.");

		if (employee != null) {

			Intervention modifiedIntervention = new Intervention(client, employee, startDate, startTime, endDate,
					endTime);

			if (hasOverlap(employee, modifiedIntervention, intervention)) {
				throw new IllegalArgumentException("Ce changement causerait un chevauchement.");
			}
		}

		LocalDateTime start = LocalDateTime.of(intervention.getStartDate(), intervention.getStartTime());
		LocalDateTime end = LocalDateTime.of(intervention.getEndDate(), intervention.getEndTime());
		DateTimeRange oldPeriod = new DateTimeRange(start, end);
		Client oldClient = intervention.getClient();
		Employee oldEmployee = intervention.getEmployee();

		try {

			intervention.setClient(client);
			intervention.setEmployee(employee);
			intervention.setStartDate(startDate);
			intervention.setStartTime(startTime);
			intervention.setEndDate(endDate);
			intervention.setEndTime(endTime);

            saveInterventions();
            CsvStore.onRollback(getDirectory(), () -> rollbackIntervention(intervention, oldPeriod, oldClient, oldEmployee));

        } catch (RuntimeException e) {
            rollbackIntervention(intervention, oldPeriod, oldClient, oldEmployee);
			throw e;
		}
	}

	public ArrayList<Intervention> getInterventionsForDay(LocalDate day) {
        if (day == null) throw new IllegalArgumentException("Veuillez renseigner un jour.");

		ArrayList<Intervention> interventionsForDay = new ArrayList<>();

		for (Intervention intervention : interventions) {

			if (!day.isBefore(intervention.getStartDate()) && !day.isAfter(intervention.getEndDate())) {
				interventionsForDay.add(intervention);
			}
		}

		return interventionsForDay;
	}

	public ArrayList<Intervention> getInterventionsForPeriod(DateRange period) {

		if (period == null) {
			throw new IllegalArgumentException("Veuillez renseigner une période");
		}

		ArrayList<Intervention> interventionsForPeriod = new ArrayList<Intervention>();

		for (Intervention intervention : interventions) {

			DateRange interventionPeriod = new DateRange(intervention.getStartDate(), intervention.getEndDate());

			if (PeriodUtils.hasDateOverlap(interventionPeriod, period)) {
				interventionsForPeriod.add(intervention);
			}
		}

		return interventionsForPeriod;
	}

	public ArrayList<Intervention> getUnassignedInterventionsForPeriod(DateRange period) {

		ArrayList<Intervention> unassignedInterventions = new ArrayList<Intervention>();

		for (Intervention intervention : getInterventionsForPeriod(period)) {

			if (intervention.getEmployee() == null) {
				unassignedInterventions.add(intervention);
			}
		}

		return unassignedInterventions;
	}

	public ArrayList<Intervention> getAssignedInterventionsForPeriod(DateRange period, Employee employee) {
        personnel.requireKnown(employee);

		if (employee == null) {
			throw new IllegalArgumentException("Veuillez renseigner un employee");
		}

		ArrayList<Intervention> assignedInterventions = new ArrayList<Intervention>();

		for (Intervention intervention : getInterventionsForPeriod(period)) {

			if (intervention.getEmployee() == employee) {
				assignedInterventions.add(intervention);
			}
		}

		return assignedInterventions;
	}

	public ArrayList<Intervention> getOtherAssignedInterventionsForPeriod(DateRange period, Employee employee) {
        personnel.requireKnown(employee);

		if (employee == null) {
			throw new IllegalArgumentException("Veuillez renseigner un employee");
		}

		ArrayList<Intervention> assignedAtOtherInterventions = new ArrayList<Intervention>();

		for (Intervention intervention : getInterventionsForPeriod(period)) {

			if (intervention.getEmployee() != null && intervention.getEmployee() != employee) {
				assignedAtOtherInterventions.add(intervention);
			}
		}

		return assignedAtOtherInterventions;
	}

	private boolean interventionsOverlap(Intervention first, Intervention second) {
        return PeriodUtils.hasDateTimeOverlap(DomainValidation.intervention(first), DomainValidation.intervention(second));
    }

	public boolean hasOverlap(Client client, Intervention interventionToCheck, Intervention interventionToIgnore) {
        clients.requireKnown(client);
        DomainValidation.intervention(interventionToCheck);

		for (Intervention intervention : interventions) {

			if (intervention == interventionToCheck || intervention == interventionToIgnore) {
				continue;
			}

			Client interventionClient = intervention.getClient();

			if (interventionClient == null || !interventionClient.equals(client)) {
				continue;
			}

			if (interventionsOverlap(interventionToCheck, intervention)) {
				return true;
			}
		}
		return false;
	}

	public boolean hasOverlap(Client client, Intervention interventionToCheck) {
		return hasOverlap(client, interventionToCheck, null);
	}

	public boolean hasOverlap(Employee employee, Intervention interventionToCheck) {
		return hasOverlap(employee, interventionToCheck, null);
	}

	public boolean hasOverlap(Employee employee, Intervention interventionToCheck, Intervention interventionToIgnore) {
        personnel.requireKnown(employee);
        DateTimeRange candidatePeriod = DomainValidation.intervention(interventionToCheck);
        if (absenceCheck.test(employee, candidatePeriod)) return true;

		for (Intervention intervention : interventions) {

			if (intervention == interventionToCheck || intervention == interventionToIgnore) {
				continue;
			}

			Employee assignedEmployee = intervention.getEmployee();

			if (assignedEmployee == null) {
				continue;
			}

            if (assignedEmployee != employee) {
				continue;
			}

			if (interventionsOverlap(interventionToCheck, intervention)) {
				return true;
			}
		}

		return false;
	}

	private void applyAssignments(List<Intervention> targets, Employee newEmployee) {
        ArrayList<Employee> previous = new ArrayList<>();
        for (Intervention target : targets) previous.add(target.getEmployee());
        ServiceChanges.apply(getDirectory(), () -> targets.forEach(target -> target.setEmployee(newEmployee)),
                this::saveInterventions, () -> {
                    for (int i = 0; i < targets.size(); i++) targets.get(i).setEmployee(previous.get(i));
                });
    }

	public void removeClientInterventions(Client client, LocalDate startDate, LocalDate endDate) {
        clients.requireKnown(client);
        new DateRange(startDate, endDate);
        changeList(list -> list.removeIf(i -> i.getClient() == client && !i.getEndDate().isBefore(startDate)
                && !i.getStartDate().isAfter(endDate)));
    }

	public void unassignEmployeeInterventions(Employee employee, LocalDate startDate, LocalDate endDate) {

		if (employee == null) {
			throw new IllegalArgumentException("Veuillez sélectionner un employé.");
		}

        personnel.requireKnown(employee);
		DateRange period = new DateRange(startDate, endDate);
		ArrayList<Intervention> targets = getAssignedInterventionsForPeriod(period, employee);

		if (targets.isEmpty()) {
			return;
		}

		applyAssignments(targets, null);

	}

	public boolean replaceEmployeeInterventions(Employee employeeToReplace, Employee replacementEmployee,
			LocalDate startDate, LocalDate endDate) {

		if (employeeToReplace == null || replacementEmployee == null) {
			throw new IllegalArgumentException("Veuillez sélectionner un employé.");
		}

        personnel.requireKnown(employeeToReplace);
        personnel.requireKnown(replacementEmployee);
        DateRange period = new DateRange(startDate, endDate);

        if (employeeToReplace == replacementEmployee) {
			return true;
		}

		ArrayList<Intervention> interventionsToAssign = getAssignedInterventionsForPeriod(period, employeeToReplace);

		if (interventionsToAssign.isEmpty()) {
			return true;
		}

		if (hasOverlap(replacementEmployee, interventionsToAssign)) {
			return false;
		}

		applyAssignments(interventionsToAssign, replacementEmployee);

		return true;
	}

	public ArrayList<Intervention> previewRecurringInterventions(RecurringIntervention recurring) {
        DomainValidation.recurring(recurring);
        clients.requireKnown(recurring.getClient());
        if (recurring.getEmployee() != null) personnel.requireKnown(recurring.getEmployee());
        ArrayList<Intervention> generated = new ArrayList<>();
        LocalDate date = recurring.getStartDate().with(java.time.temporal.TemporalAdjusters.nextOrSame(recurring.getDayOfWeek()));
        while (!date.isAfter(recurring.getEndDate())) {
            Intervention occurrence = new Intervention(recurring.getClient(), recurring.getEmployee(), date,
                    recurring.getStartTime(), date, recurring.getEndTime());
            occurrence.setRecurringId(recurring.getId());
            generated.add(occurrence);
            if (date.isAfter(LocalDate.MAX.minusWeeks(1))) break;
            date = date.plusWeeks(1);
        }
        if (generated.isEmpty()) throw new IllegalArgumentException("La période ne contient aucune occurrence du jour sélectionné.");
        return generated;
    }

    public boolean generateRecurringInterventions(RecurringIntervention recurring) {
        ArrayList<Intervention> generated = previewRecurringInterventions(recurring);
        if (!getInterventionsByRecurringId(recurring.getId()).isEmpty())
            throw new IllegalArgumentException("Cette série possède déjà des occurrences.");
        if (recurring.getEmployee() != null && hasOverlap(recurring.getEmployee(), generated)) return false;
        changeList(list -> list.addAll(generated));
        return true;
    }

	public void removeInterventionsByRecurringId(String recurringId) {
        DomainValidation.text(recurringId, true);
        changeList(list -> list.removeIf(i -> recurringId.equals(i.getRecurringId())));
    }

	public ArrayList<Intervention> getFutureInterventionsByRecurringId(String recurringId, LocalDateTime now) {
        if (now == null) throw new IllegalArgumentException("Date de référence manquante.");
        ArrayList<Intervention> targets = getInterventionsByRecurringId(recurringId);
        targets.removeIf(i -> DomainValidation.intervention(i).start().isBefore(now));
        return targets;
    }
    public void removeFutureInterventionsByRecurringId(String recurringId, LocalDateTime now) {
        List<Intervention> targets = getFutureInterventionsByRecurringId(recurringId, now);
        changeList(list -> list.removeAll(targets));
    }

    public ArrayList<Intervention> getInterventionsByAbsenceId(String id) {
        DomainValidation.text(id, true);
        ArrayList<Intervention> matches = new ArrayList<>();
        for (Intervention intervention : interventions)
            if (id.equals(intervention.getSourceAbsenceId())) matches.add(intervention);
        return matches;
    }

    public void releaseForAbsence(gestion.model.EmployeeAbsence absence) {
        DomainValidation.absence(absence);
        DateTimeRange period = new DateTimeRange(absence.getStartDate().atStartOfDay(), absence.getEndDate().plusDays(1).atStartOfDay());
        ArrayList<Intervention> targets = getReplacementTargets(absence.getEmployee(), period);
        LocalDateTime now = LocalDateTime.now();
        for (Intervention target : targets) {
            DateTimeRange range = DomainValidation.intervention(target);
            if (range.start().isBefore(now) && range.end().isAfter(now))
                throw new IllegalArgumentException("Une intervention est en cours : sa prise en charge doit être décidée avant de déclarer l'absence.");
        }
        targets.removeIf(i -> DomainValidation.intervention(i).start().isBefore(now));
        ArrayList<String> previousLinks = new ArrayList<>();
        targets.forEach(i -> previousLinks.add(i.getSourceAbsenceId()));
        if (targets.stream().anyMatch(i -> i.getSourceAbsenceId() != null))
            throw new IllegalArgumentException("Une intervention est déjà liée à une autre absence ; réorganisation manuelle nécessaire.");
        if (targets.isEmpty()) return;
        ServiceChanges.apply(getDirectory(), () -> targets.forEach(i -> { i.setEmployee(null); i.setSourceAbsenceId(absence.getId()); }),
                this::saveInterventions, () -> {
                    for (int i = 0; i < targets.size(); i++) {
                        targets.get(i).setEmployee(absence.getEmployee()); targets.get(i).setSourceAbsenceId(previousLinks.get(i));
                    }
                });
    }

    public ArrayList<Intervention> getReplacementTargets(Employee original, DateTimeRange period) {
        personnel.requireKnown(original);
        if (period == null) throw new IllegalArgumentException("Veuillez renseigner une période.");
        ArrayList<Intervention> targets = new ArrayList<>();
        for (Intervention intervention : interventions) {
            if (intervention.getEmployee() != original) continue;
            DateTimeRange range = DomainValidation.intervention(intervention);
            if (PeriodUtils.hasDateTimeOverlap(range, period)) {
                if (range.start().isBefore(period.start()) || range.end().isAfter(period.end()))
                    throw new IllegalArgumentException("Une intervention n'est couverte qu'en partie. Le découpage n'est pas encore disponible : aucun remplacement enregistré.");
                targets.add(intervention);
            }
        }
        return targets;
    }

    public void applyReplacement(Employee original, Employee substitute, DateTimeRange period) {
        List<Intervention> targets = getReplacementTargets(original, period);
        if (substitute != null) {
            personnel.requireKnown(substitute);
            if (substitute == original) throw new IllegalArgumentException("Le remplaçant doit être différent de l'employé absent.");
            if (hasOverlap(substitute, targets)) throw new IllegalArgumentException("Le remplaçant a déjà une intervention sur un des créneaux.");
        }
        if (!targets.isEmpty()) applyAssignments(targets, substitute);
    }

    public ArrayList<Intervention> getInterventionsByRecurringId(String recurringId) {

		ArrayList<Intervention> matches = new ArrayList<>();

		if (recurringId == null || recurringId.isBlank()) {
			return matches;
		}

		for (Intervention current : interventions) {
			if (recurringId.equals(current.getRecurringId())) {
				matches.add(current);
			}
		}

		return matches;
	}

	public void assignEmployee(List<Intervention> interventionsToAssign, Employee employee) {

		if (employee == null) {
			throw new IllegalArgumentException("Veuillez choisir un employé");
		}

		if (interventionsToAssign == null || interventionsToAssign.isEmpty()) {
			throw new IllegalArgumentException("Veuillez sélectionner au moins une intervention.");
		}

        personnel.requireKnown(employee);
        HashSet<Intervention> seen = new HashSet<>();

		for (Intervention current : interventionsToAssign) {

			if (current == null) {
				throw new IllegalArgumentException("Attribution impossible : une des intervention est null");
			}

            validateCandidate(current);
            if (!interventions.contains(current)) {
				throw new IllegalArgumentException("Attribution impossible : cette intervention n'existe pas");
			}

			if (!seen.add(current)) {
				throw new IllegalArgumentException("Intervention en double");
			}
		}

		if (hasOverlap(employee, interventionsToAssign)) {
			throw new IllegalArgumentException(
					"Attribution impossible : cet employé a déjà une intervention sur un des créneaux.");
		}

		applyAssignments(interventionsToAssign, employee);
	}

	public boolean hasOverlap(Employee employee, List<Intervention> interventionsToAssign) {
        personnel.requireKnown(employee);
        if (interventionsToAssign == null) throw new IllegalArgumentException("Liste d'interventions manquante.");
        for (Intervention current : interventionsToAssign) DomainValidation.intervention(current);

		ArrayList<Intervention> ordered = new ArrayList<>(interventionsToAssign);

		ordered.sort(Comparator.comparing(current -> LocalDateTime.of(current.getStartDate(), current.getStartTime())));

		LocalDateTime previousEnd = null;

		for (Intervention current : ordered) {
			if (hasOverlap(employee, current)) {
				return true;
			}

			LocalDateTime start = LocalDateTime.of(current.getStartDate(), current.getStartTime());
			LocalDateTime end = LocalDateTime.of(current.getEndDate(), current.getEndTime());

			if (previousEnd != null && start.isBefore(previousEnd)) {
				return true;
			}

			previousEnd = end;
		}

		return false;
	}

	public void assignEmployee(Intervention intervention, Employee employee) {

		if (intervention == null) {
			throw new IllegalArgumentException("Veuillez choisir une intervention.");
		}

		List<Intervention> interventionToAssign = List.of(intervention);

		assignEmployee(interventionToAssign, employee);
	}

	public void unassignEmployee(Intervention intervention) {

		if (intervention == null) {
			throw new IllegalArgumentException("Veuillez choisir une intervention.");
		}

		if (!interventions.contains(intervention)) {
			throw new IllegalArgumentException("Cette intervention n'existe pas.");
		}

		List<Intervention> interventionToAssign = List.of(intervention);
		applyAssignments(interventionToAssign, null);
	}

	public void removeIntervention(int index) {
        if (index < 0 || index >= interventions.size()) throw new IllegalArgumentException("Veuillez sélectionner une intervention.");
        removeIntervention(interventions.get(index));
    }

	public void removeIntervention(Intervention intervention) {
        if (intervention == null || !interventions.contains(intervention))
            throw new IllegalArgumentException("Cette intervention n'existe plus.");
        changeList(list -> list.remove(intervention));
    }

	public ArrayList<Intervention> getInterventions() {
		return interventions;
	}

	public boolean isEmpty() {
		return interventions.isEmpty();
	}

	private void saveInterventions() {
		repository.save(interventions);
	}
}
