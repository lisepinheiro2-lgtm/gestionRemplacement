package gestion.service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import gestion.model.Client;
import gestion.repository.ClientCsvRepository;
import gestion.repository.CsvReferences;
import gestion.repository.CsvStore;
import gestion.util.DomainValidation;

public class ClientService {

	private ArrayList<Client> clients = new ArrayList<>();
	private final ClientCsvRepository repository;
	private final List<Predicate<Client>> referenceChecks = new ArrayList<>();

	public Path getDirectory() {
		return repository.getDirectory();
	}

	public void protectReferences(Predicate<Client> check) {
		referenceChecks.add(check);
	}

	public void requireKnown(Client person) {
		if (person == null || !clients.contains(person))
			throw new IllegalArgumentException("Veuillez choisir une personne de la liste actuelle.");
	}

	private boolean referenced(Client person) {
		return referenceChecks.stream().anyMatch(check -> check.test(person));
	}

	private void validateIdentity(Client candidate, Client original) {

		DomainValidation.client(candidate);

		for (Client person : clients)

			if (person != original && person.getFirstName().equals(candidate.getFirstName())
					&& person.getLastName().equals(candidate.getLastName()))
				throw new IllegalArgumentException(
						"Ce prénom et ce nom existent déjà ; les homonymes ne sont pas encore pris en charge.");

	}

	private void changeList(Consumer<ArrayList<Client>> mutation) {
		ArrayList<Client> previous = new ArrayList<>(clients);
		ServiceChanges.apply(getDirectory(), () -> mutation.accept(clients), this::saveClients, () -> {
			clients.clear();
			clients.addAll(previous);
		});
	}

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
		addClient(new Client(firstName, lastName, address, totalHoursContract, contractStartDate, contractEndDate));
	}

	public void editClients(int index, String firstName, String lastName, String address, double totalHoursContract,
			LocalDate contractStartDate, LocalDate contractEndDate) {

		if (index < 0 || index >= clients.size())
			throw new IllegalArgumentException("Veuillez sélectionner un client.");
		Client client = clients.get(index);
		Client candidate = new Client(firstName, lastName, address, totalHoursContract, contractStartDate,
				contractEndDate, client.getClientID());
		validateIdentity(candidate, client);
		Client previous = new Client(client.getFirstName(), client.getLastName(), client.getAddress(),
				client.getTotalHoursContract(), client.getContractStartDate(), client.getContractEndDate(), client.getClientID());
		ArrayList<Client> order = new ArrayList<>(clients);
		ServiceChanges.apply(getDirectory(), () -> {
			copyValues(candidate, client);
			sortClients();
		}, this::saveClients, () -> {
			copyValues(previous, client);
			clients.clear();
			clients.addAll(order);
		});
	}

	private static void copyValues(Client source, Client target) {
		target.setFirstName(source.getFirstName());
		target.setLastName(source.getLastName());
		target.setAddress(source.getAddress());
		target.setTotalHoursContract(source.getTotalHoursContract());
		target.setContractStartDate(source.getContractStartDate());
		target.setContractEndDate(source.getContractEndDate());
	}

	public Client findClient(String firstName, String lastName) {
		return CsvReferences.findClient(clients, firstName, lastName);
	}

	public void sortClients() {
		clients.sort(Comparator.comparing(Client::getLastName, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(Client::getFirstName, String.CASE_INSENSITIVE_ORDER));
	}

	public void addClient(Client client) {

		validateIdentity(client, null);
		changeList(list -> {
			list.add(client);
			sortClients();
		});
	}

	public void removeClient(int index) {

		if (index < 0 || index >= clients.size())
			throw new IllegalArgumentException("Veuillez sélectionner un client.");

		if (referenced(clients.get(index)))
			throw new IllegalArgumentException(
					"Suppression refusée : ce client est encore référencé par une intervention ou une série.");

		changeList(list -> list.remove(index));
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
