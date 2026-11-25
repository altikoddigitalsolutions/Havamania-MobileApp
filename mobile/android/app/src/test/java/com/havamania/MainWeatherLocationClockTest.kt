package com.havamania

import java.time.Instant
import java.util.TimeZone
import org.junit.Assert.*
import org.junit.Test

class MainWeatherLocationClockTest {
    @Test fun `phase free weather description cannot infer device day or night`() { assertEquals("Açık",WeatherMapper.getWeatherCondition(0)) }
    @Test fun `current provider solar flag controls description`() {
        fun value(phase:Int) = WeatherMapper.mapToDomain(OpenMeteoResponse(41.0,29.0,current=CurrentWeatherDto(temperature=20.0,weatherCode=0,isDay=phase)),"İstanbul")
        assertEquals("Güneşli",value(1).condition); assertTrue(value(0).condition.contains("gece",true))
    }
    @Test fun `hourly description follows each samples validated solar interval`() {
        val day="2026-11-25"
        val value=OpenMeteoResponse(41.0,29.0,timezone="Europe/Istanbul",daily=DailyDto(time=listOf(day),weatherCode=listOf(0),tempMin=listOf(10.0),tempMax=listOf(20.0),sunrise=listOf("${day}T06:00"),sunset=listOf("${day}T18:00")),hourly=HourlyDto(time=listOf("${day}T00:00","${day}T12:00"),temperature=listOf(10.0,20.0),weatherCode=listOf(0,0)))
        val hours=WeatherMapper.mapToDomain(value,"İstanbul",now=Instant.parse("2026-11-25T10:00:00Z")).hourlyForecast
        assertTrue(hours[0].condition.contains("gece",true)); assertEquals("Güneşli",hours[1].condition)
    }
    @Test fun `today marker remains tied to forecast location across device zones`() {
        val previous=TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            val value=OpenMeteoResponse(35.0,139.0,timezone="Asia/Tokyo",daily=DailyDto(time=listOf("2026-11-25","2026-11-26"),weatherCode=listOf(0,0),tempMin=listOf(10.0,11.0),tempMax=listOf(20.0,21.0)))
            assertEquals("2026-11-26",WeatherMapper.mapToDomain(value,"Tokyo",now=Instant.parse("2026-11-25T16:00:00Z")).dailyForecast.single{it.isToday}.date)
        } finally { TimeZone.setDefault(previous) }
    }
}