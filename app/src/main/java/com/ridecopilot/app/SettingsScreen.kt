package com.ridecopilot.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ridecopilot.app.data.SettingsRepository
import com.ridecopilot.app.domain.AppSettings
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val settings by settingsRepository.settingsFlow.collectAsStateWithLifecycle(initialValue = AppSettings())

    var apiKeyField by remember(settings.googleMapsApiKey) { mutableStateOf(settings.googleMapsApiKey) }
    var fuelConsumptionField by remember(settings.fuelConsumptionL100km) {
        mutableStateOf(settings.fuelConsumptionL100km.toString())
    }
    var fuelPriceField by remember(settings.fuelPricePerLiter) {
        mutableStateOf(settings.fuelPricePerLiter.toString())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Ride Copilot", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Superposition Uber/Bolt : temps de trajet reel (trafic) + score de rentabilite avant d'accepter une course.",
            style = MaterialTheme.typography.bodyMedium
        )

        Divider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Surveillance active", style = MaterialTheme.typography.titleMedium)
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

        Divider()

        Text("1. Autorisations", style = MaterialTheme.typography.titleMedium)
        Button(onClick = onRequestOverlayPermission) {
            Text("Autoriser l'affichage par-dessus les autres apps")
        }
        Button(onClick = onRequestAccessibilityPermission) {
            Text("Activer le service d'accessibilite")
        }

        Divider()

        Text("2. Cle API Google Maps", style = MaterialTheme.typography.titleMedium)
        Text(
            "Necessite les API Directions et Geocoding activees sur console.cloud.google.com",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = apiKeyField,
            onValueChange = { apiKeyField = it },
            label = { Text("Cle API") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { scope.launch { settingsRepository.updateApiKey(apiKeyField) } }) {
            Text("Enregistrer la cle")
        }

        Divider()

        Text("3. Cout du vehicule", style = MaterialTheme.typography.titleMedium)
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
        Button(onClick = {
            scope.launch {
                settingsRepository.updateFuelConsumption(
                    fuelConsumptionField.toDoubleOrNull() ?: settings.fuelConsumptionL100km
                )
                settingsRepository.updateFuelPrice(
                    fuelPriceField.toDoubleOrNull() ?: settings.fuelPricePerLiter
                )
            }
        }) {
            Text("Enregistrer les couts")
        }

        Divider()

        Text(
            "Une fois les deux autorisations activees et la cle API enregistree, ouvrez Uber ou Bolt Driver : " +
                "la bulle Ride Copilot apparait automatiquement des qu'une course est proposee.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
