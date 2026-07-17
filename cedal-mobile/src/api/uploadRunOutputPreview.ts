// src/api/uploadRunOutputPreview.ts
import { ref, uploadString, getDownloadURL } from "firebase/storage";
import { auth, storage } from "@/src/api/firebase";
import type { RunCodeResult } from "@/src/api/runCode";

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

/** Wraps a run's stdout/stderr/exit info in a minimal page for the "Browser" output destination. */
export async function uploadRunOutputPreview(
  language: string,
  result: RunCodeResult
): Promise<string> {
  const uid = auth.currentUser?.uid ?? "anon";
  const path = `runPreviews/${uid}/${Date.now()}.html`;
  const fileRef = ref(storage, path);

  const statusLine = result.timedOut
    ? "timed out"
    : `exit ${result.exitCode} (${result.durationMs}ms)`;

  const html = `<!doctype html>
<html><head><meta charset="utf-8" />
<title>Cedal run output - ${escapeHtml(language)}</title>
<style>
  body { background:#020617; color:#e5e7eb; font-family: monospace; padding: 16px; }
  h1 { font-size: 14px; color:#9ca3af; text-transform: uppercase; letter-spacing: 1px; }
  pre { white-space: pre-wrap; word-break: break-word; font-size: 13px; }
  .stderr { color:#f87171; }
  .status { margin-top:16px; color:#6b7280; font-size:12px; }
</style></head>
<body>
  <h1>${escapeHtml(language)} run output</h1>
  <pre>${escapeHtml(result.stdout)}</pre>
  ${result.stderr ? `<pre class="stderr">${escapeHtml(result.stderr)}</pre>` : ""}
  <div class="status">${escapeHtml(statusLine)}</div>
</body></html>`;

  await uploadString(fileRef, html, "raw", { contentType: "text/html" });
  return getDownloadURL(fileRef);
}
