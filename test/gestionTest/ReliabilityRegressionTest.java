package gestionTest;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import gestion.model.*;
import gestion.repository.*;
import gestion.service.*;
import gestion.util.FormatUtils;
import gestionTest.support.TestFixture;

class ReliabilityRegressionTest {
    @TempDir Path directory;
    TestFixture f;
    @BeforeEach void initialize() throws IOException { f = new TestFixture(directory); }

    @Test void invalidEditsNeverMutateOrWriteEvenWithoutAnEmployee() throws IOException {
        Intervention i = f.add(null, f.future, 8, 10);
        var before = f.files();
        int index = f.planning.getInterventions().indexOf(i);
        assertThrows(IllegalArgumentException.class, () -> f.planning.editIntervention(index, f.client, null, f.future, LocalTime.NOON, f.future, LocalTime.of(11, 0)));
        assertThrows(IllegalArgumentException.class, () -> f.planning.editIntervention(index, null, null, f.future, LocalTime.of(8, 0), f.future, LocalTime.of(10, 0)));
        assertThrows(IllegalArgumentException.class, () -> f.planning.editIntervention(index, f.client, null, null, LocalTime.of(8, 0), f.future, LocalTime.of(10, 0)));
        assertEquals(LocalTime.of(8, 0), i.getStartTime()); assertNull(i.getEmployee());
        assertEquals(before, f.files()); f.reload();
    }

    @Test void publicOverlapPreconditionsDoNotDependOnPlanningContents() {
        Intervention invalid = new Intervention(f.client, null, null, null, null, null);
        assertThrows(IllegalArgumentException.class, () -> f.planning.hasOverlap(f.a, invalid));
        assertThrows(IllegalArgumentException.class, () -> f.planning.hasOverlap((Employee) null, invalid));
        assertThrows(IllegalArgumentException.class, () -> f.planning.hasOverlap(f.a, (List<Intervention>) null));
        assertThrows(IllegalArgumentException.class, () -> f.planning.hasOverlap(f.a, Arrays.asList((Intervention) null)));
        assertThrows(IllegalArgumentException.class, () -> f.planning.assignEmployee(List.of(invalid), f.a));
        Employee detached = new Employee("Ana", "Alpha", 35, "Opérationnel", 0, 0);
        assertThrows(IllegalArgumentException.class, () -> f.planning.addIntervention(new Intervention(f.client, detached, f.future, LocalTime.of(8, 0), f.future, LocalTime.of(10, 0))));
    }

