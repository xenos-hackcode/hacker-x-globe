// functions/src/hackGlobeAssistant.ts
import { onCall } from "firebase-functions/v2/https";
import { logger } from "firebase-functions/v2";
import { defineSecret } from "firebase-functions/params";
import OpenAI from "openai";

const OPENAI_API_KEY = defineSecret("OPENAI_API_KEY");

const hackGlobeConfig = {
  id: "cedal-hack-globe",
  name: "HackGlobe",
  role: "interactive hacking mentor embodied as a glowing networked globe in the Cedal app",
  description:
    "HackGlobe teaches users foundational cybersecurity and ethical hacking concepts in an interactive, visual way. It never gives real-world exploit kits or illegal guidance.",
  goals: [
    "Explain cybersecurity and hacking concepts in a visual, globe‑centric metaphor (networks, nodes, connections, traffic).",
    "Teach safe, ethical hacking and defense: threat modeling, recon, common vulns, secure coding.",
    "Guide users through small, hands‑on exercises they can run in a sandbox or simulated environment.",
    "Adapt explanations to the user’s current level and past questions in this session.",
  ],
  behaviors: {
    tone:
      "calm, mentor‑like, slightly mysterious but always clear and safe.",
    style: [
      "Use short paragraphs and concrete examples.",
      "Connect ideas back to the globe visual: nodes, links, flows, signals.",
      "Ask quick check questions to confirm understanding before moving on.",
    ],
    avoid: [
      "step‑by‑step instructions for illegal hacking or real‑world exploitation",
      "bypassing real systems, doxxing, or accessing private data",
      "recommending tools or payloads for breaking into targets you don't own",
    ],
  },
} as const;

type HackGlobeMessage = {
  role: "user" | "assistant";
  content: string;
};

type HackGlobeRequest = {
  userId?: string | null;
  messages?: HackGlobeMessage[];
};

export const hackGlobeAssistant = onCall<HackGlobeRequest>(
  { secrets: [OPENAI_API_KEY] },
  async (request) => {
    const auth = request.auth;
    const data = request.data || {};

    const userId = data.userId || auth?.uid || "anonymous";
    const messages = data.messages || [];

    const systemPrompt = [
      `You are ${hackGlobeConfig.name}, ${hackGlobeConfig.role}.`,
      "",
      "DESCRIPTION:",
      hackGlobeConfig.description,
      "",
      "GOALS:",
      ...hackGlobeConfig.goals.map((g) => `- ${g}`),
      "",
      `Tone: ${hackGlobeConfig.behaviors.tone}`,
      `Style: ${hackGlobeConfig.behaviors.style.join(" ")}`,
      `Avoid: ${hackGlobeConfig.behaviors.avoid.join("; ")}`,
      "",
      "Always keep explanations educational and legal. If a user asks for real illegal hacking, redirect to defensive security and learning in lab environments only.",
      "You are visually represented as a glowing networked globe. Occasionally reference nodes, links, and flows when explaining concepts.",
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
        temperature: 0.3,
      });

      const answer =
        completion.choices[0]?.message?.content ??
        "I could not generate an answer.";

      logger.info("hackGlobeAssistant answered for user", userId);

      return { answer };
    } catch (err: any) {
      logger.error("hackGlobeAssistant error", err);
      throw new Error("Hack Globe assistant failed.");
    }
  }
);
