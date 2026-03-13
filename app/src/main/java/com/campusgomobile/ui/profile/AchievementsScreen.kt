package com.campusgomobile.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.campusgomobile.data.model.Achievement
import com.campusgomobile.ui.auth.AuthViewModel
import com.campusgomobile.ui.theme.Amber500
import com.campusgomobile.ui.theme.Amber600

private enum class AchievementFilter { All, Achieved, Unachieved }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val achievementsState by viewModel.achievementsState.collectAsState()
    var filter by remember { mutableStateOf(AchievementFilter.All) }

    LaunchedEffect(Unit) {
        viewModel.refreshAchievements(silent = true)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ProfileScreenTopBar(
                title = "Achievements",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues: PaddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                achievementsState.isLoading && achievementsState.data == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                achievementsState.error != null && achievementsState.data == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = achievementsState.error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    val list = achievementsState.data?.achievements.orEmpty()
                    val filtered = when (filter) {
                        AchievementFilter.All -> list
                        AchievementFilter.Achieved -> list.filter { it.earned }
                        AchievementFilter.Unachieved -> list.filter { !it.earned }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = filter == AchievementFilter.All,
                                onClick = { filter = AchievementFilter.All },
                                label = { Text("All") }
                            )
                            FilterChip(
                                selected = filter == AchievementFilter.Achieved,
                                onClick = { filter = AchievementFilter.Achieved },
                                label = { Text("Achieved") }
                            )
                            FilterChip(
                                selected = filter == AchievementFilter.Unachieved,
                                onClick = { filter = AchievementFilter.Unachieved },
                                label = { Text("Unachieved") }
                            )
                        }
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = filtered,
                                key = { it.id }
                            ) { achievement ->
                                AchievementCard(achievement = achievement)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    modifier: Modifier = Modifier
) {
    val earned = achievement.earned
    val surfaceColor = if (earned) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val borderColor = if (earned) Amber500.copy(alpha = 0.5f) else Color.Transparent

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (earned) Modifier.border(2.dp, borderColor, RoundedCornerShape(12.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (earned) Amber500.copy(alpha = 0.2f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (earned) Icons.Default.EmojiEvents else Icons.Default.Lock,
                    contentDescription = if (earned) "Earned" else "Locked",
                    tint = if (earned) Amber600 else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = achievement.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (earned) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                achievement.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (earned && achievement.earnedAt != null) {
                    Text(
                        text = "Earned ${achievement.earnedAt}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Amber600
                    )
                }
            }
            if (earned) {
                Text(
                    text = "Earned",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Amber600
                )
            }
        }
    }
}
