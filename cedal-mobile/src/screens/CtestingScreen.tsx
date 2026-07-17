// src/screens/CtestingScreen.tsx
import React, { useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
} from "react-native";
import { useLocalSearchParams, useRouter } from "expo-router";

export default function CtestingScreen() {
  const router = useRouter();
  const params = useLocalSearchParams<{
    language?: string;
    code?: string;
    fileName?: string;
  }>();

  const language = params.language ?? "Unknown";
  const fileName = params.fileName ?? "Untitled";
  const code = params.code ?? "";

  const [output, setOutput] = useState<string>("");

  function handleRun() {
    // For now just simulate a run; later call your backend here
    const timestamp = new Date().toLocaleTimeString();
    const fakeResult = `▶ [${timestamp}] Ran ${fileName} as ${language}.
(No real execution yet — wire this to your backend runner.)`;

    setOutput(fakeResult);
  }

  return (
    <View style={styles.root}>
      <TouchableOpacity
        style={styles.backButton}
        onPress={() => router.back()}
      >
        <Text style={styles.backText}>← Back</Text>
      </TouchableOpacity>

      <Text style={styles.title}>Ctesting</Text>
      <Text style={styles.subtitle}>
        Console view of your current file from Work.
      </Text>

      <View style={styles.infoRow}>
        <Text style={styles.infoLabel}>FILE</Text>
        <Text style={styles.infoValue}>{fileName}</Text>
      </View>

      <View style={styles.infoRow}>
        <Text style={styles.infoLabel}>LANGUAGE</Text>
        <Text style={styles.infoValue}>{language}</Text>
      </View>

      <View style={styles.section}>
        <Text style={styles.sectionTitle}>Source</Text>
        <View style={styles.codeBox}>
          <ScrollView
            style={{ maxHeight: 200 }}
            contentContainerStyle={styles.codeScrollContent}
          >
            <Text style={styles.codeText}>
              {code.length ? code : "// No code yet"}
            </Text>
          </ScrollView>
        </View>
      </View>

      <View style={styles.section}>
        <View style={styles.outputHeader}>
          <Text style={styles.sectionTitle}>Output</Text>
          <TouchableOpacity style={styles.runButton} onPress={handleRun}>
            <Text style={styles.runText}>Run</Text>
          </TouchableOpacity>
        </View>
        <View style={styles.outputBox}>
          <ScrollView
            style={{ maxHeight: 160 }}
            contentContainerStyle={styles.codeScrollContent}
          >
            <Text style={styles.outputText}>
              {output || "Press Run to execute this file (simulated)."}
            </Text>
          </ScrollView>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: "#020617",
    padding: 20,
    paddingTop: 70,
  },
  backButton: {
    position: "absolute",
    top: 40,
    left: 20,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    backgroundColor: "rgba(15,23,42,0.9)",
  },
  backText: {
    color: "#e5e7eb",
    fontSize: 12,
    fontWeight: "500",
  },
  title: {
    color: "#e5e7eb",
    fontSize: 20,
    fontWeight: "700",
    marginBottom: 4,
  },
  subtitle: {
    color: "#9ca3af",
    fontSize: 13,
    marginBottom: 12,
  },
  infoRow: {
    flexDirection: "row",
    marginBottom: 4,
  },
  infoLabel: {
    color: "#6b7280",
    fontSize: 11,
    width: 70,
  },
  infoValue: {
    color: "#e5e7eb",
    fontSize: 12,
    flex: 1,
  },
  section: {
    marginTop: 12,
  },
  sectionTitle: {
    color: "#e5e7eb",
    fontSize: 13,
    fontWeight: "600",
    marginBottom: 4,
  },
  codeBox: {
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    backgroundColor: "rgba(15,23,42,0.98)",
    paddingHorizontal: 8,
    paddingVertical: 6,
  },
  codeScrollContent: {
    paddingBottom: 4,
  },
  codeText: {
    color: "#e5e7eb",
    fontSize: 11,
    fontFamily: "monospace",
  },
  outputHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  runButton: {
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#22d3ee",
    backgroundColor: "rgba(15,23,42,0.95)",
  },
  runText: {
    color: "#e5e7eb",
    fontSize: 11,
    fontWeight: "600",
  },
  outputBox: {
    marginTop: 4,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    backgroundColor: "rgba(3,7,18,0.98)",
    paddingHorizontal: 8,
    paddingVertical: 6,
  },
  outputText: {
    color: "#a5b4fc",
    fontSize: 11,
    fontFamily: "monospace",
  },
});
