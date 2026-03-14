package com.campusgomobile.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.campusgomobile.data.model.LeaderboardEntry
import com.campusgomobile.ui.auth.AuthViewModel
import com.campusgomobile.ui.theme.Amber500
import com.campusgomobile.ui.theme.Amber600
import com.campusgomobile.ui.theme.CampusGoBlue
import com.campusgomobile.ui.theme.Indigo500
import com.campusgomobile.util.nameToInitials

private val Gold = Color(0xFFFFD700)
private val Silver = Color(0xFFC0C0C0)
private val Bronze = Color(0xFFCD7F32)

private val Periods = listOf("today", "week", "month", "semester", "overall")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val lbState by viewModel.leaderboardState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshLeaderboard(lbState.data?.period ?: "week", silent = true)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ProfileScreenTopBar(
                title = "Leaderboard",
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            PullToRefreshBox(
                isRefreshing = lbState.isLoading,
                onRefresh = { viewModel.refreshLeaderboard(lbState.data?.period ?: "week") },
                modifier = Modifier.fillMaxSize()
            ) {
                LeaderboardContent(
                    lbState = lbState,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun LeaderboardContent(
    lbState: AuthViewModel.LeaderboardUiState,
    viewModel: AuthViewModel
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 24.dp)
    ) {
        if (lbState.isLoading && lbState.data == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (lbState.data != null) {
            val data = lbState.data
            val valueLabel = "Pts earned"
            val topThree = data.entries.take(3)
            val selectedPeriod = data.period

            // Period selector — chips in a card (home-style)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                LazyRow(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(Periods) { _, period ->
                        FilterChip(
                            selected = period == selectedPeriod,
                            onClick = { viewModel.refreshLeaderboard(period) },
                            label = { Text(period.replaceFirstChar { it.uppercase() }) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Podium card (2nd — 1st — 3rd)
            if (topThree.size >= 3) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        PodiumPlace(
                            entry = topThree.getOrNull(1)!!,
                            place = 2,
                            valueLabel = valueLabel,
                            accentColor = Silver
                        )
                        PodiumPlace(
                            entry = topThree[0],
                            place = 1,
                            valueLabel = valueLabel,
                            accentColor = Gold
                        )
                        PodiumPlace(
                            entry = topThree.getOrNull(2)!!,
                            place = 3,
                            valueLabel = valueLabel,
                            accentColor = Bronze
                        )
                    }
                }
            } else if (topThree.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        topThree.forEachIndexed { index, entry ->
                            PodiumPlace(
                                entry = entry,
                                place = index + 1,
                                valueLabel = valueLabel,
                                accentColor = when (index) {
                                    0 -> Gold
                                    1 -> Silver
                                    else -> Bronze
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Section header (home-style)
            SectionHeader(title = "Rankings", icon = Icons.Default.EmojiEvents)
            Spacer(Modifier.height(12.dp))

            val listEntries = data.myRank?.let { myRank ->
                data.entries.filter { it.rank != myRank }
            } ?: data.entries
            if (listEntries.isNotEmpty()) {
                listEntries.forEach { entry ->
                    LeaderboardRow(
                        entry = entry,
                        valueLabel = valueLabel,
                        isMyRank = false
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            // Your rank card — amber gradient (home leaderboard box style)
            data.myRank?.let { myRank ->
                Spacer(Modifier.height(16.dp))
                val rankGradient = Brush.linearGradient(listOf(Amber500, Amber600))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                            Spacer(Modifier.size(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "Your rank",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "#$myRank",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            data.myValue?.let { value ->
                                Text(
                                    text = "$value $valueLabel",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        } else if (lbState.error != null) {
            Text(
                text = lbState.error!!,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.size(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun PodiumPlace(
    entry: LeaderboardEntry,
    place: Int,
    valueLabel: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val avatarSizeDp = when (place) {
        1 -> 72.dp
        2 -> 60.dp
        else -> 56.dp
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (place == 1) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Gold
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        Box(
            modifier = Modifier.size(avatarSizeDp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(avatarSizeDp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .then(
                        when (place) {
                            1 -> Modifier.border(3.dp, Gold, CircleShape)
                            2 -> Modifier.border(2.dp, Silver, CircleShape)
                            3 -> Modifier.border(2.dp, Bronze, CircleShape)
                            else -> Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = nameToInitials(entry.userName),
                    style = when (place) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    },
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 6.dp)
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$place",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = entry.userName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (place == 1) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
        Text(
            text = "${entry.value} $valueLabel",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntry,
    valueLabel: String,
    isMyRank: Boolean,
    modifier: Modifier = Modifier
) {
    val accentColor = when (entry.rank) {
        1 -> Gold
        2 -> Silver
        3 -> Bronze
        else -> MaterialTheme.colorScheme.primary
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${entry.rank}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = accentColor,
                modifier = Modifier.padding(end = 12.dp)
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMyRank) "Y" else nameToInitials(entry.userName),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }
            Spacer(Modifier.size(14.dp))
            Text(
                text = if (isMyRank) "You" else entry.userName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${entry.value} $valueLabel",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
