# Ride Copilot

Application Android qui se superpose à Uber Driver / Bolt Driver : dès qu'une course est proposée, une bulle flottante affiche le **temps de trajet réel** (avec trafic en direct, approche + course) comparé au temps annoncé par l'app, ainsi qu'un **score de rentabilité en €/h** pour décider en quelques secondes si la course vaut le coup.

## Pourquoi ce projet

Les apps de VTC annoncent parfois un temps optimiste (ex: 30 min) alors que le trajet réel une fois la course acceptée prend beaucoup plus longtemps (ex: 45 min) à cause du trafic. Ride Copilot recalcule ce temps en temps réel via Google Maps (trafic live) et affiche un score qui tient compte du prix de la course et du coût du carburant.

## Comment ça marche

1. **Service d'accessibilité Android** (`RideAccessibilityService`) : écoute les écrans de Uber Driver (`com.ubercab.driver`) et Bolt Driver (`ee.mtakso.driver`) et détecte l'apparition d'une popup de course (durée annoncée, distance, prix, adresses).
2. **`RideOfferParser`** : extrait ces informations par regex à partir du texte affiché à l'écran (approche heuristique, voir limites ci-dessous).
3. **`OverlayService`** : affiche une bulle flottante (`WindowManager` + Jetpack Compose) au-dessus de Uber/Bolt.
4. **`DirectionsApi`** : géocode les adresses détectées et interroge l'API Google Directions avec `departure_time=now` pour obtenir le temps de trajet réel, en routant `position actuelle du chauffeur -> point de prise en charge -> destination` (donc y compris le temps pour aller chercher le client, pas seulement le trajet client).
5. **`ProfitabilityCalculator`** : calcule le gain net (prix course - coût carburant) et le taux horaire (€/h), classé Bon / Correct / Faible.

## Mise en place

### 1. Prérequis
- Android Studio (Koala ou plus récent)
- Un smartphone Android (API 26+) avec Uber Driver et/ou Bolt Driver installés
- Une clé API Google Cloud avec les APIs suivantes activées :
  - **Directions API**
  - **Geocoding API**

### 2. Ouvrir le projet
Ouvrir ce dossier dans Android Studio. Le wrapper Gradle n'est pas commité (pas de binaire dans ce repo) — au premier "Sync", Android Studio propose de le générer automatiquement. Sinon : `gradle wrapper --gradle-version 8.7` si Gradle est installé en local.

### 3. Build & installation
Lancer le module `app` sur un appareil physique (l'overlay et le service d'accessibilité ne sont pas testables sur un simple émulateur sans Play Services complets).

### 4. Configuration dans l'app
1. Ouvrir Ride Copilot
2. Autoriser l'affichage par-dessus les autres apps
3. Activer le service d'accessibilité "Ride Copilot" dans les réglages Android
4. Renseigner la clé API Google Maps
5. Renseigner la consommation du véhicule (L/100km) et le prix du carburant (€/L)
6. Ouvrir Uber Driver ou Bolt Driver : la bulle apparaît automatiquement dès qu'une course est proposée

Le toggle **"Surveillance active"** en haut de l'écran permet de mettre en pause la détection des courses (par exemple pendant une pause) sans avoir à retirer l'autorisation d'accessibilité dans les réglages Android — pratique car cette autorisation redemande souvent une confirmation manuelle une fois retirée.

## Limites connues (important)

- **iOS non supporté.** Apple interdit à une app tierce de lire l'écran d'une autre app ou d'afficher une bulle système par-dessus — ce projet ne peut exister que sur Android.
- **Zone grise CGU.** Lire automatiquement le contenu affiché par Uber/Bolt via le service d'accessibilité peut être en tension avec leurs conditions d'utilisation. C'est un usage côté utilisateur pour l'aider à décider, pas un scraping tiers, mais le risque (avertissement/suspension de compte chauffeur) est réel et à la charge de l'utilisateur.
- **Extraction fragile.** `RideOfferParser` repose sur des regex appliquées au texte visible à l'écran. Toute mise à jour de l'UI Uber/Bolt peut casser la détection. Pour calibrer : `adb shell uiautomator dump` sur l'écran de proposition de course, inspecter le XML généré, et ajuster les regex dans `RideOfferParser.kt`.
- **Noms de packages.** `com.ubercab.driver` et `ee.mtakso.driver` sont les noms de packages connus pour Uber Driver et Bolt Driver. Vérifier sur l'appareil cible avec `adb shell pm list packages | grep -iE "uber|bolt|mtakso"` et ajuster `accessibility_service_config.xml` + `RideAccessibilityService.kt` si besoin.
- **Prix de la course.** Le prix n'est pas toujours visible avant acceptation selon l'app/le marché — dans ce cas le score de rentabilité ne peut pas être calculé (seul l'écart de temps réel vs annoncé est affiché).
- **Coût réel non testé en conditions live** dans cet environnement (pas de SDK Android/émulateur disponible ici) : à valider sur un appareil physique avant usage quotidien.

## Roadmap possible

- Notification-based detection en complément de l'Accessibility Service (moins intrusif)
- Seuils de rentabilité (Bon/Correct/Faible) réglables dans l'écran de paramètres
- Historique des courses acceptées/refusées avec rentabilité réelle a posteriori
- Alerte sonore/vibration configurable quand le score est mauvais

## Avertissement

Cette application n'automatise ni n'accepte aucune course à la place de l'utilisateur : elle affiche uniquement une information d'aide à la décision. L'utilisateur reste seul responsable de l'utilisation de cette app vis-à-vis des CGU de Uber et Bolt.
