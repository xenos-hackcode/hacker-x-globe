// src/member/fun/games/GameDescript.tsx
import React from "react";
import { View, Text, StyleSheet, TouchableOpacity } from "react-native";

export default function GameDescript({
  game,
  onClose,
}: {
  game: any;
  onClose: () => void;
}) {
  return (
    <View style={styles.root}>
      <View style={styles.headerRow}>
        <Text style={styles.title}>{game.name}</Text>
        <TouchableOpacity
          onPress={onClose}
          style={styles.closeBtn}
          activeOpacity={0.8}
        >
          <Text style={styles.closeText}>Back</Text>
        </TouchableOpacity>
      </View>

      <Text style={styles.meta}>
        {game.genre} · {game.players.toLocaleString()} players ·{" "}
        {game.likes.toLocaleString()} likes
      </Text>

      <Text style={styles.body}>
        This is where you can describe the world, matchmaking rules, or attach it
        to a guild/group.
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    borderRadius: 18,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,1)",
    backgroundColor: "#020617",
    padding: 16,
  },
  headerRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 8,
  },
  title: {
    fontSize: 16,
    fontWeight: "600",
    color: "#e5e7eb",
  },
  closeBtn: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.8)",
  },
  closeText: {
    fontSize: 11,
    color: "#9ca3af",
    letterSpacing: 1,
    textTransform: "uppercase",
  },
  meta: {
    fontSize: 11,
    color: "#9ca3af",
    marginBottom: 8,
  },
  body: {
    fontSize: 12,
    color: "#e5e7eb",
  },
});
