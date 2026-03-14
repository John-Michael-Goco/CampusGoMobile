package com.campusgomobile.util

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val apiDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val apiDateTimeIso = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
private val apiDateOnly = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val humanReadable = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
private val humanReadableDateOnly = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault())

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

/**
 * Converts API date/time (inventory acquired_at, used_at, etc.) to human-readable.
 * Tries "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", then "yyyy-MM-dd".
 * Returns e.g. "Mar 10, 2026 at 2:30 PM" or "Mar 10, 2026". Falls back to original string if parsing fails.
 */
fun formatInventoryDate(iso: String): String {
    val trimmed = iso.trim()
    if (trimmed.isBlank()) return trimmed
    return try {
        val dt = LocalDateTime.parse(trimmed, apiDateTime)
        dt.format(humanReadable)
    } catch (_: Exception) {
        try {
            val dt = LocalDateTime.parse(trimmed, apiDateTimeIso)
            dt.format(humanReadable)
        } catch (_: Exception) {
            try {
                java.time.LocalDate.parse(trimmed, apiDateOnly).format(humanReadableDateOnly)
            } catch (_: Exception) {
                trimmed
            }
        }
    }
}
