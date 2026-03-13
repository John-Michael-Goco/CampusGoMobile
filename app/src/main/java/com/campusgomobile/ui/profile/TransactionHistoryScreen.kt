package com.campusgomobile.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SportsEsports
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.campusgomobile.data.model.PointTransaction
import com.campusgomobile.ui.auth.AuthViewModel
import com.campusgomobile.util.formatActivityTimestamp
import com.campusgomobile.ui.theme.Amber500
import com.campusgomobile.ui.theme.Emerald600
import com.campusgomobile.util.DateRangeOption
import com.campusgomobile.util.computeDateRange

private data class TypeFilterOption(val label: String, val type: String?)

private fun iconForTransactionType(type: String): ImageVector = when (type) {
    "quest_reward" -> Icons.Default.EmojiEvents
    "store_redeem" -> Icons.Default.ShoppingCart
    "transfer_in" -> Icons.Default.CallReceived
    "transfer_out" -> Icons.Default.CallMade
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
            silent = false
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            ProfileScreenTopBar(
                title = "Transaction history",
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
                transactionsState.isLoading && transactionsState.data == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                transactionsState.error != null && transactionsState.data == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = transactionsState.error!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    val data = transactionsState.data
                    val list = data?.transactions.orEmpty()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        data?.pointsBalance?.let { balance ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 8.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Current balance",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "$balance pts",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            typeOptions.forEach { option ->
                                FilterChip(
                                    selected = selectedType == option.type,
                                    onClick = { selectedType = option.type },
                                    label = { Text(option.label) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
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
                            ) { tx ->
                                TransactionCard(transaction = tx)
                            }
                        }
                    }
                }
            }
        }
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
    val iconTint = if (isCredit) Amber500 else MaterialTheme.colorScheme.primary

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
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(
                    text = transaction.typeLabel ?: transaction.transactionType,
                    style = MaterialTheme.typography.titleSmall,
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
                fontWeight = FontWeight.SemiBold,
                color = amountColor
            )
        }
    }
}
