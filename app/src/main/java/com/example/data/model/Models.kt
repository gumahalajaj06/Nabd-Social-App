package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class VerifiedType(val key: String) {
    NONE("none"),
    PERSONAL("personal"),
    BUSINESS("business"),
    GOVERNMENT("government");

    companion object {
        fun fromKey(raw: String?): VerifiedType =
            entries.firstOrNull { it.key == raw } ?: NONE
    }
}

enum class ReactionType(
    val key: String,
    val emoji: String,
    val labelEn: String,
    val labelAr: String,
    val colorHex: String
) {
    LIKE("like", "👍", "Like", "إعجاب", "#0D9488"),
    LOVE("love", "❤️", "Love", "أحببته", "#F43F5E"),
    HAHA("haha", "😂", "Haha", "أضحكني", "#F59E0B"),
    WOW("wow", "😮", "Wow", "أدهشني", "#3B82F6"),
    SAD("sad", "😢", "Sad", "أحزنني", "#6366F1"),
    ANGRY("angry", "😡", "Angry", "أغضبني", "#EA580C");

    companion object {
        fun fromKey(raw: String?): ReactionType =
            entries.firstOrNull { it.key == raw } ?: LIKE
    }
}

enum class CommentSortOrder(val label: String) {
    TOP("Top"),
    NEWEST("Newest"),
    OLDEST("Oldest")
}

@Keep
@IgnoreExtraProperties
data class UserPrivacySettings(
    val allowMentionsFrom: String = "everyone", // "everyone" | "following" | "nobody"
    val allowDMsFrom: String = "everyone",      // "everyone" | "following" | "nobody"
    val allowVoiceNotesInDMs: Boolean = true,
    val showReadReceipts: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val hideLikesTab: Boolean = false,
    val appLockEnabled: Boolean = false
) {
    fun toMap(): Map<String, Any> = mapOf(
        "allowMentionsFrom" to allowMentionsFrom,
        "allowDMsFrom" to allowDMsFrom,
        "allowVoiceNotesInDMs" to allowVoiceNotesInDMs,
        "showReadReceipts" to showReadReceipts,
        "showOnlineStatus" to showOnlineStatus,
        "hideLikesTab" to hideLikesTab,
        "appLockEnabled" to appLockEnabled
    )
}

@Keep
@IgnoreExtraProperties
data class UserNotificationSettings(
    val pushEnabled: Boolean = true,
    val likes: Boolean = true,
    val reactions: Boolean = true,
    val reposts: Boolean = true,
    val replies: Boolean = true,
    val mentions: Boolean = true,
    val follows: Boolean = true,
    val dms: Boolean = true,
    val qualityFilter: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietStart: String = "22:00",
    val quietEnd: String = "07:00"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "pushEnabled" to pushEnabled,
        "likes" to likes,
        "reactions" to reactions,
        "reposts" to reposts,
        "replies" to replies,
        "mentions" to mentions,
        "follows" to follows,
        "dms" to dms,
        "qualityFilter" to qualityFilter,
        "quietHoursEnabled" to quietHoursEnabled,
        "quietStart" to quietStart,
        "quietEnd" to quietEnd
    )
}

