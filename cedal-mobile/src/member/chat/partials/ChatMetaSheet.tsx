// src/member/chat/partials/ChatMetaSheet.tsx
import React from "react";
import { View } from "react-native";
import { ChatMetaSection } from "@/src/member/profile/components/ChatMetaSection";
import { useChatScreenState } from "@/src/hooks/useChatScreenState";

type Props = {
  state: ReturnType<typeof useChatScreenState>;
  visible: boolean;
  onClose: () => void;
};

export function ChatMetaSheet({ state, visible, onClose }: Props) {
  if (!visible) return null;

  const { clearLocalHistory } = state;

  return (
    // replace View with your bottom sheet component if you have one
    <View style={{ position: "absolute", left: 0, right: 0, bottom: 0 }}>
      <ChatMetaSection
        onChatPassword={() => {}}
        onDisappearingChat={() => {}}
        onSavedMessages={() => {}}
        onMedia={() => {}}
        onLinks={() => {}}
        onDocs={() => {}}
        onFavorite={() => {}}
        onBlock={() => {}}
        onClearChat={() => {
          clearLocalHistory();   // ← this does local “clear chat”
          onClose();
        }}
      />
    </View>
  );
}
