# Architecture du projet

Refactorisation du 19 octobre 2026, à partir de la version locale avec l’accueil Raven.

## Où trouver le code

```text
src/gestionRemplacement/
  Main.java                  Point d’entrée Eclipse conservé
  app/                       Assemblage des dépendances
  model/                     Employés, clients, interventions, absences, remplacements
  repository/                Lecture et écriture des six fichiers CSV
  service/                   Règles métier et calculs
  controller/                Actions de la page Personnel
  ui/                        Fenêtre principale, navigation, thème, clients, remplacements
    personnel/               Tableau, formulaire, statut et navigation des périodes
    planning/                Calendrier et fenêtres de création/modification/attribution
    intervention/            Liste des interventions et fenêtres de création
    raven/                   Composants graphiques Raven
  util/                      Conversion et présentation des dates et heures
src/icons/                   Icône d’application et SVG clair/sombre
test/gestionRemplacement/    Tests JUnit et préférences temporaires pour les essais Swing
```

`ApplicationContext` construit une seule instance de chaque service et transmet les mêmes données aux écrans. Il reçoit le dossier de données ; les tests utilisent un dossier temporaire. `Main` ouvre la fenêtre sur le thread Swing.

## Rôles et dépendances

- **Modèles** : données et petits calculs intrinsèques, sans Swing ni lecture de fichiers.
- **Dépôts CSV** : reconstruction et sérialisation des modèles. `CsvReferences` retrouve les objets partagés à partir des références historiques prénom/nom.
- **Services** : collections en mémoire et règles métier, avec sauvegarde déléguée aux dépôts.
- **Contrôleur Personnel** : vérification des saisies et coordination des services ; ne dépend d’aucun composant Swing.
- **Vues** : composants, événements, fenêtres et présentation des erreurs. Les dialogues du planning sont séparés de son dessin.
- **Calcul des heures** : `WorkHoursService`, indépendant des écrans et des CSV ; une horloge peut être fournie pour rendre les alertes reproductibles dans les tests.

Les formulaires des autres écrans gardent leur orchestration locale ; leurs accès aux données passent par les services.

## Correspondance des anciens noms

| Ancien type | Type actuel et responsabilité |
|---|---|
| `PersonnelUI` | `ui.personnel.PersonnelPanel`, avec `PersonnelController`, `PersonnelTableModel`, `HoursPeriodSelector`, `EmployeeStatusDialog` |
| `PersonnelList` | `service.PersonnelService` + `repository.EmployeeCsvRepository` |
| `ClientList` | `service.ClientService` + `repository.ClientCsvRepository` |
| `Planning` | `service.PlanningService` + `service.WorkHoursService` + `repository.InterventionCsvRepository` |
| `EmployeeAbsenceList` | `service.AbsenceService` + `repository.AbsenceCsvRepository` |
| `ReplacementList` | `service.ReplacementService` + `repository.ReplacementCsvRepository` |
| `RecurringInterventionList` | `service.RecurringInterventionService` + `repository.RecurringInterventionCsvRepository` |
| `MainUI`, `UIUtils`, `ClientUI`, `ReplacementUI` | Déplacés dans `ui` |
| `PlanningUI` | Déplacée dans `ui.planning`, avec `PlanningInterventionDialogs` et `EmployeeAssignmentDialog` |
| `InterventionUI` | Déplacée dans `ui.intervention`, avec `InterventionCreationDialogs` |
| Modèles et énumérations | Déplacés dans `model` |
| `FormatUtils` | Déplacée dans `util` |
| `gestionRemplacement.raven` | Déplacé dans `gestionRemplacement.ui.raven` ; SVG dans `icons/theme` |
| `EmployeeSettings`, `ArchivedInterventionUI` | Classes vides et inutilisées retirées ; l’accès Archives reste désactivé |

Le nom mal orthographié `getMonthyHours()` devient `WorkHoursService.getMonthlyHours()`. Les accesseurs `getAttribuateHours()` et `getNonAttribuateHours()` deviennent `getAssignedHours()` et `getRemainingHours()`.

## Compatibilité conservée

- Les noms et formats des six CSV restent compatibles, y compris les anciens formats d’employés, de clients, d’interventions et de remplacements.
- Les identifiants d’absence et de série et la résolution historique des employés par prénom/nom sont conservés.
- Les heures mensuelles prévues restent estimées par `heures hebdomadaires × 52 ÷ 12`.
- Les statuts restent calculés à partir des périodes d’absence. Enregistrer une absence synchronise les demandes de remplacement, sans désattribution automatique du planning.
- Le stockage des préférences reste `/gestionRemplacement` malgré le déplacement des classes : thème et dimensions de fenêtre conservent leurs clés.
- Le point d’entrée reste `gestionRemplacement.Main`, Java 25, avec les bibliothèques locales existantes. Aucun téléchargement de dépendance n’est nécessaire.

Les anciens champs de statut et de compteurs d’`Employee` sont conservés pour la compatibilité du CSV. La page affiche les valeurs calculées dans `EmployeeSummary`.

## Correction associée au découpage de Personnel

L’édition d’une cellule ne tente plus de convertir les compteurs affichés comme `35h00` avec `Double.parseDouble`. Le contrôleur valide les champs modifiables et le tableau est reconstruit après le tri. Une modification suivante cible donc le bon employé.

## Vérifications

La compilation Java 25 et les **39 tests JUnit** passent : les 16 tests existants et 23 tests de régression. Les essais utilisent des données synthétiques et des préférences en mémoire.

Une comparaison exécute aussi les mêmes opérations sur les versions avant/après : ajout, modification, planning, récurrences, absences, reprise, remplacement et rechargement. Les résultats et les contenus des six CSV sont identiques pour ces scénarios.

Le rendu Personnel a été vérifié en clair et en sombre, et les écrans existants ainsi que les cinq accès de l’accueil sont couverts par les essais Swing. Ces vérifications ne constituent pas une preuve exhaustive de tous les parcours possibles.

Pour reprendre Personnel : lire [PERSONNEL.md](PERSONNEL.md), choisir une fonctionnalité, suivre sa chaîne de classes, puis travailler le design et enfin les tests de cette fonctionnalité.
