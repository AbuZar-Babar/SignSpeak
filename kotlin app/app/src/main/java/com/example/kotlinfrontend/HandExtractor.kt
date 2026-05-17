package com.example.kotlinfrontend

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.components.containers.Detection
import com.google.mediapipe.tasks.components.containers.NormalizedKeypoint
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facedetector.FaceDetector
import com.google.mediapipe.tasks.vision.facedetector.FaceDetectorResult
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.Locale
import kotlin.math.max

data class HandPoint(
    val x: Float,
    val y: Float
)

enum class HandSide {
    LEFT,
    RIGHT,
    UNKNOWN
}

data class HandWireframe(
    val side: HandSide,
    val points: List<HandPoint>
)

data class FaceWireframe(
    val nose: HandPoint?,
    val leftEar: HandPoint?,
    val rightEar: HandPoint?
)

data class HandExtractionOutput(
    val vector: FloatArray,
    val hands: List<HandWireframe>,
    val face: FaceWireframe?,
    val faceInput: FloatArray? = null
)

class HandExtractor(context: Context) : Closeable {
    companion object {
        private const val MODEL_ASSET_PATH = "hand_landmarker.task"
        private const val FACE_MODEL_ASSET_PATH = "blaze_face_short_range.tflite"
        private const val HAND_LANDMARKS = 21
        private const val HAND_VECTOR_SIZE = HAND_LANDMARKS * 3
        private const val FRAME_VECTOR_SIZE = HAND_VECTOR_SIZE * 2
        private const val FACE_DETECTION_INTERVAL_MS = 180L

        private const val FACE_KEYPOINT_NOSE = 2
        private const val FACE_KEYPOINT_RIGHT_EAR = 4
        private const val FACE_KEYPOINT_LEFT_EAR = 5
        private const val FACE_INPUT_SIZE = 48
    }

    private val handLandmarker: HandLandmarker = createHandLandmarker(context)
    private val appContext = context.applicationContext
    private var faceDetector: FaceDetector? = null
    private val rotationOptionsCache = HashMap<Int, ImageProcessingOptions>(4)
    private var cachedFaceSnapshot: FaceDetectionSnapshot? = null
    private var lastFaceDetectionTimestampMs = -1L
    private var rgbaScratchBytes = ByteArray(0)
    private var rgbaScratchBuffer = ByteBuffer.wrap(rgbaScratchBytes)

    fun extractSequence(frames: List<VideoFrame>): List<FloatArray> {
        val output = ArrayList<FloatArray>(frames.size)
        var lastTimestampMs = -1L

        frames.forEach { frame ->
            val timestampMs = frame.timestampMs.coerceAtLeast(lastTimestampMs + 1L)
            val frameVector = extractFromBitmap(frame.bitmap, timestampMs)
            output.add(frameVector)
            lastTimestampMs = timestampMs
        }

        return output
    }

    fun extractFromBitmap(bitmap: Bitmap, timestampMs: Long): FloatArray {
        val mpImage = BitmapImageBuilder(bitmap).build()
        return mpImage.use { image ->
            val result = handLandmarker.detectForVideo(image, timestampMs)
            toFixedFrameVector(result)
        }
    }

    fun extractFromImageProxy(imageProxy: ImageProxy, timestampMs: Long): FloatArray {
        return extractFromImageProxyWithOverlay(
            imageProxy = imageProxy,
            timestampMs = timestampMs,
            includeOverlay = false,
            includeFace = false
        ).vector
    }

    fun extractFromImageProxyWithOverlay(
        imageProxy: ImageProxy,
        timestampMs: Long,
        includeOverlay: Boolean = true,
        includeFace: Boolean = true,
        includeFaceInput: Boolean = false
    ): HandExtractionOutput {
        val normalizedRotationDegrees = normalizeRotationDegrees(imageProxy.imageInfo.rotationDegrees)
        val processingOptions = imageProcessingOptionsFor(normalizedRotationDegrees)
        val bitmap = bitmapFromImageProxy(imageProxy)
        val mpImage = BitmapImageBuilder(bitmap).build()

        return mpImage.use { image ->
            val handResult = handLandmarker.detectForVideo(image, processingOptions, timestampMs)
            val faceSnapshot = if (includeFace || includeFaceInput) {
                detectFaceWithCaching(
                    image = image,
                    processingOptions = processingOptions,
                    timestampMs = timestampMs,
                    rotationDegrees = normalizedRotationDegrees
                )
            } else {
                null
            }

            HandExtractionOutput(
                vector = toFixedFrameVector(handResult, normalizedRotationDegrees),
                hands = if (includeOverlay) {
                    toWireframeHands(handResult, normalizedRotationDegrees)
                } else {
                    emptyList()
                },
                face = if (includeFace) faceSnapshot?.wireframe else null,
                faceInput = if (includeFaceInput) {
                    faceSnapshot?.bounds?.let { bounds -> toFaceModelInput(bitmap, bounds) }
                } else {
                    null
                }
            )
        }
    }

