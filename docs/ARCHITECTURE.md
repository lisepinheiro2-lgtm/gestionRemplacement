---
summary: "Organisation du client Swing décrite au jalon du 20 octobre 2026."
read_when:
  - "retrouver les responsabilités des classes et les formats CSV du client"
  - "préparer une évolution vers le serveur sans confondre historique et cible"
---

# Architecture du projet gestion

État relu le **20 octobre 2026**. Cette page décrit le code présent, pas les fonctionnalités futures de la [feuille de route](ROADMAP.md). Les résultats d'exécution sont séparés dans [VALIDATION.md](VALIDATION.md).

## Noms et arborescence

Le projet Eclipse et le package racine se nomment `gestion`. Le dépôt GitHub, le module Java et le nœud de préférences conservent encore le nom historique `gestionRemplacement`.

```text
src/
  module-info.java             module gestionRemplacement
  gestion/
    Main.java                  point d'entrée gestion.Main
    app/                       assemblage des dépendances
    model/                     données métier, résumés et records de périodes
    repository/                lecture et écriture des six CSV
    service/                   règles, collections et calculs
    controller/                coordination de Personnel
    ui/                        navigation, thème, clients et remplacements
      personnel/               tableau, formulaire, statut et périodes
      planning/                calendrier et dialogues
      intervention/            liste et dialogues de création
      raven/                   composants visuels partagés
    util/                      conversions et règles de périodes
  icons/                       icône d'application et SVG de thème
test/gestionTest/              trois classes JUnit
test/gestionTest/support/      préférences en mémoire
```

[Main](../src/gestion/Main.java) ouvre l'application sur le thread Swing. [ApplicationContext](../src/gestion/app/ApplicationContext.java) reçoit un dossier de données, construit les services partagés et les transmet aux vues. L'initialisation des absences synchronise les demandes de remplacement : démarrer l'application n'est donc pas une opération nécessairement sans écriture.

## Responsabilités

| Couche | État actuel |
| --- | --- |
| Modèles | Employés, clients, absences, interventions, séries, remplacements et valeurs calculées ; pas de Swing ni de persistance. |
| Dépôts | Sérialisation CSV et reconstruction des liens. `CsvReferences` recherche historiquement les personnes par prénom/nom ; le dépôt des remplacements reconstruit encore ses propres `Employee`. |
| Services | Collections en mémoire, règles et opérations avec sauvegarde déléguée. `WorkHoursService` lit les interventions réelles et accepte une horloge pour les calculs d'alertes. |
| Contrôleur Personnel | `PersonnelController` valide les saisies et coordonne personnel, absences et heures, sans composant Swing. |
| Vues | Composants, événements, dialogues et présentation des erreurs. Les autres écrans conservent une partie de leur orchestration locale. |
| Utilitaires | `FormatUtils` convertit/affiche dates et heures ; `PeriodUtils` centralise validation et chevauchement des périodes. |

Il n'existe pas encore de contrôleur pour chaque page. `TravelTimeService` est une classe vide, pas un calculateur de trajet intégré. Aucun composant de compte, SMS, courriel ou Administration/Archives opérationnel n'est établi dans cet état.

## Périodes et affectations

- [DateRange](../src/gestion/model/DateRange.java) représente des dates inclusives et accepte une seule journée ; une fin antérieure au début ou une borne nulle est refusée.
- [DateTimeRange](../src/gestion/model/DateTimeRange.java) exige une durée strictement positive. [PeriodUtils](../src/gestion/util/PeriodUtils.java) utilise des comparaisons strictes pour les créneaux : deux créneaux jointifs ne se chevauchent pas.
- Les records de `model` appellent `PeriodUtils`, qui importe ces records ; cette dépendance dans les deux sens reste à discuter lors de la revue d'architecture.
- [PlanningService](../src/gestion/service/PlanningService.java) centralise la comparaison de deux interventions, les surcharges client/employé, un filtre de période et trois filtres d'affectation.
- L'attribution individuelle délègue à l'attribution collective. Celle-ci contrôle les cibles, doublons et conflits avant de passer au helper privé `applyAssignments`.
- Le helper conserve les anciens employés et restaure les affectations par index sur `UncheckedIOException`. Il sert aussi à la désattribution et au remplacement ; les autres mutations ne bénéficient pas toutes de cette restauration.
- Le remplacement retourne `true` si les employés sont la même référence ou si aucune intervention n'est concernée, `false` pour un conflit du remplaçant. Les dates sont construites après le retour « même employé » ; décider s'il faut les valider avant ce retour.
- Les comparaisons de personnes restent hétérogènes : identité dans les filtres, noms dans un contrôle de conflit. `Employee` ne redéfinit pas `equals`/`hashCode` dans cet état.

