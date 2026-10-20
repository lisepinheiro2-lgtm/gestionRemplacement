package gestion.repository;

import java.nio.file.Path;
import java.time.*;
import java.util.*;
import gestion.model.*;
import gestion.util.DomainValidation;

public final class ClientCsvRepository {
    private final Path path;
    public ClientCsvRepository(Path directory) { path = CsvStore.directory(directory).resolve("clients.csv"); }
    public Path getDirectory() { return path.getParent(); }

    public ArrayList<Client> load() {
        Set<String> keys = new HashSet<>();
        return CsvFormat.readUnchecked(path, cells -> {
            CsvFormat.columns(cells, 2, 6);
            Client client = cells.length == 2 ? new Client(cells[0], cells[1], "N/A", 0, null, null)
                    : new Client(cells[0], cells[1], cells[2], Double.parseDouble(cells[3]),
                            cells[4].isBlank() ? null : LocalDate.parse(cells[4]),
                            cells[5].isBlank() ? null : LocalDate.parse(cells[5]));
            DomainValidation.client(client);
            if (!keys.add(cells[0] + ";" + cells[1])) throw new IllegalArgumentException("Identité ambiguë.");
            return client;
        });
    }

    public void save(List<Client> clients) {
        StringBuilder content = new StringBuilder();
        Set<String> keys = new HashSet<>();
        for (Client client : clients) {
            DomainValidation.client(client);
            if (!keys.add(client.getFirstName() + ";" + client.getLastName()))
                throw new IllegalArgumentException("Homonymes non pris en charge par ce format CSV.");
            content.append(CsvFormat.row(client.getFirstName(), client.getLastName(), client.getAddress(),
                    client.getTotalHoursContract(), client.getContractStartDate(), client.getContractEndDate()));
        }
        CsvStore.write(path, content.toString());
    }
}
