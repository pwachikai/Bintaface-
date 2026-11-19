package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.LiveMessage
import com.example.data.model.LiveRoom
import com.example.data.model.PlatformSettings
import com.example.data.model.RadioBroadcast
import com.example.data.model.RadioChannel
import com.example.data.model.formatUsdCents
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.MomentMediaView
import com.example.ui.components.PlusBadge
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.VipLevelBadge
import com.example.ui.theme.LocalBintafaceAccents
import com.example.ui.theme.MatchEmerald
import com.example.ui.theme.RadioAccentColor
import com.example.ui.theme.SuperlikeGold
import com.example.ui.viewmodel.UiState

private val LIVE_CATEGORIES = listOf("All", "Music", "Talk", "Gaming", "Q&A", "Talent")
private val GO_LIVE_CATEGORIES = listOf("Music", "Talk", "Gaming", "Q&A", "Talent")
private val STAGE_COVERS = listOf(
    "studio" to "Studio Neon",
    "sunset" to "Sunset",
    "coffee" to "Lounge",
    "hero" to "BINTAFACE Stage"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(
    currentUserId: String,
    liveRoomsState: UiState<List<LiveRoom>>,
    activeLiveRoom: LiveRoom?,
    activeLiveMessagesState: UiState<List<LiveMessage>>,
    followedUserIds: Set<String>,
    sparksBalance: Int,
    isSaving: Boolean,
    vipLevelForUser: (String) -> String = { "none" },
    onOpenRoom: (LiveRoom) -> Unit,
    onCloseRoom: () -> Unit,
    onGoLive: (title: String, category: String, coverKey: String, onDone: () -> Unit) -> Unit,
    onEndRoom: (String) -> Unit,
    onSendHeart: (String) -> Unit,
    onSendLiveMessage: (roomId: String, text: String, onSent: () -> Unit) -> Unit,
    onSendSparkGift: (LiveRoom) -> Unit,
    onToggleFollow: (String, String) -> Unit,
    initialSubMode: Int = 0,
    subModeRequestTick: Int = 0,
    hasRadioAccess: Boolean = false,
    platformSettings: PlatformSettings? = null,
    radioChannelsState: UiState<List<RadioChannel>> = UiState.Success(emptyList()),
    subscribedRadioIds: Set<String> = emptySet(),
    activeRadioChannel: RadioChannel? = null,
    activeRadioBroadcastsState: UiState<List<RadioBroadcast>> = UiState.Success(emptyList()),
    onOpenRadioPaywall: () -> Unit = {},
    onOpenRadioChannel: (RadioChannel) -> Unit = {},
    onCloseRadioChannel: () -> Unit = {},
    onCreateRadioChannel: (
        title: String,
        frequency: String,
        handle: String,
        category: String,
        description: String,
        coverKey: String,
        onDone: () -> Unit
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onToggleRadioStatus: (RadioChannel) -> Unit = {},
    onToggleRadioSubscription: (RadioChannel) -> Unit = {},
    onPublishRadioBroadcast: (
        channelId: String,
        title: String,
        text: String,
        audioBase64: String?,
        audioDurationSec: Long,
        onSent: () -> Unit
    ) -> Unit = { _, _, _, _, _, _ -> },
    onSendSparkToRadioChannel: (RadioChannel) -> Unit = {},
    onShowNotice: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var liveSubMode by rememberSaveable { mutableIntStateOf(initialSubMode) }
    LaunchedEffect(subModeRequestTick) {
        if (subModeRequestTick > 0) {
            liveSubMode = initialSubMode
        }
    }
    val accents = LocalBintafaceAccents.current
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var showGoLiveSheet by remember { mutableStateOf(false) }

    BackHandler(enabled = liveSubMode == 1 && activeRadioChannel == null) {
        liveSubMode = 0
    }
    BackHandler(enabled = liveSubMode == 0 && selectedCategory != "All" && activeLiveRoom == null && !showGoLiveSheet) {
        selectedCategory = "All"
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (liveSubMode == 1) "Global Radio" else "BINTAFACE Live",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (liveSubMode == 1) {
                            "₦2,000 Pass • FairMoney 9038006221 • Reach Billions"
                        } else {
                            "Interactive rooms • Hosts earn 80% of Spark Gifts"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (liveSubMode == 0) {
                    Button(
                        onClick = { showGoLiveSheet = true },
                        modifier = Modifier.testTag("go_live_button")
                    ) {
                        Icon(Icons.Filled.Videocam, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Go Live")
                    }
                } else {
                    OutlinedButton(
                        onClick = onOpenRadioPaywall,
                        modifier = Modifier.testTag("radio_header_pass_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Radio,
                            contentDescription = null,
                            tint = if (hasRadioAccess) MatchEmerald else RadioAccentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (hasRadioAccess) "ACTIVE" else "₦2,000 Pass")
                    }
                }
            }

            // Segmented Live Video | Global Radio switcher
            val videoBg by animateColorAsState(
                targetValue = if (liveSubMode == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                label = "videoTabBg"
            )
            val radioBg by animateColorAsState(
                targetValue = if (liveSubMode == 1) RadioAccentColor else MaterialTheme.colorScheme.surfaceContainer,
                label = "radioTabBg"
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = videoBg,
                    border = BorderStroke(
                        1.dp,
                        if (liveSubMode == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(50))
                        .clickable { liveSubMode = 0 }
                        .testTag("live_mode_video_tab")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LiveTv,
                            contentDescription = null,
                            tint = if (liveSubMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Live Video",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (liveSubMode == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = radioBg,
                    border = BorderStroke(
                        1.dp,
                        if (liveSubMode == 1) RadioAccentColor else RadioAccentColor.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(50))
                        .clickable { liveSubMode = 1 }
                        .testTag("live_mode_radio_tab")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Radio,
                            contentDescription = null,
                            tint = if (liveSubMode == 1) Color.White else RadioAccentColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Global Radio",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (liveSubMode == 1) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            AnimatedContent(
                targetState = liveSubMode,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                label = "liveSubModeContent",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { currentSubMode ->
                if (currentSubMode == 1) {
                    RadioHubContent(
                        currentUserId = currentUserId,
                        hasRadioAccess = hasRadioAccess,
                        platformSettings = platformSettings,
                        radioChannelsState = radioChannelsState,
                        subscribedRadioIds = subscribedRadioIds,
                        activeRadioChannel = activeRadioChannel,
                        activeRadioBroadcastsState = activeRadioBroadcastsState,
                        sparksBalance = sparksBalance,
                        isSaving = isSaving,
                        vipLevelForUser = vipLevelForUser,
                        onOpenRadioPaywall = onOpenRadioPaywall,
                        onOpenRadioChannel = onOpenRadioChannel,
                        onCloseRadioChannel = onCloseRadioChannel,
                        onCreateRadioChannel = onCreateRadioChannel,
                        onToggleStationStatus = onToggleRadioStatus,
                        onToggleSubscription = onToggleRadioSubscription,
                        onPublishBroadcast = onPublishRadioBroadcast,
                        onSendSparkGift = onSendSparkToRadioChannel,
                        onShowNotice = onShowNotice,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LIVE_CATEGORIES.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategory == cat,
                                    onClick = { selectedCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }

                        when (liveRoomsState) {
                            is UiState.Loading -> {
                                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                            is UiState.Error -> {
                                EmptyStateCard(
                                    icon = Icons.Filled.LiveTv,
                                    title = "Couldn't load live streams",
                                    subtitle = liveRoomsState.message
                                )
                            }
                            is UiState.Success -> {
                                val rooms = liveRoomsState.data.filter {
                                    selectedCategory == "All" || it.category == selectedCategory
                                }
                                if (rooms.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                                        EmptyStateCard(
                                            icon = Icons.Filled.LiveTv,
                                            title = "No Live Rooms Right Now",
                                            subtitle = "Start a BIGO-style live broadcast! Chat with viewers in real time and receive $1 Spark Gifts (you keep $0.80, app keeps $0.20).",
                                            actionLabel = "Start Live Stream",
                                            onAction = { showGoLiveSheet = true }
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(bottom = 24.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(rooms, key = { it.id }) { room ->
                                            LiveRoomCard(
                                                room = room,
                                                hostVipLevel = vipLevelForUser(room.hostId),
                                                onJoin = { onOpenRoom(room) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGoLiveSheet) {
        GoLiveSheet(
            isSaving = isSaving,
            onDismiss = { showGoLiveSheet = false },
            onStartLive = { title, category, coverKey ->
                onGoLive(title, category, coverKey) {
                    showGoLiveSheet = false
                }
            }
        )
    }

    if (activeLiveRoom != null) {
        LiveRoomTheaterSheet(
            currentUserId = currentUserId,
            room = activeLiveRoom,
            messagesState = activeLiveMessagesState,
            isFollowing = activeLiveRoom.hostId in followedUserIds,
            sparksBalance = sparksBalance,
            onToggleFollow = { onToggleFollow(activeLiveRoom.hostId, activeLiveRoom.hostName) },
            onSendHeart = { onSendHeart(activeLiveRoom.id) },
            onSendMessage = { text, clear -> onSendLiveMessage(activeLiveRoom.id, text, clear) },
            onSendSparkGift = { onSendSparkGift(activeLiveRoom) },
            onEndStream = { onEndRoom(activeLiveRoom.id) },
            onDismiss = onCloseRoom
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveRoomCard(
    room: LiveRoom,
    hostVipLevel: String = "none",
    onJoin: () -> Unit
) {
    val isLive = room.status == "live"
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isLive) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onJoin)
            .testTag("live_room_card_${room.id}")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
        ) {
            MomentMediaView(
                imageKey = room.coverKey ?: "studio",
                photoBase64 = room.hostPhotoBase64,
                contentDescription = room.title,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isLive) Color(0xFFE53935) else Color.Black.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = if (isLive) "● LIVE" else "REPLAY",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        Text(
                            text = room.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Text("${room.viewerCount}", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .align(Alignment.BottomStart),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileAvatar(
                        name = room.hostName,
                        avatarKey = room.hostAvatarKey,
                        photoBase64 = room.hostPhotoBase64,
                        size = 44.dp
                    )
                    Column {
                        Text(
                            text = room.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = room.hostName,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            VipLevelBadge(vipLevel = hostVipLevel)
                            if (room.hostIsPlus) PlusBadge()
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = SuperlikeGold.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, SuperlikeGold.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "${room.sparksCount}✨ (${formatUsdCents(room.totalGiftsCents)})",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoLiveSheet(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onStartLive: (title: String, category: String, coverKey: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Music") }
    var coverKey by remember { mutableStateOf("studio") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Go Live on BINTAFACE", style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 120) title = it },
                label = { Text("Stream Title") },
                placeholder = { Text("Acoustic Night & Q&A…") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_title_input")
            )

            Text("Category", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GO_LIVE_CATEGORIES.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Text("Stage Backdrop", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                STAGE_COVERS.forEach { (key, label) ->
                    FilterChip(
                        selected = coverKey == key,
                        onClick = { coverKey = key },
                        label = { Text(label) }
                    )
                }
            }

            Button(
                onClick = { onStartLive(title.trim(), category, coverKey) },
                enabled = !isSaving && title.trim().length >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("start_live_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Start Live Broadcast", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LiveRoomTheaterSheet(
    currentUserId: String,
    room: LiveRoom,
    messagesState: UiState<List<LiveMessage>>,
    isFollowing: Boolean,
    sparksBalance: Int,
    onToggleFollow: () -> Unit,
    onSendHeart: () -> Unit,
    onSendMessage: (String, () -> Unit) -> Unit,
    onSendSparkGift: () -> Unit,
    onEndStream: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isHost = room.hostId == currentUserId
    var chatText by remember { mutableStateOf("") }
    var showEndConfirm by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val pulse = rememberInfiniteTransition(label = "liveDot")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "dotAlpha"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF130F23)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Host Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileAvatar(
                        name = room.hostName,
                        avatarKey = room.hostAvatarKey,
                        photoBase64 = room.hostPhotoBase64,
                        size = 42.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = room.hostName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            if (room.status == "live") {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFFE53935),
                                    modifier = Modifier.alpha(dotAlpha)
                                ) {
                                    Text(
                                        text = "LIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${room.viewerCount} watching • ♥ ${room.likesCount} • ${formatUsdCents(room.totalGiftsCents)} earned",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isHost) {
                        OutlinedButton(
                            onClick = onToggleFollow,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text(if (isFollowing) "Following" else "Follow")
                        }
                    } else if (room.status == "live") {
                        OutlinedButton(
                            onClick = { showEndConfirm = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF8A80))
                        ) {
                            Icon(Icons.Filled.StopCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("End")
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Leave room", tint = Color.White)
                    }
                }
            }

            // Stage Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
                    .clip(MaterialTheme.shapes.large)
            ) {
                MomentMediaView(
                    imageKey = room.coverKey ?: "studio",
                    photoBase64 = room.hostPhotoBase64,
                    contentDescription = room.title,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f))
                            )
                        )
                )
                Text(
                    text = room.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(14.dp)
                )
            }

            // Real-time Live Chat
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
            ) {
                if (messages.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Say hello in live chat or send a $1 Spark Gift!",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (msg.isGiftNotice) SuperlikeGold.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.35f)
                            ) {
                                Text(
                                    text = "${msg.senderName}: ${msg.text}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (msg.isGiftNotice) SuperlikeGold else Color.White,
                                    fontWeight = if (msg.isGiftNotice) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Live Chat Input + Heart + $1 Spark Gift
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = chatText,
                    onValueChange = { if (it.length <= 500) chatText = it },
                    placeholder = { Text("Chat live…", color = Color.White.copy(alpha = 0.6f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White,
                        focusedBorderColor = Color.White.copy(alpha = 0.65f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.32f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("live_chat_input")
                )
                val canSendLiveChat = chatText.trim().isNotEmpty()
                IconButton(
                    onClick = {
                        val trimmed = chatText.trim()
                        if (trimmed.isNotEmpty()) {
                            onSendMessage(trimmed) { chatText = "" }
                        }
                    },
                    enabled = canSendLiveChat,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSendLiveChat) MaterialTheme.colorScheme.primary
                            else Color.White.copy(alpha = 0.16f)
                        )
                        .testTag("live_send_chat_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send live chat",
                        tint = if (canSendLiveChat) Color.White else Color.White.copy(alpha = 0.45f)
                    )
                }
                IconButton(
                    onClick = onSendHeart,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935).copy(alpha = 0.85f))
                        .testTag("live_heart_button")
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Send heart", tint = Color.White)
                }
                if (!isHost) {
                    Surface(
                        shape = CircleShape,
                        color = SuperlikeGold,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onSendSparkGift)
                            .testTag("live_spark_gift_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = "Send $1 Spark Gift ($sparksBalance left)",
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEndConfirm) {
        AlertDialog(
            onDismissRequest = { showEndConfirm = false },
            title = { Text("End Live Broadcast?") },
            text = { Text("Your room will move to Replay mode and new live hearts/messages will close.") },
            confirmButton = {
                Button(
                    onClick = {
                        showEndConfirm = false
                        onEndStream()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("End Stream")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEndConfirm = false }) {
                    Text("Keep Live")
                }
            }
        )
    }
}
