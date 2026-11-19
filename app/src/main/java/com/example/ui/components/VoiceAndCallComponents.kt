package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.CEO_WELCOME_NOTE
import com.example.data.model.CallSession
import com.example.ui.theme.MatchEmerald
import com.example.ui.theme.SuperlikeGold
import com.example.ui.util.formatCallOrVoiceDuration
import kotlinx.coroutines.delay

@Composable
fun CeoWelcomeBanner(
    modifier: Modifier = Modifier,
    dismissible: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ceo_welcome_banner"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Founder's Welcome Note",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = CEO_WELCOME_NOTE,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (dismissible && onDismiss != null) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Dismiss welcome message",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceNoteBubble(
    isMine: Boolean,
    isPlaying: Boolean,
    progress: Float,
    durationSec: Long,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contentTint = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val trackTint = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.28f) else MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = modifier
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .width(210.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        IconButton(
            onClick = onTogglePlay,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.primaryContainer
                )
                .testTag("voice_note_play_button")
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause voice note" else "Play voice note",
                tint = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.GraphicEq,
                    contentDescription = null,
                    tint = contentTint,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Voice Note",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentTint,
                    fontWeight = FontWeight.SemiBold
                )
            }
            LinearProgressIndicator(
                progress = { if (isPlaying) progress else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                trackColor = trackTint
            )
            Text(
                text = formatCallOrVoiceDuration(durationSec.coerceAtLeast(1L)),
                style = MaterialTheme.typography.labelSmall,
                color = contentTint.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
fun CallLogBubble(
    text: String,
    callType: String?,
    isMine: Boolean,
    modifier: Modifier = Modifier
) {
    val contentTint = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (callType == "video") Icons.Filled.Videocam else Icons.Filled.Call,
            contentDescription = null,
            tint = contentTint,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = contentTint
        )
    }
}

