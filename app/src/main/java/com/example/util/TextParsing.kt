package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.google.firebase.Timestamp
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val HASHTAG_REGEX = Regex("#([\\p{L}\\p{N}_]{1,40})")
private val MENTION_REGEX = Regex("@([a-zA-Z0-9_]{3,24})")
private val URL_REGEX = Regex("(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)")

fun extractHashtags(text: String): List<String> =
    HASHTAG_REGEX.findAll(text)
        .map { it.groupValues[1].lowercase() }
        .distinct()
        .take(15)
        .toList()

fun extractMentions(text: String): List<String> =
    MENTION_REGEX.findAll(text)
        .map { it.groupValues[1].lowercase() }
        .distinct()
        .take(15)
        .toList()

fun buildRichPostText(
    text: String,
    accentColor: Color,
    urlColor: Color
): AnnotatedString = buildAnnotatedString {
    append(text)
    HASHTAG_REGEX.findAll(text).forEach { match ->
        addStyle(
            style = SpanStyle(color = accentColor, fontWeight = FontWeight.SemiBold),
            start = match.range.first,
            end = match.range.last + 1
        )
        addStringAnnotation(
            tag = "HASHTAG",
            annotation = match.groupValues[1].lowercase(),
            start = match.range.first,
            end = match.range.last + 1
        )
    }
    MENTION_REGEX.findAll(text).forEach { match ->
        addStyle(
            style = SpanStyle(color = accentColor, fontWeight = FontWeight.Medium),
            start = match.range.first,
            end = match.range.last + 1
        )
        addStringAnnotation(
            tag = "MENTION",
            annotation = match.groupValues[1].lowercase(),
            start = match.range.first,
            end = match.range.last + 1
        )
    }
    URL_REGEX.findAll(text).forEach { match ->
        addStyle(
            style = SpanStyle(
                color = urlColor,
                textDecoration = TextDecoration.Underline
            ),
            start = match.range.first,
            end = match.range.last + 1
        )
        addStringAnnotation(
            tag = "URL",
            annotation = match.value,
            start = match.range.first,
            end = match.range.last + 1
        )
    }
}

fun formatRelativeTime(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: return "now"
    val diffSec = ((System.currentTimeMillis() - date.time) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 60 -> "now"
        diffSec < 3600 -> "${diffSec / 60}m"
        diffSec < 86400 -> "${diffSec / 3600}h"
        diffSec < 604800 -> "${diffSec / 86400}d"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}

fun formatJoinDate(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: return "Joined Nabd"
    return "Joined " + SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
}

fun formatClockTime(timestamp: Timestamp?): String {
    val date = timestamp?.toDate() ?: Date()
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}

fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.US, "%d:%02d", mins, secs)
}

fun parseHexColor(hex: String, fallback: Color = Color(0xFF0D9488)): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: Exception) {
    fallback
}

fun initialsFromName(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "ن"
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> "${parts.first().first()}${parts.last().first()}".uppercase()
    }
}

