// src/security/passcodeLockout.ts
import * as SecureStore from "expo-secure-store";

const FAIL_KEY = (uid: string) => `cedal_fail_count_${uid}`;
const LOCK_KEY = (uid: string) => `cedal_lock_until_${uid}`;

export type LockoutState = {
  locked: boolean;
  failCount: number;
  lockUntil: number | null; // ms since epoch
};

export async function getLockoutState(uid: string): Promise<LockoutState> {
  const failRaw = await SecureStore.getItemAsync(FAIL_KEY(uid));
  const lockRaw = await SecureStore.getItemAsync(LOCK_KEY(uid));

  const failCount = failRaw ? Number(failRaw) || 0 : 0;
  const lockUntil = lockRaw ? Number(lockRaw) || 0 : 0;
  const now = Date.now();

  return {
    locked: lockUntil > 0 && now < lockUntil,
    failCount,
    lockUntil: lockUntil > 0 ? lockUntil : null,
  };
}

export async function registerSuccess(uid: string) {
  await SecureStore.deleteItemAsync(FAIL_KEY(uid));
  await SecureStore.deleteItemAsync(LOCK_KEY(uid));
}

export async function registerFailure(uid: string): Promise<LockoutState> {
  const state = await getLockoutState(uid);
  const nextFail = state.failCount + 1;

  if (nextFail >= 3) {
    const lockUntil = Date.now() + 2 * 60 * 60 * 1000; // 2 hours
    await SecureStore.setItemAsync(FAIL_KEY(uid), String(nextFail));
    await SecureStore.setItemAsync(LOCK_KEY(uid), String(lockUntil));
    return { locked: true, failCount: nextFail, lockUntil };
  }

  await SecureStore.setItemAsync(FAIL_KEY(uid), String(nextFail));
  return { locked: false, failCount: nextFail, lockUntil: state.lockUntil };
}
