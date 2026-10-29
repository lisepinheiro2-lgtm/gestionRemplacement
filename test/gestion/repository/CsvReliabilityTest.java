package gestion.repository;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import gestion.model.*;
import gestionTest.support.TestFixture;

class CsvReliabilityTest {

	@TempDir
	Path directory;
	TestFixture f;

	@BeforeEach
	void initialize() throws IOException {
		f = new TestFixture(directory);
	}

	@AfterEach
	void clearHooks() {
		CsvStore.installHook = count -> {
		};
		CsvStore.writeHook = target -> {
		};
	}

	@Test
	void allSixWritersPreserveExistingBytesAfterTemporaryWriteFailure() throws Exception {
		f.add(f.a, f.future, 8, 10);
		f.recurring.addRecurringIntervention(f.series(f.b, f.future.plusDays(1), f.future.plusDays(1)));
		f.absences.addAbsence(new EmployeeAbsence(f.c, EmployeeStatus.VACATION, f.future, f.future));
		Map<String, Runnable> writers = new LinkedHashMap<>();
		writers.put("employees.csv", () -> new EmployeeCsvRepository(directory).save(f.personnel.getEmployees()));
		writers.put("clients.csv", () -> new ClientCsvRepository(directory).save(f.clients.getClients()));
		writers.put("interventions.csv",
				() -> new InterventionCsvRepository(directory).save(f.planning.getInterventions()));
		writers.put("recurringInterventions.csv",
				() -> new RecurringInterventionCsvRepository(directory).save(f.recurring.getRecurringInterventions()));
		writers.put("replacements.csv",
				() -> new ReplacementCsvRepository(directory).save(f.replacements.getReplacements()));
		writers.put("employee_absences.csv", () -> {

			try {
				new AbsenceCsvRepository(directory).save(f.absences.getAbsences());
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		});

		for (var entry : writers.entrySet()) {
			byte[] before = Files.readAllBytes(directory.resolve(entry.getKey()));
			boolean[] invoked = { false };
			CsvStore.writeHook = target -> {
				invoked[0] = true;
				throw new IOException("Panne fictive après écriture du temporaire");
			};
			assertThrows(UncheckedIOException.class, entry.getValue()::run);
			assertTrue(invoked[0]);
			assertArrayEquals(before, Files.readAllBytes(directory.resolve(entry.getKey())));

			try (var files = Files.list(directory)) {
				assertTrue(files.noneMatch(p -> p.toString().endsWith(".tmp")));
			}
		}
	}

	@Test
	void everyLoaderFailsOnMalformedTrailingRowAndLeavesAllBytesUntouched() throws Exception {
		f.add(null, f.future, 8, 10);
		f.recurring.addRecurringIntervention(f.series(null, f.future, f.future));
		f.absences.addAbsence(new EmployeeAbsence(f.c, EmployeeStatus.VACATION, f.future, f.future));
		Map<String, org.junit.jupiter.api.function.Executable> readers = new LinkedHashMap<>();
		readers.put("employees.csv", () -> new EmployeeCsvRepository(directory).load());
		readers.put("clients.csv", () -> new ClientCsvRepository(directory).load());
		readers.put("interventions.csv", () -> new InterventionCsvRepository(directory).load(f.personnel.getEmployees(),
				f.clients.getClients()));
		readers.put("recurringInterventions.csv", () -> new RecurringInterventionCsvRepository(directory)
				.load(f.personnel.getEmployees(), f.clients.getClients()));
		readers.put("replacements.csv", () -> new ReplacementCsvRepository(directory).load());
		readers.put("employee_absences.csv",
				() -> new AbsenceCsvRepository(directory).load(f.personnel.getEmployees()));

		for (var entry : readers.entrySet()) {
			Path path = directory.resolve(entry.getKey());
			byte[] original = Files.readAllBytes(path);
			Files.writeString(path, "ligne-fictive-invalide\n", StandardOpenOption.APPEND);
			byte[] corrupted = Files.readAllBytes(path);
			Exception error = assertThrows(Exception.class, entry.getValue());
			assertTrue(error instanceof IOException || error instanceof UncheckedIOException);
			assertTrue(error.getMessage().contains(entry.getKey()));
			assertTrue(error.getMessage().contains("ligne"));
			assertFalse(error.getMessage().contains("ligne-fictive-invalide"));
			assertArrayEquals(corrupted, Files.readAllBytes(path));
			Files.write(path, original);
		}
	}

	@Test
	void unknownEmployeeCannotBecomeAnUnassignedInterventionAndInvalidUtf8Fails() throws Exception {
		Path path = directory.resolve("interventions.csv");
		String line = "Client;Fictif;Inconnu;Fictif;" + f.future + ";08:00;" + f.future + ";10:00;;\n";
		Files.writeString(path, line);
		assertThrows(UncheckedIOException.class, () -> new InterventionCsvRepository(directory)
				.load(f.personnel.getEmployees(), f.clients.getClients()));
		assertEquals(line, Files.readString(path));
		Files.write(path, new byte[] { (byte) 0xff }, StandardOpenOption.APPEND);
		assertThrows(UncheckedIOException.class, () -> new InterventionCsvRepository(directory)
				.load(f.personnel.getEmployees(), f.clients.getClients()));
	}

	@Test
	void invalidGlobalAbsenceInvariantsRefuseTheEntireFile() throws Exception {
		String base = "id-fictif;Alpha;Ana;VACATION;" + f.future + ";" + f.future + "\n";
		Path path = directory.resolve("employee_absences.csv");

		for (String second : List.of(base, base.replace("id-fictif", "autre-id"),
				base.replace("VACATION", "OPERATIONAL"))) {
			Files.writeString(path, base + second);
			assertThrows(IOException.class, () -> new AbsenceCsvRepository(directory).load(f.personnel.getEmployees()));
			assertEquals(base + second, Files.readString(path));
		}
	}

	@Test
	void structuralPlanningMutationsAndFieldEditsRollbackOnWriteFailure() throws Exception {
		Intervention first = f.add(f.a, f.future, 8, 10);
		Intervention second = f.add(null, f.future, 10, 12);
		var order = new ArrayList<>(f.planning.getInterventions());
		var before = f.files();
		CsvStore.writeHook = path -> {
			throw new IOException("panne fictive");
		};
		List<Runnable> operations = List.of(() -> f.planning.removeIntervention(first),
				() -> f.planning.removeIntervention(1),
				() -> f.planning.removeClientInterventions(f.client, f.future, f.future),
				() -> f.recurring
						.createWithPlanning(f.planning, f.series(null, f.future.plusWeeks(1), f.future.plusWeeks(2))),
				() -> f.planning.assignEmployee(second, f.b), () -> f.planning.editIntervention(0, f.client, f.c,
						f.future, LocalTime.of(7, 0), f.future, LocalTime.of(8, 0)));

		for (Runnable operation : operations) {
			assertThrows(UncheckedIOException.class, operation::run);
			assertEquals(order, f.planning.getInterventions());
			assertSame(f.a, first.getEmployee());
			assertNull(second.getEmployee());
			assertEquals(LocalTime.of(8, 0), first.getStartTime());
			assertEquals(before, f.files());
		}

		CsvStore.writeHook = path -> {
		};
		f.reload();
	}

	@Test
	void personAddsEditsAndDeletesRestoreValuesIdentityAndOrder() throws Exception {
		var people = new ArrayList<>(f.personnel.getEmployees());
		var before = f.files();
		CsvStore.writeHook = path -> {
			throw new IOException("panne fictive");
		};
		assertThrows(UncheckedIOException.class,
				() -> f.personnel.addEmployee("Autre", "Test", 20, "Opérationnel", 0, 0));
		assertThrows(UncheckedIOException.class,
				() -> f.personnel.editEmployee(0, "Ana", "Zulu", 20, "Opérationnel", 0, 0));
		assertThrows(UncheckedIOException.class, () -> f.personnel.removeEmployee(0));
		assertEquals(people, f.personnel.getEmployees());
		assertEquals("Alpha", f.a.getLastName());
		assertEquals(35, f.a.getContractHours());
		assertThrows(UncheckedIOException.class,
				() -> f.clients.editClients(0, "Nouveau", "Nom", "Nouvelle adresse", 15, null, null));
		assertThrows(UncheckedIOException.class, () -> f.clients.removeClient(0));
		assertEquals("Fictif", f.client.getLastName());
		assertEquals(before, f.files());
	}

	@Test
	void secondFileFailureCompensatesSeriesCreationAndAssignmentsAndDeletion() throws Exception {
		var before = f.files();
		CsvStore.installHook = count -> {
			if (count == 1)
				throw new IOException("Deuxième installation refusée");
		};
		assertThrows(UncheckedIOException.class,
				() -> f.recurring.createWithPlanning(f.planning, f.series(null, f.future, f.future.plusWeeks(1))));
		assertTrue(f.planning.isEmpty());
		assertTrue(f.recurring.isEmpty());
		assertEquals(before, f.files());
		CsvStore.installHook = count -> {
		};
		RecurringIntervention series = f.series(null, f.future, f.future.plusWeeks(1));
		assertTrue(f.recurring.createWithPlanning(f.planning, series));
		var occurrences = new ArrayList<>(f.planning.getInterventions());
		before = f.files();
		CsvStore.installHook = count -> {
			if (count == 1)
				throw new IOException("Deuxième installation refusée");
		};
		assertThrows(UncheckedIOException.class, () -> f.recurring.assignFutureSeries(f.planning, series.getId(), f.b));
		occurrences.forEach(i -> assertNull(i.getEmployee()));
		assertNull(series.getEmployee());
		assertEquals(before, f.files());
		assertThrows(UncheckedIOException.class, () -> f.recurring.removeFutureSeries(f.planning, series.getId()));
		assertEquals(occurrences, f.planning.getInterventions());
		assertSame(series, f.recurring.getRecurringInterventions().getFirst());
		assertEquals(before, f.files());
		f.reload();
	}

	@Test
	void replacementAndThreeFileAbsenceOperationsCompensateDiskAndMemory() throws Exception {
		Intervention i = f.add(f.a, f.future, 8, 10);
		var before = f.files();
		CsvStore.installHook = count -> {
			if (count == 1)
				throw new IOException("panne fictive");
		};
		assertThrows(UncheckedIOException.class,
				() -> f.replacements.createWithPlanning(f.planning, f.a, f.b,
						gestion.util.FormatUtils.formatDate(f.future), "08h00",
						gestion.util.FormatUtils.formatDate(f.future), "10h00"));
		assertSame(f.a, i.getEmployee());
		assertTrue(f.replacements.isEmpty());
		assertEquals(before, f.files());

		for (int failAt = 1; failAt <= 3; failAt++) {
			int index = failAt;
			CsvStore.installHook = count -> {
				if (count == index)
					throw new IOException("panne fictive");
			};
			assertThrows(IOException.class,
					() -> f.absences.addAbsence(new EmployeeAbsence(f.a, EmployeeStatus.VACATION, f.future, f.future)));
			assertSame(f.a, i.getEmployee());
			assertNull(i.getSourceAbsenceId());
			assertTrue(f.absences.getAbsences().isEmpty());
			assertTrue(f.replacements.isEmpty());
			assertEquals(before, f.files());
		}

		CsvStore.installHook = count -> {
		};
		f.reload();
	}

	private int crash(String action) throws Exception {

		String testClasses = Path
				.of(TransactionCrashFixture.class.getProtectionDomain().getCodeSource().getLocation().toURI())
				.toString();
		String productionClasses = Path.of(CsvStore.class.getProtectionDomain().getCodeSource().getLocation().toURI())
				.toString();
		String cp = testClasses + File.pathSeparator + productionClasses;
		Path output = directory.resolve("processus-test.txt");
		Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java.exe").toString(),
				"-cp", cp, TransactionCrashFixture.class.getName(), action, directory.toString())
				.redirectErrorStream(true).redirectOutput(output.toFile()).start();

		assertTrue(process.waitFor(20, TimeUnit.SECONDS), "Le processus de test doit terminer.");

		int result = process.exitValue();
		int expected = action.equals("crash") ? 23 : 0;

		if (result != expected) {
			fail("Échec du processus enfant (code " + result + ") :\n" + Files.readString(output));
		}

		return result;
	}

