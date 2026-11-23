package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.FormatAlignLeft
import androidx.compose.material.icons.automirrored.outlined.FormatAlignRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StoryViewer
import com.example.data.model.TextStory
import com.example.data.model.UserProfile
import com.example.ui.components.MonogramAvatar
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.StoryPresets
import com.example.ui.theme.storyFontFamily
import com.example.util.formatRelativeTime
import com.example.util.parseHexColor
import kotlinx.coroutines.delay

@Composable
fun StoryCreatorScreen(
    isPublishing: Boolean,
    onClose: () -> Unit,
    onPublish: (text: String, bgStart: String, bgEnd: String, fontStyle: String, textAlign: String) -> Unit
) {
    BackHandler(onBack = onClose)
    var storyText by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf(StoryPresets.first()) }
    var fontStyle by remember { mutableStateOf("cairo") }
    var textAlignKey by remember { mutableStateOf("center") }

    val textAlign = when (textAlignKey) {
        "start" -> TextAlign.Start
        "end" -> TextAlign.End
        else -> TextAlign.Center
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        parseHexColor(selectedPreset.startHex),
                        parseHexColor(selectedPreset.endHex)
                    )
                )
            )
            .safeDrawingPadding()
            .imePadding()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                }
                Text(
                    text = "${storyText.length}/350",
                    color = Color.White.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.labelLarge
                )
                Button(
                    onClick = {
                        onPublish(
                            storyText,
                            selectedPreset.startHex,
                            selectedPreset.endHex,
                            fontStyle,
                            textAlignKey
                        )
                    },
                    enabled = storyText.isNotBlank() && !isPublishing,
                    modifier = Modifier.testTag("publish_story_button")
                ) {
                    Text("Share 24h")
                }
            }

            // Central typographic canvas
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                if (storyText.isEmpty()) {
                    Text(
                        text = "اكتب خاطرتك هنا…\nWrite a 24h text story…",
                        color = Color.White.copy(alpha = 0.5f),
                        fontFamily = storyFontFamily(fontStyle),
                        fontSize = 24.sp,
                        textAlign = textAlign
                    )
                }
                BasicTextField(
                    value = storyText,
                    onValueChange = { if (it.length <= 350) storyText = it },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontFamily = storyFontFamily(fontStyle),
                        fontSize = 26.sp,
                        lineHeight = 36.sp,
                        textAlign = textAlign
                    ),
                    cursorBrush = SolidColor(Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("story_text_input")
                )
            }

            // Styling controls
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("cairo" to "Cairo", "amiri" to "Amiri", "tajawal" to "Tajawal", "mono" to "Mono").forEach { (key, label) ->
                        FilterChip(
                            selected = fontStyle == key,
                            onClick = { fontStyle = key },
                            label = { Text(label) }
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StoryPresets.forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(parseHexColor(preset.startHex), parseHexColor(preset.endHex))
                                        )
                                    )
                                    .border(
                                        width = if (selectedPreset.id == preset.id) 2.5.dp else 1.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedPreset = preset }
                            )
                        }
                    }
                    Row {
                        IconButton(onClick = { textAlignKey = "start" }) {
                            Icon(Icons.AutoMirrored.Outlined.FormatAlignLeft, "Align start", tint = Color.White)
                        }
                        IconButton(onClick = { textAlignKey = "center" }) {
                            Icon(Icons.Outlined.FormatAlignCenter, "Align center", tint = Color.White)
                        }
                        IconButton(onClick = { textAlignKey = "end" }) {
                            Icon(Icons.AutoMirrored.Outlined.FormatAlignRight, "Align end", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerScreen(
    stories: List<TextStory>,
    initialIndex: Int,
    currentUser: UserProfile,
    viewers: List<StoryViewer>,
    onMarkViewed: (TextStory) -> Unit,
    onLoadViewers: (String) -> Unit,
    onSendQuickReply: (TextStory, String) -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)
    if (stories.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, stories.lastIndex)) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var quickReplyText by remember { mutableStateOf("") }
    var showViewersSheet by remember { mutableStateOf(false) }

    val story = stories[currentIndex]

    LaunchedEffect(story.id) {
        progress = 0f
        onMarkViewed(story)
        if (story.authorId == currentUser.uid) {
            onLoadViewers(story.id)
        }
    }

    LaunchedEffect(story.id, isPaused, showViewersSheet) {
        while (!isPaused && !showViewersSheet) {
            delay(50L)
            progress += 0.01f
            if (progress >= 1f) {
                if (currentIndex < stories.lastIndex) {
                    currentIndex++
                } else {
                    onClose()
                }
                break
            }
        }
    }

    val textAlign = when (story.textAlign) {
        "start" -> TextAlign.Start
        "end" -> TextAlign.End
        else -> TextAlign.Center
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        parseHexColor(story.backgroundColor),
                        parseHexColor(story.gradientEndColor.ifBlank { story.backgroundColor })
                    )
                )
            )
            .pointerInput(currentIndex) {
                detectTapGestures(
                    onPress = {
                        isPaused = true
                        tryAwaitRelease()
                        isPaused = false
                    },
                    onTap = { offset ->
                        if (offset.x < size.width * 0.35f) {
                            if (currentIndex > 0) currentIndex-- else progress = 0f
                        } else {
                            if (currentIndex < stories.lastIndex) currentIndex++ else onClose()
                        }
                    }
                )
            }
            .safeDrawingPadding()
            .imePadding()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Segmented progress bars
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                stories.forEachIndexed { idx, _ ->
                    val segProgress = when {
                        idx < currentIndex -> 1f
                        idx == currentIndex -> progress
                        else -> 0f
                    }
                    LinearProgressIndicator(
                        progress = { segProgress },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonogramAvatar(
                        name = story.authorName,
                        colorHex = story.authorAvatarColor,
                        size = 38.dp
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = story.authorName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(4.dp))
                            VerifiedBadge(story.authorVerifiedType)
                        }
                        Text(
                            text = formatRelativeTime(story.createdAt),
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Close story", tint = Color.White)
                }
            }

            // Story text
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp)
            ) {
                Text(
                    text = story.text,
                    color = Color.White,
                    fontFamily = storyFontFamily(story.fontStyle),
                    fontSize = 26.sp,
                    lineHeight = 38.sp,
                    textAlign = textAlign,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Bottom bar: Viewer list for author, or quick DM reply for others
            if (story.authorId == currentUser.uid) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable {
                            onLoadViewers(story.id)
                            showViewersSheet = true
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Outlined.Visibility, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${story.viewsCount} viewers",
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = quickReplyText,
                        onValueChange = { if (it.length <= 200) quickReplyText = it },
                        placeholder = { Text("Send quick reply to DM…", color = Color.White.copy(alpha = 0.7f)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("story_quick_reply_input")
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (quickReplyText.isNotBlank()) {
                                onSendQuickReply(story, quickReplyText)
                                quickReplyText = ""
                            }
                        },
                        enabled = quickReplyText.isNotBlank()
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send reply", tint = Color.White)
                    }
                }
            }
        }
    }

    if (showViewersSheet) {
        ModalBottomSheet(onDismissRequest = { showViewersSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Story Viewers (${viewers.size})", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(12.dp))
                if (viewers.isEmpty()) {
                    Text("No views recorded yet.", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(viewers, key = { it.viewerId }) { viewer ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            ) {
                                MonogramAvatar(name = viewer.viewerName, colorHex = viewer.viewerAvatarColor, size = 38.dp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(viewer.viewerName, style = MaterialTheme.typography.titleMedium)
                                    Text("@${viewer.viewerUsername}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
