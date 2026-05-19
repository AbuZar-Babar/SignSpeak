package com.example.kotlinfrontend

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner
import com.example.kotlinfrontend.ui.theme.KotlinFrontendTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.roundToInt

private const val COLLECTION_FORMAT = "signspeak-landmarks-v1"
private const val SEQUENCE_LENGTH = 60
private const val FRAME_DIM = 126
private const val TARGET_FPS = 20
private const val ANALYSIS_WIDTH = 640
private const val ANALYSIS_HEIGHT = 480
private const val MIN_SIGN_GAP_SECONDS = 0.5f
private const val MAX_SIGN_GAP_SECONDS = 5.0f

private val HAND_CONNECTIONS: List<Pair<Int, Int>> = listOf(
    0 to 1, 1 to 2, 2 to 3, 3 to 4,
    0 to 5, 5 to 6, 6 to 7, 7 to 8,
    5 to 9, 9 to 10, 10 to 11, 11 to 12,
    9 to 13, 13 to 14, 14 to 15, 15 to 16,
    13 to 17, 17 to 18, 18 to 19, 19 to 20,
    0 to 17
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KotlinFrontendTheme {
                CollectorApp()
            }
        }
    }
}

private enum class CapturePhase {
    Idle,
    Countdown,
    Recording,
    Captured,
    Saving
}

