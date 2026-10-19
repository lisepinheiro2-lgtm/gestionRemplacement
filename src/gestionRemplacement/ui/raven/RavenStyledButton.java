package gestionRemplacement.ui.raven;

import java.awt.Color;
import javax.swing.JButton;
import javax.swing.UIManager;

/**
 * Reusable RavenGallery adapter; original Raven sources retain their MIT
 * license.
 */
public class RavenStyledButton extends JButton {
    private RavenTone tone = RavenTone.PRIMARY;

    public RavenStyledButton() {
        super("Bouton Raven");
        applyTone();
    }

    public RavenTone getTone() {
        return tone;
    }

    public void setTone(RavenTone value) {
        tone = value == null ? RavenTone.PRIMARY : value;
        applyTone();
    }

    public void updateUI() {
        super.updateUI();
        if (tone != null)
            applyTone();
    }

    private void applyTone() {
        String color = tone.color();
        putClientProperty("FlatLaf.style", "background:" + (tone == RavenTone.GHOST ? "null" : color)
                + ";margin:5,10,5,10;arc:10;borderWidth:0;focusWidth:3");
        if (tone != RavenTone.NEUTRAL && tone != RavenTone.GHOST)
            setForeground(tone == RavenTone.WARNING ? Color.BLACK : Color.WHITE);
        else
            setForeground(UIManager.getColor("Button.foreground"));
    }
}
