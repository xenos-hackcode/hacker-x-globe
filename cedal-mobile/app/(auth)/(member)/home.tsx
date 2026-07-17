// app/(auth)/(member)/home.tsx
import ChatList from "@/src/member/chat/ChatList";
import { useTheme } from "@/src/themes/ThemeContext";
import React from "react";
import { StyleSheet, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

export default function HomeScreen() {
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();

  return (
    <View
      style={[
        styles.screen,
        {
          backgroundColor: colors.background,
          paddingBottom: insets.bottom, // keeps bar above system nav
        },
      ]}
    >
      <ChatList />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
});
