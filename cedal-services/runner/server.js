// runner/server.js
const express = require("express");
const fs = require("fs/promises");
const path = require("path");
const os = require("os");
const { execFile, execFileSync } = require("child_process");
const admin = require("firebase-admin");
const ts = require("typescript");

admin.initializeApp();

const app = express();
app.use(express.json({ limit: "256kb" }));

const MAX_OUTPUT = 64 * 1024;
const TIMEOUT_MS = 8000;
const MAX_CODE_LENGTH = 64 * 1024;

// A separate, longer budget for the optional live-install step (see
// installPackages below) - not extending the normal fast path, just giving
// the explicit "I asked for packages" path realistic room. Most of the
// common ones (numpy, pandas, requests, pillow for Python; lodash, axios,
// dayjs for JS; httparty for Ruby; guzzlehttp/guzzle for PHP) are already
// pre-baked into the image at build time (see Dockerfile) and need no
// install at all - this only runs for anything NOT already covered.
const INSTALL_TIMEOUT_MS = 25000;
// execFile/spawn already take an args array, not a shell string, so
// metacharacter injection isn't the real risk here - this exists to reject
// path traversal and keep things sane, not to defend against shell syntax.
const PACKAGE_NAME_RE = /^[A-Za-z0-9._@/-]+$/;

const PHP_VENDOR_DIR = "/opt/php-vendor/vendor";

// Cached once at boot (gem environment doesn't change at runtime) - needed
// so a Ruby run with live-installed gems can see BOTH those and the
// pre-baked system ones (see installPackages/runProcess's env handling for
// Ruby below). Ruby's GEM_PATH, when set, replaces rather than appends to
// the default, so the default has to be captured and re-included explicitly.
let defaultGemPath = "";
try {
  defaultGemPath = execFileSync("gem", ["environment", "gempath"]).toString().trim();
} catch (err) {
  console.error("failed to read default gem path", err);
}

app.get("/health", (_req, res) => {
  res.json({ ok: true, ts: Date.now() });
});

const RUNNER_SERVICE_SECRET = process.env.RUNNER_SERVICE_SECRET || "";

async function verifyAuth(req) {
  const header = req.headers.authorization || "";
  const match = header.match(/^Bearer (.+)$/);
  if (!match) return null;

  // Trusted server-to-server callers (e.g. cedal-server) authenticate with a
  // shared secret instead of a per-user Firebase ID token.
  if (RUNNER_SERVICE_SECRET && match[1] === RUNNER_SERVICE_SECRET) {
    return "service";
  }

  try {
    const decoded = await admin.auth().verifyIdToken(match[1]);
    return decoded.uid;
  } catch {
    return null;
  }
}

function runProcess(cmd, args, cwd, input, opts = {}) {
  return new Promise((resolve) => {
    const child = execFile(
      cmd,
      args,
      {
        cwd,
        env: opts.env ? { ...process.env, ...opts.env } : process.env,
        timeout: opts.timeoutMs || TIMEOUT_MS,
        maxBuffer: MAX_OUTPUT,
        killSignal: "SIGKILL",
      },
      (error, stdout, stderr) => {
        resolve({
          stdout: (stdout || "").toString().slice(0, MAX_OUTPUT),
          stderr: (stderr || "").toString().slice(0, MAX_OUTPUT),
          exitCode:
            error && typeof error.code === "number" ? error.code : error ? 1 : 0,
          timedOut: !!(error && error.killed && error.signal === "SIGKILL"),
        });
      }
    );
    if (child.stdin) {
      if (input) child.stdin.write(String(input));
      child.stdin.end();
    }
  });
}

// Explicit opt-in only (a `packages` field the caller has to actually set)
// - never auto-parsed from import/require statements, same "deliberate
// action, not a silent guess" reasoning as gui-runner's Live View button.
// Installs are scoped to this run's own ephemeral workDir, never anywhere
// global or persisted - consistent with the existing "no shared filesystem
// between runs" property; nothing here survives past this one request.
async function installPackages(language, packages, workDir) {
  for (const name of packages) {
    if (typeof name !== "string" || !PACKAGE_NAME_RE.test(name)) {
      return { ok: false, stderr: `Rejected package name: ${JSON.stringify(name)}` };
    }
  }
  if (packages.length === 0) return { ok: true, stderr: "" };

  let result;
  if (language === "Python") {
    result = await runProcess("pip3", ["install", "--target", workDir, ...packages], workDir, null, {
      timeoutMs: INSTALL_TIMEOUT_MS,
    });
  } else if (language === "JavaScript" || language === "TypeScript") {
    result = await runProcess("npm", ["install", "--no-save", "--prefix", workDir, ...packages], workDir, null, {
      timeoutMs: INSTALL_TIMEOUT_MS,
    });
  } else if (language === "Ruby") {
    const gemDir = path.join(workDir, "gems");
    result = await runProcess(
      "gem",
      ["install", "--install-dir", gemDir, "--no-document", ...packages],
      workDir,
      null,
      { timeoutMs: INSTALL_TIMEOUT_MS },
    );
  } else if (language === "PHP") {
    result = await runProcess(
      "composer",
      ["require", "--no-interaction", "--working-dir", workDir, ...packages],
      workDir,
      null,
      { timeoutMs: INSTALL_TIMEOUT_MS },
    );
  } else {
    return { ok: false, stderr: `${language} doesn't support package installs` };
  }

  if (result.timedOut) return { ok: false, stderr: "Package install timed out" };
  if (result.exitCode !== 0) return { ok: false, stderr: result.stderr || result.stdout || "Package install failed" };
  return { ok: true, stderr: "" };
}

