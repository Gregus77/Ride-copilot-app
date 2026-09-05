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
    val approachDistanceKm: Double?,
    val tripDurationMinutes: Double,
    val tripDistanceKm: Double,
    val totalDurationMinutes: Double,
    val totalDistanceKm: Double
)

enum class ProfitabilityLevel { GOOD, OK, BAD, UNKNOWN }

enum class VehicleType { THERMAL, ELECTRIC }

// PER_KWH : le chauffeur paie l'electricite au kWh consomme (recharge a la demande).
// WEEKLY_RENTAL : vehicule loue a la semaine (forfait), avec eventuellement un quota
// de km d'electricite inclus et un tarif au km au-dela de ce quota.
enum class ElectricPricingMode { PER_KWH, WEEKLY_RENTAL }

data class ProfitabilityResult(
    val netEarningsEuros: Double?,
    val hourlyRateEuros: Double?,
    val energyCostEuros: Double,
    val level: ProfitabilityLevel,
    // Non-null si une regle de refus configuree par le chauffeur (prix minimum,
    // distance max d'approche/course) a ete declenchee : le level est alors force a BAD.
    val hardRuleReason: String? = null
)

data class AppSettings(
    val googleMapsApiKey: String = "",
    val vehicleType: VehicleType = VehicleType.THERMAL,
    val fuelConsumptionL100km: Double = 6.5,
    val fuelPricePerLiter: Double = 1.85,
    val electricPricingMode: ElectricPricingMode = ElectricPricingMode.PER_KWH,
    val electricConsumptionKwh100km: Double = 17.0,
    val electricPricePerKwh: Double = 0.35,
    val weeklyRentalPriceEuros: Double = 0.0,
    val weeklyRentalElectricityIncluded: Boolean = true,
    val weeklyIncludedKm: Double = 1000.0,
    val extraKmPriceEuros: Double = 0.15,
    val goodHourlyThreshold: Double = 20.0,
    val okHourlyThreshold: Double = 12.0,
    // Regles de refus automatique, 0 = desactive. Reglables par le chauffeur.
    val minimumFareEuros: Double = 0.0,
    val maxApproachDistanceKm: Double = 0.0,
    val maxTripDistanceKm: Double = 0.0,
    val monitoringEnabled: Boolean = true
)
