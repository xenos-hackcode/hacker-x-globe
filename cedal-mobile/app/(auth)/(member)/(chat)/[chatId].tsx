// app/(auth)/(member)/(chat)/[chatId].tsx
import React from "react";
import { useLocalSearchParams, useRouter } from "expo-router";
import ChatScreen from "@/src/member/chat/ChatScreen";
import { UnlockChatGate } from "@/src/member/chat/partials/UnlockChatGate";

type Params = {
  chatId: string | string[];
  chatName?: string | string[];
  userId?: string | string[];
};

function toStr(v?: string | string[]) {
  if (Array.isArray(v)) return v[0];
  return v;
}

export default function ChatRoute() {
  const router = useRouter();
  const params = useLocalSearchParams<Params>();

  const chatId = toStr(params.chatId);
  const chatName = toStr(params.chatName);
  const userId = toStr(params.userId);

  if (!chatId) return null;

  return (
    <UnlockChatGate chatId={chatId}>
      <ChatScreen
        chatId={chatId}
        chatName={chatName}
        otherUserId={userId}
        onBack={() => router.back()}
      />
    </UnlockChatGate>
  );
}
