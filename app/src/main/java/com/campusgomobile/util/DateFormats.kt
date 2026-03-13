package com.campusgomobile.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val apiDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val humanReadable = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.getDefault())

/**
 * Converts API timestamp (e.g. "2026-03-10 14:30:00") to human-readable (e.g. "Mar 10, 2026 at 2:30 PM").
 * Returns the original string if parsing fails.
 */
fun formatActivityTimestamp(iso: String): String {
    return try {
        val dt = LocalDateTime.parse(iso, apiDateTime)
        dt.format(humanReadable)
    } catch (_: Exception) {
        iso
    }
}
