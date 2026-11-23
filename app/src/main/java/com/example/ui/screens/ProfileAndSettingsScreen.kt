package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.FollowRequest
import com.example.data.model.Post
import com.example.data.model.ReactionType
import com.example.data.model.TextStory
import com.example.data.model.UserProfile
import com.example.data.model.VerificationRequest
import com.example.ui.components.ConfirmDeletePostDialog
import com.example.ui.components.EditPostDialog
import com.example.ui.components.MonogramAvatar
import com.example.ui.components.NabdEmptyState
import com.example.ui.components.PostCard
import com.example.ui.components.UserFollowersSheet
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.LocalNabdColors
import com.example.util.buildRichPostText
import com.example.util.copyTextToClipboard
import com.example.util.formatJoinDate
import com.example.util.parseHexColor
import com.example.util.sharePostAsTextCard
import com.example.util.shareProfileLink

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profileUser: UserProfile,
    currentUser: UserProfile,
    allPosts: List<Post>,
    activeUsers: List<UserProfile> = emptyList(),
    activeStories: List<TextStory> = emptyList(),
    seenStoryIds: Set<String> = emptySet(),
    followingIds: Set<String>,
    followerIds: Set<String> = emptySet(),
    likedIds: Set<String>,
    repostedIds: Set<String>,
    bookmarkIds: Set<String>,
    blockedIds: Set<String>,
    mutedUserIds: Set<String> = emptySet(),
    notifyAuthorIds: Set<String> = emptySet(),
    userReactions: Map<String, ReactionType> = emptyMap(),
    showBack: Boolean,
    onBack: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenVerification: () -> Unit,
    onOpenSafetySettings: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    onOpenStoryViewer: ((List<TextStory>, Int) -> Unit)? = null,
    onToggleFollow: (UserProfile) -> Unit,
    onStartChat: (UserProfile) -> Unit,
    onToggleBlock: (UserProfile) -> Unit,
    onToggleMute: ((UserProfile) -> Unit)? = null,
    onToggleNotifyAuthor: ((UserProfile) -> Unit)? = null,
    onReportUser: ((UserProfile, String) -> Unit)? = null,
    onSetPinMode: ((String, String, List<Post>) -> Unit)? = null,
    onPinToProfile: ((Post) -> Unit)? = null,
    onOpenPost: (Post) -> Unit,
    onQuotePost: (Post) -> Unit,
    onLikePost: (Post) -> Unit,
    onReactPost: ((Post, ReactionType) -> Unit)? = null,
    onReactionSummaryClick: ((Post) -> Unit)? = null,
    onRepostPost: (Post) -> Unit,
    onBookmarkPost: (Post) -> Unit,
    onVotePoll: (Post, Int) -> Unit,
    onEditPost: ((Post, String) -> Unit)? = null,
    onDeletePost: ((Post) -> Unit)? = null,
    onHashtagClick: (String) -> Unit,
    onMentionClick: (String) -> Unit,
    onOpenProfile: ((String) -> Unit)? = null,
    onShowMessage: ((String) -> Unit)? = null,
    onSignOut: () -> Unit
) {
    if (showBack) BackHandler(onBack = onBack)
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val extColors = LocalNabdColors.current

    val isMe = profileUser.uid == currentUser.uid
    val isFollowing = profileUser.uid in followingIds
    val isBlocked = profileUser.uid in blockedIds
    val isMuted = profileUser.uid in mutedUserIds
    val isNotifying = profileUser.uid in notifyAuthorIds

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchInProfile by remember { mutableStateOf("") }
    var showSearchBar by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showPinModeMenu by remember { mutableStateOf(false) }
    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var followersSheetMode by remember { mutableStateOf<String?>(null) } // "following" | "followers"
    var editTarget by remember { mutableStateOf<Post?>(null) }
    var deleteTarget by remember { mutableStateOf<Post?>(null) }
    var reportDialogOpen by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }

    val authorStories = remember(activeStories, profileUser.uid) {
        activeStories.filter { it.authorId == profileUser.uid && it.isUnexpired() }
    }
    val hasUnseenStory = remember(authorStories, seenStoryIds) {
        authorStories.any { it.id !in seenStoryIds }
    }
    val hasSeenStory = authorStories.isNotEmpty() && !hasUnseenStory

    val userTopPosts = remember(allPosts, profileUser.uid) {
        allPosts.filter { it.authorId == profileUser.uid && it.replyToPostId == null }
    }
    val pinnedPost = remember(userTopPosts, profileUser.pinMode, profileUser.pinnedPostId) {
        when (profileUser.pinMode) {
            "top_engaged" -> userTopPosts.maxByOrNull { it.rawEngagementTotal() }
            else -> userTopPosts.firstOrNull { it.id == profileUser.pinnedPostId }
        }
    }
    val orderedTopPosts = remember(userTopPosts, pinnedPost) {
        if (pinnedPost == null) userTopPosts
        else listOf(pinnedPost) + userTopPosts.filter { it.id != pinnedPost.id }
    }
    val userReplies = remember(allPosts, profileUser.uid) {
        allPosts.filter { it.authorId == profileUser.uid && it.replyToPostId != null }
    }
    val userQuotes = remember(allPosts, profileUser.uid) {
        allPosts.filter { it.authorId == profileUser.uid && !it.quotePostId.isNullOrBlank() }
    }
    val userLikedPosts = remember(allPosts, likedIds) {
        allPosts.filter { it.id in likedIds }
    }

    val showLikesTab = isMe || !profileUser.privacy.hideLikesTab
    val showHighlightsTab = profileUser.preferences.showStoryHighlights || authorStories.isNotEmpty()

    val tabs = remember(isMe, showLikesTab, showHighlightsTab, userTopPosts.size, userReplies.size, userQuotes.size, userLikedPosts.size, authorStories.size) {
        buildList {
            add("Posts (${userTopPosts.size})" to "posts")
            add("Replies (${userReplies.size})" to "replies")
            add("Quotes (${userQuotes.size})" to "quotes")
            if (showLikesTab) add("Likes (${userLikedPosts.size})" to "likes")
            if (showHighlightsTab) add("Highlights (${authorStories.size})" to "highlights")
        }
    }
    val safeTabIdx = selectedTab.coerceIn(0, (tabs.size - 1).coerceAtLeast(0))
    val activeTabKey = tabs.getOrNull(safeTabIdx)?.second ?: "posts"

    val rawTabPosts = when (activeTabKey) {
        "replies" -> userReplies
        "quotes" -> userQuotes
        "likes" -> userLikedPosts
        else -> orderedTopPosts
    }
    val displayedPosts = remember(rawTabPosts, searchInProfile) {
        val q = searchInProfile.trim().lowercase()
        if (q.isEmpty()) rawTabPosts
        else rawTabPosts.filter {
            it.content.contains(q, ignoreCase = true) ||
                it.hashtags.any { tag -> tag.contains(q.removePrefix("#"), ignoreCase = true) }
        }
    }

    val mutualUsers = remember(activeUsers, followingIds, profileUser.uid, isMe) {
        if (isMe) emptyList()
        else activeUsers.filter { it.uid in followingIds && it.uid != profileUser.uid }.take(3)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(profileUser.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = "${userTopPosts.size} pulses",
                            style = MaterialTheme.typography.labelSmall,
                            color = extColors.mutedText
                        )
                    }
                },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSearchBar = !showSearchBar },
                        modifier = Modifier.testTag("profile_search_toggle_btn")
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Search profile pulses")
                    }
                    if (isMe) {
                        IconButton(
                            onClick = onOpenVerification,
                            modifier = Modifier.testTag("open_verification_btn")
                        ) {
                            Icon(Icons.Outlined.Verified, contentDescription = "Verification")
                        }
                        if (onOpenSettings != null) {
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier.testTag("open_settings_btn")
                            ) {
                                Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                            }
                        } else {
                            IconButton(
                                onClick = onOpenSafetySettings,
                                modifier = Modifier.testTag("open_safety_btn")
                            ) {
                                Icon(Icons.Outlined.Shield, contentDescription = "Safety & Moderation")
                            }
                        }
                        IconButton(
                            onClick = onSignOut,
                            modifier = Modifier.testTag("sign_out_btn")
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Sign out")
                        }
                    } else {
                        Box {
                            IconButton(onClick = { showMoreMenu = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "Profile options")
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Copy Profile Link") },
                                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                                    onClick = {
                                        showMoreMenu = false
                                        copyTextToClipboard(
                                            context,
                                            "Profile Link",
                                            "https://nabd.app/@${profileUser.username}"
                                        )
                                        onShowMessage?.invoke("Profile link copied")
                                    }
                                )
                                if (onToggleMute != null) {
                                    DropdownMenuItem(
                                        text = { Text(if (isMuted) "Unmute @${profileUser.username}" else "Mute @${profileUser.username}") },
                                        leadingIcon = { Icon(Icons.Outlined.VolumeOff, null) },
                                        onClick = {
                                            showMoreMenu = false
                                            onToggleMute(profileUser)
                                        }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text(if (isBlocked) "Unblock @${profileUser.username}" else "Block @${profileUser.username}") },
                                    leadingIcon = { Icon(Icons.Outlined.Block, null) },
                                    onClick = {
                                        showMoreMenu = false
                                        onToggleBlock(profileUser)
                                    }
                                )
                                if (onReportUser != null) {
                                    DropdownMenuItem(
                                        text = { Text("Report Account") },
                                        leadingIcon = { Icon(Icons.Outlined.Flag, null) },
                                        onClick = {
                                            showMoreMenu = false
                                            reportDialogOpen = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(bottom = 84.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MonogramAvatar(
                            name = profileUser.name,
                            colorHex = profileUser.avatarColor,
                            avatarPattern = profileUser.avatarPattern,
                            hasUnseenStory = hasUnseenStory,
                            hasSeenStory = hasSeenStory,
                            onClick = if (authorStories.isNotEmpty() && onOpenStoryViewer != null) {
                                { onOpenStoryViewer(authorStories, 0) }
                            } else null,
                            size = 76.dp
                        )

                        if (isMe) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onEditProfile,
                                    modifier = Modifier.testTag("edit_profile_button")
                                ) {
                                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Edit")
                                }
                                IconButton(
                                    onClick = { shareProfileLink(context, profileUser) },
                                    modifier = Modifier.testTag("share_profile_btn")
                                ) {
                                    Icon(Icons.Outlined.Share, contentDescription = "Share profile")
                                }
                                IconButton(
                                    onClick = { showAnalyticsDialog = true },
                                    modifier = Modifier.testTag("profile_analytics_btn")
                                ) {
                                    Icon(Icons.Outlined.Analytics, contentDescription = "Profile Analytics")
                                }
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (onToggleNotifyAuthor != null) {
                                    IconButton(
                                        onClick = { onToggleNotifyAuthor(profileUser) },
                                        modifier = Modifier.testTag("notify_author_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (isNotifying) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsNone,
                                            contentDescription = "Notify new posts",
                                            tint = if (isNotifying) MaterialTheme.colorScheme.primary else extColors.mutedText
                                        )
                                    }
                                }
                                val canDm = profileUser.privacy.allowDMsFrom != "nobody" &&
                                    (profileUser.privacy.allowDMsFrom != "following" || isFollowing)
                                if (canDm) {
                                    OutlinedButton(onClick = { onStartChat(profileUser) }) {
                                        Icon(Icons.AutoMirrored.Outlined.Chat, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Message")
                                    }
                                }
                                Button(onClick = { onToggleFollow(profileUser) }) {
                                    Text(if (isFollowing) "Following" else if (profileUser.isPrivate) "Request" else "Follow")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(profileUser.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        if (profileUser.displayedVerifiedType != "none") {
                            Spacer(Modifier.width(6.dp))
                            VerifiedBadge(profileUser.displayedVerifiedType)
                        }
                        if (profileUser.isPrivate) {
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Outlined.Lock, "Private account", modifier = Modifier.size(16.dp))
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                copyTextToClipboard(context, "Username", "@${profileUser.username}")
                                onShowMessage?.invoke("Copied @${profileUser.username}")
                            }
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = "@${profileUser.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = extColors.mutedText
                        )
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = "Copy username",
                            tint = extColors.mutedText,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    if (profileUser.roleTitle.isNotBlank() || profileUser.officialLabel.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (profileUser.officialLabel.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = profileUser.officialLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            if (profileUser.roleTitle.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = profileUser.roleTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (profileUser.bio.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        val richBio = remember(profileUser.bio) {
                            buildRichPostText(
                                text = profileUser.bio,
                                accentColor = Color(0xFF0D9488),
                                urlColor = Color(0xFF2563EB)
                            )
                        }
                        @Suppress("DEPRECATION")
                        ClickableText(
                            text = richBio,
                            style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            onClick = { offset ->
                                val hash = richBio.getStringAnnotations("HASHTAG", offset, offset).firstOrNull()
                                val mention = richBio.getStringAnnotations("MENTION", offset, offset).firstOrNull()
                                val url = richBio.getStringAnnotations("URL", offset, offset).firstOrNull()
                                when {
                                    hash != null -> onHashtagClick(hash.item)
                                    mention != null -> onMentionClick(mention.item)
                                    url != null -> try { uriHandler.openUri(url.item) } catch (_: Exception) {}
                                }
                            }
                        )
                    }

                    if (profileUser.pinnedNote.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, extColors.hairline),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.PushPin,
                                    contentDescription = "Pinned note",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = profileUser.pinnedNote,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (profileUser.category.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.Category, null, modifier = Modifier.size(15.dp), tint = extColors.mutedText)
                                Spacer(Modifier.width(4.dp))
                                Text(profileUser.category, style = MaterialTheme.typography.labelMedium, color = extColors.mutedText)
                            }
                        }
                        if (profileUser.location.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(15.dp), tint = extColors.mutedText)
                                Spacer(Modifier.width(4.dp))
                                Text(profileUser.location, style = MaterialTheme.typography.labelMedium, color = extColors.mutedText)
                            }
                        }
                        if (profileUser.website.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    val url = if (profileUser.website.startsWith("http")) profileUser.website else "https://${profileUser.website}"
                                    try { uriHandler.openUri(url) } catch (_: Exception) {}
                                }
                            ) {
                                Icon(Icons.Outlined.Link, null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(4.dp))
                                Text(profileUser.website, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(15.dp), tint = extColors.mutedText)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = formatJoinDate(profileUser.createdAt),
                                style = MaterialTheme.typography.labelMedium,
                                color = extColors.mutedText
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            text = "${profileUser.followingCount} Following",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { followersSheetMode = "following" }
                                .padding(vertical = 2.dp)
                        )
                        Text(
                            text = "${profileUser.followersCount} Followers",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { followersSheetMode = "followers" }
                                .padding(vertical = 2.dp)
                        )

                        if (isMe && onSetPinMode != null) {
                            Box {
                                FilterChip(
                                    selected = profileUser.pinMode == "top_engaged",
                                    onClick = { showPinModeMenu = true },
                                    leadingIcon = { Icon(Icons.Outlined.PushPin, null, modifier = Modifier.size(15.dp)) },
                                    label = {
                                        Text(if (profileUser.pinMode == "top_engaged") "Pin: Top Post" else "Pin: Manual")
                                    }
                                )
                                DropdownMenu(
                                    expanded = showPinModeMenu,
                                    onDismissRequest = { showPinModeMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Manual Pin (Choose from post menu)") },
                                        onClick = {
                                            showPinModeMenu = false
                                            onSetPinMode("manual", profileUser.pinnedPostId, userTopPosts)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Auto-Pin Top Engaged Post") },
                                        onClick = {
                                            showPinModeMenu = false
                                            onSetPinMode("top_engaged", "", userTopPosts)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (mutualUsers.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Followed by ${mutualUsers.joinToString(", ") { "@${it.username}" }}",
                            style = MaterialTheme.typography.labelMedium,
                            color = extColors.mutedText
                        )
                    }
                }

                if (showSearchBar) {
                    OutlinedTextField(
                        value = searchInProfile,
                        onValueChange = { searchInProfile = it },
                        placeholder = { Text("Search @${profileUser.username}'s pulses…") },
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        trailingIcon = {
                            if (searchInProfile.isNotEmpty()) {
                                IconButton(onClick = { searchInProfile = "" }) {
                                    Icon(Icons.Filled.Close, "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                ScrollableTabRow(
                    selectedTabIndex = safeTabIdx,
                    edgePadding = 12.dp
                ) {
                    tabs.forEachIndexed { index, (label, _) ->
                        Tab(
                            selected = safeTabIdx == index,
                            onClick = { selectedTab = index },
                            text = { Text(label) }
                        )
                    }
                }
            }

            if (activeTabKey == "highlights") {
                if (authorStories.isEmpty()) {
                    item {
                        NabdEmptyState(
                            icon = Icons.Outlined.BookmarkBorder,
                            title = "No active text highlights",
                            subtitle = "Active 24h text stories published by @${profileUser.username} appear here."
                        )
                    }
                } else {
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(authorStories, key = { it.id }) { story ->
                                val start = parseHexColor(story.backgroundColor, Color(0xFF0F172A))
                                val end = parseHexColor(story.gradientEndColor, Color(0xFF0D9488))
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier
                                        .size(width = 164.dp, height = 210.dp)
                                        .clip(RoundedCornerShape(18.dp))
                                        .clickable {
                                            val idx = authorStories.indexOf(story).coerceAtLeast(0)
                                            onOpenStoryViewer?.invoke(authorStories, idx)
                                        }
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.linearGradient(listOf(start, end)))
                                            .padding(14.dp)
                                    ) {
                                        Text(
                                            text = story.text,
                                            color = Color.White,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 5,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (displayedPosts.isEmpty()) {
                item {
                    NabdEmptyState(
                        icon = Icons.Outlined.BookmarkBorder,
                        title = "Nothing here yet",
                        subtitle = when (activeTabKey) {
                            "replies" -> "Replies authored by @${profileUser.username} appear here."
                            "quotes" -> "Quote pulses appear here."
                            "likes" -> "Reacted pulses appear here."
                            else -> "Published pulses will appear here."
                        }
                    )
                }
            } else {
                items(displayedPosts, key = { "${activeTabKey}_${it.id}" }) { post ->
                    val isPinnedCard = activeTabKey == "posts" && pinnedPost?.id == post.id
                    PostCard(
                        post = post,
                        currentUid = currentUser.uid,
                        isLiked = post.id in likedIds,
                        isReposted = post.id in repostedIds,
                        isBookmarked = post.id in bookmarkIds,
                        userReaction = userReactions[post.id],
                        isPinnedToProfile = isPinnedCard,
                        pinnedProfileLabel = if (profileUser.pinMode == "top_engaged") {
                            "Top Engaged · Pinned"
                        } else {
                            "Pinned Pulse"
                        },
                        onAuthorClick = { uid -> onOpenProfile?.invoke(uid) },
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
                        onEditPost = if (isMe && onEditPost != null) { p -> editTarget = p } else null,
                        onDeletePost = if (isMe && onDeletePost != null) { p -> deleteTarget = p } else null,
                        onPinToProfile = if (isMe && onPinToProfile != null) onPinToProfile else null,
                        onCopySuccess = onShowMessage
                    )
                }
            }
        }
    }

    if (showAnalyticsDialog) {
        val totalViews = userTopPosts.sumOf { it.viewsCount }
        val totalReactions = userTopPosts.sumOf { it.effectiveReactionsCount }
        val totalReposts = userTopPosts.sumOf { it.repostsCount }
        val topPulse = userTopPosts.maxByOrNull { it.rawEngagementTotal() }
        AlertDialog(
            onDismissRequest = { showAnalyticsDialog = false },
            title = { Text("Pulse Analytics") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Published Pulses: ${userTopPosts.size}")
                    Text("Total Views: $totalViews")
                    Text("Total Reactions: $totalReactions")
                    Text("Total Reposts: $totalReposts")
                    if (topPulse != null) {
                        HorizontalDivider(thickness = 0.5.dp, color = extColors.hairline)
                        Text("Top Performing Pulse:", style = MaterialTheme.typography.labelLarge)
                        Text(
                            text = topPulse.content,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = extColors.mutedText
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAnalyticsDialog = false }) { Text("Close") }
            }
        )
    }

    followersSheetMode?.let { mode ->
        val sheetUsers = remember(mode, activeUsers, followingIds, followerIds, isMe) {
            if (!isMe) {
                activeUsers.filter { it.uid in followingIds && it.uid != profileUser.uid }
            } else if (mode == "following") {
                activeUsers.filter { it.uid in followingIds }
            } else {
                activeUsers.filter { it.uid in followerIds }
            }
        }
        UserFollowersSheet(
            title = if (mode == "following") "Following" else if (isMe) "Followers" else "Mutual Connections",
            users = sheetUsers,
            onDismiss = { followersSheetMode = null },
            onUserClick = { uid -> onOpenProfile?.invoke(uid) }
        )
    }

    editTarget?.let { target ->
        EditPostDialog(
            post = target,
            maxChars = currentUser.maxPostLength,
            onDismiss = { editTarget = null },
            onSave = { updatedText ->
                onEditPost?.invoke(target, updatedText)
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
            }
        )
    }

    if (reportDialogOpen) {
        AlertDialog(
            onDismissRequest = { reportDialogOpen = false },
            title = { Text("Report @${profileUser.username}") },
            text = {
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    label = { Text("Reason (spam, impersonation, abuse…)") },
                    minLines = 2
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onReportUser?.invoke(profileUser, reportReason.ifBlank { "Abusive account" })
                        reportReason = ""
                        reportDialogOpen = false
                    }
                ) { Text("Submit Report") }
            },
            dismissButton = {
                TextButton(onClick = { reportDialogOpen = false }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationAndAdminScreen(
    currentUser: UserProfile,
    isAdmin: Boolean,
    requests: List<VerificationRequest>,
    isSubmitting: Boolean,
    onBack: () -> Unit,
    onSubmitRequest: (type: String, orgName: String, reason: String) -> Unit,
    onReviewRequest: (VerificationRequest, Boolean) -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedType by remember { mutableStateOf("personal") }
    var orgName by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Free Verification & Queue") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, LocalNabdColors.current.hairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Current Status: ${currentUser.verifiedType.uppercase()}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(6.dp))
                        VerifiedBadge(currentUser.verifiedType)
                    }
                    Text(
                        "Apply for free verification. Verified accounts unlock up to 2,000 characters per post and official badges.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalNabdColors.current.mutedText
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        listOf("personal" to "Personal (Blue)", "business" to "Business (Gold)", "government" to "Gov (Gray)").forEach { (key, label) ->
                            FilterChip(
                                selected = selectedType == key,
                                onClick = { selectedType = key },
                                label = { Text(label) }
                            )
                        }
                    }
                    if (selectedType == "business") {
                        OutlinedTextField(
                            value = orgName,
                            onValueChange = { if (it.length <= 60) orgName = it },
                            label = { Text("Organization / Brand Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { if (it.length <= 400) reason = it },
                        label = { Text("Justification (min 10 chars)") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("verification_reason_input")
                    )
                    Button(
                        onClick = {
                            onSubmitRequest(selectedType, orgName, reason)
                            reason = ""
                            orgName = ""
                        },
                        enabled = reason.trim().length >= 10 && !isSubmitting,
                        modifier = Modifier.fillMaxWidth().testTag("submit_verification_btn")
                    ) {
                        Text("Submit Verification Application")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isAdmin) "Admin Verification Queue (${requests.size})" else "My Applications (${requests.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Spacer(Modifier.height(8.dp))

            requests.forEach { req ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, LocalNabdColors.current.hairline),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${req.userName} (@${req.username})", style = MaterialTheme.typography.titleMedium)
                            Text(
                                req.status.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = when (req.status) {
                                    "approved" -> LocalNabdColors.current.repostEmerald
                                    "rejected" -> MaterialTheme.colorScheme.error
                                    else -> LocalNabdColors.current.warningAmber
                                }
                            )
                        }
                        Text("Requested: ${req.requestedType}", style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(req.reason, style = MaterialTheme.typography.bodyMedium)

                        if (isAdmin && req.status == "pending") {
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { onReviewRequest(req, true) }) { Text("Approve") }
                                OutlinedButton(onClick = { onReviewRequest(req, false) }) { Text("Reject") }
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
fun SafetyAndModerationScreen(
    currentUser: UserProfile,
    mutedWords: List<String>,
    followRequests: List<FollowRequest>,
    onBack: () -> Unit,
    onTogglePrivate: (Boolean) -> Unit,
    onAddMutedWord: (String) -> Unit,
    onRemoveMutedWord: (String) -> Unit,
    onRespondFollowRequest: (FollowRequest, Boolean) -> Unit
) {
    BackHandler(onBack = onBack)
    var wordInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety & Privacy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, LocalNabdColors.current.hairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Private Account", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Approve who can follow you",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalNabdColors.current.mutedText
                        )
                    }
                    Switch(checked = currentUser.isPrivate, onCheckedChange = onTogglePrivate)
                }
            }

            if (followRequests.isNotEmpty()) {
                Text("Follow Requests (${followRequests.size})", style = MaterialTheme.typography.titleMedium)
                followRequests.forEach { req ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, LocalNabdColors.current.hairline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            MonogramAvatar(name = req.requesterName, colorHex = req.requesterAvatarColor, size = 40.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(req.requesterName, style = MaterialTheme.typography.titleMedium)
                                Text("@${req.requesterUsername}", color = LocalNabdColors.current.mutedText)
                            }
                            TextButton(onClick = { onRespondFollowRequest(req, true) }) { Text("Accept") }
                            TextButton(onClick = { onRespondFollowRequest(req, false) }) { Text("Decline") }
                        }
                    }
                }
            }

            Text("Muted Words & Hashtags", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = wordInput,
                    onValueChange = { wordInput = it },
                    label = { Text("Word or #hashtag") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (wordInput.isNotBlank()) {
                            onAddMutedWord(wordInput)
                            wordInput = ""
                        }
                    }
                ) { Text("Mute") }
            }

            mutedWords.forEach { word ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(word, style = MaterialTheme.typography.bodyLarge)
                    IconButton(onClick = { onRemoveMutedWord(word) }) {
                        Icon(Icons.Outlined.DeleteOutline, "Unmute $word")
                    }
                }
            }
        }
    }
}
