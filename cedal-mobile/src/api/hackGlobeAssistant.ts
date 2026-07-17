// src/api/hackGlobeAssistant.ts
import { httpsCallable } from "firebase/functions";
import { functions, auth } from "@/src/api/firebase";

export type HackGlobeMessage = {
  id: string;
  role: "user" | "assistant";
  content: string;
};

const hackGlobeFn = httpsCallable(functions, "hackGlobeAssistant");

export async function askHackGlobe(params: {
  messages: HackGlobeMessage[];
}): Promise<string> {
  const userId = auth.currentUser?.uid ?? null;

  const res = await hackGlobeFn({
    userId,
    messages: params.messages.map((m) => ({
      role: m.role,
      content: m.content,
    })),
  });

  // @ts-ignore
  return res.data?.answer ?? "";
}
