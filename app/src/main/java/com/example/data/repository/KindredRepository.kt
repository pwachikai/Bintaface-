package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.billing.KindredProducts
import com.example.data.model.APP_ADMIN_EMAIL
import com.example.data.model.AdRevenueEvent
import com.example.data.model.AdminPayoutAccount
import com.example.data.model.AdminWithdrawal
import com.example.data.model.ChannelBroadcast
import com.example.data.model.DEFAULT_ADMIN_ACCOUNT_HOLDER
import com.example.data.model.DEFAULT_ADMIN_ACCOUNT_NUMBER
import com.example.data.model.DEFAULT_ADMIN_BANK_NAME
import com.example.data.model.DEFAULT_ADMOB_BANNER_UNIT_ID
import com.example.data.model.DEFAULT_ADMOB_REWARDED_UNIT_ID
import com.example.data.model.DEFAULT_APP_DOWNLOAD_URL
import com.example.data.model.DEFAULT_USD_TO_NGN_RATE
import com.example.data.model.computeAd7030Split
import com.example.data.model.computeNgnWhole
import com.example.data.model.ChannelSubscription
import com.example.data.model.ChatMessage
import com.example.data.model.Conversation
import com.example.data.model.DirectMessage
import com.example.data.model.Follow
import com.example.data.model.LevelUpgradeOrder
import com.example.data.model.LiveMessage
import com.example.data.model.LiveRoom
import com.example.data.model.MatchThread
import com.example.data.model.levelTierForId
import com.example.data.model.nextLevelTierForRank
import com.example.data.model.PageFollow
import com.example.data.model.PagePost
import com.example.data.model.PagePostLike
import com.example.data.model.PayoutRequest
import com.example.data.model.PlatformSettings
import com.example.data.model.Post
import com.example.data.model.PostComment
import com.example.data.model.PostLike
import com.example.data.model.PostRepost
import com.example.data.model.PublicProfile
import com.example.data.model.RADIO_ACCESS_FEE_NGN
import com.example.data.model.RADIO_ACCESS_USD_CENTS
import com.example.data.model.RADIO_CATEGORIES
import com.example.data.model.RadioBroadcast
import com.example.data.model.RadioChannel
import com.example.data.model.RadioPass
import com.example.data.model.RevenueLedgerEntry
import com.example.data.model.SocialChannel
import com.example.data.model.SocialPage
import com.example.data.model.SparkGift
import com.example.data.model.SwipeRecord
import com.example.data.model.UserPrivate
import com.example.data.model.isDesignatedAdminEmail
import com.example.data.model.maskAccountNumber
import com.example.data.model.sortedMatchId
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.example.data.util.unwrapFirestoreException
import java.util.UUID
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KindredRepository(
    internal val db: FirebaseFirestore,
    internal val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    internal fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun todayIsoDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun deterministicDocId(prefix: String, raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
        val hex = digest.joinToString("") { "%02x".format(it) }.take(32)
        return "${prefix}_$hex"
    }

    internal fun <T> Flow<T>.handleSnapshotErrors(
        operationType: OperationType,
        path: String,
        expectedUid: String
    ): Flow<T> = catch { error ->
        val firestoreError = error.unwrapFirestoreException()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, operationType, path)
        }
        throw error
    }

    // --- User Private & Public Profile ---

    fun observePrivateSettings(userId: String): Flow<UserPrivate?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            UserPrivate::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .handleSnapshotErrors(OperationType.GET, path, userId)
        )
    }

    fun observeMyPublicProfile(userId: String): Flow<PublicProfile?> = flow {
        val path = "public_profiles/$userId"
        emitAll(
            db.collection("public_profiles").document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            PublicProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .handleSnapshotErrors(OperationType.GET, path, userId)
        )
    }

    fun observeDiscoverableProfiles(): Flow<List<PublicProfile>> = flow {
        val uid = requireUserId()
        val path = "public_profiles"
        emitAll(
            db.collection("public_profiles")
                .whereEqualTo("isDiscoverable", true)
                .limit(60)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PublicProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun saveProfile(
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
        email: String?
    ): Result<Unit> {
        val uid = requireUserId()
        val privateRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        return try {
            val existingPrivate = privateRef.get().await()
            val existingPublic = publicRef.get().await()

            val batch = db.batch()
            if (!existingPrivate.exists()) {
                val privatePayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "showInDiscovery" to isDiscoverable,
                    "preferredIntent" to preferredIntent,
                    "isPlusMember" to false,
                    "superlikesBalance" to 1L,
                    "boostsBalance" to 0L,
                    "sparksBalance" to 0L,
                    "dailyLikesUsed" to 0L,
                    "dailyLikesDate" to todayIsoDate(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!email.isNullOrBlank()) {
                    privatePayload["email"] = email.take(120)
                }
                batch.set(privateRef, privatePayload)
            } else {
                val updateMap = mutableMapOf<String, Any>(
                    "showInDiscovery" to isDiscoverable,
                    "preferredIntent" to preferredIntent,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!email.isNullOrBlank()) {
                    updateMap["email"] = email.take(120)
                }
                batch.update(privateRef, updateMap)
            }

            val cleanInterests = interests.map { it.trim().take(30) }.filter { it.isNotEmpty() }.take(8)
            val cleanPhoto = photoBase64?.takeIf { it.isNotBlank() && it.length <= 200_000 }
            val currentIsPlus = existingPrivate.getBoolean("isPlusMember") ?: false
            val currentVipLevel = existingPrivate.getString("vipLevel") ?: "none"
            val currentVipRank = existingPrivate.getLong("vipLevelRank") ?: 0L
            val currentUpgradeId = existingPrivate.getString("lastLevelUpgradeId")
            val currentHasRadio = existingPrivate.getBoolean("hasRadioAccess") ?: false
            val currentRadioPassId = existingPrivate.getString("radioPassId")

            val cleanName = displayName.trim().take(40).ifEmpty { "BINTAFACE Member" }
            val cleanHandle = "@" + cleanName.lowercase(Locale.US).replace(Regex("[^a-z0-9]"), "").take(24).ifEmpty { "creator" }

            if (!existingPublic.exists()) {
                val publicPayload = buildMap<String, Any> {
                    put("userId", uid)
                    put("displayName", cleanName)
                    put("handle", cleanHandle)
                    put("age", age.coerceIn(18, 100).toLong())
                    put("city", city.trim().take(60))
                    put("bio", bio.trim().take(300))
                    put("lookingFor", lookingFor)
                    put("promptQuestion", promptQuestion.trim().take(120))
                    put("promptAnswer", promptAnswer.trim().take(240))
                    put("interests", cleanInterests)
                    put("avatarKey", avatarKey.take(40).ifEmpty { "coffee" })
                    if (cleanPhoto != null) put("photoBase64", cleanPhoto)
                    put("isPlusMember", currentIsPlus)
                    put("isBoosted", false)
                    if (currentVipLevel != "none" && currentVipRank >= 1L && !currentUpgradeId.isNullOrBlank()) {
                        put("vipLevel", currentVipLevel)
                        put("vipLevelRank", currentVipRank)
                        put("lastLevelUpgradeId", currentUpgradeId)
                    }
                    if (currentHasRadio && !currentRadioPassId.isNullOrBlank()) {
                        put("hasRadioAccess", true)
                        put("radioPassId", currentRadioPassId)
                    }
                    put("isDiscoverable", isDiscoverable)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.set(publicRef, publicPayload)
            } else {
                val updatePayload = buildMap<String, Any> {
                    put("displayName", cleanName)
                    put("handle", cleanHandle)
                    put("age", age.coerceIn(18, 100).toLong())
                    put("city", city.trim().take(60))
                    put("bio", bio.trim().take(300))
                    put("lookingFor", lookingFor)
                    put("promptQuestion", promptQuestion.trim().take(120))
                    put("promptAnswer", promptAnswer.trim().take(240))
                    put("interests", cleanInterests)
                    put("avatarKey", avatarKey.take(40).ifEmpty { "coffee" })
                    if (cleanPhoto != null) put("photoBase64", cleanPhoto)
                    put("isDiscoverable", isDiscoverable)
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.update(publicRef, updatePayload)
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, publicRef.path)
            Result.failure(e)
        }
    }

    suspend fun getPublicProfile(userId: String): Result<PublicProfile?> {
        val path = "public_profiles/$userId"
        return try {
            val snap = db.collection("public_profiles").document(userId).get().await()
            val profile = if (snap.exists()) {
                snap.toObject(
                    PublicProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else null
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    // --- Social Feed Posts, Likes, Comments ---

    fun observeCommunityPosts(): Flow<List<Post>> = flow {
        val uid = requireUserId()
        val path = "posts"
        emitAll(
            db.collection("posts")
                .whereEqualTo("visibility", "community")
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Post::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeMyPostLikes(): Flow<Set<String>> = flow {
        val uid = requireUserId()
        val path = "post_likes"
        emitAll(
            db.collection("post_likes")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PostLike::class.java).map { it.postId }.toSet()
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeMyPostReposts(): Flow<Set<String>> = flow {
        val uid = requireUserId()
        val path = "post_reposts"
        emitAll(
            db.collection("post_reposts")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PostRepost::class.java).map { it.postId }.toSet()
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeMyFollows(): Flow<Set<String>> = flow {
        val uid = requireUserId()
        val path = "follows"
        emitAll(
            db.collection("follows")
                .whereEqualTo("followerId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Follow::class.java).map { it.followedId }.toSet()
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun toggleFollow(
        followedId: String,
        followerName: String,
        currentlyFollowing: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        if (uid == followedId) return Result.success(Unit)
        val docRef = db.collection("follows").document("${uid}_${followedId}")
        return try {
            if (currentlyFollowing) {
                docRef.delete().await()
            } else {
                docRef.set(
                    mapOf(
                        "followerId" to uid,
                        "followedId" to followedId,
                        "followerName" to followerName.trim().take(40).ifEmpty { "Member" },
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createPost(
        authorName: String,
        authorAvatarKey: String,
        authorPhotoBase64: String?,
        authorIsPlus: Boolean = false,
        content: String,
        tag: String,
        imageKey: String?,
        postPhotoBase64: String?,
        mediaType: String = "post",
        title: String = "",
        durationLabel: String = "",
        audioTrack: String = "",
        repostedFromAuthor: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("posts").document()
        val payload = buildMap<String, Any> {
            put("authorId", uid)
            put("authorName", authorName.trim().take(40).ifEmpty { "Member" })
            put("authorAvatarKey", authorAvatarKey.take(40).ifEmpty { "coffee" })
            if (!authorPhotoBase64.isNullOrBlank() && authorPhotoBase64.length <= 200_000) {
                put("authorPhotoBase64", authorPhotoBase64)
            }
            put("authorIsPlus", authorIsPlus)
            put("mediaType", mediaType)
            if (title.isNotBlank()) put("title", title.trim().take(120))
            if (durationLabel.isNotBlank()) put("durationLabel", durationLabel.trim().take(20))
            if (audioTrack.isNotBlank()) put("audioTrack", audioTrack.trim().take(80))
            if (!repostedFromAuthor.isNullOrBlank()) put("repostedFromAuthor", repostedFromAuthor.trim().take(40))
            put("content", content.trim().take(1000))
            put("tag", tag)
            if (!imageKey.isNullOrBlank()) {
                put("imageKey", imageKey.take(40))
            }
            if (!postPhotoBase64.isNullOrBlank() && postPhotoBase64.length <= 200_000) {
                put("postPhotoBase64", postPhotoBase64)
            }
            put("visibility", "community")
            put("likesCount", 0L)
            put("commentsCount", 0L)
            put("repostsCount", 0L)
            put("viewsCount", 0L)
            put("sparksCount", 0L)
            put("sparksEarningsCents", 0L)
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun repostPost(postId: String): Result<Unit> {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        val repostRef = db.collection("post_reposts").document("${postId}_${uid}")
        return try {
            val existing = repostRef.get().await()
            if (existing.exists()) {
                return Result.success(Unit)
            }
            val batch = db.batch()
            batch.set(
                repostRef,
                mapOf(
                    "userId" to uid,
                    "postId" to postId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                postRef,
                mapOf(
                    "repostsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, postRef.path)
            Result.failure(e)
        }
    }

    suspend fun incrementPostView(postId: String): Result<Unit> {
        requireUserId()
        val postRef = db.collection("posts").document(postId)
        return try {
            postRef.update(
                mapOf(
                    "viewsCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("posts").document(postId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleLikePost(postId: String, currentlyLiked: Boolean): Result<Unit> {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        val likeRef = db.collection("post_likes").document("${postId}_${uid}")
        return try {
            val batch = db.batch()
            if (currentlyLiked) {
                batch.delete(likeRef)
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    likeRef,
                    mapOf(
                        "userId" to uid,
                        "postId" to postId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    postRef,
                    mapOf(
                        "likesCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, postRef.path)
            Result.failure(e)
        }
    }

    fun observeComments(postId: String): Flow<List<PostComment>> = flow {
        val uid = requireUserId()
        val path = "posts/$postId/comments"
        emitAll(
            db.collection("posts").document(postId).collection("comments")
                .whereEqualTo("postVisibility", "community")
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PostComment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun addComment(
        postId: String,
        authorName: String,
        authorAvatarKey: String,
        text: String
    ): Result<String> {
        val uid = requireUserId()
        val postRef = db.collection("posts").document(postId)
        val commentRef = postRef.collection("comments").document()
        return try {
            val batch = db.batch()
            batch.set(
                commentRef,
                mapOf(
                    "postId" to postId,
                    "postVisibility" to "community",
                    "authorId" to uid,
                    "authorName" to authorName.trim().take(40).ifEmpty { "Member" },
                    "authorAvatarKey" to authorAvatarKey.take(40).ifEmpty { "coffee" },
                    "text" to text.trim().take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                postRef,
                mapOf(
                    "commentsCount" to FieldValue.increment(1L),
                    "lastCommentId" to commentRef.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(commentRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            Result.failure(e)
        }
    }

    // --- Dating Swipes, Incoming Likes & Mutual Matches ---

    fun observeMySwipes(): Flow<List<SwipeRecord>> = flow {
        val uid = requireUserId()
        val path = "swipes"
        emitAll(
            db.collection("swipes")
                .whereEqualTo("fromUserId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SwipeRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeIncomingLikes(): Flow<List<SwipeRecord>> = flow {
        val uid = requireUserId()
        val path = "swipes"
        emitAll(
            db.collection("swipes")
                .whereEqualTo("positiveTargetId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SwipeRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    /**
     * Records a swipe decision ("like", "superlike", "pass").
     * If positive and the target user has already liked the current user, atomically creates a MatchThread!
     * Returns true if a mutual match was created.
     */
    suspend fun recordSwipe(
        myProfile: PublicProfile,
        targetProfile: PublicProfile,
        decision: String
    ): Result<Boolean> {
        val uid = requireUserId()
        val targetId = targetProfile.userId
        require(uid != targetId) { "Cannot swipe on self" }
        val swipeRef = db.collection("swipes").document("${uid}_${targetId}")
        val isPositive = decision == "like" || decision == "superlike"
        val swipePayload = mapOf(
            "fromUserId" to uid,
            "toUserId" to targetId,
            "decision" to decision,
            "positiveTargetId" to if (isPositive) targetId else "",
            "fromName" to myProfile.displayName.take(40).ifEmpty { "Member" },
            "fromAvatarKey" to myProfile.avatarKey.take(40).ifEmpty { "coffee" },
            "createdAt" to FieldValue.serverTimestamp()
        )
        return try {
            if (!isPositive) {
                swipeRef.set(swipePayload).await()
                return Result.success(false)
            }

            // Check if the target user already liked us
            val reverseSwipeSnap = db.collection("swipes").document("${targetId}_${uid}").get().await()
            val reverseDecision = reverseSwipeSnap.getString("decision")
            val isMutual = reverseSwipeSnap.exists() && (reverseDecision == "like" || reverseDecision == "superlike")

            val privateRef = db.collection("users").document(uid)
            val privateSnap = privateRef.get().await()
            val quotaUpdates = buildSwipeQuotaUpdate(privateSnap, decision)

            if (!isMutual) {
                val batch = db.batch()
                batch.set(swipeRef, swipePayload)
                if (quotaUpdates != null && privateSnap.exists()) {
                    batch.update(privateRef, quotaUpdates)
                }
                batch.commit().await()
                Result.success(false)
            } else {
                val matchId = sortedMatchId(uid, targetId)
                val matchRef = db.collection("matches").document(matchId)
                val existingMatch = matchRef.get().await()
                if (existingMatch.exists()) {
                    val batch = db.batch()
                    batch.set(swipeRef, swipePayload)
                    if (quotaUpdates != null && privateSnap.exists()) {
                        batch.update(privateRef, quotaUpdates)
                    }
                    batch.commit().await()
                    return Result.success(true)
                }

                val (first, second) = if (uid < targetId) myProfile to targetProfile else targetProfile to myProfile
                val batch = db.batch()
                batch.set(swipeRef, swipePayload)
                batch.set(
                    matchRef,
                    mapOf(
                        "participantIds" to listOf(first.userId, second.userId),
                        "user1Id" to first.userId,
                        "user1Name" to first.displayName.take(40).ifEmpty { "Member" },
                        "user1AvatarKey" to first.avatarKey.take(40).ifEmpty { "coffee" },
                        "user2Id" to second.userId,
                        "user2Name" to second.displayName.take(40).ifEmpty { "Member" },
                        "user2AvatarKey" to second.avatarKey.take(40).ifEmpty { "sunset" },
                        "lastMessage" to "It's a match! Say hello.",
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                if (quotaUpdates != null && privateSnap.exists()) {
                    batch.update(privateRef, quotaUpdates)
                }
                batch.commit().await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, swipeRef.path)
            Result.failure(e)
        }
    }

    private fun buildSwipeQuotaUpdate(
        privateSnap: DocumentSnapshot,
        decision: String
    ): Map<String, Any>? {
        if (!privateSnap.exists()) return null
        val isPlus = privateSnap.getBoolean("isPlusMember") ?: false
        val today = todayIsoDate()
        return when (decision) {
            "superlike" -> {
                val currentSuperlikes = privateSnap.getLong("superlikesBalance") ?: 1L
                val nextSuperlikes = (currentSuperlikes - 1L).coerceAtLeast(0L)
                mapOf(
                    "superlikesBalance" to nextSuperlikes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            "like" -> {
                if (isPlus) return null
                val storedDate = privateSnap.getString("dailyLikesDate") ?: ""
                val storedUsed = privateSnap.getLong("dailyLikesUsed") ?: 0L
                val nextUsed = if (storedDate == today) {
                    (storedUsed + 1L).coerceAtMost(1000L)
                } else {
                    1L
                }
                mapOf(
                    "dailyLikesUsed" to nextUsed,
                    "dailyLikesDate" to today,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            else -> null
        }
    }

    // --- Monetization: Play Billing Entitlements, Consumables & Boosts ---

    suspend fun syncSubscriptionEntitlement(
        productId: String,
        purchaseToken: String?,
        active: Boolean
    ): Result<Unit> {
        val uid = requireUserId()
        val privateRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        return try {
            val privateSnap = privateRef.get().await()
            val publicSnap = publicRef.get().await()
            val batch = db.batch()
            if (privateSnap.exists()) {
                val updates = buildMap<String, Any> {
                    put("isPlusMember", active)
                    if (productId.isNotBlank()) put("plusPlanId", productId.take(80))
                    if (!purchaseToken.isNullOrBlank()) put("lastPurchaseToken", purchaseToken.take(500))
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.update(privateRef, updates)
            } else {
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("showInDiscovery", true)
                    put("preferredIntent", "all")
                    put("isPlusMember", active)
                    if (productId.isNotBlank()) put("plusPlanId", productId.take(80))
                    put("superlikesBalance", 1L)
                    put("boostsBalance", 0L)
                    put("sparksBalance", 0L)
                    put("dailyLikesUsed", 0L)
                    put("dailyLikesDate", todayIsoDate())
                    if (!purchaseToken.isNullOrBlank()) put("lastPurchaseToken", purchaseToken.take(500))
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.set(privateRef, payload)
            }
            if (publicSnap.exists()) {
                batch.update(
                    publicRef,
                    mapOf(
                        "isPlusMember" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            if (active && !purchaseToken.isNullOrBlank()) {
                val ledgerId = deterministicDocId("sub", "${uid}_${productId}_${purchaseToken}")
                val ledgerRef = db.collection("revenue_ledger").document(ledgerId)
                val existingLedger = ledgerRef.get().await()
                if (!existingLedger.exists()) {
                    val gross = KindredProducts.grossCentsForProduct(productId)
                    batch.set(
                        ledgerRef,
                        mapOf(
                            "source" to "subscription",
                            "productId" to productId.take(80),
                            "payerId" to uid,
                            "recipientId" to "",
                            "grossCents" to gross,
                            "adminShareCents" to gross,
                            "creatorShareCents" to 0L,
                            "referenceToken" to purchaseToken.take(500),
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, privateRef.path)
            Result.failure(e)
        }
    }

    suspend fun grantConsumablePurchase(
        productId: String,
        quantity: Int,
        purchaseToken: String
    ): Result<Unit> {
        val uid = requireUserId()
        val privateRef = db.collection("users").document(uid)
        val safeQty = quantity.coerceAtLeast(1).toLong()
        return try {
            val snap = privateRef.get().await()
            val currentSuperlikes = if (snap.exists()) (snap.getLong("superlikesBalance") ?: 1L) else 1L
            val currentBoosts = if (snap.exists()) (snap.getLong("boostsBalance") ?: 0L) else 0L
            val currentSparks = if (snap.exists()) (snap.getLong("sparksBalance") ?: 0L) else 0L

            val nextSuperlikes = if (productId == KindredProducts.SUPERLIKES_5) {
                (currentSuperlikes + 5L * safeQty).coerceAtMost(999L)
            } else currentSuperlikes
            val nextBoosts = if (productId == KindredProducts.BOOST_1) {
                (currentBoosts + safeQty).coerceAtMost(999L)
            } else currentBoosts
            val nextSparks = if (productId == KindredProducts.SPARKS_5) {
                (currentSparks + 5L * safeQty).coerceAtMost(999L)
            } else currentSparks

            val batch = db.batch()
            if (snap.exists()) {
                val updates = buildMap<String, Any> {
                    put("superlikesBalance", nextSuperlikes)
                    put("boostsBalance", nextBoosts)
                    put("sparksBalance", nextSparks)
                    if (purchaseToken.isNotBlank()) put("lastPurchaseToken", purchaseToken.take(500))
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.update(privateRef, updates)
            } else {
                val payload = buildMap<String, Any> {
                    put("userId", uid)
                    put("showInDiscovery", true)
                    put("preferredIntent", "all")
                    put("isPlusMember", false)
                    put("superlikesBalance", nextSuperlikes)
                    put("boostsBalance", nextBoosts)
                    put("sparksBalance", nextSparks)
                    put("dailyLikesUsed", 0L)
                    put("dailyLikesDate", todayIsoDate())
                    if (purchaseToken.isNotBlank()) put("lastPurchaseToken", purchaseToken.take(500))
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.set(privateRef, payload)
            }

            val source = when (productId) {
                KindredProducts.SUPERLIKES_5 -> "superlikes"
                KindredProducts.BOOST_1 -> "boost"
                KindredProducts.SPARKS_5 -> "sparks_pack"
                else -> "boost"
            }
            val gross = (KindredProducts.grossCentsForProduct(productId) * safeQty).coerceIn(1L, 20000L)
            val ledgerId = if (purchaseToken.isNotBlank()) {
                deterministicDocId("iap", "${uid}_${productId}_${purchaseToken}")
            } else {
                db.collection("revenue_ledger").document().id
            }
            val ledgerRef = db.collection("revenue_ledger").document(ledgerId)
            val existingLedger = ledgerRef.get().await()
            if (!existingLedger.exists()) {
                batch.set(
                    ledgerRef,
                    buildMap<String, Any> {
                        put("source", source)
                        put("productId", productId.take(80))
                        put("payerId", uid)
                        put("recipientId", "")
                        put("grossCents", gross)
                        put("adminShareCents", gross)
                        put("creatorShareCents", 0L)
                        if (purchaseToken.isNotBlank()) put("referenceToken", purchaseToken.take(500))
                        put("createdAt", FieldValue.serverTimestamp())
                    }
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, privateRef.path)
            Result.failure(e)
        }
    }

    suspend fun activateProfileBoost(): Result<Unit> {
        val uid = requireUserId()
        val privateRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        return try {
            val privateSnap = privateRef.get().await()
            val publicSnap = publicRef.get().await()
            if (!publicSnap.exists()) {
                return Result.failure(IllegalStateException("Create your BINTAFACE profile card before activating Spotlight Boost."))
            }
            val currentBoosts = if (privateSnap.exists()) (privateSnap.getLong("boostsBalance") ?: 0L) else 0L
            if (currentBoosts <= 0L) {
                return Result.failure(IllegalStateException("No Spotlight Boosts remaining."))
            }
            val batch = db.batch()
            batch.update(
                privateRef,
                mapOf(
                    "boostsBalance" to (currentBoosts - 1L).coerceAtLeast(0L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                publicRef,
                mapOf(
                    "isBoosted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, publicRef.path)
            Result.failure(e)
        }
    }

    // --- Two-Sided Revenue Share: Creator Spark Gifts (80% User / 20% Admin), Payouts & Admin Console ---

    fun observePlatformSettings(): Flow<PlatformSettings?> = flow {
        val uid = requireUserId()
        val path = "app_settings/platform"
        emitAll(
            db.collection("app_settings").document("platform")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            PlatformSettings::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .handleSnapshotErrors(OperationType.GET, path, uid)
        )
    }

    suspend fun ensureAdminPlatformClaimed(email: String? = auth.currentUser?.email): Result<Unit> {
        val uid = requireUserId()
        val effectiveEmail = auth.currentUser?.email ?: email
        if (!isDesignatedAdminEmail(effectiveEmail)) {
            return Result.failure(SecurityException("Only $APP_ADMIN_EMAIL is authorized as App Admin."))
        }
        val docRef = db.collection("app_settings").document("platform")
        return try {
            val existing = docRef.get().await()
            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "adminUid" to uid,
                        "adminEmail" to APP_ADMIN_EMAIL,
                        "platformFeePercent" to 20L,
                        "payoutAccountLabel" to "$DEFAULT_ADMIN_BANK_NAME ($DEFAULT_ADMIN_ACCOUNT_NUMBER)",
                        "payoutMethod" to "ngn_bank",
                        "bankName" to DEFAULT_ADMIN_BANK_NAME,
                        "adminAccountNumber" to DEFAULT_ADMIN_ACCOUNT_NUMBER,
                        "adminAccountHolder" to DEFAULT_ADMIN_ACCOUNT_HOLDER,
                        "maskedAccountNumber" to maskAccountNumber(DEFAULT_ADMIN_ACCOUNT_NUMBER),
                        "adminPayoutCurrency" to "NGN",
                        "usdToNgnRate" to DEFAULT_USD_TO_NGN_RATE,
                        "adAdminSharePercent" to 70L,
                        "autoPayoutEnabled" to true,
                        "appDownloadUrl" to DEFAULT_APP_DOWNLOAD_URL,
                        "admobBannerUnitId" to DEFAULT_ADMOB_BANNER_UNIT_ID,
                        "admobRewardedUnitId" to DEFAULT_ADMOB_REWARDED_UNIT_ID,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val updates = mutableMapOf<String, Any>()
                if (existing.getString("adminUid") != uid || existing.getString("adminEmail") != APP_ADMIN_EMAIL) {
                    updates["adminUid"] = uid
                    updates["adminEmail"] = APP_ADMIN_EMAIL
                }
                if (existing.getString("adminAccountNumber").isNullOrBlank()) {
                    updates["bankName"] = DEFAULT_ADMIN_BANK_NAME
                    updates["adminAccountNumber"] = DEFAULT_ADMIN_ACCOUNT_NUMBER
                    updates["adminAccountHolder"] = DEFAULT_ADMIN_ACCOUNT_HOLDER
                    updates["maskedAccountNumber"] = maskAccountNumber(DEFAULT_ADMIN_ACCOUNT_NUMBER)
                    updates["payoutMethod"] = "ngn_bank"
                    updates["payoutAccountLabel"] = "$DEFAULT_ADMIN_BANK_NAME ($DEFAULT_ADMIN_ACCOUNT_NUMBER)"
                }
                if (updates.isNotEmpty()) {
                    updates["updatedAt"] = FieldValue.serverTimestamp()
                    docRef.update(updates).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun claimAdminRole(email: String?, payoutAccountLabel: String): Result<Unit> {
        val uid = requireUserId()
        val effectiveEmail = auth.currentUser?.email ?: email
        if (!isDesignatedAdminEmail(effectiveEmail)) {
            return Result.failure(SecurityException("Admin access is restricted to $APP_ADMIN_EMAIL."))
        }
        val docRef = db.collection("app_settings").document("platform")
        return try {
            val existing = docRef.get().await()
            val cleanLabel = payoutAccountLabel.trim().take(120).ifEmpty { "BINTAFACE Admin Treasury" }
            if (!existing.exists()) {
                docRef.set(
                    mapOf(
                        "adminUid" to uid,
                        "adminEmail" to APP_ADMIN_EMAIL,
                        "platformFeePercent" to 20L,
                        "payoutAccountLabel" to cleanLabel,
                        "payoutMethod" to "ngn_bank",
                        "bankName" to DEFAULT_ADMIN_BANK_NAME,
                        "adminAccountNumber" to DEFAULT_ADMIN_ACCOUNT_NUMBER,
                        "adminAccountHolder" to DEFAULT_ADMIN_ACCOUNT_HOLDER,
                        "maskedAccountNumber" to maskAccountNumber(DEFAULT_ADMIN_ACCOUNT_NUMBER),
                        "adminPayoutCurrency" to "NGN",
                        "usdToNgnRate" to DEFAULT_USD_TO_NGN_RATE,
                        "adAdminSharePercent" to 70L,
                        "autoPayoutEnabled" to true,
                        "appDownloadUrl" to DEFAULT_APP_DOWNLOAD_URL,
                        "admobBannerUnitId" to DEFAULT_ADMOB_BANNER_UNIT_ID,
                        "admobRewardedUnitId" to DEFAULT_ADMOB_REWARDED_UNIT_ID,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "adminUid" to uid,
                        "adminEmail" to APP_ADMIN_EMAIL,
                        "payoutAccountLabel" to cleanLabel,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePlatformSettings(platformFeePercent: Long, payoutAccountLabel: String): Result<Unit> {
        val uid = requireUserId()
        val effectiveEmail = auth.currentUser?.email
        if (effectiveEmail != null && !isDesignatedAdminEmail(effectiveEmail)) {
            return Result.failure(SecurityException("Admin settings are restricted to $APP_ADMIN_EMAIL."))
        }
        val docRef = db.collection("app_settings").document("platform")
        return try {
            docRef.update(
                mapOf(
                    "adminUid" to uid,
                    "adminEmail" to APP_ADMIN_EMAIL,
                    "platformFeePercent" to platformFeePercent.coerceIn(5L, 50L),
                    "payoutAccountLabel" to payoutAccountLabel.trim().take(120),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeAdminPayoutAccount(): Flow<AdminPayoutAccount?> = flow {
        val uid = requireUserId()
        val path = "admin_payout_accounts/primary"
        emitAll(
            db.collection("admin_payout_accounts").document("primary")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(
                            AdminPayoutAccount::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .handleSnapshotErrors(OperationType.GET, path, uid)
        )
    }

    suspend fun saveAdminPayoutAccount(
        payoutMethod: String,
        accountHolderName: String,
        bankName: String,
        accountNumber: String,
        routingOrSwift: String,
        payoutEmailOrPhone: String,
        platformFeePercent: Long = 20L,
        payoutCurrency: String = "NGN"
    ): Result<Unit> {
        val uid = requireUserId()
        val effectiveEmail = auth.currentUser?.email
        if (!isDesignatedAdminEmail(effectiveEmail)) {
            return Result.failure(SecurityException("Only $APP_ADMIN_EMAIL can update admin payout bank details."))
        }
        val cleanMethod = payoutMethod.takeIf {
            it in setOf("usd_bank", "ngn_bank", "bank_transfer", "paypal", "upi", "mobile_money")
        } ?: "ngn_bank"
        val cleanCurrency = when {
            payoutCurrency in setOf("USD", "NGN") -> payoutCurrency
            cleanMethod == "usd_bank" -> "USD"
            cleanMethod == "ngn_bank" -> "NGN"
            else -> "USD"
        }
        val cleanHolder = accountHolderName.trim().take(80)
        val cleanBank = bankName.trim().take(80)
        val cleanNumber = accountNumber.trim().take(64)
        val cleanRouting = routingOrSwift.trim().take(40)
        val cleanContact = payoutEmailOrPhone.trim().take(120).ifEmpty { APP_ADMIN_EMAIL }

        if (cleanHolder.length < 2) {
            return Result.failure(IllegalArgumentException("Enter the account holder's full name."))
        }
        if (cleanBank.length < 2) {
            return Result.failure(IllegalArgumentException("Enter your bank or payment provider name."))
        }
        if (cleanNumber.length < 4) {
            return Result.failure(IllegalArgumentException("Enter a valid account number (minimum 4 characters)."))
        }

        val masked = maskAccountNumber(cleanNumber)
        val label = "$cleanBank ($cleanCurrency) $masked".take(120)
        val accountRef = db.collection("admin_payout_accounts").document("primary")
        val platformRef = db.collection("app_settings").document("platform")

        return try {
            val existingAccount = accountRef.get().await()
            val existingPlatform = platformRef.get().await()
            val batch = db.batch()

            if (!existingAccount.exists()) {
                val payload = buildMap<String, Any> {
                    put("adminUid", uid)
                    put("adminEmail", APP_ADMIN_EMAIL)
                    put("payoutMethod", cleanMethod)
                    put("payoutCurrency", cleanCurrency)
                    put("accountHolderName", cleanHolder)
                    put("bankName", cleanBank)
                    put("accountNumber", cleanNumber)
                    if (cleanRouting.isNotEmpty()) put("routingOrSwift", cleanRouting)
                    put("payoutEmailOrPhone", cleanContact)
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.set(accountRef, payload)
            } else {
                val updates = mutableMapOf<String, Any>(
                    "adminUid" to uid,
                    "adminEmail" to APP_ADMIN_EMAIL,
                    "payoutMethod" to cleanMethod,
                    "payoutCurrency" to cleanCurrency,
                    "accountHolderName" to cleanHolder,
                    "bankName" to cleanBank,
                    "accountNumber" to cleanNumber,
                    "routingOrSwift" to cleanRouting,
                    "payoutEmailOrPhone" to cleanContact,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.update(accountRef, updates)
            }

            if (!existingPlatform.exists()) {
                batch.set(
                    platformRef,
                    mapOf(
                        "adminUid" to uid,
                        "adminEmail" to APP_ADMIN_EMAIL,
                        "platformFeePercent" to platformFeePercent.coerceIn(5L, 50L),
                        "payoutAccountLabel" to label,
                        "payoutMethod" to cleanMethod,
                        "bankName" to cleanBank,
                        "adminAccountNumber" to cleanNumber,
                        "adminAccountHolder" to cleanHolder,
                        "maskedAccountNumber" to masked,
                        "adminPayoutCurrency" to cleanCurrency,
                        "usdToNgnRate" to DEFAULT_USD_TO_NGN_RATE,
                        "adAdminSharePercent" to 70L,
                        "autoPayoutEnabled" to true,
                        "appDownloadUrl" to DEFAULT_APP_DOWNLOAD_URL,
                        "admobBannerUnitId" to DEFAULT_ADMOB_BANNER_UNIT_ID,
                        "admobRewardedUnitId" to DEFAULT_ADMOB_REWARDED_UNIT_ID,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.update(
                    platformRef,
                    mapOf(
                        "adminUid" to uid,
                        "adminEmail" to APP_ADMIN_EMAIL,
                        "platformFeePercent" to platformFeePercent.coerceIn(5L, 50L),
                        "payoutAccountLabel" to label,
                        "payoutMethod" to cleanMethod,
                        "bankName" to cleanBank,
                        "adminAccountNumber" to cleanNumber,
                        "adminAccountHolder" to cleanHolder,
                        "maskedAccountNumber" to masked,
                        "adminPayoutCurrency" to cleanCurrency,
                        "adAdminSharePercent" to 70L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, accountRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateAdminAdAndDownloadConfig(
        usdToNgnRate: Long,
        autoPayoutEnabled: Boolean,
        appDownloadUrl: String,
        admobBannerUnitId: String,
        admobRewardedUnitId: String
    ): Result<Unit> {
        val uid = requireUserId()
        val effectiveEmail = auth.currentUser?.email
        if (!isDesignatedAdminEmail(effectiveEmail)) {
            return Result.failure(SecurityException("Only $APP_ADMIN_EMAIL can update BINTAFACE settings."))
        }
        val safeRate = usdToNgnRate.coerceIn(100L, 10000L)
        val cleanUrl = appDownloadUrl.trim().take(300).ifBlank { DEFAULT_APP_DOWNLOAD_URL }
        val cleanBanner = admobBannerUnitId.trim().take(80).ifBlank { DEFAULT_ADMOB_BANNER_UNIT_ID }
        val cleanRewarded = admobRewardedUnitId.trim().take(80).ifBlank { DEFAULT_ADMOB_REWARDED_UNIT_ID }
        val platformRef = db.collection("app_settings").document("platform")
        return try {
            val existing = platformRef.get().await()
            if (!existing.exists()) {
                platformRef.set(
                    mapOf(
                        "adminUid" to uid,
                        "adminEmail" to APP_ADMIN_EMAIL,
                        "platformFeePercent" to 20L,
                        "payoutAccountLabel" to "BINTAFACE Admin Treasury",
                        "adminPayoutCurrency" to "NGN",
                        "usdToNgnRate" to safeRate,
                        "adAdminSharePercent" to 70L,
                        "autoPayoutEnabled" to autoPayoutEnabled,
                        "appDownloadUrl" to cleanUrl,
                        "admobBannerUnitId" to cleanBanner,
                        "admobRewardedUnitId" to cleanRewarded,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                platformRef.update(
                    mapOf(
                        "adminUid" to uid,
                        "adminEmail" to APP_ADMIN_EMAIL,
                        "usdToNgnRate" to safeRate,
                        "adAdminSharePercent" to 70L,
                        "autoPayoutEnabled" to autoPayoutEnabled,
                        "appDownloadUrl" to cleanUrl,
                        "admobBannerUnitId" to cleanBanner,
                        "admobRewardedUnitId" to cleanRewarded,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, platformRef.path)
            Result.failure(e)
        }
    }

    fun observeAdminWithdrawals(): Flow<List<AdminWithdrawal>> = flow {
        val uid = requireUserId()
        val path = "admin_payouts"
        emitAll(
            db.collection("admin_payouts")
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        AdminWithdrawal::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun withdrawAdminRevenue(
        amountCents: Long,
        usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE
    ): Result<AdminWithdrawal> {
        val uid = requireUserId()
        val effectiveEmail = auth.currentUser?.email
        if (!isDesignatedAdminEmail(effectiveEmail)) {
            return Result.failure(SecurityException("Only $APP_ADMIN_EMAIL can withdraw app admin revenue."))
        }
        if (amountCents < 100L) {
            return Result.failure(IllegalArgumentException("Minimum admin withdrawal is $1.00."))
        }
        val accountRef = db.collection("admin_payout_accounts").document("primary")
        val withdrawalRef = db.collection("admin_payouts").document()
        return try {
            val accountSnap = accountRef.get().await()
            if (!accountSnap.exists()) {
                return Result.failure(IllegalStateException("Add and save your receiving account number before withdrawing revenue."))
            }
            val holder = accountSnap.getString("accountHolderName")?.trim().orEmpty()
            val bank = accountSnap.getString("bankName")?.trim().orEmpty()
            val acctNum = accountSnap.getString("accountNumber")?.trim().orEmpty()
            val method = accountSnap.getString("payoutMethod")?.trim().orEmpty().ifEmpty { "ngn_bank" }
            val currency = accountSnap.getString("payoutCurrency")?.trim()?.takeIf { it in setOf("USD", "NGN") }
                ?: if (method == "ngn_bank") "NGN" else "USD"
            if (holder.length < 2 || bank.length < 2 || acctNum.length < 4) {
                return Result.failure(IllegalStateException("Complete your admin bank/payment account details first."))
            }
            val masked = maskAccountNumber(acctNum)
            val ngnWhole = computeNgnWhole(amountCents, usdToNgnRate)
            val referenceCode = "BINT-ADM-${UUID.randomUUID().toString().take(8).uppercase(Locale.US)}"
            db.runTransaction { transaction ->
                val freshAccount = transaction.get(accountRef)
                if (!freshAccount.exists()) {
                    throw IllegalStateException("Admin receiving account missing.")
                }
                transaction.set(
                    withdrawalRef,
                    mapOf(
                        "adminUid" to uid,
                        "adminEmail" to APP_ADMIN_EMAIL,
                        "amountCents" to amountCents,
                        "ngnAmountWhole" to ngnWhole,
                        "payoutCurrency" to currency,
                        "payoutMethod" to method,
                        "accountHolderName" to holder.take(80),
                        "bankName" to bank.take(80),
                        "maskedAccountNumber" to masked.take(40),
                        "status" to "completed",
                        "referenceCode" to referenceCode,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                null
            }.await()
            Result.success(
                AdminWithdrawal(
                    id = withdrawalRef.id,
                    adminUid = uid,
                    adminEmail = APP_ADMIN_EMAIL,
                    amountCents = amountCents,
                    ngnAmountWhole = ngnWhole,
                    payoutCurrency = currency,
                    payoutMethod = method,
                    accountHolderName = holder,
                    bankName = bank,
                    maskedAccountNumber = masked,
                    status = "completed",
                    referenceCode = referenceCode
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, withdrawalRef.path)
            Result.failure(e)
        }
    }

    fun observeCreatorAdRevenue(creatorId: String): Flow<List<AdRevenueEvent>> = flow {
        val path = "ad_revenue_events"
        emitAll(
            db.collection("ad_revenue_events")
                .whereEqualTo("creatorId", creatorId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        AdRevenueEvent::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, creatorId)
        )
    }

    suspend fun recordGoogleAdRevenue(
        adFormat: String,
        adUnitId: String,
        valueMicros: Long,
        currencyCode: String,
        creatorId: String = "",
        creatorName: String = "",
        postId: String = "feed_banner"
    ): Result<AdRevenueEvent?> {
        val uid = requireUserId()
        if (valueMicros <= 0L) {
            return Result.success(null)
        }
        val cleanFormat = if (adFormat == "rewarded") "rewarded" else "banner"
        val cleanUnit = adUnitId.trim().take(80).ifBlank { DEFAULT_ADMOB_BANNER_UNIT_ID }
        val cleanCurrency = currencyCode.trim().uppercase(Locale.US).take(10).ifBlank { "USD" }
        val cleanCreatorId = creatorId.trim().take(128)
        val cleanCreatorName = creatorName.trim().take(40)
        val cleanPostId = postId.trim().take(128).ifBlank { "feed_banner" }

        val grossCents = (valueMicros / 10_000L).coerceIn(1L, 50000L)
        val (adminShareCents, creatorShareCents) = computeAd7030Split(grossCents, cleanCreatorId, uid)

        val eventRef = db.collection("ad_revenue_events").document()
        val ledgerRef = db.collection("revenue_ledger").document(eventRef.id)
        return try {
            val batch = db.batch()
            batch.set(
                eventRef,
                mapOf(
                    "viewerId" to uid,
                    "creatorId" to cleanCreatorId,
                    "creatorName" to cleanCreatorName,
                    "postId" to cleanPostId,
                    "adFormat" to cleanFormat,
                    "adUnitId" to cleanUnit,
                    "valueMicros" to valueMicros,
                    "currencyCode" to cleanCurrency,
                    "grossCents" to grossCents,
                    "adminShareCents" to adminShareCents,
                    "creatorShareCents" to creatorShareCents,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                ledgerRef,
                mapOf(
                    "source" to "ad_revenue",
                    "productId" to "admob_$cleanFormat",
                    "payerId" to uid,
                    "recipientId" to cleanCreatorId,
                    "grossCents" to grossCents,
                    "adminShareCents" to adminShareCents,
                    "creatorShareCents" to creatorShareCents,
                    "referenceToken" to cleanUnit,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(
                AdRevenueEvent(
                    id = eventRef.id,
                    viewerId = uid,
                    creatorId = cleanCreatorId,
                    creatorName = cleanCreatorName,
                    postId = cleanPostId,
                    adFormat = cleanFormat,
                    adUnitId = cleanUnit,
                    valueMicros = valueMicros,
                    currencyCode = cleanCurrency,
                    grossCents = grossCents,
                    adminShareCents = adminShareCents,
                    creatorShareCents = creatorShareCents
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, eventRef.path)
            Result.failure(e)
        }
    }

    fun observeReceivedSparkGifts(userId: String): Flow<List<SparkGift>> = flow {
        val path = "spark_gifts"
        emitAll(
            db.collection("spark_gifts")
                .whereEqualTo("recipientId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        SparkGift::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, userId)
        )
    }

    suspend fun sendSparkGift(post: Post, senderName: String): Result<Unit> =
        sendSparkGiftToTarget(
            targetId = post.id,
            targetType = "post",
            recipientId = post.authorId,
            recipientName = post.authorName,
            senderName = senderName
        )

    suspend fun sendSparkGiftToTarget(
        targetId: String,
        targetType: String,
        recipientId: String,
        recipientName: String,
        senderName: String
    ): Result<Unit> {
        val uid = requireUserId()
        if (recipientId == uid) {
            return Result.failure(IllegalArgumentException("You can't send a paid Spark Gift to yourself."))
        }
        val privateRef = db.collection("users").document(uid)
        val targetCollection = when (targetType) {
            "live" -> "live_rooms"
            "channel" -> "channels"
            "page" -> "pages"
            "radio" -> "radio_channels"
            else -> "posts"
        }
        val targetRef = db.collection(targetCollection).document(targetId)
        val giftRef = db.collection("spark_gifts").document()
        val ledgerRef = db.collection("revenue_ledger").document(giftRef.id)
        return try {
            val privateSnap = privateRef.get().await()
            val sparksBalance = if (privateSnap.exists()) (privateSnap.getLong("sparksBalance") ?: 0L) else 0L
            if (sparksBalance < 1L) {
                return Result.failure(IllegalStateException("No Spark Coins available. Purchase a 5 Creator Sparks Pack first."))
            }
            val batch = db.batch()
            batch.update(
                privateRef,
                mapOf(
                    "sparksBalance" to (sparksBalance - 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                giftRef,
                mapOf(
                    "senderId" to uid,
                    "senderName" to senderName.trim().take(40).ifEmpty { "Member" },
                    "recipientId" to recipientId,
                    "recipientName" to recipientName.trim().take(40).ifEmpty { "Creator" },
                    "postId" to targetId,
                    "targetType" to targetType,
                    "sparksCount" to 1L,
                    "grossCents" to 100L,
                    "creatorShareCents" to 80L,
                    "adminFeeCents" to 20L,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            val targetUpdates = when (targetType) {
                "live" -> mapOf(
                    "sparksCount" to FieldValue.increment(1L),
                    "totalGiftsCents" to FieldValue.increment(80L),
                    "lastSparkGiftId" to giftRef.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                "channel", "page", "radio" -> mapOf(
                    "totalGiftsCents" to FieldValue.increment(80L),
                    "lastSparkGiftId" to giftRef.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                else -> mapOf(
                    "sparksCount" to FieldValue.increment(1L),
                    "sparksEarningsCents" to FieldValue.increment(80L),
                    "lastSparkGiftId" to giftRef.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            batch.update(targetRef, targetUpdates)
            batch.set(
                ledgerRef,
                mapOf(
                    "source" to "spark_gift",
                    "productId" to "spark_gift_1",
                    "payerId" to uid,
                    "recipientId" to recipientId,
                    "grossCents" to 100L,
                    "adminShareCents" to 20L,
                    "creatorShareCents" to 80L,
                    "referenceToken" to giftRef.id,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, giftRef.path)
            Result.failure(e)
        }
    }

    suspend fun savePayoutPreferences(
        payoutMethod: String,
        payoutAccount: String,
        payoutBankOrNetwork: String = "",
        payoutCurrency: String = "USD",
        autoPayoutEnabled: Boolean = true
    ): Result<Unit> {
        val uid = requireUserId()
        val privateRef = db.collection("users").document(uid)
        val cleanMethod = payoutMethod.takeIf {
            it in setOf("usd_bank", "ngn_bank", "crypto", "paypal", "bank", "upi")
        } ?: "ngn_bank"
        val cleanAccount = payoutAccount.trim().take(120)
        val cleanBankOrNetwork = payoutBankOrNetwork.trim().take(80)
        val cleanCurrency = when {
            payoutCurrency in setOf("USD", "NGN", "CRYPTO") -> payoutCurrency
            cleanMethod == "crypto" -> "CRYPTO"
            cleanMethod == "ngn_bank" -> "NGN"
            else -> "USD"
        }
        if (cleanAccount.length < 3) {
            return Result.failure(IllegalArgumentException("Enter a valid wallet address, bank account number, or handle."))
        }
        return try {
            val snap = privateRef.get().await()
            if (snap.exists()) {
                privateRef.update(
                    mapOf(
                        "payoutMethod" to cleanMethod,
                        "payoutAccount" to cleanAccount,
                        "payoutBankOrNetwork" to cleanBankOrNetwork,
                        "payoutCurrency" to cleanCurrency,
                        "autoPayoutEnabled" to autoPayoutEnabled,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                privateRef.set(
                    mapOf(
                        "userId" to uid,
                        "showInDiscovery" to true,
                        "preferredIntent" to "all",
                        "isPlusMember" to false,
                        "superlikesBalance" to 1L,
                        "boostsBalance" to 0L,
                        "sparksBalance" to 0L,
                        "dailyLikesUsed" to 0L,
                        "dailyLikesDate" to todayIsoDate(),
                        "payoutMethod" to cleanMethod,
                        "payoutAccount" to cleanAccount,
                        "payoutBankOrNetwork" to cleanBankOrNetwork,
                        "payoutCurrency" to cleanCurrency,
                        "autoPayoutEnabled" to autoPayoutEnabled,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, privateRef.path)
            Result.failure(e)
        }
    }

    fun observeMyPayoutRequests(userId: String): Flow<List<PayoutRequest>> = flow {
        val path = "payout_requests"
        emitAll(
            db.collection("payout_requests")
                .whereEqualTo("userId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        PayoutRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, userId)
        )
    }

    suspend fun requestCreatorPayout(
        userName: String,
        amountCents: Long,
        payoutMethod: String,
        payoutAccount: String,
        payoutCurrency: String = "USD",
        payoutBankOrNetwork: String = "",
        isAutoPayout: Boolean = false,
        usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE
    ): Result<String> {
        val uid = requireUserId()
        val cleanAccount = payoutAccount.trim().take(120)
        val cleanMethod = payoutMethod.takeIf {
            it in setOf("usd_bank", "ngn_bank", "crypto", "paypal", "bank", "upi")
        } ?: "ngn_bank"
        val cleanCurrency = when {
            payoutCurrency in setOf("USD", "NGN", "CRYPTO") -> payoutCurrency
            cleanMethod == "crypto" -> "CRYPTO"
            cleanMethod == "ngn_bank" -> "NGN"
            else -> "USD"
        }
        val cleanBankOrNetwork = payoutBankOrNetwork.trim().take(80)
        if (amountCents < 100L) {
            return Result.failure(IllegalArgumentException("Minimum payout request is $1.00."))
        }
        if (cleanAccount.length < 3) {
            return Result.failure(IllegalArgumentException("Save a valid wallet address or bank account before requesting payout."))
        }
        val ngnWhole = computeNgnWhole(amountCents, usdToNgnRate)
        val docRef = db.collection("payout_requests").document()
        return try {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "userName" to userName.trim().take(40).ifEmpty { "Creator" },
                    "amountCents" to amountCents,
                    "ngnAmountWhole" to ngnWhole,
                    "payoutMethod" to cleanMethod,
                    "payoutCurrency" to cleanCurrency,
                    "payoutBankOrNetwork" to cleanBankOrNetwork,
                    "payoutAccount" to cleanAccount,
                    "isAutoPayout" to isAutoPayout,
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeAllPayoutRequests(): Flow<List<PayoutRequest>> = flow {
        val uid = requireUserId()
        val path = "payout_requests"
        emitAll(
            db.collection("payout_requests")
                .limit(100)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        PayoutRequest::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeAllRevenueLedger(): Flow<List<RevenueLedgerEntry>> = flow {
        val uid = requireUserId()
        val path = "revenue_ledger"
        emitAll(
            db.collection("revenue_ledger")
                .limit(150)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        RevenueLedgerEntry::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun settlePayoutRequest(
        requestId: String,
        approved: Boolean,
        payoutReference: String,
        adminNote: String
    ): Result<Unit> {
        requireUserId()
        val docRef = db.collection("payout_requests").document(requestId)
        return try {
            val updates = buildMap<String, Any> {
                put("status", if (approved) "paid" else "rejected")
                if (payoutReference.isNotBlank()) put("payoutReference", payoutReference.trim().take(120))
                if (adminNote.isNotBlank()) put("adminNote", adminNote.trim().take(200))
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun processAutoPayoutBatch(pendingRequests: List<PayoutRequest>): Result<Int> {
        requireUserId()
        val effectiveEmail = auth.currentUser?.email
        if (!isDesignatedAdminEmail(effectiveEmail)) {
            return Result.failure(SecurityException("Only $APP_ADMIN_EMAIL can process automatic payouts."))
        }
        val targets = pendingRequests.filter { it.status == "pending" }.take(25)
        if (targets.isEmpty()) {
            return Result.success(0)
        }
        return try {
            val batch = db.batch()
            targets.forEach { req ->
                val ref = db.collection("payout_requests").document(req.id)
                val prefix = if (req.isAutoPayout) "YT-AUTO" else "BINT-BATCH"
                val code = "$prefix-${req.id.take(6).uppercase(Locale.US)}"
                batch.update(
                    ref,
                    mapOf(
                        "status" to "paid",
                        "payoutReference" to code,
                        "adminNote" to "Auto-settled to ${req.payoutCurrency} (${req.payoutBankOrNetwork.ifBlank { req.payoutMethod }})",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(targets.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "payout_requests")
            Result.failure(e)
        }
    }

    // --- Matches & Real-Time Messaging ---

    fun observeMatches(): Flow<List<MatchThread>> = flow {
        val uid = requireUserId()
        val path = "matches"
        emitAll(
            db.collection("matches")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        MatchThread::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.lastMessageAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeMatch(matchId: String): Flow<MatchThread?> = flow {
        val uid = requireUserId()
        val path = "matches/$matchId"
        emitAll(
            db.collection("matches").document(matchId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            MatchThread::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else null
                }
                .handleSnapshotErrors(OperationType.GET, path, uid)
        )
    }

    fun observeMessages(matchId: String): Flow<List<ChatMessage>> = flow {
        val uid = requireUserId()
        val path = "matches/$matchId/messages"
        emitAll(
            db.collection("matches").document(matchId).collection("messages")
                .whereArrayContains("participantIds", uid)
                .limit(200)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChatMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun sendMessage(
        match: MatchThread,
        senderName: String,
        text: String
    ): Result<String> {
        val uid = requireUserId()
        val cleanText = text.trim().take(1000)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty"))
        val matchRef = db.collection("matches").document(match.id)
        val msgRef = matchRef.collection("messages").document()
        return try {
            val batch = db.batch()
            batch.set(
                msgRef,
                mapOf(
                    "senderId" to uid,
                    "senderName" to senderName.trim().take(40).ifEmpty { "Member" },
                    "text" to cleanText,
                    "participantIds" to match.participantIds,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                matchRef,
                mapOf(
                    "lastMessage" to cleanText.take(500),
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(msgRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    suspend fun unmatch(matchId: String): Result<Unit> {
        requireUserId()
        val matchRef = db.collection("matches").document(matchId)
        return try {
            matchRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, matchRef.path)
            Result.failure(e)
        }
    }

    // --- BIGO Live-Style Streaming Rooms ---

    fun observeLiveRooms(): Flow<List<LiveRoom>> = flow {
        val uid = requireUserId()
        val path = "live_rooms"
        emitAll(
            db.collection("live_rooms")
                .whereEqualTo("visibility", "public")
                .limit(40)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LiveRoom::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<LiveRoom> { it.status == "live" }
                            .thenByDescending { it.viewerCount }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun createLiveRoom(
        hostName: String,
        hostAvatarKey: String,
        hostPhotoBase64: String?,
        hostIsPlus: Boolean,
        title: String,
        category: String,
        coverKey: String?
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("live_rooms").document()
        val payload = buildMap<String, Any> {
            put("hostId", uid)
            put("hostName", hostName.trim().take(40).ifEmpty { "Streamer" })
            put("hostAvatarKey", hostAvatarKey.take(40).ifEmpty { "studio" })
            if (!hostPhotoBase64.isNullOrBlank() && hostPhotoBase64.length <= 200_000) {
                put("hostPhotoBase64", hostPhotoBase64)
            }
            put("hostIsPlus", hostIsPlus)
            put("title", title.trim().take(120).ifEmpty { "Live on BINTAFACE" })
            put("category", category)
            if (!coverKey.isNullOrBlank()) put("coverKey", coverKey.take(40))
            put("visibility", "public")
            put("status", "live")
            put("viewerCount", 1L)
            put("likesCount", 0L)
            put("sparksCount", 0L)
            put("totalGiftsCents", 0L)
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun endLiveRoom(roomId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("live_rooms").document(roomId)
        return try {
            docRef.update(
                mapOf(
                    "status" to "ended",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun sendLiveHeart(roomId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("live_rooms").document(roomId)
        return try {
            docRef.update(
                mapOf(
                    "likesCount" to FieldValue.increment(1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun adjustLiveViewerCount(roomId: String, delta: Long): Result<Unit> {
        requireUserId()
        val docRef = db.collection("live_rooms").document(roomId)
        return try {
            docRef.update(
                mapOf(
                    "viewerCount" to FieldValue.increment(delta),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeLiveMessages(roomId: String): Flow<List<LiveMessage>> = flow {
        val uid = requireUserId()
        val path = "live_rooms/$roomId/messages"
        emitAll(
            db.collection("live_rooms").document(roomId).collection("messages")
                .whereEqualTo("roomVisibility", "public")
                .limit(120)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        LiveMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun sendLiveMessage(
        roomId: String,
        senderName: String,
        senderAvatarKey: String,
        text: String,
        isGiftNotice: Boolean = false
    ): Result<String> {
        val uid = requireUserId()
        val cleanText = text.trim().take(500)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty"))
        val msgRef = db.collection("live_rooms").document(roomId).collection("messages").document()
        return try {
            msgRef.set(
                mapOf(
                    "roomId" to roomId,
                    "roomVisibility" to "public",
                    "senderId" to uid,
                    "senderName" to senderName.trim().take(40).ifEmpty { "Viewer" },
                    "senderAvatarKey" to senderAvatarKey.take(40).ifEmpty { "coffee" },
                    "text" to cleanText,
                    "isGiftNotice" to isGiftNotice,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(msgRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    // --- Telegram-Style Broadcast Channels ---

    fun observeChannels(): Flow<List<SocialChannel>> = flow {
        val uid = requireUserId()
        val path = "channels"
        emitAll(
            db.collection("channels")
                .whereEqualTo("visibility", "public")
                .limit(40)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SocialChannel::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.subscriberCount }
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeMyChannelSubscriptions(): Flow<Set<String>> = flow {
        val uid = requireUserId()
        val path = "channel_subscriptions"
        emitAll(
            db.collection("channel_subscriptions")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(ChannelSubscription::class.java).map { it.channelId }.toSet()
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun createChannel(
        ownerName: String,
        ownerAvatarKey: String,
        title: String,
        handle: String,
        description: String,
        category: String
    ): Result<String> {
        val uid = requireUserId()
        val cleanTitle = title.trim().take(80)
        if (cleanTitle.length < 2) return Result.failure(IllegalArgumentException("Channel title is too short"))
        val normalizedHandle = ("@" + handle.trim().removePrefix("@").replace(Regex("[^a-zA-Z0-9_]"), "").lowercase(Locale.US))
            .take(40)
            .let { if (it.length < 2) "@channel" else it }
        val docRef = db.collection("channels").document()
        return try {
            docRef.set(
                mapOf(
                    "ownerId" to uid,
                    "ownerName" to ownerName.trim().take(40).ifEmpty { "Creator" },
                    "ownerAvatarKey" to ownerAvatarKey.take(40).ifEmpty { "coffee" },
                    "title" to cleanTitle,
                    "handle" to normalizedHandle,
                    "description" to description.trim().take(300),
                    "category" to category.take(40).ifEmpty { "General" },
                    "visibility" to "public",
                    "subscriberCount" to 1L,
                    "totalGiftsCents" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleChannelSubscription(channelId: String, currentlySubscribed: Boolean): Result<Unit> {
        val uid = requireUserId()
        val channelRef = db.collection("channels").document(channelId)
        val subRef = db.collection("channel_subscriptions").document("${channelId}_${uid}")
        return try {
            val batch = db.batch()
            if (currentlySubscribed) {
                batch.delete(subRef)
                batch.update(
                    channelRef,
                    mapOf(
                        "subscriberCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    subRef,
                    mapOf(
                        "channelId" to channelId,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    channelRef,
                    mapOf(
                        "subscriberCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, channelRef.path)
            Result.failure(e)
        }
    }

    fun observeChannelBroadcasts(channelId: String): Flow<List<ChannelBroadcast>> = flow {
        val uid = requireUserId()
        val path = "channels/$channelId/broadcasts"
        emitAll(
            db.collection("channels").document(channelId).collection("broadcasts")
                .whereEqualTo("channelVisibility", "public")
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        ChannelBroadcast::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun publishChannelBroadcast(
        channelId: String,
        ownerName: String,
        text: String,
        imageKey: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val cleanText = text.trim().take(1000)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Broadcast text cannot be empty"))
        val docRef = db.collection("channels").document(channelId).collection("broadcasts").document()
        val payload = buildMap<String, Any> {
            put("channelId", channelId)
            put("channelVisibility", "public")
            put("ownerId", uid)
            put("ownerName", ownerName.trim().take(40).ifEmpty { "Creator" })
            put("text", cleanText)
            if (!imageKey.isNullOrBlank()) put("imageKey", imageKey.take(40))
            put("createdAt", FieldValue.serverTimestamp())
        }
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Facebook-Style User Pages ---

    fun observePages(): Flow<List<SocialPage>> = flow {
        val uid = requireUserId()
        val path = "pages"
        emitAll(
            db.collection("pages")
                .whereEqualTo("visibility", "public")
                .limit(50)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SocialPage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.followersCount }
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeMyPageFollows(): Flow<Set<String>> = flow {
        val uid = requireUserId()
        val path = "page_follows"
        emitAll(
            db.collection("page_follows")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PageFollow::class.java).map { it.pageId }.toSet()
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun createPage(
        ownerName: String,
        ownerAvatarKey: String,
        name: String,
        handle: String,
        category: String,
        description: String,
        websiteUrl: String = "",
        coverKey: String = "studio",
        photoBase64: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val cleanName = name.trim().take(80)
        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Page name must be at least 2 characters."))
        }
        val cleanCategory = category.takeIf {
            it in setOf("Creator", "Business", "Community", "Entertainment", "Education", "Shopping", "News", "Artist")
        } ?: "Creator"
        val normalizedHandle = ("@" + handle.trim().removePrefix("@").replace(Regex("[^a-zA-Z0-9_.]"), "").lowercase(Locale.US))
            .take(40)
            .let { if (it.length < 2) "@${cleanName.replace(Regex("[^a-zA-Z0-9]"), "").lowercase(Locale.US).take(16).ifEmpty { "page" }}" else it }
        val docRef = db.collection("pages").document()
        val payload = buildMap<String, Any> {
            put("ownerId", uid)
            put("ownerName", ownerName.trim().take(40).ifEmpty { "Creator" })
            put("ownerAvatarKey", ownerAvatarKey.take(40).ifEmpty { "coffee" })
            put("name", cleanName)
            put("handle", normalizedHandle)
            put("category", cleanCategory)
            put("description", description.trim().take(400))
            put("websiteUrl", websiteUrl.trim().take(200))
            put("coverKey", coverKey.take(40).ifEmpty { "studio" })
            if (!photoBase64.isNullOrBlank() && photoBase64.length <= 200_000) {
                put("photoBase64", photoBase64)
            }
            put("visibility", "public")
            put("followersCount", 1L)
            put("postsCount", 0L)
            put("totalGiftsCents", 0L)
            put("createdAt", FieldValue.serverTimestamp())
            put("updatedAt", FieldValue.serverTimestamp())
        }
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePage(
        pageId: String,
        name: String,
        category: String,
        description: String,
        websiteUrl: String,
        coverKey: String,
        photoBase64: String?
    ): Result<Unit> {
        requireUserId()
        val cleanName = name.trim().take(80)
        if (cleanName.length < 2) {
            return Result.failure(IllegalArgumentException("Page name must be at least 2 characters."))
        }
        val cleanCategory = category.takeIf {
            it in setOf("Creator", "Business", "Community", "Entertainment", "Education", "Shopping", "News", "Artist")
        } ?: "Creator"
        val docRef = db.collection("pages").document(pageId)
        val updates = buildMap<String, Any> {
            put("name", cleanName)
            put("category", cleanCategory)
            put("description", description.trim().take(400))
            put("websiteUrl", websiteUrl.trim().take(200))
            put("coverKey", coverKey.take(40).ifEmpty { "studio" })
            if (!photoBase64.isNullOrBlank() && photoBase64.length <= 200_000) {
                put("photoBase64", photoBase64)
            }
            put("updatedAt", FieldValue.serverTimestamp())
        }
        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun togglePageFollow(pageId: String, currentlyFollowing: Boolean): Result<Unit> {
        val uid = requireUserId()
        val pageRef = db.collection("pages").document(pageId)
        val followRef = db.collection("page_follows").document("${pageId}_${uid}")
        return try {
            val batch = db.batch()
            if (currentlyFollowing) {
                batch.delete(followRef)
                batch.update(
                    pageRef,
                    mapOf(
                        "followersCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    followRef,
                    mapOf(
                        "pageId" to pageId,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    pageRef,
                    mapOf(
                        "followersCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, pageRef.path)
            Result.failure(e)
        }
    }

    fun observePagePosts(pageId: String): Flow<List<PagePost>> = flow {
        val uid = requireUserId()
        val path = "pages/$pageId/posts"
        emitAll(
            db.collection("pages").document(pageId).collection("posts")
                .whereEqualTo("pageVisibility", "public")
                .limit(100)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        PagePost::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeMyPagePostLikes(): Flow<Set<String>> = flow {
        val uid = requireUserId()
        val path = "page_post_likes"
        emitAll(
            db.collection("page_post_likes")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(PagePostLike::class.java).map { it.postId }.toSet()
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun publishPagePost(
        pageId: String,
        pageName: String,
        text: String,
        imageKey: String? = null,
        postPhotoBase64: String? = null
    ): Result<String> {
        val uid = requireUserId()
        val cleanText = text.trim().take(1000)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Page post cannot be empty."))
        val pageRef = db.collection("pages").document(pageId)
        val postRef = pageRef.collection("posts").document()
        val payload = buildMap<String, Any> {
            put("pageId", pageId)
            put("pageName", pageName.trim().take(80).ifEmpty { "Page" })
            put("pageVisibility", "public")
            put("ownerId", uid)
            put("text", cleanText)
            if (!imageKey.isNullOrBlank()) put("imageKey", imageKey.take(40))
            if (!postPhotoBase64.isNullOrBlank() && postPhotoBase64.length <= 200_000) {
                put("postPhotoBase64", postPhotoBase64)
            }
            put("likesCount", 0L)
            put("createdAt", FieldValue.serverTimestamp())
        }
        return try {
            val batch = db.batch()
            batch.set(postRef, payload)
            batch.update(
                pageRef,
                mapOf(
                    "postsCount" to FieldValue.increment(1L),
                    "lastPostId" to postRef.id,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(postRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, postRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleLikePagePost(pageId: String, postId: String, currentlyLiked: Boolean): Result<Unit> {
        val uid = requireUserId()
        val postRef = db.collection("pages").document(pageId).collection("posts").document(postId)
        val likeRef = db.collection("page_post_likes").document("${postId}_${uid}")
        return try {
            val batch = db.batch()
            if (currentlyLiked) {
                batch.delete(likeRef)
                batch.update(postRef, mapOf("likesCount" to FieldValue.increment(-1L)))
            } else {
                batch.set(
                    likeRef,
                    mapOf(
                        "pageId" to pageId,
                        "postId" to postId,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(postRef, mapOf("likesCount" to FieldValue.increment(1L)))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, postRef.path)
            Result.failure(e)
        }
    }

    suspend fun deletePagePost(pageId: String, postId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("pages").document(pageId).collection("posts").document(postId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- WhatsApp & Telegram Direct / Group Conversations ---

    fun observeConversations(): Flow<List<Conversation>> = flow {
        val uid = requireUserId()
        val path = "conversations"
        emitAll(
            db.collection("conversations")
                .whereArrayContains("participantIds", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Conversation::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.lastMessageAt ?: it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun createOrOpenDirectConversation(
        myName: String,
        targetId: String,
        targetName: String
    ): Result<String> {
        val uid = requireUserId()
        if (uid == targetId) return Result.failure(IllegalArgumentException("Cannot message yourself"))
        val convId = "dm_${sortedMatchId(uid, targetId)}"
        val docRef = db.collection("conversations").document(convId)
        return try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val sortedIds = if (uid < targetId) listOf(uid, targetId) else listOf(targetId, uid)
                docRef.set(
                    mapOf(
                        "type" to "direct",
                        "title" to targetName.trim().take(80).ifEmpty { "Direct Chat" },
                        "participantIds" to sortedIds,
                        "participantNames" to mapOf(
                            uid to myName.trim().take(40).ifEmpty { "Member" },
                            targetId to targetName.trim().take(40).ifEmpty { "Member" }
                        ),
                        "createdBy" to uid,
                        "lastMessage" to "Direct chat opened on BINTAFACE",
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(convId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun createGroupConversation(
        myName: String,
        title: String,
        members: Map<String, String>
    ): Result<String> {
        val uid = requireUserId()
        val allNames = members.toMutableMap().apply {
            put(uid, myName.trim().take(40).ifEmpty { "Member" })
        }
        val participantIds = allNames.keys.toList().take(25)
        if (participantIds.size < 2) {
            return Result.failure(IllegalArgumentException("Pick at least 1 other person for a group chat."))
        }
        val docRef = db.collection("conversations").document()
        return try {
            docRef.set(
                mapOf(
                    "type" to "group",
                    "title" to title.trim().take(80).ifEmpty { "BINTAFACE Group" },
                    "participantIds" to participantIds,
                    "participantNames" to allNames,
                    "createdBy" to uid,
                    "lastMessage" to "${myName.trim().ifEmpty { "Member" }} created the group",
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeDirectMessages(conversationId: String): Flow<List<DirectMessage>> = flow {
        val uid = requireUserId()
        val path = "conversations/$conversationId/messages"
        emitAll(
            db.collection("conversations").document(conversationId).collection("messages")
                .whereArrayContains("participantIds", uid)
                .limit(200)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DirectMessage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun sendDirectMessage(
        conversation: Conversation,
        senderName: String,
        text: String
    ): Result<String> {
        val uid = requireUserId()
        val cleanText = text.trim().take(1000)
        if (cleanText.isEmpty()) return Result.failure(IllegalArgumentException("Message cannot be empty"))
        val convRef = db.collection("conversations").document(conversation.id)
        val msgRef = convRef.collection("messages").document()
        return try {
            val batch = db.batch()
            batch.set(
                msgRef,
                mapOf(
                    "senderId" to uid,
                    "senderName" to senderName.trim().take(40).ifEmpty { "Member" },
                    "text" to cleanText,
                    "participantIds" to conversation.participantIds,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                convRef,
                mapOf(
                    "lastMessage" to cleanText.take(500),
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(msgRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, msgRef.path)
            Result.failure(e)
        }
    }

    // --- User Level Upgrades (Water -> Diamond, NGN 5,000+ & USD direct to App Admin with Auto-Activation) ---

    fun observeMyLevelUpgrades(userId: String): Flow<List<LevelUpgradeOrder>> = flow {
        val path = "level_upgrades"
        emitAll(
            db.collection("level_upgrades")
                .whereEqualTo("payerId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        LevelUpgradeOrder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, userId)
        )
    }

    suspend fun upgradeUserLevelDirect(
        levelId: String,
        currency: String,
        paymentMethod: String,
        paymentReference: String,
        payerName: String,
        usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE,
        googleConfirmed: Boolean = true
    ): Result<LevelUpgradeOrder> {
        val uid = requireUserId()
        if (!googleConfirmed) {
            return Result.failure(
                SecurityException("Google confirmation is required before payment and badge activation.")
            )
        }
        val tier = levelTierForId(levelId)
            ?: return Result.failure(IllegalArgumentException("Unknown level tier: $levelId"))
        val cleanCurrency = if (currency.equals("USD", ignoreCase = true)) "USD" else "NGN"
        val safeRate = usdToNgnRate.coerceIn(100L, 10000L)
        val ngnAmount = if (cleanCurrency == "NGN") {
            tier.ngnAmount
        } else {
            computeNgnWhole(tier.usdCents, safeRate).coerceAtLeast(5000L)
        }
        val amountCents = if (cleanCurrency == "NGN") {
            ((tier.ngnAmount * 100L) / safeRate).coerceIn(100L, 100000L)
        } else {
            tier.usdCents.coerceIn(100L, 100000L)
        }
        val cleanMethod = paymentMethod.takeIf {
            it in setOf("ngn_bank", "usd_bank", "card_ngn", "card_usd", "google_play")
        } ?: if (cleanCurrency == "NGN") "ngn_bank" else "usd_bank"
        val rawRef = paymentReference.trim().take(120)
        val cleanRef = rawRef.ifBlank {
            "GCONF-${tier.id.uppercase(Locale.US)}-${UUID.randomUUID().toString().take(8).uppercase(Locale.US)}"
        }
        if (cleanRef.length < 3) {
            return Result.failure(IllegalArgumentException("Enter a valid payment reference (minimum 3 characters)."))
        }
        val cleanPayerName = payerName.trim().take(40).ifBlank { "BINTAFACE Member" }

        val privateRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        val platformRef = db.collection("app_settings").document("platform")
        val upgradeRef = db.collection("level_upgrades").document()
        val ledgerRef = db.collection("revenue_ledger").document(upgradeRef.id)

        return try {
            val privateSnap = privateRef.get().await()
            val publicSnap = publicRef.get().await()
            val platformSnap = runCatching { platformRef.get().await() }.getOrNull()

            val existingRank = (privateSnap.getLong("vipLevelRank")
                ?: publicSnap.getLong("vipLevelRank")
                ?: 0L)
            if (existingRank >= 10L) {
                return Result.failure(
                    IllegalStateException("Level 10 (Professional General Diamond) is already active on your account.")
                )
            }
            val expectedNextRank = existingRank + 1L
            if (tier.rank.toLong() != expectedNextRank) {
                val expectedTier = nextLevelTierForRank(existingRank.toInt())
                val expectedLabel = expectedTier?.let { "Level ${it.rank} (${it.name})" } ?: "Level $expectedNextRank"
                return Result.failure(
                    IllegalStateException("No jumping levels allowed: upgrade step-by-step to $expectedLabel first.")
                )
            }

            val adminBankName = platformSnap?.getString("bankName")?.trim()?.takeIf { it.isNotBlank() }
                ?: DEFAULT_ADMIN_BANK_NAME
            val adminAcct = platformSnap?.getString("adminAccountNumber")?.trim()?.takeIf { it.isNotBlank() }
                ?: DEFAULT_ADMIN_ACCOUNT_NUMBER
            val adminHolder = platformSnap?.getString("adminAccountHolder")?.trim()?.takeIf { it.isNotBlank() }
                ?: DEFAULT_ADMIN_ACCOUNT_HOLDER
            val adminBankLabel = "$adminBankName $adminAcct ($adminHolder)".take(120)

            val batch = db.batch()
            if (privateSnap.exists()) {
                batch.update(
                    privateRef,
                    mapOf(
                        "vipLevel" to tier.id,
                        "vipLevelRank" to tier.rank.toLong(),
                        "lastLevelUpgradeId" to upgradeRef.id,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                val privatePayload = buildMap<String, Any> {
                    put("userId", uid)
                    put("showInDiscovery", true)
                    put("preferredIntent", "all")
                    put("isPlusMember", false)
                    put("superlikesBalance", 1L)
                    put("boostsBalance", 0L)
                    put("sparksBalance", 0L)
                    put("dailyLikesUsed", 0L)
                    put("dailyLikesDate", todayIsoDate())
                    put("vipLevel", tier.id)
                    put("vipLevelRank", tier.rank.toLong())
                    put("lastLevelUpgradeId", upgradeRef.id)
                    auth.currentUser?.email?.takeIf { it.isNotBlank() }?.let { put("email", it.take(120)) }
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.set(privateRef, privatePayload)
            }

            if (publicSnap.exists()) {
                batch.update(
                    publicRef,
                    mapOf(
                        "vipLevel" to tier.id,
                        "vipLevelRank" to tier.rank.toLong(),
                        "lastLevelUpgradeId" to upgradeRef.id,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.set(
                upgradeRef,
                mapOf(
                    "payerId" to uid,
                    "payerName" to cleanPayerName,
                    "levelId" to tier.id,
                    "levelName" to tier.name,
                    "levelRank" to tier.rank.toLong(),
                    "currency" to cleanCurrency,
                    "ngnAmount" to ngnAmount,
                    "amountCents" to amountCents,
                    "adminShareCents" to amountCents,
                    "creatorShareCents" to 0L,
                    "paymentMethod" to cleanMethod,
                    "paymentReference" to cleanRef,
                    "adminEmail" to APP_ADMIN_EMAIL,
                    "adminBankLabel" to adminBankLabel,
                    "status" to "activated",
                    "activatedAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.set(
                ledgerRef,
                mapOf(
                    "source" to "level_upgrade",
                    "productId" to "level_${tier.id}",
                    "payerId" to uid,
                    "recipientId" to "admin",
                    "grossCents" to amountCents,
                    "adminShareCents" to amountCents,
                    "creatorShareCents" to 0L,
                    "referenceToken" to "$cleanCurrency:$cleanRef".take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(
                LevelUpgradeOrder(
                    id = upgradeRef.id,
                    payerId = uid,
                    payerName = cleanPayerName,
                    levelId = tier.id,
                    levelName = tier.name,
                    levelRank = tier.rank.toLong(),
                    currency = cleanCurrency,
                    ngnAmount = ngnAmount,
                    amountCents = amountCents,
                    adminShareCents = amountCents,
                    creatorShareCents = 0L,
                    paymentMethod = cleanMethod,
                    paymentReference = cleanRef,
                    adminEmail = APP_ADMIN_EMAIL,
                    adminBankLabel = adminBankLabel,
                    status = "activated"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, upgradeRef.path)
            Result.failure(e)
        }
    }

    // --- BINTAFACE Global Radio Access (NGN 2,000) & Stations ---

    fun observeMyRadioPasses(userId: String): Flow<List<RadioPass>> = flow {
        requireUserId()
        val path = "radio_passes"
        emitAll(
            db.collection(path)
                .whereEqualTo("payerId", userId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        RadioPass::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, userId)
        )
    }

    fun observeRadioChannels(): Flow<List<RadioChannel>> = flow {
        val uid = requireUserId()
        val path = "radio_channels"
        emitAll(
            db.collection(path)
                .whereEqualTo("visibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        RadioChannel::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedWith(
                        compareByDescending<RadioChannel> { it.status == "live" }
                            .thenByDescending { it.listenersCount }
                            .thenByDescending { it.createdAt ?: Timestamp(0, 0) }
                    )
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeMyRadioSubscriptions(): Flow<Set<String>> = flow {
        val uid = requireUserId()
        val path = "radio_subscriptions"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.documents.mapNotNull { it.getString("channelId") }.toSet()
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    fun observeRadioBroadcasts(channelId: String): Flow<List<RadioBroadcast>> = flow {
        val uid = requireUserId()
        val path = "radio_channels/$channelId/broadcasts"
        emitAll(
            db.collection("radio_channels")
                .document(channelId)
                .collection("broadcasts")
                .whereEqualTo("channelVisibility", "public")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        RadioBroadcast::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .handleSnapshotErrors(OperationType.LIST, path, uid)
        )
    }

    suspend fun purchaseRadioAccessDirect(
        currency: String,
        paymentMethod: String,
        paymentReference: String,
        payerName: String,
        usdToNgnRate: Long = DEFAULT_USD_TO_NGN_RATE,
        googleConfirmed: Boolean = true
    ): Result<RadioPass> {
        val uid = requireUserId()
        if (!googleConfirmed) {
            return Result.failure(
                SecurityException("Google confirmation is required before Radio Access activation.")
            )
        }
        val cleanCurrency = if (currency.equals("USD", ignoreCase = true)) "USD" else "NGN"
        val safeRate = usdToNgnRate.coerceIn(100L, 10000L)
        val ngnAmount = if (cleanCurrency == "NGN") {
            RADIO_ACCESS_FEE_NGN
        } else {
            computeNgnWhole(RADIO_ACCESS_USD_CENTS, safeRate).coerceAtLeast(RADIO_ACCESS_FEE_NGN)
        }
        val amountCents = if (cleanCurrency == "NGN") {
            ((RADIO_ACCESS_FEE_NGN * 100L) / safeRate).coerceIn(100L, 100000L)
        } else {
            RADIO_ACCESS_USD_CENTS.coerceIn(100L, 100000L)
        }
        val cleanMethod = paymentMethod.takeIf {
            it in setOf("ngn_bank", "usd_bank", "card_ngn", "card_usd", "google_play")
        } ?: if (cleanCurrency == "NGN") "ngn_bank" else "usd_bank"
        val rawRef = paymentReference.trim().take(120)
        val cleanRef = rawRef.ifBlank {
            "GRADIO-${UUID.randomUUID().toString().take(8).uppercase(Locale.US)}"
        }
        if (cleanRef.length < 3) {
            return Result.failure(IllegalArgumentException("Enter a valid payment reference (minimum 3 characters)."))
        }
        val cleanPayerName = payerName.trim().take(40).ifBlank { "BINTAFACE Member" }

        val privateRef = db.collection("users").document(uid)
        val publicRef = db.collection("public_profiles").document(uid)
        val platformRef = db.collection("app_settings").document("platform")
        val passRef = db.collection("radio_passes").document()
        val ledgerRef = db.collection("revenue_ledger").document(passRef.id)

        return try {
            val privateSnap = privateRef.get().await()
            val publicSnap = publicRef.get().await()
            val platformSnap = runCatching { platformRef.get().await() }.getOrNull()

            if (privateSnap.exists() && privateSnap.getBoolean("hasRadioAccess") == true) {
                return Result.failure(
                    IllegalStateException("Global Radio Access is already ACTIVE on your account!")
                )
            }

            val adminBankName = platformSnap?.getString("bankName")?.trim()?.takeIf { it.isNotBlank() }
                ?: DEFAULT_ADMIN_BANK_NAME
            val adminAcct = platformSnap?.getString("adminAccountNumber")?.trim()?.takeIf { it.isNotBlank() }
                ?: DEFAULT_ADMIN_ACCOUNT_NUMBER
            val adminHolder = platformSnap?.getString("adminAccountHolder")?.trim()?.takeIf { it.isNotBlank() }
                ?: DEFAULT_ADMIN_ACCOUNT_HOLDER
            val adminBankLabel = "$adminBankName $adminAcct ($adminHolder)".take(120)

            val batch = db.batch()
            if (privateSnap.exists()) {
                batch.update(
                    privateRef,
                    mapOf(
                        "hasRadioAccess" to true,
                        "radioPassId" to passRef.id,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                val privatePayload = buildMap<String, Any> {
                    put("userId", uid)
                    put("showInDiscovery", true)
                    put("preferredIntent", "all")
                    put("isPlusMember", false)
                    put("superlikesBalance", 1L)
                    put("boostsBalance", 0L)
                    put("sparksBalance", 0L)
                    put("dailyLikesUsed", 0L)
                    put("dailyLikesDate", todayIsoDate())
                    put("hasRadioAccess", true)
                    put("radioPassId", passRef.id)
                    auth.currentUser?.email?.takeIf { it.isNotBlank() }?.let { put("email", it.take(120)) }
                    put("createdAt", FieldValue.serverTimestamp())
                    put("updatedAt", FieldValue.serverTimestamp())
                }
                batch.set(privateRef, privatePayload)
            }

            if (publicSnap.exists()) {
                batch.update(
                    publicRef,
                    mapOf(
                        "hasRadioAccess" to true,
                        "radioPassId" to passRef.id,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.set(
                passRef,
                mapOf(
                    "payerId" to uid,
                    "payerName" to cleanPayerName,
                    "currency" to cleanCurrency,
                    "ngnAmount" to ngnAmount,
                    "amountCents" to amountCents,
                    "adminShareCents" to amountCents,
                    "creatorShareCents" to 0L,
                    "paymentMethod" to cleanMethod,
                    "paymentReference" to cleanRef,
                    "adminEmail" to APP_ADMIN_EMAIL,
                    "adminBankLabel" to adminBankLabel,
                    "status" to "active",
                    "activatedAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.set(
                ledgerRef,
                mapOf(
                    "source" to "radio_access",
                    "productId" to "bintaface_radio_access",
                    "payerId" to uid,
                    "recipientId" to "admin",
                    "grossCents" to amountCents,
                    "adminShareCents" to amountCents,
                    "creatorShareCents" to 0L,
                    "referenceToken" to "$cleanCurrency:$cleanRef".take(500),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().await()
            Result.success(
                RadioPass(
                    id = passRef.id,
                    payerId = uid,
                    payerName = cleanPayerName,
                    currency = cleanCurrency,
                    ngnAmount = ngnAmount,
                    amountCents = amountCents,
                    adminShareCents = amountCents,
                    creatorShareCents = 0L,
                    paymentMethod = cleanMethod,
                    paymentReference = cleanRef,
                    adminEmail = APP_ADMIN_EMAIL,
                    adminBankLabel = adminBankLabel,
                    status = "active"
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, passRef.path)
            Result.failure(e)
        }
    }

    suspend fun createRadioChannel(
        ownerName: String,
        ownerAvatarKey: String,
        title: String,
        frequency: String,
        handle: String,
        category: String,
        description: String,
        coverKey: String = "studio"
    ): Result<String> {
        val uid = requireUserId()
        val cleanTitle = title.trim().take(80)
        if (cleanTitle.length < 2) {
            return Result.failure(IllegalArgumentException("Radio station name must be at least 2 characters."))
        }
        val cleanFrequency = frequency.trim().take(24).ifBlank { "104.7 FM" }
        val normalizedHandle = ("@" + handle.trim().removePrefix("@").replace(Regex("[^a-zA-Z0-9_]"), "").lowercase(Locale.US))
            .take(40)
            .let { if (it.length < 2) "@bintafacefm" else it }
        val safeCategory = category.takeIf { it in RADIO_CATEGORIES } ?: "Global"
        val channelRef = db.collection("radio_channels").document()
        val subRef = db.collection("radio_subscriptions").document("${channelRef.id}_$uid")
        return try {
            val batch = db.batch()
            batch.set(
                channelRef,
                mapOf(
                    "ownerId" to uid,
                    "ownerName" to ownerName.trim().take(40).ifEmpty { "Broadcaster" },
                    "ownerAvatarKey" to ownerAvatarKey.take(40).ifEmpty { "studio" },
                    "title" to cleanTitle,
                    "frequency" to cleanFrequency,
                    "handle" to normalizedHandle,
                    "category" to safeCategory,
                    "description" to description.trim().take(400),
                    "coverKey" to coverKey.take(40).ifEmpty { "studio" },
                    "visibility" to "public",
                    "status" to "live",
                    "listenersCount" to 0L,
                    "subscribersCount" to 1L,
                    "totalGiftsCents" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                subRef,
                mapOf(
                    "channelId" to channelRef.id,
                    "userId" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            Result.success(channelRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, channelRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateRadioChannelStatus(
        channelId: String,
        newStatus: String
    ): Result<Unit> {
        requireUserId()
        val safeStatus = if (newStatus == "off_air") "off_air" else "live"
        val channelRef = db.collection("radio_channels").document(channelId)
        return try {
            channelRef.update(
                mapOf(
                    "status" to safeStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, channelRef.path)
            Result.failure(e)
        }
    }

    suspend fun recordRadioListenerPulse(channelId: String, delta: Long): Result<Unit> {
        requireUserId()
        val channelRef = db.collection("radio_channels").document(channelId)
        return try {
            val snap = channelRef.get().await()
            if (!snap.exists()) return Result.success(Unit)
            val current = snap.getLong("listenersCount") ?: 0L
            val next = (current + delta).coerceAtLeast(0L)
            if (next != current) {
                channelRef.update(
                    mapOf(
                        "listenersCount" to next,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, channelRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleRadioSubscription(channelId: String, currentlySubscribed: Boolean): Result<Unit> {
        val uid = requireUserId()
        val subId = "${channelId}_$uid"
        val subRef = db.collection("radio_subscriptions").document(subId)
        val channelRef = db.collection("radio_channels").document(channelId)
        return try {
            val batch = db.batch()
            if (currentlySubscribed) {
                batch.delete(subRef)
                batch.update(
                    channelRef,
                    mapOf(
                        "subscribersCount" to FieldValue.increment(-1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.set(
                    subRef,
                    mapOf(
                        "channelId" to channelId,
                        "userId" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    channelRef,
                    mapOf(
                        "subscribersCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, channelRef.path)
            Result.failure(e)
        }
    }

    suspend fun publishRadioBroadcast(
        channelId: String,
        ownerName: String,
        title: String,
        text: String,
        audioBase64: String? = null,
        audioDurationSec: Long = 0L
    ): Result<Unit> {
        val uid = requireUserId()
        val cleanTitle = title.trim().take(120)
        val cleanText = text.trim().take(1000)
        if (cleanTitle.length < 2 || cleanText.isBlank()) {
            return Result.failure(IllegalArgumentException("Enter a show title and broadcast notes."))
        }
        val cleanAudio = audioBase64?.takeIf { it.isNotBlank() && it.length <= 140_000 }
        val broadcastRef = db.collection("radio_channels")
            .document(channelId)
            .collection("broadcasts")
            .document()
        return try {
            val payload = buildMap<String, Any> {
                put("channelId", channelId)
                put("channelVisibility", "public")
                put("ownerId", uid)
                put("ownerName", ownerName.trim().take(40).ifEmpty { "Host" })
                put("title", cleanTitle)
                put("text", cleanText)
                if (cleanAudio != null) {
                    put("audioBase64", cleanAudio)
                    put("audioDurationSec", audioDurationSec.coerceIn(0L, 180L))
                }
                put("createdAt", FieldValue.serverTimestamp())
            }
            broadcastRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, broadcastRef.path)
            Result.failure(e)
        }
    }
}
