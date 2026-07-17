// app/(auth)/(member)/group-chat.tsx
import React from "react";
import { useLocalSearchParams, useRouter } from "expo-router";
import GroupChatScreen from "@/src/member/fun/groups/GroupChatScreen";

export default function GroupChatRoute() {
  const router = useRouter();
  const { groupId, groupName } = useLocalSearchParams<{
    groupId: string;
    groupName?: string;
  }>();

  if (!groupId) return null;

  return (
    <GroupChatScreen
      groupId={groupId}
      groupName={typeof groupName === "string" ? groupName : undefined}
      onBack={() => router.back()}
    />
  );
}
