package gestion.service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;

import gestion.model.Client;
import gestion.repository.ClientCsvRepository;

public class ClientService {

	private ArrayList<Client> clients = new ArrayList<>();

	private final ClientCsvRepository repository;

	public ClientService() {
		this(new ClientCsvRepository(Path.of(".")));
	}

	public ClientService(ClientCsvRepository repository) {
		this.repository = repository;

		clients = repository.load();
		sortClients();
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

	private void saveClients() {
		repository.save(clients);
	}
}
