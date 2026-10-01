package gestionRemplacement;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Rectangle;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MainUI {

	private PersonnelList personnel;
	private Planning planning;
	private ReplacementList replacementList;
	private ClientList clients;
	private RecurringInterventionList recurringInterventionList;

	private JFrame frame;
	private CardLayout cardLayout;
	private PlanningUI planningPanel;

	private JPanel mainPanel;
	private JButton personnelManagement;
	private JButton viewPlanning;
	private JButton replacements;
	private JButton homeButton;
	private JButton clientManagement;
	private JButton interventions;

	private JPanel homePanel;
	private JPanel personnelPanel;
	private JPanel replacementPanel;
	private JPanel clientPanel;
	private InterventionUI interventionPanel;

	public MainUI(PersonnelList personnel, ClientList clients, Planning planning, ReplacementList replacementList,
			RecurringInterventionList recurringInterventionList) {

		this.personnel = personnel;
		this.clients = clients;
		this.planning = planning;
		this.replacementList = replacementList;
		this.recurringInterventionList = recurringInterventionList;

		createWindow();
	}

	private void createWindow() {

		frame = new JFrame("Gestion des remplacements");
		configureFrameSize();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		cardLayout = new CardLayout();
		mainPanel = new JPanel(cardLayout);
		homePanel = createHomePanel();
		personnelPanel = new PersonnelUI(personnel);
		planningPanel = new PlanningUI(planning, personnel, clients, recurringInterventionList);
		replacementPanel = new ReplacementUI(replacementList, personnel, planning);
		clientPanel = new ClientUI(clients);
		interventionPanel = new InterventionUI(planning, recurringInterventionList, clients, personnel);

		mainPanel.add(homePanel, "HOME");
		mainPanel.add(personnelPanel, "PERSONNEL");
		mainPanel.add(planningPanel, "PLANNING");
		mainPanel.add(replacementPanel, "REPLACEMENTS");
		mainPanel.add(clientPanel, "CLIENTS");
		mainPanel.add(interventionPanel, "INTERVENTIONS");

		frame.setLayout(new BorderLayout());

		homeButton = new JButton("Accueil");
		homeButton.setPreferredSize(new Dimension(120, 40));
		homeButton.addActionListener(event -> homeUI());

		homeButton.setVisible(false);

		JPanel navigationPanel = new JPanel(new BorderLayout());

		navigationPanel.add(homeButton, BorderLayout.CENTER);
		navigationPanel.add(UIUtils.createSettingsButton(frame), BorderLayout.EAST);
		frame.add(navigationPanel, BorderLayout.NORTH);
		frame.add(mainPanel, BorderLayout.CENTER);
		
		UIUtils.applyTheme(frame);
		frame.setVisible(true);
	}

	private JPanel createHomePanel() {

		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(3, 2));

		personnelManagement = new JButton("Management employees");
		viewPlanning = new JButton("Planning");
		replacements = new JButton("Remplacements");
		clientManagement = new JButton("Management clients");
		interventions = new JButton("Gestion interventions");

		panel.add(personnelManagement);
		panel.add(viewPlanning);
		panel.add(replacements);
		panel.add(clientManagement);
		panel.add(interventions);

		personnelManagement.addActionListener(event -> personnelUI());
		viewPlanning.addActionListener(event -> planningUI());
		replacements.addActionListener(event -> replacementUI());
		clientManagement.addActionListener(event -> clientUI());
		interventions.addActionListener(event -> interventionUI());

		return panel;
	}

	private void configureFrameSize() {

		Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

		int minWidth = (int) (screenBounds.width * 0.52);
		int minHeight = (int) (screenBounds.height * 0.4);

		frame.setMinimumSize(new Dimension(minWidth, minHeight));
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
	}

	private void interventionUI() {
		cardLayout.show(mainPanel, "INTERVENTIONS");
		homeButton.setVisible(true);

		SwingUtilities.invokeLater(() -> interventionPanel.refreshIntervention());
	}

	private void clientUI() {
		cardLayout.show(mainPanel, "CLIENTS");
		homeButton.setVisible(true);
	}

	private void homeUI() {
		cardLayout.show(mainPanel, "HOME");
		homeButton.setVisible(false);
	}

	private void personnelUI() {
		cardLayout.show(mainPanel, "PERSONNEL");
		homeButton.setVisible(true);
	}

	private void planningUI() {
		planningPanel.refreshPlanning();
		cardLayout.show(mainPanel, "PLANNING");
		homeButton.setVisible(true);
	}

	private void replacementUI() {
		cardLayout.show(mainPanel, "REPLACEMENTS");
		homeButton.setVisible(true);
	}

	public ClientList getClients() {
		return clients;
	}

	public void setClients(ClientList clients) {
		this.clients = clients;
	}
}
