package gestion;

import java.io.IOException;
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
        } catch (IOException exception) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null,
                    "Impossible de charger les absences : " + exception.getMessage(), "Erreur de lecture",
                    JOptionPane.ERROR_MESSAGE));
        }
    }
}
