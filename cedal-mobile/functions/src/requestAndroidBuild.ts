// functions/src/requestAndroidBuild.ts
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { logger } from "firebase-functions/v2";
import { db } from "./firebaseAdmin";

const ANDROID_BUILDER_URL = defineSecret("ANDROID_BUILDER_URL");
const ANDROID_BUILDER_SECRET = defineSecret("ANDROID_BUILDER_SECRET");

const ANDROID_BUILD_COST_VIP = 500;

type RequestAndroidBuildRequest = {
  code?: string;
};

export const requestAndroidBuild = onCall<RequestAndroidBuildRequest>(
  { secrets: [ANDROID_BUILDER_URL, ANDROID_BUILDER_SECRET] },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) {
      throw new HttpsError("unauthenticated", "Sign in required.");
    }

    const code = request.data?.code;
    if (typeof code !== "string" || !code.trim()) {
      throw new HttpsError("invalid-argument", "Missing Kotlin source.");
    }
    if (code.length > 64 * 1024) {
      throw new HttpsError("invalid-argument", "Source file too large.");
    }

    const userRef = db.collection("users").doc(uid);
    const jobRef = db.collection("androidBuilds").doc();

    await db.runTransaction(async (tx) => {
      const snap = await tx.get(userRef);
      const data = snap.data() || {};
      const currentVipExp = data.vipExp ?? 0;

      if (currentVipExp < ANDROID_BUILD_COST_VIP) {
        throw new HttpsError(
          "failed-precondition",
          `Not enough VIP credits. This build costs ${ANDROID_BUILD_COST_VIP}.`
        );
      }

      tx.set(
        userRef,
        { vipExp: currentVipExp - ANDROID_BUILD_COST_VIP },
        { merge: true }
      );
      tx.set(jobRef, {
        uid,
        status: "queued",
        createdAt: new Date(),
        cost: ANDROID_BUILD_COST_VIP,
      });
    });

    // Builds take minutes, so don't block the callable on it - fire the trigger
    // and let the android-builder service update the job doc as it progresses.
    fetch(`${ANDROID_BUILDER_URL.value()}/build`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${ANDROID_BUILDER_SECRET.value()}`,
      },
      body: JSON.stringify({ jobId: jobRef.id, uid, code }),
    }).catch((err) => {
      logger.error("android-builder trigger failed", err);
      void jobRef.set(
        { status: "error", errorMessage: "Failed to start build" },
        { merge: true }
      );
    });

    return { jobId: jobRef.id };
  }
);
