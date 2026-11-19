package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserPrivate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String? = null,
    val showInDiscovery: Boolean = true,
    val preferredIntent: String = "all",
    @get:PropertyName("isPlusMember") val isPlusMember: Boolean = false,
    val plusPlanId: String? = null,
    val superlikesBalance: Long = 1L,
    val boostsBalance: Long = 0L,
    val sparksBalance: Long = 0L,
    val dailyLikesUsed: Long = 0L,
    val dailyLikesDate: String = "",
    val lastPurchaseToken: String? = null,
    val payoutMethod: String? = null,
    val payoutAccount: String? = null,
    val payoutBankOrNetwork: String? = null,
    val payoutCurrency: String? = null,
    val autoPayoutEnabled: Boolean = true,
    val vipLevel: String = "none",
    val vipLevelRank: Long = 0L,
    val lastLevelUpgradeId: String? = null,
    val hasRadioAccess: Boolean = false,
    val radioPassId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val age: Long = 24L,
    val city: String = "",
    val bio: String = "",
    val lookingFor: String = "both",
    val promptQuestion: String = "My ideal weekend looks like...",
    val promptAnswer: String = "",
    val interests: List<String> = emptyList(),
    val avatarKey: String = "coffee",
    val photoBase64: String? = null,
    @get:PropertyName("isPlusMember") val isPlusMember: Boolean = false,
    @get:PropertyName("isBoosted") val isBoosted: Boolean = false,
    val vipLevel: String = "none",
    val vipLevelRank: Long = 0L,
    val lastLevelUpgradeId: String? = null,
    val hasRadioAccess: Boolean = false,
    val radioPassId: String? = null,
    @get:PropertyName("isDiscoverable") val isDiscoverable: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Post(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarKey: String = "coffee",
    val authorPhotoBase64: String? = null,
    val authorIsPlus: Boolean = false,
    val mediaType: String = "post",
    val title: String = "",
    val durationLabel: String = "",
    val audioTrack: String = "",
    val repostedFromAuthor: String? = null,
    val content: String = "",
    val tag: String = "Moment",
    val imageKey: String? = null,
    val postPhotoBase64: String? = null,
    val visibility: String = "community",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val repostsCount: Long = 0L,
    val viewsCount: Long = 0L,
    val sparksCount: Long = 0L,
    val sparksEarningsCents: Long = 0L,
    val lastCommentId: String? = null,
    val lastSparkGiftId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PostLike(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PostRepost(
    @DocumentId val id: String = "",
    val userId: String = "",
    val postId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Follow(
    @DocumentId val id: String = "",
    val followerId: String = "",
    val followedId: String = "",
    val followerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val postVisibility: String = "community",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarKey: String = "coffee",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LiveRoom(
    @DocumentId val id: String = "",
    val hostId: String = "",
    val hostName: String = "",
    val hostAvatarKey: String = "studio",
    val hostPhotoBase64: String? = null,
    val hostIsPlus: Boolean = false,
    val title: String = "",
    val category: String = "Talk",
    val coverKey: String? = "studio",
    val visibility: String = "public",
    val status: String = "live",
    val viewerCount: Long = 1L,
    val likesCount: Long = 0L,
    val sparksCount: Long = 0L,
    val totalGiftsCents: Long = 0L,
    val lastSparkGiftId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LiveMessage(
    @DocumentId val id: String = "",
    val roomId: String = "",
    val roomVisibility: String = "public",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatarKey: String = "coffee",
    val text: String = "",
    @get:PropertyName("isGiftNotice") val isGiftNotice: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SocialChannel(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerAvatarKey: String = "coffee",
    val title: String = "",
    val handle: String = "",
    val description: String = "",
    val category: String = "General",
    val visibility: String = "public",
    val subscriberCount: Long = 1L,
    val totalGiftsCents: Long = 0L,
    val lastSparkGiftId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChannelSubscription(
    @DocumentId val id: String = "",
    val channelId: String = "",
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ChannelBroadcast(
    @DocumentId val id: String = "",
    val channelId: String = "",
    val channelVisibility: String = "public",
    val ownerId: String = "",
    val ownerName: String = "",
    val text: String = "",
    val imageKey: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SocialPage(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerAvatarKey: String = "coffee",
    val name: String = "",
    val handle: String = "",
    val category: String = "Creator",
    val description: String = "",
    val websiteUrl: String = "",
    val coverKey: String = "studio",
    val photoBase64: String? = null,
    val visibility: String = "public",
    val followersCount: Long = 1L,
    val postsCount: Long = 0L,
    val totalGiftsCents: Long = 0L,
    val lastPostId: String? = null,
    val lastSparkGiftId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PageFollow(
    @DocumentId val id: String = "",
    val pageId: String = "",
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PagePost(
    @DocumentId val id: String = "",
    val pageId: String = "",
    val pageName: String = "",
    val pageVisibility: String = "public",
    val ownerId: String = "",
    val text: String = "",
    val imageKey: String? = null,
    val postPhotoBase64: String? = null,
    val likesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PagePostLike(
    @DocumentId val id: String = "",
    val pageId: String = "",
    val postId: String = "",
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

val PAGE_CATEGORIES = listOf(
    "Creator", "Business", "Community", "Entertainment", "Education", "Shopping", "News", "Artist"
)

@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val type: String = "direct",
    val title: String = "",
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val createdBy: String = "",
    val lastMessage: String = "",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun displayTitle(currentUserId: String): String {
        if (type == "group") return title.ifBlank { "Group Chat" }
        val otherEntry = participantNames.entries.firstOrNull { it.key != currentUserId }
        return otherEntry?.value?.ifBlank { title } ?: title.ifBlank { "Direct Chat" }
    }

    fun otherParticipantId(currentUserId: String): String? =
        if (type == "direct") participantIds.firstOrNull { it != currentUserId } else null
}

@IgnoreExtraProperties
data class DirectMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val participantIds: List<String> = emptyList(),
    val messageType: String = "text",
    val voiceNoteBase64: String? = null,
    val voiceNoteDurationSec: Long = 0L,
    val callType: String? = null,
    val callDurationSec: Long = 0L,
    val callStatus: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SwipeRecord(
    @DocumentId val id: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val decision: String = "like",
    val positiveTargetId: String = "",
    val fromName: String = "",
    val fromAvatarKey: String = "coffee",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MatchThread(
    @DocumentId val id: String = "",
    val participantIds: List<String> = emptyList(),
    val user1Id: String = "",
    val user1Name: String = "",
    val user1AvatarKey: String = "coffee",
    val user2Id: String = "",
    val user2Name: String = "",
    val user2AvatarKey: String = "sunset",
    val lastMessage: String = "",
    @ServerTimestamp val lastMessageAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun partnerId(currentUserId: String): String =
        if (user1Id == currentUserId) user2Id else user1Id

    fun partnerName(currentUserId: String): String =
        if (user1Id == currentUserId) user2Name else user1Name

    fun partnerAvatarKey(currentUserId: String): String =
        if (user1Id == currentUserId) user2AvatarKey else user1AvatarKey
}

@IgnoreExtraProperties
data class ChatMessage(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val participantIds: List<String> = emptyList(),
    val messageType: String = "text",
    val voiceNoteBase64: String? = null,
    val voiceNoteDurationSec: Long = 0L,
    val callType: String? = null,
    val callDurationSec: Long = 0L,
    val callStatus: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PlatformSettings(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String? = null,
    val platformFeePercent: Long = 20L,
    val payoutAccountLabel: String? = null,
    val payoutMethod: String? = "ngn_bank",
    val bankName: String? = DEFAULT_ADMIN_BANK_NAME,
    val adminAccountNumber: String? = DEFAULT_ADMIN_ACCOUNT_NUMBER,
    val adminAccountHolder: String? = DEFAULT_ADMIN_ACCOUNT_HOLDER,
    val maskedAccountNumber: String? = "••••6221",
    val adminPayoutCurrency: String? = "NGN",
    val usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE,
    val adAdminSharePercent: Long = 70L,
    val autoPayoutEnabled: Boolean = true,
    val appDownloadUrl: String? = DEFAULT_APP_DOWNLOAD_URL,
    val admobBannerUnitId: String? = DEFAULT_ADMOB_BANNER_UNIT_ID,
    val admobRewardedUnitId: String? = DEFAULT_ADMOB_REWARDED_UNIT_ID,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminPayoutAccount(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val payoutMethod: String = "ngn_bank",
    val payoutCurrency: String = "NGN",
    val accountHolderName: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val routingOrSwift: String = "",
    val payoutEmailOrPhone: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminWithdrawal(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val amountCents: Long = 0L,
    val ngnAmountWhole: Long = 0L,
    val payoutCurrency: String = "USD",
    val payoutMethod: String = "ngn_bank",
    val accountHolderName: String = "",
    val bankName: String = "",
    val maskedAccountNumber: String = "",
    val status: String = "completed",
    val referenceCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdRevenueEvent(
    @DocumentId val id: String = "",
    val viewerId: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val postId: String = "",
    val adFormat: String = "banner",
    val adUnitId: String = "",
    val valueMicros: Long = 0L,
    val currencyCode: String = "USD",
    val grossCents: Long = 0L,
    val adminShareCents: Long = 0L,
    val creatorShareCents: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SparkGift(
    @DocumentId val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val recipientId: String = "",
    val recipientName: String = "",
    val postId: String = "",
    val targetType: String = "post",
    val sparksCount: Long = 1L,
    val grossCents: Long = 100L,
    val creatorShareCents: Long = 80L,
    val adminFeeCents: Long = 20L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RevenueLedgerEntry(
    @DocumentId val id: String = "",
    val source: String = "subscription",
    val productId: String = "",
    val payerId: String = "",
    val recipientId: String = "",
    val grossCents: Long = 0L,
    val adminShareCents: Long = 0L,
    val creatorShareCents: Long = 0L,
    val referenceToken: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PayoutRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val amountCents: Long = 0L,
    val ngnAmountWhole: Long = 0L,
    val payoutMethod: String = "ngn_bank",
    val payoutCurrency: String = "NGN",
    val payoutBankOrNetwork: String = "",
    val payoutAccount: String = "",
    @get:PropertyName("isAutoPayout") val isAutoPayout: Boolean = false,
    val status: String = "pending",
    val adminNote: String? = null,
    val payoutReference: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

const val APP_ADMIN_EMAIL = "gregoryfrancis652@gmail.com"
const val DEFAULT_ADMIN_BANK_NAME = "FairMoney"
const val DEFAULT_ADMIN_ACCOUNT_NUMBER = "9038006221"
const val DEFAULT_ADMIN_ACCOUNT_HOLDER = "Francis Gregory"
const val CEO_WELCOME_NOTE = "Welcome to BINTAFACE, where knowledge meets power — CEO Francis Gregory"
const val DEFAULT_USD_TO_NGN_RATE = 1600L
const val BINTAFACE_DOMAIN = "bintaface.org"
const val LEGACY_PLAY_APP_DOWNLOAD_URL = "https://play.google.com/store/apps/details?id=com.aistudio.kindredsocial.xrmpzk"
const val UNHOSTED_BINTAFACE_URL = "https://bintaface.org"
const val DEFAULT_APP_DOWNLOAD_URL = "https://ais-pre-eairej7uik6gkhu57oas24-467447110533.europe-west1.run.app"
const val DEFAULT_ADMOB_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
const val DEFAULT_ADMOB_REWARDED_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

fun normalizeBintafaceAccessUrl(rawUrl: String?): String {
    val trimmed = rawUrl?.trim().orEmpty()
    return trimmed.ifBlank { DEFAULT_APP_DOWNLOAD_URL }
}

fun buildBintafaceUserAccessUrl(
    baseUrl: String = DEFAULT_APP_DOWNLOAD_URL,
    userHandleOrId: String? = null
): String {
    val cleanBase = normalizeBintafaceAccessUrl(baseUrl).trimEnd('/')
    val cleanSlug = userHandleOrId
        ?.trim()
        ?.removePrefix("@")
        ?.replace(Regex("[^a-zA-Z0-9._-]"), "")
        .orEmpty()
    if (cleanSlug.isBlank()) return cleanBase
    val lower = cleanBase.lowercase()
    val isDirectDownloadOrStoreUrl =
        lower.contains("play.google.com") ||
            lower.contains("drive.google.com") ||
            lower.contains("appdistribution.firebase") ||
            lower.endsWith(".apk") ||
            lower.contains("?")
    return when {
        isDirectDownloadOrStoreUrl -> cleanBase
        lower.contains("run.app") -> "$cleanBase/?invite=$cleanSlug"
        else -> "$cleanBase/u/$cleanSlug"
    }
}

fun buildBintafaceInAppDeepLink(userHandleOrId: String? = null): String {
    val cleanSlug = userHandleOrId
        ?.trim()
        ?.removePrefix("@")
        ?.replace(Regex("[^a-zA-Z0-9._-]"), "")
        .orEmpty()
    return if (cleanSlug.isBlank()) "bintaface://bintaface.org" else "bintaface://bintaface.org/u/$cleanSlug"
}

fun isDesignatedAdminEmail(email: String?): Boolean =
    email?.trim()?.equals(APP_ADMIN_EMAIL, ignoreCase = true) == true

fun maskAccountNumber(accountNumber: String): String {
    val clean = accountNumber.trim().replace(" ", "")
    return if (clean.length <= 4) {
        "••••$clean"
    } else {
        "••••${clean.takeLast(4)}"
    }
}

fun sortedMatchId(uidA: String, uidB: String): String =
    if (uidA < uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"

fun formatUsdCents(cents: Long): String {
    val safe = cents.coerceAtLeast(0L)
    val dollars = safe / 100L
    val rem = safe % 100L
    return String.format(java.util.Locale.US, "$%d.%02d", dollars, rem)
}

fun computeNgnWhole(cents: Long, usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE): Long {
    val safeCents = cents.coerceAtLeast(0L)
    val safeRate = usdToNgnRate.coerceIn(100L, 10000L)
    return (safeCents * safeRate) / 100L
}

fun formatNgnFromUsdCents(cents: Long, usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE): String {
    val ngn = computeNgnWhole(cents, usdToNgnRate)
    return String.format(java.util.Locale.US, "₦%,d", ngn)
}

fun formatDualCurrency(cents: Long, usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE): String =
    "${formatUsdCents(cents)} (${formatNgnFromUsdCents(cents, usdToNgnRate)})"

@IgnoreExtraProperties
data class LevelUpgradeOrder(
    @DocumentId val id: String = "",
    val payerId: String = "",
    val payerName: String = "",
    val levelId: String = "water",
    val levelName: String = "Water Diamond",
    val levelRank: Long = 1L,
    val currency: String = "NGN",
    val ngnAmount: Long = 5000L,
    val amountCents: Long = 350L,
    val adminShareCents: Long = 350L,
    val creatorShareCents: Long = 0L,
    val paymentMethod: String = "ngn_bank",
    val paymentReference: String = "",
    val adminEmail: String = APP_ADMIN_EMAIL,
    val adminBankLabel: String = "",
    val status: String = "activated",
    @ServerTimestamp val activatedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class MembershipLevelTier(
    val id: String,
    val name: String,
    val rank: Int,
    val ngnAmount: Long,
    val usdCents: Long,
    val emoji: String,
    val headline: String,
    val perks: String
)

val MEMBERSHIP_LEVEL_TIERS = listOf(
    MembershipLevelTier(
        id = "water",
        name = "Water Diamond",
        rank = 1,
        ngnAmount = 5000L,
        usdCents = 350L,
        emoji = "💧",
        headline = "Level 1 • Starter VIP Tier",
        perks = "Level 1 Water Diamond badge across Feed, Profile & Live • Unlocks Level 2"
    ),
    MembershipLevelTier(
        id = "black_diamond",
        name = "Black Diamond",
        rank = 2,
        ngnAmount = 5000L,
        usdCents = 350L,
        emoji = "🖤",
        headline = "Level 2 • Onyx Step",
        perks = "Level 2 Black Diamond badge • Priority replies & Live chat flair"
    ),
    MembershipLevelTier(
        id = "red_diamond",
        name = "Red Diamond",
        rank = 3,
        ngnAmount = 5000L,
        usdCents = 350L,
        emoji = "♦️",
        headline = "Level 3 • Crimson Step",
        perks = "Level 3 Red Diamond badge • Highlighted community posts & threads"
    ),
    MembershipLevelTier(
        id = "super_diamond",
        name = "Super Diamond",
        rank = 4,
        ngnAmount = 5000L,
        usdCents = 350L,
        emoji = "⚡",
        headline = "Level 4 • High-Voltage Step",
        perks = "Level 4 Super Diamond badge • Priority discovery card boost"
    ),
    MembershipLevelTier(
        id = "rock_diamond",
        name = "Rock Diamond",
        rank = 5,
        ngnAmount = 5000L,
        usdCents = 350L,
        emoji = "🪨",
        headline = "Level 5 • Bedrock Step",
        perks = "Level 5 Rock Diamond badge • Verified creator Pages spotlight"
    ),
    MembershipLevelTier(
        id = "star_diamond",
        name = "Star Diamond",
        rank = 6,
        ngnAmount = 5000L,
        usdCents = 350L,
        emoji = "⭐",
        headline = "Level 6 • Rising Star Step",
        perks = "Level 6 Star Diamond badge • Featured Reels & Watch placement"
    ),
    MembershipLevelTier(
        id = "silver_diamond",
        name = "Silver Diamond",
        rank = 7,
        ngnAmount = 5000L,
        usdCents = 350L,
        emoji = "🥈",
        headline = "Level 7 • Sterling Step",
        perks = "Level 7 Silver Diamond badge • VIP Live stream entrance highlight"
    ),
    MembershipLevelTier(
        id = "golden_diamond",
        name = "Golden Diamond",
        rank = 8,
        ngnAmount = 5000L,
        usdCents = 350L,
        emoji = "👑",
        headline = "Level 8 • Sovereign Step",
        perks = "Level 8 Golden Diamond badge • Top creator directory ranking"
    ),
    MembershipLevelTier(
        id = "moon_diamond",
        name = "Moon Diamond",
        rank = 9,
        ngnAmount = 5000L,
        usdCents = 350L,
        emoji = "🌙",
        headline = "Level 9 • Lunar Prestige Step",
        perks = "Level 9 Moon Diamond badge • Elite BINTAFACE partner showcase"
    ),
    MembershipLevelTier(
        id = "professional_general_diamond",
        name = "Professional General Diamond",
        rank = 10,
        ngnAmount = 5000L,
        usdCents = 350L,
        emoji = "💎",
        headline = "Level 10 • Supreme Commander Tier",
        perks = "Level 10 Professional General Diamond legend badge across all of BINTAFACE"
    )
)

fun levelTierForId(levelId: String?): MembershipLevelTier? {
    val normalized = levelId?.trim()?.lowercase(java.util.Locale.US) ?: return null
    return MEMBERSHIP_LEVEL_TIERS.firstOrNull { it.id == normalized }
}

fun nextLevelTierForRank(currentRank: Int): MembershipLevelTier? =
    MEMBERSHIP_LEVEL_TIERS.firstOrNull { it.rank == currentRank + 1 }

fun formatNgnWhole(ngnWhole: Long): String =
    String.format(java.util.Locale.US, "₦%,d", ngnWhole.coerceAtLeast(0L))

fun computeAd7030Split(
    grossCents: Long,
    creatorId: String,
    viewerId: String
): Pair<Long, Long> {
    val safeGross = grossCents.coerceAtLeast(1L)
    return if (creatorId.isNotBlank() && creatorId != viewerId) {
        val creatorShare = (safeGross * 30L) / 100L
        val adminShare = safeGross - creatorShare
        adminShare to creatorShare
    } else {
        safeGross to 0L
    }
}

@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerAvatarKey: String = "coffee",
    val calleeIds: List<String> = emptyList(),
    val participantIds: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val threadId: String = "",
    val threadType: String = "conversation",
    val callType: String = "voice",
    val status: String = "ringing",
    val durationSec: Long = 0L,
    val callerMuted: Boolean = false,
    val callerVideoOn: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    @ServerTimestamp val endedAt: Timestamp? = null
) {
    fun partnerDisplayName(currentUserId: String): String {
        if (callerId != currentUserId) return callerName.ifBlank { "Member" }
        val otherEntry = participantNames.entries.firstOrNull { it.key != currentUserId }
        return otherEntry?.value?.ifBlank { "BINTAFACE Call" } ?: "BINTAFACE Call"
    }
}

@IgnoreExtraProperties
data class Shop(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerAvatarKey: String = "studio",
    val name: String = "",
    val handle: String = "",
    val category: String = "General",
    val description: String = "",
    val currency: String = "NGN",
    val city: String = "",
    val coverKey: String = "studio",
    val photoBase64: String? = null,
    val visibility: String = "public",
    val productsCount: Long = 0L,
    val ordersCount: Long = 0L,
    val totalSalesCents: Long = 0L,
    val lastProductId: String? = null,
    val lastOrderId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ShopProduct(
    @DocumentId val id: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val shopVisibility: String = "public",
    val ownerId: String = "",
    val title: String = "",
    val description: String = "",
    val category: String = "General",
    val priceUsdCents: Long = 500L,
    val priceNgn: Long = 8000L,
    val stockCount: Long = 10L,
    @get:PropertyName("isAvailable") val isAvailable: Boolean = true,
    val imageKey: String? = "studio",
    val photoBase64: String? = null,
    val lastOrderId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ShopOrder(
    @DocumentId val id: String = "",
    val shopId: String = "",
    val shopName: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val buyerId: String = "",
    val buyerName: String = "",
    val sellerId: String = "",
    val sellerName: String = "",
    val quantity: Long = 1L,
    val currency: String = "NGN",
    val unitPriceCents: Long = 500L,
    val totalCents: Long = 500L,
    val ngnAmount: Long = 8000L,
    val sellerShareCents: Long = 400L,
    val adminFeeCents: Long = 100L,
    val deliveryNote: String? = null,
    val status: String = "placed",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

val SHOP_CATEGORIES = listOf(
    "Fashion", "Electronics", "Beauty", "Digital", "Food", "Services", "General"
)

fun computeShop8020Split(totalCents: Long): Pair<Long, Long> {
    val safeTotal = totalCents.coerceAtLeast(50L)
    val adminFee = (safeTotal * 20L) / 100L
    val sellerShare = safeTotal - adminFee
    return sellerShare to adminFee
}

const val RADIO_ACCESS_FEE_NGN = 2000L
const val RADIO_ACCESS_USD_CENTS = 150L

val RADIO_CATEGORIES = listOf(
    "Talk", "Music", "News", "Knowledge", "Faith", "Comedy", "Business", "Global"
)

@IgnoreExtraProperties
data class RadioPass(
    @DocumentId val id: String = "",
    val payerId: String = "",
    val payerName: String = "",
    val currency: String = "NGN",
    val ngnAmount: Long = RADIO_ACCESS_FEE_NGN,
    val amountCents: Long = RADIO_ACCESS_USD_CENTS,
    val adminShareCents: Long = RADIO_ACCESS_USD_CENTS,
    val creatorShareCents: Long = 0L,
    val paymentMethod: String = "ngn_bank",
    val paymentReference: String = "",
    val adminEmail: String = APP_ADMIN_EMAIL,
    val adminBankLabel: String = "",
    val status: String = "active",
    @ServerTimestamp val activatedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RadioChannel(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerAvatarKey: String = "studio",
    val title: String = "",
    val frequency: String = "104.7 FM",
    val handle: String = "",
    val category: String = "Global",
    val description: String = "",
    val coverKey: String? = "studio",
    val visibility: String = "public",
    val status: String = "live",
    val listenersCount: Long = 0L,
    val subscribersCount: Long = 1L,
    val totalGiftsCents: Long = 0L,
    val lastSparkGiftId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RadioBroadcast(
    @DocumentId val id: String = "",
    val channelId: String = "",
    val channelVisibility: String = "public",
    val ownerId: String = "",
    val ownerName: String = "",
    val title: String = "",
    val text: String = "",
    val audioBase64: String? = null,
    val audioDurationSec: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RadioSubscription(
    @DocumentId val id: String = "",
    val channelId: String = "",
    val userId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

fun formatListenerCount(count: Long): String {
    val safe = count.coerceAtLeast(0L)
    return when {
        safe >= 1_000_000_000L -> String.format(java.util.Locale.US, "%.1fB", safe / 1_000_000_000.0)
        safe >= 1_000_000L -> String.format(java.util.Locale.US, "%.1fM", safe / 1_000_000.0)
        safe >= 1_000L -> String.format(java.util.Locale.US, "%.1fK", safe / 1_000.0)
        else -> safe.toString()
    }
}


