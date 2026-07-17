// src/member/profile/components/ActivitySection.tsx
import React from "react";
import { View, Text, StyleSheet } from "react-native";

type Props = {
  level: number;
  points: number;
  messages: number;
  stickers: number;
  streak: number;
  reputation: number;
};

export function ActivitySection({
  level,
  points,
  messages,
  stickers,
  streak,
  reputation,
}: Props) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>Activity & reputation</Text>

      <View style={styles.statsGrid}>
        <StatCard label="Level" value={`Lv. ${level}`} />
        <StatCard label="Points" value={`${points}`} />
        <StatCard label="Messages sent" value={`${messages}`} />
        <StatCard label="Stickers sent" value={`${stickers}`} />
        <StatCard label="Streak" value={`${streak} days`} />
        <StatCard label="Reputation" value={`${reputation}`} />
      </View>
    </View>
  );
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <View style={styles.card}>
      <Text style={styles.cardLabel}>{label}</Text>
      <Text style={styles.cardValue}>{value}</Text>
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
  statsGrid: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
  card: {
    flexBasis: "48%",
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(30,64,175,0.7)",
    backgroundColor: "#020617",
    padding: 8,
  },
  cardLabel: {
    fontSize: 9,
    color: "#9ca3af",
    textTransform: "uppercase",
  },
  cardValue: {
    fontSize: 11,
    color: "#e5e7eb",
    marginTop: 2,
  },
});
