package gestion.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.prefs.Preferences;

import javax.swing.AbstractButton;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
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
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.text.JTextComponent;
import gestion.model.EmployeeStatus;
import java.awt.Graphics2D;
import gestion.model.HoursAlert;

public class UIUtils {

	private static final Preferences preferences = Preferences.userRoot().node("/gestionRemplacement");
	private static boolean darkMode = preferences.getBoolean("darkMode", false);

	public static boolean isDarkMode() {
		return darkMode;
	}

	public static void installLookAndFeel() {
		if (darkMode) {
			com.formdev.flatlaf.FlatDarkLaf.setup();
		} else {
			com.formdev.flatlaf.FlatLightLaf.setup();
		}
		javax.swing.UIManager.put("defaultFont", new java.awt.Font(java.awt.Font.DIALOG, java.awt.Font.PLAIN, 13));
	}

	public static void setDarkMode(boolean enabled) {
		if (darkMode == enabled)
			return;

		darkMode = enabled;
		preferences.putBoolean("darkMode", darkMode);
		installLookAndFeel();

		for (Window window : Window.getWindows()) {
			javax.swing.SwingUtilities.updateComponentTreeUI(window);
			applyTheme(window);
			window.revalidate();
			window.repaint();
		}
	}

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
		settingsButton.setFont(settingsButton.getFont().deriveFont(24f));
		settingsButton.setPreferredSize(new Dimension(60, 40));
		settingsButton.setToolTipText("Paramètres");

		settingsButton.addActionListener(event -> {
			JCheckBox darkModeBox = new JCheckBox("Mode sombre", darkMode);
			int result = JOptionPane.showConfirmDialog(parent, darkModeBox, "Paramètres", JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE);

			if (result == JOptionPane.OK_OPTION) {
				setDarkMode(darkModeBox.isSelected());
			}
		});

		return settingsButton;
	}

	public static void applyTheme(Component component) {

		if (component instanceof AppThemeAware themed) {
			themed.applyAppTheme(darkMode);
			return;
		}

		if (component instanceof JPanel || component instanceof JViewport) {
			component.setBackground(getBackgroundColor());
		}

		if (component instanceof JComboBox<?>) {
			JComboBox<?> comboBox = (JComboBox<?>) component;

			comboBox.setUI(new BasicComboBoxUI() {
				@Override
				protected void installDefaults() {
					super.installDefaults();
					squareButton = false;
				}

				@Override
				protected JButton createArrowButton() {
					JButton arrow = super.createArrowButton();
					arrow.setBorder(javax.swing.BorderFactory.createEmptyBorder());
					arrow.setPreferredSize(new Dimension(22, 18));
					return arrow;
				}

				@Override
				public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
					Color previousColor = g.getColor();
					g.setColor(comboBox.getBackground());
					g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

					g.setColor(previousColor);
				}

			});

			comboBox.setBackground(getBackgroundColor());
			comboBox.setForeground(getTextColor());
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

		if (component instanceof JButton button) {

			if ("roundRect".equals(button.getClientProperty("JButton.buttonType"))) {
				button.setOpaque(false);
				button.setForeground(getTextColor());

			} else {
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

	public static void configureResponsiveComboBox(JComboBox<String> comboBox, JPanel titlePanel) {

		comboBox.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {

				list.setBackground(getBackgroundColor());
				list.setForeground(getTextColor());
				list.setSelectionBackground(darkMode ? new Color(65, 85, 115) : new Color(190, 215, 245));
				list.setSelectionForeground(getTextColor());
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				String text = value == null ? "" : value.toString();
				setFont(comboBox.getFont());
				int availableWidth = comboBox.getWidth() - comboBox.getInsets().left - comboBox.getInsets().right
						- getInsets().left - getInsets().right - 4;

				for (Component component : comboBox.getComponents()) {
					if (component instanceof JButton) {
						availableWidth -= component.getWidth();
					}

				}

				if (comboBox.getWidth() > 0) {
					int textWidth = Math.max(1, availableWidth);
					setText("<html><table width='" + textWidth + "' cellpadding='0' cellspacing='0'><tr><td>" + text
							+ "</td></tr></table></html>");
				} else {
					setText(text);
				}

				return this;
			}
		});

		comboBox.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				updateResponsiveComboBoxHeight(comboBox, titlePanel);
			}
		});
	}

	public static Color getHoursColor(HoursAlert alert) {
		return switch (alert) {
		case IN_PROGRESS, TARGET_REACHED -> new Color(0x35D47B);
		case WATCH -> new Color(0xFB8C00);
		case ALERT -> new Color(0xE53935);
		};
	}

	public static String getHoursLegend() {
		return "<html>" + "<font color='#35D47B'>●</font> Attribution en bonne voie ou objectif atteint<br>"
				+ "<font color='#FB8C00'>●</font> Attribution à surveiller<br>"
				+ "<font color='#E53935'>●</font> Attribution urgente ou dépassement des heures prévues" + "</html>";
	}

	private static void updateResponsiveComboBoxHeight(JComboBox<String> comboBox, JPanel titlePanel) {

		if (comboBox.getWidth() == 0) {
			return;
		}

		JList<String> measurementList = new JList<>();
		measurementList.setFont(comboBox.getFont());
		int maxHeight = 0;

		for (int i = 0; i < comboBox.getItemCount(); i++) {
			Component rendered = comboBox.getRenderer().getListCellRendererComponent(measurementList,
					comboBox.getItemAt(i), -1, false, false);
			maxHeight = Math.max(maxHeight, rendered.getPreferredSize().height);
		}

		int height = maxHeight + comboBox.getInsets().top + comboBox.getInsets().bottom + 4;
		comboBox.setPreferredSize(new Dimension(0, height));
		int headerHeight = titlePanel.getLayout().preferredLayoutSize(titlePanel).height;

		if (titlePanel.getPreferredSize().height != headerHeight) {
			titlePanel.setPreferredSize(new Dimension(0, headerHeight));
			titlePanel.revalidate();
		}

		comboBox.repaint();
	}

	public static void drawPastStripes(Graphics g, int width, int height) {
		Graphics stripesGraphics = g.create();
		stripesGraphics.setColor(new Color(128, 128, 128, 150));

		for (int x = -height; x < width; x += 14) {
			stripesGraphics.drawLine(x, 0, x + height, height);
		}

		stripesGraphics.dispose();
	}

	public static void styleRoundedButton(AbstractButton button) {
		button.putClientProperty("JButton.buttonType", "roundRect");
		button.setFont(button.getFont().deriveFont(15f));
		button.setMargin(new Insets(6, 16, 6, 16));
	}

	public static Color getStatusGlowColor(EmployeeStatus status) {
		return switch (status) {
		case OPERATIONAL -> new Color(0x35D47B);
		case VACATION, SICK_LEAVE -> new Color(0xE64B55);
		case OTHER_ABSENCE -> new Color(0xFFB74D);
		};
	}

	public static void paintInnerGlow(Graphics2D g, Color color, int width, int height) {

		if (color == null)
			return;

		int x = 6;
		int y = 3;
		int glowWidth = width - 12;
		int glowHeight = height - 6;

		if (glowWidth <= 10 || glowHeight <= 10)
			return;

		g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 20));
		g.fillRoundRect(x, y, glowWidth, glowHeight, 16, 16);

		for (int i = 0; i < 4; i++) {

			g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60 - i * 12));
			g.drawRoundRect(x + i, y + i, glowWidth - 2 * i - 1, glowHeight - 2 * i - 1, 16, 16);
		}
	}
}
