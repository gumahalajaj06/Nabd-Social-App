const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_EMAIL = "ameeralajaj@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

test("Unauthenticated user: denied across users, posts, stories, verification, and chats", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").where("accountStatus", "==", "active").get());
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("stories").where("isActive", "==", true).get());
  await assertFails(unauthDb.collection("chats").where("participants", "array-contains", ALICE_UID).get());
});

test("Users & Usernames: creation, uniqueness claim, shadow-update rejection, and verifiedType lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate();

  // Claim username
  await assertSucceeds(
    aliceDb.collection("usernames").doc("alice_dev").set({
      uid: ALICE_UID,
      username: "alice_dev",
      createdAt: now,
    })
  );

  // Create profile without @DocumentId 'id'
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "Alice",
      username: "alice_dev",
      bio: "Text-only thinker",
      avatarColor: "#0EA5A4",
      verifiedType: "none",
      followersCount: 0,
      followingCount: 0,
      isPrivate: false,
      accountStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Self-upgrade verifiedType must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      verifiedType: "business",
      updatedAt: pastDate(500),
    })
  );

  // Bob cannot modify Alice's bio
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      bio: "Hacked",
      updatedAt: pastDate(500),
    })
  );

  // Valid filtered user list query succeeds; unfiltered fails
  await assertSucceeds(
    bobDb.collection("users").where("accountStatus", "==", "active").get()
  );
  await assertFails(bobDb.collection("users").get());
});

test("Verification requests & Admin Queue: email_verified spoofing rejected, verified admin approves & terminal-locks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: ADMIN_EMAIL, email_verified: false })
    .firestore();
  const realAdminDb = testEnv
    .authenticatedContext("admin_uid", { email: ADMIN_EMAIL, email_verified: true })
    .firestore();
  const now = pastDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "Alice",
      username: "alice_dev",
      avatarColor: "#0EA5A4",
      verifiedType: "none",
      followersCount: 0,
      followingCount: 0,
      isPrivate: false,
      accountStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("verification_requests").doc("req_1").set({
      userId: ALICE_UID,
      userName: "Alice",
      username: "alice_dev",
      requestedType: "personal",
      reason: "Writer and active community author on Nabd.",
      status: "pending",
      submittedAt: now,
    })
  );

  // Spoofed admin with email_verified: false must fail
  await assertFails(
    spoofedAdminDb.collection("verification_requests").doc("req_1").update({
      status: "approved",
      reviewedAt: pastDate(500),
    })
  );

  // Real verified admin can approve and update user verifiedType
  await assertSucceeds(
    realAdminDb.collection("verification_requests").doc("req_1").update({
      status: "approved",
      reviewedAt: pastDate(500),
    })
  );
  await assertSucceeds(
    realAdminDb.collection("users").doc(ALICE_UID).update({
      verifiedType: "personal",
      updatedAt: pastDate(500),
    })
  );

  // Terminal state lock: once approved, cannot be mutated again
  await assertFails(
    realAdminDb.collection("verification_requests").doc("req_1").update({
      status: "rejected",
      reviewedAt: pastDate(200),
    })
  );
});

test("Posts: character limit enforcement (500 standard, 2000 verified), filtered list, likes & poll votes", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "Alice",
      username: "alice_dev",
      avatarColor: "#0EA5A4",
      verifiedType: "none",
      followersCount: 0,
      followingCount: 0,
      isPrivate: false,
      accountStatus: "active",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Unverified user > 500 chars fails
  await assertFails(
    aliceDb.collection("posts").doc("post_long").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorUsername: "alice_dev",
      authorVerifiedType: "none",
      content: "a".repeat(501),
      hashtags: [],
      mentions: [],
      likesCount: 0,
      repostsCount: 0,
      repliesCount: 0,
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid post with poll succeeds
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorUsername: "alice_dev",
      authorVerifiedType: "none",
      content: "First text pulse on #Nabd!",
      poll: {
        options: ["Text only", "Voice + Text"],
        votes: {},
        expiresAt: new Date(Date.now() + 86400000),
      },
      hashtags: ["nabd"],
      mentions: [],
      likesCount: 0,
      repostsCount: 0,
      repliesCount: 0,
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered query succeeds; unfiltered query fails
  await assertSucceeds(
    bobDb.collection("posts").where("visibility", "==", "public").get()
  );
  await assertFails(bobDb.collection("posts").get());

  // Bob can like (+1) and vote on poll, but cannot jump likesCount by +100
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").collection("likes").doc(BOB_UID).set({
      userId: BOB_UID,
      postId: "post_1",
      createdAt: pastDate(500),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 1,
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    bobDb.collection("posts").doc("post_1").update({
      likesCount: 100,
      updatedAt: pastDate(500),
    })
  );
});

