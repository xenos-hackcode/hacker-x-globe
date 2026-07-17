// src/member/work/code/KotlinBuildModal.tsx
import React, { useEffect, useState } from "react";
import {
  ActivityIndicator,
  Modal,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import { doc, onSnapshot } from "firebase/firestore";
import * as FileSystem from "expo-file-system/legacy";
import * as IntentLauncher from "expo-intent-launcher";
import { db } from "@/src/api/firebase";

type BuildStatus = "queued" | "building" | "done" | "error";

type Job = {
  status: BuildStatus;
  downloadUrl?: string;
  errorMessage?: string;
};

type Props = {
  visible: boolean;
  jobId: string | null;
  onClose: () => void;
};

export default function KotlinBuildModal({ visible, jobId, onClose }: Props) {
  const [job, setJob] = useState<Job | null>(null);
  const [installing, setInstalling] = useState(false);
  const [installError, setInstallError] = useState<string | null>(null);

  useEffect(() => {
    if (!jobId) {
      setJob(null);
      return;
    }
    const unsub = onSnapshot(doc(db, "androidBuilds", jobId), (snap) => {
      setJob((snap.data() as Job) ?? null);
    });
    return unsub;
  }, [jobId]);

  async function handleInstall() {
    if (!job?.downloadUrl || !jobId) return;
    setInstalling(true);
    setInstallError(null);
    try {
      const localUri = `${FileSystem.cacheDirectory}cedal-build-${jobId}.apk`;
      const { uri } = await FileSystem.downloadAsync(job.downloadUrl, localUri);
      const contentUri = await FileSystem.getContentUriAsync(uri);
      await IntentLauncher.startActivityAsync("android.intent.action.VIEW", {
        data: contentUri,
        flags: 1, // FLAG_GRANT_READ_URI_PERMISSION
        type: "application/vnd.android.package-archive",
      });
    } catch (e: any) {
      setInstallError(e?.message ?? "Install failed");
    } finally {
      setInstalling(false);
    }
  }

  const status = job?.status ?? (jobId ? "queued" : null);

  return (
    <Modal
      visible={visible}
      animationType="slide"
      transparent
      onRequestClose={onClose}
    >
      <View style={styles.backdrop}>
        <View style={styles.card}>
          <Text style={styles.title}>Android Build</Text>

          {status === "queued" && (
            <View style={styles.row}>
              <ActivityIndicator color="#22d3ee" />
              <Text style={styles.statusText}>Queued...</Text>
            </View>
          )}
          {status === "building" && (
            <View style={styles.row}>
              <ActivityIndicator color="#22d3ee" />
              <Text style={styles.statusText}>
                Building your app (can take a few minutes)...
              </Text>
            </View>
          )}
          {status === "error" && (
            <Text style={styles.errorText}>
              Build failed: {job?.errorMessage ?? "unknown error"}
            </Text>
          )}
          {status === "done" && (
            <>
              <Text style={styles.statusText}>Build complete.</Text>
              <TouchableOpacity
                style={styles.installBtn}
                onPress={handleInstall}
                disabled={installing}
                activeOpacity={0.85}
              >
                <Text style={styles.installBtnText}>
                  {installing ? "Preparing install..." : "Install on this phone"}
                </Text>
              </TouchableOpacity>
              {installError && <Text style={styles.errorText}>{installError}</Text>}
            </>
          )}

          <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
            <Text style={styles.closeText}>Close</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.6)",
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
  title: { color: "#e5e7eb", fontSize: 15, fontWeight: "700", marginBottom: 12 },
  row: { flexDirection: "row", alignItems: "center", gap: 10 },
  statusText: { color: "#9ca3af", fontSize: 13 },
  errorText: { color: "#f87171", fontSize: 12, marginTop: 8 },
  installBtn: {
    marginTop: 12,
    paddingVertical: 10,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#22c55e",
    backgroundColor: "#14532d",
    alignItems: "center",
  },
  installBtnText: { color: "#bbf7d0", fontSize: 13, fontWeight: "600" },
  closeBtn: { marginTop: 16, alignItems: "center" },
  closeText: { color: "#6b7280", fontSize: 12 },
});
