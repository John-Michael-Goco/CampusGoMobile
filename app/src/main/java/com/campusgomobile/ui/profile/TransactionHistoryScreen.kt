package com.campusgomobile.ui.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.campusgomobile.data.model.PointTransaction
import com.campusgomobile.ui.auth.AuthViewModel
import com.campusgomobile.ui.theme.CampusGoBlue
import com.campusgomobile.ui.theme.Emerald600
import com.campusgomobile.ui.theme.Indigo500
import com.campusgomobile.util.DateRangeOption
import com.campusgomobile.util.computeDateRange
import com.campusgomobile.util.formatActivityTimestamp

private data class TypeFilterOption(val label: String, val type: String?)

private fun iconForTransactionType(type: String): ImageVector = when (type) {
    "quest_reward" -> Icons.Default.EmojiEvents
    "store_redeem" -> Icons.Default.ShoppingCart
    "transfer_in" -> Icons.AutoMirrored.Filled.CallReceived
    "transfer_out" -> Icons.AutoMirrored.Filled.CallMade
    "buy_in", "buy_in_refund" -> Icons.Default.SportsEsports
    else -> Icons.Default.History
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionHistoryScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val transactionsState by viewModel.transactionsState.collectAsState()
    var selectedType by remember { mutableStateOf<String?>(null) }
    var selectedDateRange by remember { mutableStateOf("all") }

    val typeOptions = listOf(
        TypeFilterOption("All", null),
        TypeFilterOption("Quest reward", "quest_reward"),
        TypeFilterOption("Store", "store_redeem"),
        TypeFilterOption("Transfer in", "transfer_in"),
        TypeFilterOption("Transfer out", "transfer_out"),
        TypeFilterOption("Buy-in", "buy_in")
    )
    val dateRangeOptions = listOf(
        DateRangeOption("All time", "all"),
        DateRangeOption("Last 7 days", "7d"),
        DateRangeOption("Last 30 days", "30d"),
        DateRangeOption("This month", "month")
    )

    val (dateFrom, dateTo) = computeDateRange(selectedDateRange)

    LaunchedEffect(selectedType, selectedDateRange) {
        viewModel.refreshTransactions(
            type = selectedType,
            dateFrom = dateFrom,
            dateTo = dateTo,
            silent = true
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ProfileScreenTopBar(
                title = "Transaction history",
                onBackClick = { navController.popBackStack() }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = transactionsState.isLoading,
            onRefresh = {
                viewModel.refreshTransactions(
                    type = selectedType,
                    dateFrom = dateFrom,
                    dateTo = dateTo,
                    silent = false
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val data = transactionsState.data
            val list = data?.transactions.orEmpty()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = 16.dp, bottom = 24.dp)
            ) {
                SectionHeader(title = "Transactions", icon = Icons.Default.History)
                Spacer(Modifier.height(12.dp))

                if (transactionsState.error != null && transactionsState.data == null) {
                    Text(
                        text = transactionsState.error!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    data?.pointsBalance?.let { balance ->
                        val gradient = Brush.linearGradient(listOf(CampusGoBlue, Indigo500))
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
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Current balance",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                    Text(
                                        text = "$balance Pts",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(typeOptions) { option ->
                                    FilterChip(
                                        selected = selectedType == option.type,
                                        onClick = { selectedType = option.type },
                                        label = { Text(option.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(dateRangeOptions) { option ->
                                    FilterChip(
                                        selected = selectedDateRange == option.key,
                                        onClick = { selectedDateRange = option.key },
                                        label = { Text(option.label) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(items = list, key = { it.id }) { tx ->
                            TransactionCard(transaction = tx)
                        }
                    }
                }
            }
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
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun TransactionCard(
    transaction: PointTransaction,
    modifier: Modifier = Modifier
) {
    val isCredit = transaction.amount >= 0
    val amountColor = if (isCredit) Emerald600 else MaterialTheme.colorScheme.error
    val icon = iconForTransactionType(transaction.transactionType)
    val iconTint = if (isCredit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.typeLabel ?: transaction.transactionType,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = formatActivityTimestamp(transaction.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = if (isCredit) "+${transaction.amount}" else "${transaction.amount}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
