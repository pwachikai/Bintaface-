package com.example.ui.viewmodel

import android.app.Activity
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdMobManager
import com.example.billing.BillingEvent
import com.example.billing.BillingManager
import com.example.billing.StoreOffer
import com.example.data.model.APP_ADMIN_EMAIL
import com.example.data.model.AdRevenueEvent
import com.example.data.model.AdminPayoutAccount
import com.example.data.model.AdminWithdrawal
import com.example.data.model.CallSession
import com.example.data.model.ChannelBroadcast
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.DEFAULT_ADMOB_BANNER_UNIT_ID
import com.example.data.model.DEFAULT_ADMOB_REWARDED_UNIT_ID
import com.example.data.model.DEFAULT_APP_DOWNLOAD_URL
import com.example.data.model.DEFAULT_USD_TO_NGN_RATE
import com.example.data.model.DirectMessage
import com.example.data.model.LevelUpgradeOrder
import com.example.data.model.LiveMessage
import com.example.data.model.LiveRoom
import com.example.data.model.MEMBERSHIP_LEVEL_TIERS
import com.example.data.model.MatchThread
import com.example.data.model.formatNgnWhole
import com.example.data.model.levelTierForId
import com.example.data.model.PagePost
import com.example.data.model.PayoutRequest
import com.example.data.model.PlatformSettings
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PublicProfile
import com.example.data.model.RADIO_ACCESS_FEE_NGN
import com.example.data.model.RadioBroadcast
import com.example.data.model.RadioChannel
import com.example.data.model.RadioPass
import com.example.data.model.RevenueLedgerEntry
import com.example.data.model.Shop
import com.example.data.model.ShopOrder
import com.example.data.model.ShopProduct
import com.example.data.model.SocialChannel
import com.example.data.model.SocialPage
import com.example.data.model.SparkGift
import com.example.data.model.SwipeRecord
import com.example.data.model.UserPrivate
import com.example.data.model.formatDualCurrency
import com.example.data.model.formatUsdCents
import com.example.data.model.isDesignatedAdminEmail
import com.example.data.model.sortedMatchId
import com.example.data.repository.*
import com.example.data.util.unwrapFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class KindredViewModel(
    private val repository: KindredRepository,
    val currentUserId: String,
    val currentUserEmail: String? = null,
    private val billingManager: BillingManager,
    private val adMobManager: AdMobManager? = null
) : ViewModel() {

    private fun friendlyErrorMessage(error: Throwable?, fallbackMsg: String): String {
        val root = error?.unwrapFirestoreException() ?: error
        val raw = root?.localizedMessage?.trim().orEmpty()
        return when {
            raw.contains("PERMISSION_DENIED", ignoreCase = true) ->
                "Access denied for this action. Make sure your account is signed in and authorized."
            raw.contains("UNAUTHENTICATED", ignoreCase = true) ->
                "Please sign in with Google to continue."
            raw.contains("UNAVAILABLE", ignoreCase = true) ||
                raw.contains("DEADLINE_EXCEEDED", ignoreCase = true) ||
                raw.contains("network", ignoreCase = true) ->
                "Network connection unavailable. Please check your internet and try again."
            raw.contains("FAILED_PRECONDITION", ignoreCase = true) ->
                "Action could not be completed in the current state. Please refresh and try again."
            raw.contains("ABORTED", ignoreCase = true) ->
                "A concurrent update occurred. Please try again."
            raw.isNotBlank() && !raw.startsWith("com.google.") -> raw
            else -> fallbackMsg
        }
    }

    private fun <T> Flow<T>.toUiStateFlow(
        tag: String,
        fallbackMsg: String
    ): StateFlow<UiState<T>> =
        map<T, UiState<T>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = error.unwrapFirestoreException()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(tag, fallbackMsg, rootError)
                emit(UiState.Error(friendlyErrorMessage(rootError, fallbackMsg)))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val myProfile: StateFlow<UiState<PublicProfile?>> =
        repository.observeMyPublicProfile(currentUserId)
            .toUiStateFlow(TAG, "Failed to load profile")

    val privateSettings: StateFlow<UserPrivate?> =
        repository.observePrivateSettings(currentUserId)
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                Log.w(TAG, "Failed to load private settings", error)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null
            )

    val offers: StateFlow<List<StoreOffer>> = billingManager.offers
    val isPlayStoreReady: StateFlow<Boolean> = billingManager.isPlayStoreReady
    val billingNotice: StateFlow<String?> = billingManager.billingNotice

    val isPlusMember: StateFlow<Boolean> =
        combine(privateSettings, myProfile) { priv, pubState ->
            val pub = (pubState as? UiState.Success)?.data
            (priv?.isPlusMember == true) || (pub?.isPlusMember == true)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = false
        )

    val currentVipLevel: StateFlow<String> =
        combine(privateSettings, myProfile) { priv, pubState ->
            val pub = (pubState as? UiState.Success)?.data
            val privRank = priv?.vipLevelRank ?: 0L
            val pubRank = pub?.vipLevelRank ?: 0L
            val privLevel = priv?.vipLevel?.takeIf { it.isNotBlank() && it != "none" }
            val pubLevel = pub?.vipLevel?.takeIf { it.isNotBlank() && it != "none" }
            when {
                privRank >= pubRank && privLevel != null -> privLevel
                pubLevel != null -> pubLevel
                else -> "none"
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = "none"
        )

    val currentVipLevelRank: StateFlow<Int> =
        currentVipLevel.map { levelTierForId(it)?.rank ?: 0 }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = 0
            )

    val myLevelUpgrades: StateFlow<List<LevelUpgradeOrder>> =
        repository.observeMyLevelUpgrades(currentUserId)
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                Log.w(TAG, "Failed to observe level upgrades", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val hasRadioAccess: StateFlow<Boolean> =
        combine(privateSettings, myProfile) { priv, pubState ->
            val pub = (pubState as? UiState.Success)?.data
            (priv?.hasRadioAccess == true) || (pub?.hasRadioAccess == true)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = false
        )

    val myRadioPasses: StateFlow<List<RadioPass>> =
        repository.observeMyRadioPasses(currentUserId)
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                Log.w(TAG, "Failed to observe radio passes", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val superlikesBalance: StateFlow<Int> =
        privateSettings.map { (it?.superlikesBalance ?: 1L).toInt().coerceAtLeast(0) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = 1
            )

    val boostsBalance: StateFlow<Int> =
        privateSettings.map { (it?.boostsBalance ?: 0L).toInt().coerceAtLeast(0) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = 0
            )

    val sparksBalance: StateFlow<Int> =
        privateSettings.map { (it?.sparksBalance ?: 0L).toInt().coerceAtLeast(0) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = 0
            )

    val isDesignatedAdmin: Boolean = isDesignatedAdminEmail(currentUserEmail)

    val platformSettings: StateFlow<PlatformSettings?> =
        repository.observePlatformSettings()
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                Log.w(TAG, "Failed to observe platform settings", error)
                emit(null)
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = null
            )

    val isAdmin: StateFlow<Boolean> =
        platformSettings.map {
            isDesignatedAdmin ||
                (it?.adminUid == currentUserId && isDesignatedAdminEmail(it.adminEmail))
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = isDesignatedAdmin
            )

    val usdToNgnRate: StateFlow<Long> =
        platformSettings.map { (it?.usdToNgnRate ?: DEFAULT_USD_TO_NGN_RATE).coerceIn(100L, 10000L) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DEFAULT_USD_TO_NGN_RATE
            )

    val appDownloadUrl: StateFlow<String> =
        platformSettings.map { com.example.data.model.normalizeBintafaceAccessUrl(it?.appDownloadUrl) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DEFAULT_APP_DOWNLOAD_URL
            )

    val admobBannerUnitId: StateFlow<String> =
        platformSettings.map { it?.admobBannerUnitId?.takeIf { id -> id.isNotBlank() } ?: DEFAULT_ADMOB_BANNER_UNIT_ID }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DEFAULT_ADMOB_BANNER_UNIT_ID
            )

    val admobRewardedUnitId: StateFlow<String> =
        platformSettings.map { it?.admobRewardedUnitId?.takeIf { id -> id.isNotBlank() } ?: DEFAULT_ADMOB_REWARDED_UNIT_ID }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = DEFAULT_ADMOB_REWARDED_UNIT_ID
            )

    val adImpressionsCount: StateFlow<Int> =
        adMobManager?.adImpressionsCount ?: MutableStateFlow(0).asStateFlow()

    val lastAdEventStatus: StateFlow<String?> =
        adMobManager?.lastAdEventStatus ?: MutableStateFlow<String?>(null).asStateFlow()

    val isRewardedAdReady: StateFlow<Boolean> =
        adMobManager?.isRewardedAdReady ?: MutableStateFlow(false).asStateFlow()

    val receivedSparkGifts: StateFlow<List<SparkGift>> =
        repository.observeReceivedSparkGifts(currentUserId)
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                Log.w(TAG, "Failed to observe received sparks", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val creatorAdRevenueEvents: StateFlow<List<AdRevenueEvent>> =
        repository.observeCreatorAdRevenue(currentUserId)
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                Log.w(TAG, "Failed to observe creator ad revenue", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val myPayoutRequests: StateFlow<List<PayoutRequest>> =
        repository.observeMyPayoutRequests(currentUserId)
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                Log.w(TAG, "Failed to observe payout requests", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val creatorSparkEarningsCents: StateFlow<Long> =
        receivedSparkGifts.map { gifts -> gifts.sumOf { it.creatorShareCents } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = 0L
            )

    val creatorAdEarningsCents: StateFlow<Long> =
        creatorAdRevenueEvents.map { events -> events.sumOf { it.creatorShareCents } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = 0L
            )

    val myBuyerOrders: StateFlow<List<ShopOrder>> =
        repository.observeMyBuyerOrders(currentUserId)
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                Log.w(TAG, "Failed to observe buyer orders", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val mySellerOrders: StateFlow<List<ShopOrder>> =
        repository.observeMySellerOrders(currentUserId)
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                Log.w(TAG, "Failed to observe seller orders", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val creatorShopEarningsCents: StateFlow<Long> =
        mySellerOrders.map { orders ->
            orders.filter { it.status != "cancelled" }.sumOf { it.sellerShareCents }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0L
        )

    val creatorTotalEarnedCents: StateFlow<Long> =
        combine(creatorSparkEarningsCents, creatorAdEarningsCents, creatorShopEarningsCents) { spark, ad, shop ->
            spark + ad + shop
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0L
        )

    val creatorPaidOutCents: StateFlow<Long> =
        myPayoutRequests.map { list -> list.filter { it.status == "paid" }.sumOf { it.amountCents } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = 0L
            )

    val creatorPendingCents: StateFlow<Long> =
        myPayoutRequests.map { list -> list.filter { it.status == "pending" }.sumOf { it.amountCents } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = 0L
            )

    val creatorAvailableCents: StateFlow<Long> =
        combine(creatorTotalEarnedCents, creatorPaidOutCents, creatorPendingCents) { total, paid, pending ->
            (total - paid - pending).coerceAtLeast(0L)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0L
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminAllPayoutRequests: StateFlow<List<PayoutRequest>> =
        isAdmin.flatMapLatest { admin ->
            if (!admin) flowOf(emptyList())
            else repository.observeAllPayoutRequests().catch { emit(emptyList()) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminRevenueLedger: StateFlow<List<RevenueLedgerEntry>> =
        isAdmin.flatMapLatest { admin ->
            if (!admin) flowOf(emptyList())
            else repository.observeAllRevenueLedger().catch { emit(emptyList()) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminPayoutAccount: StateFlow<AdminPayoutAccount?> =
        isAdmin.flatMapLatest { admin ->
            if (!admin) flowOf(null)
            else repository.observeAdminPayoutAccount().catch { emit(null) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val adminWithdrawals: StateFlow<List<AdminWithdrawal>> =
        isAdmin.flatMapLatest { admin ->
            if (!admin) flowOf(emptyList())
            else repository.observeAdminWithdrawals().catch { emit(emptyList()) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList()
        )

    val adminTotalEarnedCents: StateFlow<Long> =
        adminRevenueLedger.map { list -> list.sumOf { it.adminShareCents } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = 0L
            )

    val adminWithdrawnCents: StateFlow<Long> =
        adminWithdrawals.map { list -> list.sumOf { it.amountCents } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = 0L
            )

    val adminAvailableCents: StateFlow<Long> =
        combine(adminTotalEarnedCents, adminWithdrawnCents) { total, withdrawn ->
            (total - withdrawn).coerceAtLeast(0L)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0L
        )

    val dailyLikesRemaining: StateFlow<Int> =
        privateSettings.map { priv ->
            if (priv?.isPlusMember == true) {
                Int.MAX_VALUE
            } else {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                val usedToday = if (priv?.dailyLikesDate == today) priv.dailyLikesUsed.toInt() else 0
                (FREE_DAILY_LIKES_LIMIT - usedToday).coerceAtLeast(0)
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = FREE_DAILY_LIKES_LIMIT
        )

    private val _showPaywall = MutableStateFlow(false)
    val showPaywall: StateFlow<Boolean> = _showPaywall.asStateFlow()

    private val _showWalletSheet = MutableStateFlow(false)
    val showWalletSheet: StateFlow<Boolean> = _showWalletSheet.asStateFlow()

    private val _showAdminSheet = MutableStateFlow(false)
    val showAdminSheet: StateFlow<Boolean> = _showAdminSheet.asStateFlow()

    private val _showLevelUpgradeSheet = MutableStateFlow(false)
    val showLevelUpgradeSheet: StateFlow<Boolean> = _showLevelUpgradeSheet.asStateFlow()

    private val _showRadioAccessSheet = MutableStateFlow(false)
    val showRadioAccessSheet: StateFlow<Boolean> = _showRadioAccessSheet.asStateFlow()

    private val _feedTagFilter = MutableStateFlow("All")
    val feedTagFilter: StateFlow<String> = _feedTagFilter.asStateFlow()

    private val allPostsFlow = repository.observeCommunityPosts()

    val postsState: StateFlow<UiState<List<Post>>> =
        combine(
            allPostsFlow,
            _feedTagFilter
        ) { posts, tagFilter ->
            when (tagFilter) {
                "All" -> posts
                "Following" -> posts // handled if needed; kept simple
                else -> posts.filter { it.tag == tagFilter || it.mediaType.equals(tagFilter, ignoreCase = true) }
            }
        }.toUiStateFlow(TAG, "Failed to load community feed")

    val reelsState: StateFlow<UiState<List<Post>>> =
        allPostsFlow.map { posts ->
            posts.filter { it.mediaType == "reel" || it.tag == "Reel" }
        }.toUiStateFlow(TAG, "Failed to load Reels")

    val videosState: StateFlow<UiState<List<Post>>> =
        allPostsFlow.map { posts ->
            posts.filter { it.mediaType == "video" || it.tag == "Video" }
        }.toUiStateFlow(TAG, "Failed to load Videos")

    val likedPostIds: StateFlow<Set<String>> =
        repository.observeMyPostLikes()
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                Log.w(TAG, "Failed to load post likes", error)
                emit(emptySet())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptySet()
            )

    val repostedPostIds: StateFlow<Set<String>> =
        repository.observeMyPostReposts()
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                emit(emptySet())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptySet()
            )

    val followedUserIds: StateFlow<Set<String>> =
        repository.observeMyFollows()
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                emit(emptySet())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptySet()
            )

    // --- BIGO Live Rooms ---
    val liveRoomsState: StateFlow<UiState<List<LiveRoom>>> =
        repository.observeLiveRooms()
            .toUiStateFlow(TAG, "Failed to load live rooms")

    private val _activeLiveRoomId = MutableStateFlow<String?>(null)
    val activeLiveRoomId: StateFlow<String?> = _activeLiveRoomId.asStateFlow()

    val activeLiveRoom: StateFlow<LiveRoom?> =
        combine(liveRoomsState, _activeLiveRoomId) { state, roomId ->
            if (roomId == null) null
            else (state as? UiState.Success)?.data?.firstOrNull { it.id == roomId }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeLiveMessages: StateFlow<UiState<List<LiveMessage>>> =
        _activeLiveRoomId.flatMapLatest { roomId ->
            if (roomId == null) flowOf(UiState.Success(emptyList()))
            else repository.observeLiveMessages(roomId)
                .map<List<LiveMessage>, UiState<List<LiveMessage>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load live chat"))
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    // --- Telegram Broadcast Channels ---
    val channelsState: StateFlow<UiState<List<SocialChannel>>> =
        repository.observeChannels()
            .toUiStateFlow(TAG, "Failed to load channels")

    val subscribedChannelIds: StateFlow<Set<String>> =
        repository.observeMyChannelSubscriptions()
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                emit(emptySet())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptySet()
            )

    private val _activeChannelId = MutableStateFlow<String?>(null)
    val activeChannelId: StateFlow<String?> = _activeChannelId.asStateFlow()

    val activeChannel: StateFlow<SocialChannel?> =
        combine(channelsState, _activeChannelId) { state, channelId ->
            if (channelId == null) null
            else (state as? UiState.Success)?.data?.firstOrNull { it.id == channelId }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeChannelBroadcasts: StateFlow<UiState<List<ChannelBroadcast>>> =
        _activeChannelId.flatMapLatest { channelId ->
            if (channelId == null) flowOf(UiState.Success(emptyList()))
            else repository.observeChannelBroadcasts(channelId)
                .map<List<ChannelBroadcast>, UiState<List<ChannelBroadcast>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load broadcasts"))
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    // --- Facebook-Style User Pages ---
    val pagesState: StateFlow<UiState<List<SocialPage>>> =
        repository.observePages()
            .toUiStateFlow(TAG, "Failed to load pages")

    val followedPageIds: StateFlow<Set<String>> =
        repository.observeMyPageFollows()
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                emit(emptySet())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptySet()
            )

    val likedPagePostIds: StateFlow<Set<String>> =
        repository.observeMyPagePostLikes()
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                emit(emptySet())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptySet()
            )

    private val _activePageId = MutableStateFlow<String?>(null)
    val activePageId: StateFlow<String?> = _activePageId.asStateFlow()

    val activePage: StateFlow<SocialPage?> =
        combine(pagesState, _activePageId) { state, pageId ->
            if (pageId == null) null
            else (state as? UiState.Success)?.data?.firstOrNull { it.id == pageId }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activePagePosts: StateFlow<UiState<List<PagePost>>> =
        _activePageId.flatMapLatest { pageId ->
            if (pageId == null) flowOf(UiState.Success(emptyList()))
            else repository.observePagePosts(pageId)
                .map<List<PagePost>, UiState<List<PagePost>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load page posts"))
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    // --- WhatsApp Direct & Group Conversations ---
    val conversationsState: StateFlow<UiState<List<Conversation>>> =
        repository.observeConversations()
            .toUiStateFlow(TAG, "Failed to load conversations")

    private val _activeConversationId = MutableStateFlow<String?>(null)
    val activeConversationId: StateFlow<String?> = _activeConversationId.asStateFlow()

    val activeConversation: StateFlow<Conversation?> =
        combine(conversationsState, _activeConversationId) { state, convId ->
            if (convId == null) null
            else (state as? UiState.Success)?.data?.firstOrNull { it.id == convId }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeDirectMessages: StateFlow<UiState<List<DirectMessage>>> =
        _activeConversationId.flatMapLatest { convId ->
            if (convId == null) flowOf(UiState.Success(emptyList()))
            else repository.observeDirectMessages(convId)
                .map<List<DirectMessage>, UiState<List<DirectMessage>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load messages"))
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    // --- BINTAFACE Voice & Video Call Signaling ---
    val callSessions: StateFlow<List<CallSession>> =
        repository.observeMyCallSessions()
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                Log.w(TAG, "Failed to observe call sessions", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    private val _activeCallId = MutableStateFlow<String?>(null)

    val incomingRingingCall: StateFlow<CallSession?> =
        combine(callSessions, _activeCallId) { list, activeId ->
            list.firstOrNull { session ->
                session.status == "ringing" &&
                    session.callerId != currentUserId &&
                    session.id != activeId
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    val activeCallSession: StateFlow<CallSession?> =
        combine(callSessions, _activeCallId) { list, activeId ->
            if (activeId == null) null
            else list.firstOrNull { it.id == activeId && it.status in setOf("ringing", "active") }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    // --- BINTAFACE Shops Marketplace ---
    val shopsState: StateFlow<UiState<List<Shop>>> =
        repository.observeShops()
            .toUiStateFlow(TAG, "Failed to load shops")

    private val _activeShopId = MutableStateFlow<String?>(null)
    val activeShopId: StateFlow<String?> = _activeShopId.asStateFlow()

    val activeShop: StateFlow<Shop?> =
        combine(shopsState, _activeShopId) { state, shopId ->
            if (shopId == null) null
            else (state as? UiState.Success)?.data?.firstOrNull { it.id == shopId }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeShopProducts: StateFlow<UiState<List<ShopProduct>>> =
        _activeShopId.flatMapLatest { shopId ->
            if (shopId == null) flowOf(UiState.Success(emptyList()))
            else repository.observeShopProducts(shopId)
                .map<List<ShopProduct>, UiState<List<ShopProduct>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load products"))
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    // --- BINTAFACE Global Radio Stations (For Billions of Listeners) ---
    val radioChannelsState: StateFlow<UiState<List<RadioChannel>>> =
        repository.observeRadioChannels()
            .toUiStateFlow(TAG, "Failed to load global radio stations")

    val subscribedRadioChannelIds: StateFlow<Set<String>> =
        repository.observeMyRadioSubscriptions()
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                emit(emptySet())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptySet()
            )

    private val _activeRadioChannelId = MutableStateFlow<String?>(null)
    val activeRadioChannelId: StateFlow<String?> = _activeRadioChannelId.asStateFlow()

    val activeRadioChannel: StateFlow<RadioChannel?> =
        combine(radioChannelsState, _activeRadioChannelId) { state, channelId ->
            if (channelId == null) null
            else (state as? UiState.Success)?.data?.firstOrNull { it.id == channelId }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeRadioBroadcasts: StateFlow<UiState<List<RadioBroadcast>>> =
        _activeRadioChannelId.flatMapLatest { channelId ->
            if (channelId == null) flowOf(UiState.Success(emptyList()))
            else repository.observeRadioBroadcasts(channelId)
                .map<List<RadioBroadcast>, UiState<List<RadioBroadcast>>> { UiState.Success(it) }
                .catch { error ->
                    val firestoreError = error.unwrapFirestoreException()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load broadcasts"))
                }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    // --- Discover & Matches ---
    private val _discoverFilter = MutableStateFlow("all")
    val discoverFilter: StateFlow<String> = _discoverFilter.asStateFlow()

    val allDiscoverableProfiles: StateFlow<List<PublicProfile>> =
        repository.observeDiscoverableProfiles()
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                Log.w(TAG, "Failed to observe discoverable profiles", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    private val mySwipes: StateFlow<List<SwipeRecord>> =
        repository.observeMySwipes()
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                Log.w(TAG, "Failed to observe swipes", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val incomingLikes: StateFlow<List<SwipeRecord>> =
        combine(
            repository.observeIncomingLikes(),
            mySwipes
        ) { likes, swiped ->
            val alreadyDecidedIds = swiped.map { it.toUserId }.toSet()
            likes.filter { it.fromUserId !in alreadyDecidedIds }
        }
            .catch { error ->
                if (error.unwrapFirestoreException() == null && error is CancellationException) throw error
                Log.w(TAG, "Failed to observe incoming likes", error)
                emit(emptyList())
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = emptyList()
            )

    val vipLevelByUserId: StateFlow<Map<String, String>> =
        combine(allDiscoverableProfiles, currentVipLevel) { profiles, myLevel ->
            buildMap {
                profiles.forEach { profile ->
                    if (profile.vipLevel != "none" && profile.vipLevelRank > 0L) {
                        put(profile.userId, profile.vipLevel)
                    }
                }
                if (myLevel != "none") {
                    put(currentUserId, myLevel)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyMap()
        )

    val discoverDeck: StateFlow<UiState<List<PublicProfile>>> =
        combine(
            repository.observeDiscoverableProfiles(),
            mySwipes,
            _discoverFilter
        ) { profiles, swipes, filter ->
            val swipedTargetIds = swipes.map { it.toUserId }.toSet()
            profiles.filter { profile ->
                profile.userId != currentUserId &&
                    profile.userId !in swipedTargetIds &&
                    (filter == "all" || profile.lookingFor == filter || profile.lookingFor == "both")
            }.sortedWith(
                compareByDescending<PublicProfile> { it.isBoosted }
                    .thenByDescending { it.vipLevelRank }
                    .thenByDescending { it.isPlusMember }
            )
        }.toUiStateFlow(TAG, "Failed to load discovery deck")

    val matchesState: StateFlow<UiState<List<MatchThread>>> =
        repository.observeMatches()
            .toUiStateFlow(TAG, "Failed to load matches")

    private val _activeCommentPostId = MutableStateFlow<String?>(null)
    val activeCommentPostId: StateFlow<String?> = _activeCommentPostId.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeComments: StateFlow<UiState<List<PostComment>>> =
        _activeCommentPostId.flatMapLatest { postId ->
            if (postId == null) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeComments(postId)
                    .map<List<PostComment>, UiState<List<PostComment>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = error.unwrapFirestoreException()
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load comments"))
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Success(emptyList())
        )

    private val _activeChatMatchId = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMatch: StateFlow<MatchThread?> =
        _activeChatMatchId.flatMapLatest { matchId ->
            if (matchId == null) flowOf(null)
            else repository.observeMatch(matchId).catch { emit(null) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = null
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeMessages: StateFlow<UiState<List<ChatMessage>>> =
        _activeChatMatchId.flatMapLatest { matchId ->
            if (matchId == null) {
                flowOf(UiState.Success(emptyList()))
            } else {
                repository.observeMessages(matchId)
                    .map<List<ChatMessage>, UiState<List<ChatMessage>>> { UiState.Success(it) }
                    .catch { error ->
                        val firestoreError = error.unwrapFirestoreException()
                        if (firestoreError == null && error is CancellationException) throw error
                        emit(UiState.Error((firestoreError ?: error).localizedMessage ?: "Failed to load messages"))
                    }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    private val _newMatchPopup = MutableStateFlow<Pair<PublicProfile, String>?>(null)
    val newMatchPopup: StateFlow<Pair<PublicProfile, String>?> = _newMatchPopup.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val swipesInFlight = mutableSetOf<String>()
    private val autoPayoutMutex = Mutex()
    private var lastAutoPayoutAvailableCents: Long = 0L

    init {
        if (isDesignatedAdmin) {
            viewModelScope.launch {
                repository.ensureAdminPlatformClaimed(currentUserEmail)
            }
        }
        viewModelScope.launch {
            admobRewardedUnitId.collect { unitId ->
                adMobManager?.preloadRewardedAd(unitId)
            }
        }
        viewModelScope.launch {
            combine(
                privateSettings,
                platformSettings,
                creatorAvailableCents
            ) { priv, plat, avail ->
                Triple(priv, plat, avail)
            }.collect { (priv, plat, avail) ->
                val userAuto = priv?.autoPayoutEnabled == true
                val platformAuto = plat?.autoPayoutEnabled ?: true
                val account = priv?.payoutAccount?.trim().orEmpty()
                val method = priv?.payoutMethod?.trim().orEmpty()
                if (userAuto && platformAuto && avail >= 100L && account.length >= 3 && method.isNotEmpty() && avail != lastAutoPayoutAvailableCents) {
                    autoPayoutMutex.withLock {
                        if (creatorAvailableCents.value >= 100L && avail != lastAutoPayoutAvailableCents) {
                            lastAutoPayoutAvailableCents = avail
                            val rate = (plat?.usdToNgnRate ?: DEFAULT_USD_TO_NGN_RATE).coerceIn(100L, 10000L)
                            val currency = priv?.payoutCurrency?.takeIf { it.isNotBlank() } ?: when (method) {
                                "crypto" -> "CRYPTO"
                                "ngn_bank" -> "NGN"
                                else -> "USD"
                            }
                            repository.requestCreatorPayout(
                                userName = currentDisplayName(),
                                amountCents = avail,
                                payoutMethod = method,
                                payoutAccount = account,
                                payoutCurrency = currency,
                                payoutBankOrNetwork = priv?.payoutBankOrNetwork.orEmpty(),
                                isAutoPayout = true,
                                usdToNgnRate = rate
                            ).onSuccess {
                                _statusMessage.value =
                                    "YouTube-style Auto-Payout queued ${formatDualCurrency(avail, rate)} → $method"
                            }
                        }
                    }
                } else if (avail < 100L) {
                    lastAutoPayoutAvailableCents = 0L
                }
            }
        }
        viewModelScope.launch {
            billingManager.events.collect { event ->
                when (event) {
                    is BillingEvent.SubscriptionActivated -> {
                        repository.syncSubscriptionEntitlement(
                            productId = event.productId,
                            purchaseToken = event.purchaseToken,
                            active = true
                        ).onSuccess {
                            _statusMessage.value = "Welcome to Bintaface+! All perks unlocked."
                            _showPaywall.value = false
                        }.onFailure {
                            _statusMessage.value = friendlyErrorMessage(it, "Failed to sync Bintaface+ membership")
                        }
                    }
                    is BillingEvent.ConsumableGranted -> {
                        repository.grantConsumablePurchase(
                            productId = event.productId,
                            quantity = event.quantity,
                            purchaseToken = event.purchaseToken
                        ).onSuccess {
                            _statusMessage.value = "Purchase added to your BINTAFACE balance!"
                            _showPaywall.value = false
                        }.onFailure {
                            _statusMessage.value = friendlyErrorMessage(it, "Failed to apply consumable purchase")
                        }
                    }
                    is BillingEvent.LevelUpgradePurchased -> {
                        val tier = MEMBERSHIP_LEVEL_TIERS.firstOrNull { it.rank == event.levelRank }
                        if (tier != null) {
                            repository.upgradeUserLevelDirect(
                                levelId = tier.id,
                                currency = "NGN",
                                paymentMethod = "google_play",
                                paymentReference = event.purchaseToken,
                                payerName = currentDisplayName(),
                                usdToNgnRate = usdToNgnRate.value,
                                googleConfirmed = true
                            ).onSuccess { order ->
                                _statusMessage.value =
                                    "🎉 Google confirmed payment! Level ${order.levelRank} (${order.levelName}) badge is now ACTIVE on your profile."
                            }.onFailure { error ->
                                _statusMessage.value = friendlyErrorMessage(
                                    error,
                                    "Google confirmed payment, but badge activation could not finish"
                                )
                            }
                        }
                    }
                    is BillingEvent.RadioAccessPurchased -> {
                        repository.purchaseRadioAccessDirect(
                            currency = "NGN",
                            paymentMethod = "google_play",
                            paymentReference = event.purchaseToken,
                            payerName = currentDisplayName(),
                            usdToNgnRate = usdToNgnRate.value,
                            googleConfirmed = true
                        ).onSuccess {
                            _statusMessage.value =
                                "📻 Google confirmed ₦2,000 Radio Pass! Global Radio & channel creation are now ACTIVE."
                            _showRadioAccessSheet.value = false
                        }.onFailure { error ->
                            _statusMessage.value = friendlyErrorMessage(
                                error,
                                "Google confirmed payment, but Radio Access could not finish"
                            )
                        }
                    }
                    is BillingEvent.PendingPurchase -> {
                        _statusMessage.value = "Waiting for Google confirmation before activating (${event.productId})."
                    }
                    is BillingEvent.Message -> {
                        _statusMessage.value = event.text
                    }
                }
            }
        }
    }

    private fun currentDisplayName(): String =
        (myProfile.value as? UiState.Success)?.data?.displayName?.ifBlank { "Member" } ?: "Member"

    private fun currentAvatarKey(): String =
        (myProfile.value as? UiState.Success)?.data?.avatarKey ?: "coffee"

    fun openPaywall() {
        _showPaywall.value = true
        billingManager.refreshPurchases()
    }

    fun closePaywall() {
        _showPaywall.value = false
    }

    fun openWalletSheet() {
        _showWalletSheet.value = true
    }

    fun closeWalletSheet() {
        _showWalletSheet.value = false
    }

    fun openAdminSheet() {
        _showAdminSheet.value = true
    }

    fun closeAdminSheet() {
        _showAdminSheet.value = false
    }

    fun openLevelUpgradeSheet() {
        _showLevelUpgradeSheet.value = true
    }

    fun closeLevelUpgradeSheet() {
        _showLevelUpgradeSheet.value = false
    }

    fun openRadioAccessSheet() {
        _showRadioAccessSheet.value = true
    }

    fun closeRadioAccessSheet() {
        _showRadioAccessSheet.value = false
    }

    fun isRadioProductReadyInPlay(): Boolean =
        billingManager.isRadioProductReadyInPlay()

    fun launchGoogleRadioPurchase(activity: Activity): Boolean =
        billingManager.launchRadioAccessPurchase(activity, currentUserId)

    fun isLevelProductReadyInPlay(rank: Int): Boolean =
        billingManager.isLevelProductReadyInPlay(rank)

    fun launchGoogleLevelUpgradePurchase(activity: Activity, rank: Int): Boolean =
        billingManager.launchLevelUpgradePurchase(activity, rank, currentUserId)

    fun upgradeUserLevel(
        levelId: String,
        currency: String,
        paymentMethod: String,
        paymentReference: String,
        googleConfirmed: Boolean = true
    ) {
        if (_isSaving.value) return
        if (!googleConfirmed) {
            _statusMessage.value = "Google confirmation is required before payment & badge activation."
            return
        }
        val myName = currentDisplayName()
        val rate = usdToNgnRate.value
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.upgradeUserLevelDirect(
                levelId = levelId,
                currency = currency,
                paymentMethod = paymentMethod,
                paymentReference = paymentReference,
                payerName = myName,
                usdToNgnRate = rate,
                googleConfirmed = googleConfirmed
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { order ->
                    val amountLabel = if (order.currency == "NGN") {
                        formatNgnWhole(order.ngnAmount)
                    } else {
                        formatUsdCents(order.amountCents)
                    }
                    _statusMessage.value =
                        "🎉 Google confirmed! Level ${order.levelRank} (${order.levelName}) badge is now ACTIVE on your profile ($amountLabel → ${order.adminBankLabel})."
                },
                onFailure = { error ->
                    _statusMessage.value = friendlyErrorMessage(error, "Couldn't complete level upgrade")
                }
            )
        }
    }

    fun launchPurchase(activity: Activity, productId: String) {
        billingManager.launchPurchase(activity, productId, currentUserId)
    }

    fun restorePurchases() {
        billingManager.refreshPurchases()
        _statusMessage.value = "Checking Google Play for active purchases…"
    }

    fun sendSparkGift(post: Post) {
        sendSparkGiftToTarget(
            targetId = post.id,
            targetType = "post",
            recipientId = post.authorId,
            recipientName = post.authorName
        )
    }

    fun sendSparkToLiveRoom(room: LiveRoom) {
        sendSparkGiftToTarget(
            targetId = room.id,
            targetType = "live",
            recipientId = room.hostId,
            recipientName = room.hostName
        ) {
            viewModelScope.launch {
                repository.sendLiveMessage(
                    roomId = room.id,
                    senderName = currentDisplayName(),
                    senderAvatarKey = currentAvatarKey(),
                    text = "✨ Sent a $1.00 Spark Gift to ${room.hostName}!",
                    isGiftNotice = true
                )
            }
        }
    }

    fun sendSparkToChannel(channel: SocialChannel) {
        sendSparkGiftToTarget(
            targetId = channel.id,
            targetType = "channel",
            recipientId = channel.ownerId,
            recipientName = channel.ownerName
        )
    }

    fun sendSparkToPage(page: SocialPage) {
        sendSparkGiftToTarget(
            targetId = page.id,
            targetType = "page",
            recipientId = page.ownerId,
            recipientName = page.name
        )
    }

    private fun sendSparkGiftToTarget(
        targetId: String,
        targetType: String,
        recipientId: String,
        recipientName: String,
        onGiftSuccess: () -> Unit = {}
    ) {
        if (recipientId == currentUserId) {
            _statusMessage.value = "You can't send a Spark Gift to yourself."
            return
        }
        if (sparksBalance.value <= 0) {
            _statusMessage.value = "Get a 5 Creator Sparks Pack to reward creators (80% creator, 20% app)."
            openPaywall()
            return
        }
        val myName = currentDisplayName()
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.sendSparkGiftToTarget(
                targetId = targetId,
                targetType = targetType,
                recipientId = recipientId,
                recipientName = recipientName,
                senderName = myName
            )
            _isSaving.value = false
            res.fold(
                onSuccess = {
                    onGiftSuccess()
                    _statusMessage.value = "Sent $1.00 Spark to $recipientName ($0.80 to creator, $0.20 app fee)!"
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't send Spark Gift")
                }
            )
        }
    }

    fun showStatusNotice(message: String) {
        _statusMessage.value = message
    }

    fun onGoogleAdPaid(
        adFormat: String,
        adUnitId: String,
        valueMicros: Long,
        currencyCode: String,
        creatorId: String = "",
        creatorName: String = "",
        postId: String = "feed_banner"
    ) {
        if (adFormat == "banner") {
            adMobManager?.recordBannerImpression(valueMicros, currencyCode)
        }
        if (valueMicros <= 0L) return
        viewModelScope.launch {
            repository.recordGoogleAdRevenue(
                adFormat = adFormat,
                adUnitId = adUnitId,
                valueMicros = valueMicros,
                currencyCode = currencyCode,
                creatorId = creatorId,
                creatorName = creatorName,
                postId = postId
            ).onSuccess { event ->
                if (event != null) {
                    val rate = usdToNgnRate.value
                    _statusMessage.value = if (event.creatorShareCents > 0L) {
                        "Google Ad earned ${formatDualCurrency(event.grossCents, rate)} (70% Admin / 30% ${event.creatorName.ifBlank { "Creator" }})"
                    } else {
                        "Google Ad earned ${formatDualCurrency(event.grossCents, rate)} (App Admin)"
                    }
                }
            }
        }
    }

    fun watchRewardedAdForCreator(
        activity: Activity?,
        creatorId: String,
        creatorName: String,
        postId: String
    ) {
        val manager = adMobManager
        if (manager == null) {
            _statusMessage.value = "AdMob SDK unavailable in this environment."
            return
        }
        manager.showRewardedAd(
            activity = activity,
            unitId = admobRewardedUnitId.value,
            creatorId = creatorId,
            creatorName = creatorName,
            postId = postId,
            onPaidMicros = { micros, currency, unitId, cId, cName, pId ->
                onGoogleAdPaid(
                    adFormat = "rewarded",
                    adUnitId = unitId,
                    valueMicros = micros,
                    currencyCode = currency,
                    creatorId = cId,
                    creatorName = cName,
                    postId = pId
                )
            },
            onRewardEarned = {
                _statusMessage.value =
                    "Watched Google Ad for $creatorName! Paid impressions split 70% Admin / 30% Creator."
            },
            onUnavailable = { msg ->
                _statusMessage.value = msg
            }
        )
    }

    fun savePayoutPreferences(
        method: String,
        account: String,
        bankOrNetwork: String = "",
        currency: String = "USD",
        autoPayoutEnabled: Boolean = true
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.savePayoutPreferences(
                payoutMethod = method,
                payoutAccount = account,
                payoutBankOrNetwork = bankOrNetwork,
                payoutCurrency = currency,
                autoPayoutEnabled = autoPayoutEnabled
            )
            _isSaving.value = false
            res.fold(
                onSuccess = {
                    _statusMessage.value = if (autoPayoutEnabled) {
                        "Payout destination saved ($currency) — Auto-Payout enabled like YouTube!"
                    } else {
                        "Payout destination saved ($currency)"
                    }
                },
                onFailure = { _statusMessage.value = friendlyErrorMessage(it, "Couldn't save payout destination") }
            )
        }
    }

    fun requestCreatorPayout(
        payoutMethod: String,
        payoutAccount: String,
        payoutCurrency: String = "USD",
        payoutBankOrNetwork: String = ""
    ) {
        if (_isSaving.value) return
        val available = creatorAvailableCents.value
        val rate = usdToNgnRate.value
        if (available < 100L) {
            _statusMessage.value = "Earn at least $1.00 (${formatDualCurrency(100L, rate)}) from Ads (30%) & Sparks (80%) before requesting payout."
            return
        }
        val myName = currentDisplayName()
        viewModelScope.launch {
            _isSaving.value = true
            repository.savePayoutPreferences(
                payoutMethod = payoutMethod,
                payoutAccount = payoutAccount,
                payoutBankOrNetwork = payoutBankOrNetwork,
                payoutCurrency = payoutCurrency,
                autoPayoutEnabled = privateSettings.value?.autoPayoutEnabled ?: true
            )
            val res = repository.requestCreatorPayout(
                userName = myName,
                amountCents = available,
                payoutMethod = payoutMethod,
                payoutAccount = payoutAccount,
                payoutCurrency = payoutCurrency,
                payoutBankOrNetwork = payoutBankOrNetwork,
                isAutoPayout = false,
                usdToNgnRate = rate
            )
            _isSaving.value = false
            res.fold(
                onSuccess = {
                    _statusMessage.value = "Payout request for ${formatDualCurrency(available, rate)} submitted ($payoutCurrency)!"
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't submit payout request")
                }
            )
        }
    }

    fun claimAdminRole(email: String?, payoutAccountLabel: String) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.claimAdminRole(email, payoutAccountLabel)
            _isSaving.value = false
            res.fold(
                onSuccess = {
                    _statusMessage.value = "App Admin verified for $APP_ADMIN_EMAIL!"
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Admin access is restricted to $APP_ADMIN_EMAIL")
                }
            )
        }
    }

    fun updatePlatformSettings(feePercent: Long, payoutAccountLabel: String) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.updatePlatformSettings(feePercent, payoutAccountLabel)
            _isSaving.value = false
            res.fold(
                onSuccess = { _statusMessage.value = "Admin treasury settings saved" },
                onFailure = { _statusMessage.value = friendlyErrorMessage(it, "Couldn't update settings") }
            )
        }
    }

    fun saveAdminPayoutAccount(
        payoutMethod: String,
        accountHolderName: String,
        bankName: String,
        accountNumber: String,
        routingOrSwift: String,
        payoutEmailOrPhone: String,
        platformFeePercent: Long,
        payoutCurrency: String = "NGN"
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.saveAdminPayoutAccount(
                payoutMethod = payoutMethod,
                accountHolderName = accountHolderName,
                bankName = bankName,
                accountNumber = accountNumber,
                routingOrSwift = routingOrSwift,
                payoutEmailOrPhone = payoutEmailOrPhone,
                platformFeePercent = platformFeePercent,
                payoutCurrency = payoutCurrency
            )
            _isSaving.value = false
            res.fold(
                onSuccess = {
                    _statusMessage.value = "Admin $payoutCurrency receiving account saved! 70% AdMob + app revenue routes here."
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't save admin receiving account")
                }
            )
        }
    }

    fun updateAdminAdAndDownloadConfig(
        usdToNgnRate: Long,
        autoPayoutEnabled: Boolean,
        appDownloadUrl: String,
        bannerUnitId: String,
        rewardedUnitId: String
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.updateAdminAdAndDownloadConfig(
                usdToNgnRate = usdToNgnRate,
                autoPayoutEnabled = autoPayoutEnabled,
                appDownloadUrl = appDownloadUrl,
                admobBannerUnitId = bannerUnitId,
                admobRewardedUnitId = rewardedUnitId
            )
            _isSaving.value = false
            res.fold(
                onSuccess = {
                    adMobManager?.preloadRewardedAd(rewardedUnitId)
                    _statusMessage.value = "Updated AdMob units, ₦/$usdToNgnRate rate, and BINTAFACE download link!"
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't update BINTAFACE settings")
                }
            )
        }
    }

    fun withdrawAdminRevenue() {
        if (_isSaving.value) return
        val available = adminAvailableCents.value
        val rate = usdToNgnRate.value
        if (available < 100L) {
            _statusMessage.value = "Minimum withdrawal is $1.00 (${formatDualCurrency(100L, rate)}) of accumulated app revenue."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.withdrawAdminRevenue(available, rate)
            _isSaving.value = false
            res.fold(
                onSuccess = { receipt ->
                    _statusMessage.value =
                        "Transferred ${formatDualCurrency(receipt.amountCents, rate)} [${receipt.payoutCurrency}] to ${receipt.bankName} ${receipt.maskedAccountNumber} (${receipt.referenceCode})"
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't withdraw admin revenue")
                }
            )
        }
    }

    fun settlePayoutRequest(requestId: String, approved: Boolean, reference: String, note: String) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.settlePayoutRequest(requestId, approved, reference, note)
            _isSaving.value = false
            res.fold(
                onSuccess = {
                    _statusMessage.value = if (approved) "Creator payout marked as Paid!" else "Payout request declined."
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't update payout request")
                }
            )
        }
    }

    fun processAutoPayoutBatch() {
        val pending = adminAllPayoutRequests.value.filter { it.status == "pending" }
        if (pending.isEmpty()) {
            _statusMessage.value = "No pending creator payouts in the auto-payout queue."
            return
        }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.processAutoPayoutBatch(pending)
            _isSaving.value = false
            res.fold(
                onSuccess = { count ->
                    _statusMessage.value = "Processed $count YouTube-style creator payouts across USD, NGN & Crypto!"
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't batch settle payouts")
                }
            )
        }
    }

    fun activateBoost() {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.activateProfileBoost()
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    _statusMessage.value = "Spotlight Boost activated! Your card is prioritized across BINTAFACE."
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't activate Spotlight Boost")
                    if (boostsBalance.value <= 0) {
                        _showPaywall.value = true
                    }
                }
            )
        }
    }

    fun setFeedTagFilter(tag: String) {
        _feedTagFilter.value = tag
    }

    fun setDiscoverFilter(filter: String) {
        _discoverFilter.value = filter
    }

    fun openComments(postId: String) {
        _activeCommentPostId.value = postId
    }

    fun closeComments() {
        _activeCommentPostId.value = null
    }

    fun openChat(matchId: String) {
        _activeChatMatchId.value = matchId
    }

    fun closeChat() {
        _activeChatMatchId.value = null
    }

    fun dismissMatchPopup() {
        _newMatchPopup.value = null
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun findDiscoverableProfile(userId: String): PublicProfile? =
        allDiscoverableProfiles.value.firstOrNull { it.userId == userId }

    fun saveProfile(
        displayName: String,
        age: Int,
        city: String,
        bio: String,
        lookingFor: String,
        promptQuestion: String,
        promptAnswer: String,
        interests: List<String>,
        avatarKey: String,
        photoBase64: String?,
        isDiscoverable: Boolean,
        preferredIntent: String,
        email: String?,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveProfile(
                displayName = displayName,
                age = age,
                city = city,
                bio = bio,
                lookingFor = lookingFor,
                promptQuestion = promptQuestion,
                promptAnswer = promptAnswer,
                interests = interests,
                avatarKey = avatarKey,
                photoBase64 = photoBase64,
                isDiscoverable = isDiscoverable,
                preferredIntent = preferredIntent,
                email = email
            )
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    _statusMessage.value = "Profile saved"
                    onSaved()
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't save profile")
                }
            )
        }
    }

    fun createPost(
        content: String,
        tag: String,
        imageKey: String?,
        postPhotoBase64: String?,
        mediaType: String = "post",
        title: String = "",
        durationLabel: String = "",
        audioTrack: String = "",
        onPublished: () -> Unit = {}
    ) {
        val profile = (myProfile.value as? UiState.Success)?.data
        val authorName = profile?.displayName?.ifBlank { "Member" } ?: "Member"
        val authorAvatarKey = profile?.avatarKey ?: "coffee"
        val authorPhoto = profile?.photoBase64
        val authorPlus = isPlusMember.value
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.createPost(
                authorName = authorName,
                authorAvatarKey = authorAvatarKey,
                authorPhotoBase64 = authorPhoto,
                authorIsPlus = authorPlus,
                content = content,
                tag = tag,
                imageKey = imageKey,
                postPhotoBase64 = postPhotoBase64,
                mediaType = mediaType,
                title = title,
                durationLabel = durationLabel,
                audioTrack = audioTrack
            )
            _isSaving.value = false
            result.fold(
                onSuccess = {
                    _statusMessage.value = when (mediaType) {
                        "reel" -> "Reel published to Watch!"
                        "video" -> "Video uploaded to Watch!"
                        "thread" -> "Thread posted!"
                        else -> "Shared to BINTAFACE Feed"
                    }
                    onPublished()
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't publish post")
                }
            )
        }
    }

    fun repostPost(post: Post) {
        if (post.id in repostedPostIds.value) {
            _statusMessage.value = "Already reposted"
            return
        }
        viewModelScope.launch {
            repository.repostPost(post.id).fold(
                onSuccess = { _statusMessage.value = "Reposted ${post.authorName}'s post!" },
                onFailure = { _statusMessage.value = friendlyErrorMessage(it, "Couldn't repost") }
            )
        }
    }

    fun incrementPostView(postId: String) {
        viewModelScope.launch {
            repository.incrementPostView(postId)
        }
    }

    fun toggleFollow(followedId: String, followedName: String) {
        if (followedId == currentUserId) return
        val currentlyFollowing = followedId in followedUserIds.value
        val myName = currentDisplayName()
        viewModelScope.launch {
            repository.toggleFollow(followedId, myName, currentlyFollowing).fold(
                onSuccess = {
                    _statusMessage.value = if (currentlyFollowing) "Unfollowed $followedName" else "Following $followedName"
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't update follow")
                }
            )
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            repository.deletePost(postId).onFailure {
                _statusMessage.value = friendlyErrorMessage(it, "Couldn't delete post")
            }
        }
    }

    fun toggleLike(postId: String, currentlyLiked: Boolean) {
        viewModelScope.launch {
            repository.toggleLikePost(postId, currentlyLiked).onFailure {
                _statusMessage.value = friendlyErrorMessage(it, "Couldn't update like")
            }
        }
    }

    fun addComment(postId: String, text: String, onDone: () -> Unit = {}) {
        val profile = (myProfile.value as? UiState.Success)?.data
        val authorName = profile?.displayName?.ifBlank { "Member" } ?: "Member"
        val authorAvatarKey = profile?.avatarKey ?: "coffee"
        viewModelScope.launch {
            repository.addComment(
                postId = postId,
                authorName = authorName,
                authorAvatarKey = authorAvatarKey,
                text = text
            ).fold(
                onSuccess = { onDone() },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't add comment")
                }
            )
        }
    }

    // --- Live Room Actions ---

    fun createLiveRoom(title: String, category: String, coverKey: String, onCreated: (String) -> Unit = {}) {
        val profile = (myProfile.value as? UiState.Success)?.data
        val hostName = profile?.displayName?.ifBlank { "Streamer" } ?: "Streamer"
        val hostAvatar = profile?.avatarKey ?: "studio"
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.createLiveRoom(
                hostName = hostName,
                hostAvatarKey = hostAvatar,
                hostPhotoBase64 = profile?.photoBase64,
                hostIsPlus = isPlusMember.value,
                title = title,
                category = category,
                coverKey = coverKey
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { roomId ->
                    _activeLiveRoomId.value = roomId
                    _statusMessage.value = "You are LIVE on BINTAFACE!"
                    onCreated(roomId)
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't start live stream")
                }
            )
        }
    }

    fun openLiveRoom(room: LiveRoom) {
        _activeLiveRoomId.value = room.id
        if (room.hostId != currentUserId) {
            viewModelScope.launch {
                repository.adjustLiveViewerCount(room.id, 1L)
            }
        }
    }

    fun closeLiveRoom() {
        val current = activeLiveRoom.value
        _activeLiveRoomId.value = null
        if (current != null && current.hostId != currentUserId && current.viewerCount > 0L) {
            viewModelScope.launch {
                repository.adjustLiveViewerCount(current.id, -1L)
            }
        }
    }

    fun endLiveRoom(roomId: String) {
        viewModelScope.launch {
            repository.endLiveRoom(roomId).fold(
                onSuccess = {
                    _activeLiveRoomId.value = null
                    _statusMessage.value = "Live stream ended"
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't end stream")
                }
            )
        }
    }

    fun sendLiveHeart(roomId: String) {
        viewModelScope.launch {
            repository.sendLiveHeart(roomId)
        }
    }

    fun sendLiveMessage(roomId: String, text: String, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            repository.sendLiveMessage(
                roomId = roomId,
                senderName = currentDisplayName(),
                senderAvatarKey = currentAvatarKey(),
                text = text
            ).fold(
                onSuccess = { onSent() },
                onFailure = { _statusMessage.value = friendlyErrorMessage(it, "Couldn't send live chat") }
            )
        }
    }

    // --- Broadcast Channel Actions ---

    fun createChannel(title: String, handle: String, description: String, category: String, onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.createChannel(
                ownerName = currentDisplayName(),
                ownerAvatarKey = currentAvatarKey(),
                title = title,
                handle = handle,
                description = description,
                category = category
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { id ->
                    _activeChannelId.value = id
                    _statusMessage.value = "Channel created!"
                    onCreated(id)
                },
                onFailure = { _statusMessage.value = friendlyErrorMessage(it, "Couldn't create channel") }
            )
        }
    }

    fun openChannel(channelId: String) {
        _activeChannelId.value = channelId
    }

    fun closeChannel() {
        _activeChannelId.value = null
    }

    fun toggleChannelSubscription(channel: SocialChannel) {
        if (channel.ownerId == currentUserId) return
        val currentlySubscribed = channel.id in subscribedChannelIds.value
        viewModelScope.launch {
            repository.toggleChannelSubscription(channel.id, currentlySubscribed).fold(
                onSuccess = {
                    _statusMessage.value = if (currentlySubscribed) "Left ${channel.title}" else "Subscribed to ${channel.title}"
                },
                onFailure = { _statusMessage.value = friendlyErrorMessage(it, "Couldn't update subscription") }
            )
        }
    }

    fun publishChannelBroadcast(channelId: String, text: String, imageKey: String? = null, onSent: () -> Unit = {}) {
        viewModelScope.launch {
            repository.publishChannelBroadcast(
                channelId = channelId,
                ownerName = currentDisplayName(),
                text = text,
                imageKey = imageKey
            ).fold(
                onSuccess = { onSent() },
                onFailure = { _statusMessage.value = friendlyErrorMessage(it, "Couldn't send broadcast") }
            )
        }
    }

    // --- Facebook-Style Page Actions ---

    fun createPage(
        name: String,
        handle: String,
        category: String,
        description: String,
        websiteUrl: String,
        coverKey: String,
        photoBase64: String?,
        onCreated: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.createPage(
                ownerName = currentDisplayName(),
                ownerAvatarKey = currentAvatarKey(),
                name = name,
                handle = handle,
                category = category,
                description = description,
                websiteUrl = websiteUrl,
                coverKey = coverKey,
                photoBase64 = photoBase64
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { pageId ->
                    _activePageId.value = pageId
                    _statusMessage.value = "Page \"$name\" created on BINTAFACE!"
                    onCreated(pageId)
                },
                onFailure = { _statusMessage.value = friendlyErrorMessage(it, "Couldn't create Page") }
            )
        }
    }

    fun updatePage(
        pageId: String,
        name: String,
        category: String,
        description: String,
        websiteUrl: String,
        coverKey: String,
        photoBase64: String?,
        onUpdated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.updatePage(
                pageId = pageId,
                name = name,
                category = category,
                description = description,
                websiteUrl = websiteUrl,
                coverKey = coverKey,
                photoBase64 = photoBase64
            )
            _isSaving.value = false
            res.fold(
                onSuccess = {
                    _statusMessage.value = "Page details updated!"
                    onUpdated()
                },
                onFailure = { _statusMessage.value = friendlyErrorMessage(it, "Couldn't update Page") }
            )
        }
    }

    fun openPage(pageId: String) {
        _activePageId.value = pageId
    }

    fun closePage() {
        _activePageId.value = null
    }

    fun togglePageFollow(page: SocialPage) {
        if (page.ownerId == currentUserId) return
        val currentlyFollowing = page.id in followedPageIds.value
        viewModelScope.launch {
            repository.togglePageFollow(page.id, currentlyFollowing).fold(
                onSuccess = {
                    _statusMessage.value = if (currentlyFollowing) "Unfollowed ${page.name}" else "Following ${page.name}!"
                },
                onFailure = { _statusMessage.value = friendlyErrorMessage(it, "Couldn't update Page follow") }
            )
        }
    }

    fun publishPagePost(
        page: SocialPage,
        text: String,
        imageKey: String?,
        postPhotoBase64: String?,
        onPublished: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.publishPagePost(
                pageId = page.id,
                pageName = page.name,
                text = text,
                imageKey = imageKey,
                postPhotoBase64 = postPhotoBase64
            )
            _isSaving.value = false
            res.fold(
                onSuccess = {
                    _statusMessage.value = "Published update to ${page.name}!"
                    onPublished()
                },
                onFailure = { _statusMessage.value = friendlyErrorMessage(it, "Couldn't publish Page post") }
            )
        }
    }

    fun toggleLikePagePost(pageId: String, postId: String, currentlyLiked: Boolean) {
        viewModelScope.launch {
            repository.toggleLikePagePost(pageId, postId, currentlyLiked).onFailure {
                _statusMessage.value = friendlyErrorMessage(it, "Couldn't update like")
            }
        }
    }

    fun deletePagePost(pageId: String, postId: String) {
        viewModelScope.launch {
            repository.deletePagePost(pageId, postId).onFailure {
                _statusMessage.value = friendlyErrorMessage(it, "Couldn't delete Page post")
            }
        }
    }

    // --- Direct & Group Conversation Actions ---

    fun openOrCreateDirectChat(targetProfile: PublicProfile) {
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.createOrOpenDirectConversation(
                myName = currentDisplayName(),
                targetId = targetProfile.userId,
                targetName = targetProfile.displayName
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { convId -> _activeConversationId.value = convId },
                onFailure = { _statusMessage.value = friendlyErrorMessage(it, "Couldn't open direct chat") }
            )
        }
    }

    fun createGroupChat(title: String, selectedMembers: List<PublicProfile>, onCreated: () -> Unit = {}) {
        val memberMap = selectedMembers.associate { it.userId to it.displayName }
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.createGroupConversation(
                myName = currentDisplayName(),
                title = title,
                members = memberMap
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { convId ->
                    _activeConversationId.value = convId
                    onCreated()
                },
                onFailure = { _statusMessage.value = friendlyErrorMessage(it, "Couldn't create group") }
            )
        }
    }

    fun openDirectConversation(conversationId: String) {
        _activeConversationId.value = conversationId
    }

    fun closeDirectConversation() {
        _activeConversationId.value = null
    }

    fun sendDirectMessage(text: String, onSent: () -> Unit = {}) {
        val conv = activeConversation.value ?: return
        viewModelScope.launch {
            repository.sendDirectMessage(conv, currentDisplayName(), text).fold(
                onSuccess = { onSent() },
                onFailure = { _statusMessage.value = friendlyErrorMessage(it, "Couldn't send message") }
            )
        }
    }

    // --- Dating Discovery & Match Chat Actions ---

    fun swipe(targetProfile: PublicProfile, decision: String) {
        val currentProfile = (myProfile.value as? UiState.Success)?.data
        if (currentProfile == null) {
            _statusMessage.value = "Complete your profile first to start matching"
            return
        }
        if (decision == "like" && !isPlusMember.value && dailyLikesRemaining.value <= 0) {
            _statusMessage.value = "You've reached today's 10 free likes. Upgrade to Bintaface+ for unlimited likes."
            _showPaywall.value = true
            return
        }
        if (decision == "superlike" && superlikesBalance.value <= 0) {
            _statusMessage.value = "You're out of Superlikes. Grab a 5 Superlikes pack to stand out!"
            _showPaywall.value = true
            return
        }
        if (!swipesInFlight.add(targetProfile.userId)) return
        viewModelScope.launch {
            try {
                repository.recordSwipe(currentProfile, targetProfile, decision).fold(
                    onSuccess = { isMutual ->
                        if (isMutual) {
                            val matchId = sortedMatchId(currentUserId, targetProfile.userId)
                            _newMatchPopup.value = targetProfile to matchId
                        }
                    },
                    onFailure = {
                        _statusMessage.value = friendlyErrorMessage(it, "Couldn't record swipe")
                    }
                )
            } finally {
                swipesInFlight.remove(targetProfile.userId)
            }
        }
    }

    fun respondToIncomingLike(like: SwipeRecord, accept: Boolean) {
        if (accept && !isPlusMember.value) {
            openPaywall()
            return
        }
        viewModelScope.launch {
            val target = findDiscoverableProfile(like.fromUserId)
                ?: repository.getPublicProfile(like.fromUserId).getOrNull()
            if (target == null) {
                _statusMessage.value = "Profile is no longer available"
                return@launch
            }
            swipe(target, if (accept) "like" else "pass")
        }
    }

    fun sendMessage(text: String, onSent: () -> Unit = {}) {
        val match = activeMatch.value ?: return
        val profile = (myProfile.value as? UiState.Success)?.data
        val senderName = profile?.displayName?.ifBlank { "Member" } ?: "Member"
        viewModelScope.launch {
            repository.sendMessage(match, senderName, text).fold(
                onSuccess = { onSent() },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't send message")
                }
            )
        }
    }

    fun unmatch(matchId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            repository.unmatch(matchId).fold(
                onSuccess = {
                    _statusMessage.value = "Conversation removed"
                    onDone()
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't unmatch")
                }
            )
        }
    }

    // --- Voice Notes & Voice/Video Calls ---

    fun sendConversationVoiceNote(
        voiceNoteBase64: String,
        durationSec: Long,
        onDone: () -> Unit = {}
    ) {
        val conv = activeConversation.value ?: return
        viewModelScope.launch {
            repository.sendVoiceNoteToConversation(
                conversation = conv,
                senderName = currentDisplayName(),
                voiceNoteBase64 = voiceNoteBase64,
                durationSec = durationSec
            ).fold(
                onSuccess = { onDone() },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't send voice note")
                }
            )
        }
    }

    fun sendMatchVoiceNote(
        voiceNoteBase64: String,
        durationSec: Long,
        onDone: () -> Unit = {}
    ) {
        val match = activeMatch.value ?: return
        viewModelScope.launch {
            repository.sendVoiceNoteToMatch(
                match = match,
                senderName = currentDisplayName(),
                voiceNoteBase64 = voiceNoteBase64,
                durationSec = durationSec
            ).fold(
                onSuccess = { onDone() },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't send voice note")
                }
            )
        }
    }

    fun startConversationCall(conversation: Conversation, callType: String) {
        if (_activeCallId.value != null) {
            _statusMessage.value = "Finish your current call first."
            return
        }
        viewModelScope.launch {
            repository.startCallSession(
                threadId = conversation.id,
                threadType = "conversation",
                participantIds = conversation.participantIds,
                participantNames = conversation.participantNames,
                callerName = currentDisplayName(),
                callerAvatarKey = currentAvatarKey(),
                callType = callType
            ).fold(
                onSuccess = { session ->
                    _activeCallId.value = session.id
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't start call")
                }
            )
        }
    }

    fun startMatchCall(match: MatchThread, callType: String) {
        if (_activeCallId.value != null) {
            _statusMessage.value = "Finish your current call first."
            return
        }
        val names = buildMap {
            if (match.user1Id.isNotBlank()) put(match.user1Id, match.user1Name.ifBlank { "Member" })
            if (match.user2Id.isNotBlank()) put(match.user2Id, match.user2Name.ifBlank { "Member" })
        }
        viewModelScope.launch {
            repository.startCallSession(
                threadId = match.id,
                threadType = "match",
                participantIds = match.participantIds,
                participantNames = names,
                callerName = currentDisplayName(),
                callerAvatarKey = currentAvatarKey(),
                callType = callType
            ).fold(
                onSuccess = { session ->
                    _activeCallId.value = session.id
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't start call")
                }
            )
        }
    }

    fun acceptIncomingCall(session: CallSession) {
        _activeCallId.value = session.id
        viewModelScope.launch {
            repository.acceptCallSession(session.id).onFailure {
                _activeCallId.value = null
                _statusMessage.value = friendlyErrorMessage(it, "Couldn't join call")
            }
        }
    }

    fun declineIncomingCall(session: CallSession) {
        viewModelScope.launch {
            repository.declineCallSession(session.id)
            repository.appendCallLogMessage(
                session = session,
                senderName = currentDisplayName(),
                callStatus = "declined",
                durationSec = 0L
            )
        }
    }

    fun endActiveCall(session: CallSession, elapsedSec: Long) {
        _activeCallId.value = null
        val wasConnected = session.status == "active" || elapsedSec > 0L
        viewModelScope.launch {
            repository.endCallSession(session.id, elapsedSec)
            repository.appendCallLogMessage(
                session = session,
                senderName = currentDisplayName(),
                callStatus = if (wasConnected) "completed" else "missed",
                durationSec = elapsedSec
            )
        }
    }

    fun updateActiveCallMedia(sessionId: String, muted: Boolean, videoOn: Boolean) {
        viewModelScope.launch {
            repository.updateCallMediaState(sessionId, muted, videoOn)
        }
    }

    // --- BINTAFACE Shops Actions ---

    fun openShop(shopId: String) {
        _activeShopId.value = shopId
    }

    fun closeShop() {
        _activeShopId.value = null
    }

    fun createShop(
        name: String,
        handle: String,
        category: String,
        description: String,
        currency: String,
        city: String,
        coverKey: String,
        photoBase64: String?,
        onCreated: (String) -> Unit = {}
    ) {
        if (_isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.createShop(
                ownerName = currentDisplayName(),
                ownerAvatarKey = currentAvatarKey(),
                name = name,
                handle = handle,
                category = category,
                description = description,
                currency = currency,
                city = city,
                coverKey = coverKey,
                photoBase64 = photoBase64
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { shopId ->
                    _activeShopId.value = shopId
                    _statusMessage.value = "Shop \"$name\" launched on BINTAFACE!"
                    onCreated(shopId)
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't create shop")
                }
            )
        }
    }

    fun updateShop(
        shopId: String,
        name: String,
        category: String,
        description: String,
        currency: String,
        city: String,
        coverKey: String,
        photoBase64: String?,
        onUpdated: () -> Unit = {}
    ) {
        if (_isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.updateShop(
                shopId = shopId,
                name = name,
                category = category,
                description = description,
                currency = currency,
                city = city,
                coverKey = coverKey,
                photoBase64 = photoBase64
            )
            _isSaving.value = false
            res.fold(
                onSuccess = {
                    _statusMessage.value = "Shop updated!"
                    onUpdated()
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't update shop")
                }
            )
        }
    }

    fun addShopProduct(
        shop: Shop,
        title: String,
        description: String,
        category: String,
        priceUsdCents: Long,
        priceNgn: Long,
        stockCount: Long,
        imageKey: String?,
        photoBase64: String?
    ) {
        if (_isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.createShopProduct(
                shop = shop,
                title = title,
                description = description,
                category = category,
                priceUsdCents = priceUsdCents,
                priceNgn = priceNgn,
                stockCount = stockCount,
                imageKey = imageKey,
                photoBase64 = photoBase64
            )
            _isSaving.value = false
            res.fold(
                onSuccess = {
                    _statusMessage.value = "\"$title\" added to ${shop.name}!"
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't add product")
                }
            )
        }
    }

    fun placeShopOrder(
        shop: Shop,
        product: ShopProduct,
        quantity: Long,
        currency: String,
        deliveryNote: String,
        onDone: () -> Unit = {}
    ) {
        if (_isSaving.value) return
        val rate = usdToNgnRate.value
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.placeShopOrder(
                shop = shop,
                product = product,
                buyerName = currentDisplayName(),
                quantity = quantity,
                currency = currency,
                deliveryNote = deliveryNote,
                usdToNgnRate = rate
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { order ->
                    _statusMessage.value = "Order placed for ${order.productTitle} x${order.quantity}!"
                    onDone()
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't place order")
                }
            )
        }
    }

    fun updateShopOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateShopOrderStatus(orderId, newStatus).fold(
                onSuccess = {
                    _statusMessage.value = "Order marked as ${newStatus.lowercase()}"
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't update order")
                }
            )
        }
    }

    // --- BINTAFACE Global Radio Actions ---

    fun purchaseRadioAccess(
        currency: String,
        paymentMethod: String,
        paymentReference: String,
        googleConfirmed: Boolean = true
    ) {
        if (_isSaving.value) return
        if (!googleConfirmed) {
            _statusMessage.value = "Google confirmation is required before paying ₦2,000 for Radio Access."
            return
        }
        val myName = currentDisplayName()
        val rate = usdToNgnRate.value
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.purchaseRadioAccessDirect(
                currency = currency,
                paymentMethod = paymentMethod,
                paymentReference = paymentReference,
                payerName = myName,
                usdToNgnRate = rate,
                googleConfirmed = googleConfirmed
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { pass ->
                    val amountLabel = if (pass.currency == "NGN") {
                        formatNgnWhole(pass.ngnAmount)
                    } else {
                        formatUsdCents(pass.amountCents)
                    }
                    _statusMessage.value =
                        "📻 Google confirmed! Global Radio Access is ACTIVE ($amountLabel → ${pass.adminBankLabel}). You can now listen & create channels!"
                },
                onFailure = { error ->
                    _statusMessage.value = friendlyErrorMessage(error, "Couldn't activate Global Radio Access")
                }
            )
        }
    }

    fun openRadioChannel(channel: RadioChannel) {
        if (!hasRadioAccess.value) {
            _showRadioAccessSheet.value = true
            _statusMessage.value = "Pay ${formatNgnWhole(RADIO_ACCESS_FEE_NGN)} to Francis Gregory (FairMoney 9038006221) to unlock Global Radio."
            return
        }
        val previousId = _activeRadioChannelId.value
        if (previousId == channel.id) return
        _activeRadioChannelId.value = channel.id
        viewModelScope.launch {
            if (previousId != null) {
                repository.recordRadioListenerPulse(previousId, -1L)
            }
            repository.recordRadioListenerPulse(channel.id, 1L)
        }
    }

    fun closeRadioChannel() {
        val currentId = _activeRadioChannelId.value ?: return
        _activeRadioChannelId.value = null
        viewModelScope.launch {
            repository.recordRadioListenerPulse(currentId, -1L)
        }
    }

    fun createRadioChannel(
        title: String,
        frequency: String,
        handle: String,
        category: String,
        description: String,
        coverKey: String = "studio",
        onCreated: (String) -> Unit = {}
    ) {
        if (!hasRadioAccess.value) {
            _showRadioAccessSheet.value = true
            _statusMessage.value = "Unlock Radio Access (₦2,000) first before creating your Global Radio channel."
            return
        }
        if (_isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.createRadioChannel(
                ownerName = currentDisplayName(),
                ownerAvatarKey = currentAvatarKey(),
                title = title,
                frequency = frequency,
                handle = handle,
                category = category,
                description = description,
                coverKey = coverKey
            )
            _isSaving.value = false
            res.fold(
                onSuccess = { channelId ->
                    _activeRadioChannelId.value = channelId
                    _statusMessage.value = "📻 Radio station \"$title\" is now ON AIR for billions of listeners!"
                    onCreated(channelId)
                },
                onFailure = { error ->
                    _statusMessage.value = friendlyErrorMessage(error, "Couldn't launch radio station")
                }
            )
        }
    }

    fun toggleRadioChannelLiveStatus(channel: RadioChannel) {
        if (channel.ownerId != currentUserId) return
        val next = if (channel.status == "live") "off_air" else "live"
        viewModelScope.launch {
            repository.updateRadioChannelStatus(channel.id, next).onSuccess {
                _statusMessage.value = if (next == "live") "${channel.title} is now ON AIR" else "${channel.title} is Off Air"
            }.onFailure {
                _statusMessage.value = friendlyErrorMessage(it, "Couldn't update radio station status")
            }
        }
    }

    fun toggleRadioSubscription(channel: RadioChannel) {
        if (!hasRadioAccess.value) {
            _showRadioAccessSheet.value = true
            return
        }
        val subscribed = channel.id in subscribedRadioChannelIds.value
        viewModelScope.launch {
            repository.toggleRadioSubscription(channel.id, subscribed).onSuccess {
                _statusMessage.value = if (subscribed) "Unsubscribed from ${channel.title}" else "Subscribed to ${channel.title}!"
            }.onFailure {
                _statusMessage.value = friendlyErrorMessage(it, "Couldn't update radio subscription")
            }
        }
    }

    fun publishRadioBroadcast(
        channelId: String,
        title: String,
        text: String,
        audioBase64: String? = null,
        audioDurationSec: Long = 0L,
        onSent: () -> Unit = {}
    ) {
        if (_isSaving.value) return
        viewModelScope.launch {
            _isSaving.value = true
            val res = repository.publishRadioBroadcast(
                channelId = channelId,
                ownerName = currentDisplayName(),
                title = title,
                text = text,
                audioBase64 = audioBase64,
                audioDurationSec = audioDurationSec
            )
            _isSaving.value = false
            res.fold(
                onSuccess = {
                    _statusMessage.value = "Broadcast sent to listeners!"
                    onSent()
                },
                onFailure = {
                    _statusMessage.value = friendlyErrorMessage(it, "Couldn't send radio broadcast")
                }
            )
        }
    }

    fun sendSparkToRadioChannel(channel: RadioChannel) {
        sendSparkGiftToTarget(
            targetId = channel.id,
            targetType = "radio",
            recipientId = channel.ownerId,
            recipientName = channel.ownerName
        )
    }

    companion object {
        private const val TAG = "BintafaceVM"
        private const val STOP_TIMEOUT_MILLIS = 5000L
        const val FREE_DAILY_LIKES_LIMIT = 10
    }
}
