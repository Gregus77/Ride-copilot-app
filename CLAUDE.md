# CLAUDE.md — Ride Copilot

App Android (Kotlin + Jetpack Compose) qui se superpose à Uber Driver / Bolt Driver pour afficher le temps de trajet réel (trafic live) et un score de rentabilité avant d'accepter une course. Voir README.md pour le détail fonctionnel et les limites connues.

## Fichiers clés

| Fichier | Rôle |
|---------|------|
| `app/src/main/java/com/ridecopilot/app/accessibility/RideAccessibilityService.kt` | Service d'accessibilité, écoute Uber Driver / Bolt Driver |
| `app/src/main/java/com/ridecopilot/app/accessibility/RideOfferParser.kt` | Parsing regex des infos de course (durée, distance, prix, adresses) — le plus fragile aux mises à jour des apps tierces |
| `app/src/main/java/com/ridecopilot/app/overlay/OverlayService.kt` | Service foreground, gère la bulle flottante WindowManager + orchestration (geocoding, trafic, score) |
| `app/src/main/java/com/ridecopilot/app/overlay/ui/OverlayCard.kt` | UI Compose de la bulle/carte de score |
| `app/src/main/java/com/ridecopilot/app/network/DirectionsApi.kt` | Appels Google Geocoding + Directions (trafic temps réel) |
| `app/src/main/java/com/ridecopilot/app/domain/ProfitabilityCalculator.kt` | Calcul du score €/h (thermique ou électrique selon `AppSettings.vehicleType`) |
| `app/src/main/java/com/ridecopilot/app/data/SettingsRepository.kt` | Réglages persistés (clé API, type de véhicule, coûts, seuils) via DataStore |
| `app/src/main/java/com/ridecopilot/app/MainActivity.kt` + `SettingsScreen.kt` | Écran unique de configuration/permissions |
| `app/src/main/res/xml/accessibility_service_config.xml` | Packages surveillés (`com.ubercab.driver`, `ee.mtakso.driver`) |
| `app/build.gradle.kts` | `buildConfigField GOOGLE_MAPS_API_KEY` lu depuis la variable d'env `GOOGLE_MAPS_API_KEY` (secret GitHub Actions) |
| `.github/workflows/build-debug-apk.yml` | CI qui build l'APK debug, passe le secret `GOOGLE_MAPS_API_KEY` au build, et publie une Release GitHub avec l'APK en telechargement direct (tag `debug-latest`, lien stable `releases/latest/download/app-debug.apk`) |
| `app/src/main/java/com/ridecopilot/app/ui/theme/Theme.kt` + `Color.kt` | Theme Compose Material3 (vert de marque, palette clair/sombre), applique dans `MainActivity` |

## Ne jamais casser

- Le calcul du score de rentabilité doit toujours inclure le temps d'approche (chauffeur → point de prise en charge), pas seulement le trajet client → destination — c'est le problème central que l'app résout.
- L'app ne doit jamais accepter/refuser une course automatiquement : elle affiche seulement une aide à la décision.
- Le toggle "Surveillance active" (`AppSettings.monitoringEnabled`) ne doit jamais désactiver le service d'accessibilité lui-même — il court-circuite uniquement le traitement dans `RideAccessibilityService.onAccessibilityEvent`. Désactiver le service au niveau système redemande une confirmation manuelle dans les réglages Android à chaque fois, ce qui casserait l'usage du toggle.
- La clé API saisie manuellement dans les réglages (`SettingsRepository.updateApiKey`) doit toujours prendre le dessus sur `BuildConfig.GOOGLE_MAPS_API_KEY` si elle est renseignée — ne jamais inverser cette priorité.

## Contraintes connues

- Android uniquement (iOS ne permet pas ce type d'overlay/lecture d'écran tiers).
- Pas de SDK Android ni d'émulateur dans l'environnement de développement Claude — le build/run doit être vérifié sur Android Studio + appareil physique par l'utilisateur.
- Le wrapper Gradle n'est pas commité (pas de binaire versionné) : Android Studio le régénère au sync.
