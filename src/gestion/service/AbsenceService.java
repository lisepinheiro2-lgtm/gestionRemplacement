package gestion.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;

import gestion.model.Employee;
import gestion.model.EmployeeAbsence;
import gestion.model.EmployeeStatus;
import gestion.repository.AbsenceCsvRepository;

public class AbsenceService {

	private final ArrayList<EmployeeAbsence> absences = new ArrayList<>();
	private final AbsenceCsvRepository repository;
	private final ReplacementService replacementList;

	public AbsenceService(PersonnelService personnel, ReplacementService replacementList) throws IOException {
		this(personnel, replacementList, new AbsenceCsvRepository(Path.of(".")));
	}

	public AbsenceService(PersonnelService personnel, ReplacementService replacementList,
			AbsenceCsvRepository repository) throws IOException {
		this.repository = repository;

		this.replacementList = replacementList;

		absences.addAll(repository.load(personnel.getEmployees()));
		replacementList.synchronizeAbsences(absences);
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

		for (int i = 0; i < absences.size(); i++) {
			EmployeeAbsence absence = absences.get(i);

			if (absence.getEmployee() != employee || date.isBefore(absence.getStartDate())
					|| date.isAfter(absence.getEndDate())) {
				continue;
			}

			LocalDate previousEnd = absence.getEndDate();
			boolean remove = absence.getStartDate().equals(date);

			if (remove) {
				absences.remove(i);
			} else {
				absence.setEndDate(date.minusDays(1));
			}

			try {
				saveAbsences();
			} catch (IOException exception) {
				if (remove) {
					absences.add(i, absence);
				} else {
					absence.setEndDate(previousEnd);
				}
				throw exception;
			}

			replacementList.synchronizeAbsences(absences);
			return;
		}
	}

	public void addAbsence(EmployeeAbsence absence) throws IOException {

		for (EmployeeAbsence existing : absences) {
			if (existing.getEmployee() == absence.getEmployee()
					&& !absence.getEndDate().isBefore(existing.getStartDate())
					&& !absence.getStartDate().isAfter(existing.getEndDate())) {

				throw new IllegalArgumentException("Une absence existe déjà pour cet employé sur cette période.");
			}
		}

		absences.add(absence);

		try {
			saveAbsences();
		} catch (IOException exception) {
			absences.remove(absence);
			throw exception;
		}
		replacementList.synchronizeAbsences(absences);
	}

	public ArrayList<EmployeeAbsence> getAbsences() {
		return absences;
	}

	public void saveAbsences() throws IOException {
		repository.save(absences);
	}
}
