// src/member/work/hack/HackNetworkScreen.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import React, { useState } from "react";
import {
    FlatList,
    StyleSheet,
    Text,
    TouchableOpacity,
    View,
} from "react-native";

export type NetworkNode = {
  id: string;
  name: string;
  ip: string;
  os: string;
  zone: "external" | "dmz" | "internal";
  vulnHint: string;
  status: "up" | "down" | "compromised";
  difficulty: 1 | 2 | 3;
  flag?: string;
  discovered: boolean;
};

type Props = {
  nodes: NetworkNode[];
  onOpenNode?: (n: NetworkNode) => void;
  onAttackNode?: (
    n: NetworkNode,
    attack: "scan" | "bruteforce" | "exploit",
  ) => void;
};

export default function HackNetworkScreen({
  nodes,
  onOpenNode,
  onAttackNode,
}: Props) {
  const { colors } = useTheme();
  const [zoneFilter, setZoneFilter] = useState<
    "all" | "external" | "dmz" | "internal"
  >("all");

  const filtered = nodes.filter((n) =>
    zoneFilter === "all" ? true : n.zone === zoneFilter,
  );

  const zoneLabel = (z: NetworkNode["zone"]) =>
    z === "external" ? "Internet" : z === "dmz" ? "DMZ" : "Internal";

  const difficultyStars = (d: NetworkNode["difficulty"]) =>
    "★".repeat(d) + "☆".repeat(3 - d);

  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      {/* Filters / legend */}
      <View style={styles.topRow}>
        {(["all", "external", "dmz", "internal"] as const).map((key) => {
          const label =
            key === "all"
              ? "All"
              : key === "external"
              ? "Internet"
              : key === "dmz"
              ? "DMZ"
              : "Internal";
          const active = zoneFilter === key;
          return (
            <TouchableOpacity
              key={key}
              style={[styles.filterChip, active && styles.filterChipActive]}
              onPress={() => setZoneFilter(key)}
              activeOpacity={0.8}
            >
              <Text
                style={[
                  styles.filterChipText,
                  active && styles.filterChipTextActive,
                ]}
              >
                {label}
              </Text>
            </TouchableOpacity>
          );
        })}
      </View>

      {/* Network grid: each node = card */}
      <FlatList
        data={filtered}
        keyExtractor={(item) => item.id}
        numColumns={2}
        columnWrapperStyle={{ justifyContent: "space-between" }}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => {
          const statusColor =
            item.status === "up"
              ? "#22c55e"
              : item.status === "compromised"
              ? "#f97316"
              : "#6b7280";

          return (
            <TouchableOpacity
              style={styles.card}
              activeOpacity={0.8}
              onPress={() => onOpenNode?.(item)}
            >
              <View style={styles.cardHeader}>
                <View style={styles.nameRow}>
                  <View
                    style={[styles.statusDot, { backgroundColor: statusColor }]}
                  />
                  <Text style={styles.nameText} numberOfLines={1}>
                    {item.name}
                  </Text>
                </View>
                <Text style={styles.zoneText}>{zoneLabel(item.zone)}</Text>
              </View>

              <Text style={styles.ipText}>{item.ip}</Text>
              <Text style={styles.osText}>{item.os}</Text>

              <View style={styles.vulnPill}>
                <Ionicons
                  name="bug-outline"
                  size={12}
                  color="#f97316"
                  style={{ marginRight: 4 }}
                />
                <Text style={styles.vulnText} numberOfLines={1}>
                  {item.vulnHint}
                </Text>
              </View>

              <View style={styles.difficultyRow}>
                <Text style={styles.diffLabel}>Difficulty</Text>
                <Text style={styles.diffStars}>{difficultyStars(item.difficulty)}</Text>
              </View>

              {/* Quick actions */}
              <View style={styles.actionsRow}>
                <TouchableOpacity
                  style={styles.actionChip}
                  onPress={() => onAttackNode?.(item, "scan")}
                >
                  <Text style={styles.actionChipText}>Scan</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.actionChip}
                  onPress={() => onAttackNode?.(item, "bruteforce")}
                >
                  <Text style={styles.actionChipText}>Brute</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={styles.actionChip}
                  onPress={() => onAttackNode?.(item, "exploit")}
                >
                  <Text style={styles.actionChipText}>Exploit</Text>
                </TouchableOpacity>
              </View>
            </TouchableOpacity>
          );
        }}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Ionicons name="radio-outline" size={26} color="#4b5563" />
            <Text style={styles.emptyTitle}>No lab topology yet.</Text>
            <Text style={styles.emptySubtitle}>
              Later, you can spawn fake targets here and practice attacking them.
            </Text>
          </View>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  topRow: {
    flexDirection: "row",
    paddingHorizontal: 12,
    paddingTop: 4,
    paddingBottom: 4,
  },
  filterChip: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    backgroundColor: "rgba(15,23,42,0.9)",
    marginRight: 6,
  },
  filterChipActive: {
    backgroundColor: "#22c55e",
  },
  filterChipText: { fontSize: 11, color: "#e5e7eb" },
  filterChipTextActive: { color: "#020617", fontWeight: "600" },
  list: {
    paddingHorizontal: 16,
    paddingTop: 6,
    paddingBottom: 80,
  },
  card: {
    width: "48%",
    paddingVertical: 10,
    paddingHorizontal: 10,
    borderRadius: 12,
    marginBottom: 10,
    backgroundColor: "rgba(15,23,42,0.95)",
  },
  cardHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 4,
  },
  nameRow: {
    flexDirection: "row",
    alignItems: "center",
    flexShrink: 1,
  },
  statusDot: {
    width: 7,
    height: 7,
    borderRadius: 999,
    marginRight: 5,
  },
  nameText: {
    fontSize: 12,
    color: "#e5e7eb",
    fontWeight: "500",
  },
  zoneText: {
    fontSize: 10,
    color: "#9ca3af",
  },
  ipText: {
    fontSize: 11,
    color: "#9ca3af",
  },
  osText: {
    fontSize: 11,
    color: "#cbd5f5",
    marginTop: 2,
  },
  vulnPill: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 6,
    paddingHorizontal: 6,
    paddingVertical: 3,
    borderRadius: 999,
    backgroundColor: "rgba(248,113,113,0.1)",
  },
  vulnText: {
    fontSize: 10,
    color: "#fdba74",
    flexShrink: 1,
  },
  difficultyRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 6,
  },
  diffLabel: {
    fontSize: 10,
    color: "#9ca3af",
  },
  diffStars: {
    fontSize: 11,
    color: "#facc15",
  },
  actionsRow: {
    flexDirection: "row",
    marginTop: 8,
    justifyContent: "space-between",
  },
  actionChip: {
    flex: 1,
    marginHorizontal: 2,
    paddingVertical: 4,
    borderRadius: 999,
    backgroundColor: "rgba(51,65,85,0.9)",
    alignItems: "center",
  },
  actionChipText: {
    fontSize: 10,
    color: "#e5e7eb",
  },
  empty: {
    marginTop: 40,
    alignItems: "center",
    paddingHorizontal: 24,
  },
  emptyTitle: {
    color: "#e5e7eb",
    fontSize: 14,
    fontWeight: "500",
    marginTop: 8,
    textAlign: "center",
  },
  emptySubtitle: {
    color: "#9ca3af",
    fontSize: 12,
    marginTop: 4,
    textAlign: "center",
  },
});
