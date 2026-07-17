// app/(auth)/(member)/code.tsx
import CodePanel from "@/src/member/work/code/CodePanel";
import { useTheme } from "@/src/themes/ThemeContext";
import { useRouter } from "expo-router";
import React from "react";
import { StyleSheet, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

export default function CodeScreen() {
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
      <CodePanel onBack={() => router.back()} />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
});
