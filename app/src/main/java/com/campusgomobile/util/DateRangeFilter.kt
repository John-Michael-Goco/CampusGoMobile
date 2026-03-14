package com.campusgomobile.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** Shared date range option for activity log, transactions, etc. */
data class DateRangeOption(val label: String, val key: String)

fun computeDateRange(key: String): Pair<String?, String?> {
    val today = LocalDate.now()
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    return when (key) {
        "7d" -> (today.minusDays(7).format(formatter) to today.format(formatter))
        "30d" -> (today.minusDays(30).format(formatter) to today.format(formatter))
        "month" -> (today.withDayOfMonth(1).format(formatter) to today.format(formatter))
        "semester" -> {
            val semesterStart = if (today.monthValue >= 7) today.withDayOfMonth(1).withMonth(7)
            else today.withDayOfMonth(1).withMonth(1)
            (semesterStart.format(formatter) to today.format(formatter))
        }
        else -> (null to null)
    }
}
