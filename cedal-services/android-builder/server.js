// android-builder/server.js
const express = require("express");
const fs = require("fs/promises");
const path = require("path");
const os = require("os");
const crypto = require("crypto");
const { spawn } = require("child_process");
const admin = require("firebase-admin");

admin.initializeApp({ storageBucket: "cedal-fd4a2.firebasestorage.app" });

const app = express();
app.use(express.json({ limit: "256kb" }));

const TEMPLATE_DIR = "/app/template";
const PACKAGE_NAME = "com.cedal.generated";
const BUILD_TIMEOUT_MS = 5 * 60 * 1000;
const BUILD_SECRET = process.env.ANDROID_BUILDER_SECRET || "";

app.get("/health", (_req, res) => {
  res.json({ ok: true, ts: Date.now() });
});

function checkSecret(req) {
  const header = req.headers.authorization || "";
  const match = header.match(/^Bearer (.+)$/);
  return !!match && !!BUILD_SECRET && match[1] === BUILD_SECRET;
}

// Uses spawn (not execFile) with detached:true so the timeout can kill the
// *whole process group*, not just the immediate "gradle" wrapper script -
// execFile's own built-in timeout only signals the direct child, which
// silently fails to actually stop anything if that child hands real work
// off to a detached grandchild (e.g. a background Gradle daemon process),
// leaving the parent's stdio pipes open and the await hanging forever with
// no error, no timeout, nothing - exactly the failure mode this project hit.
function runProcess(cmd, args, cwd) {
  return new Promise((resolve) => {
    const child = spawn(cmd, args, { cwd, detached: true });
    let stdout = "";
    let stderr = "";
    let timedOut = false;
    child.stdout.on("data", (d) => {
      if (stdout.length < 4 * 1024 * 1024) stdout += d.toString();
    });
    child.stderr.on("data", (d) => {
      if (stderr.length < 4 * 1024 * 1024) stderr += d.toString();
    });
    const timer = setTimeout(() => {
      timedOut = true;
      try {
        process.kill(-child.pid, "SIGKILL");
      } catch (err) {
        console.error("failed to kill process group", err);
      }
    }, BUILD_TIMEOUT_MS);
    child.on("close", (code) => {
      clearTimeout(timer);
      resolve({ exitCode: code ?? 1, stdout, stderr, timedOut });
    });
    child.on("error", (err) => {
      clearTimeout(timer);
      resolve({ exitCode: 1, stdout, stderr: stderr + `\n${err.message}`, timedOut });
    });
  });
}

async function updateJob(jobId, fields) {
  await admin
    .firestore()
    .collection("androidBuilds")
    .doc(jobId)
    .set(fields, { merge: true });
}

