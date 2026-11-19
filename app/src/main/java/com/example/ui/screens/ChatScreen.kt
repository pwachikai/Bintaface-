package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChatMessage
import com.example.data.model.MatchThread
import com.example.data.model.PublicProfile
import com.example.ui.components.CallLogBubble
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.ProfileDetailBottomSheet
import com.example.ui.components.VoiceNoteBubble
import com.example.ui.components.VoiceRecorderBar
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.util.VoiceAudioController
import com.example.ui.viewmodel.UiState

private val ICEBREAKERS = listOf(
    "Favorite coffee spot around here?",
    "Best weekend adventure lately?",
    "What's on your playlist right now?"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    currentUserId: String,
    match: MatchThread?,
    messagesState: UiState<List<ChatMessage>>,
    onBack: () -> Unit,
    onSendMessage: (String, onDone: () -> Unit) -> Unit,
    onFindProfile: (String) -> PublicProfile?,
    onUnmatch: (String) -> Unit,
    onSendVoiceNote: (base64: String, durationSec: Long, onDone: () -> Unit) -> Unit = { _, _, _ -> },
    onStartCall: (callType: String) -> Unit = {},
    onShowNotice: (String) -> Unit = {}
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioController = remember(context) { VoiceAudioController(context.applicationContext, scope) }
    DisposableEffect(Unit) {
        onDispose { audioController.release() }
    }

    val isRecording by audioController.isRecording.collectAsStateWithLifecycle()
    val recordingSeconds by audioController.recordingSeconds.collectAsStateWithLifecycle()
    val playingMessageId by audioController.playingMessageId.collectAsStateWithLifecycle()
    val playbackProgress by audioController.playbackProgress.collectAsStateWithLifecycle()

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            audioController.startRecording(onShowNotice)
        } else {
            onShowNotice("Microphone permission is needed to record voice notes.")
        }
    }

    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showUnmatchConfirm by remember { mutableStateOf(false) }
    var inspectedProfile by remember { mutableStateOf<PublicProfile?>(null) }
    val listState = rememberLazyListState()

    val partnerId = match?.partnerId(currentUserId) ?: ""
    val partnerName = match?.partnerName(currentUserId) ?: "Chat"
    val partnerAvatar = match?.partnerAvatarKey(currentUserId) ?: "coffee"

    val messageList = (messagesState as? UiState.Success)?.data ?: emptyList()
    LaunchedEffect(messageList.size) {
        if (messageList.isNotEmpty()) {
            listState.animateScrollToItem(messageList.size - 1)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("chat_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to matches"
                        )
                    }
                },
                title = {
                    Row(
                        modifier = Modifier.clickable {
                            if (partnerId.isNotEmpty()) {
                                inspectedProfile = onFindProfile(partnerId)
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ProfileAvatar(
                            name = partnerName,
                            avatarKey = partnerAvatar,
                            size = 38.dp
                        )
                        Column {
                            Text(
                                text = partnerName,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tap to view card",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (match != null) {
                        IconButton(
                            onClick = {
                                audioController.release()
                                onStartCall("voice")
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("chat_voice_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = "Voice call $partnerName",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(
                            onClick = {
                                audioController.release()
                                onStartCall("video")
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("chat_video_call_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Videocam,
                                contentDescription = "Video call $partnerName",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "Chat options")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (partnerId.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("View Profile") },
                                    onClick = {
                                        showMenu = false
                                        inspectedProfile = onFindProfile(partnerId)
                                    }
                                )
                            }
                            if (match != null) {
                                DropdownMenuItem(
                                    text = { Text("Unmatch", color = MaterialTheme.colorScheme.error) },
                                    onClick = {
                                        showMenu = false
                                        showUnmatchConfirm = true
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                if (isRecording) {
                    Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                        VoiceRecorderBar(
                            recordingSeconds = recordingSeconds,
                            onCancel = { audioController.cancelRecording() },
                            onSend = {
                                audioController.stopAndEncodeRecording(onShowNotice)?.let { rec ->
                                    onSendVoiceNote(rec.base64, rec.durationSec) {}
                                }
                            }
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { if (it.length <= 1000) messageText = it },
                            placeholder = { Text("Message $partnerName…") },
                            maxLines = 4,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_message_input")
                        )
                        IconButton(
                            onClick = {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    audioController.startRecording(onShowNotice)
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .testTag("chat_voice_note_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Record voice note",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        val canSendMessage = messageText.trim().isNotEmpty()
                        IconButton(
                            onClick = {
                                val trimmed = messageText.trim()
                                if (trimmed.isNotEmpty()) {
                                    onSendMessage(trimmed) { messageText = "" }
                                }
                            },
                            enabled = canSendMessage,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (canSendMessage) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                                .testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send message",
                                tint = if (canSendMessage) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (messagesState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateCard(
                            icon = Icons.Filled.ChatBubble,
                            title = "Couldn't load messages",
                            subtitle = messagesState.message
                        )
                    }
                }
                is UiState.Success -> {
                    if (messagesState.data.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Break the ice with $partnerName",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ICEBREAKERS.forEach { prompt ->
                                    SuggestionChip(
                                        onClick = { onSendMessage(prompt) {} },
                                        label = { Text(prompt) }
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(messagesState.data, key = { it.id }) { msg ->
                                val isMine = msg.senderId == currentUserId
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(
                                            topStart = 18.dp,
                                            topEnd = 18.dp,
                                            bottomStart = if (isMine) 18.dp else 4.dp,
                                            bottomEnd = if (isMine) 4.dp else 18.dp
                                        ),
                                        color = when {
                                            msg.messageType == "call_log" -> MaterialTheme.colorScheme.secondaryContainer
                                            isMine -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                        },
                                        modifier = Modifier.widthIn(max = 290.dp)
                                    ) {
                                        when (msg.messageType) {
                                            "voice_note" -> {
                                                VoiceNoteBubble(
                                                    isMine = isMine,
                                                    isPlaying = playingMessageId == msg.id,
                                                    progress = playbackProgress,
                                                    durationSec = msg.voiceNoteDurationSec,
                                                    onTogglePlay = {
                                                        audioController.togglePlayback(
                                                            messageId = msg.id,
                                                            base64Audio = msg.voiceNoteBase64,
                                                            onError = onShowNotice
                                                        )
                                                    }
                                                )
                                            }
                                            "call_log" -> {
                                                CallLogBubble(
                                                    text = msg.text,
                                                    callType = msg.callType,
                                                    isMine = false
                                                )
                                            }
                                            else -> {
                                                Text(
                                                    text = msg.text,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = if (isMine) {
                                                        MaterialTheme.colorScheme.onPrimary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurface
                                                    },
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = formatRelativeTimestamp(msg.createdAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    inspectedProfile?.let { profile ->
        ProfileDetailBottomSheet(
            profile = profile,
            onDismiss = { inspectedProfile = null }
        )
    }

    if (showUnmatchConfirm && match != null) {
        AlertDialog(
            onDismissRequest = { showUnmatchConfirm = false },
            title = { Text("Unmatch with $partnerName?") },
            text = { Text("This will remove the match thread and clear your chat connection.") },
            confirmButton = {
                Button(
                    onClick = {
                        showUnmatchConfirm = false
                        onUnmatch(match.id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Unmatch")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showUnmatchConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
