// src/member/work/banking/screens/TasksScreen.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import React, { useState } from "react";
import {
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

type Props = {
  onBackHome: () => void;
};

type TaskFilter = "daily" | "weekly" | "monthly" | "yearly" | "events";

export default function TasksScreen({ onBackHome }: Props) {
  const { colors } = useTheme();
  const [activeFilter, setActiveFilter] = useState<TaskFilter>("daily");

  return (
    <ScrollView
      style={[styles.root, { backgroundColor: colors.background }]}
      contentContainerStyle={styles.scrollContent}
    >
      {/* Header with back */}
      <View style={styles.header}>
        <TouchableOpacity
          style={styles.backBtn}
          onPress={onBackHome}
        >
          <Ionicons name="chevron-back" size={18} color="#60a5fa" />
          <Text style={[styles.backText, { color: "#60a5fa" }]}>Back</Text>
        </TouchableOpacity>

        <View style={styles.headerTextBlock}>
          <Text style={[styles.title, { color: colors.textPrimary }]}>
            Tasks
          </Text>
          <Text style={[styles.subTitle, { color: colors.textSecondary }]}>
            Complete tasks to earn XP, items, and Star Coins.
          </Text>
        </View>
      </View>

      {/* Tabs: daily, weekly, monthly, yearly, special events */}
      <View style={styles.tabsRow}>
        <TaskTab
          label="Daily"
          active={activeFilter === "daily"}
          onPress={() => setActiveFilter("daily")}
        />
        <TaskTab
          label="Weekly"
          active={activeFilter === "weekly"}
          onPress={() => setActiveFilter("weekly")}
        />
        <TaskTab
          label="Monthly"
          active={activeFilter === "monthly"}
          onPress={() => setActiveFilter("monthly")}
        />
        <TaskTab
          label="Yearly"
          active={activeFilter === "yearly"}
          onPress={() => setActiveFilter("yearly")}
        />
        <TaskTab
          label="Events"
          active={activeFilter === "events"}
          onPress={() => setActiveFilter("events")}
        />
      </View>

      {/* Placeholder content area for whoever wires tasks later */}
      <View style={[styles.card, { borderColor: colors.border }]}>
        <Text style={[styles.cardLabel, { color: colors.textSecondary }]}>
          {activeFilter.charAt(0).toUpperCase() + activeFilter.slice(1)} tasks
        </Text>
        <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
          No tasks loaded. Tasks will appear here for this category.
        </Text>
      </View>
    </ScrollView>
  );
}

function TaskTab({
  label,
  active,
  onPress,
}: {
  label: string;
  active?: boolean;
  onPress: () => void;
}) {
  return (
    <TouchableOpacity onPress={onPress}>
      <View style={[styles.tabPill, active && styles.tabPillActive]}>
        <Text style={[styles.tabText, active && styles.tabTextActive]}>
          {label}
        </Text>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  scrollContent: {
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 24,
    gap: 12,
  },
  header: {
    marginBottom: 8,
    flexDirection: "row",
    alignItems: "center",
  },
  backBtn: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    paddingRight: 8,
    paddingVertical: 4,
  },
  backText: {
    fontSize: 13,
    fontWeight: "500",
  },
  headerTextBlock: {
    flex: 1,
  },
  title: {
    fontSize: 18,
    fontWeight: "600",
  },
  subTitle: {
    fontSize: 12,
    marginTop: 2,
  },
  tabsRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginBottom: 8,
  },
  tabPill: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: "#4b5563",
    backgroundColor: "rgba(15,23,42,0.8)",
  },
  tabPillActive: {
    borderColor: "#60a5fa",
    backgroundColor: "rgba(37,99,235,0.25)",
  },
  tabText: {
    fontSize: 11,
    color: "#9ca3af",
    textTransform: "uppercase",
    letterSpacing: 1,
  },
  tabTextActive: {
    color: "#e5e7eb",
    fontWeight: "600",
  },
  card: {
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 14,
    padding: 16,
    backgroundColor: "#020617",
  },
  cardLabel: {
    fontSize: 11,
    textTransform: "uppercase",
    letterSpacing: 1,
    marginBottom: 8,
  },
  emptyText: {
    fontSize: 13,
  },
});
