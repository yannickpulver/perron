package com.example.android.wearable.wear.model

data class Location(
    val coordinate: Coordinate,
    val distance: Any,
    val id: String,
    val name: String,
    val score: Any
)
