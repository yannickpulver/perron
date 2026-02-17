package com.yannickpulver.perron.model

@kotlinx.serialization.Serializable
data class Conenction(
    val from: From,
    val transfers: Int
)
