package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DEFAULT_ADMIN_ACCOUNT_HOLDER
import com.example.data.model.DEFAULT_ADMIN_ACCOUNT_NUMBER
import com.example.data.model.DEFAULT_ADMIN_BANK_NAME
import com.example.data.model.PlatformSettings
import com.example.data.model.RADIO_ACCESS_FEE_NGN
import com.example.data.model.RADIO_CATEGORIES
import com.example.data.model.RadioBroadcast
import com.example.data.model.RadioChannel
import com.example.data.model.formatListenerCount
import com.example.data.model.formatNgnWhole
import com.example.data.model.formatUsdCents
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.VipLevelBadge
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.theme.MatchEmerald
import com.example.ui.theme.RadioAccentColor
import com.example.ui.theme.RadioBroadcastRed
import com.example.ui.theme.SuperlikeGold
import com.example.ui.theme.VipMoonDiamondColor
import com.example.ui.util.VoiceAudioController
import com.example.ui.util.VoiceNoteRecording
import com.example.ui.util.formatCallOrVoiceDuration
import com.example.ui.viewmodel.UiState

private val RADIO_STAGE_COVERS = listOf(
    "studio" to "Studio Neon",
    "sunset" to "Sunset Waves",
    "coffee" to "Midnight Lounge",
    "hero" to "Global Broadcast"
)

