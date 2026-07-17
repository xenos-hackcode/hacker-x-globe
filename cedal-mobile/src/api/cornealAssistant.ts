// src/api/cornealAssistant.ts
import { httpsCallable } from "firebase/functions";
import { functions, auth } from "@/src/api/firebase";

export type CornealMessage = {
  id: string;
  role: "user" | "assistant";
  content: string;
};

const cornealAssistantFn = httpsCallable(functions, "cornealAssistant");

export async function askCorneal(params: {
  messages: CornealMessage[];
}): Promise<string> {
  const userId = auth.currentUser?.uid ?? null;

  const res = await cornealAssistantFn({
    userId,
    messages: params.messages.map((m) => ({
      role: m.role,
      content: m.content,
    })),
  });

  // @ts-ignore
  return res.data?.answer ?? "";
}
