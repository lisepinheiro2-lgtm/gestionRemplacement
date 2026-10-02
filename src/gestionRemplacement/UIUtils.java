package gestionRemplacement;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.prefs.Preferences;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.text.JTextComponent;

public class UIUtils {

	private static final Preferences preferences = Preferences.userNodeForPackage(UIUtils.class);

	private static boolean darkMode = preferences.getBoolean("darkMode", false);

	public static void addPlaceholder(JTextField field, String placeholder) {

		field.setText(placeholder);
		field.setForeground(getPlaceholderColor());

		field.addFocusListener(new FocusAdapter() {

			public void focusGained(FocusEvent e) {

				if (field.getText().equals(placeholder)) {
					field.setText("");
					field.setForeground(getTextColor());
				}
			}

			public void focusLost(FocusEvent e) {

				if (field.getText().isBlank()) {
					field.setText(placeholder);
					field.setForeground(getPlaceholderColor());
				}
			}
		});
	}

	public static void addTextChangeListener(JTextField field, Runnable action) {

		field.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				action.run();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				action.run();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				action.run();
			}
		});
	}

	public static Color getBackgroundColor() {
		if (darkMode) {
			return new Color(32, 35, 40);
		}
		return new Color(250, 251, 253);
	}

	public static Color getTextColor() {
		if (darkMode) {
			return new Color(230, 233, 238);
		}
		return new Color(35, 45, 55);
	}

	public static Color getPlaceholderColor() {
		if (darkMode) {
			return new Color(160, 165, 175);
		}
		return new Color(110, 115, 125);
	}

	public static void styleInterventionButton(JButton button, boolean assigned) {

		button.putClientProperty("interventionAssigned", assigned);
		Color background;

		if (darkMode) {
			background = assigned ? new Color(38, 70, 53) : new Color(90, 42, 47);
		} else {
			background = assigned ? new Color(220, 242, 225) : new Color(255, 228, 228);
		}

		button.setBackground(background);
		button.setForeground(getTextColor());
	}

	public static JButton createSettingsButton(Component parent) {

		JButton settingsButton = new JButton("⚙");
		settingsButton.setPreferredSize(new Dimension(60, 40));
		settingsButton.setToolTipText("Paramètres");
		settingsButton.addActionListener(event -> {

			JCheckBox darkModeBox = new JCheckBox("Mode sombre", darkMode);

			int result = JOptionPane.showConfirmDialog(parent, darkModeBox, "Paramètres", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE);

			if (result == JOptionPane.OK_OPTION) {
				darkMode = darkModeBox.isSelected();
				preferences.putBoolean("darkMode", darkMode);

				for (Window window : Window.getWindows()) {
					applyTheme(window);
					window.repaint();
				}
			}
		});

		return settingsButton;
	}

	public static void applyTheme(Component component) {

		if (component instanceof JPanel || component instanceof JViewport) {
			component.setBackground(getBackgroundColor());
		}

		if (component instanceof JTextComponent) {
			JTextComponent field = (JTextComponent) component;

			field.setBackground(getBackgroundColor());
			field.setForeground(getTextColor());
			field.setCaretColor(getTextColor());
			field.setSelectionColor(darkMode ? new Color(65, 85, 115) : new Color(190, 215, 245));
			field.setSelectedTextColor(getTextColor());
		}

		if (component instanceof JList<?>) {
			JList<?> list = (JList<?>) component;

			list.setBackground(getBackgroundColor());
			list.setForeground(getTextColor());

			list.setSelectionBackground(darkMode ? new Color(65, 85, 115) : new Color(190, 215, 245));

			list.setSelectionForeground(getTextColor());
		}

		if (component instanceof JLabel) {
			component.setForeground(getTextColor());
		}

		if (component instanceof JButton) {
			JButton button = (JButton) component;
			button.setUI(new BasicButtonUI());
			Object assigned = button.getClientProperty("interventionAssigned");

			if (assigned instanceof Boolean) {
				styleInterventionButton(button, (Boolean) assigned);
			} else {
				button.setBackground(darkMode ? new Color(48, 53, 61) : new Color(235, 239, 245));

				button.setForeground(getTextColor());
				button.setOpaque(true);
				button.setContentAreaFilled(true);
			}
		}

		if (component instanceof JTable) {
			JTable table = (JTable) component;

			table.setBackground(getBackgroundColor());
			table.setForeground(getTextColor());

			table.setSelectionBackground(darkMode ? new Color(65, 85, 115) : new Color(190, 215, 245));
			table.setSelectionForeground(getTextColor());

			table.setGridColor(darkMode ? new Color(65, 70, 80) : new Color(210, 215, 225));

			table.getTableHeader().setBackground(getBackgroundColor());
			table.getTableHeader().setForeground(getTextColor());
		}

		if (component instanceof Container) {
			Container container = (Container) component;

			for (Component child : container.getComponents()) {
				applyTheme(child);
			}
		}
	}

	public static void drawPastStripes(Graphics g, int width, int height) {

		Graphics stripesGraphics = g.create();
		stripesGraphics.setColor(new Color(128, 128, 128, 150));

		for (int x = -height; x < width; x += 14) {
			stripesGraphics.drawLine(x, 0, x + height, height);
		}

		stripesGraphics.dispose();
	}
}
