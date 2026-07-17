// app/(modals)/panel.tsx
import React from "react";
import { View, Text, StyleSheet, TouchableOpacity } from "react-native";
import { router } from "expo-router";

export default function MemberPanelModal() {
  return (
    <View style={styles.screen}>
      <View style={styles.topBar}>
        <TouchableOpacity
          onPress={() => router.back()}
          activeOpacity={0.7}
          style={styles.backBtn}
        >
          <Text style={styles.backText}>←</Text>
        </TouchableOpacity>
        <Text style={styles.topTitle}>Member Panel</Text>
      </View>

      <View style={styles.body}>
        <Text style={styles.title}>Member Panel</Text>
        <Text style={styles.sub}>
          Full-screen system view opened from the top circle avatar.
        </Text>
        {/* put stats / AIs / guilds etc here */}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#020617",
  },
  topBar: {
    paddingTop: 48,
    paddingHorizontal: 16,
    paddingBottom: 12,
    flexDirection: "row",
    alignItems: "center",
  },
  backBtn: {
    width: 32,
    height: 32,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 10,
  },
  backText: {
    color: "#e5e7eb",
    fontSize: 18,
  },
  topTitle: {
    color: "#e5e7eb",
    fontSize: 16,
    fontWeight: "700",
    letterSpacing: 2,
  },
  body: {
    flex: 1,
    padding: 20,
  },
  title: {
    color: "#e5e7eb",
    fontSize: 20,
    fontWeight: "700",
    letterSpacing: 2,
    marginBottom: 8,
  },
  sub: {
    color: "#9ca3af",
    fontSize: 13,
  },
});
