// app/(auth)/(member)/work.tsx
import ChatListBottom from "@/src/member/chat/ChatListBottom";
import WorkPicker, { WorkMode } from "@/src/member/work/WorkPicker";
import { useTheme } from "@/src/themes/ThemeContext";
import React, { useState } from "react";
import { ScrollView, StyleSheet, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

export default function WorkScreen() {
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();
  const [mode, setMode] = useState<WorkMode | null>(null);

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
        <WorkPicker selectedMode={mode} onSelectMode={setMode} />
      </ScrollView>
      <ChatListBottom active="work" />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { padding: 16, paddingBottom: 80 },
});
