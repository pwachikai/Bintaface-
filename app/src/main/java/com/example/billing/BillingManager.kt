package com.example.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.consumePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object KindredProducts {
    const val PLUS_MONTHLY = "kindred_plus_monthly"
    const val PLUS_YEARLY = "kindred_plus_yearly"
    const val SUPERLIKES_5 = "kindred_superlikes_5"
    const val BOOST_1 = "kindred_boost_1"
    const val SPARKS_5 = "kindred_spark_5"
    const val RADIO_ACCESS = "bintaface_radio_access"
    const val LEVEL_UPGRADE_PREFIX = "bintaface_level_"

    val SUBSCRIPTION_IDS = listOf(PLUS_MONTHLY, PLUS_YEARLY)
    val LEVEL_UPGRADE_IDS = (1..10).map { "$LEVEL_UPGRADE_PREFIX$it" }
    val INAPP_IDS = listOf(SUPERLIKES_5, BOOST_1, SPARKS_5, RADIO_ACCESS) + LEVEL_UPGRADE_IDS

    fun levelProductId(rank: Int): String = "$LEVEL_UPGRADE_PREFIX${rank.coerceIn(1, 10)}"

    fun levelRankFromProductId(productId: String): Int? =
        if (productId.startsWith(LEVEL_UPGRADE_PREFIX)) {
            productId.removePrefix(LEVEL_UPGRADE_PREFIX).toIntOrNull()?.takeIf { it in 1..10 }
        } else {
            null
        }

    fun grossCentsForProduct(productId: String): Long = when {
        productId == PLUS_YEARLY -> 8999L
        productId == PLUS_MONTHLY -> 1499L
        productId == SUPERLIKES_5 -> 499L
        productId == BOOST_1 -> 399L
        productId == SPARKS_5 -> 499L
        productId == RADIO_ACCESS -> 150L
        levelRankFromProductId(productId) != null -> 350L
        else -> 499L
    }
}

data class StoreOffer(
    val productId: String,
    val title: String,
    val subtitle: String,
    val formattedPrice: String,
    val isSubscription: Boolean,
    val badge: String? = null,
    val isAvailableInPlayStore: Boolean = false
)

sealed interface BillingEvent {
    data class SubscriptionActivated(val productId: String, val purchaseToken: String) : BillingEvent
    data class ConsumableGranted(val productId: String, val quantity: Int, val purchaseToken: String) : BillingEvent
    data class LevelUpgradePurchased(val levelRank: Int, val productId: String, val purchaseToken: String) : BillingEvent
    data class RadioAccessPurchased(val productId: String, val purchaseToken: String) : BillingEvent
    data class PendingPurchase(val productId: String) : BillingEvent
    data class Message(val text: String) : BillingEvent
}