    @Test void textSeparatorsContractDomainsAndClientDatesAreRejectedBeforeMutation() throws IOException {
        var before = f.files();
        for (String text : List.of("A;B", "A\nB", "A\rB")) {
            assertThrows(IllegalArgumentException.class, () -> f.clients.addClient("Nouveau", "Client", text, 0, null, null));
            assertThrows(IllegalArgumentException.class, () -> f.personnel.addEmployee(text, "Nom", 0, "Opérationnel", 0, 0));
        }
        for (double hours : new double[] {-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> f.clients.addClient("Nouveau", "Client", "Adresse", hours, null, null));
            assertThrows(IllegalArgumentException.class, () -> f.personnel.editEmployee(0, "Ana", "Alpha", hours, "Opérationnel", 0, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> f.clients.editClients(0, "Client", "Fictif", "Adresse", 0, f.future.plusDays(1), f.future));
        assertEquals(before, f.files());
        for (String text : List.of("-5", "1e309", "35h30h45", "12h60", "NaN", "35h-1"))
            assertThrows(NumberFormatException.class, () -> FormatUtils.parseContractHours(text));
        assertEquals(0, FormatUtils.parseContractHours("0")); assertEquals(35.5, FormatUtils.parseContractHours("35h30"));
        assertEquals(35.5, FormatUtils.parseContractHours("35,5"));
    }

    @Test void exactHomonymsAndReferencedRenamesAndDeletesAreBlocked() throws IOException {
        f.add(f.a, f.future, 8, 10);
        var before = f.files();
        assertThrows(IllegalArgumentException.class, () -> f.personnel.addEmployee("Ana", "Alpha", 20, "Opérationnel", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> f.clients.addClient("Client", "Fictif", "Autre", 0, null, null));
        assertThrows(IllegalArgumentException.class, () -> f.personnel.removeEmployee(f.personnel.getEmployees().indexOf(f.a)));
        assertThrows(IllegalArgumentException.class, () -> f.personnel.editEmployee(0, "Ana", "Renommée", 35, "Opérationnel", 0, 0));
        assertThrows(IllegalArgumentException.class, () -> f.clients.removeClient(0));
        assertThrows(IllegalArgumentException.class, () -> f.clients.editClients(0, "Autre", "Fictif", "Adresse", 0, null, null));
        assertEquals(before, f.files());
        f.personnel.editEmployee(0, "Ana", "Alpha", 24, "Opérationnel", 0, 0);
        assertEquals(24, f.a.getContractHours()); f.reload();
    }

    @Test void seriesAbsencesAndRequestsAlsoProtectTheirReferences() throws IOException {
        f.recurring.addRecurringIntervention(f.series(f.b, f.future, f.future.plusWeeks(1)));
        assertThrows(IllegalArgumentException.class, () -> f.personnel.removeEmployee(f.personnel.getEmployees().indexOf(f.b)));
        assertThrows(IllegalArgumentException.class, () -> f.clients.removeClient(0));
        f.absences.addAbsence(new EmployeeAbsence(f.a, EmployeeStatus.VACATION, f.future, f.future));
        assertThrows(IllegalArgumentException.class, () -> f.personnel.removeEmployee(0));
        f.replacements.addReplacement(f.c, 35, null, 0, FormatUtils.formatDate(f.future), "12h00", FormatUtils.formatDate(f.future), "13h00");
        assertThrows(IllegalArgumentException.class, () -> f.personnel.removeEmployee(f.personnel.getEmployees().indexOf(f.c)));
    }

    @Test void emptyOrInvalidSeriesNeverPersistAndEmployeeConflictsStillRefuse() throws IOException {
        var before = f.files();
        RecurringIntervention series = f.series(null, f.future, f.future);
        series.setDayOfWeek(f.future.plusDays(1).getDayOfWeek());
        assertThrows(IllegalArgumentException.class, () -> f.recurring.createWithPlanning(f.planning, series));
        series.setClient(null);
        assertThrows(IllegalArgumentException.class, () -> f.planning.previewRecurringInterventions(series));
        assertEquals(before, f.files());
        f.add(f.a, f.future, 8, 10);
        assertFalse(f.recurring.createWithPlanning(f.planning, f.series(f.a, f.future, f.future)));
        f.add(f.b, f.future, 8, 10); // Plusieurs salariés chez le même client restent permis.
        assertEquals(2, f.planning.getInterventions().size());
        f.add(null, f.future, 8, 10);
        f.reload();
    }

    @Test void replacementUsesHoursAndExclusiveMidnightAndRejectsPartialCoverage() throws IOException {
        Intervention morning = f.add(f.a, f.future, 8, 10);
        Intervention midday = f.add(f.a, f.future, 10, 12);
        Intervention next = f.add(f.a, f.future.plusDays(1), 0, 2);
        var before = f.files();
        assertThrows(IllegalArgumentException.class, () -> f.replacements.createWithPlanning(f.planning, f.a, f.b,
                FormatUtils.formatDate(f.future), "09h00", FormatUtils.formatDate(f.future), "11h00"));
        assertEquals(before, f.files());
        f.replacements.createWithPlanning(f.planning, f.a, f.b, FormatUtils.formatDate(f.future), "10h00",
                FormatUtils.formatDate(f.future.plusDays(1)), "00h00");
        assertSame(f.a, morning.getEmployee()); assertSame(f.b, midday.getEmployee()); assertSame(f.a, next.getEmployee());
        f.reload();
    }

    @Test void failedManualReplacementHasNoPartialAssignmentsOrRequest() throws IOException {
        Intervention i = f.add(f.a, f.future, 8, 10);
        f.add(f.b, f.future, 9, 11);
        var before = f.files();
        assertThrows(IllegalArgumentException.class, () -> f.replacements.createWithPlanning(f.planning, f.a, f.b,
                FormatUtils.formatDate(f.future), "08h00", FormatUtils.formatDate(f.future), "10h00"));
        assertSame(f.a, i.getEmployee()); assertTrue(f.replacements.isEmpty()); assertEquals(before, f.files());
    }

    @Test void absenceReleasesAndGroupsFutureInterventionsAndSupportsSeveralSubstitutes() throws IOException {
        Intervention first = f.add(f.a, f.future, 8, 10);
        Intervention second = f.add(f.a, f.future, 10, 12);
        Intervention outside = f.add(f.a, f.future.plusDays(1), 8, 10);
        EmployeeAbsence absence = new EmployeeAbsence(f.a, EmployeeStatus.VACATION, f.future, f.future);
        f.absences.addAbsence(absence);
        assertNull(first.getEmployee()); assertNull(second.getEmployee()); assertSame(f.a, outside.getEmployee());
        assertEquals(absence.getId(), first.getSourceAbsenceId());
        assertEquals(absence.getId(), f.replacements.getReplacements().getFirst().getAbsenceId());
        assertEquals(2, f.planning.getInterventionsByAbsenceId(absence.getId()).size());
        assertThrows(IllegalArgumentException.class, () -> f.planning.assignEmployee(first, f.a));
        f.planning.assignEmployee(first, f.b);
        f.planning.assignEmployee(second, f.c);
        assertEquals(absence.getId(), first.getSourceAbsenceId()); f.reload();
        PersonnelService people = new PersonnelService(new EmployeeCsvRepository(directory));
        ClientService clients = new ClientService(new ClientCsvRepository(directory));
        PlanningService planning = new PlanningService(people, clients, new InterventionCsvRepository(directory));
        var group = planning.getInterventionsByAbsenceId(absence.getId());
        assertEquals(2, group.size()); assertEquals("Béa", group.getFirst().getEmployee().getFirstName());
        assertEquals("Célia", group.getLast().getEmployee().getFirstName());
        assertThrows(IllegalArgumentException.class, () -> f.absences.resumeEmployee(f.a, f.future));
    }

    @Test void absenceGroupCanBeAssignedToOneSubstituteWithoutOverwritingExistingAssignments() throws IOException {
        Intervention first = f.add(f.a, f.future, 8, 10);
        Intervention second = f.add(f.a, f.future, 10, 12);
        f.absences.addAbsence(new EmployeeAbsence(f.a, EmployeeStatus.SICK_LEAVE, f.future, f.future));
        f.planning.assignEmployee(first, f.b);
        f.replacements.assignUnassignedGroup(f.planning, 0, f.c);
        assertSame(f.b, first.getEmployee()); assertSame(f.c, second.getEmployee());
        assertThrows(IllegalArgumentException.class, () -> f.replacements.removeReplacement(0));
        f.reload();
    }

    @Test void absenceNeverRewritesPastInterventions() throws IOException {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Intervention past = f.add(f.a, yesterday, 8, 10);
        f.absences.addAbsence(new EmployeeAbsence(f.a, EmployeeStatus.VACATION, yesterday, yesterday));
        assertSame(f.a, past.getEmployee()); assertNull(past.getSourceAbsenceId()); f.reload();
    }

    @Test void invalidAbsenceParametersAndDuplicateIdsDoNotPersist() throws IOException {
        var before = f.files();
        for (EmployeeAbsence invalid : List.of(new EmployeeAbsence(null, EmployeeStatus.VACATION, f.future, f.future),
                new EmployeeAbsence(f.a, null, f.future, f.future), new EmployeeAbsence(f.a, EmployeeStatus.OPERATIONAL, f.future, f.future),
                new EmployeeAbsence(f.a, EmployeeStatus.VACATION, f.future.plusDays(1), f.future)))
            assertThrows(IllegalArgumentException.class, () -> f.absences.addAbsence(invalid));
        assertEquals(before, f.files());
        EmployeeAbsence first = new EmployeeAbsence(f.a, EmployeeStatus.VACATION, f.future, f.future);
        f.absences.addAbsence(first);
        EmployeeAbsence duplicate = new EmployeeAbsence(f.b, EmployeeStatus.VACATION, f.future, f.future);
        duplicate.setId(first.getId());
        assertThrows(IllegalArgumentException.class, () -> f.absences.addAbsence(duplicate));
    }

    @Test void globalSeriesActionsPreserveHistoricalObjectsAndDefinitions() throws IOException {
        LocalDate past = LocalDate.now().minusWeeks(2);
        RecurringIntervention series = f.series(null, past, past.plusWeeks(4));
        assertTrue(f.recurring.createWithPlanning(f.planning, series));
        var history = new ArrayList<>(f.planning.getInterventionsByRecurringId(series.getId()));
        history.removeIf(i -> !i.getStartDate().atTime(i.getStartTime()).isBefore(LocalDateTime.now()));
        f.recurring.assignFutureSeries(f.planning, series.getId(), f.b);
        history.forEach(i -> assertNull(i.getEmployee()));
        f.recurring.removeFutureSeries(f.planning, series.getId());
        assertEquals(history, f.planning.getInterventionsByRecurringId(series.getId()));
        assertEquals(1, f.recurring.getRecurringInterventions().size()); f.reload();
    }

    @Test void startupIsReadOnlyAndOldInterventionFormatsRemainReadable() throws IOException {
        String old = "Client;Fictif;Ana;Alpha;" + f.future + ";08:00;" + f.future + ";10:00\n";
        Files.writeString(directory.resolve("interventions.csv"), old);
        var before = f.files(); f.reload(); assertEquals(before, f.files());
        assertNull(new InterventionCsvRepository(directory).load(f.personnel.getEmployees(), f.clients.getClients()).getFirst().getSourceAbsenceId());
    }

    @Test void orphanedAbsenceOrSeriesLinksRefuseStartupWithoutWriting() throws IOException {
        Intervention i = f.add(null, f.future, 8, 10);
        i.setSourceAbsenceId("absence-fictive-inconnue");
        new InterventionCsvRepository(directory).save(f.planning.getInterventions());
        var before = f.files(); assertThrows(IOException.class, f::reload); assertEquals(before, f.files());
    }
}
