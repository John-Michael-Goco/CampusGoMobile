package com.campusgomobile.data.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.campusgomobile.MainActivity
import com.campusgomobile.R
import com.campusgomobile.data.auth.TokenStorage
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CampusGoMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM onNewToken: ${token.take(20)}...")
        serviceScope.launch {
            val tokenStorage = TokenStorage(applicationContext)
            val repo = FcmRepository(tokenStorage)
            if (repo.registerTokenToBackend(token)) {
                Log.d(TAG, "FCM token registered with backend")
            } else {
                Log.w(TAG, "FCM token could not be registered (not logged in or network error)")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data ?: return
        val type = data["type"] ?: return

        val title = message.notification?.title ?: getDefaultTitle(type)
        val body = message.notification?.body ?: getDefaultBody(type, data)

        createNotificationChannel()
        val pendingIntent = createPendingIntent(type, data)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(type.hashCode(), notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createPendingIntent(type: String, data: Map<String, String>): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_FCM_TYPE, type)
            data["participant_id"]?.let { putExtra(EXTRA_PARTICIPANT_ID, it) }
            data["quest_id"]?.let { putExtra(EXTRA_QUEST_ID, it) }
            data["quest_title"]?.let { putExtra(EXTRA_QUEST_TITLE, it) }
        }
        return PendingIntent.getActivity(
            this,
            type.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun getDefaultTitle(type: String): String = when (type) {
        "ranking_resolved", "stage_unlocked" -> getString(R.string.notification_title_quest)
        "new_store_items" -> getString(R.string.notification_title_store)
        "new_quest", "quest_started" -> getString(R.string.notification_title_quests)
        else -> getString(R.string.app_name)
    }

    private fun getDefaultBody(type: String, data: Map<String, String>): String = when (type) {
        "ranking_resolved" -> data["message"] ?: getString(R.string.notification_body_ranking)
        "stage_unlocked" -> data["quest_title"]?.let { "Stage is now open for $it" }
            ?: getString(R.string.notification_body_stage)
        "new_store_items" -> data["item_name"]?.let { "$it is now in the store" }
            ?: getString(R.string.notification_body_store)
        "new_quest", "quest_started" -> data["quest_title"]?.let { "$it is available" }
            ?: getString(R.string.notification_body_quest)
        else -> ""
    }

    companion object {
        private const val TAG = "CampusGoFCM"
        const val CHANNEL_ID = "campusgo_alerts"
        const val EXTRA_FCM_TYPE = "fcm_type"
        const val EXTRA_PARTICIPANT_ID = "fcm_participant_id"
        const val EXTRA_QUEST_ID = "fcm_quest_id"
        const val EXTRA_QUEST_TITLE = "fcm_quest_title"
    }
}
