---
title: "Gestion+ — vision d’ensemble client et serveur"
summary: "Vision commune et décisions métier : structures, droits, archives, concurrence, secours hors ligne et documents sensibles."
read_when:
  - "raisonner sur l’ensemble Gestion+ avant une évolution d’un composant"
  - "retrouver les besoins Archives Personnel Clients Planning comptes et sécurité"
  - "préparer la réunion des projets et distinguer cible et état observé"
  - "retrouver DEC01 DEC02 DEC03 DEC04 DEC05 DEC06 et les arbitrages du 29 octobre 2026"
---

# Gestion+ — vision d’ensemble

[Documentation du client](index.md) · [Documentation du serveur](https://github.com/lisepinheiro2-lgtm/gestionplus-server/blob/main/docs/index.md)

**Gestion+ est un même projet métier, aujourd’hui développé dans deux dépôts.** Le client Swing porte les écrans de gestion ; le chantier Spring Boot prépare les comptes, les contrôles et le stockage centralisé PostgreSQL.

L’objectif exprimé est de réunir à terme les deux projets dans un même ensemble Gestion+. Le choix de l’organisation finale des sources, des modules et des dépôts reste à définir. Cette page donne un repère commun sans fusionner les dépôts ni imposer un nouveau découpage de code.

## Situation actuelle et cible

| Composant | Situation documentée | Responsabilité dans la cible |
| --- | --- | --- |
| Client Swing | Application autonome avec fichiers CSV locaux et démonstration fictive ; documents techniques comportant des jalons historiques. | Écrans, navigation, saisies et présentation des résultats ou refus du serveur. |
| Serveur Spring Boot | Socles de comptes, authentification, permissions, salariés/unités, persistance et audit, avec un niveau d’achèvement variable. | Vérifier l’identité, les autorisations et la portée des données ; appliquer les règles métier et protéger les opérations. |
| PostgreSQL | Configuration et migrations du serveur présentes ; une migration écrite n’est pas une preuve de son exécution. | Conserver les données avec contraintes et transactions ; accès maîtrisé par le serveur. |
| Liaison client/serveur | Intégration complète prévue, sans validation de bout en bout revendiquée. | Contrats d’échange, sessions, erreurs, révocation et compatibilité des versions. |

Pour les faits détaillés, consulter les documents datés du client et [l’avancement du serveur](https://github.com/lisepinheiro2-lgtm/gestionplus-server/blob/main/docs/AVANCEMENT.md). Cette synthèse du **28 octobre 2026** organise ces références ; elle ne constitue pas une nouvelle revue exhaustive des sources ni une exécution de tests.

## Carte fonctionnelle à suivre ensemble

La [feuille de route du client](ROADMAP.md) conserve les demandes détaillées et leurs repères. La table ci-dessous relie ces domaines à la cible serveur ; elle ne transforme pas les fonctions prévues en fonctions opérationnelles.

| Domaine | Références | Responsabilité à conserver dans l’ensemble Gestion+ |
| --- | --- | --- |
| Personnel et unités | PER01–PER08, SOC04 | Fiches salarié, identités durables, rattachements, contrats et compteurs ; séparer calculs métier et présentation. |
| Comptes et préférences | CPT01–CPT04 | Invitation, activation, connexion, identifiants personnels, rôles, récupération, désactivation et préférences par compte. |
| Clients | CLI01–CLI06 | Contrats, coordonnées utiles, contacts, statuts et conséquences sur les interventions. |
| Planning | PLA01–PLA04 | Consultation des périodes, disponibilités, affectations et données nécessaires aux vues et exports. |
| Interventions et récurrences | INT01–INT13, SOC01–SOC02 | Cohérence occurrence/série, création et modifications, conflits, attributions et conservation de l’historique. |
| Absences et remplacements | SOC05–SOC06, RMP01–RMP06 | Liens durables, disponibilité, période concernée, attributions et reprise anticipée ; conserver les décisions encore ouvertes. |
| Trajets et corrections horaires | TRJ01, HOR01 | Règles de calcul, confirmation et conséquences sur les horaires et compteurs ; prestataire et détails restant à définir. |
| Courriels et SMS | PLA03, INT10, parcours de comptes | Destinataires, contenu, autorisation, confirmation selon le parcours, échecs et prévention des doublons. |
| Administration et Archives | ADM01–ADM07 | Informations et documents du personnel et des clients, historique des interventions, finances et journal des modifications. La séparation des écrans reste à concevoir. |
| Sécurité et audit | SOC07, ADM07, feuille de route serveur | Contrôles côté serveur, portée des accès, sessions, révocation, traçabilité protégée et cas de refus. |

<a id="decisions-metier-2026-10-29"></a>
## Décisions métier transversales — 29 octobre 2026

**Cible de conception précisée par l'utilisatrice, puis ajustée après discussion des risques de concurrence et de fonctionnement hors ligne. Aucune de ces décisions n'est une preuve d'implémentation.** Les six repères DEC01–DEC06 ci-dessous constituent la référence commune ; les suivis client et serveur y renvoient. Les règles de comptes et de sécurité déjà retenues restent applicables, avec leur adaptation multi-structures encore à préciser.

<a id="dec-structures"></a>
### DEC01 — Plusieurs structures indépendantes

Gestion+ doit pouvoir accueillir plusieurs structures indépendantes, chacune avec ses salariés, clients, plannings, interventions, remplacements et documents. Le serveur devra imposer l'isolation lors des lectures et mutations, y compris recherches, téléchargements, exports et traces consultables. Le choix d'une structure dans Swing ne suffit pas à autoriser un accès.

Le découpage du stockage reste à concevoir ; aucune base par structure, colonne de partitionnement ou migration n'est décidée ici. Ne pas assimiler automatiquement une unité ou une équipe existante à une structure indépendante.

**Décision explicitement laissée ouverte :** une personne intervenant pour plusieurs structures utilisera-t-elle un compte unique avec des rattachements et droits séparés, ou un compte distinct par structure ? Cela concerne notamment les comptables externes. Aucune des deux options n'est retenue à ce stade. L'éventuelle administration technique transverse reste également à définir, sans accès inter-structures implicite.

<a id="dec-droits"></a>
### DEC02 — Responsables et secrétaires dans leur périmètre

Les responsables hiérarchiques liés aux personnes ou activités concernées et les secrétaires sont les acteurs de gestion envisagés pour les salariés, clients, interventions et remplacements. Leurs accès doivent être limités à la structure et au périmètre autorisés.

La matrice consulter/créer/modifier/supprimer reste à détailler pour chaque rôle et domaine, ainsi que les rattachements exacts des responsables et secrétaires. Cette orientation ne confère pas automatiquement tous les droits de suppression, d'accès documentaire ou de gestion des comptes. Les décisions antérieures sur MANAGER, RH et ADMIN pour les comptes ne sont pas annulées ; leur application aux structures reste à expliciter. L'autorisation finale relève du serveur.

<a id="dec-historique"></a>
### DEC03 — Départs, annulations, archives et facturation

- Le départ d'un salarié ou d'un client ne doit pas effacer automatiquement ses documents : contrats, fiches de paie, factures, solde de tout compte et autres pièces utiles restent conservés selon leur catégorie et les règles applicables.
- Les durées, accès après départ, archivage, éventuelle restauration et suppression à échéance restent à définir. « Conserver » ne signifie pas conservation illimitée ; aucune durée légale précise n'est fixée par cette note.
- Une intervention annulée reste consultable. Une alerte signale que sa facturation est à déterminer jusqu'à ce qu'une personne habilitée précise « facturée » ou « non facturée ». Conserver la décision et sa traçabilité ; aucun montant ou statut de facturation automatique n'est déduit de la seule annulation. Le parcours, les habilitations et les règles de validation de facture restent à concevoir.
- Distinguer ce cas de l'alerte future pour une intervention/remplacement devenu passé sans attribution, y compris une demande partiellement couverte. Un compteur limité aux occurrences futures peut être nul malgré ce passé à vérifier. L'absence d'attribution est un signal à examiner, pas une preuve de réalisation ou de non-réalisation ; l'absence d'alerte ne prouve pas non plus l'exécution. Prévenir la facturation erronée sans inventer des liens ou des faits manquants.

Ces besoins relèvent notamment d'INT01 et ADM02–ADM05. Archives, documents et facturation ne deviennent pas des modules opérationnels par leur inscription ici.

<a id="dec-concurrence"></a>
### DEC04 — Travail simultané sans écrasement silencieux

Deux protections complémentaires sont retenues comme cible :

1. Un verrou temporaire géré côté serveur signale qu'un autre utilisateur modifie déjà la ressource. L'utilisateur informé peut la consulter, mais ne peut pas la modifier tant que ce verrou s'applique.
2. Un contrôle au moment de l'enregistrement refuse une écriture devenue périmée ou concurrente, même si un verrou a expiré ou qu'une connexion a été perdue. Ne jamais écraser silencieusement la version d'un autre utilisateur ; présenter le conflit et permettre une reprise à partir des données courantes.

La portée du verrou (intervention, série ou autre ensemble), sa durée, son renouvellement/libération et le traitement d'une déconnexion restent à définir. Ces protections devront rester cohérentes avec les règles d'affectation et les autorisations ; un verrou n'accorde aucun droit supplémentaire. Aucune technologie de verrouillage ou de versionnement n'est implémentée par cette décision.

<a id="dec-hors-ligne"></a>
### DEC05 — ADMIN_EMERGENCY : continuité maximale sous restrictions

L'objectif est un mode de secours réellement utile sur un poste préalablement autorisé. La demande initiale « toutes les opérations » est ajustée : elle n'est pas retenue comme un accès illimité ni comme un contournement du serveur.

La liste des opérations permises et interdites reste à décider lors de la conception dédiée. Les changements de comptes, de permissions et de données financières font partie des opérations sensibles pour lesquelles une validation du serveur est à privilégier ; leurs restrictions exactes ne sont pas encore arbitrées.

Prévoir une authentification hors ligne sécurisée, des données locales chiffrées et limitées au périmètre autorisé, des traces protégées, puis une réconciliation contrôlée à la reconnexion. Un poste déconnecté ne connaît pas immédiatement les révocations ou modifications centrales ; définir la validité de l'autorisation locale, sa révocation et le traitement des conflits sans réinjecter automatiquement des changements obsolètes ou non autorisés. Aucun accès administrateur central implicite.

Ce chantier reste distinct de la démonstration Swing autonome avec données fictives. Aucun accès réel de secours, cache d'entreprise ou mécanisme de synchronisation n'est livré par cette note.

<a id="dec-documents"></a>
### DEC06 — Documents sensibles et accès administrateur/comptable

L'administrateur de la structure dispose de l'accès documentaire complet à celle-ci ; cette portée ne s'étend pas aux autres structures. Le comptable accède aux seuls documents nécessaires à sa mission, dans son périmètre autorisé. Les catégories exactes et les possibilités de lecture, téléchargement, ajout, modification, suppression ou export restent à détailler ; ne pas accorder un accès général aux autres documents personnels, médicaux ou administratifs par déduction.

« Accès complet » n'inclut jamais la consultation des mots de passe, codes ou jetons secrets. Les autres rôles ne reçoivent pas automatiquement des droits documentaires de leur seul droit de gestion métier.

Le stockage protégé, le chiffrement, les sauvegardes, la conservation, la traçabilité des accès et les refus inter-structures sont des exigences à concevoir et vérifier. Le serveur doit protéger la consultation et les téléchargements comme les modifications. Aucun fournisseur, emplacement de stockage ou format de chiffrement n'est sélectionné ici ; aucun document sensible réel dans les dépôts.

### Points encore ouverts et niveau de preuve

Restent à décider : compte unique ou séparé par structure ; rattachements et matrice des droits ; durées de conservation et règles de facturation ; portée et cycle de vie des verrous ; liste des opérations hors ligne et réconciliation ; catégories documentaires du comptable et stockage. Consulter ces décisions avant une évolution touchant les comptes, l'organisation, le planning ou les archives.

**Intervention du 29 octobre : documentation uniquement.** Aucun code, CSV, schéma, configuration applicative ou migration modifié ; aucun test, build, application ou serveur lancé. Les vérifications de métadonnées et de liens documentaires ne valident pas ces fonctionnalités. Aucune opération Git modifiante ni publication effectuée.

## Frontières à préserver

- Un bouton masqué dans Swing n’est pas une protection : le serveur doit vérifier les opérations de lecture comme de modification, d’export et de consultation des documents.
- La démonstration autonome utilise des données fictives isolées. Le serveur indisponible ne doit pas donner un accès local privilégié à des données d’entreprise.
- L’accès de secours `ADMIN_EMERGENCY` reste un mécanisme distinct à concevoir : poste autorisé, droits limités, audit et révocation. Il ne remplace pas un administrateur central.
- Préserver les règles déjà construites dans le client lorsqu’elles correspondent au besoin, puis définir leur responsabilité côté serveur ; éviter de créer deux politiques concurrentes.
- Les comptes, coordonnées, documents administratifs et traces réels ne servent pas d’exemples dans les sources ou la documentation publique.

## Préparer la réunion sans anticiper une fusion technique

1. Garder ces repères communs et une référence claire pour chaque décision.
2. Définir les contrats client/serveur au rythme des fonctionnalités autorisées : identifiants, données échangées, permissions et erreurs.
3. Distinguer les validations locales de chaque composant des essais connectés réellement exécutés.
4. Décider ultérieurement de l’organisation du projet réuni, de la reprise des historiques et de la compatibilité des versions.

Ces étapes sont des objectifs de suivi, pas des autorisations de déplacer du code, modifier Git ou lancer des services.

## Maintenir la documentation

| Information | Référence à actualiser lors d’une intervention autorisée |
| --- | --- |
| Demande fonctionnelle et décision d’écran | [Feuille de route client](ROADMAP.md), avec son repère existant |
| Structure et parcours Swing | [Architecture](ARCHITECTURE.md), [Personnel](PERSONNEL.md), [Interface](INTERFACE-RAVEN.md) |
| Protection des fichiers et résultats du client | [Fiabilisation](FIABILISATION.md), [Validations](VALIDATION.md) |
| Bloc serveur implémenté, en cours ou prévu ; preuve disponible | [Avancement serveur](https://github.com/lisepinheiro2-lgtm/gestionplus-server/blob/main/docs/AVANCEMENT.md) |
| Décision transversale affectant la réunion | Cette page, en renvoyant au détail concerné |
| Détails de séance et questions privées | Suivis locaux, sans copie intégrale dans les pages publiques |

Pour une preuve, préciser la date, le périmètre exécuté, le résultat et sa limite. Un test écrit mais non exécuté reste non vérifié. Conserver l’historique et comparer les versions des deux ordinateurs avant toute synchronisation des notes.
