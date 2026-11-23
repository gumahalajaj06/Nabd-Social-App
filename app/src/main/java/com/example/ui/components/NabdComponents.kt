package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Post
import com.example.data.model.PostPoll
import com.example.data.model.ReactionType
import com.example.data.model.VerifiedType
import com.example.ui.theme.LocalNabdColors
import com.example.util.buildRichPostText
import com.example.util.copyTextToClipboard
import com.example.util.formatRelativeTime
import com.example.util.initialsFromName
import com.example.util.parseHexColor
import com.example.util.sharePostAsTextCard
import com.example.util.sharePostAsTextImageCard

@Composable
fun MonogramAvatar(
    name: String,
    colorHex: String,
    size: Dp = 44.dp,
    avatarPattern: String = "solid",
    hasUnseenStory: Boolean = false,
    hasSeenStory: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bg = parseHexColor(colorHex, MaterialTheme.colorScheme.primary)
    val ringModifier = when {
        hasUnseenStory -> Modifier.border(
            width = 2.5.dp,
            brush = Brush.linearGradient(
                listOf(
                    MaterialTheme.colorScheme.primary,
                    Color(0xFF38BDF8),
                    Color(0xFF10B981)
                )
            ),
            shape = CircleShape
        )
        hasSeenStory -> Modifier.border(
            width = 1.5.dp,
            color = LocalNabdColors.current.hairline,
            shape = CircleShape
        )
        else -> Modifier
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .then(ringModifier)
            .padding(if (hasUnseenStory || hasSeenStory) 3.dp else 0.dp)
            .size(size)
            .clip(CircleShape)
            .background(bg)
    ) {
        if (avatarPattern != "solid") {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val overlay = Color.White.copy(alpha = 0.16f)
                when (avatarPattern) {
                    "rings" -> {
                        drawCircle(color = overlay, radius = this.size.minDimension * 0.42f, style = Stroke(width = 2f))
                        drawCircle(color = overlay, radius = this.size.minDimension * 0.28f, style = Stroke(width = 2f))
                    }
                    "dots" -> {
                        val step = this.size.minDimension / 4f
                        for (x in 1..3) {
                            for (y in 1..3) {
                                drawCircle(color = overlay, radius = 2.5f, center = Offset(x * step, y * step))
                            }
                        }
                    }
                    "diagonal" -> {
                        val step = this.size.minDimension / 4f
                        for (i in -2..4) {
                            drawLine(
                                color = overlay,
                                start = Offset(i * step, 0f),
                                end = Offset(i * step + this.size.height, this.size.height),
                                strokeWidth = 3f
                            )
                        }
                    }
                    "pulse" -> {
                        val path = Path().apply {
                            moveTo(0f, this@Canvas.size.height * 0.72f)
                            lineTo(this@Canvas.size.width * 0.3f, this@Canvas.size.height * 0.72f)
                            lineTo(this@Canvas.size.width * 0.45f, this@Canvas.size.height * 0.45f)
                            lineTo(this@Canvas.size.width * 0.6f, this@Canvas.size.height * 0.85f)
                            lineTo(this@Canvas.size.width * 0.75f, this@Canvas.size.height * 0.62f)
                            lineTo(this@Canvas.size.width, this@Canvas.size.height * 0.72f)
                        }
                        drawPath(path, color = overlay, style = Stroke(width = 3f, cap = StrokeCap.Round))
                    }
                }
            }
        }
        Text(
            text = initialsFromName(name),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.36f).sp,
            maxLines = 1
        )
    }
}

@Composable
fun VerifiedBadge(
    verifiedTypeRaw: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalNabdColors.current
    val type = VerifiedType.fromKey(verifiedTypeRaw)
    when (type) {
        VerifiedType.NONE -> Unit
        VerifiedType.PERSONAL -> Icon(
            imageVector = Icons.Filled.Verified,
            contentDescription = "Personal verified badge",
            tint = colors.verifiedPersonal,
            modifier = modifier.size(16.dp)
        )
        VerifiedType.BUSINESS -> Icon(
            imageVector = Icons.Filled.Business,
            contentDescription = "Business verified badge",
            tint = colors.verifiedBusiness,
            modifier = modifier.size(16.dp)
        )
        VerifiedType.GOVERNMENT -> Icon(
            imageVector = Icons.Outlined.AccountBalance,
            contentDescription = "Government verified badge",
            tint = colors.verifiedGovernment,
            modifier = modifier.size(16.dp)
        )
    }
}

