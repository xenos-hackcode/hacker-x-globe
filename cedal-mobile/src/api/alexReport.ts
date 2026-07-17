// src/api/alexReport.ts
import { httpsCallable } from "firebase/functions";
import { euFunctions, auth } from "@/src/api/firebase";

export type AlexReportResponse = {
  reportId: string;
  needsChatAccess: boolean;
  severity: "low" | "medium" | "high";
  summary: string;
  userMessage: string;
};

const alexReportFn = httpsCallable(euFunctions, "alexReport");

export async function sendReportToAlex(params: {
  chatId: string;
  targetUserId: string;
  reason: string;
}): Promise<AlexReportResponse> {
  const userId = auth.currentUser?.uid ?? null;

  const res = await alexReportFn({
    userId,
    chatId: params.chatId,
    targetUserId: params.targetUserId,
    reason: params.reason,
  });

  const data = res.data as AlexReportResponse;

  return data;
}
