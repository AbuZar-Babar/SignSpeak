package com.example.kotlinfrontend

import android.content.Context
import android.os.SystemClock
import org.json.JSONArray
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.min

data class FaceEmotionScore(
    val label: String,
    val confidence: Float
)

data class FaceEmotionResult(
    val label: String,
    val confidence: Float,
    val topScores: List<FaceEmotionScore>,
    val processingTimeMs: Long
)

class TfliteFaceEmotionClassifier(
    context: Context,
    useQuantizedModel: Boolean = false
) : Closeable {
    companion object {
        private const val INPUT_SIZE = 48
        private const val INPUT_CHANNELS = 1
        private const val MODEL_ASSET_PATH = "face_model.tflite"
        private const val QUANTIZED_MODEL_ASSET_PATH = "face_model_quantized.tflite"
        private const val LABELS_ASSET_PATH = "labels_face.json"
    }

    private val interpreter: Interpreter
    private val labels: List<String>
    private val outputClassCount: Int
    private val reusableInput = Array(1) {
        Array(INPUT_SIZE) {
            Array(INPUT_SIZE) {
                FloatArray(INPUT_CHANNELS)
            }
        }
    }
    private lateinit var reusableOutput: Array<FloatArray>

    init {
        val assetPath = if (useQuantizedModel) QUANTIZED_MODEL_ASSET_PATH else MODEL_ASSET_PATH
        interpreter = Interpreter(
            loadModelFile(context, assetPath),
            Interpreter.Options().apply { setNumThreads(2) }
        )

        val inputShape = interpreter.getInputTensor(0).shape()
        require(
            inputShape.contentEquals(intArrayOf(1, INPUT_SIZE, INPUT_SIZE, INPUT_CHANNELS))
        ) {
            "Unsupported face model input shape: ${inputShape.joinToString(prefix = "[", postfix = "]")}"
        }

        val outputShape = interpreter.getOutputTensor(0).shape()
        outputClassCount = when (outputShape.size) {
            1 -> outputShape[0]
            2 -> outputShape[1]
            else -> throw IllegalStateException(
                "Unsupported face model output shape: ${outputShape.joinToString(prefix = "[", postfix = "]")}"
            )
        }

        labels = loadLabels(context)
        require(labels.isNotEmpty()) { "Label file '$LABELS_ASSET_PATH' is empty." }
        require(outputClassCount > 0) { "Face model output classes must be greater than zero." }
        reusableOutput = Array(1) { FloatArray(outputClassCount) }
    }

    fun predict(facePixels: FloatArray): FaceEmotionResult {
        require(facePixels.size == INPUT_SIZE * INPUT_SIZE) {
            "Face input has ${facePixels.size} values; expected ${INPUT_SIZE * INPUT_SIZE}."
        }

        var index = 0
        for (row in 0 until INPUT_SIZE) {
            for (column in 0 until INPUT_SIZE) {
                reusableInput[0][row][column][0] = facePixels[index]
                index += 1
            }
        }

        java.util.Arrays.fill(reusableOutput[0], 0f)
        val startedAt = SystemClock.elapsedRealtime()
        interpreter.run(reusableInput, reusableOutput)
        val processingTimeMs = SystemClock.elapsedRealtime() - startedAt

        val probabilities = reusableOutput[0]
        val labelCount = min(labels.size, probabilities.size)
        if (labelCount == 0) {
            return FaceEmotionResult(
                label = "unknown",
                confidence = 0f,
                topScores = emptyList(),
                processingTimeMs = processingTimeMs
            )
        }

        var bestIndex = 0
        for (i in 1 until labelCount) {
            if (probabilities[i] > probabilities[bestIndex]) {
                bestIndex = i
            }
        }

        val sortedScores = (0 until labelCount)
            .map { scoreIndex ->
                FaceEmotionScore(
                    label = labels[scoreIndex],
                    confidence = probabilities[scoreIndex]
                )
            }
            .sortedByDescending { it.confidence }

        return FaceEmotionResult(
            label = labels[bestIndex],
            confidence = probabilities[bestIndex],
            topScores = sortedScores,
            processingTimeMs = processingTimeMs
        )
    }

    private fun loadModelFile(context: Context, assetPath: String): MappedByteBuffer {
        return try {
            val fileDescriptor = context.assets.openFd(assetPath)
            FileInputStream(fileDescriptor.fileDescriptor).channel.use { channel ->
                channel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fileDescriptor.startOffset,
                    fileDescriptor.declaredLength
                )
            }
        } catch (error: Exception) {
            throw IllegalStateException(
                "Unable to load face model asset '$assetPath'. Place it in app/src/main/assets/.",
                error
            )
        }
    }

    private fun loadLabels(context: Context): List<String> {
        val rawJson = try {
            context.assets.open(LABELS_ASSET_PATH).bufferedReader().use { it.readText() }
        } catch (error: Exception) {
            throw IllegalStateException(
                "Unable to load face labels asset '$LABELS_ASSET_PATH'. Place it in app/src/main/assets/.",
                error
            )
        }
        val jsonArray = JSONArray(rawJson)
        return List(jsonArray.length()) { index -> jsonArray.optString(index, "").trim() }
            .filter { it.isNotEmpty() }
    }

    override fun close() {
        interpreter.close()
    }
}
