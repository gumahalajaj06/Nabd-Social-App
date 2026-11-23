const { onDocumentWritten, onDocumentCreated } = require("firebase-functions/v2/firestore");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

const DATABASE_ID = "ai-studio-android-nabd-971e7e5f-0c0a-456e-938e-8d95eac5a74d";
const db = getFirestore(DATABASE_ID);

function computeScore(postData) {
  const reactions = postData.totalReactionsCount || postData.likesCount || 0;
  const comments = postData.commentsCount || postData.repliesCount || 0;
  const views = postData.viewsCount || 0;
  const reposts = postData.repostsCount || 0;
  return reactions * 3 + comments * 4 + reposts * 3 + Math.floor(views * 0.25);
}

async function recalculateAutoPinForUser(authorId) {
  if (!authorId) return;
  const userRef = db.collection("users").doc(authorId);
  const userSnap = await userRef.get();
  if (!userSnap.exists) return;
  const user = userSnap.data();
  if (user.pinMode !== "top_engaged") return;

  const postsSnap = await db
    .collection("posts")
    .where("authorId", "==", authorId)
    .get();

  let topPostId = "";
  let bestScore = -1;
  postsSnap.forEach((doc) => {
    const data = doc.data();
    if (data.replyToPostId) return;
    const score = computeScore(data);
    if (score > bestScore) {
      bestScore = score;
      topPostId = doc.id;
    }
  });

  if (topPostId !== (user.pinnedPostId || "")) {
    await userRef.update({
      pinnedPostId: topPostId,
      updatedAt: FieldValue.serverTimestamp(),
    });
  }
}

exports.onPostReactionWrite = onDocumentWritten(
  {
    database: DATABASE_ID,
    document: "posts/{postId}/reactions/{userId}",
  },
  async (event) => {
    const postId = event.params.postId;
    const postRef = db.collection("posts").doc(postId);
    const reactionsSnap = await postRef.collection("reactions").get();
    const counts = {};
    reactionsSnap.forEach((doc) => {
      const type = doc.data().type || "like";
      counts[type] = (counts[type] || 0) + 1;
    });
    const total = reactionsSnap.size;

    const postSnap = await postRef.get();
    if (!postSnap.exists) return;
    const postData = postSnap.data();
    const newScore = computeScore({
      ...postData,
      totalReactionsCount: total,
      likesCount: total,
    });

    await postRef.update({
      reactionsCount: counts,
      totalReactionsCount: total,
      likesCount: total,
      engagementScore: newScore,
      updatedAt: FieldValue.serverTimestamp(),
    });

    await recalculateAutoPinForUser(postData.authorId);
  }
);

exports.onCommentWrite = onDocumentWritten(
  {
    database: DATABASE_ID,
    document: "posts/{postId}/comments/{commentId}",
  },
  async (event) => {
    const postId = event.params.postId;
    const postRef = db.collection("posts").doc(postId);
    const postSnap = await postRef.get();
    if (!postSnap.exists) return;

    const commentsSnap = await postRef.collection("comments").get();
    const count = commentsSnap.size;
    const postData = postSnap.data();
    const newScore = computeScore({ ...postData, commentsCount: count });

    await postRef.update({
      commentsCount: count,
      repliesCount: count,
      engagementScore: newScore,
      updatedAt: FieldValue.serverTimestamp(),
    });

    await recalculateAutoPinForUser(postData.authorId);
  }
);

exports.onPostPinRecalculation = onDocumentWritten(
  {
    database: DATABASE_ID,
    document: "posts/{postId}",
  },
  async (event) => {
    const after = event.data?.after?.data();
    const before = event.data?.before?.data();
    const authorId = after?.authorId || before?.authorId;
    if (!authorId) return;
    await recalculateAutoPinForUser(authorId);
  }
);

exports.onVerificationApproved = onDocumentWritten(
  {
    database: DATABASE_ID,
    document: "verification_requests/{requestId}",
  },
  async (event) => {
    const after = event.data?.after?.data();
    const before = event.data?.before?.data();
    if (!after || after.status !== "approved" || before?.status === "approved") {
      return;
    }

    const userId = after.userId;
    const badgeType = after.requestedType || "personal";
    const batch = db.batch();

    const userUpdate = {
      verifiedType: badgeType,
      updatedAt: FieldValue.serverTimestamp(),
    };
    if (badgeType === "business" && after.organizationName) {
      userUpdate.officialLabel = `Official Organization · ${after.organizationName}`;
    } else if (badgeType === "government") {
      userUpdate.officialLabel = "Official Institution";
    }
    batch.update(db.collection("users").doc(userId), userUpdate);

    const postsSnap = await db
      .collection("posts")
      .where("authorId", "==", userId)
      .get();
    postsSnap.forEach((doc) => {
      batch.update(doc.ref, { authorVerifiedType: badgeType });
    });

    await batch.commit();
  }
);

function isInsideQuietHours(quietStart, quietEnd, nowDate = new Date()) {
  if (!quietStart || !quietEnd) return false;
  const parseMinutes = (hhmm) => {
    const [h, m] = hhmm.split(":").map((v) => parseInt(v, 10) || 0);
    return h * 60 + m;
  };
  const start = parseMinutes(quietStart);
  const end = parseMinutes(quietEnd);
  const now = nowDate.getHours() * 60 + nowDate.getMinutes();
  if (start <= end) return now >= start && now <= end;
  return now >= start || now <= end;
}

exports.onNotificationCreated = onDocumentCreated(
  {
    database: DATABASE_ID,
    document: "users/{userId}/notifications/{notifId}",
  },
  async (event) => {
    const userId = event.params.userId;
    const notif = event.data?.data();
    if (!notif) return;

    const userSnap = await db.collection("users").doc(userId).get();
    if (!userSnap.exists) return;
    const user = userSnap.data();
    const prefs = user.notifications || {};
    if (prefs.pushEnabled === false) return;
    if (prefs.quietHoursEnabled && isInsideQuietHours(prefs.quietStart, prefs.quietEnd)) {
      return;
    }

    const typeKey = notif.type === "like" ? "likes" : notif.type === "reaction" ? "reactions" : `${notif.type}s`;
    if (prefs[typeKey] === false) return;

    const privateDoc = await db.collection("users").doc(userId).collection("private").doc("fcm").get();
    const token = privateDoc.exists ? privateDoc.data().token : null;
    if (!token) return;

    await getMessaging().send({
      token,
      notification: {
        title: `${notif.actorName || "Nabd"} (@${notif.actorUsername || "user"})`,
        body: notif.previewText || "New activity on your pulse",
      },
      data: {
        type: notif.type || "reaction",
        postId: notif.postId || "",
        actorId: notif.actorId || "",
      },
    });
  }
);
