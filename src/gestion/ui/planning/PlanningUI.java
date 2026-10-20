package gestion.ui.planning;

import java.awt.BorderLayout;
import java.io.UncheckedIOException;
import gestion.ui.ActionDialogs;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import gestion.model.Intervention;
import gestion.service.ClientService;
import gestion.service.PersonnelService;
import gestion.service.PlanningService;
import gestion.service.RecurringInterventionService;
import gestion.ui.UIUtils;

public class PlanningUI extends JPanel {
    private final PlanningInterventionDialogs dialogs;
    private final EmployeeAssignmentDialog assignmentDialog;

    private PlanningService planning;

    private ClientService clients;

    private LocalDate currentDate;

    private JPanel planningPanel;

    private JButton previousButton;
    private JButton todayButton;
    private JButton nextButton;

    private JComboBox<String> viewBox;

    public PlanningUI(PlanningService planning, PersonnelService personnel, ClientService clients,
            RecurringInterventionService recurringInterventionList) {

        this.planning = planning;

        this.clients = clients;

        currentDate = LocalDate.now();
        setLayout(new BorderLayout());
        createNavigationPanel();
        planningPanel = new JPanel();
        add(planningPanel, BorderLayout.CENTER);

        assignmentDialog = new EmployeeAssignmentDialog(this, planning, personnel, recurringInterventionList,
                this::refreshPlanning);
        dialogs = new PlanningInterventionDialogs(this, planning, personnel, clients, recurringInterventionList,
                this::refreshPlanning);
    }

    private void createNavigationPanel() {

        JPanel navigationPanel = new JPanel(new BorderLayout());
        JPanel periodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel viewPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        previousButton = new JButton("<");
        todayButton = new JButton("Aujourd'hui");
        nextButton = new JButton(">");

        String[] views = { "Mois", "Semaine" };
        viewBox = new JComboBox<>(views);

        periodPanel.add(previousButton);
        periodPanel.add(todayButton);
        periodPanel.add(nextButton);
        viewPanel.add(viewBox);

        navigationPanel.add(periodPanel, BorderLayout.WEST);
        navigationPanel.add(viewPanel, BorderLayout.CENTER);
        navigationPanel.add(actionPanel, BorderLayout.EAST);
        add(navigationPanel, BorderLayout.NORTH);

        previousButton.addActionListener(event -> previousPeriod());
        todayButton.addActionListener(event -> today());
        nextButton.addActionListener(event -> nextPeriod());
        viewBox.addActionListener(event -> refreshPlanning());
    }

