package gestionTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import gestion.controller.PersonnelController;
import gestion.model.Client;
import gestion.model.Employee;
import gestion.model.EmployeeAbsence;
import gestion.model.EmployeeStatus;
import gestion.model.EmployeeSummary;
import gestion.model.HoursAlert;
import gestion.model.Intervention;
import gestion.model.RecurringIntervention;
import gestion.repository.AbsenceCsvRepository;
import gestion.repository.ClientCsvRepository;
import gestion.repository.EmployeeCsvRepository;
import gestion.repository.InterventionCsvRepository;
import gestion.repository.RecurringInterventionCsvRepository;
import gestion.repository.ReplacementCsvRepository;
import gestion.service.AbsenceService;
import gestion.service.ClientService;
import gestion.service.PersonnelService;
import gestion.service.PlanningService;
import gestion.service.RecurringInterventionService;
import gestion.service.ReplacementService;
import gestion.service.WorkHoursService;
import gestion.ui.ApplicationHeader;
import gestion.ui.ClientUI;
import gestion.ui.HomeCardButton;
import gestion.ui.HomePanel;
import gestion.ui.ReplacementUI;
import gestion.ui.intervention.InterventionUI;
import gestion.ui.personnel.PersonnelPanel;
import gestion.ui.planning.PlanningUI;
import gestion.util.FormatUtils;

class BehaviorRegressionTest {
	static {
		System.setProperty("java.util.prefs.PreferencesFactory", "gestionTest.support.InMemoryPreferencesFactory");
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
		absences = new AbsenceService(personnel, replacements, planning, new AbsenceCsvRepository(directory));
		hours = new WorkHoursService(planning, Clock.fixed(Instant.parse("2026-10-12T12:00:00Z"), ZoneOffset.UTC));
		controller = new PersonnelController(personnel, absences, hours);
	}

	Employee employee(String first, String last, double contract) {
		personnel.addEmployee(first, last, contract, "Opérationnel", 0, 0);
		return personnel.findEmployee(first, last);
	}

	Client client() {
		Client existing = clients.findClient("Élise", "Client");
		if (existing != null)
			return existing;
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
		assertTrue(gestion.repository.CsvIdentityMigration.migrate(directory).migrated());
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
		assertTrue(gestion.repository.CsvIdentityMigration.migrate(directory).migrated());
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
		EmployeeSummary row = controller.getRows(LocalDate.of(2026, 10, 12), false).getFirst();

		assertEquals(2, row.assignedHours());
		assertEquals(33, row.remainingHours());

		EmployeeSummary month = controller.getRows(LocalDate.of(2026, 10, 12), true).getFirst();

		assertEquals(7, month.assignedHours());
		assertEquals(35.0 * 52 / 12 - 7, month.remainingHours());
	}

	@Test
	void absencesIncludeBothBoundaryDaysAndReloadIdentity() throws IOException {
		Employee employee = employee("A", "Nom", 35);
		absence(employee, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 14));
		AbsenceService loaded = new AbsenceService(personnel, replacements, planning,
				new AbsenceCsvRepository(directory));

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

