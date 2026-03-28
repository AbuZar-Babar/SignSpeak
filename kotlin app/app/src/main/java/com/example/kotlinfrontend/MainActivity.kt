package com.example.kotlinfrontend

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.kotlinfrontend.app.SignSpeakApp
import com.example.kotlinfrontend.ui.theme.KotlinFrontendTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI
import java.time.Instant
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt

import com.example.kotlinfrontend.ui.root.SignSpeakRoot

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as SignSpeakApp
        setContent {
            KotlinFrontendTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    SignSpeakRoot(container = app.appContainer)
                }
            }
        }
    }
}

private val HAND_CONNECTIONS: List<Pair<Int, Int>> = listOf(
    0 to 1, 1 to 2, 2 to 3, 3 to 4,
    0 to 5, 5 to 6, 6 to 7, 7 to 8,
    5 to 9, 9 to 10, 10 to 11, 11 to 12,
    9 to 13, 13 to 14, 14 to 15, 15 to 16,
    13 to 17, 17 to 18, 18 to 19, 19 to 20,
    0 to 17
)

private fun validateAndNormalizeBackendUrl(rawValue: String): String {
    val normalized = rawValue.trim().trimEnd('/')
    require(normalized.isNotBlank()) { "Backend URL is required." }

    val uri = try {
        URI(normalized)
    } catch (_: Exception) {
        throw IllegalArgumentException("Enter a valid http:// or https:// URL.")
    }

    val scheme = uri.scheme?.lowercase().orEmpty()
    require(scheme == "http" || scheme == "https") {
        "Enter a valid http:// or https:// URL."
    }
    require(!uri.host.isNullOrBlank()) {
        "Enter a valid backend host."
    }

    return normalized
}

