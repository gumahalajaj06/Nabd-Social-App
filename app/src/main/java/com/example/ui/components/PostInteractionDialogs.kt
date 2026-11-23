package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CommentSortOrder
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PostReaction
import com.example.data.model.ReactionType
import com.example.data.model.UserProfile
import com.example.ui.theme.LocalNabdColors
import com.example.util.formatRelativeTime

@Composable
fun EditPostDialog(
    post: Post,
    maxChars: Int,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var content by remember(post.id) { mutableStateOf(post.content) }
    val clean = content.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Pulse") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= maxChars) content = it },
                    label = { Text("Content (${content.length}/$maxChars)") },
                    minLines = 3,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth().testTag("edit_post_input")
                )
                Text(
                    text = "Hashtags and @mentions will be re-indexed automatically.",
                    style = MaterialTheme.typography.labelMedium,
                    color = LocalNabdColors.current.mutedText
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(clean) },
                enabled = clean.isNotEmpty() && clean != post.content.trim()
            ) { Text("Save Changes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ConfirmDeletePostDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Pulse?") },
        text = { Text("This text pulse and its engagement counters will be permanently removed from Nabd.") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReactionBreakdownSheet(
    reactions: List<PostReaction>,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf<String?>(null) }
    val countsByType = remember(reactions) {
        reactions.groupingBy { it.type }.eachCount()
    }
    val filtered = remember(reactions, selectedFilter) {
        if (selectedFilter == null) reactions else reactions.filter { it.type == selectedFilter }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Reactions (${reactions.size})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedFilter == null,
                        onClick = { selectedFilter = null },
                        label = { Text("All (${reactions.size})") }
                    )
                }
                items(ReactionType.entries) { type ->
                    val count = countsByType[type.key] ?: 0
                    if (count > 0) {
                        FilterChip(
                            selected = selectedFilter == type.key,
                            onClick = { selectedFilter = type.key },
                            label = { Text("${type.emoji} $count") }
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = 0.5.dp, color = LocalNabdColors.current.hairline)
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(filtered, key = { "${it.userId}_${it.type}" }) { item ->
                    val type = ReactionType.fromKey(item.type)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                onUserClick(item.userId)
                            }
                            .padding(vertical = 10.dp)
                    ) {
                        MonogramAvatar(
                            name = item.userName.ifBlank { "Member" },
                            colorHex = item.userAvatarColor,
                            size = 40.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.userName.ifBlank { "Member" }, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "@${item.userUsername}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = LocalNabdColors.current.mutedText
                            )
                        }
                        Text(text = type.emoji, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CommentItemCard(
    comment: PostComment,
    currentUid: String,
    postAuthorId: String,
    myReaction: ReactionType?,
    isNested: Boolean = false,
    onAuthorClick: (String) -> Unit,
    onReplyToComment: (PostComment) -> Unit,
    onReactComment: (PostComment, ReactionType) -> Unit,
    onDeleteComment: (PostComment) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val extColors = LocalNabdColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isNested) 44.dp else 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            )
    ) {
        Row(verticalAlignment = Alignment.Top) {
            MonogramAvatar(
                name = comment.authorName,
                colorHex = comment.authorAvatarColor,
                size = if (isNested) 30.dp else 36.dp,
                onClick = { onAuthorClick(comment.authorId) }
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, extColors.hairline),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = comment.authorName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (comment.authorVerifiedType != "none") {
                                Spacer(Modifier.width(4.dp))
                                VerifiedBadge(comment.authorVerifiedType)
                            }
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "@${comment.authorUsername} · ${formatRelativeTime(comment.createdAt)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = extColors.mutedText
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(comment.content, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                AnimatedVisibility(visible = showPicker) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        ReactionType.entries.forEach { reaction ->
                            Text(
                                text = reaction.emoji,
                                fontSize = 18.sp,
                                modifier = Modifier
                                    .clickable {
                                        showPicker = false
                                        onReactComment(comment, reaction)
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(start = 6.dp, top = 4.dp)
                ) {
                    Text(
                        text = if (myReaction != null) "${myReaction.emoji} ${myReaction.labelEn}" else "React",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (myReaction != null) MaterialTheme.colorScheme.primary else extColors.mutedText,
                        modifier = Modifier.combinedClickable(
                            onClick = { onReactComment(comment, myReaction ?: ReactionType.LIKE) },
                            onLongClick = { showPicker = !showPicker }
                        )
                    )
                    if (!isNested) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onReplyToComment(comment) }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Reply, null, modifier = Modifier.size(14.dp), tint = extColors.mutedText)
                            Spacer(Modifier.width(3.dp))
                            Text("Reply", style = MaterialTheme.typography.labelMedium, color = extColors.mutedText)
                        }
                    }
                    if (comment.totalReactionsCount > 0) {
                        val emojis = comment.reactionsCount.entries
                            .filter { it.value > 0 }
                            .sortedByDescending { it.value }
                            .take(2)
                            .map { ReactionType.fromKey(it.key).emoji }
                            .joinToString("")
                        Text(
                            text = "${emojis.ifEmpty { "👍" }} ${comment.totalReactionsCount}",
                            style = MaterialTheme.typography.labelMedium,
                            color = extColors.mutedText
                        )
                    }
                    if (comment.authorId == currentUid || postAuthorId == currentUid) {
                        IconButton(
                            onClick = { onDeleteComment(comment) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete comment",
                                tint = extColors.mutedText,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentSortBar(
    selectedSort: CommentSortOrder,
    onSelectSort: (CommentSortOrder) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        CommentSortOrder.entries.forEach { order ->
            FilterChip(
                selected = selectedSort == order,
                onClick = { onSelectSort(order) },
                label = { Text(order.label) }
            )
        }
    }
}

@Composable
fun UserFollowersSheet(
    title: String,
    users: List<UserProfile>,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$title (${users.size})") },
        text = {
            if (users.isEmpty()) {
                Text("No accounts to show yet.", color = LocalNabdColors.current.mutedText)
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(users, key = { it.uid }) { user ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onUserClick(user.uid)
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            MonogramAvatar(
                                name = user.name,
                                colorHex = user.avatarColor,
                                avatarPattern = user.avatarPattern,
                                size = 38.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(user.name, style = MaterialTheme.typography.titleMedium)
                                    Spacer(Modifier.width(4.dp))
                                    VerifiedBadge(user.displayedVerifiedType)
                                }
                                Text(
                                    "@${user.username}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LocalNabdColors.current.mutedText
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
