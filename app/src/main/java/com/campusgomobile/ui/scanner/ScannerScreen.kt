package com.campusgomobile.ui.scanner

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.activity.ComponentActivity
import com.campusgomobile.ar.ArActivity
import com.campusgomobile.data.model.QrResolveResponse
import com.campusgomobile.navigation.NavRoutes
import com.campusgomobile.scanner.QrDecoder
import com.campusgomobile.ui.theme.Emerald600
import com.campusgomobile.util.QuestTimeUtils
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import java.util.concurrent.atomic.AtomicReference

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel,
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (cameraPermissionState.status.isGranted) {
            // Always show camera; only trigger scan when idle (scanning)
            CameraPreviewWithQrScanning(
                canAcceptScan = uiState.scanning,
                onQrDetected = { viewModel.onQrScanned(it) }
            )
            if (uiState.scanning) {
                ScannerOverlay()
            }
            when {
                uiState.joinResult != null -> JoinedOverlay(
                    resolveResult = uiState.resolveResult,
                    onPlay = { navController.navigate(NavRoutes.play(uiState.joinResult!!.participantId)) },
                    onViewInAr = { (context as? ComponentActivity)?.let { act ->
                        val r = uiState.resolveResult
                        ArActivity.launch(act,
                            questTitle = r?.questTitle,
                            stageNumber = r?.stageNumber ?: 0,
                            locationHint = r?.locationHint,
                            questId = r?.questId ?: 0,
                            stageId = r?.stageId ?: 0,
                            participantId = uiState.joinResult!!.participantId,
                            questionType = r?.questionType
                        )
                    } },
                    onScanAgain = { viewModel.resetScanner() }
                )
                uiState.resolving -> ResolvingOverlay()
                uiState.resolveResult != null -> {
                    val r = uiState.resolveResult!!
                    val isUpcoming = QuestTimeUtils.isStageStartInFuture(r.stageStart)
                    val reasonSaysNotAvailable = QuestTimeUtils.reasonSuggestsUpcomingOrNotAvailable(r.reason)
                    val hasParticipantId = r.participantId != null && r.participantId != 0
                    val cannotJoinStage1 = r.stageNumber == 1 && (!r.canJoin || isUpcoming || reasonSaysNotAvailable)
                    val cannotPlayLater = r.stageNumber > 1 && !r.canPlay && !hasParticipantId
                    val rejected = cannotJoinStage1 || cannotPlayLater
                    val rejectMsg = when {
                        !rejected -> null
                        !r.reason.isNullOrBlank() -> r.reason
                        isUpcoming && !r.stageStart.isNullOrBlank() -> "Quest starts at ${r.stageStart}"
                        isUpcoming -> "This quest hasn't started yet."
                        !r.canJoin && r.stageNumber == 1 -> "This quest is full. No more participants can join."
                        !r.canPlay && r.stageNumber > 1 -> "You are not a participant in this quest. Join by scanning the first stage QR."
                        else -> "You cannot enter this quest at this time."
                    }
                    if (rejected) {
                        LaunchedEffect(r) {
                            (context as? ComponentActivity)?.let { act ->
                                ArActivity.launch(act,
                                    questTitle = r.questTitle,
                                    stageNumber = r.stageNumber,
                                    locationHint = r.locationHint,
                                    questId = r.questId,
                                    stageId = r.stageId,
                                    rejectReason = rejectMsg
                                )
                            }
                            viewModel.resetScanner()
                        }
                    } else {
                        ResolveResultOverlay(
                            result = r,
                            joining = uiState.joining,
                            joinError = uiState.joinError,
                            onJoin = { viewModel.joinQuest(r.questId, r.stageId) },
                            onPlay = { r.participantId?.let { id -> navController.navigate(NavRoutes.play(id)) } },
                            onViewInAr = { (context as? ComponentActivity)?.let { act ->
                                val allowJoin = r.stageNumber == 1 && !hasParticipantId && !isUpcoming && !reasonSaysNotAvailable
                                ArActivity.launch(act,
                                    questTitle = r.questTitle,
                                    stageNumber = r.stageNumber,
                                    locationHint = r.locationHint,
                                    questId = r.questId,
                                    stageId = r.stageId,
                                    participantId = r.participantId ?: 0,
                                    showJoinOnCard = allowJoin,
                                    stageStart = r.stageStart,
                                    questionType = r.questionType
                                )
                            } },
                            onScanAgain = { viewModel.resetScanner() }
                        )
                    }
                }
                uiState.resolveError != null -> ErrorOverlay(
                    message = uiState.resolveError!!,
                    onScanAgain = { viewModel.resetScanner() }
                )
            }
        } else {
            CameraPermissionRequest(
                shouldShowRationale = cameraPermissionState.status.shouldShowRationale,
                onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
            )
        }
    }
}

