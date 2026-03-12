package com.campusgomobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.campusgomobile.BuildConfig
import com.campusgomobile.data.auth.AuthRepository
import com.campusgomobile.data.auth.TokenStorage
import com.campusgomobile.data.network.NetworkModule
import com.campusgomobile.navigation.NavRoutes
import com.campusgomobile.ui.auth.AuthViewModel
import com.campusgomobile.ui.auth.SignInScreen
import com.campusgomobile.ui.auth.SignUpScreen
import com.campusgomobile.ui.home.HomeScreen
import com.campusgomobile.ui.splash.SplashScreen
import com.campusgomobile.ui.theme.CampusGoMobileTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                val authViewModel: AuthViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            @Suppress("UNCHECKED_CAST")
                            return AuthViewModel(authRepository) as T
                        }
                    }
                )
                val navController = rememberNavController()
                val uiState by authViewModel.uiState.collectAsState()

                LaunchedEffect(uiState.isLoggedIn) {
                    val current = navController.currentBackStackEntry?.destination?.route
                    when {
                        uiState.isLoggedIn && current != NavRoutes.HOME ->
                            navController.navigate(NavRoutes.HOME) {
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
                    composable(NavRoutes.HOME) {
                        HomeScreen(viewModel = authViewModel)
                    }
                }
            }
        }
    }
}
