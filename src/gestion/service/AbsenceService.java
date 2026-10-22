package gestion.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;

import gestion.model.Employee;
import gestion.model.EmployeeAbsence;
import gestion.model.EmployeeStatus;
import gestion.repository.AbsenceCsvRepository;
import gestion.repository.CsvStore;
import gestion.util.DomainValidation;
import java.io.UncheckedIOException;

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

			if (request.getEmployeeToReplace() != absence.getEmployee()) {
				throw new IOException("La demande ne correspond pas au salarié absent.");
			}

			var expected = new gestion.model.DateTimeRange(absence.getStartDate().atStartOfDay(),
					absence.getEndDate().plusDays(1).atStartOfDay());

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

			if (range.start().isBefore(source.getStartDate().atStartOfDay())
					|| range.end().isAfter(source.getEndDate().plusDays(1).atStartOfDay())) {
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

		previous.forEach(absence -> ends.add(absence.getEndDate()));

		Runnable restore = () -> {
			absences.clear();
			absences.addAll(previous);

			for (int i = 0; i < previous.size(); i++) {
				previous.get(i).setEndDate(ends.get(i));
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
						&& gestion.util.PeriodUtils.hasDateTimeOverlap(period, new gestion.model.DateTimeRange(
								a.getStartDate().atStartOfDay(), a.getEndDate().plusDays(1).atStartOfDay()))));
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

	public void resumeEmployee(Employee employee, LocalDate date) throws IOException {

		personnel.requireKnown(employee);

		if (date == null)
			throw new IllegalArgumentException("Date de reprise manquante.");

		for (EmployeeAbsence absence : absences) {

			if (absence.getEmployee() == employee && !date.isBefore(absence.getStartDate())
					&& !date.isAfter(absence.getEndDate())) {
				if (!planning.getInterventionsByAbsenceId(absence.getId()).isEmpty())
					throw new IllegalArgumentException(
							"Reprise temporairement bloquée : des interventions sont liées à cette absence. Le rétablissement des affectations doit être décidé sans annuler les remplacements existants.");

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

	public void addAbsence(EmployeeAbsence absence) throws IOException {

		DomainValidation.absence(absence);
		personnel.requireKnown(absence.getEmployee());

		for (EmployeeAbsence existing : absences) {

			if (existing.getId().equals(absence.getId())) {
				throw new IllegalArgumentException("Une absence identique existe déjà.");
			}

			if (existing.getEmployee() == absence.getEmployee()
					&& !absence.getEndDate().isBefore(existing.getStartDate())
					&& !absence.getStartDate().isAfter(existing.getEndDate())) {
				throw new IllegalArgumentException("Une absence existe déjà pour cet employé sur cette période.");
			}
		}

		change(() -> absences.add(absence), () -> planning.releaseForAbsence(absence));
	}

	public ArrayList<EmployeeAbsence> getAbsences() {
		return absences;
	}

	public void saveAbsences() throws IOException {
		repository.save(absences);
	}
}
