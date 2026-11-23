package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Poll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CommentSortOrder
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.ReactionType
import com.example.data.model.UserProfile
import com.example.ui.components.CommentItemCard
import com.example.ui.components.CommentSortBar
import com.example.ui.components.ConfirmDeletePostDialog
import com.example.ui.components.EditPostDialog
import com.example.ui.components.MonogramAvatar
import com.example.ui.components.PostCard
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.LocalNabdColors
import com.example.util.sharePostAsTextCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposerScreen(
    currentUser: UserProfile,
    quotePost: Post? = null,
    isPublishing: Boolean,
    onClose: () -> Unit,
    onPublish: (
        segments: List<String>,
        pollOptions: List<String>?,
        pollDurationHours: Int
    ) -> Unit
) {
    BackHandler(onBack = onClose)
    val maxChars = currentUser.maxPostLength
    val segments = remember { mutableStateListOf("") }
    var showPoll by remember { mutableStateOf(false) }
    val pollOptions = remember { mutableStateListOf("", "") }
    var pollDurationHours by remember { mutableIntStateOf(24) }

    val canPublish = segments.any { it.trim().isNotEmpty() } &&
        segments.all { it.length <= maxChars } &&
        (!showPoll || pollOptions.count { it.trim().isNotEmpty() } >= 2)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (quotePost != null) "Quote Pulse" else "New Pulse") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close composer")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val activePoll = if (showPoll) {
                                pollOptions.map { it.trim() }.filter { it.isNotEmpty() }.take(4)
                            } else null
                            onPublish(segments.toList(), activePoll, pollDurationHours)
                        },
                        enabled = canPublish && !isPublishing,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("publish_pulse_button")
                    ) {
                        if (isPublishing) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Publish")
                        }
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MonogramAvatar(
                    name = currentUser.name,
                    colorHex = currentUser.avatarColor,
                    avatarPattern = currentUser.avatarPattern,
                    size = 40.dp
                )
                Spacer(Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(currentUser.name, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(4.dp))
                        VerifiedBadge(currentUser.displayedVerifiedType)
                    }
                    Text(
                        text = "@${currentUser.username} · Limit: $maxChars chars",
                        style = MaterialTheme.typography.labelMedium,
                        color = LocalNabdColors.current.mutedText
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            segments.forEachIndexed { index, text ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { if (it.length <= maxChars) segments[index] = it },
                        label = {
                            Text(
                                if (segments.size > 1) "Thread ${index + 1}/${segments.size} (${text.length}/$maxChars)"
                                else "What's on your mind? (${text.length}/$maxChars)"
                            )
                        },
                        placeholder = { Text("Share an idea, #hashtag, or @mention…") },
                        minLines = 4,
                        maxLines = 10,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("composer_segment_$index")
                    )
                    if (segments.size > 1) {
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextButton(onClick = { segments.removeAt(index) }) {
                                Text("Remove segment")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            if (quotePost != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LocalNabdColors.current.hairline),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "${quotePost.authorName} (@${quotePost.authorUsername})",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(quotePost.content, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (segments.size < 6) {
                    OutlinedButton(
                        onClick = { segments.add("") },
                        modifier = Modifier.testTag("add_thread_segment_btn")
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add to thread")
                    }
                }
                if (quotePost == null) {
                    OutlinedButton(
                        onClick = { showPoll = !showPoll },
                        modifier = Modifier.testTag("toggle_poll_btn")
                    ) {
                        Icon(Icons.Outlined.Poll, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (showPoll) "Remove poll" else "Add poll")
                    }
                }
            }

            if (showPoll) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, LocalNabdColors.current.hairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Text("Text Poll Options (2–4)", style = MaterialTheme.typography.titleMedium)
                        pollOptions.forEachIndexed { idx, opt ->
                            OutlinedTextField(
                                value = opt,
                                onValueChange = { if (it.length <= 80) pollOptions[idx] = it },
                                label = { Text("Option ${idx + 1}") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (pollOptions.size < 4) {
                            TextButton(onClick = { pollOptions.add("") }) {
                                Text("+ Add Option")
                            }
                        }
                        Text("Duration", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1 to "1h", 24 to "1d", 72 to "3d", 168 to "7d").forEach { (hrs, label) ->
                                FilterChip(
                                    selected = pollDurationHours == hrs,
                                    onClick = { pollDurationHours = hrs },
                                    label = { Text(label) }
                                )
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
fun PostDetailScreen(
    post: Post,
    allPosts: List<Post>,
    currentUser: UserProfile,
    likedIds: Set<String>,
    repostedIds: Set<String>,
    bookmarkIds: Set<String>,
    userReactions: Map<String, ReactionType> = emptyMap(),
    comments: List<PostComment> = emptyList(),
    commentReactions: Map<String, ReactionType> = emptyMap(),
    onObserveComments: (String) -> Unit = {},
    onHydrateCommentReactions: (String, List<PostComment>) -> Unit = { _, _ -> },
    onBack: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onOpenPost: (Post) -> Unit,
    onQuotePost: (Post) -> Unit,
    onLikePost: (Post) -> Unit,
    onReactPost: ((Post, ReactionType) -> Unit)? = null,
    onReactionSummaryClick: ((Post) -> Unit)? = null,
    onRepostPost: (Post) -> Unit,
    onBookmarkPost: (Post) -> Unit,
    onVotePoll: (Post, Int) -> Unit,
    onPinReply: (Post, String?) -> Unit,
    onSendReply: (Post, String) -> Unit,
    onAddComment: ((Post, String, String?) -> Unit)? = null,
    onDeleteComment: ((String, String) -> Unit)? = null,
    onReactComment: ((String, PostComment, ReactionType) -> Unit)? = null,
    onEditPost: ((Post, String) -> Unit)? = null,
    onDeletePost: ((Post) -> Unit)? = null,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onShowMessage: ((String) -> Unit)? = null
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var replyText by remember { mutableStateOf("") }
    var sortOrder by remember { mutableStateOf(CommentSortOrder.TOP) }
    var replyingToComment by remember { mutableStateOf<PostComment?>(null) }
    var editTarget by remember { mutableStateOf<Post?>(null) }
    var deleteTarget by remember { mutableStateOf<Post?>(null) }

    LaunchedEffect(post.id) {
        onObserveComments(post.id)
    }
    LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) {
            onHydrateCommentReactions(post.id, comments)
        }
    }

    val topLevelComments = remember(comments, sortOrder) {
        val parents = comments.filter { it.parentCommentId.isNullOrBlank() }
        when (sortOrder) {
            CommentSortOrder.TOP -> parents.sortedByDescending { it.totalReactionsCount }
            CommentSortOrder.NEWEST -> parents.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            CommentSortOrder.OLDEST -> parents.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
        }
    }

    val directReplies = remember(allPosts, post.id, post.pinnedReplyId, sortOrder) {
        val list = allPosts.filter { it.replyToPostId == post.id }
        val sorted = when (sortOrder) {
            CommentSortOrder.TOP -> list.sortedByDescending { it.effectiveReactionsCount }
            CommentSortOrder.NEWEST -> list.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            CommentSortOrder.OLDEST -> list.sortedBy { it.createdAt?.toDate()?.time ?: 0L }
        }
        val pinned = sorted.firstOrNull { it.id == post.pinnedReplyId }
        if (pinned != null) listOf(pinned) + sorted.filter { it.id != pinned.id } else sorted
    }

    val threadContinuations = remember(allPosts, post.id) {
        allPosts.filter { it.threadParentId == post.id && it.replyToPostId == null }
            .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pulse Thread") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth().imePadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (replyingToComment != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Replying to @${replyingToComment!!.authorUsername}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = { replyingToComment = null }) {
                                Text("Cancel")
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        OutlinedTextField(
                            value = replyText,
                            onValueChange = { if (it.length <= 1000) replyText = it },
                            placeholder = {
                                Text(
                                    if (replyingToComment != null) "Write a nested reply…"
                                    else "Write a comment…"
                                )
                            },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier.weight(1f).testTag("reply_input")
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val clean = replyText.trim()
                                if (clean.isNotEmpty()) {
                                    if (onAddComment != null) {
                                        onAddComment(post, clean, replyingToComment?.id)
                                    } else {
                                        onSendReply(post, clean)
                                    }
                                    replyText = ""
                                    replyingToComment = null
                                }
                            },
                            enabled = replyText.isNotBlank(),
                            modifier = Modifier.testTag("send_reply_button")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send comment",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) {
            item(key = "main_${post.id}") {
                PostCard(
                    post = post,
                    currentUid = currentUser.uid,
                    isLiked = post.id in likedIds,
                    isReposted = post.id in repostedIds,
                    isBookmarked = post.id in bookmarkIds,
                    userReaction = userReactions[post.id],
                    onAuthorClick = onOpenProfile,
                    onPostClick = {},
                    onLikeClick = onLikePost,
                    onReactClick = onReactPost,
                    onReactionSummaryClick = onReactionSummaryClick,
                    onRepostClick = onRepostPost,
                    onQuoteClick = onQuotePost,
                    onReplyClick = {},
                    onBookmarkClick = onBookmarkPost,
                    onShareClick = { sharePostAsTextCard(context, it) },
                    onVotePoll = onVotePoll,
                    onHashtagClick = onHashtagClick,
                    onMentionClick = onMentionClick,
                    onEditPost = if (onEditPost != null) { p -> editTarget = p } else null,
                    onDeletePost = if (onDeletePost != null) { p -> deleteTarget = p } else null,
                    onCopySuccess = onShowMessage
                )
            }

            if (threadContinuations.isNotEmpty()) {
                item {
                    Text(
                        text = "Thread Continuation",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(threadContinuations, key = { "thread_${it.id}" }) { seg ->
                    PostCard(
                        post = seg,
                        currentUid = currentUser.uid,
                        isLiked = seg.id in likedIds,
                        isReposted = seg.id in repostedIds,
                        isBookmarked = seg.id in bookmarkIds,
                        userReaction = userReactions[seg.id],
                        onAuthorClick = onOpenProfile,
                        onPostClick = onOpenPost,
                        onLikeClick = onLikePost,
                        onReactClick = onReactPost,
                        onReactionSummaryClick = onReactionSummaryClick,
                        onRepostClick = onRepostPost,
                        onQuoteClick = onQuotePost,
                        onReplyClick = onOpenPost,
                        onBookmarkClick = onBookmarkPost,
                        onShareClick = { sharePostAsTextCard(context, it) },
                        onVotePoll = onVotePoll,
                        onHashtagClick = onHashtagClick,
                        onMentionClick = onMentionClick,
                        onCopySuccess = onShowMessage
                    )
                }
            }

            item {
                val countTotal = topLevelComments.size + directReplies.size
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Comments ($countTotal)",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                CommentSortBar(selectedSort = sortOrder, onSelectSort = { sortOrder = it })
                HorizontalDivider(thickness = 0.5.dp, color = LocalNabdColors.current.hairline)
            }

            // Dedicated /posts/{postId}/comments thread with nested replies & Facebook reactions
            items(topLevelComments, key = { "c_${it.id}" }) { comment ->
                val nested = comments.filter { it.parentCommentId == comment.id }
                    .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
                Column {
                    CommentItemCard(
                        comment = comment,
                        currentUid = currentUser.uid,
                        postAuthorId = post.authorId,
                        myReaction = commentReactions[comment.id],
                        isNested = false,
                        onAuthorClick = onOpenProfile,
                        onReplyToComment = { replyingToComment = it },
                        onReactComment = { c, r -> onReactComment?.invoke(post.id, c, r) },
                        onDeleteComment = { c -> onDeleteComment?.invoke(post.id, c.id) }
                    )
                    nested.forEach { child ->
                        CommentItemCard(
                            comment = child,
                            currentUid = currentUser.uid,
                            postAuthorId = post.authorId,
                            myReaction = commentReactions[child.id],
                            isNested = true,
                            onAuthorClick = onOpenProfile,
                            onReplyToComment = {},
                            onReactComment = { c, r -> onReactComment?.invoke(post.id, c, r) },
                            onDeleteComment = { c -> onDeleteComment?.invoke(post.id, c.id) }
                        )
                    }
                }
            }

            // Legacy thread replies if any
            items(directReplies, key = { it.id }) { reply ->
                val isPinned = post.pinnedReplyId == reply.id
                val childReplies = allPosts.filter { it.replyToPostId == reply.id }
                Column {
                    PostCard(
                        post = reply,
                        currentUid = currentUser.uid,
                        isLiked = reply.id in likedIds,
                        isReposted = reply.id in repostedIds,
                        isBookmarked = reply.id in bookmarkIds,
                        userReaction = userReactions[reply.id],
                        isPinned = isPinned,
                        onAuthorClick = onOpenProfile,
                        onPostClick = onOpenPost,
                        onLikeClick = onLikePost,
                        onReactClick = onReactPost,
                        onReactionSummaryClick = onReactionSummaryClick,
                        onRepostClick = onRepostPost,
                        onQuoteClick = onQuotePost,
                        onReplyClick = onOpenPost,
                        onBookmarkClick = onBookmarkPost,
                        onShareClick = { sharePostAsTextCard(context, it) },
                        onVotePoll = onVotePoll,
                        onHashtagClick = onHashtagClick,
                        onMentionClick = onMentionClick,
                        onPinReply = if (post.authorId == currentUser.uid) {
                            { onPinReply(post, if (isPinned) null else reply.id) }
                        } else null
                    )
                    childReplies.take(2).forEach { nested ->
                        Box(modifier = Modifier.padding(start = 28.dp)) {
                            PostCard(
                                post = nested,
                                currentUid = currentUser.uid,
                                isLiked = nested.id in likedIds,
                                isReposted = nested.id in repostedIds,
                                isBookmarked = nested.id in bookmarkIds,
                                userReaction = userReactions[nested.id],
                                onAuthorClick = onOpenProfile,
                                onPostClick = onOpenPost,
                                onLikeClick = onLikePost,
                                onReactClick = onReactPost,
                                onRepostClick = onRepostPost,
                                onQuoteClick = onQuotePost,
                                onReplyClick = onOpenPost,
                                onBookmarkClick = onBookmarkPost,
                                onShareClick = { sharePostAsTextCard(context, it) },
                                onVotePoll = onVotePoll,
                                onHashtagClick = onHashtagClick,
                                onMentionClick = onMentionClick
                            )
                        }
                    }
                }
            }
        }
    }

    editTarget?.let { target ->
        EditPostDialog(
            post = target,
            maxChars = currentUser.maxPostLength,
            onDismiss = { editTarget = null },
            onSave = { updated ->
                onEditPost?.invoke(target, updated)
                editTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        ConfirmDeletePostDialog(
            onDismiss = { deleteTarget = null },
            onConfirm = {
                onDeletePost?.invoke(target)
                deleteTarget = null
                if (target.id == post.id) onBack()
            }
        )
    }
}
