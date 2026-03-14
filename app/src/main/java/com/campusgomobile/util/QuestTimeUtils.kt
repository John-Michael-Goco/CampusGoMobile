package com.campusgomobile.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object QuestTimeUtils {
    /**
     * Returns true if [stageStart] is a non-blank datetime and represents a time in the future.
     * Used to block joining when the quest/stage has not started yet.
     */
    @JvmStatic
    fun isStageStartInFuture(stageStart: String?): Boolean {
        if (stageStart.isNullOrBlank()) return false
        val trimmed = stageStart.trim()
        val parsers = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd"
        )
        for (pattern in parsers) {
            try {
                val date = SimpleDateFormat(pattern, Locale.US).parse(trimmed) ?: continue
                return date.after(Date())
            } catch (_: Exception) {
                continue
            }
        }
        return false
    }

    /**
     * Parse a backend datetime string to epoch millis, or null if unparseable.
     */
    @JvmStatic
    fun parseToEpochMs(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        val trimmed = dateStr.trim()
        val parsers = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd"
        )
        for (pattern in parsers) {
            try {
                return SimpleDateFormat(pattern, Locale.US).parse(trimmed)?.time
            } catch (_: Exception) {
                continue
            }
        }
        return null
    }

    /**
     * Returns true if the API [reason] text indicates the quest/stage is not yet available (e.g. upcoming).
     * Use when backend may send can_join=true but reason explains why join is not allowed.
     */
    @JvmStatic
    fun reasonSuggestsUpcomingOrNotAvailable(reason: String?): Boolean {
        if (reason.isNullOrBlank()) return false
        val r = reason.lowercase()
        return "upcoming" in r ||
            "not yet" in r ||
            "opens at" in r ||
            "not available" in r ||
            "not started" in r ||
            "will open" in r
    }
}
