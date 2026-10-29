package gestion.repository;

import java.nio.file.Path;

public final class TransactionCrashFixture {

	public static void main(String[] args) throws Exception {
		Path directory = Path.of(args[1]);

		if (args[0].equals("recover")) {
			CsvStore.recover(directory);
			return;
		}

		CsvStore.installHook = count -> {
			if (count == 1)
				Runtime.getRuntime().halt(23);
		};

		var clients = new ClientCsvRepository(directory).load();
		var employees = new EmployeeCsvRepository(directory).load();
		clients.getFirst().setAddress("Adresse fictive après interruption");
		employees.getFirst().setContractHours(20);
		CsvStore.transaction(directory, () -> {
			new ClientCsvRepository(directory).save(clients);
			new EmployeeCsvRepository(directory).save(employees);
		});
	}
}