@Composable
private fun CameraPreviewWithQrScanning(
    canAcceptScan: Boolean,
    onQrDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraController = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_ANALYSIS)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    val lastScanned = remember { AtomicReference<String?>(null) }
    val canAcceptRef = remember { AtomicReference(true) }
    canAcceptRef.set(canAcceptScan)

    DisposableEffect(lifecycleOwner) {
        val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
        cameraController.setImageAnalysisAnalyzer(executor) { imageProxy ->
            @Suppress("UnsafeOptInUsageError")
            try {
                val raw = QrDecoder.decodeFromImageProxy(imageProxy)
                if (canAcceptRef.get() && !raw.isNullOrBlank() && raw != lastScanned.getAndSet(raw)) {
                    onQrDetected(raw)
                }
            } finally {
                imageProxy.close()
            }
        }
        cameraController.bindToLifecycle(lifecycleOwner)
        onDispose {
            cameraController.unbind()
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                controller = cameraController
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ScannerOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.5f))
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(250.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                )
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .border(
                            width = 3.dp,
                            color = Emerald600,
                            shape = RoundedCornerShape(16.dp)
                        )
                )
                Spacer(
                    modifier = Modifier
                        .weight(1f)
                        .height(250.dp)
                        .background(Color.Black.copy(alpha = 0.5f))
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(Color.Black.copy(alpha = 0.5f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(Modifier.height(24.dp))
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Scan quest QR (quest ID + stage ID)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun ResolvingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Emerald600)
            Spacer(Modifier.height(16.dp))
            Text("Resolving…", color = Color.White, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun CannotJoinOverlay(
    questTitle: String,
    reason: String?,
    startsAt: String? = null,
    onScanAgain: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    questTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "You cannot join this quest.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    reason?.takeIf { it.isNotBlank() }
                        ?: "You are not eligible to join at this time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!startsAt.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Quest starts at $startsAt",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) {
                    Text("Scan again")
                }
            }
        }
    }
}

@Composable
private fun ResolveResultOverlay(
    result: QrResolveResponse,
    joining: Boolean = false,
    joinError: String? = null,
    onJoin: () -> Unit,
    onPlay: () -> Unit,
    onViewInAr: () -> Unit,
    onScanAgain: () -> Unit
) {
    val hasParticipantId = result.participantId != null && result.participantId != 0
    val isUpcoming = QuestTimeUtils.isStageStartInFuture(result.stageStart)
    val reasonSaysNotAvailable = QuestTimeUtils.reasonSuggestsUpcomingOrNotAvailable(result.reason)
    val showJoinButton = result.canJoin && !hasParticipantId && !isUpcoming && !reasonSaysNotAvailable
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    result.questTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Stage ${result.stageNumber}${result.locationHint?.let { " · $it" } ?: ""}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!result.reason.isNullOrBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        result.reason!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(16.dp))
                if (showJoinButton) {
                    Button(
                        onClick = onJoin,
                        enabled = !joining,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (joining) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Join quest")
                    }
                    joinError?.let { err ->
                        Spacer(Modifier.height(4.dp))
                        Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
                if (result.canPlay && hasParticipantId) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onPlay,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Go to Play")
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onViewInAr, modifier = Modifier.fillMaxWidth()) {
                    Text("View in AR")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onScanAgain, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors()) {
                    Text("Scan again")
                }
            }
        }
    }
}

@Composable
private fun JoinedOverlay(
    resolveResult: QrResolveResponse?,
    onPlay: () -> Unit,
    onViewInAr: () -> Unit,
    onScanAgain: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("You joined!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
                    Text("Go to Play")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onViewInAr, modifier = Modifier.fillMaxWidth()) {
                    Text("View in AR")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onScanAgain, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors()) {
                    Text("Scan again")
                }
            }
        }
    }
}

@Composable
private fun ErrorOverlay(
    message: String,
    onScanAgain: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text("Couldn't use this QR", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) {
                    Text("Scan again")
                }
            }
        }
    }
}

@Composable
private fun CameraPermissionRequest(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Camera permission is needed to scan QR codes",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (shouldShowRationale) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = "The camera is used only for scanning quest QR codes.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant camera permission")
        }
    }
}
