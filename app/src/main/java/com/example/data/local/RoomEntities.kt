package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.Post
import com.google.firebase.Timestamp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

@Entity(tableName = "cached_chats")
data class CachedChatEntity(
    @PrimaryKey val chatId: String,
    val participantsJson: String,
    val participantNamesJson: String,
    val participantUsernamesJson: String,
    val participantAvatarColorsJson: String,
    val participantVerifiedTypesJson: String,
    val lastMessageText: String,
    val lastMessageType: String,
    val lastMessageSenderId: String,
    val lastMessageTimestampMs: Long,
    val unreadCountsJson: String
) {
    fun toDomain(): ChatThread = ChatThread(
        id = chatId,
        participants = decodeStringList(participantsJson),
        participantNames = decodeStringMap(participantNamesJson),
        participantUsernames = decodeStringMap(participantUsernamesJson),
        participantAvatarColors = decodeStringMap(participantAvatarColorsJson),
        participantVerifiedTypes = decodeStringMap(participantVerifiedTypesJson),
        lastMessageText = lastMessageText,
        lastMessageType = lastMessageType,
        lastMessageSenderId = lastMessageSenderId,
        lastMessageTimestamp = Timestamp(Date(lastMessageTimestampMs)),
        unreadCounts = decodeLongMap(unreadCountsJson)
    )

    companion object {
        fun fromDomain(thread: ChatThread): CachedChatEntity = CachedChatEntity(
            chatId = thread.id,
            participantsJson = encodeStringList(thread.participants),
            participantNamesJson = encodeStringMap(thread.participantNames),
            participantUsernamesJson = encodeStringMap(thread.participantUsernames),
            participantAvatarColorsJson = encodeStringMap(thread.participantAvatarColors),
            participantVerifiedTypesJson = encodeStringMap(thread.participantVerifiedTypes),
            lastMessageText = thread.lastMessageText,
            lastMessageType = thread.lastMessageType,
            lastMessageSenderId = thread.lastMessageSenderId,
            lastMessageTimestampMs = thread.lastMessageTimestamp?.toDate()?.time ?: System.currentTimeMillis(),
            unreadCountsJson = encodeLongMap(thread.unreadCounts)
        )
    }
}

@Entity(tableName = "cached_messages")
data class CachedMessageEntity(
    @PrimaryKey val messageId: String,
    val chatId: String,
    val participantsJson: String,
    val senderId: String,
    val type: String,
    val textContent: String?,
    val audioBase64OrUrl: String?,
    val audioDurationSec: Int?,
    val waveformJson: String,
    val attachmentName: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentSizeBytes: Long? = null,
    val attachmentData: String? = null,
    val attachmentLocalUri: String? = null,
    val replyToMessageId: String?,
    val replyPreviewText: String?,
    val reactionsJson: String,
    val deletedForJson: String,
    val deletedForEveryone: Boolean,
    val status: String,
    val isQueuedOffline: Boolean,
    val createdAtMs: Long
) {
    fun toDomain(): ChatMessage = ChatMessage(
        id = messageId,
        participants = decodeStringList(participantsJson),
        senderId = senderId,
        type = type,
        textContent = textContent,
        audioBase64OrUrl = audioBase64OrUrl,
        audioDurationSec = audioDurationSec,
        waveform = decodeIntList(waveformJson),
        attachmentName = attachmentName,
        attachmentMimeType = attachmentMimeType,
        attachmentSizeBytes = attachmentSizeBytes,
        attachmentData = attachmentData,
        attachmentLocalUri = attachmentLocalUri,
        replyToMessageId = replyToMessageId,
        replyPreviewText = replyPreviewText,
        reactions = decodeStringMap(reactionsJson),
        deletedFor = decodeStringList(deletedForJson),
        deletedForEveryone = deletedForEveryone,
        status = if (isQueuedOffline) "queued" else status,
        createdAt = Timestamp(Date(createdAtMs)),
        updatedAt = Timestamp(Date(createdAtMs))
    )

    companion object {
        fun fromDomain(
            chatId: String,
            msg: ChatMessage,
            isQueuedOffline: Boolean = false
        ): CachedMessageEntity = CachedMessageEntity(
            messageId = msg.id,
            chatId = chatId,
            participantsJson = encodeStringList(msg.participants),
            senderId = msg.senderId,
            type = msg.type,
            textContent = msg.textContent,
            audioBase64OrUrl = msg.audioBase64OrUrl,
            audioDurationSec = msg.audioDurationSec,
            waveformJson = encodeIntList(msg.waveform),
            attachmentName = msg.attachmentName,
            attachmentMimeType = msg.attachmentMimeType,
            attachmentSizeBytes = msg.attachmentSizeBytes,
            attachmentData = msg.attachmentData,
            attachmentLocalUri = msg.attachmentLocalUri,
            replyToMessageId = msg.replyToMessageId,
            replyPreviewText = msg.replyPreviewText,
            reactionsJson = encodeStringMap(msg.reactions),
            deletedForJson = encodeStringList(msg.deletedFor),
            deletedForEveryone = msg.deletedForEveryone,
            status = msg.status,
            isQueuedOffline = isQueuedOffline,
            createdAtMs = msg.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        )
    }
}