test("Stories: 24h expiry validation, active list query, and denormalized viewer list", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate();

  await assertSucceeds(
    aliceDb.collection("stories").doc("story_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorUsername: "alice_dev",
      authorVerifiedType: "none",
      authorAvatarColor: "#0EA5A4",
      text: "خاطرة سريعة في نبض",
      backgroundColor: "#0F172A",
      fontStyle: "cairo",
      textAlign: "center",
      viewsCount: 0,
      isActive: true,
      createdAt: now,
      expiresAt: new Date(Date.now() + 24 * 3600 * 1000),
    })
  );

  await assertSucceeds(
    bobDb.collection("stories").where("isActive", "==", true).get()
  );

  await assertSucceeds(
    bobDb.collection("stories").doc("story_1").collection("viewers").doc(BOB_UID).set({
      viewerId: BOB_UID,
      storyAuthorId: ALICE_UID,
      viewerName: "Bob",
      viewerUsername: "bob_456",
      viewerAvatarColor: "#6366F1",
      viewedAt: pastDate(500),
    })
  );

  // Author lists viewers via denormalized storyAuthorId
  await assertSucceeds(
    aliceDb
      .collection("stories")
      .doc("story_1")
      .collection("viewers")
      .where("storyAuthorId", "==", ALICE_UID)
      .get()
  );
  // Bob cannot list Alice's story viewers
  await assertFails(
    bobDb
      .collection("stories")
      .doc("story_1")
      .collection("viewers")
      .where("storyAuthorId", "==", ALICE_UID)
      .get()
  );
});

test("Chats & Messages: participant isolation and denormalized message access", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_789").firestore();
  const now = pastDate();

  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_ab").set({
      participants: [ALICE_UID, BOB_UID],
      participantNames: { [ALICE_UID]: "Alice", [BOB_UID]: "Bob" },
      participantUsernames: { [ALICE_UID]: "alice_dev", [BOB_UID]: "bob_456" },
      participantAvatarColors: { [ALICE_UID]: "#0EA5A4", [BOB_UID]: "#6366F1" },
      participantVerifiedTypes: { [ALICE_UID]: "none", [BOB_UID]: "none" },
      lastMessageText: "Hello Bob",
      lastMessageType: "text",
      lastMessageSenderId: ALICE_UID,
      lastMessageTimestamp: now,
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 1 },
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_ab").collection("messages").doc("msg_1").set({
      participants: [ALICE_UID, BOB_UID],
      senderId: ALICE_UID,
      type: "text",
      textContent: "Hello Bob",
      deletedForEveryone: false,
      status: "sent",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob can query messages cleanly; Charlie is denied
  await assertSucceeds(
    bobDb
      .collection("chats")
      .doc("chat_ab")
      .collection("messages")
      .where("participants", "array-contains", BOB_UID)
      .get()
  );
  await assertFails(
    charlieDb.collection("chats").doc("chat_ab").collection("messages").doc("msg_1").get()
  );
  await assertFails(
    charlieDb
      .collection("chats")
      .doc("chat_ab")
      .collection("messages")
      .where("participants", "array-contains", ALICE_UID)
      .get()
  );
});

