// runner-compiled/server.js
const express = require("express");
const fs = require("fs/promises");
const path = require("path");
const os = require("os");
const { execFile } = require("child_process");
const admin = require("firebase-admin");

admin.initializeApp();

const app = express();
app.use(express.json({ limit: "256kb" }));

const MAX_OUTPUT = 64 * 1024;
const TIMEOUT_MS = 40000;
const MAX_CODE_LENGTH = 64 * 1024;
const CSHARP_TEMPLATE = "/app/csharp-template";

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

function runProcess(cmd, args, cwd, input) {
  return new Promise((resolve) => {
    const child = execFile(
      cmd,
      args,
      {
        cwd,
        timeout: TIMEOUT_MS,
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

async function compileThenRun(compileCmd, compileArgs, runCmd, runArgs, cwd, input) {
  const compileResult = await runProcess(compileCmd, compileArgs, cwd);
  if (compileResult.exitCode !== 0 || compileResult.timedOut) {
    return compileResult;
  }
  return runProcess(runCmd, runArgs, cwd, input);
}

app.post("/run", async (req, res) => {
  const uid = await verifyAuth(req);
  if (!uid) {
    return res.status(401).json({ error: "unauthorized" });
  }

  const { language, code, stdin, extraFiles } = req.body || {};
  if (typeof code !== "string" || !code.trim()) {
    return res.status(400).json({ error: "missing code" });
  }
  if (code.length > MAX_CODE_LENGTH) {
    return res.status(400).json({ error: "code too large" });
  }

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

    let result;
    if (language === "C") {
      const file = path.join(workDir, "main.c");
      const bin = path.join(workDir, "a.out");
      await fs.writeFile(file, code);
      result = await compileThenRun("gcc", [file, "-o", bin], bin, [], workDir, stdin);
    } else if (language === "C++") {
      const file = path.join(workDir, "main.cpp");
      const bin = path.join(workDir, "a.out");
      await fs.writeFile(file, code);
      result = await compileThenRun("g++", [file, "-o", bin], bin, [], workDir, stdin);
    } else if (language === "Rust") {
      const file = path.join(workDir, "main.rs");
      const bin = path.join(workDir, "a.out");
      await fs.writeFile(file, code);
      result = await compileThenRun("rustc", [file, "-o", bin], bin, [], workDir, stdin);
    } else if (language === "Go") {
      const file = path.join(workDir, "main.go");
      await fs.writeFile(file, code);
      result = await runProcess("go", ["run", file], workDir, stdin);
    } else if (language === "Java") {
      const file = path.join(workDir, "Main.java");
      await fs.writeFile(file, code);
      result = await runProcess("java", [file], workDir, stdin);
    } else if (language === "C#") {
      const projectDir = path.join(workDir, "proj");
      await fs.cp(CSHARP_TEMPLATE, projectDir, { recursive: true });
      await fs.writeFile(path.join(projectDir, "Program.cs"), code);
      result = await runProcess(
        "dotnet",
        ["run", "--project", projectDir, "-c", "Release", "--no-restore"],
        projectDir,
        stdin
      );
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
  console.log(`code-runner-compiled listening on ${port}`);
});
