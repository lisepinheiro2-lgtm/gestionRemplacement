package gestion.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import gestion.model.Employee;
import gestion.model.EmployeeAbsence;
import gestion.model.EmployeeStatus;
import gestion.repository.AbsenceCsvRepository;
import gestion.repository.CsvStore;
import gestion.util.DomainValidation;

public class AbsenceService {

	private final ArrayList<EmployeeAbsence> absences = new ArrayList<>();
	private final AbsenceCsvRepository repository;
	private final ReplacementService replacementList;
	private final PersonnelService personnel;
	private final PlanningService planning;

	public Path getDirectory() {
		return repository.getDirectory();
	}

	private void validateLinks() throws IOException {

		for (EmployeeAbsence absence : absences) {

			var requests = replacementList.getReplacements().stream()
					.filter(r -> absence.getId().equals(r.getAbsenceId())).toList();
			boolean hasLinkedInterventions = !planning.getInterventionsByAbsenceId(absence.getId()).isEmpty();

			if (requests.size() > 1 || (hasLinkedInterventions && requests.isEmpty())) {
				throw new IOException("Liens d'absence et de remplacement incohérents.");
			}

			if (requests.isEmpty()) {
				continue;
			}

			var request = requests.getFirst();

			if (!request.getEmployeeToReplace().getEmployeeID().equals(absence.getEmployee().getEmployeeID())) {
				throw new IOException("La demande ne correspond pas au salarié absent.");
			}

			var expected = DomainValidation.absencePeriod(absence);

			if (!expected.equals(DomainValidation.replacement(request))) {
				throw new IOException("Période de remplacement incohérente.");
			}
		}

		for (gestion.model.Intervention intervention : planning.getInterventions()) {

			String id = intervention.getSourceAbsenceId();

			if (id == null) {
				continue;
			}

			EmployeeAbsence source = absences.stream().filter(a -> id.equals(a.getId())).findFirst()
					.orElseThrow(() -> new IOException("Absence source introuvable."));
			boolean requestExists = replacementList.getReplacements().stream()
					.anyMatch(r -> id.equals(r.getAbsenceId()));

			if (!requestExists) {
				throw new IOException("Demande de remplacement liée introuvable.");
			}

			var range = DomainValidation.intervention(intervention);

			var sourcePeriod = DomainValidation.absencePeriod(source);

			if (range.start().isBefore(sourcePeriod.start()) || range.end().isAfter(sourcePeriod.end())) {
				throw new IOException("Intervention hors de sa période d'absence.");
			}
		}

		for (gestion.model.Replacement request : replacementList.getReplacements()) {

			if (request.getAbsenceId() != null
					&& absences.stream().noneMatch(a -> request.getAbsenceId().equals(a.getId()))) {
				throw new IOException("Demande liée à une absence inexistante.");
			}
		}
	}

	private void change(Runnable mutation) throws IOException {
		change(mutation, () -> {
		});
	}

	private void change(Runnable mutation, Runnable afterMutation) throws IOException {

		ArrayList<EmployeeAbsence> previous = new ArrayList<>(absences);
		ArrayList<LocalDate> ends = new ArrayList<>();
		ArrayList<LocalTime> endTimes = new ArrayList<>();

		previous.forEach(absence -> ends.add(absence.getEndDate()));
		previous.forEach(absence -> endTimes.add(absence.getEndTime()));

		Runnable restore = () -> {
			absences.clear();
			absences.addAll(previous);

			for (int i = 0; i < previous.size(); i++) {
				previous.get(i).setEndDate(ends.get(i));
				previous.get(i).setEndTime(endTimes.get(i));
			}
		};

		try {

			CsvStore.transaction(getDirectory(), () -> {
				ServiceChanges.apply(getDirectory(), mutation, () -> {

					try {

						saveAbsences();

					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}

				}, restore);

				afterMutation.run();
				replacementList.synchronizeAbsences(absences, planning);
			});

		} catch (UncheckedIOException e) {
			throw e.getCause();
		}
	}

	public AbsenceService(PersonnelService personnel, ReplacementService replacementList, PlanningService planning)
			throws IOException {
		this(personnel, replacementList, planning, new AbsenceCsvRepository(planning.getDirectory()));
	}

	public AbsenceService(PersonnelService personnel, ReplacementService replacementList, PlanningService planning,
			AbsenceCsvRepository repository) throws IOException {
		this.repository = repository;
		this.personnel = personnel;
		this.planning = planning;

		if (!getDirectory().equals(planning.getDirectory()))
			throw new IllegalArgumentException("Dossiers de données différents.");
		if (!getDirectory().equals(replacementList.getDirectory()))
			throw new IllegalArgumentException("Dossiers de données différents.");

		this.replacementList = replacementList;

		absences.addAll(repository.load(personnel.getEmployees()));
		replacementList.bindPersonnel(personnel);
		personnel.protectReferences(employee -> absences.stream().anyMatch(a -> a.getEmployee() == employee));
		planning.setAbsenceCheck((employee, period) -> absences.stream()
				.anyMatch(a -> a.getEmployee() == employee
						&& gestion.util.PeriodUtils.hasDateTimeOverlap(period, DomainValidation.absencePeriod(a))));
		validateLinks();
	}

