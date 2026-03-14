package com.campusgomobile.ui.quests

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import com.campusgomobile.ui.profile.ProfileScreenTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.campusgomobile.data.model.DiscoverQuestDetailData
import com.campusgomobile.data.model.MyQuestDetailData
import com.campusgomobile.data.model.Quest
import com.campusgomobile.data.model.StageDetail
import com.campusgomobile.ui.theme.Amber500
import com.campusgomobile.ui.theme.Blue600
import com.campusgomobile.ui.theme.Emerald600
import com.campusgomobile.util.formatActivityTimestamp

@Composable
fun MyQuestDetailScreen(
    navController: NavController,
    viewModel: QuestsViewModel,
    participantId: Int,
    questId: Int,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(participantId, questId) {
        viewModel.loadMyQuestDetail(participantId, questId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ProfileScreenTopBar(
                title = "Quest details",
                onBackClick = {
                    viewModel.clearMyQuestDetail()
                    navController.popBackStack()
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.myQuestDetailLoading && uiState.myQuestDetail == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.myQuestDetailError != null && uiState.myQuestDetail == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.myQuestDetailError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
            uiState.myQuestDetail != null -> {
                val data = uiState.myQuestDetail!!
                LaunchedEffect(uiState.quitSuccess) {
                    if (uiState.quitSuccess) {
                        viewModel.clearQuitSuccess()
                        navController.popBackStack()
                    }
                }
                MyQuestDetailContent(
                    data = data,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    viewModel = viewModel,
                    quitLoading = uiState.quitLoading,
                    quitError = uiState.quitError
                )
            }
        }
    }
}

@Composable
fun MyQuestDetailContent(
    data: MyQuestDetailData,
    modifier: Modifier = Modifier,
    viewModel: QuestsViewModel? = null,
    quitLoading: Boolean = false,
    quitError: String? = null,
    currentStageLabel: String = "Current"
) {
    val quest = data.quest
    val playState = data.playState
    val stages = data.stages
    val currentStage = playState.currentStage
    val stageLocked = playState.stageLocked
    val questionType = playState.questionType ?: quest.questionType
    val inProgress = playState.status.equals("active", ignoreCase = true) ||
        playState.status.equals("awaiting_ranking", ignoreCase = true)
    val showQuitSection = viewModel != null && inProgress
    val canQuit = playState.canQuit

    LazyColumn(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            QuestInfoCard(
                quest = quest,
                status = playState.status,
                isElimination = playState.isElimination
            )
        }
        item {
            Text(
                text = "Stages",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        itemsIndexed(stages) { index, stage ->
            val isUnlocked = stage.stageNumber < currentStage ||
                (stage.stageNumber == currentStage && !stageLocked)
            val isCurrent = stage.stageNumber == currentStage
            StageCard(
                stage = stage,
                isUnlocked = isUnlocked,
                isCurrent = isCurrent,
                showPassingScore = "multiple_choice".equals(questionType, ignoreCase = true),
                currentStageLabel = currentStageLabel
            )
        }
        if (showQuitSection) {
            item {
                Spacer(Modifier.height(8.dp))
                if (quitError != null) {
                    Text(
                        text = quitError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (!canQuit && playState.quitGuardReason != null) {
                    Text(
                        text = playState.quitGuardReason!!,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                if (canQuit) {
                    Button(
                        onClick = { viewModel!!.quitQuest(playState.participantId) },
                        enabled = !quitLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        if (quitLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onError
                            )
                        } else {
                            Text("Leave quest")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestInfoCard(
    quest: Quest,
    status: String,
    isElimination: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(Emerald600, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )
            Column(modifier = Modifier.weight(1f).padding(20.dp)) {
                Text(
                    text = quest.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                quest.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(16.dp))

                // Type, Status, Elimination (if applicable)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LabelLine(label = "Type", value = formatQuestTypeDisplay(quest.questType))
                    LabelLine(label = "Status", value = formatParticipantStatus(status))
                    if (isElimination) LabelLine(label = "Elimination", value = "Yes")
                }

            // Buy-in (only if > 0)
            if (quest.buyInPoints > 0) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Buy-in:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "${quest.buyInPoints} pts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Reward points
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Reward:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${quest.rewardPoints} pts",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Amber500
                )
            }

            // Reward custom prize (only if present)
            quest.rewardCustomPrize?.takeIf { it.isNotBlank() }?.let { prize ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Custom prize: $prize",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Amber500
                )
            }

            // Current / max participants (only if max > 0)
            if (quest.maxParticipants > 0) {
                Spacer(Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = "Participants:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "${quest.currentParticipants} / ${quest.maxParticipants}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun LabelLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LabelChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StageCard(
    stage: StageDetail,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    showPassingScore: Boolean,
    showLocationHint: Boolean = true,
    currentStageLabel: String = "Current",
    modifier: Modifier = Modifier
) {
    val stageColor = if (isCurrent) Emerald600 else Blue600
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = stageColor
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Stage ${stage.stageNumber}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isCurrent) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = currentStageLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = Emerald600,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Emerald600.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Location hint (only if unlocked and not hidden e.g. for upcoming quests)
            if (showLocationHint && isUnlocked && !stage.locationHint.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = stageColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stage.locationHint!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Passing score (if multiple choice)
            if (showPassingScore && stage.passingScore != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Quiz,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Passing score: ${stage.passingScore}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

        }
    }
}

/** Detail view for a quest from history (winner = all stages; eliminated/quit = stages before quit). */
@Composable
fun QuestHistoryDetailScreen(
    navController: NavController,
    viewModel: QuestsViewModel,
    participantId: Int,
    questId: Int,
    status: String,
    currentStage: Int,
    totalStages: Int,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(participantId, questId, status, currentStage, totalStages) {
        viewModel.loadQuestHistoryDetail(
            participantId = participantId,
            questId = questId,
            status = status,
            currentStage = currentStage,
            totalStages = totalStages
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ProfileScreenTopBar(
                title = "Past quest",
                onBackClick = {
                    viewModel.clearQuestHistoryDetail()
                    navController.popBackStack()
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.questHistoryDetailLoading && uiState.questHistoryDetail == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.questHistoryDetailError != null && uiState.questHistoryDetail == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.questHistoryDetailError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
            uiState.questHistoryDetail != null -> {
                MyQuestDetailContent(
                    data = uiState.questHistoryDetail!!,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    currentStageLabel = "Last stage taken"
                )
            }
        }
    }
}

/** Detail view for a quest from Discover (view before join). */
@Composable
fun DiscoverQuestDetailScreen(
    navController: NavController,
    viewModel: QuestsViewModel,
    questId: Int,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(questId) {
        viewModel.loadDiscoverQuestDetail(questId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ProfileScreenTopBar(
                title = "Quest",
                onBackClick = {
                    viewModel.clearDiscoverQuestDetail()
                    navController.popBackStack()
                }
            )
        }
    ) { paddingValues ->
        when {
            uiState.discoverQuestDetailLoading && uiState.discoverQuestDetail == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.discoverQuestDetailError != null && uiState.discoverQuestDetail == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.discoverQuestDetailError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
            uiState.discoverQuestDetail != null -> {
                DiscoverQuestDetailContent(
                    data = uiState.discoverQuestDetail!!,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
fun DiscoverQuestDetailContent(
    data: DiscoverQuestDetailData,
    modifier: Modifier = Modifier
) {
    val quest = data.quest
    val stages = data.stages
    val questionType = quest.questionType
    val showPassingScore = "multiple_choice".equals(questionType, ignoreCase = true)
    val isUpcoming = quest.status.equals("upcoming", ignoreCase = true)

    LazyColumn(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            QuestInfoCard(
                quest = quest,
                status = formatQuestStatusDisplay(quest.status),
                isElimination = quest.isElimination
            )
        }
        item {
            Text(
                text = "Stages",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = "Find and scan the QR code to join the quest.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            if (isUpcoming && !quest.startDate.isNullOrBlank()) {
                Text(
                    text = "Quest unlocks on: ${formatStageDate(quest.startDate)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else if (isUpcoming) {
                Text(
                    text = "Quest unlocks soon.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
        itemsIndexed(stages) { _, stage ->
            StageCard(
                stage = stage,
                isUnlocked = true,
                isCurrent = false,
                showPassingScore = showPassingScore,
                showLocationHint = !isUpcoming
            )
        }
    }
}

private fun formatQuestTypeDisplay(questType: String?): String {
    if (questType.isNullOrBlank()) return "Not set"
    return questType.trim().replaceFirstChar { it.uppercase() }.replace('_', ' ')
}

private fun formatQuestStatusDisplay(questStatus: String?): String {
    if (questStatus.isNullOrBlank()) return "—"
    return when (questStatus.lowercase()) {
        "ongoing" -> "Ongoing"
        "upcoming" -> "Upcoming"
        else -> questStatus.trim().replaceFirstChar { it.uppercase() }
    }
}

private fun formatParticipantStatus(status: String): String {
    return when (status.lowercase()) {
        "completed", "winner", "won" -> "Winner"
        "eliminated" -> "Eliminated"
        "quit", "left", "withdrawn" -> "Quit"
        "active", "awaiting_ranking" -> status.replaceFirstChar { it.uppercase() }.replace('_', ' ')
        else -> status.replaceFirstChar { it.uppercase() }.replace('_', ' ')
    }
}

private fun formatStageDate(apiDate: String): String {
    val normalized = apiDate.replace("T", " ")
    return try {
        formatActivityTimestamp(normalized)
    } catch (_: Exception) {
        apiDate
    }
}
