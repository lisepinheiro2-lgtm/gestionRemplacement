# Tests et état des validations

État documentaire au **20 octobre 2026**. Présence de code, test déclaré et résultat exécuté sont trois informations différentes. Cette revue a vérifié des textes, chemins et sources, sans compiler ni lancer l'application ou les tests Java.

## Suite présente dans le dépôt

| Classe | Déclarations @Test | Périmètre existant |
| --- | --- | --- |
| [InterventionTest](../test/gestionTest/InterventionTest.java) | 8 | Durées, minutes, nuits et interventions sur plusieurs jours. |
| [PlanningTest](../test/gestionTest/PlanningTest.java) | 8 | Heures dans une période, intersections et limites de mois ; cible WorkHoursService. |
| [BehaviorRegressionTest](../test/gestionTest/BehaviorRegressionTest.java) | 23 | CSV historiques, références, personnel/tri, heures, absences/reprise, erreurs de sauvegarde d'absence, demandes liées, récurrences et assemblage Swing. |
| [InMemoryPreferencesFactory](../test/gestionTest/support/InMemoryPreferencesFactory.java) | Infrastructure | Préférences en mémoire pour isoler les essais des préférences personnelles. |

Le total de **39 déclarations** n'est pas un résultat de 39 réussites pour la version actuelle. Les scénarios de rollback d'absence existants ne couvrent pas automatiquement tous les nouveaux chemins de rollback d'affectation.

## Chronologie des résultats rapportés

| Jalon | Résultat rapporté | Limite |
| --- | --- | --- |
| 2 octobre 2026 | Kit autonome de 48 scénarios : 45 réussites, 3 échecs. | Outil séparé, extérieur au dépôt actuel ; état ancien. |
| Refactorisation du 19 octobre | Compilation Java 25 et 39 tests JUnit réussis, avec comparaison de certains CSV avant/après et observations de Personnel en clair/sombre. | Jalon historique décrit dans la carte du projet ; pas une validation après les derniers changements. |
| Avant la dernière vague du 19 octobre | 23/23 tests de régression passés. | Résultat de régression rapporté avant les changements récents ; ne pas en déduire une exécution récente de toutes les classes. |
| Après cette vague | Aucune campagne établie. | Affectations, périodes, sauvegarde, interface et autres parcours restent à vérifier. |

Les anciens défauts ne deviennent ni résolus ni encore présents uniquement parce qu'un ancien rapport les mentionne : relire le code et, à la phase de tests, vérifier le comportement ciblé.

## Configuration Eclipse

- Sources de production sous `src/gestion`, tests sous `test/gestionTest`, support sous `gestionTest.support`.
- Point d'entrée `gestion.Main`, module encore nommé `gestionRemplacement`.
- `.classpath` déclare le conteneur JUnit 6 et un dossier de sortie distinct `bin-test`.
- Le jalon utilisait Java 25. Sélectionner la classe ou le dossier `test`, puis Run As → JUnit Test.
- Pour la configuration de régression utilisant les préférences en mémoire, conserver l'option VM suivante, constatée dans la configuration locale de lancement :

```text
--add-exports=gestionRemplacement/gestionTest.support=java.prefs
```

Cette option dépend du nom du module, pas du nom du projet Eclipse ; la revoir si le module change. La configuration de lancement locale n'est pas un fichier présent dans le dépôt examiné.

## Vérifications à regrouper après le bloc de modifications

- Périodes nulles, inversées, durée nulle, journée unique, fin à minuit, nuits, limites de semaine/mois et créneaux jointifs.
- Attribution simple/collective, cible absente, doublon, conflit interne/externe, remplacement par le même employé, aucune cible et désattribution.
- Erreur d'écriture/déplacement : propagation, restauration mémoire exacte, conservation du fichier cible et nettoyage du temporaire.
- Autres mutations du planning et cohérence définition/occurrences/demandes/absences entre fichiers.
- Rechargement, identité des personnes, changement de nom et données historiques invalides.
- Parcours des vues, annulations, dialogues, messages de succès/refus/erreur et compteurs rafraîchis.
- Après leur réalisation : trajets, comptes, SMS/courriels et accès aux archives.

Utiliser des données synthétiques, un dossier isolé et les préférences en mémoire. Le démarrage initialise la synchronisation des demandes ; il ne constitue pas une lecture sans effet de tous les fichiers. Ne pas utiliser les données réelles pour provoquer un échec.

Ordre de travail retenu : inventaire, factorisation, fonctionnalités, design, campagne regroupée, puis bilan avant commit. Les vérifications de documentation et de liens ne remplacent pas cette campagne.
