package com.example.android.wearable.wear.wearcomplicationproviderstestsuite

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.graphics.drawable.Icon
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.example.android.wearable.wear.json
import com.example.android.wearable.wear.model.ConnectionList
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TransportDataSourceService : SuspendingComplicationDataSourceService() {
    private val client = HttpClient(OkHttp)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) return null

        val args = ComplicationToggleArgs(
            providerComponent = ComponentName(this, javaClass),
            complication = Complication.ICON,
            complicationInstanceId = request.complicationInstanceId
        )
        val state = args.getState(this)

        val routes = RouteRepository.getRoutes(this)
        if (routes.isEmpty()) {
            return getNoRouteComplication(openConfigIntent())
        }

        val route = selectRoute(routes, state)
        Log.d(TAG, "Selected route: ${route.fromStation.name} -> ${route.toStation.name}")

        val from = java.net.URLEncoder.encode(route.fromStation.name, "UTF-8")
        val to = java.net.URLEncoder.encode(route.toStation.name, "UTF-8")
        val response = client.get("https://transport.opendata.ch/v1/connections?from=$from&to=$to&limit=3")
        val connectionList = json.decodeFromString<ConnectionList>(response.body())

        val tapIntent = ComplicationToggleReceiver.getComplicationToggleIntent(this, args)
        val iconRes = RouteIcon.fromKey(route.icon).drawableRes
        return buildComplicationData(tapIntent, connectionList.toTime(), iconRes)
    }

    private suspend fun selectRoute(routes: List<Route>, state: Long): Route {
        val locationProvider = WearLocationProvider(this)
        return when (val locationResult = locationProvider.getLocation()) {
            is LocationResult.Success -> {
                StationSelector.selectClosestRoute(
                    locationResult.latitude, locationResult.longitude, routes
                ) ?: routes[state.mod(routes.size).toInt()]
            }
            is LocationResult.Error -> {
                Log.d(TAG, "Location error: ${locationResult.reason}")
                routes[state.mod(routes.size).toInt()]
            }
        }
    }

    private fun openConfigIntent(): PendingIntent {
        val intent = Intent(this, RouteConfigActivity::class.java)
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getNoRouteComplication(tapAction: PendingIntent): ComplicationData =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("--").build(),
            contentDescription = PlainComplicationText.Builder("No routes configured").build()
        )
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.drawable.ic_data_usage_vd_theme_24)
                ).build()
            )
            .setTapAction(tapAction)
            .build()

    private fun buildComplicationData(tapAction: PendingIntent, time: String, iconRes: Int): ComplicationData =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(time).build(),
            contentDescription = PlainComplicationText.Builder("Transport departures").build()
        )
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(this, iconRes)
                ).build()
            )
            .setTapAction(tapAction)
            .build()

    override fun getPreviewData(type: ComplicationType): ComplicationData =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder("12,15").build(),
            contentDescription = PlainComplicationText.Builder("Transport departures").build()
        )
            .setMonochromaticImage(
                MonochromaticImage.Builder(
                    Icon.createWithResource(this, R.drawable.ic_data_usage_vd_theme_24)
                ).build()
            )
            .build()

    companion object {
        private const val TAG = "TransportDataSource"
    }
}

fun ConnectionList.toTime(): String {
    if (connections.isEmpty()) return "--,--"
    val currentMinute = LocalDateTime.now().minute
    val departures = connections.map { it.from.departure.chunked(22).joinToString(":") }
        .map { ZonedDateTime.parse(it, DateTimeFormatter.ISO_OFFSET_DATE_TIME).minute }
        .filter { it != currentMinute }.take(2)
    return departures.joinToString(",") { String.format("%02d", it) }
}
