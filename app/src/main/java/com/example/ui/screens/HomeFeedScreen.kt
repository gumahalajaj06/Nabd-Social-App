package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Post
import com.example.data.model.ReactionType
import com.example.data.model.TextStory
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.ConfirmDeletePostDialog
import com.example.ui.components.EditPostDialog
import com.example.ui.components.MonogramAvatar
import com.example.ui.components.NabdEmptyState
import com.example.ui.components.OfflineStatusBanner
import com.example.ui.components.PostCard
import com.example.ui.theme.LocalNabdColors
import com.example.util.sharePostAsTextCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeFeedScreen(
    currentUser: UserProfile,
    postsState: UiState<List<Post>>,
    storiesState: UiState<List<TextStory>>,
    followingIds: Set<String>,
    bookmarkIds: Set<String>,
    likedIds: Set<String>,
    repostedIds: Set<String>,
    blockedIds: Set<String>,
    mutedUserIds: Set<String> = emptySet(),
    mutedWords: List<String>,
    seenStoryIds: Set<String>,
    userReactions: Map<String, ReactionType> = emptyMap(),
    unreadNotifCount: Int,
    isOnline: Boolean,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    onHydratePosts: (List<Post>) -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenComposer: () -> Unit,
    onOpenCreateStory: () -> Unit,
    onOpenStoryViewer: (List<TextStory>, Int) -> Unit,
    onOpenPostDetail: (Post) -> Unit,
    onOpenQuoteComposer: (Post) -> Unit,
    onOpenProfile: (String) -> Unit,
    onSearchHashtag: (String) -> Unit,
    onOpenMention: (String) -> Unit,
    onLikePost: (Post) -> Unit,
    onReactPost: ((Post, ReactionType) -> Unit)? = null,
    onReactionSummaryClick: ((Post) -> Unit)? = null,
    onRepostPost: (Post) -> Unit,
    onBookmarkPost: (Post) -> Unit,
    onVotePoll: (Post, Int) -> Unit,
    onEditPost: ((Post, String) -> Unit)? = null,
    onDeletePost: (Post) -> Unit,
    onPinToProfile: ((Post) -> Unit)? = null,
    onReportPost: (Post, String) -> Unit,
    onBlockAuthor: (Post) -> Unit,
    onShowMessage: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var reportTarget by remember { mutableStateOf<Post?>(null) }
    var reportReason by remember { mutableStateOf("") }
    var editTarget by remember { mutableStateOf<Post?>(null) }
    var deleteTarget by remember { mutableStateOf<Post?>(null) }

    val allPosts = (postsState as? UiState.Success)?.data ?: emptyList()
    LaunchedEffect(allPosts.size) {
        if (allPosts.isNotEmpty()) onHydratePosts(allPosts)
    }

    val topPosts = remember(allPosts, blockedIds, mutedUserIds, mutedWords) {
        allPosts.filter { post ->
            post.replyToPostId == null &&
                post.authorId !in blockedIds &&
                post.authorId !in mutedUserIds &&
                mutedWords.none { word -> post.content.contains(word, ignoreCase = true) }
        }
    }

    val displayedPosts = remember(topPosts, selectedTab, followingIds, currentUser.uid) {
        if (selectedTab == 0) {
            val now = System.currentTimeMillis()
            topPosts.sortedByDescending { it.engagementScore(now) }
        } else {
            topPosts.filter { it.authorId in followingIds || it.authorId == currentUser.uid }
        }
    }

    val activeStories = (storiesState as? UiState.Success)?.data
        ?.filter { it.authorId !in blockedIds && it.authorId !in mutedUserIds }
        ?: emptyList()

    val storiesByAuthor = remember(activeStories) {
        activeStories.groupBy { it.authorId }.values.toList()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "نَبْض",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Nabd",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = "Toggle theme"
                        )
                    }
                    IconButton(
                        onClick = onOpenNotifications,
                        modifier = Modifier.testTag("notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifCount > 0) {
                                    Badge { Text(unreadNotifCount.coerceAtMost(99).toString()) }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            OfflineStatusBanner(isOnline = isOnline)

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                item(key = "add_story") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(66.dp)
                            .clickable { onOpenCreateStory() }
                            .testTag("add_story_button")
                    ) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            MonogramAvatar(
                                name = currentUser.name,
                                colorHex = currentUser.avatarColor,
                                avatarPattern = currentUser.avatarPattern,
                                size = 54.dp
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface),
                                modifier = Modifier.size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "New text story",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "خاطرة",
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                items(storiesByAuthor, key = { it.first().authorId }) { authorStories ->
                    val first = authorStories.first()
                    val hasUnseen = authorStories.any { it.id !in seenStoryIds }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(66.dp)
                            .clickable { onOpenStoryViewer(authorStories, 0) }
                            .testTag("story_ring_${first.authorId}")
                    ) {
                        MonogramAvatar(
                            name = first.authorName,
                            colorHex = first.authorAvatarColor,
                            size = 54.dp,
                            hasUnseenStory = hasUnseen,
                            hasSeenStory = !hasUnseen
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = first.authorName.split(" ").first(),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 0.5.dp, color = LocalNabdColors.current.hairline)

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("For You · لك") },
                    modifier = Modifier.testTag("tab_for_you")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Following · متابَعون") },
                    modifier = Modifier.testTag("tab_following")
                )
            }

            when (postsState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    NabdEmptyState(
                        icon = Icons.Outlined.AutoAwesome,
                        title = "Unable to load feed",
                        subtitle = postsState.message
                    )
                }
                is UiState.Success -> {
                    if (displayedPosts.isEmpty()) {
                        NabdEmptyState(
                            icon = Icons.Outlined.AutoAwesome,
                            title = if (selectedTab == 0) "No pulses yet" else "Your Following feed is quiet",
                            subtitle = if (selectedTab == 0) {
                                "Tap the compose button to publish the first thought on Nabd."
                            } else {
                                "Follow writers and thinkers from Explore to see their pulses chronologically."
                            }
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 96.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(displayedPosts, key = { it.id }) { post ->
                                PostCard(
                                    post = post,
                                    currentUid = currentUser.uid,
                                    isLiked = post.id in likedIds,
                                    isReposted = post.id in repostedIds,
                                    isBookmarked = post.id in bookmarkIds,
                                    userReaction = userReactions[post.id],
                                    onAuthorClick = onOpenProfile,
                                    onPostClick = onOpenPostDetail,
                                    onLikeClick = onLikePost,
                                    onReactClick = onReactPost,
                                    onReactionSummaryClick = onReactionSummaryClick,
                                    onRepostClick = onRepostPost,
                                    onQuoteClick = onOpenQuoteComposer,
                                    onReplyClick = onOpenPostDetail,
                                    onBookmarkClick = onBookmarkPost,
                                    onShareClick = { sharePostAsTextCard(context, it) },
                                    onVotePoll = onVotePoll,
                                    onHashtagClick = onSearchHashtag,
                                    onMentionClick = onOpenMention,
                                    onEditPost = if (onEditPost != null) { p -> editTarget = p } else null,
                                    onDeletePost = { p -> deleteTarget = p },
                                    onPinToProfile = onPinToProfile,
                                    onReportPost = { reportTarget = it },
                                    onBlockAuthor = onBlockAuthor,
                                    onCopySuccess = onShowMessage
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onOpenComposer,
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 18.dp, bottom = 18.dp)
                .testTag("compose_fab")
        ) {
            Icon(
                imageVector = Icons.Outlined.Create,
                contentDescription = "Compose pulse"
            )
        }
    }

    if (reportTarget != null) {
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            title = { Text("Report Pulse") },
            text = {
                OutlinedTextField(
                    value = reportReason,
                    onValueChange = { if (it.length <= 300) reportReason = it },
                    label = { Text("Reason") },
                    placeholder = { Text("Spam, harassment, or misinformation…") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        reportTarget?.let { onReportPost(it, reportReason.ifBlank { "Inappropriate content" }) }
                        reportTarget = null
                        reportReason = ""
                    }
                ) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { reportTarget = null }) { Text("Cancel") }
            }
        )
    }

    editTarget?.let { post ->
        EditPostDialog(
            post = post,
            maxChars = currentUser.maxPostLength,
            onDismiss = { editTarget = null },
            onSave = { updated ->
                onEditPost?.invoke(post, updated)
                editTarget = null
            }
        )
    }

    deleteTarget?.let { post ->
        ConfirmDeletePostDialog(
            onDismiss = { deleteTarget = null },
            onConfirm = {
                onDeletePost(post)
                deleteTarget = null
            }
        )
    }
}
