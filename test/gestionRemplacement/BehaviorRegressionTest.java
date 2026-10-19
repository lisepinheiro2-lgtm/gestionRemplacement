package gestionRemplacement;

import gestionRemplacement.controller.PersonnelController;
import gestionRemplacement.model.Client;
import gestionRemplacement.model.Employee;
import gestionRemplacement.model.EmployeeAbsence;
import gestionRemplacement.model.EmployeeStatus;
import gestionRemplacement.model.EmployeeSummary;
import gestionRemplacement.model.HoursAlert;
import gestionRemplacement.model.Intervention;
import gestionRemplacement.model.RecurringIntervention;
import gestionRemplacement.repository.AbsenceCsvRepository;
import gestionRemplacement.repository.ClientCsvRepository;
import gestionRemplacement.repository.EmployeeCsvRepository;
import gestionRemplacement.repository.InterventionCsvRepository;
import gestionRemplacement.repository.RecurringInterventionCsvRepository;
import gestionRemplacement.repository.ReplacementCsvRepository;
import gestionRemplacement.service.AbsenceService;
import gestionRemplacement.service.ClientService;
import gestionRemplacement.service.PersonnelService;
import gestionRemplacement.service.PlanningService;
import gestionRemplacement.service.RecurringInterventionService;
import gestionRemplacement.service.ReplacementService;
import gestionRemplacement.service.WorkHoursService;
import gestionRemplacement.ui.ApplicationHeader;
import gestionRemplacement.ui.ClientUI;
import gestionRemplacement.ui.HomeCardButton;
import gestionRemplacement.ui.HomePanel;
import gestionRemplacement.ui.ReplacementUI;
import gestionRemplacement.ui.intervention.InterventionUI;
import gestionRemplacement.ui.personnel.PersonnelPanel;
import gestionRemplacement.ui.planning.PlanningUI;
import java.awt.Component;
import java.awt.Container;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Régressions fonctionnelles avec données synthétiques dans un dossier
 * temporaire.
 */
class BehaviorRegressionTest {
    static {
        System.setProperty("java.util.prefs.PreferencesFactory",
                "gestionRemplacement.support.InMemoryPreferencesFactory");
    }
    @TempDir
    Path directory;
    PersonnelService personnel;
    ClientService clients;
    PlanningService planning;
    ReplacementService replacements;
    AbsenceService absences;
    WorkHoursService hours;
    PersonnelController controller;

