---
summary: "Repères de l’interface Swing et des composants Raven au 20 octobre 2026."
read_when:
  - "travailler sur le design les thèmes et la navigation Swing"
---

# Accueil et composants Raven

État relu le **20 octobre 2026**. L'accueil conserve trois rangées : Personnel / Clients, Planning / Remplacements, Interventions / Archives. Cinq accès sont actifs. **Archives reste désactivé avec « À venir »** : aucun écran d'archives n'est construit ; l'ancienne classe vide `ArchivedInterventionUI` a été retirée.

## Composants actuels

Les cartes héritent du bouton stylé Raven. L'interrupteur clair/sombre utilise `DarkLightSwitchIcon` et les SVG de thème. Les icônes des six espaces sont dessinées en Java. Les dépendances configurées dans `lib` sont FlatLaf 3.7.2, FlatLaf Extras 3.7.2 et JSVG 2.1.0.

| Fichier | Responsabilité |
| --- | --- |
| [HomePanel](../src/gestion/ui/HomePanel.java) | Présentation des six accès et callbacks des cinq destinations actives. |
| [HomeCardButton](../src/gestion/ui/HomeCardButton.java) | Apparence, clavier et état activé/désactivé des cartes. |
| [ApplicationHeader](../src/gestion/ui/ApplicationHeader.java) | Titre courant, retour accueil, paramètres et thème. |
| [NavigationIcon](../src/gestion/ui/NavigationIcon.java) | Icônes vectorielles de navigation. |
| [MainUI](../src/gestion/ui/MainUI.java) | Assemblage des vues, navigation et préférences de fenêtre. |
| [UIUtils](../src/gestion/ui/UIUtils.java) | Thème partagé, couleurs métier et application du style. |
| [Composants Raven](../src/gestion/ui/raven/) | Boutons et interrupteur adaptés à l'application. |
| [SVG de thème](../src/icons/theme/) | Images clair/sombre. |
| [Licence Raven](../licenses/Raven-MIT.txt) | Notice MIT conservée. |

L'interrupteur du bandeau et l'option Mode sombre des paramètres utilisent le même réglage. Les préférences d'affichage utilisent toujours `/gestionRemplacement`. La fenêtre a une taille minimale de 760 × 650. Le point d'entrée est `gestion.Main` ; le module reste `gestionRemplacement`.

## Évolutions demandées

L'interface présente n'inclut pas encore le menu Compte, la connexion ou les préférences propres à chaque utilisateur. Ces demandes et les changements de lueurs, marges, sélecteurs, statuts, hachures et cellules figurent dans [ROADMAP.md](ROADMAP.md).

Le nouveau souhait de période commune pour Personnel remplace le maintien des deux sélecteurs indépendants comme cible future. Les deux sélecteurs décrivent seulement l'état actuel. Le voile gris demandé remplace les hachures dans la cible future, sans prétendre que ce changement est déjà réalisé.

Les pictogrammes et lueurs de statut doivent rester lisibles et compréhensibles dans les deux thèmes ; les composants métier et les calculs ne sont pas déplacés dans les renderers pour réaliser ce design.

## Validation

Des observations clair/sombre et essais d'assemblage ont été rapportés lors de l'intégration et de la refactorisation. Ils sont historiques. La revue documentaire actuelle n'a ni ouvert l'interface ni relancé ces essais ; voir [VALIDATION.md](VALIDATION.md).

Voir aussi [l'architecture](ARCHITECTURE.md) et [la carte Personnel](PERSONNEL.md).
