const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE = "alice_uid";
const BOB = "bob_uid";
const CHARLIE = "charlie_uid";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 1000);
}

function sortedMatchId(a, b) {
  return a < b ? `${a}_${b}` : `${b}_${a}`;
}

function validPublicProfile(uid, name) {
  const ts = pastTimestamp();
  return {
    userId: uid,
    displayName: name,
    age: 26,
    city: "San Francisco",
    bio: "Coffee walks and coastal sunsets.",
    lookingFor: "both",
    promptQuestion: "Ideal Sunday morning",
    promptAnswer: "Oat latte and a farmers market stroll",
    interests: ["Coffee", "Hiking", "Photography"],
    avatarKey: "coffee",
    isDiscoverable: true,
    createdAt: ts,
    updatedAt: ts,
  };
}

function validPost(uid, name) {
  const ts = pastTimestamp();
  return {
    authorId: uid,
    authorName: name,
    authorAvatarKey: "coffee",
    content: "Anyone up for a sunset rooftop hangout this Friday?",
    tag: "Hangout",
    imageKey: "sunset",
    visibility: "community",
    likesCount: 0,
    commentsCount: 0,
    createdAt: ts,
    updatedAt: ts,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated user cannot read or list any collection", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("users").doc(ALICE).get());
  await assertFails(unauth.collection("public_profiles").where("isDiscoverable", "==", true).get());
  await assertFails(unauth.collection("posts").where("visibility", "==", "community").get());
  await assertFails(unauth.collection("matches").where("participantIds", "array-contains", ALICE).get());
});

test("UserPrivate: owner can create/read/update, other users denied (PII isolation)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE).firestore();
  const bobDb = testEnv.authenticatedContext(BOB).firestore();
  const ts = pastTimestamp();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE).set({
      userId: ALICE,
      email: "alice@example.com",
      showInDiscovery: true,
      preferredIntent: "all",
      isPlusMember: false,
      superlikesBalance: 1,
      boostsBalance: 0,
      dailyLikesUsed: 0,
      dailyLikesDate: "2026-11-13",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE).get());
  await assertFails(bobDb.collection("users").doc(ALICE).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE).update({ isPlusMember: true, updatedAt: pastTimestamp() })
  );
  // Negative superlikes balance rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE).update({ superlikesBalance: -1, updatedAt: pastTimestamp() })
  );
  // Ghost field rejection
  await assertFails(
    aliceDb.collection("users").doc(ALICE).update({ isAdmin: true, updatedAt: pastTimestamp() })
  );
});

test("PublicProfile: owner can write, other users can query discoverable profiles, cannot overwrite", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE).firestore();
  const bobDb = testEnv.authenticatedContext(BOB).firestore();

  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE).set(validPublicProfile(ALICE, "Alice"))
  );
  await assertSucceeds(
    bobDb.collection("public_profiles").where("isDiscoverable", "==", true).get()
  );
  await assertFails(bobDb.collection("public_profiles").get());
  await assertFails(
    bobDb.collection("public_profiles").doc(ALICE).update({ bio: "Hacked", updatedAt: pastTimestamp() })
  );
});

test("Posts: create, query community feed, atomic like/unlike, atomic comment", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE).firestore();
  const bobDb = testEnv.authenticatedContext(BOB).firestore();

  const postRef = aliceDb.collection("posts").doc("post_1");
  await assertSucceeds(postRef.set(validPost(ALICE, "Alice")));

  await assertSucceeds(
    bobDb.collection("posts").where("visibility", "==", "community").get()
  );
  await assertFails(
    bobDb.collection("posts").doc("post_spoof").set(validPost(ALICE, "Alice"))
  );
  // Cannot arbitrarily inflate likesCount without post_likes batch
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({ likesCount: 99, updatedAt: pastTimestamp() })
  );

  // Atomic batch like by Bob
  const likeBatch = bobDb.batch();
  likeBatch.set(bobDb.collection("post_likes").doc(`post_1_${BOB}`), {
    userId: BOB,
    postId: "post_1",
    createdAt: pastTimestamp(),
  });
  likeBatch.update(bobDb.collection("posts").doc("post_1"), {
    likesCount: 1,
    updatedAt: pastTimestamp(),
  });
  await assertSucceeds(likeBatch.commit());

  // Atomic batch comment by Bob
  const commentBatch = bobDb.batch();
  commentBatch.set(bobDb.collection("posts").doc("post_1").collection("comments").doc("c_1"), {
    postId: "post_1",
    postVisibility: "community",
    authorId: BOB,
    authorName: "Bob",
    authorAvatarKey: "sunset",
    text: "Count me in for Friday!",
    createdAt: pastTimestamp(),
  });
  commentBatch.update(bobDb.collection("posts").doc("post_1"), {
    commentsCount: 1,
    lastCommentId: "c_1",
    updatedAt: pastTimestamp(),
  });
  await assertSucceeds(commentBatch.commit());

  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").collection("comments").where("postVisibility", "==", "community").get()
  );
});

