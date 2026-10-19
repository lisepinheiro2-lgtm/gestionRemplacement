package gestion.repository;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import gestion.model.Employee;
import gestion.model.EmployeeAbsence;
import gestion.model.EmployeeStatus;


public final class AbsenceCsvRepository {
    private final Path path;

    public AbsenceCsvRepository(Path directory) {
        path = directory.resolve("employee_absences.csv");
    }

    public ArrayList<EmployeeAbsence> load(List<Employee> employees) throws IOException {
        ArrayList<EmployeeAbsence> absences = new ArrayList<>();

        Path path = this.path;

        if (Files.notExists(path)) {
            return absences;
        }

        ArrayList<EmployeeAbsence> loadedAbsences = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {

            String line;

            while ((line = reader.readLine()) != null) {

                if (line.isBlank()) {
                    continue;
                }

                String[] data = line.split(";", -1);

                if (data.length != 6 || data[0].isBlank()) {
                    throw new IOException("Ligne d'absence invalide : " + line);
                }

                Employee employee = CsvReferences.findEmployee(employees, data[2], data[1]);

                if (employee == null) {
                    throw new IOException("Employé introuvable : " + data[2] + " " + data[1]);
                }

                try {
                    EmployeeStatus status = EmployeeStatus.valueOf(data[3]);
                    LocalDate startDate = LocalDate.parse(data[4]);
                    LocalDate endDate = LocalDate.parse(data[5]);

                    if (endDate.isBefore(startDate)) {
                        throw new IOException("Dates d'absence inversées.");
                    }

                    EmployeeAbsence absence = new EmployeeAbsence(employee, status, startDate, endDate);

                    absence.setId(data[0]);
                    loadedAbsences.add(absence);

                } catch (IllegalArgumentException | DateTimeParseException exception) {
                    throw new IOException("Absence invalide : " + line, exception);
                }
            }
        }

        absences.clear();
        absences.addAll(loadedAbsences);
        return absences;
    }

    public void save(List<EmployeeAbsence> absences) throws IOException {

        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {

            for (EmployeeAbsence absence : absences) {
                Employee employee = absence.getEmployee();

                writer.write(String.join(";", absence.getId(), employee.getLastName(), employee.getFirstName(),
                        absence.getStatus().name(), absence.getStartDate().toString(),
                        absence.getEndDate().toString()));

                writer.newLine();
            }
        }
    }
}