    @BeforeEach
    void initialize() throws IOException {
        personnel = new PersonnelService(new EmployeeCsvRepository(directory));
        clients = new ClientService(new ClientCsvRepository(directory));
        planning = new PlanningService(personnel, clients, new InterventionCsvRepository(directory));
        replacements = new ReplacementService(new ReplacementCsvRepository(directory));
        absences = new AbsenceService(personnel, replacements, new AbsenceCsvRepository(directory));
        hours = new WorkHoursService(planning, Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneOffset.UTC));
        controller = new PersonnelController(personnel, absences, hours);
    }

    Employee employee(String first, String last, double contract) {
        personnel.addEmployee(first, last, contract, "Opérationnel", 0, 0);
        return personnel.findEmployee(first, last);
    }

    Client client() {
        clients.addClient("Élise", "Client", "Adresse de test", 20, LocalDate.of(2026, 1, 1), null);
        return clients.getClients().getFirst();
    }

    Intervention intervention(Employee employee, LocalDate day, int start, int end) {
        Intervention intervention = new Intervention(client(), employee, day, LocalTime.of(start, 0), day,
                LocalTime.of(end, 0));
        planning.addIntervention(intervention);
        return intervention;
    }

    EmployeeAbsence absence(Employee employee, LocalDate start, LocalDate end) throws IOException {
        EmployeeAbsence absence = new EmployeeAbsence(employee, EmployeeStatus.VACATION, start, end);
        absences.addAbsence(absence);
        return absence;
    }

    @Test
    void employeeCsvSupportsLegacyAndCurrentRows() throws IOException {
        Files.writeString(directory.resolve("employees.csv"),
                "Ana;Ancien;24.5\nZoé;Récent;35.0;Opérationnel;2.0;33.0\n");
        PersonnelService loaded = new PersonnelService(new EmployeeCsvRepository(directory));
        assertEquals(2, loaded.getEmployees().size());
        assertEquals(24.5, loaded.findEmployee("Ana", "Ancien").getContractHours());
        loaded.addEmployee("Alex", "Autre", 30, "Opérationnel", 0, 0);
        assertEquals(3, new PersonnelService(new EmployeeCsvRepository(directory)).getEmployees().size());
    }

    @Test
    void employeeEditsKeepIdentityAndResortTheList() {
        Employee a = employee("A", "Alpha", 35);
        Employee b = employee("B", "Beta", 20);
        controller.editEmployee(a, "A", "Zulu", "24h30");
        assertSame(a, personnel.getEmployees().getLast());
        assertSame(b, personnel.getEmployees().getFirst());
        assertEquals(24.5, a.getContractHours());
        controller.editEmployee(a, "A", "Omega", "25,5");
        assertEquals("Beta", b.getLastName());
    }

    @Test
    void blankEmployeeInputDoesNotMutateTheList() {
        assertThrows(IllegalArgumentException.class, () -> controller.addEmployee("", "Nom", "35"));
        assertThrows(IllegalArgumentException.class, () -> controller.addEmployee("Prénom", "Nom", "24h99"));
        assertTrue(personnel.isEmpty());
    }

    @Test
    void employeeDeletionPersists() {
        Employee a = employee("A", "Nom", 35);
        controller.removeEmployee(a);
        assertTrue(new PersonnelService(new EmployeeCsvRepository(directory)).isEmpty());
    }

    @Test
    void clientCsvRetainsLegacyRowsAndOptionalDates() throws IOException {
        Files.writeString(directory.resolve("clients.csv"), "Ancien;Client\n");
        ClientService loaded = new ClientService(new ClientCsvRepository(directory));
        assertEquals("N/A", loaded.getClients().getFirst().getAddress());
        loaded.addClient("Nouveau", "Client", "Rue", 20, LocalDate.of(2026, 1, 1), null);
        ClientService again = new ClientService(new ClientCsvRepository(directory));
        assertEquals(2, again.getClients().size());
        assertNull(again.findClient("Nouveau", "Client").getContractEndDate());
    }

    @Test
    void planningReloadResolvesTheSameEmployeeObjects() {
        Employee employee = employee("Ana", "Nom", 35);
        intervention(employee, LocalDate.of(2026, 10, 12), 8, 10);
        PlanningService loaded = new PlanningService(personnel, clients, new InterventionCsvRepository(directory));
        assertSame(employee, loaded.getInterventions().getFirst().getEmployee());
        assertEquals(2, new WorkHoursService(loaded).getWeeklyHours(employee, LocalDate.of(2026, 10, 12)));
    }

    @Test
    void weeklyAndMonthlyHoursSplitAtTheirBoundaries() {
        Employee employee = employee("A", "Nom", 35);
        planning.addIntervention(new Intervention(client(), employee, LocalDate.of(2026, 10, 31), LocalTime.of(23, 0),
                LocalDate.of(2026, 11, 2), LocalTime.of(2, 0)));
        assertEquals(1, hours.getMonthlyHours(employee, LocalDate.of(2026, 10, 1)));
        assertEquals(26, hours.getMonthlyHours(employee, LocalDate.of(2026, 11, 1)));
        assertEquals(2, hours.getWeeklyHours(employee, LocalDate.of(2026, 11, 2)));
    }

    @Test
    void personnelUsesIndependentAssignedAndRemainingPeriods() {
        Employee employee = employee("A", "Nom", 35);
        intervention(employee, LocalDate.of(2026, 10, 12), 8, 10);
        intervention(employee, LocalDate.of(2026, 10, 19), 8, 13);
        EmployeeSummary row = controller.getRows(LocalDate.of(2026, 10, 12), false, LocalDate.of(2026, 10, 19), false)
                .getFirst();
        assertEquals(2, row.assignedHours());
        assertEquals(30, row.remainingHours());
        EmployeeSummary month = controller.getRows(LocalDate.of(2026, 10, 12), true, LocalDate.of(2026, 10, 12), true)
                .getFirst();
        assertEquals(7, month.assignedHours());
        assertEquals(35.0 * 52 / 12 - 7, month.remainingHours());
    }

    @Test
    void absencesIncludeBothBoundaryDaysAndReloadIdentity() throws IOException {
        Employee employee = employee("A", "Nom", 35);
        absence(employee, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 14));
        AbsenceService loaded = new AbsenceService(personnel, replacements, new AbsenceCsvRepository(directory));
        assertSame(employee, loaded.getAbsences().getFirst().getEmployee());
        assertEquals(EmployeeStatus.VACATION, loaded.getStatusAt(employee, LocalDate.of(2026, 10, 12)));
        assertEquals(EmployeeStatus.VACATION, loaded.getStatusAt(employee, LocalDate.of(2026, 10, 14)));
        assertEquals(EmployeeStatus.OPERATIONAL, loaded.getStatusAt(employee, LocalDate.of(2026, 10, 15)));
    }

    @Test
    void overlappingAbsenceIsRejectedWithoutChangingTheFile() throws IOException {
        Employee employee = employee("A", "Nom", 35);
        absence(employee, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 14));
        String saved = Files.readString(directory.resolve("employee_absences.csv"));
        assertThrows(IllegalArgumentException.class,
                () -> absence(employee, LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 16)));
        assertEquals(saved, Files.readString(directory.resolve("employee_absences.csv")));
        assertEquals(1, absences.getAbsences().size());
    }

    @Test
    void sameDatesAreAllowedForDifferentEmployees() throws IOException {
        absence(employee("A", "Nom", 35), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 14));
        absence(employee("B", "Nom", 35), LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 14));
        assertEquals(2, replacements.getReplacements().size());
    }

    @Test
    void resumingShortensTheAbsenceAndItsLinkedRequest() throws IOException {
        Employee employee = employee("A", "Nom", 35);
        EmployeeAbsence absence = absence(employee, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 16));
        absences.resumeEmployee(employee, LocalDate.of(2026, 10, 14));
        assertEquals(LocalDate.of(2026, 10, 13), absence.getEndDate());
        assertEquals("14/10/2026", replacements.getReplacements().getFirst().getEndDate());
        assertEquals(EmployeeStatus.OPERATIONAL, absences.getStatusAt(employee, LocalDate.of(2026, 10, 14)));
    }

    @Test
    void resumingOnFirstDayRemovesTheLinkedRequest() throws IOException {
        Employee employee = employee("A", "Nom", 35);
        absence(employee, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 16));
        absences.resumeEmployee(employee, LocalDate.of(2026, 10, 12));
        assertTrue(absences.getAbsences().isEmpty());
        assertTrue(replacements.isEmpty());
    }

    @Test
    void absenceSaveFailureRollsBackMemory() throws IOException {
        Employee employee = employee("A", "Nom", 35);
        Files.createDirectory(directory.resolve("employee_absences.csv"));
        assertThrows(IOException.class,
                () -> absence(employee, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 14)));
        assertTrue(absences.getAbsences().isEmpty());
        assertTrue(replacements.isEmpty());
    }

    @Test
    void resumeSaveFailureRestoresThePreviousEndDate() throws IOException {
        Employee employee = employee("A", "Nom", 35);
        EmployeeAbsence absence = absence(employee, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 16));
        Files.delete(directory.resolve("employee_absences.csv"));
        Files.createDirectory(directory.resolve("employee_absences.csv"));
        assertThrows(IOException.class, () -> absences.resumeEmployee(employee, LocalDate.of(2026, 10, 14)));
        assertEquals(LocalDate.of(2026, 10, 16), absence.getEndDate());
    }

    @Test
    void corruptAbsenceFileFailsWithoutOverwritingIt() throws IOException {
        Path path = directory.resolve("employee_absences.csv");
        Files.writeString(path, "ligne invalide\n");
        assertThrows(IOException.class,
                () -> new AbsenceService(personnel, replacements, new AbsenceCsvRepository(directory)));
        assertEquals("ligne invalide\n", Files.readString(path));
    }

    @Test
    void absenceSynchronizationIsIdempotentAndPersistsItsLink() throws IOException {
        Employee employee = employee("A", "Nom", 35);
        EmployeeAbsence absence = absence(employee, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 14));
        replacements.synchronizeAbsences(absences.getAbsences());
        replacements.synchronizeAbsences(absences.getAbsences());
        ReplacementService loaded = new ReplacementService(new ReplacementCsvRepository(directory));
        assertEquals(1, loaded.getReplacements().size());
        assertEquals(absence.getId(), loaded.getReplacements().getFirst().getAbsenceId());
        assertEquals("15/10/2026", loaded.getReplacements().getFirst().getEndDate());
    }

    @Test
    void recurringOccurrencesAndAssignmentSurviveReload() {
        Employee employee = employee("A", "Nom", 35);
        RecurringIntervention recurring = new RecurringIntervention(client(), null, DayOfWeek.MONDAY,
                LocalTime.of(8, 0), LocalTime.of(10, 0), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        RecurringInterventionService service = new RecurringInterventionService(clients, personnel,
                new RecurringInterventionCsvRepository(directory));
        assertTrue(planning.generateRecurringInterventions(recurring));
        service.addRecurringIntervention(recurring);
        planning.assignEmployee(planning.getInterventionsByRecurringId(recurring.getId()), employee);
        service.assignEmployeeByRecurringId(recurring.getId(), employee);
        assertEquals(8, hours.getMonthlyHours(employee, LocalDate.of(2026, 10, 1)));
        assertSame(employee,
                new RecurringInterventionService(clients, personnel, new RecurringInterventionCsvRepository(directory))
                        .getRecurringInterventions().getFirst().getEmployee());
        planning.removeInterventionsByRecurringId(recurring.getId());
        service.removeRecurringInterventionById(recurring.getId());
        assertTrue(planning.isEmpty());
        assertTrue(service.isEmpty());
    }

    @Test
    void adjacentInterventionsAreAllowedAndConflictingReplacementDoesNotPartiallyApply() {
        Employee original = employee("A", "Nom", 35);
        Employee replacement = employee("B", "Nom", 35);
        LocalDate day = LocalDate.of(2026, 10, 12);
        Intervention first = intervention(original, day, 8, 10);
        assertFalse(planning.hasOverlap(original,
                new Intervention(first.getClient(), original, day, LocalTime.of(10, 0), day, LocalTime.of(12, 0))));
        intervention(replacement, day, 9, 11);
        assertFalse(planning.replaceEmployeeInterventions(original, replacement, day, day));
        assertSame(original, first.getEmployee());
    }

    @Test
    void hoursAlertsUseTheSelectedPeriodAndTheClock() {
        Employee employee = employee("A", "Nom", 2);
        LocalDate day = LocalDate.of(2026, 10, 12);
        assertEquals(HoursAlert.IN_PROGRESS, hours.getHoursAlert(employee, day, false));
        intervention(employee, day, 8, 10);
        assertEquals(HoursAlert.TARGET_REACHED, hours.getHoursAlert(employee, day, false));
        intervention(employee, day, 11, 12);
        assertEquals(HoursAlert.ALERT, hours.getHoursAlert(employee, day, false));
    }

    @Test
    void personnelSwingEditingStillTargetsTheRightEmployeeAfterResorting() throws Exception {
        Employee first = employee("A", "Alpha", 35);
        Employee second = employee("B", "Beta", 20);
        SwingUtilities.invokeAndWait(() -> {
            PersonnelPanel panel = new PersonnelPanel(controller);
            JTable table = find(panel, JTable.class).getFirst();
            assertEquals(6, table.getColumnCount());
            table.getModel().setValueAt("Zulu", 0, 0);
            assertEquals("Zulu", first.getLastName());
            table.getModel().setValueAt("24h30", 0, 2);
            assertEquals(24.5, second.getContractHours());
            assertEquals(35, first.getContractHours());
            find(panel, JComboBox.class).forEach(combo -> combo.setSelectedIndex(1));
            assertEquals(2, table.getRowCount());
            assertNotNull(table.prepareRenderer(table.getCellRenderer(0, 4), 0, 4));
        });
    }

    @Test
    void existingPagesAndHomeNavigationStillAssemble() throws Exception {
        employee("A", "Nom", 35);
        client();
        RecurringInterventionService recurring = new RecurringInterventionService(clients, personnel,
                new RecurringInterventionCsvRepository(directory));
        SwingUtilities.invokeAndWait(() -> {
            new ClientUI(clients);
            new ReplacementUI(replacements, personnel, planning).refreshTable();
            new InterventionUI(planning, recurring, clients, personnel).refreshIntervention();
            new PlanningUI(planning, personnel, clients, recurring).refreshPlanning();
            int[] calls = { 0 };
            Runnable callback = () -> calls[0]++;
            HomePanel home = new HomePanel(callback, callback, callback, callback, callback);
            home.applyAppTheme(false);
            home.applyAppTheme(true);
            List<HomeCardButton> buttons = find(home, HomeCardButton.class);
            assertEquals(6, buttons.size());
            assertEquals(List.of("Personnel", "Clients", "Planning", "Remplacements", "Interventions", "Archives"),
                    buttons.stream().map(button -> button.getAccessibleContext().getAccessibleName()).toList());
            buttons.forEach(AbstractButton::doClick);
            assertEquals(5, calls[0]);
            assertFalse(buttons.getLast().isEnabled());
        });
    }

    @Test
    void themeIconsAndApplicationHeaderRenderAfterPackageMoves() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var icon = new gestionRemplacement.ui.raven.DarkLightSwitchIcon();
            assertTrue(icon.getIconWidth() > 0);
            assertTrue(icon.getDarkIcon().getIconHeight() > 0);
            assertTrue(icon.getLightIcon().getIconWidth() > 0);
            JPanel page = new JPanel();
            var image = new javax.swing.ImageIcon(getClass().getResource("/icons/gestion-64.png")).getImage();
            ApplicationHeader header = new ApplicationHeader(page, new JButton("Accueil"), image);
            header.setPageTitle("Personnel");
            header.applyAppTheme(false);
            header.setSize(1108, 80);
            header.doLayout();
            var canvas = new java.awt.image.BufferedImage(1108, 80, java.awt.image.BufferedImage.TYPE_INT_RGB);
            var graphics = canvas.createGraphics();
            header.printAll(graphics);
            graphics.dispose();
            header.applyAppTheme(true);
        });
    }

    static <T> List<T> find(Container root, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child))
                result.add(type.cast(child));
            if (child instanceof Container container)
                result.addAll(find(container, type));
        }
        return result;
    }
}