	public EmployeeStatus getStatusAt(Employee employee, LocalDate date) {

		for (EmployeeAbsence absence : absences) {
			if (absence.getEmployee() == employee && !date.isBefore(absence.getStartDate())
					&& !date.isAfter(absence.getEndDate())) {

				return absence.getStatus();
			}
		}

		return EmployeeStatus.OPERATIONAL;
	}

	public EmployeeStatus getStatusAt(Employee employee, LocalDateTime dateTime) {
		for (EmployeeAbsence absence : absences) {
			if (absence.getEmployee() != employee)
				continue;

			var period = DomainValidation.absencePeriod(absence);

			if (!dateTime.isBefore(period.start()) && dateTime.isBefore(period.end()))
				return absence.getStatus();
		}

		return EmployeeStatus.OPERATIONAL;
	}

	public void resumeEmployee(Employee employee, LocalDate date) throws IOException {

		personnel.requireKnown(employee);

		if (date == null)
			throw new IllegalArgumentException("Date de reprise manquante.");

		for (EmployeeAbsence absence : absences) {

			if (absence.getEmployee() == employee && !date.isBefore(absence.getStartDate())
					&& !date.isAfter(absence.getEndDate())) {
				ensureResumeAllowed(absence);

				change(() -> {
					if (date.equals(absence.getStartDate()))
						absences.remove(absence);
					else
						absence.setEndDate(date.minusDays(1));
				});
				return;
			}
		}
	}

	public void resumeEmployeeAt(Employee employee, LocalDateTime dateTime) throws IOException {
		personnel.requireKnown(employee);

		if (dateTime == null)
			throw new IllegalArgumentException("Date et heure de reprise manquantes.");

		for (EmployeeAbsence absence : absences) {
			var period = DomainValidation.absencePeriod(absence);

			if (absence.getEmployee() != employee || dateTime.isBefore(period.start()) || !dateTime.isBefore(period.end()))
				continue;

			ensureResumeAllowed(absence);
			change(() -> {
				if (dateTime.equals(period.start())) {
					absences.remove(absence);
				} else {
					absence.setEndDate(dateTime.toLocalDate());
					absence.setEndTime(dateTime.toLocalTime());
				}
			});
			return;
		}
	}

	private void ensureResumeAllowed(EmployeeAbsence absence) {
		if (!planning.getInterventionsByAbsenceId(absence.getId()).isEmpty())
			throw new IllegalArgumentException(
					"Reprise temporairement bloquée : des interventions sont liées à cette absence. Le rétablissement des affectations doit être décidé sans annuler les remplacements existants.");
	}

	public void addAbsence(EmployeeAbsence absence) throws IOException {
		validateNewAbsence(absence);
		change(() -> absences.add(absence), () -> planning.releaseForAbsence(absence));
	}

	public List<PlanningService.AbsenceRelease> previewTimedAbsence(EmployeeAbsence absence) {
		validateNewAbsence(absence);
		return planning.previewAbsenceReleases(absence, true);
	}

	public void addTimedAbsence(EmployeeAbsence absence, List<PlanningService.AbsenceRelease> approved) throws IOException {
		List<PlanningService.AbsenceRelease> current = previewTimedAbsence(absence);

		if (approved == null || !current.equals(approved))
			throw new IllegalArgumentException("Le planning a changé : vérifiez à nouveau les créneaux à libérer.");

		List<PlanningService.AbsenceRelease> confirmed = List.copyOf(approved);
		change(() -> absences.add(absence), () -> planning.releaseForConfirmedAbsence(absence, confirmed));
	}

	private void validateNewAbsence(EmployeeAbsence absence) {

		DomainValidation.absence(absence);
		personnel.requireKnown(absence.getEmployee());

		for (EmployeeAbsence existing : absences) {

			if (existing.getId().equals(absence.getId())) {
				throw new IllegalArgumentException("Une absence identique existe déjà.");
			}

			if (existing.getEmployee() == absence.getEmployee() && gestion.util.PeriodUtils.hasDateTimeOverlap(
					DomainValidation.absencePeriod(existing), DomainValidation.absencePeriod(absence))) {
				throw new IllegalArgumentException("Une absence existe déjà pour cet employé sur cette période.");
			}
		}
	}

	public ArrayList<EmployeeAbsence> getAbsences() {
		return absences;
	}

	public void saveAbsences() throws IOException {
		repository.save(absences);
	}
}