@Composable
fun OfflineStatusBanner(isOnline: Boolean, queuedCount: Int = 0) {
    if (!isOnline || queuedCount > 0) {
        val colors = LocalNabdColors.current
        Surface(
            color = colors.warningAmber.copy(alpha = 0.14f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.CloudOff,
                    contentDescription = "Offline status",
                    tint = colors.warningAmber,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (!isOnline) {
                        "Offline mode · Reading cached feed & queuing messages"
                    } else {
                        "Syncing $queuedCount queued message(s)…"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.warningAmber
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostCard(
    post: Post,
    currentUid: String,
    isLiked: Boolean,
    isReposted: Boolean,
    isBookmarked: Boolean,
    userReaction: ReactionType? = if (isLiked) ReactionType.LIKE else null,
    isPinned: Boolean = false,
    isPinnedToProfile: Boolean = false,
    pinnedProfileLabel: String = "Pinned Pulse",
    onAuthorClick: (String) -> Unit,
    onPostClick: (Post) -> Unit,
    onLikeClick: (Post) -> Unit,
    onReactClick: ((Post, ReactionType) -> Unit)? = null,
    onReactionSummaryClick: ((Post) -> Unit)? = null,
    onRepostClick: (Post) -> Unit,
    onQuoteClick: (Post) -> Unit,
    onReplyClick: (Post) -> Unit,
    onBookmarkClick: (Post) -> Unit,
    onShareClick: (Post) -> Unit,
    onVotePoll: (Post, Int) -> Unit,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onEditPost: ((Post) -> Unit)? = null,
    onDeletePost: ((Post) -> Unit)? = null,
    onPinToProfile: ((Post) -> Unit)? = null,
    onReportPost: ((Post) -> Unit)? = null,
    onBlockAuthor: ((Post) -> Unit)? = null,
    onPinReply: ((Post) -> Unit)? = null,
    onCopySuccess: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val extColors = LocalNabdColors.current
    val uriHandler = LocalUriHandler.current
    var showMenu by remember { mutableStateOf(false) }
    var showRepostMenu by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }

    val activeReaction = userReaction
    val reactionScale by animateFloatAsState(
        targetValue = if (activeReaction != null) 1.15f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "reactionScale"
    )
    val reactionTint by animateColorAsState(
        targetValue = if (activeReaction != null) {
            parseHexColor(activeReaction.colorHex, extColors.likeCoral)
        } else {
            extColors.mutedText
        },
        label = "reactionTint"
    )
    val repostColor by animateColorAsState(
        targetValue = if (isReposted) extColors.repostEmerald else extColors.mutedText,
        label = "repostColor"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onPostClick(post) }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (isPinned || isPinnedToProfile) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 52.dp, bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = "Pinned",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (isPinnedToProfile) pinnedProfileLabel else "Pinned Reply",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        if (post.authorLikedReply) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 52.dp, bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = "Liked by author",
                    tint = extColors.likeCoral,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "Liked by author",
                    style = MaterialTheme.typography.labelMedium,
                    color = extColors.likeCoral
                )
            }
        }

        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            MonogramAvatar(
                name = post.authorName,
                colorHex = post.authorAvatarColor,
                size = 44.dp,
                onClick = { onAuthorClick(post.authorId) }
            )
            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onAuthorClick(post.authorId) }
                    ) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (post.authorVerifiedType != "none") {
                            Spacer(Modifier.width(4.dp))
                            VerifiedBadge(post.authorVerifiedType)
                        }
                        Spacer(Modifier.width(6.dp))
                        val editedBadge = if (post.isEdited) " · Edited" else ""
                        Text(
                            text = "@${post.authorUsername} · ${formatRelativeTime(post.createdAt)}$editedBadge",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extColors.mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(36.dp).testTag("post_more_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreHoriz,
                                contentDescription = "Post options",
                                tint = extColors.mutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (post.authorId == currentUid && onEditPost != null) {
                                DropdownMenuItem(
                                    text = { Text("Edit pulse") },
                                    leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                                    onClick = {
                                        showMenu = false
                                        onEditPost(post)
                                    }
                                )
                            }
                            if (post.authorId == currentUid && onPinToProfile != null && post.replyToPostId == null) {
                                DropdownMenuItem(
                                    text = { Text(if (isPinnedToProfile) "Unpin from profile" else "Pin to profile") },
                                    leadingIcon = { Icon(Icons.Outlined.PushPin, null) },
                                    onClick = {
                                        showMenu = false
                                        onPinToProfile(post)
                                    }
                                )
                            }
                            if (onPinReply != null) {
                                DropdownMenuItem(
                                    text = { Text(if (isPinned) "Unpin reply" else "Pin reply") },
                                    leadingIcon = { Icon(Icons.Outlined.PushPin, null) },
                                    onClick = {
                                        showMenu = false
                                        onPinReply(post)
                                    }
                                )
                            }
                            if (post.authorId == currentUid && onDeletePost != null) {
                                DropdownMenuItem(
                                    text = { Text("Delete pulse") },
                                    leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null) },
                                    onClick = {
                                        showMenu = false
                                        onDeletePost(post)
                                    }
                                )
                            }
                            if (post.authorId != currentUid && onReportPost != null) {
                                DropdownMenuItem(
                                    text = { Text("Report post") },
                                    leadingIcon = { Icon(Icons.Outlined.Flag, null) },
                                    onClick = {
                                        showMenu = false
                                        onReportPost(post)
                                    }
                                )
                            }
                            if (post.authorId != currentUid && onBlockAuthor != null) {
                                DropdownMenuItem(
                                    text = { Text("Block @${post.authorUsername}") },
                                    leadingIcon = { Icon(Icons.Outlined.Block, null) },
                                    onClick = {
                                        showMenu = false
                                        onBlockAuthor(post)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                val annotated = remember(post.content) {
                    buildRichPostText(
                        text = post.content,
                        accentColor = Color(0xFF0D9488),
                        urlColor = Color(0xFF2563EB)
                    )
                }
                @Suppress("DEPRECATION")
                ClickableText(
                    text = annotated,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    onClick = { offset ->
                        val hash = annotated.getStringAnnotations("HASHTAG", offset, offset).firstOrNull()
                        val mention = annotated.getStringAnnotations("MENTION", offset, offset).firstOrNull()
                        val url = annotated.getStringAnnotations("URL", offset, offset).firstOrNull()
                        when {
                            hash != null -> onHashtagClick(hash.item)
                            mention != null -> onMentionClick(mention.item)
                            url != null -> try {
                                uriHandler.openUri(url.item)
                            } catch (_: Exception) {
                            }
                            else -> onPostClick(post)
                        }
                    }
                )

                if (!post.quoteContent.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, extColors.hairline),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = post.quoteAuthorName ?: "Author",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "@${post.quoteAuthorUsername ?: "user"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = extColors.mutedText
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = post.quoteContent,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (post.poll != null && post.poll.options.size >= 2) {
                    Spacer(Modifier.height(10.dp))
                    PollSection(
                        poll = post.poll,
                        currentUid = currentUid,
                        onVote = { optionIdx -> onVotePoll(post, optionIdx) }
                    )
                }

                // Reaction summary chips + View Count
                val totalReacts = post.effectiveReactionsCount
                if (totalReacts > 0 || post.viewsCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    val topEmojis = remember(post.reactionsCount, totalReacts) {
                        val sorted = post.reactionsCount.entries
                            .filter { it.value > 0L }
                            .sortedByDescending { it.value }
                            .take(3)
                            .map { ReactionType.fromKey(it.key).emoji }
                        if (sorted.isEmpty() && totalReacts > 0L) listOf("👍") else sorted
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = totalReacts > 0 && onReactionSummaryClick != null) {
                                onReactionSummaryClick?.invoke(post)
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        if (totalReacts > 0) {
                            Text(
                                text = "${topEmojis.joinToString("")} $totalReacts",
                                style = MaterialTheme.typography.labelMedium,
                                color = extColors.mutedText
                            )
                        }
                        if (post.viewsCount > 0) {
                            Text(
                                text = "${post.viewsCount} views",
                                style = MaterialTheme.typography.labelMedium,
                                color = extColors.mutedText
                            )
                        }
                    }
                }

                // Animated Facebook-style reaction picker popup
                AnimatedVisibility(
                    visible = showReactionPicker,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        shadowElevation = 6.dp,
                        border = BorderStroke(1.dp, extColors.hairline),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .testTag("reaction_picker_${post.id}")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            ReactionType.entries.forEach { reaction ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .clickable {
                                            showReactionPicker = false
                                            if (onReactClick != null) onReactClick(post, reaction)
                                            else onLikeClick(post)
                                        }
                                        .padding(horizontal = 7.dp, vertical = 4.dp)
                                        .testTag("react_${reaction.key}_${post.id}")
                                ) {
                                    Text(text = reaction.emoji, fontSize = 22.sp)
                                    Text(
                                        text = reaction.labelEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = extColors.mutedText
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Comment button
                    ActionCounterButton(
                        icon = Icons.AutoMirrored.Outlined.Chat,
                        count = post.effectiveCommentsCount,
                        tint = extColors.mutedText,
                        contentDescription = "Comments",
                        onClick = { onReplyClick(post) },
                        testTag = "reply_btn_${post.id}"
                    )

                    // Repost / Quote
                    Box {
                        ActionCounterButton(
                            icon = if (isReposted) Icons.Filled.Repeat else Icons.Outlined.Repeat,
                            count = post.repostsCount,
                            tint = repostColor,
                            contentDescription = "Repost or quote",
                            onClick = { showRepostMenu = true },
                            testTag = "repost_btn_${post.id}"
                        )
                        DropdownMenu(
                            expanded = showRepostMenu,
                            onDismissRequest = { showRepostMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (isReposted) "Undo Repost" else "Repost") },
                                leadingIcon = { Icon(Icons.Outlined.Repeat, null) },
                                onClick = {
                                    showRepostMenu = false
                                    onRepostClick(post)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Quote Post") },
                                leadingIcon = { Icon(Icons.Outlined.FormatQuote, null) },
                                onClick = {
                                    showRepostMenu = false
                                    onQuoteClick(post)
                                }
                            )
                        }
                    }

                    // Facebook-style Reaction trigger (Tap to toggle, Long-Press to open picker)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .combinedClickable(
                                onClick = {
                                    if (onReactClick != null) {
                                        onReactClick(post, activeReaction ?: ReactionType.LIKE)
                                    } else {
                                        onLikeClick(post)
                                    }
                                },
                                onLongClick = { showReactionPicker = !showReactionPicker }
                            )
                            .padding(horizontal = 6.dp, vertical = 6.dp)
                            .testTag("like_btn_${post.id}")
                    ) {
                        if (activeReaction != null) {
                            Text(
                                text = activeReaction.emoji,
                                fontSize = 17.sp,
                                modifier = Modifier.scale(reactionScale)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.ThumbUp,
                                contentDescription = "React",
                                tint = extColors.mutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (totalReacts > 0) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = totalReacts.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                color = reactionTint
                            )
                        }
                    }

                    // Bookmark
                    IconButton(
                        onClick = { onBookmarkClick(post) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                            .testTag("bookmark_btn_${post.id}")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else extColors.mutedText,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Share menu (Copy Text, Share Text/Link, Share as Text Image Card)
                    Box {
                        IconButton(
                            onClick = { showShareMenu = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .size(40.dp)
                                .testTag("share_btn_${post.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = "Share options",
                                tint = extColors.mutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Copy Post Text") },
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                onClick = {
                                    showShareMenu = false
                                    copyTextToClipboard(context, "Nabd Post", post.content)
                                    onCopySuccess?.invoke("Post text copied to clipboard")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Link / Text") },
                                leadingIcon = { Icon(Icons.Outlined.Share, null) },
                                onClick = {
                                    showShareMenu = false
                                    onShareClick(post)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share as Text Image Card") },
                                leadingIcon = { Icon(Icons.Outlined.Image, null) },
                                onClick = {
                                    showShareMenu = false
                                    sharePostAsTextImageCard(context, post)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    HorizontalDivider(thickness = 0.5.dp, color = extColors.hairline)
}

@Composable
private fun ActionCounterButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Long,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
    iconModifier: Modifier = Modifier,
    testTag: String = ""
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = iconModifier.size(18.dp)
        )
        if (count > 0) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = tint
            )
        }
    }
}

@Composable
fun PollSection(
    poll: PostPoll,
    currentUid: String,
    onVote: (Int) -> Unit
) {
    val extColors = LocalNabdColors.current
    val totalVotes = poll.votes.size
    val myVoteIdx = poll.votes[currentUid]?.toIntOrNull()
    val hasVoted = myVoteIdx != null
    val isExpired = (poll.expiresAt?.toDate()?.time ?: Long.MAX_VALUE) < System.currentTimeMillis()
    val showResults = hasVoted || isExpired

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        poll.options.forEachIndexed { index, label ->
            val votesForOption = poll.votes.values.count { it == index.toString() }
            val fraction = if (totalVotes == 0) 0f else votesForOption.toFloat() / totalVotes.toFloat()
            val percent = (fraction * 100).toInt()
            val isSelected = myVoteIdx == index

            Surface(
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else extColors.hairline
                ),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isExpired) { onVote(index) }
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (isSelected) {
                                Spacer(Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "Your vote",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        if (showResults) {
                            Text(
                                text = "$percent%",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else extColors.mutedText
                            )
                        }
                    }
                    if (showResults) {
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { fraction },
                            color = if (isSelected) MaterialTheme.colorScheme.primary else extColors.mutedText.copy(alpha = 0.5f),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
        Text(
            text = "$totalVotes vote${if (totalVotes == 1) "" else "s"} · ${if (isExpired) "Final results" else "Live poll"}",
            style = MaterialTheme.typography.labelMedium,
            color = extColors.mutedText
        )
    }
}

@Composable
fun NabdEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = LocalNabdColors.current.mutedText,
            textAlign = TextAlign.Center
        )
    }
}
