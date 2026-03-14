package com.campusgomobile.ui.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.FilterChip
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.campusgomobile.data.model.Participation
import com.campusgomobile.data.model.ParticipationPreview
import com.campusgomobile.data.model.Quest
import com.campusgomobile.navigation.NavRoutes
import com.campusgomobile.ui.theme.Blue600
import com.campusgomobile.ui.theme.Emerald600
import com.campusgomobile.ui.theme.Zinc500

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestsScreen(
    viewModel: QuestsViewModel,
    navController: NavController?,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(isVisible) {
        if (isVisible) viewModel.loadAll(silent = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Segment: Discover | My Quests (Discover is landing)
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp)
        ) {
            SegmentedButton(
                selected = uiState.segment == QuestsSegment.Discover,
                onClick = { viewModel.setSegment(QuestsSegment.Discover) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text("Discover") }
            )
            SegmentedButton(
                selected = uiState.segment == QuestsSegment.MyQuests,
                onClick = { viewModel.setSegment(QuestsSegment.MyQuests) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text("My Quests") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        when (uiState.segment) {
            QuestsSegment.MyQuests -> {
                if (navController != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp)
                            .clickable { navController.navigate(NavRoutes.QUEST_HISTORY) },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Quest history",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                val activeList = uiState.participations.filter { it.status.lowercase() in activeStatuses }
                val questTypeByQuestId = uiState.quests.associate { it.id to it.questType }
                val filteredMyQuests = if (uiState.selectedQuestTypeFilter == null) activeList
                    else activeList.filter { p ->
                        val effectiveType = p.questType ?: questTypeByQuestId[p.questId]
                        matchesQuestTypeFilter(effectiveType, uiState.selectedQuestTypeFilter)
                    }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    QUEST_TYPE_FILTER_OPTIONS.forEach { (label, type) ->
                        FilterChip(
                            selected = uiState.selectedQuestTypeFilter == type,
                            onClick = { viewModel.setQuestTypeFilter(type) },
                            label = { Text(label) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                MyQuestsContent(
                    participations = filteredMyQuests,
                    quests = uiState.quests,
                    isLoading = uiState.isLoading,
                    onRefresh = { viewModel.refresh() },
                    onNavigateToHistory = { navController?.navigate(NavRoutes.QUEST_HISTORY) },
                    onQuestClick = { p -> navController?.navigate(NavRoutes.myQuestDetail(p.participantId, p.questId)) }
                )
            }
            QuestsSegment.Discover -> {
                val joinableQuests = uiState.quests.filter { q ->
                    val full = q.maxParticipants > 0 && q.currentParticipants >= q.maxParticipants
                    !full
                }
                val filteredQuests = if (uiState.selectedQuestTypeFilter == null) joinableQuests
                    else joinableQuests.filter { matchesQuestTypeFilter(it.questType, uiState.selectedQuestTypeFilter) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    QUEST_TYPE_FILTER_OPTIONS.forEach { (label, type) ->
                        FilterChip(
                            selected = uiState.selectedQuestTypeFilter == type,
                            onClick = { viewModel.setQuestTypeFilter(type) },
                            label = { Text(label) },
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                DiscoverContent(
                    quests = filteredQuests,
                    isLoading = uiState.isLoading,
                    onRefresh = { viewModel.refresh() },
                    onQuestClick = { quest -> navController?.navigate(NavRoutes.discoverQuestDetail(quest.id)) }
                )
            }
        }
    }
}

private val activeStatuses = setOf("active", "awaiting_ranking")

/** Quest type filter options for My Quests and Discover: Daily, Event, Custom. (Enrollment-only list shows no filter.) */
private val QUEST_TYPE_FILTER_OPTIONS = listOf(
    "All" to null,
    "Daily" to "daily",
    "Event" to "event",
    "Custom" to "custom"
)

private fun normalizeQuestType(questType: String?): String? {
    if (questType.isNullOrBlank()) return null
    return questType.lowercase().replace("_quest", "").trim()
}

private fun matchesQuestTypeFilter(questType: String?, filter: String?): Boolean {
    if (filter == null) return true
    val normalized = normalizeQuestType(questType) ?: return false
    return normalized == filter.lowercase()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MyQuestsContent(
    participations: List<Participation>,
    quests: List<Quest>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onNavigateToHistory: (() -> Unit)? = null,
    onQuestClick: (Participation) -> Unit = {}
) {
    val activeList = participations.filter { it.status.lowercase() in activeStatuses }

    when {
        isLoading && participations.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        activeList.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No active quests",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Scan a QR to join one or browse Discover.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Zinc500
                    )
                }
            }
        }
        else -> {
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeList, key = { it.participantId }) { participation ->
                        val questFromList = quests.find { it.id == participation.questId }
                        val questDescription = questFromList?.description
                        val questType = participation.questType ?: questFromList?.questType
                        MyQuestCard(
                            participation = participation,
                            questDescription = questDescription,
                            questType = questType,
                            onClick = { onQuestClick(participation) }
                        )
                    }
                }
            }
        }
    }
}

private fun formatQuestTypeForList(questType: String?): String {
    if (questType.isNullOrBlank()) return ""
    return questType.trim().replaceFirstChar { it.uppercase() }.replace('_', ' ')
}

@Composable
private fun MyQuestCard(
    participation: Participation,
    questDescription: String? = null,
    questType: String? = null,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val statusLabel = when (participation.status) {
        "awaiting_ranking" -> "Results pending"
        else -> "Active"
    }
    val statusColor = if (participation.status == "awaiting_ranking") Blue600 else Emerald600
    val isAwaitingRanking = participation.status.equals("awaiting_ranking", ignoreCase = true)
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val typeLabel = formatQuestTypeForList(questType)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(statusColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )
            Column(modifier = Modifier.weight(1f).padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = participation.questTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = statusColor
                        )
                    }
                }
                if (typeLabel.isNotBlank()) {
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                questDescription?.takeIf { it.isNotBlank() }?.let { desc ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Emerald600
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Stage ${participation.currentStage} of ${participation.totalStages}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (isAwaitingRanking) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Blue600.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Blue600
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Waiting for ranking results",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Blue600
                        )
                    }
                }
                participation.preview?.let { preview ->
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Emerald600.copy(alpha = 0.08f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PreviewRow(preview = preview)
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewRow(
    preview: ParticipationPreview,
    modifier: Modifier = Modifier
) {
    val hasLocation = !preview.nextLocationHint.isNullOrBlank()
    val opensAt = preview.nextStageOpensAt
    val stageNum = preview.nextStageNumber
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (hasLocation) {
            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Emerald600
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Next: ${preview.nextLocationHint}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (!opensAt.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = Blue600
            )
            Spacer(Modifier.width(6.dp))
            val stageText = if (stageNum != null) "Stage $stageNum opens at " else "Opens at "
            Text(
                text = stageText + formatDateTime(opensAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiscoverContent(
    quests: List<Quest>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onQuestClick: (Quest) -> Unit = {}
) {
    when {
        isLoading && quests.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        quests.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Explore,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No quests available",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Check back later or scan a QR at an event.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Zinc500
                    )
                }
            }
        }
        else -> {
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(quests, key = { it.id }) { quest ->
                        DiscoverQuestCard(
                            quest = quest,
                            onClick = { onQuestClick(quest) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiscoverQuestCard(
    quest: Quest,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val statusLabel = when (quest.status) {
        "upcoming" -> "Upcoming"
        else -> "Ongoing"
    }
    val statusColor = if (quest.status == "ongoing") Emerald600 else Blue600
    val statusBgAlpha = if (quest.status == "ongoing") 0.12f else 0.15f
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val mutedColor = MaterialTheme.colorScheme.outline
    val accentColor = Emerald600

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor, RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
            )
            Column(modifier = Modifier.weight(1f).padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = statusBgAlpha))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = statusColor
                    )
                }
                }
                if (formatQuestTypeForList(quest.questType).isNotBlank()) {
                    Text(
                        text = formatQuestTypeForList(quest.questType),
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                quest.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${quest.stagesCount} stages",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryColor
                    )
                    if (quest.rewardPoints > 0) {
                        Text(
                            text = "${quest.rewardPoints} pts reward",
                            style = MaterialTheme.typography.bodyMedium,
                            color = accentColor
                        )
                    }
                    if (quest.buyInPoints > 0) {
                        Text(
                            text = "Entry: ${quest.buyInPoints} pts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = secondaryColor
                        )
                    }
                }
                if (quest.maxParticipants > 0) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Participants:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "${quest.currentParticipants} / ${quest.maxParticipants}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = accentColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (quest.status == "upcoming" || quest.firstStageLocationHint.isNullOrBlank())
                            "Coming soon"
                        else
                            "First stage: ${quest.firstStageLocationHint}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryColor
                    )
                }
            }
        }
    }
}

private fun formatDateTime(dateTimeStr: String): String {
    return try {
        val parts = dateTimeStr.replace(" ", "T").split("T")
        val datePart = parts.getOrNull(0) ?: dateTimeStr
        val timePart = parts.getOrNull(1)?.take(5) // HH:mm
        when {
            timePart != null -> "$datePart $timePart"
            else -> datePart
        }
    } catch (_: Exception) {
        dateTimeStr
    }
}
