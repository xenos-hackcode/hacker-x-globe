// app/(auth)/(member)/_layout.tsx
import { PresenceWatcher } from "@/src/member/presence/PresenceWatcher";
import { BankSettingsProvider } from "@/src/member/work/banking/BankSettingsContext";
import { useTheme } from "@/src/themes/ThemeContext";
import { Stack } from "expo-router";
import React from "react";

export default function MemberLayout() {
  const { colors } = useTheme();

  return (
    <BankSettingsProvider>
      {/* runs once for member area, sets up RTDB presence */}
      <PresenceWatcher />

      <Stack
        screenOptions={{
          headerShown: false,
          contentStyle: { backgroundColor: colors.background },
        }}
      >
        <Stack.Screen name="(chat)/[chatId]" />
        <Stack.Screen name="user/[userId]" />
      </Stack>
    </BankSettingsProvider>
  );
}
