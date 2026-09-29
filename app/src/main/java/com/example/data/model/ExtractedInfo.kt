package com.example.data.model

data class ExtractedInfo(
    val ip: String = "127.0.0.1",
    val country: String = "United States",
    val countryCode: String = "US",
    val city: String = "New York",
    val region: String = "NY",
    val street: String = "",
    val postalCode: String = "10001",
    val timezone: String = "America/New_York",
    val language: String = "en-US",
    val currency: String = "USD",
    val isp: String = "Broadband",
    val org: String = "Telecommunications",
    val latitude: Double = 40.7128,
    val longitude: Double = -74.0060,
    val isProxy: Boolean = false
)
