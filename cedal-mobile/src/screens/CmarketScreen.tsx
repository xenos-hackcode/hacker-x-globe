// src/screens/CmarketScreen.tsx
import React from "react";
import { View, Text, StyleSheet, TouchableOpacity } from "react-native";
import { useRouter } from "expo-router";

export default function CmarketScreen() {
  const router = useRouter();

  return (
    <View style={styles.root}>
      <TouchableOpacity
        style={styles.backButton}
        onPress={() => router.back()}
      >
        <Text style={styles.backText}>← Back</Text>
      </TouchableOpacity>

      <Text style={styles.title}>Cmarket</Text>
      <Text style={styles.subtitle}>No items in stock yet.</Text>
      <Text style={styles.hint}>
        Soon you’ll be able to buy upgrades like advanced admin here.
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
    marginTop: 40,
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
