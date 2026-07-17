// functions/src/presence.ts
import * as admin from "firebase-admin";
import { onValueWritten } from "firebase-functions/v2/database";

if (!admin.apps.length) {
  admin.initializeApp();
}

const firestore = admin.firestore();

/**
 * Mirror Realtime Database /status/{uid} into Firestore users/{uid}
 * RTDB node shape: { state: "online" | "offline", last_changed: number }
 */
export const onUserStatusChanged = onValueWritten(
  "/status/{uid}",
  async (event) => {
    const uid = event.params.uid as string;

    const after = event.data.after.val();
    const before = event.data.before.val();

    // If nothing changed, skip
    if (JSON.stringify(after) === JSON.stringify(before)) {
      return;
    }

    // If node was deleted or is missing, mark offline
    if (!after) {
      await firestore.doc(`users/${uid}`).set(
        {
          online: false,
          lastSeen: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true },
      );
      return;
    }

    const isOnline = after.state === "online";
    const lastChanged = after.last_changed || Date.now();

    await firestore.doc(`users/${uid}`).set(
      {
        online: isOnline,
        lastSeen: new Date(lastChanged),
      },
      { merge: true },
    );
  },
);