    private fun detectFaceWithCaching(
        image: MPImage,
        processingOptions: ImageProcessingOptions,
        timestampMs: Long,
        rotationDegrees: Int
    ): FaceDetectionSnapshot? {
        val shouldRefresh = lastFaceDetectionTimestampMs < 0L ||
            (timestampMs - lastFaceDetectionTimestampMs) >= FACE_DETECTION_INTERVAL_MS
        if (shouldRefresh) {
            val faceResult = getOrCreateFaceDetector()
                .detectForVideo(image, processingOptions, timestampMs)
            cachedFaceSnapshot = toFaceDetectionSnapshot(faceResult, rotationDegrees)
            lastFaceDetectionTimestampMs = timestampMs
        }
        return cachedFaceSnapshot
    }

    private fun bitmapFromImageProxy(imageProxy: ImageProxy): Bitmap {
        require(imageProxy.format == PixelFormat.RGBA_8888) {
            "Expected RGBA_8888 analysis frames but found format ${imageProxy.format}."
        }

        val plane = imageProxy.planes.firstOrNull()
            ?: throw IllegalStateException("ImageProxy contains no pixel planes.")
        require(plane.pixelStride == 4) {
            "Expected RGBA_8888 pixel stride of 4 but found ${plane.pixelStride}."
        }

        val width = imageProxy.width
        val height = imageProxy.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val source = plane.buffer.duplicate().apply { rewind() }
        val contiguousRowBytes = width * 4
        require(plane.rowStride >= contiguousRowBytes) {
            "Expected row stride >= $contiguousRowBytes but found ${plane.rowStride}."
        }

        val scratch = ensureRgbaScratchBuffer(contiguousRowBytes * height)
        val scratchArray = scratch.array()
        for (row in 0 until height) {
            source.position(row * plane.rowStride)
            source.get(scratchArray, row * contiguousRowBytes, contiguousRowBytes)
        }
        scratch.rewind()
        bitmap.copyPixelsFromBuffer(scratch)
        return bitmap
    }

    private fun ensureRgbaScratchBuffer(requiredBytes: Int): ByteBuffer {
        if (rgbaScratchBytes.size < requiredBytes) {
            rgbaScratchBytes = ByteArray(requiredBytes)
            rgbaScratchBuffer = ByteBuffer.wrap(rgbaScratchBytes)
        }
        rgbaScratchBuffer.clear()
        rgbaScratchBuffer.limit(requiredBytes)
        return rgbaScratchBuffer
    }

    private fun imageProcessingOptionsFor(rotationDegrees: Int): ImageProcessingOptions {
        return rotationOptionsCache.getOrPut(rotationDegrees) {
            ImageProcessingOptions.builder()
                .setRotationDegrees(rotationDegrees)
                .build()
        }
    }

    private fun toFixedFrameVector(
        result: HandLandmarkerResult,
        rotationDegrees: Int = 0
    ): FloatArray {
        val frameVector = FloatArray(FRAME_VECTOR_SIZE)

        val landmarksByHand = result.landmarks()
        val handednessByHand = if (result.handednesses().isNotEmpty()) {
            result.handednesses()
        } else {
            result.handedness()
        }

        landmarksByHand.forEachIndexed { handIndex, handLandmarks ->
            val slot = resolveHandSlot(handednessByHand.getOrNull(handIndex))
            val offset = when (slot) {
                HandSlot.LEFT -> 0
                HandSlot.RIGHT -> HAND_VECTOR_SIZE
                null -> return@forEachIndexed
            }

            if (handLandmarks.size < HAND_LANDMARKS) {
                return@forEachIndexed
            }

            copyLandmarksIntoSlot(
                landmarks = handLandmarks,
                target = frameVector,
                offset = offset,
                rotationDegrees = rotationDegrees
            )
        }
        return frameVector
    }

    private fun toWireframeHands(
        result: HandLandmarkerResult,
        rotationDegrees: Int = 0
    ): List<HandWireframe> {
        val landmarksByHand = result.landmarks()
        val handednessByHand = if (result.handednesses().isNotEmpty()) {
            result.handednesses()
        } else {
            result.handedness()
        }

        return landmarksByHand.mapIndexedNotNull { handIndex, handLandmarks ->
            if (handLandmarks.size < HAND_LANDMARKS) {
                return@mapIndexedNotNull null
            }
            val side = when (resolveHandSlot(handednessByHand.getOrNull(handIndex))) {
                HandSlot.LEFT -> HandSide.LEFT
                HandSlot.RIGHT -> HandSide.RIGHT
                null -> HandSide.UNKNOWN
            }
            val points = handLandmarks.take(HAND_LANDMARKS).map { landmark ->
                rotatePoint(landmark.x(), landmark.y(), rotationDegrees)
            }
            HandWireframe(side = side, points = points)
        }
    }

