package com.example.android.wearable.wear.wearcomplicationproviderstestsuite

import kotlin.math.*

object StationSelector {
    fun selectClosestRoute(latitude: Double, longitude: Double, routes: List<Route>): Route? {
        if (routes.isEmpty()) return null
        return routes.minBy { route ->
            haversineDistance(latitude, longitude, route.fromStation.latitude, route.fromStation.longitude)
        }
    }

    fun haversineDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val earthRadiusKm = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val lat1Rad = Math.toRadians(lat1)
        val lat2Rad = Math.toRadians(lat2)
        val a = sin(dLat / 2).pow(2) +
            sin(dLon / 2).pow(2) * cos(lat1Rad) * cos(lat2Rad)
        val c = 2 * asin(sqrt(a))
        return earthRadiusKm * c
    }
}