@Composable
private fun CollectorApp() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    val captureGate = remember { AtomicBoolean(false) }
    val lastAcceptedFrameNs = remember { AtomicLong(0L) }
    val frameBuffer = remember { mutableListOf<FloatArray>() }
    val frameBufferLock = remember { Any() }
    val statsStore = remember { CollectorStatsStore(context.applicationContext) }
    val writer = remember { JsonWriter(context.applicationContext) }
    val coroutineScope = rememberCoroutineScope()
    val builtInActions = remember { loadCollectorActions(context) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var suggestedActions by remember {
        mutableStateOf(mergeCollectorActions(builtInActions, statsStore.loadCustomActions()))
    }
    var actionText by remember { mutableStateOf(suggestedActions.firstOrNull().orEmpty()) }
    var useFrontCamera by remember { mutableStateOf(true) }
    var autoContinue by remember { mutableStateOf(statsStore.loadAutoContinue()) }
    var signGapSeconds by remember { mutableStateOf(statsStore.loadSignGapSeconds()) }
    var phase by remember { mutableStateOf(CapturePhase.Idle) }
    var countdown by remember { mutableIntStateOf(0) }
    var recordedFrames by remember { mutableIntStateOf(0) }
    var detectedFrames by remember { mutableIntStateOf(0) }
    var capturedFrames by remember { mutableStateOf<List<FloatArray>>(emptyList()) }
    var overlayHands by remember { mutableStateOf<List<HandWireframe>>(emptyList()) }
    var statusText by remember { mutableStateOf("Grant camera permission, choose a sign, then record.") }
    var lastExportPath by remember { mutableStateOf<String?>(null) }
    var savedCounts by remember { mutableStateOf(statsStore.loadAll()) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        statusText = if (granted) {
            "Camera ready. Choose a sign and record."
        } else {
            "Camera permission is required for collection."
        }
    }
    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        statusText = if (granted) {
            "Storage permission granted. Tap Save Sample again."
        } else {
            "Storage permission is required on this Android version."
        }
    }

    val selectedAction = normalizeActionName(actionText)
    val selectedActionCount = if (selectedAction.isBlank()) {
        0
    } else {
        savedCounts[selectedAction] ?: 0
    }

    fun persistAction(action: String) {
        if (action.isBlank()) {
            return
        }
        statsStore.addCustomAction(action)
        suggestedActions = mergeCollectorActions(builtInActions, statsStore.loadCustomActions())
    }

    fun addCurrentAction() {
        val action = normalizeActionName(actionText)
        if (action.isBlank()) {
            statusText = "Enter a word before adding it."
            return
        }
        actionText = action
        persistAction(action)
        statusText = "Added $action to this phone."
    }

    fun resetCaptureState(message: String) {
        captureGate.set(false)
        lastAcceptedFrameNs.set(0L)
        synchronized(frameBufferLock) {
            frameBuffer.clear()
        }
        recordedFrames = 0
        detectedFrames = 0
        overlayHands = emptyList()
        capturedFrames = emptyList()
        countdown = 0
        phase = CapturePhase.Idle
        statusText = message
    }

    fun startCapture() {
        val action = normalizeActionName(actionText)
        if (action.isBlank()) {
            statusText = "Enter an action name before recording."
            return
        }
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            return
        }
        persistAction(action)

        countdownJob?.cancel()
        captureGate.set(false)
        lastAcceptedFrameNs.set(0L)
        synchronized(frameBufferLock) {
            frameBuffer.clear()
        }
        recordedFrames = 0
        detectedFrames = 0
        capturedFrames = emptyList()
        overlayHands = emptyList()
        lastExportPath = null

        countdownJob = coroutineScope.launch {
            val delayMillis = (signGapSeconds * 1_000f).roundToInt().toLong()
            phase = CapturePhase.Countdown
            statusText = "Get ready for $action. Starting in ${"%.1f".format(Locale.US, signGapSeconds)}s."
            val halfSecondTicks = (delayMillis / 500L).toInt()
            for (tick in halfSecondTicks downTo 1) {
                val remainingMillis = tick * 500L
                countdown = ((remainingMillis + 999L) / 1_000L).toInt()
                delay(500L)
            }
            countdown = 0
            phase = CapturePhase.Recording
            statusText = "Recording $SEQUENCE_LENGTH frames at $TARGET_FPS FPS."
            captureGate.set(true)
        }
    }

    fun stopCapture() {
        countdownJob?.cancel()
        resetCaptureState("Recording stopped. Ready for another sample.")
    }

    fun saveFrames(
        action: String,
        frames: List<FloatArray>,
        continueAfterSave: Boolean
    ) {
        if (action.isBlank()) {
            statusText = "Enter an action name before saving."
            return
        }
        if (frames.size != SEQUENCE_LENGTH) {
            statusText = "No complete sample is ready to save."
            return
        }
        if (needsLegacyStoragePermission(context)) {
            capturedFrames = frames
            phase = CapturePhase.Captured
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            return
        }

        persistAction(action)
        phase = CapturePhase.Saving
        statusText = "Saving $action sample..."
        coroutineScope.launch {
            val result = withContext(Dispatchers.IO) {
                writer.writeToDownloads(
                    action = action,
                    frameVectors = frames,
                    sequenceLength = SEQUENCE_LENGTH,
                    dim = FRAME_DIM
                )
            }
            val newCount = statsStore.increment(action)
            savedCounts = savedCounts + (action to newCount)
            lastExportPath = result.displayPath
            capturedFrames = emptyList()
            recordedFrames = 0
            detectedFrames = 0
            overlayHands = emptyList()
            phase = CapturePhase.Idle
            statusText = if (continueAfterSave && autoContinue) {
                "Saved ${result.fileName}. Continuing..."
            } else {
                "Saved ${result.fileName}."
            }

            if (continueAfterSave && autoContinue && normalizeActionName(actionText) == action) {
                delay(650L)
                startCapture()
            }
        }
    }

    fun saveCapturedSample() {
        saveFrames(
            action = normalizeActionName(actionText),
            frames = capturedFrames,
            continueAfterSave = false
        )
    }

    DisposableEffect(hasCameraPermission, useFrontCamera) {
        if (!hasCameraPermission) {
            return@DisposableEffect onDispose { }
        }

        val disposeCamera = bindCollectorCamera(
            context = context.applicationContext,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            analysisExecutor = analysisExecutor,
            mainExecutor = mainExecutor,
            useFrontCamera = useFrontCamera,
            captureGate = captureGate,
            lastAcceptedFrameNs = lastAcceptedFrameNs,
            frameBuffer = frameBuffer,
            frameBufferLock = frameBufferLock,
            onCameraReady = {
                statusText = "Camera ready. Choose a sign and record."
            },
            onFrame = { frameCount, hasHand, hands, snapshot ->
                recordedFrames = frameCount
                if (hasHand) {
                    detectedFrames += 1
                }
                overlayHands = hands
                if (snapshot != null) {
                    val action = normalizeActionName(actionText)
                    if (autoContinue) {
                        saveFrames(
                            action = action,
                            frames = snapshot,
                            continueAfterSave = true
                        )
                    } else {
                        capturedFrames = snapshot
                        phase = CapturePhase.Captured
                        statusText = "Captured $SEQUENCE_LENGTH frames. Save or retake."
                    }
                }
            },
            onError = { message ->
                captureGate.set(false)
                phase = CapturePhase.Idle
                statusText = message
            }
        )

        onDispose {
            captureGate.set(false)
            disposeCamera()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            countdownJob?.cancel()
            captureGate.set(false)
            analysisExecutor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    CollectorScreen(
        hasCameraPermission = hasCameraPermission,
        actionText = actionText,
        selectedAction = selectedAction,
        suggestedActions = suggestedActions,
        selectedActionCount = selectedActionCount,
        phase = phase,
        countdown = countdown,
        recordedFrames = recordedFrames,
        detectedFrames = detectedFrames,
        lastExportPath = lastExportPath,
        statusText = statusText,
        useFrontCamera = useFrontCamera,
        autoContinue = autoContinue,
        signGapSeconds = signGapSeconds,
        previewView = previewView,
        overlayHands = overlayHands,
        onActionTextChange = { actionText = it },
        onActionSelected = { actionText = it },
        onAddAction = ::addCurrentAction,
        onAutoContinueChange = { enabled ->
            autoContinue = enabled
            statsStore.saveAutoContinue(enabled)
            statusText = if (enabled) {
                "Auto continue enabled."
            } else {
                "Auto continue disabled."
            }
        },
        onSignGapChange = { value ->
            signGapSeconds = value
            statsStore.saveSignGapSeconds(value)
        },
        onRequestCameraPermission = {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        },
        onToggleCamera = {
            useFrontCamera = !useFrontCamera
            resetCaptureState("Camera switched. Ready for another sample.")
        },
        onStartCapture = ::startCapture,
        onStopCapture = ::stopCapture,
        onRetake = ::startCapture,
        onSave = ::saveCapturedSample
    )
}

@Composable
private fun CollectorScreen(
    hasCameraPermission: Boolean,
    actionText: String,
    selectedAction: String,
    suggestedActions: List<String>,
    selectedActionCount: Int,
    phase: CapturePhase,
    countdown: Int,
    recordedFrames: Int,
    detectedFrames: Int,
    lastExportPath: String?,
    statusText: String,
    useFrontCamera: Boolean,
    autoContinue: Boolean,
    signGapSeconds: Float,
    previewView: PreviewView,
    overlayHands: List<HandWireframe>,
    onActionTextChange: (String) -> Unit,
    onActionSelected: (String) -> Unit,
    onAddAction: () -> Unit,
    onAutoContinueChange: (Boolean) -> Unit,
    onSignGapChange: (Float) -> Unit,
    onRequestCameraPermission: () -> Unit,
    onToggleCamera: () -> Unit,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onRetake: () -> Unit,
    onSave: () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5FAF1),
                        Color(0xFFE8F1E7),
                        Color(0xFFF8F4E3)
                    )
                )
            )
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderSection()

            CameraPanel(
                hasCameraPermission = hasCameraPermission,
                previewView = previewView,
                overlayHands = overlayHands,
                mirrorOverlayHorizontally = useFrontCamera,
                phase = phase,
                countdown = countdown,
                recordedFrames = recordedFrames,
                onRequestCameraPermission = onRequestCameraPermission
            )

            ControlPanel(
                actionText = actionText,
                selectedAction = selectedAction,
                suggestedActions = suggestedActions,
                selectedActionCount = selectedActionCount,
                phase = phase,
                recordedFrames = recordedFrames,
                detectedFrames = detectedFrames,
                statusText = statusText,
                lastExportPath = lastExportPath,
                useFrontCamera = useFrontCamera,
                autoContinue = autoContinue,
                signGapSeconds = signGapSeconds,
                onActionTextChange = onActionTextChange,
                onActionSelected = onActionSelected,
                onAddAction = onAddAction,
                onAutoContinueChange = onAutoContinueChange,
                onSignGapChange = onSignGapChange,
                onToggleCamera = onToggleCamera,
                onStartCapture = onStartCapture,
                onStopCapture = onStopCapture,
                onRetake = onRetake,
                onSave = onSave
            )
        }
    }
}

