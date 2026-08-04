package com.ridecopilot.app.accessibility

import com.ridecopilot.app.domain.RideOffer

/**
 * Extraction heuristique par regex. Les ecrans Uber/Bolt changent avec les versions :
 * ajuster ces motifs si la detection rate (voir README pour la methode de calibration).
 */
object RideOfferParser {

    private val durationRegex = Regex("""(\d{1,3})\s*min""", RegexOption.IGNORE_CASE)
    private val distanceRegex = Regex("""(\d{1,3}[.,]?\d{0,2})\s*km""", RegexOption.IGNORE_CASE)
    private val fareRegex = Regex("""(\d{1,3}[.,]\d{2})\s*€""")
    private val addressHintRegex = Regex("""\d{1,4}\s+[A-Za-zÀ-ÿ' -]{3,60}""")

    fun parse(sourceApp: String, textNodes: List<String>): RideOffer? {
        if (textNodes.isEmpty()) return null
        val joined = textNodes.joinToString(" | ")

        val durationMatch = durationRegex.find(joined)
        val distanceMatch = distanceRegex.find(joined)
        val fareMatch = fareRegex.find(joined)

        val hasRideSignal = durationMatch != null || distanceMatch != null || fareMatch != null
        if (!hasRideSignal) return null

        val addresses = addressHintRegex.findAll(joined).map { it.value.trim() }.toList()

        return RideOffer(
            sourceApp = sourceApp,
            announcedDurationMinutes = durationMatch?.groupValues?.get(1)?.toDoubleOrNull(),
            announcedDistanceKm = distanceMatch?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull(),
            announcedFareEuros = fareMatch?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull(),
            pickupAddress = addresses.getOrNull(0),
            dropoffAddress = addresses.getOrNull(1) ?: addresses.getOrNull(0),
            rawText = joined
        )
    }
}
