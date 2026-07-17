// server/audioServer.ts
import express, { Request, Response } from "express";
import multer from "multer";
import cors from "cors";
import { tmpdir } from "os";
import { join } from "path";
import { promises as fs } from "fs";
import { spawn } from "child_process";
import { v4 as uuid } from "uuid";

const app = express();
app.use(cors());

const upload = multer({ dest: tmpdir() });

type EditConfig = {
  startMs?: number;
  endMs?: number;
  noiseReduction?: boolean;
  normalize?: boolean;
};

app.post(
  "/audio/process",
  upload.single("file"),
  async (req: Request, res: Response) => {
    try {
      const file = req.file;
      if (!file) {
        return res.status(400).send("No file uploaded");
      }

      const editConfigRaw = req.body.editConfig;
      let editConfig: EditConfig | null = null;
      if (editConfigRaw) {
        try {
          editConfig = JSON.parse(editConfigRaw);
        } catch {
          editConfig = null;
        }
      }

      const inputPath = file.path;
      const outputPath = join(tmpdir(), `${uuid()}.m4a`);

      const startSec =
        editConfig && typeof editConfig.startMs === "number"
          ? editConfig.startMs / 1000
          : 0;

      const endSec =
        editConfig && typeof editConfig.endMs === "number"
          ? editConfig.endMs / 1000
          : null;

      const durationSec =
        endSec != null ? Math.max(endSec - startSec, 0.1) : null;

      const filters: string[] = [];

      if (editConfig?.noiseReduction) {
        filters.push("highpass=f=120", "agate=threshold=-30dB:ratio=4");
      }

      if (editConfig?.normalize) {
        filters.push("loudnorm=I=-16:TP=-1.5:LRA=11");
      }

      const af = filters.length ? ["-af", filters.join(",")] : [];

      const args = [
        "-y",
        ...(startSec ? ["-ss", String(startSec)] : []),
        "-i",
        inputPath,
        ...(durationSec ? ["-t", String(durationSec)] : []),
        ...af,
        "-c:a",
        "aac",
        "-b:a",
        "96k",
        outputPath,
      ];

      const ff = spawn("ffmpeg", args);
      let stderr = "";
      ff.stderr.on("data", (d) => {
        stderr += d.toString();
      });

      ff.on("close", async (code) => {
        try {
          if (code !== 0) {
            console.error("ffmpeg failed", stderr);
            await fs.unlink(inputPath).catch(() => {});
            await fs.unlink(outputPath).catch(() => {});
            return res.status(500).send("ffmpeg error");
          }

          const publicUrl = await fakeUploadAndGetUrl(outputPath);

          await fs.unlink(inputPath).catch(() => {});
          await fs.unlink(outputPath).catch(() => {});

          return res.json({ url: publicUrl });
        } catch (err: any) {
          console.error("post-ffmpeg error", err?.message);
          return res.status(500).send("upload failed");
        }
      });
    } catch (err: any) {
      console.error("audio process handler error", err?.message);
      return res.status(500).send("server error");
    }
  }
);

async function fakeUploadAndGetUrl(localPath: string): Promise<string> {
  // For initial testing you can:
  // - Move file into a public 'static' folder and return its URL
  // - Or integrate Firebase Storage/S3 here.
  return "https://your-api.com/static/example.m4a";
}

const PORT = process.env.PORT || 4000;
app.listen(PORT, () => {
  console.log(`Audio server listening on http://localhost:${PORT}`);
});
