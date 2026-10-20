package gestion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import gestion.app.ApplicationContext;


public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        try {
            ApplicationContext application = new ApplicationContext(Path.of("."));
            SwingUtilities.invokeLater(application::openWindow);
        } catch (IOException | UncheckedIOException | IllegalArgumentException exception) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Impossible de charger les données : " + exception.getMessage(), "Erreur de lecture",
                    JOptionPane.ERROR_MESSAGE));
        }
    }
}
