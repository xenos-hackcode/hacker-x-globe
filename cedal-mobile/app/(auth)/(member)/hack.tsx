// app/(auth)/(member)/hack.tsx
import HackPanel from "@/src/member/work/hack/HackPanel";
import { useTheme } from "@/src/themes/ThemeContext";
import { useRouter } from "expo-router";
import React from "react";
import { StyleSheet, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

export default function HackScreen() {
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
      <HackPanel onBack={() => router.back()} />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
});
