// app/(auth)/(member)/update-passcode-simple.tsx
import React, { useState } from "react";
import { updatePasscode } from "@/src/api/account";
import {
  Alert,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";
import { useRouter } from "expo-router";
// import { updatePasscode } from "@/src/api/account"; // <- your own helper

export default function UpdatePasscodeSimpleScreen() {
  const router = useRouter();
  const [newCode, setNewCode] = useState("");
  const [confirmCode, setConfirmCode] = useState("");
  const [busy, setBusy] = useState(false);

  async function handleUpdate() {
    try {
      if (!newCode || !confirmCode) {
        Alert.alert("Missing info", "Enter and confirm your new passcode.");
        return;
      }
      if (newCode !== confirmCode) {
        Alert.alert("Mismatch", "New passcode and confirmation do not match.");
        return;
      }
      if (newCode.length < 6) {
        Alert.alert("Too short", "Use at least 6 characters for your passcode.");
        return;
      }

      setBusy(true);

await updatePasscode(newCode);   // <- actually updates current user passcode now
      Alert.alert("Updated", "Your passcode has been changed.");
      router.replace("/home"); // <- navigate to your desired screen after update
    } catch (e: any) {
      Alert.alert("Error", e?.message ?? "Could not update passcode.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <View style={styles.root}>
      <View style={styles.card}>
        <Text style={styles.title}>Set New Passcode</Text>
        <Text style={styles.subtitle}>
          Choose a new access code. You will not be asked for your current one here.
        </Text>

        <Text style={styles.label}>New passcode</Text>
        <TextInput
          value={newCode}
          onChangeText={setNewCode}
          placeholder="••••••"
          placeholderTextColor="#6b7280"
          secureTextEntry
          style={styles.input}
        />

        <Text style={styles.label}>Confirm new passcode</Text>
        <TextInput
          value={confirmCode}
          onChangeText={setConfirmCode}
          placeholder="••••••"
          placeholderTextColor="#6b7280"
          secureTextEntry
          style={styles.input}
        />

        <TouchableOpacity
          onPress={handleUpdate}
          disabled={busy}
          style={[styles.btn, busy && { opacity: 0.6 }]}
          activeOpacity={0.8}
        >
          <Text style={styles.btnText}>
            {busy ? "UPDATING…" : "SAVE NEW PASSCODE"}
          </Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: "#020617",
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 24,
  },
  card: {
    width: "100%",
    borderRadius: 20,
    backgroundColor: "rgba(15,23,42,0.96)",
    padding: 20,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.8)",
  },
  title: {
    fontSize: 20,
    fontWeight: "700",
    color: "#e5e7eb",
    letterSpacing: 2,
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 12,
    color: "#9ca3af",
    marginBottom: 16,
  },
  label: {
    fontSize: 11,
    color: "#a5b4fc",
    letterSpacing: 1.5,
    textTransform: "uppercase",
    marginBottom: 4,
    marginTop: 8,
  },
  input: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.9)",
    backgroundColor: "#020617",
    paddingHorizontal: 14,
    paddingVertical: 10,
    color: "#e5e7eb",
    fontSize: 14,
  },
  btn: {
    marginTop: 18,
    borderRadius: 999,
    backgroundColor: "#22d3ee",
    paddingVertical: 12,
    alignItems: "center",
  },
  btnText: {
    color: "#020617",
    fontSize: 14,
    fontWeight: "700",
    letterSpacing: 2,
  },
});
