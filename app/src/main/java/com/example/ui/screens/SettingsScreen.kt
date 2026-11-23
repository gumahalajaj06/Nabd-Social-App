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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.FollowRequest
import com.example.data.model.UserAppearancePreferences
import com.example.data.model.UserNotificationSettings
import com.example.data.model.UserPrivacySettings
import com.example.data.model.UserProfile
import com.example.ui.components.MonogramAvatar
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.LocalNabdColors
import com.example.util.LocalNabdStrings
import com.example.util.NabdLanguage
import com.example.util.parseHexColor
import com.example.util.sharePlainTextExport
import kotlin.math.roundToInt

private val ACCENT_SWATCHES = listOf(
    "#0D9488",
    "#2563EB",
    "#7C3AED",
    "#E11D48",
    "#EA580C",
    "#16A34A"
)

private val AVATAR_PATTERNS = listOf("solid", "rings", "dots", "diagonal", "pulse")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    currentUser: UserProfile,
    currentUserEmail: String,
    activeUsers: List<UserProfile>,
    mutedWords: List<String>,
    mutedUserIds: Set<String>,
    blockedIds: Set<String>,
    followRequests: List<FollowRequest>,
    cacheStats: Triple<Int, Int, Int>,
    onBack: () -> Unit,
    onRefreshCacheStats: () -> Unit,
    onClearCache: () -> Unit,
    onOpenEditProfile: () -> Unit,
    onSaveAvatarStyle: (colorHex: String, pattern: String) -> Unit,
    onExportDataJson: () -> String,
    onDeactivateAccount: () -> Unit,
    onDeleteAccount: () -> Unit,
    onOpenVerification: () -> Unit,
    onToggleShowVerifiedBadge: (Boolean) -> Unit,
    onTogglePrivateAccount: (Boolean) -> Unit,
    onUpdatePrivacy: (UserPrivacySettings) -> Unit,
    onAddMutedWord: (String, Int?) -> Unit,
    onRemoveMutedWord: (String) -> Unit,
    onUnmuteUser: (UserProfile) -> Unit,
    onUnblockUser: (UserProfile) -> Unit,
    onRespondFollowRequest: (FollowRequest, Boolean) -> Unit,
    onUpdateNotifications: (UserNotificationSettings) -> Unit,
    onUpdateAppearance: (UserAppearancePreferences) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onSignOut: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val extColors = LocalNabdColors.current
    val strings = LocalNabdStrings.current

    var searchQuery by remember { mutableStateOf("") }
    var mutedWordInput by remember { mutableStateOf("") }
    var selectedExpiryDays by remember { mutableStateOf<Int?>(null) }
    var showDeactivateConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var deleteConfirmHandle by remember { mutableStateOf("") }
    var fontScaleSlider by remember(currentUser.preferences.fontScale) {
        mutableFloatStateOf(currentUser.preferences.fontScale.toFloat().coerceIn(0.85f, 1.30f))
    }

    LaunchedEffect(Unit) { onRefreshCacheStats() }

    val q = searchQuery.trim().lowercase()
    fun matches(vararg keywords: String): Boolean =
        q.isEmpty() || keywords.any { it.contains(q, ignoreCase = true) }

    val privacy = currentUser.privacy
    val notifs = currentUser.notifications
    val prefs = currentUser.preferences

    val mutedUsersList = remember(activeUsers, mutedUserIds) {
        activeUsers.filter { it.uid in mutedUserIds }
    }
    val blockedUsersList = remember(activeUsers, blockedIds) {
        activeUsers.filter { it.uid in blockedIds }
    }

    val daysUntilUsernameChange = remember(currentUser.usernameChangedAt) {
        val changedMs = currentUser.usernameChangedAt?.toDate()?.time ?: 0L
        if (changedMs <= 0L) 0L
        else {
            val elapsedDays = (System.currentTimeMillis() - changedMs) / 86_400_000L
            (14L - elapsedDays).coerceAtLeast(0L)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settingsTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search settings (theme, privacy, quiet hours, export…)") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_search_input")
            )

            // 1. ACCOUNT SECTION
            if (matches("account", "profile", "username", "avatar", "pattern", "google", "export", "json", "deactivate", "delete")) {
                SettingsSectionCard(
                    title = "Account",
                    icon = Icons.Outlined.AccountCircle
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            MonogramAvatar(
                                name = currentUser.name,
                                colorHex = currentUser.avatarColor,
                                avatarPattern = currentUser.avatarPattern,
                                size = 48.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(currentUser.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "@${currentUser.username}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = extColors.mutedText
                                )
                            }
                        }
                        OutlinedButton(onClick = onOpenEditProfile) {
                            Text("Edit Profile")
                        }
                    }

                    Text(
                        text = if (daysUntilUsernameChange > 0) {
                            "Username cooldown: $daysUntilUsernameChange day(s) remaining before next change."
                        } else {
                            "Username @${currentUser.username} is eligible to change (14-day cooldown applies)."
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = extColors.mutedText
                    )

                    Text("Avatar Pattern", style = MaterialTheme.typography.labelLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        AVATAR_PATTERNS.forEach { pattern ->
                            FilterChip(
                                selected = currentUser.avatarPattern == pattern,
                                onClick = { onSaveAvatarStyle(currentUser.avatarColor, pattern) },
                                label = { Text(pattern.replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    Text("Avatar Color", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ACCENT_SWATCHES.forEach { hex ->
                            val selected = currentUser.avatarColor.equals(hex, ignoreCase = true)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex))
                                    .border(
                                        width = if (selected) 2.5.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { onSaveAvatarStyle(hex, currentUser.avatarPattern) }
                            ) {
                                if (selected) {
                                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    if (currentUserEmail.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Google Sign-In Linked: $currentUserEmail",
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val json = onExportDataJson()
                            sharePlainTextExport(context, "Nabd Data Export (@${currentUser.username})", json)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.Download, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Export My Data (JSON)")
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { showDeactivateConfirm = true },
                            modifier = Modifier.weight(1f)
                        ) { Text("Deactivate") }
                        Button(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) { Text("Delete Account") }
                    }
                }
            }

            // 2. FREE VERIFICATION SECTION
            if (matches("verification", "badge", "blue", "gold", "government", "status", "queue")) {
                SettingsSectionCard(
                    title = "Free Verification",
                    icon = Icons.Outlined.Verified
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Status: ${currentUser.verifiedType.uppercase()}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.width(6.dp))
                            VerifiedBadge(currentUser.verifiedType)
                        }
                        Button(onClick = onOpenVerification) {
                            Text("Manage / Apply")
                        }
                    }

                    SettingsSwitchRow(
                        title = "Show Verification Badge Publicly",
                        subtitle = "Display badge next to your name across feeds",
                        checked = currentUser.showVerifiedBadge,
                        onCheckedChange = onToggleShowVerifiedBadge
                    )
                }
            }

            // 3. PRIVACY & SAFETY SECTION
            if (matches("privacy", "safety", "private", "follow", "mention", "dm", "voice", "receipt", "online", "likes", "lock", "mute", "block")) {
                SettingsSectionCard(
                    title = "Privacy & Safety",
                    icon = Icons.Outlined.Shield
                ) {
                    SettingsSwitchRow(
                        title = "Private Account",
                        subtitle = "Approve follow requests manually",
                        checked = currentUser.isPrivate,
                        onCheckedChange = onTogglePrivateAccount
                    )

                    if (followRequests.isNotEmpty()) {
                        Text("Pending Follow Requests (${followRequests.size})", style = MaterialTheme.typography.labelLarge)
                        followRequests.forEach { req ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${req.requesterName} (@${req.requesterUsername})", modifier = Modifier.weight(1f))
                                TextButton(onClick = { onRespondFollowRequest(req, true) }) { Text("Accept") }
                                TextButton(onClick = { onRespondFollowRequest(req, false) }) { Text("Decline") }
                            }
                        }
                    }

                    Text("Who can @mention me", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("everyone" to "Everyone", "following" to "Following", "nobody" to "Nobody").forEach { (key, label) ->
                            FilterChip(
                                selected = privacy.allowMentionsFrom == key,
                                onClick = { onUpdatePrivacy(privacy.copy(allowMentionsFrom = key)) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text("Who can send me DMs", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("everyone" to "Everyone", "following" to "Following", "nobody" to "Nobody").forEach { (key, label) ->
                            FilterChip(
                                selected = privacy.allowDMsFrom == key,
                                onClick = { onUpdatePrivacy(privacy.copy(allowDMsFrom = key)) },
                                label = { Text(label) }
                            )
                        }
                    }

                    SettingsSwitchRow(
                        title = "Allow Voice Notes in DMs",
                        subtitle = "Receive audio notes inside direct messages",
                        checked = privacy.allowVoiceNotesInDMs,
                        onCheckedChange = { onUpdatePrivacy(privacy.copy(allowVoiceNotesInDMs = it)) }
                    )
                    SettingsSwitchRow(
                        title = "Show Read Receipts",
                        subtitle = "Let DM peers see blue read checkmarks",
                        checked = privacy.showReadReceipts,
                        onCheckedChange = { onUpdatePrivacy(privacy.copy(showReadReceipts = it)) }
                    )
                    SettingsSwitchRow(
                        title = "Show Online Status",
                        subtitle = "Display active status in chats",
                        checked = privacy.showOnlineStatus,
                        onCheckedChange = { onUpdatePrivacy(privacy.copy(showOnlineStatus = it)) }
                    )
                    SettingsSwitchRow(
                        title = "Hide Likes Tab on Profile",
                        subtitle = "Keep your reacted pulses private from visitors",
                        checked = privacy.hideLikesTab,
                        onCheckedChange = { onUpdatePrivacy(privacy.copy(hideLikesTab = it)) }
                    )
                    SettingsSwitchRow(
                        title = "Require Session Lock Gate",
                        subtitle = "Show quick unlock gate when opening Nabd",
                        checked = privacy.appLockEnabled,
                        onCheckedChange = { onUpdatePrivacy(privacy.copy(appLockEnabled = it)) }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = extColors.hairline)
                    Text("Muted Words & Hashtags", style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = mutedWordInput,
                            onValueChange = { mutedWordInput = it },
                            label = { Text("Word or #hashtag") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (mutedWordInput.isNotBlank()) {
                                    onAddMutedWord(mutedWordInput, selectedExpiryDays)
                                    mutedWordInput = ""
                                }
                            }
                        ) { Text("Mute") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null to "Forever", 1 to "24h", 7 to "7d", 30 to "30d").forEach { (days, label) ->
                            FilterChip(
                                selected = selectedExpiryDays == days,
                                onClick = { selectedExpiryDays = days },
                                label = { Text(label) }
                            )
                        }
                    }
                    if (mutedWords.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            mutedWords.forEach { word ->
                                FilterChip(
                                    selected = true,
                                    onClick = { onRemoveMutedWord(word) },
                                    label = { Text(word) },
                                    trailingIcon = {
                                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "Unmute $word", modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                    }

                    if (mutedUsersList.isNotEmpty()) {
                        Text("Muted Accounts (${mutedUsersList.size})", style = MaterialTheme.typography.labelLarge)
                        mutedUsersList.forEach { user ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${user.name} (@${user.username})")
                                TextButton(onClick = { onUnmuteUser(user) }) { Text("Unmute") }
                            }
                        }
                    }

                    if (blockedUsersList.isNotEmpty()) {
                        Text("Blocked Accounts (${blockedUsersList.size})", style = MaterialTheme.typography.labelLarge)
                        blockedUsersList.forEach { user ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${user.name} (@${user.username})")
                                TextButton(onClick = { onUnblockUser(user) }) { Text("Unblock") }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                }
            }

            // 4. NOTIFICATIONS SECTION
            if (matches("notification", "push", "like", "reaction", "repost", "reply", "mention", "follow", "dm", "quiet", "filter")) {
                SettingsSectionCard(
                    title = "Notifications",
                    icon = Icons.Outlined.Notifications
                ) {
                    SettingsSwitchRow(
                        title = "Push Notifications",
                        subtitle = "Master switch for activity alerts",
                        checked = notifs.pushEnabled,
                        onCheckedChange = { onUpdateNotifications(notifs.copy(pushEnabled = it)) }
                    )
                    SettingsSwitchRow(
                        title = "Likes & Emoji Reactions",
                        subtitle = "Alerts when someone reacts to your pulses",
                        checked = notifs.reactions && notifs.likes,
                        onCheckedChange = { onUpdateNotifications(notifs.copy(reactions = it, likes = it)) }
                    )
                    SettingsSwitchRow(
                        title = "Reposts & Quotes",
                        subtitle = "Alerts when your pulse is amplified",
                        checked = notifs.reposts,
                        onCheckedChange = { onUpdateNotifications(notifs.copy(reposts = it)) }
                    )
                    SettingsSwitchRow(
                        title = "Replies & Comments",
                        subtitle = "Alerts for thread conversations",
                        checked = notifs.replies,
                        onCheckedChange = { onUpdateNotifications(notifs.copy(replies = it)) }
                    )
                    SettingsSwitchRow(
                        title = "@Mentions",
                        subtitle = "Alerts when someone tags your handle",
                        checked = notifs.mentions,
                        onCheckedChange = { onUpdateNotifications(notifs.copy(mentions = it)) }
                    )
                    SettingsSwitchRow(
                        title = "New Followers",
                        subtitle = "Alerts for new followers and requests",
                        checked = notifs.follows,
                        onCheckedChange = { onUpdateNotifications(notifs.copy(follows = it)) }
                    )
                    SettingsSwitchRow(
                        title = "Direct Messages",
                        subtitle = "Alerts for new text & voice messages",
                        checked = notifs.dms,
                        onCheckedChange = { onUpdateNotifications(notifs.copy(dms = it)) }
                    )
                    SettingsSwitchRow(
                        title = "Quality Filter",
                        subtitle = "Hide repetitive or low-quality notifications",
                        checked = notifs.qualityFilter,
                        onCheckedChange = { onUpdateNotifications(notifs.copy(qualityFilter = it)) }
                    )
                    SettingsSwitchRow(
                        title = "Quiet Hours (${notifs.quietStart} – ${notifs.quietEnd})",
                        subtitle = "Pause push delivery during scheduled rest hours",
                        checked = notifs.quietHoursEnabled,
                        onCheckedChange = { onUpdateNotifications(notifs.copy(quietHoursEnabled = it)) }
                    )
                    if (notifs.quietHoursEnabled) {
                        Text("Quiet Window Preset", style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "22:00" to "07:00",
                                "23:00" to "06:00",
                                "00:00" to "08:00"
                            ).forEach { (start, end) ->
                                FilterChip(
                                    selected = notifs.quietStart == start && notifs.quietEnd == end,
                                    onClick = { onUpdateNotifications(notifs.copy(quietStart = start, quietEnd = end)) },
                                    label = { Text("$start–$end") }
                                )
                            }
                        }
                    }
                }
            }

            // 5. APPEARANCE & OFFLINE SECTION
            if (matches("appearance", "theme", "dark", "light", "oled", "accent", "color", "font", "size", "scale", "language", "arabic", "english", "voice", "story", "offline", "cache")) {
                SettingsSectionCard(
                    title = "Appearance & Offline",
                    icon = Icons.Outlined.Palette
                ) {
                    Text("Theme Mode", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "system" to "System",
                            "light" to "Light",
                            "dark" to "Dark",
                            "oled" to "OLED Black"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = prefs.themeMode == key,
                                onClick = { onUpdateAppearance(prefs.copy(themeMode = key)) },
                                label = { Text(label) }
                            )
                        }
                    }

                    Text("Accent Color", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ACCENT_SWATCHES.forEach { hex ->
                            val selected = prefs.accentColor.equals(hex, ignoreCase = true)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex))
                                    .border(
                                        width = if (selected) 2.5.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape
                                    )
                                    .clickable { onUpdateAppearance(prefs.copy(accentColor = hex)) }
                            ) {
                                if (selected) {
                                    Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    Text("Reading Font Family", style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "cairo" to "Cairo",
                            "amiri" to "Amiri",
                            "tajawal" to "Tajawal",
                            "jakarta" to "Jakarta",
                            "mono" to "Mono"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = prefs.fontFamily == key,
                                onClick = { onUpdateAppearance(prefs.copy(fontFamily = key)) },
                                label = { Text(label) }
                            )
                        }
                    }

                    val roundedScale = ((fontScaleSlider * 20).roundToInt() / 20f)
                    Text("Text Scale (${String.format("%.2fx", roundedScale)})", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = fontScaleSlider,
                        onValueChange = { fontScaleSlider = it },
                        onValueChangeFinished = {
                            val clean = ((fontScaleSlider * 20).roundToInt() / 20.0)
                            onUpdateAppearance(prefs.copy(fontScale = clean))
                        },
                        valueRange = 0.85f..1.30f
                    )

                    Text(strings.languageTitle, style = MaterialTheme.typography.labelLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        NabdLanguage.entries.forEach { lang ->
                            FilterChip(
                                selected = prefs.language.equals(lang.code, ignoreCase = true),
                                onClick = {
                                    onUpdateAppearance(prefs.copy(language = lang.code))
                                    onSelectLanguage(lang.code)
                                },
                                label = { Text(lang.nativeLabel) }
                            )
                        }
                    }

                    Text("Voice Note Audio Quality", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = prefs.voiceQuality == "low",
                            onClick = { onUpdateAppearance(prefs.copy(voiceQuality = "low")) },
                            label = { Text("Low Data") }
                        )
                        FilterChip(
                            selected = prefs.voiceQuality == "standard",
                            onClick = { onUpdateAppearance(prefs.copy(voiceQuality = "standard")) },
                            label = { Text("Standard") }
                        )
                    }

                    SettingsSwitchRow(
                        title = "Show Story Highlights on Profile",
                        subtitle = "Display active 24h text stories in Highlights tab",
                        checked = prefs.showStoryHighlights,
                        onCheckedChange = { onUpdateAppearance(prefs.copy(showStoryHighlights = it)) }
                    )

                    HorizontalDivider(thickness = 0.5.dp, color = extColors.hairline)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Storage, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Offline Cache: ${cacheStats.first} pulses · ${cacheStats.second} messages · ${cacheStats.third} chats",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedButton(
                        onClick = onClearCache,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Clear Offline Cache") }
                }
            }
        }
    }

    if (showDeactivateConfirm) {
        AlertDialog(
            onDismissRequest = { showDeactivateConfirm = false },
            title = { Text("Deactivate Account?") },
            text = { Text("Your profile will be hidden from active lists and discovery until you reactivate.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeactivateConfirm = false
                        onDeactivateAccount()
                    }
                ) { Text("Deactivate") }
            },
            dismissButton = {
                TextButton(onClick = { showDeactivateConfirm = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Permanently Delete Account?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Type your handle \"${currentUser.username}\" to confirm permanent deletion.")
                    OutlinedTextField(
                        value = deleteConfirmHandle,
                        onValueChange = { deleteConfirmHandle = it },
                        singleLine = true,
                        label = { Text("@${currentUser.username}") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                    enabled = deleteConfirmHandle.trim().equals(currentUser.username, ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete Forever") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, LocalNabdColors.current.hairline),
        color = LocalNabdColors.current.cardElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp)
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = LocalNabdColors.current.mutedText
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
