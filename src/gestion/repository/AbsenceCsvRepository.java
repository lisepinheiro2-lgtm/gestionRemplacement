package gestion.repository;

import java.nio.file.Path;
import java.time.*;
import java.util.*;
import gestion.model.*;
import gestion.util.DomainValidation;

import java.io.IOException;

public final class AbsenceCsvRepository {
    private final Path path;
    public AbsenceCsvRepository(Path directory) { path = CsvStore.directory(directory).resolve("employee_absences.csv"); }
    public Path getDirectory() { return path.getParent(); }

    public ArrayList<EmployeeAbsence> load(List<Employee> employees) throws IOException {
        List<EmployeeAbsence> checked = new ArrayList<>();
        return CsvFormat.read(path, cells -> {
            CsvFormat.columns(cells, 6);
            Employee employee = CsvReferences.optionalEmployee(employees, cells[2], cells[1]);
            EmployeeAbsence absence = new EmployeeAbsence(employee, EmployeeStatus.valueOf(cells[3]),
                    LocalDate.parse(cells[4]), LocalDate.parse(cells[5]));
            absence.setId(cells[0]);
            validate(absence, checked);
            checked.add(absence);
            return absence;
        });
    }

    private static void validate(EmployeeAbsence absence, List<EmployeeAbsence> checked) {
        DomainValidation.absence(absence);
        for (EmployeeAbsence other : checked) {
            if (absence.getId().equals(other.getId())) throw new IllegalArgumentException("Identifiant d'absence dupliqué.");
            if (absence.getEmployee() == other.getEmployee()
                    && gestion.util.PeriodUtils.hasDateOverlap(new DateRange(absence.getStartDate(), absence.getEndDate()),
                            new DateRange(other.getStartDate(), other.getEndDate())))
                throw new IllegalArgumentException("Absences superposées.");
        }
    }

    public void save(List<EmployeeAbsence> absences) throws IOException {
        StringBuilder content = new StringBuilder();
        List<EmployeeAbsence> checked = new ArrayList<>();
        for (EmployeeAbsence absence : absences) {
            validate(absence, checked);
            checked.add(absence);
            content.append(CsvFormat.row(absence.getId(), absence.getEmployee().getLastName(), absence.getEmployee().getFirstName(),
                    absence.getStatus().name(), absence.getStartDate(), absence.getEndDate()));
        }
        CsvStore.writeChecked(path, content.toString());
    }
}
