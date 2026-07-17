// src/api/devAssistant.ts
import { httpsCallable } from "firebase/functions";
import { functions } from "@/src/api/firebase";

export type DevMessage = {
  id: string;
  role: "user" | "assistant";
  content: string;
};

const devAssistantFn = httpsCallable(functions, "devAssistant");

export async function askDevAssistant(params: {
  userId: string | null;
  messages: DevMessage[];
}): Promise<string> {
  const res = await devAssistantFn({
    userId: params.userId,
    messages: params.messages.map((m) => ({
      role: m.role,
      content: m.content,
    })),
  });

  // @ts-ignore
  return res.data?.answer ?? "";
}
