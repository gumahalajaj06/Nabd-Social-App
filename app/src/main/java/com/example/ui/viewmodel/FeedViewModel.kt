package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.FollowRequest
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PostReaction
import com.example.data.model.ReactionType
import com.example.data.model.StoryViewer
import com.example.data.model.TextStory
import com.example.data.model.TrendingHashtag
import com.example.data.model.UiState
import com.example.data.model.UserAppearancePreferences
import com.example.data.model.UserNotificationSettings
import com.example.data.model.UserPrivacySettings
import com.example.data.model.UserProfile
import com.example.data.model.VerificationRequest
import com.example.data.remote.NabdSocialRepository
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class FeedViewModel(
    private val socialRepo: NabdSocialRepository,
    val currentUserId: String
) : ViewModel() {

    val isAdmin: Boolean = socialRepo.isCurrentUserAdmin()
    val currentUserEmail: String = socialRepo.currentUserEmail()

    private val _actionMessage = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val actionMessage: SharedFlow<String> = _actionMessage.asSharedFlow()

    val currentUserState: StateFlow<UiState<UserProfile?>> = socialRepo.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error -> unwrapFirestoreError(error, "profile") { emit(UiState.Error(it)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val activeUsers: StateFlow<List<UserProfile>> = socialRepo.observeActiveUsers()
        .catch { error -> unwrapFirestoreError(error, "users") { emit(emptyList()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val cachedPosts = socialRepo.observeCachedPosts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val postsState: StateFlow<UiState<List<Post>>> = socialRepo.observePublicPosts()
        .combine(cachedPosts) { remote, cached ->
            if (remote.isNotEmpty()) remote else cached
        }
        .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
        .catch { error ->
            unwrapFirestoreError(error, "posts") { msg ->
                val cached = cachedPosts.value
                if (cached.isNotEmpty()) emit(UiState.Success(cached)) else emit(UiState.Error(msg))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val storiesState: StateFlow<UiState<List<TextStory>>> = socialRepo.observeActiveStories()
        .map<List<TextStory>, UiState<List<TextStory>>> { UiState.Success(it) }
        .catch { error -> unwrapFirestoreError(error, "stories") { emit(UiState.Error(it)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val followingIds: StateFlow<Set<String>> = socialRepo.observeFollowingIds(currentUserId)
        .catch { error -> unwrapFirestoreError(error, "following") { emit(emptySet()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val followerIds: StateFlow<Set<String>> = socialRepo.observeFollowerIds(currentUserId)
        .catch { error -> unwrapFirestoreError(error, "followers") { emit(emptySet()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val bookmarkIds: StateFlow<Set<String>> = socialRepo.observeBookmarkIds(currentUserId)
        .catch { error -> unwrapFirestoreError(error, "bookmarks") { emit(emptySet()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val blockedIds: StateFlow<Set<String>> = socialRepo.observeBlockedIds(currentUserId)
        .catch { error -> unwrapFirestoreError(error, "blocked") { emit(emptySet()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val mutedUserIds: StateFlow<Set<String>> = socialRepo.observeMutedUserIds(currentUserId)
        .catch { error -> unwrapFirestoreError(error, "muted_users") { emit(emptySet()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val notifyAuthorIds: StateFlow<Set<String>> = socialRepo.observeNotifyAuthorIds(currentUserId)
        .catch { error -> unwrapFirestoreError(error, "notify_authors") { emit(emptySet()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    val mutedWords: StateFlow<List<String>> = socialRepo.observeMutedWords(currentUserId)
        .catch { error -> unwrapFirestoreError(error, "muted_words") { emit(emptyList()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<AppNotification>> = socialRepo.observeNotifications(currentUserId)
        .catch { error -> unwrapFirestoreError(error, "notifications") { emit(emptyList()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val followRequests: StateFlow<List<FollowRequest>> = socialRepo.observeFollowRequests(currentUserId)
        .catch { error -> unwrapFirestoreError(error, "follow_requests") { emit(emptyList()) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val verificationRequests: StateFlow<List<VerificationRequest>> =
        socialRepo.observeVerificationRequests(currentUserId)
            .catch { error -> unwrapFirestoreError(error, "verification_requests") { emit(emptyList()) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _userReactions = MutableStateFlow<Map<String, ReactionType>>(emptyMap())
    val userReactions: StateFlow<Map<String, ReactionType>> = _userReactions.asStateFlow()

    val likedPostIds: StateFlow<Set<String>> = _userReactions
        .map { it.keys }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptySet())

    private val _repostedPostIds = MutableStateFlow<Set<String>>(emptySet())
    val repostedPostIds: StateFlow<Set<String>> = _repostedPostIds.asStateFlow()

    private val _seenStoryIds = MutableStateFlow<Set<String>>(emptySet())
    val seenStoryIds: StateFlow<Set<String>> = _seenStoryIds.asStateFlow()

    private val _storyViewers = MutableStateFlow<List<StoryViewer>>(emptyList())
    val storyViewers: StateFlow<List<StoryViewer>> = _storyViewers.asStateFlow()

    private val _observedCommentPostId = MutableStateFlow<String?>(null)
    val activePostComments: StateFlow<List<PostComment>> = _observedCommentPostId
        .flatMapLatest { postId ->
            if (postId.isNullOrBlank()) flowOf(emptyList())
            else socialRepo.observePostComments(postId)
                .catch { error -> unwrapFirestoreError(error, "comments") { emit(emptyList()) } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _commentReactions = MutableStateFlow<Map<String, ReactionType>>(emptyMap())
    val commentReactions: StateFlow<Map<String, ReactionType>> = _commentReactions.asStateFlow()

    private val _postReactionsBreakdown = MutableStateFlow<List<PostReaction>>(emptyList())
    val postReactionsBreakdown: StateFlow<List<PostReaction>> = _postReactionsBreakdown.asStateFlow()

    private val _cacheStats = MutableStateFlow(Triple(0, 0, 0))
    val cacheStats: StateFlow<Triple<Int, Int, Int>> = _cacheStats.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private var lastHydratedPostIds: Set<String> = emptySet()
    private val viewedPostsThisSession = mutableSetOf<String>()

    fun hydrateReactionStates(posts: List<Post>) {
        val topIds = posts.filter { it.replyToPostId == null }.take(25).map { it.id }.toSet()
        if (topIds.isEmpty() || topIds == lastHydratedPostIds) return
        lastHydratedPostIds = topIds
        viewModelScope.launch {
            val (reactions, _, reposted) = socialRepo.getHydratedPostInteractions(topIds.toList())
            _userReactions.value = _userReactions.value + reactions
            _repostedPostIds.value = _repostedPostIds.value + reposted
        }
    }

    fun visibleTopLevelPosts(
        allPosts: List<Post>,
        blocked: Set<String>,
        muted: List<String>
    ): List<Post> {
        return allPosts.filter { post ->
            post.replyToPostId == null &&
                post.authorId !in blocked &&
                muted.none { word -> post.content.contains(word, ignoreCase = true) }
        }
    }

    fun computeTrendingHashtags(allPosts: List<Post>): List<TrendingHashtag> {
        val cutoffMs = System.currentTimeMillis() - 24 * 3_600_000L
        val counts = mutableMapOf<String, Int>()
        allPosts.forEach { post ->
            val ts = post.createdAt?.toDate()?.time ?: System.currentTimeMillis()
            if (ts >= cutoffMs) {
                post.hashtags.forEach { tag ->
                    val clean = tag.lowercase()
                    counts[clean] = (counts[clean] ?: 0) + 1
                }
            }
        }
        return counts.entries
            .sortedByDescending { it.value }
            .take(15)
            .map { TrendingHashtag(it.key, it.value) }
    }

    suspend fun checkUsernameAvailable(username: String): Boolean =
        socialRepo.isUsernameAvailable(username)

    fun saveProfile(
        name: String,
        username: String,
        bio: String,
        location: String,
        website: String,
        avatarColor: String,
        isPrivate: Boolean,
        isNewProfile: Boolean,
        previousUsername: String? = null,
        category: String = "",
        roleTitle: String = "",
        pinnedNote: String = "",
        avatarPattern: String = "solid",
        showVerifiedBadge: Boolean = true,
        onComplete: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = socialRepo.saveUserProfile(
                name = name,
                rawUsername = username,
                bio = bio,
                location = location,
                website = website,
                avatarColor = avatarColor,
                isPrivate = isPrivate,
                isNewProfile = isNewProfile,
                previousUsername = previousUsername,
                category = category,
                roleTitle = roleTitle,
                pinnedNote = pinnedNote,
                avatarPattern = avatarPattern,
                showVerifiedBadge = showVerifiedBadge
            )
            _isSaving.value = false
            res.onSuccess {
                _actionMessage.tryEmit("Profile saved")
                onComplete(true)
            }.onFailure { err ->
                _actionMessage.tryEmit(err.localizedMessage ?: "Failed to save profile")
                onComplete(false)
            }
        }
    }

    fun updatePrivacySettings(privacy: UserPrivacySettings) {
        viewModelScope.launch {
            socialRepo.updatePrivacySettings(privacy).onSuccess {
                _actionMessage.tryEmit("Privacy settings updated")
            }.onFailure {
                _actionMessage.tryEmit(it.localizedMessage ?: "Update failed")
            }
        }
    }

    fun updateNotificationSettings(notifications: UserNotificationSettings) {
        viewModelScope.launch {
            socialRepo.updateNotificationSettings(notifications).onSuccess {
                _actionMessage.tryEmit("Notification preferences updated")
            }.onFailure {
                _actionMessage.tryEmit(it.localizedMessage ?: "Update failed")
            }
        }
    }

    fun updateAppearancePreferences(preferences: UserAppearancePreferences) {
        viewModelScope.launch {
            socialRepo.updateAppearancePreferences(preferences).onSuccess {
                _actionMessage.tryEmit("Appearance updated")
            }.onFailure {
                _actionMessage.tryEmit(it.localizedMessage ?: "Update failed")
            }
        }
    }

    fun setPinMode(pinMode: String, manualPostId: String, userPosts: List<Post>) {
        val resolvedPostId = if (pinMode == "top_engaged") {
            userPosts
                .filter { it.replyToPostId == null }
                .maxByOrNull { it.rawEngagementTotal() }
                ?.id ?: ""
        } else {
            manualPostId
        }
        viewModelScope.launch {
            socialRepo.updatePinSettings(pinMode, resolvedPostId).onSuccess {
                val msg = when {
                    pinMode == "top_engaged" -> "Auto-Pin Top Post enabled"
                    resolvedPostId.isBlank() -> "Post unpinned"
                    else -> "Pinned to profile"
                }
                _actionMessage.tryEmit(msg)
            }.onFailure {
                _actionMessage.tryEmit(it.localizedMessage ?: "Failed to update pin")
            }
        }
    }

    fun toggleManualPinPost(post: Post, currentUser: UserProfile) {
        val nextId = if (currentUser.pinnedPostId == post.id && currentUser.pinMode == "manual") "" else post.id
        viewModelScope.launch {
            socialRepo.updatePinSettings("manual", nextId).onSuccess {
                _actionMessage.tryEmit(if (nextId.isEmpty()) "Unpinned from profile" else "Pinned to top of profile")
            }
        }
    }

    fun publishPostOrThread(
        author: UserProfile,
        segments: List<String>,
        quotePost: Post? = null,
        replyToPost: Post? = null,
        pollOptions: List<String>? = null,
        pollDurationHours: Int = 24,
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = socialRepo.createPostOrThread(
                author, segments, quotePost, replyToPost, pollOptions, pollDurationHours
            )
            _isSaving.value = false
            res.onSuccess {
                _actionMessage.tryEmit(if (replyToPost != null) "Reply posted" else "Pulse published")
                onDone(true)
            }.onFailure { err ->
                _actionMessage.tryEmit(err.localizedMessage ?: "Failed to publish")
                onDone(false)
            }
        }
    }

    fun editPost(post: Post, newContent: String, author: UserProfile, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = socialRepo.editPost(post, newContent, author)
            _isSaving.value = false
            res.onSuccess {
                _actionMessage.tryEmit("Pulse edited")
                onDone(true)
            }.onFailure { err ->
                _actionMessage.tryEmit(err.localizedMessage ?: "Failed to edit")
                onDone(false)
            }
        }
    }

    fun reactToPost(post: Post, actor: UserProfile, type: ReactionType? = null) {
        val existing = _userReactions.value[post.id]
        val targetType = type ?: existing ?: ReactionType.LIKE
        val expectedNext = if (existing == targetType) null else targetType
        _userReactions.value = if (expectedNext == null) {
            _userReactions.value - post.id
        } else {
            _userReactions.value + (post.id to expectedNext)
        }
        viewModelScope.launch {
            socialRepo.reactToPost(post, actor, targetType).onSuccess { actual ->
                _userReactions.value = if (actual == null) {
                    _userReactions.value - post.id
                } else {
                    _userReactions.value + (post.id to actual)
                }
            }.onFailure { err ->
                // revert optimistic state
                _userReactions.value = if (existing == null) {
                    _userReactions.value - post.id
                } else {
                    _userReactions.value + (post.id to existing)
                }
                _actionMessage.tryEmit(err.localizedMessage ?: "Reaction failed")
            }
        }
    }

    fun loadPostReactionsBreakdown(postId: String) {
        viewModelScope.launch {
            _postReactionsBreakdown.value = socialRepo.getPostReactionsList(postId)
        }
    }

    fun openPostComments(postId: String) {
        _observedCommentPostId.value = postId
        recordPostView(postId)
    }

    fun hydrateCommentReactions(postId: String, comments: List<PostComment>) {
        if (comments.isEmpty()) return
        viewModelScope.launch {
            val map = socialRepo.getUserCommentReactions(postId, comments.map { it.id })
            _commentReactions.value = _commentReactions.value + map
        }
    }

    fun addComment(post: Post, author: UserProfile, content: String, parentCommentId: String? = null) {
        viewModelScope.launch {
            socialRepo.addComment(post, author, content, parentCommentId).onSuccess {
                _actionMessage.tryEmit("Comment posted")
            }.onFailure {
                _actionMessage.tryEmit(it.localizedMessage ?: "Comment failed")
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            socialRepo.deleteComment(postId, commentId).onSuccess {
                _actionMessage.tryEmit("Comment deleted")
            }
        }
    }

    fun reactToComment(postId: String, comment: PostComment, type: ReactionType) {
        val current = _commentReactions.value[comment.id]
        val optimistic = if (current == type) null else type
        _commentReactions.value = if (optimistic == null) {
            _commentReactions.value - comment.id
        } else {
            _commentReactions.value + (comment.id to optimistic)
        }
        viewModelScope.launch {
            socialRepo.reactToComment(postId, comment, type).onFailure {
                _commentReactions.value = if (current == null) {
                    _commentReactions.value - comment.id
                } else {
                    _commentReactions.value + (comment.id to current)
                }
            }
        }
    }

    fun recordPostView(postId: String) {
        if (viewedPostsThisSession.add(postId)) {
            viewModelScope.launch {
                socialRepo.incrementPostView(postId)
            }
        }
    }

    fun toggleRepost(post: Post, actor: UserProfile) {
        val currentlyReposted = post.id in _repostedPostIds.value
        _repostedPostIds.value = if (currentlyReposted) _repostedPostIds.value - post.id else _repostedPostIds.value + post.id
        viewModelScope.launch {
            socialRepo.toggleRepost(post, actor).onFailure {
                _repostedPostIds.value = if (currentlyReposted) _repostedPostIds.value + post.id else _repostedPostIds.value - post.id
                _actionMessage.tryEmit(it.localizedMessage ?: "Repost failed")
            }
        }
    }

    fun toggleBookmark(postId: String) {
        viewModelScope.launch {
            socialRepo.toggleBookmark(postId).onSuccess { saved ->
                _actionMessage.tryEmit(if (saved) "Saved to Bookmarks" else "Removed from Bookmarks")
            }.onFailure {
                _actionMessage.tryEmit(it.localizedMessage ?: "Bookmark failed")
            }
        }
    }

    fun votePoll(post: Post, optionIndex: Int) {
        viewModelScope.launch {
            socialRepo.votePoll(post, optionIndex).onFailure {
                _actionMessage.tryEmit(it.localizedMessage ?: "Vote failed")
            }
        }
    }

    fun pinReply(parentPostId: String, replyId: String?) {
        viewModelScope.launch {
            socialRepo.pinReply(parentPostId, replyId).onSuccess {
                _actionMessage.tryEmit(if (replyId.isNullOrBlank()) "Reply unpinned" else "Reply pinned")
            }.onFailure {
                _actionMessage.tryEmit(it.localizedMessage ?: "Pin failed")
            }
        }
    }

    fun deletePost(postId: String, author: UserProfile? = null, allPosts: List<Post> = emptyList()) {
        viewModelScope.launch {
            socialRepo.deletePost(postId, author, allPosts).onSuccess {
                _actionMessage.tryEmit("Pulse deleted")
            }.onFailure {
                _actionMessage.tryEmit(it.localizedMessage ?: "Delete failed")
            }
        }
    }

    fun reportPost(postId: String, reason: String) {
        viewModelScope.launch {
            socialRepo.reportPost(postId, reason).onSuccess {
                _actionMessage.tryEmit("Report submitted")
            }.onFailure {
                _actionMessage.tryEmit(it.localizedMessage ?: "Report failed")
            }
        }
    }

    fun createStory(
        author: UserProfile,
        text: String,
        bgStart: String,
        bgEnd: String,
        fontStyle: String,
        textAlign: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = socialRepo.createStory(author, text, bgStart, bgEnd, fontStyle, textAlign)
            _isSaving.value = false
            res.onSuccess {
                _actionMessage.tryEmit("Story shared for 24h")
                onDone(true)
            }.onFailure {
                _actionMessage.tryEmit(it.localizedMessage ?: "Story failed")
                onDone(false)
            }
        }
    }

    fun markStoryViewed(story: TextStory, viewer: UserProfile) {
        _seenStoryIds.value = _seenStoryIds.value + story.id
        viewModelScope.launch {
            socialRepo.recordStoryView(story, viewer)
        }
    }

    fun loadStoryViewers(storyId: String) {
        viewModelScope.launch {
            _storyViewers.value = socialRepo.getStoryViewers(storyId)
        }
    }

    fun toggleFollow(currentUser: UserProfile, target: UserProfile) {
        viewModelScope.launch {
            socialRepo.toggleFollow(currentUser, target).onSuccess { status ->
                val msg = when (status) {
                    "followed" -> "Following @${target.username}"
                    "requested" -> "Follow request sent to @${target.username}"
                    else -> "Unfollowed @${target.username}"
                }
                _actionMessage.tryEmit(msg)
            }.onFailure {
                _actionMessage.tryEmit(it.localizedMessage ?: "Follow action failed")
            }
        }
    }

    fun respondFollowRequest(currentUser: UserProfile, req: FollowRequest, accept: Boolean) {
        viewModelScope.launch {
            socialRepo.respondFollowRequest(currentUser, req, accept).onSuccess {
                _actionMessage.tryEmit(if (accept) "Accepted @${req.requesterUsername}" else "Declined")
            }
        }
    }

    fun toggleBlockUser(targetUid: String, targetUsername: String) {
        viewModelScope.launch {
            socialRepo.toggleBlockUser(targetUid, targetUsername).onSuccess { blocked ->
                _actionMessage.tryEmit(if (blocked) "Blocked @$targetUsername" else "Unblocked @$targetUsername")
            }
        }
    }

    fun toggleMuteUser(targetUid: String, targetUsername: String) {
        viewModelScope.launch {
            socialRepo.toggleMuteUser(targetUid, targetUsername).onSuccess { muted ->
                _actionMessage.tryEmit(if (muted) "Muted @$targetUsername" else "Unmuted @$targetUsername")
            }
        }
    }

    fun toggleNotifyAuthor(targetUid: String, targetUsername: String) {
        viewModelScope.launch {
            socialRepo.toggleNotifyAuthor(targetUid).onSuccess { enabled ->
                _actionMessage.tryEmit(
                    if (enabled) "Notifications enabled for @$targetUsername"
                    else "Notifications turned off for @$targetUsername"
                )
            }
        }
    }

    fun addMutedWord(word: String, durationDays: Int? = null) {
        viewModelScope.launch {
            socialRepo.addMutedWord(word, durationDays).onSuccess {
                _actionMessage.tryEmit("Muted \"$word\"")
            }
        }
    }

    fun removeMutedWord(word: String) {
        viewModelScope.launch {
            socialRepo.removeMutedWord(word)
        }
    }

    fun submitVerification(
        user: UserProfile,
        requestedType: String,
        organizationName: String,
        reason: String,
        onDone: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = socialRepo.submitVerificationRequest(user, requestedType, organizationName, reason)
            _isSaving.value = false
            res.onSuccess {
                _actionMessage.tryEmit("Verification request submitted")
                onDone(true)
            }.onFailure {
                _actionMessage.tryEmit(it.localizedMessage ?: "Submission failed")
                onDone(false)
            }
        }
    }

    fun reviewVerification(req: VerificationRequest, approve: Boolean, reviewer: UserProfile) {
        viewModelScope.launch {
            socialRepo.reviewVerificationRequest(req, approve, reviewer).onSuccess {
                _actionMessage.tryEmit(if (approve) "Approved @${req.username}" else "Rejected @${req.username}")
            }.onFailure {
                _actionMessage.tryEmit(it.localizedMessage ?: "Review failed")
            }
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch {
            socialRepo.markNotificationRead(notifId)
        }
    }

    fun exportUserData(user: UserProfile, allPosts: List<Post>): String =
        socialRepo.buildExportDataJson(user, allPosts)

    fun deactivateAccount(onDone: () -> Unit) {
        viewModelScope.launch {
            socialRepo.deactivateAccount().onSuccess {
                _actionMessage.tryEmit("Account deactivated")
                onDone()
            }
        }
    }

    fun deleteAccount(username: String, onDone: () -> Unit) {
        viewModelScope.launch {
            socialRepo.deleteAccount(username).onSuccess {
                _actionMessage.tryEmit("Account deleted")
                onDone()
            }.onFailure {
                _actionMessage.tryEmit(it.localizedMessage ?: "Account deletion failed")
            }
        }
    }

    fun refreshCacheStats() {
        viewModelScope.launch {
            _cacheStats.value = socialRepo.getCacheStats()
        }
    }

    fun clearOfflineCache() {
        viewModelScope.launch {
            socialRepo.clearOfflineCache()
            _cacheStats.value = socialRepo.getCacheStats()
            _actionMessage.tryEmit("Offline cache cleared")
        }
    }

    fun showMessage(msg: String) {
        _actionMessage.tryEmit(msg)
    }

    private suspend inline fun unwrapFirestoreError(
        error: Throwable,
        tag: String,
        onEmitError: suspend (String) -> Unit
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        Log.w("FeedVM", "Error observing $tag", rootError)
        onEmitError(rootError.localizedMessage ?: "Failed to load $tag")
    }
}
