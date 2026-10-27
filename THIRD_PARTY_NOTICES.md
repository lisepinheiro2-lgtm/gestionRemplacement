# Composants tiers — attributions et licences

Inventaire documentaire du **27 octobre 2026**. Les restrictions du [code original de Gestion+](LICENSE.md) ne s'appliquent pas aux éléments tiers. Les textes complets des licences prévalent sur les résumés ci-dessous. Aucune exclusivité sur ces composants n'est revendiquée.

## Raven — composants Swing

Attribution conservée : **Raven Laing (DJ-Raven)**, auteur de composants graphiques réutilisés et adaptés dans Gestion+. Merci à Raven pour leur mise à disposition. [Profil officiel](https://github.com/DJ-Raven).

La notice existante [licenses/Raven-MIT.txt](licenses/Raven-MIT.txt), laissée intacte, porte `Copyright (c) 2022 Raven Laing` et le texte MIT complet. `src/gestion/ui/raven/DarkLightSwitchIcon.java` conserve `@author RAVEN`. `RavenDarkLightSwitch.java`, `RavenStyledButton.java` et `RavenTone.java` se présentent comme des adaptations RavenGallery relevant de la licence des sources Raven originales. Les portions Raven de ces fichiers restent sous MIT ; elles ne sont pas placées sous les restrictions du code original de Gestion+.

MIT permet notamment l'utilisation, la modification, la redistribution et la vente, y compris commerciale, sous réserve de conserver la notice de copyright et la permission dans les copies ou portions substantielles. Conserver le texte complet fourni, y compris l'exclusion de garantie. Une utilisation conforme à MIT n'est pas un vol et ne nécessite pas l'autorisation de l'autrice de Gestion+ pour les portions Raven.

**Limite de provenance :** aucun lien vers le dépôt amont exact, tag ou commit d'origine n'a été retrouvé dans les fichiers locaux examinés. Le profil officiel confirme l'identité Raven Laing ; il ne suffit pas à authentifier la provenance de chaque adaptation. La notice locale est conservée sans prétendre qu'une comparaison complète avec une révision amont a été effectuée. Les modifications locales (dont le package `gestion.ui.raven` et les adaptations d'intégration) ne doivent pas être attribuées indistinctement à Raven.

Les ressources `src/icons/theme/dark.svg` et `light.svg`, utilisées par cet interrupteur, ne comportent pas de notice de provenance dans les fichiers examinés. Leur auteur et leur licence exacte restent à confirmer ; leur présence aux côtés du code Raven ne prouve pas qu'elles sont sous MIT. Aucune nouvelle licence ni revendication de propriété exclusive n'est attribuée à ces SVG par cette documentation. Cette incertitude reste à résoudre pour établir une provenance complète.

## FlatLaf et FlatLaf Extras — Apache License 2.0

- Fichiers présents et référencés dans `.classpath` : `lib/flatlaf-3.7.2.jar` et `lib/flatlaf-extras-3.7.2.jar`.
- Éditeur : **FormDev Software GmbH** ; les manifestes indiquent la version 3.7.2 et le copyright 2019–2026 FormDev Software GmbH.
- Chaque JAR contient `META-INF/LICENSE`, texte Apache 2.0. Une copie exacte de celui du JAR FlatLaf est fournie dans [licenses/Apache-2.0.txt](licenses/Apache-2.0.txt) ; les deux textes embarqués ont été comparés.
- Aucun fichier `NOTICE` n'a été trouvé dans ces deux JAR lors de cette inspection. Cela ne dispense pas de conserver un éventuel `NOTICE` fourni par une autre version ou distribution.
- Références officielles : [FlatLaf](https://www.formdev.com/flatlaf/), [licence du tag 3.7.2](https://github.com/JFormDesigner/FlatLaf/blob/3.7.2/LICENSE), [texte Apache 2.0](https://www.apache.org/licenses/LICENSE-2.0).

Apache 2.0 autorise les usages commerciaux. Lors d'une redistribution, fournir la licence, conserver les notices pertinentes dans les sources distribuées et reproduire les attributions d'un éventuel `NOTICE` selon la section 4. Les fichiers tiers modifiés doivent porter une indication visible de modification. La licence comprend aussi des dispositions sur les brevets, les marques et les garanties ; ce résumé ne les remplace pas. Les JAR et leurs notices embarquées n'ont pas été modifiés.

## JSVG — MIT

- Fichier présent et référencé dans `.classpath` : `lib/jsvg-2.1.0.jar` ; version 2.1.0 confirmée dans son manifeste.
- Auteur et notice embarquée : **Jannis Weis**, `Copyright (c) 2021-2024 Jannis Weis`.
- `META-INF/LICENSE` contient MIT. Copie exacte dans [licenses/JSVG-MIT.txt](licenses/JSVG-MIT.txt).
- Référence officielle : [weisJ/jsvg](https://github.com/weisJ/jsvg), qui annonce MIT. La notice du JAR local fait foi pour le texte reproduit ici, sans déduire sa date de celle de la dernière version du site.

Conserver la notice de copyright et la permission dans les copies ou portions substantielles, avec le texte complet fourni et son exclusion de garantie. Les usages commerciaux restent autorisés selon MIT.

## Portée de la vérification

Lecture des fichiers locaux, de la documentation d'interface, des sources et mentions Raven, des trois JAR et de leurs licences embarquées, complétée par les références officielles ci-dessus. Les JAR ont été lus comme archives, sans exécution. Cette inspection n'est ni une certification de provenance exhaustive de tout l'historique, ni une validation juridique.

Pour toute distribution autorisée de Gestion+, accompagner les composants des licences et attributions applicables, y compris si l'emballage change. Les permissions propres aux composants tiers restent indépendantes des permissions portant sur l'application originale.
