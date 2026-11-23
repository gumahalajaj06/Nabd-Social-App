package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.PlaybackUiState
import com.example.audio.VoiceAudioManager
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.UserProfile
import com.example.data.remote.NabdChatRepository
import com.example.util.ConnectivityObserver
import com.example.util.prepareChatAttachment
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(
    private val chatRepo: NabdChatRepository,
    private val audioManager: VoiceAudioManager,
    private val connectivityObserver: ConnectivityObserver,
    val currentUserId: String
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
    val liveAmplitudes: StateFlow<List<Int>> = audioManager.liveAmplitudes
    val recordingDurationSec: StateFlow<Int> = audioManager.recordingDurationSec
    val playbackState: StateFlow<PlaybackUiState> = audioManager.playbackState

    private val _chatEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val chatEvents: SharedFlow<String> = _chatEvents.asSharedFlow()

    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    private val _replyingTo = MutableStateFlow<ChatMessage?>(null)
    val replyingTo: StateFlow<ChatMessage?> = _replyingTo.asStateFlow()

    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private val cachedChats = chatRepo.observeCachedChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val chats: StateFlow<List<ChatThread>> = chatRepo.observeRemoteChats(currentUserId)
        .combine(cachedChats) { remote, cached ->
            if (remote.isNotEmpty()) remote else cached
        }
        .catch { error ->
            unwrapFirestoreError(error, "chats") {
                emit(cachedChats.value)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val activeChatMessages: StateFlow<List<ChatMessage>> = _activeChatId
        .flatMapLatest { chatId ->
            if (chatId.isNullOrBlank()) {
                flowOf(emptyList())
            } else {
                val cached = chatRepo.observeCachedMessages(chatId)
                val remote = chatRepo.observeRemoteMessages(chatId, currentUserId)
                    .catch { error ->
                        unwrapFirestoreError(error, "messages") {
                            // Keep cached flow active offline
                        }
                    }
                combine(cached, remote) { localList, remoteList ->
                    val merged = LinkedHashMap<String, ChatMessage>()
                    remoteList.forEach { merged[it.id] = it }
                    localList.forEach { msg ->
                        if (msg.status == "queued" || !merged.containsKey(msg.id)) {
                            merged[msg.id] = msg
                        }
                    }
                    merged.values
                        .filter { currentUserId !in it.deletedFor }
                        .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                if (online) {
                    chatRepo.flushPendingMessages()
                }
            }
        }
    }

    fun selectChat(chatId: String?) {
        _activeChatId.value = chatId
        _replyingTo.value = null
    }

    fun startOrOpenChat(me: UserProfile, peer: UserProfile, onOpened: (String) -> Unit) {
        viewModelScope.launch {
            val chatId = chatRepo.deterministicChatId(me.uid, peer.uid)
            val res = chatRepo.getOrCreateChatThread(me, peer)
            res.onSuccess { id ->
                _activeChatId.value = id
                onOpened(id)
            }.onFailure { err ->
                // Even if offline, allow opening deterministic chatId if cached
                if (!isOnline.value) {
                    _activeChatId.value = chatId
                    onOpened(chatId)
                } else {
                    _chatEvents.tryEmit(err.localizedMessage ?: "Failed to open chat")
                }
            }
        }
    }

    fun sendQuickStoryReply(me: UserProfile, author: UserProfile, replyText: String) {
        val clean = replyText.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            chatRepo.getOrCreateChatThread(me, author).onSuccess { chatId ->
                val participants = listOf(me.uid, author.uid).sorted()
                chatRepo.sendTextOrVoiceMessage(
                    chatId = chatId,
                    participants = participants,
                    type = "text",
                    textContent = clean,
                    audioBase64 = null,
                    audioDurationSec = null,
                    waveform = emptyList(),
                    replyToMessageId = null,
                    replyPreviewText = "Replied to story",
                    isOnline = isOnline.value
                )
                _chatEvents.tryEmit("Reply sent to @${author.username}")
            }
        }
    }

    fun setReplyingTo(msg: ChatMessage?) {
        _replyingTo.value = msg
    }

    fun sendTextMessage(chatId: String, participants: List<String>, text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val reply = _replyingTo.value
        _replyingTo.value = null
        viewModelScope.launch {
            chatRepo.setTypingOrRecording(chatId, typing = false)
            chatRepo.sendTextOrVoiceMessage(
                chatId = chatId,
                participants = participants,
                type = "text",
                textContent = clean,
                audioBase64 = null,
                audioDurationSec = null,
                waveform = emptyList(),
                replyToMessageId = reply?.id,
                replyPreviewText = reply?.let { if (it.type == "voice") "🎙 Voice note" else it.textContent?.take(80) },
                isOnline = isOnline.value
            )
        }
    }

    fun startVoiceRecording(chatId: String) {
        if (audioManager.startRecording(viewModelScope)) {
            _isRecordingVoice.value = true
            viewModelScope.launch {
                chatRepo.setTypingOrRecording(chatId, recording = true)
            }
        } else {
            _chatEvents.tryEmit("Unable to start microphone")
        }
    }

    fun cancelVoiceRecording(chatId: String) {
        audioManager.cancelRecording()
        _isRecordingVoice.value = false
        viewModelScope.launch {
            chatRepo.setTypingOrRecording(chatId, recording = false)
        }
    }

    fun stopAndSendVoiceNote(chatId: String, participants: List<String>) {
        _isRecordingVoice.value = false
        val note = audioManager.stopAndEncode()
        viewModelScope.launch {
            chatRepo.setTypingOrRecording(chatId, recording = false)
            if (note == null) {
                _chatEvents.tryEmit("Voice note too short or exceeded 490KB limit")
                return@launch
            }
            val reply = _replyingTo.value
            _replyingTo.value = null
            chatRepo.sendTextOrVoiceMessage(
                chatId = chatId,
                participants = participants,
                type = "voice",
                textContent = null,
                audioBase64 = note.base64Audio,
                audioDurationSec = note.durationSec,
                waveform = note.waveform,
                replyToMessageId = reply?.id,
                replyPreviewText = reply?.let { formatReplyPreview(it) },
                isOnline = isOnline.value
            )
        }
    }

    fun sendUriAttachment(
        context: Context,
        chatId: String,
        participants: List<String>,
        uri: Uri,
        caption: String? = null
    ) {
        val reply = _replyingTo.value
        _replyingTo.value = null
        viewModelScope.launch {
            val prepared = prepareChatAttachment(context, uri)
            if (prepared == null) {
                _chatEvents.tryEmit("Could not read selected file")
                return@launch
            }
            chatRepo.sendAttachmentMessage(
                chatId = chatId,
                participants = participants,
                type = prepared.type,
                caption = caption,
                fileName = prepared.fileName,
                mimeType = prepared.mimeType,
                sizeBytes = prepared.sizeBytes,
                base64Data = prepared.base64Data,
                localUri = prepared.localCachePath,
                replyToMessageId = reply?.id,
                replyPreviewText = reply?.let { formatReplyPreview(it) },
                isOnline = isOnline.value
            ).onFailure { err ->
                _chatEvents.tryEmit(err.localizedMessage ?: "Attachment saved locally")
            }
        }
    }

    private fun formatReplyPreview(msg: ChatMessage): String = when (msg.type) {
        "voice" -> "🎙 Voice note"
        "image" -> "🖼️ ${msg.attachmentName ?: "Photo"}"
        "video" -> "🎬 ${msg.attachmentName ?: "Video"}"
        "file" -> "📎 ${msg.attachmentName ?: "File"}"
        else -> msg.textContent?.take(80) ?: ""
    }

    fun togglePlayVoice(messageId: String, base64Audio: String) {
        audioManager.togglePlayVoice(messageId, base64Audio, viewModelScope)
    }

    fun cyclePlaybackSpeed() {
        audioManager.cyclePlaybackSpeed()
    }

    fun setTyping(chatId: String, typing: Boolean) {
        viewModelScope.launch {
            chatRepo.setTypingOrRecording(chatId, typing = typing)
        }
    }

    fun markMessagesRead(chatId: String, messages: List<ChatMessage>) {
        if (!isOnline.value) return
        viewModelScope.launch {
            chatRepo.markChatMessagesRead(chatId, messages)
        }
    }

    fun reactToMessage(chatId: String, message: ChatMessage, emoji: String) {
        viewModelScope.launch {
            chatRepo.reactToMessage(chatId, message, emoji)
        }
    }

    fun deleteMessage(chatId: String, message: ChatMessage, forEveryone: Boolean) {
        viewModelScope.launch {
            chatRepo.deleteMessage(chatId, message, forEveryone)
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioManager.stopPlayback()
        audioManager.cancelRecording()
    }

    private suspend inline fun unwrapFirestoreError(
        error: Throwable,
        tag: String,
        onFallback: suspend (String) -> Unit
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w("ChatVM", "Error observing $tag", rootError)
        onFallback(rootError.localizedMessage ?: "Failed to load $tag")
    }
}
