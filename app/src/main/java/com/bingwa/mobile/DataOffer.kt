package com.bingwa.mobile

data class DataOffer(
    val name: String,
    val price: Int,
    val ussdCode: String,
    val executionMode: String = "SIMPLE",   // SIMPLE or ADVANCED
    val mode: String = "daily"              // daily, weekly, monthly
)