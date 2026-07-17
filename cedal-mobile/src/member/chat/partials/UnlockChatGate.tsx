// src/member/chat/partials/UnlockChatGate.tsx
import React, { useEffect, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  Platform,
  ActivityIndicator,
  Alert,
} from "react-native";
import * as SecureStore from "expo-secure-store";
import * as LocalAuthentication from "expo-local-authentication";
import { auth } from "@/src/api/firebase";

type Props = {
  chatId: string;
  children: React.ReactNode;
};

export function UnlockChatGate({ chatId, children }: Props) {
  const [loading, setLoading] = useState(true);
  const [locked, setLocked] = useState(false);
  const [mode, setMode] = useState<"unlock" | "reset">("unlock");

  const [codeInput, setCodeInput] = useState("");
  const [favoriteAnimal, setFavoriteAnimal] = useState("");
  const [busy, setBusy] = useState(false);
  const [biometricAvailable, setBiometricAvailable] = useState(false);

  const uid = auth.currentUser?.uid ?? null;
  const passKey = uid ? `chat_lock_${uid}_${chatId}_pass` : null;
  const animalKey = uid ? `chat_lock_${uid}_${chatId}_animal` : null;

  useEffect(() => {
    let active = true;
    (async () => {
      if (!uid || !passKey) {
        if (!active) return;
        setLocked(false);
        setLoading(false);
        return;
      }

      const stored = await SecureStore.getItemAsync(passKey);
      const isLocked = !!stored;

      let canBio = false;
      if (isLocked) {
        const hasHardware = await LocalAuthentication.hasHardwareAsync();
        const enrolled = await LocalAuthentication.isEnrolledAsync();
        canBio = hasHardware && enrolled;
      }

      if (!active) return;
      setLocked(isLocked);
      setBiometricAvailable(canBio);
      setLoading(false);
    })();

    return () => {
      active = false;
    };
  }, [uid, passKey]);

  if (loading) {
    return (
      <View style={styles.fullscreen}>
        <ActivityIndicator color="#38bdf8" />
      </View>
    );
  }

  if (!locked) {
    return <>{children}</>;
  }

  async function handleBiometric() {
    try {
      setBusy(true);
      const result = await LocalAuthentication.authenticateAsync({
        promptMessage: "Unlock chat",
        fallbackLabel: "Use passcode",
      });
      if (result.success) {
        setLocked(false);
      }
    } catch (e: any) {
      Alert.alert("Error", e?.message ?? "Biometric auth failed.");
    } finally {
      setBusy(false);
    }
  }

  async function handleCodeUnlock() {
    if (!uid || !passKey) return;
    const stored = (await SecureStore.getItemAsync(passKey)) ?? "";

    if (stored !== codeInput) {
      Alert.alert("Wrong code", "Passcode is incorrect.");
      return;
    }

    setLocked(false);
    setCodeInput("");
  }

  async function handleResetVerify() {
    if (!animalKey || !passKey) return;
    const stored = (await SecureStore.getItemAsync(animalKey)) ?? "";
    if (stored !== favoriteAnimal.trim().toLowerCase()) {
      Alert.alert("Incorrect", "Favorite animal does not match.");
      return;
    }

    // Clear pass so they can enter chat and set a new one later
    await SecureStore.deleteItemAsync(passKey);
    setLocked(false);
    setFavoriteAnimal("");
  }

  const showUnlock = mode === "unlock";
  const showReset = mode === "reset";

  return (
    <View style={styles.fullscreen}>
      <View style={styles.card}>
        {showUnlock && (
          <>
            <Text style={styles.title}>Chat locked</Text>
            <Text style={styles.subtitle}>
              Enter your chat password or use biometrics to continue.
            </Text>

            {biometricAvailable && (
              <TouchableOpacity
                style={[styles.btn, styles.bioBtn, busy && { opacity: 0.6 }]}
                onPress={handleBiometric}
                disabled={busy}
                activeOpacity={0.8}
              >
                <Text style={styles.btnText}>
                  Unlock with Face / Touch ID
                </Text>
              </TouchableOpacity>
            )}

            <Text style={[styles.label, { marginTop: 14 }]}>Passcode</Text>
            <TextInput
              value={codeInput}
              onChangeText={setCodeInput}
              keyboardType="number-pad"
              secureTextEntry
              maxLength={6}
              style={styles.codeInput}
              placeholder="••••"
              placeholderTextColor="#64748b"
            />

            <TouchableOpacity
              style={[styles.btn, busy && { opacity: 0.6 }]}
              onPress={handleCodeUnlock}
              disabled={busy}
              activeOpacity={0.8}
            >
              <Text style={styles.btnText}>Unlock</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.forgotBtn}
              onPress={() => {
                setFavoriteAnimal("");
                setMode("reset");
              }}
            >
              <Text style={styles.forgotText}>Forgot password?</Text>
            </TouchableOpacity>
          </>
        )}

        {showReset && (
          <>
            <Text style={styles.title}>Reset password</Text>
            <Text style={styles.subtitle}>
              Enter your favorite animal to remove the lock for this chat.
            </Text>

            <Text style={styles.label}>Favorite animal</Text>
            <TextInput
              value={favoriteAnimal}
              onChangeText={setFavoriteAnimal}
              style={styles.resetInput}
              placeholder="favorite animal"
              placeholderTextColor="#64748b"
            />

            <TouchableOpacity
              style={styles.btn}
              onPress={handleResetVerify}
              activeOpacity={0.8}
            >
              <Text style={styles.btnText}>Verify & unlock chat</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.forgotBtn}
              onPress={() => setMode("unlock")}
            >
              <Text style={styles.forgotText}>Back to passcode</Text>
            </TouchableOpacity>
          </>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  fullscreen: {
    flex: 1,
    backgroundColor: "#020617",
    justifyContent: "center",
    alignItems: "center",
  },
  card: {
    width: "86%",
    maxWidth: 420,
    borderRadius: 20,
    paddingHorizontal: 18,
    paddingVertical: 20,
    backgroundColor: "#020617",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
  },
  title: {
    fontSize: 18,
    color: "#e5e7eb",
    fontWeight: "600",
    textAlign: "center",
  },
  subtitle: {
    fontSize: 12,
    color: "#9ca3af",
    textAlign: "center",
    marginTop: 6,
    marginBottom: 14,
  },
  label: {
    fontSize: 11,
    color: "#9ca3af",
    marginBottom: 4,
  },
  codeInput: {
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.9)",
    backgroundColor: "#020617",
    color: "#e5e7eb",
    fontSize: 18,
    letterSpacing: 6,
    paddingVertical: Platform.select({ ios: 9, android: 7 }),
    paddingHorizontal: 10,
    textAlign: "center",
  },
  resetInput: {
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.9)",
    backgroundColor: "#020617",
    color: "#e5e7eb",
    fontSize: 13,
    paddingVertical: Platform.select({ ios: 9, android: 7 }),
    paddingHorizontal: 10,
  },
  btn: {
    marginTop: 14,
    borderRadius: 999,
    paddingVertical: 10,
    alignItems: "center",
    backgroundColor: "#22d3ee",
  },
  bioBtn: {
    marginTop: 4,
    backgroundColor: "#22c55e",
  },
  btnText: {
    color: "#020617",
    fontSize: 13,
    fontWeight: "700",
    letterSpacing: 1,
    textTransform: "uppercase",
  },
  forgotBtn: {
    marginTop: 10,
    alignItems: "center",
  },
  forgotText: {
    fontSize: 11,
    color: "#93c5fd",
    textDecorationLine: "underline",
  },
});
