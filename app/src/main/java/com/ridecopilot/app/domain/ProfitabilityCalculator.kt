package com.ridecopilot.app.domain

object ProfitabilityCalculator {

    fun compute(
        fareEuros: Double?,
        traffic: TrafficEstimate,
        settings: AppSettings,
        weeklyKmAlreadyDriven: Double
    ): ProfitabilityResult {
        val energyCost = computeEnergyCost(traffic.totalDistanceKm, settings, weeklyKmAlreadyDriven)

        val hardRuleReason = checkHardRules(fareEuros, traffic, settings)
        if (hardRuleReason != null) {
            val net = fareEuros?.minus(energyCost)
            val hourlyRate = net?.let { if (traffic.totalDurationMinutes > 0) it / (traffic.totalDurationMinutes / 60.0) else null }
            return ProfitabilityResult(
                netEarningsEuros = net,
                hourlyRateEuros = hourlyRate,
                energyCostEuros = energyCost,
                level = ProfitabilityLevel.BAD,
                hardRuleReason = hardRuleReason
            )
        }

        if (fareEuros == null || traffic.totalDurationMinutes <= 0.0) {
            return ProfitabilityResult(
                netEarningsEuros = null,
                hourlyRateEuros = null,
                energyCostEuros = energyCost,
                level = ProfitabilityLevel.UNKNOWN
            )
        }

        val net = fareEuros - energyCost
        val hourlyRate = net / (traffic.totalDurationMinutes / 60.0)

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

    /**
     * Cout variable de l'energie pour CE trajet uniquement. Le forfait hebdomadaire
     * lui-meme (cout fixe) n'entre pas dans ce calcul : il ne change pas la decision
     * de prendre ou non une course precise, seul le cout marginal compte.
     */
    private fun computeEnergyCost(distanceKm: Double, settings: AppSettings, weeklyKmAlreadyDriven: Double): Double {
        return when (settings.vehicleType) {
            VehicleType.THERMAL ->
                distanceKm * (settings.fuelConsumptionL100km / 100.0) * settings.fuelPricePerLiter

            VehicleType.ELECTRIC -> when (settings.electricPricingMode) {
                ElectricPricingMode.PER_KWH ->
                    distanceKm * (settings.electricConsumptionKwh100km / 100.0) * settings.electricPricePerKwh

                ElectricPricingMode.WEEKLY_RENTAL -> {
                    if (!settings.weeklyRentalElectricityIncluded) {
                        // Forfait hebdo sans electricite incluse : facturee comme en mode kWh.
                        distanceKm * (settings.electricConsumptionKwh100km / 100.0) * settings.electricPricePerKwh
                    } else {
                        // Seule la portion de CE trajet qui depasse le quota hebdo restant est facturee.
                        val kmOverBefore = (weeklyKmAlreadyDriven - settings.weeklyIncludedKm).coerceAtLeast(0.0)
                        val kmOverAfter = (weeklyKmAlreadyDriven + distanceKm - settings.weeklyIncludedKm).coerceAtLeast(0.0)
                        val kmChargedForThisTrip = kmOverAfter - kmOverBefore
                        kmChargedForThisTrip * settings.extraKmPriceEuros
                    }
                }
            }
        }
    }

    private fun checkHardRules(fareEuros: Double?, traffic: TrafficEstimate, settings: AppSettings): String? {
        if (fareEuros != null && settings.minimumFareEuros > 0.0 && fareEuros < settings.minimumFareEuros) {
            return "Prix (%.2f €) sous le minimum configure (%.2f €)".format(fareEuros, settings.minimumFareEuros)
        }
        val approachKm = traffic.approachDistanceKm
        if (approachKm != null && settings.maxApproachDistanceKm > 0.0 && approachKm > settings.maxApproachDistanceKm) {
            return "Approche trop longue (%.1f km > max %.1f km)".format(approachKm, settings.maxApproachDistanceKm)
        }
        if (settings.maxTripDistanceKm > 0.0 && traffic.tripDistanceKm > settings.maxTripDistanceKm) {
            return "Course trop longue (%.1f km > max %.1f km)".format(traffic.tripDistanceKm, settings.maxTripDistanceKm)
        }
        return null
    }
}
