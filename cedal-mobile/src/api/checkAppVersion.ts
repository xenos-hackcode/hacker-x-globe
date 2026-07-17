// src/api/checkAppVersion.ts
import { doc, getDoc } from "firebase/firestore";
import { db } from "@/src/api/firebase";

export type AppVersionConfig = {
  minVersion?: string;
  latestVersion?: string;
  updateUrl?: string;
};

export type AppVersionStatus = {
  updateRequired: boolean; // current < minVersion -> block usage
  updateAvailable: boolean; // current < latestVersion -> dismissible nudge
  updateUrl: string;
};

const DEFAULT_UPDATE_URL =
  "https://play.google.com/store/apps/details?id=com.xhacker.cedalmobiledev";

/** Compares "x.y.z" version strings. Returns <0, 0, or >0 like Array.sort comparators. */
function compareVersions(a: string, b: string): number {
  const partsA = a.split(".").map((n) => parseInt(n, 10) || 0);
  const partsB = b.split(".").map((n) => parseInt(n, 10) || 0);
  const len = Math.max(partsA.length, partsB.length);
  for (let i = 0; i < len; i++) {
    const diff = (partsA[i] ?? 0) - (partsB[i] ?? 0);
    if (diff !== 0) return diff;
  }
  return 0;
}

export async function checkAppVersion(
  currentVersion: string
): Promise<AppVersionStatus> {
  const snap = await getDoc(doc(db, "appConfig", "android"));
  const config = (snap.data() as AppVersionConfig) ?? {};

  const updateRequired = !!config.minVersion && compareVersions(currentVersion, config.minVersion) < 0;
  const updateAvailable =
    !!config.latestVersion && compareVersions(currentVersion, config.latestVersion) < 0;

  return {
    updateRequired,
    updateAvailable,
    updateUrl: config.updateUrl || DEFAULT_UPDATE_URL,
  };
}
