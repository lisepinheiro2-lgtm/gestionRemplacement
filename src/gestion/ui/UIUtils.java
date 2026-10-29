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
		javax.swing.UIManager.put("defaultFont", new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
		for (String key : new String[] { "Panel.background", "OptionPane.background", "Viewport.background", "Table.background" })
			javax.swing.UIManager.put(key, getBackgroundColor());
		for (String key : new String[] { "Button.background", "ToggleButton.background", "TextField.background", "ComboBox.background" })
			javax.swing.UIManager.put(key, getSurfaceColor());
		javax.swing.UIManager.put("Table.alternateRowColor", new Color(darkMode ? 0x1D2735 : 0xEDF3FA));
		javax.swing.UIManager.put("Component.borderColor", getBorderColor());
		javax.swing.UIManager.put("Component.focusColor", getTurquoiseColor());
		javax.swing.UIManager.put("Button.arc", 14);
		javax.swing.UIManager.put("Component.arc", 12);
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
			return new Color(23, 30, 42);
		}

		return new Color(244, 247, 251);
	}

	public static Color getTextColor() {
		if (darkMode) {
			return new Color(237, 242, 249);
		}

		return new Color(30, 43, 63);
	}

	public static Color getPlaceholderColor() {
		if (darkMode) {
			return new Color(159, 174, 194);
		}

		return new Color(102, 118, 139);
	}

	public static Color getSurfaceColor() {
		return new Color(darkMode ? 0x212A38 : 0xFFFFFF);
	}

	public static Color getBorderColor() {
		return new Color(darkMode ? 0x333F50 : 0xDEE5EE);
	}

	public static Color getTurquoiseColor() {
		return new Color(darkMode ? 0x67D9CD : 0x087F78);
	}

	public static Color getVioletColor() {
		return new Color(darkMode ? 0xC5A0ED : 0x7845AE);
	}

	public static Color getChampagneColor() {
		return new Color(darkMode ? 0xDFC391 : 0x88642A);
	}

	public static String hex(Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	public static void styleActionButton(AbstractButton button, String tone) {
		button.putClientProperty("gestion.tone", tone);
		button.putClientProperty("JButton.buttonType", null);
		Color accent = "primary".equals(tone) ? getTurquoiseColor()
				: "danger".equals(tone) ? getVioletColor()
				: "warning".equals(tone) ? getChampagneColor() : getTextColor();
		button.putClientProperty("FlatLaf.style", "arc:14;margin:6,12,6,12;borderWidth:1;focusWidth:1;"
				+ "innerFocusWidth:0;background:" + hex(getSurfaceColor()) + ";foreground:" + hex(accent)
				+ ";borderColor:" + hex("neutral".equals(tone) ? getBorderColor() : accent)
				+ ";hoverBackground:" + hex(new Color(darkMode ? 0x283446 : 0xF5F9FF))
				+ ";focusedBorderColor:" + hex(getTurquoiseColor()));
		button.setOpaque(false);
		button.setForeground(accent);
		String label = button.getText() == null ? "" : button.getText();
		NavigationIcon.Kind kind = label.startsWith("Ajouter") ? NavigationIcon.Kind.ADD
				: label.startsWith("Supprimer") ? NavigationIcon.Kind.DELETE
				: label.startsWith("Attribuer") || label.startsWith("Réattribuer") ? NavigationIcon.Kind.REPLACEMENTS : null;
		if (button.getClientProperty("gestion.icon") instanceof NavigationIcon.Kind explicit)
			kind = explicit;
		if (kind != null)
			button.setIcon(new NavigationIcon(kind, 18, accent));
	}

	public static void styleInterventionButton(JButton button, boolean assigned) {
		button.putClientProperty("interventionAssigned", assigned);
		Color accent = assigned ? getTurquoiseColor() : getVioletColor();
		Color background = new Color(darkMode ? (assigned ? 0x244C50 : 0x44345C)
				: (assigned ? 0xD5F3EE : 0xEBDDF9));
		button.putClientProperty("FlatLaf.style", "arc:10;margin:2,4,2,4;borderWidth:1;focusWidth:1;"
				+ "background:" + hex(background) + ";foreground:" + hex(getTextColor())
				+ ";borderColor:" + hex(accent) + ";focusedBorderColor:" + hex(accent));
		button.setBackground(background);
		button.setForeground(getTextColor());
	}

	public static javax.swing.border.Border panelBorder(String title) {
		return javax.swing.BorderFactory.createCompoundBorder(
				javax.swing.BorderFactory.createTitledBorder(
						javax.swing.BorderFactory.createLineBorder(getBorderColor(), 1, true), title),
				javax.swing.BorderFactory.createEmptyBorder(10, 14, 12, 14));
	}

	public static JButton createSettingsButton(Component parent) {
		JButton settingsButton = new JButton();
		settingsButton.putClientProperty("gestion.icon", NavigationIcon.Kind.SETTINGS);
		styleActionButton(settingsButton, "neutral");
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
		String type = component.getClass().getName();
		if (type.startsWith("com.formdev.flatlaf.ui.") || type.startsWith("javax.swing.plaf."))
			return;

		if (component instanceof AppThemeAware themed) {
			themed.applyAppTheme(darkMode);
			return;
		}

		if (component instanceof JPanel || component instanceof JViewport)
			component.setBackground(getBackgroundColor());

		if (component instanceof javax.swing.JComponent styled) {
			Object title = styled.getClientProperty("gestion.panelTitle");
			if (title instanceof String text)
				styled.setBorder(panelBorder(text));
		}

		if (component instanceof JComboBox<?> comboBox) {
			comboBox.putClientProperty("JComponent.roundRect", true);
			comboBox.setBackground(getSurfaceColor());
			comboBox.setForeground(getTextColor());
		}

		if (component instanceof JTextComponent field) {
			field.setBackground(getSurfaceColor());
			field.setForeground(getTextColor());
			field.setCaretColor(getTextColor());
			field.setSelectionColor(new Color(darkMode ? 0x304D68 : 0xD8EAFE));
			field.setSelectedTextColor(getTextColor());
			field.putClientProperty("JComponent.roundRect", true);
		}

		if (component instanceof JList<?> list) {
			list.setBackground(getSurfaceColor());
			list.setForeground(getTextColor());
			list.setSelectionBackground(new Color(darkMode ? 0x304D68 : 0xD8EAFE));
			list.setSelectionForeground(getTextColor());
		}

		if (component instanceof JLabel)
			component.setForeground(getTextColor());

		if (component instanceof JButton button) {
			Object assigned = button.getClientProperty("interventionAssigned");
			if (assigned instanceof Boolean value) {
				styleInterventionButton(button, value);
			} else {
				Object tone = button.getClientProperty("gestion.tone");
				String label = button.getText() == null ? "" : button.getText();
				String defaultTone = label.startsWith("Supprimer") || label.equals("Désattribuer") ? "danger"
						: label.startsWith("Ajouter") || label.startsWith("Attribuer") || label.startsWith("Réattribuer")
						|| label.contains("confirmer") || label.equals("Confirmer") ? "primary" : "neutral";
				styleActionButton(button, tone instanceof String text ? text : defaultTone);
			}
		}

		if (component instanceof javax.swing.JToggleButton toggle
				&& !(toggle instanceof JCheckBox) && !(toggle instanceof javax.swing.JRadioButton)) {
			styleActionButton(toggle, toggle.isSelected() ? "primary" : "neutral");
			if (toggle.getClientProperty("gestion.selectionStyle") == null) {
				toggle.putClientProperty("gestion.selectionStyle", true);
				toggle.addItemListener(event -> styleActionButton(toggle, toggle.isSelected() ? "primary" : "neutral"));
			}
		}

		if (component instanceof JTable table) {
			table.setBackground(new javax.swing.plaf.ColorUIResource(getBackgroundColor()));
			table.setForeground(getTextColor());
			table.setSelectionBackground(new Color(darkMode ? 0x304D68 : 0xD8EAFE));
			table.setSelectionForeground(getTextColor());
			table.setGridColor(getBorderColor());
			table.setShowHorizontalLines(false);
			table.setShowVerticalLines(true);
			table.setIntercellSpacing(new Dimension(1, 0));
			table.setRowHeight(Math.max(28, table.getRowHeight()));

			table.getTableHeader().setBackground(getSurfaceColor());
			table.getTableHeader().setForeground(getTextColor());
			table.getTableHeader().setPreferredSize(new Dimension(0, 32));
		}

		if (component instanceof javax.swing.JScrollPane scroll)
			scroll.setBorder(javax.swing.BorderFactory.createLineBorder(getBorderColor(), 1, true));

		if (component instanceof Container container)
			for (Component child : container.getComponents())
				applyTheme(child);
	}

	@SuppressWarnings("rawtypes")
	public static boolean updateTableRows(javax.swing.table.DefaultTableModel model, java.util.List<Object[]> rows) {
		java.util.Vector<java.util.Vector> updated = new java.util.Vector<>(rows.size());
		for (Object[] row : rows) {
			if (row.length != model.getColumnCount())
				throw new IllegalArgumentException("Nombre de colonnes incohérent.");
			updated.add(new java.util.Vector<>(java.util.Arrays.asList(row)));
		}

		if (model.getDataVector().equals(updated))
			return false;

		model.getDataVector().clear();
		model.getDataVector().addAll(updated);
		model.fireTableDataChanged();
		return true;
	}

	public static void configureTableViewport(JTable table, javax.swing.JScrollPane scroll) {
		Runnable resize = () -> {
			int minimum = 0;
			for (int column = 0; column < table.getColumnCount(); column++)
				minimum += table.getColumnModel().getColumn(column).getMinWidth();
			int mode = scroll.getViewport().getWidth() >= minimum
					? JTable.AUTO_RESIZE_ALL_COLUMNS : JTable.AUTO_RESIZE_OFF;
			if (table.getAutoResizeMode() != mode)
				table.setAutoResizeMode(mode);
		};
		scroll.getViewport().addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				resize.run();
			}
		});
		resize.run();
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
		case IN_PROGRESS, TARGET_REACHED -> getTurquoiseColor();
		case WATCH -> getChampagneColor();
		case ALERT -> getVioletColor();
		};
	}

	public static String getHoursLegend() {
		return "<html><font color='" + hex(getTurquoiseColor())
				+ "'>●</font> Attribution en bonne voie ou objectif atteint<br><font color='"
				+ hex(getChampagneColor()) + "'>●</font> Attribution à surveiller<br><font color='"
				+ hex(getVioletColor()) + "'>●</font> Attribution urgente ou dépassement des heures prévues</html>";
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