class BillingManager(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _isPlayStoreReady = MutableStateFlow(false)
    val isPlayStoreReady: StateFlow<Boolean> = _isPlayStoreReady.asStateFlow()

    private val _billingNotice = MutableStateFlow<String?>(null)
    val billingNotice: StateFlow<String?> = _billingNotice.asStateFlow()

    private val _offers = MutableStateFlow(defaultCatalog())
    val offers: StateFlow<List<StoreOffer>> = _offers.asStateFlow()

    private val _events = MutableSharedFlow<BillingEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<BillingEvent> = _events.asSharedFlow()

    private var productDetailsMap: Map<String, ProductDetails> = emptyMap()
    private val processedTokens = mutableSetOf<String>()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { purchase ->
                    scope.launch { handlePurchase(purchase) }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _events.tryEmit(BillingEvent.Message("Purchase cancelled."))
            }
            else -> {
                Log.w(TAG, "Billing error ${billingResult.responseCode}: ${billingResult.debugMessage}")
                _events.tryEmit(
                    BillingEvent.Message(
                        billingResult.debugMessage.ifBlank { "Google Play Billing returned code ${billingResult.responseCode}" }
                    )
                )
            }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            scope.launch { refreshCatalogAndPurchases() }
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _isPlayStoreReady.value = true
                    _billingNotice.value = null
                    scope.launch { refreshCatalogAndPurchases() }
                } else {
                    _isPlayStoreReady.value = false
                    _billingNotice.value =
                        "Google Play Billing unavailable (${billingResult.debugMessage.ifBlank { "code ${billingResult.responseCode}" }})."
                    Log.w(TAG, "Billing setup failed: ${billingResult.responseCode} ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                _isPlayStoreReady.value = false
            }
        })
    }

    fun refreshPurchases() {
        if (!billingClient.isReady) {
            startConnection()
            return
        }
        scope.launch { queryExistingPurchases() }
    }

    private suspend fun refreshCatalogAndPurchases() {
        val subsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                KindredProducts.SUBSCRIPTION_IDS.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()

        val inappParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                KindredProducts.INAPP_IDS.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                }
            )
            .build()

        val subsResult = billingClient.queryProductDetails(subsParams)
        val inappResult = billingClient.queryProductDetails(inappParams)

        val fetched = mutableMapOf<String, ProductDetails>()
        subsResult.productDetailsList.orEmpty().forEach { fetched[it.productId] = it }
        inappResult.productDetailsList.orEmpty().forEach { fetched[it.productId] = it }
        productDetailsMap = fetched

        if (fetched.isEmpty()) {
            _billingNotice.value =
                "Play Billing connected. Register SKUs (${KindredProducts.SUBSCRIPTION_IDS.plus(KindredProducts.INAPP_IDS).joinToString()}) in Google Play Console to enable live checkout."
        } else {
            _billingNotice.value = null
            _offers.value = defaultCatalog().map { base ->
                val details = fetched[base.productId] ?: return@map base
                val livePrice = if (base.isSubscription) {
                    details.subscriptionOfferDetails
                        ?.firstOrNull()
                        ?.pricingPhases
                        ?.pricingPhaseList
                        ?.firstOrNull()
                        ?.formattedPrice
                } else {
                    details.oneTimePurchaseOfferDetails?.formattedPrice
                }
                base.copy(
                    formattedPrice = livePrice ?: base.formattedPrice,
                    isAvailableInPlayStore = true
                )
            }
        }

        queryExistingPurchases()
    }

    private suspend fun queryExistingPurchases() {
        val subsPurchases = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )
        if (subsPurchases.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            subsPurchases.purchasesList.forEach { handlePurchase(it) }
        }

        val inappPurchases = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        if (inappPurchases.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            inappPurchases.purchasesList.forEach { handlePurchase(it) }
        }
    }

    fun isLevelProductReadyInPlay(rank: Int): Boolean =
        billingClient.isReady && productDetailsMap.containsKey(KindredProducts.levelProductId(rank))

    fun isRadioProductReadyInPlay(): Boolean =
        billingClient.isReady && productDetailsMap.containsKey(KindredProducts.RADIO_ACCESS)

    fun launchRadioAccessPurchase(activity: Activity, userId: String): Boolean {
        if (!billingClient.isReady) {
            startConnection()
            return false
        }
        val details = productDetailsMap[KindredProducts.RADIO_ACCESS] ?: return false
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .setObfuscatedAccountId(userId.take(64))
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _events.tryEmit(
                BillingEvent.Message(
                    result.debugMessage.ifBlank { "Google Play checkout could not start (code ${result.responseCode})." }
                )
            )
            return false
        }
        return true
    }

    fun launchLevelUpgradePurchase(activity: Activity, rank: Int, userId: String): Boolean {
        val productId = KindredProducts.levelProductId(rank)
        if (!billingClient.isReady) {
            startConnection()
            return false
        }
        val details = productDetailsMap[productId] ?: return false
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .setObfuscatedAccountId(userId.take(64))
            .build()
        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _events.tryEmit(
                BillingEvent.Message(
                    result.debugMessage.ifBlank { "Google Play checkout could not start (code ${result.responseCode})." }
                )
            )
            return false
        }
        return true
    }

    fun launchPurchase(activity: Activity, productId: String, userId: String) {
        if (!billingClient.isReady) {
            startConnection()
            _events.tryEmit(
                BillingEvent.Message(
                    "Google Play Billing is not connected on this device. Install via Play Console internal testing to complete checkout."
                )
            )
            return
        }

        val details = productDetailsMap[productId]
        if (details == null) {
            _events.tryEmit(
                BillingEvent.Message(
                    "Product \"$productId\" is not yet configured in Google Play Console for this package."
                )
            )
            return
        }

        val productParamsBuilder = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)

        if (details.productType == BillingClient.ProductType.SUBS) {
            val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
            if (offerToken.isNullOrBlank()) {
                _events.tryEmit(BillingEvent.Message("No active subscription offer found for $productId."))
                return
            }
            productParamsBuilder.setOfferToken(offerToken)
        }

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParamsBuilder.build()))
            .setObfuscatedAccountId(userId.take(64))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _events.tryEmit(
                BillingEvent.Message(
                    result.debugMessage.ifBlank { "Unable to launch checkout (code ${result.responseCode})." }
                )
            )
        }
    }

    private suspend fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
            purchase.products.forEach { productId ->
                _events.emit(BillingEvent.PendingPurchase(productId))
            }
            return
        }

        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (purchase.purchaseToken in processedTokens) return

        val isSubscription = purchase.products.any { it in KindredProducts.SUBSCRIPTION_IDS }
        if (isSubscription) {
            if (!purchase.isAcknowledged) {
                val ackResult = billingClient.acknowledgePurchase(
                    AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                )
                if (ackResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.e(TAG, "Failed to acknowledge subscription: ${ackResult.debugMessage}")
                    return
                }
            }
            processedTokens.add(purchase.purchaseToken)
            val subId = purchase.products.firstOrNull { it in KindredProducts.SUBSCRIPTION_IDS }
                ?: KindredProducts.PLUS_MONTHLY
            _events.emit(BillingEvent.SubscriptionActivated(subId, purchase.purchaseToken))
        } else {
            val consumeResult = billingClient.consumePurchase(
                ConsumeParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            )
            if (consumeResult.billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.e(TAG, "Failed to consume product: ${consumeResult.billingResult.debugMessage}")
                return
            }
            processedTokens.add(purchase.purchaseToken)
            purchase.products.forEach { productId ->
                val levelRank = KindredProducts.levelRankFromProductId(productId)
                when {
                    levelRank != null -> {
                        _events.emit(
                            BillingEvent.LevelUpgradePurchased(
                                levelRank = levelRank,
                                productId = productId,
                                purchaseToken = purchase.purchaseToken
                            )
                        )
                    }
                    productId == KindredProducts.RADIO_ACCESS -> {
                        _events.emit(
                            BillingEvent.RadioAccessPurchased(
                                productId = productId,
                                purchaseToken = purchase.purchaseToken
                            )
                        )
                    }
                    else -> {
                        _events.emit(
                            BillingEvent.ConsumableGranted(
                                productId = productId,
                                quantity = purchase.quantity.coerceAtLeast(1),
                                purchaseToken = purchase.purchaseToken
                            )
                        )
                    }
                }
            }
        }
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    private fun defaultCatalog(): List<StoreOffer> = listOf(
        StoreOffer(
            productId = KindredProducts.PLUS_YEARLY,
            title = "Bintaface+ Annual",
            subtitle = "Unlimited likes, Likes You reveal, Bintaface+ crown badge & creator priority",
            formattedPrice = "$89.99 / yr",
            isSubscription = true,
            badge = "Best Value • Save 50%"
        ),
        StoreOffer(
            productId = KindredProducts.PLUS_MONTHLY,
            title = "Bintaface+ Monthly",
            subtitle = "Full Bintaface+ membership billed monthly, cancel anytime",
            formattedPrice = "$14.99 / mo",
            isSubscription = true
        ),
        StoreOffer(
            productId = KindredProducts.SUPERLIKES_5,
            title = "5 Superlikes Pack",
            subtitle = "Stand out immediately at the top of someone's Likes You inbox",
            formattedPrice = "$4.99",
            isSubscription = false,
            badge = "Popular"
        ),
        StoreOffer(
            productId = KindredProducts.BOOST_1,
            title = "Spotlight Boost",
            subtitle = "Highlight your profile, posts, Reels & streams with a Spotlight badge",
            formattedPrice = "$3.99",
            isSubscription = false
        ),
        StoreOffer(
            productId = KindredProducts.SPARKS_5,
            title = "5 Creator Sparks Pack",
            subtitle = "Tip creators on Posts, Reels, Videos, Live & Channels ($1/Spark: 80% creator, 20% app)",
            formattedPrice = "$4.99",
            isSubscription = false,
            badge = "Pay Creators"
        )
    )

    private companion object {
        const val TAG = "BintafaceBilling"
    }
}
