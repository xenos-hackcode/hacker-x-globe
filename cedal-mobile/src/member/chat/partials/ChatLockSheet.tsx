// src/member/chat/partials/ChatLockSheet.tsx
import React, { useEffect, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
  Platform,
  Alert,
} from "react-native";
import * as SecureStore from "expo-secure-store";
import { auth } from "@/src/api/firebase";
import { useSafeAreaInsets } from "react-native-safe-area-context";

type Props = {
  chatId: string;
  open: boolean;
  onClose: () => void;
};

export function ChatLockSheet({ chatId, open, onClose }: Props) {
  const insets = useSafeAreaInsets();

  const [mode, setMode] = useState<"create" | "locked" | "reset">("create");
  const [hasPass, setHasPass] = useState(false);

  const [code, setCode] = useState("");
  const [confirm, setConfirm] = useState("");
  const [favoriteAnimal, setFavoriteAnimal] = useState("");

  const [busy, setBusy] = useState(false);
  const [showCode, setShowCode] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);

  const uid = auth.currentUser?.uid ?? null;
  const passKey = uid ? `chat_lock_${uid}_${chatId}_pass` : null;
  const animalKey = uid ? `chat_lock_${uid}_${chatId}_animal` : null;

  useEffect(() => {
    if (!open || !passKey) return;
    (async () => {
      const existing = await SecureStore.getItemAsync(passKey);
      setHasPass(!!existing);
      setMode(existing ? "locked" : "create");
      setCode("");
      setConfirm("");
      setFavoriteAnimal("");
      setShowCode(false);
      setShowConfirm(false);
    })();
  }, [open, passKey]);

  if (!open) return null;

  async function handleCreate() {
    if (!uid || !passKey || !animalKey) {
      Alert.alert("No node", "Sign in again.");
      return;
    }

    if (code.length < 4 || code.length > 6) {
      Alert.alert("Code length", "Use 4–6 digits for this chat.");
      return;
    }
    if (code !== confirm) {
      Alert.alert("Mismatch", "Codes must match.");
      return;
    }
    if (!favoriteAnimal.trim()) {
      Alert.alert("Required", "Enter your favorite animal.");
      return;
    }

    try {
      setBusy(true);
      await SecureStore.setItemAsync(passKey, code);
      await SecureStore.setItemAsync(
        animalKey,
        favoriteAnimal.trim().toLowerCase(),
      );
      setHasPass(true);
      setMode("locked");
      Alert.alert("Chat locked", "This chat now requires your passcode.");
    } catch (e: any) {
      Alert.alert("Error", e?.message ?? "Could not save chat lock.");
    } finally {
      setBusy(false);
    }
  }

  async function handleCheckReset() {
    if (!animalKey || !passKey) return;
    const stored = (await SecureStore.getItemAsync(animalKey)) ?? "";
    if (stored !== favoriteAnimal.trim().toLowerCase()) {
      Alert.alert("Incorrect", "Favorite animal does not match.");
      return;
    }

    setCode("");
    setConfirm("");
    setShowCode(false);
    setShowConfirm(false);
    setMode("create");
  }

  const showCreate = mode === "create";
  const showLocked = mode === "locked";
  const showReset = mode === "reset";

  return (
    <View style={styles.overlay}>
      <View style={[styles.sheet, { paddingBottom: insets.bottom + 16 }]}>
        <View style={styles.headerRow}>
          <Text style={styles.title}>Chat password</Text>
          <TouchableOpacity onPress={onClose}>
            <Text style={styles.closeText}>Close</Text>
          </TouchableOpacity>
        </View>

        {showCreate && (
          <>
            <Text style={styles.help}>
              Lock this chat only on this device. The other person will not see this.
            </Text>

            <Text style={styles.label}>Favorite animal (for reset)</Text>
            <TextInput
              value={favoriteAnimal}
              onChangeText={setFavoriteAnimal}
              style={styles.textInput}
              placeholder="lion, dog, etc."
              placeholderTextColor="#64748b"
            />

            <View style={styles.row}>
              <View style={styles.half}>
                <Text style={styles.label}>Create code</Text>
                <View style={styles.codeRow}>
                  <TextInput
                    value={code}
                    onChangeText={setCode}
                    keyboardType="number-pad"
                    secureTextEntry={!showCode}
                    maxLength={6}
                    style={[styles.codeInput, { flex: 1 }]}
                    placeholder="••••"
                    placeholderTextColor="#64748b"
                  />
                  <TouchableOpacity
                    onPress={() => setShowCode((v) => !v)}
                    style={styles.eyeBtn}
                    activeOpacity={0.7}
                  >
                    <Text style={styles.eyeText}>
                      {showCode ? "🙈" : "👁️"}
                    </Text>
                  </TouchableOpacity>
                </View>
              </View>

              <View style={styles.half}>
                <Text style={styles.label}>Confirm code</Text>
                <View style={styles.codeRow}>
                  <TextInput
                    value={confirm}
                    onChangeText={setConfirm}
                    keyboardType="number-pad"
                    secureTextEntry={!showConfirm}
                    maxLength={6}
                    style={[styles.codeInput, { flex: 1 }]}
                    placeholder="••••"
                    placeholderTextColor="#64748b"
                  />
                  <TouchableOpacity
                    onPress={() => setShowConfirm((v) => !v)}
                    style={styles.eyeBtn}
                    activeOpacity={0.7}
                  >
                    <Text style={styles.eyeText}>
                      {showConfirm ? "🙈" : "👁️"}
                    </Text>
                  </TouchableOpacity>
                </View>
              </View>
            </View>

            <TouchableOpacity
              onPress={handleCreate}
              disabled={busy}
              style={[styles.btn, busy && { opacity: 0.6 }]}
              activeOpacity={0.8}
            >
              <Text style={styles.btnText}>
                {busy ? "Saving…" : "Lock chat"}
              </Text>
            </TouchableOpacity>
          </>
        )}

        {showLocked && (
          <>
            <Text style={styles.help}>
              You already have a password for this chat on this device.
            </Text>
            <View style={styles.bottomRow}>
              <TouchableOpacity
                style={styles.resetBtn}
                onPress={() => setMode("reset")}
              >
                <Text style={styles.resetText}>Forgot / reset password</Text>
              </TouchableOpacity>
            </View>
          </>
        )}

        {showReset && (
          <>
            <Text style={styles.help}>
              Enter your favorite animal to reset this chat password.
            </Text>

            <Text style={styles.label}>Favorite animal</Text>
            <TextInput
              value={favoriteAnimal}
              onChangeText={setFavoriteAnimal}
              style={styles.textInput}
              placeholder="favorite animal"
              placeholderTextColor="#64748b"
            />

            <TouchableOpacity
              onPress={handleCheckReset}
              style={styles.btn}
              activeOpacity={0.8}
            >
              <Text style={styles.btnText}>Verify and create new code</Text>
            </TouchableOpacity>
          </>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    position: "absolute",
    left: 0,
    right: 0,
    top: 0,
    bottom: 0,
    backgroundColor: "rgba(0,0,0,0.6)",
    justifyContent: "flex-end",
  },
  sheet: {
    paddingHorizontal: 16,
    paddingTop: 14,
    backgroundColor: "#020617",
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
  },
  headerRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 10,
  },
  title: { fontSize: 16, fontWeight: "600", color: "#e5e7eb" },
  closeText: { fontSize: 13, color: "#9ca3af" },
  help: { fontSize: 11, color: "#9ca3af", marginBottom: 10 },
  label: { fontSize: 11, color: "#9ca3af", marginBottom: 4 },
  textInput: {
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.9)",
    backgroundColor: "#020617",
    color: "#e5e7eb",
    fontSize: 13,
    paddingVertical: Platform.select({ ios: 9, android: 6 }),
    paddingHorizontal: 10,
    marginBottom: 10,
  },
  row: {
    flexDirection: "row",
    columnGap: 8,
    marginTop: 4,
  },
  half: {
    flex: 1,
  },
  codeRow: {
    flexDirection: "row",
    alignItems: "center",
    columnGap: 6,
  },
  codeInput: {
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.9)",
    backgroundColor: "#020617",
    color: "#e5e7eb",
    fontSize: 16,
    letterSpacing: 6,
    paddingVertical: Platform.select({ ios: 8, android: 6 }),
    paddingHorizontal: 8,
    textAlign: "center",
  },
  eyeBtn: {
    paddingHorizontal: 6,
    paddingVertical: 4,
  },
  eyeText: {
    fontSize: 16,
    color: "#9ca3af",
  },
  btn: {
    marginTop: 14,
    borderRadius: 999,
    paddingVertical: 10,
    alignItems: "center",
    backgroundColor: "#22d3ee",
  },
  btnText: {
    color: "#020617",
    fontSize: 13,
    fontWeight: "700",
    letterSpacing: 1,
    textTransform: "uppercase",
  },
  bottomRow: {
    flexDirection: "row",
    justifyContent: "flex-end",
    marginTop: 8,
  },
  resetBtn: {
    paddingVertical: 4,
  },
  resetText: {
    fontSize: 11,
    color: "#93c5fd",
    textDecorationLine: "underline",
  },
});
