// src/api/runCode.ts
import AsyncStorage from "@react-native-async-storage/async-storage";
import { auth } from "@/src/api/firebase";
import {
  COMPILED_LANGUAGES,
  type CodeLanguage,
} from "@/src/member/work/code/EditorPanel";

type Tier = "interpreted" | "compiled";

const REGION_URLS: Record<Tier, Record<string, string>> = {
  interpreted: {
    lon: "https://code-runner-717899371194.europe-west2.run.app",
    fra: "https://code-runner-717899371194.europe-west3.run.app",
    nyc: "https://code-runner-717899371194.us-east1.run.app",
  },
  compiled: {
    lon: "https://code-runner-compiled-717899371194.europe-west2.run.app",
    fra: "https://code-runner-compiled-717899371194.europe-west3.run.app",
    nyc: "https://code-runner-compiled-717899371194.us-east1.run.app",
  },
};

const REGION_CACHE_KEY: Record<Tier, string> = {
  interpreted: "cedal.runCode.region.interpreted",
  compiled: "cedal.runCode.region.compiled",
};

const PING_TIMEOUT_MS = 2500;

export type RunCodeResult = {
  stdout: string;
  stderr: string;
  exitCode: number;
  timedOut: boolean;
  durationMs: number;
};

function tierFor(language: CodeLanguage): Tier {
  return COMPILED_LANGUAGES.includes(language) ? "compiled" : "interpreted";
}

async function pingRegion(regionKey: string, baseUrl: string): Promise<number> {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), PING_TIMEOUT_MS);
  const started = Date.now();
  try {
    const res = await fetch(`${baseUrl}/health`, { signal: controller.signal });
    if (!res.ok) throw new Error("bad status");
    return Date.now() - started;
  } finally {
    clearTimeout(timer);
  }
}

async function pickNearestRegion(tier: Tier): Promise<string> {
  const urls = REGION_URLS[tier];
  const cacheKey = REGION_CACHE_KEY[tier];
  const cached = await AsyncStorage.getItem(cacheKey);
  if (cached && urls[cached]) return cached;

  const entries = Object.entries(urls);
  const results = await Promise.allSettled(
    entries.map(([key, url]) => pingRegion(key, url).then((ms) => ({ key, ms })))
  );

  let best: { key: string; ms: number } | null = null;
  for (const r of results) {
    if (r.status === "fulfilled" && (!best || r.value.ms < best.ms)) {
      best = r.value;
    }
  }

  const winner = best?.key ?? entries[0][0];
  await AsyncStorage.setItem(cacheKey, winner);
  return winner;
}

export async function runCode(params: {
  language: CodeLanguage;
  code: string;
  stdin?: string;
}): Promise<RunCodeResult> {
  const user = auth.currentUser;
  if (!user) {
    throw new Error("You must be signed in to run code.");
  }

  const tier = tierFor(params.language);
  const regionKey = await pickNearestRegion(tier);
  const baseUrl = REGION_URLS[tier][regionKey];
  const idToken = await user.getIdToken();

  const res = await fetch(`${baseUrl}/run`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${idToken}`,
    },
    body: JSON.stringify({
      language: params.language,
      code: params.code,
      stdin: params.stdin,
    }),
  });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw new Error(body?.error ?? `Run failed (${res.status})`);
  }

  return res.json();
}
