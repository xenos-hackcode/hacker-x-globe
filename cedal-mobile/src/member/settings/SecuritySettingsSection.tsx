// src/member/settings/SecuritySettingsSection.tsx
import SettingsRow from "@/src/member/settings/SettingsRow";
import { useTheme } from "@/src/themes/ThemeContext";
import AsyncStorage from "@react-native-async-storage/async-storage";
import * as ScreenCapture from "expo-screen-capture";
import React, { useEffect, useState } from "react";
import { StyleSheet, Text, View } from "react-native";

export const APP_VIEW_ONCE_KEY = "cedal.security.appViewOnce";

export default function SecuritySettingsSection() {
  const { colors } = useTheme();
  const [appViewOnce, setAppViewOnce] = useState(false);

  useEffect(() => {
    AsyncStorage.getItem(APP_VIEW_ONCE_KEY).then((saved) => {
      if (saved === "true") setAppViewOnce(true);
    });
  }, []);

  async function handleToggle(val: boolean) {
    setAppViewOnce(val);
    await AsyncStorage.setItem(APP_VIEW_ONCE_KEY, val ? "true" : "false");
    if (val) {
      await ScreenCapture.preventScreenCaptureAsync();
    } else {
      await ScreenCapture.allowScreenCaptureAsync();
    }
  }

  return (
    <>
      <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>
        Security
      </Text>

      <View
        style={[
          styles.card,
          { borderColor: colors.border, backgroundColor: colors.background },
        ]}
      >
        <SettingsRow
          label="App View Once"
          description="Blocks screenshots and screen recording anywhere in the app."
          value={appViewOnce}
          onValueChange={handleToggle}
          last
        />
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  sectionLabel: {
    fontSize: 11,
    letterSpacing: 2,
    textTransform: "uppercase",
    marginBottom: 6,
  },
  card: {
    borderRadius: 18,
    borderWidth: 1,
    marginBottom: 18,
    overflow: "hidden",
  },
});
