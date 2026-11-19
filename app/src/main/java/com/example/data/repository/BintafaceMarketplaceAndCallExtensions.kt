package com.example.data.repository

import com.example.data.model.CallSession
import com.example.data.model.Conversation
import com.example.data.model.DEFAULT_USD_TO_NGN_RATE
import com.example.data.model.MatchThread
import com.example.data.model.SHOP_CATEGORIES
import com.example.data.model.Shop
import com.example.data.model.ShopOrder
import com.example.data.model.ShopProduct
import com.example.data.model.computeShop8020Split
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

private const val MAX_VOICE_NOTE_BASE64_LEN = 135_000

// =========================================================================
// 1. Voice Notes & Call Log Messages (Conversations & Match Chats)
// =========================================================================

suspend fun KindredRepository.sendVoiceNoteToConversation(
    conversation: Conversation,
    senderName: String,
    voiceNoteBase64: String,
    durationSec: Long
): Result<Unit> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    if (voiceNoteBase64.isBlank()) {
        return Result.failure(IllegalArgumentException("Voice note recording is empty."))
    }
    if (voiceNoteBase64.length > MAX_VOICE_NOTE_BASE64_LEN) {
        return Result.failure(IllegalArgumentException("Voice note is too long. Please keep recordings under 35 seconds."))
    }
    val safeDur = durationSec.coerceIn(1L, 180L)
    val summary = "🎤 Voice note (${formatDurationMMSS(safeDur)})"
    val convRef = db.collection("conversations").document(conversation.id)
    val msgRef = convRef.collection("messages").document()
    return try {
        val batch = db.batch()
        batch.set(
            msgRef,
            mapOf(
                "senderId" to uid,
                "senderName" to senderName.take(40).ifBlank { "Member" },
                "text" to summary,
                "participantIds" to conversation.participantIds,
                "messageType" to "voice_note",
                "voiceNoteBase64" to voiceNoteBase64,
                "voiceNoteDurationSec" to safeDur,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            convRef,
            mapOf(
                "lastMessage" to summary,
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, msgRef.path)
        Result.failure(e)
    }
}

suspend fun KindredRepository.sendVoiceNoteToMatch(
    match: MatchThread,
    senderName: String,
    voiceNoteBase64: String,
    durationSec: Long
): Result<Unit> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    if (voiceNoteBase64.isBlank()) {
        return Result.failure(IllegalArgumentException("Voice note recording is empty."))
    }
    if (voiceNoteBase64.length > MAX_VOICE_NOTE_BASE64_LEN) {
        return Result.failure(IllegalArgumentException("Voice note is too long. Please keep recordings under 35 seconds."))
    }
    val safeDur = durationSec.coerceIn(1L, 180L)
    val summary = "🎤 Voice note (${formatDurationMMSS(safeDur)})"
    val matchRef = db.collection("matches").document(match.id)
    val msgRef = matchRef.collection("messages").document()
    return try {
        val batch = db.batch()
        batch.set(
            msgRef,
            mapOf(
                "senderId" to uid,
                "senderName" to senderName.take(40).ifBlank { "Member" },
                "text" to summary,
                "participantIds" to match.participantIds,
                "messageType" to "voice_note",
                "voiceNoteBase64" to voiceNoteBase64,
                "voiceNoteDurationSec" to safeDur,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            matchRef,
            mapOf(
                "lastMessage" to summary,
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, msgRef.path)
        Result.failure(e)
    }
}

suspend fun KindredRepository.appendCallLogMessage(
    session: CallSession,
    senderName: String,
    callStatus: String,
    durationSec: Long
): Result<Unit> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val cleanStatus = callStatus.takeIf { it in setOf("completed", "missed", "declined") } ?: "completed"
    val icon = if (session.callType == "video") "📹" else "📞"
    val typeLabel = if (session.callType == "video") "Video Call" else "Voice Call"
    val text = when (cleanStatus) {
        "declined" -> "$icon $typeLabel • Declined"
        "missed" -> "$icon $typeLabel • Unanswered"
        else -> "$icon $typeLabel • ${formatDurationMMSS(durationSec)}"
    }
    val parentCollection = if (session.threadType == "match") "matches" else "conversations"
    val threadRef = db.collection(parentCollection).document(session.threadId)
    val msgRef = threadRef.collection("messages").document()
    return try {
        val batch = db.batch()
        batch.set(
            msgRef,
            mapOf(
                "senderId" to uid,
                "senderName" to senderName.take(40).ifBlank { "Member" },
                "text" to text,
                "participantIds" to session.participantIds,
                "messageType" to "call_log",
                "callType" to session.callType,
                "callDurationSec" to durationSec.coerceIn(0L, 86400L),
                "callStatus" to cleanStatus,
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            threadRef,
            mapOf(
                "lastMessage" to text,
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.commit().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, msgRef.path)
        Result.failure(e)
    }
}

// =========================================================================
// 2. Real-Time Voice & Video Call Signaling (/call_sessions)
// =========================================================================

fun KindredRepository.observeMyCallSessions(): Flow<List<CallSession>> = flow {
    val uid = requireUserId()
    val path = "call_sessions"
    emitAll(
        db.collection("call_sessions")
            .whereArrayContains("participantIds", uid)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.updatedAt?.toDate()?.time ?: it.createdAt?.toDate()?.time ?: 0L }
            }
            .handleSnapshotErrors(OperationType.LIST, path, uid)
    )
}

suspend fun KindredRepository.startCallSession(
    threadId: String,
    threadType: String,
    participantIds: List<String>,
    participantNames: Map<String, String>,
    callerName: String,
    callerAvatarKey: String,
    callType: String
): Result<CallSession> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val cleanType = if (callType == "video") "video" else "voice"
    val cleanThreadType = if (threadType == "match") "match" else "conversation"
    val callees = participantIds.filter { it != uid }
    if (callees.isEmpty()) {
        return Result.failure(IllegalArgumentException("No other participant available to call."))
    }
    val callRef = db.collection("call_sessions").document()
    return try {
        db.runTransaction { tx ->
            tx.set(
                callRef,
                mapOf(
                    "callerId" to uid,
                    "callerName" to callerName.take(40).ifBlank { "Member" },
                    "callerAvatarKey" to callerAvatarKey.take(40).ifBlank { "coffee" },
                    "calleeIds" to callees,
                    "participantIds" to participantIds,
                    "participantNames" to participantNames,
                    "threadId" to threadId,
                    "threadType" to cleanThreadType,
                    "callType" to cleanType,
                    "status" to "ringing",
                    "callerMuted" to false,
                    "callerVideoOn" to (cleanType == "video"),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            null
        }.await()
        Result.success(
            CallSession(
                id = callRef.id,
                callerId = uid,
                callerName = callerName.take(40).ifBlank { "Member" },
                callerAvatarKey = callerAvatarKey.take(40).ifBlank { "coffee" },
                calleeIds = callees,
                participantIds = participantIds,
                participantNames = participantNames,
                threadId = threadId,
                threadType = cleanThreadType,
                callType = cleanType,
                status = "ringing",
                callerMuted = false,
                callerVideoOn = (cleanType == "video")
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, callRef.path)
        Result.failure(e)
    }
}

suspend fun KindredRepository.acceptCallSession(callId: String): Result<Unit> {
    runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val callRef = db.collection("call_sessions").document(callId)
    return try {
        callRef.update(
            mapOf(
                "status" to "active",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, callRef.path)
        Result.failure(e)
    }
}

suspend fun KindredRepository.declineCallSession(callId: String): Result<Unit> {
    runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val callRef = db.collection("call_sessions").document(callId)
    return try {
        callRef.update(
            mapOf(
                "status" to "declined",
                "endedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, callRef.path)
        Result.failure(e)
    }
}

suspend fun KindredRepository.endCallSession(
    callId: String,
    durationSec: Long
): Result<Unit> {
    runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val callRef = db.collection("call_sessions").document(callId)
    return try {
        callRef.update(
            mapOf(
                "status" to "ended",
                "durationSec" to durationSec.coerceIn(0L, 86400L),
                "endedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, callRef.path)
        Result.failure(e)
    }
}

suspend fun KindredRepository.updateCallMediaState(
    callId: String,
    muted: Boolean,
    videoOn: Boolean
): Result<Unit> {
    runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val callRef = db.collection("call_sessions").document(callId)
    return try {
        callRef.update(
            mapOf(
                "callerMuted" to muted,
                "callerVideoOn" to videoOn,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, callRef.path)
        Result.failure(e)
    }
}

// =========================================================================
// 3. Creator & Merchant Shops Marketplace (/shops, /products, /shop_orders)
// =========================================================================

fun KindredRepository.observeShops(): Flow<List<Shop>> = flow {
    val uid = requireUserId()
    val path = "shops"
    emitAll(
        db.collection("shops")
            .whereEqualTo("visibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Shop::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedWith(
                    compareByDescending<Shop> { it.ordersCount }
                        .thenByDescending { it.productsCount }
                        .thenByDescending { it.updatedAt?.toDate()?.time ?: 0L }
                )
            }
            .handleSnapshotErrors(OperationType.LIST, path, uid)
    )
}

fun KindredRepository.observeShopProducts(shopId: String): Flow<List<ShopProduct>> = flow {
    val uid = requireUserId()
    val path = "shops/$shopId/products"
    emitAll(
        db.collection("shops").document(shopId).collection("products")
            .whereEqualTo("shopVisibility", "public")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ShopProduct::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .handleSnapshotErrors(OperationType.LIST, path, uid)
    )
}

fun KindredRepository.observeMyBuyerOrders(buyerId: String): Flow<List<ShopOrder>> = flow {
    val uid = requireUserId()
    val path = "shop_orders"
    emitAll(
        db.collection("shop_orders")
            .whereEqualTo("buyerId", buyerId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ShopOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .handleSnapshotErrors(OperationType.LIST, path, uid)
    )
}

fun KindredRepository.observeMySellerOrders(sellerId: String): Flow<List<ShopOrder>> = flow {
    val uid = requireUserId()
    val path = "shop_orders"
    emitAll(
        db.collection("shop_orders")
            .whereEqualTo("sellerId", sellerId)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ShopOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }.sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .handleSnapshotErrors(OperationType.LIST, path, uid)
    )
}

suspend fun KindredRepository.createShop(
    ownerName: String,
    ownerAvatarKey: String,
    name: String,
    handle: String,
    category: String,
    description: String,
    currency: String,
    city: String,
    coverKey: String,
    photoBase64: String?
): Result<String> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val cleanName = name.trim().take(80)
    if (cleanName.length < 2) {
        return Result.failure(IllegalArgumentException("Shop name must be at least 2 characters."))
    }
    val rawHandle = handle.trim().removePrefix("@").lowercase().replace(Regex("[^a-z0-9._-]"), "")
    val finalHandle = "@" + (rawHandle.ifBlank { cleanName.lowercase().replace(Regex("[^a-z0-9]"), "").take(18) }.ifBlank { "shop" }).take(38)
    val cleanCat = if (category in SHOP_CATEGORIES) category else "General"
    val cleanCurrency = if (currency.uppercase() == "USD") "USD" else "NGN"
    val shopRef = db.collection("shops").document()
    return try {
        val data = mutableMapOf<String, Any>(
            "ownerId" to uid,
            "ownerName" to ownerName.take(40).ifBlank { "Seller" },
            "ownerAvatarKey" to ownerAvatarKey.take(40).ifBlank { "studio" },
            "name" to cleanName,
            "handle" to finalHandle,
            "category" to cleanCat,
            "description" to description.trim().take(400),
            "currency" to cleanCurrency,
            "city" to city.trim().take(60),
            "coverKey" to coverKey.take(40).ifBlank { "studio" },
            "visibility" to "public",
            "productsCount" to 0L,
            "ordersCount" to 0L,
            "totalSalesCents" to 0L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!photoBase64.isNullOrBlank()) {
            data["photoBase64"] = photoBase64
        }
        shopRef.set(data).await()
        Result.success(shopRef.id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, shopRef.path)
        Result.failure(e)
    }
}

suspend fun KindredRepository.updateShop(
    shopId: String,
    name: String,
    category: String,
    description: String,
    currency: String,
    city: String,
    coverKey: String,
    photoBase64: String?
): Result<Unit> {
    runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val cleanName = name.trim().take(80)
    if (cleanName.length < 2) {
        return Result.failure(IllegalArgumentException("Shop name must be at least 2 characters."))
    }
    val cleanCat = if (category in SHOP_CATEGORIES) category else "General"
    val cleanCurrency = if (currency.uppercase() == "USD") "USD" else "NGN"
    val shopRef = db.collection("shops").document(shopId)
    return try {
        val data = mutableMapOf<String, Any>(
            "name" to cleanName,
            "category" to cleanCat,
            "description" to description.trim().take(400),
            "currency" to cleanCurrency,
            "city" to city.trim().take(60),
            "coverKey" to coverKey.take(40).ifBlank { "studio" },
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!photoBase64.isNullOrBlank()) {
            data["photoBase64"] = photoBase64
        }
        shopRef.update(data).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, shopRef.path)
        Result.failure(e)
    }
}

suspend fun KindredRepository.createShopProduct(
    shop: Shop,
    title: String,
    description: String,
    category: String,
    priceUsdCents: Long,
    priceNgn: Long,
    stockCount: Long,
    imageKey: String?,
    photoBase64: String?
): Result<String> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val cleanTitle = title.trim().take(100)
    if (cleanTitle.length < 2) {
        return Result.failure(IllegalArgumentException("Product title must be at least 2 characters."))
    }
    val safeUsd = priceUsdCents.coerceIn(50L, 100000L)
    val safeNgn = priceNgn.coerceIn(100L, 10000000L)
    val safeStock = stockCount.coerceIn(1L, 100000L)
    val shopRef = db.collection("shops").document(shop.id)
    val prodRef = shopRef.collection("products").document()
    return try {
        db.runTransaction { tx ->
            val currentShop = tx.get(shopRef)
            val currentCount = currentShop.getLong("productsCount") ?: 0L
            val prodData = mutableMapOf<String, Any>(
                "shopId" to shop.id,
                "shopName" to shop.name.take(80),
                "shopVisibility" to "public",
                "ownerId" to uid,
                "title" to cleanTitle,
                "description" to description.trim().take(500),
                "category" to category.trim().take(40).ifBlank { shop.category },
                "priceUsdCents" to safeUsd,
                "priceNgn" to safeNgn,
                "stockCount" to safeStock,
                "isAvailable" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!imageKey.isNullOrBlank()) {
                prodData["imageKey"] = imageKey.take(40)
            }
            if (!photoBase64.isNullOrBlank()) {
                prodData["photoBase64"] = photoBase64
            }
            tx.set(prodRef, prodData)
            tx.update(
                shopRef,
                mapOf(
                    "productsCount" to currentCount + 1L,
                    "lastProductId" to prodRef.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            null
        }.await()
        Result.success(prodRef.id)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, prodRef.path)
        Result.failure(e)
    }
}

suspend fun KindredRepository.placeShopOrder(
    shop: Shop,
    product: ShopProduct,
    buyerName: String,
    quantity: Long,
    currency: String,
    deliveryNote: String,
    usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE
): Result<ShopOrder> {
    val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    if (shop.ownerId == uid) {
        return Result.failure(IllegalArgumentException("You cannot buy products from your own shop."))
    }
    val safeQty = quantity.coerceIn(1L, 20L)
    val cleanCurrency = if (currency.uppercase() == "USD") "USD" else "NGN"
    val safeRate = usdToNgnRate.coerceIn(100L, 10000L)
    val unitPriceCents = if (cleanCurrency == "NGN") {
        ((product.priceNgn * 100L) / safeRate).coerceIn(50L, 100000L)
    } else {
        product.priceUsdCents.coerceIn(50L, 100000L)
    }
    val totalCents = unitPriceCents * safeQty
    val ngnAmount = if (cleanCurrency == "NGN") {
        (product.priceNgn * safeQty).coerceIn(100L, 20000000L)
    } else {
        ((totalCents * safeRate) / 100L).coerceIn(100L, 20000000L)
    }
    val (sellerShareCents, adminFeeCents) = computeShop8020Split(totalCents)
    val cleanBuyer = buyerName.trim().take(40).ifBlank { "Member" }
    val cleanNote = deliveryNote.trim().take(240)

    val shopRef = db.collection("shops").document(shop.id)
    val prodRef = shopRef.collection("products").document(product.id)
    val orderRef = db.collection("shop_orders").document()
    val ledgerRef = db.collection("revenue_ledger").document(orderRef.id)

    return try {
        db.runTransaction { tx ->
            val currentShop = tx.get(shopRef)
            val currentProd = tx.get(prodRef)
            val currentStock = currentProd.getLong("stockCount") ?: 0L
            if (currentStock < safeQty) {
                throw IllegalStateException("Only $currentStock left in stock.")
            }
            val newStock = currentStock - safeQty
            val currentOrders = currentShop.getLong("ordersCount") ?: 0L
            val currentSales = currentShop.getLong("totalSalesCents") ?: 0L

            val orderMap = mutableMapOf<String, Any>(
                "shopId" to shop.id,
                "shopName" to shop.name.take(80),
                "productId" to product.id,
                "productTitle" to product.title.take(100),
                "buyerId" to uid,
                "buyerName" to cleanBuyer,
                "sellerId" to shop.ownerId,
                "sellerName" to shop.ownerName.take(40).ifBlank { "Seller" },
                "quantity" to safeQty,
                "currency" to cleanCurrency,
                "unitPriceCents" to unitPriceCents,
                "totalCents" to totalCents,
                "ngnAmount" to ngnAmount,
                "sellerShareCents" to sellerShareCents,
                "adminFeeCents" to adminFeeCents,
                "status" to "placed",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (cleanNote.isNotBlank()) {
                orderMap["deliveryNote"] = cleanNote
            }
            tx.set(orderRef, orderMap)
            tx.set(
                ledgerRef,
                mapOf(
                    "source" to "shop_order",
                    "productId" to product.id,
                    "payerId" to uid,
                    "recipientId" to shop.ownerId,
                    "grossCents" to totalCents,
                    "adminShareCents" to adminFeeCents,
                    "creatorShareCents" to sellerShareCents,
                    "referenceToken" to shop.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            tx.update(
                shopRef,
                mapOf(
                    "ordersCount" to currentOrders + 1L,
                    "totalSalesCents" to currentSales + totalCents,
                    "lastOrderId" to orderRef.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            tx.update(
                prodRef,
                mapOf(
                    "stockCount" to newStock,
                    "isAvailable" to (newStock > 0L),
                    "lastOrderId" to orderRef.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            null
        }.await()

        Result.success(
            ShopOrder(
                id = orderRef.id,
                shopId = shop.id,
                shopName = shop.name,
                productId = product.id,
                productTitle = product.title,
                buyerId = uid,
                buyerName = cleanBuyer,
                sellerId = shop.ownerId,
                sellerName = shop.ownerName,
                quantity = safeQty,
                currency = cleanCurrency,
                unitPriceCents = unitPriceCents,
                totalCents = totalCents,
                ngnAmount = ngnAmount,
                sellerShareCents = sellerShareCents,
                adminFeeCents = adminFeeCents,
                deliveryNote = cleanNote.ifBlank { null },
                status = "placed"
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, orderRef.path)
        Result.failure(e)
    }
}

suspend fun KindredRepository.updateShopOrderStatus(
    orderId: String,
    newStatus: String
): Result<Unit> {
    runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
    val cleanStatus = if (newStatus == "cancelled") "cancelled" else "fulfilled"
    val orderRef = db.collection("shop_orders").document(orderId)
    return try {
        orderRef.update(
            mapOf(
                "status" to cleanStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, orderRef.path)
        Result.failure(e)
    }
}

private fun formatDurationMMSS(durationSec: Long): String {
    val s = durationSec.coerceAtLeast(0L)
    val mins = s / 60L
    val rem = s % 60L
    return "%d:%02d".format(mins, rem)
}
