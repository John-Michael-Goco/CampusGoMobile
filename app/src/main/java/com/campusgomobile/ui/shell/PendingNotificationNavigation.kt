package com.campusgomobile.ui.shell

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds pending navigation from a push notification tap.
 * MainActivity sets this when it receives FCM intent extras; AppShell consumes and navigates.
 */
object PendingNotificationNavigation {
    data class Target(
        val type: String,
        val participantId: String? = null,
        val questId: String? = null
    )

    private val _pending = MutableStateFlow<Target?>(null)
    val pending: StateFlow<Target?> = _pending.asStateFlow()

    fun set(target: Target) {
        _pending.value = target
    }

    fun consume(): Target? {
        val value = _pending.value
        _pending.value = null
        return value
    }
}
