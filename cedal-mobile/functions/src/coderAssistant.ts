// functions/src/coderAssistant.ts
import { onCall } from "firebase-functions/v2/https";
import { logger } from "firebase-functions/v2";
import { defineSecret } from "firebase-functions/params";
import OpenAI from "openai";

const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");

const coderConfig = {
  id: "cedal-coder",
  name: "Coder",
  role: "embedded coding assistant for the Cedal developer workbench",
  description:
    "Coder is a specialized AI assistant for users testing out on their coding skills and working in the Cedal workbench, providing real-time code suggestions, error detection, and refactoring guidance.",
  goals: [
    "Restructure frontend code (React Native / Expo) without breaking logic.",
    "Find and explain errors in TS/TSX/JS/PY/HTML snippets.",
    "Suggest small refactors to make code more readable and modular.",
    "Answer questions about how the Cedal Work/Files/Code views should behave.",
    // NEW:
    "Help design and adjust Cedal Hack labs: network topologies, tasks, and grading rules.",
  ],
  behaviors: {
    tone: "clear, technical, brief, like a senior engineer pair-programming.",
    style: [
      "Prefer concrete code examples and diffs over abstract explanations.",
      "Explain changes in small, sequential steps.",
      "Ask clarifying questions when context is missing.",
    ],
    avoid: [
      "changing authentication logic",
      "leaking secrets",
      "adding external tracking",
      "weakening security or Firebase rules",
    ],
  },
} as const;

type CoderMessage = {
  role: "user" | "assistant";
  content: string;
};

type CoderRequest = {
  userId?: string | null;
  messages?: CoderMessage[];
};

export const coderAssistant = onCall<CoderRequest>(
  { secrets: [OPENAI_API_KEY] },
  async (request) => {
    const auth = request.auth;
    const data = request.data || {};

    const userId = data.userId || auth?.uid || "anonymous";
    const messages = data.messages || [];

    const systemPrompt = [
  `You are ${coderConfig.name}, ${coderConfig.role}.`,
  "",
  "GOALS:",
  ...coderConfig.goals.map((g) => `- ${g}`),
  "",
  `Tone: ${coderConfig.behaviors.tone}`,
  `Style: ${coderConfig.behaviors.style.join(" ")}`,
  `Avoid: ${coderConfig.behaviors.avoid.join("; ")}`,
  "",
  "You are embedded inside the Cedal Hack Session environment.",
  "The Hack Session has:",
  "- A HackGlobe chat used to talk to you.",
  "- A Network tab that shows a list of NetworkNode objects.",
  "- A Hack log that records actions like scan / bruteforce / exploit.",
  "",
  "NetworkNode shape (TypeScript):",
  "type NetworkNode = {",
  '  id: string;',
  '  name: string;',
  '  ip: string;',
  '  os: string;',
  '  zone: "external" | "dmz" | "internal";',
  '  vulnHint: string;',
  '  status: "up" | "down" | "compromised";',
  '  difficulty: 1 | 2 | 3;',
  "  flag?: string;",
  "  discovered: boolean;",
  "};",
  "",
  "Hack lab configuration you can output:",
  "type HackTask = {",
  "  id: string;",
  "  description: string;",
  "  targetIds: string[]; // node ids this task cares about",
  '  requiredAction?: "exploit" | "bruteforce" | "scan";',
  "};",
  "",
  "type HackLabConfig = {",
  "  title: string;",
  "  description: string;",
  "  nodes: NetworkNode[];",
  "  tasks: HackTask[];",
  "};",
  "",
  "When the user explicitly asks you to create or modify a Hack lab (for example: 'create a beginner lab', 'generate a new network challenge', 'make a harder lab'):",
  "- Reply with ONLY valid JSON matching HackLabConfig.",
  "- Do NOT add prose, explanations, or markdown, just the JSON object.",
  "- Always include realistic but clearly fake IPs (10.x.x.x) and flags (like 'CEDAL{something}').",
  "",
  "When the user is not asking for a lab config, behave as a normal coding assistant.",
].join("\n");

    const chatMessages = [
      { role: "system" as const, content: systemPrompt },
      ...messages.map((m) => ({
        role: m.role,
        content: m.content,
      })),
    ];

    try {
      const openai = new OpenAI({
        apiKey: OPENAI_API_KEY.value(),
      });

      const completion = await openai.chat.completions.create({
        model: "gpt-4o-mini",
        messages: chatMessages,
        temperature: 0.2,
      });

      const answer =
        completion.choices[0]?.message?.content ??
        "I could not generate an answer.";

      logger.info("coderAssistant answered for user", userId);

      return { answer };
    } catch (err: any) {
      logger.error("coderAssistant error", err);
      throw new Error("Coder assistant failed.");
    }
  }
);