test("Swipes, Mutual Matches, and Match Chat Isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE).firestore();
  const bobDb = testEnv.authenticatedContext(BOB).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE).firestore();

  await assertSucceeds(aliceDb.collection("public_profiles").doc(ALICE).set(validPublicProfile(ALICE, "Alice")));
  await assertSucceeds(bobDb.collection("public_profiles").doc(BOB).set(validPublicProfile(BOB, "Bob")));

  // Alice likes Bob
  await assertSucceeds(
    aliceDb.collection("swipes").doc(`${ALICE}_${BOB}`).set({
      fromUserId: ALICE,
      toUserId: BOB,
      decision: "like",
      positiveTargetId: BOB,
      fromName: "Alice",
      fromAvatarKey: "coffee",
      createdAt: pastTimestamp(),
    })
  );

  await assertFails(charlieDb.collection("swipes").doc(`${ALICE}_${BOB}`).get());
  await assertSucceeds(
    bobDb.collection("swipes").where("positiveTargetId", "==", BOB).get()
  );

  // Alice cannot force a match before Bob likes Alice back
  const matchId = sortedMatchId(ALICE, BOB);
  await assertFails(
    aliceDb.collection("matches").doc(matchId).set({
      participantIds: [ALICE, BOB],
      user1Id: ALICE,
      user1Name: "Alice",
      user1AvatarKey: "coffee",
      user2Id: BOB,
      user2Name: "Bob",
      user2AvatarKey: "sunset",
      lastMessage: "Hey!",
      lastMessageAt: pastTimestamp(),
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  // Bob likes Alice back + creates Match in a batch
  const ts = pastTimestamp();
  const matchBatch = bobDb.batch();
  matchBatch.set(bobDb.collection("swipes").doc(`${BOB}_${ALICE}`), {
    fromUserId: BOB,
    toUserId: ALICE,
    decision: "like",
    positiveTargetId: ALICE,
    fromName: "Bob",
    fromAvatarKey: "sunset",
    createdAt: ts,
  });
  matchBatch.set(bobDb.collection("matches").doc(matchId), {
    participantIds: [ALICE, BOB],
    user1Id: ALICE,
    user1Name: "Alice",
    user1AvatarKey: "coffee",
    user2Id: BOB,
    user2Name: "Bob",
    user2AvatarKey: "sunset",
    lastMessage: "It's a match! Say hello.",
    lastMessageAt: ts,
    createdAt: ts,
    updatedAt: ts,
  });
  await assertSucceeds(matchBatch.commit());

  // Alice and Bob can send & list messages; Charlie cannot
  await assertSucceeds(
    aliceDb.collection("matches").doc(matchId).collection("messages").doc("msg_1").set({
      senderId: ALICE,
      senderName: "Alice",
      text: "Hey Bob! Coffee this weekend?",
      participantIds: [ALICE, BOB],
      createdAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    bobDb.collection("matches").doc(matchId).collection("messages").where("participantIds", "array-contains", BOB).get()
  );
  await assertFails(
    charlieDb.collection("matches").doc(matchId).collection("messages").doc("msg_1").get()
  );
  await assertFails(
    charlieDb.collection("matches").doc(matchId).collection("messages").get()
  );
  await assertFails(
    charlieDb.collection("matches").doc(matchId).collection("messages").where("participantIds", "array-contains", ALICE).get()
  );
  await assertFails(
    charlieDb.collection("matches").doc(matchId).collection("messages").doc("msg_bad").set({
      senderId: CHARLIE,
      senderName: "Charlie",
      text: "Intruder",
      participantIds: [ALICE, BOB],
      createdAt: pastTimestamp(),
    })
  );
});

test("Two-sided Monetization: Exclusive Admin (gregoryfrancis652@gmail.com), Private Bank Account, Revenue Ledger & Withdrawals", async () => {
  const adminDb = testEnv.authenticatedContext(ALICE, {
    email: "gregoryfrancis652@gmail.com",
    email_verified: true,
  }).firestore();
  const creatorDb = testEnv.authenticatedContext(BOB, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const supporterDb = testEnv.authenticatedContext(CHARLIE, {
    email: "charlie@example.com",
    email_verified: true,
  }).firestore();
  const ts = pastTimestamp();

  // Non-admin cannot claim or hijack singleton platform settings
  await assertFails(
    supporterDb.collection("app_settings").doc("platform").set({
      adminUid: CHARLIE,
      adminEmail: "charlie@example.com",
      platformFeePercent: 10,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    supporterDb.collection("app_settings").doc("platform").set({
      adminUid: CHARLIE,
      adminEmail: "gregoryfrancis652@gmail.com",
      platformFeePercent: 10,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Only gregoryfrancis652@gmail.com can configure platform settings and private bank account number
  await assertSucceeds(
    adminDb.collection("app_settings").doc("platform").set({
      adminUid: ALICE,
      adminEmail: "gregoryfrancis652@gmail.com",
      platformFeePercent: 20,
      payoutAccountLabel: "Chase Bank ••••6789",
      payoutMethod: "bank_transfer",
      bankName: "Chase Bank",
      maskedAccountNumber: "••••6789",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    adminDb.collection("admin_payout_accounts").doc("primary").set({
      adminUid: ALICE,
      adminEmail: "gregoryfrancis652@gmail.com",
      payoutMethod: "bank_transfer",
      accountHolderName: "Gregory Francis",
      bankName: "Chase Bank",
      accountNumber: "12345678906789",
      routingOrSwift: "021000021",
      payoutEmailOrPhone: "gregoryfrancis652@gmail.com",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Regular users can read non-PII platform summary, but CANNOT read or write Admin's private bank account number
  await assertSucceeds(creatorDb.collection("app_settings").doc("platform").get());
  await assertFails(creatorDb.collection("admin_payout_accounts").doc("primary").get());
  await assertFails(supporterDb.collection("admin_payout_accounts").get());
  await assertFails(
    supporterDb.collection("admin_payout_accounts").doc("primary").set({
      adminUid: CHARLIE,
      adminEmail: "gregoryfrancis652@gmail.com",
      payoutMethod: "bank_transfer",
      accountHolderName: "Intruder",
      bankName: "Fake Bank",
      accountNumber: "99999999",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Bob creates a community post; Charlie starts with 2 Spark Coins
  await assertSucceeds(creatorDb.collection("posts").doc("post_creator").set(validPost(BOB, "Bob")));
  await assertSucceeds(
    supporterDb.collection("users").doc(CHARLIE).set({
      userId: CHARLIE,
      showInDiscovery: true,
      preferredIntent: "all",
      sparksBalance: 2,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Forged Spark Gift without deducting sparksBalance fails
  await assertFails(
    supporterDb.collection("spark_gifts").doc("gift_forged").set({
      senderId: CHARLIE,
      senderName: "Charlie",
      recipientId: BOB,
      recipientName: "Bob",
      postId: "post_creator",
      sparksCount: 1,
      grossCents: 100,
      creatorShareCents: 80,
      adminFeeCents: 20,
      createdAt: pastTimestamp(),
    })
  );

  // Atomic Spark Gift: deducts 1 Spark Coin, credits 80c user + 20c admin, updates post & ledger
  const giftBatch = supporterDb.batch();
  giftBatch.update(supporterDb.collection("users").doc(CHARLIE), {
    sparksBalance: 1,
    updatedAt: pastTimestamp(),
  });
  giftBatch.set(supporterDb.collection("spark_gifts").doc("gift_1"), {
    senderId: CHARLIE,
    senderName: "Charlie",
    recipientId: BOB,
    recipientName: "Bob",
    postId: "post_creator",
    sparksCount: 1,
    grossCents: 100,
    creatorShareCents: 80,
    adminFeeCents: 20,
    createdAt: pastTimestamp(),
  });
  giftBatch.update(supporterDb.collection("posts").doc("post_creator"), {
    sparksCount: 1,
    sparksEarningsCents: 80,
    lastSparkGiftId: "gift_1",
    updatedAt: pastTimestamp(),
  });
  giftBatch.set(supporterDb.collection("revenue_ledger").doc("gift_1"), {
    source: "spark_gift",
    productId: "spark_gift_1",
    payerId: CHARLIE,
    recipientId: BOB,
    grossCents: 100,
    adminShareCents: 20,
    creatorShareCents: 80,
    referenceToken: "gift_1",
    createdAt: pastTimestamp(),
  });
  await assertSucceeds(giftBatch.commit());

  // Bob requests payout; Charlie cannot read or approve it; Gregory (Admin) can list, settle & withdraw admin revenue
  await assertSucceeds(
    creatorDb.collection("payout_requests").doc("payout_1").set({
      userId: BOB,
      userName: "Bob",
      amountCents: 800,
      payoutMethod: "paypal",
      payoutAccount: "bob@paypal.com",
      status: "pending",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(supporterDb.collection("payout_requests").doc("payout_1").get());
  await assertFails(
    creatorDb.collection("payout_requests").doc("payout_1").update({
      status: "paid",
      updatedAt: pastTimestamp(),
    })
  );
  await assertSucceeds(adminDb.collection("revenue_ledger").get());
  await assertSucceeds(
    adminDb.collection("payout_requests").doc("payout_1").update({
      status: "paid",
      payoutReference: "PAYPAL-99881",
      adminNote: "Settled",
      updatedAt: pastTimestamp(),
    })
  );

  // Admin withdraws accumulated revenue; non-admin cannot create or read admin withdrawals
  await assertSucceeds(
    adminDb.collection("admin_payouts").doc("adm_w_1").set({
      adminUid: ALICE,
      adminEmail: "gregoryfrancis652@gmail.com",
      amountCents: 500,
      payoutMethod: "bank_transfer",
      accountHolderName: "Gregory Francis",
      bankName: "Chase Bank",
      maskedAccountNumber: "••••6789",
      status: "completed",
      referenceCode: "BINT-ADM-TEST1",
      createdAt: pastTimestamp(),
    })
  );
  await assertFails(creatorDb.collection("admin_payouts").doc("adm_w_1").get());
  await assertFails(supporterDb.collection("admin_payouts").get());
});

test("BINTAFACE All-in-One: Live Rooms, Broadcast Channels, Group Chats, Follows & Multi-target Spark Gifts", async () => {
  const hostDb = testEnv.authenticatedContext(ALICE).firestore();
  const viewerDb = testEnv.authenticatedContext(BOB).firestore();
  const outsiderDb = testEnv.authenticatedContext(CHARLIE).firestore();
  const ts = pastTimestamp();

  // Supporter starts with 3 Spark Coins
  await assertSucceeds(
    viewerDb.collection("users").doc(BOB).set({
      userId: BOB,
      showInDiscovery: true,
      preferredIntent: "all",
      sparksBalance: 3,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // 1. Twitter/X Follow + Repost
  await assertSucceeds(
    viewerDb.collection("follows").doc(`${BOB}_${ALICE}`).set({
      followerId: BOB,
      followedId: ALICE,
      followerName: "Bob",
      createdAt: ts,
    })
  );
  await assertSucceeds(hostDb.collection("posts").doc("reel_1").set({
    authorId: ALICE,
    authorName: "Alice",
    authorAvatarKey: "studio",
    mediaType: "reel",
    title: "BINTAFACE Studio Jam",
    durationLabel: "0:32",
    audioTrack: "Original Sound • Alice",
    content: "Behind the scenes beat drop!",
    tag: "Reel",
    imageKey: "studio",
    visibility: "community",
    likesCount: 0,
    commentsCount: 0,
    repostsCount: 0,
    viewsCount: 0,
    createdAt: ts,
    updatedAt: ts,
  }));
  const repostBatch = viewerDb.batch();
  repostBatch.set(viewerDb.collection("post_reposts").doc(`reel_1_${BOB}`), {
    userId: BOB,
    postId: "reel_1",
    createdAt: pastTimestamp(),
  });
  repostBatch.update(viewerDb.collection("posts").doc("reel_1"), {
    repostsCount: 1,
    updatedAt: pastTimestamp(),
  });
  await assertSucceeds(repostBatch.commit());

  // 2. BIGO Live Room + Live Chat + Live 80/20 Spark Gift
  await assertSucceeds(
    hostDb.collection("live_rooms").doc("room_1").set({
      hostId: ALICE,
      hostName: "Alice",
      hostAvatarKey: "studio",
      title: "Midnight Acoustic Live",
      category: "Music",
      coverKey: "studio",
      visibility: "public",
      status: "live",
      viewerCount: 1,
      likesCount: 0,
      sparksCount: 0,
      totalGiftsCents: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    viewerDb.collection("live_rooms").doc("room_1").collection("messages").doc("lm_1").set({
      roomId: "room_1",
      roomVisibility: "public",
      senderId: BOB,
      senderName: "Bob",
      senderAvatarKey: "coffee",
      text: "Sounding amazing!",
      isGiftNotice: false,
      createdAt: pastTimestamp(),
    })
  );
  const liveGiftBatch = viewerDb.batch();
  liveGiftBatch.update(viewerDb.collection("users").doc(BOB), {
    sparksBalance: 2,
    updatedAt: pastTimestamp(),
  });
  liveGiftBatch.set(viewerDb.collection("spark_gifts").doc("gift_live_1"), {
    senderId: BOB,
    senderName: "Bob",
    recipientId: ALICE,
    recipientName: "Alice",
    postId: "room_1",
    targetType: "live",
    sparksCount: 1,
    grossCents: 100,
    creatorShareCents: 80,
    adminFeeCents: 20,
    createdAt: pastTimestamp(),
  });
  liveGiftBatch.update(viewerDb.collection("live_rooms").doc("room_1"), {
    sparksCount: 1,
    totalGiftsCents: 80,
    lastSparkGiftId: "gift_live_1",
    updatedAt: pastTimestamp(),
  });
  liveGiftBatch.set(viewerDb.collection("revenue_ledger").doc("gift_live_1"), {
    source: "spark_gift",
    productId: "spark_gift_1",
    payerId: BOB,
    recipientId: ALICE,
    grossCents: 100,
    adminShareCents: 20,
    creatorShareCents: 80,
    referenceToken: "gift_live_1",
    createdAt: pastTimestamp(),
  });
  await assertSucceeds(liveGiftBatch.commit());

  // 3. Telegram Broadcast Channel + Subscribe + Owner-only Broadcast
  await assertSucceeds(
    hostDb.collection("channels").doc("chan_1").set({
      ownerId: ALICE,
      ownerName: "Alice",
      ownerAvatarKey: "studio",
      title: "Alice Studio Updates",
      handle: "@alicestudio",
      description: "Daily drops and tour news",
      category: "Music",
      visibility: "public",
      subscriberCount: 1,
      totalGiftsCents: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    hostDb.collection("channels").doc("chan_1").collection("broadcasts").doc("b_1").set({
      channelId: "chan_1",
      channelVisibility: "public",
      ownerId: ALICE,
      ownerName: "Alice",
      text: "New EP teaser tonight at 8PM!",
      createdAt: pastTimestamp(),
    })
  );
  // Non-owner cannot broadcast to Alice's channel
  await assertFails(
    viewerDb.collection("channels").doc("chan_1").collection("broadcasts").doc("b_forged").set({
      channelId: "chan_1",
      channelVisibility: "public",
      ownerId: BOB,
      ownerName: "Bob",
      text: "Hijack",
      createdAt: pastTimestamp(),
    })
  );
  const subBatch = viewerDb.batch();
  subBatch.set(viewerDb.collection("channel_subscriptions").doc(`chan_1_${BOB}`), {
    channelId: "chan_1",
    userId: BOB,
    createdAt: pastTimestamp(),
  });
  subBatch.update(viewerDb.collection("channels").doc("chan_1"), {
    subscriberCount: 2,
    updatedAt: pastTimestamp(),
  });
  await assertSucceeds(subBatch.commit());

  // 4. WhatsApp Direct/Group Conversations Isolation
  await assertSucceeds(
    hostDb.collection("conversations").doc("conv_1").set({
      type: "group",
      title: "Weekend Creators",
      participantIds: [ALICE, BOB],
      participantNames: { [ALICE]: "Alice", [BOB]: "Bob" },
      createdBy: ALICE,
      lastMessage: "Welcome!",
      lastMessageAt: ts,
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    viewerDb.collection("conversations").doc("conv_1").collection("messages").doc("dm_1").set({
      senderId: BOB,
      senderName: "Bob",
      text: "Glad to be here!",
      participantIds: [ALICE, BOB],
      createdAt: pastTimestamp(),
    })
  );
  await assertFails(outsiderDb.collection("conversations").doc("conv_1").get());
  await assertFails(
    outsiderDb.collection("conversations").doc("conv_1").collection("messages").where("participantIds", "array-contains", ALICE).get()
  );
});

test("Google AdMob 70/30 Revenue Split, USD/NGN/Crypto Payouts & BINTAFACE Download Config", async () => {
  const adminDb = testEnv.authenticatedContext(ALICE, {
    email: "gregoryfrancis652@gmail.com",
    email_verified: true,
  }).firestore();
  const creatorDb = testEnv.authenticatedContext(BOB, {
    email: "creator@example.com",
    email_verified: true,
  }).firestore();
  const viewerDb = testEnv.authenticatedContext(CHARLIE, {
    email: "viewer@example.com",
    email_verified: true,
  }).firestore();
  const ts = pastTimestamp();

  // 1. Admin configures NGN bank account + BINTAFACE download URL, rate, and AdMob units
  await assertSucceeds(
    adminDb.collection("app_settings").doc("platform").set({
      adminUid: ALICE,
      adminEmail: "gregoryfrancis652@gmail.com",
      platformFeePercent: 20,
      payoutAccountLabel: "GTBank (NGN) ••••7890",
      payoutMethod: "ngn_bank",
      bankName: "GTBank Nigeria",
      maskedAccountNumber: "••••7890",
      adminPayoutCurrency: "NGN",
      usdToNgnRate: 1600,
      adAdminSharePercent: 70,
      autoPayoutEnabled: true,
      appDownloadUrl: "https://ais-pre-eairej7uik6gkhu57oas24-467447110533.europe-west1.run.app",
      admobBannerUnitId: "ca-app-pub-3940256099942544/6300978111",
      admobRewardedUnitId: "ca-app-pub-3940256099942544/5224354917",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    adminDb.collection("admin_payout_accounts").doc("primary").set({
      adminUid: ALICE,
      adminEmail: "gregoryfrancis652@gmail.com",
      payoutMethod: "ngn_bank",
      payoutCurrency: "NGN",
      accountHolderName: "Gregory Francis",
      bankName: "GTBank Nigeria",
      accountNumber: "0123457890",
      routingOrSwift: "GTBINGLA",
      payoutEmailOrPhone: "gregoryfrancis652@gmail.com",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Non-admin cannot change adAdminSharePercent, download URL, or rate
  await assertFails(
    viewerDb.collection("app_settings").doc("platform").update({
      appDownloadUrl: "https://evil.example.com/apk",
      updatedAt: pastTimestamp(),
    })
  );

  // 2. Viewer watches Rewarded Ad on Bob's post ($1.15 = 115 cents -> 34c creator / 81c admin)
  const adBatch = viewerDb.batch();
  adBatch.set(viewerDb.collection("ad_revenue_events").doc("ad_ev_1"), {
    viewerId: CHARLIE,
    creatorId: BOB,
    creatorName: "Bob",
    postId: "post_1",
    adFormat: "rewarded",
    adUnitId: "ca-app-pub-3940256099942544/5224354917",
    valueMicros: 1150000,
    currencyCode: "USD",
    grossCents: 115,
    adminShareCents: 81,
    creatorShareCents: 34,
    createdAt: ts,
  });
  adBatch.set(viewerDb.collection("revenue_ledger").doc("ad_ev_1"), {
    source: "ad_revenue",
    productId: "admob_rewarded",
    payerId: CHARLIE,
    recipientId: BOB,
    grossCents: 115,
    adminShareCents: 81,
    creatorShareCents: 34,
    referenceToken: "ca-app-pub-3940256099942544/5224354917",
    createdAt: ts,
  });
  await assertSucceeds(adBatch.commit());

  // General banner ad with no creatorId credits 100% Admin / 0% Creator
  const bannerBatch = viewerDb.batch();
  bannerBatch.set(viewerDb.collection("ad_revenue_events").doc("ad_ev_banner"), {
    viewerId: CHARLIE,
    creatorId: "",
    creatorName: "",
    postId: "feed_banner",
    adFormat: "banner",
    adUnitId: "ca-app-pub-3940256099942544/6300978111",
    valueMicros: 1000000,
    currencyCode: "USD",
    grossCents: 100,
    adminShareCents: 100,
    creatorShareCents: 0,
    createdAt: ts,
  });
  bannerBatch.set(viewerDb.collection("revenue_ledger").doc("ad_ev_banner"), {
    source: "ad_revenue",
    productId: "admob_banner",
    payerId: CHARLIE,
    recipientId: "",
    grossCents: 100,
    adminShareCents: 100,
    creatorShareCents: 0,
    referenceToken: "ca-app-pub-3940256099942544/6300978111",
    createdAt: ts,
  });
  await assertSucceeds(bannerBatch.commit());

  // Tampered 50/50 split MUST FAIL
  const badSplitBatch = viewerDb.batch();
  badSplitBatch.set(viewerDb.collection("ad_revenue_events").doc("ad_ev_bad"), {
    viewerId: CHARLIE,
    creatorId: BOB,
    creatorName: "Bob",
    postId: "post_1",
    adFormat: "rewarded",
    adUnitId: "ca-app-pub-3940256099942544/5224354917",
    valueMicros: 1000000,
    currencyCode: "USD",
    grossCents: 100,
    adminShareCents: 50,
    creatorShareCents: 50,
    createdAt: ts,
  });
  badSplitBatch.set(viewerDb.collection("revenue_ledger").doc("ad_ev_bad"), {
    source: "ad_revenue",
    productId: "admob_rewarded",
    payerId: CHARLIE,
    recipientId: BOB,
    grossCents: 100,
    adminShareCents: 50,
    creatorShareCents: 50,
    referenceToken: "ca-app-pub-3940256099942544/5224354917",
    createdAt: ts,
  });
  await assertFails(badSplitBatch.commit());

  // Forged ad_revenue ledger entry without matching ad_revenue_events doc MUST FAIL
  await assertFails(
    viewerDb.collection("revenue_ledger").doc("ad_forged_only").set({
      source: "ad_revenue",
      productId: "admob_rewarded",
      payerId: CHARLIE,
      recipientId: BOB,
      grossCents: 100,
      adminShareCents: 70,
      creatorShareCents: 30,
      referenceToken: "ca-app-pub-3940256099942544/5224354917",
      createdAt: ts,
    })
  );

  // 3. Creator configures Crypto (USDT TRC20) and Naira auto-payout preferences & requests auto-payout
  await assertSucceeds(
    creatorDb.collection("users").doc(BOB).set({
      userId: BOB,
      showInDiscovery: true,
      preferredIntent: "all",
      payoutMethod: "crypto",
      payoutAccount: "TX9zMockCryptoWalletAddress998877",
      payoutBankOrNetwork: "USDT TRC20",
      payoutCurrency: "CRYPTO",
      autoPayoutEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    creatorDb.collection("payout_requests").doc("auto_req_1").set({
      userId: BOB,
      userName: "Bob",
      amountCents: 250,
      ngnAmountWhole: 4000,
      payoutMethod: "ngn_bank",
      payoutCurrency: "NGN",
      payoutBankOrNetwork: "Access Bank",
      payoutAccount: "0099887766",
      isAutoPayout: true,
      status: "pending",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Admin batch-settles payout request + withdraws Admin revenue in NGN
  await assertSucceeds(
    adminDb.collection("payout_requests").doc("auto_req_1").update({
      status: "paid",
      payoutReference: "YT-AUTO-123456",
      adminNote: "Auto-settled to NGN (Access Bank)",
      updatedAt: pastTimestamp(),
    })
  );
  await assertSucceeds(
    adminDb.collection("admin_payouts").doc("adm_ngn_1").set({
      adminUid: ALICE,
      adminEmail: "gregoryfrancis652@gmail.com",
      amountCents: 1000,
      ngnAmountWhole: 16000,
      payoutCurrency: "NGN",
      payoutMethod: "ngn_bank",
      accountHolderName: "Gregory Francis",
      bankName: "GTBank Nigeria",
      maskedAccountNumber: "••••7890",
      status: "completed",
      referenceCode: "BINT-ADM-NGN01",
      createdAt: pastTimestamp(),
    })
  );
  await assertFails(creatorDb.collection("admin_payouts").doc("adm_ngn_1").get());
});

test("Facebook-style User Pages: create, owner-only edit & post, atomic follow, post like & Spark gifts", async () => {
  const ownerDb = testEnv.authenticatedContext(ALICE).firestore();
  const fanDb = testEnv.authenticatedContext(BOB).firestore();
  const ts = pastTimestamp();

  // Fan starts with 2 Spark Coins
  await assertSucceeds(
    fanDb.collection("users").doc(BOB).set({
      userId: BOB,
      showInDiscovery: true,
      preferredIntent: "all",
      sparksBalance: 2,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // 1. Alice creates a public Facebook-style Page
  await assertSucceeds(
    ownerDb.collection("pages").doc("page_1").set({
      ownerId: ALICE,
      ownerName: "Alice",
      ownerAvatarKey: "studio",
      name: "Alice Creative Studio",
      handle: "@alicecreative",
      category: "Creator",
      description: "Digital art, live sessions, and merch drops.",
      websiteUrl: "https://alice.example.com",
      coverKey: "studio",
      visibility: "public",
      followersCount: 1,
      postsCount: 0,
      totalGiftsCents: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Public Pages can be listed by signed-in users
  await assertSucceeds(
    fanDb.collection("pages").where("visibility", "==", "public").get()
  );

  // Non-owner Bob cannot edit Alice's Page details
  await assertFails(
    fanDb.collection("pages").doc("page_1").update({
      name: "Hijacked Page",
      updatedAt: pastTimestamp(),
    })
  );

  // Alice (owner) can edit her Page details
  await assertSucceeds(
    ownerDb.collection("pages").doc("page_1").update({
      name: "Alice Official Studio",
      category: "Business",
      updatedAt: pastTimestamp(),
    })
  );

  // 2. Non-owner Bob cannot publish a post onto Alice's Page
  await assertFails(
    fanDb.collection("pages").doc("page_1").collection("posts").doc("pp_forged").set({
      pageId: "page_1",
      pageName: "Alice Official Studio",
      pageVisibility: "public",
      ownerId: BOB,
      text: "Unauthorized spam post",
      likesCount: 0,
      createdAt: pastTimestamp(),
    })
  );

  // 3. Alice atomically publishes a Page post (creates post + increments postsCount & sets lastPostId)
  const postBatch = ownerDb.batch();
  postBatch.set(ownerDb.collection("pages").doc("page_1").collection("posts").doc("pp_1"), {
    pageId: "page_1",
    pageName: "Alice Official Studio",
    pageVisibility: "public",
    ownerId: ALICE,
    text: "Grand opening of our new studio collection!",
    imageKey: "studio",
    likesCount: 0,
    createdAt: pastTimestamp(),
  });
  postBatch.update(ownerDb.collection("pages").doc("page_1"), {
    postsCount: 1,
    lastPostId: "pp_1",
    updatedAt: pastTimestamp(),
  });
  await assertSucceeds(postBatch.commit());

  await assertSucceeds(
    fanDb.collection("pages").doc("page_1").collection("posts").where("pageVisibility", "==", "public").get()
  );

  // 4. Bob atomically follows the Page (page_follows/{pageId}_{uid} + followersCount: 2)
  // Tampered follow count without page_follows doc fails
  await assertFails(
    fanDb.collection("pages").doc("page_1").update({
      followersCount: 99,
      updatedAt: pastTimestamp(),
    })
  );

  const followBatch = fanDb.batch();
  followBatch.set(fanDb.collection("page_follows").doc(`page_1_${BOB}`), {
    pageId: "page_1",
    userId: BOB,
    createdAt: pastTimestamp(),
  });
  followBatch.update(fanDb.collection("pages").doc("page_1"), {
    followersCount: 2,
    updatedAt: pastTimestamp(),
  });
  await assertSucceeds(followBatch.commit());

  // 5. Bob atomically likes and unlikes Page post pp_1
  const likeBatch = fanDb.batch();
  likeBatch.set(fanDb.collection("page_post_likes").doc(`pp_1_${BOB}`), {
    pageId: "page_1",
    postId: "pp_1",
    userId: BOB,
    createdAt: pastTimestamp(),
  });
  likeBatch.update(fanDb.collection("pages").doc("page_1").collection("posts").doc("pp_1"), {
    likesCount: 1,
  });
  await assertSucceeds(likeBatch.commit());

  const unlikeBatch = fanDb.batch();
  unlikeBatch.delete(fanDb.collection("page_post_likes").doc(`pp_1_${BOB}`));
  unlikeBatch.update(fanDb.collection("pages").doc("page_1").collection("posts").doc("pp_1"), {
    likesCount: 0,
  });
  await assertSucceeds(unlikeBatch.commit());

  // 6. Bob sends an atomic 80/20 Spark Gift to Alice's Page
  const pageGiftBatch = fanDb.batch();
  pageGiftBatch.update(fanDb.collection("users").doc(BOB), {
    sparksBalance: 1,
    updatedAt: pastTimestamp(),
  });
  pageGiftBatch.set(fanDb.collection("spark_gifts").doc("gift_page_1"), {
    senderId: BOB,
    senderName: "Bob",
    recipientId: ALICE,
    recipientName: "Alice Official Studio",
    postId: "page_1",
    targetType: "page",
    sparksCount: 1,
    grossCents: 100,
    creatorShareCents: 80,
    adminFeeCents: 20,
    createdAt: pastTimestamp(),
  });
  pageGiftBatch.update(fanDb.collection("pages").doc("page_1"), {
    totalGiftsCents: 80,
    lastSparkGiftId: "gift_page_1",
    updatedAt: pastTimestamp(),
  });
  pageGiftBatch.set(fanDb.collection("revenue_ledger").doc("gift_page_1"), {
    source: "spark_gift",
    productId: "spark_gift_1",
    payerId: BOB,
    recipientId: ALICE,
    grossCents: 100,
    adminShareCents: 20,
    creatorShareCents: 80,
    referenceToken: "gift_page_1",
    createdAt: pastTimestamp(),
  });
  await assertSucceeds(pageGiftBatch.commit());
});

test("User Level Upgrade: 10 sequential steps (Water Diamond -> Professional General Diamond) NGN 5,000 each & No Jumping Levels", async () => {
  const adminDb = testEnv.authenticatedContext(ALICE, {
    email: "gregoryfrancis652@gmail.com",
    email_verified: true,
  }).firestore();
  const memberDb = testEnv.authenticatedContext(BOB, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const outsiderDb = testEnv.authenticatedContext(CHARLIE, {
    email: "charlie@example.com",
    email_verified: true,
  }).firestore();
  const ts = pastTimestamp();

  // Admin initializes platform receiving bank summary
  await assertSucceeds(
    adminDb.collection("app_settings").doc("platform").set({
      adminUid: ALICE,
      adminEmail: "gregoryfrancis652@gmail.com",
      platformFeePercent: 20,
      bankName: "FairMoney",
      adminAccountNumber: "9038006221",
      adminAccountHolder: "Francis Gregory",
      maskedAccountNumber: "••••6221",
      adminPayoutCurrency: "NGN",
      usdToNgnRate: 1600,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Member creates baseline private + public profile docs (Level 0)
  await assertSucceeds(
    memberDb.collection("users").doc(BOB).set({
      userId: BOB,
      showInDiscovery: true,
      preferredIntent: "all",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    memberDb.collection("public_profiles").doc(BOB).set(validPublicProfile(BOB, "Bob"))
  );

  // Negative 1: Unbacked self-promotion on users or public_profiles MUST FAIL
  await assertFails(
    memberDb.collection("users").doc(BOB).update({
      vipLevel: "water",
      vipLevelRank: 1,
      lastLevelUpgradeId: "fake_upg",
      updatedAt: pastTimestamp(),
    })
  );

  // Negative 2: Jumping from Level 0 directly to Level 2 (black_diamond) MUST FAIL
  const jumpZeroToTwo = memberDb.batch();
  jumpZeroToTwo.update(memberDb.collection("users").doc(BOB), {
    vipLevel: "black_diamond",
    vipLevelRank: 2,
    lastLevelUpgradeId: "upg_jump_2",
    updatedAt: ts,
  });
  jumpZeroToTwo.update(memberDb.collection("public_profiles").doc(BOB), {
    vipLevel: "black_diamond",
    vipLevelRank: 2,
    lastLevelUpgradeId: "upg_jump_2",
    updatedAt: ts,
  });
  jumpZeroToTwo.set(memberDb.collection("level_upgrades").doc("upg_jump_2"), {
    payerId: BOB,
    payerName: "Bob",
    levelId: "black_diamond",
    levelName: "Black Diamond",
    levelRank: 2,
    currency: "NGN",
    ngnAmount: 5000,
    amountCents: 312,
    adminShareCents: 312,
    creatorShareCents: 0,
    paymentMethod: "ngn_bank",
    paymentReference: "NGN-JUMP-2",
    adminEmail: "gregoryfrancis652@gmail.com",
    status: "activated",
    activatedAt: ts,
    createdAt: ts,
  });
  jumpZeroToTwo.set(memberDb.collection("revenue_ledger").doc("upg_jump_2"), {
    source: "level_upgrade",
    productId: "level_black_diamond",
    payerId: BOB,
    recipientId: "admin",
    grossCents: 312,
    adminShareCents: 312,
    creatorShareCents: 0,
    referenceToken: "NGN:NGN-JUMP-2",
    createdAt: ts,
  });
  await assertFails(jumpZeroToTwo.commit());

  // Negative 3: Forged NGN amount != 5,000 MUST FAIL
  const badPriceBatch = memberDb.batch();
  badPriceBatch.set(memberDb.collection("level_upgrades").doc("upg_low"), {
    payerId: BOB,
    payerName: "Bob",
    levelId: "water",
    levelName: "Water Diamond",
    levelRank: 1,
    currency: "NGN",
    ngnAmount: 4999,
    amountCents: 312,
    adminShareCents: 312,
    creatorShareCents: 0,
    paymentMethod: "ngn_bank",
    paymentReference: "NGN-LOW-1",
    adminEmail: "gregoryfrancis652@gmail.com",
    status: "activated",
    activatedAt: ts,
    createdAt: ts,
  });
  badPriceBatch.set(memberDb.collection("revenue_ledger").doc("upg_low"), {
    source: "level_upgrade",
    productId: "level_water",
    payerId: BOB,
    recipientId: "admin",
    grossCents: 312,
    adminShareCents: 312,
    creatorShareCents: 0,
    referenceToken: "NGN:NGN-LOW-1",
    createdAt: ts,
  });
  badPriceBatch.update(memberDb.collection("users").doc(BOB), {
    vipLevel: "water",
    vipLevelRank: 1,
    lastLevelUpgradeId: "upg_low",
    updatedAt: ts,
  });
  await assertFails(badPriceBatch.commit());

  // Positive: Step sequentially through all 10 levels (NGN 5,000 each step; last step also tested in USD $3.50)
  const tenLevels = [
    ["water", "Water Diamond", 1, "NGN", 5000, 312],
    ["black_diamond", "Black Diamond", 2, "NGN", 5000, 312],
    ["red_diamond", "Red Diamond", 3, "NGN", 5000, 312],
    ["super_diamond", "Super Diamond", 4, "NGN", 5000, 312],
    ["rock_diamond", "Rock Diamond", 5, "NGN", 5000, 312],
    ["star_diamond", "Star Diamond", 6, "NGN", 5000, 312],
    ["silver_diamond", "Silver Diamond", 7, "NGN", 5000, 312],
    ["golden_diamond", "Golden Diamond", 8, "NGN", 5000, 312],
    ["moon_diamond", "Moon Diamond", 9, "NGN", 5000, 312],
    ["professional_general_diamond", "Professional General Diamond", 10, "USD", 5600, 350],
  ];

  for (const [levelId, levelName, levelRank, currency, ngnAmount, amountCents] of tenLevels) {
    // Before unlocking Level 2, verify jumping from Level 1 directly to Level 3 FAILS
    if (levelRank === 2) {
      const jumpOneToThree = memberDb.batch();
      const jTs = pastTimestamp();
      jumpOneToThree.update(memberDb.collection("users").doc(BOB), {
        vipLevel: "red_diamond",
        vipLevelRank: 3,
        lastLevelUpgradeId: "upg_jump_3",
        updatedAt: jTs,
      });
      jumpOneToThree.update(memberDb.collection("public_profiles").doc(BOB), {
        vipLevel: "red_diamond",
        vipLevelRank: 3,
        lastLevelUpgradeId: "upg_jump_3",
        updatedAt: jTs,
      });
      jumpOneToThree.set(memberDb.collection("level_upgrades").doc("upg_jump_3"), {
        payerId: BOB,
        payerName: "Bob",
        levelId: "red_diamond",
        levelName: "Red Diamond",
        levelRank: 3,
        currency: "NGN",
        ngnAmount: 5000,
        amountCents: 312,
        adminShareCents: 312,
        creatorShareCents: 0,
        paymentMethod: "ngn_bank",
        paymentReference: "NGN-JUMP-3",
        adminEmail: "gregoryfrancis652@gmail.com",
        status: "activated",
        activatedAt: jTs,
        createdAt: jTs,
      });
      jumpOneToThree.set(memberDb.collection("revenue_ledger").doc("upg_jump_3"), {
        source: "level_upgrade",
        productId: "level_red_diamond",
        payerId: BOB,
        recipientId: "admin",
        grossCents: 312,
        adminShareCents: 312,
        creatorShareCents: 0,
        referenceToken: "NGN:NGN-JUMP-3",
        createdAt: jTs,
      });
      await assertFails(jumpOneToThree.commit());
    }

    const stepTs = pastTimestamp();
    const upgId = `upg_step_${levelRank}`;
    const stepBatch = memberDb.batch();
    stepBatch.update(memberDb.collection("users").doc(BOB), {
      vipLevel: levelId,
      vipLevelRank: levelRank,
      lastLevelUpgradeId: upgId,
      updatedAt: stepTs,
    });
    stepBatch.update(memberDb.collection("public_profiles").doc(BOB), {
      vipLevel: levelId,
      vipLevelRank: levelRank,
      lastLevelUpgradeId: upgId,
      updatedAt: stepTs,
    });
    stepBatch.set(memberDb.collection("level_upgrades").doc(upgId), {
      payerId: BOB,
      payerName: "Bob",
      levelId,
      levelName,
      levelRank,
      currency,
      ngnAmount,
      amountCents,
      adminShareCents: amountCents,
      creatorShareCents: 0,
      paymentMethod: levelRank === 1 ? "google_play" : (currency === "NGN" ? "ngn_bank" : "usd_bank"),
      paymentReference: `${currency}-STEP-${levelRank}`,
      adminEmail: "gregoryfrancis652@gmail.com",
      adminBankLabel: "FairMoney 9038006221 (Francis Gregory)",
      status: "activated",
      activatedAt: stepTs,
      createdAt: stepTs,
    });
    stepBatch.set(memberDb.collection("revenue_ledger").doc(upgId), {
      source: "level_upgrade",
      productId: `level_${levelId}`,
      payerId: BOB,
      recipientId: "admin",
      grossCents: amountCents,
      adminShareCents: amountCents,
      creatorShareCents: 0,
      referenceToken: `${currency}:${currency}-STEP-${levelRank}`,
      createdAt: stepTs,
    });
    await assertSucceeds(stepBatch.commit());
  }

  // Positive: Admin updates FairMoney 9038006221 on /app_settings/platform and member can read it; outsider/member update denied
  await assertSucceeds(
    adminDb.collection("app_settings").doc("platform").update({
      adminUid: ALICE,
      adminEmail: "gregoryfrancis652@gmail.com",
      platformFeePercent: 20,
      payoutMethod: "ngn_bank",
      bankName: "FairMoney",
      adminAccountNumber: "9038006221",
      adminAccountHolder: "Francis Gregory",
      maskedAccountNumber: "••••6221",
      adminPayoutCurrency: "NGN",
      updatedAt: ts,
    })
  );
  await assertSucceeds(memberDb.collection("app_settings").doc("platform").get());
  await assertFails(
    memberDb.collection("app_settings").doc("platform").update({
      adminAccountNumber: "0000000000",
      updatedAt: ts,
    })
  );

  // Isolation: Outsider cannot read Bob's receipts; App Admin CAN list all 10 upgrades & ledger
  await assertFails(outsiderDb.collection("level_upgrades").doc("upg_step_1").get());
  await assertSucceeds(memberDb.collection("level_upgrades").where("payerId", "==", BOB).get());
  await assertSucceeds(adminDb.collection("level_upgrades").get());
  await assertSucceeds(adminDb.collection("revenue_ledger").get());
});

test("messaging: voice notes & call logs succeed for participants and enforce payload cap", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE).firestore();
  const bobDb = testEnv.authenticatedContext(BOB).firestore();
  const outsiderDb = testEnv.authenticatedContext(CHARLIE).firestore();
  const ts = pastTimestamp();

  const convRef = aliceDb.collection("conversations").doc("conv_voice_1");
  await assertSucceeds(convRef.set({
    type: "direct",
    title: "Alice & Bob",
    participantIds: [ALICE, BOB],
    participantNames: { [ALICE]: "Alice", [BOB]: "Bob" },
    createdBy: ALICE,
    lastMessage: "Hey!",
    lastMessageAt: ts,
    createdAt: ts,
    updatedAt: ts,
  }));

  // Valid voice note
  await assertSucceeds(convRef.collection("messages").doc("vn_1").set({
    senderId: ALICE,
    senderName: "Alice",
    text: "Voice note (0:08)",
    participantIds: [ALICE, BOB],
    messageType: "voice_note",
    voiceNoteBase64: "AAAAFGZ0eXBtcDQyAAAAGl",
    voiceNoteDurationSec: 8,
    createdAt: ts,
  }));

  // Valid call log
  await assertSucceeds(bobDb.collection("conversations").doc("conv_voice_1").collection("messages").doc("cl_1").set({
    senderId: BOB,
    senderName: "Bob",
    text: "Video Call • 0:42",
    participantIds: [ALICE, BOB],
    messageType: "call_log",
    callType: "video",
    callDurationSec: 42,
    callStatus: "completed",
    createdAt: ts,
  }));

  // Negative: oversized voiceNoteBase64 > 140000 rejected
  await assertFails(convRef.collection("messages").doc("vn_huge").set({
    senderId: ALICE,
    senderName: "Alice",
    text: "Voice note",
    participantIds: [ALICE, BOB],
    messageType: "voice_note",
    voiceNoteBase64: "A".repeat(145000),
    voiceNoteDurationSec: 15,
    createdAt: ts,
  }));

  // Negative: outsider cannot post or read
  await assertFails(outsiderDb.collection("conversations").doc("conv_voice_1").collection("messages").doc("vn_1").get());
  await assertFails(outsiderDb.collection("conversations").doc("conv_voice_1").collection("messages").doc("vn_bad").set({
    senderId: CHARLIE,
    senderName: "Charlie",
    text: "Intruder voice note",
    participantIds: [ALICE, BOB, CHARLIE],
    messageType: "voice_note",
    voiceNoteBase64: "AAAA",
    voiceNoteDurationSec: 2,
    createdAt: ts,
  }));
});

test("call_sessions: caller creates ringing session, callee accepts and ends, outsider denied", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE).firestore();
  const bobDb = testEnv.authenticatedContext(BOB).firestore();
  const outsiderDb = testEnv.authenticatedContext(CHARLIE).firestore();
  const ts = pastTimestamp();

  await assertSucceeds(aliceDb.collection("conversations").doc("conv_call_1").set({
    type: "direct",
    title: "Alice & Bob",
    participantIds: [ALICE, BOB],
    participantNames: { [ALICE]: "Alice", [BOB]: "Bob" },
    createdBy: ALICE,
    lastMessage: "Ready",
    lastMessageAt: ts,
    createdAt: ts,
    updatedAt: ts,
  }));

  const callRef = aliceDb.collection("call_sessions").doc("call_1");
  await assertSucceeds(callRef.set({
    callerId: ALICE,
    callerName: "Alice",
    callerAvatarKey: "coffee",
    calleeIds: [BOB],
    participantIds: [ALICE, BOB],
    participantNames: { [ALICE]: "Alice", [BOB]: "Bob" },
    threadId: "conv_call_1",
    threadType: "conversation",
    callType: "video",
    status: "ringing",
    callerMuted: false,
    callerVideoOn: true,
    createdAt: ts,
    updatedAt: ts,
  }));

  // Negative: caller cannot self-accept to 'active'
  await assertFails(callRef.update({
    status: "active",
    updatedAt: pastTimestamp(),
  }));

  // Negative: outsider cannot read or update
  await assertFails(outsiderDb.collection("call_sessions").doc("call_1").get());
  await assertFails(outsiderDb.collection("call_sessions").doc("call_1").update({
    status: "ended",
    durationSec: 0,
    endedAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  }));

  // Callee accepts -> active
  await assertSucceeds(bobDb.collection("call_sessions").doc("call_1").update({
    status: "active",
    updatedAt: pastTimestamp(),
  }));

  // Either participant ends call -> ended
  await assertSucceeds(callRef.update({
    status: "ended",
    durationSec: 25,
    endedAt: pastTimestamp(),
    updatedAt: pastTimestamp(),
  }));

  // Terminal session cannot be re-opened
  await assertFails(bobDb.collection("call_sessions").doc("call_1").update({
    status: "active",
    updatedAt: pastTimestamp(),
  }));
});

test("shops: storefront, products, atomic 80/20 shop_order + revenue_ledger & seller fulfillment", async () => {
  const sellerDb = testEnv.authenticatedContext(ALICE).firestore();
  const buyerDb = testEnv.authenticatedContext(BOB).firestore();
  const outsiderDb = testEnv.authenticatedContext(CHARLIE).firestore();
  const ts = pastTimestamp();

  // 1. Seller creates shop
  const shopRef = sellerDb.collection("shops").doc("shop_1");
  await assertSucceeds(shopRef.set({
    ownerId: ALICE,
    ownerName: "Alice",
    ownerAvatarKey: "studio",
    name: "Alice Knowledge Hub",
    handle: "@alicehub",
    category: "Digital",
    description: "Books, guides & merch on BINTAFACE",
    currency: "USD",
    city: "Lagos",
    coverKey: "studio",
    visibility: "public",
    productsCount: 0,
    ordersCount: 0,
    totalSalesCents: 0,
    createdAt: ts,
    updatedAt: ts,
  }));

  // 2. Seller adds product atomically
  const prodRef = shopRef.collection("products").doc("prod_1");
  const addProdBatch = sellerDb.batch();
  addProdBatch.set(prodRef, {
    shopId: "shop_1",
    shopName: "Alice Knowledge Hub",
    shopVisibility: "public",
    ownerId: ALICE,
    title: "BINTAFACE Creator Playbook",
    description: "Complete guide to scaling creator revenue.",
    category: "Digital",
    priceUsdCents: 1000,
    priceNgn: 16000,
    stockCount: 5,
    isAvailable: true,
    imageKey: "studio",
    createdAt: ts,
    updatedAt: ts,
  });
  addProdBatch.update(shopRef, {
    productsCount: 1,
    lastProductId: "prod_1",
    updatedAt: ts,
  });
  await assertSucceeds(addProdBatch.commit());

  // Negative: Outsider cannot add product to Alice's shop
  await assertFails(outsiderDb.collection("shops").doc("shop_1").collection("products").doc("prod_bad").set({
    shopId: "shop_1",
    shopName: "Alice Knowledge Hub",
    shopVisibility: "public",
    ownerId: CHARLIE,
    title: "Hijack item",
    description: "No",
    category: "Digital",
    priceUsdCents: 500,
    priceNgn: 8000,
    stockCount: 1,
    isAvailable: true,
    createdAt: ts,
    updatedAt: ts,
  }));

  // 3. Buyer places order atomically (qty 2 x $10.00 = $20.00 = 2000 cents; 80% seller = 1600, 20% admin = 400)
  const orderTs = pastTimestamp();
  const orderBatch = buyerDb.batch();
  orderBatch.set(buyerDb.collection("shop_orders").doc("order_1"), {
    shopId: "shop_1",
    shopName: "Alice Knowledge Hub",
    productId: "prod_1",
    productTitle: "BINTAFACE Creator Playbook",
    buyerId: BOB,
    buyerName: "Bob",
    sellerId: ALICE,
    sellerName: "Alice",
    quantity: 2,
    currency: "USD",
    unitPriceCents: 1000,
    totalCents: 2000,
    ngnAmount: 32000,
    sellerShareCents: 1600,
    adminFeeCents: 400,
    deliveryNote: "Deliver link in chat",
    status: "placed",
    createdAt: orderTs,
    updatedAt: orderTs,
  });
  orderBatch.set(buyerDb.collection("revenue_ledger").doc("order_1"), {
    source: "shop_order",
    productId: "prod_1",
    payerId: BOB,
    recipientId: ALICE,
    grossCents: 2000,
    adminShareCents: 400,
    creatorShareCents: 1600,
    referenceToken: "shop_1",
    createdAt: orderTs,
  });
  orderBatch.update(buyerDb.collection("shops").doc("shop_1"), {
    ordersCount: 1,
    totalSalesCents: 2000,
    lastOrderId: "order_1",
    updatedAt: orderTs,
  });
  orderBatch.update(buyerDb.collection("shops").doc("shop_1").collection("products").doc("prod_1"), {
    stockCount: 3,
    isAvailable: true,
    lastOrderId: "order_1",
    updatedAt: orderTs,
  });
  await assertSucceeds(orderBatch.commit());

  // 4. Negative: Outsider cannot read order; Buyer cannot mark own order fulfilled
  await assertFails(outsiderDb.collection("shop_orders").doc("order_1").get());
  await assertFails(buyerDb.collection("shop_orders").doc("order_1").update({
    status: "fulfilled",
    updatedAt: pastTimestamp(),
  }));

  // 5. Positive: Seller fulfills order
  await assertSucceeds(sellerDb.collection("shop_orders").doc("order_1").update({
    status: "fulfilled",
    updatedAt: pastTimestamp(),
  }));
});

test("global_radio: ₦2,000 FairMoney 9038006221 pass unlocks station creation, live broadcasts, subscriptions & 80/20 sparks", async () => {
  const adminDb = testEnv.authenticatedContext(ALICE, { email: "gregoryfrancis652@gmail.com" }).firestore();
  const broadcasterDb = testEnv.authenticatedContext(ALICE).firestore();
  const listenerDb = testEnv.authenticatedContext(BOB).firestore();
  const outsiderDb = testEnv.authenticatedContext(CHARLIE).firestore();
  const ts = pastTimestamp();

  // Seed user docs for Alice and Bob (no Radio Pass yet)
  await assertSucceeds(broadcasterDb.collection("users").doc(ALICE).set({
    userId: ALICE,
    showInDiscovery: true,
    preferredIntent: "all",
    isPlusMember: false,
    superlikesBalance: 1,
    boostsBalance: 0,
    sparksBalance: 5,
    dailyLikesUsed: 0,
    dailyLikesDate: "2026-11-18",
    createdAt: ts,
    updatedAt: ts,
  }));
  await assertSucceeds(broadcasterDb.collection("public_profiles").doc(ALICE).set({
    userId: ALICE,
    displayName: "Alice",
    age: 26,
    city: "Lagos",
    bio: "Knowledge & Global Radio Host",
    lookingFor: "both",
    promptQuestion: "Topic?",
    promptAnswer: "Tech & Culture",
    interests: ["Radio", "Music"],
    avatarKey: "studio",
    isDiscoverable: true,
    createdAt: ts,
    updatedAt: ts,
  }));
  await assertSucceeds(listenerDb.collection("users").doc(BOB).set({
    userId: BOB,
    showInDiscovery: true,
    preferredIntent: "all",
    isPlusMember: false,
    superlikesBalance: 1,
    boostsBalance: 0,
    sparksBalance: 3,
    dailyLikesUsed: 0,
    dailyLikesDate: "2026-11-18",
    createdAt: ts,
    updatedAt: ts,
  }));

  // 1. Negative: User cannot spoof hasRadioAccess = true without radio_passes + revenue_ledger
  await assertFails(broadcasterDb.collection("users").doc(ALICE).update({
    hasRadioAccess: true,
    radioPassId: "fake_pass",
    updatedAt: pastTimestamp(),
  }));
  await assertFails(broadcasterDb.collection("public_profiles").doc(ALICE).update({
    hasRadioAccess: true,
    radioPassId: "fake_pass",
    updatedAt: pastTimestamp(),
  }));

  // 2. Negative: Non-payer cannot create a Global Radio channel
  await assertFails(broadcasterDb.collection("radio_channels").doc("rad_unpaid").set({
    ownerId: ALICE,
    ownerName: "Alice",
    ownerAvatarKey: "studio",
    title: "Unauthorized Radio",
    frequency: "99.1 FM",
    handle: "@unauthfm",
    category: "Global",
    description: "No pass",
    visibility: "public",
    status: "live",
    listenersCount: 0,
    subscribersCount: 1,
    totalGiftsCents: 0,
    createdAt: ts,
    updatedAt: ts,
  }));

  // 3. Negative: Underpaying (< 2000 NGN) is REJECTED
  const underpayBatch = broadcasterDb.batch();
  underpayBatch.update(broadcasterDb.collection("users").doc(ALICE), {
    hasRadioAccess: true,
    radioPassId: "rad_pass_low",
    updatedAt: ts,
  });
  underpayBatch.set(broadcasterDb.collection("radio_passes").doc("rad_pass_low"), {
    payerId: ALICE,
    payerName: "Alice",
    currency: "NGN",
    ngnAmount: 1999,
    amountCents: 150,
    adminShareCents: 150,
    creatorShareCents: 0,
    paymentMethod: "ngn_bank",
    paymentReference: "FM-LOW-1999",
    adminEmail: "gregoryfrancis652@gmail.com",
    adminBankLabel: "FairMoney 9038006221 (Francis Gregory)",
    status: "active",
    activatedAt: ts,
    createdAt: ts,
  });
  underpayBatch.set(broadcasterDb.collection("revenue_ledger").doc("rad_pass_low"), {
    source: "radio_access",
    productId: "bintaface_radio_access",
    payerId: ALICE,
    recipientId: "admin",
    grossCents: 150,
    adminShareCents: 150,
    creatorShareCents: 0,
    referenceToken: "NGN:FM-LOW-1999",
    createdAt: ts,
  });
  await assertFails(underpayBatch.commit());

  // 4. Positive: Alice pays ₦2,000 to Francis Gregory (FairMoney 9038006221) atomically
  const passTs = pastTimestamp();
  const passBatch = broadcasterDb.batch();
  passBatch.update(broadcasterDb.collection("users").doc(ALICE), {
    hasRadioAccess: true,
    radioPassId: "rad_pass_alice",
    updatedAt: passTs,
  });
  passBatch.update(broadcasterDb.collection("public_profiles").doc(ALICE), {
    hasRadioAccess: true,
    radioPassId: "rad_pass_alice",
    updatedAt: passTs,
  });
  passBatch.set(broadcasterDb.collection("radio_passes").doc("rad_pass_alice"), {
    payerId: ALICE,
    payerName: "Alice",
    currency: "NGN",
    ngnAmount: 2000,
    amountCents: 150,
    adminShareCents: 150,
    creatorShareCents: 0,
    paymentMethod: "ngn_bank",
    paymentReference: "FM-9038006221-ALICE",
    adminEmail: "gregoryfrancis652@gmail.com",
    adminBankLabel: "FairMoney 9038006221 (Francis Gregory)",
    status: "active",
    activatedAt: passTs,
    createdAt: passTs,
  });
  passBatch.set(broadcasterDb.collection("revenue_ledger").doc("rad_pass_alice"), {
    source: "radio_access",
    productId: "bintaface_radio_access",
    payerId: ALICE,
    recipientId: "admin",
    grossCents: 150,
    adminShareCents: 150,
    creatorShareCents: 0,
    referenceToken: "NGN:FM-9038006221-ALICE",
    createdAt: passTs,
  });
  await assertSucceeds(passBatch.commit());

  // 5. Positive: Alice creates her Global Radio Channel + owner subscription atomically
  const chTs = pastTimestamp();
  const createRadioBatch = broadcasterDb.batch();
  const chRef = broadcasterDb.collection("radio_channels").doc("rad_ch_1");
  createRadioBatch.set(chRef, {
    ownerId: ALICE,
    ownerName: "Alice",
    ownerAvatarKey: "studio",
    title: "BINTAFACE Knowledge FM",
    frequency: "104.7 FM",
    handle: "@knowledgefm",
    category: "Knowledge",
    description: "Broadcasting where knowledge meets power for billions.",
    coverKey: "hero",
    visibility: "public",
    status: "live",
    listenersCount: 0,
    subscribersCount: 1,
    totalGiftsCents: 0,
    createdAt: chTs,
    updatedAt: chTs,
  });
  createRadioBatch.set(broadcasterDb.collection("radio_subscriptions").doc(`rad_ch_1_${ALICE}`), {
    channelId: "rad_ch_1",
    userId: ALICE,
    createdAt: chTs,
  });
  await assertSucceeds(createRadioBatch.commit());

  // 6. Positive: Alice toggles station status and publishes an audio broadcast
  await assertSucceeds(chRef.update({
    status: "off_air",
    updatedAt: pastTimestamp(),
  }));
  await assertSucceeds(chRef.update({
    status: "live",
    updatedAt: pastTimestamp(),
  }));
  await assertSucceeds(chRef.collection("broadcasts").doc("bcast_1").set({
    channelId: "rad_ch_1",
    channelVisibility: "public",
    ownerId: ALICE,
    ownerName: "Alice",
    title: "Morning Global Brief",
    text: "Welcome to BINTAFACE Global Radio!",
    audioBase64: "AAAAFGZ0eXBtcDQyAAAAGl",
    audioDurationSec: 18,
    createdAt: pastTimestamp(),
  }));

  // 7. Negative: Unverified Bob (without Radio Pass) cannot subscribe, tune in, or hijack broadcasts
  await assertFails(
    listenerDb.collection("radio_channels").doc("rad_ch_1").update({
      listenersCount: 1,
      updatedAt: pastTimestamp(),
    })
  );
  await assertFails(
    listenerDb.collection("radio_channels").doc("rad_ch_1").collection("broadcasts").doc("bcast_hijack").set({
      channelId: "rad_ch_1",
      channelVisibility: "public",
      ownerId: BOB,
      ownerName: "Bob",
      title: "Hijack",
      text: "Nope",
      createdAt: pastTimestamp(),
    })
  );

  // 8. Positive: Bob unlocks ₦2,000 Radio Access Pass, subscribes, tunes in, and tips $1 Spark (80% host / 20% admin)
  const bobPassTs = pastTimestamp();
  const bobPassBatch = listenerDb.batch();
  bobPassBatch.update(listenerDb.collection("users").doc(BOB), {
    hasRadioAccess: true,
    radioPassId: "rad_pass_bob",
    updatedAt: bobPassTs,
  });
  bobPassBatch.set(listenerDb.collection("radio_passes").doc("rad_pass_bob"), {
    payerId: BOB,
    payerName: "Bob",
    currency: "NGN",
    ngnAmount: 2000,
    amountCents: 150,
    adminShareCents: 150,
    creatorShareCents: 0,
    paymentMethod: "google_play",
    paymentReference: "GP-RADIO-BOB-1",
    adminEmail: "gregoryfrancis652@gmail.com",
    adminBankLabel: "FairMoney 9038006221 (Francis Gregory)",
    status: "active",
    activatedAt: bobPassTs,
    createdAt: bobPassTs,
  });
  bobPassBatch.set(listenerDb.collection("revenue_ledger").doc("rad_pass_bob"), {
    source: "radio_access",
    productId: "bintaface_radio_access",
    payerId: BOB,
    recipientId: "admin",
    grossCents: 150,
    adminShareCents: 150,
    creatorShareCents: 0,
    referenceToken: "NGN:GP-RADIO-BOB-1",
    createdAt: bobPassTs,
  });
  await assertSucceeds(bobPassBatch.commit());

  // Bob subscribes atomically
  const subTs = pastTimestamp();
  const subBatch = listenerDb.batch();
  subBatch.set(listenerDb.collection("radio_subscriptions").doc(`rad_ch_1_${BOB}`), {
    channelId: "rad_ch_1",
    userId: BOB,
    createdAt: subTs,
  });
  subBatch.update(listenerDb.collection("radio_channels").doc("rad_ch_1"), {
    subscribersCount: 2,
    updatedAt: subTs,
  });
  await assertSucceeds(subBatch.commit());

  // Bob tunes in (+1 listener) & sends $1 Spark gift (80 cents Alice / 20 cents Admin)
  await assertSucceeds(listenerDb.collection("radio_channels").doc("rad_ch_1").update({
    listenersCount: 1,
    updatedAt: pastTimestamp(),
  }));
  const sparkTs = pastTimestamp();
  const sparkBatch = listenerDb.batch();
  sparkBatch.set(listenerDb.collection("spark_gifts").doc("rad_spark_1"), {
    senderId: BOB,
    senderName: "Bob",
    recipientId: ALICE,
    recipientName: "Alice",
    postId: "rad_ch_1",
    targetType: "radio",
    sparksCount: 1,
    grossCents: 100,
    creatorShareCents: 80,
    adminFeeCents: 20,
    createdAt: sparkTs,
  });
  sparkBatch.set(listenerDb.collection("revenue_ledger").doc("rad_spark_1"), {
    source: "spark_gift",
    productId: "spark_gift",
    payerId: BOB,
    recipientId: ALICE,
    grossCents: 100,
    adminShareCents: 20,
    creatorShareCents: 80,
    referenceToken: "rad_spark_1",
    createdAt: sparkTs,
  });
  sparkBatch.update(listenerDb.collection("users").doc(BOB), {
    sparksBalance: 2,
    updatedAt: sparkTs,
  });
  sparkBatch.update(listenerDb.collection("radio_channels").doc("rad_ch_1"), {
    totalGiftsCents: 80,
    lastSparkGiftId: "rad_spark_1",
    updatedAt: sparkTs,
  });
  await assertSucceeds(sparkBatch.commit());

  // 9. Isolation: Outsider Charlie cannot read Alice's Radio Pass; App Admin CAN read all Radio passes
  await assertFails(outsiderDb.collection("radio_passes").doc("rad_pass_alice").get());
  await assertSucceeds(adminDb.collection("radio_passes").get());
});