@Composable
fun VoiceRecorderBar(
    recordingSeconds: Long,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("cancel_voice_note_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Cancel voice note",
                    tint = MaterialTheme.colorScheme.error
                )
            }
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Recording Voice Note…",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${formatCallOrVoiceDuration(recordingSeconds)} / 0:35",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .testTag("send_voice_note_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send voice note",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
fun IncomingCallBanner(
    callSession: CallSession?,
    onAccept: (CallSession) -> Unit,
    onDecline: (CallSession) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = callSession != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        if (callSession != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("incoming_call_banner"),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileAvatar(
                        name = callSession.callerName,
                        avatarKey = callSession.callerAvatarKey,
                        size = 44.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = callSession.callerName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (callSession.callType == "video") "Incoming BINTAFACE Video Call…"
                            else "Incoming BINTAFACE Voice Call…",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f)
                        )
                    }
                    IconButton(
                        onClick = { onDecline(callSession) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.error)
                            .testTag("decline_incoming_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CallEnd,
                            contentDescription = "Decline call",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { onAccept(callSession) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MatchEmerald)
                            .testTag("accept_incoming_call_button")
                    ) {
                        Icon(
                            imageVector = if (callSession.callType == "video") Icons.Filled.Videocam else Icons.Filled.Call,
                            contentDescription = "Accept call",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CallOverlaySheet(
    currentUserId: String,
    session: CallSession,
    onToggleMute: (Boolean) -> Unit,
    onToggleVideo: (Boolean) -> Unit,
    onEndCall: (elapsedSec: Long) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isVideoCall = session.callType == "video"
    var isMuted by remember(session.id) { mutableStateOf(false) }
    var isSpeakerOn by remember(session.id) { mutableStateOf(isVideoCall) }
    var isVideoEnabled by remember(session.id) { mutableStateOf(isVideoCall) }
    var useFrontCamera by remember(session.id) { mutableStateOf(true) }
    var elapsedSeconds by remember(session.id) { mutableLongStateOf(0L) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.containsKey(Manifest.permission.RECORD_AUDIO)) {
            hasAudioPermission = results[Manifest.permission.RECORD_AUDIO] == true
        }
        if (results.containsKey(Manifest.permission.CAMERA)) {
            hasCameraPermission = results[Manifest.permission.CAMERA] == true
        }
    }

    LaunchedEffect(session.id, isVideoCall) {
        val missing = buildList {
            if (!hasAudioPermission) add(Manifest.permission.RECORD_AUDIO)
            if (isVideoCall && !hasCameraPermission) add(Manifest.permission.CAMERA)
        }
        if (missing.isNotEmpty()) {
            permLauncher.launch(missing.toTypedArray())
        }
    }

    LaunchedEffect(session.id, session.status) {
        if (session.status == "active") {
            while (true) {
                delay(1000L)
                elapsedSeconds += 1L
            }
        }
    }

    val partnerName = session.partnerDisplayName(currentUserId)
    val statusText = when (session.status) {
        "ringing" -> if (session.callerId == currentUserId) "Ringing $partnerName…" else "Connecting…"
        "active" -> "${if (isVideoCall) "Video" else "Voice"} Connected • ${formatCallOrVoiceDuration(elapsedSeconds)}"
        "declined" -> "Call declined"
        else -> "Call ended"
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0E1324), Color(0xFF171E38), Color(0xFF0A0D18))
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .testTag("active_call_overlay")
        ) {
            if (isVideoCall && isVideoEnabled && hasCameraPermission) {
                CameraPreviewSurface(
                    useFrontCamera = useFrontCamera,
                    lifecycleOwner = lifecycleOwner,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.55f),
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.72f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = if (isVideoCall) "BINTAFACE VIDEO CALL" else "BINTAFACE VOICE CALL",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    if (!(isVideoCall && isVideoEnabled && hasCameraPermission)) {
                        ProfileAvatar(
                            name = partnerName,
                            avatarKey = session.callerAvatarKey,
                            size = 104.dp
                        )
                    }
                    Text(
                        text = partnerName,
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (session.status == "active") MatchEmerald else Color.White.copy(alpha = 0.8f)
                    )
                    if (!hasAudioPermission) {
                        Text(
                            text = "Microphone permission needed for live voice",
                            style = MaterialTheme.typography.labelMedium,
                            color = SuperlikeGold
                        )
                    } else if (isVideoCall && !hasCameraPermission) {
                        Text(
                            text = "Camera permission off — audio call mode active",
                            style = MaterialTheme.typography.labelMedium,
                            color = SuperlikeGold
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CallControlPill(
                            icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                            label = if (isMuted) "Unmute" else "Mute",
                            active = isMuted,
                            onClick = {
                                isMuted = !isMuted
                                onToggleMute(isMuted)
                            },
                            testTag = "call_mute_button"
                        )
                        CallControlPill(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            label = if (isSpeakerOn) "Speaker On" else "Earpiece",
                            active = isSpeakerOn,
                            onClick = { isSpeakerOn = !isSpeakerOn },
                            testTag = "call_speaker_button"
                        )
                        if (isVideoCall) {
                            CallControlPill(
                                icon = if (isVideoEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff,
                                label = if (isVideoEnabled) "Cam On" else "Cam Off",
                                active = !isVideoEnabled,
                                onClick = {
                                    isVideoEnabled = !isVideoEnabled
                                    onToggleVideo(isVideoEnabled)
                                },
                                testTag = "call_video_toggle_button"
                            )
                            CallControlPill(
                                icon = Icons.Filled.Cameraswitch,
                                label = "Flip",
                                active = false,
                                onClick = { useFrontCamera = !useFrontCamera },
                                testTag = "call_flip_camera_button"
                            )
                        }
                    }

                    Button(
                        onClick = { onEndCall(elapsedSeconds) },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE53935),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .height(58.dp)
                            .width(200.dp)
                            .testTag("end_call_button")
                    ) {
                        Icon(Icons.Filled.CallEnd, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("End Call", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CallControlPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (active) Color.White
                    else Color.White.copy(alpha = 0.16f)
                )
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) Color(0xFF171E38) else Color.White
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun CameraPreviewSurface(
    useFrontCamera: Boolean,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(useFrontCamera, lifecycleOwner) {
        var boundProvider: ProcessCameraProvider? = null
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            runCatching {
                val provider = cameraProviderFuture.get()
                boundProvider = provider
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val primarySelector = if (useFrontCamera) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
                val selector = when {
                    provider.hasCamera(primarySelector) -> primarySelector
                    provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                    provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                    else -> null
                }
                if (selector != null) {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, selector, preview)
                }
            }
        }
        cameraProviderFuture.addListener(listener, executor)
        onDispose {
            runCatching { boundProvider?.unbindAll() }
        }
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier
    )
}
