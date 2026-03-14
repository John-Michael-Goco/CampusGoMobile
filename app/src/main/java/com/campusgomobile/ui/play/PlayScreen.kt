package com.campusgomobile.ui.play

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.campusgomobile.data.model.PlayChoice
import com.campusgomobile.data.model.PlayQuestion
import com.campusgomobile.data.model.PlayStateResponse
import com.campusgomobile.ui.profile.ProfileScreenTopBar
import com.campusgomobile.ui.theme.Amber500
import com.campusgomobile.ui.theme.Emerald600

@Composable
fun PlayScreen(
    navController: NavController,
    viewModel: PlayViewModel,
    participantId: Int,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(participantId) {
        viewModel.loadPlayState(participantId)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ProfileScreenTopBar(
                title = "Play",
                onBackClick = {
                    viewModel.reset()
                    navController.popBackStack()
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.playState == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
                uiState.submitResult != null -> {
                    SubmitResultContent(
                        result = uiState.submitResult!!,
                        onDone = {
                            viewModel.reset()
                            navController.popBackStack()
                        }
                    )
                }
                uiState.playState != null -> {
                    val ps = uiState.playState!!
                    when {
                        ps.awaitingRanking -> {
                            AwaitingRankingContent()
                        }
                        ps.status.equals("completed", ignoreCase = true) ||
                        ps.status.equals("eliminated", ignoreCase = true) -> {
                            SubmitResultContent(
                                result = ps,
                                onDone = {
                                    viewModel.reset()
                                    navController.popBackStack()
                                }
                            )
                        }
                        ps.stageLocked -> {
                            StageLockedContent(playState = ps)
                        }
                        ps.stage != null && !ps.stage.questions.isNullOrEmpty() -> {
                            McqContent(
                                playState = ps,
                                questions = ps.stage.questions!!,
                                selectedAnswers = uiState.selectedAnswers,
                                onSelectAnswer = { qId, cId -> viewModel.selectAnswer(qId, cId) },
                                onSubmit = { viewModel.submit(participantId) },
                                submitting = uiState.submitting,
                                submitError = uiState.submitError
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No questions available for this stage.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun McqContent(
    playState: PlayStateResponse,
    questions: List<PlayQuestion>,
    selectedAnswers: Map<Int, Int>,
    onSelectAnswer: (Int, Int) -> Unit,
    onSubmit: () -> Unit,
    submitting: Boolean,
    submitError: String?
) {
    val unansweredQuestions = questions.filter { !it.alreadyAnswered }
    val allAnswered = unansweredQuestions.all { selectedAnswers.containsKey(it.id) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = Emerald600,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Stage ${playState.currentStage} of ${playState.totalStages}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            val passingScore = playState.stage?.passingScore
            if (passingScore != null && passingScore > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Passing score: $passingScore/${unansweredQuestions.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        itemsIndexed(unansweredQuestions) { index, question ->
            QuestionCard(
                index = index + 1,
                question = question,
                selectedChoiceId = selectedAnswers[question.id],
                onSelectChoice = { choiceId -> onSelectAnswer(question.id, choiceId) }
            )
        }

        item {
            if (submitError != null) {
                Text(
                    text = submitError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = onSubmit,
                enabled = allAnswered && !submitting,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald600)
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text("Submit Answers", color = Color.White)
                }
            }

            if (!allAnswered) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Answer all questions to submit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuestionCard(
    index: Int,
    question: PlayQuestion,
    selectedChoiceId: Int?,
    onSelectChoice: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Question $index",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = question.questionText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(12.dp))

            question.choices?.forEach { choice ->
                ChoiceItem(
                    choice = choice,
                    isSelected = selectedChoiceId == choice.id,
                    onClick = { onSelectChoice(choice.id) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ChoiceItem(
    choice: PlayChoice,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Emerald600 else MaterialTheme.colorScheme.outlineVariant,
        label = "choiceBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Emerald600.copy(alpha = 0.08f) else Color.Transparent,
        label = "choiceBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (isSelected) Emerald600 else MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = choice.choiceText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SubmitResultContent(
    result: PlayStateResponse,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val passed = result.passed == true
        val eliminated = result.failed == true
        val completed = result.outcome.equals("completed", ignoreCase = true)

        val icon = when {
            completed -> Icons.Default.EmojiEvents
            eliminated -> Icons.Default.SentimentDissatisfied
            else -> Icons.Default.CheckCircle
        }
        val iconColor = when {
            completed -> Amber500
            eliminated -> MaterialTheme.colorScheme.error
            else -> Emerald600
        }

        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = iconColor
        )
        Spacer(Modifier.height(16.dp))

        if (result.correctCount != null && result.totalCount != null) {
            Text(
                text = "${result.correctCount} / ${result.totalCount} correct",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))
        }

        if (!result.message.isNullOrBlank()) {
            Text(
                text = result.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
        }

        if (result.rewards != null) {
            val rewards = result.rewards
            if (rewards.pointsEarned > 0) {
                Text(
                    text = "+${rewards.pointsEarned} Pts earned",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Amber500
                )
            }
            if (!rewards.customPrize.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Prize: ${rewards.customPrize}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (rewards.levelUp) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Level up! ${rewards.previousLevel ?: ""} → ${rewards.newLevel ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Emerald600
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        if (!result.nextStageLocationHint.isNullOrBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Emerald600,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Next: ${result.nextStageLocationHint}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done")
        }
    }
}

@Composable
private fun AwaitingRankingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.HourglassTop,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Amber500
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Waiting for results\u2026",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "You will advance or be eliminated based on your score.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun StageLockedContent(playState: PlayStateResponse) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.HourglassTop,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Stage ${playState.nextStageNumber ?: playState.currentStage} is locked",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (!playState.nextStageOpensAt.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Opens at: ${playState.nextStageOpensAt}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
