package gestion.ui.intervention;

import java.awt.BorderLayout;
import java.io.UncheckedIOException;
import gestion.ui.ActionDialogs;
import java.awt.FlowLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import gestion.model.Client;
import gestion.model.Employee;
import gestion.model.Intervention;
import gestion.model.RecurringIntervention;
import gestion.service.ClientService;
import gestion.service.PersonnelService;
import gestion.service.PlanningService;
import gestion.service.RecurringInterventionService;
import gestion.ui.UIUtils;
import gestion.util.FormatUtils;

public class InterventionUI extends JPanel {
    private final InterventionCreationDialogs dialogs;

    private PlanningService planning;
    private RecurringInterventionService recurringInterventionList;

    private JPanel interventionPanel;

    private JButton previousButton;
    private JButton nextButton;
    private JTextField searchButton;
    private JButton addInterventionButton;
    private JButton addRecurringButton;
    private JButton removeInterventionButton;
    private JButton removeRecurringInterventionButton;
    private JComboBox<String> viewBox;
    private int currentPage = 0;
    private final ArrayList<Intervention> visibleRows = new ArrayList<>();
    private DefaultTableModel interventionTableModel;
    private JTable interventionTable;
    private JScrollPane interventionScrollPane;
    private String currentSearchText = "";
    private JLabel searchIcon;

    public InterventionUI(PlanningService planning, RecurringInterventionService recurringInterventionList,
            ClientService clients, PersonnelService personnel) {

        this.planning = planning;
        this.recurringInterventionList = recurringInterventionList;

        setLayout(new BorderLayout());
        createNavigationPanel();
        interventionPanel = new JPanel();
        add(interventionPanel, BorderLayout.CENTER);

        createInterventionTable();

        dialogs = new InterventionCreationDialogs(this, planning, personnel, clients, recurringInterventionList,
                this::refreshAfterCreation);
    }

    private void createNavigationPanel() {

        JPanel navigationPanel = new JPanel(new BorderLayout());

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel pagePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        addInterventionButton = new JButton("Ajouter une intervention");
        addRecurringButton = new JButton("Ajouter une intervention récurrente");
        removeInterventionButton = new JButton("Supprimer une intervention");
        removeRecurringInterventionButton = new JButton("Supprimer une intervention récurrente");
        searchIcon = new JLabel("🔍");
        searchButton = new JTextField(20);
        previousButton = new JButton("<");
        nextButton = new JButton(">");

        String[] views = { "Trier par...", "Dates", "Clients", "Intervenants" };
        viewBox = new JComboBox<>(views);
        viewBox.setSelectedItem("Dates");

        actionsPanel.add(addInterventionButton);
        actionsPanel.add(addRecurringButton);
        actionsPanel.add(removeInterventionButton);
        actionsPanel.add(removeRecurringInterventionButton);
        actionsPanel.add(viewBox);

        searchPanel.add(searchIcon);
        searchPanel.add(searchButton);

        pagePanel.add(previousButton);
        pagePanel.add(nextButton);

        UIUtils.addTextChangeListener(searchButton, this::resetSearchIfEmpty);
        navigationPanel.add(actionsPanel, BorderLayout.WEST);
        navigationPanel.add(searchPanel, BorderLayout.CENTER);
        navigationPanel.add(pagePanel, BorderLayout.EAST);
        add(navigationPanel, BorderLayout.NORTH);

        previousButton.addActionListener(event -> previousPage());
        searchButton.addActionListener(event -> search());
        nextButton.addActionListener(event -> nextPage());
        viewBox.addActionListener(event -> view());
        addInterventionButton.addActionListener(event -> dialogs.openAddIntervention());
        addRecurringButton.addActionListener(event -> dialogs.openAddRecurringIntervention());
        removeInterventionButton.addActionListener(event -> removeIntervention());
        removeRecurringInterventionButton.addActionListener(event -> removeRecurringIntervention());

    }

