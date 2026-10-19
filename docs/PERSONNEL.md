[PERSONNEL.md](https://github.com/user-attachments/files/33687903/PERSONNEL.md)

# Page Personnel : carte des classes

Tous les noms ci-dessous correspondent à la version locale refactorisée. Les chemins sont relatifs à ce document.

## Commencer par les fonctionnalités

| Fonctionnalité | Chaîne à suivre |
|---|---|
| Ajouter, modifier, supprimer un employé | `PersonnelPanel` → `PersonnelController` → `PersonnelService` → `EmployeeCsvRepository` → `employees.csv` |
| Déclarer une absence ou une reprise | `EmployeeStatusDialog` → `PersonnelController` → `AbsenceService` → `AbsenceCsvRepository` et `ReplacementService` → `ReplacementCsvRepository` |
| Afficher les heures et le statut | `PersonnelController` → `WorkHoursService` / `AbsenceService` → `EmployeeSummary` → `PersonnelTableModel` |
| Changer de semaine ou de mois | `HoursPeriodSelector` → rafraîchissement de `PersonnelPanel` → `PersonnelController.getRows()` |
| Ouvrir la page | `HomePanel` / `HomeCardButton` → `MainUI` → `PersonnelPanel.refreshTable()` |

## Interface et contrôleur

| Classe | Rôle exact |
|---|---|
| [PersonnelPanel](../src/gestion/ui/personnel/PersonnelPanel.java) | Assemble formulaire et tableau ; transmet les actions au contrôleur et présente les erreurs. |
| [PersonnelController](../src/gestion/controller/PersonnelController.java) | Valide les noms, heures et dates ; ajoute, modifie ou supprime un employé ; change son statut ; construit les lignes calculées. Aucun composant Swing. |
| [PersonnelTableModel](../src/gestion/ui/personnel/PersonnelTableModel.java) | Adapte les résumés au tableau Swing ; les trois premières colonnes sont modifiables. Son interface interne `EditHandler` transmet les modifications. |
| [HoursPeriodSelector](../src/gestion/ui/personnel/HoursPeriodSelector.java) | Choisit semaine/mois, avance ou recule et affiche la période. Les colonnes attribuées/restantes ont chacune leur sélecteur indépendant. |
| [EmployeeStatusDialog](../src/gestion/ui/personnel/EmployeeStatusDialog.java) | Saisie du statut et des dates, affichage des erreurs de validation ou de sauvegarde. |

## Services, modèles et persistance

| Classe | Rôle exact |
|---|---|
| [PersonnelService](../src/gestion/service/PersonnelService.java) | Collection des employés, ajout, modification, suppression, recherche et tri. |
| [EmployeeCsvRepository](../src/gestion/repository/EmployeeCsvRepository.java) | Charge et sauvegarde `employees.csv`, avec compatibilité des lignes historiques à trois colonnes. |
| [Employee](../src/gestion/model/Employee.java) | Identité et contrat hebdomadaire ; conserve les anciens champs nécessaires au format CSV. |
| [AbsenceService](../src/gestion/service/AbsenceService.java) | Statut à une date, contrôle des chevauchements, ajout d’absence, reprise et synchronisation des demandes de remplacement. Rétablit la mémoire si la sauvegarde de l’absence échoue. |
| [AbsenceCsvRepository](../src/gestion/repository/AbsenceCsvRepository.java) | Charge et sauvegarde `employee_absences.csv` ; valide la structure des lignes et retrouve l’employé partagé. |
| [EmployeeAbsence](../src/gestion/model/EmployeeAbsence.java) | Identifiant d’absence, employé, statut et dates inclusives. |
| [EmployeeStatus](../src/gestion/model/EmployeeStatus.java) | Énumération : opérationnel, congés, maladie, autre absence. |
| [WorkHoursService](../src/gestion/service/WorkHoursService.java) | Calcule les heures hebdomadaires/mensuelles, les dépassements projetés et les alertes. |
| [PlanningService](../src/gestion/service/PlanningService.java) | Collection des interventions, affectations, contrôles de chevauchement, génération et suppression des occurrences. Fournit les interventions au calcul des heures. |
| [InterventionCsvRepository](../src/gestion/repository/InterventionCsvRepository.java) | Charge et sauvegarde `interventions.csv` avec les références aux employés et clients. |
| [Intervention](../src/gestion/model/Intervention.java) | Client, employé éventuellement affecté, début, fin et identifiant de série. |
| [HoursAlert](../src/gestion/model/HoursAlert.java) | Énumération des quatre niveaux d’alerte sur les heures. |
| [EmployeeSummary](../src/gestion/model/EmployeeSummary.java) | Résultat calculé pour l’affichage : employé, statut, heures attribuées/restantes et alertes des deux périodes. |
| [ReplacementService](../src/gestion/service/ReplacementService.java) | Collection des remplacements ; crée, actualise et retire les demandes liées aux absences. |
| [ReplacementCsvRepository](../src/gestion/repository/ReplacementCsvRepository.java) | Charge et sauvegarde `replacements.csv`, y compris le lien `absenceId`. |
| [Replacement](../src/gestion/model/Replacement.java) | Employé à remplacer, remplaçant éventuel, période, contrats et lien avec l’absence. |
| [CsvReferences](../src/gestion/repository/CsvReferences.java) | Résout les références historiques prénom/nom lors du chargement des absences, interventions et récurrences. |
| [FormatUtils](../src/gestion/util/FormatUtils.java) | Conversion des saisies de dates/heures et affichage des durées. |

## Usages indirects dans les autres fonctionnalités

| Classe | Lien avec le personnel |
|---|---|
| [Main](../src/gestion/Main.java), [ApplicationContext](../src/gestion/app/ApplicationContext.java) | Démarrage, construction et partage des services. |
| [MainUI](../src/gestion/ui/MainUI.java) | Navigation et rafraîchissement de Personnel à son ouverture. |
| [PlanningUI](../src/gestion/ui/planning/PlanningUI.java) | Affiche le calendrier et les employés affectés. |
| [PlanningInterventionDialogs](../src/gestion/ui/planning/PlanningInterventionDialogs.java) | Création et modification des interventions du calendrier, avec choix d’un employé. |
| [EmployeeAssignmentDialog](../src/gestion/ui/planning/EmployeeAssignmentDialog.java) | Attribution individuelle ou à une série ; affiche les heures restantes et contrôle chevauchements/dépassements. |
| [InterventionUI](../src/gestion/ui/intervention/InterventionUI.java) | Liste, recherche, tri et suppression des interventions utilisant les employés. |
| [InterventionCreationDialogs](../src/gestion/ui/intervention/InterventionCreationDialogs.java) | Création d’interventions ponctuelles ou récurrentes avec employé éventuel. |
| [RecurringInterventionService](../src/gestion/service/RecurringInterventionService.java), [RecurringInterventionCsvRepository](../src/gestion/repository/RecurringInterventionCsvRepository.java), [RecurringIntervention](../src/gestion/model/RecurringIntervention.java) | Modèle, attribution et persistance des séries. Leurs occurrences dans le planning alimentent les heures de Personnel. |
| [ReplacementUI](../src/gestion/ui/ReplacementUI.java) | Affiche les demandes créées par les absences et utilise les employés pour les remplacements et transferts d’interventions. |
| [ClientService](../src/gestion/service/ClientService.java), [ClientCsvRepository](../src/gestion/repository/ClientCsvRepository.java), [Client](../src/gestion/model/Client.java) | Fournissent les clients nécessaires au chargement des interventions qui servent au calcul des heures. |

## Ensuite, le design

`UIUtils` gère le thème, les indications des champs, les listes adaptatives et les couleurs/légendes d’alertes. `HomePanel` et `HomeCardButton` donnent accès à Personnel ; `ApplicationHeader` affiche le titre courant, l’accueil, les paramètres et le thème. `NavigationIcon` dessine leurs icônes. `AppThemeAware` est le contrat de thème de ces composants.

Dans `ui.raven`, `RavenStyledButton` et l’énumération `RavenTone` habillent les cartes ; `RavenDarkLightSwitch` et `DarkLightSwitchIcon` affichent l’interrupteur de thème. Les SVG résident dans `src/icons/theme`.

## Enfin, les tests

- `InterventionTest` : durées, nuits et durées réparties sur plusieurs jours.
- `PlanningTest` : heures comprises dans une période et limites de mois ; cible maintenant `WorkHoursService`.
- `BehaviorRegressionTest` : CSV historiques et actuels, identité des références au rechargement, édition et tri du personnel, périodes indépendantes, absences, reprise, chevauchements, échecs de sauvegarde, remplacements liés, séries, assemblage Swing et ressources graphiques.
- `support.InMemoryPreferencesFactory` : infrastructure des tests Swing ; les préférences personnelles ne sont pas modifiées par les essais.

Pour avancer sans se disperser : traiter une fonctionnalité avec son contrôleur/service/dépôt, travailler ensuite son affichage, puis vérifier son parcours et ses tests.

