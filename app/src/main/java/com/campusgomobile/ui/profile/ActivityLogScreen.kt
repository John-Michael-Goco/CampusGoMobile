package com.campusgomobile.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.campusgomobile.data.model.ActivityLogEntry
import com.campusgomobile.ui.auth.AuthViewModel
import com.campusgomobile.util.formatActivityTimestamp
import com.campusgomobile.ui.theme.Amber500
import com.campusgomobile.ui.theme.Emerald600
import com.campusgomobile.ui.theme.Violet600
import com.campusgomobile.util.DateRangeOption
import com.campusgomobile.util.computeDateRange

/** Action filter: null = All, or API prefix e.g. "quest_", "store_". */
private data class ActivityFilterOption(val label: String, val actionPrefix: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val activityState by viewModel.activityState.collectAsState()
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    var selectedDateRange by remember { mutableStateOf("all") }

    val filterOptions = listOf(
        ActivityFilterOption("All", null),
        ActivityFilterOption("Quests", "quest_"),
        ActivityFilterOption("Store", "store_"),
        ActivityFilterOption("Achievements", "achievement_"),
        ActivityFilterOption("Items", "item_"),
        ActivityFilterOption("Sign in", "auth_")
    )
    val dateRangeOptions = listOf(
        DateRangeOption("All time", "all"),
        DateRangeOption("Last 7 days", "7d"),
        DateRangeOption("Last 30 days", "30d"),
        DateRangeOption("This month", "month")
    )

    val (dateFrom, dateTo) = computeDateRange(selectedDateRange)

    LaunchedEffect(selectedFilter, selectedDateRange) {
        viewModel.refreshActivity(
            action = selectedFilter,
            dateFrom = dateFrom,
            dateTo = dateTo,
            silent = false
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ProfileScreenTopBar(
                title = "Activity log",
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
                activityState.isLoading && activityState.data == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                activityState.error != null && activityState.data == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = activityState.error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    val list = activityState.data?.activity.orEmpty()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(top = 12.dp, bottom = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            filterOptions.forEach { option ->
                                FilterChip(
                                    selected = selectedFilter == option.actionPrefix,
                                    onClick = { selectedFilter = option.actionPrefix },
                                    label = { Text(option.label) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            dateRangeOptions.forEach { option ->
                                FilterChip(
                                    selected = selectedDateRange == option.key,
                                    onClick = { selectedDateRange = option.key },
                                    label = { Text(option.label) }
                                )
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = list,
                                key = { it.id }
                            ) { entry ->
                                ActivityLogCard(entry = entry)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun iconForAction(actionKey: String): ImageVector = when {
    actionKey.startsWith("quest_joined") -> Icons.Default.Flag
    actionKey.startsWith("quest_stage") -> Icons.Default.CheckCircle
    actionKey.startsWith("store_redeem") -> Icons.Default.ShoppingCart
    actionKey.startsWith("achievement") -> Icons.Default.EmojiEvents
    actionKey.startsWith("item_used") -> Icons.Default.Inventory
    actionKey.startsWith("auth_signin") -> Icons.Default.Login
    else -> Icons.Default.History
}


@Composable
private fun ActivityLogCard(
    entry: ActivityLogEntry,
    modifier: Modifier = Modifier
) {
    val icon = iconForAction(entry.actionKey)
    val tint = when {
        entry.actionKey.startsWith("quest_joined") -> Violet600
        entry.actionKey.startsWith("quest_stage") -> Emerald600
        entry.actionKey.startsWith("store_redeem") -> MaterialTheme.colorScheme.primary
        entry.actionKey.startsWith("achievement") -> Amber500
        entry.actionKey.startsWith("item_used") -> MaterialTheme.colorScheme.tertiary
        entry.actionKey.startsWith("auth_signin") -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(
                    text = entry.displayLabel ?: entry.actionKey,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                entry.detail?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = formatActivityTimestamp(entry.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
