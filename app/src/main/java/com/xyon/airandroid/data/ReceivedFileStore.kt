package com.xyon.airandroid.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import com.xyon.airandroid.util.FileNames
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

class ReceivedFileStore(private val context: Context) {

    class Sink internal constructor(
        val uri: Uri,
        val displayName: String,
        val output: OutputStream,
        private val onFinish: () -> Unit,
        private val onAbort: () -> Unit,
    ) {
        private val finished = AtomicBoolean(false)

        fun finish() {
            if (!finished.compareAndSet(false, true)) return
            runCatching { output.flush() }
            runCatching { output.close() }
            onFinish()
        }

        fun abort() {
            if (!finished.compareAndSet(false, true)) return
            runCatching { output.close() }
            onAbort()
        }
    }

    fun hasSpace(bytes: Long): Boolean {
        val target = context.getExternalFilesDir(null) ?: context.filesDir
        val stat = StatFs(target.absolutePath)
        return stat.availableBytes > bytes + MIN_FREE_BYTES
    }

    fun createSink(rawName: String, declaredMime: String): Sink {
        val name = FileNames.sanitize(rawName)
        val mime = safeMime(name, declaredMime)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val media = createMediaStoreSink(name, mime)
            if (media != null) return media
        }
        return createAppFileSink(name, mime)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createMediaStoreSink(name: String, mime: String): Sink? {
        val isImage = mime.startsWith("image/")
        val isVideo = mime.startsWith("video/")
        val collection = when {
            isImage -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            isVideo -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else -> MediaStore.Downloads.EXTERNAL_CONTENT_URI
        }
        val relativePath = when {
            isImage -> Environment.DIRECTORY_PICTURES + "/AirAndroid"
            isVideo -> Environment.DIRECTORY_MOVIES + "/AirAndroid"
            else -> Environment.DIRECTORY_DOWNLOADS + "/AirAndroid"
        }

        val displayName = uniqueMediaName(collection, relativePath, name)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        return try {
            val uri = context.contentResolver.insert(collection, values) ?: return null
            val stream = context.contentResolver.openOutputStream(uri) ?: run {
                context.contentResolver.delete(uri, null, null)
                return null
            }
            Sink(
                uri = uri,
                displayName = displayName,
                output = stream,
                onFinish = {
                    val done = ContentValues().apply {
                        put(MediaStore.MediaColumns.IS_PENDING, 0)
                    }
                    runCatching { context.contentResolver.update(uri, done, null, null) }
                },
                onAbort = {
                    runCatching { context.contentResolver.delete(uri, null, null) }
                },
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun uniqueMediaName(collection: Uri, relativePath: String, name: String): String {
        var candidate = name
        var index = 0
        while (index < 1000) {
            val exists = context.contentResolver.query(
                collection,
                arrayOf(MediaStore.MediaColumns._ID),
                "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND " +
                    "${MediaStore.MediaColumns.RELATIVE_PATH}=?",
                arrayOf(candidate, relativePath),
                null,
            )?.use { it.count > 0 } ?: false
            if (!exists) return candidate
            index++
            candidate = FileNames.withSuffix(name, index)
        }
        return FileNames.withSuffix(name, System.currentTimeMillis().toInt())
    }

    private fun createAppFileSink(name: String, mime: String): Sink {
        val root = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            "AirAndroid",
        ).apply { mkdirs() }
        var file = File(root, name)
        var index = 0
        while (file.exists() && index < 1000) {
            index++
            file = File(root, FileNames.withSuffix(name, index))
        }
        val output = FileOutputStream(file)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.files",
            file,
        )
        return Sink(
            uri = uri,
            displayName = file.name,
            output = output,
            onFinish = {},
            onAbort = { runCatching { file.delete() } },
        )
    }

    private fun safeMime(name: String, declaredMime: String): String {
        val extension = name.substringAfterLast('.', "").lowercase()
        val extensionMime = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension)
        if (declaredMime.startsWith("image/") &&
            extensionMime?.startsWith("image/") == true
        ) {
            return declaredMime
        }
        if (declaredMime.startsWith("video/") &&
            extensionMime?.startsWith("video/") == true
        ) {
            return declaredMime
        }
        return when {
            declaredMime == "application/octet-stream" && extensionMime != null -> extensionMime
            declaredMime.isBlank() -> extensionMime ?: "application/octet-stream"
            declaredMime.startsWith("image/") || declaredMime.startsWith("video/") ->
                extensionMime ?: "application/octet-stream"

            else -> declaredMime
        }
    }

    companion object {
        private const val MIN_FREE_BYTES = 100L * 1024 * 1024
    }
}
