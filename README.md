# Gestion+

Application de bureau en **Java avec Swing** pour gérer le personnel, les clients, les interventions et les remplacements. Le projet Eclipse s'appelle `gestion` ; le dépôt conserve son nom historique `gestionRemplacement`.

Le projet est en développement. L'état ci-dessous a été relu dans les sources le **20 octobre 2026**. « Codé » signifie présent dans le code, sans garantir une validation récente de tous les parcours.

## Portfolio public, droits et composants tiers

Gestion+ est le projet portfolio de l'autrice du compte [lisepinheiro2-lgtm](https://github.com/lisepinheiro2-lgtm). Les sources restent publiques pour permettre leur consultation et l'évaluation technique de ses compétences.

**Droits réservés sur ses créations originales uniquement**, avec une [permission gratuite d'évaluation technique](LICENSE.md) : consultation, copie, compilation, essais locaux sur données fictives et adaptations nécessaires à l'évaluation, notamment par les recruteurs. Les CSV de démonstration sont fictifs selon l'autrice. Les autres usages du code original nécessitent une autorisation selon ces conditions ; les modalités de contact y figurent.

**Les composants tiers gardent leurs propres licences et droits de réutilisation**, y compris commerciale lorsque leur licence l'autorise : Raven et JSVG sous MIT, FlatLaf et FlatLaf Extras sous Apache 2.0. Voir les [attributions, textes de licence et limites de provenance](THIRD_PARTY_NOTICES.md). La licence MIT de Raven ne place pas l'ensemble de Gestion+ sous MIT, et les droits réservés de Gestion+ ne restreignent pas la licence de Raven.

Ces mentions ne bloquent pas techniquement les copies et ne garantissent pas une exclusivité totale. Les droits de consultation et de fork prévus par les conditions de GitHub sont préservés.

## Documentation

- [Commencer ici : sommaire de la documentation](docs/index.md)
- [Vision d’ensemble Gestion+ : client, serveur et réunion future](docs/vision-ensemble.md)
- [Chantier du serveur Spring Boot](https://github.com/lisepinheiro2-lgtm/gestionplus-server)
- [Architecture et noms actuels](docs/ARCHITECTURE.md)
- [Personnel et parcours entre classes](docs/PERSONNEL.md)
- [Accueil et composants Raven](docs/INTERFACE-RAVEN.md)
- [Fonctionnalités demandées et feuille de route](docs/ROADMAP.md)
- [Tests et limites des validations](docs/VALIDATION.md)

## Fonctionnalités codées

### Personnel et heures

- Ajouter, modifier le nom, le prénom et le contrat, supprimer un employé.
- Déclarer une absence et calculer le statut au jour courant : opérationnel, congés, maladie ou autre absence.
- Reprendre une absence active en la raccourcissant, ou la retirer si elle commence le jour de la reprise.
- Calculer les heures attribuées et restantes, à la semaine ou au mois, depuis les interventions réelles.
- Choisir actuellement deux périodes indépendantes pour les deux colonnes ; leur regroupement est demandé mais pas réalisé.
- Répartir les durées aux limites de semaine/mois, y compris pour les interventions traversant minuit.
- Afficher les durées au format `35h00`, `24h30`, `-1h30`, avec alertes et légende.

L'objectif mensuel reste une estimation : **contrat hebdomadaire × 52 ÷ 12**. Un dépassement de cet objectif n'est pas, à lui seul, une qualification juridique d'heures supplémentaires.

### Clients

- Ajouter et supprimer des clients ; enregistrer identité, adresse, volume d'heures et dates facultatives du contrat.
- Proposer ces clients lors de la création d'interventions.

Le contrôle actuel refuse une adresse vide ; il ne garantit pas une adresse complète. Les cellules du tableau ne sont pas reliées à une sauvegarde d'édition. Téléphones, contacts associés et statuts d'intervention du client restent à développer.

### Planning et interventions

- Créer des interventions ponctuelles ou des séries sur un jour de semaine et une période.
- Consulter le planning en semaine/mois, naviguer et revenir à la période d'aujourd'hui.
- Ajouter au clic avec préremplissage de date et, en semaine, d'heure ; attribuer, réattribuer, modifier ou supprimer depuis le planning.
- Attribuer une occurrence ou une série ; détecter les chevauchements employé/client avec des dates et heures complètes.
- Afficher les heures disponibles et demander confirmation pour un dépassement projeté des heures prévues.
- Consulter la liste avec recherche, tris et pagination. Les tris existent encore ; leur sélecteur doit être retiré au profit de l'ordre chronologique demandé.
- Distinguer les interventions attribuées/non attribuées et hachurer les périodes passées. Le voile gris et la vue journée restent futurs.

### Remplacements

- Créer/supprimer des demandes manuelles avec période et remplaçant éventuel.
- Maintenir une demande automatique par absence avec le lien `absenceId` et synchroniser ses dates.
- Transférer ou désattribuer les interventions sélectionnées sur une plage de dates via `PlanningService`.

La demande et les affectations sont deux objets distincts. Supprimer une demande automatique ne supprime pas l'absence ; une synchronisation peut la recréer. L'intégration complète des absences à la disponibilité et la sélection précise des horaires de remplacement restent à terminer.

### Interface et persistance

- Accueil à cinq destinations actives, thème clair/sombre, préférences de thème et de fenêtre.
- Séparation entre modèles, services, dépôts CSV, contrôleur Personnel et vues.
- Six fichiers CSV locaux pour les employés, clients, absences, interventions, récurrences et remplacements.
- `DateRange` et `DateTimeRange` validés, comparaisons communes dans `PeriodUtils`.
- Helper privé d'affectation avec restauration par index en cas d'`UncheckedIOException`.
- Écriture des interventions dans un temporaire puis déplacement avec `ATOMIC_MOVE` et tentative de nettoyage.

Ces protections ne couvrent pas toutes les mutations ni tous les fichiers. Elles ne constituent pas une transaction globale. Le service `TravelTimeService` existe mais est vide. Archives reste désactivé ; il n'existe pas encore d'authentification, de SMS ou d'envoi de planning par courriel.

## Suite du projet

La [feuille de route](docs/ROADMAP.md) conserve les demandes par écran et sépare fonctions, présentation et décisions ouvertes. Elle comprend notamment :

- Finaliser les contrats de périodes, les erreurs, le rollback et la cohérence des remplacements/récurrences.
- Intégrer les trajets automatiques avec la marge demandée de cinq minutes et les corrections d'horaires avec confirmation.
- Faire évoluer Personnel : attribution au double clic, période commune et navigation unique.
- Ajouter comptes, connexion, préférences individuelles et journal des modifications.
- Compléter Clients : adresses, téléphones/contacts, statuts et présentation des dates.
- Faire évoluer Planning : vue journée, retour à aujourd'hui avec défilement, export par courriel et voile gris.
- Revoir les heures et attributions des remplacements, les récurrences et la liste des interventions.
- Ajouter les SMS optionnels lors du déplacement d'une intervention et préparer Administration/Archives.

Ordre de travail : inventaire, factorisation, fonctionnalités, design, tests regroupés, puis commit après bilan.

## État des validations

Le dossier `test/gestionTest/` contient **39 déclarations `@Test`** : 8 sur les interventions, 8 sur les calculs de périodes et 23 régressions. Ce comptage du code ne prouve pas leur réussite actuelle.

Le résultat de 39 réussites appartient au jalon historique de refactorisation. Le dernier résultat de régression rapporté est **23/23 avant la dernière vague de modifications**. **Aucune campagne après cette vague n'est établie.** La présente mise à jour est documentaire ; elle n'a ni lancé l'application ni exécuté les tests Java.

Voir [VALIDATION.md](docs/VALIDATION.md) pour la chronologie, la configuration et les limites. Le kit autonome de 48 scénarios exécuté le 2 octobre est un autre outil historique, distinct de la suite JUnit présente au dépôt.

## Ouvrir le projet dans Eclipse

1. Utiliser le JDK 25 qui a servi au jalon de refactorisation et importer le projet existant `gestion`.
2. Conserver les dossiers `src` et `test` et les bibliothèques de `lib` configurées dans `.classpath` : FlatLaf, FlatLaf Extras et JSVG.
3. Le point d'entrée actuel est **`gestion.Main`**. Le module porte encore le nom **`gestionRemplacement`** ; ces deux noms ne désignent pas la même chose.
4. Vérifier le répertoire de lancement : les CSV par défaut sont relatifs au dossier de données, initialisé par `Path.of(".")` au démarrage. Pour une vérification, utiliser une copie isolée et des données fictives.

Les préférences d'affichage utilisent encore le nœud `/gestionRemplacement`. Les comptes utilisateurs et le stockage sécurisé des données sensibles restent des chantiers futurs.
