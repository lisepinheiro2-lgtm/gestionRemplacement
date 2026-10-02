# Gestion des remplacements

Application de bureau développée en **Java avec Swing** pour gérer les employés, les clients, les interventions et les remplacements.

Le projet est en cours de développement. Les fonctionnalités disponibles et les améliorations prévues sont présentées ci-dessous.

## Fonctionnalités déjà codées

### Personnel et statuts

- Ajout et suppression des employés, avec leurs nom, prénom et heures prévues au contrat.
- Changement de statut en cliquant sur la cellule correspondante : **Opérationnel**, **Congés**, **Maladie** ou **Autre absence**.
- Saisie des dates de début et de fin d'une absence.
- Calcul du statut de l'employé à la date du jour à partir des périodes enregistrées.
- Retour en « Opérationnel » pour terminer une absence en cours, avec conservation de l'historique lorsque l'absence a commencé avant le jour de reprise.
- Repérage visuel des statuts : vert foncé pour un employé opérationnel, rouge foncé pour un employé absent.

### Suivi des heures

- Calcul des heures attribuées et des heures restantes par employé, en **vue semaine** ou **vue mois**.
- Choix indépendant de la période pour les deux colonnes, avec flèches de navigation et affichage des dates au centre.
- Actualisation des compteurs au rafraîchissement de la vue Personnel, sans relancer l'application.
- Prise en compte des interventions de nuit et répartition des heures entre les semaines ou les mois concernés.
- Affichage des durées au format `35h00`, `24h30` ou `-1h30`.
- Alertes tenant compte des heures restantes et du temps disponible avant la fin de la période.
- Légende des couleurs au survol des cellules :
  - 🔵 Attribution en cours.
  - 🟢 Objectif atteint.
  - 🟠 Retard d'attribution à surveiller.
  - 🔴 Attribution urgente ou dépassement des heures prévues.

L'objectif mensuel est actuellement estimé à partir du contrat hebdomadaire : **heures par semaine × 52 ÷ 12**.

### Clients

- Ajout et suppression des clients.
- Enregistrement des coordonnées, du volume d'heures et des dates de contrat.
- Sélection des clients lors de la création des interventions.

### Interventions et planning

- Création d'interventions ponctuelles ou récurrentes.
- Définition du jour de la semaine et de la période d'une récurrence.
- Planning en **vue semaine** et en **vue mois**, avec navigation et retour à aujourd'hui.
- Création d'une intervention au clic dans le planning, avec préremplissage de la date et, en vue semaine, de l'heure.
- Attribution, réattribution, modification et suppression depuis le planning.
- Attribution d'une seule occurrence ou de toutes les occurrences d'une intervention récurrente au même employé.
- Détection des chevauchements d'interventions pour les employés et les clients.
- Affichage des heures disponibles lors du choix d'un employé.
- Avertissement et confirmation en cas de dépassement des heures contractuelles sur les semaines concernées.
- Distinction visuelle entre les interventions attribuées et non attribuées.
- Repérage des jours et créneaux déjà passés par des hachures.
- Vue Liste avec recherche, tri par date, client ou intervenant, et pagination.

### Remplacements

- Création et suppression de remplacements manuels, avec dates et horaires.
- Choix d'un remplaçant et option d'attribution des interventions sur la période sélectionnée.
- Génération d'une **demande de remplacement non attribuée** lors de l'enregistrement d'une absence.
- Association des demandes automatiques à leur absence et synchronisation de leurs dates.
- Conservation du remplaçant choisi lors de la synchronisation d'une demande existante.
- Sauvegarde et rechargement des demandes automatiques au démarrage.

La génération des demandes automatiques est codée ; leur prise en charge complète dans le planning reste à finaliser.

### Interface et sauvegarde

- Modes clair et sombre, avec mémorisation du thème choisi.
- Mémorisation de la taille de la fenêtre et de son état maximisé.
- En-têtes et menus de suivi des heures adaptés au redimensionnement.
- Sauvegarde locale des employés, clients, interventions, récurrences, absences et remplacements dans des fichiers CSV.

## Fonctionnalités à venir

### Absences et remplacements — priorités

- Consulter, modifier et annuler les périodes d'absence déjà enregistrées, notamment les absences futures.
- Permettre d'annuler une absence future même si l'employé est encore opérationnel aujourd'hui.
- Clarifier la suppression d'une demande automatique : supprimer uniquement la demande laisse l'absence enregistrée et provoque sa recréation au démarrage.
- Finaliser la gestion des interventions affectées par une absence et l'attribution de leurs remplacements.
- Prendre en compte les absences dans la disponibilité des employés proposés à l'attribution.
- Respecter précisément les dates **et les horaires** lors de l'application d'un remplacement.
- Gérer les conséquences d'une modification ou d'une annulation de remplacement sur les interventions déjà réattribuées.

### Fiabilité et validation

- Renforcer les contrôles dans le modèle : refuser les dates d'absence inversées et le statut « Opérationnel » comme période d'absence.
- Transmettre les erreurs de sauvegarde des remplacements à l'interface pour éviter qu'une opération semble enregistrée alors qu'elle a échoué.
- Sécuriser les écritures des fichiers CSV et la cohérence entre les données liées.
- Corriger l'édition des informations d'un employé lorsque les compteurs d'heures sont affichés sous forme de texte, par exemple `5h00`.
- Préserver les liens entre employés, absences et interventions lors d'un changement de nom.

### Ergonomie et évolution du projet

- Simplifier l'affectation d'un remplaçant depuis une demande existante.
- Améliorer la vue Liste en conservant la recherche et le tri.
- Poursuivre l'adaptation des interfaces au redimensionnement et harmoniser les dialogues.
- Renforcer la validation des adresses et ajouter des profils clients.
- Mieux séparer l'interface, les règles métier et la sauvegarde ; réduire les duplications de code.
- Intégrer le kit de tests automatisés au dépôt et compléter les tests du planning, des remplacements et de l'interface.

## Validation actuelle

Un kit autonome de **48 tests automatisés**, préparé séparément du dépôt, a été exécuté sur le code du projet le **2 octobre 2026** : **45 réussites et 3 échecs**.

Ces tests couvrent notamment les calculs d'heures, les chevauchements, les attributions en lot, les absences et la sauvegarde/relecture des données. Chaque scénario utilise un dossier séparé avec des données fictives.

Les trois échecs concernent les validations du modèle et la transmission des erreurs de sauvegarde, listées parmi les priorités ci-dessus. Les tests de reproduction du cas d'une absence future ne signifient pas que sa modification dans l'interface est déjà disponible. Les clics et l'apparence des fenêtres restent à vérifier.

## Lancer le projet

Le projet est développé dans **Eclipse**, avec **Java 25** comme version utilisée pour la compilation et les tests actuels.
