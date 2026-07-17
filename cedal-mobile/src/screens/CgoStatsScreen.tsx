// src/screens/CgoStatsScreen.tsx
import React from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
} from "react-native";

type Props = {
  navigation: any;
  route: {
    params: {
      fileName?: string;
      folderPath?: string;
      language?: string;
      submissionStatus?: string;
      adminFeedback?: string;
      creatorFeedback?: string;
    };
  };
};

export default function CgoStatsScreen({ navigation, route }: Props) {
  const {
    fileName = "Unknown",
    folderPath = "/",
    language = "Unknown",
    submissionStatus = "idle",
    adminFeedback = "",
    creatorFeedback = "",
  } = route.params ?? {};

  function statusLabel() {
    switch (submissionStatus) {
      case "idle":
        return "Not submitted";
      case "submitted":
        return "Submitted (waiting for admin)";
      case "admin_approved":
        return "Approved by admin (with creator)";
      case "admin_rejected":
        return "Rejected by admin";
      case "creator_approved":
        return "Approved by creator (final)";
      case "creator_rejected":
        return "Rejected by creator (final)";
      default:
        return submissionStatus;
    }
  }

  function feedbackText() {
    if (submissionStatus === "admin_rejected" && adminFeedback) {
      return `Admin: ${adminFeedback}`;
    }
    if (submissionStatus === "creator_rejected" && creatorFeedback) {
      return `Creator: ${creatorFeedback}`;
    }
    if (submissionStatus === "creator_approved") {
      return "This file is approved and treated as final.";
    }
    if (submissionStatus === "admin_approved") {
      return "Admin approved this and sent it to the creator.";
    }
    if (submissionStatus === "submitted") {
      return "Waiting for admin to run and review your work.";
    }
    return "No feedback yet.";
  }

  return (
    <View style={styles.root}>
      <TouchableOpacity
        style={styles.backButton}
        onPress={() => navigation.goBack()}
      >
        <Text style={styles.backText}>← Back</Text>
      </TouchableOpacity>

      <Text style={styles.title}>Your stats</Text>
      <Text style={styles.subtitle}>
        See the status of this file and how it was reviewed.
      </Text>

      <View style={styles.card}>
        <Text style={styles.label}>File</Text>
        <Text style={styles.value}>{fileName}</Text>

        <Text style={styles.label}>Folder</Text>
        <Text style={styles.value}>{folderPath}</Text>

        <Text style={styles.label}>Language</Text>
        <Text style={styles.value}>{language}</Text>

        <Text style={styles.label}>Submission status</Text>
        <Text style={styles.value}>{statusLabel()}</Text>

        <Text style={styles.label}>Feedback</Text>
        <ScrollView style={styles.feedbackBox}>
          <Text style={styles.feedbackText}>{feedbackText()}</Text>
        </ScrollView>
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
    marginBottom: 8,
  },
  subtitle: {
    color: "#9ca3af",
    fontSize: 14,
    marginBottom: 12,
  },
  card: {
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    backgroundColor: "rgba(15,23,42,0.98)",
    padding: 12,
  },
  label: {
    color: "#6b7280",
    fontSize: 11,
    marginTop: 6,
  },
  value: {
    color: "#e5e7eb",
    fontSize: 13,
  },
  feedbackBox: {
    maxHeight: 140,
    marginTop: 4,
  },
  feedbackText: {
    color: "#e5e7eb",
    fontSize: 12,
  },
});
