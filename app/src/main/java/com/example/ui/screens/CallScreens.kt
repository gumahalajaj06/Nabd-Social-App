package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.CallSession
import com.example.ui.theme.LocalNabdColors
import com.example.ui.components.MonogramAvatar
import kotlinx.coroutines.delay

@Composable
fun IncomingCallOverlay(
    callSession: CallSession,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val colors = LocalNabdColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (callSession.callType == "video") "Incoming Video Call" else "Incoming Voice Call",
                style = MaterialTheme.typography.titleMedium,
                color = colors.mutedText,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(32.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(120.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size((120f * pulseScale).dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                )
                MonogramAvatar(
                    name = callSession.callerName,
                    colorHex = "#0D9488",
                    size = 96.dp
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = callSession.callerName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "@${callSession.callerUsername.ifEmpty { "caller" }}",
                style = MaterialTheme.typography.bodyLarge,
                color = colors.mutedText
            )

            Spacer(Modifier.height(64.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onDecline,
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFFEF4444), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CallEnd,
                            contentDescription = "Decline",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Decline", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onAccept,
                        modifier = Modifier
                            .size(72.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = "Accept",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Accept", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun ActiveVoiceCallScreen(
    callSession: CallSession,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onSwitchToVideo: () -> Unit,
    onEndCall: () -> Unit
) {
    val colors = LocalNabdColors.current
    var secondsElapsed by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            secondsElapsed++
        }
    }

    val minutes = secondsElapsed / 60
    val seconds = secondsElapsed % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0B0D10))
                )
            )
            .padding(24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Spacer(Modifier.height(48.dp))

            Text(
                text = "Ethora Secure Voice Call",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.5.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            Spacer(Modifier.height(80.dp))

            MonogramAvatar(
                name = callSession.calleeName.ifEmpty { callSession.callerName },
                colorHex = "#0D9488",
                size = 120.dp
            )

            Spacer(Modifier.height(24.dp))

            Text(
                text = callSession.calleeName.ifEmpty { callSession.callerName },
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Connected · HD Audio",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF34D399)
            )

            Spacer(Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                CallControlButton(
                    icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                    label = if (isMuted) "Unmute" else "Mute",
                    isActive = isMuted,
                    onClick = onToggleMute
                )

                CallControlButton(
                    icon = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                    label = "Speaker",
                    isActive = isSpeakerOn,
                    onClick = onToggleSpeaker
                )

                CallControlButton(
                    icon = Icons.Filled.Videocam,
                    label = "Video",
                    isActive = false,
                    onClick = onSwitchToVideo
                )

                CallControlButton(
                    icon = Icons.Filled.CallEnd,
                    label = "End",
                    isActive = true,
                    activeColor = Color(0xFFEF4444),
                    onClick = onEndCall
                )
            }
        }
    }
}

@Composable
fun ActiveVideoCallScreen(
    callSession: CallSession,
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isCameraOn: Boolean,
    isFrontCamera: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleCamera: () -> Unit,
    onFlipCamera: () -> Unit,
    onEndCall: () -> Unit
) {
    var secondsElapsed by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            secondsElapsed++
        }
    }

    val minutes = secondsElapsed / 60
    val seconds = secondsElapsed % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MonogramAvatar(
                    name = callSession.calleeName.ifEmpty { callSession.callerName },
                    colorHex = "#3B82F6",
                    size = 100.dp
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Ethora WebRTC Video Stream",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(timeFormatted, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            IconButton(
                onClick = onFlipCamera,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(Icons.Filled.Cameraswitch, "Flip Camera", tint = Color.White)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.TopEnd
        ) {
            Surface(
                modifier = Modifier
                    .size(100.dp, 150.dp)
                    .padding(top = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                color = Color.DarkGray
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isCameraOn) {
                        Text("You (${if (isFrontCamera) "Front" else "Back"})", color = Color.White, fontSize = 11.sp)
                    } else {
                        Text("Camera Off", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(32.dp),
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onToggleMute) {
                        Icon(
                            if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            contentDescription = "Mute",
                            tint = if (isMuted) Color(0xFFEF4444) else Color.White
                        )
                    }
                    IconButton(onClick = onToggleCamera) {
                        Icon(
                            if (isCameraOn) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                            contentDescription = "Camera",
                            tint = if (isCameraOn) Color.White else Color(0xFFEF4444)
                        )
                    }
                    IconButton(onClick = onToggleSpeaker) {
                        Icon(
                            if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                            contentDescription = "Speaker",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = onEndCall,
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFFEF4444), CircleShape)
                    ) {
                        Icon(Icons.Filled.CallEnd, "End Call", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color = Color.White.copy(alpha = 0.2f),
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(60.dp)
                .background(if (isActive) activeColor else Color.White.copy(alpha = 0.1f), CircleShape)
        ) {
            Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
    }
}
