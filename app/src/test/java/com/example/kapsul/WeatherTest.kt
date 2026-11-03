package com.example.kapsul

import com.example.kapsul.capsule.describeWeather
import org.junit.Assert.assertEquals
import org.junit.Test

class WeatherTest {
    @Test
    fun describesWmoCodesInTurkish() {
        assertEquals("☀️ 18°C, açık", describeWeather(17.6, 0, isDay = true))
        assertEquals("🌙 -3°C, açık", describeWeather(-2.6, 0, isDay = false))
        assertEquals("🌧️ 9°C, yağmurlu", describeWeather(9.2, 63, isDay = true))
        assertEquals("⛈️ 21°C, fırtınalı", describeWeather(21.0, 95, isDay = true))
    }
}
