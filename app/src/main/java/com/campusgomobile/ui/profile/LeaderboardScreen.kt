package com.campusgomobile.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.campusgomobile.data.model.LeaderboardEntry
import com.campusgomobile.ui.auth.AuthViewModel
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

    androidx.compose.material3.Scaffold(
        modifier = modifier,
        topBar = {
            ProfileScreenTopBar(
                title = "Leaderboard",
                onBackClick = { navController.popBackStack() }
            )
        }
    ) { paddingValues: PaddingValues ->
        // Apply scaffold padding first so content (including loading) sits below the top bar
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
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        // Period selector — horizontally scrollable
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(Periods) { _, period ->
                FilterChip(
                    selected = period == (lbState.data?.period ?: "week"),
                    onClick = { viewModel.refreshLeaderboard(period) },
                    label = { Text(period.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

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
            val valueLabel = data.valueLabel ?: "Points"
            val topThree = data.entries.take(3)
            val rest = data.entries.drop(3)

            // Podium: 2nd — 1st — 3rd
            if (topThree.size >= 3) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
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
            } else if (topThree.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
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

            // Rankings list (all entries; top 3 get colored cards; current user shown only in bottom card)
            val listEntries = data.myRank?.let { myRank ->
                data.entries.filter { it.rank != myRank }
            } ?: data.entries
            if (listEntries.isNotEmpty()) {
                Text(
                    text = "Rankings",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(listEntries) { _, entry ->
                        LeaderboardRow(
                            entry = entry,
                            valueLabel = valueLabel,
                            isMyRank = false
                        )
                    }
                }
            }

            // Your rank card — blue border, "You" label (reference style)
            data.myRank?.let { myRank ->
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "#$myRank",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Y",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = "You",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        data.myValue?.let { value ->
                            Text(
                                text = "$value $valueLabel",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        } else if (lbState.error != null) {
            Text(
                text = lbState.error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(24.dp)
            )
        }
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
        // Crown above avatar for #1 only (reference style)
        if (place == 1) {
            Icon(
                imageVector = Icons.Default.WorkspacePremium,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Gold
            )
            Spacer(modifier = Modifier.height(6.dp))
        }
        // Profile circle with rank badge at bottom-center
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
                        if (place == 1) Modifier.border(3.dp, Gold, CircleShape)
                        else if (place == 3) Modifier.border(2.dp, Bronze, CircleShape)
                        else Modifier
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
            // Rank badge at bottom-center of circle
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
    val cardColor = when {
        isMyRank -> MaterialTheme.colorScheme.surface
        entry.rank == 1 -> Color(0xFFFFF9C4) // light yellow
        entry.rank == 2 -> Color(0xFFF5F5F5) // light grey
        entry.rank == 3 -> Color(0xFFFFE0B2) // light orange
        else -> MaterialTheme.colorScheme.surface
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${entry.rank}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 12.dp)
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isMyRank) "Y" else nameToInitials(entry.userName),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = if (isMyRank) "You" else entry.userName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isMyRank) FontWeight.Medium else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${entry.value} $valueLabel",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