## Données et cohérence

| Fichier | Dépôt | Usage |
| --- | --- | --- |
| `employees.csv` | `EmployeeCsvRepository` | Employés et anciens champs compatibles. |
| `clients.csv` | `ClientCsvRepository` | Clients et contrats. |
| `employee_absences.csv` | `AbsenceCsvRepository` | Périodes d'absence à dates inclusives et identifiant. |
| `interventions.csv` | `InterventionCsvRepository` | Occurrences réelles, affectations et `recurringId` éventuel. |
| `recurringInterventions.csv` | `RecurringInterventionCsvRepository` | Définitions des séries. |
| `replacements.csv` | `ReplacementCsvRepository` | Demandes manuelles ou liées à une absence par `absenceId`. |

Planning et liste Interventions utilisent les mêmes occurrences ; les définitions récurrentes ne sont pas un second volume d'heures à additionner. Une attribution de série peut modifier occurrences et définition dans deux fichiers distincts.

Une absence synchronise une demande de remplacement sans désattribuer automatiquement le planning. Supprimer une demande ne supprime pas son absence et n'annule pas un transfert. Les demandes automatiques convertissent le dernier jour inclus en lendemain à minuit ; les opérations globales du planning sélectionnent encore par dates inclusives. Cette différence de granularité reste à traiter.

[InterventionCsvRepository](../src/gestion/repository/InterventionCsvRepository.java) écrit dans un temporaire puis utilise `Files.move` avec `REPLACE_EXISTING` et `ATOMIC_MOVE`. Les `IOException` remontent en `UncheckedIOException` ; `finally` tente le nettoyage. Aucun repli non atomique n'est prévu. Cette stratégie et le rollback ne constituent pas une transaction entre les six fichiers.

`ReplacementCsvRepository.save` écrit encore directement et intercepte ses erreurs sans les propager. `AbsenceService` restaure la mémoire si la sauvegarde de l'absence échoue, mais l'absence puis sa demande sont sauvegardées séparément. La validation de dates inversées existe au contrôleur Personnel et au chargement CSV ; `EmployeeAbsence`/`AbsenceService.addAbsence` ne centralisent pas encore toutes les préconditions.

## Correspondance des anciens noms

| Ancien type | Organisation actuelle |
| --- | --- |
| `PersonnelUI` | `gestion.ui.personnel.PersonnelPanel`, contrôleur, modèle de tableau, sélecteurs et dialogue de statut. |
| `PersonnelList` | `PersonnelService` et `EmployeeCsvRepository`. |
| `ClientList` | `ClientService` et `ClientCsvRepository`. |
| `Planning` | `PlanningService`, `WorkHoursService`, `InterventionCsvRepository`. |
| `EmployeeAbsenceList` | `AbsenceService`, `AbsenceCsvRepository`. |
| `ReplacementList` | `ReplacementService`, `ReplacementCsvRepository`. |
| `RecurringInterventionList` | `RecurringInterventionService`, `RecurringInterventionCsvRepository`. |
| Ancien package `gestionRemplacement` | Package racine `gestion`, avec sous-packages par responsabilité. |
| Anciens composants Raven | `gestion.ui.raven`, SVG sous `src/icons/theme`. |
| `EmployeeSettings`, `ArchivedInterventionUI` | Anciennes classes vides retirées ; Archives est désactivé sans écran actif. |

`getMonthyHours()` est devenu `WorkHoursService.getMonthlyHours()` ; les anciens accesseurs de compteurs deviennent `getAssignedHours()` et `getRemainingHours()`. Les anciens champs d'`Employee` restent présents pour le CSV ; Personnel affiche les calculs d'`EmployeeSummary`.

## Configuration et suite

Le module déclare `java.desktop`, `java.prefs`, FlatLaf, FlatLaf Extras et JSVG. `.classpath` contient les bibliothèques locales et le conteneur JUnit 6. Le jalon historique utilisait Java 25 ; aucune compilation actuelle n'a été exécutée pour cette revue.

Les préférences de thème et de fenêtre restent sous `/gestionRemplacement`. Les préférences par compte sont futures. Les formats CSV historiques lus restent pris en charge ; cela n'établit pas que toutes les données ou références sont protégées contre les erreurs.

Pour poursuivre : lire [Personnel](PERSONNEL.md), la [feuille de route](ROADMAP.md) et les [validations](VALIDATION.md). Stabiliser les responsabilités et contrats d'erreur avant de brancher trajets, comptes ou nouveaux écrans.
