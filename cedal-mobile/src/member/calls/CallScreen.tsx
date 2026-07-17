// src/member/calls/CallScreen.tsx
import React, { useState } from "react";
import VoiceCallScreen from "./VoiceCallScreen";
import VideoCallScreen from "./VideoCallScreen";

type CallMode = "voice" | "video";

type Props = {
  calleeId: string;
  mode: CallMode;
  onEnd?: () => void;
};

export default function CallScreen({ calleeId, mode: initialMode, onEnd }: Props) {
  const [mode, setMode] = useState<CallMode>(initialMode);

  if (mode === "voice") {
    return (
      <VoiceCallScreen
        calleeId={calleeId}
        onSwitchToVideo={() => setMode("video")}
        onEnd={onEnd}
      />
    );
  }

  return (
    <VideoCallScreen
      calleeId={calleeId}
      onSwitchToVoice={() => setMode("voice")}
      onEnd={onEnd}
    />
  );
}
