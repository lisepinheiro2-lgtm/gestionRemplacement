package gestion.repository;

import java.nio.file.Path;
import java.time.*;
import java.util.*;
import gestion.model.*;
import gestion.util.DomainValidation;

public final class InterventionCsvRepository {
    private final Path path;
    public InterventionCsvRepository(Path directory) { path = CsvStore.directory(directory).resolve("interventions.csv"); }
    public Path getDirectory() { return path.getParent(); }

    public ArrayList<Intervention> load(List<Employee> employees, List<Client> clients) {
        ArrayList<Intervention> checked = new ArrayList<>();
        return CsvFormat.readUnchecked(path, cells -> {
            CsvFormat.columns(cells, 8, 9, 10);
            Intervention intervention = new Intervention(CsvReferences.requiredClient(clients, cells[0], cells[1]),
                    CsvReferences.optionalEmployee(employees, cells[2], cells[3]), LocalDate.parse(cells[4]),
                    LocalTime.parse(cells[5]), LocalDate.parse(cells[6]), LocalTime.parse(cells[7]));
            if (cells.length >= 9 && !cells[8].isBlank()) intervention.setRecurringId(cells[8]);
            if (cells.length == 10 && !cells[9].isBlank()) intervention.setSourceAbsenceId(cells[9]);
            validateOverlap(intervention, checked);
            checked.add(intervention);
            return intervention;
        });
    }

    private static void validateOverlap(Intervention candidate, List<Intervention> checked) {
        DateTimeRange range = DomainValidation.intervention(candidate);
        for (Intervention other : checked)
            if (candidate.getEmployee() != null && candidate.getEmployee() == other.getEmployee()
                    && gestion.util.PeriodUtils.hasDateTimeOverlap(range, DomainValidation.intervention(other)))
                throw new IllegalArgumentException("Un salarié ne peut effectuer deux interventions simultanées.");
    }

    public void save(List<Intervention> interventions) {
        StringBuilder content = new StringBuilder();
        ArrayList<Intervention> checked = new ArrayList<>();
        for (Intervention intervention : interventions) {
            validateOverlap(intervention, checked);
            checked.add(intervention);
            Employee employee = intervention.getEmployee();
            content.append(CsvFormat.row(intervention.getClient().getFirstName(), intervention.getClient().getLastName(),
                    employee == null ? "" : employee.getFirstName(), employee == null ? "" : employee.getLastName(),
                    intervention.getStartDate(), intervention.getStartTime(), intervention.getEndDate(),
                    intervention.getEndTime(), intervention.getRecurringId(), intervention.getSourceAbsenceId()));
        }
        CsvStore.write(path, content.toString());
    }
}
