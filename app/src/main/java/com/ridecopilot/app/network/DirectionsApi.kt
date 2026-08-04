package com.ridecopilot.app.network

import com.ridecopilot.app.domain.TrafficEstimate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

class DirectionsApi(private val apiKey: String) {

    private val client = OkHttpClient()

    suspend fun geocode(address: String): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(address, "UTF-8")
        val url = "https://maps.googleapis.com/maps/api/geocode/json?address=$encoded&key=$apiKey&language=fr"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val results = json.optJSONArray("results") ?: return@withContext null
            if (results.length() == 0) return@withContext null
            val location = results.getJSONObject(0)
                .getJSONObject("geometry")
                .getJSONObject("location")
            location.getDouble("lat") to location.getDouble("lng")
        }
    }

    /**
     * origin -> [pickup] -> destination, avec trafic temps reel (departure_time=now).
     * pickup null = trajet direct origin -> destination.
     */
    suspend fun getRealTimeEstimate(
        originLat: Double,
        originLng: Double,
        pickup: Pair<Double, Double>?,
        destLat: Double,
        destLng: Double
    ): TrafficEstimate? = withContext(Dispatchers.IO) {
        val waypointsParam = pickup?.let { "&waypoints=${it.first},${it.second}" } ?: ""
        val url = "https://maps.googleapis.com/maps/api/directions/json" +
            "?origin=$originLat,$originLng" +
            "&destination=$destLat,$destLng" +
            waypointsParam +
            "&departure_time=now" +
            "&traffic_model=best_guess" +
            "&key=$apiKey&language=fr"
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string() ?: return@withContext null
            val json = JSONObject(body)
            val routes = json.optJSONArray("routes") ?: return@withContext null
            if (routes.length() == 0) return@withContext null
            val legs = routes.getJSONObject(0).getJSONArray("legs")

            var totalDurationSec = 0
            var totalDistanceMeters = 0
            var approachDurationSec: Int? = null

            for (i in 0 until legs.length()) {
                val leg = legs.getJSONObject(i)
                val legDurationSec = leg.optJSONObject("duration_in_traffic")?.optInt("value")
                    ?: leg.getJSONObject("duration").getInt("value")
                totalDurationSec += legDurationSec
                totalDistanceMeters += leg.getJSONObject("distance").getInt("value")
                if (i == 0 && legs.length() > 1) {
                    approachDurationSec = legDurationSec
                }
            }

            TrafficEstimate(
                approachDurationMinutes = approachDurationSec?.let { it / 60.0 },
                tripDurationMinutes = if (approachDurationSec != null) {
                    (totalDurationSec - approachDurationSec) / 60.0
                } else {
                    totalDurationSec / 60.0
                },
                totalDurationMinutes = totalDurationSec / 60.0,
                totalDistanceKm = totalDistanceMeters / 1000.0
            )
        }
    }
}
