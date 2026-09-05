package com.ridecopilot.app.data

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

private val Context.weeklyDistanceStore by preferencesDataStore(name = "ride_copilot_weekly_distance")

/**
 * Estimation du kilometrage parcouru depuis le debut de la semaine (reset chaque
 * lundi), utilisee pour calculer le depassement de quota du forfait electrique
 * hebdomadaire. Additionnee automatiquement par l'app a chaque course jouable
 * analysee (approximation, pas un releve GPS reel) — corrigeable manuellement
 * par le chauffeur dans les reglages.
 */
class WeeklyDistanceTracker(private val context: Context) {

    private object Keys {
        val KM = doublePreferencesKey("weekly_km")
        val WEEK_ID = intPreferencesKey("weekly_km_week_id")
    }

    private fun currentWeekId(): Int {
        val today = LocalDate.now()
        val weekFields = WeekFields.of(Locale.FRANCE)
        return today.year * 100 + today.get(weekFields.weekOfWeekBasedYear())
    }

    val kmThisWeekFlow: Flow<Double> = context.weeklyDistanceStore.data.map { prefs ->
        if (prefs[Keys.WEEK_ID] != currentWeekId()) 0.0 else (prefs[Keys.KM] ?: 0.0)
    }

    suspend fun getKmThisWeek(): Double = kmThisWeekFlow.first()

    suspend fun addKm(km: Double) {
        if (km <= 0.0) return
        context.weeklyDistanceStore.edit { prefs ->
            val weekId = currentWeekId()
            val current = if (prefs[Keys.WEEK_ID] == weekId) (prefs[Keys.KM] ?: 0.0) else 0.0
            prefs[Keys.KM] = current + km
            prefs[Keys.WEEK_ID] = weekId
        }
    }

    suspend fun resetNow() {
        context.weeklyDistanceStore.edit { prefs ->
            prefs[Keys.KM] = 0.0
            prefs[Keys.WEEK_ID] = currentWeekId()
        }
    }

    suspend fun setKmThisWeek(km: Double) {
        context.weeklyDistanceStore.edit { prefs ->
            prefs[Keys.KM] = km.coerceAtLeast(0.0)
            prefs[Keys.WEEK_ID] = currentWeekId()
        }
    }
}
