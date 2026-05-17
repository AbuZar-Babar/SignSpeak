package com.example.kotlinfrontend

import android.content.Context
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.components.containers.Category
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import java.io.Closeable

data class FaceBlendshapeScore(
    val name: String,
    val score: Float
)

data class FaceExpressionSignal(
    val label: String,
    val confidence: Float,
    val topScores: List<FaceBlendshapeScore>
)

class FaceBlendshapeAnalyzer(context: Context) : Closeable {
    companion object {
        private const val MODEL_ASSET_PATH = "face_landmarker.task"
    }

    private val faceLandmarker: FaceLandmarker = createFaceLandmarker(context)

    fun analyze(
        image: MPImage,
        processingOptions: ImageProcessingOptions,
        timestampMs: Long
    ): FaceExpressionSignal? {
        val result = faceLandmarker.detectForVideo(image, processingOptions, timestampMs)
        val blendshapes = result.faceBlendshapes()
            .orElse(emptyList())
            .firstOrNull()
            ?: return null

        val scoreMap = blendshapes.associateBy { it.categoryName() }
        val signals = listOf(
            compositeExpression(
                name = "Amazed",
                scoreMap = scoreMap,
                weightedKeys = listOf(
                    "eyeWideLeft" to 0.25f,
                    "eyeWideRight" to 0.25f,
                    "jawOpen" to 0.25f,
                    "browInnerUp" to 0.15f,
                    "browOuterUpLeft" to 0.05f,
                    "browOuterUpRight" to 0.05f
                )
            ),
            compositeExpression(
                name = "Sad",
                scoreMap = scoreMap,
                weightedKeys = listOf(
                    "mouthFrownLeft" to 0.25f,
                    "mouthFrownRight" to 0.25f,
                    "browInnerUp" to 0.2f,
                    "browDownLeft" to 0.15f,
                    "browDownRight" to 0.15f
                )
            ),
            expression("Smile", scoreMap, "mouthSmileLeft", "mouthSmileRight"),
            singleExpression("Smile left", scoreMap, "mouthSmileLeft"),
            singleExpression("Smile right", scoreMap, "mouthSmileRight"),
            expression("Frown", scoreMap, "mouthFrownLeft", "mouthFrownRight"),
            expression("Mouth open", scoreMap, "jawOpen", "mouthFunnel", "mouthPucker"),
            singleExpression("Jaw open", scoreMap, "jawOpen"),
            singleExpression("Mouth pucker", scoreMap, "mouthPucker"),
            singleExpression("Mouth funnel", scoreMap, "mouthFunnel"),
            expression("Mouth stretch", scoreMap, "mouthStretchLeft", "mouthStretchRight"),
            expression("Mouth press", scoreMap, "mouthPressLeft", "mouthPressRight"),
            expression("Upper lip raise", scoreMap, "mouthUpperUpLeft", "mouthUpperUpRight"),
            expression("Lower lip down", scoreMap, "mouthLowerDownLeft", "mouthLowerDownRight"),
            expression("Brows raised", scoreMap, "browInnerUp", "browOuterUpLeft", "browOuterUpRight"),
            singleExpression("Inner brow raise", scoreMap, "browInnerUp"),
            expression("Outer brows raised", scoreMap, "browOuterUpLeft", "browOuterUpRight"),
            expression("Brows lowered", scoreMap, "browDownLeft", "browDownRight"),
            singleExpression("Left brow lowered", scoreMap, "browDownLeft"),
            singleExpression("Right brow lowered", scoreMap, "browDownRight"),
            expression("Eyes squint", scoreMap, "eyeSquintLeft", "eyeSquintRight"),
            expression("Eyes wide", scoreMap, "eyeWideLeft", "eyeWideRight"),
            expression("Blink", scoreMap, "eyeBlinkLeft", "eyeBlinkRight"),
            singleExpression("Left wink", scoreMap, "eyeBlinkLeft"),
            singleExpression("Right wink", scoreMap, "eyeBlinkRight"),
            expression("Cheek raise", scoreMap, "cheekSquintLeft", "cheekSquintRight"),
            singleExpression("Cheek puff", scoreMap, "cheekPuff"),
            expression("Nose sneer", scoreMap, "noseSneerLeft", "noseSneerRight"),
            singleExpression("Jaw left", scoreMap, "jawLeft"),
            singleExpression("Jaw right", scoreMap, "jawRight"),
            singleExpression("Jaw forward", scoreMap, "jawForward")
        ).sortedByDescending { it.score }

        val best = signals.firstOrNull()
        val topBlendshapes = blendshapes
            .filter { it.score() > 0.05f }
            .sortedByDescending { it.score() }
            .take(5)
            .map { FaceBlendshapeScore(name = it.categoryName(), score = it.score()) }

        return if (best == null || best.score < 0.25f) {
            FaceExpressionSignal(
                label = "Neutral",
                confidence = best?.score ?: 0f,
                topScores = topBlendshapes
            )
        } else {
            FaceExpressionSignal(
                label = best.name,
                confidence = best.score.coerceIn(0f, 1f),
                topScores = topBlendshapes
            )
        }
    }

    private fun expression(
        name: String,
        scores: Map<String, Category>,
        vararg keys: String
    ): FaceBlendshapeScore {
        val average = keys
            .mapNotNull { key -> scores[key]?.score() }
            .takeIf { it.isNotEmpty() }
            ?.average()
            ?.toFloat()
            ?: 0f
        return FaceBlendshapeScore(name = name, score = average)
    }

    private fun singleExpression(
        name: String,
        scores: Map<String, Category>,
        key: String
    ): FaceBlendshapeScore {
        return FaceBlendshapeScore(
            name = name,
            score = scores[key]?.score() ?: 0f
        )
    }

    private fun compositeExpression(
        name: String,
        scoreMap: Map<String, Category>,
        weightedKeys: List<Pair<String, Float>>
    ): FaceBlendshapeScore {
        val score = weightedKeys.sumOf { (key, weight) ->
            ((scoreMap[key]?.score() ?: 0f) * weight).toDouble()
        }.toFloat()
        return FaceBlendshapeScore(name = name, score = score)
    }

    private fun createFaceLandmarker(context: Context): FaceLandmarker {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET_PATH)
            .build()

        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)
            .setNumFaces(1)
            .setOutputFaceBlendshapes(true)
            .setOutputFacialTransformationMatrixes(false)
            .setMinFaceDetectionConfidence(0.5f)
            .setMinFacePresenceConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .build()

        return try {
            FaceLandmarker.createFromOptions(context, options)
        } catch (error: Exception) {
            throw IllegalStateException(
                "Unable to load '$MODEL_ASSET_PATH'. Place it in app/src/main/assets/.",
                error
            )
        }
    }

    override fun close() {
        faceLandmarker.close()
    }
}
