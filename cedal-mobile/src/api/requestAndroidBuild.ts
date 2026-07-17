// src/api/requestAndroidBuild.ts
import { httpsCallable } from "firebase/functions";
import { euFunctions } from "@/src/api/firebase";

// Deployed in europe-west2, not the default us-central1: pingServers.ts calls
// setGlobalOptions() at module load, which becomes the default region for every
// function exported after it in functions/src/index.ts (same reason alexReport
// uses euFunctions instead of the default `functions` client).
const requestAndroidBuildFn = httpsCallable(euFunctions, "requestAndroidBuild");

export async function requestAndroidBuild(code: string): Promise<{ jobId: string }> {
  const res = await requestAndroidBuildFn({ code });
  // @ts-ignore
  return { jobId: res.data?.jobId };
}
