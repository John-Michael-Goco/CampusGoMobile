package com.campusgomobile.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.campusgomobile.data.model.Achievement
import com.campusgomobile.data.model.InventoryEntry
import com.campusgomobile.data.model.Participation
import com.campusgomobile.navigation.NavRoutes
import com.campusgomobile.ui.auth.AuthViewModel
import com.campusgomobile.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel,
    homeViewModel: HomeViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.uiState.collectAsState()
    val user by authViewModel.currentUser.collectAsState(initial = null)
    val homeState by homeViewModel.uiState.collectAsState()
    val lbState by authViewModel.leaderboardState.collectAsState()
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        authViewModel.refreshLeaderboard("week", silent = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (authState.isLoading && homeState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            PullToRefreshBox(
                isRefreshing = homeState.isLoading,
                onRefresh = {
                    homeViewModel.loadAll()
                    authViewModel.refreshLeaderboard("week", silent = true)
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp, bottom = 24.dp)
                ) {
                HeroCard(
                    greeting = getTimeBasedGreeting(),
                    userName = user?.name?.trim()?.takeIf { it.isNotBlank() } ?: "there",
                    points = user?.pointsBalance ?: 0,
                    level = user?.level ?: 1,
                    totalXp = user?.totalXpEarned ?: 0,
                    completedQuests = user?.totalCompletedQuests ?: 0
                )

                Spacer(Modifier.height(16.dp))

                LeaderboardRankBox(
                    rank = lbState.data?.myRank,
                    points = lbState.data?.myValue,
                    onClick = {
                        navController.navigate(NavRoutes.PROFILE_LEADERBOARD) {
                            launchSingleTop = true
                        }
                    }
                )

                Spacer(Modifier.height(24.dp))

                // ── Active Quests ──
                SectionHeader(
                    title = "Active Quests",
                    icon = Icons.Default.Map,
                    onViewAll = {
                        navController.navigate(NavRoutes.TAB_QUESTS) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                if (homeState.activeQuests.isEmpty()) {
                    EmptyPlaceholder("No active quests — go discover one!")
                } else {
                    homeState.activeQuests.forEach { quest ->
                        ActiveQuestCard(
                            quest = quest,
                            onClick = {
                                navController.navigate(
                                    NavRoutes.myQuestDetail(quest.participantId, quest.questId)
                                ) { launchSingleTop = true }
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Inventory ──
                SectionHeader(
                    title = "Inventory",
                    icon = Icons.Default.Backpack,
                    onViewAll = {
                        navController.navigate(NavRoutes.PROFILE_INVENTORY) {
                            launchSingleTop = true
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                if (homeState.inventoryItems.isEmpty()) {
                    EmptyPlaceholder("Your backpack is empty.")
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(end = 4.dp)
                    ) {
                        items(homeState.inventoryItems) { entry ->
                            InventoryItemCard(entry)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Achievements ──
                SectionHeader(
                    title = "Recent Achievements",
                    icon = Icons.Default.MilitaryTech,
                    onViewAll = {
                        navController.navigate(NavRoutes.PROFILE_ACHIEVEMENTS) {
                            launchSingleTop = true
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                if (homeState.earnedAchievements.isEmpty()) {
                    EmptyPlaceholder("No achievements yet — start questing!")
                } else {
                    homeState.earnedAchievements.forEach { achievement ->
                        AchievementCard(achievement)
                        Spacer(Modifier.height(10.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Hero Card
// ─────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(
    greeting: String,
    userName: String,
    points: Int,
    level: Int,
    totalXp: Int,
    completedQuests: Int
) {
    val gradient = Brush.linearGradient(listOf(CampusGoBlue, Indigo500))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(gradient)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.75f)
                )
                Text(
                    text = userName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(Modifier.height(18.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatPill(label = "Pts", value = "$points", color = Amber400)
                    StatPill(label = "Level", value = "$level", color = Emerald500)
                    StatPill(label = "XP", value = "$totalXp", color = Teal400)
                    StatPill(label = "Quests", value = "$completedQuests", color = Violet600)
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Leaderboard Rank Floating Box
// ─────────────────────────────────────────────────────────────

@Composable
private fun LeaderboardRankBox(rank: Int?, points: Int?, onClick: () -> Unit) {
    val rankGradient = Brush.linearGradient(listOf(Amber500, Amber600))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(rankGradient)
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Leaderboard",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Text(
                        text = if (rank != null) "Rank #$rank" else "Unranked",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (points != null) {
                        Text(
                            text = "$points",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Pts earned",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "View leaderboard",
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(18.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Section Header
// ─────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, icon: ImageVector, onViewAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f)
        )
        if (onViewAll != null) {
            TextButton(onClick = onViewAll) {
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Active Quest Card
// ─────────────────────────────────────────────────────────────

@Composable
private fun ActiveQuestCard(quest: Participation, onClick: () -> Unit) {
    val progress = if (quest.totalStages > 0)
        quest.currentStage.toFloat() / quest.totalStages else 0f
    val questTypeColor = when (quest.questType) {
        "event" -> Violet600
        "daily" -> Teal500
        "custom" -> Indigo500
        else -> CampusGoBlue
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(questTypeColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = questTypeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = quest.questTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = quest.questType?.replaceFirstChar { it.uppercase() } ?: "Quest",
                        style = MaterialTheme.typography.labelSmall,
                        color = questTypeColor,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "  ·  Stage ${quest.currentStage}/${quest.totalStages}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = questTypeColor,
                    trackColor = questTypeColor.copy(alpha = 0.12f),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Inventory Item Card
// ─────────────────────────────────────────────────────────────

@Composable
private fun InventoryItemCard(entry: InventoryEntry) {
    val name = entry.storeItem?.name ?: entry.customPrizeDescription ?: "Item"
    Card(
        modifier = Modifier
            .width(140.dp)
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .background(Teal50, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = Teal600,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Achievement Card
// ─────────────────────────────────────────────────────────────

@Composable
private fun AchievementCard(achievement: Achievement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(Amber100, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WorkspacePremium,
                    contentDescription = null,
                    tint = Amber600,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = achievement.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!achievement.description.isNullOrBlank()) {
                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Amber500,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Empty placeholder
// ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyPlaceholder(message: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Greeting helper
// ─────────────────────────────────────────────────────────────

private fun getTimeBasedGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 5..11 -> "Good morning"
        hour in 12..16 -> "Good afternoon"
        else -> "Good evening"
    }
}
