package gestion.ui.personnel;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.formdev.flatlaf.icons.FlatSearchIcon;

import gestion.model.DateRange;
import gestion.model.Employee;
import gestion.model.Intervention;
import gestion.service.InterventionAssignmentService;
import gestion.service.InterventionAssignmentService.Plan;
import gestion.service.PlanningService;
import gestion.service.RecurringInterventionService;
import gestion.service.WorkHoursService;
import gestion.ui.UIUtils;

public class PersonnelInterventionsDialog {

	private final PlanningService planning;
	private final Runnable onChange;
	private final InterventionAssignmentService assignments;
	private final WorkHoursService hours;
	private final PersonnelInterventionsTable[] tables = new PersonnelInterventionsTable[3];
	private JDialog dialog;
	private AssignmentImpactPanel impact;
	private JButton validate;
	private JTextField search;
	private Timer searchDelay;
	private Employee employee;
	private DateRange period;
	private int activeTab;
	private boolean refreshing;

	public PersonnelInterventionsDialog(PlanningService planning, Runnable onChange) {
		this(planning, null, onChange);
	}

	public PersonnelInterventionsDialog(PlanningService planning, RecurringInterventionService recurring,
			Runnable onChange) {
		this.planning = planning;
		this.onChange = onChange;
		assignments = new InterventionAssignmentService(planning, recurring);
		hours = new WorkHoursService(planning);
	}

	public void open(Employee employee, DateRange period, boolean assignedHours, Component parent) {
		this.employee = employee;
		this.period = period;
		dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Interventions",
				ModalityType.APPLICATION_MODAL);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		dialog.setContentPane(createContent(assignedHours));
		dialog.setMinimumSize(new Dimension(860, 560));
		dialog.setPreferredSize(new Dimension(1140, 730));
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		Timer availability = new Timer(1000, event -> refreshAvailability());
		availability.start();

