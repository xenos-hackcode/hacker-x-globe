// src/api/botAssistant.ts
import { httpsCallable } from "firebase/functions";
import { functions } from "@/src/api/firebase";

export type BotMessage = {
  id: string;
  role: "user" | "assistant";
  content: string;
};

const botAssistantFn = httpsCallable(functions, "botAssistant");

export async function askBotAssistant(params: {
  userId: string | null;
  messages: BotMessage[];
}): Promise<string> {
  const res = await botAssistantFn({
    userId: params.userId,
    messages: params.messages.map((m) => ({
      role: m.role,
      content: m.content,
    })),
  });

  // @ts-ignore
  return res.data?.answer ?? "";
}
