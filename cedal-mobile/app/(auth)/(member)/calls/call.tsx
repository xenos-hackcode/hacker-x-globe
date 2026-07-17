// app/(auth)/(member)/calls/call.tsx
import React from "react";
import { useLocalSearchParams, useRouter } from "expo-router";
import CallScreen from "@/src/member/calls/CallScreen";

export default function CallRoute() {
  const { calleeId, mode } = useLocalSearchParams<{
    calleeId?: string;
    mode?: "voice" | "video";
  }>();
  const router = useRouter();

  if (!calleeId) {
    router.back();
    return null;
  }

  return (
    <CallScreen
      calleeId={calleeId}
      mode={mode === "video" ? "video" : "voice"}
      onEnd={() => router.back()}
    />
  );
}
