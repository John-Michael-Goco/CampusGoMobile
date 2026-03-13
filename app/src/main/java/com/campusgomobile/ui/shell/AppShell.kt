package com.campusgomobile.ui.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.campusgomobile.navigation.NavRoutes
import com.campusgomobile.ui.auth.AuthViewModel
import com.campusgomobile.ui.home.HomeScreen
import com.campusgomobile.ui.profile.AchievementsScreen
import com.campusgomobile.ui.profile.ActivityLogScreen
import com.campusgomobile.ui.profile.EditProfileScreen
import com.campusgomobile.ui.profile.InventoryHistoryScreen
import com.campusgomobile.ui.profile.InventoryScreen
import com.campusgomobile.ui.profile.LeaderboardScreen
import com.campusgomobile.ui.profile.ProfileScreen
import com.campusgomobile.ui.profile.TransactionHistoryScreen
import com.campusgomobile.ui.quests.QuestsScreen
import com.campusgomobile.ui.scanner.ScannerScreen
import com.campusgomobile.ui.store.StoreScreen
import com.campusgomobile.ui.store.StoreViewModel
import com.campusgomobile.ui.profile.InventoryViewModel

private val ScannerFabSize = 56.dp
private val BottomBarHeight = 80.dp
/** How far the QR FAB sits above the visible bar (extra space so it doesn't clip). */
private val FabRaiseAboveBar = 28.dp

@Composable
fun AppShell(
    viewModel: AuthViewModel,
    storeViewModel: StoreViewModel,
    inventoryViewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val leftTabs = listOf(
        TabItem(NavRoutes.TAB_HOME, "Home", Icons.Default.SportsEsports),
        TabItem(NavRoutes.TAB_QUESTS, "Quests", Icons.Default.Flag)
    )
    val rightTabs = listOf(
        TabItem(NavRoutes.TAB_STORE, "Store", Icons.Default.Redeem),
        TabItem(NavRoutes.TAB_PROFILE, "Profile", Icons.Default.AccountCircle)
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            // Taller slot so the raised FAB has room; visible bar stays BottomBarHeight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BottomBarHeight + FabRaiseAboveBar)
            ) {
                // Visible bar (shell) at the bottom only
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BottomBarHeight)
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {}
                // Row of tabs + FAB; FAB is offset up so it sits above the bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BottomBarHeight)
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    leftTabs.forEach { tab ->
                        NavBarItem(
                            modifier = Modifier.weight(1f),
                            tab = tab,
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                    // Big circle in the middle for QR scan — sits above the bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(BottomBarHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        FloatingActionButton(
                            onClick = {
                                navController.navigate(NavRoutes.SCANNER) {
                                    launchSingleTop = true
                                }
                            },
                            modifier = Modifier
                                .size(ScannerFabSize)
                                .offset(y = -FabRaiseAboveBar),
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            elevation = FloatingActionButtonDefaults.elevation(
                                defaultElevation = 6.dp,
                                pressedElevation = 8.dp,
                                hoveredElevation = 8.dp,
                                focusedElevation = 8.dp
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Scan QR code",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    rightTabs.forEach { tab ->
                        NavBarItem(
                            modifier = Modifier.weight(1f),
                            tab = tab,
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = NavRoutes.TAB_HOME,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(NavRoutes.TAB_HOME) {
                HomeScreen(viewModel = viewModel)
            }
            composable(NavRoutes.TAB_QUESTS) {
                QuestsScreen()
            }
            composable(NavRoutes.TAB_STORE) {
                StoreScreen(
                    viewModel = storeViewModel,
                    navController = navController,
                    isVisible = currentRoute == NavRoutes.TAB_STORE
                )
            }
            composable(NavRoutes.TAB_PROFILE) {
                ProfileScreen(
                    viewModel = viewModel,
                    navController = navController,
                    isVisible = currentRoute == NavRoutes.TAB_PROFILE
                )
            }
            composable(NavRoutes.SCANNER) {
                ScannerScreen()
            }
            composable(NavRoutes.PROFILE_EDIT) {
                EditProfileScreen(navController = navController, viewModel = viewModel)
            }
            composable(NavRoutes.PROFILE_TRANSACTIONS) {
                TransactionHistoryScreen(navController = navController, viewModel = viewModel)
            }
            composable(NavRoutes.PROFILE_ACTIVITY) {
                ActivityLogScreen(navController = navController, viewModel = viewModel)
            }
            composable(NavRoutes.PROFILE_LEADERBOARD) {
                LeaderboardScreen(navController = navController, viewModel = viewModel)
            }
            composable(NavRoutes.PROFILE_ACHIEVEMENTS) {
                AchievementsScreen(navController = navController, viewModel = viewModel)
            }
            composable(NavRoutes.PROFILE_INVENTORY) {
                InventoryScreen(
                    viewModel = inventoryViewModel,
                    navController = navController
                )
            }
            composable(NavRoutes.PROFILE_INVENTORY_HISTORY) {
                InventoryHistoryScreen(
                    viewModel = inventoryViewModel,
                    navController = navController
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    tab: TabItem,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .height(BottomBarHeight)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
