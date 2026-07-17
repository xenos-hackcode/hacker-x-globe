// functions/src/verifyOwnerCode.ts
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { logger } from "firebase-functions/v2";
import { defineSecret } from "firebase-functions/params";

const OWNER_CODE = defineSecret("CEDAL_OWNER_CODE");

type VerifyOwnerCodeRequest = { code?: string };
type VerifyOwnerCodeResponse = {
  ok: boolean;
  ownerSession: { createdAt: number };
};

export const verifyOwnerCode = onCall<VerifyOwnerCodeRequest>(
  { secrets: [OWNER_CODE] },
  async (request): Promise<VerifyOwnerCodeResponse> => {
    const data = request.data || {};
    const ownerCodeSecret = OWNER_CODE.value();

    if (!ownerCodeSecret) {
      logger.error("Missing CEDAL_OWNER_CODE secret");
      throw new HttpsError(
        "failed-precondition",
        "Owner code not configured on server."
      );
    }

    const code = data.code?.trim();
    if (!code || code !== ownerCodeSecret) {
      throw new HttpsError("permission-denied", "Invalid owner passcode.");
    }

    const ownerSession = { createdAt: Date.now() };
    return { ok: true, ownerSession };
  }
);
