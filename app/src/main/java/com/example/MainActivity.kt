package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DynamicFeed
import androidx.compose.material.icons.outlined.LiveTv
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ads.AdMobManager
import com.example.billing.BillingManager
import com.example.data.model.maskAccountNumber
import com.example.data.repository.KindredRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutUser
import com.example.ui.components.CallOverlaySheet
import com.example.ui.components.CeoWelcomeBanner
import com.example.ui.components.IncomingCallBanner
import com.example.ui.components.ProfileEditorForm
import com.example.ui.screens.AdminRevenueSheet
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.ChatsAndChannelsScreen
import com.example.ui.screens.CreatorWalletSheet
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.LevelUpgradeSheet
import com.example.ui.screens.LiveScreen
import com.example.ui.screens.PaywallSheet
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.RadioAccessSheet
import com.example.ui.screens.WatchScreen
import com.example.ui.theme.AppDarkMode
import com.example.ui.theme.KindredTheme
import com.example.ui.theme.LocalBintafaceAccents
import com.example.ui.theme.ThemeController
import com.example.ui.viewmodel.KindredViewModel
import com.example.ui.viewmodel.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_FEED = "feed"
const val ROUTE_WATCH = "watch"
const val ROUTE_LIVE = "live"
const val ROUTE_CHATS = "chats"
const val ROUTE_PROFILE = "profile"
const val ROUTE_CHAT = "chat/{matchId}"

