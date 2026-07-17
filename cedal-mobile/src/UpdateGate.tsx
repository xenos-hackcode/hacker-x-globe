// src/UpdateGate.tsx
import React, { useEffect, useState } from "react";
import {
  Linking,
  Modal,
  Platform,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import Constants from "expo-constants";
import { checkAppVersion } from "@/src/api/checkAppVersion";

export default function UpdateGate() {
  const [blocking, setBlocking] = useState(false);
  const [nudgeVisible, setNudgeVisible] = useState(false);
  const [updateUrl, setUpdateUrl] = useState<string | null>(null);

  useEffect(() => {
    if (Platform.OS !== "android") return;

    const currentVersion = Constants.expoConfig?.version ?? "0.0.0";

    checkAppVersion(currentVersion)
      .then((status) => {
        setUpdateUrl(status.updateUrl);
        if (status.updateRequired) {
          setBlocking(true);
        } else if (status.updateAvailable) {
          setNudgeVisible(true);
        }
      })
      .catch(() => {
        // If the check fails (offline, doc missing, etc.) don't block the app.
      });
  }, []);

  function openStore() {
    if (updateUrl) Linking.openURL(updateUrl);
  }

  return (
    <>
      <Modal visible={blocking} transparent animationType="fade">
        <View style={styles.backdrop}>
          <View style={styles.card}>
            <Text style={styles.title}>Update required</Text>
            <Text style={styles.body}>
              A new version of Cedal is required to continue.
            </Text>
            <TouchableOpacity style={styles.button} onPress={openStore} activeOpacity={0.85}>
              <Text style={styles.buttonText}>Update now</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {nudgeVisible && !blocking && (
        <View style={styles.banner}>
          <Text style={styles.bannerText}>A new version of Cedal is available.</Text>
          <TouchableOpacity onPress={openStore} activeOpacity={0.85}>
            <Text style={styles.bannerAction}>Update</Text>
          </TouchableOpacity>
          <TouchableOpacity onPress={() => setNudgeVisible(false)} activeOpacity={0.85}>
            <Text style={styles.bannerDismiss}>Dismiss</Text>
          </TouchableOpacity>
        </View>
      )}
    </>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.75)",
    justifyContent: "center",
    alignItems: "center",
    padding: 24,
  },
  card: {
    width: "100%",
    borderRadius: 16,
    backgroundColor: "#0f172a",
    padding: 20,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.3)",
  },
  title: { color: "#e5e7eb", fontSize: 16, fontWeight: "700", marginBottom: 8 },
  body: { color: "#9ca3af", fontSize: 13, marginBottom: 16 },
  button: {
    paddingVertical: 10,
    borderRadius: 999,
    backgroundColor: "#22c55e",
    alignItems: "center",
  },
  buttonText: { color: "#052e16", fontSize: 13, fontWeight: "700" },

  banner: {
    position: "absolute",
    left: 12,
    right: 12,
    bottom: 24,
    flexDirection: "row",
    alignItems: "center",
    gap: 12,
    backgroundColor: "#0f172a",
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.3)",
    paddingVertical: 10,
    paddingHorizontal: 14,
  },
  bannerText: { flex: 1, color: "#e5e7eb", fontSize: 12 },
  bannerAction: { color: "#22d3ee", fontSize: 12, fontWeight: "700" },
  bannerDismiss: { color: "#6b7280", fontSize: 12 },
});