    private void createInterventionTable() {

        String[] columns = { "Client", "Jour(s)", "Horaires", "Intervenant", "Type" };

        interventionTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        interventionTable = new JTable(interventionTableModel);

        interventionScrollPane = new JScrollPane(interventionTable);
        interventionScrollPane.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override public void componentResized(java.awt.event.ComponentEvent event) {
                Intervention selected = getSelectedIntervention();
                if (selected != null) {
                    int position = buildDisplayedInterventions().indexOf(selected);
                    if (position >= 0) currentPage = position / getInterventionsPerPage();
                }
                refreshIntervention();
                int row = visibleRows.indexOf(selected);
                if (row >= 0) interventionTable.setRowSelectionInterval(row, row);
            }
        });

        interventionPanel.setLayout(new BorderLayout());
        interventionPanel.add(interventionScrollPane, BorderLayout.CENTER);
    }

    private void search() {

        currentSearchText = searchButton.getText().trim().toLowerCase();
        currentPage = 0;

        ArrayList<Intervention> results = buildDisplayedInterventions();

        if (!currentSearchText.isBlank() && results.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Aucune correspondance.");
        }

        refreshIntervention(results);
    }

    private ArrayList<Intervention> buildDisplayedInterventions() {

        ArrayList<Intervention> displayedInterventions = new ArrayList<>(planning.getInterventions());

        if (!currentSearchText.isBlank()) {
            boolean searchApplied = false;

            try {

                LocalDate searchedDate = FormatUtils.parseDate(currentSearchText);

                displayedInterventions.removeIf(intervention -> searchedDate.isBefore(intervention.getStartDate())
                        || searchedDate.isAfter(intervention.getEndDate()));

                searchApplied = true;

            } catch (DateTimeParseException e) {
            }

            if (!searchApplied) {

                try {

                    LocalTime searchedTime = FormatUtils.parseTime(currentSearchText);

                    displayedInterventions.removeIf(intervention -> !intervention.getStartTime().equals(searchedTime));
                    searchApplied = true;

                } catch (DateTimeParseException e) {

                }
                if (!searchApplied) {
                    displayedInterventions.removeIf(intervention -> !matchesName(intervention, currentSearchText));
                }
            }
        }

        String selectedView = (String) viewBox.getSelectedItem();

        switch (selectedView) {

        case "Dates":
            displayedInterventions
                    .sort(Comparator.comparing(Intervention::getStartDate).thenComparing(Intervention::getStartTime));
            break;
        case "Clients":
            displayedInterventions
                    .sort(Comparator.comparing((Intervention intervention) -> intervention.getClient().getLastName())
                            .thenComparing(intervention -> intervention.getClient().getFirstName()));
            break;
        case "Intervenants":
            displayedInterventions.sort(Comparator.comparing(Intervention::getEmployee, Comparator
                    .nullsLast(Comparator.comparing(Employee::getLastName).thenComparing(Employee::getFirstName))));
            break;
        default:
            break;
        }

        return displayedInterventions;
    }

    private void refreshIntervention(ArrayList<Intervention> displayedInterventions) {

        interventionTableModel.setRowCount(0);
        visibleRows.clear();
        int interventionsPerPage = getInterventionsPerPage();
        currentPage = Math.min(currentPage, Math.max(0, (displayedInterventions.size() - 1) / interventionsPerPage));
        int startIndex = currentPage * interventionsPerPage;
        int endIndex = startIndex + interventionsPerPage;

        endIndex = Math.min(endIndex, displayedInterventions.size());

        for (int i = startIndex; i < endIndex; i++) {

            Intervention intervention = displayedInterventions.get(i);
            visibleRows.add(intervention);

            String clientFirstName = intervention.getClient().getFirstName();
            String clientLastName = intervention.getClient().getLastName();
            String clientFullName = clientLastName + " " + clientFirstName;

            String employeeFullName = "Non attribué";

            if (intervention.getEmployee() != null) {

                String employeeFirstName = intervention.getEmployee().getFirstName();
                String employeeLastName = intervention.getEmployee().getLastName();
                employeeFullName = employeeLastName + " " + employeeFirstName;
            }

            DayOfWeek day = intervention.getStartDate().getDayOfWeek();
            LocalTime startHour = intervention.getStartTime();
            LocalTime endHour = intervention.getEndTime();

            String dayOfIntervention = FormatUtils.formatDay(day) + " "
                    + FormatUtils.formatDate(intervention.getStartDate());
            String hours = FormatUtils.formatTime(startHour) + " / " + FormatUtils.formatTime(endHour);
            String type = intervention.getRecurringId();

            if (type == null || type.isBlank()) {
                type = "Unique";

            } else {
                type = "Récurrent";
            }

            interventionTableModel
                    .addRow(new Object[] { clientFullName, dayOfIntervention, hours, employeeFullName, type });
        }

        updatePageButtons(displayedInterventions);
        interventionPanel.revalidate();
        interventionPanel.repaint();
    }

    private boolean matchesName(Intervention intervention, String searchText) {

        Client client = intervention.getClient();
        Employee employee = intervention.getEmployee();

        return (client != null && matchesPerson(client.getFirstName(), client.getLastName(), searchText))
                || (employee != null && matchesPerson(employee.getFirstName(), employee.getLastName(), searchText));
    }

    private boolean matchesPerson(String firstName, String lastName, String searchText) {

        firstName = firstName.toLowerCase();
        lastName = lastName.toLowerCase();
        searchText = searchText.toLowerCase();

        String fullName = firstName + " " + lastName;
        String fullNameReverse = lastName + " " + firstName;

        return firstName.contains(searchText) || lastName.contains(searchText) || fullName.contains(searchText)
                || fullNameReverse.contains(searchText);
    }

    private void removeIntervention() {

        Intervention interventionToRemove = getSelectedIntervention();

        if (interventionToRemove == null) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une intervention.");
            return;
        }

        try { planning.removeIntervention(interventionToRemove); }
        catch (UncheckedIOException | IllegalArgumentException e) { ActionDialogs.error(this, e); return; }

        adjustCurrentPage();
        refreshIntervention();
    }

    public void removeRecurringIntervention() {

        Intervention interventionToRemove = getSelectedIntervention();

        if (interventionToRemove == null) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une intervention récurrente.");
            return;
        }

        String recurringIdToRemove = interventionToRemove.getRecurringId();

        if (recurringIdToRemove == null || recurringIdToRemove.isBlank()) {
            JOptionPane.showMessageDialog(this, "Veuillez sélectionner une intervention récurrente.");
            return;
        }

        if (!ActionDialogs.confirm(this, "Supprimer les occurrences à venir de cette série ? Les interventions déjà commencées et leur historique seront conservés.")) return;
        try { recurringInterventionList.removeFutureSeries(planning, recurringIdToRemove); }
        catch (UncheckedIOException | IllegalArgumentException e) { ActionDialogs.error(this, e); return; }

        adjustCurrentPage();
        refreshIntervention();
    }

    private void adjustCurrentPage() {

        ArrayList<Intervention> displayedInterventions = buildDisplayedInterventions();

        if (displayedInterventions.isEmpty()) {
            currentPage = 0;
            return;
        }

        int interventionsPerPage = getInterventionsPerPage();
        int endPage = (displayedInterventions.size() - 1) / interventionsPerPage;

        if (currentPage > endPage) {
            currentPage = endPage;
        }
    }

    private Intervention getSelectedIntervention() {
        int row = interventionTable.getSelectedRow();
        if (row < 0) return null;
        row = interventionTable.convertRowIndexToModel(row);
        return row < visibleRows.size() ? visibleRows.get(row) : null;
    }

    private int getInterventionsPerPage() {

        int size = interventionScrollPane.getViewport().getExtentSize().height;
        int rowSize = interventionTable.getRowHeight();
        int interventionsVisiblePerPage = 0;

        if (rowSize > 0) {
            interventionsVisiblePerPage = size / rowSize;
        }

        if (interventionsVisiblePerPage > 0) {
            return interventionsVisiblePerPage;
        } else {
            return 1;
        }
    }

    private void resetSearchIfEmpty() {

        if (searchButton.getText().isBlank()) {
            currentSearchText = "";
            currentPage = 0;
            refreshIntervention();
        }
    }

    private void previousPage() {

        if (currentPage > 0) {
            currentPage--;
        }

        refreshIntervention();
    }

    private void nextPage() {

        ArrayList<Intervention> displayedInterventions = buildDisplayedInterventions();
        int interventionsPerPage = getInterventionsPerPage();
        int endPage = (displayedInterventions.size() - 1) / interventionsPerPage;

        if (currentPage < endPage) {
            currentPage++;
        }

        refreshIntervention();
    }

    private void updatePageButtons(ArrayList<Intervention> displayedInterventions) {

        int interventionsPerPage = getInterventionsPerPage();
        int endPage = (displayedInterventions.size() - 1) / interventionsPerPage;

        previousButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage < endPage);
    }

    private void view() {

        currentPage = 0;
        refreshIntervention();
    }

    private void refreshAfterCreation() {
        currentPage = 0;
        refreshIntervention();
    }

    public void refreshIntervention() {
        refreshIntervention(buildDisplayedInterventions());
    }

    public void addRecurringIntervention(RecurringIntervention recurringIntervention) {
        recurringInterventionList.addRecurringIntervention(recurringIntervention);
    }

    public void removeRecurringIntervention(int index) {
        recurringInterventionList.removeRecurringIntervention(index);
    }

}
