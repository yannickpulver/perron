package com.example.android.wearable.wear.model

@kotlinx.serialization.Serializable
data class ConnectionList(
    val connections: List<Conenction> = emptyList(),
)
