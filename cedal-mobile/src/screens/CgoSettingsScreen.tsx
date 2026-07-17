// src/screens/CgoSettingsScreen.tsx
import React from "react";
import { View, Text, StyleSheet } from "react-native";

export default function CgoSettingsScreen() {
  return (
    <View style={styles.root}>
      <Text style={styles.title}>Cgo Settings</Text>
      <Text style={styles.subtitle}>
       Here you’ll pick Cedal phone background colors and custom wallpapers.
      </Text>
      <Text style={styles.hint}>
       Later: add color pickers, theme presets, and background image upload.
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
