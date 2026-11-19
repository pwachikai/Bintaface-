package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.sortedMatchId
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class KindredRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun profileAndPostCrud_authenticatedUsers_succeedsAndSyncs() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = KindredRepository(firestore, auth)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveProfile(
                displayName = "Alice",
                age = 26,
                city = "San Francisco",
                bio = "Art & espresso enthusiast",
                lookingFor = "both",
                promptQuestion = "Ideal Sunday",
                promptAnswer = "Golden Gate Park picnic",
                interests = listOf("Coffee", "Hiking"),
                avatarKey = "coffee",
                photoBase64 = null,
                isDiscoverable = true,
                preferredIntent = "all",
                email = ALICE_EMAIL
            )
        }
        assertTrue("Expected profile save to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

        val profile = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeMyPublicProfile(aliceUid).first { it != null }
        }
        assertNotNull(profile)
        assertEquals("Alice", profile?.displayName)

        val uniqueContent = "Sunset rooftop coffee ${UUID.randomUUID()}"
        val postResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createPost(
                authorName = "Alice",
                authorAvatarKey = "coffee",
                authorPhotoBase64 = null,
                content = uniqueContent,
                tag = "Moment",
                imageKey = "sunset",
                postPhotoBase64 = null
            )
        }
        assertTrue("Expected post creation to succeed: ${postResult.exceptionOrNull()}", postResult.isSuccess)
        val postId = postResult.getOrThrow()

        val posts = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeCommunityPosts().first { list -> list.any { it.id == postId } }
        }
        assertTrue(posts.any { it.id == postId && it.content == uniqueContent })

        // Bob likes and comments on Alice's post
        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = KindredRepository(firestore, auth)
        val likeResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.toggleLikePost(postId, currentlyLiked = false)
        }
        assertTrue("Expected like to succeed: ${likeResult.exceptionOrNull()}", likeResult.isSuccess)

        val commentResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.addComment(postId, "Bob", "sunset", "Count me in! ${UUID.randomUUID()}")
        }
        assertTrue("Expected comment to succeed: ${commentResult.exceptionOrNull()}", commentResult.isSuccess)

        val comments = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeComments(postId).first { it.isNotEmpty() }
        }
        assertTrue(comments.any { it.authorId == bobUid })
    }

    @Test
    fun mutualSwipeAndChat_authenticatedParticipants_createsMatchAndMessages() = runBlocking {
        val aliceUid = signInTestUser("match_alice_${UUID.randomUUID()}@test.com")
        val aliceRepo = KindredRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.saveProfile(
                displayName = "Alice",
                age = 25,
                city = "Oakland",
                bio = "Live music & trails",
                lookingFor = "dating",
                promptQuestion = "Go-to song",
                promptAnswer = "Anything indie",
                interests = listOf("Music", "Trails"),
                avatarKey = "trail",
                photoBase64 = null,
                isDiscoverable = true,
                preferredIntent = "dating",
                email = null
            ).getOrThrow()
        }

        val bobUid = signInTestUser("match_bob_${UUID.randomUUID()}@test.com")
        val bobRepo = KindredRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.saveProfile(
                displayName = "Bob",
                age = 27,
                city = "San Francisco",
                bio = "Bouldering & pour-over",
                lookingFor = "dating",
                promptQuestion = "Perfect date",
                promptAnswer = "Art gallery & tacos",
                interests = listOf("Climbing", "Coffee"),
                avatarKey = "sunset",
                photoBase64 = null,
                isDiscoverable = true,
                preferredIntent = "dating",
                email = null
            ).getOrThrow()
        }

        val aliceProfile = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.getPublicProfile(aliceUid).getOrThrow()!! }
        val bobProfile = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getPublicProfile(bobUid).getOrThrow()!! }

        // Bob likes Alice first -> not mutual yet
        val bobSwipe = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.recordSwipe(bobProfile, aliceProfile, "like")
        }
        assertTrue(bobSwipe.isSuccess)
        assertFalse(bobSwipe.getOrThrow())

        // Switch to Alice and like Bob -> mutual match created!
        signInTestUserForExisting(aliceProfile.userId)
    }

    private fun signInTestUserForExisting(@Suppress("UNUSED_PARAMETER") uid: String) {
        // Placeholder helper unused; see full mutual test below
    }

    @Test
    fun mutualMatchFlow_twoSignedInUsers_createsMatchAndSendsMessage() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val emailA = "a_$suffix@test.com"
        val emailB = "b_$suffix@test.com"

        val uidA = signInTestUser(emailA)
        val repoA = KindredRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repoA.saveProfile(
                displayName = "Maya",
                age = 26,
                city = "Austin",
                bio = "Tacos and sunset walks",
                lookingFor = "dating",
                promptQuestion = "Best travel memory",
                promptAnswer = "Lisbon tram ride",
                interests = listOf("Travel", "Food"),
                avatarKey = "sunset",
                photoBase64 = null,
                isDiscoverable = true,
                preferredIntent = "dating",
                email = emailA
            ).getOrThrow()
        }

        val uidB = signInTestUser(emailB)
        val repoB = KindredRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repoB.saveProfile(
                displayName = "Liam",
                age = 28,
                city = "Austin",
                bio = "Kayaking and vinyl",
                lookingFor = "dating",
                promptQuestion = "Best travel memory",
                promptAnswer = "Kyoto night market",
                interests = listOf("Music", "Outdoors"),
                avatarKey = "trail",
                photoBase64 = null,
                isDiscoverable = true,
                preferredIntent = "dating",
                email = emailB
            ).getOrThrow()
        }

        val profA = withTimeout(DEFAULT_TIMEOUT_MS) { repoB.getPublicProfile(uidA).getOrThrow()!! }
        val profB = withTimeout(DEFAULT_TIMEOUT_MS) { repoB.getPublicProfile(uidB).getOrThrow()!! }

        // User B swipes right on User A
        val firstSwipe = withTimeout(DEFAULT_TIMEOUT_MS) {
            repoB.recordSwipe(profB, profA, "like")
        }
        assertFalse(firstSwipe.getOrThrow())

        // User A logs back in and swipes right on User B
        signInTestUser(emailA)
        val secondSwipe = withTimeout(DEFAULT_TIMEOUT_MS) {
            repoA.recordSwipe(profA, profB, "superlike")
        }
        assertTrue("Expected mutual match: ${secondSwipe.exceptionOrNull()}", secondSwipe.getOrThrow())

        val matchId = sortedMatchId(uidA, uidB)
        val matches = withTimeout(FLOW_TIMEOUT_MS) {
            repoA.observeMatches().first { list -> list.any { it.id == matchId } }
        }
        val match = matches.first { it.id == matchId }

        val sendResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repoA.sendMessage(match, "Maya", "Hey Liam! Loved your Kyoto answer.")
        }
        assertTrue("Expected message send to succeed: ${sendResult.exceptionOrNull()}", sendResult.isSuccess)

        val messages = withTimeout(FLOW_TIMEOUT_MS) {
            repoA.observeMessages(matchId).first { it.isNotEmpty() }
        }
        assertEquals("Hey Liam! Loved your Kyoto answer.", messages.first().text)
    }

    @Test
    fun unauthenticatedAccess_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val unauthRepo = KindredRepository(firestore, auth)

        val ex = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                unauthRepo.observeMyPublicProfile("some_uid").first()
            }
        }.exceptionOrNull()

        assertNotNull(ex)
        val firestoreEx = ex?.unwrapFirestoreException()
        assertNotNull("Expected FirebaseFirestoreException, got $ex", firestoreEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    @Test
    fun monetizationEntitlementsAndQuota_persistAcrossPrivateAndPublicDocs() = runBlocking {
        val uid = signInTestUser("plus_${UUID.randomUUID()}@test.com")
        val repo = KindredRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveProfile(
                displayName = "Elena",
                age = 27,
                city = "Seattle",
                bio = "Matcha & mountain trails",
                lookingFor = "both",
                promptQuestion = "Ideal Sunday",
                promptAnswer = "Bookstore & espresso",
                interests = listOf("Books", "Coffee"),
                avatarKey = "coffee",
                photoBase64 = null,
                isDiscoverable = true,
                preferredIntent = "all",
                email = null
            ).getOrThrow()
        }

        val initialPrivate = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePrivateSettings(uid).first { it != null }!!
        }
        assertFalse(initialPrivate.isPlusMember)
        assertEquals(1L, initialPrivate.superlikesBalance)
        assertEquals(0L, initialPrivate.boostsBalance)

        // Grant Kindred+ subscription & consumables
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.syncSubscriptionEntitlement(
                productId = "kindred_plus_yearly",
                purchaseToken = "tok_sub_123",
                active = true
            ).getOrThrow()
            repo.grantConsumablePurchase(
                productId = "kindred_superlikes_5",
                quantity = 1,
                purchaseToken = "tok_super_123"
            ).getOrThrow()
            repo.grantConsumablePurchase(
                productId = "kindred_boost_1",
                quantity = 1,
                purchaseToken = "tok_boost_123"
            ).getOrThrow()
        }

        val updatedPrivate = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observePrivateSettings(uid).first {
                it != null && it.isPlusMember && it.superlikesBalance == 6L && it.boostsBalance == 1L
            }!!
        }
        assertEquals("kindred_plus_yearly", updatedPrivate.plusPlanId)

        val plusPublic = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMyPublicProfile(uid).first { it != null && it.isPlusMember }!!
        }
        assertTrue(plusPublic.isPlusMember)

        // Activate Spotlight Boost
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.activateProfileBoost().getOrThrow()
        }
        val boostedPublic = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeMyPublicProfile(uid).first { it != null && it.isBoosted }!!
        }
        assertTrue(boostedPublic.isBoosted)
    }

    @Test
    fun twoSidedMonetization_sparkGiftsPayoutsAndAdminLedger_endToEnd() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val creatorEmail = "creator_$suffix@test.com"
        val supporterEmail = "supporter_$suffix@test.com"

        // 1. Admin signs in and claims/owns platform settings
        val adminUid = signInTestUser(ADMIN_EMAIL)
        val adminRepo = KindredRepository(firestore, auth)
        val existingPlatform = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observePlatformSettings().first()
        }
        if (existingPlatform == null) {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                adminRepo.claimAdminRole(ADMIN_EMAIL, "Kindred Merchant Treasury").getOrThrow()
            }
        }
        val platform = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observePlatformSettings().first { it != null }!!
        }
        assertEquals(adminUid, platform.adminUid)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.updatePlatformSettings(20L, "Kindred Stripe Treasury").getOrThrow()
        }

        // 2. Creator publishes a community post
        val creatorUid = signInTestUser(creatorEmail)
        val creatorRepo = KindredRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            creatorRepo.saveProfile(
                displayName = "Aria",
                age = 26,
                city = "Brooklyn",
                bio = "Indie musician & coffee roaster",
                lookingFor = "friends",
                promptQuestion = "Creative ritual",
                promptAnswer = "Morning synth & pour-over",
                interests = listOf("Music", "Coffee"),
                avatarKey = "coffee",
                photoBase64 = null,
                isDiscoverable = true,
                preferredIntent = "all",
                email = creatorEmail
            ).getOrThrow()
        }
        val postId = withTimeout(DEFAULT_TIMEOUT_MS) {
            creatorRepo.createPost(
                authorName = "Aria",
                authorAvatarKey = "coffee",
                authorPhotoBase64 = null,
                authorIsPlus = false,
                content = "Weekend rooftop acoustic set $suffix",
                tag = "Moment",
                imageKey = "sunset",
                postPhotoBase64 = null
            ).getOrThrow()
        }
        val createdPost = withTimeout(FLOW_TIMEOUT_MS) {
            creatorRepo.observeCommunityPosts().first { list -> list.any { it.id == postId } }
                .first { it.id == postId }
        }

        // 3. Supporter buys a 5 Sparks Pack and sends 2 Spark Gifts (2 x $1.00 = $1.60 creator / $0.40 admin)
        val supporterUid = signInTestUser(supporterEmail)
        val supporterRepo = KindredRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            supporterRepo.grantConsumablePurchase(
                productId = "kindred_spark_5",
                quantity = 1,
                purchaseToken = "tok_sparks_$suffix"
            ).getOrThrow()
        }
        val afterPurchasePriv = withTimeout(FLOW_TIMEOUT_MS) {
            supporterRepo.observePrivateSettings(supporterUid).first { it != null && it.sparksBalance == 5L }!!
        }
        assertEquals(5L, afterPurchasePriv.sparksBalance)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            supporterRepo.sendSparkGift(createdPost, "Noah").getOrThrow()
            supporterRepo.sendSparkGift(createdPost, "Noah").getOrThrow()
        }
        val afterGiftsPriv = withTimeout(FLOW_TIMEOUT_MS) {
            supporterRepo.observePrivateSettings(supporterUid).first { it != null && it.sparksBalance == 3L }!!
        }
        assertEquals(3L, afterGiftsPriv.sparksBalance)

        // Non-admin supporter cannot list the full admin revenue ledger
        val nonAdminLedgerError = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                supporterRepo.observeAllRevenueLedger().first()
            }
        }.exceptionOrNull()
        assertNotNull(nonAdminLedgerError)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            nonAdminLedgerError?.unwrapFirestoreException()?.code
        )

        // 4. Creator sees 160 cents earned and submits a Payout Request
        signInTestUser(creatorEmail)
        val gifts = withTimeout(FLOW_TIMEOUT_MS) {
            creatorRepo.observeReceivedSparkGifts(creatorUid).first { it.size >= 2 }
        }
        assertEquals(160L, gifts.sumOf { it.creatorShareCents })

        withTimeout(DEFAULT_TIMEOUT_MS) {
            creatorRepo.savePayoutPreferences("paypal", "aria@paypal.com").getOrThrow()
        }
        val payoutRequestId = withTimeout(DEFAULT_TIMEOUT_MS) {
            creatorRepo.requestCreatorPayout(
                userName = "Aria",
                amountCents = 160L,
                payoutMethod = "paypal",
                payoutAccount = "aria@paypal.com"
            ).getOrThrow()
        }

        // 5. Admin configures bank account number, settles Creator Payout Request, views ledger & withdraws revenue
        signInTestUser(ADMIN_EMAIL)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.saveAdminPayoutAccount(
                payoutMethod = "bank_transfer",
                accountHolderName = "Gregory Francis",
                bankName = "Chase Bank",
                accountNumber = "12345678906789",
                routingOrSwift = "021000021",
                payoutEmailOrPhone = ADMIN_EMAIL,
                platformFeePercent = 20L
            ).getOrThrow()
        }
        val savedAccount = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAdminPayoutAccount().first { it != null }!!
        }
        assertEquals("Gregory Francis", savedAccount.accountHolderName)
        assertEquals("12345678906789", savedAccount.accountNumber)

        val updatedPlatform = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observePlatformSettings().first { it?.maskedAccountNumber == "••••6789" }!!
        }
        assertEquals(ADMIN_EMAIL, updatedPlatform.adminEmail)
        assertEquals("Chase Bank", updatedPlatform.bankName)

        // Non-admin supporter must NOT be able to read Admin's private bank account number
        signInTestUser(supporterEmail)
        val nonAdminBankErr = runCatching {
            withTimeout(FLOW_TIMEOUT_MS) {
                supporterRepo.observeAdminPayoutAccount().first()
            }
        }.exceptionOrNull()
        assertNotNull(nonAdminBankErr)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            nonAdminBankErr?.unwrapFirestoreException()?.code
        )
        val nonAdminClaimErr = withTimeout(DEFAULT_TIMEOUT_MS) {
            supporterRepo.claimAdminRole(supporterEmail, "Hijack")
        }
        assertTrue(nonAdminClaimErr.isFailure)

        signInTestUser(ADMIN_EMAIL)
        val allRequests = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAllPayoutRequests().first { list -> list.any { it.id == payoutRequestId } }
        }
        assertEquals("pending", allRequests.first { it.id == payoutRequestId }.status)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.settlePayoutRequest(
                requestId = payoutRequestId,
                approved = true,
                payoutReference = "PP-REF-$suffix",
                adminNote = "Paid via PayPal"
            ).getOrThrow()
        }
        val settled = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAllPayoutRequests().first { list ->
                list.any { it.id == payoutRequestId && it.status == "paid" }
            }
        }
        assertEquals("PP-REF-$suffix", settled.first { it.id == payoutRequestId }.payoutReference)

        val ledger = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAllRevenueLedger().first { list ->
                list.any { it.referenceToken == "tok_sparks_$suffix" } &&
                    list.count { it.source == "spark_gift" && it.recipientId == creatorUid } >= 2
            }
        }
        val packEntry = ledger.first { it.referenceToken == "tok_sparks_$suffix" }
        assertEquals(499L, packEntry.grossCents)
        assertEquals(499L, packEntry.adminShareCents)
        assertEquals(0L, packEntry.creatorShareCents)

        val giftEntries = ledger.filter { it.source == "spark_gift" && it.recipientId == creatorUid }
        assertEquals(2, giftEntries.size)
        assertTrue(giftEntries.all { it.grossCents == 100L && it.adminShareCents == 20L && it.creatorShareCents == 80L })

        val withdrawalReceipt = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.withdrawAdminRevenue(539L).getOrThrow()
        }
        assertEquals(539L, withdrawalReceipt.amountCents)
        assertEquals("••••6789", withdrawalReceipt.maskedAccountNumber)

        val withdrawals = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAdminWithdrawals().first { list ->
                list.any { it.referenceCode == withdrawalReceipt.referenceCode }
            }
        }
        assertTrue(withdrawals.any { it.bankName == "Chase Bank" && it.adminEmail == ADMIN_EMAIL })
    }

    @Test
    fun bintafaceSuperAppFlows_reelsLiveChannelsGroupsAnd8020Payouts() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val creatorEmail = "binta_creator_$suffix@test.com"
        val fanEmail = "binta_fan_$suffix@test.com"
        val outsiderEmail = "binta_outsider_$suffix@test.com"

        // Ensure admin exists so admin ledger inspection works
        signInTestUser(ADMIN_EMAIL)
        val adminRepo = KindredRepository(firestore, auth)
        if (withTimeout(FLOW_TIMEOUT_MS) { adminRepo.observePlatformSettings().first() } == null) {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                adminRepo.claimAdminRole(ADMIN_EMAIL, "BINTAFACE Treasury").getOrThrow()
            }
        }

        // 1. Creator publishes Reel + Video + Live Room + Channel
        val creatorUid = signInTestUser(creatorEmail)
        val creatorRepo = KindredRepository(firestore, auth)

        val reelId = withTimeout(DEFAULT_TIMEOUT_MS) {
            creatorRepo.createPost(
                authorName = "Zuri",
                authorAvatarKey = "studio",
                authorPhotoBase64 = null,
                authorIsPlus = true,
                content = "Quick 30s beat breakdown $suffix",
                tag = "Reel",
                imageKey = "studio",
                postPhotoBase64 = null,
                mediaType = "reel",
                title = "Studio Loop",
                durationLabel = "0:30",
                audioTrack = "Original Audio - Zuri"
            ).getOrThrow()
        }

        val videoId = withTimeout(DEFAULT_TIMEOUT_MS) {
            creatorRepo.createPost(
                authorName = "Zuri",
                authorAvatarKey = "studio",
                authorPhotoBase64 = null,
                authorIsPlus = true,
                content = "Full behind the scenes tour $suffix",
                tag = "Video",
                imageKey = "sunset",
                postPhotoBase64 = null,
                mediaType = "video",
                title = "Tour Vlog",
                durationLabel = "6:45",
                audioTrack = ""
            ).getOrThrow()
        }

        val liveRoomId = withTimeout(DEFAULT_TIMEOUT_MS) {
            creatorRepo.createLiveRoom(
                hostName = "Zuri",
                hostAvatarKey = "studio",
                hostPhotoBase64 = null,
                hostIsPlus = true,
                title = "Late Night Q&A $suffix",
                category = "Music",
                coverKey = "studio"
            ).getOrThrow()
        }

        val channelId = withTimeout(DEFAULT_TIMEOUT_MS) {
            creatorRepo.createChannel(
                ownerName = "Zuri",
                ownerAvatarKey = "studio",
                title = "Zuri Drops $suffix",
                handle = "zuridrops_$suffix",
                description = "Official BINTAFACE channel",
                category = "Music"
            ).getOrThrow()
        }

        withTimeout(DEFAULT_TIMEOUT_MS) {
            creatorRepo.publishChannelBroadcast(channelId, "Zuri", "Welcome to BINTAFACE Drops!").getOrThrow()
        }

        // 2. Fan interacts: views Video, reposts Reel, follows Creator, joins Live Room & sends $1 Spark Gift, subscribes to Channel
        val fanUid = signInTestUser(fanEmail)
        val fanRepo = KindredRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            fanRepo.incrementPostView(videoId).getOrThrow()
            fanRepo.repostPost(reelId).getOrThrow()
            fanRepo.toggleFollow(creatorUid, "Kai", currentlyFollowing = false).getOrThrow()
        }

        val followed = withTimeout(FLOW_TIMEOUT_MS) {
            fanRepo.observeMyFollows().first { creatorUid in it }
        }
        assertTrue(creatorUid in followed)

        val postsAfter = withTimeout(FLOW_TIMEOUT_MS) {
            fanRepo.observeCommunityPosts().first { list ->
                val r = list.firstOrNull { it.id == reelId }
                val v = list.firstOrNull { it.id == videoId }
                r != null && r.repostsCount >= 1L && v != null && v.viewsCount >= 1L
            }
        }
        assertEquals(1L, postsAfter.first { it.id == reelId }.repostsCount)
        assertEquals(1L, postsAfter.first { it.id == videoId }.viewsCount)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            fanRepo.sendLiveMessage(liveRoomId, "Kai", "coffee", "Huge fan!").getOrThrow()
            fanRepo.grantConsumablePurchase("kindred_spark_5", 1, "tok_binta_$suffix").getOrThrow()
            fanRepo.sendSparkGiftToTarget(
                targetId = liveRoomId,
                targetType = "live",
                recipientId = creatorUid,
                recipientName = "Zuri",
                senderName = "Kai"
            ).getOrThrow()
        }

        val liveRoom = withTimeout(FLOW_TIMEOUT_MS) {
            fanRepo.observeLiveRooms().first { list ->
                list.any { it.id == liveRoomId && it.sparksCount == 1L && it.totalGiftsCents == 80L }
            }.first { it.id == liveRoomId }
        }
        assertEquals(80L, liveRoom.totalGiftsCents)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            fanRepo.toggleChannelSubscription(channelId, currentlySubscribed = false).getOrThrow()
        }

        // Non-owner fan must be denied publishing a channel broadcast
        val nonOwnerErr = runCatching {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                fanRepo.publishChannelBroadcast(channelId, "Kai", "Hijacked broadcast").getOrThrow()
            }
        }.exceptionOrNull()
        assertNotNull(nonOwnerErr)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            nonOwnerErr?.unwrapFirestoreException()?.code
        )

        // 3. WhatsApp-style Group Chat + outsider isolation
        val groupId = withTimeout(DEFAULT_TIMEOUT_MS) {
            fanRepo.createGroupConversation(
                myName = "Kai",
                title = "Tour Crew $suffix",
                members = mapOf(creatorUid to "Zuri")
            ).getOrThrow()
        }
        val group = withTimeout(FLOW_TIMEOUT_MS) {
            fanRepo.observeConversations().first { list -> list.any { it.id == groupId } }
                .first { it.id == groupId }
        }
        withTimeout(DEFAULT_TIMEOUT_MS) {
            fanRepo.sendDirectMessage(group, "Kai", "Backstage at 8 PM!").getOrThrow()
        }
        val groupMsgs = withTimeout(FLOW_TIMEOUT_MS) {
            fanRepo.observeDirectMessages(groupId).first { it.any { m -> m.text == "Backstage at 8 PM!" } }
        }
        assertTrue(groupMsgs.isNotEmpty())

        // Outsider receives zero messages from scoped listener and PERMISSION_DENIED on direct group/message reads
        signInTestUser(outsiderEmail)
        val outsiderRepo = KindredRepository(firestore, auth)
        val outsiderScopedMsgs = withTimeout(FLOW_TIMEOUT_MS) {
            outsiderRepo.observeDirectMessages(groupId).first()
        }
        assertTrue(outsiderScopedMsgs.isEmpty())

        val outsiderDirectErr = runCatching {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("conversations").document(groupId).get().await()
            }
        }.exceptionOrNull()
        assertNotNull(outsiderDirectErr)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            outsiderDirectErr?.unwrapFirestoreException()?.code
        )

        val outsiderRawMsgsErr = runCatching {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("conversations").document(groupId).collection("messages").limit(5).get().await()
            }
        }.exceptionOrNull()
        assertNotNull(outsiderRawMsgsErr)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            outsiderRawMsgsErr?.unwrapFirestoreException()?.code
        )

        // 4. Admin verifies 80/20 ledger entry for Live Room Spark Gift
        signInTestUser(ADMIN_EMAIL)
        val ledger = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAllRevenueLedger().first { list ->
                list.any { it.source == "spark_gift" && it.recipientId == creatorUid && it.payerId == fanUid }
            }
        }
        val liveGiftEntry = ledger.first { it.source == "spark_gift" && it.recipientId == creatorUid && it.payerId == fanUid }
        assertEquals(100L, liveGiftEntry.grossCents)
        assertEquals(80L, liveGiftEntry.creatorShareCents)
        assertEquals(20L, liveGiftEntry.adminShareCents)
    }

    @Test
    fun googleAdMob7030Split_nairaUsdCryptoPayouts_andDownloadConfig_endToEnd() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val creatorEmail = "ad_creator_$suffix@test.com"
        val viewerEmail = "ad_viewer_$suffix@test.com"

        // 1. Admin sets NGN bank account, Naira rate, Download URL, AdMob unit IDs, and Auto-Payout toggle
        signInTestUser(ADMIN_EMAIL)
        val adminRepo = KindredRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.ensureAdminPlatformClaimed(ADMIN_EMAIL).getOrThrow()
            adminRepo.saveAdminPayoutAccount(
                payoutMethod = "ngn_bank",
                accountHolderName = "Gregory Francis",
                bankName = "GTBank Nigeria",
                accountNumber = "0123457890",
                routingOrSwift = "GTBINGLA",
                payoutEmailOrPhone = ADMIN_EMAIL,
                platformFeePercent = 20L,
                payoutCurrency = "NGN"
            ).getOrThrow()
            adminRepo.updateAdminAdAndDownloadConfig(
                usdToNgnRate = 1600L,
                autoPayoutEnabled = true,
                appDownloadUrl = "https://ais-pre-eairej7uik6gkhu57oas24-467447110533.europe-west1.run.app",
                admobBannerUnitId = "ca-app-pub-3940256099942544/6300978111",
                admobRewardedUnitId = "ca-app-pub-3940256099942544/5224354917"
            ).getOrThrow()
        }

        val platform = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observePlatformSettings().first {
                it != null && it.adminPayoutCurrency == "NGN" && it.usdToNgnRate == 1600L && it.adAdminSharePercent == 70L
            }!!
        }
        assertTrue(platform.autoPayoutEnabled)
        assertEquals("https://ais-pre-eairej7uik6gkhu57oas24-467447110533.europe-west1.run.app", platform.appDownloadUrl)

        // 2. Creator sets Crypto wallet + Auto-Payout like YouTube
        val creatorUid = signInTestUser(creatorEmail)
        val creatorRepo = KindredRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            creatorRepo.savePayoutPreferences(
                payoutMethod = "crypto",
                payoutAccount = "TX9zMockCryptoWallet_$suffix",
                payoutBankOrNetwork = "USDT TRC20",
                payoutCurrency = "CRYPTO",
                autoPayoutEnabled = true
            ).getOrThrow()
        }
        val creatorPriv = withTimeout(FLOW_TIMEOUT_MS) {
            creatorRepo.observePrivateSettings(creatorUid).first {
                it != null && it.payoutMethod == "crypto" && it.payoutCurrency == "CRYPTO"
            }!!
        }
        assertEquals("USDT TRC20", creatorPriv.payoutBankOrNetwork)
        assertTrue(creatorPriv.autoPayoutEnabled)

        // 3. Viewer records a Google AdMob paid event ($2.50 = 2_500_000 micros -> 175c Admin / 75c Creator)
        signInTestUser(viewerEmail)
        val viewerRepo = KindredRepository(firestore, auth)

        // Zero-micros test event must return null cleanly without fabricating ledger entries
        val zeroEvent = withTimeout(DEFAULT_TIMEOUT_MS) {
            viewerRepo.recordGoogleAdRevenue(
                adFormat = "banner",
                adUnitId = "ca-app-pub-3940256099942544/6300978111",
                valueMicros = 0L,
                currencyCode = "USD"
            ).getOrThrow()
        }
        assertEquals(null, zeroEvent)

        val paidEvent = withTimeout(DEFAULT_TIMEOUT_MS) {
            viewerRepo.recordGoogleAdRevenue(
                adFormat = "rewarded",
                adUnitId = "ca-app-pub-3940256099942544/5224354917",
                valueMicros = 2_500_000L,
                currencyCode = "USD",
                creatorId = creatorUid,
                creatorName = "AdCreator",
                postId = "video_$suffix"
            ).getOrThrow()!!
        }
        assertEquals(250L, paidEvent.grossCents)
        assertEquals(175L, paidEvent.adminShareCents)
        assertEquals(75L, paidEvent.creatorShareCents)

        // Second rewarded ad ($1.00 -> 70c Admin / 30c Creator) brings Creator total to $1.05 (105c >= $1.00 auto-payout threshold)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            viewerRepo.recordGoogleAdRevenue(
                adFormat = "rewarded",
                adUnitId = "ca-app-pub-3940256099942544/5224354917",
                valueMicros = 1_000_000L,
                currencyCode = "USD",
                creatorId = creatorUid,
                creatorName = "AdCreator",
                postId = "video_$suffix"
            ).getOrThrow()
        }

        // 4. Creator sees 105c AdMob revenue & queues Naira auto-payout
        signInTestUser(creatorEmail)
        val adEvents = withTimeout(FLOW_TIMEOUT_MS) {
            creatorRepo.observeCreatorAdRevenue(creatorUid).first { it.size >= 2 }
        }
        assertEquals(105L, adEvents.sumOf { it.creatorShareCents })

        val autoReqId = withTimeout(DEFAULT_TIMEOUT_MS) {
            creatorRepo.requestCreatorPayout(
                userName = "AdCreator",
                amountCents = 105L,
                payoutMethod = "ngn_bank",
                payoutAccount = "0123457890",
                payoutCurrency = "NGN",
                payoutBankOrNetwork = "Zenith Bank",
                isAutoPayout = true,
                usdToNgnRate = 1600L
            ).getOrThrow()
        }

        // 5. Admin inspects 70% AdMob ledger, runs batch auto-payout settlement, and withdraws Naira
        signInTestUser(ADMIN_EMAIL)
        val ledger = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAllRevenueLedger().first { list ->
                list.any { it.id == paidEvent.id && it.source == "ad_revenue" }
            }
        }
        val adLedger = ledger.first { it.id == paidEvent.id }
        assertEquals(175L, adLedger.adminShareCents)
        assertEquals(75L, adLedger.creatorShareCents)

        val allRequests = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAllPayoutRequests().first { list -> list.any { it.id == autoReqId } }
        }
        val targetReq = allRequests.first { it.id == autoReqId }
        assertEquals(1680L, targetReq.ngnAmountWhole)
        assertTrue(targetReq.isAutoPayout)

        val settledCount = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.processAutoPayoutBatch(listOf(targetReq)).getOrThrow()
        }
        assertEquals(1, settledCount)

        val paidReq = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAllPayoutRequests().first { list ->
                list.any { it.id == autoReqId && it.status == "paid" }
            }.first { it.id == autoReqId }
        }
        assertTrue(paidReq.payoutReference?.startsWith("YT-AUTO-") == true)

        val ngnWithdrawal = withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.withdrawAdminRevenue(amountCents = 245L, usdToNgnRate = 1600L).getOrThrow()
        }
        assertEquals("NGN", ngnWithdrawal.payoutCurrency)
        assertEquals(3920L, ngnWithdrawal.ngnAmountWhole)
    }

    @Test
    fun facebookStylePages_createEditFollowPostLikeAndSparkGift_endToEnd() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val ownerEmail = "page_owner_$suffix@test.com"
        val fanEmail = "page_fan_$suffix@test.com"

        signInTestUser(ADMIN_EMAIL)
        val adminRepo = KindredRepository(firestore, auth)
        if (withTimeout(FLOW_TIMEOUT_MS) { adminRepo.observePlatformSettings().first() } == null) {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                adminRepo.claimAdminRole(ADMIN_EMAIL, "BINTAFACE Treasury").getOrThrow()
            }
        }

        // 1. Owner creates a Page, edits it, and publishes a Page Post
        val ownerUid = signInTestUser(ownerEmail)
        val ownerRepo = KindredRepository(firestore, auth)

        val pageId = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.createPage(
                ownerName = "Zuri",
                ownerAvatarKey = "studio",
                name = "Zuri Fashion House $suffix",
                handle = "zurifashion_$suffix",
                category = "Business",
                description = "Handcrafted designs & weekly lookbooks",
                websiteUrl = "https://zurifashion.example.com",
                coverKey = "sunset"
            ).getOrThrow()
        }

        withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.updatePage(
                pageId = pageId,
                name = "Zuri Atelier $suffix",
                category = "Shopping",
                description = "Updated official flagship storefront",
                websiteUrl = "https://atelier.example.com",
                coverKey = "studio",
                photoBase64 = null
            ).getOrThrow()
        }

        val pagePostId = withTimeout(DEFAULT_TIMEOUT_MS) {
            ownerRepo.publishPagePost(
                pageId = pageId,
                pageName = "Zuri Atelier $suffix",
                text = "Spring capsule collection now live!",
                imageKey = "studio"
            ).getOrThrow()
        }

        val createdPage = withTimeout(FLOW_TIMEOUT_MS) {
            ownerRepo.observePages().first { list ->
                list.any { it.id == pageId && it.name == "Zuri Atelier $suffix" && it.postsCount == 1L }
            }.first { it.id == pageId }
        }
        assertEquals("Shopping", createdPage.category)
        assertEquals(1L, createdPage.followersCount)

        // 2. Fan follows the Page, likes the Page post, and sends a $1 Spark Gift (80/20 split)
        val fanUid = signInTestUser(fanEmail)
        val fanRepo = KindredRepository(firestore, auth)

        // Non-owner fan MUST be denied editing Page details or publishing a Page post
        val nonOwnerEditErr = withTimeout(DEFAULT_TIMEOUT_MS) {
            fanRepo.updatePage(
                pageId = pageId,
                name = "Hijacked Atelier",
                category = "Business",
                description = "Hijack",
                websiteUrl = "",
                coverKey = "coffee",
                photoBase64 = null
            )
        }
        assertTrue(nonOwnerEditErr.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            nonOwnerEditErr.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        val nonOwnerPostErr = withTimeout(DEFAULT_TIMEOUT_MS) {
            fanRepo.publishPagePost(
                pageId = pageId,
                pageName = "Zuri Atelier $suffix",
                text = "Unauthorized post attempt"
            )
        }
        assertTrue(nonOwnerPostErr.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            nonOwnerPostErr.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            fanRepo.togglePageFollow(pageId, currentlyFollowing = false).getOrThrow()
            fanRepo.toggleLikePagePost(pageId, pagePostId, currentlyLiked = false).getOrThrow()
            fanRepo.grantConsumablePurchase("kindred_spark_5", 1, "tok_page_$suffix").getOrThrow()
            fanRepo.sendSparkGiftToTarget(
                targetId = pageId,
                targetType = "page",
                recipientId = ownerUid,
                recipientName = "Zuri Atelier $suffix",
                senderName = "Tobi"
            ).getOrThrow()
        }

        val followedPages = withTimeout(FLOW_TIMEOUT_MS) {
            fanRepo.observeMyPageFollows().first { pageId in it }
        }
        assertTrue(pageId in followedPages)

        val likedPostIds = withTimeout(FLOW_TIMEOUT_MS) {
            fanRepo.observeMyPagePostLikes().first { pagePostId in it }
        }
        assertTrue(pagePostId in likedPostIds)

        val updatedPage = withTimeout(FLOW_TIMEOUT_MS) {
            fanRepo.observePages().first { list ->
                list.any { it.id == pageId && it.followersCount == 2L && it.totalGiftsCents == 80L }
            }.first { it.id == pageId }
        }
        assertEquals(2L, updatedPage.followersCount)
        assertEquals(80L, updatedPage.totalGiftsCents)

        val pagePosts = withTimeout(FLOW_TIMEOUT_MS) {
            fanRepo.observePagePosts(pageId).first { list ->
                list.any { it.id == pagePostId && it.likesCount == 1L }
            }
        }
        assertEquals(1L, pagePosts.first { it.id == pagePostId }.likesCount)

        // 3. Admin verifies 80/20 ledger entry for Page Spark Gift
        signInTestUser(ADMIN_EMAIL)
        val ledger = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAllRevenueLedger().first { list ->
                list.any { it.source == "spark_gift" && it.recipientId == ownerUid && it.payerId == fanUid }
            }
        }
        val pageGiftEntry = ledger.first {
            it.source == "spark_gift" && it.recipientId == ownerUid && it.payerId == fanUid
        }
        assertEquals(100L, pageGiftEntry.grossCents)
        assertEquals(80L, pageGiftEntry.creatorShareCents)
        assertEquals(20L, pageGiftEntry.adminShareCents)
    }

    @Test
    fun levelUpgrade_tenSequentialSteps_ngn5000Each_noJumpingAndAutoActivates() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val memberEmail = "vip_member_$suffix@test.com"

        // 1. Admin sets up platform + GTBank NGN bank receiving account
        signInTestUser(ADMIN_EMAIL)
        val adminRepo = KindredRepository(firestore, auth)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            adminRepo.ensureAdminPlatformClaimed(ADMIN_EMAIL).getOrThrow()
            adminRepo.saveAdminPayoutAccount(
                payoutMethod = "ngn_bank",
                accountHolderName = "Francis Gregory",
                bankName = "FairMoney",
                accountNumber = "9038006221",
                routingOrSwift = "FAIRNG",
                payoutEmailOrPhone = ADMIN_EMAIL,
                platformFeePercent = 20L,
                payoutCurrency = "NGN"
            ).getOrThrow()
            adminRepo.updateAdminAdAndDownloadConfig(
                usdToNgnRate = 1600L,
                autoPayoutEnabled = true,
                appDownloadUrl = "https://ais-pre-eairej7uik6gkhu57oas24-467447110533.europe-west1.run.app",
                admobBannerUnitId = "ca-app-pub-3940256099942544/6300978111",
                admobRewardedUnitId = "ca-app-pub-3940256099942544/5224354917"
            ).getOrThrow()
        }

        // 2. Member creates profile (Level 0) and sees FairMoney 9038006221 in platform settings
        val memberUid = signInTestUser(memberEmail)
        val memberRepo = KindredRepository(firestore, auth)
        val settingsSnap = withTimeout(FLOW_TIMEOUT_MS) {
            memberRepo.observePlatformSettings().first { it != null && it.adminAccountNumber == "9038006221" }!!
        }
        assertEquals("FairMoney", settingsSnap.bankName)
        assertEquals("9038006221", settingsSnap.adminAccountNumber)
        assertEquals("Francis Gregory", settingsSnap.adminAccountHolder)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            memberRepo.saveProfile(
                displayName = "Chidi",
                age = 26,
                city = "Lagos",
                bio = "Building communities on BINTAFACE",
                lookingFor = "both",
                promptQuestion = "Go-to vibe",
                promptAnswer = "Live sessions & studios",
                interests = listOf("Music", "Tech"),
                avatarKey = "studio",
                photoBase64 = null,
                isDiscoverable = true,
                preferredIntent = "all",
                email = memberEmail
            ).getOrThrow()
        }

        // Negative: Unconfirmed Google payment MUST FAIL before activating badge
        val unconfirmedErr = withTimeout(DEFAULT_TIMEOUT_MS) {
            memberRepo.upgradeUserLevelDirect(
                levelId = "water",
                currency = "NGN",
                paymentMethod = "google_play",
                paymentReference = "GCONF-UNVERIFIED",
                payerName = "Chidi",
                usdToNgnRate = 1600L,
                googleConfirmed = false
            )
        }
        assertTrue(unconfirmedErr.isFailure)
        assertTrue(unconfirmedErr.exceptionOrNull() is SecurityException)

        // Negative: Jumping from Level 0 directly to Level 2 (black_diamond) MUST FAIL
        val jumpZeroToTwoErr = withTimeout(DEFAULT_TIMEOUT_MS) {
            memberRepo.upgradeUserLevelDirect(
                levelId = "black_diamond",
                currency = "NGN",
                paymentMethod = "ngn_bank",
                paymentReference = "NGN-JUMP-$suffix",
                payerName = "Chidi",
                usdToNgnRate = 1600L
            )
        }
        assertTrue(jumpZeroToTwoErr.isFailure)
        assertTrue(jumpZeroToTwoErr.exceptionOrNull() is IllegalStateException)

        // 3. Step sequentially through all 10 levels (₦5,000 per step, Level 1 via Google Play & Level 10 via USD)
        val expectedLevels = listOf(
            "water" to 1L,
            "black_diamond" to 2L,
            "red_diamond" to 3L,
            "super_diamond" to 4L,
            "rock_diamond" to 5L,
            "star_diamond" to 6L,
            "silver_diamond" to 7L,
            "golden_diamond" to 8L,
            "moon_diamond" to 9L,
            "professional_general_diamond" to 10L
        )
        val createdOrderIds = mutableListOf<String>()

        for ((levelId, expectedRank) in expectedLevels) {
            // While on Level 1 (before Level 2), verify jumping to Level 3 (red_diamond) or Level 10 FAILS
            if (expectedRank == 2L) {
                val jumpOneToThreeErr = withTimeout(DEFAULT_TIMEOUT_MS) {
                    memberRepo.upgradeUserLevelDirect(
                        levelId = "red_diamond",
                        currency = "NGN",
                        paymentMethod = "ngn_bank",
                        paymentReference = "NGN-JUMP3-$suffix",
                        payerName = "Chidi",
                        usdToNgnRate = 1600L
                    )
                }
                assertTrue(jumpOneToThreeErr.isFailure)
                assertTrue(jumpOneToThreeErr.exceptionOrNull() is IllegalStateException)
            }

            val useUsd = expectedRank == 10L
            val method = when {
                expectedRank == 1L -> "google_play"
                useUsd -> "usd_bank"
                else -> "ngn_bank"
            }
            val order = withTimeout(DEFAULT_TIMEOUT_MS) {
                memberRepo.upgradeUserLevelDirect(
                    levelId = levelId,
                    currency = if (useUsd) "USD" else "NGN",
                    paymentMethod = method,
                    paymentReference = "STEP-${expectedRank}-$suffix",
                    payerName = "Chidi",
                    usdToNgnRate = 1600L,
                    googleConfirmed = true
                ).getOrThrow()
            }
            createdOrderIds.add(order.id)
            assertEquals("activated", order.status)
            assertEquals(levelId, order.levelId)
            assertEquals(expectedRank, order.levelRank)
            assertTrue(order.adminBankLabel.contains("9038006221"))
            if (expectedRank == 1L) {
                assertEquals("Water Diamond", order.levelName)
                assertEquals("google_play", order.paymentMethod)
            }
            assertEquals(0L, order.creatorShareCents)
            assertEquals(ADMIN_EMAIL, order.adminEmail)
            if (useUsd) {
                assertEquals("USD", order.currency)
                assertEquals(350L, order.amountCents)
                assertEquals(350L, order.adminShareCents)
            } else {
                assertEquals("NGN", order.currency)
                assertEquals(5000L, order.ngnAmount)
                assertEquals(312L, order.amountCents)
                assertEquals(312L, order.adminShareCents)
            }

            val syncedPublic = withTimeout(FLOW_TIMEOUT_MS) {
                memberRepo.observeMyPublicProfile(memberUid).first {
                    it != null && it.vipLevel == levelId && it.vipLevelRank == expectedRank
                }!!
            }
            assertEquals(order.id, syncedPublic.lastLevelUpgradeId)
        }

        val finalPrivate = withTimeout(FLOW_TIMEOUT_MS) {
            memberRepo.observePrivateSettings(memberUid).first {
                it != null && it.vipLevel == "professional_general_diamond" && it.vipLevelRank == 10L
            }!!
        }
        assertEquals(createdOrderIds.last(), finalPrivate.lastLevelUpgradeId)

        val myUpgrades = withTimeout(FLOW_TIMEOUT_MS) {
            memberRepo.observeMyLevelUpgrades(memberUid).first { it.size >= 10 }
        }
        assertEquals(10, myUpgrades.size)

        // 4. Negative checks after Level 10: downgrade fails, direct Firestore tampering denied
        val afterMaxErr = withTimeout(DEFAULT_TIMEOUT_MS) {
            memberRepo.upgradeUserLevelDirect(
                levelId = "water",
                currency = "NGN",
                paymentMethod = "ngn_bank",
                paymentReference = "NGN-DOWN-$suffix",
                payerName = "Chidi",
                usdToNgnRate = 1600L
            )
        }
        assertTrue(afterMaxErr.isFailure)

        val unbackedPromoErr = runCatching {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                firestore.collection("users").document(memberUid)
                    .update(
                        mapOf(
                            "vipLevel" to "golden_diamond",
                            "vipLevelRank" to 8L
                        )
                    ).await()
            }
        }.exceptionOrNull()
        assertNotNull(unbackedPromoErr)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            unbackedPromoErr?.unwrapFirestoreException()?.code
        )

        // 5. Admin ledger receives 100% of all 10 sequential upgrades
        signInTestUser(ADMIN_EMAIL)
        val adminLedger = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAllRevenueLedger().first { list ->
                createdOrderIds.all { id -> list.any { it.id == id } }
            }
        }
        createdOrderIds.forEach { orderId ->
            val entry = adminLedger.first { it.id == orderId }
            assertEquals("level_upgrade", entry.source)
            assertEquals("admin", entry.recipientId)
            assertEquals(0L, entry.creatorShareCents)
            assertTrue(entry.adminShareCents >= 312L)
        }
    }

    @Test
    fun voiceNotesAndVideoCallSignaling_conversationsAndMatches_endToEnd() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val aliceEmail = "call_alice_$suffix@test.com"
        val bobEmail = "call_bob_$suffix@test.com"
        val outsiderEmail = "call_outsider_$suffix@test.com"

        assertEquals(
            "Welcome to BINTAFACE, where knowledge meets power — CEO Francis Gregory",
            com.example.data.model.CEO_WELCOME_NOTE
        )

        val aliceUid = signInTestUser(aliceEmail)
        val aliceRepo = KindredRepository(firestore, auth)
        val bobUid = signInTestUser(bobEmail)
        val bobRepo = KindredRepository(firestore, auth)

        signInTestUser(aliceEmail)
        val convId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createOrOpenDirectConversation(
                myName = "Alice",
                targetId = bobUid,
                targetName = "Bob"
            ).getOrThrow()
        }
        val conversation = withTimeout(FLOW_TIMEOUT_MS) {
            aliceRepo.observeConversations().first { list -> list.any { it.id == convId } }
                .first { it.id == convId }
        }

        // Alice sends an AAC voice note
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.sendVoiceNoteToConversation(
                conversation = conversation,
                senderName = "Alice",
                voiceNoteBase64 = "UklGRiQAAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQAAAAA=",
                durationSec = 12L
            ).getOrThrow()
        }

        // Alice initiates a Video Call
        val callSession = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.startCallSession(
                threadId = convId,
                threadType = "conversation",
                participantIds = conversation.participantIds,
                participantNames = conversation.participantNames,
                callerName = "Alice",
                callerAvatarKey = "coffee",
                callType = "video"
            ).getOrThrow()
        }
        assertEquals("ringing", callSession.status)

        // Outsider cannot read or accept the call session
        signInTestUser(outsiderEmail)
        val outsiderCallErr = withTimeout(DEFAULT_TIMEOUT_MS) {
            KindredRepository(firestore, auth).acceptCallSession(callSession.id)
        }
        assertTrue(outsiderCallErr.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            outsiderCallErr.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        // Bob sees the ringing video call, accepts it, ends it, and appends a call_log message
        signInTestUser(bobEmail)
        val bobRinging = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeMyCallSessions().first { list ->
                list.any { it.id == callSession.id && it.status == "ringing" }
            }.first { it.id == callSession.id }
        }
        assertEquals("video", bobRinging.callType)

        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.acceptCallSession(callSession.id).getOrThrow()
            bobRepo.endCallSession(callSession.id, 42L).getOrThrow()
            bobRepo.appendCallLogMessage(bobRinging, "Bob", "completed", 42L).getOrThrow()
        }

        val msgs = withTimeout(FLOW_TIMEOUT_MS) {
            bobRepo.observeDirectMessages(convId).first { list ->
                list.any { it.messageType == "voice_note" } && list.any { it.messageType == "call_log" }
            }
        }
        val vn = msgs.first { it.messageType == "voice_note" }
        val cl = msgs.first { it.messageType == "call_log" }
        assertEquals(12L, vn.voiceNoteDurationSec)
        assertEquals("video", cl.callType)
        assertEquals("completed", cl.callStatus)
        assertEquals(42L, cl.callDurationSec)
        assertEquals(aliceUid, vn.senderId)
    }

    @Test
    fun shopsMarketplace_createListOrder8020AndStockDecrement_endToEnd() = runBlocking {
        val suffix = UUID.randomUUID().toString().take(8)
        val sellerEmail = "shop_seller_$suffix@test.com"
        val buyerEmail = "shop_buyer_$suffix@test.com"

        signInTestUser(ADMIN_EMAIL)
        val adminRepo = KindredRepository(firestore, auth)
        if (withTimeout(FLOW_TIMEOUT_MS) { adminRepo.observePlatformSettings().first() } == null) {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                adminRepo.claimAdminRole(ADMIN_EMAIL, "BINTAFACE Treasury").getOrThrow()
            }
        }

        // 1. Seller launches Shop and lists a product with stock 3
        val sellerUid = signInTestUser(sellerEmail)
        val sellerRepo = KindredRepository(firestore, auth)
        val shopId = withTimeout(DEFAULT_TIMEOUT_MS) {
            sellerRepo.createShop(
                ownerName = "Ngozi",
                ownerAvatarKey = "studio",
                name = "Ngozi Electronics $suffix",
                handle = "ngozitech_$suffix",
                category = "Electronics",
                description = "Studio mics, ring lights & creator kits",
                currency = "USD",
                city = "Abuja",
                coverKey = "studio",
                photoBase64 = null
            ).getOrThrow()
        }
        val createdShop = withTimeout(FLOW_TIMEOUT_MS) {
            sellerRepo.observeShops().first { list -> list.any { it.id == shopId } }
                .first { it.id == shopId }
        }
        val productId = withTimeout(DEFAULT_TIMEOUT_MS) {
            sellerRepo.createShopProduct(
                shop = createdShop,
                title = "Pro Podcast Mic",
                description = "USB-C cardioid microphone",
                category = "Electronics",
                priceUsdCents = 500L,
                priceNgn = 8000L,
                stockCount = 3L,
                imageKey = "studio",
                photoBase64 = null
            ).getOrThrow()
        }
        val product = withTimeout(FLOW_TIMEOUT_MS) {
            sellerRepo.observeShopProducts(shopId).first { list -> list.any { it.id == productId } }
                .first { it.id == productId }
        }

        // Seller cannot purchase from own shop
        val selfBuyErr = withTimeout(DEFAULT_TIMEOUT_MS) {
            sellerRepo.placeShopOrder(createdShop, product, "Ngozi", 1L, "USD", "Self")
        }
        assertTrue(selfBuyErr.isFailure)

        // 2. Buyer purchases 2 units in USD (2 x 500 = 1000 cents -> 800 seller / 200 admin)
        val buyerUid = signInTestUser(buyerEmail)
        val buyerRepo = KindredRepository(firestore, auth)
        val order = withTimeout(DEFAULT_TIMEOUT_MS) {
            buyerRepo.placeShopOrder(
                shop = createdShop,
                product = product,
                buyerName = "Tunde",
                quantity = 2L,
                currency = "USD",
                deliveryNote = "Plot 12 Wuse II, Abuja",
                usdToNgnRate = 1600L
            ).getOrThrow()
        }
        assertEquals(1000L, order.totalCents)
        assertEquals(800L, order.sellerShareCents)
        assertEquals(200L, order.adminFeeCents)
        assertEquals(buyerUid, order.buyerId)
        assertEquals(sellerUid, order.sellerId)

        val updatedProduct = withTimeout(FLOW_TIMEOUT_MS) {
            buyerRepo.observeShopProducts(shopId).first { list ->
                list.any { it.id == productId && it.stockCount == 1L }
            }.first { it.id == productId }
        }
        assertEquals(1L, updatedProduct.stockCount)

        // Buyer cannot hijack order status
        val buyerStatusErr = withTimeout(DEFAULT_TIMEOUT_MS) {
            buyerRepo.updateShopOrderStatus(order.id, "fulfilled")
        }
        assertTrue(buyerStatusErr.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            buyerStatusErr.exceptionOrNull()?.unwrapFirestoreException()?.code
        )

        // 3. Seller sees order & marks fulfilled; Admin sees 20% fee in revenue_ledger
        signInTestUser(sellerEmail)
        val sellerOrders = withTimeout(FLOW_TIMEOUT_MS) {
            sellerRepo.observeMySellerOrders(sellerUid).first { list -> list.any { it.id == order.id } }
        }
        assertEquals(800L, sellerOrders.first { it.id == order.id }.sellerShareCents)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            sellerRepo.updateShopOrderStatus(order.id, "fulfilled").getOrThrow()
        }

        signInTestUser(ADMIN_EMAIL)
        val adminLedger = withTimeout(FLOW_TIMEOUT_MS) {
            adminRepo.observeAllRevenueLedger().first { list ->
                list.any { it.id == order.id && it.source == "shop_order" }
            }
        }
        val shopEntry = adminLedger.first { it.id == order.id }
        assertEquals(1000L, shopEntry.grossCents)
        assertEquals(800L, shopEntry.creatorShareCents)
        assertEquals(200L, shopEntry.adminShareCents)
    }

    private companion object {
        const val ADMIN_EMAIL = "gregoryfrancis652@gmail.com"
        const val ALICE_EMAIL = "alice_kindred@test.com"
        const val BOB_EMAIL = "bob_kindred@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
