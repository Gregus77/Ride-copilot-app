package com.ridecopilot.app.domain

data class RideOffer(
    val sourceApp: String,
    val announcedDurationMinutes: Double?,
    val announcedDistanceKm: Double?,
    val announcedFareEuros: Double?,
    val pickupAddress: String?,
    val dropoffAddress: String?,
    val rawText: String,
    val capturedAtMillis: Long = System.currentTimeMillis()
)

data class TrafficEstimate(
    val approachDurationMinutes: Double?,
    val tripDurationMinutes: Double,
    val totalDurationMinutes: Double,
    val totalDistanceKm: Double
)

enum class ProfitabilityLevel { GOOD, OK, BAD, UNKNOWN }

data class ProfitabilityResult(
    val netEarningsEuros: Double?,
    val hourlyRateEuros: Double?,
    val fuelCostEuros: Double,
    val level: ProfitabilityLevel
)

data class AppSettings(
    val googleMapsApiKey: String = "",
    val fuelConsumptionL100km: Double = 6.5,
    val fuelPricePerLiter: Double = 1.85,
    val goodHourlyThreshold: Double = 20.0,
    val okHourlyThreshold: Double = 12.0
)
