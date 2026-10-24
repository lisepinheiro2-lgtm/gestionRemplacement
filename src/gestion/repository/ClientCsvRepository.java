package gestion.repository;

import java.nio.file.Path;
import java.time.*;
import java.util.*;

import gestion.model.*;
import gestion.util.DomainValidation;

public final class ClientCsvRepository {

	private final Path path;

	public ClientCsvRepository(Path directory) {
		path = CsvStore.directory(directory).resolve("clients.csv");
	}

	public Path getDirectory() {
		return path.getParent();
	}

	public ArrayList<Client> load() {

		Set<UUID> clientIds = new HashSet<>();

		return CsvFormat.readUnchecked(path, cells -> {
			CsvFormat.columns(cells, 7);

			UUID clientUuid = DomainValidation.uuid(cells[6]);

			Client client = new Client(cells[0], cells[1], cells[2], Double.parseDouble(cells[3]),
					cells[4].isBlank() ? null : LocalDate.parse(cells[4]),
					cells[5].isBlank() ? null : LocalDate.parse(cells[5]), clientUuid);

			if (!clientIds.add(client.getClientID())) {
				throw new IllegalArgumentException("UUID client en double : " + client.getClientID());
			}

			DomainValidation.client(client);

			return client;
		});
	}

	public void save(List<Client> clients) {
		CsvIdentityMigration.requireCurrentFormat(getDirectory());

		StringBuilder content = new StringBuilder();
		Set<UUID> savedIds = new HashSet<>();

		for (Client client : clients) {
			DomainValidation.client(client);
			if (!savedIds.add(client.getClientID()))
				throw new IllegalArgumentException("UUID client dupliqué : " + client.getClientID());
			content.append(CsvFormat.row(client.getFirstName(), client.getLastName(), client.getAddress(),
					client.getTotalHoursContract(), client.getContractStartDate(), client.getContractEndDate(),
					client.getClientID().toString()));
		}

		CsvStore.write(path, content.toString());
	}
}
