package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.audio.PlaybackUiState
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.UserProfile
import com.example.ui.components.MonogramAvatar
import com.example.ui.components.NabdEmptyState
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.LocalNabdColors
import com.example.util.LocalNabdStrings
import com.example.util.decodeAttachmentImageBitmap
import com.example.util.formatClockTime
import com.example.util.formatDuration
import com.example.util.formatFileSize
import com.example.util.formatRelativeTime
import com.example.util.openAttachmentWithSystem
import com.example.util.shareAttachmentWithSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsListScreen(
    currentUser: UserProfile,
    chats: List<ChatThread>,
    activeUsers: List<UserProfile>,
    isOnline: Boolean,
    onOpenChat: (ChatThread) -> Unit,
    onStartChatWithUser: (UserProfile) -> Unit
) {
    val strings = LocalNabdStrings.current
    var showNewChatSheet by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(title = { Text(strings.navChats) })
            OfflineStatusBanner(isOnline = isOnline)

            if (chats.isEmpty()) {
                NabdEmptyState(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    title = "No conversations yet",
                    subtitle = "Start an offline-first text, voice, media, or file chat with anyone on Nabd."
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 88.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(chats, key = { it.id }) { thread ->
                        val peerName = thread.peerName(currentUser.uid)
                        val peerUsername = thread.peerUsername(currentUser.uid)
                        val peerColor = thread.peerAvatarColor(currentUser.uid)
                        val peerVerified = thread.peerVerifiedType(currentUser.uid)
                        val unread = thread.unreadCounts[currentUser.uid] ?: 0L
                        val peerUid = thread.peerUid(currentUser.uid)
                        val peerTyping = thread.typingUsers[peerUid] == true
                        val peerRecording = thread.recordingUsers[peerUid] == true

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenChat(thread) }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .testTag("chat_thread_${thread.id}")
                        ) {
                            MonogramAvatar(name = peerName, colorHex = peerColor, size = 48.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(peerName, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.width(4.dp))
                                    VerifiedBadge(peerVerified)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "@$peerUsername",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = LocalNabdColors.current.mutedText
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = when {
                                        peerRecording -> "Recording audio…"
                                        peerTyping -> "Typing…"
                                        thread.lastMessageText.isBlank() -> "Tap to send a message"
                                        else -> thread.lastMessageText
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (peerTyping || peerRecording) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        LocalNabdColors.current.mutedText
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatRelativeTime(thread.lastMessageTimestamp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LocalNabdColors.current.mutedText
                                )
                                if (unread > 0) {
                                    Spacer(Modifier.height(4.dp))
                                    Badge { Text(unread.toString()) }
                                }
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = LocalNabdColors.current.hairline)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showNewChatSheet = true },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("new_chat_fab")
        ) {
            Icon(Icons.Filled.AddComment, contentDescription = "New message")
        }
    }

    if (showNewChatSheet) {
        ModalBottomSheet(onDismissRequest = { showNewChatSheet = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Start Conversation", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(10.dp))
                val peers = activeUsers.filter { it.uid != currentUser.uid }
                if (peers.isEmpty()) {
                    Text(
                        "No other users discovered yet.",
                        color = LocalNabdColors.current.mutedText,
                        modifier = Modifier.padding(vertical = 20.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(peers, key = { it.uid }) { peer ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showNewChatSheet = false
                                        onStartChatWithUser(peer)
                                    }
                                    .padding(vertical = 10.dp)
                            ) {
                                MonogramAvatar(name = peer.name, colorHex = peer.avatarColor, size = 42.dp)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(peer.name, style = MaterialTheme.typography.titleMedium)
                                        Spacer(Modifier.width(4.dp))
                                        VerifiedBadge(peer.verifiedType)
                                    }
                                    Text("@${peer.username}", color = LocalNabdColors.current.mutedText)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    chatThread: ChatThread,
    currentUid: String,
    messages: List<ChatMessage>,
    replyingTo: ChatMessage?,
    isOnline: Boolean,
    isRecording: Boolean,
    liveAmplitudes: List<Int>,
    recordingDurationSec: Int,
    playbackState: PlaybackUiState,
    onBack: () -> Unit,
    onTypingChange: (Boolean) -> Unit,
    onSendText: (String) -> Unit,
    onSendAttachment: (Uri) -> Unit,
    onStartRecording: () -> Unit,
    onStopAndSendVoice: () -> Unit,
    onCancelRecording: () -> Unit,
    onTogglePlayVoice: (String, String) -> Unit,
    onCyclePlaybackSpeed: () -> Unit,
    onSetReply: (ChatMessage?) -> Unit,
    onReact: (ChatMessage, String) -> Unit,
    onDeleteMessage: (ChatMessage, Boolean) -> Unit,
    onMarkRead: (List<ChatMessage>) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val strings = LocalNabdStrings.current
    val clipboard = LocalClipboardManager.current
    var textInput by remember { mutableStateOf("") }
    var dragCancelOffset by remember { mutableFloatStateOf(0f) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var previewImageMessage by remember { mutableStateOf<ChatMessage?>(null) }
    val listState = rememberLazyListState()

    val peerUid = chatThread.peerUid(currentUid)
    val peerName = chatThread.peerName(currentUid)
    val peerColor = chatThread.peerAvatarColor(currentUid)
    val peerVerified = chatThread.peerVerifiedType(currentUid)
    val peerTyping = chatThread.typingUsers[peerUid] == true
    val peerRecording = chatThread.recordingUsers[peerUid] == true

    val micPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartRecording()
        }
    }

    // Zero-permission Android Photo Picker for images & videos
    val photoOrVideoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSendAttachment(uri)
        }
    }

    // Zero-permission SAF OpenDocument for all file formats (PDF, DOCX, ZIP, audio, APK, etc.)
    val anyFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onSendAttachment(uri)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
            onMarkRead(messages)
        }
    }

    val queuedCount = messages.count { it.status == "queued" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MonogramAvatar(name = peerName, colorHex = peerColor, size = 36.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(peerName, style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.width(4.dp))
                                VerifiedBadge(peerVerified)
                            }
                            Text(
                                text = when {
                                    peerRecording -> "Recording audio…"
                                    peerTyping -> "Typing…"
                                    isOnline -> "Online · Offline-first synced"
                                    else -> "Offline · Queuing locally"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (peerTyping || peerRecording) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    LocalNabdColors.current.mutedText
                                }
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                border = BorderStroke(0.5.dp, LocalNabdColors.current.hairline),
                modifier = Modifier.fillMaxWidth().imePadding()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    if (replyingTo != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                val replySnippet = when (replyingTo.type) {
                                    "voice" -> "🎙 Voice note"
                                    "image" -> "🖼️ ${replyingTo.attachmentName ?: "Photo"}"
                                    "video" -> "🎬 ${replyingTo.attachmentName ?: "Video"}"
                                    "file" -> "📎 ${replyingTo.attachmentName ?: "File"}"
                                    else -> replyingTo.textContent ?: ""
                                }
                                Text(
                                    text = "Replying: $replySnippet",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onSetReply(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Filled.Close, "Cancel reply", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    if (isRecording) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.error)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = formatDuration(recordingDurationSec),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                            ) {
                                liveAmplitudes.takeLast(20).forEach { amp ->
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height((amp * 0.24f).coerceIn(4f, 24f).dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.error)
                                    )
                                }
                            }
                            TextButton(onClick = onCancelRecording) { Text("Cancel") }
                            IconButton(onClick = onStopAndSendVoice) {
                                Icon(Icons.AutoMirrored.Filled.Send, "Send voice note", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { showAttachmentSheet = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("chat_attach_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AttachFile,
                                    contentDescription = strings.attachAnyFileTitle,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            OutlinedTextField(
                                value = textInput,
                                onValueChange = {
                                    if (it.length <= 2000) {
                                        textInput = it
                                        onTypingChange(it.isNotEmpty())
                                    }
                                },
                                placeholder = { Text("Message…") },
                                maxLines = 4,
                                shape = RoundedCornerShape(22.dp),
                                modifier = Modifier.weight(1f).testTag("chat_text_input")
                            )
                            Spacer(Modifier.width(8.dp))
                            if (textInput.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        onSendText(textInput)
                                        textInput = ""
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .testTag("chat_send_button")
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Send text message",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            } else {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .pointerInput(Unit) {
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    dragCancelOffset = 0f
                                                    val granted = ContextCompat.checkSelfPermission(
                                                        context,
                                                        Manifest.permission.RECORD_AUDIO
                                                    ) == PackageManager.PERMISSION_GRANTED
                                                    if (granted) onStartRecording()
                                                    else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                                },
                                                onDrag = { _, dragAmount ->
                                                    dragCancelOffset += dragAmount.x
                                                    if (dragCancelOffset < -120f) {
                                                        onCancelRecording()
                                                    }
                                                },
                                                onDragEnd = {
                                                    if (dragCancelOffset >= -120f) onStopAndSendVoice()
                                                },
                                                onDragCancel = { onCancelRecording() }
                                            )
                                        }
                                        .clickable {
                                            val granted = ContextCompat.checkSelfPermission(
                                                context,
                                                Manifest.permission.RECORD_AUDIO
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (granted) onStartRecording()
                                            else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                        .testTag("record_voice_button")
                                ) {
                                    Icon(
                                        Icons.Filled.Mic,
                                        contentDescription = "Hold or tap to record voice note",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            OfflineStatusBanner(isOnline = isOnline, queuedCount = queuedCount)

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(messages, key = { it.id }) { msg ->
                    val isMine = msg.senderId == currentUid
                    var showMsgMenu by remember { mutableStateOf(false) }

                    Box(
                        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
                            Surface(
                                shape = RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (isMine) 18.dp else 4.dp,
                                    bottomEnd = if (isMine) 4.dp else 18.dp
                                ),
                                color = if (isMine) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier
                                    .widthIn(max = 306.dp)
                                    .clickable { showMsgMenu = true }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                                    if (!msg.replyPreviewText.isNullOrBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.Black.copy(alpha = 0.14f),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                                        ) {
                                            Text(
                                                text = msg.replyPreviewText,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    when {
                                        msg.deletedForEveryone -> {
                                            Text(
                                                text = "This message was deleted",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else LocalNabdColors.current.mutedText
                                            )
                                        }

                                        msg.type == "voice" -> {
                                            val isPlayingThis = playbackState.activeMessageId == msg.id && playbackState.isPlaying
                                            val wave = msg.waveform.ifEmpty { List(22) { 35 } }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                IconButton(
                                                    onClick = {
                                                        msg.audioBase64OrUrl?.let { onTogglePlayVoice(msg.id, it) }
                                                    },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isMine) Color.White.copy(alpha = 0.22f)
                                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                                        )
                                                ) {
                                                    Icon(
                                                        imageVector = if (isPlayingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                        contentDescription = "Play voice note",
                                                        tint = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                ) {
                                                    wave.take(22).forEachIndexed { i, amp ->
                                                        val barProgress = i.toFloat() / 22f
                                                        val active = playbackState.activeMessageId == msg.id && barProgress <= playbackState.progress
                                                        Box(
                                                            modifier = Modifier
                                                                .width(3.dp)
                                                                .height((amp * 0.22f).coerceIn(5f, 22f).dp)
                                                                .clip(RoundedCornerShape(2.dp))
                                                                .background(
                                                                    when {
                                                                        active && isMine -> Color.White
                                                                        active -> MaterialTheme.colorScheme.primary
                                                                        isMine -> Color.White.copy(alpha = 0.45f)
                                                                        else -> LocalNabdColors.current.mutedText.copy(alpha = 0.4f)
                                                                    }
                                                                )
                                                        )
                                                    }
                                                }
                                                Text(
                                                    text = formatDuration(msg.audioDurationSec ?: 1),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color.Black.copy(alpha = 0.15f),
                                                    modifier = Modifier.clickable { onCyclePlaybackSpeed() }
                                                ) {
                                                    Text(
                                                        text = "${playbackState.speed}x",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }

                                        msg.type == "image" -> {
                                            val imgBitmap = remember(msg.id, msg.attachmentData, msg.attachmentLocalUri) {
                                                decodeAttachmentImageBitmap(msg)
                                            }
                                            if (imgBitmap != null) {
                                                Image(
                                                    bitmap = imgBitmap,
                                                    contentDescription = msg.attachmentName ?: "Shared photo",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(min = 120.dp, max = 230.dp)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .clickable { previewImageMessage = msg }
                                                )
                                                if (!msg.attachmentName.isNullOrBlank()) {
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        text = "${msg.attachmentName} · ${formatFileSize(msg.attachmentSizeBytes)}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f) else LocalNabdColors.current.mutedText,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            } else {
                                                AttachmentFileCard(
                                                    msg = msg,
                                                    isMine = isMine,
                                                    openLabel = strings.openAttachment,
                                                    onOpen = { openAttachmentWithSystem(context, msg) }
                                                )
                                            }
                                        }

                                        msg.type == "video" -> {
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color.Black.copy(alpha = if (isMine) 0.24f else 0.10f),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { openAttachmentWithSystem(context, msg) }
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(10.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.PlayCircleFilled,
                                                        contentDescription = "Play video",
                                                        tint = if (isMine) Color.White else MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(38.dp)
                                                    )
                                                    Spacer(Modifier.width(10.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = msg.attachmentName ?: "Video attachment",
                                                            style = MaterialTheme.typography.labelLarge,
                                                            color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        Text(
                                                            text = "${formatFileSize(msg.attachmentSizeBytes)} · ${(msg.attachmentMimeType ?: "video").uppercase()}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else LocalNabdColors.current.mutedText
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        msg.type == "file" -> {
                                            AttachmentFileCard(
                                                msg = msg,
                                                isMine = isMine,
                                                openLabel = strings.openAttachment,
                                                onOpen = { openAttachmentWithSystem(context, msg) }
                                            )
                                        }

                                        else -> {
                                            Text(
                                                text = msg.textContent ?: "",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.align(Alignment.End)
                                    ) {
                                        Text(
                                            text = formatClockTime(msg.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f) else LocalNabdColors.current.mutedText
                                        )
                                        if (isMine) {
                                            Spacer(Modifier.width(4.dp))
                                            when (msg.status) {
                                                "queued" -> Icon(
                                                    Icons.Outlined.Schedule,
                                                    contentDescription = "Queued offline",
                                                    tint = LocalNabdColors.current.warningAmber,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                "read" -> Icon(
                                                    Icons.Filled.DoneAll,
                                                    contentDescription = "Read",
                                                    tint = LocalNabdColors.current.readBlue,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                "delivered" -> Icon(
                                                    Icons.Filled.DoneAll,
                                                    contentDescription = "Delivered",
                                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                else -> Icon(
                                                    Icons.Filled.Check,
                                                    contentDescription = "Sent",
                                                    tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (msg.reactions.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(0.5.dp, LocalNabdColors.current.hairline),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = msg.reactions.values.distinct().joinToString(" "),
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showMsgMenu,
                                onDismissRequest = { showMsgMenu = false }
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    listOf("❤️", "👍", "😂", "🔥", "🤲").forEach { emoji ->
                                        Text(
                                            text = emoji,
                                            modifier = Modifier
                                                .clickable {
                                                    showMsgMenu = false
                                                    onReact(msg, emoji)
                                                }
                                                .padding(4.dp)
                                        )
                                    }
                                }
                                if (msg.type in listOf("image", "video", "file") && !msg.deletedForEveryone) {
                                    DropdownMenuItem(
                                        text = { Text(strings.openAttachment) },
                                        leadingIcon = { Icon(Icons.AutoMirrored.Outlined.OpenInNew, null) },
                                        onClick = {
                                            showMsgMenu = false
                                            openAttachmentWithSystem(context, msg)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(strings.shareAttachment) },
                                        leadingIcon = { Icon(Icons.Outlined.Share, null) },
                                        onClick = {
                                            showMsgMenu = false
                                            shareAttachmentWithSystem(context, msg)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Reply") },
                                    leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Reply, null) },
                                    onClick = {
                                        showMsgMenu = false
                                        onSetReply(msg)
                                    }
                                )
                                if (!msg.textContent.isNullOrBlank()) {
                                    DropdownMenuItem(
                                        text = { Text("Copy text") },
                                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                        onClick = {
                                            showMsgMenu = false
                                            clipboard.setText(AnnotatedString(msg.textContent))
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Delete for me") },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                                    onClick = {
                                        showMsgMenu = false
                                        onDeleteMessage(msg, false)
                                    }
                                )
                                if (isMine && !msg.deletedForEveryone) {
                                    DropdownMenuItem(
                                        text = { Text("Delete for everyone") },
                                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                                        onClick = {
                                            showMsgMenu = false
                                            onDeleteMessage(msg, true)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAttachmentSheet) {
        ModalBottomSheet(onDismissRequest = { showAttachmentSheet = false }) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, LocalNabdColors.current.hairline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAttachmentSheet = false
                            photoOrVideoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        }
                        .testTag("pick_media_option")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PermMedia,
                            contentDescription = strings.attachMediaTitle,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(strings.attachMediaTitle, style = MaterialTheme.typography.titleMedium)
                            Text(
                                strings.attachPhotoOrVideo,
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalNabdColors.current.mutedText
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, LocalNabdColors.current.hairline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAttachmentSheet = false
                            anyFileLauncher.launch(arrayOf("*/*"))
                        }
                        .testTag("pick_any_file_option")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AttachFile,
                            contentDescription = strings.attachAnyFileTitle,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(strings.attachAnyFileTitle, style = MaterialTheme.typography.titleMedium)
                            Text(
                                strings.attachAllFiles,
                                style = MaterialTheme.typography.bodySmall,
                                color = LocalNabdColors.current.mutedText
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    val fullImageMsg = previewImageMessage
    if (fullImageMsg != null) {
        val fullBmp = remember(fullImageMsg.id) { decodeAttachmentImageBitmap(fullImageMsg) }
        Dialog(onDismissRequest = { previewImageMessage = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, LocalNabdColors.current.hairline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (fullBmp != null) {
                        Image(
                            bitmap = fullBmp,
                            contentDescription = fullImageMsg.attachmentName ?: "Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = fullImageMsg.attachmentName ?: "Photo",
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { shareAttachmentWithSystem(context, fullImageMsg) }) {
                            Text(strings.shareAttachment)
                        }
                        TextButton(onClick = { previewImageMessage = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttachmentFileCard(
    msg: ChatMessage,
    isMine: Boolean,
    openLabel: String,
    onOpen: () -> Unit
) {
    val fileName = msg.attachmentName ?: "file"
    val ext = fileName.substringAfterLast('.', "FILE").uppercase().take(5)
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Black.copy(alpha = if (isMine) 0.20f else 0.08f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isMine) Color.White.copy(alpha = 0.22f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (ext == "FILE") {
                        Icon(
                            imageVector = Icons.Outlined.Description,
                            contentDescription = fileName,
                            tint = if (isMine) Color.White else MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = ext,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isMine) Color.White else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatFileSize(msg.attachmentSizeBytes)} · $openLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else LocalNabdColors.current.mutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
