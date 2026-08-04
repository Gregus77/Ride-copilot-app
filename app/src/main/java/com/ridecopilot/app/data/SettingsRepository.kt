package com.ridecopilot.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ridecopilot.app.domain.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ride_copilot_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("google_maps_api_key")
        val FUEL_CONSUMPTION = doublePreferencesKey("fuel_consumption_l100km")
        val FUEL_PRICE = doublePreferencesKey("fuel_price_per_liter")
        val GOOD_THRESHOLD = doublePreferencesKey("good_hourly_threshold")
        val OK_THRESHOLD = doublePreferencesKey("ok_hourly_threshold")
        val MONITORING_ENABLED = booleanPreferencesKey("monitoring_enabled")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            googleMapsApiKey = prefs[Keys.API_KEY] ?: "",
            fuelConsumptionL100km = prefs[Keys.FUEL_CONSUMPTION] ?: 6.5,
            fuelPricePerLiter = prefs[Keys.FUEL_PRICE] ?: 1.85,
            goodHourlyThreshold = prefs[Keys.GOOD_THRESHOLD] ?: 20.0,
            okHourlyThreshold = prefs[Keys.OK_THRESHOLD] ?: 12.0,
            monitoringEnabled = prefs[Keys.MONITORING_ENABLED] ?: true
        )
    }

    suspend fun updateApiKey(key: String) {
        context.dataStore.edit { it[Keys.API_KEY] = key }
    }

    suspend fun updateFuelConsumption(value: Double) {
        context.dataStore.edit { it[Keys.FUEL_CONSUMPTION] = value }
    }

    suspend fun updateFuelPrice(value: Double) {
        context.dataStore.edit { it[Keys.FUEL_PRICE] = value }
    }

    suspend fun updateMonitoringEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.MONITORING_ENABLED] = enabled }
    }
}
