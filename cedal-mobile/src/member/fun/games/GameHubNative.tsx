// src/member/fun/games/GameHubNative.tsx
import { Game, useGames } from "@/src/hooks/useGames";
import { useUserProfile } from "@/src/hooks/useUserProfile";
import { useTheme } from "@/src/themes/ThemeContext";
import { useRouter } from "expo-router";
import React from "react";
import {
  Image,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

export type GameGenreFilter = "all" | "Action" | "Casual" | "RPG" | "Simulation";

const MOCK_GAMES: Game[] = [
  {
    id: "mock-g1",
    name: "Neon Blocks",
    createdBy: "app",
    genre: "Action",
    players: 1284,
    likes: 342,
    createdAt: Date.now(),
  },
  {
    id: "mock-g2",
    name: "Orbital Drift",
    createdBy: "ai",
    genre: "Simulation",
    players: 532,
    likes: 201,
    createdAt: Date.now(),
  },
  {
    id: "mock-g3",
    name: "Void Runner",
    createdBy: "user",
    genre: "RPG",
    players: 78,
    likes: 29,
    createdAt: Date.now(),
  },
  {
    id: "mock-g4",
    name: "Gridfall",
    createdBy: "app",
    genre: "Casual",
    players: 412,
    likes: 150,
    createdAt: Date.now(),
  },
];

type Props = {
  onClose?: () => void;
  onSelectGame?: (game: Game) => void;
  onCreateGame?: () => void;
};

export default function GameHub({ onClose, onSelectGame, onCreateGame }: Props) {
  const { colors, isDark } = useTheme();
  const styles = makeStyles(colors, isDark);
  const router = useRouter();
  const { profile } = useUserProfile();
  const { games, loading } = useGames();

  const avatarUrl = (profile as any)?.avatarUrl ?? null;

  const [filter, setFilter] = React.useState<GameGenreFilter>("all");

  const sourceGames = games.length > 0 ? games : MOCK_GAMES;
  const filteredGames = sourceGames.filter((g) =>
    filter === "all" ? true : g.genre === filter
  );

  return (
    <View style={styles.root}>
      {/* HEADER */}
      <View style={styles.headerBlock}>
        <View style={styles.topRow}>
          <TouchableOpacity
            activeOpacity={0.8}
            onPress={() => router.push("/(auth)/(member)/profile" as any)}
          >
            <View style={styles.avatarCircle}>
              {avatarUrl ? (
                <Image source={{ uri: avatarUrl }} style={styles.avatarImage} />
              ) : (
                <View style={styles.avatarFallback} />
              )}
            </View>
          </TouchableOpacity>

          <TouchableOpacity
            activeOpacity={0.8}
            onPress={() => router.push("/(auth)/(member)/settings" as any)}
          >
            <View style={styles.settingsCircle}>
              <Text style={styles.settingsIcon}>⚙️</Text>
            </View>
          </TouchableOpacity>
        </View>

        <View style={styles.headerCenter}>
          <Text style={styles.headerTitle}>Game hub</Text>
          <Text style={styles.headerSubtitle}>
            Discover, pin, and launch shared worlds.
          </Text>
        </View>

        <View style={styles.actionRow}>
          <TouchableOpacity
            activeOpacity={0.9}
            onPress={onCreateGame}
            style={styles.plusBtn}
          >
            <Text style={styles.plusText}>＋ Create game</Text>
          </TouchableOpacity>

          <TouchableOpacity
            activeOpacity={0.8}
            onPress={onClose}
            style={styles.closeBtn}
          >
            <Text style={styles.closeText}>Close</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.subHeader}>
          <Text style={styles.subHeaderText}>
            {loading ? "Loading games…" : `${filteredGames.length} games · scroll to explore`}
          </Text>
        </View>
      </View>

      {/* SCROLLABLE LIST */}
      <View style={styles.listContainer}>
        <ScrollView
          style={styles.list}
          contentContainerStyle={styles.listContent}
          showsVerticalScrollIndicator={false}
        >
          {filteredGames.map((game) => (
            <View key={game.id} style={styles.row}>
              <View style={styles.iconTile} />

              <View style={styles.midBlock}>
                <Text style={styles.gameName}>{game.name}</Text>
                <View style={styles.metaRow}>
                  <Text style={styles.metaText}>
                    {game.createdBy === "ai"
                      ? "AI‑created"
                      : game.createdBy === "user"
                      ? "User‑created"
                      : "App‑created"}
                  </Text>
                  <Text style={styles.metaDot}>•</Text>
                  <Text style={styles.metaText}>{game.genre}</Text>
                </View>
              </View>

              <View style={styles.stats}>
                <Text style={styles.statsText}>
                  {game.players.toLocaleString()} players
                </Text>
                <Text style={styles.statsText}>
                  {game.likes.toLocaleString()} likes
                </Text>
              </View>

              <TouchableOpacity
                style={styles.enterBtn}
                activeOpacity={0.9}
                onPress={() => onSelectGame?.(game)}
              >
                <Text style={styles.enterText}>Enter</Text>
              </TouchableOpacity>
            </View>
          ))}
        </ScrollView>
      </View>

      {/* BOTTOM FILTER BAR */}
      <View style={styles.bottomBar}>
        {[
          { key: "all", label: "All" },
          { key: "Action", label: "Action" },
          { key: "Casual", label: "Casual" },
          { key: "RPG", label: "RPG" },
          { key: "Simulation", label: "Simulation" },
        ].map((item) => {
          const active = filter === item.key;
          return (
            <TouchableOpacity
              key={item.key}
              onPress={() => setFilter(item.key as GameGenreFilter)}
              activeOpacity={0.9}
              style={[styles.filterChip, active && styles.filterChipActive]}
            >
              <Text
                style={[
                  styles.filterText,
                  active && styles.filterTextActive,
                ]}
              >
                {item.label}
              </Text>
            </TouchableOpacity>
          );
        })}
      </View>
    </View>
  );
}

