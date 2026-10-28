---
summary: "Protections et limites documentées lors de la fiabilisation locale du 20 octobre 2026."
read_when:
  - "examiner les sauvegardes CSV et la récupération après erreur"
  - "reprendre les liens entre absences interventions et remplacements"
---

# Fiabilisation de la version locale — 20 octobre 2026

Les six CSV fournis avec ce projet sont un **jeu de données fictives**, confirmé comme tel par l'utilisatrice, et doivent être conservés dans le dépôt. Les noms, contrats, absences et affectations de ce jeu servent à la démonstration ; ils n'ont pas été remplacés ou convertis pendant la fiabilisation. Les journaux de récupération, temporaires et profils de recette locaux restent exclus de Git. D'éventuelles futures données réelles devront être conservées séparément du jeu de démonstration versionné.

La cible décidée est une application utilisable depuis plusieurs ordinateurs, avec plusieurs comptes, une base centrale et des autorisations contrôlées **sur le serveur**. Cette livraison conserve une application locale utilisant les CSV. Elle ne fournit ni serveur, ni comptes, ni utilisation concurrente de ces fichiers depuis plusieurs postes. Aucune migration des données existantes n'est lancée.

## Protections livrées

- Les six dépôts valident et sérialisent le contenu avant installation. Ils écrivent en UTF-8 dans un temporaire du même dossier, ferment et forcent ce fichier, puis demandent un déplacement atomique. Un système de fichiers refusant ce déplacement provoque un échec explicite, sans repli par troncature du CSV final.
- Les lecteurs acceptent les anciens formats documentés, mais refusent une ligne malformée, une référence absente ou ambiguë, une lecture interrompue et les invariants globaux invalides. Ils ne retournent jamais une liste partielle après une erreur. Un démarrage valide ne réécrit plus les demandes d'absence.
- Les services restaurent leurs listes, leur ordre et les champs modifiés après un échec. Les messages des écrans ne présentent pas l'action comme réussie et conservent les saisies des formulaires intégrés.
- Les caractères `;`, CR et LF sont refusés dans les champs persistés par ce format. Les contrats sont finis et non négatifs, les dates sont comparées et les nouvelles valeurs d'une intervention sont validées avant modification.
- Les homonymes exacts sont temporairement refusés. La suppression et le renommage d'une personne encore référencée sont bloqués. Les autres informations restent modifiables.
- Les conflits d'un salarié sont refusés. Les créneaux jointifs restent permis ; les interventions non attribuées aussi. Un même client peut recevoir plusieurs salariés simultanément après confirmation à l'écran. Les créations ou déplacements vers le passé demandent confirmation.
- La sélection de la liste repose sur les objets effectivement affichés. Les tableaux sans parcours de sauvegarde ne proposent plus d'édition de cellules. Les listes de salariés des remplacements sont actualisées.

## Opérations composées et récupération locale

La création et l'attribution d'une série, les suppressions de série qui affectent deux fichiers, les remplacements manuels et les déclarations/reprises d'absence passent par un coordinateur de sauvegarde. Les appels UI ne juxtaposent plus deux sauvegardes indépendantes.

Le dossier local `.gestion-transaction` contient des copies précédentes, les contenus préparés et un manifeste avec empreintes. Toutes les écritures sont préparées avant la première installation. Un marqueur `prepared` est installé avant de remplacer les CSV, puis `committed` après la dernière installation. **Les remplacements de plusieurs fichiers ne sont pas atomiques.**

- Échec avant la validation finale : compensation des fichiers et restauration de la mémoire.
- Interruption du processus avant `committed` : au prochain chargement, restauration de tous les fichiers précédents avant de lire les services.
- Marqueur `committed` présent : les nouveaux fichiers sont conservés et le journal peut être nettoyé.
- Compensation impossible ou fichier modifié extérieurement : erreur explicite, conservation du journal ; les écritures sont bloquées dans le processus ayant échoué. Fermer l'application, conserver une copie du dossier de données **avec son journal**, puis faire vérifier la récupération. Ne jamais effacer ce journal pour contourner l'erreur.

