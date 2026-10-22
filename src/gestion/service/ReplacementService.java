package gestion.service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

import gestion.model.Employee;
import gestion.model.EmployeeAbsence;
import gestion.model.Replacement;
import gestion.repository.ReplacementCsvRepository;
import gestion.util.FormatUtils;
import gestion.util.DomainValidation;
import gestion.repository.CsvStore;
import java.io.IOException;
import java.io.UncheckedIOException;

public class ReplacementService {

	private ArrayList<Replacement> replacements = new ArrayList<>();
	private final ReplacementCsvRepository repository;
	private PersonnelService personnel;

	public Path getDirectory() {
		return repository.getDirectory();
	}

	public void bindPersonnel(PersonnelService personnel) {

		if (this.personnel == personnel)
			return;
		if (this.personnel != null)
			throw new IllegalArgumentException("Personnel déjà associé.");

		for (Replacement replacement : replacements) {
			Employee original = personnel.findEmployee(replacement.getEmployeeToReplace());
			Employee substitute = personnel.findEmployee(replacement.getReplacementEmployee());

			if (original == null || (replacement.getReplacementEmployee() != null && substitute == null))
				throw new UncheckedIOException(
						new IOException("replacements.csv : référence employé introuvable ; chargement refusé."));

			replacement.setEmployeeToReplace(original);
			replacement.setReplacementEmployee(substitute);
		}

		this.personnel = personnel;
		personnel.protectReferences(employee -> replacements.stream()
				.anyMatch(r -> r.getEmployeeToReplace() == employee || r.getReplacementEmployee() == employee));
	}

	private void validate(Replacement replacement) {
		DomainValidation.replacement(replacement);

		if (personnel != null) {
			personnel.requireKnown(replacement.getEmployeeToReplace());

			if (replacement.getReplacementEmployee() != null)
				personnel.requireKnown(replacement.getReplacementEmployee());
		}
	}

	private static Replacement copy(Replacement source) {
		Replacement result = new Replacement(source.getEmployeeToReplace(), source.getContractHoursEmployeeToReplace(),
				source.getReplacementEmployee(), source.getContractHoursReplacementEmployee(), source.getStartDate(),
				source.getStartTime(), source.getEndDate(), source.getEndTime());
		result.setAbsenceId(source.getAbsenceId());
		return result;
	}

	private static void copyValues(Replacement source, Replacement target) {
		target.setEmployeeToReplace(source.getEmployeeToReplace());
		target.setReplacementEmployee(source.getReplacementEmployee());
		target.setContractHoursEmployeeToReplace(source.getContractHoursEmployeeToReplace());
		target.setContractHoursReplacementEmployee(source.getContractHoursReplacementEmployee());
		target.setStartDate(source.getStartDate());
		target.setStartTime(source.getStartTime());
		target.setEndDate(source.getEndDate());
		target.setEndTime(source.getEndTime());
		target.setAbsenceId(source.getAbsenceId());
	}

	private Runnable snapshot() {
		ArrayList<Replacement> previous = new ArrayList<>(replacements);
		ArrayList<Replacement> values = new ArrayList<>();
		previous.forEach(r -> values.add(copy(r)));

		return () -> {
			replacements.clear();
			replacements.addAll(previous);

			for (int i = 0; i < previous.size(); i++)
				copyValues(values.get(i), previous.get(i));
		};
	}

	private void change(Runnable mutation) {
		ServiceChanges.apply(getDirectory(), mutation, this::saveReplacements, snapshot());
	}

	public void assignUnassignedGroup(PlanningService planning, int index, Employee employee) {

		if (!getDirectory().equals(planning.getDirectory()))
			throw new IllegalArgumentException("Dossiers de données différents.");

		if (index < 0 || index >= replacements.size() || replacements.get(index).getAbsenceId() == null)
			throw new IllegalArgumentException("Sélectionnez une demande liée à une absence.");

		var targets = planning.getInterventionsByAbsenceId(replacements.get(index).getAbsenceId());
		var now = java.time.LocalDateTime.now();
		targets.removeIf(i -> i.getEmployee() != null || DomainValidation.intervention(i).start().isBefore(now));

		if (targets.isEmpty())
			throw new IllegalArgumentException(
					"Aucune intervention future non attribuée dans cette demande. Les anciennes demandes ne sont pas converties automatiquement.");
		planning.assignEmployee(targets, employee);
	}

	public void createWithPlanning(PlanningService planning, Employee original, Employee substitute, String startDate,
			String startTime, String endDate, String endTime) {

		if (!getDirectory().equals(planning.getDirectory()))
			throw new IllegalArgumentException("Dossiers de données différents.");

		if (personnel != null) {
			personnel.requireKnown(original);

			if (substitute != null)
				personnel.requireKnown(substitute);
		}

		DomainValidation.employee(original);
		Replacement candidate = new Replacement(original, original.getContractHours(), substitute,
				substitute == null ? 0 : substitute.getContractHours(), startDate, startTime, endDate, endTime);
		var period = DomainValidation.replacement(candidate);
		CsvStore.transaction(getDirectory(), () -> {
			planning.applyReplacement(original, substitute, period);
			addReplacement(original, original.getContractHours(), substitute,
					substitute == null ? 0 : substitute.getContractHours(), startDate, startTime, endDate, endTime);
		});
	}

