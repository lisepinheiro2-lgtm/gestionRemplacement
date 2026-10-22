package gestion.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import gestion.controller.PersonnelController;
import gestion.service.AbsenceService;
import gestion.service.ClientService;
import gestion.service.PersonnelService;
import gestion.service.PlanningService;
import gestion.service.RecurringInterventionService;
import gestion.service.ReplacementService;
import gestion.service.WorkHoursService;
import gestion.ui.intervention.InterventionUI;
import gestion.ui.personnel.PersonnelPanel;
import gestion.ui.planning.PlanningUI;

public class MainUI {

	private ApplicationHeader navigationHeader;

	private PersonnelService personnel;
	private PlanningService planning;
	private ReplacementService replacementList;
	private ClientService clients;
	private RecurringInterventionService recurringInterventionList;

	private JFrame frame;
	private CardLayout cardLayout;
	private PlanningUI planningPanel;

	private JPanel mainPanel;
	private JButton homeButton;

	private JPanel homePanel;
	private PersonnelPanel personnelPanel;
	private ReplacementUI replacementPanel;
	private JPanel clientPanel;
	private InterventionUI interventionPanel;
	private final Preferences preferences = Preferences.userRoot().node("/gestionRemplacement");
	private Dimension normalWindowSize;
	private AbsenceService absenceList;

	public MainUI(PersonnelService personnel, ClientService clients, PlanningService planning,
			ReplacementService replacementList, RecurringInterventionService recurringInterventionList,
			AbsenceService absenceList) {

		this.personnel = personnel;
		this.clients = clients;
		this.planning = planning;
		this.replacementList = replacementList;
		this.recurringInterventionList = recurringInterventionList;
		this.absenceList = absenceList;

		createWindow();
	}

	private void createWindow() {

		UIUtils.installLookAndFeel();
		frame = new JFrame("Gestion");

		ImageIcon appIcon = new ImageIcon(MainUI.class.getResource("/icons/gestion-64.png"));
		frame.setIconImage(appIcon.getImage());
		configureFrameSize();
		frame.setMinimumSize(new Dimension(760, 650));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		cardLayout = new CardLayout();
		mainPanel = new JPanel(cardLayout);
		homePanel = createHomePanel();
		personnelPanel = new PersonnelPanel(
				new PersonnelController(personnel, absenceList, new WorkHoursService(planning)), planning);
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

		navigationHeader = new ApplicationHeader(frame, homeButton, appIcon.getImage());
		frame.add(navigationHeader, BorderLayout.NORTH);
		frame.add(mainPanel, BorderLayout.CENTER);

		UIUtils.applyTheme(frame);

		frame.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				if (frame.isShowing() && frame.getExtendedState() == JFrame.NORMAL) {
					normalWindowSize = frame.getSize();
				}
			}
		});

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				preferences.putInt("windowWidth", normalWindowSize.width);
				preferences.putInt("windowHeight", normalWindowSize.height);
				boolean maximized = (frame.getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;
				preferences.putBoolean("windowMaximized", maximized);
			}
		});

		frame.setVisible(true);
	}

	private JPanel createHomePanel() {
		return new HomePanel(this::personnelUI, this::clientUI, this::planningUI, this::replacementUI,
				this::interventionUI);
	}

	private void configureFrameSize() {
		Rectangle screenBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int width = preferences.getInt("windowWidth", (int) (screenBounds.width * 0.8));
		int height = preferences.getInt("windowHeight", (int) (screenBounds.height * 0.8));
		frame.setSize(width, height);
		normalWindowSize = frame.getSize();
		boolean maximized = preferences.getBoolean("windowMaximized", true);

		if (maximized) {
			frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		}
	}

	private void interventionUI() {
		cardLayout.show(mainPanel, "INTERVENTIONS");
		navigationHeader.setPageTitle("Interventions");
		homeButton.setVisible(true);

		SwingUtilities.invokeLater(() -> interventionPanel.refreshIntervention());
	}

	private void clientUI() {
		cardLayout.show(mainPanel, "CLIENTS");
		navigationHeader.setPageTitle("Clients");
		homeButton.setVisible(true);
	}

	private void homeUI() {
		cardLayout.show(mainPanel, "HOME");
		navigationHeader.setPageTitle("Accueil");
		homeButton.setVisible(false);
	}

	private void personnelUI() {
		cardLayout.show(mainPanel, "PERSONNEL");
		navigationHeader.setPageTitle("Personnel");
		personnelPanel.refreshTable();
		homeButton.setVisible(true);
	}

	private void planningUI() {
		planningPanel.refreshPlanning();
		cardLayout.show(mainPanel, "PLANNING");
		navigationHeader.setPageTitle("Planning");
		homeButton.setVisible(true);
	}

	private void replacementUI() {
		replacementPanel.refreshTable();
		cardLayout.show(mainPanel, "REPLACEMENTS");
		navigationHeader.setPageTitle("Remplacements");
		homeButton.setVisible(true);
	}

	public ClientService getClients() {
		return clients;
	}

	public void setClients(ClientService clients) {
		this.clients = clients;
	}
}
