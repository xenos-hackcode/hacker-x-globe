// src/screens/CgoScreen.tsx
import React from "react";
import { View, Text, StyleSheet, TouchableOpacity } from "react-native";

type Props = {
  navigation: any;
  route: any;
};

export default function CgoScreen({ navigation }: Props) {
  // TODO: replace these with real data from Firestore / params
  const fileName = "main.py";
  const folderPath = "/src";
  const language = "Python";
  const submissionStatus = "creator_approved";
  const adminFeedback = "";
  const creatorFeedback = "Looks good, approved.";

  function openSettings() {
    navigation.navigate("CgoSettingsScreen");
  }

  function openLeaderboard() {
    navigation.navigate("CgoLeaderboardScreen");
  }

  function openStats() {
    navigation.navigate("CgoStatsScreen", {
      fileName,
      folderPath,
      language,
      submissionStatus,
      adminFeedback,
      creatorFeedback,
    });
  }

  return (
    <View style={styles.root}>
      <TouchableOpacity
        style={styles.backButton}
        onPress={() => navigation.goBack()}
      >
        <Text style={styles.backText}>← Back</Text>
      </TouchableOpacity>

      <Text style={styles.title}>Cgo</Text>
      <Text style={styles.subtitle}>
        Control your Cedal phone, see your performance, and compare with others.
      </Text>

      <View style={styles.buttonsRow}>
        <TouchableOpacity style={styles.button} onPress={openSettings}>
          <Text style={styles.buttonText}>Settings</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.button} onPress={openLeaderboard}>
          <Text style={styles.buttonText}>Leaderboard</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.button} onPress={openStats}>
          <Text style={styles.buttonText}>Stats</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: "#020617",
    padding: 20,
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
    marginBottom: 8,
    marginTop: 60,
  },
  subtitle: {
    color: "#9ca3af",
    fontSize: 13,
    marginBottom: 20,
  },
  buttonsRow: {
    gap: 10,
  },
  button: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#22d3ee",
    paddingVertical: 10,
    paddingHorizontal: 18,
    alignItems: "center",
    backgroundColor: "rgba(15,23,42,0.9)",
  },
  buttonText: {
    color: "#e5e7eb",
    fontSize: 13,
    fontWeight: "600",
  },
});
