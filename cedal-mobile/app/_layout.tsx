// app/_layout.tsx
import { ThemeProvider } from "@/src/themes/ThemeContext";
import { Stack } from "expo-router";
import { GestureHandlerRootView } from "react-native-gesture-handler";
import { BottomSheetModalProvider } from "@gorhom/bottom-sheet";
import React, { useEffect } from "react";
import { Platform } from "react-native";
import * as Notifications from "expo-notifications";
import { startLongVibration } from "@/src/member/utils/countdownAlarm";
import {
  APP_VIEW_ONCE_KEY,
} from "@/src/member/settings/SecuritySettingsSection";
import UpdateGate from "@/src/UpdateGate";
import AsyncStorage from "@react-native-async-storage/async-storage";
import * as ScreenCapture from "expo-screen-capture";

export default function RootLayout() {
  // Skia needs CanvasKit (wasm) loaded before any <Canvas> can render on web
  const [skiaReady, setSkiaReady] = React.useState(Platform.OS !== "web");

  useEffect(() => {
    if (Platform.OS !== "web") return;
    import("@shopify/react-native-skia/lib/module/web").then(
      ({ LoadSkiaWeb }) => LoadSkiaWeb().then(() => setSkiaReady(true))
    );
  }, []);

  // ask for notification permission once (not supported on web)
  useEffect(() => {
    if (Platform.OS === "web") return;
    (async () => {
      const { status } = await Notifications.getPermissionsAsync();
      if (status !== "granted") {
        await Notifications.requestPermissionsAsync();
      }
    })();
  }, []);

  // start vibration when any local notification is received (not supported on web)
  useEffect(() => {
    if (Platform.OS === "web") return;
    const sub = Notifications.addNotificationReceivedListener(() => {
      startLongVibration();
    });
    return () => {
      sub.remove();
    };
  }, []);

  // apply the persisted "App View Once" preference from the very first screen
  useEffect(() => {
    if (Platform.OS === "web") return;
    AsyncStorage.getItem(APP_VIEW_ONCE_KEY).then((saved) => {
      if (saved === "true") ScreenCapture.preventScreenCaptureAsync();
    });
  }, []);

  if (!skiaReady) {
    return null;
  }

  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <BottomSheetModalProvider>
        <ThemeProvider>
          <Stack screenOptions={{ headerShown: false }} />
          <UpdateGate />
        </ThemeProvider>
      </BottomSheetModalProvider>
    </GestureHandlerRootView>
  );
}
