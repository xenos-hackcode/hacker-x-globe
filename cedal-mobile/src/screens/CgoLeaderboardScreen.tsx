// src/screens/CgoLeaderboardScreen.tsx
import React from "react";
import { View, Text, StyleSheet } from "react-native";

export default function CgoLeaderboardScreen() {
  return (
    <View style={styles.root}>
      <Text style={styles.title}>Leaderboard</Text>
      <Text style={styles.subtitle}>
       See top developers using Cedal around the world.
      </Text>
      <Text style={styles.hint}>
       Later: connect to backend scores, show global ranks, and your position.
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: "#020617",
    padding: 20,
    justifyContent: "center",
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
    marginBottom: 6,
  },
  hint: {
    color: "#6b7280",
    fontSize: 12,
  },
});
