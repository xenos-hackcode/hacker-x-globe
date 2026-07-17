// src/api/owner.ts
import { httpsCallable } from "firebase/functions";
import { functions } from "./firebase";

type VerifyOwnerCodeResp = {
  ok: boolean;
  ownerSession: { uid: string; createdAt: number };
};

export async function verifyOwnerCodeRemote(code: string) {
  const fn = httpsCallable<{ code: string }, VerifyOwnerCodeResp>(
    functions,
    "verifyOwnerCode"
  );
  const res = await fn({ code });
  return res.data;
}
