package gestionRemplacement.service;

import gestionRemplacement.model.Employee;
import gestionRemplacement.model.EmployeeAbsence;
import gestionRemplacement.model.Replacement;
import gestionRemplacement.repository.ReplacementCsvRepository;
import gestionRemplacement.util.FormatUtils;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

public class ReplacementService {

	private ArrayList<Replacement> replacements = new ArrayList<>();
	private final ReplacementCsvRepository repository;

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

		replacements.add(new Replacement(employeeToReplace, contractHoursEmployeeToReplace, replacementEmployee,
				contractHoursReplacementEmployee, startDate, startTime, endDate, endTime));

		sortReplacements();
		saveReplacements();
	}

	public void editReplacements(int index, Employee employeeToReplace, double contractHoursEmployeeToReplace,
			Employee replacementEmployee, double contractHoursReplacementEmployee, String startDate, String startTime,
			String endDate, String endTime) {

		Replacement replacement = replacements.get(index);

		replacement.setEmployeeToReplace(employeeToReplace);
		replacement.setContractHoursEmployeeToReplace(contractHoursEmployeeToReplace);
		replacement.setReplacementEmployee(replacementEmployee);
		replacement.setContractHoursReplacementEmployee(contractHoursReplacementEmployee);
		replacement.setStartDate(startDate);
		replacement.setStartTime(startTime);
		replacement.setEndDate(endDate);
		replacement.setEndTime(endTime);

		sortReplacements();
		saveReplacements();
	}

	public void synchronizeAbsences(ArrayList<EmployeeAbsence> absences) {

		HashSet<String> absenceIds = new HashSet<>();

		for (EmployeeAbsence absence : absences) {
			absenceIds.add(absence.getId());
		}

		replacements.removeIf(
				replacement -> replacement.getAbsenceId() != null && !absenceIds.contains(replacement.getAbsenceId()));

		for (EmployeeAbsence absence : absences) {
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

				linkedReplacement = new Replacement(employee, employee.getContractHours(), null, 0, startDate, "00h00",
						endDate, "00h00");

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
		saveReplacements();
	}

	public void sortReplacements() {

		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

		replacements.sort(Comparator.comparing(replacement -> LocalDate.parse(replacement.getStartDate(), formatter)));
	}

	public void removeReplacement(int index) {
		replacements.remove(index);

		saveReplacements();
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
