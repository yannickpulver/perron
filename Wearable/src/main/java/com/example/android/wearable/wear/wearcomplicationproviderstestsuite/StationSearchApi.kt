package com.example.android.wearable.wear.wearcomplicationproviderstestsuite

import com.example.android.wearable.wear.json
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import kotlinx.serialization.Serializable

@Serializable
private data class LocationsResponse(
    val stations: List<ApiStation> = emptyList()
)

@Serializable
private data class ApiStation(
    val id: String? = null,
    val name: String? = null,
    val coordinate: ApiCoordinate? = null
)

@Serializable
private data class ApiCoordinate(
    val type: String? = null,
    val x: Double? = null,
    val y: Double? = null
)

object StationSearchApi {
    private val client = HttpClient(OkHttp)

    suspend fun searchStations(query: String): List<StationInfo> {
        if (query.isBlank()) return emptyList()
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")
        val response = client.get("https://transport.opendata.ch/v1/locations?query=$encoded&type=station")
        val body: String = response.body()
        val result = json.decodeFromString<LocationsResponse>(body)
        return result.stations.mapNotNull { station ->
            val id = station.id ?: return@mapNotNull null
            val name = station.name ?: return@mapNotNull null
            val coord = station.coordinate ?: return@mapNotNull null
            val lat = coord.x ?: return@mapNotNull null
            val lon = coord.y ?: return@mapNotNull null
            StationInfo(name = name, id = id, latitude = lat, longitude = lon)
        }
    }
}
