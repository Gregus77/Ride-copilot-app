package com.ridecopilot.app.domain

object ProfitabilityCalculator {

    fun compute(
        fareEuros: Double?,
        totalDurationMinutes: Double,
        totalDistanceKm: Double,
        settings: AppSettings
    ): ProfitabilityResult {
        val energyCost = when (settings.vehicleType) {
            VehicleType.ELECTRIC ->
                totalDistanceKm * (settings.electricConsumptionKwh100km / 100.0) * settings.electricPricePerKwh
            VehicleType.THERMAL ->
                totalDistanceKm * (settings.fuelConsumptionL100km / 100.0) * settings.fuelPricePerLiter
        }

        if (fareEuros == null || totalDurationMinutes <= 0.0) {
            return ProfitabilityResult(
                netEarningsEuros = null,
                hourlyRateEuros = null,
                energyCostEuros = energyCost,
                level = ProfitabilityLevel.UNKNOWN
            )
        }

        val net = fareEuros - energyCost
        val hourlyRate = net / (totalDurationMinutes / 60.0)

        val level = when {
            hourlyRate >= settings.goodHourlyThreshold -> ProfitabilityLevel.GOOD
            hourlyRate >= settings.okHourlyThreshold -> ProfitabilityLevel.OK
            else -> ProfitabilityLevel.BAD
        }

        return ProfitabilityResult(
            netEarningsEuros = net,
            hourlyRateEuros = hourlyRate,
            energyCostEuros = energyCost,
            level = level
        )
    }
}
