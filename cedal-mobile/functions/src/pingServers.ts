// functions/src/pingServers.ts
import { onRequest } from "firebase-functions/v2/https";
import { setGlobalOptions } from "firebase-functions/v2/options";

setGlobalOptions({ region: "europe-west2" });

function withCors(
  handler: (req: any, res: any) => void,
) {
  return (req: any, res: any) => {
    res.set("Access-Control-Allow-Origin", "*");
    if (req.method === "OPTIONS") {
      res.set("Access-Control-Allow-Methods", "GET, OPTIONS");
      res.set("Access-Control-Allow-Headers", "Content-Type");
      res.status(204).send("");
      return;
    }
    handler(req, res);
  };
}

export const pingLon01 = onRequest(
  { region: "europe-west2" },
  withCors((_req, res) => {
    res.status(200).json({ ok: true, serverId: "lon-01", ts: Date.now() });
  }),
);

export const pingFra01 = onRequest(
  { region: "europe-west3" },
  withCors((_req, res) => {
    res.status(200).json({ ok: true, serverId: "fra-01", ts: Date.now() });
  }),
);

export const pingNyc01 = onRequest(
  { region: "us-east1" },
  withCors((_req, res) => {
    res.status(200).json({ ok: true, serverId: "nyc-01", ts: Date.now() });
  }),
);
