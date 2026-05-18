package com.example.kotlinfrontend

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import java.util.Locale

data class JsonSaveResult(
    val uri: Uri,
    val fileName: String,
    val displayPath: String
)

class JsonWriter(private val context: Context) {
    companion object {
        const val FORMAT = "signspeak-landmarks-v1"
        const val SOURCE = "mobile"
        const val SEQUENCE_LENGTH = 60
        const val FRAME_DIM = 126
        private const val DOWNLOADS_FOLDER = "SignSpeakCollector"
    }

    fun writeToDownloads(
        action: String,
        frameVectors: List<FloatArray>,
        sequenceLength: Int = SEQUENCE_LENGTH,
        dim: Int = FRAME_DIM
    ): JsonSaveResult {
        val safeAction = normalizeAction(action)
        validateShape(safeAction, frameVectors, sequenceLength, dim)

        val payload = buildJsonPayload(
            action = safeAction,
            sequenceLength = sequenceLength,
            dim = dim,
            frameVectors = frameVectors
        )
        val fileName = buildFileName(safeAction)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            writeWithMediaStore(safeAction, fileName, payload)
        } else {
            writeLegacy(safeAction, fileName, payload)
        }
    }

    private fun normalizeAction(raw: String): String {
        return raw
            .trim()
            .lowercase(Locale.US)
            .replace(Regex("\\s+"), "_")
            .replace(Regex("[^a-z0-9_-]"), "")
    }

    private fun validateShape(
        action: String,
        frameVectors: List<FloatArray>,
        sequenceLength: Int,
        dim: Int
    ) {
        require(action.isNotBlank()) { "Action name is required." }
        require(frameVectors.size == sequenceLength) {
            "Expected $sequenceLength frames, received ${frameVectors.size}."
        }
        frameVectors.forEachIndexed { index, frame ->
            require(frame.size == dim) {
                "Frame $index has ${frame.size} values, expected $dim."
            }
        }
    }

    private fun buildJsonPayload(
        action: String,
        sequenceLength: Int,
        dim: Int,
        frameVectors: List<FloatArray>
    ): String {
        val framesArray = JSONArray()
        frameVectors.forEach { frame ->
            val frameArray = JSONArray()
            frame.forEach { value ->
                frameArray.put(value.toDouble())
            }
            framesArray.put(frameArray)
        }

        return JSONObject().apply {
            put("format", FORMAT)
            put("source", SOURCE)
            put("action", action)
            put("sequence_length", sequenceLength)
            put("dim", dim)
            put("created_at", Instant.now().toString())
            put("frames", framesArray)
        }.toString(2)
    }

    private fun buildFileName(action: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
        return "${action}_${timestamp}.json"
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun writeWithMediaStore(
        action: String,
        fileName: String,
        payload: String
    ): JsonSaveResult {
        val resolver = context.contentResolver
        val relativePath = "${Environment.DIRECTORY_DOWNLOADS}/$DOWNLOADS_FOLDER/$action"
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Failed to create Downloads entry.")

        try {
            resolver.openOutputStream(itemUri)?.use { stream ->
                stream.write(payload.toByteArray(Charsets.UTF_8))
            } ?: throw IOException("Failed to open output stream for $itemUri")

            resolver.update(
                itemUri,
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null
            )
            return JsonSaveResult(
                uri = itemUri,
                fileName = fileName,
                displayPath = "$relativePath/$fileName"
            )
        } catch (error: Exception) {
            resolver.delete(itemUri, null, null)
            throw error
        }
    }

    @Suppress("DEPRECATION")
    private fun writeLegacy(
        action: String,
        fileName: String,
        payload: String
    ): JsonSaveResult {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        val outputDir = File(File(downloadsDir, DOWNLOADS_FOLDER), action)
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw IOException("Unable to create ${outputDir.absolutePath}.")
        }

        val outputFile = File(outputDir, fileName)
        FileOutputStream(outputFile).use { stream ->
            stream.write(payload.toByteArray(Charsets.UTF_8))
        }
        return JsonSaveResult(
            uri = Uri.fromFile(outputFile),
            fileName = fileName,
            displayPath = outputFile.absolutePath
        )
    }
}
