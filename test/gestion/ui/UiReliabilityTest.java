package gestion.ui;

import static org.junit.jupiter.api.Assertions.*;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.List;
import java.util.function.*;
import javax.swing.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import gestion.model.*;
import gestion.ui.intervention.InterventionUI;
import gestionTest.support.TestFixture;

class UiReliabilityTest {
    @TempDir Path directory;
    TestFixture f;
    List<String> errors = new ArrayList<>();
    BiConsumer<Component, String> oldError;
    BiPredicate<Component, String> oldConfirm;
    @BeforeEach void initialize() throws IOException {
        f = new TestFixture(directory); oldError = ActionDialogs.errorHandler; oldConfirm = ActionDialogs.confirmHandler;
        ActionDialogs.errorHandler = (parent, message) -> errors.add(message);
        ActionDialogs.confirmHandler = (parent, message) -> true;
    }
    @AfterEach void restore() { ActionDialogs.errorHandler = oldError; ActionDialogs.confirmHandler = oldConfirm; }
    static Object field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(target);
    }
    static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name); field.setAccessible(true); field.set(target, value);
    }
    static Object invoke(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name); method.setAccessible(true);
        try { return method.invoke(target); }
        catch (InvocationTargetException e) { throw new AssertionError(e.getCause()); }
    }
    static <T> List<T> find(Container root, Class<T> type) {
        List<T> found = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) found.add(type.cast(child));
            if (child instanceof Container container) found.addAll(find(container, type));
        }
        return found;
    }
    @Test void selectionRemainsTheDisplayedObjectAfterPageSizeChangesAndDeletesThatObject() throws Exception {
        for (int day = 0; day < 35; day++) f.add(null, f.future.plusDays(day), 8, 10);
        SwingUtilities.invokeAndWait(() -> {
            try {
                InterventionUI panel = new InterventionUI(f.planning, f.recurring, f.clients, f.personnel);
                JTable table = (JTable) field(panel, "interventionTable");
                JScrollPane scroll = (JScrollPane) field(panel, "interventionScrollPane");
                scroll.getViewport().setExtentSize(new Dimension(500, table.getRowHeight() * 10));
                setField(panel, "currentPage", 1); panel.refreshIntervention(); table.setRowSelectionInterval(0, 0);
                Intervention displayed = f.planning.getInterventions().get(10);
                Intervention unrelated = f.planning.getInterventions().get(20);
                assertSame(displayed, invoke(panel, "getSelectedIntervention"));
                scroll.getViewport().setExtentSize(new Dimension(500, table.getRowHeight() * 20));
                assertSame(displayed, invoke(panel, "getSelectedIntervention"));
                var resized = new java.awt.event.ComponentEvent(scroll.getViewport(), java.awt.event.ComponentEvent.COMPONENT_RESIZED);
                for (var listener : scroll.getViewport().getComponentListeners()) listener.componentResized(resized);
                assertSame(displayed, invoke(panel, "getSelectedIntervention"));
                invoke(panel, "removeIntervention");
                assertFalse(f.planning.getInterventions().contains(displayed));
                assertTrue(f.planning.getInterventions().contains(unrelated));
                assertEquals(34, f.planning.getInterventions().size());
            } catch (Exception e) { throw new AssertionError(e); }
        });
        f.reload();
    }

    @Test void threeTablesDoNotOfferUnsavedCellEditsAndNamesKeepTheirCase() throws Exception {
        f.add(f.a, f.future, 8, 10);
        SwingUtilities.invokeAndWait(() -> {
            for (JPanel panel : List.of(new ClientUI(f.clients), new ReplacementUI(f.replacements, f.personnel, f.planning),
                    new InterventionUI(f.planning, f.recurring, f.clients, f.personnel))) {
                if (panel instanceof InterventionUI interventions) interventions.refreshIntervention();
                JTable table = find(panel, JTable.class).getFirst();
                for (int column = 0; column < table.getColumnCount(); column++) assertFalse(table.getModel().isCellEditable(0, column));
                if (panel instanceof InterventionUI) assertEquals("Fictif Client", table.getValueAt(0, 0));
            }
        });
    }

    @Test void failedClientSaveShowsErrorKeepsInputsAndDoesNotAddARow() throws Exception {
        Files.delete(directory.resolve("clients.csv")); Files.createDirectory(directory.resolve("clients.csv"));
        SwingUtilities.invokeAndWait(() -> {
            try {
                ClientUI panel = new ClientUI(f.clients);
                Map<String, String> values = Map.of("lastNameField", "Nouveau", "firstNameField", "Client", "addressField", "Adresse fictive",
                        "totalHoursField", "5", "startDateField", "", "endDateField", "");
                for (var entry : values.entrySet()) ((JTextField) field(panel, entry.getKey())).setText(entry.getValue());
                invoke(panel, "addClient");
                assertEquals(1, errors.size()); assertEquals(1, f.clients.getClients().size());
                assertEquals("Nouveau", ((JTextField) field(panel, "lastNameField")).getText());
                assertEquals(1, find(panel, JTable.class).getFirst().getRowCount());
            } catch (Exception e) { throw new AssertionError(e); }
        });
    }

    @Test void replacementChoicesRefreshAndEmptyPersonnelIsReportedWithoutFalseSuccess() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                ReplacementUI panel = new ReplacementUI(f.replacements, f.personnel, f.planning);
                f.personnel.addEmployee("Dora", "Delta", 20, "Opérationnel", 0, 0);
                panel.refreshTable();
                assertEquals(4, ((JComboBox<?>) field(panel, "employeeToReplaceBox")).getItemCount());
                while (!f.personnel.isEmpty()) f.personnel.removeEmployee(0);
                panel.refreshTable(); assertEquals(0, ((JComboBox<?>) field(panel, "employeeToReplaceBox")).getItemCount());
                ((JTextField) field(panel, "startDateField")).setText(gestion.util.FormatUtils.formatDate(f.future));
                ((JTextField) field(panel, "endDateField")).setText(gestion.util.FormatUtils.formatDate(f.future));
                ((JTextField) field(panel, "startTimeField")).setText("08h00");
                ((JTextField) field(panel, "endTimeField")).setText("10h00");
                invoke(panel, "addReplacement");
                assertEquals(1, errors.size()); assertTrue(f.replacements.isEmpty());
            } catch (Exception e) { throw new AssertionError(e); }
        });
    }

    @Test void pastAndClientOverlapRequireConfirmationButRemainAllowedAfterConsent() throws Exception {
        f.add(f.a, f.future, 8, 10);
        Intervention candidate = new Intervention(f.client, f.b, f.future, LocalTime.of(8, 0), f.future, LocalTime.of(10, 0));
        List<String> confirmations = new ArrayList<>();
        ActionDialogs.confirmHandler = (parent, message) -> { confirmations.add(message); return false; };
        assertFalse(ActionDialogs.confirmSaving(null, f.planning, List.of(candidate), null, false)); assertEquals(1, confirmations.size());
        ActionDialogs.confirmHandler = (parent, message) -> true;
        assertTrue(ActionDialogs.confirmSaving(null, f.planning, List.of(candidate), null, false)); f.planning.addIntervention(candidate);
        LocalDate yesterday = LocalDate.now().minusDays(1);
        Intervention past = new Intervention(f.client, null, yesterday, LocalTime.of(8, 0), yesterday, LocalTime.of(10, 0));
        ActionDialogs.confirmHandler = (parent, message) -> false;
        assertFalse(ActionDialogs.confirmSaving(null, f.planning, List.of(past), null, false));
        assertTrue(ActionDialogs.confirmSaving(null, f.planning, List.of(past), null, true));
    }
}