@Keep
@IgnoreExtraProperties
data class UserAppearancePreferences(
    val themeMode: String = "system", // "light" | "dark" | "oled" | "system"
    val accentColor: String = "#0D9488",
    val fontFamily: String = "cairo", // "cairo" | "amiri" | "tajawal" | "jakarta" | "mono"
    val fontScale: Double = 1.0,
    val language: String = "en",      // "en" | "ar"
    val voiceQuality: String = "standard", // "low" | "standard"
    val showStoryHighlights: Boolean = true
) {
    fun toMap(): Map<String, Any> = mapOf(
        "themeMode" to themeMode,
        "accentColor" to accentColor,
        "fontFamily" to fontFamily,
        "fontScale" to fontScale,
        "language" to language,
        "voiceQuality" to voiceQuality,
        "showStoryHighlights" to showStoryHighlights
    )
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val uid: String = "",
    val name: String = "",
    val username: String = "",
    val bio: String = "",
    val location: String = "",
    val website: String = "",
    val category: String = "",
    val roleTitle: String = "",
    val pinnedNote: String = "",
    val avatarColor: String = "#0D9488",
    val avatarPattern: String = "solid", // "solid" | "rings" | "dots" | "diagonal" | "pulse"
    val verifiedType: String = "none",
    val showVerifiedBadge: Boolean = true,
    val officialLabel: String = "",
    val pinnedPostId: String = "",
    val pinMode: String = "manual", // "manual" | "top_engaged"
    val postsCount: Long = 0L,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    @get:PropertyName("isPrivate") @PropertyName("isPrivate")
    val isPrivate: Boolean = false,
    val accountStatus: String = "active",
    val privacy: UserPrivacySettings = UserPrivacySettings(),
    val notifications: UserNotificationSettings = UserNotificationSettings(),
    val preferences: UserAppearancePreferences = UserAppearancePreferences(),
    val usernameChangedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val maxPostLength: Int
        get() = if (VerifiedType.fromKey(verifiedType) != VerifiedType.NONE) 2000 else 500

    val displayedVerifiedType: String
        get() = if (showVerifiedBadge) verifiedType else "none"
}

@Keep
@IgnoreExtraProperties
data class VerificationRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val username: String = "",
    val requestedType: String = "personal",
    val organizationName: String = "",
    val reason: String = "",
    val status: String = "pending",
    @ServerTimestamp val submittedAt: Timestamp? = null,
    @ServerTimestamp val reviewedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostPoll(
    val options: List<String> = emptyList(),
    val votes: Map<String, String> = emptyMap(),
    val expiresAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorVerifiedType: String = "none",
    val authorAvatarColor: String = "#0D9488",
    val content: String = "",
    val threadParentId: String? = null,
    val threadRootId: String? = null,
    val quotePostId: String? = null,
    val quoteAuthorName: String? = null,
    val quoteAuthorUsername: String? = null,
    val quoteContent: String? = null,
    val replyToPostId: String? = null,
    val pinnedReplyId: String? = null,
    val authorLikedReply: Boolean = false,
    val poll: PostPoll? = null,
    val hashtags: List<String> = emptyList(),
    val mentions: List<String> = emptyList(),
    val likesCount: Long = 0L,
    val repostsCount: Long = 0L,
    val repliesCount: Long = 0L,
    val reactionsCount: Map<String, Long> = emptyMap(),
    val totalReactionsCount: Long = 0L,
    val viewsCount: Long = 0L,
    val commentsCount: Long = 0L,
    val engagementScore: Double = 0.0,
    @get:PropertyName("isEdited") @PropertyName("isEdited")
    val isEdited: Boolean = false,
    val editedAt: Timestamp? = null,
    val visibility: String = "public",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val effectiveReactionsCount: Long
        get() = maxOf(totalReactionsCount, likesCount, reactionsCount.values.sum())

    val effectiveCommentsCount: Long
        get() = maxOf(commentsCount, repliesCount)

    fun rawEngagementTotal(): Double =
        effectiveReactionsCount * 3.0 + effectiveCommentsCount * 4.0 + repostsCount * 3.0 + viewsCount * 0.25

    fun engagementScore(nowMs: Long = System.currentTimeMillis()): Double {
        val createdMs = createdAt?.toDate()?.time ?: nowMs
        val ageHours = ((nowMs - createdMs).coerceAtLeast(0L)) / 3_600_000.0
        val engagement = effectiveReactionsCount * 2.0 + repostsCount * 3.0 + effectiveCommentsCount * 2.5 + viewsCount * 0.1 + 1.0
        return engagement / Math.pow(ageHours + 2.0, 1.35)
    }
}

