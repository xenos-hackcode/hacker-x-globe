// app/(auth)/(member)/history.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import { useRouter } from "expo-router";
import React, { useMemo, useState } from "react";
import {
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

type HistoryKind =
  | "group"
  | "games"
  | "guilds"
  | "chats"
  | "calls"
  | "streams"
  | "bank"
  | "invest"
  | "code"
  | "hack"
  | "bots";

type HistoryItem = {
  id: string;
  kind: HistoryKind;
  title: string;
  subtitle: string;
  timestamp: string; // ISO date string
};

// later you’ll load this from backend
const MOCK_HISTORY: HistoryItem[] = [
  {
    id: "1",
    kind: "chats",
    title: "Chat with Corneal",
    subtitle: "Continued a deep dive thread.",
    timestamp: "2026-01-24T21:14:00Z",
  },
  {
    id: "2",
    kind: "bank",
    title: "Bank session",
    subtitle: "Focused for 45 minutes.",
    timestamp: "2026-01-24T19:02:00Z",
  },
  {
    id: "3",
    kind: "games",
    title: "Bloodstrike link",
    subtitle: "Docked a game lobby to a group.",
    timestamp: "2026-01-10T16:30:00Z",
  },
];

const FILTERS: { id: HistoryKind; label: string }[] = [
  { id: "group", label: "Groups" },
  { id: "games", label: "Games" },
  { id: "guilds", label: "Guilds" },
  { id: "chats", label: "Chats" },
  { id: "calls", label: "Calls" },
  { id: "streams", label: "Streams" },
  { id: "bank", label: "Bank" },
  { id: "invest", label: "Invest" },
  { id: "code", label: "Code" },
  { id: "hack", label: "Hack" },
  { id: "bots", label: "Bots" },
];

export default function HistoryScreen() {
  const router = useRouter();
  const { colors } = useTheme();

  const [activeFilter, setActiveFilter] = useState<HistoryKind | "all">("all");

  function handleBack() {
    router.back();
  }

  const filtered = useMemo(() => {
    if (activeFilter === "all") return MOCK_HISTORY;
    return MOCK_HISTORY.filter((item) => item.kind === activeFilter);
  }, [activeFilter]);

  // group by month label like "January 2026"
  const groupedByMonth = useMemo(() => {
    const groups: Record<string, HistoryItem[]> = {};
    for (const item of filtered) {
      const date = new Date(item.timestamp);
      const label = date.toLocaleString("default", {
        month: "long",
        year: "numeric",
      });
      if (!groups[label]) groups[label] = [];
      groups[label].push(item);
    }
    // sort months descending by date
    const entries = Object.entries(groups).sort(
      ([a], [b]) =>
        new Date(b + " 1").getTime() - new Date(a + " 1").getTime()
    );
    return entries;
  }, [filtered]);

  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      {/* Top bar */}
      <View
        style={[
          styles.topBar,
          {
            borderBottomColor: colors.border,
            backgroundColor:
              (colors as any).headerBackground ?? colors.background,
          },
        ]}
      >
        <TouchableOpacity
          onPress={handleBack}
          activeOpacity={0.7}
          style={styles.backBtn}
        >
          <Text style={[styles.backText, { color: colors.textPrimary }]}>
            Back
          </Text>
        </TouchableOpacity>
        <Text style={[styles.topTitle, { color: colors.textPrimary }]}>
          History
        </Text>
      </View>

      {/* Filters row */}
      <View
        style={[
          styles.filtersBar,
          { borderBottomColor: colors.border, backgroundColor: colors.background },
        ]}
      >
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.filtersContent}
        >
          <Chip
            label="All"
            active={activeFilter === "all"}
            onPress={() => setActiveFilter("all")}
          />
          {FILTERS.map((f) => (
            <Chip
              key={f.id}
              label={f.label}
              active={activeFilter === f.id}
              onPress={() => setActiveFilter(f.id)}
            />
          ))}
        </ScrollView>
      </View>

      {/* History list */}
      <ScrollView contentContainerStyle={styles.historyContent}>
        {groupedByMonth.length === 0 && (
          <View style={styles.emptyState}>
            <Text style={[styles.emptyTitle, { color: colors.textPrimary }]}>
              Nothing yet.
            </Text>
            <Text style={[styles.emptyBody, { color: colors.textSecondary }]}>
              When you chat, grind, play, or call, your activity will show up
              here grouped by month.
            </Text>
          </View>
        )}

        {groupedByMonth.map(([monthLabel, items]) => (
          <View key={monthLabel} style={styles.monthBlock}>
            <Text style={[styles.monthLabel, { color: colors.textSecondary }]}>
              {monthLabel}
            </Text>
            <View
              style={[
                styles.monthCard,
                { borderColor: colors.border, backgroundColor: colors.background },
              ]}
            >
              {items
                .sort(
                  (a, b) =>
                    new Date(b.timestamp).getTime() -
                    new Date(a.timestamp).getTime()
                )
                .map((item, index) => (
                  <View
                    key={item.id}
                    style={[
                      styles.itemRow,
                      index === items.length - 1 && styles.itemRowLast,
                    ]}
                  >
                    <View style={styles.itemDotWrap}>
                      <View style={styles.itemDot} />
                    </View>
                    <View style={styles.itemTextBlock}>
                      <Text
                        style={[styles.itemTitle, { color: colors.textPrimary }]}
                        numberOfLines={1}
                      >
                        {item.title}
                      </Text>
                      <Text
                        style={[
                          styles.itemSubtitle,
                          { color: colors.textSecondary },
                        ]}
                        numberOfLines={1}
                      >
                        {item.subtitle}
                      </Text>
                    </View>
                    <Text
                      style={[
                        styles.itemTime,
                        { color: colors.textSecondary },
                      ]}
                    >
                      {new Date(item.timestamp).toLocaleTimeString(undefined, {
                        hour: "2-digit",
                        minute: "2-digit",
                      })}
                    </Text>
                  </View>
                ))}
            </View>
          </View>
        ))}
      </ScrollView>
    </View>
  );
}