const makeStyles = (colors: any, isDark: boolean) =>
  StyleSheet.create({
    root: {
      borderRadius: 18,
      borderWidth: 1,
      borderColor: "rgba(15,23,42,1)",
      backgroundColor: "#020617",
      padding: 16,
      flex: 1,
    },
    headerBlock: {
      marginBottom: 10,
    },
    topRow: {
      flexDirection: "row",
      justifyContent: "space-between",
      alignItems: "center",
      marginBottom: 10,
    },
    avatarCircle: {
      width: 32,
      height: 32,
      borderRadius: 16,
      borderWidth: 1,
      borderColor: "rgba(56,189,248,0.9)",
      overflow: "hidden",
      backgroundColor: "#0f172a",
    },
    avatarImage: {
      width: "100%",
      height: "100%",
    },
    avatarFallback: {
      flex: 1,
      backgroundColor: "#0f172a",
    },
    settingsCircle: {
      width: 30,
      height: 30,
      borderRadius: 15,
      borderWidth: 1,
      borderColor: "rgba(148,163,184,0.9)",
      alignItems: "center",
      justifyContent: "center",
      backgroundColor: "#020617",
    },
    settingsIcon: {
      fontSize: 16,
      color: "#e5e7eb",
    },
    headerCenter: {
      marginBottom: 10,
    },
    headerTitle: {
      fontSize: 18,
      fontWeight: "600",
      color: "#e5e7eb",
    },
    headerSubtitle: {
      fontSize: 12,
      color: "#9ca3af",
      marginTop: 2,
    },
    actionRow: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      marginBottom: 10,
      gap: 8,
    },
    plusBtn: {
      flex: 1,
      borderRadius: 999,
      borderWidth: 1,
      borderColor: "rgba(56,189,248,0.9)",
      backgroundColor: "#0b1120",
      paddingVertical: 6,
      paddingHorizontal: 12,
      alignItems: "center",
      justifyContent: "center",
    },
    plusText: {
      fontSize: 12,
      color: "#e0f2fe",
      letterSpacing: 0.6,
      textTransform: "uppercase",
    },
    closeBtn: {
      paddingHorizontal: 10,
      paddingVertical: 6,
      borderRadius: 999,
      borderWidth: 1,
      borderColor: "rgba(148,163,184,0.7)",
      backgroundColor: "#020617",
    },
    closeText: {
      fontSize: 11,
      color: "#9ca3af",
      letterSpacing: 0.8,
      textTransform: "uppercase",
    },
    subHeader: {
      borderRadius: 12,
      borderWidth: 1,
      borderColor: "rgba(31,41,55,1)",
      paddingHorizontal: 8,
      paddingVertical: 4,
      marginTop: 2,
    },
    subHeaderText: {
      fontSize: 11,
      color: "#6b7280",
    },
    listContainer: {
      flex: 1,
      marginTop: 8,
      marginBottom: 10,
    },
    list: {
      flex: 1,
      borderRadius: 16,
      borderWidth: 1,
      borderColor: "rgba(31,41,55,1)",
      backgroundColor: "#020617",
    },
    listContent: {
      paddingVertical: 4,
    },
    row: {
      flexDirection: "row",
      alignItems: "center",
      paddingHorizontal: 8,
      paddingVertical: 8,
      borderBottomWidth: StyleSheet.hairlineWidth,
      borderBottomColor: "rgba(31,41,55,0.7)",
      gap: 10,
    },
    iconTile: {
      width: 32,
      height: 32,
      borderRadius: 10,
      borderWidth: 1,
      borderColor: "rgba(56,189,248,0.8)",
      backgroundColor: "#0f172a",
    },
    midBlock: {
      flex: 1,
      minWidth: 0,
    },
    gameName: {
      fontSize: 13,
      fontWeight: "600",
      color: "#e5e7eb",
    },
    metaRow: {
      flexDirection: "row",
      alignItems: "center",
      gap: 6,
      marginTop: 2,
    },
    metaText: {
      fontSize: 11,
      color: "#9ca3af",
    },
    metaDot: {
      fontSize: 11,
      color: "#4b5563",
    },
    stats: {
      alignItems: "flex-end",
      marginRight: 6,
    },
    statsText: {
      fontSize: 11,
      color: "#9ca3af",
    },
    enterBtn: {
      borderRadius: 999,
      borderWidth: 1,
      borderColor: "rgba(56,189,248,0.9)",
      paddingHorizontal: 10,
      paddingVertical: 4,
      backgroundColor: "#0f172a",
    },
    enterText: {
      fontSize: 11,
      color: "#e0f2fe",
      letterSpacing: 0.8,
      textTransform: "uppercase",
    },
    bottomBar: {
      flexDirection: "row",
      justifyContent: "space-between",
      alignItems: "center",
      gap: 6,
      paddingVertical: 6,
      paddingHorizontal: 8,
      borderRadius: 999,
      borderWidth: 1,
      borderColor: "rgba(31,41,55,1)",
      backgroundColor: "#020617",
    },
    filterChip: {
      flex: 1,
      borderRadius: 999,
      paddingVertical: 4,
      paddingHorizontal: 8,
      alignItems: "center",
      justifyContent: "center",
      borderWidth: 1,
      borderColor: "rgba(55,65,81,0.8)",
      backgroundColor: "#020617",
    },
    filterChipActive: {
      borderColor: "rgba(56,189,248,0.9)",
      backgroundColor: "#0b1120",
    },
    filterText: {
      fontSize: 11,
      color: "#9ca3af",
      letterSpacing: 0.3,
      textTransform: "uppercase",
    },
    filterTextActive: {
      color: "#e5e7eb",
    },
  });