	public ReplacementService() {
		this(new ReplacementCsvRepository(Path.of(".")));
	}

	public ReplacementService(ReplacementCsvRepository repository) {
		this.repository = repository;

		replacements = repository.load();
		sortReplacements();
	}

	public void addReplacement(Employee employeeToReplace, double contractHoursEmployeeToReplace,
			Employee replacementEmployee, double contractHoursReplacementEmployee, String startDate, String startTime,
			String endDate, String endTime) {

		Replacement candidate = new Replacement(employeeToReplace, contractHoursEmployeeToReplace, replacementEmployee,
				contractHoursReplacementEmployee, startDate, startTime, endDate, endTime);
		validate(candidate);

		change(() -> {
			replacements.add(candidate);
			sortReplacements();
		});
	}

	public void editReplacements(int index, Employee employeeToReplace, double contractHoursEmployeeToReplace,
			Employee replacementEmployee, double contractHoursReplacementEmployee, String startDate, String startTime,
			String endDate, String endTime) {

		if (index < 0 || index >= replacements.size())
			throw new IllegalArgumentException("Sélectionnez un remplacement.");

		Replacement candidate = new Replacement(employeeToReplace, contractHoursEmployeeToReplace, replacementEmployee,
				contractHoursReplacementEmployee, startDate, startTime, endDate, endTime);
		candidate.setAbsenceId(replacements.get(index).getAbsenceId());
		validate(candidate);
		Replacement original = replacements.get(index);

		change(() -> {
			copyValues(candidate, original);
			sortReplacements();
		});
	}

	public void synchronizeAbsences(ArrayList<EmployeeAbsence> absences, PlanningService planning) {

		if (absences == null || planning == null) {
			throw new IllegalArgumentException(
					"Impossible de synchroniser les absences : certaines données sont indisponibles. "
							+ "Veuillez redémarrer l'application. Si le problème persiste, signalez-le.");
		}

		if (!getDirectory().equals(planning.getDirectory())) {
			throw new IllegalStateException("Incohérence interne : dossiers de données différents.");
		}

		absences.forEach(DomainValidation::absence);
		HashSet<String> requiredIds = new HashSet<>();

		for (var intervention : planning.getInterventions()) {
			if (intervention.getSourceAbsenceId() != null) {
				requiredIds.add(intervention.getSourceAbsenceId());
			}
		}

		HashSet<String> knownIds = new HashSet<>();

		for (EmployeeAbsence absence : absences) {
			knownIds.add(absence.getId());
		}

		if (!knownIds.containsAll(requiredIds)) {
			throw new IllegalArgumentException("Au moins une intervention est liée à une absence introuvable.");
		}

		change(() -> {

			replacements.removeIf(replacement -> replacement.getAbsenceId() != null
					&& !requiredIds.contains(replacement.getAbsenceId()));

			for (EmployeeAbsence absence : absences) {

				if (!requiredIds.contains(absence.getId())) {
					continue;
				}

				Replacement linkedReplacement = null;

				for (Replacement replacement : replacements) {
					if (absence.getId().equals(replacement.getAbsenceId())) {
						linkedReplacement = replacement;
						break;
					}
				}

				String startDate = FormatUtils.formatDate(absence.getStartDate());
				String endDate = FormatUtils.formatDate(absence.getEndDate().plusDays(1));

				if (linkedReplacement == null) {

					Employee employee = absence.getEmployee();

					linkedReplacement = new Replacement(employee, employee.getContractHours(), null, 0, startDate,
							"00h00", endDate, "00h00");

					linkedReplacement.setAbsenceId(absence.getId());
					replacements.add(linkedReplacement);

				} else {

					linkedReplacement.setStartDate(startDate);
					linkedReplacement.setStartTime("00h00");
					linkedReplacement.setEndDate(endDate);
					linkedReplacement.setEndTime("00h00");
				}
			}

			sortReplacements();
		});
	}

	public void sortReplacements() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		replacements.sort(Comparator.comparing(replacement -> LocalDate.parse(replacement.getStartDate(), formatter)));
	}

	public void removeReplacement(int index) {

		if (index < 0 || index >= replacements.size())
			throw new IllegalArgumentException("Sélectionnez un remplacement.");

		if (replacements.get(index).getAbsenceId() != null)
			throw new IllegalArgumentException(
					"Cette demande est liée à une absence ; sa suppression directe est désactivée pour préserver le lien.");

		change(() -> replacements.remove(index));
	}

	public ArrayList<Replacement> getReplacements() {
		return replacements;
	}

	public boolean isEmpty() {
		return replacements.isEmpty();
	}

	private void saveReplacements() {
		repository.save(replacements);
	}
}
