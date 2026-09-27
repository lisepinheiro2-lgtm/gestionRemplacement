package gestionRemplacement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;

public class ClientList {

	private ArrayList<Client> clients = new ArrayList<>();

	private final String fileName = "clients.csv";

	public ClientList() {

		loadClients();
		sortClients();
	}

	private void saveClients() {

		try {

			FileWriter writer = new FileWriter(fileName);

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

			writer.close();

		} catch (IOException e) {
			System.out.println("Erreur à la sauvegarde du fichier clients.");
			e.printStackTrace();
		}
	}

	public void addClient(String firstName, String lastName, String address, double totalHoursContract,
			LocalDate contractStartDate, LocalDate contractEndDate) {

		clients.add(new Client(firstName, lastName, address, totalHoursContract, contractStartDate, contractEndDate));

		sortClients();
		saveClients();
	}

	public void editClients(int index, String firstName, String lastName, String address, double totalHoursContract,
			LocalDate contractStartDate, LocalDate contractEndDate) {

		Client client = clients.get(index);

		client.setFirstName(firstName);
		client.setLastName(lastName);
		client.setAddress(address);
		client.setTotalHoursContract(totalHoursContract);
		client.setContractStartDate(contractStartDate);
		client.setContractEndDate(contractEndDate);

		sortClients();
		saveClients();
	}

	public Client findClient(String firstName, String lastName) {

		for (Client client : clients) {

			if (client.getFirstName().equals(firstName) && client.getLastName().equals(lastName)) {
				return client;
			}
		}

		return null;
	}

	private void loadClients() {

		File file = new File(fileName);

		if (!file.exists()) {
			return;
		}

		try {

			BufferedReader reader = new BufferedReader(new FileReader(file));
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

			reader.close();

		} catch (IOException | NumberFormatException | DateTimeParseException e) {
		    System.out.println("Erreur au chargement du fichier clients.");
		    e.printStackTrace();
		}
	}

	public void sortClients() {

		clients.sort(Comparator.comparing(Client::getLastName, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(Client::getFirstName, String.CASE_INSENSITIVE_ORDER));
	}

	public void addClient(Client client) {
	    clients.add(client);
	    sortClients();
	    saveClients();
	}

	public void removeClient(int index) {
		clients.remove(index);
		saveClients();
	}

	public ArrayList<Client> getClients() {
		return clients;
	}

	public boolean isEmpty() {
		return clients.isEmpty();
	}
}