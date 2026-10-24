package gestion.app;

import java.io.IOException;
import java.nio.file.Path;

import gestion.repository.AbsenceCsvRepository;
import gestion.repository.ClientCsvRepository;
import gestion.repository.CsvIdentityMigration;
import gestion.repository.CsvStore;
import gestion.repository.EmployeeCsvRepository;
import gestion.repository.InterventionCsvRepository;
import gestion.repository.RecurringInterventionCsvRepository;
import gestion.repository.ReplacementCsvRepository;
import gestion.service.AbsenceService;
import gestion.service.ClientService;
import gestion.service.PersonnelService;
import gestion.service.PlanningService;
import gestion.service.RecurringInterventionService;
import gestion.service.ReplacementService;
import gestion.ui.MainUI;

public final class ApplicationContext {
    private final PersonnelService personnel;
    private final ClientService clients;
    private final PlanningService planning;
    private final RecurringInterventionService recurring;
    private final ReplacementService replacements;
    private final AbsenceService absences;

    public ApplicationContext(Path directory) throws IOException {

        CsvStore.recover(directory);
        CsvIdentityMigration.migrate(directory);

        personnel = new PersonnelService(new EmployeeCsvRepository(directory));
        clients = new ClientService(new ClientCsvRepository(directory));
        planning = new PlanningService(personnel, clients, new InterventionCsvRepository(directory));
        recurring = new RecurringInterventionService(clients, personnel,
                new RecurringInterventionCsvRepository(directory));
        replacements = new ReplacementService(new ReplacementCsvRepository(directory));

        for (var intervention : planning.getInterventions()) {

            String id = intervention.getRecurringId();

            if (id != null && recurring.getRecurringInterventions().stream().noneMatch(r -> id.equals(r.getId())))
                throw new IOException("interventions.csv : série référencée introuvable ; aucun fichier modifié.");
        }

        absences = new AbsenceService(personnel, replacements, planning, new AbsenceCsvRepository(directory));
    }

    public void openWindow() {
        new MainUI(personnel, clients, planning, replacements, recurring, absences);
    }
}
