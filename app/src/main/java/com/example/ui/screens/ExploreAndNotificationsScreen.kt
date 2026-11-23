package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AppNotification
import com.example.data.model.Post
import com.example.data.model.ReactionType
import com.example.data.model.TrendingHashtag
import com.example.data.model.UserProfile
import com.example.ui.components.MonogramAvatar
import com.example.ui.components.NabdEmptyState
import com.example.ui.components.PostCard
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.LocalNabdColors
import com.example.util.formatRelativeTime
import com.example.util.sharePostAsTextCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    initialQuery: String,
    currentUser: UserProfile,
    allPosts: List<Post>,
    activeUsers: List<UserProfile>,
    trendingHashtags: List<TrendingHashtag>,
    followingIds: Set<String>,
    likedIds: Set<String>,
    repostedIds: Set<String>,
    bookmarkIds: Set<String>,
    blockedIds: Set<String>,
    mutedUserIds: Set<String> = emptySet(),
    userReactions: Map<String, ReactionType> = emptyMap(),
    onOpenProfile: (String) -> Unit,
    onOpenPost: (Post) -> Unit,
    onQuotePost: (Post) -> Unit,
    onLikePost: (Post) -> Unit,
    onReactPost: ((Post, ReactionType) -> Unit)? = null,
    onReactionSummaryClick: ((Post) -> Unit)? = null,
    onRepostPost: (Post) -> Unit,
    onBookmarkPost: (Post) -> Unit,
    onVotePoll: (Post, Int) -> Unit,
    onToggleFollow: (UserProfile) -> Unit,
    onShowMessage: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }
    val cleanQuery = query.trim().removePrefix("#").removePrefix("@").lowercase()

    val matchingUsers = remember(cleanQuery, activeUsers, blockedIds, mutedUserIds) {
        activeUsers.filter { u ->
            u.uid !in blockedIds && u.uid !in mutedUserIds && (
                cleanQuery.isEmpty() ||
                    u.username.contains(cleanQuery, ignoreCase = true) ||
                    u.name.contains(cleanQuery, ignoreCase = true)
                )
        }.take(12)
    }

    val matchingPosts = remember(cleanQuery, allPosts, blockedIds, mutedUserIds) {
        if (cleanQuery.isEmpty()) emptyList()
        else allPosts.filter { p ->
            p.authorId !in blockedIds && p.authorId !in mutedUserIds && (
                p.content.contains(cleanQuery, ignoreCase = true) ||
                    p.hashtags.any { it.equals(cleanQuery, ignoreCase = true) } ||
                    p.authorUsername.contains(cleanQuery, ignoreCase = true)
                )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("Explore & Trends") })

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search @username, name, #hashtag, or keyword…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("explore_search_input")
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = 84.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            if (cleanQuery.isEmpty()) {
                item {
                    Text(
                        text = "Trending #Hashtags (24h)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
                if (trendingHashtags.isEmpty()) {
                    item {
                        Text(
                            text = "No trending hashtags yet. Publish a post with #Nabd!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalNabdColors.current.mutedText,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    items(trendingHashtags, key = { it.tag }) { trend ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, LocalNabdColors.current.hairline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable { query = "#${trend.tag}" }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Tag,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = "#${trend.tag}",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Text(
                                    text = "${trend.postCount} pulses",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LocalNabdColors.current.mutedText
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (cleanQuery.isEmpty()) "People on Nabd" else "Matching Accounts (${matchingUsers.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            items(matchingUsers, key = { "u_${it.uid}" }) { user ->
                val isFollowing = user.uid in followingIds
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenProfile(user.uid) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    MonogramAvatar(name = user.name, colorHex = user.avatarColor, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(user.name, style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(4.dp))
                            VerifiedBadge(user.verifiedType)
                        }
                        Text(
                            "@${user.username}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalNabdColors.current.mutedText
                        )
                    }
                    if (user.uid != currentUser.uid) {
                        if (isFollowing) {
                            OutlinedButton(onClick = { onToggleFollow(user) }) { Text("Following") }
                        } else {
                            Button(onClick = { onToggleFollow(user) }) { Text("Follow") }
                        }
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = LocalNabdColors.current.hairline)
            }

            if (cleanQuery.isNotEmpty()) {
                item {
                    Text(
                        text = "Matching Pulses (${matchingPosts.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                items(matchingPosts, key = { "p_${it.id}" }) { post ->
                    PostCard(
                        post = post,
                        currentUid = currentUser.uid,
                        isLiked = post.id in likedIds,
                        isReposted = post.id in repostedIds,
                        isBookmarked = post.id in bookmarkIds,
                        userReaction = userReactions[post.id],
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
                        onHashtagClick = { query = "#$it" },
                        onMentionClick = { query = "@$it" },
                        onCopySuccess = onShowMessage
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    notifications: List<AppNotification>,
    onMarkRead: (String) -> Unit,
    onOpenProfile: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedTab by remember { mutableIntStateOf(0) }
    val filtered = remember(notifications, selectedTab) {
        when (selectedTab) {
            1 -> notifications.filter { it.actorVerifiedType != "none" || it.type == "verified" }
            2 -> notifications.filter { it.type == "mention" }
            3 -> notifications.filter { it.type == "reply" }
            else -> notifications
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            TabRow(selectedTabIndex = selectedTab) {
                listOf("All", "Verified", "Mentions", "Replies").forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (filtered.isEmpty()) {
                NabdEmptyState(
                    icon = Icons.Outlined.NotificationsNone,
                    title = "No notifications here",
                    subtitle = "Likes, replies, reposts, mentions, and verification updates appear here."
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { notif ->
                        val icon = when (notif.type) {
                            "like" -> Icons.Outlined.FavoriteBorder
                            "repost" -> Icons.Outlined.Repeat
                            "mention" -> Icons.Outlined.AlternateEmail
                            "follow" -> Icons.Outlined.PersonAdd
                            "verified" -> Icons.Outlined.Verified
                            else -> Icons.Outlined.NotificationsNone
                        }
                        Surface(
                            color = if (!notif.isRead) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onMarkRead(notif.id)
                                    onOpenProfile(notif.actorId)
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.Top,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = notif.type,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp).padding(top = 2.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                MonogramAvatar(
                                    name = notif.actorName,
                                    colorHex = notif.actorAvatarColor,
                                    size = 38.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(notif.actorName, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(4.dp))
                                        VerifiedBadge(notif.actorVerifiedType)
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            formatRelativeTime(notif.createdAt),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = LocalNabdColors.current.mutedText
                                        )
                                    }
                                    Text(
                                        text = "${notif.type.replaceFirstChar { it.uppercase() }} · ${notif.previewText}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = LocalNabdColors.current.mutedText
                                    )
                                }
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = LocalNabdColors.current.hairline)
                    }
                }
            }
        }
    }
}
