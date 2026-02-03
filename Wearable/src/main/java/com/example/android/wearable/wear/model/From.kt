package com.example.android.wearable.wear.model

@kotlinx.serialization.Serializable
data class From(
    val departure: String,
    val departureTimestamp: Int,
)
