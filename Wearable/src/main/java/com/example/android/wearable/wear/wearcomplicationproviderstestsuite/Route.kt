package com.example.android.wearable.wear.wearcomplicationproviderstestsuite

import androidx.annotation.DrawableRes
import kotlinx.serialization.Serializable

@Serializable
data class StationInfo(
    val name: String,
    val id: String,
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class Route(
    val id: String,
    val fromStation: StationInfo,
    val toStation: StationInfo,
    val icon: String = "default"
)

enum class RouteIcon(
    val key: String,
    val label: String,
    @DrawableRes val drawableRes: Int
) {
    HOME("home", "Home", R.drawable.home_48px),
    WORK("work", "Work", R.drawable.apartment_48px),
    TRAIN("train", "Train", R.drawable.ic_train),
    TRAM("tram", "Tram", R.drawable.ic_tram),
    BUS("bus", "Bus", R.drawable.ic_bus),
    FLIGHT("flight", "Airport", R.drawable.ic_flight),
    HOSPITAL("hospital", "Hospital", R.drawable.ic_hospital),
    PARK("park", "Park", R.drawable.ic_park),
    SCHOOL("school", "School", R.drawable.ic_school),
    SHOPPING("shopping", "Shopping", R.drawable.ic_shopping),
    GYM("gym", "Gym", R.drawable.ic_gym),
    DEFAULT("default", "Default", R.drawable.ic_data_usage_vd_theme_24);

    companion object {
        fun fromKey(key: String): RouteIcon = entries.find { it.key == key } ?: DEFAULT
    }
}