@Composable
fun LiveCameraScreen(
    productMode: Boolean = false,
    isAuthenticated: Boolean = false,
    onRequireAuth: () -> Unit = {},
    onWordCommitted: (word: String, confidence: Float, model: SignModel, createdAt: String) -> Unit =
        { _, _, _, _ -> },
    onReportPrediction: (word: String, confidence: Float, model: SignModel) -> Unit =
        { _, _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
    }
    val analysisExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val modelLoadExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val recordingGate = remember { AtomicBoolean(false) }
    val modelReadyGate = remember { AtomicBoolean(false) }
    val sessionCounter = remember { AtomicInteger(0) }
    val liveEngineRef = remember { AtomicReference<StreamingSignEngine?>(null) }
    val targetFpsGate = remember { AtomicInteger(50) }
    val lastProcessedFrameNs = remember { AtomicLong(0L) }
    val overlayEnabledGate = remember { AtomicBoolean(false) }
    val faceEnabledGate = remember { AtomicBoolean(false) }
    val backendSettingsRepository = remember { BackendSettingsRepository(context.applicationContext) }
    val backendClient = remember { SignBackendClient() }
    val coroutineScope = rememberCoroutineScope()
    // Active camera reference (for torch control)
    val activeCameraRef = remember { AtomicReference<Camera?>(null) }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var inferenceMode by remember { mutableStateOf(InferenceMode.ON_DEVICE) }
    var selectedModel by remember { mutableStateOf(SignModel.AUGMENTED) }
    var useQuantizedModel by remember { mutableStateOf(false) }
    var performanceMode by remember { mutableStateOf(true) }
    var showHandOverlay by remember { mutableStateOf(false) }
    var showFaceOverlay by remember { mutableStateOf(false) }
    // ── New user-facing camera controls ──────────────────────────────────────
    var useFrontCamera by remember { mutableStateOf(true) }
    var torchEnabled by remember { mutableStateOf(false) }
    var settingsExpanded by remember { mutableStateOf(false) }
    // ─────────────────────────────────────────────────────────────────────────
    var controlsExpanded by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var isModelLoading by remember { mutableStateOf(false) }
    var isModelReady by remember { mutableStateOf(false) }
    var targetCaptureFps by remember { mutableIntStateOf(50) }
    var statusText by remember { mutableStateOf("Initializing live camera...") }
    var predictedText by remember { mutableStateOf("--") }
    var confidence by remember { mutableFloatStateOf(0f) }
    var stability by remember { mutableFloatStateOf(0f) }
    var transcript by remember { mutableStateOf("") }
    var overlayHands by remember { mutableStateOf<List<HandWireframe>>(emptyList()) }
    var overlayFace by remember { mutableStateOf<FaceWireframe?>(null) }
    var fps by remember { mutableFloatStateOf(0f) }
    var handsInFrame by remember { mutableIntStateOf(0) }
    var bufferCount by remember { mutableIntStateOf(0) }
    var sequenceLength by remember { mutableIntStateOf(60) }
    var capturedFrameCount by remember { mutableIntStateOf(0) }
    var inferenceMs by remember { mutableLongStateOf(0L) }
    var roundTripMs by remember { mutableStateOf<Long?>(null) }
    var requestInFlight by remember { mutableStateOf(false) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var backendUrlInput by remember { mutableStateOf("") }
    var activeBackendUrl by remember { mutableStateOf("") }
    var backendUrlError by remember { mutableStateOf<String?>(null) }
    var backendConnectionMessage by remember { mutableStateOf<String?>(null) }
    var isBackendReachable by remember { mutableStateOf(false) }
    var isBackendChecking by remember { mutableStateOf(false) }
    var didLoadSavedBackendUrl by remember { mutableStateOf(false) }
    var lastObservedCommitCount by remember { mutableIntStateOf(0) }

    // Sync torch whenever torchEnabled changes
    LaunchedEffect(torchEnabled) {
        activeCameraRef.get()?.cameraControl?.enableTorch(torchEnabled)
    }
    // Front camera doesn't support torch — disable when switching to front
    LaunchedEffect(useFrontCamera) {
        if (useFrontCamera) torchEnabled = false
    }

    LaunchedEffect(productMode) {
        if (productMode) {
            inferenceMode = InferenceMode.ON_DEVICE
        }
    }

    overlayEnabledGate.set(showHandOverlay && !performanceMode)
    faceEnabledGate.set(showFaceOverlay && !performanceMode)

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        statusText = if (granted) {
            "Camera permission granted. Initializing translator..."
        } else {
            "Camera permission denied."
        }
    }

    suspend fun refreshBackendHealth(updateStatusText: Boolean): Boolean {
        if (activeBackendUrl.isBlank()) {
            isBackendReachable = false
            isBackendChecking = false
            backendConnectionMessage = "Set and save a backend URL first."
            if (updateStatusText && inferenceMode == InferenceMode.BACKEND_LANDMARKS) {
                statusText = "Backend mode needs a reachable server before recording."
            }
            return false
        }

        isBackendChecking = true
        backendConnectionMessage = "Checking backend health..."
        val connected = withContext(Dispatchers.IO) {
            backendClient.checkHealth(activeBackendUrl)
        }
        isBackendChecking = false
        isBackendReachable = connected
        backendConnectionMessage = if (connected) {
            "Backend reachable at $activeBackendUrl"
        } else {
            "Backend unreachable from phone"
        }

        if (updateStatusText && inferenceMode == InferenceMode.BACKEND_LANDMARKS) {
            statusText = if (connected) {
                "Backend connected. Press Record."
            } else {
                "Backend health check failed. Verify the URL and server."
            }
        }
        return connected
    }

    suspend fun applyBackendUrl() {
        val normalized = try {
            validateAndNormalizeBackendUrl(backendUrlInput)
        } catch (error: IllegalArgumentException) {
            backendUrlError = error.message ?: "Enter a valid backend URL."
            isBackendReachable = false
            backendConnectionMessage = null
            return
        }

        backendUrlError = null
        backendUrlInput = normalized
        activeBackendUrl = normalized
        withContext(Dispatchers.IO) {
            backendSettingsRepository.saveBackendBaseUrl(normalized)
        }
        refreshBackendHealth(updateStatusText = inferenceMode == InferenceMode.BACKEND_LANDMARKS)
    }

    fun stopRecording() {
        isRecording = false
        recordingGate.set(false)
        lastProcessedFrameNs.set(0L)
        overlayHands = emptyList()
        overlayFace = null
        roundTripMs = null
        requestInFlight = false
        lastObservedCommitCount = 0
    }

    suspend fun startRecording() {
        if (!isModelReady) {
            statusText = "Pipeline not ready yet. Please wait..."
            return
        }

        if (inferenceMode == InferenceMode.BACKEND_LANDMARKS) {
            val connected = refreshBackendHealth(updateStatusText = true)
            if (!connected) {
                return
            }
        }

        liveEngineRef.get()?.resetCapture(clearTranscript = false)
        predictedText = "--"
        confidence = 0f
        stability = 0f
        inferenceMs = 0L
        roundTripMs = null
        requestInFlight = false
        capturedFrameCount = 0
        overlayHands = emptyList()
        overlayFace = null
        lastProcessedFrameNs.set(0L)
        isRecording = true
        recordingGate.set(true)
        statusText = when (inferenceMode) {
            InferenceMode.ON_DEVICE -> {
                if (performanceMode) {
                    "Recording started at $targetCaptureFps FPS (speed mode)."
                } else {
                    "Recording started at $targetCaptureFps FPS."
                }
            }
            InferenceMode.BACKEND_LANDMARKS -> {
                "Recording started at $targetCaptureFps FPS. Sending landmarks to backend."
            }
        }
    }

    fun shutdownSession() {
        stopRecording()
        modelReadyGate.set(false)
        isModelReady = false
        isModelLoading = false
        cameraProvider?.unbindAll()
        liveEngineRef.getAndSet(null)?.close()
    }

    fun bindLivePipeline(
        mode: InferenceMode,
        model: SignModel,
        performanceModeEnabled: Boolean,
        quantizedEnabled: Boolean,
        backendBaseUrl: String,
        frontCamera: Boolean = true
    ) {
        val sessionId = sessionCounter.incrementAndGet()
        stopRecording()
        modelReadyGate.set(false)
        isModelReady = false
        isModelLoading = true
        val modelVariantText = if (quantizedEnabled && mode == InferenceMode.ON_DEVICE) {
            "quantized"
        } else {
            "standard"
        }
        statusText = when (mode) {
            InferenceMode.ON_DEVICE -> "Loading ${model.displayName} ($modelVariantText) model..."
            InferenceMode.BACKEND_LANDMARKS -> "Initializing backend landmark pipeline..."
        }
        predictedText = "--"
        confidence = 0f
        stability = 0f
        inferenceMs = 0L
        roundTripMs = null
        requestInFlight = false
        bufferCount = 0
        transcript = ""
        overlayHands = emptyList()
        overlayFace = null
        capturedFrameCount = 0
        lastProcessedFrameNs.set(0L)
        lastObservedCommitCount = 0

        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener(
            {
                try {
                    val provider = providerFuture.get()
                    cameraProvider = provider
                    provider.unbindAll()
                    liveEngineRef.getAndSet(null)?.close()

                    val previewUseCase = Preview.Builder().build().also { preview ->
                        preview.surfaceProvider = previewView.surfaceProvider
                    }
                    val analysisResolution = if (performanceModeEnabled) {
                        if (mode == InferenceMode.BACKEND_LANDMARKS) {
                            Size(256, 192)
                        } else {
                            Size(320, 240)
                        }
                    } else {
                        Size(640, 480)
                    }
                    val analysisUseCase = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setTargetResolution(analysisResolution)
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                        .build()

                    analysisUseCase.setAnalyzer(analysisExecutor) { imageProxy ->
                        if (!recordingGate.get() || !modelReadyGate.get()) {
                            imageProxy.close()
                            return@setAnalyzer
                        }

                        val targetFps = targetFpsGate.get().coerceAtLeast(1)
                        val frameTimestampNs = imageProxy.imageInfo.timestamp
                        val minFrameIntervalNs = 1_000_000_000L / targetFps.toLong()
                        val previousTimestampNs = lastProcessedFrameNs.get()
                        if (previousTimestampNs > 0L &&
                            frameTimestampNs > previousTimestampNs &&
                            frameTimestampNs - previousTimestampNs < minFrameIntervalNs
                        ) {
                            imageProxy.close()
                            return@setAnalyzer
                        }
                        lastProcessedFrameNs.set(frameTimestampNs)

                        try {
                            val engine = liveEngineRef.get() ?: return@setAnalyzer
                            val liveState = engine.process(
                                imageProxy = imageProxy,
                                includeOverlay = overlayEnabledGate.get(),
                                includeFace = faceEnabledGate.get()
                            )
                            mainExecutor.execute {
                                if (sessionCounter.get() != sessionId) {
                                    return@execute
                                }
                                capturedFrameCount += 1
                                bufferCount = liveState.bufferCount
                                sequenceLength = liveState.sequenceLength
                                handsInFrame = liveState.handsInFrame
                                fps = liveState.fps
                                predictedText = liveState.displayedLabel
                                confidence = liveState.displayedConfidence.coerceIn(0f, 1f)
                                stability = liveState.stability.coerceIn(0f, 1f)
                                transcript = liveState.transcript
                                overlayHands = liveState.overlayHands
                                overlayFace = liveState.overlayFace
                                roundTripMs = liveState.roundTripMs
                                requestInFlight = liveState.requestInFlight

                                if (liveState.commitCount > lastObservedCommitCount) {
                                    lastObservedCommitCount = liveState.commitCount
                                    val committedWord = liveState.lastCommittedLabel
                                        ?: liveState.displayedLabel
                                    if (committedWord.isNotBlank() && committedWord != "--") {
                                        onWordCommitted(
                                            committedWord,
                                            liveState.displayedConfidence,
                                            selectedModel,
                                            Instant.now().toString()
                                        )
                                    }
                                }

                                liveState.rawPrediction?.let { prediction ->
                                    inferenceMs = prediction.processingTimeMs
                                }

                                statusText = when {
                                    !liveState.errorMessage.isNullOrBlank() -> liveState.errorMessage
                                    liveState.rawPrediction != null &&
                                        mode == InferenceMode.BACKEND_LANDMARKS &&
                                        liveState.requestInFlight ->
                                        "Recording at ${targetFpsGate.get()} FPS. Backend request in flight..."
                                    liveState.rawPrediction != null &&
                                        mode == InferenceMode.BACKEND_LANDMARKS ->
                                        "Recording at ${targetFpsGate.get()} FPS and predicting on backend..."
                                    liveState.rawPrediction != null ->
                                        "Recording at ${targetFpsGate.get()} FPS and predicting..."
                                    liveState.requestInFlight &&
                                        mode == InferenceMode.BACKEND_LANDMARKS ->
                                        "Sending landmarks to backend..."
                                    else ->
                                        "Collecting landmarks (${liveState.bufferCount}/${liveState.sequenceLength})"
                                }
                            }
                        } catch (error: Exception) {
                            mainExecutor.execute {
                                if (sessionCounter.get() != sessionId) {
                                    return@execute
                                }
                                statusText = error.message ?: "Live inference error."
                            }
                        } finally {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = if (frontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }
                    val boundCamera = provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        previewUseCase,
                        analysisUseCase
                    )
                    activeCameraRef.set(boundCamera)
                    // Apply torch state immediately after binding
                    boundCamera.cameraControl.enableTorch(torchEnabled)

                    modelLoadExecutor.execute {
                        try {
                            val engine: StreamingSignEngine = when (mode) {
                                InferenceMode.ON_DEVICE -> LiveSignEngine(
                                    context = context,
                                    model = model,
                                    useQuantizedModel = quantizedEnabled
                                )
                                InferenceMode.BACKEND_LANDMARKS -> BackendLiveSignEngine(
                                    context = context,
                                    baseUrl = backendBaseUrl,
                                    model = model
                                )
                            }
                            engine.warmUp()
                            mainExecutor.execute {
                                if (sessionCounter.get() != sessionId) {
                                    engine.close()
                                    return@execute
                                }
                                liveEngineRef.getAndSet(engine)?.close()
                                modelReadyGate.set(true)
                                isModelLoading = false
                                isModelReady = true
                                statusText = when (mode) {
                                    InferenceMode.ON_DEVICE -> {
                                        val readyVariantText = if (quantizedEnabled) {
                                            "Quantized"
                                        } else {
                                            "Standard"
                                        }
                                        if (performanceModeEnabled) {
                                            "$readyVariantText model ready in speed mode (${analysisResolution.width}x${analysisResolution.height}). Press Record."
                                        } else {
                                            "$readyVariantText model ready. Set FPS and press Record."
                                        }
                                    }
                                    InferenceMode.BACKEND_LANDMARKS -> {
                                        if (backendBaseUrl.isBlank()) {
                                            "Backend pipeline ready. Set backend URL and check health."
                                        } else {
                                            "Backend pipeline ready. Check health before recording."
                                        }
                                    }
                                }
                            }
                        } catch (error: Exception) {
                            mainExecutor.execute {
                                if (sessionCounter.get() != sessionId) {
                                    return@execute
                                }
                                modelReadyGate.set(false)
                                isModelLoading = false
                                isModelReady = false
                                statusText = error.message ?: "Failed to load pipeline."
                            }
                        }
                    }
                } catch (error: Exception) {
                    isModelLoading = false
                    isModelReady = false
                    modelReadyGate.set(false)
                    statusText = error.message ?: "Failed to start camera."
                }
            },
            mainExecutor
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            shutdownSession()
            analysisExecutor.shutdown()
            modelLoadExecutor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        val savedBackendUrl = backendSettingsRepository.backendBaseUrlFlow.first()
        backendUrlInput = savedBackendUrl
        activeBackendUrl = savedBackendUrl
        didLoadSavedBackendUrl = true
        if (savedBackendUrl.isNotBlank()) {
            refreshBackendHealth(updateStatusText = false)
        }
    }

    LaunchedEffect(hasPermission, inferenceMode, selectedModel, performanceMode, useQuantizedModel, activeBackendUrl, useFrontCamera) {
        if (hasPermission) {
            bindLivePipeline(
                mode = inferenceMode,
                model = selectedModel,
                performanceModeEnabled = performanceMode,
                quantizedEnabled = inferenceMode == InferenceMode.ON_DEVICE && useQuantizedModel,
                backendBaseUrl = activeBackendUrl,
                frontCamera = useFrontCamera
            )
        }
    }

    LaunchedEffect(inferenceMode, activeBackendUrl, didLoadSavedBackendUrl) {
        if (!didLoadSavedBackendUrl) {
            return@LaunchedEffect
        }
        if (inferenceMode == InferenceMode.BACKEND_LANDMARKS) {
            if (activeBackendUrl.isBlank()) {
                isBackendReachable = false
                backendConnectionMessage = "Enter a backend URL to enable server inference."
            } else {
                refreshBackendHealth(updateStatusText = !isRecording)
            }
        }
    }

    // Derived state
    val readyIndicatorColor = when {
        isModelLoading || isBackendChecking -> Color(0xFFF59E0B)
        inferenceMode == InferenceMode.BACKEND_LANDMARKS && isModelReady && isBackendReachable -> Color(0xFF58CC02)
        inferenceMode == InferenceMode.BACKEND_LANDMARKS && isModelReady -> Color(0xFF0EA5E9)
        isModelReady -> Color(0xFF58CC02)
        else -> Color(0xFF94A3B8)
    }
    val canStartRecording = if (isRecording) true else {
        hasPermission && isModelReady && !isModelLoading &&
            (inferenceMode == InferenceMode.ON_DEVICE || (isBackendReachable && !isBackendChecking))
    }

    // ── Root layout (white background, full screen) ───────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAF5)),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {

        // ── Status bar (compact top chip) ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Status pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(readyIndicatorColor)
                )
                Text(
                    text = if (isModelLoading) "Loading model…"
                           else if (isRecording) "Translating…"
                           else if (isModelReady) "Ready"
                           else "Initialising…",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A1A)
                )
            }
            // Loading progress pill (only while loading)
            if (isModelLoading || isBackendChecking) {
                LinearProgressIndicator(
                    progress = { if (isModelLoading) 0.4f else 0.8f },
                    modifier = Modifier
                        .width(80.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    color = Color(0xFF58CC02),
                    trackColor = Color(0xFFE2E8F0)
                )
            }
        }

        // ── Camera preview (takes most of the space) ──────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(Color(0xFF0B1623))
        ) {
            if (hasPermission) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                // ── Hand wireframe overlay (recording only) ────────────────
                if (isRecording && (overlayHands.isNotEmpty() || overlayFace != null)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        overlayHands.forEach { hand ->
                            val color = when (hand.side) {
                                HandSide.LEFT  -> Color(0xFF58CC02)
                                HandSide.RIGHT -> Color(0xFFFFC800)
                                HandSide.UNKNOWN -> Color(0xFF60A5FA)
                            }
                            val mappedPoints = hand.points.map { p ->
                                Offset(((1f - p.x).coerceIn(0f, 1f)) * size.width,
                                       p.y.coerceIn(0f, 1f) * size.height)
                            }
                            HAND_CONNECTIONS.forEach { (s, e) ->
                                if (s < mappedPoints.size && e < mappedPoints.size) {
                                    drawLine(color.copy(alpha = 0.9f), mappedPoints[s], mappedPoints[e], strokeWidth = 4f)
                                }
                            }
                            mappedPoints.forEach { pt ->
                                drawCircle(Color.White, radius = 5f, center = pt)
                                drawCircle(color,      radius = 3f, center = pt)
                            }
                        }
                    }
                }

                // ── Floating camera controls (top-right of preview) ────────
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Flip camera
                    FilledTonalIconButton(
                        onClick = { useFrontCamera = !useFrontCamera },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = Color(0xCC1C1C1E),
                            contentColor   = Color.White
                        )
                    ) {
                        Icon(Icons.Rounded.Cameraswitch, contentDescription = "Flip camera")
                    }
                    // Torch (back camera only)
                    if (!useFrontCamera) {
                        FilledTonalIconButton(
                            onClick = { torchEnabled = !torchEnabled },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = if (torchEnabled) Color(0xFFFFC800) else Color(0xCC1C1C1E),
                                contentColor   = if (torchEnabled) Color(0xFF1A1A1A) else Color.White
                            )
                        ) {
                            Icon(
                                if (torchEnabled) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff,
                                contentDescription = "Torch"
                            )
                        }
                    }
                }

                // ── Prediction overlay (bottom of preview) ─────────────────
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xE6080F1A))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (predictedText == "--") "Waiting…" else predictedText,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (confidence > 0f) "${(confidence * 100f).roundToInt()}%" else "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF58CC02)
                            )
                        }
                        // Confidence bar
                        if (confidence > 0f) {
                            LinearProgressIndicator(
                                progress = { confidence.coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                color = Color(0xFF58CC02),
                                trackColor = Color(0x3358CC02)
                            )
                        }
                        // Transcript row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (transcript.isBlank()) "—" else transcript,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCCE5FF),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                if (transcript.isNotBlank()) {
                                    TextButton(
                                        onClick = {
                                            liveEngineRef.get()?.clearTranscript()
                                            transcript = ""
                                            lastObservedCommitCount = 0
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) { Text("Clear", color = Color(0xFF94A3B8)) }
                                }
                                if (productMode && predictedText != "--") {
                                    TextButton(
                                        onClick = {
                                            if (!isAuthenticated) onRequireAuth()
                                            else onReportPrediction(predictedText, confidence, selectedModel)
                                        },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) { Text("Report", color = Color(0xFF94A3B8)) }
                                }
                            }
                        }
                    }
                }
            } else {
                // ── No permission state ────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Camera access needed", color = Color.White, fontWeight = FontWeight.Bold)
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.padding(top = 12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58CC02))
                    ) { Text("Grant Permission") }
                }
            }
        } // end camera Box

        // ── Record button ─────────────────────────────────────────────────────
        Button(
            onClick = {
                coroutineScope.launch {
                    if (isRecording) {
                        stopRecording()
                        statusText = "Recording stopped."
                    } else {
                        startRecording()
                    }
                }
            },
            enabled = canStartRecording,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRecording) Color(0xFFFF4B4B) else Color(0xFF58CC02),
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFD1D5DB),
                disabledContentColor = Color(0xFF9CA3AF)
            )
        ) {
            Text(
                text = if (isRecording) "⏹  Stop" else "⏺  Start Translating",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // ── Expandable Settings panel ─────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                // Settings header row (tap to expand/collapse)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Settings, contentDescription = null, tint = Color(0xFF58CC02))
                        Text(
                            "Settings",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1A1A1A)
                        )
                    }
                    IconButton(onClick = { settingsExpanded = !settingsExpanded }) {
                        Icon(
                            if (settingsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = if (settingsExpanded) "Collapse" else "Expand",
                            tint = Color(0xFF64748B)
                        )
                    }
                }

                // Collapsible content
                AnimatedVisibility(
                    visible = settingsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit  = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 0.dp).padding(bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {

                        // ── Camera section ─────────────────────────────────
                        SettingsSectionLabel("Camera")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CameraOptionChip(
                                label = "Front",
                                selected = useFrontCamera,
                                modifier = Modifier.weight(1f),
                                onClick = { useFrontCamera = true }
                            )
                            CameraOptionChip(
                                label = "Back",
                                selected = !useFrontCamera,
                                modifier = Modifier.weight(1f),
                                onClick = { useFrontCamera = false }
                            )
                        }
                        if (!useFrontCamera) {
                            SettingsToggleRow(
                                label = "Flashlight",
                                description = "Turn on torch for better lighting",
                                checked = torchEnabled,
                                onCheckedChange = { torchEnabled = it }
                            )
                        }

                        // ── Model quality section ──────────────────────────
                        SettingsSectionLabel("Model Quality")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CameraOptionChip(
                                label = "Augmented",
                                selected = selectedModel == SignModel.AUGMENTED,
                                modifier = Modifier.weight(1f),
                                onClick = { if (selectedModel != SignModel.AUGMENTED) selectedModel = SignModel.AUGMENTED }
                            )
                            CameraOptionChip(
                                label = "Baseline",
                                selected = selectedModel == SignModel.BASELINE,
                                modifier = Modifier.weight(1f),
                                onClick = { if (selectedModel != SignModel.BASELINE) selectedModel = SignModel.BASELINE }
                            )
                        }

                        // ── Capture speed section ──────────────────────────
                        SettingsSectionLabel("Capture Speed — $targetCaptureFps FPS")
                        Slider(
                            value = targetCaptureFps.toFloat(),
                            onValueChange = { rawValue ->
                                val adjusted = rawValue.roundToInt().coerceIn(5, 60)
                                targetCaptureFps = adjusted
                                targetFpsGate.set(adjusted)
                            },
                            valueRange = 5f..60f,
                            steps = 10,
                            enabled = !isModelLoading,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF58CC02),
                                activeTrackColor = Color(0xFF58CC02),
                                inactiveTrackColor = Color(0xFFE2E8F0)
                            )
                        )

                        // ── Dev-only section (non-product mode) ───────────
                        if (!productMode) {
                            SettingsSectionLabel("Advanced")
                            SettingsToggleRow(
                                label = "Speed Mode",
                                description = "Lower resolution for faster processing",
                                checked = performanceMode,
                                onCheckedChange = { performanceMode = it },
                                enabled = !isModelLoading
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CameraOptionChip(
                                    label = "On Device",
                                    selected = inferenceMode == InferenceMode.ON_DEVICE,
                                    modifier = Modifier.weight(1f),
                                    onClick = { inferenceMode = InferenceMode.ON_DEVICE }
                                )
                                CameraOptionChip(
                                    label = "Backend",
                                    selected = inferenceMode == InferenceMode.BACKEND_LANDMARKS,
                                    modifier = Modifier.weight(1f),
                                    onClick = { inferenceMode = InferenceMode.BACKEND_LANDMARKS }
                                )
                            }
                            if (inferenceMode == InferenceMode.BACKEND_LANDMARKS) {
                                OutlinedTextField(
                                    value = backendUrlInput,
                                    onValueChange = { backendUrlInput = it; backendUrlError = null },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    enabled = !isRecording,
                                    label = { Text("Backend URL") },
                                    placeholder = { Text("http://192.168.x.x:8000") },
                                    isError = backendUrlError != null
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { coroutineScope.launch { applyBackendUrl() } },
                                        enabled = !isRecording && !isBackendChecking,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Save") }
                                    Button(
                                        onClick = { coroutineScope.launch { refreshBackendHealth(true) } },
                                        enabled = activeBackendUrl.isNotBlank() && !isBackendChecking,
                                        modifier = Modifier.weight(1f)
                                    ) { Text(if (isBackendChecking) "Checking…" else "Health") }
                                }
                                backendConnectionMessage?.let { msg ->
                                    Text(msg, style = MaterialTheme.typography.bodySmall,
                                         color = if (isBackendReachable) Color(0xFF16A34A) else Color(0xFFB91C1C))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Helper composables ─────────────────────────────────────────────────────────

@Composable
private fun CameraOptionChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) Color(0xFF58CC02) else Color(0xFFF1F5F9),
            contentColor   = if (selected) Color.White      else Color(0xFF374151)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (selected) 2.dp else 0.dp
        )
    ) {
        Text(
            text = label,
            style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun SettingsSectionLabel(label: String) {
    Text(
        text = label,
        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF6B7280)
    )
}

@Composable
private fun SettingsToggleRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1A1A1A)
            )
            Text(
                text = description,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = Color(0xFF6B7280)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF58CC02),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFD1D5DB)
            )
        )
    }
}

