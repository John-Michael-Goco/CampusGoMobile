package com.campusgomobile.ui.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.campusgomobile.data.model.Participation
import com.campusgomobile.ui.theme.Amber600
import com.campusgomobile.ui.theme.Emerald600
import com.campusgomobile.ui.theme.Red600
import com.campusgomobile.ui.theme.Zinc500

private fun formatQuestTypeForDisplay(questType: String?): String? {
    if (questType.isNullOrBlank()) return null
    return questType.trim().replaceFirstChar { it.uppercase() }.replace('_', ' ')
}

/** Format API date (e.g. "2026-03-15 20:00:00" or "2026-03-15") for display. */
private fun formatHistoryDate(updatedAt: String): String {
    return try {
        val s = updatedAt.trim().substringBefore(".")
        val formatter = java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy")
        when {
            s.length >= 19 -> {
                val normalized = s.take(19).replace(" ", "T")
                val dt = java.time.LocalDateTime.parse(normalized, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                dt.format(formatter)
            }
            s.length >= 10 -> {
                val d = java.time.LocalDate.parse(s.take(10), java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                d.format(formatter)
            }
            else -> updatedAt
        }
    } catch (_: Exception) {
        updatedAt.take(10)
    }
}

val HistoryCardCompletedBg = Color(0xFFECFDF5)
val HistoryCardEliminatedBg = Color(0xFFFEF2F2)
val HistoryCardQuitBg = Color(0xFFFFFBEB)
val HistoryCardOtherBg = Color(0xFFF4F4F5)

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun QuestHistoryCard(
    participation: Participation,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusLower = participation.status.lowercase()
    val (statusLabel, statusColor, statusIcon, cardBg) = when (statusLower) {
        "completed", "winner", "won" -> Quad("Winner", Emerald600, Icons.Default.EmojiEvents, HistoryCardCompletedBg)
        "eliminated" -> Quad("Eliminated", Red600, Icons.Default.Cancel, HistoryCardEliminatedBg)
        "quit", "left", "withdrawn" -> Quad("Quit", Amber600, Icons.AutoMirrored.Filled.ExitToApp, HistoryCardQuitBg)
        else -> Quad(
            participation.status.replaceFirstChar { it.uppercase() },
            Zinc500,
            Icons.Default.History,
            HistoryCardOtherBg
        )
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = statusLabel,
                        modifier = Modifier.size(24.dp),
                        tint = statusColor
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = participation.questTitle,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val questTypeLabel = formatQuestTypeForDisplay(participation.questType)
                    if (!questTypeLabel.isNullOrBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = questTypeLabel ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = Zinc500
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Stage ${participation.currentStage} of ${participation.totalStages}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Zinc500
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Last activity: ${(participation.lastSubmissionAt ?: participation.updatedAt)?.let { formatHistoryDate(it) } ?: "—"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Zinc500
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(statusColor.copy(alpha = 0.3f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )
            }
        }
    }
}
