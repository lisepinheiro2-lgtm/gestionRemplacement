package gestionRemplacement.ui.raven;

/**
 * Reusable RavenGallery adapter; original Raven sources retain their MIT
 * license.
 */
public enum RavenTone {
    NEUTRAL, PRIMARY, SECONDARY, INFO, SUCCESS, WARNING, DANGER, GHOST;

    String color() {
        String[] light = { "#dddddd", "#3949AB", "#5E35B1", "#1E88E5", "#43A047", "#FFB300", "#E53935", "#dddddd" };
        String[] dark = { "#555555", "#5C6BC0", "#7E57C2", "#42A5F5", "#66BB6A", "#FFCA28", "#EF5350", "#555555" };
        return (com.formdev.flatlaf.FlatLaf.isLafDark() ? dark : light)[ordinal()];
    }
}
