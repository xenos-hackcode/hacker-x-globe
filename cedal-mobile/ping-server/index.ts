// ping-server/index.ts
import express from "express";

const app = express();

app.get("/ping", (_req, res) => {
  res.status(200).json({ ok: true, ts: Date.now() });
});

const port = process.env.PORT || 8080;
app.listen(port, () => {
  console.log("Ping server listening on", port);
});
