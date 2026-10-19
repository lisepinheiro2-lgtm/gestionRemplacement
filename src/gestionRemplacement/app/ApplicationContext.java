package gestionRemplacement.app;

import gestionRemplacement.repository.AbsenceCsvRepository;
import gestionRemplacement.repository.ClientCsvRepository;
import gestionRemplacement.repository.EmployeeCsvRepository;
import gestionRemplacement.repository.InterventionCsvRepository;
import gestionRemplacement.repository.RecurringInterventionCsvRepository;
import gestionRemplacement.repository.ReplacementCsvRepository;
import gestionRemplacement.service.AbsenceService;
import gestionRemplacement.service.ClientService;
import gestionRemplacement.service.PersonnelService;
import gestionRemplacement.service.PlanningService;
import gestionRemplacement.service.RecurringInterventionService;
import gestionRemplacement.service.ReplacementService;
import gestionRemplacement.ui.MainUI;
import java.io.IOException;
import java.nio.file.Path;

public final class ApplicationContext {
    private final PersonnelService personnel;
    private final ClientService clients;
    private final PlanningService planning;
    private final RecurringInterventionService recurring;
    private final ReplacementService replacements;
    private final AbsenceService absences;

    public ApplicationContext(Path directory) throws IOException {
        personnel = new PersonnelService(new EmployeeCsvRepository(directory));
        clients = new ClientService(new ClientCsvRepository(directory));
        planning = new PlanningService(personnel, clients, new InterventionCsvRepository(directory));
        recurring = new RecurringInterventionService(clients, personnel,
                new RecurringInterventionCsvRepository(directory));
        replacements = new ReplacementService(new ReplacementCsvRepository(directory));
        absences = new AbsenceService(personnel, replacements, new AbsenceCsvRepository(directory));
    }

    public void openWindow() {
        new MainUI(personnel, clients, planning, replacements, recurring, absences);
    }
}
