package gestionRemplacement;

import java.awt.Color;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JTextField;

public class UIUtils {

	public static void addPlaceholder(JTextField field, String placeholder) {

		field.setText(placeholder);
		field.setForeground(Color.GRAY);

		field.addFocusListener(new FocusAdapter() {

			public void focusGained(FocusEvent e) {

				if (field.getText().equals(placeholder)) {
					field.setText("");
					field.setForeground(Color.BLACK);
				}
			}

			public void focusLost(FocusEvent e) {

				if (field.getText().isBlank()) {
					field.setText(placeholder);
					field.setForeground(Color.GRAY);
				}
			}
		});
	}
}
