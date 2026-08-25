# Ride Copilot

Application Android qui se superpose à Uber Driver / Bolt Driver : dès qu'une course est proposée, une bulle flottante affiche le **temps de trajet réel** (avec trafic en direct, approche + course) comparé au temps annoncé par l'app, ainsi qu'un **score de rentabilité en €/h** pour décider en quelques secondes si la course vaut le coup.

## Pourquoi ce projet

Les apps de VTC annoncent parfois un temps optimiste (ex: 30 min) alors que le trajet réel une fois la course acceptée prend beaucoup plus longtemps (ex: 45 min) à cause du trafic. Ride Copilot recalcule ce temps en temps réel via Google Maps (trafic live) et affiche un score qui tient compte du prix de la course et du coût du carburant.

## Comment ça marche

1. **Service d'accessibilité Android** (`RideAccessibilityService`) : écoute les écrans de Uber Driver (`com.ubercab.driver`) et Bolt Driver (`ee.mtakso.driver`) et détecte l'apparition d'une popup de course (durée annoncée, distance, prix, adresses).
2. **`RideOfferParser`** : extrait ces informations par regex à partir du texte affiché à l'écran (approche heuristique, voir limites ci-dessous).
3. **`OverlayService`** : affiche une bulle flottante (`WindowManager` + Jetpack Compose) au-dessus de Uber/Bolt.
4. **`DirectionsApi`** : géocode les adresses détectées et interroge l'API Google Directions avec `departure_time=now` pour obtenir le temps de trajet réel, en routant `position actuelle du chauffeur -> point de prise en charge -> destination` (donc y compris le temps pour aller chercher le client, pas seulement le trajet client).
5. **`ProfitabilityCalculator`** : calcule le gain net (prix course - coût de l'énergie) et le taux horaire (€/h), classé Bon / Correct / Faible. Fonctionne pour un véhicule thermique (L/100km + €/L) ou électrique (kWh/100km + €/kWh).

## Mise en place

### 1. Prérequis
- Un smartphone Android (API 26+) avec Uber Driver et/ou Bolt Driver installés
- Une clé API Google Cloud avec les APIs suivantes activées :
  - **Directions API**
  - **Geocoding API**

### 2. Build via GitHub Actions (recommandé, pas besoin d'Android Studio)
Chaque push sur `main` compile automatiquement un APK debug.

**Telechargement direct (le plus simple)** : ouvre ce lien depuis le navigateur du telephone —
```
https://github.com/Gregus77/Ride-copilot-app/releases/latest/download/app-debug.apk
```
Ca telecharge le `.apk` directement, sans zip a extraire. Une notification de telechargement apparait ensuite : tape dessus pour lancer l'installation directement.

(Alternative : l'onglet **Actions** du repo propose aussi l'APK zippe en artifact `ride-copilot-debug`, mais ca demande une extraction manuelle avant installation — a eviter si possible.)

Pour que la clé API Google Maps soit **injectée automatiquement** au build (pas besoin de la saisir dans l'app) :
1. GitHub → ce repo → **Settings → Secrets and variables → Actions → New repository secret**
2. Nom : `GOOGLE_MAPS_API_KEY`, valeur : ta clé API Google Cloud
3. Au prochain push, le build embarque cette clé (`BuildConfig.GOOGLE_MAPS_API_KEY`) — l'écran de réglages affichera "Cle deja configuree" et aucune saisie manuelle n'est nécessaire. Une saisie manuelle dans l'app reste possible et prend toujours le dessus si renseignée.

### 3. Build local (alternative)
Ouvrir ce dossier dans Android Studio (Koala ou plus récent). Le wrapper Gradle n'est pas commité — au premier "Sync", Android Studio propose de le générer automatiquement. Sans le secret CI, définir `GOOGLE_MAPS_API_KEY` comme variable d'environnement avant de builder, sinon saisir la clé manuellement dans l'app.

### 4. Installation
Installer l'APK sur un appareil physique (l'overlay et le service d'accessibilité ne sont pas testables sur un simple émulateur sans Play Services complets). Comme l'app n'est pas publiée sur le Play Store, Android/Play Protect afficheront un avertissement "application inconnue" — c'est normal pour tout APK installé hors Play Store, il suffit de choisir "Installer quand même".

### 5. Configuration dans l'app
1. Ouvrir Ride Copilot
2. Autoriser l'affichage par-dessus les autres apps
3. Activer le service d'accessibilité "Ride Copilot" dans les réglages Android
4. Si la clé API n'est pas déjà configurée automatiquement, la renseigner
5. Choisir le type de véhicule (Thermique ou Électrique) et renseigner le coût correspondant
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
