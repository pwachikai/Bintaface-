package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.PublicProfile
import com.example.data.model.SwipeRecord
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.IntentBadge
import com.example.ui.components.MomentMediaView
import com.example.ui.components.PlusBadge
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.ProfileDetailBottomSheet
import com.example.ui.components.SpotlightBadge
import com.example.ui.components.VipLevelBadge
import com.example.ui.theme.MatchEmerald
import com.example.ui.theme.PassSlate
import com.example.ui.theme.SuperlikeGold
import com.example.ui.util.presetDrawableForKey
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DiscoverScreen(
    myProfile: PublicProfile?,
    deckState: UiState<List<PublicProfile>>,
    incomingLikes: List<SwipeRecord>,
    selectedFilter: String,
    newMatchPopup: Pair<PublicProfile, String>?,
    isPlusMember: Boolean,
    dailyLikesRemaining: Int,
    superlikesBalance: Int,
    onSelectFilter: (String) -> Unit,
    onSwipe: (PublicProfile, String) -> Unit,
    onRespondToLike: (SwipeRecord, Boolean) -> Unit,
    onDismissMatchPopup: () -> Unit,
    onOpenMatchChat: (String) -> Unit,
    onEditOwnProfile: () -> Unit,
    onOpenPaywall: () -> Unit,
    applyStatusBarPadding: Boolean = true,
    modifier: Modifier = Modifier
) {
    var subTab by rememberSaveable { mutableIntStateOf(0) } // 0 = Swipe Deck, 1 = Likes You
    var inspectedProfile by remember { mutableStateOf<PublicProfile?>(null) }

    BackHandler(enabled = subTab != 0 && inspectedProfile == null && newMatchPopup == null) {
        subTab = 0
    }

    val rootModifier = if (applyStatusBarPadding) {
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    } else {
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    }

    Box(
        modifier = rootModifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 580.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Discover",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = subTab == 0,
                        onClick = { subTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("discover_deck_tab")
                    ) {
                        Text("Cards")
                    }
                    SegmentedButton(
                        selected = subTab == 1,
                        onClick = { subTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("discover_likes_tab")
                    ) {
                        val badge = if (incomingLikes.isNotEmpty()) " (${incomingLikes.size})" else ""
                        Text("Likes You$badge")
                    }
                }
            }

            if (subTab == 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("all" to "Everyone", "dating" to "Dating", "friends" to "Friends").forEach { (key, label) ->
                            FilterChip(
                                selected = selectedFilter == key,
                                onClick = { onSelectFilter(key) },
                                label = { Text(label) }
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clickable { onOpenPaywall() }
                            .testTag("discover_quota_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlusMember) Icons.Filled.WorkspacePremium else Icons.Filled.Star,
                                contentDescription = "Likes and Superlikes status",
                                tint = if (isPlusMember) MaterialTheme.colorScheme.primary else SuperlikeGold,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isPlusMember) {
                                    "Plus • ${superlikesBalance}★"
                                } else {
                                    "${dailyLikesRemaining}♥ • ${superlikesBalance}★"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                when (deckState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is UiState.Error -> {
                        EmptyStateCard(
                            icon = Icons.Filled.Favorite,
                            title = "Couldn't load profiles",
                            subtitle = deckState.message
                        )
                    }
                    is UiState.Success -> {
                        val topProfile = deckState.data.firstOrNull()
                        if (topProfile == null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                EmptyStateCard(
                                    icon = Icons.Filled.Favorite,
                                    title = "You're all caught up",
                                    subtitle = "No new profiles match your current filter right now. Check the Feed or preview how your own dating card looks!",
                                    actionLabel = if (myProfile != null) "Preview My Card" else "Complete My Profile",
                                    onAction = {
                                        if (myProfile != null) inspectedProfile = myProfile
                                        else onEditOwnProfile()
                                    }
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                key(topProfile.userId) {
                                    SwipeableProfileCard(
                                        profile = topProfile,
                                        onSwiped = { decision -> onSwipe(topProfile, decision) },
                                        onInspect = { inspectedProfile = topProfile }
                                    )
                                }
                            }

                            // Tactile Action Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 4.dp,
                                    border = BorderStroke(1.dp, PassSlate.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clickable { onSwipe(topProfile, "pass") }
                                        .testTag("pass_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = "Pass",
                                            tint = PassSlate,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surface,
                                    shadowElevation = 4.dp,
                                    border = BorderStroke(1.dp, SuperlikeGold.copy(alpha = 0.35f)),
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clickable { onSwipe(topProfile, "superlike") }
                                        .testTag("superlike_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = "Superlike",
                                            tint = SuperlikeGold,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }

                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    shadowElevation = 6.dp,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clickable { onSwipe(topProfile, "like") }
                                        .testTag("like_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Favorite,
                                            contentDescription = "Like",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Likes You Sub-Tab
                if (incomingLikes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateCard(
                            icon = Icons.Filled.Star,
                            title = "No pending likes yet",
                            subtitle = "When someone swipes right or superlikes your card, they'll appear right here so you can match back instantly."
                        )
                    }
                } else {
                    if (!isPlusMember) {
                        Surface(
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.WorkspacePremium,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${incomingLikes.size} ${if (incomingLikes.size == 1) "person likes" else "people like"} you",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Upgrade to Bintaface+ to reveal their profiles and match instantly.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    )
                                }
                                Button(
                                    onClick = onOpenPaywall,
                                    modifier = Modifier.testTag("unlock_likes_you_button")
                                ) {
                                    Text("Unlock")
                                }
                            }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(incomingLikes, key = { it.fromUserId }) { like ->
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        ProfileAvatar(
                                            name = like.fromName,
                                            avatarKey = like.fromAvatarKey,
                                            size = 54.dp,
                                            modifier = if (!isPlusMember) Modifier.blur(12.dp) else Modifier
                                        )
                                        if (!isPlusMember) {
                                            Icon(
                                                imageVector = Icons.Filled.Lock,
                                                contentDescription = "Locked admirer preview",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.45f))
                                                    .padding(4.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isPlusMember) like.fromName else "BINTAFACE Admirer",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = if (like.decision == "superlike") "Superliked your profile!" else "Liked your profile",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (like.decision == "superlike") SuperlikeGold else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (isPlusMember) {
                                        OutlinedButton(
                                            onClick = { onRespondToLike(like, false) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Text("Pass")
                                        }
                                        Button(
                                            onClick = { onRespondToLike(like, true) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Text("Match")
                                        }
                                    } else {
                                        OutlinedButton(
                                            onClick = onOpenPaywall,
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Text("Reveal")
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

    // Celebratory Mutual Match Dialog
    newMatchPopup?.let { (partner, matchId) ->
        AlertDialog(
            onDismissRequest = onDismissMatchPopup,
            icon = {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "It's a Match!",
                    style = MaterialTheme.typography.headlineLarge
                )
            },
            text = {
                Text(
                    text = "You and ${partner.displayName} both sparked interest. Start a conversation while the vibe is fresh!",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismissMatchPopup()
                        onOpenMatchChat(matchId)
                    }
                ) {
                    Text("Send a Message")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissMatchPopup) {
                    Text("Keep Exploring")
                }
            }
        )
    }

    inspectedProfile?.let { profile ->
        ProfileDetailBottomSheet(
            profile = profile,
            onDismiss = { inspectedProfile = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwipeableProfileCard(
    profile: PublicProfile,
    onSwiped: (String) -> Unit,
    onInspect: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    val swipeThreshold = 260f

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
            .rotate((offsetX.value / 35f).coerceIn(-14f, 14f))
            .pointerInput(profile.userId) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch {
                            when {
                                offsetX.value > swipeThreshold -> {
                                    offsetX.animateTo(1200f, spring())
                                    onSwiped("like")
                                }
                                offsetX.value < -swipeThreshold -> {
                                    offsetX.animateTo(-1200f, spring())
                                    onSwiped("pass")
                                }
                                else -> {
                                    launch { offsetX.animateTo(0f, spring()) }
                                    launch { offsetY.animateTo(0f, spring()) }
                                }
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo(offsetY.value + dragAmount.y * 0.35f)
                        }
                    }
                )
            }
            .testTag("discover_card_${profile.userId}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val hasMedia = !profile.photoBase64.isNullOrBlank() || presetDrawableForKey(profile.avatarKey) != null
            if (hasMedia) {
                MomentMediaView(
                    imageKey = profile.avatarKey,
                    photoBase64 = profile.photoBase64,
                    contentDescription = "${profile.displayName} profile card",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        )
                )
            }

            // Editorial Dark Scrim for high legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.08f),
                                Color.Black.copy(alpha = 0.18f),
                                Color.Black.copy(alpha = 0.84f)
                            )
                        )
                    )
            )

            // Drag Stamps
            val likeAlpha = (offsetX.value / swipeThreshold).coerceIn(0f, 1f)
            val passAlpha = (-offsetX.value / swipeThreshold).coerceIn(0f, 1f)
            if (likeAlpha > 0.05f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(24.dp)
                        .alpha(likeAlpha)
                        .border(3.dp, MatchEmerald, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "LIKE",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MatchEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (passAlpha > 0.05f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(24.dp)
                        .alpha(passAlpha)
                        .border(3.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "NOPE",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Top Inspect Button
            IconButton(
                onClick = onInspect,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(14.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "View full profile",
                    tint = Color.White
                )
            }

            // Profile Details Overlay
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IntentBadge(lookingFor = profile.lookingFor)
                    VipLevelBadge(vipLevel = profile.vipLevel)
                    if (profile.isBoosted) {
                        SpotlightBadge()
                    }
                    if (profile.isPlusMember) {
                        PlusBadge()
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "${profile.displayName}, ${profile.age}",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White
                    )
                    if (profile.city.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = profile.city,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.92f)
                            )
                        }
                    }
                }

                if (profile.bio.isNotBlank()) {
                    Text(
                        text = profile.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.92f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (profile.promptAnswer.isNotBlank()) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = Color.White.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = profile.promptQuestion,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Text(
                                text = profile.promptAnswer,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (profile.interests.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        profile.interests.take(5).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.Black.copy(alpha = 0.45f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
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
