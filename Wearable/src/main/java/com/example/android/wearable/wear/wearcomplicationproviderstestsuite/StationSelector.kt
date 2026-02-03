/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.android.wearable.wear.wearcomplicationproviderstestsuite

import kotlin.math.*

data class Station(val name: String, val latitude: Double, val longitude: Double) {
    companion object {
        val WYLEREGG = Station("Bern Wyleregg", 46.96062275478817, 7.4481615082331905)
        val SCHÖNEGG = Station("Bern Schönegg", 46.93399118862752, 7.4398990730989665)
        val BAHNHOF = Station("Bern Bahnhof", 46.94781551463594, 7.440441322038131)
    }
}

object StationSelector {
    fun selectClosestStation(latitude: Double, longitude: Double): Station {
        val stations = listOf(Station.WYLEREGG, Station.SCHÖNEGG, Station.BAHNHOF)
        return stations.minBy { station ->
            haversineDistance(latitude, longitude, station.latitude, station.longitude)
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
