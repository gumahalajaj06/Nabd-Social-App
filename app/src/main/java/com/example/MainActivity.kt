package com.example

import android.content.Context
import android.os.Bundle
import com.example.data.remote.EthoraCallManager
import com.example.data.remote.CallType
import com.example.data.remote.CallState
import com.example.data.remote.CallSession
import com.example.ui.screens.IncomingCallOverlay
import com.example.ui.screens.ActiveVoiceCallScreen
import com.example.ui.screens.ActiveVideoCallScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.audio.VoiceAudioManager
import com.example.data.local.NabdDatabase
import com.example.data.model.Post
import com.example.data.model.TextStory
import com.example.data.model.UiState
import com.example.data.model.UserAppearancePreferences
import com.example.data.model.UserProfile
import com.example.data.remote.FirestoreProvider
import com.example.data.remote.NabdChatRepository
import com.example.data.remote.NabdSocialRepository
import com.example.data.remote.attemptAutoSignIn
import com.example.data.remote.signOutUser
import com.example.ui.components.ReactionBreakdownSheet
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ChatRoomScreen
import com.example.ui.screens.ChatsListScreen
import com.example.ui.screens.ComposerScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeFeedScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.PostDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProfileSetupScreen
import com.example.ui.screens.SafetyAndModerationScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StoryCreatorScreen
import com.example.ui.screens.StoryViewerScreen
import com.example.ui.screens.VerificationAndAdminScreen
import com.example.ui.theme.NabdTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.FeedViewModel
import com.example.util.ConnectivityObserver
import com.example.util.LocalNabdLanguage
import com.example.util.LocalNabdStrings
import com.example.util.NabdLanguage
import com.example.util.parseHexColor
import com.example.util.stringsForLanguage
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

private val Context.dataStore by preferencesDataStore(name = "nabd_prefs")
private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme_override")
private val HAS_SEEN_ONBOARDING_KEY = booleanPreferencesKey("has_seen_onboarding")
private val APP_LANGUAGE_KEY = stringPreferencesKey("app_language_code")

sealed interface NabdOverlay {
    data class Composer(val quotePost: Post? = null) : NabdOverlay
    data class PostDetail(val postId: String) : NabdOverlay
    data object StoryCreator : NabdOverlay
    data class StoryViewer(val stories: List<TextStory>, val initialIndex: Int) : NabdOverlay
    data object Notifications : NabdOverlay
    data class ChatRoom(val chatId: String) : NabdOverlay
    data class UserProfileView(val userId: String) : NabdOverlay
    data object Verification : NabdOverlay
    data object Safety : NabdOverlay
    data object Settings : NabdOverlay
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val systemDark = isSystemInDarkTheme()
            val defaultDeviceLang = remember {
                NabdLanguage.fromCode(Locale.getDefault().language).code
            }
            val darkThemePref by remember {
                context.dataStore.data.map { prefs -> prefs[DARK_THEME_KEY] ?: systemDark }
            }.collectAsStateWithLifecycle(initialValue = systemDark)
            val hasSeenOnboarding by remember {
                context.dataStore.data.map { prefs -> prefs[HAS_SEEN_ONBOARDING_KEY] ?: false }
            }.collectAsStateWithLifecycle(initialValue = true)
            val appLanguageCode by remember {
                context.dataStore.data.map { prefs -> prefs[APP_LANGUAGE_KEY] ?: defaultDeviceLang }
            }.collectAsStateWithLifecycle(initialValue = defaultDeviceLang)
            val scope = rememberCoroutineScope()

            val currentNabdLang = NabdLanguage.fromCode(appLanguageCode)
            val currentNabdStrings = remember(currentNabdLang) { stringsForLanguage(currentNabdLang) }

