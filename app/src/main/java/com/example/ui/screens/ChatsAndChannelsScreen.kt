package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ChannelBroadcast
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.MatchThread
import com.example.data.model.PublicProfile
import com.example.data.model.SocialChannel
import com.example.data.model.SwipeRecord
import com.example.data.model.formatUsdCents
import com.example.ui.components.CallLogBubble
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.VoiceNoteBubble
import com.example.ui.components.VoiceRecorderBar
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.theme.LocalBintafaceAccents
import com.example.ui.theme.MatchEmerald
import com.example.ui.theme.SuperlikeGold
import com.example.ui.util.VoiceAudioController
import com.example.ui.viewmodel.UiState

private val CHANNEL_CATEGORIES = listOf("General", "Music", "Tech", "Lifestyle", "News")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatsAndChannelsScreen(
    currentUserId: String,
    myProfile: PublicProfile?,
    communityDirectory: List<PublicProfile>,
    conversationsState: UiState<List<Conversation>>,
    activeConversation: Conversation?,
    activeDirectMessagesState: UiState<List<DirectMessage>>,
    matchesState: UiState<List<MatchThread>>,
    channelsState: UiState<List<SocialChannel>>,
    subscribedChannelIds: Set<String>,
    activeChannel: SocialChannel?,
    activeChannelBroadcastsState: UiState<List<ChannelBroadcast>>,
    discoverDeckState: UiState<List<PublicProfile>>,
    incomingLikes: List<SwipeRecord>,
    discoverFilter: String,
    newMatchPopup: Pair<PublicProfile, String>?,
    isPlusMember: Boolean,
    dailyLikesRemaining: Int,
    superlikesBalance: Int,
    isSaving: Boolean,
    onOpenDirectConversation: (String) -> Unit,
    onCloseDirectConversation: () -> Unit,
    onCreateOrOpenDirectChat: (PublicProfile) -> Unit,
    onCreateGroupChat: (title: String, members: List<PublicProfile>, onDone: () -> Unit) -> Unit,
    onSendDirectMessage: (String, () -> Unit) -> Unit,
    onOpenMatchChat: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    onCloseChannel: () -> Unit,
    onCreateChannel: (title: String, handle: String, desc: String, cat: String, onDone: () -> Unit) -> Unit,
    onToggleChannelSubscription: (SocialChannel) -> Unit,
    onPublishChannelBroadcast: (channelId: String, text: String, onDone: () -> Unit) -> Unit,
    onSendSparkToChannel: (SocialChannel) -> Unit,
    onSelectDiscoverFilter: (String) -> Unit,
    onSwipe: (PublicProfile, String) -> Unit,
    onRespondToLike: (SwipeRecord, Boolean) -> Unit,
    onDismissMatchPopup: () -> Unit,
    onEditOwnProfile: () -> Unit,
    onOpenPaywall: () -> Unit,
    onSendDirectVoiceNote: (base64: String, durationSec: Long, onDone: () -> Unit) -> Unit = { _, _, _ -> },
    onStartConversationCall: (Conversation, String) -> Unit = { _, _ -> },
    onShowNotice: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var hubTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Chats (WhatsApp), 1 = Channels (Telegram), 2 = Connect (Discover)
    var showNewChatSheet by remember { mutableStateOf(false) }
    var showCreateChannelSheet by remember { mutableStateOf(false) }

    BackHandler(
        enabled = hubTab != 0 &&
            activeConversation == null &&
            activeChannel == null &&
            !showNewChatSheet &&
            !showCreateChannelSheet
    ) {
        hubTab = 0
    }

    val accents = LocalBintafaceAccents.current

    AnimatedContent(
        targetState = hubTab == 2,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
        label = "chatsConnectSwitch"
    ) { isConnectMode ->
        if (isConnectMode) {
            Column(modifier = modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HubSegmentedBar(hubTab = hubTab, onSelect = { hubTab = it })
                }
                DiscoverScreen(
                    myProfile = myProfile,
                    deckState = discoverDeckState,
                    incomingLikes = incomingLikes,
                    selectedFilter = discoverFilter,
                    newMatchPopup = newMatchPopup,
                    isPlusMember = isPlusMember,
                    dailyLikesRemaining = dailyLikesRemaining,
                    superlikesBalance = superlikesBalance,
                    onSelectFilter = onSelectDiscoverFilter,
                    onSwipe = onSwipe,
                    onRespondToLike = onRespondToLike,
                    onDismissMatchPopup = onDismissMatchPopup,
                    onOpenMatchChat = onOpenMatchChat,
                    onEditOwnProfile = onEditOwnProfile,
                    onOpenPaywall = onOpenPaywall,
                    applyStatusBarPadding = false,
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
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
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (hubTab == 0) "Messages & Groups" else "Broadcast Channels",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )
                        OutlinedButton(
                            onClick = {
                                if (hubTab == 0) showNewChatSheet = true
                                else showCreateChannelSheet = true
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag(if (hubTab == 0) "new_chat_button" else "new_channel_button")
                        ) {
                            Icon(
                                imageVector = if (hubTab == 0) Icons.Filled.GroupAdd else Icons.Filled.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(if (hubTab == 0) "New Chat" else "Create")
                        }
                    }

                    HubSegmentedBar(hubTab = hubTab, onSelect = { hubTab = it })

                    AnimatedContent(
                        targetState = hubTab,
                        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(160)) },
                        label = "chatsOrChannelsSwitch",
                        modifier = Modifier.weight(1f)
                    ) { activeTab ->
                        if (activeTab == 0) {
                            ChatsOverviewList(
                                currentUserId = currentUserId,
                                conversationsState = conversationsState,
                                matchesState = matchesState,
                                onOpenConversation = onOpenDirectConversation,
                                onOpenMatchChat = onOpenMatchChat,
                                onOpenNewChat = { showNewChatSheet = true },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            ChannelsOverviewList(
                                currentUserId = currentUserId,
                                channelsState = channelsState,
                                subscribedChannelIds = subscribedChannelIds,
                                onOpenChannel = onOpenChannel,
                                onToggleSubscribe = onToggleChannelSubscription,
                                onCreateChannel = { showCreateChannelSheet = true },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showNewChatSheet) {
        NewChatOrGroupSheet(
            people = communityDirectory.filter { it.userId != currentUserId },
            isSaving = isSaving,
            onStartDirectChat = { person ->
                showNewChatSheet = false
                onCreateOrOpenDirectChat(person)
            },
            onCreateGroupChat = { title, selected ->
                onCreateGroupChat(title, selected) { showNewChatSheet = false }
            },
            onDismiss = { showNewChatSheet = false }
        )
    }

    if (showCreateChannelSheet) {
        CreateChannelSheet(
            isSaving = isSaving,
            onDismiss = { showCreateChannelSheet = false },
            onCreate = { title, handle, desc, cat ->
                onCreateChannel(title, handle, desc, cat) {
                    showCreateChannelSheet = false
                }
            }
        )
    }

    if (activeConversation != null) {
        DirectConversationSheet(
            currentUserId = currentUserId,
            conversation = activeConversation,
            messagesState = activeDirectMessagesState,
            onSendMessage = onSendDirectMessage,
            onSendVoiceNote = onSendDirectVoiceNote,
            onStartCall = { callType -> onStartConversationCall(activeConversation, callType) },
            onShowNotice = onShowNotice,
            onDismiss = onCloseDirectConversation
        )
    }

    if (activeChannel != null) {
        ChannelDetailSheet(
            currentUserId = currentUserId,
            channel = activeChannel,
            isSubscribed = activeChannel.id in subscribedChannelIds,
            broadcastsState = activeChannelBroadcastsState,
            onToggleSubscribe = { onToggleChannelSubscription(activeChannel) },
            onPublishBroadcast = { text, clear ->
                onPublishChannelBroadcast(activeChannel.id, text, clear)
            },
            onSendSpark = { onSendSparkToChannel(activeChannel) },
            onDismiss = onCloseChannel
        )
    }
}

@Composable
private fun HubSegmentedBar(
    hubTab: Int,
    onSelect: (Int) -> Unit
) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SegmentedButton(
            selected = hubTab == 0,
            onClick = { onSelect(0) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
            modifier = Modifier.testTag("hub_chats_tab")
        ) {
            Text("Chats")
        }
        SegmentedButton(
            selected = hubTab == 1,
            onClick = { onSelect(1) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
            modifier = Modifier.testTag("hub_channels_tab")
        ) {
            Text("Channels")
        }
        SegmentedButton(
            selected = hubTab == 2,
            onClick = { onSelect(2) },
            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
            modifier = Modifier.testTag("hub_connect_tab")
        ) {
            Text("Connect")
        }
    }
}

@Composable
private fun ChatsOverviewList(
    currentUserId: String,
    conversationsState: UiState<List<Conversation>>,
    matchesState: UiState<List<MatchThread>>,
    onOpenConversation: (String) -> Unit,
    onOpenMatchChat: (String) -> Unit,
    onOpenNewChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations = (conversationsState as? UiState.Success)?.data ?: emptyList()
    val matches = (matchesState as? UiState.Success)?.data ?: emptyList()

    when {
        conversationsState is UiState.Loading && matchesState is UiState.Loading -> {
            Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        conversationsState is UiState.Error && matchesState is UiState.Error -> {
            Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyStateCard(
                    icon = Icons.Filled.ChatBubble,
                    title = "Couldn't load conversations",
                    subtitle = conversationsState.message
                )
            }
        }
        conversations.isEmpty() && matches.isEmpty() -> {
            Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyStateCard(
                    icon = Icons.Filled.ChatBubble,
                    title = "No Chats or Groups Yet",
                    subtitle = "Start a WhatsApp-style 1-on-1 DM, spin up a group chat, or match with people in Connect.",
                    actionLabel = "Start a Chat",
                    onAction = onOpenNewChat
                )
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (matches.isNotEmpty()) {
                    item {
                        Text(
                            text = "Mutual Matches",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(matches, key = { "match_chip_${it.id}" }) { match ->
                                val partnerName = match.partnerName(currentUserId)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier
                                        .clickable { onOpenMatchChat(match.id) }
                                        .testTag("match_quick_${match.id}")
                                ) {
                                    ProfileAvatar(
                                        name = partnerName,
                                        avatarKey = match.partnerAvatarKey(currentUserId),
                                        size = 56.dp
                                    )
                                    Text(
                                        text = partnerName,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                if (conversations.isNotEmpty()) {
                    item {
                        Text(
                            text = "Direct & Group Chats",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(conversations, key = { it.id }) { conv ->
                        val title = conv.displayTitle(currentUserId)
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenConversation(conv.id) }
                                .testTag("conversation_row_${conv.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (conv.type == "group") {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Groups,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                } else {
                                    ProfileAvatar(name = title, avatarKey = "coffee", size = 48.dp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = formatRelativeTimestamp(conv.lastMessageAt ?: conv.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = conv.lastMessage.ifBlank { "Tap to message" },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                if (matches.isNotEmpty()) {
                    item {
                        Text(
                            text = "Match Threads",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(matches, key = { it.id }) { match ->
                        val partnerName = match.partnerName(currentUserId)
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenMatchChat(match.id) }
                                .testTag("match_row_${match.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ProfileAvatar(
                                    name = partnerName,
                                    avatarKey = match.partnerAvatarKey(currentUserId),
                                    size = 48.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = partnerName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = formatRelativeTimestamp(match.lastMessageAt ?: match.createdAt),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = match.lastMessage.ifBlank { "It's a match! Say hello." },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
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

@Composable
private fun ChannelsOverviewList(
    currentUserId: String,
    channelsState: UiState<List<SocialChannel>>,
    subscribedChannelIds: Set<String>,
    onOpenChannel: (String) -> Unit,
    onToggleSubscribe: (SocialChannel) -> Unit,
    onCreateChannel: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (channelsState) {
        is UiState.Loading -> {
            Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            EmptyStateCard(
                icon = Icons.Filled.Campaign,
                title = "Couldn't load channels",
                subtitle = channelsState.message
            )
        }
        is UiState.Success -> {
            val channels = channelsState.data
            if (channels.isEmpty()) {
                Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    EmptyStateCard(
                        icon = Icons.Filled.Campaign,
                        title = "No Broadcast Channels Yet",
                        subtitle = "Launch a Telegram-style public Channel to broadcast announcements and receive $1 Spark tips (80% creator, 20% app).",
                        actionLabel = "Create Channel",
                        onAction = onCreateChannel
                    )
                }
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(channels, key = { it.id }) { channel ->
                        val isOwner = channel.ownerId == currentUserId
                        val isSubscribed = channel.id in subscribedChannelIds
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenChannel(channel.id) }
                                .testTag("channel_row_${channel.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Campaign,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = channel.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${channel.handle} • ${channel.subscriberCount} subscribers • ${formatUsdCents(channel.totalGiftsCents)} earned",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (channel.description.isNotBlank()) {
                                        Text(
                                            text = channel.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (!isOwner) {
                                    OutlinedButton(
                                        onClick = { onToggleSubscribe(channel) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Text(if (isSubscribed) "Joined" else "Join")
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = "Owner",
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewChatOrGroupSheet(
    people: List<PublicProfile>,
    isSaving: Boolean,
    onStartDirectChat: (PublicProfile) -> Unit,
    onCreateGroupChat: (String, List<PublicProfile>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isGroupMode by remember { mutableStateOf(false) }
    var groupTitle by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<String>() }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isGroupMode) "Create Group Chat" else "New Direct Message",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !isGroupMode,
                    onClick = { isGroupMode = false },
                    label = { Text("1-on-1 DM") }
                )
                FilterChip(
                    selected = isGroupMode,
                    onClick = { isGroupMode = true },
                    label = { Text("Group Chat") }
                )
            }

            if (isGroupMode) {
                OutlinedTextField(
                    value = groupTitle,
                    onValueChange = { if (it.length <= 80) groupTitle = it },
                    label = { Text("Group Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (people.isEmpty()) {
                Text(
                    text = "No other community members are discoverable yet. As soon as others join BINTAFACE, they'll appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(people, key = { it.userId }) { person ->
                        val checked = person.userId in selectedIds
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isGroupMode) {
                                        onStartDirectChat(person)
                                    } else {
                                        if (checked) selectedIds.remove(person.userId)
                                        else selectedIds.add(person.userId)
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ProfileAvatar(
                                    name = person.displayName,
                                    avatarKey = person.avatarKey,
                                    photoBase64 = person.photoBase64,
                                    size = 40.dp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(person.displayName, style = MaterialTheme.typography.titleMedium)
                                    if (person.city.isNotBlank()) {
                                        Text(
                                            text = person.city,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isGroupMode) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = {
                                            if (checked) selectedIds.remove(person.userId)
                                            else selectedIds.add(person.userId)
                                        }
                                    )
                                } else {
                                    Text(
                                        text = "Message",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (isGroupMode) {
                Button(
                    onClick = {
                        val chosen = people.filter { it.userId in selectedIds }
                        onCreateGroupChat(groupTitle.trim(), chosen)
                    },
                    enabled = !isSaving && groupTitle.trim().isNotEmpty() && selectedIds.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Create Group (${selectedIds.size} selected)")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectConversationSheet(
    currentUserId: String,
    conversation: Conversation,
    messagesState: UiState<List<DirectMessage>>,
    onSendMessage: (String, () -> Unit) -> Unit,
    onSendVoiceNote: (base64: String, durationSec: Long, onDone: () -> Unit) -> Unit,
    onStartCall: (callType: String) -> Unit,
    onShowNotice: (String) -> Unit,
    onDismiss: () -> Unit
) {
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = conversation.displayTitle(currentUserId),
                        style = MaterialTheme.typography.headlineMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (conversation.type == "group") {
                            "${conversation.participantIds.size} members • Group Chat"
                        } else {
                            "Private Direct Chat • Voice & Video Ready"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            audioController.release()
                            onStartCall("voice")
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("dm_voice_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "Start voice call",
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
                            .testTag("dm_video_call_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Videocam,
                            contentDescription = "Start video call",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close chat")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                when (messagesState) {
                    is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    is UiState.Error -> Text(messagesState.message, modifier = Modifier.align(Alignment.Center))
                    is UiState.Success -> {
                        if (messages.isEmpty()) {
                            Text(
                                text = "Send a message, voice note, or start a call!",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(messages, key = { it.id }) { msg ->
                                    val isMine = msg.senderId == currentUserId
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
                                    ) {
                                        if (!isMine && conversation.type == "group") {
                                            Text(
                                                text = msg.senderName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(16.dp),
                                            color = when {
                                                msg.messageType == "call_log" -> MaterialTheme.colorScheme.secondaryContainer
                                                isMine -> MaterialTheme.colorScheme.primary
                                                else -> MaterialTheme.colorScheme.surfaceContainerHigh
                                            },
                                            modifier = Modifier.widthIn(max = 280.dp)
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
                                                        color = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
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

            if (isRecording) {
                VoiceRecorderBar(
                    recordingSeconds = recordingSeconds,
                    onCancel = { audioController.cancelRecording() },
                    onSend = {
                        audioController.stopAndEncodeRecording(onShowNotice)?.let { rec ->
                            onSendVoiceNote(rec.base64, rec.durationSec) {}
                        }
                    }
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = { if (it.length <= 1000) text = it },
                        placeholder = { Text("Message…") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dm_message_input")
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
                            .testTag("dm_voice_note_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Record voice note",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    val canSendDm = text.trim().isNotEmpty()
                    IconButton(
                        onClick = {
                            val trimmed = text.trim()
                            if (trimmed.isNotEmpty()) {
                                onSendMessage(trimmed) { text = "" }
                            }
                        },
                        enabled = canSendDm,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSendDm) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            .testTag("dm_send_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = if (canSendDm) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateChannelSheet(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (title: String, handle: String, desc: String, category: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("General") }

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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Create Broadcast Channel", style = MaterialTheme.typography.headlineMedium)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 80) title = it },
                label = { Text("Channel Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = handle,
                onValueChange = { if (it.length <= 40) handle = it },
                label = { Text("@handle") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { if (it.length <= 300) description = it },
                label = { Text("What do you broadcast?") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CHANNEL_CATEGORIES.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }
            Button(
                onClick = { onCreate(title, handle, description, category) },
                enabled = !isSaving && title.trim().length >= 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Launch Channel")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChannelDetailSheet(
    currentUserId: String,
    channel: SocialChannel,
    isSubscribed: Boolean,
    broadcastsState: UiState<List<ChannelBroadcast>>,
    onToggleSubscribe: () -> Unit,
    onPublishBroadcast: (String, () -> Unit) -> Unit,
    onSendSpark: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isOwner = channel.ownerId == currentUserId
    var broadcastText by remember { mutableStateOf("") }
    val broadcasts = (broadcastsState as? UiState.Success)?.data ?: emptyList()

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
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(channel.title, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        text = "${channel.handle} • ${channel.subscriberCount} subscribers • ${formatUsdCents(channel.totalGiftsCents)} earned",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (!isOwner) {
                        Button(onClick = onSendSpark) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Spark $1")
                        }
                        OutlinedButton(onClick = onToggleSubscribe) {
                            if (isSubscribed) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(if (isSubscribed) "Joined" else "Join")
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
            ) {
                when (broadcastsState) {
                    is UiState.Loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    is UiState.Error -> Text(broadcastsState.message, modifier = Modifier.align(Alignment.Center))
                    is UiState.Success -> {
                        if (broadcasts.isEmpty()) {
                            Text(
                                text = if (isOwner) "Post your first channel announcement below!" else "No broadcasts yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(broadcasts, key = { it.id }) { item ->
                                    Surface(
                                        shape = MaterialTheme.shapes.medium,
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = item.ownerName,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = formatRelativeTimestamp(item.createdAt),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(item.text, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isOwner) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = broadcastText,
                        onValueChange = { if (it.length <= 1000) broadcastText = it },
                        placeholder = { Text("Broadcast to all subscribers…") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("channel_broadcast_input")
                    )
                    val canSendBroadcast = broadcastText.trim().isNotEmpty()
                    IconButton(
                        onClick = {
                            val trimmed = broadcastText.trim()
                            if (trimmed.isNotEmpty()) {
                                onPublishBroadcast(trimmed) { broadcastText = "" }
                            }
                        },
                        enabled = canSendBroadcast,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSendBroadcast) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                            .testTag("send_broadcast_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send broadcast",
                            tint = if (canSendBroadcast) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                    }
                }
            }
        }
    }
}
