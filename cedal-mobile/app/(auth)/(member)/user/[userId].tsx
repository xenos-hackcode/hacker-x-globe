// app/(auth)/(member)/user/[userId].tsx
import React from "react";
import { useLocalSearchParams, useRouter } from "expo-router";
import UserProfileScreen from "@/src/member/profile/UserProfileScreen";

type Params = {
  userId?: string | string[];
};

function toStr(v?: string | string[]) {
  if (Array.isArray(v)) return v[0];
  return v;
}

export default function UserProfileRoute() {
  const router = useRouter();
  const params = useLocalSearchParams<Params>();
  const userId = toStr(params.userId);

  if (!userId) {
    router.back();
    return null;
  }

  // This route has no styling/layout of its own, it just delegates to the screen
  return (
    <UserProfileScreen
      userId={userId}
      onBack={() => router.back()}
    />
  );
}