            CompositionLocalProvider(
                LocalLayoutDirection provides currentNabdLang.layoutDirection,
                LocalNabdLanguage provides currentNabdLang,
                LocalNabdStrings provides currentNabdStrings
            ) {
                NabdTheme(
                    darkTheme = darkThemePref,
                    languageCode = currentNabdLang.code
                ) {
                    NabdRootApp(
                        isDarkTheme = darkThemePref,
                        hasSeenOnboarding = hasSeenOnboarding,
                        appLanguageCode = currentNabdLang.code,
                        onSelectLanguage = { newCode ->
                            scope.launch {
                                context.dataStore.edit { prefs ->
                                    prefs[APP_LANGUAGE_KEY] = newCode
                                }
                            }
                        },
                        onFinishOnboarding = {
                            scope.launch {
                                context.dataStore.edit { prefs ->
                                    prefs[HAS_SEEN_ONBOARDING_KEY] = true
                                }
                            }
                        },
                        onToggleTheme = {
                            scope.launch {
                                context.dataStore.edit { prefs ->
                                    prefs[DARK_THEME_KEY] = !darkThemePref
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NabdRootApp(
    isDarkTheme: Boolean,
    hasSeenOnboarding: Boolean,
    appLanguageCode: String,
    onSelectLanguage: (String) -> Unit,
    onFinishOnboarding: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    var checkedStartupAuth by remember { mutableStateOf(Firebase.auth.currentUser != null) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                    checkedStartupAuth = true
                },
                onUnauthenticated = {
                    checkedStartupAuth = true
                },
                scope = scope
            )
        }
    }

    val user = currentUser
    when {
        !hasSeenOnboarding && user == null -> {
            OnboardingScreen(
                currentLanguageCode = appLanguageCode,
                onSelectLanguage = onSelectLanguage,
                onFinish = onFinishOnboarding
            )
        }
        !checkedStartupAuth -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        user == null -> {
            AuthScreen(
                currentLanguageCode = appLanguageCode,
                onSelectLanguage = onSelectLanguage,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )
        }
        else -> {
            key(user.uid) {
                AuthenticatedNabdShell(
                    currentUserId = user.uid,
                    initialDisplayName = user.displayName ?: "",
                    appLanguageCode = appLanguageCode,
                    isDarkTheme = isDarkTheme,
                    onSelectLanguage = onSelectLanguage,
                    onToggleTheme = onToggleTheme,
                    onSignOut = {
                        signOutUser(
                            credentialManager = credentialManager,
                            scope = scope
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedNabdShell(
    currentUserId: String,
    initialDisplayName: String,
    appLanguageCode: String,
    isDarkTheme: Boolean,
    onSelectLanguage: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val feedViewModel: FeedViewModel = viewModel(
        key = "feed_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirestoreProvider.get(app)
                val dao = NabdDatabase.getInstance(app).nabdDao()
                FeedViewModel(NabdSocialRepository(db, dao), currentUserId)
            }
        }
    )

    val chatViewModel: ChatViewModel = viewModel(
        key = "chat_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirestoreProvider.get(app)
                val dao = NabdDatabase.getInstance(app).nabdDao()
                ChatViewModel(
                    chatRepo = NabdChatRepository(db, dao),
                    audioManager = VoiceAudioManager(app),
                    connectivityObserver = ConnectivityObserver(app),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val userState by feedViewModel.currentUserState.collectAsStateWithLifecycle()
    val isSaving by feedViewModel.isSaving.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        feedViewModel.actionMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }
    LaunchedEffect(Unit) {
        chatViewModel.chatEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    when (val profileState = userState) {
        is UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(profileState.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onSignOut) { Text("Sign out") }
                }
            }
        }
        is UiState.Success -> {
            val me = profileState.data
            if (me == null) {
                ProfileSetupScreen(
                    initialName = initialDisplayName,
                    isSaving = isSaving,
                    onCheckUsername = feedViewModel::checkUsernameAvailable,
                    onSaveProfile = { name, username, bio, location, website, avatarColor, isPrivate ->
                        feedViewModel.saveProfile(
                            name = name,
                            username = username,
                            bio = bio,
                            location = location,
                            website = website,
                            avatarColor = avatarColor,
                            isPrivate = isPrivate,
                            isNewProfile = true
                        ) { ok ->
                            if (ok) {
                                feedViewModel.updateAppearancePreferences(
                                    UserAppearancePreferences(language = appLanguageCode)
                                )
                            }
                        }
                    }
                )
            } else {
                MainWorkspace(
                    me = me,
                    feedViewModel = feedViewModel,
                    chatViewModel = chatViewModel,
                    snackbarHostState = snackbarHostState,
                    isDarkTheme = isDarkTheme,
                    onSelectLanguage = onSelectLanguage,
                    onToggleTheme = onToggleTheme,
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@Composable
private fun MainWorkspace(
    me: UserProfile,
    feedViewModel: FeedViewModel,
    chatViewModel: ChatViewModel,
    snackbarHostState: SnackbarHostState,
    isDarkTheme: Boolean,
    onSelectLanguage: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val postsState by feedViewModel.postsState.collectAsStateWithLifecycle()
    val storiesState by feedViewModel.storiesState.collectAsStateWithLifecycle()
    val activeUsers by feedViewModel.activeUsers.collectAsStateWithLifecycle()
    val followingIds by feedViewModel.followingIds.collectAsStateWithLifecycle()
    val followerIds by feedViewModel.followerIds.collectAsStateWithLifecycle()
    val bookmarkIds by feedViewModel.bookmarkIds.collectAsStateWithLifecycle()
    val likedIds by feedViewModel.likedPostIds.collectAsStateWithLifecycle()
    val userReactions by feedViewModel.userReactions.collectAsStateWithLifecycle()
    val repostedIds by feedViewModel.repostedPostIds.collectAsStateWithLifecycle()
    val blockedIds by feedViewModel.blockedIds.collectAsStateWithLifecycle()
    val mutedUserIds by feedViewModel.mutedUserIds.collectAsStateWithLifecycle()
    val notifyAuthorIds by feedViewModel.notifyAuthorIds.collectAsStateWithLifecycle()
    val mutedWords by feedViewModel.mutedWords.collectAsStateWithLifecycle()
    val seenStoryIds by feedViewModel.seenStoryIds.collectAsStateWithLifecycle()
    val storyViewers by feedViewModel.storyViewers.collectAsStateWithLifecycle()
    val notifications by feedViewModel.notifications.collectAsStateWithLifecycle()
    val followRequests by feedViewModel.followRequests.collectAsStateWithLifecycle()
    val verificationRequests by feedViewModel.verificationRequests.collectAsStateWithLifecycle()
    val activePostComments by feedViewModel.activePostComments.collectAsStateWithLifecycle()
    val commentReactions by feedViewModel.commentReactions.collectAsStateWithLifecycle()
    val postReactionsBreakdown by feedViewModel.postReactionsBreakdown.collectAsStateWithLifecycle()
    val cacheStats by feedViewModel.cacheStats.collectAsStateWithLifecycle()
    val isSaving by feedViewModel.isSaving.collectAsStateWithLifecycle()

    val chats by chatViewModel.chats.collectAsStateWithLifecycle()
    val isOnline by chatViewModel.isOnline.collectAsStateWithLifecycle()
    val activeMessages by chatViewModel.activeChatMessages.collectAsStateWithLifecycle()
    val replyingTo by chatViewModel.replyingTo.collectAsStateWithLifecycle()
    val isRecording by chatViewModel.isRecordingVoice.collectAsStateWithLifecycle()
    val liveAmplitudes by chatViewModel.liveAmplitudes.collectAsStateWithLifecycle()
    val recordingDurationSec by chatViewModel.recordingDurationSec.collectAsStateWithLifecycle()
    val playbackState by chatViewModel.playbackState.collectAsStateWithLifecycle()

    val callManager = remember { EthoraCallManager(context) }
    val currentCall by callManager.currentCall.collectAsStateWithLifecycle()
    val callState by callManager.callState.collectAsStateWithLifecycle()
    val isMuted by callManager.isMuted.collectAsStateWithLifecycle()
    val isSpeakerOn by callManager.isSpeakerOn.collectAsStateWithLifecycle()
    val isCameraOn by callManager.isCameraOn.collectAsStateWithLifecycle()
    val isFrontCamera by callManager.isFrontCamera.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var exploreQuery by remember { mutableStateOf("") }
    val overlayStack = remember { mutableStateListOf<NabdOverlay>() }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var reactionSheetPostId by remember { mutableStateOf<String?>(null) }
    var sessionLocked by remember(me.uid) { mutableStateOf(me.privacy.appLockEnabled) }

    val allPosts = (postsState as? UiState.Success)?.data ?: emptyList()
    val activeStories = (storiesState as? UiState.Success)?.data ?: emptyList()
    val unreadNotifs = notifications.count { !it.isRead }
    val unreadChats = chats.sumOf { (it.unreadCounts[me.uid] ?: 0L).toInt() }

    val topOverlay = overlayStack.lastOrNull()

    fun pushOverlay(overlay: NabdOverlay) {
        overlayStack.add(overlay)
    }

    fun popOverlay() {
        if (overlayStack.isNotEmpty()) {
            val removed = overlayStack.removeAt(overlayStack.lastIndex)
            if (removed is NabdOverlay.ChatRoom) {
                chatViewModel.selectChat(null)
            }
        }
    }

    fun openUserByUsernameOrId(idOrUsername: String) {
        val target = activeUsers.firstOrNull {
            it.uid == idOrUsername || it.username.equals(idOrUsername, ignoreCase = true)
        }
        if (target != null) {
            pushOverlay(NabdOverlay.UserProfileView(target.uid))
        } else if (idOrUsername == me.uid) {
            selectedTab = 3
        }
    }

    fun openReactionSheet(post: Post) {
        reactionSheetPostId = post.id
        feedViewModel.loadPostReactionsBreakdown(post.id)
    }

    fun toggleQuickTheme() {
        val nextMode = when (me.preferences.themeMode) {
            "dark", "oled" -> "light"
            "light" -> "dark"
            else -> if (isDarkTheme) "light" else "dark"
        }
        feedViewModel.updateAppearancePreferences(me.preferences.copy(themeMode = nextMode))
        onToggleTheme()
    }

    val effectiveLanguage = NabdLanguage.fromCode(me.preferences.language)
    val effectiveStrings = remember(effectiveLanguage) { stringsForLanguage(effectiveLanguage) }
    val effectiveThemeMode = me.preferences.themeMode

    CompositionLocalProvider(
        LocalLayoutDirection provides effectiveLanguage.layoutDirection,
        LocalNabdLanguage provides effectiveLanguage,
        LocalNabdStrings provides effectiveStrings
    ) {
        NabdTheme(
            darkTheme = isDarkTheme,
            themeMode = effectiveThemeMode,
            accentColorHex = me.preferences.accentColor,
            fontFamilyKey = me.preferences.fontFamily,
            languageCode = effectiveLanguage.code,
            fontScaleMultiplier = me.preferences.fontScale.toFloat()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (sessionLocked) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize().padding(24.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = "Nabd Locked",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text("Nabd Session Locked", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = "Signed in as @${me.username}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Button(
                                    onClick = { sessionLocked = false },
                                    modifier = Modifier.testTag("unlock_nabd_btn")
                                ) {
                                    Text("Unlock Nabd")
                                }
                            }
                        }
                    } else {
                    BackHandler(enabled = topOverlay != null || selectedTab != 0) {
                        if (topOverlay != null) {
                            popOverlay()
                        } else {
                            selectedTab = 0
                        }
                    }

                    if (topOverlay != null) {
                        Scaffold(
                            snackbarHost = { SnackbarHost(snackbarHostState) },
                            contentWindowInsets = WindowInsets.safeDrawing
                        ) { _ ->
                            when (topOverlay) {
                                is NabdOverlay.Composer -> ComposerScreen(
                                    currentUser = me,
                                    quotePost = topOverlay.quotePost,
                                    isPublishing = isSaving,
                                    onClose = ::popOverlay,
                                    onPublish = { segments, pollOpts, pollHrs ->
                                        feedViewModel.publishPostOrThread(
                                            author = me,
                                            segments = segments,
                                            quotePost = topOverlay.quotePost,
                                            pollOptions = pollOpts,
                                            pollDurationHours = pollHrs
                                        ) { ok -> if (ok) popOverlay() }
                                    }
                                )

                                is NabdOverlay.PostDetail -> {
                                    val targetPost = allPosts.firstOrNull { it.id == topOverlay.postId }
                                    if (targetPost != null) {
                                        PostDetailScreen(
                                            post = targetPost,
                                            allPosts = allPosts,
                                            currentUser = me,
                                            likedIds = likedIds,
                                            repostedIds = repostedIds,
                                            bookmarkIds = bookmarkIds,
                                            userReactions = userReactions,
                                            comments = activePostComments,
                                            commentReactions = commentReactions,
                                            onObserveComments = feedViewModel::openPostComments,
                                            onHydrateCommentReactions = feedViewModel::hydrateCommentReactions,
                                            onBack = ::popOverlay,
                                            onOpenProfile = ::openUserByUsernameOrId,
                                            onOpenPost = { pushOverlay(NabdOverlay.PostDetail(it.id)) },
                                            onQuotePost = { pushOverlay(NabdOverlay.Composer(it)) },
                                            onLikePost = { feedViewModel.reactToPost(it, me) },
                                            onReactPost = { p, type -> feedViewModel.reactToPost(p, me, type) },
                                            onReactionSummaryClick = ::openReactionSheet,
                                            onRepostPost = { feedViewModel.toggleRepost(it, me) },
                                            onBookmarkPost = { feedViewModel.toggleBookmark(it.id) },
                                            onVotePoll = feedViewModel::votePoll,
                                            onPinReply = { parent, replyId -> feedViewModel.pinReply(parent.id, replyId) },
                                            onSendReply = { parent, text ->
                                                feedViewModel.publishPostOrThread(
                                                    author = me,
                                                    segments = listOf(text),
                                                    replyToPost = parent
                                                )
                                            },
                                            onAddComment = { p, text, parentCommentId ->
                                                feedViewModel.addComment(p, me, text, parentCommentId)
                                            },
                                            onDeleteComment = feedViewModel::deleteComment,
                                            onReactComment = feedViewModel::reactToComment,
                                            onEditPost = { p, newText -> feedViewModel.editPost(p, newText, me) },
                                            onDeletePost = { p ->
                                                feedViewModel.deletePost(p.id, me, allPosts)
                                                popOverlay()
                                            },
                                            onHashtagClick = { tag ->
                                                overlayStack.clear()
                                                exploreQuery = "#$tag"
                                                selectedTab = 1
                                            },
                                            onMentionClick = ::openUserByUsernameOrId,
                                            onShowMessage = feedViewModel::showMessage
                                        )
                                    } else {
                                        LaunchedEffect(Unit) { popOverlay() }
                                    }
                                }

                                is NabdOverlay.StoryCreator -> StoryCreatorScreen(
                                    isPublishing = isSaving,
                                    onClose = ::popOverlay,
                                    onPublish = { text, bgStart, bgEnd, font, align ->
                                        feedViewModel.createStory(me, text, bgStart, bgEnd, font, align) { ok ->
                                            if (ok) popOverlay()
                                        }
                                    }
                                )

                                is NabdOverlay.StoryViewer -> StoryViewerScreen(
                                    stories = topOverlay.stories,
                                    initialIndex = topOverlay.initialIndex,
                                    currentUser = me,
                                    viewers = storyViewers,
                                    onMarkViewed = { feedViewModel.markStoryViewed(it, me) },
                                    onLoadViewers = feedViewModel::loadStoryViewers,
                                    onSendQuickReply = { story, reply ->
                                        val author = activeUsers.firstOrNull { it.uid == story.authorId }
                                            ?: UserProfile(
                                                uid = story.authorId,
                                                name = story.authorName,
                                                username = story.authorUsername,
                                                avatarColor = story.authorAvatarColor,
                                                verifiedType = story.authorVerifiedType
                                            )
                                        chatViewModel.sendQuickStoryReply(me, author, reply)
                                    },
                                    onClose = ::popOverlay
                                )

                                is NabdOverlay.Notifications -> NotificationsScreen(
                                    notifications = notifications,
                                    onMarkRead = feedViewModel::markNotificationRead,
                                    onOpenProfile = ::openUserByUsernameOrId,
                                    onBack = ::popOverlay
                                )

                                is NabdOverlay.ChatRoom -> {
                                    val thread = chats.firstOrNull { it.id == topOverlay.chatId }
                                    if (thread != null) {
                                        ChatRoomScreen(
                                            chatThread = thread,
                                            currentUid = me.uid,
                                            messages = activeMessages,
                                            replyingTo = replyingTo,
                                            isOnline = isOnline,
                                            isRecording = isRecording,
                                            liveAmplitudes = liveAmplitudes,
                                            recordingDurationSec = recordingDurationSec,
                                            playbackState = playbackState,
                                            onBack = ::popOverlay,
                                            onTypingChange = { typing -> chatViewModel.setTyping(thread.id, typing) },
                                            onSendText = { text ->
                                                chatViewModel.sendTextMessage(thread.id, thread.participants, text)
                                            },
                                            onSendAttachment = { uri ->
                                                chatViewModel.sendUriAttachment(context, thread.id, thread.participants, uri)
                                            },
                                            onStartRecording = { chatViewModel.startVoiceRecording(thread.id) },
                                            onStopAndSendVoice = {
                                                chatViewModel.stopAndSendVoiceNote(thread.id, thread.participants)
                                            },
                                            onCancelRecording = { chatViewModel.cancelVoiceRecording(thread.id) },
                                            onTogglePlayVoice = chatViewModel::togglePlayVoice,
                                            onCyclePlaybackSpeed = chatViewModel::cyclePlaybackSpeed,
                                            onSetReply = chatViewModel::setReplyingTo,
                                            onReact = { msg, emoji -> chatViewModel.reactToMessage(thread.id, msg, emoji) },
                                            onDeleteMessage = { msg, forEveryone ->
                                                chatViewModel.deleteMessage(thread.id, msg, forEveryone)
                                            },
                                            onMarkRead = { msgs -> chatViewModel.markMessagesRead(thread.id, msgs) }
                                        )
                                    } else {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator()
                                        }
                                    }
                                }

                                is NabdOverlay.UserProfileView -> {
                                    val target = activeUsers.firstOrNull { it.uid == topOverlay.userId } ?: me
                                    ProfileScreen(
                                        profileUser = target,
                                        currentUser = me,
                                        allPosts = allPosts,
                                        activeUsers = activeUsers,
                                        activeStories = activeStories,
                                        seenStoryIds = seenStoryIds,
                                        followingIds = followingIds,
                                        followerIds = followerIds,
                                        likedIds = likedIds,
                                        repostedIds = repostedIds,
                                        bookmarkIds = bookmarkIds,
                                        blockedIds = blockedIds,
                                        mutedUserIds = mutedUserIds,
                                        notifyAuthorIds = notifyAuthorIds,
                                        userReactions = userReactions,
                                        showBack = true,
                                        onBack = ::popOverlay,
                                        onEditProfile = { showEditProfileDialog = true },
                                        onOpenVerification = { pushOverlay(NabdOverlay.Verification) },
                                        onOpenSafetySettings = { pushOverlay(NabdOverlay.Safety) },
                                        onOpenSettings = { pushOverlay(NabdOverlay.Settings) },
                                        onOpenStoryViewer = { list, idx ->
                                            pushOverlay(NabdOverlay.StoryViewer(list, idx))
                                        },
                                        onToggleFollow = { feedViewModel.toggleFollow(me, it) },
                                        onStartChat = { peer ->
                                            chatViewModel.startOrOpenChat(me, peer) { chatId ->
                                                pushOverlay(NabdOverlay.ChatRoom(chatId))
                                            }
                                        },
                                        onToggleBlock = { feedViewModel.toggleBlockUser(it.uid, it.username) },
                                        onToggleMute = { feedViewModel.toggleMuteUser(it.uid, it.username) },
                                        onToggleNotifyAuthor = { feedViewModel.toggleNotifyAuthor(it.uid, it.username) },
                                        onReportUser = { u, reason -> feedViewModel.reportPost("user_${u.uid}", reason) },
                                        onSetPinMode = feedViewModel::setPinMode,
                                        onPinToProfile = { feedViewModel.toggleManualPinPost(it, me) },
                                        onOpenPost = { pushOverlay(NabdOverlay.PostDetail(it.id)) },
                                        onQuotePost = { pushOverlay(NabdOverlay.Composer(it)) },
                                        onLikePost = { feedViewModel.reactToPost(it, me) },
                                        onReactPost = { p, type -> feedViewModel.reactToPost(p, me, type) },
                                        onReactionSummaryClick = ::openReactionSheet,
                                        onRepostPost = { feedViewModel.toggleRepost(it, me) },
                                        onBookmarkPost = { feedViewModel.toggleBookmark(it.id) },
                                        onVotePoll = feedViewModel::votePoll,
                                        onEditPost = { p, newText -> feedViewModel.editPost(p, newText, me) },
                                        onDeletePost = { p -> feedViewModel.deletePost(p.id, me, allPosts) },
                                        onHashtagClick = { tag ->
                                            overlayStack.clear()
                                            exploreQuery = "#$tag"
                                            selectedTab = 1
                                        },
                                        onMentionClick = ::openUserByUsernameOrId,
                                        onOpenProfile = ::openUserByUsernameOrId,
                                        onShowMessage = feedViewModel::showMessage,
                                        onSignOut = onSignOut
                                    )
                                }

                                is NabdOverlay.Verification -> VerificationAndAdminScreen(
                                    currentUser = me,
                                    isAdmin = feedViewModel.isAdmin,
                                    requests = verificationRequests,
                                    isSubmitting = isSaving,
                                    onBack = ::popOverlay,
                                    onSubmitRequest = { type, org, reason ->
                                        feedViewModel.submitVerification(me, type, org, reason)
                                    },
                                    onReviewRequest = { req, approve ->
                                        feedViewModel.reviewVerification(req, approve, me)
                                    }
                                )

                                is NabdOverlay.Safety -> SafetyAndModerationScreen(
                                    currentUser = me,
                                    mutedWords = mutedWords,
                                    followRequests = followRequests,
                                    onBack = ::popOverlay,
                                    onTogglePrivate = { priv ->
                                        feedViewModel.saveProfile(
                                            name = me.name,
                                            username = me.username,
                                            bio = me.bio,
                                            location = me.location,
                                            website = me.website,
                                            avatarColor = me.avatarColor,
                                            isPrivate = priv,
                                            isNewProfile = false,
                                            previousUsername = me.username,
                                            category = me.category,
                                            roleTitle = me.roleTitle,
                                            pinnedNote = me.pinnedNote,
                                            avatarPattern = me.avatarPattern,
                                            showVerifiedBadge = me.showVerifiedBadge
                                        )
                                    },
                                    onAddMutedWord = { w -> feedViewModel.addMutedWord(w, null) },
                                    onRemoveMutedWord = feedViewModel::removeMutedWord,
                                    onRespondFollowRequest = { req, accept ->
                                        feedViewModel.respondFollowRequest(me, req, accept)
                                    }
                                )

                                is NabdOverlay.Settings -> SettingsScreen(
                                    currentUser = me,
                                    currentUserEmail = feedViewModel.currentUserEmail,
                                    activeUsers = activeUsers,
                                    mutedWords = mutedWords,
                                    mutedUserIds = mutedUserIds,
                                    blockedIds = blockedIds,
                                    followRequests = followRequests,
                                    cacheStats = cacheStats,
                                    onBack = ::popOverlay,
                                    onRefreshCacheStats = feedViewModel::refreshCacheStats,
                                    onClearCache = feedViewModel::clearOfflineCache,
                                    onOpenEditProfile = { showEditProfileDialog = true },
                                    onSaveAvatarStyle = { colorHex, pattern ->
                                        feedViewModel.saveProfile(
                                            name = me.name,
                                            username = me.username,
                                            bio = me.bio,
                                            location = me.location,
                                            website = me.website,
                                            avatarColor = colorHex,
                                            isPrivate = me.isPrivate,
                                            isNewProfile = false,
                                            previousUsername = me.username,
                                            category = me.category,
                                            roleTitle = me.roleTitle,
                                            pinnedNote = me.pinnedNote,
                                            avatarPattern = pattern,
                                            showVerifiedBadge = me.showVerifiedBadge
                                        )
                                    },
                                    onExportDataJson = { feedViewModel.exportUserData(me, allPosts) },
                                    onDeactivateAccount = {
                                        feedViewModel.deactivateAccount {
                                            overlayStack.clear()
                                            onSignOut()
                                        }
                                    },
                                    onDeleteAccount = {
                                        feedViewModel.deleteAccount(me.username) {
                                            overlayStack.clear()
                                            onSignOut()
                                        }
                                    },
                                    onOpenVerification = { pushOverlay(NabdOverlay.Verification) },
                                    onToggleShowVerifiedBadge = { show ->
                                        feedViewModel.saveProfile(
                                            name = me.name,
                                            username = me.username,
                                            bio = me.bio,
                                            location = me.location,
                                            website = me.website,
                                            avatarColor = me.avatarColor,
                                            isPrivate = me.isPrivate,
                                            isNewProfile = false,
                                            previousUsername = me.username,
                                            category = me.category,
                                            roleTitle = me.roleTitle,
                                            pinnedNote = me.pinnedNote,
                                            avatarPattern = me.avatarPattern,
                                            showVerifiedBadge = show
                                        )
                                    },
                                    onTogglePrivateAccount = { priv ->
                                        feedViewModel.saveProfile(
                                            name = me.name,
                                            username = me.username,
                                            bio = me.bio,
                                            location = me.location,
                                            website = me.website,
                                            avatarColor = me.avatarColor,
                                            isPrivate = priv,
                                            isNewProfile = false,
                                            previousUsername = me.username,
                                            category = me.category,
                                            roleTitle = me.roleTitle,
                                            pinnedNote = me.pinnedNote,
                                            avatarPattern = me.avatarPattern,
                                            showVerifiedBadge = me.showVerifiedBadge
                                        )
                                    },
                                    onUpdatePrivacy = feedViewModel::updatePrivacySettings,
                                    onAddMutedWord = feedViewModel::addMutedWord,
                                    onRemoveMutedWord = feedViewModel::removeMutedWord,
                                    onUnmuteUser = { u -> feedViewModel.toggleMuteUser(u.uid, u.username) },
                                    onUnblockUser = { u -> feedViewModel.toggleBlockUser(u.uid, u.username) },
                                    onRespondFollowRequest = { req, accept ->
                                        feedViewModel.respondFollowRequest(me, req, accept)
                                    },
                                    onUpdateNotifications = feedViewModel::updateNotificationSettings,
                                    onUpdateAppearance = feedViewModel::updateAppearancePreferences,
                                    onSelectLanguage = onSelectLanguage,
                                    onSignOut = onSignOut
                                )
                            }
                        }
                    } else {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val useRail = maxWidth >= 600.dp

                            Scaffold(
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                                contentWindowInsets = WindowInsets.safeDrawing,
                                bottomBar = {
                                    if (!useRail) {
                                        NavigationBar {
                                            NavigationBarItem(
                                                selected = selectedTab == 0,
                                                onClick = { selectedTab = 0 },
                                                icon = { Icon(if (selectedTab == 0) Icons.Filled.Home else Icons.Outlined.Home, effectiveStrings.navFeed) },
                                                label = { Text(effectiveStrings.navFeed) },
                                                modifier = Modifier.testTag("nav_feed")
                                            )
                                            NavigationBarItem(
                                                selected = selectedTab == 1,
                                                onClick = { selectedTab = 1 },
                                                icon = { Icon(if (selectedTab == 1) Icons.Filled.Search else Icons.Outlined.Search, effectiveStrings.navExplore) },
                                                label = { Text(effectiveStrings.navExplore) },
                                                modifier = Modifier.testTag("nav_explore")
                                            )
                                            NavigationBarItem(
                                                selected = selectedTab == 2,
                                                onClick = { selectedTab = 2 },
                                                icon = {
                                                    BadgedBox(badge = { if (unreadChats > 0) Badge { Text(unreadChats.toString()) } }) {
                                                        Icon(if (selectedTab == 2) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline, effectiveStrings.navChats)
                                                    }
                                                },
                                                label = { Text(effectiveStrings.navChats) },
                                                modifier = Modifier.testTag("nav_chats")
                                            )
                                            NavigationBarItem(
                                                selected = selectedTab == 3,
                                                onClick = { selectedTab = 3 },
                                                icon = { Icon(if (selectedTab == 3) Icons.Filled.Person else Icons.Outlined.Person, effectiveStrings.navProfile) },
                                                label = { Text(effectiveStrings.navProfile) },
                                                modifier = Modifier.testTag("nav_profile")
                                            )
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                                    if (useRail) {
                                        NavigationRail {
                                            NavigationRailItem(
                                                selected = selectedTab == 0,
                                                onClick = { selectedTab = 0 },
                                                icon = { Icon(Icons.Filled.Home, effectiveStrings.navFeed) },
                                                label = { Text(effectiveStrings.navFeed) }
                                            )
                                            NavigationRailItem(
                                                selected = selectedTab == 1,
                                                onClick = { selectedTab = 1 },
                                                icon = { Icon(Icons.Filled.Search, effectiveStrings.navExplore) },
                                                label = { Text(effectiveStrings.navExplore) }
                                            )
                                            NavigationRailItem(
                                                selected = selectedTab == 2,
                                                onClick = { selectedTab = 2 },
                                                icon = { Icon(Icons.Filled.ChatBubble, effectiveStrings.navChats) },
                                                label = { Text(effectiveStrings.navChats) }
                                            )
                                            NavigationRailItem(
                                                selected = selectedTab == 3,
                                                onClick = { selectedTab = 3 },
                                                icon = { Icon(Icons.Filled.Person, effectiveStrings.navProfile) },
                                                label = { Text(effectiveStrings.navProfile) }
                                            )
                                        }
                                    }

                                    Box(
                                        contentAlignment = Alignment.TopCenter,
                                        modifier = Modifier.weight(1f).fillMaxSize()
                                    ) {
                                        Box(modifier = Modifier.widthIn(max = 720.dp).fillMaxSize()) {
                                            when (selectedTab) {
                                                0 -> HomeFeedScreen(
                                                    currentUser = me,
                                                    postsState = postsState,
                                                    storiesState = storiesState,
                                                    followingIds = followingIds,
                                                    bookmarkIds = bookmarkIds,
                                                    likedIds = likedIds,
                                                    repostedIds = repostedIds,
                                                    blockedIds = blockedIds,
                                                    mutedUserIds = mutedUserIds,
                                                    mutedWords = mutedWords,
                                                    seenStoryIds = seenStoryIds,
                                                    userReactions = userReactions,
                                                    unreadNotifCount = unreadNotifs,
                                                    isOnline = isOnline,
                                                    isDarkTheme = effectiveThemeMode == "dark" || effectiveThemeMode == "oled" || (effectiveThemeMode == "system" && isDarkTheme),
                                                    onToggleTheme = ::toggleQuickTheme,
                                                    onHydratePosts = feedViewModel::hydrateReactionStates,
                                                    onOpenNotifications = { pushOverlay(NabdOverlay.Notifications) },
                                                    onOpenComposer = { pushOverlay(NabdOverlay.Composer()) },
                                                    onOpenCreateStory = { pushOverlay(NabdOverlay.StoryCreator) },
                                                    onOpenStoryViewer = { list, idx ->
                                                        pushOverlay(NabdOverlay.StoryViewer(list, idx))
                                                    },
                                                    onOpenPostDetail = { pushOverlay(NabdOverlay.PostDetail(it.id)) },
                                                    onOpenQuoteComposer = { pushOverlay(NabdOverlay.Composer(it)) },
                                                    onOpenProfile = ::openUserByUsernameOrId,
                                                    onSearchHashtag = { tag ->
                                                        exploreQuery = "#$tag"
                                                        selectedTab = 1
                                                    },
                                                    onOpenMention = ::openUserByUsernameOrId,
                                                    onLikePost = { feedViewModel.reactToPost(it, me) },
                                                    onReactPost = { p, type -> feedViewModel.reactToPost(p, me, type) },
                                                    onReactionSummaryClick = ::openReactionSheet,
                                                    onRepostPost = { feedViewModel.toggleRepost(it, me) },
                                                    onBookmarkPost = { feedViewModel.toggleBookmark(it.id) },
                                                    onVotePoll = feedViewModel::votePoll,
                                                    onEditPost = { p, newText -> feedViewModel.editPost(p, newText, me) },
                                                    onDeletePost = { p -> feedViewModel.deletePost(p.id, me, allPosts) },
                                                    onPinToProfile = { p -> feedViewModel.toggleManualPinPost(p, me) },
                                                    onReportPost = { post, reason -> feedViewModel.reportPost(post.id, reason) },
                                                    onBlockAuthor = { feedViewModel.toggleBlockUser(it.authorId, it.authorUsername) },
                                                    onShowMessage = feedViewModel::showMessage
                                                )

                                                1 -> ExploreScreen(
                                                    initialQuery = exploreQuery,
                                                    currentUser = me,
                                                    allPosts = allPosts,
                                                    activeUsers = activeUsers,
                                                    trendingHashtags = feedViewModel.computeTrendingHashtags(allPosts),
                                                    followingIds = followingIds,
                                                    likedIds = likedIds,
                                                    repostedIds = repostedIds,
                                                    bookmarkIds = bookmarkIds,
                                                    blockedIds = blockedIds,
                                                    mutedUserIds = mutedUserIds,
                                                    userReactions = userReactions,
                                                    onOpenProfile = ::openUserByUsernameOrId,
                                                    onOpenPost = { pushOverlay(NabdOverlay.PostDetail(it.id)) },
                                                    onQuotePost = { pushOverlay(NabdOverlay.Composer(it)) },
                                                    onLikePost = { feedViewModel.reactToPost(it, me) },
                                                    onReactPost = { p, type -> feedViewModel.reactToPost(p, me, type) },
                                                    onReactionSummaryClick = ::openReactionSheet,
                                                    onRepostPost = { feedViewModel.toggleRepost(it, me) },
                                                    onBookmarkPost = { feedViewModel.toggleBookmark(it.id) },
                                                    onVotePoll = feedViewModel::votePoll,
                                                    onToggleFollow = { feedViewModel.toggleFollow(me, it) },
                                                    onShowMessage = feedViewModel::showMessage
                                                )

                                                2 -> ChatsListScreen(
                                                    currentUser = me,
                                                    chats = chats,
                                                    activeUsers = activeUsers,
                                                    isOnline = isOnline,
                                                    onOpenChat = { thread ->
                                                        chatViewModel.selectChat(thread.id)
                                                        pushOverlay(NabdOverlay.ChatRoom(thread.id))
                                                    },
                                                    onStartChatWithUser = { peer ->
                                                        chatViewModel.startOrOpenChat(me, peer) { chatId ->
                                                            pushOverlay(NabdOverlay.ChatRoom(chatId))
                                                        }
                                                    }
                                                )

                                                else -> ProfileScreen(
                                                    profileUser = me,
                                                    currentUser = me,
                                                    allPosts = allPosts,
                                                    activeUsers = activeUsers,
                                                    activeStories = activeStories,
                                                    seenStoryIds = seenStoryIds,
                                                    followingIds = followingIds,
                                                    followerIds = followerIds,
                                                    likedIds = likedIds,
                                                    repostedIds = repostedIds,
                                                    bookmarkIds = bookmarkIds,
                                                    blockedIds = blockedIds,
                                                    mutedUserIds = mutedUserIds,
                                                    notifyAuthorIds = notifyAuthorIds,
                                                    userReactions = userReactions,
                                                    showBack = false,
                                                    onBack = {},
                                                    onEditProfile = { showEditProfileDialog = true },
                                                    onOpenVerification = { pushOverlay(NabdOverlay.Verification) },
                                                    onOpenSafetySettings = { pushOverlay(NabdOverlay.Safety) },
                                                    onOpenSettings = { pushOverlay(NabdOverlay.Settings) },
                                                    onOpenStoryViewer = { list, idx ->
                                                        pushOverlay(NabdOverlay.StoryViewer(list, idx))
                                                    },
                                                    onToggleFollow = {},
                                                    onStartChat = {},
                                                    onToggleBlock = {},
                                                    onSetPinMode = feedViewModel::setPinMode,
                                                    onPinToProfile = { p -> feedViewModel.toggleManualPinPost(p, me) },
                                                    onOpenPost = { pushOverlay(NabdOverlay.PostDetail(it.id)) },
                                                    onQuotePost = { pushOverlay(NabdOverlay.Composer(it)) },
                                                    onLikePost = { feedViewModel.reactToPost(it, me) },
                                                    onReactPost = { p, type -> feedViewModel.reactToPost(p, me, type) },
                                                    onReactionSummaryClick = ::openReactionSheet,
                                                    onRepostPost = { feedViewModel.toggleRepost(it, me) },
                                                    onBookmarkPost = { feedViewModel.toggleBookmark(it.id) },
                                                    onVotePoll = feedViewModel::votePoll,
                                                    onEditPost = { p, newText -> feedViewModel.editPost(p, newText, me) },
                                                    onDeletePost = { p -> feedViewModel.deletePost(p.id, me, allPosts) },
                                                    onHashtagClick = { tag ->
                                                        exploreQuery = "#$tag"
                                                        selectedTab = 1
                                                    },
                                                    onMentionClick = ::openUserByUsernameOrId,
                                                    onOpenProfile = ::openUserByUsernameOrId,
                                                    onShowMessage = feedViewModel::showMessage,
                                                    onSignOut = onSignOut
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (reactionSheetPostId != null) {
                    ReactionBreakdownSheet(
                        reactions = postReactionsBreakdown,
                        onDismiss = { reactionSheetPostId = null },
                        onUserClick = ::openUserByUsernameOrId
                    )
                }

                if (showEditProfileDialog) {
                    var name by remember { mutableStateOf(me.name) }
                    var bio by remember { mutableStateOf(me.bio) }
                    var location by remember { mutableStateOf(me.location) }
                    var website by remember { mutableStateOf(me.website) }
                    var category by remember { mutableStateOf(me.category) }
                    var roleTitle by remember { mutableStateOf(me.roleTitle) }
                    var pinnedNote by remember { mutableStateOf(me.pinnedNote) }
                    var avatarColor by remember { mutableStateOf(me.avatarColor) }
                    var avatarPattern by remember { mutableStateOf(me.avatarPattern) }

                    AlertDialog(
                        onDismissRequest = { showEditProfileDialog = false },
                        title = { Text("Edit Profile") },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            ) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { if (it.length <= 60) name = it },
                                    label = { Text("Display Name") },
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = bio,
                                    onValueChange = { if (it.length <= 160) bio = it },
                                    label = { Text("Bio (${bio.length}/160)") },
                                    minLines = 2
                                )
                                OutlinedTextField(
                                    value = roleTitle,
                                    onValueChange = { if (it.length <= 40) roleTitle = it },
                                    label = { Text("Role / Title") },
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = category,
                                    onValueChange = { if (it.length <= 40) category = it },
                                    label = { Text("Category (Writer, Tech, News…)") },
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = pinnedNote,
                                    onValueChange = { if (it.length <= 140) pinnedNote = it },
                                    label = { Text("Pinned Status Note") },
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = location,
                                    onValueChange = { if (it.length <= 60) location = it },
                                    label = { Text("Location") },
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = website,
                                    onValueChange = { if (it.length <= 120) website = it },
                                    label = { Text("Website") },
                                    singleLine = true
                                )
                                Text("Avatar Pattern", style = MaterialTheme.typography.labelLarge)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                ) {
                                    listOf("solid", "rings", "dots", "diagonal", "pulse").forEach { pattern ->
                                        FilterChip(
                                            selected = avatarPattern == pattern,
                                            onClick = { avatarPattern = pattern },
                                            label = { Text(pattern.replaceFirstChar { it.uppercase() }) }
                                        )
                                    }
                                }
                                Text("Avatar Color", style = MaterialTheme.typography.labelLarge)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("#0D9488", "#2563EB", "#7C3AED", "#E11D48", "#EA580C", "#16A34A").forEach { hex ->
                                        val selected = avatarColor.equals(hex, ignoreCase = true)
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(parseHexColor(hex))
                                                .border(
                                                    width = if (selected) 2.dp else 0.dp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    shape = CircleShape
                                                )
                                                .clickable { avatarColor = hex }
                                        ) {
                                            if (selected) {
                                                Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    feedViewModel.saveProfile(
                                        name = name,
                                        username = me.username,
                                        bio = bio,
                                        location = location,
                                        website = website,
                                        avatarColor = avatarColor,
                                        isPrivate = me.isPrivate,
                                        isNewProfile = false,
                                        previousUsername = me.username,
                                        category = category,
                                        roleTitle = roleTitle,
                                        pinnedNote = pinnedNote,
                                        avatarPattern = avatarPattern,
                                        showVerifiedBadge = me.showVerifiedBadge
                                    ) { ok ->
                                        if (ok) showEditProfileDialog = false
                                    }
                                },
                                enabled = name.isNotBlank()
                            ) { Text("Save") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancel") }
                        }
                    )
                }
            }
        }

            if (callState == CallState.INCOMING_RINGING && currentCall != null && currentCall?.calleeId == me.uid) {
                IncomingCallOverlay(
                    callSession = currentCall!!,
                    onAccept = { callManager.acceptCall(currentCall!!) },
                    onDecline = { callManager.declineCall(currentCall!!) }
                )
            } else if (callState == CallState.ACTIVE || callState == CallState.OUTGOING_RINGING) {
                if (currentCall?.callType == "video") {
                    ActiveVideoCallScreen(
                        callSession = currentCall ?: CallSession(),
                        isMuted = isMuted,
                        isSpeakerOn = isSpeakerOn,
                        isCameraOn = isCameraOn,
                        isFrontCamera = isFrontCamera,
                        onToggleMute = callManager::toggleMute,
                        onToggleSpeaker = callManager::toggleSpeaker,
                        onToggleCamera = callManager::toggleCamera,
                        onFlipCamera = callManager::flipCamera,
                        onEndCall = { callManager.endCall(currentCall) }
                    )
                } else {
                    ActiveVoiceCallScreen(
                        callSession = currentCall ?: CallSession(),
                        isMuted = isMuted,
                        isSpeakerOn = isSpeakerOn,
                        onToggleMute = callManager::toggleMute,
                        onToggleSpeaker = callManager::toggleSpeaker,
                        onSwitchToVideo = {},
                        onEndCall = { callManager.endCall(currentCall) }
                    )
                }
            }
        }
    }
}