    private void openInterventionActions(Intervention intervention) {

        String[] options = { "Attribuer / Réattribuer", "Modifier", "Supprimer" };
        int choice = JOptionPane.showOptionDialog(this, "Que voulez-vous faire ?", "Choix de l'action",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        switch (choice) {
        case 0:
            assignmentDialog.open(intervention);
            break;

        case 1:
            dialogs.openEditIntervention(intervention);
            break;

        case 2:
            int confirmation = JOptionPane.showConfirmDialog(this, "Supprimer cette intervention ?",
                    "Confirmer la suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirmation == JOptionPane.YES_OPTION) {
                try { planning.removeIntervention(intervention); refreshPlanning(); }
                catch (IllegalArgumentException | UncheckedIOException e) { ActionDialogs.error(this, e); }
            }
            break;

        default:
            break;
        }
    }

    private void previousPeriod() {

        if (viewBox.getSelectedItem().equals("Semaine")) {
            currentDate = currentDate.minusWeeks(1);
        } else {
            currentDate = currentDate.minusMonths(1);
        }

        refreshPlanning();
    }

    private void nextPeriod() {

        if (viewBox.getSelectedItem().equals("Semaine")) {
            currentDate = currentDate.plusWeeks(1);
        } else {
            currentDate = currentDate.plusMonths(1);
        }

        refreshPlanning();
    }

    private void today() {
        currentDate = LocalDate.now();
        refreshPlanning();
    }

    private void displayWeek() {

        LocalDate monday = currentDate.with(DayOfWeek.MONDAY);

        planningPanel.setLayout(new BorderLayout());
        JPanel bodyPanel = new JPanel(new BorderLayout());
        JPanel daysPanel = new JPanel(new GridBagLayout());
        bodyPanel.add(daysPanel, BorderLayout.CENTER);
        JPanel weekHeaderPanel = new JPanel(new BorderLayout());
        JLabel hoursHeader = new JLabel();
        hoursHeader.setPreferredSize(new Dimension(80, 25));
        weekHeaderPanel.add(hoursHeader, BorderLayout.WEST);
        JPanel dayHeadersPanel = new JPanel(new GridBagLayout());
        weekHeaderPanel.add(dayHeadersPanel, BorderLayout.CENTER);
        JPanel hoursPanel = new JPanel(new GridLayout(24, 1));
        hoursPanel.setPreferredSize(new Dimension(80, 24 * 60));

        for (int hour = 0; hour < 24; hour++) {

            JLabel hourLabel = new JLabel(String.format("%02dh00", hour), JLabel.CENTER);
            hourLabel.setVerticalAlignment(JLabel.TOP);

            hoursPanel.add(hourLabel);
        }

        bodyPanel.add(hoursPanel, BorderLayout.WEST);

        for (int day = 0; day < 7; day++) {

            LocalDate currentDay = monday.plusDays(day);
            final int minimumInterventionWidth = 95;
            JPanel dayColumn = new JPanel(new BorderLayout());
            final int minimumDayWidth = 120;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE dd/MM", Locale.FRENCH);
            JLabel dayLabel = new JLabel(currentDay.format(formatter), JLabel.CENTER);
            dayLabel.setPreferredSize(new Dimension(120, 25));

            dayLabel.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent event) {
                    if (SwingUtilities.isLeftMouseButton(event)) {
                        dialogs.openAddInterventionChoice(currentDay, null);
                    }
                }
            });

            JPanel dayPanel = new JPanel() {

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);

                    g.setColor(Color.LIGHT_GRAY);

                    for (int hour = 1; hour < 24; hour++) {
                        int y = hour * 60;
                        g.drawLine(0, y, getWidth(), y);
                    }

                    LocalDateTime now = LocalDateTime.now();

