// src/member/profile/component/QuickActionsSection.tsx
import React from "react";
import { View, Text, StyleSheet, TouchableOpacity } from "react-native";

type Props = {
  randomLink: string;
  onOpenGroups?: () => void;
};

export function QuickActionsSection({ randomLink, onOpenGroups }: Props) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>Quick actions</Text>

      <View style={styles.quickGrid}>
        {["Group", "Guild", "Medias", "Files"].map((label) => {
          const isGroup = label === "Group";
          return (
            <TouchableOpacity
              key={label}
              style={styles.quickBtn}
              activeOpacity={0.8}
              onPress={isGroup ? onOpenGroups : undefined}
            >
              <View style={styles.quickDot} />
              <Text style={styles.quickText}>{label}</Text>
            </TouchableOpacity>
          );
        })}
      </View>

      <TouchableOpacity style={styles.randomBtn} activeOpacity={0.8}>
        <View style={styles.randomDot} />
        <Text style={styles.randomText}>
          Random link ·{" "}
          {(randomLink || "https://cedal.dev/xxxxxx").replace(
            "https://",
            ""
          )}
        </Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  section: { marginBottom: 16 },
  sectionTitle: {
    fontSize: 13,
    color: "#e5e7eb",
    fontWeight: "600",
    marginBottom: 8,
  },
  quickGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginBottom: 8,
  },
  quickBtn: {
    flexBasis: "48%",
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.7)",
    backgroundColor: "#020617",
    paddingVertical: 6,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 6,
  },
  quickDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: "#38bdf8" },
  quickText: {
    fontSize: 11,
    color: "#e5e7eb",
    letterSpacing: 0.12,
    textTransform: "uppercase",
  },
  randomBtn: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(52,211,153,0.7)",
    backgroundColor: "#020617",
    paddingVertical: 6,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    gap: 6,
  },
  randomDot: { width: 6, height: 6, borderRadius: 3, backgroundColor: "#22c55e" },
  randomText: {
    fontSize: 11,
    color: "#bbf7d0",
    letterSpacing: 0.12,
    textTransform: "uppercase",
  },
});
