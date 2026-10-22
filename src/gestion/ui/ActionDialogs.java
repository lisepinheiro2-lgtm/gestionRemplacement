package gestion.ui;

import java.awt.Component;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import javax.swing.JOptionPane;
import gestion.model.Intervention;
import gestion.service.PlanningService;
import gestion.util.DomainValidation;

public final class ActionDialogs {

	private ActionDialogs() {
	}

	static BiConsumer<Component, String> errorHandler = (parent, message) -> JOptionPane.showMessageDialog(parent,
			message, "Opération refusée", JOptionPane.ERROR_MESSAGE);
	static BiPredicate<Component, String> confirmHandler = (parent, message) -> JOptionPane.showConfirmDialog(parent,
			message, "Confirmer", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;

	public static void error(Component parent, Exception failure) {
		String message = failure.getMessage();

		if (failure instanceof IOException || failure instanceof UncheckedIOException) {
			message = "L'enregistrement a échoué. L'opération n'a pas été confirmée.";

			for (Throwable cause = failure; cause != null; cause = cause.getCause())
				if (cause.getMessage() != null && (cause.getMessage().contains("Compensation incomplète")
						|| cause.getMessage().contains("Récupération incomplète")))
					message = "Récupération incomplète : fermez l'application et conservez le journal local avant toute nouvelle modification.";
		}

		errorHandler.accept(parent, message);
	}

	public static boolean confirm(Component parent, String message) {
		return confirmHandler.test(parent, message);
	}

	public static boolean confirmSaving(Component parent, PlanningService planning, List<Intervention> candidates,
			Intervention ignored, boolean pastAlreadyConfirmed) {
		candidates.forEach(DomainValidation::intervention);

		if (!pastAlreadyConfirmed
				&& candidates.stream()
						.anyMatch(i -> DomainValidation.intervention(i).start().isBefore(LocalDateTime.now()))
				&& !confirm(parent, "Une ou plusieurs interventions débutent dans le passé. Voulez-vous continuer ?"))
			return false;

		boolean clientOverlap = candidates.stream().anyMatch(i -> planning.hasOverlap(i.getClient(), i, ignored));

		return !clientOverlap || confirm(parent,
				"Une autre intervention du même client chevauche au moins un créneau. Plusieurs salariés simultanés sont autorisés pour le client. Voulez-vous continuer ?");
	}
}
