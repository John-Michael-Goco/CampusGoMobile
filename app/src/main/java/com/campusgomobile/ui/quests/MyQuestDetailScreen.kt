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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.campusgomobile.data.model.DiscoverQuestDetailData
import com.campusgomobile.data.model.MyQuestDetailData
import com.campusgomobile.data.model.Quest
import com.campusgomobile.data.model.StageDetail
import com.campusgomobile.ui.theme.Amber500
import com.campusgomobile.ui.theme.Blue600
import com.campusgomobile.ui.theme.CampusGoBlue
import com.campusgomobile.ui.theme.Emerald600
import com.campusgomobile.ui.theme.Indigo500
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
        modifier = modifier.fillMaxSize(),
        topBar = {
            ProfileScreenTopBar(
                title = "Quest details",
                onBackClick = {
                    viewModel.clearMyQuestDetail()
                    navController.popBackStack()
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                isElimination = playState.isElimination,
                currentStage = playState.currentStage,
                totalStages = stages.size,
                questionType = questionType
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Stages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.height(4.dp))
        }
        itemsIndexed(stages) { index, stage ->
            val isUnlocked = stage.stageNumber < currentStage ||
                (stage.stageNumber == currentStage && !stageLocked)
            val isCurrent = stage.stageNumber == currentStage
            StageCard(
                stage = stage,
                isUnlocked = isUnlocked,
                isCurrent = isCurrent,
                questQuestionType = questionType,
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
    currentStage: Int = 1,
    totalStages: Int = 1,
    questionType: String? = null,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(listOf(CampusGoBlue, Indigo500))

    Column(modifier = modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(gradient)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = quest.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (isElimination && totalStages > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Stage $currentStage of $totalStages",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                quest.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(16.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LabelLine(label = "Type", value = formatQuestTypeDisplay(quest.questType))
                    LabelLine(label = "Status", value = formatParticipantStatus(status))
                    questionType?.takeIf { it.isNotBlank() }?.let { qt ->
                        LabelLine(label = "Question type", value = formatQuestionTypeDisplay(qt))
                    }
                    if (isElimination) LabelLine(label = "Elimination", value = "Yes")
                }

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
                            text = "${quest.buyInPoints} Pts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

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
                        text = "${quest.rewardPoints} Pts",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Amber500
                    )
                }

                quest.rewardCustomPrize?.takeIf { it.isNotBlank() }?.let { prize ->
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Custom prize: $prize",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Amber500
                    )
                }

                if (isElimination && totalStages > 0) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Emerald600.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Stage:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "$currentStage / $totalStages",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Emerald600
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
    questQuestionType: String? = null,
    showLocationHint: Boolean = true,
    currentStageLabel: String = "Current",
    modifier: Modifier = Modifier
) {
    val stageColor = if (isCurrent) Emerald600 else Blue600
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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

            // Passing score only for multiple-choice; hide for QR-only. Don't show when 0 (backend may send 0 when unset).
            val effectiveQuestionType = stage.questionType ?: questQuestionType
            val showPassingScore = "multiple_choice".equals(effectiveQuestionType, ignoreCase = true)
            if (showPassingScore && (stage.passingScore != null && stage.passingScore!! > 0)) {
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
        modifier = modifier.fillMaxSize(),
        topBar = {
            ProfileScreenTopBar(
                title = "Past quest",
                onBackClick = {
                    viewModel.clearQuestHistoryDetail()
                    navController.popBackStack()
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
        modifier = modifier.fillMaxSize(),
        topBar = {
            ProfileScreenTopBar(
                title = "Quest",
                onBackClick = {
                    viewModel.clearDiscoverQuestDetail()
                    navController.popBackStack()
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
                isElimination = quest.isElimination,
                questionType = questionType
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Stages",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.height(4.dp))
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
                questQuestionType = questionType,
                showLocationHint = !isUpcoming
            )
        }
    }
}

private fun formatQuestTypeDisplay(questType: String?): String {
    if (questType.isNullOrBlank()) return "Not set"
    return questType.trim().replaceFirstChar { it.uppercase() }.replace('_', ' ')
}

private fun formatQuestionTypeDisplay(questionType: String?): String {
    if (questionType.isNullOrBlank()) return "—"
    return when (questionType.trim().lowercase()) {
        "multiple_choice" -> "Multiple choice"
        "qr_scan" -> "QR scan"
        "qr_only" -> "QR only"
        else -> questionType.trim().replaceFirstChar { it.uppercase() }.replace('_', ' ')
    }
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
