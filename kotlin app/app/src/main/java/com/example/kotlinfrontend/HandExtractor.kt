package com.example.kotlinfrontend

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.io.Closeable
import java.nio.ByteBuffer
import java.util.Locale

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

data class HandExtractionOutput(
    val vector: FloatArray,
    val hands: List<HandWireframe>
)

class HandExtractor(context: Context) : Closeable {
    companion object {
        private const val MODEL_ASSET_PATH = "hand_landmarker.task"
        private const val HAND_LANDMARKS = 21
        private const val HAND_VECTOR_SIZE = HAND_LANDMARKS * 3
        private const val FRAME_VECTOR_SIZE = HAND_VECTOR_SIZE * 2
    }

    private val handLandmarker: HandLandmarker = createHandLandmarker(context)
    private val rotationOptionsCache = HashMap<Int, ImageProcessingOptions>(4)
    private var rgbaScratchBytes = ByteArray(0)
    private var rgbaScratchBuffer = ByteBuffer.wrap(rgbaScratchBytes)

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
            includeOverlay = false
        ).vector
    }

    fun extractFromImageProxyWithOverlay(
        imageProxy: ImageProxy,
        timestampMs: Long,
        includeOverlay: Boolean = true
    ): HandExtractionOutput {
        val normalizedRotationDegrees = normalizeRotationDegrees(imageProxy.imageInfo.rotationDegrees)
        val processingOptions = imageProcessingOptionsFor(normalizedRotationDegrees)
        val bitmap = bitmapFromImageProxy(imageProxy)
        val mpImage = BitmapImageBuilder(bitmap).build()

        return mpImage.use { image ->
            val handResult = handLandmarker.detectForVideo(image, processingOptions, timestampMs)

            HandExtractionOutput(
                vector = toFixedFrameVector(handResult, normalizedRotationDegrees),
                hands = if (includeOverlay) {
                    toWireframeHands(handResult, normalizedRotationDegrees)
                } else {
                    emptyList()
                }
            )
        }
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
        val handednessByHand = result.handedness()

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
        val handednessByHand = result.handedness()

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

    override fun close() {
        handLandmarker.close()
    }

    private enum class HandSlot {
        LEFT,
        RIGHT
    }
}
