package gestion.ui;

import static org.junit.jupiter.api.Assertions.*;
import static gestion.ui.UiReliabilityTest.field;
import static gestion.ui.UiReliabilityTest.find;
import static gestion.ui.UiReliabilityTest.setField;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import gestion.model.DateRange;
import gestion.model.Intervention;
import gestion.repository.InterventionCsvRepository;
import gestion.service.PlanningService;
import gestion.ui.planning.PlanningUI;
import gestionTest.support.TestFixture;

class PlanningWeekRegressionTest {
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

    private PlanningUI panel(PlanningService planning) throws Exception {
        PlanningUI panel = new PlanningUI(planning, f.personnel, f.clients, f.recurring);
        setField(panel, "currentDate", f.future);
        setField(panel, "selectedDay", f.future);
        ((JComboBox<?>) field(panel, "viewBox")).setSelectedItem("Semaine");
        panel.setSize(1200, 800);
        layout(panel);
        return panel;
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents())
            if (child instanceof Container nested)
                layout(nested);
    }

    private static Object call(Object target, String name, Class<?>[] types, Object... values) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, values);
    }

    private static void select(PlanningUI panel, LocalDate day) throws Exception {
        call(panel, "selectDay", new Class<?>[] { LocalDate.class }, day);
    }

    private static void move(PlanningUI panel, String name) throws Exception {
        call(panel, name, new Class<?>[0]);
    }

    private static JSplitPane week(PlanningUI panel) throws Exception {
        return (JSplitPane) ((JPanel) field(panel, "planningPanel")).getComponent(0);
    }

    private static JPanel grid(JSplitPane week) {
        return (JPanel) ((JScrollPane) week.getLeftComponent()).getViewport().getView();
    }

    private static List<JButton> blocks(JSplitPane week) {
        return find(grid(week), JButton.class);
    }

    private static Set<Intervention> represented(Container container) {
        Set<Intervention> values = new HashSet<>();
        for (JButton button : find(container, JButton.class)) {
            if (button.getClientProperty("planning.intervention") instanceof Intervention intervention)
                values.add(intervention);
            if (button.getClientProperty("planning.group") instanceof List<?> group)
                for (Object value : group)
                    values.add((Intervention) value);
        }
        return values;
    }

    private static JList<?> details(JSplitPane week) {
        return find((Container) week.getRightComponent(), JList.class).getFirst();
    }

    private final class CountingPlanning extends PlanningService {
        private int queries;

        CountingPlanning() throws Exception {
            super(f.personnel, f.clients, new InterventionCsvRepository(directory));
            getInterventions().clear();
            getInterventions().addAll(f.planning.getInterventions());
        }

        @Override
        public ArrayList<Intervention> getInterventionsForPeriod(DateRange period) {
            queries++;
            return super.getInterventionsForPeriod(period);
        }
    }

    @Test
    void selectingDaysKeepsGridButtonsScrollAndDividerWithoutQueryingData() throws Exception {
        Intervention monday = f.add(null, f.future, 8, 9);
        Intervention tuesday = f.add(null, f.future.plusDays(1), 11, 12);
        onEdt(() -> {
            CountingPlanning planning = new CountingPlanning();
            PlanningUI panel = panel(planning);
            JSplitPane week = week(panel);
            JScrollPane scroll = (JScrollPane) week.getLeftComponent();
            List<JButton> original = blocks(week);
            AtomicInteger changes = new AtomicInteger();
            ContainerAdapter listener = new ContainerAdapter() {
                @Override public void componentAdded(ContainerEvent event) { changes.incrementAndGet(); }
                @Override public void componentRemoved(ContainerEvent event) { changes.incrementAndGet(); }
            };
            grid(week).addContainerListener(listener);
            for (Component column : grid(week).getComponents())
                ((Container) column).addContainerListener(listener);
            ((JPanel) field(panel, "planningPanel")).addContainerListener(listener);
            week.setDividerLocation(750);
            scroll.getVerticalScrollBar().setValue(420);
            int divider = week.getDividerLocation();
            int position = scroll.getVerticalScrollBar().getValue();
            int queries = planning.queries;
            Container strip = (Container) scroll.getColumnHeader().getView();
            JLabel heading = (JLabel) strip.getComponent(1);
            heading.dispatchEvent(new MouseEvent(heading, MouseEvent.MOUSE_CLICKED, 0, 0, 2, 2, 1, false, MouseEvent.BUTTON1));
            assertEquals(f.future.plusDays(1), field(panel, "selectedDay"));
            assertEquals(tuesday, details(week).getModel().getElementAt(0));
            JList<?> unchangedDetails = details(week);
            select(panel, f.future.plusDays(1));
            assertSame(unchangedDetails, details(week));
            select(panel, f.future);
            assertEquals(monday, details(week).getModel().getElementAt(0));
            layout(panel);
            assertSame(week, week(panel));
            assertEquals(original, blocks(week));
            assertEquals(0, changes.get());
            assertEquals(queries, planning.queries);
            assertEquals(position, scroll.getVerticalScrollBar().getValue());
            assertEquals(divider, week.getDividerLocation());
            assertEquals(1, ((Map<?, ?>) field(panel, "views")).size());
        });
    }

    @Test
    void resizingReusesButtonsUntilOverlapCapacityChanges() throws Exception {
        for (int index = 0; index < 4; index++)
            f.add(null, f.future, 8, 10);
        onEdt(() -> {
            JSplitPane week = week(panel(f.planning));
            JPanel column = (JPanel) grid(week).getComponent(0);
            column.setSize(190, 1440);
            column.doLayout();
            List<JButton> original = find(column, JButton.class);
            Object placement = column.getClientProperty("planning.layout");
            column.setSize(220, 1440);
            column.doLayout();
            assertEquals(original, find(column, JButton.class));
            assertSame(placement, column.getClientProperty("planning.layout"));
            column.setSize(300, 1440);
            column.doLayout();
            assertNotSame(placement, column.getClientProperty("planning.layout"));
            assertEquals(new HashSet<>(f.planning.getInterventions()), represented(column));
            assertTrue(find(column, JButton.class).stream().anyMatch(button -> button.getClientProperty("planning.group") instanceof List<?>));
        });
    }

    @Test
    void businessRefreshInvalidatesAddedAssignedReassignedEditedAndDeletedInterventions() throws Exception {
        Intervention first = f.add(null, f.future, 8, 9);
        onEdt(() -> {
            PlanningUI panel = panel(f.planning);
            select(panel, f.future.plusDays(1));
            JSplitPane previous = week(panel);
            Intervention added = f.add(null, f.future.plusDays(1), 10, 11);
            panel.refreshPlanning();
            assertNotSame(previous, week(panel));
            assertTrue(represented(grid(week(panel))).contains(added));
            assertEquals(added, details(week(panel)).getModel().getElementAt(0));
            previous = week(panel);
            f.planning.assignEmployee(first, f.a);
            panel.refreshPlanning();
            assertNotSame(previous, week(panel));
            previous = week(panel);
            f.planning.assignEmployee(first, f.b);
            panel.refreshPlanning();
            assertNotSame(previous, week(panel));
            previous = week(panel);
            f.planning.editIntervention(f.planning.getInterventions().indexOf(first), f.client, f.b,
                    f.future, LocalTime.of(8, 15), f.future, LocalTime.of(9, 15));
            panel.refreshPlanning();
            assertNotSame(previous, week(panel));
            JButton edited = blocks(week(panel)).stream()
                    .filter(button -> button.getClientProperty("planning.intervention") == first).findFirst().orElseThrow();
            assertEquals(8 * 60 + 15, edited.getY());
            assertEquals(60, edited.getHeight());
            f.planning.removeIntervention(first);
            panel.refreshPlanning();
            assertFalse(represented(grid(week(panel))).contains(first));
            assertEquals(f.future.plusDays(1), field(panel, "selectedDay"));
        });
    }

    @Test
    void returningToCachedWeekReusesItAndRejectsOutdatedData() throws Exception {
        f.add(null, f.future, 8, 9);
        onEdt(() -> {
            PlanningUI panel = panel(f.planning);
            JSplitPane original = week(panel);
            move(panel, "nextPeriod");
            move(panel, "previousPeriod");
            assertSame(original, week(panel));
            move(panel, "nextPeriod");
            Intervention added = f.add(null, f.future, 10, 11);
            move(panel, "previousPeriod");
            assertNotSame(original, week(panel));
            assertTrue(represented(grid(week(panel))).contains(added));
        });
    }

    @Test
    void filteringUpdatesGridAndDetailsAndClearingSearchRestoresAllData() throws Exception {
        Intervention assigned = f.add(f.a, f.future, 8, 9);
        Intervention unassigned = f.add(null, f.future, 10, 11);
        onEdt(() -> {
            PlanningUI panel = panel(f.planning);
            JTextField search = (JTextField) field(panel, "search");
            search.setText("Ana");
            search.postActionEvent();
            assertEquals(Set.of(assigned), represented(grid(week(panel))));
            assertEquals(1, details(week(panel)).getModel().getSize());
            select(panel, f.future.plusDays(1));
            assertTrue(find((Container) week(panel).getRightComponent(), JLabel.class).stream()
                    .anyMatch(label -> "Aucune intervention à afficher.".equals(label.getText())));
            search.setText("");
            search.postActionEvent();
            assertEquals(Set.of(assigned, unassigned), represented(grid(week(panel))));
        });
    }

    @Test
    void navigatingManyWeeksKeepsOnlyThreeCachedPeriodsAndOneDisplayedGrid() throws Exception {
        onEdt(() -> {
            PlanningUI panel = panel(f.planning);
            JSplitPane initial = week(panel);
            for (int index = 0; index < 24; index++) {
                move(panel, "nextPeriod");
                Map<?, ?> views = (Map<?, ?>) field(panel, "views");
                assertTrue(views.size() <= 3);
                assertEquals(1, ((JPanel) field(panel, "planningPanel")).getComponentCount());
            }
            for (Object cached : ((Map<?, ?>) field(panel, "views")).values())
                assertNotSame(initial, field(cached, "component"));
        });
    }

    @Test
    void themeChangesKeepGridAndAvoidReinstallingTheCurrentLookAndFeel() throws Exception {
        f.add(null, f.future, 8, 9);
        onEdt(() -> {
            boolean dark = UIUtils.isDarkMode();
            try {
                PlanningUI panel = panel(f.planning);
                JSplitPane original = week(panel);
                JButton button = blocks(original).getFirst();
                AtomicInteger uiChanges = new AtomicInteger();
                button.addPropertyChangeListener("UI", event -> uiChanges.incrementAndGet());
                UIUtils.setDarkMode(!dark);
                SwingUtilities.updateComponentTreeUI(panel);
                UIUtils.applyTheme(panel);
                uiChanges.set(0);
                select(panel, f.future.plusDays(1));
                select(panel, f.future);
                panel.refreshPlanning();
                assertSame(original, week(panel));
                assertEquals(0, uiChanges.get());
                assertEquals(UIUtils.getTextColor(), button.getForeground());
                move(panel, "nextPeriod");
                UIUtils.setDarkMode(dark);
                SwingUtilities.updateComponentTreeUI(panel);
                UIUtils.applyTheme(panel);
                move(panel, "previousPeriod");
                assertSame(original, week(panel));
                assertEquals(UIUtils.getTextColor(), button.getForeground());
                int applied = uiChanges.get();
                panel.refreshPlanning();
                assertEquals(applied, uiChanges.get());
            } finally {
                UIUtils.setDarkMode(dark);
            }
        });
    }

    @Test
    void shortInterventionsMidnightAndTemporalGroupsKeepTheirBoundsAndDayDetails() throws Exception {
        Intervention night = new Intervention(f.client, null, f.future, LocalTime.of(22, 45),
                f.future.plusDays(1), LocalTime.of(0, 15));
        Intervention shortOne = new Intervention(f.client, null, f.future, LocalTime.of(9, 0),
                f.future, LocalTime.of(9, 5));
        f.planning.addIntervention(night);
        f.planning.addIntervention(shortOne);
        for (int index = 0; index < 4; index++)
            f.add(null, f.future, 10, 12);
        onEdt(() -> {
            PlanningUI panel = panel(f.planning);
            JSplitPane week = week(panel);
            JPanel monday = (JPanel) grid(week).getComponent(0);
            JPanel tuesday = (JPanel) grid(week).getComponent(1);
            assertTrue(find(monday, JButton.class).stream().anyMatch(button -> button.getClientProperty("planning.intervention") == shortOne
                    && button.getY() == 540 && button.getHeight() == 5));
            assertTrue(find(monday, JButton.class).stream().anyMatch(button -> button.getClientProperty("planning.intervention") == night
                    && button.getY() == 1365 && button.getHeight() == 75));
            assertTrue(find(tuesday, JButton.class).stream().anyMatch(button -> button.getClientProperty("planning.intervention") == night
                    && button.getY() == 0 && button.getHeight() == 15));
            assertEquals(new HashSet<>(f.planning.getInterventions()), represented(grid(week)));
            select(panel, f.future.plusDays(1));
            assertSame(week, week(panel));
            assertEquals(night, details(week).getModel().getElementAt(0));
        });
    }

    @Test
    void denseWeeksReuseGridAndDetailsRendererWithoutQueriesDuringSelection() throws Exception {
        for (int day = 0; day < 7; day++)
            for (int index = 0; index < 200; index++)
                f.planning.getInterventions().add(new Intervention(f.client, null, f.future.plusDays(day),
                        LocalTime.of(8, index % 60), f.future.plusDays(day), LocalTime.of(18, index % 60)));
        onEdt(() -> {
            CountingPlanning planning = new CountingPlanning();
            PlanningUI panel = panel(planning);
            JSplitPane original = week(panel);
            List<JButton> originalButtons = blocks(original);
            int queries = planning.queries;
            for (int index = 0; index < 100; index++) {
                select(panel, f.future.plusDays(index % 7));
                assertSame(original, week(panel));
                assertEquals(200, details(original).getModel().getSize());
            }
            assertEquals(queries, planning.queries);
            assertEquals(originalButtons, blocks(original));
            @SuppressWarnings("unchecked")
            JList<Intervention> list = (JList<Intervention>) details(original);
            Component first = list.getCellRenderer().getListCellRendererComponent(list, list.getModel().getElementAt(0), 0, false, false);
            Component last = list.getCellRenderer().getListCellRendererComponent(list, list.getModel().getElementAt(199), 199, false, false);
            assertSame(first, last);
            assertEquals(1, ((Map<?, ?>) field(panel, "views")).size());
        });
    }
}
