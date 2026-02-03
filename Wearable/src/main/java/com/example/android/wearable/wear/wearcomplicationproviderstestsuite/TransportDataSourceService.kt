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

import android.app.PendingIntent
import android.content.ComponentName
import android.graphics.drawable.Icon
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService
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
import kotlinx.serialization.decodeFromString

/**
 * A complication provider that supports only [ComplicationType.MONOCHROMATIC_IMAGE] and cycles through
 * a few different icons on each tap.
 *
 * Note: This subclasses [SuspendingComplicationDataSourceService] instead of [ComplicationDataSourceService] to support
 * coroutines, so data operations (specifically, calls to [DataStore]) can be supported directly in the
 * [onComplicationRequest].
 *
 * If you don't perform any suspending operations to update your complications, you can subclass
 * [ComplicationDataSourceService] and override [onComplicationRequest] directly.
 * (see [NoDataDataSourceService] for an example)
 */
class TransportDataSourceService : SuspendingComplicationDataSourceService() {
    private val client = HttpClient(OkHttp)

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        if (request.complicationType != ComplicationType.SHORT_TEXT) {
            return null
        }
        val args = ComplicationToggleArgs(
            providerComponent = ComponentName(this, javaClass),
            complication = Complication.ICON,
            complicationInstanceId = request.complicationInstanceId
        )
        val complicationTogglePendingIntent =
            ComplicationToggleReceiver.getComplicationToggleIntent(
                context = this,
                args = args
            )
        // Suspending function to retrieve the complication's state
        val state = args.getState(this@TransportDataSourceService)

        // Try location-based selection, fall back to DataStore toggle
        val locationProvider = WearLocationProvider(this)
        val locationResult = locationProvider.getLocation()
        Log.d(TAG, "Location result: $locationResult")

        val case = when (locationResult) {
            is LocationResult.Success -> {
                val closestStation = StationSelector.selectClosestStation(
                    locationResult.latitude,
                    locationResult.longitude
                )
                Log.d(TAG, "Closest station: ${closestStation.name}")
                when (closestStation) {
                    Station.WYLEREGG -> Case.HOME
                    Station.SCHÖNEGG -> Case.WORK
                    Station.BAHNHOF -> Case.BAHNHOF
                    else -> Case.values()[state.mod(Case.values().size)]
                }
            }
            is LocationResult.Error -> {
                Log.d(TAG, "Location error, falling back to toggle: ${locationResult.reason}")
                Case.values()[state.mod(Case.values().size)]
            }
        }
        Log.d(TAG, "Selected case: $case")
        val response = when (case) {
            Case.HOME -> client.get("https://transport.opendata.ch/v1/connections?from=Bern%20Wyleregg&to=Bern&limit=3")
            Case.WORK -> client.get("https://transport.opendata.ch/v1/connections?from=Bern%20Schönegg&to=Bern&limit=3")
            Case.BAHNHOF -> client.get("https://transport.opendata.ch/v1/connections?from=Bern&to=Bern%20Wyleregg&limit=3")
        }
        val connectionList = json.decodeFromString<ConnectionList>(response.body())


        return getComplicationData(
            tapAction = complicationTogglePendingIntent, case, connectionList.toTime()
        )
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData =
        getComplicationData(
            tapAction = null,
            Case.HOME,
            "12:12"
        )

    private fun getComplicationData(
        tapAction: PendingIntent?,
        case: Case,
        time: String
    ): ComplicationData = when (case) {
        Case.HOME -> {
            ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(
                    text = time
                ).build(),
                contentDescription = PlainComplicationText.Builder(
                    text = getText(R.string.short_text_only_content_description)
                ).build()
            )
                .setMonochromaticImage(
                    MonochromaticImage.Builder(
                        image = Icon.createWithResource(this, R.drawable.home_48px)
                    )
                        .setAmbientImage(
                            ambientImage = Icon.createWithResource(
                                this,
                                R.drawable.ic_battery_burn_protect
                            )
                        )
                        .build()
                )
        }
        Case.WORK -> {
            ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(
                    text = time
                ).build(),
                contentDescription = PlainComplicationText.Builder(
                    text = getText(R.string.short_text_only_content_description)
                ).build()
            )
                .setMonochromaticImage(
                    MonochromaticImage.Builder(
                        image = Icon.createWithResource(this, R.drawable.apartment_48px)
                    )
                        .build()
                )
        }
        Case.BAHNHOF -> {
            ShortTextComplicationData.Builder(
                text = PlainComplicationText.Builder(
                    text = time
                ).build(),
                contentDescription = PlainComplicationText.Builder(
                    text = getText(R.string.short_text_only_content_description)
                ).build()
            )
                .setMonochromaticImage(
                    MonochromaticImage.Builder(
                        image = Icon.createWithResource(this, R.drawable.ic_data_usage_vd_theme_24)
                    )
                        .build()
                )
        }
    }.setTapAction(tapAction)
        .build()


    private enum class Case {
        HOME, WORK, BAHNHOF
    }

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
