---
summary: "Demandes par écran et décisions sur structures, droits, archives, facturation, concurrence et secours hors ligne."
read_when:
  - "retrouver les fonctionnalités prévues Personnel Clients Planning et Archives"
  - "préparer l’intégration des comptes et permissions avec le serveur"
  - "retrouver les décisions DEC01–DEC06 du 29 octobre 2026"
---

# Feuille de route du projet gestion

## Décisions transversales du 29 octobre 2026

**Précisions de conception, sans livraison de code.** Cette section complète les demandes ci-dessous ; les états techniques historiques ne sont pas réévalués. La référence détaillée commune est la [vision d'ensemble — décisions du 29 octobre](vision-ensemble.md#decisions-metier-2026-10-29).

| Repère commun | Besoin retenu et lien au carnet client |
| --- | --- |
| [DEC01 — Structures](vision-ensemble.md#dec-structures) | Plusieurs structures indépendantes et données isolées côté serveur. Le choix compte unique multi-structures ou comptes séparés reste explicitement ouvert. |
| [DEC02 — Droits](vision-ensemble.md#dec-droits) | Gestion par responsables liés et secrétaires dans leur périmètre ; matrice consulter/créer/modifier/supprimer à préciser. Concerne SOC07 et CPT01–CPT04. |
| [DEC03 — Historique](vision-ensemble.md#dec-historique) | Conserver les documents après départ selon les règles de conservation ; garder les interventions annulées avec une alerte « facturation à déterminer », puis une décision habilitée facturée/non facturée. Concerne INT01 et ADM02–ADM05 ; préserver aussi l'alerte future sur le passé non attribué sans présumer qu'une intervention a été effectuée. |
| [DEC04 — Concurrence](vision-ensemble.md#dec-concurrence) | Verrou temporaire côté serveur et information de l'autre utilisateur, plus contrôle final à l'enregistrement ; aucune modification concurrente écrasée silencieusement. Concerne notamment PLA, INT et RMP. |
| [DEC05 — Secours hors ligne](vision-ensemble.md#dec-hors-ligne) | ADMIN_EMERGENCY utile sur poste autorisé, avec restrictions à définir ; authentification locale sécurisée, chiffrement, audit et réconciliation. « Toutes les opérations » ne vaut pas accès illimité validé. |
| [DEC06 — Documents](vision-ensemble.md#dec-documents) | Administrateur : accès documentaire complet dans sa structure ; comptable : seules pièces nécessaires à sa mission. Catégories, droits et stockage à détailler. Aucun accès aux secrets d'authentification. Concerne SOC07 et ADM01–ADM07. |

L'interface Swing devra présenter les périmètres et les conflits sans tenir lieu de contrôle d'autorisation. Documents et Archives restent des chantiers ; ne pas transformer ces décisions en fonctionnalités annoncées disponibles. Les paramètres encore ouverts sont regroupés dans la référence commune. Aucun test, build, serveur, migration ou changement Git réalisé par cette mise à jour.

## Décisions et livraison de fiabilisation du 20 octobre 2026

**Nouvelle cible validée :** plusieurs postes et comptes, base centrale, autorisations vérifiées côté serveur. Pas de migration ni de serveur mis en place dans cette livraison. Limiter désormais l'investissement dans les CSV aux protections indispensables pendant la transition.

Les règles effectivement livrées et leurs limites sont décrites dans [FIABILISATION.md](FIABILISATION.md). Cette mise à jour prime sur les constats historiques ci-dessous : écritures/lectures et restaurations des six dépôts, coordination avec récupération, protection des références, validations et confirmations, sélection stable, remplacements à la bonne période.

**Absences :** lien durable aux interventions futures désattribuées et regroupement dans la demande, attribution collective des non-attribuées ou individuelle depuis Planning. Les anciennes absences/demandes ne sont pas converties automatiquement. **À développer ensuite : reprise anticipée d'une absence ayant des interventions liées, actuellement bloquée par décision explicite de l'utilisatrice.** Définir le sort des remplaçants déjà prévus, les conflits et le rétablissement des affectations avant de débloquer cette action.

**Historique :** actions globales des séries limitées aux occurrences non commencées ; prévoir ultérieurement une case explicite autorisant le traitement des occurrences passées. Les définitions nécessaires à l'historique sont conservées.

**Autres décisions différées :** découpage d'une intervention partiellement remplacée avec accord du client ; identifiants stables et homonymes avec la base centrale ; modification des dates/clients d'interventions liées à une absence ; suppression/archivage de personnes référencées. Pas de fusion des deux formulaires de création dans cette passe.

Les entrées plus anciennes ci-dessous restent un carnet historique et ne signifient pas que ces corrections sont encore toutes à faire.

Mise en cohérence documentaire du **20 octobre 2026**. Cette liste conserve les améliorations prévues ainsi que les chantiers de fiabilité déjà suivis. **Une amélioration inscrite ici n'est pas une fonctionnalité terminée.** Aucun calendrier de livraison n'est fixé.

« À réaliser » signifie qu'aucune implémentation complète n'est établie dans l'état lu. « Partiel » signale un socle ou une partie existante. La réussite des parcours reste distincte de leur présence dans le code ; voir [VALIDATION.md](VALIDATION.md).

## Ordre de travail

Inventaire → factorisation → fonctionnalités → design → tests regroupés → commit après bilan. La reprise porte d'abord sur la sécurisation des affectations et des erreurs, puis les trajets et Personnel. Les autres domaines restent au carnet de demandes sans priorité relative inventée.

## Fiabilité et services communs

| Repère | État | Travail à terminer |
| --- | --- | --- |
| SOC01 | En cours | Stabiliser les préconditions, les conventions de retour et les exceptions de PlanningService ; garder le succès sans modification pour le même employé et décider si les dates doivent être validées avant ce retour. |
| SOC02 | Partiel | Consolider l'attribution/désattribution/remplacement avec rollback par index ; examiner aussi ajout, édition, suppression, génération/suppression récurrente et tous les autres chemins de mutation. |
| SOC03 | Partiel | Sécuriser les écritures et erreurs des six CSV et leur cohérence. Le temporaire et ATOMIC_MOVE concernent les interventions ; aucune transaction globale n'est démontrée. |
| SOC04 | À réaliser | Harmoniser l'identité des employés/clients, y compris après changement de nom, homonymie et rechargement. |
| SOC05 | À réaliser | Consulter, modifier et annuler absences actuelles/futures ; intégrer leur disponibilité aux attributions et garder une cohérence avec les demandes et transferts. |
| SOC06 | Partiel | Respecter dates et heures de remplacement, dates inclusives des absences et fin exclusive à minuit des demandes ; éviter de sélectionner à tort le jour suivant. |
| TRJ01 | À réaliser | Intégrer les trajets automatiques avec la marge demandée de +5 minutes ; TravelTimeService est présent mais vide. |
| HOR01 | À réaliser | Permettre les corrections d'horaires avec confirmation avant application et recalcul des conséquences ; le détail du parcours reste à préciser. |
| SOC07 | À réaliser | Protéger les données sensibles et secrets, en cohérence avec les futurs comptes/droits ; aucun suivi privé ou fichier personnel réel ne doit devenir une documentation publique. |

Pour les trajets, définir les entrées, la règle précise des cinq minutes, les échecs/adresses absentes, les recalculs et l'effet sur les compteurs. Aucun prestataire de trajet n'est choisi dans cet état.

## Personnel

| Repère | Type | Demande et état |
| --- | --- | --- |
| PER01 | Fonction | Attribuer ou désattribuer des heures par double clic sur le compteur, avec choix des interventions. Non branché dans Personnel ; les méthodes de PlanningService sont un socle à utiliser. |
| PER02 | Fonction et UI | Regrouper les deux choix de période en une seule sélection : attribuées/restantes pour la même semaine ou le même mois. Les sélecteurs indépendants sont l'état actuel, pas la cible future. |
| PER03 | Navigation | Remplacer les navigations séparées par un seul spinner suivant semaine/mois selon le choix commun. |
| PER04 | UI | Éloigner les labels du bord gauche et les champs du bord droit en gardant l'adaptation au redimensionnement. |
| PER05 | UI | Corriger la lueur qui reste active après un clic sans sélection et augmenter légèrement l'effet demandé. |
| PER06 | UI | Opérationnel : lueur verte interne et personnage vert marchant ; Congés : lueur rouge et personnage orange sur chaise longue ; Maladie : lueur rouge et croix médicale. L'apparence « Autre absence » reste à préciser. |
| PER07 | UI | Lueur bleue au survol de chaque zone réellement modifiable. |
| PER08 | Compte | Remplacer le bouton Paramètres par un pictogramme/menu Compte ; détails communs ci-dessous. |

## Comptes et préférences utilisateur

| Repère | Type | Demande et état |
| --- | --- | --- |
| CPT01 | Fonction | Nouvelle vue de connexion avant MainUI ; conserver HomePanel pour l'utilisateur connecté. À réaliser. |
| CPT02 | Fonction et UI | Menu avec Mon compte, Préférences utilisateur, Changer d'utilisateur et Se déconnecter. À réaliser. |
| CPT03 | Fonction | Comptes distincts, stockage sécurisé des mots de passe et gestion d'une session active ; droits d'accès éventuels à concevoir. Aucun système d'authentification n'est établi actuellement. |
| CPT04 | Fonction | Préférences par utilisateur : thème, affichage, période par défaut et autres réglages retenus. Les préférences actuelles sont celles du profil système, pas de comptes applicatifs distincts. |

La conception des comptes et des droits doit précéder l'exposition des documents sensibles et du journal complet. Le fournisseur d'authentification, le stockage et le détail des rôles ne sont pas arrêtés.

## Clients

| Repère | Type | Demande et état |
| --- | --- | --- |
| CLI01 | Fonction | Refuser une adresse incomplète, notamment sans code postal ou ville. Le contrôle actuel vérifie surtout qu'elle n'est pas vide. |
| CLI02 | Fonction et UI | Ajouter téléphone, saisie et colonne ; permettre de choisir l'affichage du numéro du client, du contact d'urgence ou du gestionnaire des interventions (parent, tuteur, etc.). |
| CLI03 | Fonction et UI | Statut des interventions du client éditable par double clic : en cours vert, suspendu rouge avec dates au survol, délocalisé orange avec nouvelle adresse au survol. Définir ses effets sur le planning ; ne pas le confondre avec le statut d'un employé. |
| CLI04 | Présentation | Afficher les dates en `dd/MM/uuuu`. FormatUtils sait déjà le faire, mais le tableau Clients utilise encore `LocalDate.toString()` ; harmonisation restante. Ne pas modifier le format CSV pour une seule demande d'affichage. |
| CLI05 | UI | Lueur bleue au survol des zones éditables par double clic ; relier toute édition à une vraie sauvegarde ou la rendre non modifiable. |
| CLI06 | Fonction | Conserver le besoin déjà suivi d'un profil client aidant au choix de l'intervenant ; articuler ce profil avec les futures informations Administration/Archives. |

## Planning

| Repère | Type | Demande et état |
| --- | --- | --- |
| PLA01 | Navigation et UI | Aujourd'hui doit faire défiler jusqu'au jour courant et surligner sa date en bleu. Le retour à sa période existe ; le défilement/surlignage demandé reste à réaliser. |
| PLA02 | Fonction | Ajouter une vue journée aux vues semaine et mois. |
| PLA03 | Fonction | Envoyer le planning semaine/mois par courriel à tous ou à un seul destinataire. Destinataires, aperçu et prestataire à définir. |
| PLA04 | UI | Remplacer les hachures des périodes passées par un voile grisé. Les hachures restent l'état actuel. |

## Remplacements

| Repère | Type | Demande et état |
| --- | --- | --- |
| RMP01 | Calcul et UI | Remplacer Heures contrat par les heures nécessitant un remplaçant et les heures disponibles des remplaçants potentiels ; définir la période et les règles de disponibilité. |
| RMP02 | Présentation | Regrouper début date+heure et fin date+heure dans leurs deux colonnes respectives. |
| RMP03 | Fonction | Double clic sur les heures à remplacer : ouvrir la liste des interventions concernées et permettre une attribution unique ou multiple. Le socle collectif du service ne constitue pas cette interface. |
| RMP04 | Fonction et UI | Retirer le champ Employé remplaçant et la case Tout attribuer une fois le nouveau parcours branché ; ils existent encore actuellement. |
| RMP05 | UI | Lueur bleue sur les zones éditables au survol. |
| RMP06 | Calcul et UI | Vert si tout est attribué, rouge si rien ne l'est, orange si partiellement attribué ; calculer ces états depuis les interventions, pas seulement depuis un remplaçant global. |

Préserver le lien à l'absence, les affectations existantes et les annulations. Supprimer une demande ne doit pas être assimilé silencieusement à une annulation d'absence.

## Liste des interventions et récurrences

| Repère | Type | Demande et état |
| --- | --- | --- |
| INT01 | Fonction | Masquer les interventions passées de la liste active et les rendre consultables dans Archives. Prévoir leur accès avant de les masquer ; aucune suppression de l'historique n'est demandée. |
| INT02 | Présentation | Rétablir les majuscules des noms/prénoms clients et intervenants. Le tableau force actuellement les minuscules ; conserver l'orthographe stockée puis préciser la règle de présentation voulue. |
| INT03 | Fonction et UI | Retirer le sélecteur Trier par et utiliser l'ordre Dates par défaut. Cette demande récente remplace l'ancien souhait de conserver ce sélecteur ; préserver la recherche. |
| INT04 | Fonction | Rendre la colonne Client non modifiable et supprimer l'édition visuelle sans persistance. Le modèle actuel laisse éditer les cellules sans enregistrer ces changements. |
| INT05 | Fonction | Repenser les récurrences pour plusieurs jours et horaires, identiques ou différents, avec un modèle de série explicite. La définition actuelle représente un jour et une plage horaire. |
| INT06 | Fonction | Rendre le double clic effectif sur les autres colonnes avec validation, sauvegarde et rafraîchissement ; définir le périmètre occurrence/série. |
| INT07 | Fonction et UI | Une intervention unique ne doit pas demander de date de fin dans le parcours courant. Préciser le cas des nuits/pluri-journalières pour conserver la fin réelle dans le modèle. |
| INT08 | Fonction | Préremplir les dates de récurrence depuis le contrat du client, en les gardant éditables ; définir le cas des dates de contrat absentes. |
| INT09 | Fonction | Après Aucun résultat, ne pas laisser la liste vide une fois le message fermé ; choisir retour à la liste précédente ou remise à zéro du filtre. Le code actuel rafraîchit avec une liste vide. |
| INT10 | Fonction | Lors du déplacement d'une intervention, envoyer automatiquement un SMS au client si l'utilisatrice coche l'option prévue. Envoi non implémenté ; préciser destinataire, contenu, confirmation et prévention des doublons. |
| INT11 | UI | Lueur verte si attribuée, rouge sinon. |
| INT12 | UI | Lueur bleue au survol des zones éditables. |
| INT13 | UI | Retirer la lueur au survol des titres de colonnes sans action. |

La création, modification, suppression et attribution de séries doivent conserver la cohérence entre la définition et les occurrences, y compris lors d'un échec de sauvegarde. Examiner aussi les récurrences nocturnes, que la génération actuelle ne modélise pas avec une date de fin différente.

## Administration et Archives

Le cahier de demandes regroupe sous « Archives » des informations actuelles, documents administratifs et historique. La séparation future Administration/Archives est à concevoir ; ne pas faire passer ce choix d'organisation pour un écran déjà créé.

| Repère | Type | Demande et état |
| --- | --- | --- |
| ADM01 | Fonction | Informations du personnel : adresse, courriel, téléphone et contact d'urgence. |
| ADM02 | Fonction | Documents du personnel : fiches de paie, contrat, RIB, diplômes/certifications. |
| ADM03 | Fonction | Informations clients : adresse, coordonnées du gestionnaire du contrat, contexte familial, pathologie, besoins et contact d'urgence. |
| ADM04 | Fonction | Documents clients : factures, contrat, RIB et autorisation de prélèvement. |
| ADM05 | Fonction | Consultation de l'archive des interventions. |
| ADM06 | Fonction | Frais et revenus de l'entreprise, liste détaillée par mois/année et prévisionnel. |
| ADM07 | Traçabilité | Journal des modifications réservé à un compte avec accès complet : changement effectué, auteur, date/heure et appareil. Le niveau de détail et l'identification de l'appareil restent à définir. |

Ces besoins peuvent concerner des données sensibles. Définir accès, stockage et conservation avant leur implémentation. Aucun document réel ni donnée personnelle n'est nécessaire dans le dépôt public pour décrire ces fonctions.

## Décisions encore ouvertes

- Règle des cinq minutes et comptabilisation des trajets ; parcours précis des corrections d'horaires.
- Date de fin implicite d'une intervention unique nocturne/pluri-journalière.
- Format souple des récurrences et portée occurrence/série des modifications.
- Actions exactes des nouveaux statuts clients sur les interventions existantes et futures.
- Retour après une recherche sans correspondance ; présentation exacte des majuscules.
- Effet d'un double clic sur des heures nulles/négatives ou une période passée.
- Apparence du statut Autre absence, rôles des comptes et séparation Administration/Archives.
- Prestataires et configuration des courriels/SMS, en évitant les doublons et tout secret publié.

Aucune de ces décisions n'a été tranchée par la seule mise à jour documentaire.
