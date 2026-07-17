// app/(auth)/(member)/passcode-update.tsx
import React, { useEffect, useState } from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Alert,
  Platform,
} from "react-native";
import { useRouter } from "expo-router";
import * as SecureStore from "expo-secure-store";
import { auth, db } from "@/src/api/firebase";
import { doc, getDoc, setDoc } from "firebase/firestore";

export default function PasscodeUpdateScreen() {
  const router = useRouter();
  const [currentCode, setCurrentCode] = useState("");
  const [newCode, setNewCode] = useState("");
  const [confirmCode, setConfirmCode] = useState("");
  const [busy, setBusy] = useState(false);
  const [storedUserKey, setStoredUserKey] = useState<string | null>(null);

  // Load current userKey from Firestore
  useEffect(() => {
    (async () => {
      const user = auth.currentUser;
      if (!user) {
        Alert.alert("Session", "Sign in first.");
        router.replace("/(auth)/sign-in");
        return;
      }

      try {
        const ref = doc(db, "users", user.uid);
        const snap = await getDoc(ref);
        if (!snap.exists()) {
          Alert.alert("Profile", "No profile found for this node.");
          return;
        }
        const data = snap.data() as any;
        setStoredUserKey(data.userKey ?? null);
      } catch (e: any) {
        Alert.alert(
          "Error",
          e?.message ?? "Failed to load current passcode."
        );
      }
    })();
  }, [router]);

  async function handleUpdate() {
    const user = auth.currentUser;
    if (!user) {
      Alert.alert("Session", "Sign in first.");
      return;
    }

    if (!storedUserKey) {
      Alert.alert("Passcode", "No existing passcode is registered.");
      return;
    }

    if (!currentCode.trim()) {
      Alert.alert("Current passcode", "Enter your current Cedal passcode.");
      return;
    }

    if (currentCode !== storedUserKey) {
      Alert.alert("Passcode", "Current passcode does not match.");
      return;
    }

    if (newCode.length < 4 || newCode.length > 6) {
      Alert.alert("New passcode", "Use 4–6 digits for your new passcode.");
      return;
    }

    if (newCode !== confirmCode) {
      Alert.alert("Mismatch", "New passcode and confirmation must match.");
      return;
    }

    try {
      setBusy(true);

      const uid = user.uid;

      // 1) Update Firestore userKey
      const ref = doc(db, "users", uid);
      await setDoc(
        ref,
        {
          userKey: newCode,
        },
        { merge: true }
      );

      // 2) Update local device passcode
      await SecureStore.setItemAsync(`cedal_passcode_${uid}`, newCode);

      Alert.alert("Passcode updated", "Your Cedal passcode has been changed.", [
        {
          text: "OK",
          onPress: () => router.replace("/home"),
        },
      ]);
    } catch (e: any) {
      Alert.alert(
        "Update error",
        e?.message ?? "Failed to update Cedal passcode."
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <View style={styles.root}>
      <View style={styles.card}>
        <Text style={styles.title}>UPDATE PASSCODE</Text>
        <Text style={styles.subtitle}>
          verify your current code then bind a new one
        </Text>

        <Text style={styles.label}>CURRENT PASSCODE</Text>
        <TextInput
          value={currentCode}
          onChangeText={setCurrentCode}
          keyboardType="number-pad"
          secureTextEntry
          maxLength={6}
          style={styles.input}
          placeholder="••••"
          placeholderTextColor="#64748b"
        />

        <Text style={[styles.label, { marginTop: 12 }]}>NEW PASSCODE</Text>
        <TextInput
          value={newCode}
          onChangeText={setNewCode}
          keyboardType="number-pad"
          secureTextEntry
          maxLength={6}
          style={styles.input}
          placeholder="••••"
          placeholderTextColor="#64748b"
        />

        <Text style={[styles.label, { marginTop: 12 }]}>
          CONFIRM NEW PASSCODE
        </Text>
        <TextInput
          value={confirmCode}
          onChangeText={setConfirmCode}
          keyboardType="number-pad"
          secureTextEntry
          maxLength={6}
          style={styles.input}
          placeholder="••••"
          placeholderTextColor="#64748b"
        />

        <Text style={styles.hint}>
          This will update both your node passcode in the cloud and the local
          code stored on this device.
        </Text>

        <TouchableOpacity
          onPress={handleUpdate}
          disabled={busy}
          style={[styles.btn, busy && { opacity: 0.6 }]}
          activeOpacity={0.8}
        >
          <Text style={styles.btnText}>
            {busy ? "UPDATING…" : "UPDATE PASSCODE"}
          </Text>
        </TouchableOpacity>

        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
          <Text style={styles.backText}>back</Text>
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
  backBtn: {
    marginTop: 10,
    alignItems: "center",
  },
  backText: {
    fontSize: 11,
    color: "#93c5fd",
    letterSpacing: 2,
    textTransform: "uppercase",
  },
});
