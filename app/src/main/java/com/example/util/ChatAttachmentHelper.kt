package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import android.webkit.MimeTypeMap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.example.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.max

data class PreparedChatAttachment(
    val type: String, // "image" | "video" | "file"
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val base64Data: String?,
    val localCachePath: String?
)

private const val MAX_INLINE_FIRESTORE_BYTES = 450 * 1024L // Safe under 1MB Firestore doc limit after Base64 expansion (~600KB)

suspend fun prepareChatAttachment(
    context: Context,
    uri: Uri
): PreparedChatAttachment? = withContext(Dispatchers.IO) {
    val resolver = context.contentResolver
    val rawMime = resolver.getType(uri)
    var displayName = "attachment"
    var reportedSize = -1L

    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                    displayName = cursor.getString(nameIdx)
                }
                if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                    reportedSize = cursor.getLong(sizeIdx)
                }
            }
        }

    val safeName = displayName.replace(Regex("[^a-zA-Z0-9._\\-\\u0600-\\u06FF ]"), "_").take(120)
        .ifBlank { "file_${System.currentTimeMillis()}" }

    val ext = safeName.substringAfterLast('.', "").lowercase(Locale.US)
    val guessedMime = rawMime
        ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        ?: "application/octet-stream"

    val msgType = when {
        guessedMime.startsWith("image/") -> "image"
        guessedMime.startsWith("video/") -> "video"
        else -> "file"
    }

    val cacheDir = File(context.cacheDir, "chat_attachments").apply { mkdirs() }
    val cachedFile = File(cacheDir, "${System.currentTimeMillis()}_$safeName")

    var copiedSize = 0L
    try {
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(cachedFile).use { output ->
                val buf = ByteArray(16 * 1024)
                while (true) {
                    val read = input.read(buf)
                    if (read <= 0) break
                    output.write(buf, 0, read)
                    copiedSize += read
                }
            }
        } ?: return@withContext null
    } catch (_: Exception) {
        return@withContext null
    }

    val finalSize = if (copiedSize > 0L) copiedSize else reportedSize.coerceAtLeast(0L)

    val base64Payload: String? = when {
        msgType == "image" -> compressImageFileToBase64(cachedFile)
        finalSize in 1L..MAX_INLINE_FIRESTORE_BYTES -> {
            try {
                val bytes = cachedFile.readBytes()
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } catch (_: Exception) {
                null
            }
        }
        else -> null
    }

    PreparedChatAttachment(
        type = msgType,
        fileName = safeName,
        mimeType = guessedMime,
        sizeBytes = finalSize,
        base64Data = base64Payload,
        localCachePath = cachedFile.absolutePath
    )
}

private fun compressImageFileToBase64(file: File): String? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val maxDim = max(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
        var sampleSize = 1
        while (maxDim / sampleSize > 1280) {
            sampleSize *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, opts) ?: return null
        val out = ByteArrayOutputStream()
        var quality = 82
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        while (out.size() > 360 * 1024 && quality > 35) {
            out.reset()
            quality -= 12
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        bitmap.recycle()
        val bytes = out.toByteArray()
        if (bytes.size > 460 * 1024) null else Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (_: Exception) {
        null
    }
}

fun decodeAttachmentImageBitmap(msg: ChatMessage): ImageBitmap? {
    val b64 = msg.attachmentData
    if (!b64.isNullOrBlank()) {
        try {
            val bytes = Base64.decode(b64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bmp != null) return bmp.asImageBitmap()
        } catch (_: Exception) {
        }
    }
    val localPath = msg.attachmentLocalUri
    if (!localPath.isNullOrBlank()) {
        try {
            val f = File(localPath)
            if (f.exists()) {
                val bmp = BitmapFactory.decodeFile(f.absolutePath)
                if (bmp != null) return bmp.asImageBitmap()
            }
        } catch (_: Exception) {
        }
    }
    return null
}

fun materializeAttachmentFile(context: Context, msg: ChatMessage): File? {
    val existingPath = msg.attachmentLocalUri
    if (!existingPath.isNullOrBlank()) {
        val f = File(existingPath)
        if (f.exists() && f.length() > 0L) return f
    }
    val b64 = msg.attachmentData ?: return null
    return try {
        val bytes = Base64.decode(b64, Base64.DEFAULT)
        val cacheDir = File(context.cacheDir, "chat_attachments").apply { mkdirs() }
        val safeName = (msg.attachmentName ?: "nabd_${msg.id}")
            .replace(Regex("[^a-zA-Z0-9._\\-\\u0600-\\u06FF ]"), "_")
        val outFile = File(cacheDir, "${msg.id}_$safeName")
        FileOutputStream(outFile).use { it.write(bytes) }
        outFile
    } catch (_: Exception) {
        null
    }
}

fun openAttachmentWithSystem(context: Context, msg: ChatMessage): Boolean {
    val file = materializeAttachmentFile(context, msg) ?: return false
    return try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = msg.attachmentMimeType?.takeIf { it.isNotBlank() } ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, msg.attachmentName ?: "Open"))
        true
    } catch (_: Exception) {
        shareAttachmentWithSystem(context, msg)
    }
}

fun shareAttachmentWithSystem(context: Context, msg: ChatMessage): Boolean {
    val file = materializeAttachmentFile(context, msg) ?: return false
    return try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = msg.attachmentMimeType?.takeIf { it.isNotBlank() } ?: "*/*"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, msg.attachmentName ?: "Nabd Attachment")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, msg.attachmentName ?: "Share"))
        true
    } catch (_: Exception) {
        false
    }
}

fun formatFileSize(bytes: Long?): String {
    if (bytes == null || bytes <= 0L) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
        else -> "$bytes B"
    }
}