function Chip({
  label,
  active,
  onPress,
}: {
  label: string;
  active: boolean;
  onPress: () => void;
}) {
  return (
    <TouchableOpacity
      style={[styles.chip, active && styles.chipActive]}
      activeOpacity={0.8}
      onPress={onPress}
    >
      <Text style={[styles.chipText, active && styles.chipTextActive]}>
        {label}
      </Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
  },
  topBar: {
    paddingTop: 48,
    paddingHorizontal: 16,
    paddingBottom: 12,
    flexDirection: "row",
    alignItems: "center",
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  backBtn: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
    marginRight: 12,
  },
  backText: {
    fontSize: 13,
    letterSpacing: 1.5,
  },
  topTitle: {
    fontSize: 16,
    fontWeight: "600",
    letterSpacing: 2,
    textTransform: "uppercase",
  },
  filtersBar: {
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  filtersContent: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    gap: 8,
  },
  chip: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(55,65,81,0.9)",
  },
  chipActive: {
    borderColor: "#22c55e",
    backgroundColor: "rgba(34,197,94,0.15)",
  },
  chipText: {
    fontSize: 11,
    color: "#9ca3af",
    letterSpacing: 1,
    textTransform: "uppercase",
  },
  chipTextActive: {
    color: "#e5e7eb",
  },
  historyContent: {
    paddingHorizontal: 16,
    paddingVertical: 16,
    paddingBottom: 32,
  },
  monthBlock: {
    marginBottom: 16,
  },
  monthLabel: {
    fontSize: 11,
    letterSpacing: 2,
    textTransform: "uppercase",
    marginBottom: 4,
  },
  monthCard: {
    borderRadius: 18,
    borderWidth: 1,
    overflow: "hidden",
  },
  itemRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(31,41,55,0.9)",
  },
  itemRowLast: {
    borderBottomWidth: 0,
  },
  itemDotWrap: {
    width: 18,
    alignItems: "center",
  },
  itemDot: {
    width: 6,
    height: 6,
    borderRadius: 999,
    backgroundColor: "#22c55e",
  },
  itemTextBlock: {
    flex: 1,
    marginRight: 8,
  },
  itemTitle: {
    fontSize: 13,
    fontWeight: "500",
  },
  itemSubtitle: {
    fontSize: 11,
    marginTop: 2,
  },
  itemTime: {
    fontSize: 11,
  },
  emptyState: {
    alignItems: "center",
    paddingVertical: 40,
  },
  emptyTitle: {
    fontSize: 14,
    fontWeight: "600",
    marginBottom: 4,
  },
  emptyBody: {
    fontSize: 12,
    textAlign: "center",
    maxWidth: 260,
  },
});
