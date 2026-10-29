---
title: "Documentation du client Swing"
summary: "Point d’entrée du client et liens vers la cible commune Gestion+."
read_when:
  - "commencer une séance sur le client ou retrouver un document"
  - "préparer la réunion du client et du serveur dans Gestion+"
---

# Documentation du client Swing

[Présentation du client](../README.md) · [Vision d’ensemble de Gestion+](vision-ensemble.md) · [Documentation du serveur](https://github.com/lisepinheiro2-lgtm/gestionplus-server/blob/main/docs/index.md)

Gestion+ est suivi comme un même projet métier, actuellement réparti entre ce client Swing et un serveur Spring Boot. La réunion des deux projets est un objectif ; leur organisation technique finale reste à décider. La démonstration Swing avec données fictives conserve son fonctionnement autonome.

## Quoi lire selon la tâche ?

| Besoin | Document de référence |
| --- | --- |
| Comprendre le produit, les responsabilités client/serveur et leur future réunion | [Vision d’ensemble](vision-ensemble.md) |
| Retrouver les six décisions métier du 29 octobre : structures, droits, archives/facturation, concurrence, hors ligne et documents | [Référence commune DEC01–DEC06](vision-ensemble.md#decisions-metier-2026-10-29) |
| Retrouver une demande ou une décision encore ouverte | [Feuille de route fonctionnelle](ROADMAP.md) — repères SOC, PER, CPT, CLI, PLA, RMP, INT et ADM |
| Retrouver les couches, classes et formats du client | [Architecture](ARCHITECTURE.md) |
| Comprendre les protections des fichiers et les liens absence/intervention/remplacement | [Fiabilisation](FIABILISATION.md) |
| Suivre un parcours de la page Personnel | [Carte des classes Personnel](PERSONNEL.md) |
| Travailler sur l’accueil, les thèmes et les composants visuels | [Interface Raven](INTERFACE-RAVEN.md) |
| Savoir quelles validations sont établies et lesquelles restent nécessaires | [Tests et validations](VALIDATION.md) |
| Examiner les comptes, permissions, stockage et audit du serveur | [Avancement du serveur](https://github.com/lisepinheiro2-lgtm/gestionplus-server/blob/main/docs/AVANCEMENT.md) |

## Lire les dates et les preuves

Les documents techniques du client comportent des états et résultats historiques, notamment du **20 octobre 2026**. Certains conservent un ancien état après une section de mise à jour : lire les réserves et la section prioritaire avant de conclure qu’un défaut existe encore ou qu’une fonction est terminée.

La page Fiabilisation précise les protections livrées à ce jalon. La feuille de route décrit aussi des demandes futures ; une ligne de cette liste ne constitue pas une preuve d’exécution. Pour une modification actuelle, recouper le document concerné avec les sources et les preuves datées.

Ce sommaire a été préparé le **28 octobre 2026**, puis complété le **29 octobre 2026** par les renvois aux décisions métier. Aucune nouvelle validation du code, des tests ou des écrans.

## Garder un suivi lisible

Conserver les repères fonctionnels existants. Pour chaque évolution documentaire autorisée, préciser le composant concerné, le comportement écrit, les vérifications réellement exécutées, leurs limites et les questions restantes. Les détails privés et journaux bruts restent dans le suivi local.

Ces pages se lisent directement dans GitHub ou un éditeur. Docmap est une aide facultative de consultation ; il ne remplace ni les documents ni leur historique.