Le mécanisme suppose une seule instance utilisant un dossier de données. Il ne remplace pas les sauvegardes, une transaction de base centrale ni une garantie contre toute coupure électrique ou panne matérielle. Les copies du journal contiennent des données privées ; elles ne doivent pas être publiées.

## Absences et groupes de remplacement

Extension expressément demandée pendant cette passe : une **nouvelle absence** désattribue les interventions futures entièrement couvertes de son salarié et conserve leur origine dans `Intervention.sourceAbsenceId`. La demande dans `replacements.csv` reste liée par `absenceId`. Les affectations existantes du passé sont conservées. Une intervention en cours ou partiellement couverte entraîne un refus explicite plutôt qu'un découpage implicite.

Le lecteur d'interventions accepte 8, 9 ou 10 colonnes. La dixième est `sourceAbsenceId`, facultative. Le prochain enregistrement normal utilise ce format compatible avec le nouveau lecteur. **Ne pas réutiliser l'ancien exécutable sur des fichiers au nouveau format.** Aucun fichier réel n'est converti par l'installation du code ; aucune reprise rétroactive des anciennes absences n'est exécutée.

La demande affiche le nombre d'interventions attribuées dans son groupe. Le bouton d'attribution de la demande attribue ses interventions futures encore non attribuées au salarié choisi et conserve les attributions déjà faites. Pour répartir le groupe entre plusieurs salariés, utiliser les attributions individuelles du Planning ; le lien au groupe reste enregistré.

Les anciennes demandes sans liens d'interventions restent consultables et sont signalées comme telles. Elles ne sont pas assimilées à des groupes vides à reconstruire automatiquement. Une incohérence de références ou de périodes au chargement demande une vérification manuelle.

## Confinements et suite à développer

- **Reprise anticipée avec interventions liées : bloquée**, même si tout le groupe est encore non attribué. Décider ensuite comment rétablir les affectations, gérer les conflits et respecter les remplacements déjà organisés. Ne pas supprimer les liens pour contourner cette protection.
- Suppression directe d'une demande liée à une absence : bloquée. Modifier les dates ou le client d'une intervention liée : bloqué tant que la réorganisation du groupe n'est pas définie. Son attribution reste possible.
- Remplacement partiel d'une intervention : refus temporaire. Le futur découpage devra conserver la partie assurée par le salarié initial et recueillir l'accord du client pour la partie remplacée.
- « Attribuer la série » et « Supprimer la série » ne touchent qu'aux occurrences qui n'ont pas commencé. Une définition encore référencée par le passé est conservée pour l'historique ; il n'existe pas de régénération automatique. Une future option explicite permettra de traiter le passé.
- Renommage référencé, homonymes réels et identifiants stables : à concevoir avec la base centrale, sans migration CSV lourde maintenant.
- Les objets et certaines collections restent mutables. Les futurs appels doivent passer par les services ; les méthodes techniques d'une seule sauvegarde ne coordonnent pas à elles seules une opération composée. Utiliser `createWithPlanning`, `assignFutureSeries`, `removeFutureSeries` et les opérations d'absence pour les parcours correspondants.
- Pas de fusion des deux formulaires de création, pas de trajets, d'authentification ou de refonte visuelle dans cette livraison.

## Ce qui reste utile côté serveur

Les invariants de `DomainValidation`, les périodes à fin exclusive, les contrôles de conflit, le lien durable d'origine des remplacements, la protection de l'historique et leurs tests sont réutilisables. Leur application autoritaire devra être côté serveur et dans les transactions/contraintes de la base, avec contrôle de concurrence et autorisations de chaque action.

Les dépôts CSV, les clés par noms, le journal de compensation local et les contrôles Swing ne sont pas le stockage ni la frontière de sécurité de cette future architecture. Aucun choix de fournisseur de base, d'authentification, de rôles ou de schéma de migration n'est imposé ici.