	@Test
	void realProcessInterruptionIsRecoveredBeforeLoadingAndRecoveryIsIdempotent() throws Exception {
		var before = f.files();
		assertEquals(23, crash("crash"));
		assertTrue(Files.isDirectory(directory.resolve(".gestion-transaction")));
		CsvStore.recover(directory);
		assertEquals(before, f.files());
		CsvStore.recover(directory);
		assertEquals(before, f.files());
		f.reload();
	}

	@Test
	void recoveryRefusesExternalChangesAndRetainsTheJournal() throws Exception {
		assertEquals(23, crash("crash"));
		Path path = directory.resolve("clients.csv");
		Files.writeString(path, "Modification externe fictive\n");
		assertThrows(IOException.class, () -> CsvStore.recover(directory));
		assertEquals("Modification externe fictive\n", Files.readString(path));
		assertTrue(Files.isDirectory(directory.resolve(".gestion-transaction")));
	}

	@Test
	void incompleteCompensationBlocksFurtherWritesAndCanBeRecoveredByNewProcess() throws Exception {
		var before = f.files();
		var clients = new ClientCsvRepository(directory).load();
		var employees = new EmployeeCsvRepository(directory).load();
		clients.getFirst().setAddress("Adresse fictive modifiée");
		employees.getFirst().setContractHours(20);
		boolean[] restoring = { false };
		CsvStore.installHook = count -> {
			restoring[0] = true;
			throw new IOException("installation refusée");
		};
		CsvStore.writeHook = path -> {
			if (restoring[0])
				throw new IOException("restauration refusée");
		};
		assertThrows(UncheckedIOException.class, () -> CsvStore.transaction(directory, () -> {
			new ClientCsvRepository(directory).save(clients);
			new EmployeeCsvRepository(directory).save(employees);
		}));

		assertTrue(Files.isDirectory(directory.resolve(".gestion-transaction")));
		assertThrows(UncheckedIOException.class, () -> new ClientCsvRepository(directory).save(f.clients.getClients()));
		assertEquals(0, crash("recover"));
		assertEquals(before, f.files());
	}
}
