package com.campusgomobile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.campusgomobile.data.fcm.CampusGoMessagingService
import com.campusgomobile.ui.shell.PendingNotificationNavigation
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.campusgomobile.data.auth.AuthRepository
import com.campusgomobile.data.auth.TokenStorage
import com.campusgomobile.data.fcm.FcmRepository
import com.campusgomobile.data.network.NetworkModule
import com.campusgomobile.data.inventory.InventoryRepository
import com.campusgomobile.data.quests.QuestsRepository
import com.campusgomobile.data.store.StoreRepository
import com.campusgomobile.navigation.NavRoutes
import com.campusgomobile.ui.auth.AuthViewModel
import com.campusgomobile.ui.auth.SignInScreen
import com.campusgomobile.ui.auth.SignUpScreen
import com.campusgomobile.ui.home.HomeViewModel
import com.campusgomobile.ui.profile.InventoryViewModel
import com.campusgomobile.ui.quests.QuestsViewModel
import com.campusgomobile.ui.scanner.ScannerViewModel
import com.campusgomobile.ui.shell.AppShell
import com.campusgomobile.ui.splash.SplashScreen
import com.campusgomobile.ui.store.StoreViewModel
import com.campusgomobile.ui.theme.CampusGoMobileTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        handleFcmIntent(intent)
        enableEdgeToEdge()
        setContent {
            CampusGoMobileTheme {
                val context = LocalContext.current
                val tokenStorage = remember { TokenStorage(context.applicationContext) }
                val authRepository = remember {
                    AuthRepository(
                        NetworkModule.authApi,
                        tokenStorage,
                        useDemoAuth = false  // true = demo (no API); false = real API/database
                    )
                }
                val fcmRepository = remember { FcmRepository(tokenStorage) }
                val authViewModel: AuthViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return AuthViewModel(authRepository, fcmRepository) as T
                        }
                    }
                )
                val storeRepository = remember { StoreRepository(tokenStorage) }
                val storeViewModel: StoreViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return StoreViewModel(storeRepository) as T
                        }
                    }
                )
                val inventoryRepository = remember { InventoryRepository(tokenStorage) }
                val inventoryViewModel: InventoryViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return InventoryViewModel(inventoryRepository) as T
                        }
                    }
                )
                val questsRepository = remember { QuestsRepository(tokenStorage) }
                val questsViewModel: QuestsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return QuestsViewModel(questsRepository) as T
                        }
                    }
                )
                val homeViewModel: HomeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return HomeViewModel(authRepository, questsRepository, inventoryRepository) as T
                        }
                    }
                )
                val scannerViewModel: ScannerViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return ScannerViewModel(questsRepository) as T
                        }
                    }
                )
                val navController = rememberNavController()
                val uiState by authViewModel.uiState.collectAsState()

                LaunchedEffect(uiState.isLoggedIn) {
                    val current = navController.currentBackStackEntry?.destination?.route
                    when {
                        uiState.isLoggedIn && current != NavRoutes.APP ->
                            navController.navigate(NavRoutes.APP) {
                                popUpTo(NavRoutes.SIGN_IN) { inclusive = true }
                            }
                        !uiState.isLoggedIn && current != NavRoutes.SIGN_IN && current != NavRoutes.SIGN_UP && current != NavRoutes.SPLASH ->
                            navController.navigate(NavRoutes.SIGN_IN) {
                                popUpTo(NavRoutes.SPLASH) { inclusive = true }
                            }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = NavRoutes.SPLASH
                ) {
                    composable(NavRoutes.SPLASH) {
                        SplashScreen(
                            onNavigateToAuth = {
                                navController.navigate(NavRoutes.SIGN_IN) {
                                    popUpTo(NavRoutes.SPLASH) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(NavRoutes.SIGN_IN) {
                        SignInScreen(
                            viewModel = authViewModel,
                            onSignUpClick = { navController.navigate(NavRoutes.SIGN_UP) }
                        )
                    }
                    composable(NavRoutes.SIGN_UP) {
                        SignUpScreen(
                            viewModel = authViewModel,
                            onSignInClick = { navController.popBackStack() }
                        )
                    }
                    composable(NavRoutes.APP) {
                        AppShell(
                            viewModel = authViewModel,
                            homeViewModel = homeViewModel,
                            storeViewModel = storeViewModel,
                            inventoryViewModel = inventoryViewModel,
                            questsViewModel = questsViewModel,
                            scannerViewModel = scannerViewModel
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleFcmIntent(intent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CampusGoMessagingService.CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun handleFcmIntent(intent: android.content.Intent?) {
        val type = intent?.getStringExtra(CampusGoMessagingService.EXTRA_FCM_TYPE) ?: return
        val participantId = intent.getStringExtra(CampusGoMessagingService.EXTRA_PARTICIPANT_ID)
        val questId = intent.getStringExtra(CampusGoMessagingService.EXTRA_QUEST_ID)
        PendingNotificationNavigation.set(
            PendingNotificationNavigation.Target(
                type = type,
                participantId = participantId,
                questId = questId
            )
        )
    }
}
