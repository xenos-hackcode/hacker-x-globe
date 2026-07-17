// src/member/chat/ChatScreenHeaderSection.tsx
import React from "react";
import ChatScreenHeader from "@/src/member/chat/ChatScreenHeader";

type Props = {
  onBack: () => void;
  headerTitle: string;
  headerSubtitle: string;
  isGeneral: boolean;
  isCorneal: boolean;
  otherAvatarUrl: string | null;
  otherUserId: string | null;
  presence: "online" | "offline";
  onClearChat?: () => void;
  onOpenTheme?: () => void;
  onOpenSearch?: () => void;
  onOpenDisappearing?: () => void;
  onOpenChatInfo?: () => void; 
  onOpenReport?: () => void;  
  onOpenSaved?: () => void;
};

export function ChatScreenHeaderSection({
  onBack,
  headerTitle,
  headerSubtitle,
  isGeneral,
  isCorneal,
  otherAvatarUrl,
  otherUserId,
  presence,
  onClearChat,
  onOpenTheme,
  onOpenSearch,
  onOpenDisappearing,
  onOpenChatInfo,
  onOpenReport,
  onOpenSaved,
}: Props) {
  const avatarInitial = headerTitle.charAt(0).toUpperCase();

  let headerAvatarUri: string | undefined;
  let headerAvatarLocal: any | undefined;

  if (isGeneral) {
    headerAvatarLocal = require("./general.png");
  } else if (isCorneal) {
    headerAvatarLocal = require("./corneal.png");
  } else {
    headerAvatarUri = otherAvatarUrl || undefined;
  }

  return (
    <ChatScreenHeader
      title={headerTitle}
      subtitle={headerSubtitle}
      onBack={onBack}
      avatarInitial={avatarInitial}
      avatarUri={headerAvatarUri}
      avatarSource={headerAvatarLocal}
      userId={otherUserId || undefined}
      presence={presence}
      onClearChat={onClearChat}
      onOpenTheme={onOpenTheme}
      onOpenSearch={onOpenSearch}
      onOpenDisappearing={onOpenDisappearing}
      onOpenInfo={onOpenChatInfo}
      onOpenReport={onOpenReport}   
      onOpenSaved={onOpenSaved}
    />
  );
}
