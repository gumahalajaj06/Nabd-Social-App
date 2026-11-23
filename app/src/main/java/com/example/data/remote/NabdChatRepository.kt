package com.example.data.remote

import com.example.data.local.CachedChatEntity
import com.example.data.local.CachedMessageEntity
import com.example.data.local.NabdDao
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class NabdChatRepository(
    private val db: FirebaseFirestore,
    private val nabdDao: NabdDao,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing chats.")

    fun deterministicChatId(uid1: String, uid2: String): String {
        val sorted = listOf(uid1, uid2).sorted()
        return "${sorted[0]}_${sorted[1]}"
    }

    fun observeCachedChats(): Flow<List<ChatThread>> =
        nabdDao.observeChats().map { list -> list.map { it.toDomain() } }

    fun observeRemoteChats(uid: String): Flow<List<ChatThread>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("chats")
                .whereArrayContains("participants", uid)
                .snapshots()
                .map { snap ->
                    val threads = snap.toObjects(ChatThread::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.lastMessageTimestamp ?: Timestamp(0, 0) }
                    nabdDao.upsertChats(threads.map { CachedChatEntity.fromDomain(it) })
                    threads
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "chats", uid)
                    throw error
                }
        )
    }

    fun observeCachedMessages(chatId: String): Flow<List<ChatMessage>> =
        nabdDao.observeMessages(chatId).map { list -> list.map { it.toDomain() } }

    fun observeRemoteMessages(chatId: String, uid: String): Flow<List<ChatMessage>> = flow {
        if (uid.isBlank() || chatId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("chats").document(chatId).collection("messages")
                .whereArrayContains("participants", uid)
                .snapshots()
                .map { snap ->
                    val remoteMessages = snap.toObjects(ChatMessage::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.createdAt ?: Timestamp(0, 0) }
                    nabdDao.upsertMessages(remoteMessages.map { CachedMessageEntity.fromDomain(chatId, it, isQueuedOffline = false) })
                    remoteMessages
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "chats/$chatId/messages", uid)
                    throw error
                }
        )
    }

    suspend fun getOrCreateChatThread(me: UserProfile, peer: UserProfile): Result<String> = try {
        val uid = requireUserId()
        val chatId = deterministicChatId(uid, peer.uid)
        val chatRef = db.collection("chats").document(chatId)
        val snap = chatRef.get().await()
        if (!snap.exists()) {
            val participants = listOf(uid, peer.uid).sorted()
            val payload = mapOf(
                "participants" to participants,
                "participantNames" to mapOf(uid to me.name, peer.uid to peer.name),
                "participantUsernames" to mapOf(uid to me.username, peer.uid to peer.username),
                "participantAvatarColors" to mapOf(uid to me.avatarColor, peer.uid to peer.avatarColor),
                "participantVerifiedTypes" to mapOf(uid to me.verifiedType, peer.uid to peer.verifiedType),
                "lastMessageText" to "",
                "lastMessageType" to "text",
                "lastMessageSenderId" to uid,
                "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                "unreadCounts" to mapOf(uid to 0L, peer.uid to 0L),
                "typingUsers" to mapOf(uid to false, peer.uid to false),
                "recordingUsers" to mapOf(uid to false, peer.uid to false),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            chatRef.set(payload).await()
        }
        Result.success(chatId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "chats")
        Result.failure(e)
    }

    suspend fun sendTextOrVoiceMessage(
        chatId: String,
        participants: List<String>,
        type: String,
        textContent: String?,
        audioBase64: String?,
        audioDurationSec: Int?,
        waveform: List<Int>,
        replyToMessageId: String?,
        replyPreviewText: String?,
        isOnline: Boolean
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val nowTs = Timestamp.now()
        val localMsg = ChatMessage(
            id = msgId,
            participants = participants,
            senderId = uid,
            type = type,
            textContent = textContent,
            audioBase64OrUrl = audioBase64,
            audioDurationSec = audioDurationSec,
            waveform = waveform,
            replyToMessageId = replyToMessageId,
            replyPreviewText = replyPreviewText,
            reactions = emptyMap(),
            deletedFor = emptyList(),
            deletedForEveryone = false,
            status = if (isOnline) "sent" else "queued",
            createdAt = nowTs,
            updatedAt = nowTs
        )

        // Always cache locally in Room immediately so UI renders instantaneously offline or online
        nabdDao.upsertMessage(CachedMessageEntity.fromDomain(chatId, localMsg, isQueuedOffline = !isOnline))

        if (!isOnline) {
            return Result.success(Unit)
        }
        return pushMessageToFirestore(chatId, localMsg)
    }

    suspend fun sendAttachmentMessage(
        chatId: String,
        participants: List<String>,
        type: String,
        caption: String?,
        fileName: String,
        mimeType: String,
        sizeBytes: Long,
        base64Data: String?,
        localUri: String?,
        replyToMessageId: String?,
        replyPreviewText: String?,
        isOnline: Boolean
    ): Result<Unit> {
        val uid = try {
            requireUserId()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val nowTs = Timestamp.now()
        val localMsg = ChatMessage(
            id = msgId,
            participants = participants,
            senderId = uid,
            type = type,
            textContent = caption?.takeIf { it.isNotBlank() },
            attachmentName = fileName.take(180),
            attachmentMimeType = mimeType.take(128),
            attachmentSizeBytes = sizeBytes,
            attachmentData = base64Data,
            attachmentLocalUri = localUri,
            replyToMessageId = replyToMessageId,
            replyPreviewText = replyPreviewText,
            reactions = emptyMap(),
            deletedFor = emptyList(),
            deletedForEveryone = false,
            status = if (isOnline) "sent" else "queued",
            createdAt = nowTs,
            updatedAt = nowTs
        )

        nabdDao.upsertMessage(CachedMessageEntity.fromDomain(chatId, localMsg, isQueuedOffline = !isOnline))
        if (!isOnline) {
            return Result.success(Unit)
        }
        return pushMessageToFirestore(chatId, localMsg)
    }

    suspend fun flushPendingMessages() {
        val uid = auth.currentUser?.uid ?: return
        val queued = nabdDao.getQueuedOfflineMessages()
        for (entity in queued) {
            val msg = entity.toDomain()
            if (msg.senderId == uid) {
                pushMessageToFirestore(entity.chatId, msg)
            }
        }
    }

    private suspend fun pushMessageToFirestore(chatId: String, msg: ChatMessage): Result<Unit> = try {
        val uid = requireUserId()
        val msgRef = db.collection("chats").document(chatId).collection("messages").document(msg.id)
        val payload = mutableMapOf<String, Any?>(
            "participants" to msg.participants,
            "senderId" to uid,
            "type" to msg.type,
            "textContent" to msg.textContent?.take(2000),
            "audioBase64OrUrl" to msg.audioBase64OrUrl,
            "audioDurationSec" to msg.audioDurationSec,
            "waveform" to msg.waveform,
            "attachmentName" to msg.attachmentName?.take(180),
            "attachmentMimeType" to msg.attachmentMimeType?.take(128),
            "attachmentSizeBytes" to msg.attachmentSizeBytes,
            "attachmentData" to msg.attachmentData?.take(679999),
            "replyToMessageId" to msg.replyToMessageId,
            "replyPreviewText" to msg.replyPreviewText?.take(200),
            "reactions" to msg.reactions,
            "deletedFor" to msg.deletedFor,
            "deletedForEveryone" to false,
            "status" to "sent",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>

        msgRef.set(payload).await()
        nabdDao.markMessageSynced(msg.id, "sent")

        val peerUid = msg.participants.firstOrNull { it != uid } ?: uid
        val preview = when (msg.type) {
            "voice" -> "🎙 Voice note (${msg.audioDurationSec ?: 1}s)"
            "image" -> "🖼️ ${msg.attachmentName ?: "Photo"}"
            "video" -> "🎬 ${msg.attachmentName ?: "Video"}"
            "file" -> "📎 ${msg.attachmentName ?: "File"}"
            else -> msg.textContent ?: ""
        }
        db.collection("chats").document(chatId).update(
            mapOf(
                "lastMessageText" to preview.take(300),
                "lastMessageType" to msg.type,
                "lastMessageSenderId" to uid,
                "lastMessageTimestamp" to FieldValue.serverTimestamp(),
                "unreadCounts.$peerUid" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "chats/$chatId/messages/${msg.id}")
        // Keep queued in Room so next connectivity restoration retries
        nabdDao.upsertMessage(CachedMessageEntity.fromDomain(chatId, msg, isQueuedOffline = true))
        Result.failure(e)
    }

    suspend fun setTypingOrRecording(chatId: String, typing: Boolean? = null, recording: Boolean? = null) {
        try {
            val uid = requireUserId()
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            if (typing != null) updates["typingUsers.$uid"] = typing
            if (recording != null) updates["recordingUsers.$uid"] = recording
            db.collection("chats").document(chatId).update(updates).await()
        } catch (_: Exception) {
        }
    }

    suspend fun markChatMessagesRead(chatId: String, messages: List<ChatMessage>) {
        try {
            val uid = requireUserId()
            db.collection("chats").document(chatId).update(
                mapOf(
                    "unreadCounts.$uid" to 0L,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            messages.filter { it.senderId != uid && it.status != "read" }.forEach { msg ->
                db.collection("chats").document(chatId).collection("messages").document(msg.id)
                    .update(
                        mapOf(
                            "status" to "read",
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            }
        } catch (_: Exception) {
        }
    }

    suspend fun reactToMessage(chatId: String, message: ChatMessage, emoji: String): Result<Unit> = try {
        val uid = requireUserId()
        val updated = message.reactions.toMutableMap().apply {
            if (this[uid] == emoji) remove(uid) else put(uid, emoji)
        }
        db.collection("chats").document(chatId).collection("messages").document(message.id)
            .update(
                mapOf(
                    "reactions" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/${message.id}")
        Result.failure(e)
    }

    suspend fun deleteMessage(chatId: String, message: ChatMessage, forEveryone: Boolean): Result<Unit> = try {
        val uid = requireUserId()
        val ref = db.collection("chats").document(chatId).collection("messages").document(message.id)
        if (forEveryone && message.senderId == uid) {
            ref.update(
                mapOf(
                    "deletedForEveryone" to true,
                    "textContent" to "This message was deleted",
                    "audioBase64OrUrl" to "",
                    "attachmentData" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            val updated = (message.deletedFor + uid).distinct().take(2)
            ref.update(
                mapOf(
                    "deletedFor" to updated,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "chats/$chatId/messages/${message.id}")
        Result.failure(e)
    }

    private fun logStreamError(error: Throwable, op: OperationType, path: String, expectedUid: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }
}
