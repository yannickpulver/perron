package com.yannickpulver.perron.model

@kotlinx.serialization.Serializable
data class From(
    val departure: String,
    val departureTimestamp: Int,
)
