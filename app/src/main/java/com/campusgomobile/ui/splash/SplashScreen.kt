package com.campusgomobile.ui.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.campusgomobile.BuildConfig
import com.campusgomobile.R
import com.campusgomobile.data.network.ApiConnectivity
import com.campusgomobile.data.network.NetworkModule
import kotlinx.coroutines.delay

private const val PHASE1_DURATION_MS = 2500L  // CAMPUS GO logo only (2 seconds)
private const val PHASE2_DURATION_MS = 2000L  // + 2 logos at bottom, then navigate

@Composable
fun SplashScreen(
    onNavigateToAuth: () -> Unit,
    apiBaseUrl: String = NetworkModule.baseUrl,
    modifier: Modifier = Modifier
) {
    var phase by remember { mutableStateOf(0) }
    var apiReachable by remember { mutableStateOf<Boolean?>(null) }
    val isDemoMode = BuildConfig.DEBUG

    LaunchedEffect(Unit) {
        delay(PHASE1_DURATION_MS)
        phase = 1
        delay(PHASE2_DURATION_MS)
        onNavigateToAuth()
    }

    LaunchedEffect(apiBaseUrl, isDemoMode) {
        apiReachable = if (isDemoMode) true else ApiConnectivity.checkReachable(apiBaseUrl)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.splash_logo_campus_go),
                contentDescription = "Campus Go",
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .height(350.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(
                visible = phase >= 1,
                enter = scaleIn(initialScale = 0.5f) + fadeIn()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 48.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.splash_logo_hcc),
                        contentDescription = "Holy Cross College",
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Fit
                    )
                    Image(
                        painter = painterResource(R.drawable.splash_logo_scite),
                        contentDescription = "School of Computing, IT and Engineering",
                        modifier = Modifier.size(80.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }

        if (apiReachable != null) {
            Text(
                text = when {
                    isDemoMode -> "Demo mode"
                    apiReachable == true -> "API connected"
                    else -> "API offline"
                },
                style = MaterialTheme.typography.labelSmall,
                color = when {
                    isDemoMode -> MaterialTheme.colorScheme.tertiary
                    apiReachable == true -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.error
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
            )
        }
    }
}
