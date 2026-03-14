package com.campusgomobile.ui.store

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.campusgomobile.data.model.StoreItem
import com.campusgomobile.navigation.NavRoutes
import com.campusgomobile.ui.theme.CampusGoBlue
import com.campusgomobile.ui.theme.Indigo500
import com.campusgomobile.ui.theme.Teal500
import com.campusgomobile.ui.theme.Teal600
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionHeader(title = "Store", icon = Icons.Default.Redeem)
                Spacer(Modifier.weight(1f))
                if (navController != null) {
                    TextButton(onClick = { navController.navigate(NavRoutes.PROFILE_INVENTORY) }) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Teal600
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "My items",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Teal600
                        )
                    }
                }
            }

            if (uiState.errorMessage != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(12.dp))

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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Teal500.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Redeem,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = Teal600
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "No items available",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                else -> {
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
                                    text = "Your balance",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "${uiState.pointsBalance} Pts",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    PullToRefreshBox(
                        isRefreshing = uiState.isLoading,
                        onRefresh = { viewModel.loadStore() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            contentPadding = PaddingValues(bottom = 8.dp)
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
    }

    itemToRedeem?.let { item ->
        AlertDialog(
            onDismissRequest = { itemToRedeem = null },
            title = { Text("Redeem item") },
            text = {
                Text(
                    "Redeem \"${item.name}\" for ${item.costPoints} Pts?"
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
private fun StoreItemCard(
    item: StoreItem,
    onRedeemClick: () -> Unit,
    isRedeemLoading: Boolean
) {
    val canRedeem = item.isAvailable && item.canAfford && (item.stock == null || item.stock > 0)
    val iconTint = Teal600
    val pointsColor = Teal600

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                            .clip(RoundedCornerShape(12.dp))
                            .background(iconTint.copy(alpha = 0.12f)),
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
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${item.costPoints} Pts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
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
            item.startDate?.let { start ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Available from ${formatStoreDate(start)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                        .clip(RoundedCornerShape(8.dp))
                        .background(chipBg)
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
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
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