    private fun toFaceWireframe(result: FaceDetectorResult, rotationDegrees: Int): FaceWireframe? {
        val detection = result.detections().firstOrNull() ?: return null
        val keypoints = detection.extractKeypoints()

        return FaceWireframe(
            nose = keypoints.getOrNull(FACE_KEYPOINT_NOSE)?.toHandPoint(rotationDegrees),
            leftEar = keypoints.getOrNull(FACE_KEYPOINT_LEFT_EAR)?.toHandPoint(rotationDegrees),
            rightEar = keypoints.getOrNull(FACE_KEYPOINT_RIGHT_EAR)?.toHandPoint(rotationDegrees)
        )
    }

    private fun toFaceDetectionSnapshot(
        result: FaceDetectorResult,
        rotationDegrees: Int
    ): FaceDetectionSnapshot? {
        val detection = result.detections().firstOrNull() ?: return null
        val keypoints = detection.extractKeypoints()

        return FaceDetectionSnapshot(
            wireframe = FaceWireframe(
                nose = keypoints.getOrNull(FACE_KEYPOINT_NOSE)?.toHandPoint(rotationDegrees),
                leftEar = keypoints.getOrNull(FACE_KEYPOINT_LEFT_EAR)?.toHandPoint(rotationDegrees),
                rightEar = keypoints.getOrNull(FACE_KEYPOINT_RIGHT_EAR)?.toHandPoint(rotationDegrees)
            ),
            bounds = toNormalizedFaceBounds(keypoints)
        )
    }

    private fun toNormalizedFaceBounds(keypoints: List<NormalizedKeypoint>): NormalizedFaceBounds? {
        val validPoints = keypoints.filter { point ->
            point.x().isFinite() && point.y().isFinite() &&
                point.x() in 0f..1f && point.y() in 0f..1f
        }
        if (validPoints.isEmpty()) {
            return null
        }

        val minX = validPoints.minOf { it.x() }
        val maxX = validPoints.maxOf { it.x() }
        val minY = validPoints.minOf { it.y() }
        val maxY = validPoints.maxOf { it.y() }
        val centerX = ((minX + maxX) / 2f).coerceIn(0f, 1f)
        val centerY = ((minY + maxY) / 2f).coerceIn(0f, 1f)
        val featureSpan = max(maxX - minX, maxY - minY).coerceAtLeast(0.16f)
        val halfSize = (featureSpan * 0.9f).coerceIn(0.12f, 0.55f)

        return NormalizedFaceBounds(
            left = (centerX - halfSize).coerceIn(0f, 1f),
            top = (centerY - halfSize).coerceIn(0f, 1f),
            right = (centerX + halfSize).coerceIn(0f, 1f),
            bottom = (centerY + halfSize).coerceIn(0f, 1f)
        )
    }

