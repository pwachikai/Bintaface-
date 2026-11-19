package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DEFAULT_ADMOB_BANNER_UNIT_ID
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.formatUsdCents
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GoogleAdBannerCard
import com.example.ui.components.MomentMediaView
import com.example.ui.components.PlusBadge
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.VipLevelBadge
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.theme.LocalBintafaceAccents
import com.example.ui.theme.MatchEmerald
import com.example.ui.theme.SuperlikeGold
import com.example.ui.util.encodeUriToCompactBase64
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val WATCH_BACKDROPS = listOf(
    "studio" to "Studio Stage",
    "sunset" to "Golden Hour",
    "coffee" to "Cafe Chill",
    "trail" to "Outdoor",
    "hero" to "Neon Lounge"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WatchScreen(
    currentUserId: String,
    reelsState: UiState<List<Post>>,
    videosState: UiState<List<Post>>,
    likedPostIds: Set<String>,
    repostedPostIds: Set<String>,
    followedUserIds: Set<String>,
    activeCommentPostId: String?,
    activeCommentsState: UiState<List<PostComment>>,
    sparksBalance: Int,
    isSaving: Boolean,
    vipLevelForUser: (String) -> String = { "none" },
    onToggleLike: (String, Boolean) -> Unit,
    onRepost: (Post) -> Unit,
    onToggleFollow: (String, String) -> Unit,
    onOpenComments: (String) -> Unit,
    onCloseComments: () -> Unit,
    onAddComment: (String, String, () -> Unit) -> Unit,
    onSendSparkGift: (Post) -> Unit,
    onIncrementView: (String) -> Unit,
    onPublishWatchPost: (
        mediaType: String,
        title: String,
        content: String,
        durationLabel: String,
        audioTrack: String,
        imageKey: String?,
        photoBase64: String?,
        onDone: () -> Unit
    ) -> Unit,
    admobBannerUnitId: String = DEFAULT_ADMOB_BANNER_UNIT_ID,
    onGoogleAdPaid: (
        adFormat: String,
        adUnitId: String,
        valueMicros: Long,
        currencyCode: String,
        creatorId: String,
        creatorName: String,
        postId: String
    ) -> Unit = { _, _, _, _, _, _, _ -> },
    onWatchRewardedAd: (creatorId: String, creatorName: String, postId: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 = Reels (TikTok), 1 = Videos (YouTube)
    var showCreateSheet by remember { mutableStateOf(false) }
    var activeVideoPlayer by remember { mutableStateOf<Post?>(null) }

    BackHandler(enabled = mode != 0 && activeVideoPlayer == null && !showCreateSheet && activeCommentPostId == null) {
        mode = 0
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_watch_fab")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Upload Reel or Video")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Top Mode Bar: Reels (TikTok) vs Videos (YouTube)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Watch",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "$sparksBalance ✨ Sparks • Creators earn 80%",
                        style = MaterialTheme.typography.labelSmall,
                        color = SuperlikeGold
                    )
                }
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = mode == 0,
                        onClick = { mode = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("watch_reels_tab")
                    ) {
                        Text("Reels")
                    }
                    SegmentedButton(
                        selected = mode == 1,
                        onClick = { mode = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("watch_videos_tab")
                    ) {
                        Text("Videos")
                    }
                }
            }

            AnimatedContent(
                targetState = mode,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
                label = "watchModeContent",
                modifier = Modifier.weight(1f)
            ) { currentMode ->
                if (currentMode == 0) {
                    ReelsVerticalFeed(
                        currentUserId = currentUserId,
                        reelsState = reelsState,
                        likedPostIds = likedPostIds,
                        repostedPostIds = repostedPostIds,
                        followedUserIds = followedUserIds,
                        vipLevelForUser = vipLevelForUser,
                        onToggleLike = onToggleLike,
                        onRepost = onRepost,
                        onToggleFollow = onToggleFollow,
                        onOpenComments = onOpenComments,
                        onSendSparkGift = onSendSparkGift,
                        onWatchRewardedAd = onWatchRewardedAd,
                        onIncrementView = onIncrementView,
                        onOpenComposer = { showCreateSheet = true },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    VideosFeed(
                        currentUserId = currentUserId,
                        videosState = videosState,
                        followedUserIds = followedUserIds,
                        admobBannerUnitId = admobBannerUnitId,
                        onGoogleAdPaid = onGoogleAdPaid,
                        onToggleFollow = onToggleFollow,
                        onSendSparkGift = onSendSparkGift,
                        onOpenVideo = { video ->
                            onIncrementView(video.id)
                            activeVideoPlayer = video
                        },
                        onOpenComposer = { showCreateSheet = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    if (showCreateSheet) {
        PublishWatchSheet(
            initialMediaType = if (mode == 0) "reel" else "video",
            isSaving = isSaving,
            onDismiss = { showCreateSheet = false },
            onPublish = { mediaType, title, content, duration, audio, imageKey, photoBase64 ->
                onPublishWatchPost(mediaType, title, content, duration, audio, imageKey, photoBase64) {
                    showCreateSheet = false
                    mode = if (mediaType == "reel") 0 else 1
                }
            }
        )
    }

    activeVideoPlayer?.let { selected ->
        val latest = (videosState as? UiState.Success)?.data?.firstOrNull { it.id == selected.id } ?: selected
        VideoPlayerSheet(
            video = latest,
            isOwnVideo = latest.authorId == currentUserId,
            isLiked = latest.id in likedPostIds,
            isFollowing = latest.authorId in followedUserIds,
            onToggleLike = { onToggleLike(latest.id, latest.id in likedPostIds) },
            onToggleFollow = { onToggleFollow(latest.authorId, latest.authorName) },
            onOpenComments = { onOpenComments(latest.id) },
            onSendSpark = { onSendSparkGift(latest) },
            onWatchRewardedAd = { onWatchRewardedAd(latest.authorId, latest.authorName, latest.id) },
            onDismiss = { activeVideoPlayer = null }
        )
    }

    if (activeCommentPostId != null) {
        CommentsSheet(
            commentsState = activeCommentsState,
            onDismiss = onCloseComments,
            onSendComment = { text, clear ->
                onAddComment(activeCommentPostId, text, clear)
            }
        )
    }
}

@Composable
private fun ReelsVerticalFeed(
    currentUserId: String,
    reelsState: UiState<List<Post>>,
    likedPostIds: Set<String>,
    repostedPostIds: Set<String>,
    followedUserIds: Set<String>,
    vipLevelForUser: (String) -> String = { "none" },
    onToggleLike: (String, Boolean) -> Unit,
    onRepost: (Post) -> Unit,
    onToggleFollow: (String, String) -> Unit,
    onOpenComments: (String) -> Unit,
    onSendSparkGift: (Post) -> Unit,
    onWatchRewardedAd: (String, String, String) -> Unit,
    onIncrementView: (String) -> Unit,
    onOpenComposer: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (reelsState) {
        is UiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                EmptyStateCard(
                    icon = Icons.Filled.VideoLibrary,
                    title = "Couldn't load Reels",
                    subtitle = reelsState.message
                )
            }
        }
        is UiState.Success -> {
            val reels = reelsState.data
            if (reels.isEmpty()) {
                Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    EmptyStateCard(
                        icon = Icons.Filled.PlayCircleFilled,
                        title = "No Short Reels Yet",
                        subtitle = "Drop the first TikTok-style short Reel on BINTAFACE! Viewers can like, comment, repost & send $1 Spark Gifts (80% yours).",
                        actionLabel = "Create First Reel",
                        onAction = onOpenComposer
                    )
                }
            } else {
                val pagerState = rememberPagerState(pageCount = { reels.size })
                LaunchedEffect(pagerState.currentPage, reels.size) {
                    reels.getOrNull(pagerState.currentPage)?.let { reel ->
                        onIncrementView(reel.id)
                    }
                }
                VerticalPager(
                    state = pagerState,
                    modifier = modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .testTag("reels_vertical_pager")
                ) { page ->
                    val reel = reels[page]
                    ReelStageCard(
                        reel = reel,
                        authorVipLevel = vipLevelForUser(reel.authorId),
                        isActive = pagerState.currentPage == page,
                        isOwn = reel.authorId == currentUserId,
                        isLiked = reel.id in likedPostIds,
                        isReposted = reel.id in repostedPostIds,
                        isFollowing = reel.authorId in followedUserIds,
                        onToggleLike = { onToggleLike(reel.id, reel.id in likedPostIds) },
                        onRepost = { onRepost(reel) },
                        onToggleFollow = { onToggleFollow(reel.authorId, reel.authorName) },
                        onOpenComments = { onOpenComments(reel.id) },
                        onSendSpark = { onSendSparkGift(reel) },
                        onWatchAd = { onWatchRewardedAd(reel.authorId, reel.authorName, reel.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReelStageCard(
    reel: Post,
    authorVipLevel: String = "none",
    isActive: Boolean,
    isOwn: Boolean,
    isLiked: Boolean,
    isReposted: Boolean,
    isFollowing: Boolean,
    onToggleLike: () -> Unit,
    onRepost: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenComments: () -> Unit,
    onSendSpark: () -> Unit,
    onWatchAd: () -> Unit
) {
    var isPlaying by remember(reel.id) { mutableStateOf(true) }
    var progress by remember(reel.id) { mutableFloatStateOf(0.15f) }

    LaunchedEffect(isActive, isPlaying) {
        while (isActive && isPlaying) {
            delay(180L)
            progress = if (progress >= 1f) 0f else progress + 0.025f
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "reelPulse")
    val eqHeight by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520, easing = LinearEasing), RepeatMode.Reverse),
        label = "eqPulse"
    )

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = Color(0xFF141122),
        modifier = Modifier
            .fillMaxSize()
            .clickable { isPlaying = !isPlaying }
            .testTag("reel_card_${reel.id}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            MomentMediaView(
                imageKey = reel.imageKey ?: "studio",
                photoBase64 = reel.postPhotoBase64,
                contentDescription = reel.title.ifBlank { "Reel video" },
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.86f)
                            )
                        )
                    )
            )

            // Top Badges: REEL + Views + Duration
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size((14f * if (isPlaying) eqHeight + 0.3f else 0.9f).dp)
                        )
                        Text(
                            text = "REEL • ${reel.durationLabel.ifBlank { "0:30" }}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${reel.viewsCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            if (!isPlaying) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f),
                    modifier = Modifier
                        .size(72.dp)
                        .align(Alignment.Center)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Resume reel",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }

            // Right Action Rail (TikTok style)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ReelRailButton(
                    icon = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    label = "${reel.likesCount}",
                    tint = if (isLiked) MaterialTheme.colorScheme.primary else Color.White,
                    onClick = onToggleLike
                )
                ReelRailButton(
                    icon = Icons.Filled.ChatBubbleOutline,
                    label = "${reel.commentsCount}",
                    tint = Color.White,
                    onClick = onOpenComments
                )
                ReelRailButton(
                    icon = Icons.Filled.Repeat,
                    label = "${reel.repostsCount}",
                    tint = if (isReposted) MatchEmerald else Color.White,
                    onClick = onRepost
                )
                if (!isOwn || reel.sparksCount > 0L) {
                    ReelRailButton(
                        icon = Icons.Filled.AutoAwesome,
                        label = if (reel.sparksCount > 0L) "${reel.sparksCount}✨" else "Tip $1",
                        tint = SuperlikeGold,
                        onClick = { if (!isOwn) onSendSpark() }
                    )
                }
                if (!isOwn) {
                    ReelRailButton(
                        icon = Icons.Filled.AdsClick,
                        label = "Ad 30%",
                        tint = Color.White,
                        onClick = onWatchAd
                    )
                }
            }

            // Bottom Creator Info & Scrubber
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.78f)
                    .padding(start = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ProfileAvatar(
                        name = reel.authorName,
                        avatarKey = reel.authorAvatarKey,
                        photoBase64 = reel.authorPhotoBase64,
                        size = 40.dp,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    Text(
                        text = reel.authorName,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    VipLevelBadge(
                        vipLevel = authorVipLevel,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                    if (reel.authorIsPlus) {
                        PlusBadge(modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    if (!isOwn) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isFollowing) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onToggleFollow)
                        ) {
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                if (reel.title.isNotBlank()) {
                    Text(
                        text = reel.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                Text(
                    text = reel.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = reel.audioTrack.ifBlank { "Original Sound • ${reel.authorName}" },
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
private fun ReelRailButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.45f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier
                    .padding(10.dp)
                    .size(24.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun VideosFeed(
    currentUserId: String,
    videosState: UiState<List<Post>>,
    followedUserIds: Set<String>,
    admobBannerUnitId: String,
    onGoogleAdPaid: (String, String, Long, String, String, String, String) -> Unit,
    onToggleFollow: (String, String) -> Unit,
    onSendSparkGift: (Post) -> Unit,
    onOpenVideo: (Post) -> Unit,
    onOpenComposer: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (videosState) {
        is UiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                EmptyStateCard(
                    icon = Icons.Filled.VideoLibrary,
                    title = "Couldn't load Videos",
                    subtitle = videosState.message
                )
            }
        }
        is UiState.Success -> {
            val videos = videosState.data
            if (videos.isEmpty()) {
                Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    EmptyStateCard(
                        icon = Icons.Filled.VideoLibrary,
                        title = "No Long Videos Yet",
                        subtitle = "Upload YouTube-style widescreen vlogs, tutorials & music sessions. Supporters tip $1 Sparks with an 80/20 creator/admin split.",
                        actionLabel = "Upload Video",
                        onAction = onOpenComposer
                    )
                }
            } else {
                Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 640.dp)
                            .fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 92.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            val featured = videos.firstOrNull { it.authorId != currentUserId }
                            GoogleAdBannerCard(
                                adUnitId = admobBannerUnitId,
                                creatorId = featured?.authorId.orEmpty(),
                                creatorName = featured?.authorName.orEmpty(),
                                postId = featured?.id ?: "watch_banner",
                                onPaidMicros = { micros, currency, unitId, cId, cName, pId ->
                                    onGoogleAdPaid("banner", unitId, micros, currency, cId, cName, pId)
                                }
                            )
                        }
                        items(videos, key = { it.id }) { video ->
                            val isOwn = video.authorId == currentUserId
                            val isFollowing = video.authorId in followedUserIds
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenVideo(video) }
                                    .testTag("video_card_${video.id}")
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .background(Color(0xFF141122))
                                    ) {
                                        MomentMediaView(
                                            imageKey = video.imageKey ?: "studio",
                                            photoBase64 = video.postPhotoBase64,
                                            contentDescription = video.title.ifBlank { "Video thumbnail" },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.Black.copy(alpha = 0.6f),
                                            modifier = Modifier.align(Alignment.Center)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.PlayArrow,
                                                contentDescription = "Play video",
                                                tint = Color.White,
                                                modifier = Modifier
                                                    .padding(14.dp)
                                                    .size(32.dp)
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color.Black.copy(alpha = 0.78f),
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(10.dp)
                                        ) {
                                            Text(
                                                text = video.durationLabel.ifBlank { "8:24" },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.Top,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            ProfileAvatar(
                                                name = video.authorName,
                                                avatarKey = video.authorAvatarKey,
                                                photoBase64 = video.authorPhotoBase64,
                                                size = 40.dp
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = video.title.ifBlank { video.content.take(60) },
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${video.authorName} • ${video.viewsCount} views • ${formatRelativeTimestamp(video.createdAt)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (!isOwn) {
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = if (isFollowing) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .clickable { onToggleFollow(video.authorId, video.authorName) }
                                                ) {
                                                    Text(
                                                        text = if (isFollowing) "Subscribed" else "Subscribe",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isFollowing) MaterialTheme.colorScheme.onSurface else Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Earned ${formatUsdCents(video.sparksEarningsCents)} (${video.sparksCount} Sparks)",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = SuperlikeGold
                                            )
                                            if (!isOwn) {
                                                OutlinedButton(
                                                    onClick = { onSendSparkGift(video) },
                                                    modifier = Modifier.minimumInteractiveComponentSize()
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.AutoAwesome,
                                                        contentDescription = null,
                                                        tint = SuperlikeGold,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Spark $1")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoPlayerSheet(
    video: Post,
    isOwnVideo: Boolean,
    isLiked: Boolean,
    isFollowing: Boolean,
    onToggleLike: () -> Unit,
    onToggleFollow: () -> Unit,
    onOpenComments: () -> Unit,
    onSendSpark: () -> Unit,
    onWatchRewardedAd: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isPlaying by remember { mutableStateOf(true) }
    var position by remember { mutableFloatStateOf(0.1f) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(250L)
            position = if (position >= 1f) 0f else position + 0.01f
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(215.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(Color(0xFF141122))
                    .clickable { isPlaying = !isPlaying }
            ) {
                MomentMediaView(
                    imageKey = video.imageKey ?: "studio",
                    photoBase64 = video.postPhotoBase64,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f))
                )
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }

            Slider(
                value = position,
                onValueChange = { position = it },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = video.title.ifBlank { "BINTAFACE Video" },
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "${video.viewsCount} views • ${formatRelativeTimestamp(video.createdAt)} • ${video.durationLabel.ifBlank { "8:24" }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileAvatar(
                        name = video.authorName,
                        avatarKey = video.authorAvatarKey,
                        photoBase64 = video.authorPhotoBase64,
                        size = 42.dp
                    )
                    Column {
                        Text(video.authorName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${video.sparksCount} Sparks (${formatUsdCents(video.sparksEarningsCents)})",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuperlikeGold
                        )
                    }
                }
                if (!isOwnVideo) {
                    Button(onClick = onToggleFollow) {
                        Text(if (isFollowing) "Subscribed" else "Subscribe")
                    }
                }
            }

            Text(
                text = video.content,
                style = MaterialTheme.typography.bodyMedium
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleLike,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text("${video.likesCount}")
                }
                OutlinedButton(
                    onClick = onOpenComments,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("${video.commentsCount}")
                }
                if (!isOwnVideo) {
                    Button(
                        onClick = onSendSpark,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Spark $1")
                    }
                }
            }

            if (!isOwnVideo) {
                OutlinedButton(
                    onClick = onWatchRewardedAd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("watch_rewarded_ad_button")
                ) {
                    Icon(Icons.Filled.AdsClick, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Watch Google Ad • Support ${video.authorName} (30% / 70%)")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PublishWatchSheet(
    initialMediaType: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onPublish: (
        mediaType: String,
        title: String,
        content: String,
        durationLabel: String,
        audioTrack: String,
        imageKey: String?,
        photoBase64: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var mediaType by remember { mutableStateOf(initialMediaType) }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var duration by remember(mediaType) { mutableStateOf(if (mediaType == "reel") "0:30" else "8:24") }
    var audioTrack by remember { mutableStateOf("Original Sound") }
    var selectedBackdrop by remember { mutableStateOf<String?>("studio") }
    var pickedPhotoBase64 by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isProcessing = true
                val encoded = encodeUriToCompactBase64(context, uri)
                if (encoded != null) {
                    pickedPhotoBase64 = encoded
                    selectedBackdrop = null
                }
                isProcessing = false
            }
        }
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
                .verticalScroll(rememberScrollState())
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
                    text = if (mediaType == "reel") "Create Short Reel" else "Upload Video",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mediaType == "reel",
                    onClick = { mediaType = "reel" },
                    label = { Text("Short Reel (TikTok)") }
                )
                FilterChip(
                    selected = mediaType == "video",
                    onClick = { mediaType = "video" },
                    label = { Text("Long Video (YouTube)") }
                )
            }

            OutlinedTextField(
                value = title,
                onValueChange = { if (it.length <= 120) title = it },
                label = { Text("Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("watch_title_input")
            )

            OutlinedTextField(
                value = content,
                onValueChange = { if (it.length <= 1000) content = it },
                label = { Text("Description / Caption") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("watch_description_input")
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = duration,
                    onValueChange = { if (it.length <= 20) duration = it },
                    label = { Text("Duration") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                if (mediaType == "reel") {
                    OutlinedTextField(
                        value = audioTrack,
                        onValueChange = { if (it.length <= 80) audioTrack = it },
                        label = { Text("Audio Track") },
                        singleLine = true,
                        modifier = Modifier.weight(1.6f)
                    )
                }
            }

            Text(
                text = "Cover Thumbnail",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (pickedPhotoBase64 != null) "Cover Ready" else "Pick Photo")
                }
                WATCH_BACKDROPS.forEach { (key, label) ->
                    FilterChip(
                        selected = pickedPhotoBase64 == null && selectedBackdrop == key,
                        onClick = {
                            pickedPhotoBase64 = null
                            selectedBackdrop = key
                        },
                        label = { Text(label) }
                    )
                }
            }

            Button(
                onClick = {
                    onPublish(
                        mediaType,
                        title.trim(),
                        content.trim(),
                        duration.trim(),
                        if (mediaType == "reel") audioTrack.trim() else "",
                        selectedBackdrop,
                        pickedPhotoBase64
                    )
                },
                enabled = !isSaving && !isProcessing && title.trim().isNotEmpty() && content.trim().isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("publish_watch_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (mediaType == "reel") "Publish Reel" else "Publish Video")
                }
            }
        }
    }
}
