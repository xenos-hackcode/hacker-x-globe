// app/(auth)/(member)/create-passcode.tsx
import { auth, db } from "@/src/api/firebase";
import { useRouter } from "expo-router";
import * as SecureStore from "expo-secure-store";
import React, { useState } from "react";
import {
  Alert,
  Platform,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";
import { doc, setDoc } from "firebase/firestore";

export default function CreatePasscodeScreen() {
  const router = useRouter();
  const [code, setCode] = useState("");
  const [confirm, setConfirm] = useState("");
  const [busy, setBusy] = useState(false);

  const [age, setAge] = useState("");
  const [favoriteColor, setFavoriteColor] = useState("");

  async function handleSave() {
    if (!age.trim()) {
      Alert.alert("Age required", "Enter your age.");
      return;
    }
    if (!favoriteColor.trim()) {
      Alert.alert("Color required", "Enter your favorite color.");
      return;
    }

    if (code.length < 4 || code.length > 6) {
      Alert.alert("Weak code", "Use 4–6 digits for your Cedal passcode.");
      return;
    }
    if (code !== confirm) {
      Alert.alert("Mismatch", "Both passcode entries must match exactly.");
      return;
    }

    const uid = auth.currentUser?.uid;
    if (!uid) {
      Alert.alert(
        "No node",
        "Sign in again to bind this device to your node."
      );
      return;
    }

    try {
      setBusy(true);

      // local secure data
      await SecureStore.setItemAsync(`cedal_passcode_${uid}`, code);
      await SecureStore.setItemAsync("cedal_session_token", uid);
      await SecureStore.setItemAsync("cedal_biometric_enabled", "true");
      await SecureStore.setItemAsync(`cedal_age_${uid}`, age.trim());
      await SecureStore.setItemAsync(
        `cedal_color_${uid}`,
        favoriteColor.trim()
      );

      // write userKey + age + color into Firestore profile
const ref = doc(db, "users", uid);
await setDoc(
  ref,
  {
    userKey: code,
    age: Number(age) || null,
    favoriteColor: favoriteColor.trim() || null,
  },
  { merge: true }
);

      await SecureStore.setItemAsync("cedal_passcode_done", "true");

      // go to loading sign-in screen
      router.replace("/loading-profile" as any);
    } catch (e: any) {
      Alert.alert(
        "Passcode error",
        e?.message ?? "Could not save Cedal passcode."
      );
      // optional: if you do not want to block the user, you can still navigate:
      // router.replace("/loading-profile" as any);
    } finally {
      setBusy(false);
    }
  }

  async function handleSkip() {
    await SecureStore.setItemAsync("cedal_passcode_done", "true");
    router.replace("/loading-profile" as any);
  }

  return (
    <View style={styles.root}>
      <View style={styles.card}>
        <Text style={styles.title}>CEDAL PASSCODE</Text>
        <Text style={styles.subtitle}>bind a local access pattern</Text>

        {/* age */}
        <Text style={styles.label}>AGE</Text>
        <TextInput
          value={age}
          onChangeText={setAge}
          keyboardType="number-pad"
          style={styles.textInput}
          placeholder="your age"
          placeholderTextColor="#64748b"
        />

        {/* favorite color */}
        <Text style={[styles.label, { marginTop: 12 }]}>
          FAVORITE COLOR
        </Text>
        <TextInput
          value={favoriteColor}
          onChangeText={setFavoriteColor}
          style={styles.textInput}
          placeholder="neon green, cyan, etc."
          placeholderTextColor="#64748b"
        />

        {/* passcode fields */}
        <Text style={[styles.label, { marginTop: 16 }]}>CREATE CODE</Text>
        <TextInput
          value={code}
          onChangeText={setCode}
          keyboardType="number-pad"
          secureTextEntry
          maxLength={6}
          style={styles.input}
          placeholder="••••"
          placeholderTextColor="#64748b"
        />

        <Text style={[styles.label, { marginTop: 12 }]}>CONFIRM CODE</Text>
        <TextInput
          value={confirm}
          onChangeText={setConfirm}
          keyboardType="number-pad"
          secureTextEntry
          maxLength={6}
          style={styles.input}
          placeholder="••••"
          placeholderTextColor="#64748b"
        />

        <Text style={styles.hint}>
          This passcode stays on this device and can unlock your node alongside
          biometric access.
        </Text>

        <TouchableOpacity
          onPress={handleSave}
          disabled={busy}
          style={[styles.btn, busy && { opacity: 0.6 }]}
          activeOpacity={0.8}
        >
          <Text style={styles.btnText}>
            {busy ? "BINDING…" : "BIND PASSCODE"}
          </Text>
        </TouchableOpacity>

        <TouchableOpacity onPress={handleSkip} style={styles.skipBtn}>
          <Text style={styles.skipText}>skip for now</Text>
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
    maxWidth: 420,
    borderRadius: 24,
    borderWidth: 1.5,
    borderColor: "rgba(56,189,248,0.9)",
    backgroundColor: "rgba(15,23,42,0.97)",
    padding: 20,
  },
  title: {
    fontSize: 18,
    letterSpacing: 3,
    color: "#e5e7eb",
    fontWeight: "700",
    textAlign: "center",
  },
  subtitle: {
    fontSize: 11,
    color: "#93c5fd",
    letterSpacing: 2,
    textAlign: "center",
    marginTop: 4,
    marginBottom: 18,
  },
  label: {
    fontSize: 11,
    color: "#9ca3af",
    letterSpacing: 2,
    marginBottom: 4,
  },
  textInput: {
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: "rgba(148,163,184,0.9)",
    backgroundColor: "#020617",
    color: "#e5e7eb",
    fontSize: 14,
    paddingVertical: Platform.select({ ios: 10, android: 6 }),
    paddingHorizontal: 12,
  },
  input: {
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: "rgba(148,163,184,0.9)",
    backgroundColor: "#020617",
    color: "#e5e7eb",
    fontSize: 18,
    letterSpacing: 8,
    paddingVertical: Platform.select({ ios: 10, android: 6 }),
    paddingHorizontal: 12,
    textAlign: "center",
  },
  hint: {
    fontSize: 10,
    color: "#6b7280",
    marginTop: 10,
  },
  btn: {
    marginTop: 18,
    borderRadius: 999,
    paddingVertical: 11,
    alignItems: "center",
    backgroundColor: "#22d3ee",
  },
  btnText: {
    color: "#020617",
    fontSize: 13,
    fontWeight: "800",
    letterSpacing: 3,
  },
  skipBtn: {
    marginTop: 10,
    alignItems: "center",
  },
  skipText: {
    fontSize: 11,
    color: "#93c5fd",
    letterSpacing: 2,
    textTransform: "uppercase",
  },
});
