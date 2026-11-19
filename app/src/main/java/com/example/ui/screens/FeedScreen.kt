package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DEFAULT_ADMOB_BANNER_UNIT_ID
import com.example.data.model.DEFAULT_USD_TO_NGN_RATE
import com.example.data.model.PagePost
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PublicProfile
import com.example.data.model.Shop
import com.example.data.model.ShopOrder
import com.example.data.model.ShopProduct
import com.example.data.model.SocialPage
import com.example.ui.components.CeoWelcomeBanner
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.GoogleAdBannerCard
import com.example.ui.components.MomentMediaView
import com.example.ui.components.PlusBadge
import com.example.ui.components.ProfileAvatar
import com.example.ui.components.ProfileDetailBottomSheet
import com.example.ui.components.VipLevelBadge
import com.example.ui.components.formatRelativeTimestamp
import com.example.ui.theme.LocalBintafaceAccents
import com.example.ui.theme.SuperlikeGold
import com.example.ui.util.encodeUriToCompactBase64
import com.example.ui.viewmodel.UiState
import kotlinx.coroutines.launch

private val FEED_TAGS = listOf("All", "Thread", "Moment", "Hangout", "Date Idea", "Question", "Reel", "Video")
private val COMPOSER_TAGS = listOf("Moment", "Thread", "Hangout", "Date Idea", "Question")
private val MOMENT_BACKDROPS = listOf(
    null to "Text Only",
    "coffee" to "Cafe",
    "sunset" to "Rooftop",
    "trail" to "Coastal"
)

