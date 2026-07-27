// gui-runner/server.js
//
// Unlike runner/server.js (one-shot execFile, request/response), this keeps
// a real process tree alive for the life of a session: Xvfb (virtual
// display) -> the user's python3 script drawing into it -> x11vnc (exposes
// that display as VNC) -> websockify (bridges VNC over a websocket, and
// also serves noVNC's static web client). Cloud Run only forwards one
// public port per service, so this Express app is the single entry point -
// it owns the control routes (/session/start, /session/:id/stop) and
// proxies everything else (noVNC's static files + the websocket upgrade)
// through to websockify's internal port.
//
// Multi-user: each instance still only ever holds one `current` session
// (concurrency=1 - a session needs a whole instance to itself), but
// deploy.sh now allows up to 10 instances plus --session-affinity, so
// different users' sessions land on different instances concurrently. The
// affinity cookie Cloud Run sets has to be relayed from cedal-server's
// server-to-server /session/start call through to the Android client (see
// GuiSessionService.kt), since that's the client that actually needs to
// stay pinned for its later page load + websocket upgrade.
const express = require("express");
const http = require("http");
const net = require("net");
const fs = require("fs/promises");
const path = require("path");
const os = require("os");
const { spawn } = require("child_process");
const { createProxyMiddleware } = require("http-proxy-middleware");

const app = express();
app.use(express.json({ limit: "256kb" }));

const SESSION_TIMEOUT_MS = 10 * 60 * 1000; // hard wall-clock cap per session
const READY_TIMEOUT_MS = 8000; // how long to wait for Xvfb/x11vnc/websockify to come up
const WEBSOCKIFY_PORT = 6080;
const VNC_PORT = 5900;
const DISPLAY = ":99";
const NOVNC_DIR = "/usr/share/novnc";
const GUI_RUNNER_SECRET = process.env.GUI_RUNNER_SECRET || "";

app.get("/health", (_req, res) => res.json({ ok: true, ts: Date.now() }));

function checkSecret(req) {
  const header = req.headers.authorization || "";
  const match = header.match(/^Bearer (.+)$/);
  return !!match && !!GUI_RUNNER_SECRET && match[1] === GUI_RUNNER_SECRET;
}

// Only one session can ever be alive on this instance (see the file-level
// comment on why max-instances is pinned to 1) - keeping a single `current`
// slot instead of a map keeps teardown unambiguous.
let current = null; // { sessionId, workDir, procs: Set<ChildProcess>, timer, stderr }

function killCurrent() {
  if (!current) return;
  clearTimeout(current.timer);
  for (const p of current.procs) {
    try {
      p.kill("SIGKILL");
    } catch {
      // already dead - fine
    }
  }
  fs.rm(current.workDir, { recursive: true, force: true }).catch(() => {});
  current = null;
}

function waitForPort(port, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  return new Promise((resolve, reject) => {
    function attempt() {
      const socket = net.connect(port, "127.0.0.1");
      socket.once("connect", () => {
        socket.destroy();
        resolve();
      });
      socket.once("error", () => {
        socket.destroy();
        if (Date.now() > deadline) {
          reject(new Error(`timed out waiting for port ${port}`));
        } else {
          setTimeout(attempt, 200);
        }
      });
    }
    attempt();
  });
}

app.post("/session/start", async (req, res) => {
  if (!checkSecret(req)) {
    return res.status(401).json({ error: "unauthorized" });
  }
  const { sessionId, code } = req.body || {};
  if (!sessionId || typeof code !== "string" || !code.trim()) {
    return res.status(400).json({ error: "missing fields" });
  }

  // A second start request just replaces whatever's currently running -
  // containerConcurrency=1 means Cloud Run never actually sends this
  // concurrently, but a client retry after a dropped response should still
  // behave sanely rather than erroring on "already busy".
  killCurrent();

  const workDir = await fs.mkdtemp(path.join(os.tmpdir(), "gui-"));
  const mainFile = path.join(workDir, "main.py");
  await fs.writeFile(mainFile, code);

  const procs = new Set();
  let stderr = "";
  try {
    const xvfb = spawn("Xvfb", [DISPLAY, "-screen", "0", "1024x768x24", "-nolisten", "tcp"]);
    procs.add(xvfb);

    const x11vnc = spawn("x11vnc", [
      "-display", DISPLAY,
      "-forever", "-shared", "-nopw", "-quiet",
      "-rfbport", String(VNC_PORT),
    ]);
    procs.add(x11vnc);

    await waitForPort(VNC_PORT, READY_TIMEOUT_MS);

    const websockify = spawn("websockify", [
      "--web", NOVNC_DIR,
      String(WEBSOCKIFY_PORT),
      `localhost:${VNC_PORT}`,
    ]);
    procs.add(websockify);

    await waitForPort(WEBSOCKIFY_PORT, READY_TIMEOUT_MS);

    // Starts only after the display chain is confirmed up, so the script's
    // first frame doesn't race Xvfb's own startup.
    const python = spawn("python3", [mainFile], {
      cwd: workDir,
      env: { ...process.env, DISPLAY },
    });
    procs.add(python);
    python.stderr.on("data", (d) => {
      if (stderr.length < 8 * 1024) stderr += d.toString();
    });
  } catch (err) {
    for (const p of procs) {
      try {
        p.kill("SIGKILL");
      } catch {
        // already dead
      }
    }
    await fs.rm(workDir, { recursive: true, force: true }).catch(() => {});
    return res.status(500).json({ error: `failed to start session: ${err.message}` });
  }

  const timer = setTimeout(killCurrent, SESSION_TIMEOUT_MS);
  current = { sessionId, workDir, procs, timer, get stderr() { return stderr; } };

  res.json({
    sessionId,
    path: "/vnc_lite.html?path=websockify&autoconnect=true&resize=scale",
    expiresInMs: SESSION_TIMEOUT_MS,
  });
});

app.post("/session/:id/stop", (req, res) => {
  if (!checkSecret(req)) {
    return res.status(401).json({ error: "unauthorized" });
  }
  if (current && current.sessionId === req.params.id) {
    killCurrent();
  }
  res.json({ ok: true });
});

app.get("/session/:id/status", (req, res) => {
  if (!checkSecret(req)) {
    return res.status(401).json({ error: "unauthorized" });
  }
  if (!current || current.sessionId !== req.params.id) {
    return res.json({ active: false });
  }
  res.json({ active: true, stderr: current.stderr });
});

// Everything else (noVNC's static assets, the /websockify upgrade) goes
// straight through to websockify - it's already a full static-file server
// plus VNC-over-websocket bridge on its own port, no need to reimplement
// either half of that here.
const proxy = createProxyMiddleware({
  target: `http://localhost:${WEBSOCKIFY_PORT}`,
  ws: true,
  changeOrigin: true,
  logLevel: "warn",
});
app.use("/", proxy);

const server = http.createServer(app);
// createProxyMiddleware's `ws: true` only rewrites the HTTP-level upgrade
// headers - the actual 'upgrade' event on the underlying http.Server still
// has to be forwarded to it explicitly, since Express itself never sees
// upgrade events (they bypass the normal request/response cycle entirely).
server.on("upgrade", (req, socket, head) => {
  proxy.upgrade(req, socket, head);
});

const port = process.env.PORT || 8080;
server.listen(port, () => {
  console.log(`gui-runner listening on ${port}`);
});