@Keep
@IgnoreExtraProperties
data class PostReaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    val type: String = "like",
    val userName: String = "",
    val userUsername: String = "",
    val userAvatarColor: String = "#0D9488",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val parentCommentId: String? = null,
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorVerifiedType: String = "none",
    val authorAvatarColor: String = "#0D9488",
    val content: String = "",
    val reactionsCount: Map<String, Long> = emptyMap(),
    val totalReactionsCount: Long = 0L,
    @get:PropertyName("isEdited") @PropertyName("isEdited")
    val isEdited: Boolean = false,
    val editedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TextStory(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorVerifiedType: String = "none",
    val authorAvatarColor: String = "#0D9488",
    val text: String = "",
    val backgroundColor: String = "#0F172A",
    val gradientEndColor: String = "#0D9488",
    val fontStyle: String = "cairo",
    val textAlign: String = "center",
    val viewsCount: Long = 0L,
    @get:PropertyName("isActive") @PropertyName("isActive")
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    val expiresAt: Timestamp? = null
) {
    fun isUnexpired(nowMs: Long = System.currentTimeMillis()): Boolean {
        val exp = expiresAt?.toDate()?.time ?: return false
        return isActive && exp > nowMs
    }
}

@Keep
@IgnoreExtraProperties
data class StoryViewer(
    @DocumentId val id: String = "",
    val viewerId: String = "",
    val storyAuthorId: String = "",
    val viewerName: String = "",
    val viewerUsername: String = "",
    val viewerAvatarColor: String = "#0D9488",
    @ServerTimestamp val viewedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChatThread(
    @DocumentId val id: String = "",
    val participants: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val participantUsernames: Map<String, String> = emptyMap(),
    val participantAvatarColors: Map<String, String> = emptyMap(),
    val participantVerifiedTypes: Map<String, String> = emptyMap(),
    val lastMessageText: String = "",
    val lastMessageType: String = "text",
    val lastMessageSenderId: String = "",
    @ServerTimestamp val lastMessageTimestamp: Timestamp? = null,
    val unreadCounts: Map<String, Long> = emptyMap(),
    val typingUsers: Map<String, Boolean> = emptyMap(),
    val recordingUsers: Map<String, Boolean> = emptyMap(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun peerUid(myUid: String): String =
        participants.firstOrNull { it != myUid } ?: myUid

    fun peerName(myUid: String): String =
        participantNames[peerUid(myUid)] ?: "User"

    fun peerUsername(myUid: String): String =
        participantUsernames[peerUid(myUid)] ?: "user"

    fun peerAvatarColor(myUid: String): String =
        participantAvatarColors[peerUid(myUid)] ?: "#0D9488"

    fun peerVerifiedType(myUid: String): String =
        participantVerifiedTypes[peerUid(myUid)] ?: "none"
}

@Keep
@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val participants: List<String> = emptyList(),
    val senderId: String = "",
    val type: String = "text", // "text" | "voice" | "image" | "video" | "file"
    val textContent: String? = null,
    val audioBase64OrUrl: String? = null,
    val audioDurationSec: Int? = null,
    val waveform: List<Int> = emptyList(),
    val attachmentName: String? = null,
    val attachmentMimeType: String? = null,
    val attachmentSizeBytes: Long? = null,
    val attachmentData: String? = null,
    val attachmentLocalUri: String? = null,
    val replyToMessageId: String? = null,
    val replyPreviewText: String? = null,
    val reactions: Map<String, String> = emptyMap(),
    val deletedFor: List<String> = emptyList(),
    val deletedForEveryone: Boolean = false,
    val status: String = "sent",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val targetUserId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val actorUsername: String = "",
    val actorAvatarColor: String = "#0D9488",
    val actorVerifiedType: String = "none",
    val type: String = "like",
    val postId: String? = null,
    val previewText: String = "",
    @get:PropertyName("isRead") @PropertyName("isRead")
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FollowRequest(
    @DocumentId val id: String = "",
    val requesterUid: String = "",
    val requesterName: String = "",
    val requesterUsername: String = "",
    val requesterAvatarColor: String = "#0D9488",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class MutedWordEntry(
    val word: String = "",
    val expiresAt: Timestamp? = null
) {
    fun isCurrentlyActive(nowMs: Long = System.currentTimeMillis()): Boolean =
        expiresAt == null || expiresAt.toDate().time > nowMs
}

data class TrendingHashtag(
    val tag: String,
    val postCount: Int
)
