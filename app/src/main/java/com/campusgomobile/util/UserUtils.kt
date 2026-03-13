package com.campusgomobile.util

import com.campusgomobile.data.model.User

/**
 * Returns initials for the user: first letter of first name + first letter of last name.
 * Uses student.firstName/lastName if available, otherwise parses user.name (e.g. "Doe, Jane" or "Jane Doe").
 */
fun userInitials(user: User?): String {
    if (user == null) return "?"
    val s = user.student
    if (s != null) {
        val first = s.firstName?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
        val last = s.lastName?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: ""
        if (first.isNotEmpty() || last.isNotEmpty()) return first + last
    }
    val name = user.name.trim()
    if (name.isEmpty()) return "?"
    val parts = name.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size >= 2) {
        val last = parts[0].firstOrNull()?.uppercaseChar()?.toString() ?: ""
        val first = parts[1].firstOrNull()?.uppercaseChar()?.toString() ?: ""
        return first + last
    }
    val words = name.split(" ").filter { it.isNotEmpty() }
    if (words.size >= 2) {
        return (words[0].firstOrNull()?.uppercaseChar()?.toString() ?: "") +
            (words.last().firstOrNull()?.uppercaseChar()?.toString() ?: "")
    }
    return name.take(2).uppercase()
}

/**
 * Returns initials from a display name string (e.g. "Jane Doe" -> "JD", "Doe, Jane" -> "DJ").
 * Use for leaderboard or any place where only the name is available.
 */
fun nameToInitials(name: String?): String {
    if (name.isNullOrBlank()) return "?"
    val n = name.trim()
    if (n.isEmpty()) return "?"
    val parts = n.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    if (parts.size >= 2) {
        val last = parts[0].firstOrNull()?.uppercaseChar()?.toString() ?: ""
        val first = parts[1].firstOrNull()?.uppercaseChar()?.toString() ?: ""
        return first + last
    }
    val words = n.split(" ").filter { it.isNotEmpty() }
    if (words.size >= 2) {
        return (words[0].firstOrNull()?.uppercaseChar()?.toString() ?: "") +
            (words.last().firstOrNull()?.uppercaseChar()?.toString() ?: "")
    }
    return n.take(2).uppercase()
}
