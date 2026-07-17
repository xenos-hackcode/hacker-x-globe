// src/api/coderAssistant.ts
import { httpsCallable } from "firebase/functions";
import { functions, auth } from "@/src/api/firebase";

export type CoderMessage = {
  id: string;
  role: "user" | "assistant";
  content: string;
};

const coderAssistantFn = httpsCallable(functions, "coderAssistant");

export async function askCoderAssistant(params: {
  messages: CoderMessage[];
}): Promise<string> {
  const userId = auth.currentUser?.uid ?? null;

  const res = await coderAssistantFn({
    userId,
    messages: params.messages.map((m) => ({
      role: m.role,
      content: m.content,
    })),
  });

  // @ts-ignore
  return res.data?.answer ?? "";
}
