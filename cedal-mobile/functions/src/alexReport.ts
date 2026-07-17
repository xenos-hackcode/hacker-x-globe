// functions/src/alexReport.ts
import { onCall, HttpsError } from "firebase-functions/v2/https";
import { logger } from "firebase-functions/v2";
import { defineSecret } from "firebase-functions/params";
import OpenAI from "openai";
import { getFirestore } from "firebase-admin/firestore";

const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");

type AlexReportRequest = {
  userId?: string | null;
  chatId?: string;
  targetUserId?: string;
  reason?: string;
};

type AlexReportResponse = {
  reportId: string;
  needsChatAccess: boolean;
  severity: "low" | "medium" | "high";
  summary: string;
  userMessage: string;
};

const alexConfig = {
  name: "Alex",
  role: "background moderation and safety AI for the Cedal app",
  rules: [
    "Alex receives user abuse reports and decides whether deeper chat review is needed.",
    "Alex never reveals internal implementation details.",
    "Alex writes short, clear summaries of the issue and suggested action.",
    "Alex can request access to chat history, but final decision may require Xenos (a higher-level AI).",
  ],
};

export const alexReport = onCall<AlexReportRequest>(
  { secrets: [OPENAI_API_KEY] },
  async (request): Promise<AlexReportResponse> => {
    const auth = request.auth;
    const data = request.data || {};

    const reporterId = data.userId || auth?.uid;
    const chatId = data.chatId;
    const targetUserId = data.targetUserId;
    const reason = (data.reason || "").trim();

    if (!reporterId) {
      throw new HttpsError("unauthenticated", "Must be authenticated");
    }
    if (!chatId || !targetUserId || !reason) {
      throw new HttpsError(
        "invalid-argument",
        "chatId, targetUserId and reason are required"
      );
    }

    const db = getFirestore();

    // 1) Create a report doc in Firestore
    const reportRef = await db.collection("reports").add({
      reporterId,
      targetUserId,
      chatId,
      reason,
      status: "pending", // Alex will analyze and may ask for consent later
      createdAt: new Date(),
      updatedAt: new Date(),
    });

    // 2) Ask OpenAI (Alex) for an initial classification/summary
    const openai = new OpenAI({ apiKey: OPENAI_API_KEY.value() });

    const systemPrompt = [
      `You are ${alexConfig.name}, ${alexConfig.role}.`,
      "",
      "RULES:",
      ...alexConfig.rules.map((r) => `- ${r}`),
      "",
      "You are processing a user report about chat behavior.",
      "You only know the report text right now, not the full chat.",
      "Decide whether chat review is likely needed and write a very short summary.",
      "",
      "Your response MUST be JSON with fields:",
      `{"needsChatAccess": boolean, "severity": "low"|"medium"|"high", "summary": string, "userMessage": string}`,
      "",
      'userMessage should be what you say directly to the reporter, either thanking them or asking ONE short follow-up question.',
    ].join("\n");

    const completion = await openai.chat.completions.create({
      model: "gpt-4o-mini",
      messages: [
        { role: "system", content: systemPrompt },
        {
          role: "user",
          content: `ReporterId: ${reporterId}\nTargetUserId: ${targetUserId}\nChatId: ${chatId}\nReason: ${reason}`,
        },
      ],
      temperature: 0.2,
    });

    const raw = completion.choices[0]?.message?.content || "{}";

    let parsed: any = {};
    try {
      parsed = JSON.parse(raw);
    } catch (e) {
      logger.warn("alexReport JSON parse failed, raw:", raw);
      parsed = {
        needsChatAccess: true,
        severity: "medium",
        summary: "AI could not parse its own output, treat as medium severity.",
        userMessage:
          "Thanks for your report. I will treat this as medium severity and may need more information.",
      };
    }

    const needsChatAccess = !!parsed.needsChatAccess;
    const severity =
      parsed.severity === "low" ||
      parsed.severity === "medium" ||
      parsed.severity === "high"
        ? parsed.severity
        : "medium";
    const summary = parsed.summary || "No summary";
    const userMessage =
      parsed.userMessage ||
      (needsChatAccess
        ? "Thanks for your report. I may need your consent to review more of the chat if this looks serious."
        : "Thanks for your report. I’ve recorded it and will flag this conversation for review.");

    // 3) Update report with Alex's initial view
    await reportRef.update({
      alexInitial: {
        needsChatAccess,
        severity,
        summary,
      },
      status: needsChatAccess ? "needs_consent" : "resolved",
      updatedAt: new Date(),
    });

    logger.info("alexReport created", {
      reportId: reportRef.id,
      reporterId,
      targetUserId,
      needsChatAccess,
    });

    // 4) Return everything the client needs
    return {
      reportId: reportRef.id,
      needsChatAccess,
      severity,
      summary,
      userMessage,
    };
  }
);
