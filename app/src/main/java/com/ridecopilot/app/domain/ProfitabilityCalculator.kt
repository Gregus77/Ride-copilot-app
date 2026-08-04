package com.ridecopilot.app.domain

object ProfitabilityCalculator {

    fun compute(
        fareEuros: Double?,
        totalDurationMinutes: Double,
        totalDistanceKm: Double,
        settings: AppSettings
    ): ProfitabilityResult {
        val fuelCost = totalDistanceKm * (settings.fuelConsumptionL100km / 100.0) * settings.fuelPricePerLiter

        if (fareEuros == null || totalDurationMinutes <= 0.0) {
            return ProfitabilityResult(
                netEarningsEuros = null,
                hourlyRateEuros = null,
                fuelCostEuros = fuelCost,
                level = ProfitabilityLevel.UNKNOWN
            )
        }

        val net = fareEuros - fuelCost
        val hourlyRate = net / (totalDurationMinutes / 60.0)

        val level = when {
            hourlyRate >= settings.goodHourlyThreshold -> ProfitabilityLevel.GOOD
            hourlyRate >= settings.okHourlyThreshold -> ProfitabilityLevel.OK
            else -> ProfitabilityLevel.BAD
        }

        return ProfitabilityResult(
            netEarningsEuros = net,
            hourlyRateEuros = hourlyRate,
            fuelCostEuros = fuelCost,
            level = level
        )
    }
}