@Composable
private fun HeaderSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "SignSpeak Collector",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF203017)
        )
        Text(
            text = "$COLLECTION_FORMAT - mobile landmark samples",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF4F5E47)
        )
    }
}

@Composable
private fun CameraPanel(
    hasCameraPermission: Boolean,
    previewView: PreviewView,
    overlayHands: List<HandWireframe>,
    mirrorOverlayHorizontally: Boolean,
    phase: CapturePhase,
    countdown: Int,
    recordedFrames: Int,
    onRequestCameraPermission: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp)
            .height(420.dp),
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 2.dp,
        shadowElevation = 6.dp,
        color = Color(0xFF172012)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCameraPermission) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
                HandOverlay(
                    hands = overlayHands,
                    mirrorHorizontally = mirrorOverlayHorizontally,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = null,
                        tint = Color(0xFFFFD84D),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera permission required",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onRequestCameraPermission) {
                        Text("Grant Camera")
                    }
                }
            }

            if (phase == CapturePhase.Countdown) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.42f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countdown.toString(),
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            AssistChip(
                onClick = { },
                label = {
                    Text(
                        text = when (phase) {
                            CapturePhase.Recording -> "$recordedFrames / $SEQUENCE_LENGTH"
                            CapturePhase.Captured -> "Sample ready"
                            CapturePhase.Saving -> "Saving"
                            CapturePhase.Countdown -> "Starting"
                            CapturePhase.Idle -> "Ready"
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
            )
        }
    }
}

