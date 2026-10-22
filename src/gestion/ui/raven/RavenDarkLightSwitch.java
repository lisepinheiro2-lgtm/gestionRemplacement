package gestion.ui.raven;

import java.beans.Beans;
import javax.swing.JToggleButton;

/**
 * Reusable RavenGallery adapter; original Raven sources license.
 */
public class RavenDarkLightSwitch extends JToggleButton {
	private boolean changeApplicationTheme;

	public RavenDarkLightSwitch() {
		setToolTipText("Thème clair / sombre");
		setIcon(new gestion.ui.raven.DarkLightSwitchIcon());
		putClientProperty("FlatLaf.style", "arc:999;borderWidth:0;focusWidth:0;innerFocusWidth:0");
		setSelected(com.formdev.flatlaf.FlatLaf.isLafDark());
		addActionListener(e -> {

			if (!Beans.isDesignTime() && changeApplicationTheme) {
				if (isSelected())
					com.formdev.flatlaf.FlatDarkLaf.setup();
				else
					com.formdev.flatlaf.FlatLightLaf.setup();
				com.formdev.flatlaf.FlatLaf.updateUI();
			}
		});
	}

	public boolean isChangeApplicationTheme() {
		return changeApplicationTheme;
	}

	public void setChangeApplicationTheme(boolean value) {
		changeApplicationTheme = value;
	}
}