@Composable
fun RadioHubContent(
    currentUserId: String,
    hasRadioAccess: Boolean,
    platformSettings: PlatformSettings?,
    radioChannelsState: UiState<List<RadioChannel>>,
    subscribedRadioIds: Set<String>,
    activeRadioChannel: RadioChannel?,
    activeRadioBroadcastsState: UiState<List<RadioBroadcast>>,
    sparksBalance: Int,
    isSaving: Boolean,
    vipLevelForUser: (String) -> String = { "none" },
    onOpenRadioPaywall: () -> Unit,
    onOpenRadioChannel: (RadioChannel) -> Unit,
    onCloseRadioChannel: () -> Unit,
    onCreateRadioChannel: (
        title: String,
        frequency: String,
        handle: String,
        category: String,
        description: String,
        coverKey: String,
        onDone: () -> Unit
    ) -> Unit,
    onToggleStationStatus: (RadioChannel) -> Unit,
    onToggleSubscription: (RadioChannel) -> Unit,
    onPublishBroadcast: (
        channelId: String,
        title: String,
        text: String,
        audioBase64: String?,
        audioDurationSec: Long,
        onSent: () -> Unit
    ) -> Unit,
    onSendSparkGift: (RadioChannel) -> Unit,
    onShowNotice: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by rememberSaveable { mutableStateOf("All") }
    var showCreateStationSheet by remember { mutableStateOf(false) }

    val adminBankName = remember(platformSettings?.bankName) {
        platformSettings?.bankName?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_ADMIN_BANK_NAME
    }
    val adminAccountNumber = remember(platformSettings?.adminAccountNumber) {
        platformSettings?.adminAccountNumber?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_ADMIN_ACCOUNT_NUMBER
    }
    val adminAccountHolder = remember(platformSettings?.adminAccountHolder) {
        platformSettings?.adminAccountHolder?.trim()?.takeIf { it.isNotBlank() } ?: DEFAULT_ADMIN_ACCOUNT_HOLDER
    }

    BackHandler(enabled = selectedCategory != "All" && activeRadioChannel == null && !showCreateStationSheet) {
        selectedCategory = "All"
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Global Radio Hero Card
        Surface(
            shape = MaterialTheme.shapes.large,
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(
                    Brush.linearGradient(
                        colors = listOf(RadioAccentColor, VipMoonDiamondColor)
                    )
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Radio,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(8.dp)
                                    .size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "BINTAFACE Global Radio",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Broadcast to billions worldwide • 80% Spark tips for hosts",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }

                    if (hasRadioAccess) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MatchEmerald
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = formatNgnWhole(RADIO_ACCESS_FEE_NGN),
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                if (!hasRadioAccess) {
                    Text(
                        text = "Pay ${formatNgnWhole(RADIO_ACCESS_FEE_NGN)} to $adminAccountHolder ($adminBankName $adminAccountNumber) via Google Verified Checkout to tune in & launch your own Global Radio Station.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    Button(
                        onClick = onOpenRadioPaywall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = RadioAccentColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("unlock_radio_banner_button")
                    ) {
                        Icon(Icons.Filled.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Pay ${formatNgnWhole(RADIO_ACCESS_FEE_NGN)} to Unlock Radio & Create Station",
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showCreateStationSheet = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = RadioAccentColor
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("create_radio_station_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create Radio Station", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onOpenRadioPaywall,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Pass Receipt")
                        }
                    }
                }
            }
        }

        // Category Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All").plus(RADIO_CATEGORIES).forEach { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    label = { Text(cat) }
                )
            }
        }

        when (radioChannelsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                EmptyStateCard(
                    icon = Icons.Filled.Radio,
                    title = "Couldn't load Global Radio",
                    subtitle = radioChannelsState.message
                )
            }
            is UiState.Success -> {
                val stations = radioChannelsState.data.filter {
                    selectedCategory == "All" || it.category == selectedCategory
                }
                if (stations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        EmptyStateCard(
                            icon = Icons.Filled.Podcasts,
                            title = "No Radio Stations Yet",
                            subtitle = if (hasRadioAccess) {
                                "Your Global Radio Pass is ACTIVE! Launch the first worldwide station to broadcast voice & talk shows to billions of people."
                            } else {
                                "Pay ${formatNgnWhole(RADIO_ACCESS_FEE_NGN)} to $adminAccountHolder ($adminBankName $adminAccountNumber) to launch your own global radio station."
                            },
                            actionLabel = if (hasRadioAccess) "Create First Station" else "Unlock Global Radio (${formatNgnWhole(RADIO_ACCESS_FEE_NGN)})",
                            onAction = {
                                if (hasRadioAccess) showCreateStationSheet = true
                                else onOpenRadioPaywall()
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(stations, key = { it.id }) { station ->
                            RadioStationCard(
                                station = station,
                                hasRadioAccess = hasRadioAccess,
                                isSubscribed = station.id in subscribedRadioIds,
                                ownerVipLevel = vipLevelForUser(station.ownerId),
                                onTuneIn = {
                                    if (hasRadioAccess) onOpenRadioChannel(station)
                                    else onOpenRadioPaywall()
                                },
                                onToggleSubscribe = {
                                    if (hasRadioAccess) onToggleSubscription(station)
                                    else onOpenRadioPaywall()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateStationSheet) {
        CreateRadioChannelSheet(
            isSaving = isSaving,
            onDismiss = { showCreateStationSheet = false },
            onCreate = { title, frequency, handle, category, description, coverKey ->
                onCreateRadioChannel(title, frequency, handle, category, description, coverKey) {
                    showCreateStationSheet = false
                }
            }
        )
    }

    if (activeRadioChannel != null) {
        RadioStationStudioSheet(
            currentUserId = currentUserId,
            station = activeRadioChannel,
            broadcastsState = activeRadioBroadcastsState,
            isSubscribed = activeRadioChannel.id in subscribedRadioIds,
            sparksBalance = sparksBalance,
            isSaving = isSaving,
            onToggleLiveStatus = { onToggleStationStatus(activeRadioChannel) },
            onToggleSubscribe = { onToggleSubscription(activeRadioChannel) },
            onSendSparkGift = { onSendSparkGift(activeRadioChannel) },
            onPublishBroadcast = { title, text, audioBase64, audioDurationSec, onSent ->
                onPublishBroadcast(
                    activeRadioChannel.id,
                    title,
                    text,
                    audioBase64,
                    audioDurationSec,
                    onSent
                )
            },
            onShowNotice = onShowNotice,
            onDismiss = onCloseRadioChannel
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RadioStationCard(
    station: RadioChannel,
    hasRadioAccess: Boolean,
    isSubscribed: Boolean,
    ownerVipLevel: String,
    onTuneIn: () -> Unit,
    onToggleSubscribe: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "radio_on_air")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "radio_pulse"
    )
    val isLive = station.status == "live"

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isLive) 1.5.dp else 1.dp,
            color = if (isLive) RadioAccentColor.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClick = onTuneIn)
            .testTag("radio_station_card_${station.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isLive) RadioBroadcastRed else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = if (isLive) Modifier.alpha(pulseAlpha) else Modifier
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = if (isLive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isLive) "ON AIR" else "OFF AIR",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = RadioAccentColor.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = station.frequency,
                            style = MaterialTheme.typography.labelSmall,
                            color = RadioAccentColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = station.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Headphones,
                        contentDescription = null,
                        tint = RadioAccentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = formatListenerCount(station.listenersCount),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileAvatar(
                    name = station.ownerName,
                    avatarKey = station.ownerAvatarKey,
                    size = 48.dp
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = station.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${station.ownerName} • ${station.handle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        VipLevelBadge(vipLevel = ownerVipLevel)
                    }
                }
            }

            if (station.description.isNotBlank()) {
                Text(
                    text = station.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${formatListenerCount(station.subscribersCount)} subscribers • ${formatUsdCents(station.totalGiftsCents)} tips",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onToggleSubscribe,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(if (isSubscribed) "Subscribed" else "Subscribe")
                    }
                    Button(
                        onClick = onTuneIn,
                        colors = ButtonDefaults.buttonColors(containerColor = RadioAccentColor),
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = if (hasRadioAccess) Icons.Filled.PlayArrow else Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (hasRadioAccess) "Tune In" else "Unlock")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateRadioChannelSheet(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (
        title: String,
        frequency: String,
        handle: String,
        category: String,
        description: String,
        coverKey: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("104.7 FM") }
    var handle by remember { mutableStateOf("@") }
    var category by remember { mutableStateOf("Global") }
    var description by remember { mutableStateOf("") }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Launch Global Radio Channel",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close create radio sheet")
                }
            }
            Text(
                text = "Broadcast live voice shows, music, talk, and knowledge to billions of BINTAFACE listeners.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(80) },
                label = { Text("Radio Station Name *") },
                placeholder = { Text("e.g. Lagos Power FM / Global Knowledge Radio") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("radio_title_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = frequency,
                    onValueChange = { frequency = it.take(24) },
                    label = { Text("Frequency / Dial") },
                    placeholder = { Text("104.7 FM") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("radio_frequency_input")
                )
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it.take(40) },
                    label = { Text("Station @handle") },
                    placeholder = { Text("@powerfm") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("radio_handle_input")
                )
            }

            Text("Station Category", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RADIO_CATEGORIES.forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat) }
                    )
                }
            }

            Text("Studio Theme", style = MaterialTheme.typography.labelLarge)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RADIO_STAGE_COVERS.forEach { (key, label) ->
                    FilterChip(
                        selected = coverKey == key,
                        onClick = { coverKey = key },
                        label = { Text(label) }
                    )
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(400) },
                label = { Text("Station Bio & Show Schedule") },
                placeholder = { Text("Tell billions of listeners what your radio channel broadcasts…") },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("radio_description_input")
            )

            Button(
                onClick = {
                    val autoHandle = if (handle.trim().length <= 1) {
                        "@" + title.trim().lowercase().replace(Regex("[^a-z0-9]"), "").take(20)
                    } else handle
                    onCreate(title, frequency, autoHandle, category, description, coverKey)
                },
                enabled = title.trim().length >= 2 && !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = RadioAccentColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("launch_radio_station_button")
            ) {
                Icon(Icons.Filled.Podcasts, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isSaving) "Launching Station…" else "Launch Radio Channel On Air")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RadioStationStudioSheet(
    currentUserId: String,
    station: RadioChannel,
    broadcastsState: UiState<List<RadioBroadcast>>,
    isSubscribed: Boolean,
    sparksBalance: Int,
    isSaving: Boolean,
    onToggleLiveStatus: () -> Unit,
    onToggleSubscribe: () -> Unit,
    onSendSparkGift: () -> Unit,
    onPublishBroadcast: (
        title: String,
        text: String,
        audioBase64: String?,
        audioDurationSec: Long,
        onSent: () -> Unit
    ) -> Unit,
    onShowNotice: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val voiceController = remember(context) { VoiceAudioController(context, scope) }

    val isRecording by voiceController.isRecording.collectAsStateWithLifecycle()
    val recordingSec by voiceController.recordingSeconds.collectAsStateWithLifecycle()
    val playingBroadcastId by voiceController.playingMessageId.collectAsStateWithLifecycle()
    val playbackProgress by voiceController.playbackProgress.collectAsStateWithLifecycle()

    DisposableEffect(voiceController) {
        onDispose { voiceController.release() }
    }

    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastNotes by remember { mutableStateOf("") }
    var pendingRecording by remember { mutableStateOf<VoiceNoteRecording?>(null) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voiceController.startRecording(onShowNotice)
        } else {
            onShowNotice("Microphone permission is needed to record a live radio voice broadcast.")
        }
    }

    val isOwner = station.ownerId == currentUserId
    val isLive = station.status == "live"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Studio Header
            Surface(
                shape = MaterialTheme.shapes.large,
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(RadioAccentColor, VipMoonDiamondColor)
                        )
                    )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isLive) RadioBroadcastRed else Color.Black.copy(alpha = 0.35f)
                                ) {
                                    Text(
                                        text = if (isLive) "● ON AIR" else "OFF AIR",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                                    )
                                }
                                Text(
                                    text = "${station.frequency} • ${station.category}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = station.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Hosted by ${station.ownerName} (${station.handle}) • ${formatListenerCount(station.listenersCount)} tuned in • ${formatListenerCount(station.subscribersCount)} subscribers",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Leave radio station", tint = Color.White)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isOwner) {
                            Button(
                                onClick = onToggleLiveStatus,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLive) RadioBroadcastRed else MatchEmerald
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("toggle_radio_on_air_button")
                            ) {
                                Text(if (isLive) "Go Off Air" else "Go On Air")
                            }
                        } else {
                            Button(
                                onClick = onToggleSubscribe,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = RadioAccentColor
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                            ) {
                                Text(if (isSubscribed) "Subscribed ✓" else "Subscribe", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onSendSparkGift,
                                colors = ButtonDefaults.buttonColors(containerColor = SuperlikeGold),
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("radio_send_spark_button")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Spark $1 ($sparksBalance)")
                            }
                        }
                    }
                }
            }

            if (station.description.isNotBlank()) {
                Text(
                    text = station.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Host Studio Broadcast Composer (supports real Mic AAC audio + Show notes)
            if (isOwner) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, RadioAccentColor.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Broadcaster Studio Console",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = broadcastTitle,
                            onValueChange = { broadcastTitle = it.take(120) },
                            label = { Text("Episode / Segment Title *") },
                            placeholder = { Text("e.g. Morning Knowledge Hour") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("radio_broadcast_title_input")
                        )
                        OutlinedTextField(
                            value = broadcastNotes,
                            onValueChange = { broadcastNotes = it.take(1000) },
                            label = { Text("On-Air Message / Show Notes *") },
                            placeholder = { Text("Share what listeners are hearing live…") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("radio_broadcast_notes_input")
                        )

                        // Audio Mic Recorder Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isRecording) {
                                Button(
                                    onClick = {
                                        val rec = voiceController.stopAndEncodeRecording(onShowNotice)
                                        if (rec != null) {
                                            pendingRecording = rec
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = RadioBroadcastRed),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Stop Mic (${formatCallOrVoiceDuration(recordingSec)})")
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            voiceController.startRecording(onShowNotice)
                                        } else {
                                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (pendingRecording == null) "Record Audio Broadcast" else "Re-record Audio")
                                }
                            }
                        }

                        pendingRecording?.let { rec ->
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = RadioAccentColor.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, RadioAccentColor.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            voiceController.togglePlayback("preview_rec", rec.base64, onShowNotice)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (playingBroadcastId == "preview_rec") Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = "Preview broadcast audio",
                                            tint = RadioAccentColor
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Audio Broadcast Ready (${formatCallOrVoiceDuration(rec.durationSec)})",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = RadioAccentColor
                                        )
                                        if (playingBroadcastId == "preview_rec") {
                                            LinearProgressIndicator(
                                                progress = { playbackProgress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 4.dp)
                                            )
                                        }
                                    }
                                    IconButton(onClick = { pendingRecording = null }) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Remove audio attachment")
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val rec = pendingRecording
                                onPublishBroadcast(
                                    broadcastTitle,
                                    broadcastNotes,
                                    rec?.base64,
                                    rec?.durationSec ?: 0L
                                ) {
                                    broadcastTitle = ""
                                    broadcastNotes = ""
                                    pendingRecording = null
                                }
                            },
                            enabled = broadcastTitle.trim().length >= 2 && broadcastNotes.isNotBlank() && !isSaving && !isRecording,
                            colors = ButtonDefaults.buttonColors(containerColor = RadioAccentColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("publish_radio_broadcast_button")
                        ) {
                            Icon(Icons.Filled.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (isSaving) "Broadcasting…" else "Send Live Broadcast to Listeners")
                        }
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "Live Broadcasts & Radio Episodes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            when (broadcastsState) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    EmptyStateCard(
                        icon = Icons.Filled.Radio,
                        title = "Couldn't load broadcasts",
                        subtitle = broadcastsState.message
                    )
                }
                is UiState.Success -> {
                    if (broadcastsState.data.isEmpty()) {
                        EmptyStateCard(
                            icon = Icons.Filled.Podcasts,
                            title = "Waiting for Broadcast",
                            subtitle = if (isOwner) "Use the Studio Console above to record an audio show or send your first broadcast!" else "Stay tuned! ${station.title} will air its next show shortly."
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            broadcastsState.data.forEach { episode ->
                                val isPlayingThis = playingBroadcastId == episode.id
                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    border = BorderStroke(
                                        width = if (isPlayingThis) 1.5.dp else 1.dp,
                                        color = if (isPlayingThis) RadioAccentColor else MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = episode.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = formatRelativeTimestamp(episode.createdAt),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = episode.text,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        if (!episode.audioBase64.isNullOrBlank()) {
                                            Surface(
                                                shape = MaterialTheme.shapes.medium,
                                                color = RadioAccentColor.copy(alpha = 0.12f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    IconButton(
                                                        onClick = {
                                                            voiceController.togglePlayback(
                                                                episode.id,
                                                                episode.audioBase64,
                                                                onShowNotice
                                                            )
                                                        },
                                                        modifier = Modifier.minimumInteractiveComponentSize()
                                                    ) {
                                                        Icon(
                                                            imageVector = if (isPlayingThis) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                            contentDescription = if (isPlayingThis) "Pause radio audio" else "Play radio audio",
                                                            tint = RadioAccentColor
                                                        )
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = if (isPlayingThis) "Playing On-Air Audio…" else "Listen to Broadcast Audio (${formatCallOrVoiceDuration(episode.audioDurationSec)})",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            color = RadioAccentColor,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        LinearProgressIndicator(
                                                            progress = { if (isPlayingThis) playbackProgress else 0f },
                                                            color = RadioAccentColor,
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(top = 4.dp)
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
            }
        }
    }
}