                    if (currentDay.isBefore(now.toLocalDate())) {
                        UIUtils.drawPastStripes(g, getWidth(), getHeight());

                    } else if (currentDay.equals(now.toLocalDate())) {
                        int elapsedMinutes = now.getHour() * 60 + now.getMinute();

                        Graphics pastGraphics = g.create();
                        pastGraphics.clipRect(0, 0, getWidth(), elapsedMinutes);

                        UIUtils.drawPastStripes(pastGraphics, getWidth(), getHeight());
                        pastGraphics.dispose();
                    }
                }
            };

            ArrayList<Integer> columnEndMinutes = new ArrayList<>();

            dayPanel.setLayout(null);
            dayPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            dayPanel.setPreferredSize(new Dimension(120, 24 * 60));

            dayPanel.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent event) {

                    if (SwingUtilities.isLeftMouseButton(event)) {
                        int clickedHour = Math.max(0, Math.min(23, event.getY() / 60));
                        LocalTime clickedTime = LocalTime.of(clickedHour, 0);

                        dialogs.openAddInterventionChoice(currentDay, clickedTime);
                    }
                }
            });

            dayPanel.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentResized(ComponentEvent e) {

                    int columnCount = Math.max(1, columnEndMinutes.size());
                    int columnWidth = Math.max(minimumInterventionWidth + 4, dayPanel.getWidth() / columnCount);

                    for (Component component : dayPanel.getComponents()) {

                        JButton button = (JButton) component;
                        int columnIndex = (Integer) button.getClientProperty("columnIndex");
                        int x = columnIndex * columnWidth + 2;
                        int width = Math.max(1, columnWidth - 4);

                        button.setBounds(x, button.getY(), width, button.getHeight());
                    }
                }
            });

            dayColumn.add(dayPanel, BorderLayout.CENTER);

            ArrayList<Intervention> dayInterventions = planning.getInterventionsForDay(currentDay);

            dayInterventions
                    .sort(Comparator.comparing(Intervention::getStartDate).thenComparing(Intervention::getStartTime));

            for (Intervention intervention : dayInterventions) {

                int startMinutes;

                if (intervention.getStartDate().equals(currentDay)) {
                    startMinutes = intervention.getStartTime().getHour() * 60 + intervention.getStartTime().getMinute();

                } else {
                    startMinutes = 0;
                }

                int endMinutes;

                if (intervention.getEndDate().equals(currentDay)) {
                    endMinutes = intervention.getEndTime().getHour() * 60 + intervention.getEndTime().getMinute();

                } else {
                    endMinutes = 24 * 60;
                }

                int y = startMinutes;
                int height = endMinutes - startMinutes;
                int columnIndex = 0;

                while (columnIndex < columnEndMinutes.size() && columnEndMinutes.get(columnIndex) > startMinutes) {
                    columnIndex++;
                }

                if (columnIndex == columnEndMinutes.size()) {
                    columnEndMinutes.add(endMinutes);
                } else {
                    columnEndMinutes.set(columnIndex, endMinutes);
                }

                String employeeText;

                if (intervention.getEmployee() == null) {
                    employeeText = "Non attribué";
                } else {
                    employeeText = intervention.getEmployee().toString();
                }

                String startText = String.format("%02dh%02d", startMinutes / 60, startMinutes % 60);
                String endText = String.format("%02dh%02d", (endMinutes / 60) % 24, endMinutes % 60);
                String text = "<html><div style='text-align: center;'>" + intervention.getClient() + "<br>" + startText
                        + " → " + endText + "<br>" + employeeText + "</div></html>";

                JButton interventionButton = new JButton(text) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);

                        LocalDateTime interventionEnd = LocalDateTime.of(intervention.getEndDate(),
                                intervention.getEndTime());

                        if (!interventionEnd.isAfter(LocalDateTime.now())) {
                            UIUtils.drawPastStripes(g, getWidth(), getHeight());
                        }
                    }
                };

                interventionButton.setHorizontalAlignment(JLabel.CENTER);
                interventionButton.setVerticalAlignment(JLabel.CENTER);
                interventionButton.putClientProperty("columnIndex", columnIndex);
                UIUtils.styleInterventionButton(interventionButton, intervention.getEmployee() != null);

                interventionButton.addActionListener(event -> openInterventionActions(intervention));
                interventionButton.setBounds(2, y, 116, height);

                dayPanel.add(interventionButton);
            }

            int columnCount = columnEndMinutes.size();

            int dayWidth = Math.max(minimumDayWidth, columnCount * (minimumInterventionWidth + 4));

            dayPanel.setPreferredSize(new Dimension(dayWidth, 24 * 60));
            dayPanel.setMinimumSize(new Dimension(dayWidth, 24 * 60));

            dayLabel.setPreferredSize(new Dimension(dayWidth, 25));
            dayLabel.setMinimumSize(new Dimension(dayWidth, 25));

            GridBagConstraints constraints = new GridBagConstraints();
            constraints.gridx = day;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 1;
            constraints.fill = GridBagConstraints.BOTH;

            daysPanel.add(dayColumn, constraints);
            dayHeadersPanel.add(dayLabel, constraints);
        }

        JScrollPane scrollPane = new JScrollPane(bodyPanel);
        scrollPane.setColumnHeaderView(weekHeaderPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);

        planningPanel.add(scrollPane, BorderLayout.CENTER);
        UIUtils.applyTheme(planningPanel);
    }

    private void displayMonth() {

        LocalDate firstDay = currentDate.withDayOfMonth(1);

        int numberOfDays = currentDate.lengthOfMonth();
        int firstDayPosition = firstDay.getDayOfWeek().getValue();
        int numberOfWeeks = (int) Math.ceil((firstDayPosition - 1 + numberOfDays) / 7.0);

        JPanel monthPanel = new JPanel(new GridLayout(0, 7));
        LocalDate calendarStart = firstDay.minusDays(firstDayPosition - 1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE dd/MM", Locale.FRENCH);

        for (int i = 0; i < numberOfWeeks * 7; i++) {

            LocalDate day = calendarStart.plusDays(i);
            String dayText = day.format(formatter);
            JPanel dayPanel = new JPanel(new BorderLayout()) {

                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);

                    if (day.isBefore(LocalDate.now())) {
                        UIUtils.drawPastStripes(g, getWidth(), getHeight());
                    }
                }
            };

            dayPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

            JLabel dayLabel = new JLabel(dayText, JLabel.CENTER);
            dayLabel.setFont(new Font("Arial", Font.BOLD, 17));
            dayPanel.add(dayLabel, BorderLayout.NORTH);

            ArrayList<Intervention> dayInterventions = planning.getInterventionsForDay(day);
            dayInterventions
                    .sort(Comparator.comparing(Intervention::getStartDate).thenComparing(Intervention::getStartTime));

            JPanel interventionsPanel = new JPanel();
            interventionsPanel.setOpaque(false);
            interventionsPanel.setLayout(new BoxLayout(interventionsPanel, BoxLayout.Y_AXIS));

            MouseAdapter addInterventionListener = new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent event) {

                    if (SwingUtilities.isLeftMouseButton(event)) {
                        dialogs.openAddInterventionChoice(day, null);
                    }
                }
            };

            dayPanel.add(interventionsPanel, BorderLayout.CENTER);
            dayPanel.addMouseListener(addInterventionListener);
            dayLabel.addMouseListener(addInterventionListener);
            interventionsPanel.addMouseListener(addInterventionListener);

            for (Intervention intervention : dayInterventions) {

                int startMinutes = 0;

                if (intervention.getStartDate().equals(day)) {
                    startMinutes = intervention.getStartTime().getHour() * 60 + intervention.getStartTime().getMinute();
                }

                int endMinutes = 24 * 60;

                if (intervention.getEndDate().equals(day)) {
                    endMinutes = intervention.getEndTime().getHour() * 60 + intervention.getEndTime().getMinute();
                }

                if (endMinutes <= startMinutes) {
                    continue;
                }

                String employeeText;

                if (intervention.getEmployee() == null) {
                    employeeText = "Non attribué";
                } else {
                    employeeText = intervention.getEmployee().toString();
                }

                String startText = String.format("%02dh%02d", startMinutes / 60, startMinutes % 60);
                String endText = String.format("%02dh%02d", (endMinutes / 60) % 24, endMinutes % 60);
                String text = "<html><div style='text-align: center;'>" + intervention.getClient() + "<br>" + startText
                        + " → " + endText + "<br>" + employeeText + "</div></html>";

                JButton interventionButton = new JButton(text) {

                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);

                        LocalDateTime interventionEnd = LocalDateTime.of(intervention.getEndDate(),
                                intervention.getEndTime());

                        if (!interventionEnd.isAfter(LocalDateTime.now())) {
                            UIUtils.drawPastStripes(g, getWidth(), getHeight());
                        }
                    }
                };

                interventionButton.setHorizontalAlignment(JLabel.CENTER);
                interventionButton.setVerticalAlignment(JLabel.CENTER);
                interventionButton.setFont(new Font("Arial", Font.PLAIN, 16));
                interventionButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));
                interventionButton.addActionListener(event -> openInterventionActions(intervention));
                UIUtils.styleInterventionButton(interventionButton, intervention.getEmployee() != null);

                interventionsPanel.add(interventionButton);
            }

            monthPanel.add(dayPanel);
        }

        planningPanel.setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(monthPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(25);

        planningPanel.add(scrollPane, BorderLayout.CENTER);
        UIUtils.applyTheme(planningPanel);
    }

    public void refreshPlanning() {

        planningPanel.removeAll();

        if (viewBox.getSelectedItem().equals("Semaine")) {
            displayWeek();
        } else {
            displayMonth();
        }

        UIUtils.applyTheme(planningPanel);
        planningPanel.revalidate();
        planningPanel.repaint();
    }

    public ClientService getClients() {
        return clients;
    }

    public void setClients(ClientService clients) {
        this.clients = clients;
    }
}