@Entity(tableName = "cached_posts")
data class CachedPostEntity(
    @PrimaryKey val postId: String,
    val authorId: String,
    val authorName: String,
    val authorUsername: String,
    val authorVerifiedType: String,
    val authorAvatarColor: String,
    val content: String,
    val hashtagsJson: String,
    val mentionsJson: String,
    val likesCount: Long,
    val repostsCount: Long,
    val repliesCount: Long,
    val visibility: String,
    val createdAtMs: Long
) {
    fun toDomain(): Post = Post(
        id = postId,
        authorId = authorId,
        authorName = authorName,
        authorUsername = authorUsername,
        authorVerifiedType = authorVerifiedType,
        authorAvatarColor = authorAvatarColor,
        content = content,
        hashtags = decodeStringList(hashtagsJson),
        mentions = decodeStringList(mentionsJson),
        likesCount = likesCount,
        repostsCount = repostsCount,
        repliesCount = repliesCount,
        visibility = visibility,
        createdAt = Timestamp(Date(createdAtMs)),
        updatedAt = Timestamp(Date(createdAtMs))
    )

    companion object {
        fun fromDomain(post: Post): CachedPostEntity = CachedPostEntity(
            postId = post.id,
            authorId = post.authorId,
            authorName = post.authorName,
            authorUsername = post.authorUsername,
            authorVerifiedType = post.authorVerifiedType,
            authorAvatarColor = post.authorAvatarColor,
            content = post.content,
            hashtagsJson = encodeStringList(post.hashtags),
            mentionsJson = encodeStringList(post.mentions),
            likesCount = post.likesCount,
            repostsCount = post.repostsCount,
            repliesCount = post.repliesCount,
            visibility = post.visibility,
            createdAtMs = post.createdAt?.toDate()?.time ?: System.currentTimeMillis()
        )
    }
}

private fun encodeStringList(list: List<String>): String = JSONArray(list).toString()

private fun decodeStringList(json: String): List<String> = try {
    val arr = JSONArray(json)
    List(arr.length()) { idx -> arr.optString(idx) }
} catch (_: Exception) {
    emptyList()
}

private fun encodeIntList(list: List<Int>): String = JSONArray(list).toString()

private fun decodeIntList(json: String): List<Int> = try {
    val arr = JSONArray(json)
    List(arr.length()) { idx -> arr.optInt(idx) }
} catch (_: Exception) {
    emptyList()
}

private fun encodeStringMap(map: Map<String, String>): String = JSONObject(map).toString()

private fun decodeStringMap(json: String): Map<String, String> = try {
    val obj = JSONObject(json)
    buildMap {
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            put(k, obj.optString(k))
        }
    }
} catch (_: Exception) {
    emptyMap()
}

private fun encodeLongMap(map: Map<String, Long>): String =
    JSONObject(map.mapValues { it.value }).toString()

private fun decodeLongMap(json: String): Map<String, Long> = try {
    val obj = JSONObject(json)
    buildMap {
        val keys = obj.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            put(k, obj.optLong(k))
        }
    }
} catch (_: Exception) {
    emptyMap()
}
