package com.eveexite.coffeemaker.presentation.util

import java.util.*



object DateUiUtils {

    val TIME_ZONE: String = "America/Lima"
    val TIME_ZONE_PERU: Locale = Locale("es", "PE")

    fun now(): Date {
        val calendar: Calendar = Calendar.getInstance(TIME_ZONE_PERU)
        return Date(calendar.timeInMillis)
    }

    fun millisToDate(millis: Long): Date {
        val calendar: Calendar = Calendar.getInstance(TIME_ZONE_PERU)
        calendar.timeInMillis = millis
        return Date(calendar.timeInMillis)
    }

}