		assertEquals(0, replacements.getReplacements().size());
	}

	@Test
	void resumingShortensAbsenceWithoutReplacement() throws IOException {

		Employee employee = employee("A", "Nom", 35);
		EmployeeAbsence absence = absence(employee, LocalDate.of(2026, 10, 12), LocalDate.of(2026, 10, 16));

		assertTrue(replacements.getReplacements().isEmpty());

		absences.resumeEmployee(employee, LocalDate.of(2026, 10, 14));

		assertEquals(LocalDate.of(2026, 10, 13), absence.getEndDate());
		assertEquals(EmployeeStatus.OPERATIONAL, absences.getStatusAt(employee, LocalDate.of(2026, 10, 14)));
		assertTrue(replacements.getReplacements().isEmpty());
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
				() -> new AbsenceService(personnel, replacements, planning, new AbsenceCsvRepository(directory)));
		assertEquals("ligne invalide\n", Files.readString(path));
	}

	@Test
	void absenceSynchronizationIsIdempotentAndPersistsItsLink() throws IOException {

		Employee employee = employee("A", "Nom", 35);
		LocalDate date = LocalDate.now().plusDays(30);
		Intervention intervention = intervention(employee, date, 8, 10);
		EmployeeAbsence absence = absence(employee, date, date);

		assertNull(intervention.getEmployee());
		assertEquals(absence.getId(), intervention.getSourceAbsenceId());
		assertEquals(1, replacements.getReplacements().size());

		String previousContent = Files.readString(directory.resolve("replacements.csv"));
		replacements.synchronizeAbsences(absences.getAbsences(), planning);
		replacements.synchronizeAbsences(absences.getAbsences(), planning);

		assertEquals(1, replacements.getReplacements().size());
		assertEquals(previousContent, Files.readString(directory.resolve("replacements.csv")));

		ReplacementService loaded = new ReplacementService(new ReplacementCsvRepository(directory));

		assertEquals(1, loaded.getReplacements().size());
		assertEquals(absence.getId(), loaded.getReplacements().getFirst().getAbsenceId());
	}

	@Test
	void recurringOccurrencesAndAssignmentSurviveReload() {
		Employee employee = employee("A", "Nom", 35);
		LocalDate start = LocalDate.now().plusYears(1).withDayOfMonth(1)
				.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
		RecurringIntervention recurring = new RecurringIntervention(client(), null, DayOfWeek.MONDAY,
				LocalTime.of(8, 0), LocalTime.of(10, 0), start, start.plusWeeks(3));
		RecurringInterventionService service = new RecurringInterventionService(clients, personnel,
				new RecurringInterventionCsvRepository(directory));

		assertTrue(service.createWithPlanning(planning, recurring));
		service.assignFutureSeries(planning, recurring.getId(), employee);

		assertEquals(8, hours.getMonthlyHours(employee, start));
		assertSame(employee,
				new RecurringInterventionService(clients, personnel, new RecurringInterventionCsvRepository(directory))
						.getRecurringInterventions().getFirst().getEmployee());

		service.removeFutureSeries(planning, recurring.getId());

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

		assertEquals(HoursAlert.ALERT, hours.getHoursAlert(employee, day, false));

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
			PersonnelPanel panel = new PersonnelPanel(controller, planning);
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
			var icon = new gestion.ui.raven.DarkLightSwitchIcon();

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

	@Test
	void assigningInterventionUpdatesWeeklyHours() {
		LocalDate date = LocalDate.of(2026, 10, 13);
		Employee employee = employee("Alice", "Test", 24);
		Intervention intervention = intervention(null, date, 10, 12);

		assertEquals(0.0, hours.getWeeklyHours(employee, date));

		planning.assignEmployee(intervention, employee);

		assertSame(employee, intervention.getEmployee());
		assertEquals(2.0, hours.getWeeklyHours(employee, date));
		assertEquals(22.0, employee.getContractHours() - hours.getWeeklyHours(employee, date));
	}

	@Test
	void unassigningInterventionUpdatesWeeklyHours() {
		LocalDate date = LocalDate.of(2026, 10, 13);
		Employee employee = employee("Alice", "Test", 24);
		Intervention intervention = intervention(employee, date, 10, 12);

		assertEquals(2.0, hours.getWeeklyHours(employee, date));

		planning.unassignEmployee(List.of(intervention));

		assertNull(intervention.getEmployee());
		assertEquals(0.0, hours.getWeeklyHours(employee, date));
		assertEquals(24.0, employee.getContractHours() - hours.getWeeklyHours(employee, date));
	}

	@Test
	void absenceBlocksBothDaysWithoutCreatingReplacement() throws IOException {

		Employee employee = employee("Inès", "Brindille", 30);
		LocalDate firstDay = LocalDate.now().plusDays(30);
		LocalDate lastDay = firstDay.plusDays(1);
		absence(employee, firstDay, lastDay);

		assertTrue(replacements.getReplacements().isEmpty());

		Intervention first = intervention(null, firstDay, 8, 10);
		Intervention last = intervention(null, lastDay, 8, 10);
		Intervention after = intervention(null, lastDay.plusDays(1), 8, 10);

		assertThrows(IllegalArgumentException.class, () -> planning.assignEmployee(first, employee));
		assertThrows(IllegalArgumentException.class, () -> planning.assignEmployee(last, employee));
		assertNull(first.getEmployee());
		assertNull(last.getEmployee());

		planning.assignEmployee(after, employee);

		assertSame(employee, after.getEmployee());
	}

	@Test
	void absenceCreatesReplacementOnlyWhenNeeded() throws IOException {

		Employee employee = employee("Alice", "Test", 24);
		LocalDate date = LocalDate.now().plusDays(30);
		Intervention intervention = intervention(employee, date, 8, 10);
		EmployeeAbsence absence = absence(employee, date, date.plusDays(1));

		assertNull(intervention.getEmployee());
		assertEquals(absence.getId(), intervention.getSourceAbsenceId());
		assertEquals(1, replacements.getReplacements().size());
		assertEquals(absence.getId(), replacements.getReplacements().getFirst().getAbsenceId());
		assertEquals(FormatUtils.formatDate(date.plusDays(2)), replacements.getReplacements().getFirst().getEndDate());
	}
}
