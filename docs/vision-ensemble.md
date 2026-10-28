---
title: "Gestion+ — vision d’ensemble client et serveur"
summary: "Carte du produit, responsabilités Swing et Spring Boot, domaines suivis et réunion future."
read_when:
  - "raisonner sur l’ensemble Gestion+ avant une évolution d’un composant"
  - "retrouver les besoins Archives Personnel Clients Planning comptes et sécurité"
  - "préparer la réunion des projets et distinguer cible et état observé"
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