    private fun toFaceModelInput(bitmap: Bitmap, bounds: NormalizedFaceBounds): FloatArray? {
        val left = (bounds.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (bounds.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = (bounds.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
        val bottom = (bounds.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
        val cropWidth = right - left
        val cropHeight = bottom - top
        if (cropWidth <= 1 || cropHeight <= 1) {
            return null
        }

        val crop = Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
        val scaled = Bitmap.createScaledBitmap(crop, FACE_INPUT_SIZE, FACE_INPUT_SIZE, true)
        val pixels = IntArray(FACE_INPUT_SIZE * FACE_INPUT_SIZE)
        scaled.getPixels(pixels, 0, FACE_INPUT_SIZE, 0, 0, FACE_INPUT_SIZE, FACE_INPUT_SIZE)

        return FloatArray(pixels.size) { index ->
            val pixel = pixels[index]
            val red = (pixel shr 16) and 0xFF
            val green = (pixel shr 8) and 0xFF
            val blue = pixel and 0xFF
            ((red * 0.299f) + (green * 0.587f) + (blue * 0.114f)) / 255f
        }
    }

    private fun Detection.extractKeypoints(): List<NormalizedKeypoint> {
        return if (keypoints().isPresent) {
            keypoints().get()
        } else {
            emptyList()
        }
    }

    private fun NormalizedKeypoint.toHandPoint(rotationDegrees: Int): HandPoint {
        return rotatePoint(x(), y(), rotationDegrees)
    }

    private fun copyLandmarksIntoSlot(
        landmarks: List<NormalizedLandmark>,
        target: FloatArray,
        offset: Int,
        rotationDegrees: Int
    ) {
        for (i in 0 until HAND_LANDMARKS) {
            val landmark = landmarks[i]
            val base = offset + (i * 3)
            val rawX = landmark.x()
            val rawY = landmark.y()
            val rotatedX: Float
            val rotatedY: Float
            when (rotationDegrees) {
                90 -> {
                    rotatedX = 1f - rawY
                    rotatedY = rawX
                }
                180 -> {
                    rotatedX = 1f - rawX
                    rotatedY = 1f - rawY
                }
                270 -> {
                    rotatedX = rawY
                    rotatedY = 1f - rawX
                }
                else -> {
                    rotatedX = rawX
                    rotatedY = rawY
                }
            }
            target[base] = rotatedX.coerceIn(0f, 1f)
            target[base + 1] = rotatedY.coerceIn(0f, 1f)
            target[base + 2] = landmark.z()
        }
    }

    private fun rotatePoint(x: Float, y: Float, rotationDegrees: Int): HandPoint {
        val rotatedX: Float
        val rotatedY: Float
        when (rotationDegrees) {
            90 -> {
                rotatedX = 1f - y
                rotatedY = x
            }
            180 -> {
                rotatedX = 1f - x
                rotatedY = 1f - y
            }
            270 -> {
                rotatedX = y
                rotatedY = 1f - x
            }
            else -> {
                rotatedX = x
                rotatedY = y
            }
        }
        return HandPoint(
            x = rotatedX.coerceIn(0f, 1f),
            y = rotatedY.coerceIn(0f, 1f)
        )
    }

    private fun normalizeRotationDegrees(rotationDegrees: Int): Int {
        val normalized = ((rotationDegrees % 360) + 360) % 360
        return when (normalized) {
            90, 180, 270 -> normalized
            else -> 0
        }
    }

    private fun resolveHandSlot(categories: List<Category>?): HandSlot? {
        val category = categories?.firstOrNull() ?: return null
        val rawLabel = if (category.categoryName().isNotBlank()) {
            category.categoryName()
        } else {
            category.displayName()
        }
        val label = rawLabel.lowercase(Locale.US)

        return when {
            label.contains("left") -> HandSlot.LEFT
            label.contains("right") -> HandSlot.RIGHT
            else -> null
        }
    }

    private fun createHandLandmarker(context: Context): HandLandmarker {
        createHandLandmarker(context, Delegate.GPU)?.let { return it }
        return createHandLandmarker(context, Delegate.CPU)
            ?: throw IllegalStateException(
                "Unable to load '$MODEL_ASSET_PATH'. Place the model in app/src/main/assets/."
            )
    }

    private fun createHandLandmarker(context: Context, delegate: Delegate): HandLandmarker? {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET_PATH)
            .setDelegate(delegate)
            .build()

        val options = HandLandmarker.HandLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)
            .setNumHands(2)
            .setMinHandDetectionConfidence(0.5f)
            .setMinHandPresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build()

        return try {
            HandLandmarker.createFromOptions(context, options)
        } catch (error: Exception) {
            if (delegate == Delegate.GPU) {
                null
            } else {
                throw IllegalStateException(
                    "Unable to load '$MODEL_ASSET_PATH'. Place the model in app/src/main/assets/.",
                    error
                )
            }
        }
    }

    private fun getOrCreateFaceDetector(): FaceDetector {
        val existing = faceDetector
        if (existing != null) {
            return existing
        }
        val created = createFaceDetector(appContext)
        faceDetector = created
        return created
    }

    private fun createFaceDetector(context: Context): FaceDetector {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(FACE_MODEL_ASSET_PATH)
            .build()

        val options = FaceDetector.FaceDetectorOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)
            .setMinDetectionConfidence(0.5f)
            .setMinSuppressionThreshold(0.3f)
            .build()

        return try {
            FaceDetector.createFromOptions(context, options)
        } catch (error: Exception) {
            throw IllegalStateException(
                "Unable to load '$FACE_MODEL_ASSET_PATH'. Place it in app/src/main/assets/.",
                error
            )
        }
    }

    override fun close() {
        handLandmarker.close()
        faceDetector?.close()
    }

    private enum class HandSlot {
        LEFT,
        RIGHT
    }

    private data class FaceDetectionSnapshot(
        val wireframe: FaceWireframe?,
        val bounds: NormalizedFaceBounds?
    )

    private data class NormalizedFaceBounds(
        val left: Float,
        val top: Float,
        val right: Float,
        val bottom: Float
    )
}