@Composable
private fun ControlPanel(
    actionText: String,
    selectedAction: String,
    suggestedActions: List<String>,
    selectedActionCount: Int,
    phase: CapturePhase,
    recordedFrames: Int,
    detectedFrames: Int,
    statusText: String,
    lastExportPath: String?,
    useFrontCamera: Boolean,
    autoContinue: Boolean,
    signGapSeconds: Float,
    onActionTextChange: (String) -> Unit,
    onActionSelected: (String) -> Unit,
    onAddAction: () -> Unit,
    onAutoContinueChange: (Boolean) -> Unit,
    onSignGapChange: (Float) -> Unit,
    onToggleCamera: () -> Unit,
    onStartCapture: () -> Unit,
    onStopCapture: () -> Unit,
    onRetake: () -> Unit,
    onSave: () -> Unit
) {
    var showWordList by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 720.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.94f),
        tonalElevation = 1.dp,
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = actionText,
                onValueChange = onActionTextChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Action") },
                supportingText = {
                    val nextSequence = selectedActionCount + 1
                    Text("Saved on this phone: $selectedActionCount - next phone sample: $nextSequence")
                },
                enabled = phase == CapturePhase.Idle
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onAddAction,
                    enabled = phase == CapturePhase.Idle && selectedAction.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Add Word")
                }
                Row(
                    modifier = Modifier.weight(1.2f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Auto continue",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFF33402C)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Switch(
                        checked = autoContinue,
                        onCheckedChange = onAutoContinueChange,
                        enabled = phase == CapturePhase.Idle || phase == CapturePhase.Captured
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Gap between signs: ${"%.1f".format(Locale.US, signGapSeconds)}s",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF33402C)
                )
                Slider(
                    value = signGapSeconds,
                    onValueChange = { raw ->
                        val snapped = (raw * 2f).roundToInt() / 2f
                        onSignGapChange(snapped.coerceIn(MIN_SIGN_GAP_SECONDS, MAX_SIGN_GAP_SECONDS))
                    },
                    valueRange = MIN_SIGN_GAP_SECONDS..MAX_SIGN_GAP_SECONDS,
                    steps = 8,
                    enabled = phase == CapturePhase.Idle || phase == CapturePhase.Captured
                )
            }

            if (suggestedActions.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF4F6ED)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Words",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF33402C),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${suggestedActions.size} saved words",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF66715D)
                                )
                            }
                            IconButton(
                                onClick = { showWordList = !showWordList },
                                enabled = phase == CapturePhase.Idle
                            ) {
                                Icon(
                                    imageVector = if (showWordList) {
                                        Icons.Rounded.ExpandLess
                                    } else {
                                        Icons.Rounded.ExpandMore
                                    },
                                    contentDescription = if (showWordList) {
                                        "Collapse words"
                                    } else {
                                        "Show words"
                                    }
                                )
                            }
                        }

                        if (showWordList) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 132.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                contentPadding = PaddingValues(bottom = 4.dp)
                            ) {
                                items(suggestedActions) { action ->
                                    FilterChip(
                                        selected = selectedAction == action,
                                        onClick = {
                                            onActionSelected(action)
                                            showWordList = false
                                        },
                                        label = {
                                            Text(
                                                text = action.replace("_", " "),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        },
                                        enabled = phase == CapturePhase.Idle
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LinearProgressIndicator(
                progress = { (recordedFrames.toFloat() / SEQUENCE_LENGTH).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF2B6C00),
                trackColor = Color(0xFFE5E7D8)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricTile(
                    label = "Frames",
                    value = "$recordedFrames/$SEQUENCE_LENGTH",
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Detected",
                    value = "$detectedFrames",
                    modifier = Modifier.weight(1f)
                )
                MetricTile(
                    label = "Camera",
                    value = if (useFrontCamera) "Front" else "Back",
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF33402C)
            )

            lastExportPath?.let { path ->
                Text(
                    text = path,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF5F4E00)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleCamera,
                    enabled = phase == CapturePhase.Idle,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Cameraswitch, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Camera")
                }

                when (phase) {
                    CapturePhase.Recording, CapturePhase.Countdown -> {
                        Button(
                            onClick = onStopCapture,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBA1A1A)),
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Icon(Icons.Rounded.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Stop")
                        }
                    }
                    CapturePhase.Captured -> {
                        OutlinedButton(
                            onClick = onRetake,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Replay, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Retake")
                        }
                        Button(
                            onClick = onSave,
                            modifier = Modifier.weight(1.25f)
                        ) {
                            Icon(Icons.Rounded.Save, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Save Sample")
                        }
                    }
                    CapturePhase.Saving -> {
                        Button(
                            onClick = { },
                            enabled = false,
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Text("Saving")
                        }
                    }
                    CapturePhase.Idle -> {
                        Button(
                            onClick = onStartCapture,
                            enabled = selectedAction.isNotBlank(),
                            modifier = Modifier.weight(1.4f)
                        ) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Record Sample")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF4F6ED)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF66715D)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF203017),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HandOverlay(
    hands: List<HandWireframe>,
    mirrorHorizontally: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        hands.forEach { hand ->
            val points = hand.points
            val color = when (hand.side) {
                HandSide.LEFT -> Color(0xFF00C2A8)
                HandSide.RIGHT -> Color(0xFFFFD84D)
                HandSide.UNKNOWN -> Color.White
            }
            HAND_CONNECTIONS.forEach { (start, end) ->
                val startPoint = points.getOrNull(start)
                val endPoint = points.getOrNull(end)
                if (startPoint != null && endPoint != null) {
                    val startX = if (mirrorHorizontally) 1f - startPoint.x else startPoint.x
                    val endX = if (mirrorHorizontally) 1f - endPoint.x else endPoint.x
                    drawLine(
                        color = color,
                        start = Offset(startX * size.width, startPoint.y * size.height),
                        end = Offset(endX * size.width, endPoint.y * size.height),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                }
            }
            points.forEach { point ->
                val x = if (mirrorHorizontally) 1f - point.x else point.x
                drawCircle(
                    color = color,
                    radius = 5f,
                    center = Offset(x * size.width, point.y * size.height)
                )
            }
        }
    }
}

@Suppress("DEPRECATION")
private fun bindCollectorCamera(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    analysisExecutor: ExecutorService,
    mainExecutor: java.util.concurrent.Executor,
    useFrontCamera: Boolean,
    captureGate: AtomicBoolean,
    lastAcceptedFrameNs: AtomicLong,
    frameBuffer: MutableList<FloatArray>,
    frameBufferLock: Any,
    onCameraReady: () -> Unit,
    onFrame: (frameCount: Int, hasHand: Boolean, hands: List<HandWireframe>, snapshot: List<FloatArray>?) -> Unit,
    onError: (String) -> Unit
): () -> Unit {
    val disposed = AtomicBoolean(false)
    var provider: ProcessCameraProvider? = null
    var extractor: HandExtractor? = null
    val providerFuture = ProcessCameraProvider.getInstance(context)

    providerFuture.addListener(
        {
            if (disposed.get()) {
                return@addListener
            }
            try {
                val cameraProvider = providerFuture.get()
                provider = cameraProvider
                cameraProvider.unbindAll()

                val handExtractor = HandExtractor(context)
                extractor = handExtractor

                val previewUseCase = Preview.Builder().build().also { preview ->
                    preview.surfaceProvider = previewView.surfaceProvider
                }
                val analysisUseCase = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(Size(ANALYSIS_WIDTH, ANALYSIS_HEIGHT))
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()

                analysisUseCase.setAnalyzer(analysisExecutor) { imageProxy ->
                    if (!captureGate.get()) {
                        imageProxy.close()
                        return@setAnalyzer
                    }

                    val timestampNs = imageProxy.imageInfo.timestamp
                    val minFrameIntervalNs = 1_000_000_000L / TARGET_FPS.toLong()
                    val previousTimestampNs = lastAcceptedFrameNs.get()
                    if (previousTimestampNs > 0L &&
                        timestampNs > previousTimestampNs &&
                        timestampNs - previousTimestampNs < minFrameIntervalNs
                    ) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    lastAcceptedFrameNs.set(timestampNs)

                    try {
                        val timestampMs = timestampNs / 1_000_000L
                        val output = handExtractor.extractFromImageProxyWithOverlay(
                            imageProxy = imageProxy,
                            timestampMs = timestampMs,
                            includeOverlay = true
                        )
                        val frameVector = output.vector.copyOf()
                        val hasHand = frameVector.any { abs(it) > 0.000001f }
                        var snapshot: List<FloatArray>? = null
                        val frameCount = synchronized(frameBufferLock) {
                            if (frameBuffer.size < SEQUENCE_LENGTH) {
                                frameBuffer.add(frameVector)
                            }
                            if (frameBuffer.size == SEQUENCE_LENGTH) {
                                captureGate.set(false)
                                snapshot = frameBuffer.map { it.copyOf() }
                            }
                            frameBuffer.size
                        }
                        mainExecutor.execute {
                            onFrame(frameCount, hasHand, output.hands, snapshot)
                        }
                    } catch (error: Exception) {
                        captureGate.set(false)
                        mainExecutor.execute {
                            onError(error.message ?: "Camera analysis failed.")
                        }
                    } finally {
                        imageProxy.close()
                    }
                }

                val cameraSelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    previewUseCase,
                    analysisUseCase
                )
                onCameraReady()
            } catch (error: Exception) {
                extractor?.close()
                extractor = null
                onError(error.message ?: "Failed to start camera.")
            }
        },
        mainExecutor
    )

    return {
        disposed.set(true)
        captureGate.set(false)
        provider?.unbindAll()
        extractor?.close()
    }
}

private fun normalizeActionName(raw: String): String {
    return raw
        .trim()
        .lowercase(Locale.US)
        .replace(Regex("\\s+"), "_")
        .replace(Regex("[^a-z0-9_-]"), "")
}

private fun loadCollectorActions(context: Context): List<String> {
    return runCatching {
        context.assets.open("collector_actions.txt").bufferedReader().useLines { lines ->
            lines
                .map(::normalizeActionName)
                .filter { it.isNotBlank() }
                .distinct()
                .toList()
        }
    }.getOrDefault(emptyList())
}

private fun mergeCollectorActions(
    builtInActions: List<String>,
    customActions: Set<String>
): List<String> {
    val merged = LinkedHashSet<String>()
    builtInActions.forEach { action ->
        normalizeActionName(action).takeIf { it.isNotBlank() }?.let(merged::add)
    }
    customActions
        .map(::normalizeActionName)
        .filter { it.isNotBlank() }
        .sorted()
        .forEach(merged::add)
    return merged.toList()
}

private fun needsLegacyStoragePermission(context: Context): Boolean {
    return Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
}

private class CollectorStatsStore(context: Context) {
    private val prefs = context.getSharedPreferences("collector_stats", Context.MODE_PRIVATE)

    fun loadAll(): Map<String, Int> {
        return prefs.all.mapNotNull { (key, value) ->
            if (!key.startsWith(COUNT_PREFIX) || value !is Int) {
                null
            } else {
                key.removePrefix(COUNT_PREFIX) to value
            }
        }.toMap()
    }

    fun increment(action: String): Int {
        val key = COUNT_PREFIX + action
        val next = prefs.getInt(key, 0) + 1
        prefs.edit().putInt(key, next).apply()
        return next
    }

    fun loadCustomActions(): Set<String> {
        return prefs.getStringSet(ACTIONS_KEY, emptySet()).orEmpty()
    }

    fun addCustomAction(action: String) {
        val normalized = normalizeActionName(action)
        if (normalized.isBlank()) {
            return
        }
        val updated = loadCustomActions().toMutableSet().apply {
            add(normalized)
        }
        prefs.edit().putStringSet(ACTIONS_KEY, updated).apply()
    }

    fun loadAutoContinue(): Boolean {
        return prefs.getBoolean(AUTO_CONTINUE_KEY, false)
    }

    fun saveAutoContinue(enabled: Boolean) {
        prefs.edit().putBoolean(AUTO_CONTINUE_KEY, enabled).apply()
    }

    fun loadSignGapSeconds(): Float {
        return prefs
            .getFloat(SIGN_GAP_SECONDS_KEY, 3.0f)
            .coerceIn(MIN_SIGN_GAP_SECONDS, MAX_SIGN_GAP_SECONDS)
    }

    fun saveSignGapSeconds(seconds: Float) {
        val safeSeconds = seconds.coerceIn(MIN_SIGN_GAP_SECONDS, MAX_SIGN_GAP_SECONDS)
        prefs.edit().putFloat(SIGN_GAP_SECONDS_KEY, safeSeconds).apply()
    }

    private companion object {
        const val COUNT_PREFIX = "count_"
        const val ACTIONS_KEY = "custom_actions"
        const val AUTO_CONTINUE_KEY = "auto_continue"
        const val SIGN_GAP_SECONDS_KEY = "sign_gap_seconds"
    }
}