@Composable
fun FeedScreen(
    currentUserId: String,
    myProfile: PublicProfile?,
    postsState: UiState<List<Post>>,
    likedPostIds: Set<String>,
    repostedPostIds: Set<String>,
    followedUserIds: Set<String>,
    selectedTagFilter: String,
    activeCommentPostId: String?,
    activeCommentsState: UiState<List<PostComment>>,
    isSaving: Boolean,
    isPlusMember: Boolean,
    onSelectTagFilter: (String) -> Unit,
    onCreatePost: (content: String, tag: String, imageKey: String?, postPhotoBase64: String?, mediaType: String, title: String, onDone: () -> Unit) -> Unit,
    onDeletePost: (String) -> Unit,
    onToggleLike: (postId: String, currentlyLiked: Boolean) -> Unit,
    onRepost: (Post) -> Unit,
    onToggleFollow: (authorId: String, authorName: String) -> Unit,
    onOpenComments: (String) -> Unit,
    onCloseComments: () -> Unit,
    onAddComment: (postId: String, text: String, onDone: () -> Unit) -> Unit,
    onSendSparkGift: (Post) -> Unit,
    onFindProfile: (String) -> PublicProfile?,
    vipLevelForUser: (String) -> String = { "none" },
    onOpenProfileTab: () -> Unit,
    onOpenPaywall: () -> Unit,
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
    pagesState: UiState<List<SocialPage>> = UiState.Loading,
    followedPageIds: Set<String> = emptySet(),
    activePage: SocialPage? = null,
    activePagePostsState: UiState<List<PagePost>> = UiState.Success(emptyList()),
    likedPagePostIds: Set<String> = emptySet(),
    onCreatePage: (
        name: String,
        handle: String,
        category: String,
        description: String,
        websiteUrl: String,
        coverKey: String,
        photoBase64: String?,
        onDone: () -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onUpdatePage: (
        pageId: String,
        name: String,
        category: String,
        description: String,
        websiteUrl: String,
        coverKey: String,
        photoBase64: String?,
        onDone: () -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    onOpenPage: (String) -> Unit = {},
    onClosePage: () -> Unit = {},
    onToggleFollowPage: (SocialPage) -> Unit = {},
    onPublishPagePost: (page: SocialPage, text: String, imageKey: String?, photoBase64: String?, onDone: () -> Unit) -> Unit = { _, _, _, _, _ -> },
    onToggleLikePagePost: (pageId: String, postId: String, currentlyLiked: Boolean) -> Unit = { _, _, _ -> },
    onDeletePagePost: (pageId: String, postId: String) -> Unit = { _, _ -> },
    onSendSparkToPage: (SocialPage) -> Unit = {},
    shopsState: UiState<List<Shop>> = UiState.Loading,
    activeShop: Shop? = null,
    activeShopProductsState: UiState<List<ShopProduct>> = UiState.Success(emptyList()),
    myBuyerOrders: List<ShopOrder> = emptyList(),
    mySellerOrders: List<ShopOrder> = emptyList(),
    usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE,
    initialSubMode: Int = 0,
    subModeRequestTick: Int = 0,
    onOpenShop: (String) -> Unit = {},
    onCloseShop: () -> Unit = {},
    onCreateShop: (
        name: String,
        handle: String,
        category: String,
        description: String,
        currency: String,
        city: String,
        coverKey: String,
        photoBase64: String?,
        onDone: () -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onUpdateShop: (
        shopId: String,
        name: String,
        category: String,
        description: String,
        currency: String,
        city: String,
        coverKey: String,
        photoBase64: String?,
        onDone: () -> Unit
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onAddShopProduct: (
        shop: Shop,
        title: String,
        description: String,
        category: String,
        priceUsdCents: Long,
        priceNgn: Long,
        stockCount: Long,
        imageKey: String?,
        photoBase64: String?
    ) -> Unit = { _, _, _, _, _, _, _, _, _ -> },
    onPlaceShopOrder: (
        shop: Shop,
        product: ShopProduct,
        quantity: Long,
        currency: String,
        deliveryNote: String,
        onDone: () -> Unit
    ) -> Unit = { _, _, _, _, _, _ -> },
    onUpdateShopOrderStatus: (orderId: String, newStatus: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showCreateSheet by remember { mutableStateOf(false) }
    var inspectedProfile by remember { mutableStateOf<PublicProfile?>(null) }
    var feedSubMode by rememberSaveable { mutableIntStateOf(initialSubMode) }
    LaunchedEffect(subModeRequestTick) {
        if (subModeRequestTick > 0) {
            feedSubMode = initialSubMode
        }
    }
    val accents = LocalBintafaceAccents.current
    var showWelcomeBanner by rememberSaveable { mutableStateOf(true) }
    var selectedPageCategory by rememberSaveable { mutableStateOf("All") }
    var showCreateOrEditPage by remember { mutableStateOf(false) }
    var editingPage by remember { mutableStateOf<SocialPage?>(null) }
    var selectedShopCategory by rememberSaveable { mutableStateOf("All") }
    var showCreateOrEditShop by remember { mutableStateOf(false) }
    var editingShop by remember { mutableStateOf<Shop?>(null) }
    var showShopOrdersSheet by remember { mutableStateOf(false) }
    var pendingDeletePostId by remember { mutableStateOf<String?>(null) }

    BackHandler(
        enabled = feedSubMode != 0 &&
            activePage == null &&
            activeShop == null &&
            !showCreateOrEditPage &&
            !showCreateOrEditShop &&
            !showShopOrdersSheet &&
            !showCreateSheet &&
            activeCommentPostId == null
    ) {
        feedSubMode = 0
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (feedSubMode) {
                        1 -> {
                            editingPage = null
                            showCreateOrEditPage = true
                        }
                        2 -> {
                            editingShop = null
                            showCreateOrEditShop = true
                        }
                        else -> showCreateSheet = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag(
                    when (feedSubMode) {
                        1 -> "create_page_fab"
                        2 -> "create_shop_fab"
                        else -> "create_post_fab"
                    }
                )
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = when (feedSubMode) {
                        1 -> "Create a Page"
                        2 -> "Open a Shop"
                        else -> "Share a moment"
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 92.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "BINTAFACE",
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Feed, threads, reels, live & chat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isPlusMember) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onOpenPaywall() }
                                    .testTag("feed_plus_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.WorkspacePremium,
                                        contentDescription = "Bintaface+",
                                        tint = if (isPlusMember) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isPlusMember) "Plus" else "Bintaface+",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isPlusMember) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clickable { onOpenProfileTab() }
                                    .testTag("feed_profile_shortcut")
                            ) {
                                ProfileAvatar(
                                    name = myProfile?.displayName ?: "You",
                                    avatarKey = myProfile?.avatarKey ?: "coffee",
                                    photoBase64 = myProfile?.photoBase64,
                                    size = 42.dp
                                )
                            }
                        }
                    }
                }

                // Feed | Pages | Shops Sub-Navigation Switcher
                item {
                    val pagesCount = (pagesState as? UiState.Success)?.data?.size ?: 0
                    val shopsCount = (shopsState as? UiState.Success)?.data?.size ?: 0
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FeedSubModePill(
                            selected = feedSubMode == 0,
                            icon = Icons.Filled.DynamicFeed,
                            label = "Feed",
                            accent = MaterialTheme.colorScheme.primary,
                            onClick = { feedSubMode = 0 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_feed_tab")
                        )
                        FeedSubModePill(
                            selected = feedSubMode == 1,
                            icon = Icons.Filled.Flag,
                            label = if (pagesCount > 0) "Pages ($pagesCount)" else "Pages",
                            accent = MaterialTheme.colorScheme.primary,
                            onClick = { feedSubMode = 1 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_pages_tab")
                        )
                        FeedSubModePill(
                            selected = feedSubMode == 2,
                            icon = Icons.Filled.Storefront,
                            label = if (shopsCount > 0) "Shops ($shopsCount)" else "Shops",
                            accent = MaterialTheme.colorScheme.primary,
                            onClick = { feedSubMode = 2 },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("mode_shops_tab")
                        )
                    }
                }

                when (feedSubMode) {
                    1 -> {
                        pagesDirectoryItems(
                            currentUserId = currentUserId,
                            pagesState = pagesState,
                            followedPageIds = followedPageIds,
                            selectedCategory = selectedPageCategory,
                            onSelectCategory = { selectedPageCategory = it },
                            onOpenCreatePage = {
                                editingPage = null
                                showCreateOrEditPage = true
                            },
                            onOpenPage = { page -> onOpenPage(page.id) },
                            onToggleFollowPage = onToggleFollowPage
                        )
                    }
                    2 -> {
                        shopsDirectoryItems(
                            currentUserId = currentUserId,
                            shopsState = shopsState,
                            buyerOrdersCount = myBuyerOrders.size,
                            sellerOrdersCount = mySellerOrders.size,
                            usdToNgnRate = usdToNgnRate,
                            selectedCategory = selectedShopCategory,
                            onSelectCategory = { selectedShopCategory = it },
                            onOpenCreateShop = {
                                editingShop = null
                                showCreateOrEditShop = true
                            },
                            onOpenOrdersSheet = { showShopOrdersSheet = true },
                            onOpenShop = { shop -> onOpenShop(shop.id) }
                        )
                    }
                    else -> {
                        if (showWelcomeBanner) {
                            item {
                                CeoWelcomeBanner(
                                    dismissible = true,
                                    onDismiss = { showWelcomeBanner = false }
                                )
                            }
                        }
                        // Quick Composer Card (Facebook-style status bar)
                item {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreateSheet = true }
                            .testTag("quick_composer_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ProfileAvatar(
                                name = myProfile?.displayName ?: "You",
                                avatarKey = myProfile?.avatarKey ?: "coffee",
                                photoBase64 = myProfile?.photoBase64,
                                size = 40.dp
                            )
                            Text(
                                text = "Post a thread, moment, hangout or update…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddPhotoAlternate,
                                    contentDescription = "Add photo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Category Filter Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FEED_TAGS.forEach { tag ->
                            FilterChip(
                                selected = selectedTagFilter == tag,
                                onClick = { onSelectTagFilter(tag) },
                                label = { Text(tag) }
                            )
                        }
                    }
                }

                item {
                    val featured = (postsState as? UiState.Success)?.data?.firstOrNull { it.authorId != currentUserId }
                    GoogleAdBannerCard(
                        adUnitId = admobBannerUnitId,
                        creatorId = featured?.authorId.orEmpty(),
                        creatorName = featured?.authorName.orEmpty(),
                        postId = featured?.id ?: "feed_banner",
                        onPaidMicros = { micros, currency, unitId, cId, cName, pId ->
                            onGoogleAdPaid("banner", unitId, micros, currency, cId, cName, pId)
                        }
                    )
                }

                when (postsState) {
                    is UiState.Loading -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                    is UiState.Error -> {
                        item {
                            EmptyStateCard(
                                icon = Icons.Filled.DynamicFeed,
                                title = "Couldn't load feed",
                                subtitle = postsState.message
                            )
                        }
                    }
                    is UiState.Success -> {
                        if (postsState.data.isEmpty()) {
                            item {
                                EmptyStateCard(
                                    icon = Icons.Filled.DynamicFeed,
                                    title = "Start the conversation",
                                    subtitle = "Post a moment, thread, hangout, or update to kick off the BINTAFACE feed.",
                                    actionLabel = "Share First Post",
                                    onAction = { showCreateSheet = true }
                                )
                            }
                        } else {
                            items(postsState.data, key = { it.id }) { post ->
                                PostCard(
                                    post = post,
                                    authorVipLevel = vipLevelForUser(post.authorId),
                                    isOwnPost = post.authorId == currentUserId,
                                    isLiked = post.id in likedPostIds,
                                    isReposted = post.id in repostedPostIds,
                                    isFollowing = post.authorId in followedUserIds,
                                    onAuthorClick = {
                                        val profile = if (post.authorId == currentUserId) {
                                            myProfile
                                        } else {
                                            onFindProfile(post.authorId)
                                        }
                                        if (profile != null) {
                                            inspectedProfile = profile
                                        }
                                    },
                                    onToggleFollow = { onToggleFollow(post.authorId, post.authorName) },
                                    onToggleLike = { onToggleLike(post.id, post.id in likedPostIds) },
                                    onRepost = { onRepost(post) },
                                    onOpenComments = { onOpenComments(post.id) },
                                    onSendSpark = { onSendSparkGift(post) },
                                    onDelete = { pendingDeletePostId = post.id }
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

    if (activePage != null) {
        PageDetailOverlay(
            page = activePage,
            currentUserId = currentUserId,
            isFollowing = activePage.id in followedPageIds,
            postsState = activePagePostsState,
            likedPagePostIds = likedPagePostIds,
            isSaving = isSaving,
            admobBannerUnitId = admobBannerUnitId,
            onBack = onClosePage,
            onToggleFollow = { onToggleFollowPage(activePage) },
            onEditPage = {
                editingPage = activePage
                showCreateOrEditPage = true
            },
            onSendSparkGift = { onSendSparkToPage(activePage) },
            onPublishPagePost = { text, imageKey, photoBase64, onDone ->
                onPublishPagePost(activePage, text, imageKey, photoBase64, onDone)
            },
            onToggleLikePost = { postId, currentlyLiked ->
                onToggleLikePagePost(activePage.id, postId, currentlyLiked)
            },
            onDeletePost = { postId -> onDeletePagePost(activePage.id, postId) },
            onGoogleAdPaid = onGoogleAdPaid
        )
    }

    if (showCreateOrEditPage) {
        CreateOrEditPageSheet(
            existingPage = editingPage,
            isSaving = isSaving,
            onDismiss = {
                showCreateOrEditPage = false
                editingPage = null
            },
            onSubmit = { name, handle, category, description, websiteUrl, coverKey, photoBase64 ->
                val target = editingPage
                if (target == null) {
                    onCreatePage(name, handle, category, description, websiteUrl, coverKey, photoBase64) {
                        showCreateOrEditPage = false
                    }
                } else {
                    onUpdatePage(target.id, name, category, description, websiteUrl, coverKey, photoBase64) {
                        showCreateOrEditPage = false
                        editingPage = null
                    }
                }
            }
        )
    }

    if (activeShop != null) {
        ShopDetailOverlay(
            currentUserId = currentUserId,
            shop = activeShop,
            productsState = activeShopProductsState,
            usdToNgnRate = usdToNgnRate,
            isSaving = isSaving,
            onDismiss = onCloseShop,
            onEditShop = {
                editingShop = activeShop
                showCreateOrEditShop = true
            },
            onAddProduct = { title, desc, cat, usdCents, ngn, stock, imgKey, photo ->
                onAddShopProduct(activeShop, title, desc, cat, usdCents, ngn, stock, imgKey, photo)
            },
            onPlaceOrder = { prod, qty, currency, note, onDone ->
                onPlaceShopOrder(activeShop, prod, qty, currency, note, onDone)
            }
        )
    }

    if (showCreateOrEditShop) {
        CreateOrEditShopSheet(
            existingShop = editingShop,
            isSaving = isSaving,
            onDismiss = {
                showCreateOrEditShop = false
                editingShop = null
            },
            onSubmit = { name, handle, category, description, currency, city, coverKey, photoBase64 ->
                val target = editingShop
                if (target == null) {
                    onCreateShop(name, handle, category, description, currency, city, coverKey, photoBase64) {
                        showCreateOrEditShop = false
                    }
                } else {
                    onUpdateShop(target.id, name, category, description, currency, city, coverKey, photoBase64) {
                        showCreateOrEditShop = false
                        editingShop = null
                    }
                }
            }
        )
    }

    if (showShopOrdersSheet) {
        MyShopOrdersSheet(
            buyerOrders = myBuyerOrders,
            sellerOrders = mySellerOrders,
            usdToNgnRate = usdToNgnRate,
            onUpdateOrderStatus = onUpdateShopOrderStatus,
            onDismiss = { showShopOrdersSheet = false }
        )
    }

    if (showCreateSheet) {
        CreatePostSheet(
            isSaving = isSaving,
            onDismiss = { showCreateSheet = false },
            onPublish = { content, tag, imageKey, photoBase64, title ->
                val mediaType = if (tag == "Thread") "thread" else "post"
                onCreatePost(content, tag, imageKey, photoBase64, mediaType, title) {
                    showCreateSheet = false
                }
            }
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

    inspectedProfile?.let { profile ->
        ProfileDetailBottomSheet(
            profile = profile,
            onDismiss = { inspectedProfile = null }
        )
    }

    pendingDeletePostId?.let { targetPostId ->
        AlertDialog(
            onDismissRequest = { pendingDeletePostId = null },
            title = { Text("Delete this post?") },
            text = { Text("This will permanently remove your post and its comments from BINTAFACE.") },
            confirmButton = {
                Button(
                    onClick = {
                        pendingDeletePostId = null
                        onDeletePost(targetPostId)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingDeletePostId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FeedSubModePill(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.surfaceContainer,
        label = "feedSubModeBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) accent else accent.copy(alpha = 0.25f),
        label = "feedSubModeBorder"
    )
    val contentTint by animateColorAsState(
        targetValue = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        label = "feedSubModeTint"
    )
    Surface(
        shape = RoundedCornerShape(50),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) Color.White else accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = contentTint
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PostCard(
    post: Post,
    authorVipLevel: String = "none",
    isOwnPost: Boolean,
    isLiked: Boolean,
    isReposted: Boolean,
    isFollowing: Boolean,
    onAuthorClick: () -> Unit,
    onToggleFollow: () -> Unit,
    onToggleLike: () -> Unit,
    onRepost: () -> Unit,
    onOpenComments: () -> Unit,
    onSendSpark: () -> Unit,
    onDelete: () -> Unit
) {
    val heartColor by animateColorAsState(
        targetValue = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "heartColor"
    )
    val heartScale by animateFloatAsState(
        targetValue = if (isLiked) 1.18f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "heartScale"
    )

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("post_card_${post.id}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(MaterialTheme.shapes.small)
                        .clickable(onClick = onAuthorClick)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileAvatar(
                        name = post.authorName,
                        avatarKey = post.authorAvatarKey,
                        photoBase64 = post.authorPhotoBase64,
                        size = 42.dp
                    )
                    Column {
                        FlowRow(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = post.authorName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            VipLevelBadge(vipLevel = authorVipLevel)
                            if (post.authorIsPlus) {
                                PlusBadge()
                            }
                        }
                        Text(
                            text = formatRelativeTimestamp(post.createdAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!isOwnPost) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isFollowing) MaterialTheme.colorScheme.surfaceContainerHigh
                            else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onToggleFollow)
                                .testTag("follow_author_${post.authorId}")
                        ) {
                            Text(
                                text = if (isFollowing) "Following" else "Follow",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isFollowing) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = post.tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    if (isOwnPost) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete post",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (post.title.isNotBlank()) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = post.content,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            val hasMedia = !post.postPhotoBase64.isNullOrBlank() || !post.imageKey.isNullOrBlank()
            if (hasMedia) {
                MomentMediaView(
                    imageKey = post.imageKey,
                    photoBase64 = post.postPhotoBase64,
                    contentDescription = "Post attachment",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onToggleLike)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("like_post_button_${post.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike post" else "Like post",
                        tint = heartColor,
                        modifier = Modifier
                            .size(19.dp)
                            .scale(heartScale)
                    )
                    Text(
                        text = "${post.likesCount}",
                        style = MaterialTheme.typography.labelLarge,
                        color = heartColor
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onOpenComments)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("comment_post_button_${post.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ChatBubbleOutline,
                        contentDescription = "Open comments",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "${post.commentsCount}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .minimumInteractiveComponentSize()
                        .clickable(onClick = onRepost)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .testTag("repost_post_button_${post.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val repostTint = if (isReposted) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                    Icon(
                        imageVector = Icons.Filled.Repeat,
                        contentDescription = "Repost",
                        tint = repostTint,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "${post.repostsCount}",
                        style = MaterialTheme.typography.labelLarge,
                        color = repostTint
                    )
                }

                if (!isOwnPost || post.sparksCount > 0L) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .minimumInteractiveComponentSize()
                            .clickable(enabled = !isOwnPost, onClick = onSendSpark)
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .testTag("spark_post_button_${post.id}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "Send paid Spark Gift to creator",
                            tint = SuperlikeGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (post.sparksCount > 0L) "${post.sparksCount}✨" else "$1",
                            style = MaterialTheme.typography.labelLarge,
                            color = SuperlikeGold,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreatePostSheet(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onPublish: (content: String, tag: String, imageKey: String?, postPhotoBase64: String?, title: String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf("Moment") }
    var selectedBackdrop by remember { mutableStateOf<String?>(null) }
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
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Share a Moment",
                    style = MaterialTheme.typography.headlineMedium
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close composer")
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                COMPOSER_TAGS.forEach { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { selectedTag = tag },
                        label = { Text(tag) }
                    )
                }
            }

            if (selectedTag == "Thread") {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 120) title = it },
                    label = { Text("Thread Topic (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = content,
                onValueChange = { if (it.length <= 1000) content = it },
                placeholder = { Text("Share a thread, moment, hangout, or update on BINTAFACE…") },
                minLines = 3,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_content_input")
            )

            Text(
                text = "Attach Visual",
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
                    Text(if (pickedPhotoBase64 != null) "Photo Ready" else "Gallery")
                }

                MOMENT_BACKDROPS.forEach { (key, label) ->
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

            if (pickedPhotoBase64 != null || selectedBackdrop != null) {
                MomentMediaView(
                    imageKey = selectedBackdrop,
                    photoBase64 = pickedPhotoBase64,
                    contentDescription = "Selected post media preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            }

            Button(
                onClick = { onPublish(content.trim(), selectedTag, selectedBackdrop, pickedPhotoBase64, title.trim()) },
                enabled = !isSaving && !isProcessing && content.trim().isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("publish_post_button")
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Publish Post", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    commentsState: UiState<List<PostComment>>,
    onDismiss: () -> Unit,
    onSendComment: (String, onDone: () -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var commentText by remember { mutableStateOf("") }

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
            Text(
                text = "Comments",
                style = MaterialTheme.typography.headlineMedium
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                when (commentsState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    is UiState.Error -> {
                        Text(
                            text = commentsState.message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    is UiState.Success -> {
                        if (commentsState.data.isEmpty()) {
                            Text(
                                text = "No comments yet. Say something kind!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(commentsState.data, key = { it.id }) { comment ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        ProfileAvatar(
                                            name = comment.authorName,
                                            avatarKey = comment.authorAvatarKey,
                                            size = 34.dp
                                        )
                                        Surface(
                                            shape = MaterialTheme.shapes.medium,
                                            color = MaterialTheme.colorScheme.surfaceContainer,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = comment.authorName,
                                                        style = MaterialTheme.typography.labelLarge
                                                    )
                                                    Text(
                                                        text = formatRelativeTimestamp(comment.createdAt),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Text(
                                                    text = comment.text,
                                                    style = MaterialTheme.typography.bodyMedium
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { if (it.length <= 500) commentText = it },
                    placeholder = { Text("Write a reply…") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("comment_input")
                )
                val canSendComment = commentText.trim().isNotEmpty()
                IconButton(
                    onClick = {
                        val trimmed = commentText.trim()
                        if (trimmed.isNotEmpty()) {
                            onSendComment(trimmed) { commentText = "" }
                        }
                    },
                    enabled = canSendComment,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (canSendComment) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                        .testTag("send_comment_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send comment",
                        tint = if (canSendComment) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }
            }
        }
    }
}
