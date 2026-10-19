# Accueil Gestion + avec Raven

L'accueil conserve les trois rangées initiales : Personnel / Clients, Planning / Remplacements, Interventions / Archives. Les cinq destinations actives utilisent les méthodes de navigation existantes. Archives reste désactivé avec « À venir », car ArchivedInterventionUI est encore vide.

Les cartes héritent du bouton stylé Raven. L'interrupteur animé clair/sombre et ses deux SVG viennent du composant Raven DarkLightSwitchIcon. Les icônes des six espaces sont dessinées en Java et s'adaptent à la taille d'affichage. FlatLaf 3.7.2, FlatLaf Extras 3.7.2 et JSVG 2.1.0 sont repris des bibliothèques locales de RavenGallery.

Le thème est mémorisé comme avant. L'interrupteur du bandeau et la case Mode sombre dans Paramètres utilisent le même réglage. UIUtils préserve le rendu propre aux nouveaux composites et continue d'appliquer les couleurs métier des écrans existants. La taille minimale est 760 × 650 afin de garder les six cartes utilisables.

Fichiers principaux :

- `src/gestionRemplacement/ui/HomePanel.java` : présentation de l'accueil et ses six accès.
- `src/gestionRemplacement/ui/HomeCardButton.java` : apparence et comportement des cartes.
- `src/gestionRemplacement/ui/ApplicationHeader.java` : identité, écran courant, retour à l'accueil, thème et paramètres.
- `src/gestionRemplacement/ui/NavigationIcon.java` : icônes vectorielles.
- `src/gestionRemplacement/ui/raven/` : composants Raven adaptés au package de l'application, avec licence MIT dans `licenses/`.
- `src/gestionRemplacement/ui/MainUI.java` : assemblage et navigation.
- `src/gestionRemplacement/ui/UIUtils.java` : thème partagé et installation de FlatLaf.

Sauvegarde préalable des sources et du Build Path : `C:/Users/lisep/.codex/.chatgpt-projects/g-p-6ab52fec5098819184c6c56fef03660f/gestion-redesign/backup`.

Validation préparatoire : compilation Java 25 en mode modules, rendu clair/sombre à 1108 et 760 pixels de large, cinq callbacks de navigation conservés et archives désactivées. Les essais visuels dans Eclipse sont consignés dans le rapport de cette intégration. Aucune classe métier ni aucun fichier CSV n'est modifié par l'installateur.


Après la refactorisation du 19 octobre : les icônes SVG du thème sont dans `src/icons/theme`. Le point d’entrée reste `gestionRemplacement.Main`. Voir `ARCHITECTURE.md` et `PERSONNEL.md` pour les classes actuelles.