private data class TopDestination(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val TOP_DESTINATIONS = listOf(
    TopDestination(ROUTE_FEED, R.string.nav_feed, Icons.Filled.DynamicFeed, Icons.Outlined.DynamicFeed, "nav_feed"),
    TopDestination(ROUTE_WATCH, R.string.nav_watch, Icons.Filled.PlayCircleFilled, Icons.Outlined.PlayCircleOutline, "nav_watch"),
    TopDestination(ROUTE_LIVE, R.string.nav_live, Icons.Filled.LiveTv, Icons.Outlined.LiveTv, "nav_live"),
    TopDestination(ROUTE_CHATS, R.string.nav_chats, Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, "nav_chats"),
    TopDestination(ROUTE_PROFILE, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.PersonOutline, "nav_profile")
)
private val TOP_ROUTE_SET = TOP_DESTINATIONS.mapTo(mutableSetOf()) { it.route }

class MainActivity : ComponentActivity() {
    private var incomingDeepLinkUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeController.init(this)
        enableEdgeToEdge()
        incomingDeepLinkUri = intent?.data
        setContent {
            val themeSettings by ThemeController.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeSettings.darkMode) {
                AppDarkMode.SYSTEM -> systemDark
                AppDarkMode.LIGHT -> false
                AppDarkMode.DARK -> true
            }
            DisposableEffect(isDark) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    },
                    navigationBarStyle = if (isDark) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                    }
                )
                onDispose {}
            }
            KindredTheme(
                preset = themeSettings.preset,
                darkTheme = isDark
            ) {
                KindredAppRoot(
                    deepLinkUri = incomingDeepLinkUri,
                    onConsumeDeepLink = { incomingDeepLinkUri = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { uri ->
            incomingDeepLinkUri = uri
        }
    }
}

@Composable
fun KindredAppRoot(
    deepLinkUri: Uri? = null,
    onConsumeDeepLink: () -> Unit = {}
) {
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            KindredAuthenticatedShell(
                user = user,
                deepLinkUri = deepLinkUri,
                onConsumeDeepLink = onConsumeDeepLink
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KindredAuthenticatedShell(
    user: FirebaseUser,
    deepLinkUri: Uri? = null,
    onConsumeDeepLink: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val billingManager = remember(context.applicationContext, user.uid) {
        BillingManager(context.applicationContext)
    }
    val adMobManager = remember(context.applicationContext, user.uid) {
        AdMobManager(context.applicationContext)
    }

    LaunchedEffect(adMobManager) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            adMobManager.initialize()
        }
    }

    DisposableEffect(lifecycleOwner, billingManager) {
        billingManager.startConnection()
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                billingManager.refreshPurchases()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            billingManager.endConnection()
        }
    }

    val viewModel: KindredViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                KindredViewModel(
                    repository = KindredRepository(db, Firebase.auth),
                    currentUserId = user.uid,
                    currentUserEmail = user.email,
                    billingManager = billingManager,
                    adMobManager = adMobManager
                )
            }
        }
    )

    val myProfileState by viewModel.myProfile.collectAsStateWithLifecycle()
    val privateSettings by viewModel.privateSettings.collectAsStateWithLifecycle()
    val postsState by viewModel.postsState.collectAsStateWithLifecycle()
    val reelsState by viewModel.reelsState.collectAsStateWithLifecycle()
    val videosState by viewModel.videosState.collectAsStateWithLifecycle()
    val likedPostIds by viewModel.likedPostIds.collectAsStateWithLifecycle()
    val repostedPostIds by viewModel.repostedPostIds.collectAsStateWithLifecycle()
    val followedUserIds by viewModel.followedUserIds.collectAsStateWithLifecycle()
    val feedTagFilter by viewModel.feedTagFilter.collectAsStateWithLifecycle()
    val activeCommentPostId by viewModel.activeCommentPostId.collectAsStateWithLifecycle()
    val activeCommentsState by viewModel.activeComments.collectAsStateWithLifecycle()
    val liveRoomsState by viewModel.liveRoomsState.collectAsStateWithLifecycle()
    val activeLiveRoom by viewModel.activeLiveRoom.collectAsStateWithLifecycle()
    val activeLiveMessagesState by viewModel.activeLiveMessages.collectAsStateWithLifecycle()
    val channelsState by viewModel.channelsState.collectAsStateWithLifecycle()
    val subscribedChannelIds by viewModel.subscribedChannelIds.collectAsStateWithLifecycle()
    val activeChannel by viewModel.activeChannel.collectAsStateWithLifecycle()
    val activeChannelBroadcastsState by viewModel.activeChannelBroadcasts.collectAsStateWithLifecycle()
    val pagesState by viewModel.pagesState.collectAsStateWithLifecycle()
    val followedPageIds by viewModel.followedPageIds.collectAsStateWithLifecycle()
    val likedPagePostIds by viewModel.likedPagePostIds.collectAsStateWithLifecycle()
    val activePage by viewModel.activePage.collectAsStateWithLifecycle()
    val activePagePostsState by viewModel.activePagePosts.collectAsStateWithLifecycle()
    val conversationsState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val activeConversation by viewModel.activeConversation.collectAsStateWithLifecycle()
    val activeDirectMessagesState by viewModel.activeDirectMessages.collectAsStateWithLifecycle()
    val allDiscoverableProfiles by viewModel.allDiscoverableProfiles.collectAsStateWithLifecycle()
    val discoverDeckState by viewModel.discoverDeck.collectAsStateWithLifecycle()
    val incomingLikes by viewModel.incomingLikes.collectAsStateWithLifecycle()
    val discoverFilter by viewModel.discoverFilter.collectAsStateWithLifecycle()
    val matchesState by viewModel.matchesState.collectAsStateWithLifecycle()
    val activeMatch by viewModel.activeMatch.collectAsStateWithLifecycle()
    val activeMessagesState by viewModel.activeMessages.collectAsStateWithLifecycle()
    val newMatchPopup by viewModel.newMatchPopup.collectAsStateWithLifecycle()
    val statusMessage by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val showPaywall by viewModel.showPaywall.collectAsStateWithLifecycle()
    val offers by viewModel.offers.collectAsStateWithLifecycle()
    val isPlayStoreReady by viewModel.isPlayStoreReady.collectAsStateWithLifecycle()
    val billingNotice by viewModel.billingNotice.collectAsStateWithLifecycle()
    val isPlusMember by viewModel.isPlusMember.collectAsStateWithLifecycle()
    val currentVipLevel by viewModel.currentVipLevel.collectAsStateWithLifecycle()
    val currentVipLevelRank by viewModel.currentVipLevelRank.collectAsStateWithLifecycle()
    val myLevelUpgrades by viewModel.myLevelUpgrades.collectAsStateWithLifecycle()
    val vipLevelByUserId by viewModel.vipLevelByUserId.collectAsStateWithLifecycle()
    val dailyLikesRemaining by viewModel.dailyLikesRemaining.collectAsStateWithLifecycle()
    val superlikesBalance by viewModel.superlikesBalance.collectAsStateWithLifecycle()
    val boostsBalance by viewModel.boostsBalance.collectAsStateWithLifecycle()
    val sparksBalance by viewModel.sparksBalance.collectAsStateWithLifecycle()
    val platformSettings by viewModel.platformSettings.collectAsStateWithLifecycle()
    val isAdmin by viewModel.isAdmin.collectAsStateWithLifecycle()
    val usdToNgnRate by viewModel.usdToNgnRate.collectAsStateWithLifecycle()
    val appDownloadUrl by viewModel.appDownloadUrl.collectAsStateWithLifecycle()
    val admobBannerUnitId by viewModel.admobBannerUnitId.collectAsStateWithLifecycle()
    val adImpressionsCount by viewModel.adImpressionsCount.collectAsStateWithLifecycle()
    val lastAdEventStatus by viewModel.lastAdEventStatus.collectAsStateWithLifecycle()
    val receivedSparkGifts by viewModel.receivedSparkGifts.collectAsStateWithLifecycle()
    val creatorAdRevenueEvents by viewModel.creatorAdRevenueEvents.collectAsStateWithLifecycle()
    val myPayoutRequests by viewModel.myPayoutRequests.collectAsStateWithLifecycle()
    val creatorSparkEarningsCents by viewModel.creatorSparkEarningsCents.collectAsStateWithLifecycle()
    val creatorAdEarningsCents by viewModel.creatorAdEarningsCents.collectAsStateWithLifecycle()
    val creatorTotalEarnedCents by viewModel.creatorTotalEarnedCents.collectAsStateWithLifecycle()
    val creatorPaidOutCents by viewModel.creatorPaidOutCents.collectAsStateWithLifecycle()
    val creatorPendingCents by viewModel.creatorPendingCents.collectAsStateWithLifecycle()
    val creatorAvailableCents by viewModel.creatorAvailableCents.collectAsStateWithLifecycle()
    val adminAllPayoutRequests by viewModel.adminAllPayoutRequests.collectAsStateWithLifecycle()
    val adminRevenueLedger by viewModel.adminRevenueLedger.collectAsStateWithLifecycle()
    val adminPayoutAccount by viewModel.adminPayoutAccount.collectAsStateWithLifecycle()
    val adminWithdrawals by viewModel.adminWithdrawals.collectAsStateWithLifecycle()
    val adminTotalEarnedCents by viewModel.adminTotalEarnedCents.collectAsStateWithLifecycle()
    val adminWithdrawnCents by viewModel.adminWithdrawnCents.collectAsStateWithLifecycle()
    val adminAvailableCents by viewModel.adminAvailableCents.collectAsStateWithLifecycle()
    val showWalletSheet by viewModel.showWalletSheet.collectAsStateWithLifecycle()
    val showAdminSheet by viewModel.showAdminSheet.collectAsStateWithLifecycle()
    val showLevelUpgradeSheet by viewModel.showLevelUpgradeSheet.collectAsStateWithLifecycle()
    val shopsState by viewModel.shopsState.collectAsStateWithLifecycle()
    val activeShop by viewModel.activeShop.collectAsStateWithLifecycle()
    val activeShopProductsState by viewModel.activeShopProducts.collectAsStateWithLifecycle()
    val myBuyerOrders by viewModel.myBuyerOrders.collectAsStateWithLifecycle()
    val mySellerOrders by viewModel.mySellerOrders.collectAsStateWithLifecycle()
    val creatorShopEarningsCents by viewModel.creatorShopEarningsCents.collectAsStateWithLifecycle()
    val incomingRingingCall by viewModel.incomingRingingCall.collectAsStateWithLifecycle()
    val activeCallSession by viewModel.activeCallSession.collectAsStateWithLifecycle()
    val hasRadioAccess by viewModel.hasRadioAccess.collectAsStateWithLifecycle()
    val myRadioPasses by viewModel.myRadioPasses.collectAsStateWithLifecycle()
    val showRadioAccessSheet by viewModel.showRadioAccessSheet.collectAsStateWithLifecycle()
    val radioChannelsState by viewModel.radioChannelsState.collectAsStateWithLifecycle()
    val subscribedRadioChannelIds by viewModel.subscribedRadioChannelIds.collectAsStateWithLifecycle()
    val activeRadioChannel by viewModel.activeRadioChannel.collectAsStateWithLifecycle()
    val activeRadioBroadcastsState by viewModel.activeRadioBroadcasts.collectAsStateWithLifecycle()

    val hostActivity = remember(context) { context.findActivity() }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // Prompt first-time users to finish setting up their BINTAFACE card once loaded
    var onboardingDismissed by rememberSaveable(user.uid) { mutableStateOf(false) }
    var requestedFeedSubMode by rememberSaveable { mutableIntStateOf(0) }
    var feedSubModeRequestTick by rememberSaveable { mutableIntStateOf(0) }
    var requestedLiveSubMode by rememberSaveable { mutableIntStateOf(0) }
    var liveSubModeRequestTick by rememberSaveable { mutableIntStateOf(0) }
    val needsProfileSetup = (myProfileState as? UiState.Success)?.data == null && !onboardingDismissed

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showTopLevelNav = currentRoute in TOP_ROUTE_SET

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openChatRoute: (String) -> Unit = { matchId ->
        viewModel.openChat(matchId)
        navController.navigate("chat/$matchId")
    }

    LaunchedEffect(deepLinkUri) {
        val uri = deepLinkUri ?: return@LaunchedEffect
        val host = uri.host.orEmpty().lowercase()
        val isSupportedHost = host == "bintaface.org" ||
            host == "www.bintaface.org" ||
            host.endsWith(".run.app")
        if (isSupportedHost) {
            val inviteParam = uri.getQueryParameter("invite")
            val segments = uri.pathSegments.map { it.lowercase() }
            when (segments.firstOrNull()) {
                "watch", "reels" -> navigateToTab(ROUTE_WATCH)
                "live" -> {
                    requestedLiveSubMode = 0
                    liveSubModeRequestTick++
                    navigateToTab(ROUTE_LIVE)
                }
                "radio" -> {
                    requestedLiveSubMode = 1
                    liveSubModeRequestTick++
                    navigateToTab(ROUTE_LIVE)
                }
                "chats", "channels", "messages" -> navigateToTab(ROUTE_CHATS)
                "shops", "shop" -> {
                    requestedFeedSubMode = 2
                    feedSubModeRequestTick++
                    navigateToTab(ROUTE_FEED)
                }
                "pages", "page" -> {
                    requestedFeedSubMode = 1
                    feedSubModeRequestTick++
                    navigateToTab(ROUTE_FEED)
                }
                "profile" -> navigateToTab(ROUTE_PROFILE)
                "upgrade", "levels" -> {
                    navigateToTab(ROUTE_PROFILE)
                    viewModel.openLevelUpgradeSheet()
                }
                "u" -> {
                    val handle = uri.pathSegments.getOrNull(1)
                    navigateToTab(ROUTE_FEED)
                    if (!handle.isNullOrBlank()) {
                        snackbarHostState.showSnackbar("Opened BINTAFACE invite link for @$handle")
                    }
                }
                else -> {
                    navigateToTab(ROUTE_FEED)
                    if (!inviteParam.isNullOrBlank()) {
                        snackbarHostState.showSnackbar("Opened BINTAFACE invite link for @$inviteParam")
                    }
                }
            }
        }
        onConsumeDeepLink()
    }

    val myProfile = (myProfileState as? UiState.Success)?.data
    val myPostsCount = (postsState as? UiState.Success)?.data?.count { it.authorId == user.uid } ?: 0
    val matchesCount = (matchesState as? UiState.Success)?.data?.size ?: 0
    val liveRoomsCount = (liveRoomsState as? UiState.Success)?.data?.count { it.status == "live" } ?: 0
    val conversationsCount = (conversationsState as? UiState.Success)?.data?.size ?: 0

    val accents = LocalBintafaceAccents.current
    Box(modifier = Modifier.fillMaxSize()) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showTopLevelNav && !useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        TOP_DESTINATIONS.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val tabColor = when (dest.route) {
                                ROUTE_FEED -> accents.feedTabColor
                                ROUTE_WATCH -> accents.watchTabColor
                                ROUTE_LIVE -> accents.liveTabColor
                                ROUTE_CHATS -> accents.chatsTabColor
                                else -> accents.profileTabColor
                            }
                            val badgeCount = when (dest.route) {
                                ROUTE_LIVE -> liveRoomsCount
                                ROUTE_CHATS -> incomingLikes.size + conversationsCount + matchesCount
                                else -> 0
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = tabColor,
                                    selectedTextColor = tabColor,
                                    indicatorColor = tabColor.copy(alpha = 0.16f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (badgeCount > 0) {
                                                Badge(containerColor = tabColor) {
                                                    Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useNavigationRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
                        TOP_DESTINATIONS.forEach { dest ->
                            val selected = currentRoute == dest.route
                            val tabColor = when (dest.route) {
                                ROUTE_FEED -> accents.feedTabColor
                                ROUTE_WATCH -> accents.watchTabColor
                                ROUTE_LIVE -> accents.liveTabColor
                                ROUTE_CHATS -> accents.chatsTabColor
                                else -> accents.profileTabColor
                            }
                            val badgeCount = when (dest.route) {
                                ROUTE_LIVE -> liveRoomsCount
                                ROUTE_CHATS -> incomingLikes.size + conversationsCount + matchesCount
                                else -> 0
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(dest.route) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = tabColor,
                                    selectedTextColor = tabColor,
                                    indicatorColor = tabColor.copy(alpha = 0.16f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (badgeCount > 0) {
                                                Badge(containerColor = tabColor) {
                                                    Text(if (badgeCount > 99) "99+" else badgeCount.toString())
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = stringResource(dest.labelRes)
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_FEED,
                    enterTransition = { fadeIn(tween(220)) + scaleIn(initialScale = 0.985f, animationSpec = tween(220)) },
                    exitTransition = { fadeOut(tween(180)) },
                    popEnterTransition = { fadeIn(tween(220)) },
                    popExitTransition = { fadeOut(tween(180)) },
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_FEED) {
                        FeedScreen(
                            currentUserId = user.uid,
                            myProfile = myProfile,
                            postsState = postsState,
                            likedPostIds = likedPostIds,
                            repostedPostIds = repostedPostIds,
                            followedUserIds = followedUserIds,
                            selectedTagFilter = feedTagFilter,
                            activeCommentPostId = activeCommentPostId,
                            activeCommentsState = activeCommentsState,
                            isSaving = isSaving,
                            isPlusMember = isPlusMember,
                            onSelectTagFilter = viewModel::setFeedTagFilter,
                            onCreatePost = { content, tag, imageKey, photoBase64, mediaType, title, onDone ->
                                viewModel.createPost(
                                    content = content,
                                    tag = tag,
                                    imageKey = imageKey,
                                    postPhotoBase64 = photoBase64,
                                    mediaType = mediaType,
                                    title = title,
                                    onPublished = onDone
                                )
                            },
                            onDeletePost = viewModel::deletePost,
                            onToggleLike = viewModel::toggleLike,
                            onRepost = viewModel::repostPost,
                            onToggleFollow = viewModel::toggleFollow,
                            onOpenComments = viewModel::openComments,
                            onCloseComments = viewModel::closeComments,
                            onAddComment = viewModel::addComment,
                            onSendSparkGift = viewModel::sendSparkGift,
                            onFindProfile = viewModel::findDiscoverableProfile,
                            vipLevelForUser = { uid -> vipLevelByUserId[uid] ?: "none" },
                            onOpenProfileTab = { navigateToTab(ROUTE_PROFILE) },
                            onOpenPaywall = viewModel::openPaywall,
                            admobBannerUnitId = admobBannerUnitId,
                            onGoogleAdPaid = viewModel::onGoogleAdPaid,
                            pagesState = pagesState,
                            followedPageIds = followedPageIds,
                            activePage = activePage,
                            activePagePostsState = activePagePostsState,
                            likedPagePostIds = likedPagePostIds,
                            onCreatePage = { name, handle, category, description, websiteUrl, coverKey, photoBase64, onDone ->
                                viewModel.createPage(
                                    name = name,
                                    handle = handle,
                                    category = category,
                                    description = description,
                                    websiteUrl = websiteUrl,
                                    coverKey = coverKey,
                                    photoBase64 = photoBase64,
                                    onCreated = { onDone() }
                                )
                            },
                            onUpdatePage = { pageId, name, category, description, websiteUrl, coverKey, photoBase64, onDone ->
                                viewModel.updatePage(
                                    pageId = pageId,
                                    name = name,
                                    category = category,
                                    description = description,
                                    websiteUrl = websiteUrl,
                                    coverKey = coverKey,
                                    photoBase64 = photoBase64,
                                    onUpdated = onDone
                                )
                            },
                            onOpenPage = viewModel::openPage,
                            onClosePage = viewModel::closePage,
                            onToggleFollowPage = viewModel::togglePageFollow,
                            onPublishPagePost = { page, text, imageKey, photoBase64, onDone ->
                                viewModel.publishPagePost(
                                    page = page,
                                    text = text,
                                    imageKey = imageKey,
                                    postPhotoBase64 = photoBase64,
                                    onPublished = onDone
                                )
                            },
                            onToggleLikePagePost = viewModel::toggleLikePagePost,
                            onDeletePagePost = viewModel::deletePagePost,
                            onSendSparkToPage = viewModel::sendSparkToPage,
                            shopsState = shopsState,
                            activeShop = activeShop,
                            activeShopProductsState = activeShopProductsState,
                            myBuyerOrders = myBuyerOrders,
                            mySellerOrders = mySellerOrders,
                            usdToNgnRate = usdToNgnRate,
                            initialSubMode = requestedFeedSubMode,
                            subModeRequestTick = feedSubModeRequestTick,
                            onOpenShop = viewModel::openShop,
                            onCloseShop = viewModel::closeShop,
                            onCreateShop = { name, handle, category, description, currency, city, coverKey, photoBase64, onDone ->
                                viewModel.createShop(
                                    name = name,
                                    handle = handle,
                                    category = category,
                                    description = description,
                                    currency = currency,
                                    city = city,
                                    coverKey = coverKey,
                                    photoBase64 = photoBase64,
                                    onCreated = { onDone() }
                                )
                            },
                            onUpdateShop = { shopId, name, category, description, currency, city, coverKey, photoBase64, onDone ->
                                viewModel.updateShop(
                                    shopId = shopId,
                                    name = name,
                                    category = category,
                                    description = description,
                                    currency = currency,
                                    city = city,
                                    coverKey = coverKey,
                                    photoBase64 = photoBase64,
                                    onUpdated = onDone
                                )
                            },
                            onAddShopProduct = viewModel::addShopProduct,
                            onPlaceShopOrder = viewModel::placeShopOrder,
                            onUpdateShopOrderStatus = viewModel::updateShopOrderStatus
                        )
                    }

                    composable(ROUTE_WATCH) {
                        WatchScreen(
                            currentUserId = user.uid,
                            reelsState = reelsState,
                            videosState = videosState,
                            likedPostIds = likedPostIds,
                            repostedPostIds = repostedPostIds,
                            followedUserIds = followedUserIds,
                            activeCommentPostId = activeCommentPostId,
                            activeCommentsState = activeCommentsState,
                            sparksBalance = sparksBalance,
                            isSaving = isSaving,
                            vipLevelForUser = { uid -> vipLevelByUserId[uid] ?: "none" },
                            onToggleLike = viewModel::toggleLike,
                            onRepost = viewModel::repostPost,
                            onToggleFollow = viewModel::toggleFollow,
                            onOpenComments = viewModel::openComments,
                            onCloseComments = viewModel::closeComments,
                            onAddComment = viewModel::addComment,
                            onSendSparkGift = viewModel::sendSparkGift,
                            onIncrementView = viewModel::incrementPostView,
                            onPublishWatchPost = { mediaType, title, content, durationLabel, audioTrack, imageKey, photoBase64, onDone ->
                                viewModel.createPost(
                                    content = content,
                                    tag = if (mediaType == "reel") "Reel" else "Video",
                                    imageKey = imageKey,
                                    postPhotoBase64 = photoBase64,
                                    mediaType = mediaType,
                                    title = title,
                                    durationLabel = durationLabel,
                                    audioTrack = audioTrack,
                                    onPublished = onDone
                                )
                            },
                            admobBannerUnitId = admobBannerUnitId,
                            onGoogleAdPaid = viewModel::onGoogleAdPaid,
                            onWatchRewardedAd = { cId, cName, pId ->
                                viewModel.watchRewardedAdForCreator(hostActivity, cId, cName, pId)
                            }
                        )
                    }

                    composable(ROUTE_LIVE) {
                        LiveScreen(
                            currentUserId = user.uid,
                            liveRoomsState = liveRoomsState,
                            activeLiveRoom = activeLiveRoom,
                            activeLiveMessagesState = activeLiveMessagesState,
                            followedUserIds = followedUserIds,
                            sparksBalance = sparksBalance,
                            isSaving = isSaving,
                            vipLevelForUser = { uid -> vipLevelByUserId[uid] ?: "none" },
                            onOpenRoom = viewModel::openLiveRoom,
                            onCloseRoom = viewModel::closeLiveRoom,
                            onGoLive = { title, category, coverKey, onDone ->
                                viewModel.createLiveRoom(title, category, coverKey) {
                                    onDone()
                                }
                            },
                            onEndRoom = viewModel::endLiveRoom,
                            onSendHeart = viewModel::sendLiveHeart,
                            onSendLiveMessage = viewModel::sendLiveMessage,
                            onSendSparkGift = viewModel::sendSparkToLiveRoom,
                            onToggleFollow = viewModel::toggleFollow,
                            initialSubMode = requestedLiveSubMode,
                            subModeRequestTick = liveSubModeRequestTick,
                            hasRadioAccess = hasRadioAccess,
                            platformSettings = platformSettings,
                            radioChannelsState = radioChannelsState,
                            subscribedRadioIds = subscribedRadioChannelIds,
                            activeRadioChannel = activeRadioChannel,
                            activeRadioBroadcastsState = activeRadioBroadcastsState,
                            onOpenRadioPaywall = viewModel::openRadioAccessSheet,
                            onOpenRadioChannel = viewModel::openRadioChannel,
                            onCloseRadioChannel = viewModel::closeRadioChannel,
                            onCreateRadioChannel = { title, frequency, handle, category, description, coverKey, onDone ->
                                viewModel.createRadioChannel(
                                    title = title,
                                    frequency = frequency,
                                    handle = handle,
                                    category = category,
                                    description = description,
                                    coverKey = coverKey,
                                    onCreated = { onDone() }
                                )
                            },
                            onToggleRadioStatus = viewModel::toggleRadioChannelLiveStatus,
                            onToggleRadioSubscription = viewModel::toggleRadioSubscription,
                            onPublishRadioBroadcast = viewModel::publishRadioBroadcast,
                            onSendSparkToRadioChannel = viewModel::sendSparkToRadioChannel,
                            onShowNotice = viewModel::showStatusNotice
                        )
                    }

                    composable(ROUTE_CHATS) {
                        ChatsAndChannelsScreen(
                            currentUserId = user.uid,
                            myProfile = myProfile,
                            communityDirectory = allDiscoverableProfiles,
                            conversationsState = conversationsState,
                            activeConversation = activeConversation,
                            activeDirectMessagesState = activeDirectMessagesState,
                            matchesState = matchesState,
                            channelsState = channelsState,
                            subscribedChannelIds = subscribedChannelIds,
                            activeChannel = activeChannel,
                            activeChannelBroadcastsState = activeChannelBroadcastsState,
                            discoverDeckState = discoverDeckState,
                            incomingLikes = incomingLikes,
                            discoverFilter = discoverFilter,
                            newMatchPopup = newMatchPopup,
                            isPlusMember = isPlusMember,
                            dailyLikesRemaining = dailyLikesRemaining,
                            superlikesBalance = superlikesBalance,
                            isSaving = isSaving,
                            onOpenDirectConversation = viewModel::openDirectConversation,
                            onCloseDirectConversation = viewModel::closeDirectConversation,
                            onCreateOrOpenDirectChat = viewModel::openOrCreateDirectChat,
                            onCreateGroupChat = viewModel::createGroupChat,
                            onSendDirectMessage = viewModel::sendDirectMessage,
                            onOpenMatchChat = openChatRoute,
                            onOpenChannel = viewModel::openChannel,
                            onCloseChannel = viewModel::closeChannel,
                            onCreateChannel = { title, handle, desc, cat, onDone ->
                                viewModel.createChannel(title, handle, desc, cat) { onDone() }
                            },
                            onToggleChannelSubscription = viewModel::toggleChannelSubscription,
                            onPublishChannelBroadcast = { channelId, text, onDone ->
                                viewModel.publishChannelBroadcast(channelId, text, onSent = onDone)
                            },
                            onSendSparkToChannel = viewModel::sendSparkToChannel,
                            onSelectDiscoverFilter = viewModel::setDiscoverFilter,
                            onSwipe = viewModel::swipe,
                            onRespondToLike = viewModel::respondToIncomingLike,
                            onDismissMatchPopup = viewModel::dismissMatchPopup,
                            onEditOwnProfile = { navigateToTab(ROUTE_PROFILE) },
                            onOpenPaywall = viewModel::openPaywall,
                            onSendDirectVoiceNote = viewModel::sendConversationVoiceNote,
                            onStartConversationCall = viewModel::startConversationCall,
                            onShowNotice = viewModel::showStatusNotice
                        )
                    }

                    composable(ROUTE_PROFILE) {
                        ProfileScreen(
                            defaultName = user.displayName ?: "BINTAFACE Member",
                            userEmail = user.email,
                            profileState = myProfileState,
                            postsCount = myPostsCount,
                            matchesCount = matchesCount,
                            isSaving = isSaving,
                            isPlusMember = isPlusMember,
                            currentVipLevel = currentVipLevel,
                            currentVipRank = currentVipLevelRank,
                            hasRadioAccess = hasRadioAccess,
                            superlikesBalance = superlikesBalance,
                            boostsBalance = boostsBalance,
                            sparksBalance = sparksBalance,
                            creatorAvailableCents = creatorAvailableCents,
                            usdToNgnRate = usdToNgnRate,
                            appDownloadUrl = appDownloadUrl,
                            isAdmin = isAdmin,
                            adminAvailableCents = adminAvailableCents,
                            adminAccountSummary = adminPayoutAccount?.let {
                                "${it.bankName} (${maskAccountNumber(it.accountNumber)})"
                            },
                            pendingAdminPayoutsCount = adminAllPayoutRequests.count { it.status == "pending" },
                            onSaveProfile = { name, age, city, bio, lookingFor, q, a, interests, avatarKey, photo, discoverable, onSaved ->
                                viewModel.saveProfile(
                                    displayName = name,
                                    age = age,
                                    city = city,
                                    bio = bio,
                                    lookingFor = lookingFor,
                                    promptQuestion = q,
                                    promptAnswer = a,
                                    interests = interests,
                                    avatarKey = avatarKey,
                                    photoBase64 = photo,
                                    isDiscoverable = discoverable,
                                    preferredIntent = privateSettings?.preferredIntent ?: "all",
                                    email = user.email,
                                    onSaved = onSaved
                                )
                            },
                            onActivateBoost = viewModel::activateBoost,
                            onOpenLevelUpgrade = viewModel::openLevelUpgradeSheet,
                            onOpenRadioAccess = viewModel::openRadioAccessSheet,
                            onOpenRadioHub = {
                                requestedLiveSubMode = 1
                                liveSubModeRequestTick++
                                navigateToTab(ROUTE_LIVE)
                            },
                            onOpenPaywall = viewModel::openPaywall,
                            onOpenWallet = viewModel::openWalletSheet,
                            onOpenAdminConsole = viewModel::openAdminSheet,
                            onShowMessage = viewModel::showStatusNotice,
                            onSignOut = {
                                signOutUser(credentialManager, scope)
                            }
                        )
                    }

                    composable(
                        route = ROUTE_CHAT,
                        arguments = listOf(navArgument("matchId") { type = NavType.StringType }),
                        enterTransition = { slideInHorizontally(tween(240)) { it / 3 } + fadeIn(tween(220)) },
                        exitTransition = { fadeOut(tween(180)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition = { slideOutHorizontally(tween(240)) { it / 3 } + fadeOut(tween(180)) }
                    ) { backStack ->
                        val matchId = backStack.arguments?.getString("matchId") ?: ""
                        LaunchedEffect(matchId) {
                            if (matchId.isNotEmpty()) viewModel.openChat(matchId)
                        }
                        ChatScreen(
                            currentUserId = user.uid,
                            match = activeMatch,
                            messagesState = activeMessagesState,
                            onBack = {
                                viewModel.closeChat()
                                navController.popBackStack()
                            },
                            onSendMessage = viewModel::sendMessage,
                            onFindProfile = viewModel::findDiscoverableProfile,
                            onUnmatch = { id ->
                                viewModel.unmatch(id) {
                                    viewModel.closeChat()
                                    navController.popBackStack()
                                }
                            },
                            onSendVoiceNote = viewModel::sendMatchVoiceNote,
                            onStartCall = { callType ->
                                activeMatch?.let { viewModel.startMatchCall(it, callType) }
                            },
                            onShowNotice = viewModel::showStatusNotice
                        )
                    }
                }
            }
        }
    }

        IncomingCallBanner(
            callSession = if (activeCallSession == null) incomingRingingCall else null,
            onAccept = viewModel::acceptIncomingCall,
            onDecline = viewModel::declineIncomingCall,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }

    if (needsProfileSetup) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { onboardingDismissed = true },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Row(
                    modifier = Modifier.padding(bottom = 8.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Welcome to BINTAFACE",
                            style = MaterialTheme.typography.headlineLarge
                        )
                        Text(
                            text = "Set up your creator & social profile to post, go live, chat, and earn.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { onboardingDismissed = true }) {
                        Text("Later")
                    }
                }
                CeoWelcomeBanner()
                Spacer(Modifier.height(12.dp))
                ProfileEditorForm(
                    initialProfile = null,
                    defaultName = user.displayName ?: "BINTAFACE Member",
                    isSaving = isSaving,
                    ctaLabel = "Join BINTAFACE",
                    onSave = { name, age, city, bio, lookingFor, q, a, interests, avatarKey, photo, discoverable ->
                        viewModel.saveProfile(
                            displayName = name,
                            age = age,
                            city = city,
                            bio = bio,
                            lookingFor = lookingFor,
                            promptQuestion = q,
                            promptAnswer = a,
                            interests = interests,
                            avatarKey = avatarKey,
                            photoBase64 = photo,
                            isDiscoverable = discoverable,
                            preferredIntent = "all",
                            email = user.email,
                            onSaved = { onboardingDismissed = true }
                        )
                    }
                )
            }
        }
    }

    if (showPaywall) {
        PaywallSheet(
            offers = offers,
            isPlusMember = isPlusMember,
            activePlanId = privateSettings?.plusPlanId,
            superlikesBalance = superlikesBalance,
            boostsBalance = boostsBalance,
            sparksBalance = sparksBalance,
            isPlayStoreReady = isPlayStoreReady,
            billingNotice = billingNotice,
            onPurchase = { productId ->
                if (hostActivity != null) {
                    viewModel.launchPurchase(hostActivity, productId)
                }
            },
            onRestorePurchases = viewModel::restorePurchases,
            onDismiss = viewModel::closePaywall
        )
    }

    if (showWalletSheet) {
        CreatorWalletSheet(
            sparksBalance = sparksBalance,
            sparkEarnedCents = creatorSparkEarningsCents,
            adEarnedCents = creatorAdEarningsCents,
            shopEarnedCents = creatorShopEarningsCents,
            totalEarnedCents = creatorTotalEarnedCents,
            paidOutCents = creatorPaidOutCents,
            pendingCents = creatorPendingCents,
            availableCents = creatorAvailableCents,
            usdToNgnRate = usdToNgnRate,
            adImpressionsCount = adImpressionsCount,
            lastAdEventStatus = lastAdEventStatus,
            savedMethod = privateSettings?.payoutMethod,
            savedAccount = privateSettings?.payoutAccount,
            savedBankOrNetwork = privateSettings?.payoutBankOrNetwork,
            savedCurrency = privateSettings?.payoutCurrency,
            autoPayoutEnabled = privateSettings?.autoPayoutEnabled ?: true,
            receivedGifts = receivedSparkGifts,
            adRevenueEvents = creatorAdRevenueEvents,
            myPayoutRequests = myPayoutRequests,
            isSaving = isSaving,
            onBuySparks = viewModel::openPaywall,
            onSavePayoutMethod = viewModel::savePayoutPreferences,
            onRequestPayout = viewModel::requestCreatorPayout,
            onDismiss = viewModel::closeWalletSheet
        )
    }

    if (showAdminSheet) {
        AdminRevenueSheet(
            userEmail = user.email,
            platformSettings = platformSettings,
            adminPayoutAccount = adminPayoutAccount,
            adminWithdrawals = adminWithdrawals,
            adminTotalEarnedCents = adminTotalEarnedCents,
            adminWithdrawnCents = adminWithdrawnCents,
            adminAvailableCents = adminAvailableCents,
            usdToNgnRate = usdToNgnRate,
            adImpressionsCount = adImpressionsCount,
            lastAdEventStatus = lastAdEventStatus,
            isAdmin = isAdmin,
            revenueLedger = adminRevenueLedger,
            payoutRequests = adminAllPayoutRequests,
            isSaving = isSaving,
            onSaveAdminPayoutAccount = viewModel::saveAdminPayoutAccount,
            onUpdateAdminConfig = viewModel::updateAdminAdAndDownloadConfig,
            onWithdrawAdminRevenue = viewModel::withdrawAdminRevenue,
            onProcessAutoPayoutBatch = viewModel::processAutoPayoutBatch,
            onSettlePayoutRequest = viewModel::settlePayoutRequest,
            onDismiss = viewModel::closeAdminSheet
        )
    }

    if (showLevelUpgradeSheet) {
        val nextLevelRank = (currentVipLevelRank + 1).coerceIn(1, 10)
        LevelUpgradeSheet(
            userEmail = user.email,
            currentVipLevel = currentVipLevel,
            currentVipRank = currentVipLevelRank,
            usdToNgnRate = usdToNgnRate,
            platformSettings = platformSettings,
            myUpgrades = myLevelUpgrades,
            isSaving = isSaving,
            isPlayStoreReady = isPlayStoreReady,
            isLevelPlaySkuReady = viewModel.isLevelProductReadyInPlay(nextLevelRank),
            onLaunchGooglePlayLevel = { rank ->
                hostActivity?.let { viewModel.launchGoogleLevelUpgradePurchase(it, rank) } ?: false
            },
            onConfirmUpgrade = viewModel::upgradeUserLevel,
            onDismiss = viewModel::closeLevelUpgradeSheet
        )
    }

    if (showRadioAccessSheet) {
        RadioAccessSheet(
            userEmail = user.email,
            hasRadioAccess = hasRadioAccess,
            usdToNgnRate = usdToNgnRate,
            platformSettings = platformSettings,
            myRadioPasses = myRadioPasses,
            isSaving = isSaving,
            isPlayStoreReady = isPlayStoreReady,
            isRadioPlaySkuReady = viewModel.isRadioProductReadyInPlay(),
            onLaunchGooglePlayRadio = {
                hostActivity?.let { viewModel.launchGoogleRadioPurchase(it) } ?: false
            },
            onConfirmPurchase = viewModel::purchaseRadioAccess,
            onOpenCreateStation = {
                viewModel.closeRadioAccessSheet()
                requestedLiveSubMode = 1
                liveSubModeRequestTick++
                navigateToTab(ROUTE_LIVE)
            },
            onDismiss = viewModel::closeRadioAccessSheet
        )
    }

    activeCallSession?.let { session ->
        CallOverlaySheet(
            currentUserId = user.uid,
            session = session,
            onToggleMute = { muted ->
                viewModel.updateActiveCallMedia(session.id, muted = muted, videoOn = session.callerVideoOn)
            },
            onToggleVideo = { videoOn ->
                viewModel.updateActiveCallMedia(session.id, muted = session.callerMuted, videoOn = videoOn)
            },
            onEndCall = { elapsedSec ->
                viewModel.endActiveCall(session, elapsedSec)
            }
        )
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
