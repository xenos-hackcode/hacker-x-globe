// app/(auth)/(owner)/creator-passcode.tsx
import React, { useState } from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  Alert,
  StyleSheet,
} from "react-native";
import { useRouter } from "expo-router";
import * as SecureStore from "expo-secure-store";

import { verifyOwnerCodeRemote } from "@/src/api/owner";

export default function CreatorPasscodeScreen() {
  const router = useRouter();
  const [creatorCode, setCreatorCode] = useState("");
  const [ownerCode, setOwnerCode] = useState("");
  const [busy, setBusy] = useState(false);

  const [showCreator, setShowCreator] = useState(false);
  const [showOwner, setShowOwner] = useState(false);

 async function handleApplyCreator() {
  if (!creatorCode.trim()) {
    Alert.alert("Creator", "Enter creator passcode.");
    return;
  }

  try {
    setBusy(true);

    const data = await verifyOwnerCodeRemote(creatorCode.trim());

    await SecureStore.setItemAsync(
      "cedal_owner_session",
      JSON.stringify({
        ...data.ownerSession,
        isOwner: true,
      }),
      {
        keychainService: "cedal_owner",
        requireAuthentication: false,
      }
    );

    Alert.alert("Creator", "Owner access granted.");

    // CREATOR → go to new owner-loading screen
    router.replace("/(auth)/(owner)/owner-loading");
  } catch (e: any) {
    Alert.alert(
      "Creator",
      e?.message ?? "Failed to verify creator passcode."
    );
  } finally {
    setBusy(false);
  }
}

async function handleApplyOwnerPass() {
  if (!ownerCode.trim()) {
    Alert.alert("Owner", "Enter owner passcode.");
    return;
  }

  try {
    setBusy(true);

    const data = await verifyOwnerCodeRemote(ownerCode.trim());

    await SecureStore.setItemAsync(
      "cedal_owner_session",
      JSON.stringify({
        ...data.ownerSession,
        isOwner: true,
      }),
      {
        keychainService: "cedal_owner",
        requireAuthentication: false,
      }
    );

    Alert.alert("Owner", "Owner passcode accepted.");

    // OWNER → still goes to loading-owner (or change if you want)
    router.replace("/(auth)/(owner)/loading-owner");
  } catch (e: any) {
    Alert.alert("Owner", e?.message ?? "Failed to verify owner passcode.");
  } finally {
    setBusy(false);
  }
}

  return (
    <View style={styles.root}>
      <View className="card" style={styles.card}>
        <Text style={styles.title}>CREATOR ACCESS</Text>
        <Text style={styles.subtitle}>hidden control panel</Text>

        {/* creator passcode */}
        <Text style={styles.label}>CREATOR PASSCODE</Text>
        <View style={styles.inputRow}>
          <TextInput
            value={creatorCode}
            onChangeText={setCreatorCode}
            placeholder="••••••••"
            placeholderTextColor="#64748b"
            secureTextEntry={!showCreator}
            style={[styles.input, { flex: 1 }]}
          />
          <TouchableOpacity
            onPress={() => setShowCreator((v) => !v)}
            style={styles.toggleBtn}
          >
            <Text style={styles.toggleText}>
              {showCreator ? "HIDE" : "SHOW"}
            </Text>
          </TouchableOpacity>
        </View>

        <TouchableOpacity
          onPress={handleApplyCreator}
          disabled={busy}
          style={[styles.btn, busy && { opacity: 0.6 }]}
        >
          <Text style={styles.btnText}>
            {busy ? "CHECKING…" : "UNLOCK OWNER VIA CREATOR"}
          </Text>
        </TouchableOpacity>

        {/* separator */}
        <View style={styles.separator} />

        {/* owner passcode (optional second path, same backend verify) */}
        <Text style={styles.label}>OWNER PASSCODE</Text>
        <View style={styles.inputRow}>
          <TextInput
            value={ownerCode}
            onChangeText={setOwnerCode}
            placeholder="••••••••"
            placeholderTextColor="#64748b"
            secureTextEntry={!showOwner}
            style={[styles.input, { flex: 1 }]}
          />
          <TouchableOpacity
            onPress={() => setShowOwner((v) => !v)}
            style={styles.toggleBtn}
          >
            <Text style={styles.toggleText}>
              {showOwner ? "HIDE" : "SHOW"}
            </Text>
          </TouchableOpacity>
        </View>

        <TouchableOpacity
          onPress={handleApplyOwnerPass}
          disabled={busy}
          style={[styles.btnSecondary, busy && { opacity: 0.6 }]}
        >
          <Text style={styles.btnSecondaryText}>USE OWNER PASSCODE</Text>
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
    padding: 24,
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
    fontSize: 16,
    color: "#e5e7eb",
    fontWeight: "700",
    letterSpacing: 3,
    textAlign: "center",
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 11,
    color: "#93c5fd",
    letterSpacing: 2,
    textAlign: "center",
    marginBottom: 16,
  },
  label: {
    fontSize: 11,
    color: "#9ca3af",
    letterSpacing: 2,
    marginBottom: 4,
    marginTop: 8,
  },
  inputRow: {
    flexDirection: "row",
    alignItems: "center",
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: "rgba(148,163,184,0.9)",
    backgroundColor: "#020617",
    paddingHorizontal: 10,
  },
  input: {
    color: "#e5e7eb",
    fontSize: 14,
    paddingVertical: 8,
  },
  toggleBtn: {
    paddingHorizontal: 6,
    paddingVertical: 4,
  },
  toggleText: {
    fontSize: 10,
    color: "#e5e7eb",
    letterSpacing: 1.5,
  },
  btn: {
    marginTop: 12,
    borderRadius: 999,
    paddingVertical: 10,
    alignItems: "center",
    backgroundColor: "#22d3ee",
  },
  btnText: {
    color: "#020617",
    fontSize: 12,
    fontWeight: "700",
    letterSpacing: 2,
  },
  separator: {
    height: 1,
    backgroundColor: "rgba(148,163,184,0.5)",
    marginVertical: 16,
  },
  btnSecondary: {
    marginTop: 8,
    borderRadius: 999,
    paddingVertical: 9,
    alignItems: "center",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.9)",
  },
  btnSecondaryText: {
    color: "#e5e7eb",
    fontSize: 11,
    letterSpacing: 2,
  },
  backBtn: {
    marginTop: 14,
    alignItems: "center",
  },
  backText: {
    fontSize: 11,
    color: "#64748b",
    letterSpacing: 1.5,
    textTransform: "uppercase",
  },
});
