package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.data.remote.onGoogleSignInClicked
import com.example.ui.components.MonogramAvatar
import com.example.ui.components.VerifiedBadge
import com.example.ui.theme.AvatarSwatches
import com.example.ui.theme.LocalNabdColors
import com.example.util.LocalNabdStrings
import com.example.util.NabdLanguage
import com.example.util.parseHexColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NabdLanguagePickerPill(
    currentLanguageCode: String,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val current = NabdLanguage.fromCode(currentLanguageCode)

    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            border = BorderStroke(1.dp, LocalNabdColors.current.hairline),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .clickable { expanded = true }
                .testTag("language_picker_pill")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = current.nativeLabel,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = current.nativeLabel,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            NabdLanguage.entries.forEach { lang ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = lang.nativeLabel,
                                fontWeight = if (lang == current) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.weight(1f)
                            )
                            if (lang == current) {
                                Spacer(Modifier.width(12.dp))
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        expanded = false
                        onSelectLanguage(lang.code)
                    }
                )
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    currentLanguageCode: String,
    onSelectLanguage: (String) -> Unit,
    onFinish: () -> Unit
) {
    val strings = LocalNabdStrings.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // Editorial Top Bar with Language Switcher & Skip
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            NabdLanguagePickerPill(
                currentLanguageCode = currentLanguageCode,
                onSelectLanguage = onSelectLanguage
            )
            TextButton(
                onClick = onFinish,
                modifier = Modifier.testTag("onboarding_skip_btn")
            ) {
                Text(strings.skip, style = MaterialTheme.typography.labelLarge)
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 500.dp)
            ) {
                OnboardingEditorialMockupCard(page = page)
                Spacer(Modifier.height(28.dp))
                val (title, desc) = when (page) {
                    0 -> strings.slide1Title to strings.slide1Desc
                    1 -> strings.slide2Title to strings.slide2Desc
                    2 -> strings.slide3Title to strings.slide3Desc
                    else -> strings.slide4Title to strings.slide4Desc
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodyLarge,
                    color = LocalNabdColors.current.mutedText,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Animated page indicator + CTA
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(4) { idx ->
                    val active = pagerState.currentPage == idx
                    val width by animateDpAsState(
                        targetValue = if (active) 24.dp else 8.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "dotWidth"
                    )
                    val color by animateColorAsState(
                        targetValue = if (active) MaterialTheme.colorScheme.primary else LocalNabdColors.current.hairline,
                        label = "dotColor"
                    )
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            Button(
                onClick = {
                    if (pagerState.currentPage < 3) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onFinish()
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .height(50.dp)
                    .testTag("onboarding_next_btn")
            ) {
                Text(
                    text = if (pagerState.currentPage == 3) strings.getStarted else strings.next,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun OnboardingEditorialMockupCard(page: Int) {
    val extColors = LocalNabdColors.current
    Surface(
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, extColors.hairline),
        color = extColors.cardElevated,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 380.dp)
    ) {
        when (page) {
            0 -> Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MonogramAvatar(name = "Salma Nour", colorHex = "#0D9488", avatarPattern = "pulse", size = 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Salma Nour", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(4.dp))
                            VerifiedBadge("personal")
                        }
                        Text("@salma_writes · 2m", style = MaterialTheme.typography.labelMedium, color = extColors.mutedText)
                    }
                }
                Text(
                    text = "Good writing respects your attention. Clean margins, calm typography, and honest conversations.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, extColors.hairline),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Text & Focus", style = MaterialTheme.typography.labelLarge)
                        Text("78%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            1 -> Surface(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .padding(14.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0F172A), Color(0xFF0D9488))
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "نَبْض الكلمة الصادقة",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "24h Typographic Story",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            2 -> Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Icon(Icons.Outlined.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("0:18 · Voice Note", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Icon(Icons.Outlined.AttachFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Editorial_Brief.pdf · 240 KB", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Verified,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VerifiedBadge("personal")
                    VerifiedBadge("business")
                    VerifiedBadge("government")
                }
                Text(
                    text = "AR · EN · TR · FR · ES · DE",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun AuthScreen(
    currentLanguageCode: String,
    onSelectLanguage: (String) -> Unit,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val strings = LocalNabdStrings.current
    val extColors = LocalNabdColors.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var selectedAuthTab by remember { mutableIntStateOf(0) } // 0 = Sign In, 1 = Create Account
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier.fillMaxWidth()
        ) {
            NabdLanguagePickerPill(
                currentLanguageCode = currentLanguageCode,
                onSelectLanguage = onSelectLanguage
            )
        }

        Spacer(Modifier.height(20.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
        ) {
            // Custom Vector Pulse Mark
            val primaryColor = MaterialTheme.colorScheme.primary
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF0E1621),
                modifier = Modifier.size(74.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    val path = Path().apply {
                        moveTo(0f, size.height * 0.55f)
                        lineTo(size.width * 0.22f, size.height * 0.55f)
                        lineTo(size.width * 0.35f, size.height * 0.2f)
                        lineTo(size.width * 0.52f, size.height * 0.85f)
                        lineTo(size.width * 0.68f, size.height * 0.35f)
                        lineTo(size.width * 0.8f, size.height * 0.55f)
                        lineTo(size.width, size.height * 0.55f)
                    }
                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = Stroke(width = 7f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = strings.appTitle,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = strings.tagline,
                style = MaterialTheme.typography.bodyLarge,
                color = extColors.mutedText,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            // Editorial Segmented Switcher: Sign In vs Create Account
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, extColors.hairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    listOf(strings.signInTab, strings.createAccountTab).forEachIndexed { idx, label ->
                        val active = selectedAuthTab == idx
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) MaterialTheme.colorScheme.surface else Color.Transparent,
                            border = if (active) BorderStroke(0.5.dp, extColors.hairline) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedAuthTab = idx
                                    errorMessage = null
                                }
                                .testTag(if (idx == 0) "auth_tab_signin" else "auth_tab_signup")
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            Surface(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, extColors.hairline),
                color = extColors.cardElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = if (selectedAuthTab == 0) strings.signInHeadline else strings.createAccountHeadline,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (selectedAuthTab == 0) strings.signInSubtitle else strings.createAccountSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = extColors.mutedText
                    )

                    Button(
                        onClick = {
                            errorMessage = null
                            isLoading = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isLoading = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = { isLoading = false }
                            )
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Text(
                                text = if (selectedAuthTab == 0) strings.continueWithGoogle else strings.signUpWithGoogle,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = strings.googleAuthNotice,
                                style = MaterialTheme.typography.labelMedium,
                                color = extColors.mutedText
                            )
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(Modifier.height(14.dp))
                Text(
                    text = errorMessage!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun ProfileSetupScreen(
    initialName: String,
    isSaving: Boolean,
    onCheckUsername: suspend (String) -> Boolean,
    onSaveProfile: (
        name: String,
        username: String,
        bio: String,
        location: String,
        website: String,
        avatarColor: String,
        isPrivate: Boolean
    ) -> Unit
) {
    val strings = LocalNabdStrings.current
    var name by remember { mutableStateOf(initialName.ifBlank { "Nabd Member" }) }
    var username by remember {
        mutableStateOf(
            initialName.lowercase().replace(Regex("[^a-z0-9_]"), "").take(16).ifBlank { "nabd_user" }
        )
    }
    var bio by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(AvatarSwatches.first()) }
    var isPrivate by remember { mutableStateOf(false) }

    var usernameAvailable by remember { mutableStateOf<Boolean?>(null) }
    var checkingUsername by remember { mutableStateOf(false) }

    LaunchedEffect(username) {
        val clean = username.trim().lowercase()
        if (!clean.matches(Regex("^[a-z0-9_]{3,24}$"))) {
            usernameAvailable = false
            return@LaunchedEffect
        }
        checkingUsername = true
        delay(350L)
        usernameAvailable = onCheckUsername(clean)
        checkingUsername = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()) {
            Text(
                text = strings.createAccountHeadline,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = strings.createAccountSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = LocalNabdColors.current.mutedText
            )

            Spacer(Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MonogramAvatar(name = name, colorHex = selectedColor, size = 68.dp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Avatar Accent", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AvatarSwatches.forEach { hex ->
                            val isSelected = selectedColor == hex
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(hex))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = hex }
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Outlined.CheckCircle,
                                        contentDescription = "Selected color",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 60) name = it },
                label = { Text(strings.fullNameLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("setup_name_input")
            )

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it.lowercase().filter { c -> c in 'a'..'z' || c in '0'..'9' || c == '_' }.take(24) },
                label = { Text(strings.usernameLabel) },
                prefix = { Text("@") },
                singleLine = true,
                supportingText = {
                    when {
                        checkingUsername -> Text("Checking availability…")
                        username.length < 3 -> Text("Minimum 3 lowercase letters, numbers, or _")
                        usernameAvailable == true -> Text("Available!", color = LocalNabdColors.current.repostEmerald)
                        usernameAvailable == false -> Text("Username unavailable", color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("setup_username_input")
            )

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= 160) bio = it },
                label = { Text("Bio (${bio.length}/160)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().testTag("setup_bio_input")
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { if (it.length <= 60) location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = website,
                    onValueChange = { if (it.length <= 120) website = it },
                    label = { Text("Website") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, LocalNabdColors.current.hairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Private Account", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Require approval for new followers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalNabdColors.current.mutedText
                        )
                    }
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    onSaveProfile(name, username, bio, location, website, selectedColor, isPrivate)
                },
                enabled = !isSaving && name.isNotBlank() && usernameAvailable == true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("complete_setup_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Text(strings.getStarted, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