// cedal-mobile (Firebase-based) has no callbackUrl - keeps using the
// Firestore job doc, exactly as before. cedal-server (its own Postgres job
// table, no Firestore) passes a callbackUrl instead, so status updates go
// there and Firestore is skipped entirely for that job.
async function reportUpdate(jobId, callbackUrl, fields) {
  if (callbackUrl) {
    try {
      // A hanging fetch (no response, connection never closes) would
      // otherwise stall runBuild's whole await chain forever - this was a
      // real, silent failure mode with no logging to reveal it before.
      await fetch(callbackUrl, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${BUILD_SECRET}`,
        },
        body: JSON.stringify(fields),
        signal: AbortSignal.timeout(15_000),
      });
    } catch (err) {
      console.error(`[${jobId}] callback failed`, err);
    }
    return;
  }
  await updateJob(jobId, fields);
}

app.post("/build", (req, res) => {
  if (!checkSecret(req)) {
    return res.status(401).json({ error: "unauthorized" });
  }

  const { jobId, uid, code, callbackUrl } = req.body || {};
  if (!jobId || !uid || typeof code !== "string" || !code.trim()) {
    return res.status(400).json({ error: "missing fields" });
  }

  // Acknowledge immediately - the build itself takes minutes and reports
  // progress via the job doc / callback instead of this response.
  res.status(202).json({ ok: true });

  runBuild(jobId, uid, code, callbackUrl).catch(async (err) => {
    console.error("build failed", err);
    await reportUpdate(jobId, callbackUrl, {
      status: "error",
      errorMessage: String(err?.message || err),
    }).catch(() => {});
  });
});

async function runBuild(jobId, uid, code, callbackUrl) {
  console.log(`[${jobId}] runBuild starting`);
  await reportUpdate(jobId, callbackUrl, { status: "building" });
  console.log(`[${jobId}] reported status=building`);

  const workDir = await fs.mkdtemp(path.join(os.tmpdir(), "build-"));
  const projectDir = path.join(workDir, "proj");
  console.log(`[${jobId}] workDir=${workDir}`);

  try {
    await fs.cp(TEMPLATE_DIR, projectDir, { recursive: true });
    console.log(`[${jobId}] template copied`);

    // Always force the package to match where this file physically lives
    // (app/src/main/java/com/cedal/generated/) - Kotlin, unlike Java,
    // doesn't require a source file's package to match its folder location,
    // so previously-only-prepending when the code had NO package statement
    // let any user code with its own `package com.whatever` compile fine
    // under that package while AndroidManifest.xml (part of the fixed
    // template) still expected com.cedal.generated.MainActivity - a
    // guaranteed ClassNotFoundException crash on launch, not a build
    // failure, so it looked like a successful build every time.
    const codeWithoutPackageLine = code.replace(/^\s*package\s+[\w.]+\s*\n?/, "");
    const source = `package ${PACKAGE_NAME}\n\n${codeWithoutPackageLine.trimStart()}`;

    const activityPath = path.join(
      projectDir,
      "app/src/main/java/com/cedal/generated/MainActivity.kt"
    );
    await fs.writeFile(activityPath, source);
    console.log(`[${jobId}] source written, starting gradle`);

    // Daemon left on deliberately (not --no-daemon): this service runs with
    // containerConcurrency=1, so only one build ever runs per container
    // instance at a time - no risk of multiple daemons piling up
    // concurrently. A warm daemon skips JVM+Gradle startup cost on every
    // build after the first one on a given (still-warm) instance, which is
    // the single biggest reason this is slower than a local Android Studio
    // build. Each build still gets its own fresh projectDir (mkdtemp above),
    // so builds never see stale state from a previous one.
    const buildResult = await runProcess(
      "gradle",
      ["assembleDebug"],
      projectDir
    );
    console.log(`[${jobId}] gradle finished exitCode=${buildResult.exitCode} timedOut=${buildResult.timedOut}`);

    if (buildResult.exitCode !== 0 || buildResult.timedOut) {
      console.log(`[${jobId}] reporting error status`);
      await reportUpdate(jobId, callbackUrl, {
        status: "error",
        errorMessage: buildResult.timedOut
          ? "Build timed out"
          : buildResult.stderr.slice(-4000) || buildResult.stdout.slice(-4000),
      });
      console.log(`[${jobId}] error status reported`);
      return;
    }

    const apkPath = path.join(
      projectDir,
      "app/build/outputs/apk/debug/app-debug.apk"
    );
    const apkBuffer = await fs.readFile(apkPath);
    console.log(`[${jobId}] apk read, size=${apkBuffer.length}`);

    const storagePath = `androidBuilds/${uid}/${jobId}.apk`;
    const bucket = admin.storage().bucket();
    const file = bucket.file(storagePath);
    const token = crypto.randomUUID();

    await file.save(apkBuffer, {
      contentType: "application/vnd.android.package-archive",
      metadata: { metadata: { firebaseStorageDownloadTokens: token } },
    });
    console.log(`[${jobId}] apk uploaded to storage`);

    const downloadUrl = `https://firebasestorage.googleapis.com/v0/b/${bucket.name}/o/${encodeURIComponent(
      storagePath
    )}?alt=media&token=${token}`;

    await reportUpdate(jobId, callbackUrl, { status: "done", downloadUrl });
    console.log(`[${jobId}] reported status=done`);
  } finally {
    await fs.rm(workDir, { recursive: true, force: true }).catch(() => {});
    console.log(`[${jobId}] workDir cleaned up`);
  }
}

const port = process.env.PORT || 8080;
app.listen(port, () => {
  console.log(`android-builder listening on ${port}`);
});
