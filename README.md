# Gestion+

**Une application Java de gestion du personnel, des clients, des plannings et des remplacements.**

Gestion+ est un projet personnel de portfolio, développé progressivement autour de besoins concrets de planification : suivre les heures, gérer les absences et réattribuer les interventions sans perdre leur historique.

Ce dépôt contient le **client de bureau Java Swing**, utilisable de façon autonome avec des fichiers CSV locaux. Le [serveur Spring Boot](https://github.com/lisepinheiro2-lgtm/gestionplus-server) est développé dans un dépôt distinct, pour préparer les comptes, les permissions et le stockage centralisé.

> **En développement actif.** État du client relu dans les sources le **29 octobre 2026**. Les fonctionnalités décrites sont présentes dans le code ; cette revue documentaire ne constitue pas une validation complète de l'application.

[Documentation](docs/index.md) · [Vision client–serveur](docs/vision-ensemble.md) · [Feuille de route](docs/ROADMAP.md) · [Serveur](https://github.com/lisepinheiro2-lgtm/gestionplus-server)

## Le projet aujourd'hui

| Composant | État actuel |
| --- | --- |
| **Client Swing — ce dépôt** | Application locale : personnel, clients, interventions, absences et remplacements. Persistance dans six CSV. |
| **Serveur Spring Boot** | Chantier séparé pour l'authentification, les autorisations et la persistance centralisée. Voir son [README](https://github.com/lisepinheiro2-lgtm/gestionplus-server#readme). |
| **Connexion client–serveur** | Intégration à réaliser. Le client actuel ne demande ni serveur ni compte pour démarrer. |

Le dépôt GitHub s'appelle désormais `gestionplus-client`. Le projet Eclipse et le package racine s'appellent `gestion` ; le module Java et le nœud de préférences conservent le nom historique `gestionRemplacement`.

## Fonctionnalités présentes

### Personnel, heures et absences

- Ajouter, modifier et supprimer des employés, avec contrôle des références existantes.
- Consulter les heures attribuées et restantes sur une **période commune**, à la semaine ou au mois.
- Ouvrir les interventions par **double clic sur un compteur**, puis attribuer, réattribuer ou désattribuer une sélection, avec aperçu de son impact sur les heures.
- Déclarer un statut et une absence ; préciser les heures pour une absence partielle ou signalée tardivement et confirmer les créneaux à libérer.
- Libérer les portions d'interventions concernées par une absence, conserver les portions hors absence et relier les besoins de remplacement à leur origine.
- Répartir les durées aux limites de semaine et de mois, y compris pour les interventions de nuit.

Les durées sont affichées en heures et minutes. L'objectif mensuel reste une estimation : **contrat hebdomadaire × 52 ÷ 12** ; le dépassement affiché est un indicateur de planification.

### Planning et interventions

- Consulter les vues **semaine et mois**, naviguer entre les périodes, revenir à aujourd'hui et rechercher des interventions.
- Créer une intervention ponctuelle ou une série récurrente, avec préremplissage depuis le calendrier.
- Attribuer, réattribuer, modifier ou supprimer une intervention depuis le planning.
- Afficher les interventions simultanées et le détail d'une journée ; distinguer les attributions et appliquer un **voile gris aux périodes passées**.
- Contrôler les conflits d'un salarié et ses absences ; autoriser les créneaux jointifs et demander confirmation pour un chevauchement côté client.
- Consulter une liste séparée avec recherche, tris et pagination.

Les actions globales de série protègent les occurrences déjà commencées. La vue journée dédiée reste à développer : le panneau de détail d'un jour ne la remplace pas.

### Remplacements

- Créer des demandes manuelles et gérer les demandes liées aux absences.
- Suivre, pour un groupe lié à une absence, son état d'attribution, ses interventions et ses heures restant à couvrir.
- Attribuer les interventions futures encore libres du groupe à un remplaçant, ou répartir les attributions individuellement depuis le planning.
- Conserver le lien entre absence, demande et interventions, même après une réattribution.

Une demande de remplacement et une affectation au planning restent des objets distincts. La reprise anticipée d'une absence ayant des interventions liées est encore bloquée, en attendant un parcours de réorganisation adapté.

### Clients et interface

- Enregistrer identité, adresse, volume horaire et dates facultatives de contrat ; supprimer un client dans les limites des références existantes.
- Afficher les dates au format français et garder les tableaux sans édition directe non sauvegardée.
- Naviguer depuis un accueil à cinq destinations actives et un en-tête commun.
- Utiliser les thèmes clair et sombre, avec conservation des préférences de thème et de fenêtre.
- Bénéficier d'écrans et de dialogues remaniés pour le redimensionnement, la recherche et la lecture des informations.

Le contrôle d'adresse ne garantit pas encore une adresse complète. Les téléphones et contacts associés, les trajets, les SMS, l'envoi de planning par courriel et les Archives restent des chantiers futurs.

## Architecture et stockage local

Le code sépare les **modèles**, les **services métier**, les **dépôts CSV** et les **vues Swing**. Le contrôleur Personnel coordonne ses parcours ; les périodes, validations et calculs d'heures sont regroupés dans des composants communs.

```text
src/gestion/
  app/           assemblage des dépendances
  model/         employés, clients, périodes, interventions, absences…
  repository/    CSV, identifiants, migration et récupération
  service/       règles métier, affectations et calculs d'heures
  controller/    coordination de Personnel
  ui/            écrans, dialogues, navigation et thèmes
  util/          formats, périodes et validations
test/            tests métier, persistance et composants Swing
tools/verify.py  compilation et tests sur données isolées
```

Les six CSV couvrent employés, clients, absences, interventions, récurrences et remplacements. Les identifiants **UUID** stabilisent les références. Les écritures utilisent des fichiers temporaires ; les opérations composées disposent d'un journal local de récupération et d'une restauration de l'état en cas d'échec.

**Le stockage reste prévu pour une seule instance par dossier de données.** Les remplacements de plusieurs fichiers ne forment pas une opération atomique unique, et ce mécanisme local ne remplace ni les sauvegardes ni les transactions du futur serveur.

Le démarrage appelle la récupération puis la migration des anciens CSV vers les identifiants UUID. Si une migration est nécessaire, une copie préalable est créée dans `.gestion-uuid-backups`, à côté du dossier de données. **Lancer l'application peut donc modifier ses fichiers de données.**

## Ouvrir et lancer le client dans Eclipse

1. Cloner ou télécharger ce dépôt, puis importer le projet Java existant `gestion` dans Eclipse.
2. Sélectionner un **JDK 25**.
3. Conserver les dossiers `src` et `test`, ainsi que les bibliothèques de `lib` déclarées dans `.classpath` : **FlatLaf 3.7.2**, **FlatLaf Extras 3.7.2** et **JSVG 2.1.0**.
4. Vérifier le répertoire de travail de la configuration de lancement : les CSV sont recherchés dans ce dossier, initialisé par `Path.of(".")`.
5. Exécuter **`gestion.Main`** comme application Java.

Le module s'appelle toujours `gestionRemplacement`. Le conteneur **JUnit 6** d'Eclipse est utilisé pour les tests ; les sorties sont séparées entre `bin` et `bin-test`.

Les CSV versionnés sont un **jeu de démonstration fictif**. Pour essayer une migration ou un scénario d'erreur, utiliser une copie isolée du dossier de données.

## Tests et niveau de validation

Au 29 octobre 2026, le dépôt contient **100 méthodes annotées `@Test` dans neuf classes de test**. Elles portent sur les durées et périodes, les affectations, les absences, la fiabilité des CSV, la récupération après interruption et plusieurs comportements des composants Swing.

**Ce nombre est un inventaire des sources, pas un résultat de 100 tests réussis.** Aucun test Java ni lancement de l'application n'a été effectué pour cette mise à jour du README.

Le script [`tools/verify.py`](tools/verify.py) compile les sources et lance les tests en mode classique puis modulaire. Il demande un JDK 25, les JAR JUnit 6.0.1 locaux et un dossier de sortie extérieur au projet. Les essais utilisent des données fictives et des préférences en mémoire.

Les [résultats historiques](docs/VALIDATION.md) gardent leurs dates et leur périmètre. Les vérifications automatisées de composants ne remplacent pas une recette manuelle complète des écrans et dialogues.

## Prochaines étapes

- Relier progressivement le client au serveur, avec contrôle des droits côté serveur.
- Compléter les parcours clients, les trajets et les corrections d'horaires.
- Terminer les réorganisations d'absences et de remplacements encore limitées.
- Développer la vue journée, les communications et les fonctions d'archives.
- Poursuivre les validations métier et la recette de l'interface.

La [feuille de route](docs/ROADMAP.md) et la [vision d'ensemble](docs/vision-ensemble.md) conservent les besoins et décisions détaillés. Certaines pages techniques décrivent les jalons des 19 et 20 octobre : leurs constats doivent être lus avec leur date, en complément du code actuel.

## Documentation

- [Sommaire de la documentation](docs/index.md)
- [Vision d'ensemble Gestion+ et évolution client–serveur](docs/vision-ensemble.md)
- [Architecture et responsabilités](docs/ARCHITECTURE.md)
- [Personnel et parcours entre classes](docs/PERSONNEL.md)
- [Accueil et composants Raven](docs/INTERFACE-RAVEN.md)
- [Fiabilisation locale — jalon du 20 octobre](docs/FIABILISATION.md)
- [Feuille de route](docs/ROADMAP.md)
- [Tests et historique des validations](docs/VALIDATION.md)
- [Chantier du serveur Spring Boot](https://github.com/lisepinheiro2-lgtm/gestionplus-server)

## Portfolio public, droits et composants tiers

Gestion+ est le projet portfolio de l'autrice du compte [lisepinheiro2-lgtm](https://github.com/lisepinheiro2-lgtm). Les sources restent publiques pour permettre leur consultation et l'évaluation technique de ses compétences.

**Droits réservés sur ses créations originales uniquement**, avec une [permission gratuite d'évaluation technique](LICENSE.md) : consultation, copie, compilation, essais locaux sur données fictives et adaptations nécessaires à l'évaluation, notamment par les recruteurs. Les CSV de démonstration sont fictifs selon l'autrice. Les autres usages du code original nécessitent une autorisation selon ces conditions ; les modalités de contact y figurent.

**Les composants tiers gardent leurs propres licences et droits de réutilisation**, y compris commerciale lorsque leur licence l'autorise : Raven et JSVG sous MIT, FlatLaf et FlatLaf Extras sous Apache 2.0. Voir les [attributions, textes de licence et limites de provenance](THIRD_PARTY_NOTICES.md). La licence MIT de Raven ne place pas l'ensemble de Gestion+ sous MIT, et les droits réservés de Gestion+ ne restreignent pas la licence de Raven.

Ces mentions ne bloquent pas techniquement les copies et ne garantissent pas une exclusivité totale. Les droits de consultation et de fork prévus par les conditions de GitHub sont préservés.
