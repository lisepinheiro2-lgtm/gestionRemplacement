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
	private ClientUI clientPanel;
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
		frame = new JFrame("Gestion +");

		ImageIcon appIcon = new ImageIcon(MainUI.class.getResource("/icons/gestion-64.png"));
		frame.setIconImage(appIcon.getImage());
		configureFrameSize();
		frame.setMinimumSize(new Dimension(760, 650));
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		cardLayout = new CardLayout();
		mainPanel = new JPanel(cardLayout);
		homePanel = createHomePanel();

		mainPanel.add(homePanel, "HOME");

		frame.setLayout(new BorderLayout());

		homeButton = new JButton("Accueil");
		homeButton.setPreferredSize(new Dimension(120, 40));
		homeButton.addActionListener(event -> homeUI());

		homeButton.setVisible(true);

		navigationHeader = new ApplicationHeader(frame, homeButton, appIcon.getImage());
		navigationHeader.configureNavigation(this::personnelUI, this::clientUI, this::planningUI, this::replacementUI);
		homeButton.setVisible(true);
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
		if (interventionPanel == null) {
			interventionPanel = new InterventionUI(planning, recurringInterventionList, clients, personnel);
			addPage(interventionPanel, "INTERVENTIONS");
		}

		showPage("INTERVENTIONS", "Interventions");
		SwingUtilities.invokeLater(interventionPanel::refreshIntervention);
	}

	private void clientUI() {
		if (clientPanel == null) {
			clientPanel = new ClientUI(clients);
			addPage(clientPanel, "CLIENTS");
		} else {
			clientPanel.refreshTable();
		}
		showPage("CLIENTS", "Clients");
	}

	private void homeUI() {
		cardLayout.show(mainPanel, "HOME");
		navigationHeader.setPageTitle("Accueil");
		homeButton.setVisible(true);
	}

	private void personnelUI() {
		if (personnelPanel == null) {
			personnelPanel = new PersonnelPanel(
					new PersonnelController(personnel, absenceList, new WorkHoursService(planning)), planning,
					recurringInterventionList);
			addPage(personnelPanel, "PERSONNEL");
		} else {
			personnelPanel.refreshTable();
		}

		showPage("PERSONNEL", "Personnel");
	}

	private void planningUI() {
		if (planningPanel == null) {
			planningPanel = new PlanningUI(planning, personnel, clients, recurringInterventionList);
			addPage(planningPanel, "PLANNING");
		}

		showPage("PLANNING", "Planning");
		planningPanel.refreshPlanning();
	}

	private void replacementUI() {
		if (replacementPanel == null) {
			replacementPanel = new ReplacementUI(replacementList, personnel, planning);
			addPage(replacementPanel, "REPLACEMENTS");
		} else {
			replacementPanel.refreshTable();
		}
		showPage("REPLACEMENTS", "Remplacements");
	}

	private void addPage(JPanel panel, String name) {
		UIUtils.applyTheme(panel);
		mainPanel.add(panel, name);
	}

	private void showPage(String name, String title) {
		cardLayout.show(mainPanel, name);
		navigationHeader.setPageTitle(title);
		homeButton.setVisible(true);
	}

	public ClientService getClients() {
		return clients;
	}

	public void setClients(ClientService clients) {
		this.clients = clients;
	}
}
