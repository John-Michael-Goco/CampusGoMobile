package com.campusgomobile.ui.store

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.campusgomobile.data.model.StoreItem
import com.campusgomobile.navigation.NavRoutes
import com.campusgomobile.ui.theme.Blue50
import com.campusgomobile.ui.theme.Blue600
import com.campusgomobile.ui.util.showStyledToast
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreScreen(
    viewModel: StoreViewModel,
    navController: NavController?,
    isVisible: Boolean = true,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var itemToRedeem by remember { mutableStateOf<StoreItem?>(null) }
    val isDark = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        viewModel.clearError()
        viewModel.clearRedeemSuccess()
    }

    LaunchedEffect(uiState.redeemSuccessMessage) {
        uiState.redeemSuccessMessage?.let { itemName ->
            showStyledToast(context, "$itemName redeemed")
            viewModel.clearRedeemSuccess()
        }
    }

    LaunchedEffect(isVisible) {
        if (isVisible) viewModel.loadStore(silent = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        // Points balance + My items link — blue accent
        val balanceBg = if (isDark) MaterialTheme.colorScheme.primaryContainer else Blue50
        val balanceFg = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else Blue600
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = balanceBg),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(balanceFg.copy(alpha = 0.15f), shape = RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redeem,
                            contentDescription = null,
                            tint = balanceFg,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(
                        text = "${uiState.pointsBalance} pts",
                        style = MaterialTheme.typography.titleMedium,
                        color = balanceFg
                    )
                }
            }
            if (navController != null) {
                TextButton(onClick = { navController.navigate(NavRoutes.PROFILE_INVENTORY) }) {
                    Icon(
                        imageVector = Icons.Default.ShoppingBag,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (isDark) MaterialTheme.colorScheme.primary else Blue600
                    )
                    Spacer(Modifier.size(6.dp))
                    Text("My items", color = if (isDark) MaterialTheme.colorScheme.primary else Blue600)
                }
            }
        }

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        when {
            uiState.isLoading && uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.items.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Redeem,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "No items available",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isLoading,
                    onRefresh = { viewModel.loadStore() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.items, key = { it.id }) { item ->
                            StoreItemCard(
                                item = item,
                                onRedeemClick = { itemToRedeem = item },
                                isRedeemLoading = uiState.redeemLoadingId == item.id
                            )
                        }
                    }
                }
            }
        }
    }

    itemToRedeem?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToRedeem = null },
            title = { Text("Redeem item") },
            text = {
                Text(
                    "Redeem \"${item.name}\" for ${item.costPoints} points?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.redeem(item)
                        itemToRedeem = null
                    }
                ) {
                    Text("Redeem", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToRedeem = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }
}

@Composable
private fun StoreItemCard(
    item: StoreItem,
    onRedeemClick: () -> Unit,
    isRedeemLoading: Boolean
) {
    val canRedeem = item.isAvailable && item.canAfford && (item.stock == null || item.stock > 0)
    val isDark = isSystemInDarkTheme()
    val iconBg = if (isDark) MaterialTheme.colorScheme.primaryContainer else Blue50
    val iconTint = if (isDark) MaterialTheme.colorScheme.onPrimaryContainer else Blue600
    val pointsColor = if (isDark) MaterialTheme.colorScheme.primary else Blue600
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(iconBg, shape = RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redeem,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.size(14.dp))
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${item.costPoints} pts",
                    style = MaterialTheme.typography.titleMedium,
                    color = pointsColor
                )
            }
            item.description?.let { desc ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Start date: show when item becomes available (if set)
            item.startDate?.let { start ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Available from ${formatStoreDate(start)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // End date: show when item is available until (if set)
            item.endDate?.let { end ->
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Available until ${formatStoreDate(end)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.stock != null) {
                Spacer(Modifier.height(10.dp))
                val stock = item.stock
                val (chipBg, chipText) = when {
                    stock == 0 -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
                    stock <= 5 -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) to MaterialTheme.colorScheme.onSurfaceVariant
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(chipBg, shape = RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (stock == 0) "Out of stock" else "In stock: $stock",
                        style = MaterialTheme.typography.bodyMedium,
                        color = chipText
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = onRedeemClick,
                enabled = canRedeem && !isRedeemLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isRedeemLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (canRedeem) "Redeem" else "Unavailable")
                }
            }
        }
    }
}

/** Format API date (e.g. "2026-06-30 23:59:59" or "2026-06-30") to "Jun 30, 2026". */
private fun formatStoreDate(apiDate: String): String {
    return try {
        val s = apiDate.trim().substringBefore(".")
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        when {
            s.length >= 19 -> {
                val normalized = s.take(19).replace(" ", "T")
                val dt = LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                dt.format(formatter)
            }
            s.length >= 10 -> {
                val d = LocalDate.parse(s.take(10), DateTimeFormatter.ISO_LOCAL_DATE)
                d.format(formatter)
            }
            else -> apiDate.take(10)
        }
    } catch (_: Exception) {
        apiDate.take(10)
    }
}
