package com.campusgomobile.ui.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.campusgomobile.data.model.Participation
import com.campusgomobile.navigation.NavRoutes
import com.campusgomobile.ui.profile.ProfileScreenTopBar
import com.campusgomobile.ui.theme.Emerald600
import com.campusgomobile.util.computeDateRange
import com.campusgomobile.util.DateRangeOption

/** Quest type filter: value in API is lowercase (enrollment, daily, event, custom). */
private val QUEST_TYPE_OPTIONS = listOf(
    "All" to null,
    "Enrollment" to "enrollment",
    "Daily" to "daily",
    "Event" to "event",
    "Custom" to "custom"
)

private val DATE_RANGE_OPTIONS = listOf(
    DateRangeOption("All time", ""),
    DateRangeOption("Last 7 days", "7d"),
    DateRangeOption("Last 30 days", "30d"),
    DateRangeOption("This month", "month"),
    DateRangeOption("This semester", "semester")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestHistoryScreen(
    navController: NavController,
    viewModel: QuestsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val historyList = uiState.historyList

    var searchQuery by remember { mutableStateOf("") }
    var debouncedSearch by remember { mutableStateOf("") }
    var selectedQuestType by remember { mutableStateOf<String?>(null) }
    var selectedDateRange by remember { mutableStateOf("") }
    val (dateFrom, dateTo) = computeDateRange(selectedDateRange)
    val focusManager = LocalFocusManager.current

    LaunchedEffect(searchQuery) {
        delay(400)
        debouncedSearch = searchQuery
    }

    LaunchedEffect(debouncedSearch, selectedQuestType, dateFrom, dateTo) {
        viewModel.loadHistory(
            search = debouncedSearch.ifBlank { null },
            questType = selectedQuestType,
            dateFrom = dateFrom,
            dateTo = dateTo,
            page = 1
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ProfileScreenTopBar(
                title = "Quest history",
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 24.dp)
            ) {
                QuestHistorySectionHeader()
                Spacer(Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search by quest name") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                var questTypeExpanded by remember { mutableStateOf(false) }
                val questTypeLabel = QUEST_TYPE_OPTIONS.find { it.second == selectedQuestType }?.first ?: "All"
                @Suppress("DEPRECATION")
                ExposedDropdownMenuBox(
                    expanded = questTypeExpanded,
                    onExpandedChange = { questTypeExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = questTypeLabel,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = questTypeExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                    DropdownMenu(
                        expanded = questTypeExpanded,
                        onDismissRequest = { questTypeExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .clip(RoundedCornerShape(12.dp)),
                        tonalElevation = 4.dp
                    ) {
                        QUEST_TYPE_OPTIONS.forEach { (label, type) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                onClick = {
                                    selectedQuestType = type
                                    questTypeExpanded = false
                                }
                            )
                        }
                    }
                }

                var dateRangeExpanded by remember { mutableStateOf(false) }
                val dateRangeLabel = DATE_RANGE_OPTIONS.find { it.key == selectedDateRange }?.label ?: "All time"
                @Suppress("DEPRECATION")
                ExposedDropdownMenuBox(
                    expanded = dateRangeExpanded,
                    onExpandedChange = { dateRangeExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = dateRangeLabel,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dateRangeExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    )
                    DropdownMenu(
                        expanded = dateRangeExpanded,
                        onDismissRequest = { dateRangeExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.5f)
                            .clip(RoundedCornerShape(12.dp)),
                        tonalElevation = 4.dp
                    ) {
                        DATE_RANGE_OPTIONS.forEach { (label, key) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                },
                                onClick = {
                                    selectedDateRange = key
                                    dateRangeExpanded = false
                                }
                            )
                        }
                    }
                }
                }
            }

            if (uiState.historyError != null) {
                Text(
                    text = uiState.historyError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            }
            Box(modifier = Modifier.weight(1f)) {
                val isInitialLoad = debouncedSearch.isBlank() && selectedQuestType == null && selectedDateRange.isEmpty()
                if (uiState.historyLoading && historyList.isEmpty() && isInitialLoad) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator()
                    }
                } else if (historyList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Emerald600.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = Emerald600
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "No quest history yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Completed, eliminated, or quit quests will appear here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    val pullToRefreshState = rememberPullToRefreshState()
                    PullToRefreshBox(
                        isRefreshing = uiState.historyLoading,
                        onRefresh = {
                            viewModel.loadHistory(
                                search = debouncedSearch.ifBlank { null },
                                questType = selectedQuestType,
                                dateFrom = dateFrom,
                                dateTo = dateTo,
                                page = 1
                            )
                        },
                        state = pullToRefreshState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(historyList, key = { it.participantId }) { participation ->
                                QuestHistoryCard(
                                    participation = participation,
                                    onClick = {
                                        navController.navigate(
                                            NavRoutes.questHistoryDetail(
                                                participation.participantId,
                                                participation.questId,
                                                participation.status,
                                                participation.currentStage,
                                                participation.totalStages
                                            )
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun QuestHistorySectionHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Quest history",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
