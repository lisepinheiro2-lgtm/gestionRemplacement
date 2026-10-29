package gestion.ui;

import static org.junit.jupiter.api.Assertions.*;
import static gestion.ui.UiReliabilityTest.field;
import static gestion.ui.UiReliabilityTest.find;
import static gestion.ui.UiReliabilityTest.invoke;
import static gestion.ui.UiReliabilityTest.setField;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import gestion.controller.PersonnelController;
import gestion.model.*;
import gestion.repository.InterventionCsvRepository;
import gestion.service.PlanningService;
import gestion.service.WorkHoursService;
import gestion.ui.intervention.InterventionUI;
import gestion.ui.personnel.PersonnelPanel;
import gestion.ui.personnel.PersonnelTableModel;
import gestionTest.support.TestFixture;

class ClientFluidityRegressionTest {
    @TempDir Path directory;
    private TestFixture f;

    @BeforeEach
    void initialize() throws Exception {
        f = new TestFixture(directory);
    }

    @FunctionalInterface
    private interface EdtAction {
        void run() throws Exception;
    }

    private static void onEdt(EdtAction action) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                action.run();
            } catch (Exception exception) {
                throw new AssertionError(exception);
            }
        });
    }

    private static Object call(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static void resized(JViewport viewport) {
        ComponentEvent event = new ComponentEvent(viewport, ComponentEvent.COMPONENT_RESIZED);
        for (var listener : viewport.getComponentListeners())
            listener.componentResized(event);
    }

    private final class CountingPlanning extends PlanningService {
        int reads;
        int absenceReads;
        int seriesReads;

        CountingPlanning() {
            super(f.personnel, f.clients, new InterventionCsvRepository(directory));
            super.getInterventions().clear();
            super.getInterventions().addAll(f.planning.getInterventions());
        }

        @Override
        public ArrayList<Intervention> getInterventions() {
            reads++;
            return super.getInterventions();
        }

        @Override
        public ArrayList<Intervention> getInterventionsByAbsenceId(String id) {
            absenceReads++;
            return super.getInterventionsByAbsenceId(id);
        }

        @Override
        public ArrayList<Intervention> getInterventionsByRecurringId(String id) {
            seriesReads++;
            return super.getInterventionsByRecurringId(id);
        }
    }

    @Test
    void tableRefreshPublishesOneCompleteUpdateAndLeavesColumnsIntact() throws Exception {
        onEdt(() -> {
            DefaultTableModel model = new DefaultTableModel(new Object[] { "Nom", "Heures" }, 0);
            JTable table = new JTable(model);
            Object column = table.getColumnModel().getColumn(0);
            AtomicInteger changes = new AtomicInteger();
            model.addTableModelListener(event -> {
                assertEquals(1000, model.getRowCount());
                changes.incrementAndGet();
            });
            List<Object[]> rows = new ArrayList<>();
            for (int index = 0; index < 1000; index++)
                rows.add(new Object[] { "Fictif " + index, index });
            assertTrue(UIUtils.updateTableRows(model, rows));
            assertEquals(1, changes.get());
            assertFalse(UIUtils.updateTableRows(model, rows));
            assertEquals(1, changes.get());
            assertSame(column, table.getColumnModel().getColumn(0));
            assertThrows(IllegalArgumentException.class,
                    () -> UIUtils.updateTableRows(model, java.util.Collections.singletonList(new Object[] { "Invalide" })));
            assertEquals(1000, model.getRowCount());
        });
    }

    @Test
    void clientsKeepTheirSelectionAfterInsertionAndAllowNarrowWindowScrolling() throws Exception {
        onEdt(() -> {
            ClientUI panel = new ClientUI(f.clients);
            JTable table = (JTable) field(panel, "clientTable");
            table.setRowSelectionInterval(0, 0);
            AtomicInteger changes = new AtomicInteger();
            table.getModel().addTableModelListener(event -> changes.incrementAndGet());
            panel.refreshTable();
            assertEquals(0, changes.get());
            f.clients.addClient("Autre", "AAAA", "Adresse fictive", 10, null, null);
            panel.refreshTable();
            assertEquals(1, changes.get());
            assertEquals(f.client.getLastName(), table.getValueAt(table.getSelectedRow(), 0));
            JScrollPane scroll = find(panel, JScrollPane.class).getFirst();
            scroll.getViewport().setExtentSize(new Dimension(300, 400));
            resized(scroll.getViewport());
            assertEquals(JTable.AUTO_RESIZE_OFF, table.getAutoResizeMode());
            scroll.getViewport().setExtentSize(new Dimension(3000, 400));
            resized(scroll.getViewport());
            assertEquals(JTable.AUTO_RESIZE_ALL_COLUMNS, table.getAutoResizeMode());
        });
    }

    @Test
    void identicalTextOnDifferentInterventionPagesCannotRetargetASelectionSilently() throws Exception {
        for (int index = 0; index < 12; index++)
            f.add(null, f.future, 8, 9);
        onEdt(() -> {
            InterventionUI panel = new InterventionUI(f.planning, f.recurring, f.clients, f.personnel);
            JTable table = (JTable) field(panel, "interventionTable");
            JScrollPane scroll = (JScrollPane) field(panel, "interventionScrollPane");
            scroll.getViewport().setExtentSize(new Dimension(800, table.getRowHeight() * 3));
            panel.refreshIntervention();
            table.setRowSelectionInterval(0, 0);
            Intervention selected = (Intervention) invoke(panel, "getSelectedIntervention");
            invoke(panel, "nextPage");
            assertEquals(-1, table.getSelectedRow());
            assertNull(invoke(panel, "getSelectedIntervention"));
            assertNotSame(selected, ((List<?>) field(panel, "visibleRows")).getFirst());
            assertTrue(((JLabel) field(panel, "pageSummary")).getText().contains("2 / 4"));
        });
    }

    @Test
    void interventionResizeAndNextPageAvoidDuplicateFiltering() throws Exception {
        for (int index = 0; index < 20; index++)
            f.add(null, f.future.plusDays(index), 8, 9);
        onEdt(() -> {
            CountingPlanning planning = new CountingPlanning();
            InterventionUI panel = new InterventionUI(planning, f.recurring, f.clients, f.personnel);
            JTable table = (JTable) field(panel, "interventionTable");
            JViewport viewport = ((JScrollPane) field(panel, "interventionScrollPane")).getViewport();
            viewport.setExtentSize(new Dimension(800, table.getRowHeight() * 5));
            panel.refreshIntervention();
            table.setRowSelectionInterval(2, 2);
            Intervention selected = (Intervention) invoke(panel, "getSelectedIntervention");
            planning.reads = 0;
            viewport.setExtentSize(new Dimension(900, table.getRowHeight() * 5));
            resized(viewport);
            assertEquals(0, planning.reads);
            viewport.setExtentSize(new Dimension(900, table.getRowHeight() * 7));
            resized(viewport);
            assertEquals(1, planning.reads);
            assertSame(selected, invoke(panel, "getSelectedIntervention"));
            planning.reads = 0;
            invoke(panel, "nextPage");
            assertEquals(1, planning.reads);
        });
    }

    @Test
    void replacementCountersAndTooltipsAvoidRepeatedAbsenceScansAndKeepChoices() throws Exception {
        f.add(f.a, f.future, 8, 10);
        f.absences.addAbsence(new EmployeeAbsence(f.a, EmployeeStatus.SICK_LEAVE, f.future, f.future));
        onEdt(() -> {
            CountingPlanning planning = new CountingPlanning();
            ReplacementUI panel = new ReplacementUI(f.replacements, f.personnel, planning);
            assertFalse(f.replacements.getReplacements().isEmpty());
            JTable table = (JTable) field(panel, "replacementTable");
            assertEquals("1 / 1", table.getValueAt(0, 2));
            assertEquals("2h00 / 2h00", table.getValueAt(0, 3));
            @SuppressWarnings("unchecked")
            JComboBox<Employee> substitutes = (JComboBox<Employee>) field(panel, "replacementEmployeeBox");
            substitutes.setSelectedItem(f.b);
            Object model = substitutes.getModel();
            ((JCheckBox) field(panel, "assignAllCheckBox")).setSelected(true);
            table.setRowSelectionInterval(0, 0);
            panel.refreshTable();
            assertSame(model, substitutes.getModel());
            assertSame(f.b, substitutes.getSelectedItem());
            assertTrue(((JCheckBox) field(panel, "assignAllCheckBox")).isSelected());
            assertEquals(0, table.getSelectedRow());
            MouseEvent hover = new MouseEvent(table, MouseEvent.MOUSE_MOVED, 0, 0, 3, 3, 0, false);
            assertNotNull(table.getToolTipText(hover));
            assertNotNull(table.getToolTipText(hover));
            assertEquals(0, planning.absenceReads);
        });
    }

    @Test
    void personnelSnapshotsDetectMutableNamesAndKeepSelectedEmployeeAfterSorting() throws Exception {
        onEdt(() -> {
            PersonnelController controller = new PersonnelController(f.personnel, f.absences, new WorkHoursService(f.planning));
            PersonnelTableModel model = new PersonnelTableModel((employee, first, last, hours) -> { });
            model.setRows(controller.getRows(f.future, false));
            AtomicInteger changes = new AtomicInteger();
            model.addTableModelListener(event -> changes.incrementAndGet());
            model.setRows(controller.getRows(f.future, false));
            assertEquals(0, changes.get());
            PersonnelPanel panel = new PersonnelPanel(controller, f.planning, f.recurring);
            JTable table = find(panel, JTable.class).getFirst();
            table.setRowSelectionInterval(0, 0);
            f.personnel.editEmployee(0, f.a.getFirstName(), "Zulu", 35, "Opérationnel", 0, 0);
            model.setRows(controller.getRows(f.future, false));
            assertEquals(1, changes.get());
            panel.refreshTable();
            PersonnelTableModel displayed = (PersonnelTableModel) table.getModel();
            assertSame(f.a, displayed.rowAt(table.convertRowIndexToModel(table.getSelectedRow())).employee());
        });
    }

    @Test
    void recurringHoursAreComputedOnHoverInsteadOfEveryCellPaintAndRefreshInvalidatesThem() throws Exception {
        RecurringIntervention series = f.series(null, f.future, f.future.plusWeeks(1));
        f.recurring.createWithPlanning(f.planning, series);
        onEdt(() -> {
            CountingPlanning planning = new CountingPlanning();
            Class<?> type = Class.forName("gestion.ui.personnel.PersonnelInterventionsTable");
            Constructor<?> constructor = type.getDeclaredConstructor(PlanningService.class, boolean.class);
            constructor.setAccessible(true);
            JTable table = (JTable) constructor.newInstance(planning, false);
            call(table, "showInterventions", new Class<?>[] { List.class }, planning.getInterventions());
            table.setSize(900, 200);
            table.doLayout();
            for (int index = 0; index < 10; index++)
                for (int column = 0; column < table.getColumnCount(); column++)
                    table.prepareRenderer(table.getCellRenderer(0, column), 0, column);
            assertEquals(0, planning.seriesReads);
            var cell = table.getCellRect(0, 1, true);
            MouseEvent hover = new MouseEvent(table, MouseEvent.MOUSE_MOVED, 0, 0, cell.x + 1, cell.y + 1, 0, false);
            String first = table.getToolTipText(hover);
            assertTrue(first.contains("2h00"));
            assertEquals(first, table.getToolTipText(hover));
            assertEquals(1, planning.seriesReads);
            Intervention occurrence = planning.getInterventions().getFirst();
            occurrence.setEndTime(occurrence.getEndTime().plusHours(1));
            call(table, "showInterventions", new Class<?>[] { List.class }, new ArrayList<>(planning.getInterventions()));
            assertTrue(table.getToolTipText(hover).contains("3h00"));
            assertEquals(2, planning.seriesReads);
        });
    }

    @Test
    void assignmentImpactRequestsAreCoalescedWithoutOpeningADialog() throws Exception {
        Object[] dialog = new Object[1];
        AtomicInteger changes = new AtomicInteger();
        onEdt(() -> {
            Class<?> type = Class.forName("gestion.ui.personnel.PersonnelInterventionsDialog");
            dialog[0] = type.getConstructor(PlanningService.class, Runnable.class).newInstance(f.planning, (Runnable) () -> { });
            setField(dialog[0], "employee", f.a);
            setField(dialog[0], "period", new DateRange(f.future, f.future.plusDays(6)));
            call(dialog[0], "createContent", new Class<?>[] { boolean.class }, false);
            Object impact = field(dialog[0], "impact");
            ((AbstractTableModel) field(impact, "model")).addTableModelListener(event -> changes.incrementAndGet());
            for (int index = 0; index < 20; index++)
                invoke(dialog[0], "requestImpact");
            assertEquals(0, changes.get());
        });
        onEdt(() -> {
            assertEquals(1, changes.get());
            assertFalse((boolean) field(dialog[0], "impactPending"));
        });
    }

    @Test
    void applicationThemeLeavesLookAndFeelOwnedControlsUntouched() throws Exception {
        onEdt(() -> {
            JButton internal = new javax.swing.plaf.basic.BasicArrowButton(SwingConstants.SOUTH);
            Object style = internal.getClientProperty("FlatLaf.style");
            var background = internal.getBackground();
            UIUtils.applyTheme(internal);
            assertEquals(style, internal.getClientProperty("FlatLaf.style"));
            assertEquals(background, internal.getBackground());
            JButton application = new JButton("Ajouter");
            UIUtils.applyTheme(application);
            assertNotNull(application.getClientProperty("FlatLaf.style"));
        });
    }

    @Test
    void denseMonthKeepsSevenColumnsAndAllInterventionsWithBoundedWidgets() throws Exception {
        for (int day = 0; day < 7; day++)
            for (int index = 0; index < 60; index++)
                f.planning.getInterventions().add(new Intervention(f.client, null, f.future.plusDays(day),
                        java.time.LocalTime.of(8, index), f.future.plusDays(day), java.time.LocalTime.of(10, index)));
        onEdt(() -> {
            var panel = new gestion.ui.planning.PlanningUI(f.planning, f.personnel, f.clients, f.recurring);
            setField(panel, "currentDate", f.future);
            setField(panel, "selectedDay", f.future);
            ((JComboBox<?>) field(panel, "viewBox")).setSelectedItem("Mois");
            panel.refreshPlanning();
            JScrollPane scroll = find(panel, JScrollPane.class).getFirst();
            JPanel month = (JPanel) scroll.getViewport().getView();
            assertEquals(7, ((java.awt.GridLayout) month.getLayout()).getColumns());
            assertEquals(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER, scroll.getHorizontalScrollBarPolicy());
            assertTrue(month.getPreferredSize().height < 1500);
            for (Component component : month.getComponents()) {
                JPanel day = (JPanel) component;
                JLabel date = (JLabel) ((java.awt.BorderLayout) day.getLayout()).getLayoutComponent(java.awt.BorderLayout.NORTH);
                assertNotNull(date);
                assertEquals(((java.time.LocalDate) day.getClientProperty("planning.day"))
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM")), date.getText());
                assertFalse(date.getToolTipText().isBlank());
                assertTrue(date.getMouseListeners().length > 0);
            }
            var represented = new java.util.HashSet<Intervention>();
            int groups = 0;
            for (JButton button : find(month, JButton.class)) {
                if (button.getClientProperty("planning.intervention") instanceof Intervention value)
                    represented.add(value);
                if (button.getClientProperty("planning.group") instanceof List<?> values) {
                    groups++;
                    for (Object value : values)
                        represented.add((Intervention) value);
                }
            }
            assertEquals(7, groups);
            assertTrue(find(month, JButton.class).size() <= 21);
            assertEquals(new java.util.HashSet<>(f.planning.getInterventions()), represented);
        });
    }

    @Test
    void narrowWeeklyGroupsKeepTheirCountsAndDayListTracksViewportWidth() throws Exception {
        for (int index = 0; index < 30; index++)
            f.planning.getInterventions().add(new Intervention(f.client, null, f.future,
                    java.time.LocalTime.of(8, 0), f.future, java.time.LocalTime.of(10, 0)));
        onEdt(() -> {
            var panel = new gestion.ui.planning.PlanningUI(f.planning, f.personnel, f.clients, f.recurring);
            setField(panel, "currentDate", f.future);
            setField(panel, "selectedDay", f.future);
            ((JComboBox<?>) field(panel, "viewBox")).setSelectedItem("Semaine");
            JSplitPane split = (JSplitPane) ((JPanel) field(panel, "planningPanel")).getComponent(0);
            JPanel grid = (JPanel) ((JScrollPane) split.getLeftComponent()).getViewport().getView();
            grid.setSize(700, 1440);
            grid.doLayout();
            for (Component day : grid.getComponents())
                ((Container) day).doLayout();
            UIUtils.applyTheme(panel);
            JButton group = find(grid, JButton.class).stream()
                    .filter(button -> button.getClientProperty("planning.group") instanceof List<?>).findFirst().orElseThrow();
            assertTrue(group.getMargin().left <= 2);
            assertTrue(group.getFontMetrics(group.getFont()).stringWidth(group.getText()) < group.getWidth() - 4);
            JList<?> list = find((Container) split.getRightComponent(), JList.class).getFirst();
            ((JViewport) list.getParent()).setExtentSize(new Dimension(140, 200));
            assertTrue(list.getScrollableTracksViewportWidth());
            assertEquals(30, list.getModel().getSize());
        });
    }


    @Test
    void replacementActionsStayCompactAndPlanningStartsInWeeklyView() throws Exception {
        onEdt(() -> {
            ReplacementUI replacements = new ReplacementUI(f.replacements, f.personnel, f.planning);
            UIUtils.applyTheme(replacements);
            List<JButton> actions = find(replacements, JButton.class).stream()
                    .filter(button -> List.of("Ajouter un remplacement", "Supprimer le remplacement",
                            "Attribuer les non attribuées").contains(button.getText())).toList();
            assertEquals(3, actions.size());
            for (JButton action : actions) {
                assertEquals(32, action.getPreferredSize().height);
                assertTrue(action.getMargin().top <= 3);
                assertNotNull(action.getToolTipText());
                assertTrue(action.getActionListeners().length > 0);
            }
            var planning = new gestion.ui.planning.PlanningUI(f.planning, f.personnel, f.clients, f.recurring);
            assertEquals("Semaine", ((JComboBox<?>) field(planning, "viewBox")).getSelectedItem());
        });
    }

}