app.post("/run", async (req, res) => {
  const uid = await verifyAuth(req);
  if (!uid) {
    return res.status(401).json({ error: "unauthorized" });
  }

  const { language, code, stdin, extraFiles, packages } = req.body || {};
  if (typeof code !== "string" || !code.trim()) {
    return res.status(400).json({ error: "missing code" });
  }
  if (code.length > MAX_CODE_LENGTH) {
    return res.status(400).json({ error: "code too large" });
  }
  const pkgList = Array.isArray(packages) ? packages.filter((p) => typeof p === "string" && p.trim()) : [];

  const workDir = await fs.mkdtemp(path.join(os.tmpdir(), "run-"));
  const started = Date.now();

  try {
    if (Array.isArray(extraFiles)) {
      for (const f of extraFiles) {
        if (!f || typeof f.name !== "string" || typeof f.content !== "string") continue;
        const dest = path.join(workDir, f.name);
        if (!dest.startsWith(workDir)) continue; // guard against path traversal
        await fs.mkdir(path.dirname(dest), { recursive: true });
        await fs.writeFile(dest, f.content);
      }
    }

    // PHP always gets the pre-baked vendor/ (guzzlehttp/guzzle + its
    // autoloader) copied in, live packages or not - cheap (a directory
    // copy, not a fresh install) and means `require __DIR__.'/vendor/
    // autoload.php'` always works.
    if (language === "PHP") {
      await fs.cp(PHP_VENDOR_DIR, path.join(workDir, "vendor"), { recursive: true }).catch(() => {});
    }

    if (pkgList.length > 0) {
      const installResult = await installPackages(language, pkgList, workDir);
      if (!installResult.ok) {
        res.json({ stdout: "", stderr: installResult.stderr, exitCode: 1, durationMs: Date.now() - started });
        return;
      }
    }

    let result;
    if (language === "JavaScript") {
      const file = path.join(workDir, "main.js");
      await fs.writeFile(file, code);
      // Live-installed packages land in workDir/node_modules, which
      // require() already checks by default for a script that lives in
      // workDir - only the pre-baked global set needs NODE_PATH (set at
      // the container level, see Dockerfile).
      result = await runProcess("node", [file], workDir, stdin);
    } else if (language === "TypeScript") {
      const transpiled = ts.transpileModule(code, {
        compilerOptions: {
          module: ts.ModuleKind.CommonJS,
          target: ts.ScriptTarget.ES2019,
        },
      }).outputText;
      const file = path.join(workDir, "main.js");
      await fs.writeFile(file, transpiled);
      result = await runProcess("node", [file], workDir, stdin);
    } else if (language === "Python") {
      const file = path.join(workDir, "main.py");
      await fs.writeFile(file, code);
      // python3 <script> puts the script's own directory (workDir) at the
      // front of sys.path automatically - pip installing --target workDir
      // above is enough for import to find them, no PYTHONPATH needed.
      result = await runProcess("python3", [file], workDir, stdin);
    } else if (language === "PHP") {
      const file = path.join(workDir, "main.php");
      await fs.writeFile(file, code);
      result = await runProcess("php", [file], workDir, stdin);
    } else if (language === "Ruby") {
      const file = path.join(workDir, "main.rb");
      await fs.writeFile(file, code);
      // Only override GEM_PATH when this run actually live-installed gems -
      // otherwise leave Ruby's own default alone so the pre-baked system
      // gems (httparty, see Dockerfile) keep resolving normally.
      const hasLiveGems = pkgList.length > 0;
      result = await runProcess("ruby", [file], workDir, stdin, {
        env: hasLiveGems ? { GEM_PATH: `${path.join(workDir, "gems")}:${defaultGemPath}` } : undefined,
      });
    } else if (language === "Lua") {
      const file = path.join(workDir, "main.lua");
      await fs.writeFile(file, code);
      result = await runProcess("lua5.4", [file], workDir, stdin);
    } else if (language === "Bash") {
      const file = path.join(workDir, "main.sh");
      await fs.writeFile(file, code);
      result = await runProcess("bash", [file], workDir, stdin);
} else if (language === "Kotlin") {
  const file = path.join(workDir, "Main.kt");
  await fs.writeFile(file, code);
  // Compile Kotlin source to a runnable JAR
  await runProcess("kotlinc", [file, "-include-runtime", "-d", "main.jar"], workDir, null);
  // Execute the compiled program
  result = await runProcess("kotlin", ["-classpath", "main.jar", "MainKt"], workDir, stdin);
}
    } else {
      return res.status(400).json({ error: "unsupported language" });
    }

    res.json({ ...result, durationMs: Date.now() - started });
  } catch {
    res.status(500).json({ error: "execution failed" });
  } finally {
    await fs.rm(workDir, { recursive: true, force: true }).catch(() => {});
  }
});

const port = process.env.PORT || 8080;
app.listen(port, () => {
  console.log(`code-runner listening on ${port}`);
});
