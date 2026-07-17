// app/(auth)/(member)/calls.tsx
import CallsPanel from "@/src/member/calls/CallsPanel";
import ChatListBottom from "@/src/member/chat/ChatListBottom";
import { useTheme } from "@/src/themes/ThemeContext";
import { useRouter } from "expo-router";
import React from "react";
import { ScrollView, StyleSheet, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

export default function CallsScreen() {
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();
  const router = useRouter();

  return (
    <View
      style={[
        styles.screen,
        {
          backgroundColor: colors.background,
          paddingBottom: insets.bottom,
        },
      ]}
    >
      <ScrollView contentContainerStyle={styles.content}>
        <CallsPanel
          onStartVoice={(userId) => {
            if (!userId) return;
            router.push({
              pathname: "/(auth)/(member)/calls/call",
              params: { calleeId: userId, mode: "voice" },
            });
          }}
          onStartVideo={(userId) => {
            if (!userId) return;
            router.push({
              pathname: "/(auth)/(member)/calls/call",
              params: { calleeId: userId, mode: "video" },
            });
          }}
          onOpenSettings={() => router.push("/settings")}
        />
      </ScrollView>
      <ChatListBottom active="calls" />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { padding: 16, paddingBottom: 80 },
});
