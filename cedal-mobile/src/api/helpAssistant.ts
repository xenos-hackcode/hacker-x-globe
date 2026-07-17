// src/api/helpAssistant.ts
import { httpsCallable } from "firebase/functions";
import { functions } from "@/src/api/firebase";

export type HelpMessage = {
  id: string;
  role: "user" | "assistant";
  content: string;
};

type AskHelpPayload = {
  userId?: string | null;
  messages: Omit<HelpMessage, "id">[];
};

export async function askHelpAssistant(payload: AskHelpPayload) {
  const fn = httpsCallable(functions, "helpAssistant");
  const res = await fn({
    userId: payload.userId ?? null,
    messages: payload.messages,
  });
  return (res.data as any).answer as string;
}
