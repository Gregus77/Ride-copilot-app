package com.ridecopilot.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ridecopilot.app.data.SettingsRepository
import com.ridecopilot.app.data.WeeklyDistanceTracker
import com.ridecopilot.app.domain.AppSettings
import com.ridecopilot.app.domain.ElectricPricingMode
import com.ridecopilot.app.domain.VehicleType
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    weeklyDistanceTracker: WeeklyDistanceTracker,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings by settingsRepository.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())
    val kmThisWeek by weeklyDistanceTracker.kmThisWeekFlow.collectAsStateWithLifecycle(initialValue = 0.0)

    var apiKeyField by remember(settings.googleMapsApiKey) { mutableStateOf(settings.googleMapsApiKey) }

    var fuelConsumptionField by remember(settings.fuelConsumptionL100km) {
        mutableStateOf(settings.fuelConsumptionL100km.toString())
    }
    var fuelPriceField by remember(settings.fuelPricePerLiter) {
        mutableStateOf(settings.fuelPricePerLiter.toString())
    }
    var electricConsumptionField by remember(settings.electricConsumptionKwh100km) {
        mutableStateOf(settings.electricConsumptionKwh100km.toString())
    }
    var electricPriceField by remember(settings.electricPricePerKwh) {
        mutableStateOf(settings.electricPricePerKwh.toString())
    }
    var weeklyPriceField by remember(settings.weeklyRentalPriceEuros) {
        mutableStateOf(settings.weeklyRentalPriceEuros.toString())
    }
    var weeklyIncludedKmField by remember(settings.weeklyIncludedKm) {
        mutableStateOf(settings.weeklyIncludedKm.toString())
    }
    var extraKmPriceField by remember(settings.extraKmPriceEuros) {
        mutableStateOf(settings.extraKmPriceEuros.toString())
    }
    var kmThisWeekField by remember(kmThisWeek) { mutableStateOf(kmThisWeek.toInt().toString()) }

    var goodThresholdField by remember(settings.goodHourlyThreshold) {
        mutableStateOf(settings.goodHourlyThreshold.toString())
    }
    var okThresholdField by remember(settings.okHourlyThreshold) {
        mutableStateOf(settings.okHourlyThreshold.toString())
    }

    var minFareField by remember(settings.minimumFareEuros) { mutableStateOf(settings.minimumFareEuros.toString()) }
    var maxApproachField by remember(settings.maxApproachDistanceKm) {
        mutableStateOf(settings.maxApproachDistanceKm.toString())
    }
    var maxTripField by remember(settings.maxTripDistanceKm) {
        mutableStateOf(settings.maxTripDistanceKm.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 20.dp, vertical = 28.dp)
        ) {
            Column {
                Text(
                    "Ride Copilot",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "Temps de trajet reel + score de rentabilite avant d'accepter une course.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Surveillance active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Desactive la detection des courses sans retirer l'autorisation d'accessibilite.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = settings.monitoringEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch { settingsRepository.updateMonitoringEnabled(enabled) }
                        }
                    )
                }
            }

            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "1. Autorisations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Button(onClick = onRequestOverlayPermission, modifier = Modifier.fillMaxWidth()) {
                        Text("Autoriser l'affichage par-dessus les autres apps")
                    }
                    Button(onClick = onRequestAccessibilityPermission, modifier = Modifier.fillMaxWidth()) {
                        Text("Activer le service d'accessibilite")
                    }
                }
            }

            // La cle API n'est visible que si aucune n'est deja configuree (secret CI ou
            // saisie precedente) : les utilisateurs non-techniques n'ont jamais a y toucher.
            if (settings.googleMapsApiKey.isBlank()) {
                ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            "2. Cle API Google Maps",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Aucune cle configuree. Idealement, demande a la personne qui a compile " +
                                "l'app de l'integrer directement (secret GOOGLE_MAPS_API_KEY) pour ne " +
                                "jamais avoir a la saisir ici. Sinon, colle-la ci-dessous.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        OutlinedTextField(
                            value = apiKeyField,
                            onValueChange = { apiKeyField = it },
                            label = { Text("Cle API") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = { scope.launch { settingsRepository.updateApiKey(apiKeyField) } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enregistrer la cle")
                        }
                    }
                }
            }

            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "3. Vehicule et cout de l'energie",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { scope.launch { settingsRepository.updateVehicleType(VehicleType.THERMAL) } },
                            colors = if (settings.vehicleType == VehicleType.THERMAL) {
                                ButtonDefaults.buttonColors()
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            }
                        ) {
                            Text("Thermique")
                        }
                        Button(
                            onClick = { scope.launch { settingsRepository.updateVehicleType(VehicleType.ELECTRIC) } },
                            colors = if (settings.vehicleType == VehicleType.ELECTRIC) {
                                ButtonDefaults.buttonColors()
                            } else {
                                ButtonDefaults.outlinedButtonColors()
                            }
                        ) {
                            Text("Electrique")
                        }
                    }

                    if (settings.vehicleType == VehicleType.THERMAL) {
                        OutlinedTextField(
                            value = fuelConsumptionField,
                            onValueChange = { fuelConsumptionField = it },
                            label = { Text("Consommation (L/100km)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = fuelPriceField,
                            onValueChange = { fuelPriceField = it },
                            label = { Text("Prix carburant (€/L)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                scope.launch {
                                    settingsRepository.updateFuelConsumption(
                                        fuelConsumptionField.toDoubleOrNull() ?: settings.fuelConsumptionL100km
                                    )
                                    settingsRepository.updateFuelPrice(
                                        fuelPriceField.toDoubleOrNull() ?: settings.fuelPricePerLiter
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Enregistrer les couts")
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch { settingsRepository.updateElectricPricingMode(ElectricPricingMode.PER_KWH) }
                                }
                            ) {
                                Text(
                                    "Prix au kWh",
                                    fontWeight = if (settings.electricPricingMode == ElectricPricingMode.PER_KWH) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    scope.launch { settingsRepository.updateElectricPricingMode(ElectricPricingMode.WEEKLY_RENTAL) }
                                }
                            ) {
                                Text(
                                    "Forfait hebdo",
                                    fontWeight = if (settings.electricPricingMode == ElectricPricingMode.WEEKLY_RENTAL) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                            }
                        }

                        if (settings.electricPricingMode == ElectricPricingMode.PER_KWH) {
                            OutlinedTextField(
                                value = electricConsumptionField,
                                onValueChange = { electricConsumptionField = it },
                                label = { Text("Consommation (kWh/100km)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = electricPriceField,
                                onValueChange = { electricPriceField = it },
                                label = { Text("Prix electricite (€/kWh)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        settingsRepository.updateElectricConsumption(
                                            electricConsumptionField.toDoubleOrNull() ?: settings.electricConsumptionKwh100km
                                        )
                                        settingsRepository.updateElectricPrice(
                                            electricPriceField.toDoubleOrNull() ?: settings.electricPricePerKwh
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enregistrer les couts")
                            }
                        } else {
                            OutlinedTextField(
                                value = weeklyPriceField,
                                onValueChange = { weeklyPriceField = it },
                                label = { Text("Prix de location a la semaine (€)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Electricite incluse dans ce forfait", style = MaterialTheme.typography.bodyMedium)
                                Switch(
                                    checked = settings.weeklyRentalElectricityIncluded,
                                    onCheckedChange = { included ->
                                        scope.launch {
                                            settingsRepository.updateWeeklyRental(
                                                priceEuros = weeklyPriceField.toDoubleOrNull() ?: settings.weeklyRentalPriceEuros,
                                                electricityIncluded = included,
                                                includedKm = weeklyIncludedKmField.toDoubleOrNull() ?: settings.weeklyIncludedKm,
                                                extraKmPriceEuros = extraKmPriceField.toDoubleOrNull() ?: settings.extraKmPriceEuros
                                            )
                                        }
                                    }
                                )
                            }

                            if (settings.weeklyRentalElectricityIncluded) {
                                OutlinedTextField(
                                    value = weeklyIncludedKmField,
                                    onValueChange = { weeklyIncludedKmField = it },
                                    label = { Text("Km d'electricite inclus par semaine") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = extraKmPriceField,
                                    onValueChange = { extraKmPriceField = it },
                                    label = { Text("Prix du km supplementaire au-dela du quota (€)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    "Electricite non incluse : facturee au kWh, voir mode \"Prix au kWh\" ci-dessus pour la consommation/le tarif.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        settingsRepository.updateWeeklyRental(
                                            priceEuros = weeklyPriceField.toDoubleOrNull() ?: settings.weeklyRentalPriceEuros,
                                            electricityIncluded = settings.weeklyRentalElectricityIncluded,
                                            includedKm = weeklyIncludedKmField.toDoubleOrNull() ?: settings.weeklyIncludedKm,
                                            extraKmPriceEuros = extraKmPriceField.toDoubleOrNull() ?: settings.extraKmPriceEuros
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enregistrer le forfait")
                            }

                            Divider()

                            Text(
                                "Km parcourus cette semaine (estimation) : ${kmThisWeek.toInt()} km",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Compte automatiquement les courses jugees rentables. Corrige si besoin :",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = kmThisWeekField,
                                    onValueChange = { kmThisWeekField = it },
                                    label = { Text("Km") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.weight(1f)
                                )
                                Button(onClick = {
                                    scope.launch {
                                        weeklyDistanceTracker.setKmThisWeek(kmThisWeekField.toDoubleOrNull() ?: 0.0)
                                    }
                                }) {
                                    Text("OK")
                                }
                            }
                            OutlinedButton(
                                onClick = { scope.launch { weeklyDistanceTracker.resetNow() } },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Reinitialiser le compteur a 0")
                            }
                        }
                    }
                }
            }

            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "4. Regles de refus automatique",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Laisse a 0 pour desactiver une regle. Une course qui depasse une de ces limites " +
                            "est marquee \"Ne pas jouer\" quel que soit son taux horaire.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = minFareField,
                        onValueChange = { minFareField = it },
                        label = { Text("Prix minimum de la course (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = maxApproachField,
                        onValueChange = { maxApproachField = it },
                        label = { Text("Distance max pour venir chercher le client (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = maxTripField,
                        onValueChange = { maxTripField = it },
                        label = { Text("Distance max de la course (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                settingsRepository.updateRejectionRules(
                                    minimumFareEuros = minFareField.toDoubleOrNull() ?: 0.0,
                                    maxApproachKm = maxApproachField.toDoubleOrNull() ?: 0.0,
                                    maxTripKm = maxTripField.toDoubleOrNull() ?: 0.0
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enregistrer les regles")
                    }
                }
            }

            ElevatedCard(shape = RoundedCornerShape(16.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "5. Seuils de rentabilite",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = goodThresholdField,
                        onValueChange = { goodThresholdField = it },
                        label = { Text("Seuil \"Bon\" (€/h)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = okThresholdField,
                        onValueChange = { okThresholdField = it },
                        label = { Text("Seuil \"Correct\" (€/h)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                settingsRepository.updateProfitabilityThresholds(
                                    good = goodThresholdField.toDoubleOrNull() ?: settings.goodHourlyThreshold,
                                    ok = okThresholdField.toDoubleOrNull() ?: settings.okHourlyThreshold
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enregistrer les seuils")
                    }
                }
            }

            Text(
                "Une fois les deux autorisations activees, ouvrez Uber ou Bolt Driver : " +
                    "la bulle Ride Copilot apparait automatiquement des qu'une course est proposee.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}