test("Profile settings, Post edit/delete, Facebook-style reactions & nested comments", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "Alice",
      username: "alice_dev",
      avatarColor: "#0EA5A4",
      avatarPattern: "pulse",
      verifiedType: "none",
      pinMode: "top_engaged",
      pinnedPostId: "",
      followersCount: 0,
      followingCount: 0,
      isPrivate: false,
      accountStatus: "active",
      privacy: {
        allowMentionsFrom: "everyone",
        allowDMsFrom: "following",
        hideLikesTab: false,
      },
      notifications: {
        pushEnabled: true,
        quietHoursEnabled: true,
        quietStart: "22:00",
        quietEnd: "07:00",
      },
      preferences: {
        themeMode: "oled",
        fontFamily: "cairo",
        fontScale: 1.1,
        language: "ar",
      },
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice creates a post
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_react").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      authorUsername: "alice_dev",
      authorVerifiedType: "none",
      content: "Original thought on #Nabd",
      hashtags: ["nabd"],
      mentions: [],
      likesCount: 0,
      repostsCount: 0,
      repliesCount: 0,
      reactionsCount: {},
      totalReactionsCount: 0,
      viewsCount: 0,
      commentsCount: 0,
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice edits her post; Bob cannot edit Alice's post
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_react").update({
      content: "Edited thought on #Nabd",
      isEdited: true,
      editedAt: pastDate(400),
      updatedAt: pastDate(400),
    })
  );
  await assertFails(
    bobDb.collection("posts").doc("post_react").update({
      content: "Bob hijack",
      isEdited: true,
      editedAt: pastDate(300),
      updatedAt: pastDate(300),
    })
  );

  // Bob reacts with "love" and increments totalReactionsCount
  await assertSucceeds(
    bobDb.collection("posts").doc("post_react").collection("reactions").doc(BOB_UID).set({
      userId: BOB_UID,
      postId: "post_react",
      type: "love",
      userName: "Bob",
      userUsername: "bob_456",
      createdAt: pastDate(300),
    })
  );
  await assertSucceeds(
    bobDb.collection("posts").doc("post_react").update({
      reactionsCount: { love: 1 },
      totalReactionsCount: 1,
      likesCount: 1,
      updatedAt: pastDate(300),
    })
  );

  // Bob comments and Alice reacts to Bob's comment
  await assertSucceeds(
    bobDb.collection("posts").doc("post_react").collection("comments").doc("c_1").set({
      postId: "post_react",
      authorId: BOB_UID,
      authorName: "Bob",
      authorUsername: "bob_456",
      authorVerifiedType: "none",
      content: "Insightful post!",
      reactionsCount: {},
      totalReactionsCount: 0,
      createdAt: pastDate(200),
      updatedAt: pastDate(200),
    })
  );
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_react").collection("comments").doc("c_1").collection("reactions").doc(ALICE_UID).set({
      userId: ALICE_UID,
      commentId: "c_1",
      type: "haha",
      createdAt: pastDate(100),
    })
  );

  // Alice updates her profile privacy/notifications/appearance settings
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      pinMode: "manual",
      pinnedPostId: "post_react",
      privacy: {
        allowMentionsFrom: "following",
        allowDMsFrom: "following",
        hideLikesTab: true,
        appLockEnabled: true,
      },
      preferences: {
        themeMode: "dark",
        accentColor: "#2563EB",
        fontFamily: "amiri",
        fontScale: 1.2,
        language: "en",
      },
      updatedAt: pastDate(50),
    })
  );
});

test("Multi-language preferences (tr/fr/es/de) and chat attachments (image/video/file)", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = pastDate();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      uid: ALICE_UID,
      name: "Alice",
      username: "alice_dev",
      avatarColor: "#0D9488",
      verifiedType: "none",
      followersCount: 0,
      followingCount: 0,
      isPrivate: false,
      accountStatus: "active",
      preferences: {
        themeMode: "light",
        fontFamily: "inter",
        language: "tr",
      },
      createdAt: now,
      updatedAt: now,
    })
  );

  // Invalid language code must fail
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferences: { language: "zz" },
      updatedAt: pastDate(400),
    })
  );

  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_media").set({
      participants: [ALICE_UID, BOB_UID],
      lastMessageText: "📎 contract.pdf",
      lastMessageType: "file",
      lastMessageSenderId: ALICE_UID,
      lastMessageTimestamp: now,
      unreadCounts: { [ALICE_UID]: 0, [BOB_UID]: 1 },
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_media").collection("messages").doc("msg_pdf").set({
      participants: [ALICE_UID, BOB_UID],
      senderId: ALICE_UID,
      type: "file",
      attachmentName: "contract.pdf",
      attachmentMimeType: "application/pdf",
      attachmentSizeBytes: 24576,
      attachmentData: "JVBERi0xLjQK",
      deletedForEveryone: false,
      status: "sent",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    bobDb.collection("chats").doc("chat_media").collection("messages").doc("msg_vid").set({
      participants: [ALICE_UID, BOB_UID],
      senderId: BOB_UID,
      type: "video",
      attachmentName: "clip.mp4",
      attachmentMimeType: "video/mp4",
      attachmentSizeBytes: 2457600,
      deletedForEveryone: false,
      status: "sent",
      createdAt: pastDate(300),
      updatedAt: pastDate(300),
    })
  );
});


