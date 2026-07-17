// app/index.tsx
import { auth } from "@/src/api/firebase";
import { useAppVisibility } from "@/src/hooks/useAppVisibility";
import { useRouter } from "expo-router";
import * as SecureStore from "expo-secure-store";
import { onAuthStateChanged, User } from "firebase/auth";
import React, { useCallback, useEffect, useRef, useState } from "react";
import { ActivityIndicator, View } from "react-native";

export default function Index() {
  const router = useRouter();
  const [booting, setBooting] = useState(true);
  const hasRoutedRef = useRef(false);

  const safeReplace = useCallback(
    (path: string) => {
      if (hasRoutedRef.current) return;
      hasRoutedRef.current = true;
      router.replace(path as any);
    },
    [router]
  );

  const handleBackground = useCallback(async () => {
    await SecureStore.setItemAsync(
      "cedal_last_background_at",
      Date.now().toString()
    );
  }, []);

  const handleForeground = useCallback(async () => {
    const user = auth.currentUser;
    if (!user) return;

    const passcodeDone = await SecureStore.getItemAsync("cedal_passcode_done");
    const last = await SecureStore.getItemAsync("cedal_last_background_at");

    if (passcodeDone === "true" && last) {
      const diff = Date.now() - Number(last);
      if (diff > 10_000) {
        safeReplace("/(auth)/enter-password");
      }
    }
  }, [safeReplace]);

  useAppVisibility(handleBackground, handleForeground);

  useEffect(() => {
    const sub = onAuthStateChanged(auth, async (user: User | null) => {
      try {
        if (hasRoutedRef.current) return;

        if (!user) {
          safeReplace("/(auth)/sign-up");
          return;
        }

        safeReplace("/(auth)/enter-password");
      } catch {
        safeReplace("/home");
      } finally {
        setBooting(false);
      }
    });

    return () => sub();
  }, [safeReplace]);

  if (booting) {
    return (
      <View
        style={{ flex: 1, alignItems: "center", justifyContent: "center" }}
      >
        <ActivityIndicator />
      </View>
    );
  }

  return null;
}