		try {
			dialog.setVisible(true);
		} finally {
			availability.stop();
			searchDelay.stop();
		}
	}

	private void refreshAvailability() {
		boolean changed = false;
		refreshing = true;

		try {
			for (PersonnelInterventionsTable table : tables)
				changed |= table.hideStartedInterventions();
		} finally {
			refreshing = false;
		}

		if (changed)
			updateImpact();
	}

	private JPanel createContent(boolean assignedHours) {

		JPanel content = new JPanel(new BorderLayout(0, 4));
		content.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));
		CardLayout layout = new CardLayout();
		JPanel cards = new JPanel(layout);
		JPanel tabs = new JPanel(new GridLayout(1, 3, 18, 0));
		tabs.setBorder(BorderFactory.createEmptyBorder(2, 0, 6, 0));
		String[] labels = { "Libres", "Attribuées à " + employee, "Attribuées à d'autres employés" };
		ButtonGroup group = new ButtonGroup();
		JToggleButton[] buttons = new JToggleButton[3];

		for (int index = 0; index < tables.length; index++) {
			int tab = index;
			tables[index] = new PersonnelInterventionsTable(planning, index == 2, index != 1);
			tables[index].getSelectionModel().addListSelectionListener(event -> {
				if (!event.getValueIsAdjusting() && !refreshing)
					updateImpact();
			});
			tables[index].getRowSorter().addRowSorterListener(event -> SwingUtilities.invokeLater(() -> {
				if (!refreshing)
					updateImpact();
			}));
			JScrollPane scroll = new JScrollPane(tables[index]);
			scroll.setColumnHeaderView(tables[index].getTableHeader());
			cards.add(scroll, Integer.toString(index));

			buttons[index] = new JToggleButton(labels[index]);
			UIUtils.styleRoundedButton(buttons[index]);
			buttons[index].setPreferredSize(new Dimension(0, 40));
			buttons[index].addActionListener(event -> {
				activeTab = tab;
				layout.show(cards, Integer.toString(tab));
				updateImpact();
			});
			group.add(buttons[index]);
			tabs.add(buttons[index]);
		}

		content.add(tabs, BorderLayout.NORTH);
		content.add(cards, BorderLayout.CENTER);
		content.add(createFooter(), BorderLayout.SOUTH);
		activeTab = assignedHours ? 1 : 0;
		buttons[activeTab].setSelected(true);
		layout.show(cards, Integer.toString(activeTab));
		refreshTables();
		UIUtils.applyTheme(content);
		return content;
	}

	private JPanel createFooter() {
		JPanel footer = new JPanel(new BorderLayout(0, 8));
		impact = new AssignmentImpactPanel(hours, 116);
		impact.addHeaderAccessory(createSearchField());
		footer.add(impact, BorderLayout.CENTER);
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 8));
		validate = new JButton();
		JButton cancel = new JButton("Annuler");
		UIUtils.styleRoundedButton(validate);
		UIUtils.styleRoundedButton(cancel);
		validate.addActionListener(event -> validateSelection());
		cancel.addActionListener(event -> dialog.dispose());
		actions.add(validate);
		actions.add(cancel);
		footer.add(actions, BorderLayout.SOUTH);
		return footer;
	}

	private JTextField createSearchField() {
		search = new JTextField(20);
		search.putClientProperty("JComponent.roundRect", true);
		search.putClientProperty("FlatLaf.style", "borderWidth: 1.5; "
				+ "[dark]borderColor: #648D9F; [light]borderColor: #5E879C; "
				+ "[dark]focusedBorderColor: #62D3FF; [light]focusedBorderColor: #087FAC; focusColor: #339FCA");
		search.putClientProperty("JTextField.leadingIcon", new AzureSearchIcon());
		search.putClientProperty("JTextField.placeholderText", "Date, nom ou prénom…");
		search.putClientProperty("JTextField.showClearButton", false);
		search.setToolTipText("Recherche sur toutes les interventions futures ; champ vide : période sélectionnée. "
				+ "Client ou intervenant : nom et prénom dans les deux ordres. "
				+ "Jour seul : 1 à 31, avec ou sans zéro initial. "
				+ "Dates : dd/MM, dd-MM, dd/MM/aaaa, dd-MM-aaaa, dd/MM/aa ou dd-MM-aa.");
		search.getAccessibleContext().setAccessibleName("Rechercher une intervention par date ou par nom");
		searchDelay = new Timer(150, event -> refreshTables());
		searchDelay.setRepeats(false);
		UIUtils.addTextChangeListener(search, searchDelay::restart);
		search.addActionListener(event -> {
			searchDelay.stop();
			refreshTables();
		});
		return search;
	}

	private Employee destination() {
		return activeTab == 1 ? null : employee;
	}

	private void updateImpact() {
		if (impact == null)
			return;

		List<Intervention> selected = tables[activeTab].selectedInterventions();
		impact.showImpact(selected, destination(), period, "occurrences sélectionnées");
		validate.setText(switch (activeTab) {
		case 0 -> "Attribuer à " + employee;
		case 1 -> "Désattribuer";
		default -> "Réattribuer à " + employee;
		});
		validate.setEnabled(!selected.isEmpty());
	}

	private void refreshTables() {
		refreshing = true;

		try {
			List<Intervention> matching = matchingInterventions();

			tables[0].showInterventions(matching.stream()
					.filter(intervention -> intervention.getEmployee() == null).toList());
			tables[1].showInterventions(matching.stream()
					.filter(intervention -> intervention.getEmployee() == employee).toList());
			tables[2].showInterventions(matching.stream()
					.filter(intervention -> intervention.getEmployee() != null && intervention.getEmployee() != employee)
					.toList());
		} finally {
			refreshing = false;
		}

		updateImpact();
	}

	private List<Intervention> matchingInterventions() {
		String query = search.getText().strip();
		List<Intervention> source = query.isEmpty()
				? planning.getInterventionsForPeriod(period) : planning.getInterventions();
		LocalDateTime now = LocalDateTime.now();
		var filter = InterventionSearch.filter(query);

		return source.stream()
				.filter(intervention -> InterventionAssignmentService.isModifiable(intervention, now))
				.filter(filter)
				.toList();
	}

	private void validateSelection() {
		searchDelay.stop();
		refreshTables();
		List<Intervention> selected = tables[activeTab].selectedInterventions();

		if (selected.isEmpty())
			return;

		try {
			Plan initial = assignments.preview(selected, destination(), false);
			boolean recurring = selected.stream().anyMatch(i -> i.getRecurringId() != null);
			Plan plan = recurring ? reviewSelection(selected) : initial;

			if (plan == null)
				return;

			assignments.apply(plan);
			onChange.run();
			refreshTables();
		} catch (IllegalArgumentException | IllegalStateException exception) {
			refreshTables();
			JOptionPane.showMessageDialog(dialog, exception.getMessage(), "Opération impossible",
					JOptionPane.ERROR_MESSAGE);
		} catch (UncheckedIOException exception) {
			JOptionPane.showMessageDialog(dialog,
					"La sauvegarde a échoué. Aucun changement du lot n'a été conservé.",
					"Échec de l'opération", JOptionPane.ERROR_MESSAGE);
		}
	}

	private Plan reviewSelection(List<Intervention> selected) {
		boolean recurring = selected.stream().anyMatch(i -> i.getRecurringId() != null);
		JDialog warning = new JDialog(dialog, recurring
				? "Interventions récurrentes — choix obligatoire" : "Conflits d'attribution", true);
		warning.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		Plan[] result = { null };
		AssignmentReviewPanel review = new AssignmentReviewPanel(assignments, hours, selected, destination(), period,
				plan -> {
					result[0] = plan;
					warning.dispose();
				}, warning::dispose);
		warning.setContentPane(review);
		warning.pack();
		Rectangle screen = warning.getGraphicsConfiguration().getBounds();
		Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(warning.getGraphicsConfiguration());
		int width = Math.min(1060, screen.width - insets.left - insets.right - 24);
		int height = Math.min(680, screen.height - insets.top - insets.bottom - 24);
		warning.setMinimumSize(new Dimension(Math.min(920, width), Math.min(580, height)));
		warning.setSize(width, height);
		warning.setLocationRelativeTo(dialog);
		warning.setVisible(true);
		return result[0];
	}

	private static final class AzureSearchIcon extends FlatSearchIcon {

		private static final Color DARK_AZURE = new Color(0x62D3FF);
		private static final Color LIGHT_AZURE = new Color(0x087FAC);

		AzureSearchIcon() {
			super(true);
		}

		@Override
		protected void paintIcon(Component component, Graphics2D graphics) {
			boolean dark = UIUtils.isDarkMode();
			searchIconColor = dark ? DARK_AZURE : LIGHT_AZURE;
			Graphics2D glow = (Graphics2D) graphics.create();

			try {
				glow.setComposite(AlphaComposite.SrcOver.derive(dark ? 0.10f : 0.06f));
				glow.translate(-0.65, 0);
				super.paintIcon(component, glow);
				glow.translate(1.3, 0);
				super.paintIcon(component, glow);
				glow.translate(-0.65, -0.65);
				super.paintIcon(component, glow);
				glow.translate(0, 1.3);
				super.paintIcon(component, glow);
			} finally {
				glow.dispose();
			}

			super.paintIcon(component, graphics);
		}
	}
}
