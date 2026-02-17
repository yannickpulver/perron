package com.yannickpulver.perron.model

@kotlinx.serialization.Serializable
data class ConnectionList(
    val connections: List<Conenction> = emptyList(),
)
