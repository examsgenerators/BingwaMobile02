package com.bingwa.mobile

data class DataOffer(
    val name: String,
    val price: Int,
    val tokenCost: Int = 1,
    val ussdCode: String,
    val executionMode: String = "SIMPLE",
    val mode: String = "daily"
)
