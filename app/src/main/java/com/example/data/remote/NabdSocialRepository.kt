package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.local.CachedPostEntity
import com.example.data.local.NabdDao
import com.example.data.model.AppNotification
import com.example.data.model.FollowRequest
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PostReaction
import com.example.data.model.ReactionType
import com.example.data.model.StoryViewer
import com.example.data.model.TextStory
import com.example.data.model.UserAppearancePreferences
import com.example.data.model.UserNotificationSettings
import com.example.data.model.UserPrivacySettings
import com.example.data.model.UserProfile
import com.example.data.model.VerificationRequest
import com.example.util.extractHashtags
import com.example.util.extractMentions
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

object FirestoreProvider {
    @Volatile
    private var instance: FirebaseFirestore? = null

    fun get(context: Context): FirebaseFirestore {
        return instance ?: synchronized(this) {
            instance ?: run {
                val databaseId = context.applicationContext.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                try {
                    db.firestoreSettings = firestoreSettings {
                        setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    }
                } catch (_: Exception) {
                }
                db
            }.also { instance = it }
        }
    }
}

class NabdSocialRepository(
    private val db: FirebaseFirestore,
    private val nabdDao: NabdDao,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    fun currentUserEmail(): String = auth.currentUser?.email ?: ""

    fun isCurrentUserAdmin(): Boolean {
        val email = auth.currentUser?.email ?: return false
        return email.equals("ameeralajaj@gmail.com", ignoreCase = true) &&
            (auth.currentUser?.isEmailVerified == true)
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> = flow {
        if (uid.isBlank()) {
            emit(null)
            return@flow
        }
        val ref = db.collection("users").document(uid)
        emitAll(
            ref.snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .catch { error ->
                    logStreamError(error, OperationType.GET, ref.path, uid)
                    throw error
                }
        )
    }

    fun observeActiveUsers(): Flow<List<UserProfile>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("users")
                .whereEqualTo("accountStatus", "active")
                .snapshots()
                .map { snap ->
                    snap.toObjects(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.followersCount }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users", uid)
                    throw error
                }
        )
    }

    suspend fun isUsernameAvailable(rawUsername: String): Boolean {
        val clean = rawUsername.trim().lowercase()
        if (!clean.matches(Regex("^[a-z0-9_]{3,24}$"))) return false
        return try {
            val doc = db.collection("usernames").document(clean).get().await()
            !doc.exists() || doc.getString("uid") == auth.currentUser?.uid
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "usernames/$clean")
            false
        }
    }

    suspend fun saveUserProfile(
        name: String,
        rawUsername: String,
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
        showVerifiedBadge: Boolean = true
    ): Result<Unit> = try {
        val uid = requireUserId()
        val cleanName = name.trim().take(60)
        val cleanUsername = rawUsername.trim().lowercase()
        require(cleanName.isNotEmpty()) { "Display name is required" }
        require(cleanUsername.matches(Regex("^[a-z0-9_]{3,24}$"))) {
            "Username must be 3-24 lowercase letters, numbers, or underscores"
        }

        val usernameChanged = !isNewProfile && previousUsername != null && previousUsername != cleanUsername
        if (isNewProfile || usernameChanged) {
            val claimDoc = db.collection("usernames").document(cleanUsername).get().await()
            if (claimDoc.exists() && claimDoc.getString("uid") != uid) {
                error("@$cleanUsername is already taken")
            }
            db.collection("usernames").document(cleanUsername).set(
                mapOf(
                    "uid" to uid,
                    "username" to cleanUsername,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (!previousUsername.isNullOrBlank() && previousUsername != cleanUsername) {
                try {
                    db.collection("usernames").document(previousUsername).delete().await()
                } catch (_: Exception) {
                }
            }
        }

        val userRef = db.collection("users").document(uid)
        if (isNewProfile) {
            val payload = mapOf(
                "uid" to uid,
                "name" to cleanName,
                "username" to cleanUsername,
                "bio" to bio.trim().take(160),
                "location" to location.trim().take(60),
                "website" to website.trim().take(120),
                "category" to category.trim().take(40),
                "roleTitle" to roleTitle.trim().take(40),
                "pinnedNote" to pinnedNote.trim().take(120),
                "avatarColor" to avatarColor,
                "avatarPattern" to avatarPattern,
                "verifiedType" to "none",
                "showVerifiedBadge" to true,
                "pinnedPostId" to "",
                "pinMode" to "manual",
                "postsCount" to 0L,
                "followersCount" to 0L,
                "followingCount" to 0L,
                "isPrivate" to isPrivate,
                "accountStatus" to "active",
                "privacy" to UserPrivacySettings().toMap(),
                "notifications" to UserNotificationSettings().toMap(),
                "preferences" to UserAppearancePreferences().toMap(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            userRef.set(payload).await()
        } else {
            val updates = mutableMapOf<String, Any>(
                "name" to cleanName,
                "username" to cleanUsername,
                "bio" to bio.trim().take(160),
                "location" to location.trim().take(60),
                "website" to website.trim().take(120),
                "category" to category.trim().take(40),
                "roleTitle" to roleTitle.trim().take(40),
                "pinnedNote" to pinnedNote.trim().take(120),
                "avatarColor" to avatarColor,
                "avatarPattern" to avatarPattern,
                "showVerifiedBadge" to showVerifiedBadge,
                "isPrivate" to isPrivate,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (usernameChanged) {
                updates["usernameChangedAt"] = FieldValue.serverTimestamp()
            }
            userRef.update(updates).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
        Result.failure(e)
    }

    suspend fun updatePrivacySettings(privacy: UserPrivacySettings): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("users").document(uid).update(
            mapOf(
                "privacy" to privacy.toMap(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/privacy")
        Result.failure(e)
    }

    suspend fun updateNotificationSettings(notifications: UserNotificationSettings): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("users").document(uid).update(
            mapOf(
                "notifications" to notifications.toMap(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/notifications")
        Result.failure(e)
    }

    suspend fun updateAppearancePreferences(preferences: UserAppearancePreferences): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("users").document(uid).update(
            mapOf(
                "preferences" to preferences.toMap(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/preferences")
        Result.failure(e)
    }

    suspend fun updatePinSettings(pinMode: String, pinnedPostId: String): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("users").document(uid).update(
            mapOf(
                "pinMode" to pinMode,
                "pinnedPostId" to pinnedPostId,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/pin")
        Result.failure(e)
    }

    suspend fun deactivateAccount(): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("users").document(uid).update(
            mapOf(
                "accountStatus" to "deactivated",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/deactivate")
        Result.failure(e)
    }

    suspend fun deleteAccount(username: String): Result<Unit> = try {
        val uid = requireUserId()
        if (username.isNotBlank()) {
            try {
                db.collection("usernames").document(username).delete().await()
            } catch (_: Exception) {
            }
        }
        db.collection("users").document(uid).delete().await()
        nabdDao.clearPosts()
        nabdDao.clearMessages()
        nabdDao.clearChats()
        try {
            auth.currentUser?.delete()?.await()
        } catch (_: Exception) {
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "users/delete")
        Result.failure(e)
    }

    fun buildExportDataJson(user: UserProfile, allPosts: List<Post>): String {
        val myPosts = allPosts.filter { it.authorId == user.uid }
        val root = JSONObject().apply {
            put("app", "Nabd")
            put("exportedAt", Date().toString())
            put("profile", JSONObject().apply {
                put("uid", user.uid)
                put("name", user.name)
                put("username", user.username)
                put("bio", user.bio)
                put("location", user.location)
                put("website", user.website)
                put("category", user.category)
                put("verifiedType", user.verifiedType)
                put("followersCount", user.followersCount)
                put("followingCount", user.followingCount)
            })
            put("posts", JSONArray().apply {
                myPosts.forEach { p ->
                    put(JSONObject().apply {
                        put("id", p.id)
                        put("content", p.content)
                        put("reactions", p.effectiveReactionsCount)
                        put("comments", p.effectiveCommentsCount)
                        put("reposts", p.repostsCount)
                        put("views", p.viewsCount)
                        put("isEdited", p.isEdited)
                    })
                }
            })
        }
        return root.toString(2)
    }

    suspend fun getCacheStats(): Triple<Int, Int, Int> = Triple(
        nabdDao.countCachedPosts(),
        nabdDao.countCachedChats(),
        nabdDao.countCachedMessages()
    )

    suspend fun clearOfflineCache() {
        nabdDao.clearPosts()
        nabdDao.clearMessages()
        nabdDao.clearChats()
    }

    suspend fun getHydratedPostInteractions(postIds: List<String>): Triple<Map<String, ReactionType>, Set<String>, Set<String>> {
        val uid = auth.currentUser?.uid ?: return Triple(emptyMap(), emptySet(), emptySet())
        val reactionMap = mutableMapOf<String, ReactionType>()
        val reposted = mutableSetOf<String>()
        postIds.take(40).forEach { postId ->
            try {
                val reactionSnap = db.collection("posts").document(postId)
                    .collection("reactions").document(uid).get().await()
                if (reactionSnap.exists()) {
                    val type = ReactionType.fromKey(reactionSnap.getString("type"))
                    reactionMap[postId] = type
                } else {
                    val likeSnap = db.collection("posts").document(postId)
                        .collection("likes").document(uid).get().await()
                    if (likeSnap.exists()) {
                        reactionMap[postId] = ReactionType.LIKE
                    }
                }
                if (db.collection("posts").document(postId).collection("reposts").document(uid).get().await().exists()) {
                    reposted.add(postId)
                }
            } catch (_: Exception) {
            }
        }
        return Triple(reactionMap, reactionMap.keys.toSet(), reposted)
    }

    fun observePublicPosts(): Flow<List<Post>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("posts")
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    val posts = snap.toObjects(Post::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                    nabdDao.upsertPosts(posts.take(100).map { CachedPostEntity.fromDomain(it) })
                    posts
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "posts", uid)
                    throw error
                }
        )
    }

    fun observeCachedPosts(): Flow<List<Post>> =
        nabdDao.observeCachedPosts().map { list -> list.map { it.toDomain() } }

    suspend fun createPostOrThread(
        author: UserProfile,
        segments: List<String>,
        quotePost: Post? = null,
        replyToPost: Post? = null,
        pollOptions: List<String>? = null,
        pollDurationHours: Int = 24
    ): Result<Unit> = try {
        val uid = requireUserId()
        require(author.uid == uid) { "Author mismatch" }
        val maxChars = author.maxPostLength
        val cleanSegments = segments.map { it.trim() }.filter { it.isNotEmpty() }
        require(cleanSegments.isNotEmpty()) { "Post cannot be empty" }
        cleanSegments.forEach { seg ->
            require(seg.length <= maxChars) { "Post exceeds $maxChars characters" }
        }

        var parentId: String? = replyToPost?.id
        var rootId: String? = replyToPost?.threadRootId ?: replyToPost?.id

        cleanSegments.forEachIndexed { index, text ->
            val postRef = db.collection("posts").document()
            if (index == 0 && rootId == null && cleanSegments.size > 1) {
                rootId = postRef.id
            }
            val hashtags = extractHashtags(text)
            val mentions = extractMentions(text)

            val pollMap = if (index == 0 && pollOptions != null && pollOptions.size >= 2) {
                val expiresMs = System.currentTimeMillis() + pollDurationHours.coerceIn(1, 168) * 3_600_000L
                mapOf(
                    "options" to pollOptions.take(4).map { it.trim().take(80) },
                    "votes" to emptyMap<String, String>(),
                    "expiresAt" to Timestamp(Date(expiresMs))
                )
            } else null

            val payload = mutableMapOf<String, Any?>(
                "authorId" to uid,
                "authorName" to author.name,
                "authorUsername" to author.username,
                "authorVerifiedType" to author.verifiedType,
                "authorAvatarColor" to author.avatarColor,
                "content" to text,
                "threadParentId" to parentId,
                "threadRootId" to rootId,
                "quotePostId" to if (index == 0) quotePost?.id else null,
                "quoteAuthorName" to if (index == 0) quotePost?.authorName else null,
                "quoteAuthorUsername" to if (index == 0) quotePost?.authorUsername else null,
                "quoteContent" to if (index == 0) quotePost?.content else null,
                "replyToPostId" to if (index == 0) replyToPost?.id else null,
                "poll" to pollMap,
                "hashtags" to hashtags,
                "mentions" to mentions,
                "likesCount" to 0L,
                "repostsCount" to 0L,
                "repliesCount" to 0L,
                "reactionsCount" to emptyMap<String, Long>(),
                "totalReactionsCount" to 0L,
                "viewsCount" to 0L,
                "commentsCount" to 0L,
                "engagementScore" to 0.0,
                "isEdited" to false,
                "visibility" to "public",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            postRef.set(payload).await()
            parentId = postRef.id

            if (replyToPost == null) {
                db.collection("users").document(uid).update(
                    mapOf(
                        "postsCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            if (index == 0 && replyToPost != null && replyToPost.authorId != uid) {
                db.collection("posts").document(replyToPost.id).update(
                    mapOf(
                        "repliesCount" to FieldValue.increment(1L),
                        "commentsCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                sendNotification(
                    targetUserId = replyToPost.authorId,
                    actor = author,
                    type = "reply",
                    postId = postRef.id,
                    previewText = text.take(140)
                )
            }
            if (index == 0 && quotePost != null && quotePost.authorId != uid) {
                db.collection("posts").document(quotePost.id).update(
                    mapOf(
                        "repostsCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                sendNotification(
                    targetUserId = quotePost.authorId,
                    actor = author,
                    type = "repost",
                    postId = postRef.id,
                    previewText = text.take(140)
                )
            }
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "posts")
        Result.failure(e)
    }

    suspend fun reactToPost(
        post: Post,
        actor: UserProfile,
        requestedType: ReactionType
    ): Result<ReactionType?> = try {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(post.id)
        val reactionRef = postRef.collection("reactions").document(uid)
        val legacyLikeRef = postRef.collection("likes").document(uid)

        val resultType = db.runTransaction { tx ->
            val postSnap = tx.get(postRef)
            if (!postSnap.exists()) return@runTransaction null
            val existingReactSnap = tx.get(reactionRef)
            val currentTypeKey = existingReactSnap.getString("type")
            @Suppress("UNCHECKED_CAST")
            val rawCounts = (postSnap.get("reactionsCount") as? Map<String, Number>)?.mapValues { it.value.toLong() }
                ?.toMutableMap() ?: mutableMapOf()
            val prevTotal = postSnap.getLong("totalReactionsCount")
                ?: postSnap.getLong("likesCount")
                ?: rawCounts.values.sum()

            val nextType: ReactionType?
            val newTotal: Long

            if (existingReactSnap.exists() && currentTypeKey == requestedType.key) {
                // Toggle off
                tx.delete(reactionRef)
                tx.delete(legacyLikeRef)
                val cur = rawCounts[requestedType.key] ?: 1L
                if (cur <= 1L) rawCounts.remove(requestedType.key) else rawCounts[requestedType.key] = cur - 1L
                newTotal = (prevTotal - 1L).coerceAtLeast(0L)
                nextType = null
            } else {
                if (existingReactSnap.exists() && currentTypeKey != null) {
                    val oldCount = rawCounts[currentTypeKey] ?: 1L
                    if (oldCount <= 1L) rawCounts.remove(currentTypeKey) else rawCounts[currentTypeKey] = oldCount - 1L
                    newTotal = prevTotal.coerceAtLeast(1L)
                } else {
                    newTotal = prevTotal + 1L
                }
                rawCounts[requestedType.key] = (rawCounts[requestedType.key] ?: 0L) + 1L
                tx.set(
                    reactionRef,
                    mapOf(
                        "userId" to uid,
                        "postId" to post.id,
                        "type" to requestedType.key,
                        "userName" to actor.name,
                        "userUsername" to actor.username,
                        "userAvatarColor" to actor.avatarColor,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    legacyLikeRef,
                    mapOf(
                        "userId" to uid,
                        "postId" to post.id,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                nextType = requestedType
            }

            val comments = postSnap.getLong("commentsCount") ?: postSnap.getLong("repliesCount") ?: 0L
            val reposts = postSnap.getLong("repostsCount") ?: 0L
            val views = postSnap.getLong("viewsCount") ?: 0L
            val score = newTotal * 3.0 + comments * 4.0 + reposts * 3.0 + views * 0.25

            tx.update(
                postRef,
                mapOf(
                    "reactionsCount" to rawCounts,
                    "totalReactionsCount" to newTotal,
                    "likesCount" to newTotal,
                    "engagementScore" to score,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            nextType
        }.await()

        if (resultType != null && post.authorId != uid) {
            sendNotification(
                targetUserId = post.authorId,
                actor = actor,
                type = "reaction",
                postId = post.id,
                previewText = "${resultType.emoji} reacted to your pulse: ${post.content.take(90)}"
            )
        }
        if (resultType != null && post.replyToPostId != null) {
            val parentSnap = db.collection("posts").document(post.replyToPostId).get().await()
            if (parentSnap.getString("authorId") == uid) {
                postRef.update(
                    mapOf(
                        "authorLikedReply" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
        Result.success(resultType)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "posts/${post.id}/reactions")
        Result.failure(e)
    }

    suspend fun getPostReactionsList(postId: String): List<PostReaction> = try {
        requireUserId()
        db.collection("posts").document(postId).collection("reactions")
            .whereEqualTo("postId", postId)
            .get()
            .await()
            .toObjects(PostReaction::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.LIST, "posts/$postId/reactions")
        emptyList()
    }

    // Dedicated Comments & Nested Replies
    fun observePostComments(postId: String): Flow<List<PostComment>> = flow {
        val uid = requireUserId()
        if (postId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("postId", postId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PostComment::class.java, ServerTimestampBehavior.ESTIMATE)
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "posts/$postId/comments", uid)
                    throw error
                }
        )
    }

    suspend fun addComment(
        post: Post,
        author: UserProfile,
        content: String,
        parentCommentId: String? = null
    ): Result<Unit> = try {
        val uid = requireUserId()
        val clean = content.trim()
        require(clean.isNotEmpty() && clean.length <= 1000) { "Comment must be 1-1000 characters" }

        val postRef = db.collection("posts").document(post.id)
        val commentRef = postRef.collection("comments").document()
        val payload = mutableMapOf<String, Any?>(
            "postId" to post.id,
            "parentCommentId" to parentCommentId,
            "authorId" to uid,
            "authorName" to author.name,
            "authorUsername" to author.username,
            "authorVerifiedType" to author.verifiedType,
            "authorAvatarColor" to author.avatarColor,
            "content" to clean,
            "reactionsCount" to emptyMap<String, Long>(),
            "totalReactionsCount" to 0L,
            "isEdited" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>

        commentRef.set(payload).await()
        postRef.update(
            mapOf(
                "commentsCount" to FieldValue.increment(1L),
                "repliesCount" to FieldValue.increment(1L),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        if (post.authorId != uid) {
            sendNotification(
                targetUserId = post.authorId,
                actor = author,
                type = "comment",
                postId = post.id,
                previewText = clean.take(140)
            )
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "posts/${post.id}/comments")
        Result.failure(e)
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> = try {
        requireUserId()
        val postRef = db.collection("posts").document(postId)
        postRef.collection("comments").document(commentId).delete().await()
        db.runTransaction { tx ->
            val snap = tx.get(postRef)
            if (snap.exists()) {
                val curComments = (snap.getLong("commentsCount") ?: snap.getLong("repliesCount") ?: 1L)
                val curReplies = snap.getLong("repliesCount") ?: curComments
                tx.update(
                    postRef,
                    mapOf(
                        "commentsCount" to (curComments - 1L).coerceAtLeast(0L),
                        "repliesCount" to (curReplies - 1L).coerceAtLeast(0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }.await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "posts/$postId/comments/$commentId")
        Result.failure(e)
    }

    suspend fun reactToComment(
        postId: String,
        comment: PostComment,
        type: ReactionType
    ): Result<ReactionType?> = try {
        val uid = requireUserId()
        val commentRef = db.collection("posts").document(postId).collection("comments").document(comment.id)
        val reactRef = commentRef.collection("reactions").document(uid)

        val result = db.runTransaction { tx ->
            val cSnap = tx.get(commentRef)
            if (!cSnap.exists()) return@runTransaction null
            val rSnap = tx.get(reactRef)
            val prevType = rSnap.getString("type")
            @Suppress("UNCHECKED_CAST")
            val counts = (cSnap.get("reactionsCount") as? Map<String, Number>)?.mapValues { it.value.toLong() }
                ?.toMutableMap() ?: mutableMapOf()
            val prevTotal = cSnap.getLong("totalReactionsCount") ?: counts.values.sum()

            val next: ReactionType?
            val newTotal: Long
            if (rSnap.exists() && prevType == type.key) {
                tx.delete(reactRef)
                val cur = counts[type.key] ?: 1L
                if (cur <= 1L) counts.remove(type.key) else counts[type.key] = cur - 1L
                newTotal = (prevTotal - 1L).coerceAtLeast(0L)
                next = null
            } else {
                if (rSnap.exists() && prevType != null) {
                    val cur = counts[prevType] ?: 1L
                    if (cur <= 1L) counts.remove(prevType) else counts[prevType] = cur - 1L
                    newTotal = prevTotal.coerceAtLeast(1L)
                } else {
                    newTotal = prevTotal + 1L
                }
                counts[type.key] = (counts[type.key] ?: 0L) + 1L
                tx.set(
                    reactRef,
                    mapOf(
                        "userId" to uid,
                        "commentId" to comment.id,
                        "type" to type.key,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                next = type
            }

            tx.update(
                commentRef,
                mapOf(
                    "reactionsCount" to counts,
                    "totalReactionsCount" to newTotal,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            next
        }.await()
        Result.success(result)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "posts/$postId/comments/${comment.id}/reactions")
        Result.failure(e)
    }

    suspend fun getUserCommentReactions(postId: String, commentIds: List<String>): Map<String, ReactionType> {
        val uid = auth.currentUser?.uid ?: return emptyMap()
        val map = mutableMapOf<String, ReactionType>()
        commentIds.take(30).forEach { cId ->
            try {
                val snap = db.collection("posts").document(postId)
                    .collection("comments").document(cId)
                    .collection("reactions").document(uid).get().await()
                if (snap.exists()) {
                    map[cId] = ReactionType.fromKey(snap.getString("type"))
                }
            } catch (_: Exception) {
            }
        }
        return map
    }

    suspend fun editPost(post: Post, newContent: String, author: UserProfile): Result<Unit> = try {
        val uid = requireUserId()
        require(post.authorId == uid) { "Only the author can edit this post" }
        val clean = newContent.trim()
        require(clean.isNotEmpty() && clean.length <= author.maxPostLength) {
            "Post must be 1-${author.maxPostLength} characters"
        }
        db.collection("posts").document(post.id).update(
            mapOf(
                "content" to clean,
                "hashtags" to extractHashtags(clean),
                "mentions" to extractMentions(clean),
                "isEdited" to true,
                "editedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "posts/${post.id}")
        Result.failure(e)
    }

    suspend fun incrementPostView(postId: String) {
        try {
            requireUserId()
            db.collection("posts").document(postId).update(
                mapOf(
                    "viewsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
        }
    }

    suspend fun toggleRepost(post: Post, actor: UserProfile): Result<Boolean> = try {
        val uid = requireUserId()
        val repostRef = db.collection("posts").document(post.id).collection("reposts").document(uid)
        val existing = repostRef.get().await()
        val postRef = db.collection("posts").document(post.id)
        if (existing.exists()) {
            repostRef.delete().await()
            if (post.repostsCount > 0) {
                postRef.update(
                    mapOf(
                        "repostsCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(false)
        } else {
            repostRef.set(
                mapOf(
                    "userId" to uid,
                    "postId" to post.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            postRef.update(
                mapOf(
                    "repostsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            if (post.authorId != uid) {
                sendNotification(
                    targetUserId = post.authorId,
                    actor = actor,
                    type = "repost",
                    postId = post.id,
                    previewText = post.content.take(120)
                )
            }
            Result.success(true)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "posts/${post.id}/reposts")
        Result.failure(e)
    }

    suspend fun votePoll(post: Post, optionIndex: Int): Result<Unit> = try {
        val uid = requireUserId()
        val poll = post.poll ?: error("Post does not contain a poll")
        require(optionIndex in poll.options.indices) { "Invalid poll option" }
        val updatedVotes = poll.votes.toMutableMap().apply {
            put(uid, optionIndex.toString())
        }
        val updatedPoll = mapOf(
            "options" to poll.options,
            "votes" to updatedVotes,
            "expiresAt" to (poll.expiresAt ?: Timestamp(Date(System.currentTimeMillis() + 86_400_000L)))
        )
        db.collection("posts").document(post.id).update(
            mapOf(
                "poll" to updatedPoll,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "posts/${post.id}")
        Result.failure(e)
    }

    suspend fun pinReply(parentPostId: String, replyId: String?): Result<Unit> = try {
        requireUserId()
        val value = replyId ?: ""
        db.collection("posts").document(parentPostId).update(
            mapOf(
                "pinnedReplyId" to value,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "posts/$parentPostId")
        Result.failure(e)
    }

    suspend fun deletePost(
        postId: String,
        author: UserProfile?,
        remainingPosts: List<Post>
    ): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("posts").document(postId).delete().await()
        if (author != null && author.uid == uid) {
            val updates = mutableMapOf<String, Any>(
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (author.postsCount > 0L) {
                updates["postsCount"] = (author.postsCount - 1L).coerceAtLeast(0L)
            }
            if (author.pinnedPostId == postId) {
                if (author.pinMode == "top_engaged") {
                    val nextTop = remainingPosts
                        .filter { it.authorId == uid && it.id != postId && it.replyToPostId == null }
                        .maxByOrNull { it.rawEngagementTotal() }
                    updates["pinnedPostId"] = nextTop?.id ?: ""
                } else {
                    updates["pinnedPostId"] = ""
                }
            }
            db.collection("users").document(uid).update(updates).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "posts/$postId")
        Result.failure(e)
    }

    suspend fun reportPost(postId: String, reason: String): Result<Unit> = try {
        val uid = requireUserId()
        db.collection("posts").document(postId).collection("reports").document().set(
            mapOf(
                "reporterId" to uid,
                "postId" to postId,
                "reason" to reason.trim().take(300),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "posts/$postId/reports")
        Result.failure(e)
    }

    fun observeBookmarkIds(uid: String): Flow<Set<String>> = flow {
        if (uid.isBlank()) { emit(emptySet()); return@flow }
        emitAll(
            db.collection("users").document(uid).collection("bookmarks")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users/$uid/bookmarks", uid)
                    throw error
                }
        )
    }

    suspend fun toggleBookmark(postId: String): Result<Boolean> = try {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("bookmarks").document(postId)
        val snap = ref.get().await()
        if (snap.exists()) {
            ref.delete().await()
            Result.success(false)
        } else {
            ref.set(mapOf("postId" to postId, "createdAt" to FieldValue.serverTimestamp())).await()
            Result.success(true)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/bookmarks/$postId")
        Result.failure(e)
    }

    fun observeFollowingIds(uid: String): Flow<Set<String>> = flow {
        if (uid.isBlank()) { emit(emptySet()); return@flow }
        emitAll(
            db.collection("users").document(uid).collection("following")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users/$uid/following", uid)
                    throw error
                }
        )
    }

    fun observeFollowerIds(uid: String): Flow<Set<String>> = flow {
        if (uid.isBlank()) { emit(emptySet()); return@flow }
        emitAll(
            db.collection("users").document(uid).collection("followers")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users/$uid/followers", uid)
                    throw error
                }
        )
    }

    suspend fun toggleFollow(currentUser: UserProfile, target: UserProfile): Result<String> = try {
        val uid = requireUserId()
        val followingRef = db.collection("users").document(uid).collection("following").document(target.uid)
        val followerRef = db.collection("users").document(target.uid).collection("followers").document(uid)
        val isCurrentlyFollowing = followingRef.get().await().exists()

        if (isCurrentlyFollowing) {
            followingRef.delete().await()
            followerRef.delete().await()
            if (currentUser.followingCount > 0) {
                db.collection("users").document(uid).update(
                    mapOf("followingCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
            if (target.followersCount > 0) {
                db.collection("users").document(target.uid).update(
                    mapOf("followersCount" to FieldValue.increment(-1L), "updatedAt" to FieldValue.serverTimestamp())
                ).await()
            }
            Result.success("unfollowed")
        } else if (target.isPrivate) {
            val reqRef = db.collection("users").document(target.uid).collection("follow_requests").document(uid)
            reqRef.set(
                mapOf(
                    "requesterUid" to uid,
                    "requesterName" to currentUser.name,
                    "requesterUsername" to currentUser.username,
                    "requesterAvatarColor" to currentUser.avatarColor,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success("requested")
        } else {
            followingRef.set(
                mapOf(
                    "targetUid" to target.uid,
                    "targetName" to target.name,
                    "targetUsername" to target.username,
                    "targetAvatarColor" to target.avatarColor,
                    "targetVerifiedType" to target.verifiedType,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            followerRef.set(
                mapOf(
                    "followerUid" to uid,
                    "followerName" to currentUser.name,
                    "followerUsername" to currentUser.username,
                    "followerAvatarColor" to currentUser.avatarColor,
                    "followerVerifiedType" to currentUser.verifiedType,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(uid).update(
                mapOf("followingCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            db.collection("users").document(target.uid).update(
                mapOf("followersCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            sendNotification(
                targetUserId = target.uid,
                actor = currentUser,
                type = "follow",
                postId = null,
                previewText = "@${currentUser.username} started following you"
            )
            Result.success("followed")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/${target.uid}/follow")
        Result.failure(e)
    }

    fun observeFollowRequests(uid: String): Flow<List<FollowRequest>> = flow {
        if (uid.isBlank()) { emit(emptyList()); return@flow }
        emitAll(
            db.collection("users").document(uid).collection("follow_requests")
                .snapshots()
                .map { it.toObjects(FollowRequest::class.java, ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users/$uid/follow_requests", uid)
                    throw error
                }
        )
    }

    suspend fun respondFollowRequest(currentUser: UserProfile, req: FollowRequest, accept: Boolean): Result<Unit> = try {
        val uid = requireUserId()
        val reqRef = db.collection("users").document(uid).collection("follow_requests").document(req.requesterUid)
        if (accept) {
            db.collection("users").document(uid).collection("followers").document(req.requesterUid).set(
                mapOf(
                    "followerUid" to req.requesterUid,
                    "followerName" to req.requesterName,
                    "followerUsername" to req.requesterUsername,
                    "followerAvatarColor" to req.requesterAvatarColor,
                    "followerVerifiedType" to "none",
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(req.requesterUid).collection("following").document(uid).set(
                mapOf(
                    "targetUid" to uid,
                    "targetName" to currentUser.name,
                    "targetUsername" to currentUser.username,
                    "targetAvatarColor" to currentUser.avatarColor,
                    "targetVerifiedType" to currentUser.verifiedType,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("users").document(uid).update(
                mapOf("followersCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            db.collection("users").document(req.requesterUid).update(
                mapOf("followingCount" to FieldValue.increment(1L), "updatedAt" to FieldValue.serverTimestamp())
            ).await()
        }
        reqRef.delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/follow_requests")
        Result.failure(e)
    }

    fun observeBlockedIds(uid: String): Flow<Set<String>> = flow {
        if (uid.isBlank()) { emit(emptySet()); return@flow }
        emitAll(
            db.collection("users").document(uid).collection("blocked")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users/$uid/blocked", uid)
                    throw error
                }
        )
    }

    suspend fun toggleBlockUser(targetUid: String, targetUsername: String): Result<Boolean> = try {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("blocked").document(targetUid)
        if (ref.get().await().exists()) {
            ref.delete().await()
            Result.success(false)
        } else {
            ref.set(
                mapOf(
                    "blockedUid" to targetUid,
                    "blockedUsername" to targetUsername,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(true)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/blocked/$targetUid")
        Result.failure(e)
    }

    fun observeMutedUserIds(uid: String): Flow<Set<String>> = flow {
        if (uid.isBlank()) { emit(emptySet()); return@flow }
        emitAll(
            db.collection("users").document(uid).collection("muted_users")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users/$uid/muted_users", uid)
                    throw error
                }
        )
    }

    suspend fun toggleMuteUser(targetUid: String, targetUsername: String): Result<Boolean> = try {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("muted_users").document(targetUid)
        if (ref.get().await().exists()) {
            ref.delete().await()
            Result.success(false)
        } else {
            ref.set(
                mapOf(
                    "mutedUid" to targetUid,
                    "mutedUsername" to targetUsername,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(true)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/muted_users/$targetUid")
        Result.failure(e)
    }

    fun observeNotifyAuthorIds(uid: String): Flow<Set<String>> = flow {
        if (uid.isBlank()) { emit(emptySet()); return@flow }
        emitAll(
            db.collection("users").document(uid).collection("notify_authors")
                .snapshots()
                .map { snap -> snap.documents.map { it.id }.toSet() }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users/$uid/notify_authors", uid)
                    throw error
                }
        )
    }

    suspend fun toggleNotifyAuthor(targetUid: String): Result<Boolean> = try {
        val uid = requireUserId()
        val ref = db.collection("users").document(uid).collection("notify_authors").document(targetUid)
        if (ref.get().await().exists()) {
            ref.delete().await()
            Result.success(false)
        } else {
            ref.set(mapOf("targetUid" to targetUid, "createdAt" to FieldValue.serverTimestamp())).await()
            Result.success(true)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/notify_authors/$targetUid")
        Result.failure(e)
    }

    fun observeMutedWords(uid: String): Flow<List<String>> = flow {
        if (uid.isBlank()) { emit(emptyList()); return@flow }
        emitAll(
            db.collection("users").document(uid).collection("muted_words")
                .snapshots()
                .map { snap ->
                    val now = System.currentTimeMillis()
                    snap.documents.mapNotNull { doc ->
                        val word = doc.getString("word") ?: return@mapNotNull null
                        val exp = doc.getTimestamp("expiresAt")?.toDate()?.time
                        if (exp == null || exp > now) word else null
                    }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users/$uid/muted_words", uid)
                    throw error
                }
        )
    }

    suspend fun addMutedWord(word: String, durationDays: Int? = null): Result<Unit> = try {
        val uid = requireUserId()
        val clean = word.trim().lowercase()
        require(clean.isNotEmpty()) { "Word cannot be empty" }
        val docId = clean.replace(Regex("[^a-z0-9_]"), "_").take(40).ifEmpty { "w_${clean.hashCode()}" }
        val payload = mutableMapOf<String, Any>(
            "word" to clean,
            "createdAt" to FieldValue.serverTimestamp()
        )
        if (durationDays != null && durationDays > 0) {
            payload["expiresAt"] = Timestamp(Date(System.currentTimeMillis() + durationDays * 86_400_000L))
        }
        db.collection("users").document(uid).collection("muted_words").document(docId).set(payload).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "users/muted_words")
        Result.failure(e)
    }

    suspend fun removeMutedWord(word: String): Result<Unit> = try {
        val uid = requireUserId()
        val clean = word.trim().lowercase()
        val snap = db.collection("users").document(uid).collection("muted_words")
            .whereEqualTo("word", clean).get().await()
        snap.documents.forEach { it.reference.delete().await() }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "users/muted_words")
        Result.failure(e)
    }

    // Stories
    fun observeActiveStories(): Flow<List<TextStory>> = flow {
        val uid = requireUserId()
        emitAll(
            db.collection("stories")
                .whereEqualTo("isActive", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TextStory::class.java, ServerTimestampBehavior.ESTIMATE)
                        .filter { it.isUnexpired() }
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "stories", uid)
                    throw error
                }
        )
    }

    suspend fun createStory(
        author: UserProfile,
        text: String,
        backgroundColor: String,
        gradientEndColor: String,
        fontStyle: String,
        textAlign: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        val cleanText = text.trim()
        require(cleanText.isNotEmpty() && cleanText.length <= 350) { "Story must be 1-350 characters" }
        val expiresAt = Timestamp(Date(System.currentTimeMillis() + 24 * 3_600_000L))
        db.collection("stories").document().set(
            mapOf(
                "authorId" to uid,
                "authorName" to author.name,
                "authorUsername" to author.username,
                "authorVerifiedType" to author.verifiedType,
                "authorAvatarColor" to author.avatarColor,
                "text" to cleanText,
                "backgroundColor" to backgroundColor,
                "gradientEndColor" to gradientEndColor,
                "fontStyle" to fontStyle,
                "textAlign" to textAlign,
                "viewsCount" to 0L,
                "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "expiresAt" to expiresAt
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "stories")
        Result.failure(e)
    }

    suspend fun recordStoryView(story: TextStory, viewer: UserProfile) {
        try {
            val uid = requireUserId()
            val viewerRef = db.collection("stories").document(story.id).collection("viewers").document(uid)
            if (!viewerRef.get().await().exists()) {
                viewerRef.set(
                    mapOf(
                        "viewerId" to uid,
                        "storyAuthorId" to story.authorId,
                        "viewerName" to viewer.name,
                        "viewerUsername" to viewer.username,
                        "viewerAvatarColor" to viewer.avatarColor,
                        "viewedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                db.collection("stories").document(story.id).update(
                    mapOf("viewsCount" to FieldValue.increment(1L))
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "stories/${story.id}/viewers")
        }
    }

    suspend fun getStoryViewers(storyId: String): List<StoryViewer> = try {
        val uid = requireUserId()
        db.collection("stories").document(storyId).collection("viewers")
            .whereEqualTo("storyAuthorId", uid)
            .get()
            .await()
            .toObjects(StoryViewer::class.java, ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { it.viewedAt ?: Timestamp(0, 0) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.LIST, "stories/$storyId/viewers")
        emptyList()
    }

    // Verification & Admin Queue
    fun observeVerificationRequests(uid: String): Flow<List<VerificationRequest>> = flow {
        if (uid.isBlank()) { emit(emptyList()); return@flow }
        val query = if (isCurrentUserAdmin()) {
            db.collection("verification_requests")
        } else {
            db.collection("verification_requests").whereEqualTo("userId", uid)
        }
        emitAll(
            query.snapshots()
                .map { snap ->
                    snap.toObjects(VerificationRequest::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.submittedAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "verification_requests", uid)
                    throw error
                }
        )
    }

    suspend fun submitVerificationRequest(
        user: UserProfile,
        requestedType: String,
        organizationName: String,
        reason: String
    ): Result<Unit> = try {
        val uid = requireUserId()
        val cleanReason = reason.trim()
        require(cleanReason.length >= 10) { "Justification must be at least 10 characters" }
        val payload = mutableMapOf<String, Any?>(
            "userId" to uid,
            "userName" to user.name,
            "username" to user.username,
            "requestedType" to requestedType,
            "organizationName" to organizationName.trim().take(60).ifEmpty { null },
            "reason" to cleanReason.take(400),
            "status" to "pending",
            "submittedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null } as Map<String, Any>
        db.collection("verification_requests").document().set(payload).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "verification_requests")
        Result.failure(e)
    }

    suspend fun reviewVerificationRequest(
        req: VerificationRequest,
        approve: Boolean,
        reviewer: UserProfile
    ): Result<Unit> = try {
        requireUserId()
        val newStatus = if (approve) "approved" else "rejected"
        db.collection("verification_requests").document(req.id).update(
            mapOf(
                "status" to newStatus,
                "reviewedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        if (approve) {
            val userUpdates = mutableMapOf<String, Any>(
                "verifiedType" to req.requestedType,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (req.requestedType == "business" && req.organizationName.isNotBlank()) {
                userUpdates["officialLabel"] = "Official Organization · ${req.organizationName}"
            } else if (req.requestedType == "government") {
                userUpdates["officialLabel"] = "Official Institution"
            }
            db.collection("users").document(req.userId).update(userUpdates).await()
            sendNotification(
                targetUserId = req.userId,
                actor = reviewer,
                type = "verified",
                postId = null,
                previewText = "Your ${req.requestedType} verification badge has been approved!"
            )
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "verification_requests/${req.id}")
        Result.failure(e)
    }

    // Notifications
    fun observeNotifications(uid: String): Flow<List<AppNotification>> = flow {
        if (uid.isBlank()) { emit(emptyList()); return@flow }
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { error ->
                    logStreamError(error, OperationType.LIST, "users/$uid/notifications", uid)
                    throw error
                }
        )
    }

    suspend fun markNotificationRead(notifId: String) {
        try {
            val uid = requireUserId()
            db.collection("users").document(uid).collection("notifications")
                .document(notifId).update("isRead", true).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications/$notifId")
        }
    }

    private suspend fun sendNotification(
        targetUserId: String,
        actor: UserProfile,
        type: String,
        postId: String?,
        previewText: String
    ) {
        if (targetUserId.isBlank() || targetUserId == actor.uid) return
        try {
            val payload = mutableMapOf<String, Any?>(
                "targetUserId" to targetUserId,
                "actorId" to actor.uid,
                "actorName" to actor.name,
                "actorUsername" to actor.username,
                "actorAvatarColor" to actor.avatarColor,
                "actorVerifiedType" to actor.verifiedType,
                "type" to type,
                "postId" to postId,
                "previewText" to previewText.take(200),
                "isRead" to false,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>
            db.collection("users").document(targetUserId).collection("notifications")
                .document().set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "users/$targetUserId/notifications")
        }
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