fun copyTextToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun sharePostAsTextCard(context: Context, post: Post) {
    val cardText = buildString {
        appendLine("━━ Nabd · نَبْض ━━")
        appendLine("${post.authorName} (@${post.authorUsername})")
        appendLine()
        appendLine(post.content)
        appendLine()
        appendLine("👍 ${post.effectiveReactionsCount}  ↻ ${post.repostsCount}  💬 ${post.effectiveCommentsCount}")
        append("https://nabd.app/p/${post.id}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, cardText)
    }
    context.startActivity(Intent.createChooser(intent, "Share Pulse"))
}

fun shareProfileLink(context: Context, user: UserProfile) {
    val text = buildString {
        appendLine("${user.name} (@${user.username}) on Nabd · نَبْض")
        if (user.bio.isNotBlank()) appendLine(user.bio)
        append("https://nabd.app/@${user.username}")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Profile"))
}

fun sharePlainTextExport(context: Context, title: String, content: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(intent, title))
}

fun sharePostAsTextImageCard(context: Context, post: Post) {
    try {
        val width = 1080
        val pad = 76f
        val innerWidth = (width - pad * 2).toInt()
        val typefaceCairo = try {
            ResourcesCompat.getFont(context, R.font.cairo) ?: Typeface.DEFAULT
        } catch (_: Exception) {
            Typeface.DEFAULT
        }

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#F1F5F9")
            textSize = 44f
            typeface = typefaceCairo
        }

        val bodyLayout = StaticLayout.Builder
            .obtain(post.content, 0, post.content.length, bodyPaint, innerWidth - 48)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(10f, 1.15f)
            .build()

        val cardHeight = (460 + bodyLayout.height).coerceIn(680, 1920)
        val bitmap = Bitmap.createBitmap(width, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Background gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), cardHeight.toFloat(),
                intArrayOf(
                    android.graphics.Color.parseColor("#0B111A"),
                    android.graphics.Color.parseColor("#111C2B"),
                    android.graphics.Color.parseColor("#0D2D34")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), cardHeight.toFloat(), bgPaint)

        // Elevated card container
        val cardRect = RectF(44f, 44f, width - 44f, cardHeight - 44f)
        val cardBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#162131")
        }
        val cardBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = android.graphics.Color.parseColor("#263548")
        }
        canvas.drawRoundRect(cardRect, 42f, 42f, cardBgPaint)
        canvas.drawRoundRect(cardRect, 42f, 42f, cardBorderPaint)

        // Avatar circle
        val avatarRadius = 46f
        val avatarCx = pad + avatarRadius
        val avatarCy = 132f
        val avatarColorInt = try {
            android.graphics.Color.parseColor(post.authorAvatarColor)
        } catch (_: Exception) {
            android.graphics.Color.parseColor("#0D9488")
        }
        val avatarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = avatarColorInt }
        canvas.drawCircle(avatarCx, avatarCy, avatarRadius, avatarPaint)

        val initialsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 34f
            typeface = Typeface.create(typefaceCairo, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(initialsFromName(post.authorName), avatarCx, avatarCy + 12f, initialsPaint)

        // Author name + badge + handle
        val verifiedSuffix = when (post.authorVerifiedType) {
            "personal" -> " ✓"
            "business" -> " ★"
            "government" -> " 🏛"
            else -> ""
        }
        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 38f
            typeface = Typeface.create(typefaceCairo, Typeface.BOLD)
        }
        canvas.drawText("${post.authorName}$verifiedSuffix", avatarCx + avatarRadius + 24f, 124f, namePaint)

        val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 28f
            typeface = typefaceCairo
        }
        val editedTag = if (post.isEdited) " · Edited" else ""
        canvas.drawText(
            "@${post.authorUsername} · ${formatRelativeTime(post.createdAt)}$editedTag",
            avatarCx + avatarRadius + 24f,
            164f,
            handlePaint
        )

        // Body text
        canvas.save()
        canvas.translate(pad, 232f)
        bodyLayout.draw(canvas)
        canvas.restore()

        // Divider + Footer Watermark
        val footerY = cardHeight - 132f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#2A394D")
            strokeWidth = 2f
        }
        canvas.drawLine(pad, footerY - 34f, width - pad, footerY - 34f, linePaint)

        val statsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#CBD5E1")
            textSize = 28f
            typeface = typefaceCairo
        }
        canvas.drawText(
            "👍 ${post.effectiveReactionsCount}   💬 ${post.effectiveCommentsCount}   ↻ ${post.repostsCount}",
            pad,
            footerY + 12f,
            statsPaint
        )

        val watermarkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#2DD4BF")
            textSize = 30f
            typeface = Typeface.create(typefaceCairo, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("nabd.app · نَبْض", width - pad, footerY + 12f, watermarkPaint)

        val cacheFolder = File(context.cacheDir, "shared_cards").apply { mkdirs() }
        val file = File(cacheFolder, "nabd_card_${post.id.ifBlank { System.currentTimeMillis() }}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
        }
        bitmap.recycle()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "${post.authorName} (@${post.authorUsername}) on Nabd · https://nabd.app/p/${post.id}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share as Text Image Card"))
    } catch (_: Exception) {
        sharePostAsTextCard(context, post)
    }
}
