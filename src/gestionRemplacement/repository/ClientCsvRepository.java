package gestionRemplacement.repository;

import gestionRemplacement.model.Client;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;


public final class ClientCsvRepository {
    private final Path path;

    public ClientCsvRepository(Path directory) {
        path = directory.resolve("clients.csv");
    }

    public ArrayList<Client> load() {
        ArrayList<Client> clients = new ArrayList<>();

        File file = path.toFile();

        if (!file.exists()) {
            return clients;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;

            while ((line = reader.readLine()) != null) {
                String[] data = line.split(";", -1);

                if (data.length == 6) {

                    String firstName = data[0];
                    String lastName = data[1];
                    String address = data[2];
                    double totalHoursContract = Double.parseDouble(data[3]);
                    LocalDate contractStartDay = null;
                    LocalDate contractEndDay = null;

                    if (!data[4].isBlank()) {
                        contractStartDay = LocalDate.parse(data[4]);
                    }

                    if (!data[5].isBlank()) {
                        contractEndDay = LocalDate.parse(data[5]);
                    }

                    clients.add(new Client(firstName, lastName, address, totalHoursContract, contractStartDay,
                            contractEndDay));

                } else if (data.length == 2) {

                    String firstName = data[0];
                    String lastName = data[1];

                    clients.add(new Client(firstName, lastName, "N/A", 0, null, null));
                }
            }

        } catch (IOException | NumberFormatException | DateTimeParseException e) {
            System.out.println("Erreur au chargement du fichier clients.");
            e.printStackTrace();
        }
        return clients;
    }

    public void save(List<Client> clients) {

        try (FileWriter writer = new FileWriter(path.toFile())) {

            for (Client client : clients) {

                String startDate = "";
                String endDate = "";

                if (client.getContractStartDate() != null) {
                    startDate = client.getContractStartDate().toString();
                }

                if (client.getContractEndDate() != null) {
                    endDate = client.getContractEndDate().toString();
                }

                writer.write(client.getFirstName() + ";" + client.getLastName() + ";" + client.getAddress() + ";"
                        + client.getTotalHoursContract() + ";" + startDate + ";" + endDate + System.lineSeparator());
            }

        } catch (IOException e) {
            System.out.println("Erreur à la sauvegarde du fichier clients.");
            e.printStackTrace();
        }
    }
}
