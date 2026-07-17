// src/member/profile/ActivityScreen.tsx
import React, { useMemo, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  FlatList,
  ScrollView,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useRouter } from "expo-router";

type ActivityFilter = "all" | "events" | "polls";

export type ActivityMessage = {
  id: string;
  type: "event" | "poll";
  // event fields
  eventTitle?: string | null;
  eventStartsAt?: string | null;
  eventLocation?: string | null;
  eventNotes?: string | null;
  // poll fields
  question?: string | null;
  options?: { id: string; label: string }[];
  allowMulti?: boolean;
  // common
  userId: string;
  userName?: string;
  timestamp?: number;
};

type Props = {
  title: string;
  otherDisplayName: string;
  currentUserId: string;
  activityMessages: ActivityMessage[]; // all events + polls from this chat
  onBack?: () => void;
};

export function ActivityScreen({
  title,
  otherDisplayName,
  currentUserId,
  activityMessages,
  onBack,
}: Props) {
  const router = useRouter();
  const [filter, setFilter] = useState<ActivityFilter>("all");

  const filtered = useMemo(() => {
    return activityMessages.filter((m) => {
      if (filter === "events") return m.type === "event";
      if (filter === "polls") return m.type === "poll";
      return m.type === "event" || m.type === "poll";
    });
  }, [activityMessages, filter]);

  const chips: { key: ActivityFilter; label: string }[] = [
    { key: "all", label: "All" },
    { key: "events", label: "Events" },
    { key: "polls", label: "Polls" },
  ];

  function handleBack() {
    if (onBack) onBack();
    else router.back();
  }

  return (
    <SafeAreaView style={styles.screen} edges={["top", "bottom"]}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity
          onPress={handleBack}
          hitSlop={16}
          style={styles.backBtn}
        >
          <Text style={styles.backText}>Back</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{title}</Text>
        <View style={{ width: 48 }} />
      </View>

      {/* Filter */}
      <View style={styles.filterBlock}>
        <Text style={styles.filterLabel}>Content</Text>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.chipRowHorizontal}
        >
          {chips.map((chip) => {
            const selected = chip.key === filter;
            return (
              <TouchableOpacity
                key={chip.key}
                style={[styles.chip, selected && styles.chipSelected]}
                activeOpacity={0.85}
                onPress={() => setFilter(chip.key)}
              >
                <Text
                  style={[
                    styles.chipText,
                    selected && styles.chipTextSelected,
                  ]}
                >
                  {chip.label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>
      </View>

      {/* List */}
      <FlatList
        data={filtered}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.listContent}
        renderItem={({ item }) => {
          const createdAt = item.timestamp ? new Date(item.timestamp) : null;
          const dateText = createdAt ? createdAt.toLocaleString() : "";

          const isEvent = item.type === "event";
          const titleText = isEvent
            ? item.eventTitle ?? "Event"
            : item.question ?? "Poll";

          const subtitle = isEvent
            ? item.eventStartsAt ?? ""
            : item.allowMulti
            ? "Multiple choice poll"
            : "Single choice poll";

          const sender =
            item.userId === currentUserId
              ? "You"
              : item.userName ?? otherDisplayName;

          const extra =
            isEvent && item.eventLocation
              ? item.eventLocation
              : !isEvent && item.options && item.options.length > 0
              ? `${item.options.length} options`
              : "";

          return (
            <View style={styles.row}>
              <Text style={styles.rowSender}>{sender}</Text>

              <View style={styles.rowMain}>
                <View
                  style={[
                    styles.iconCircle,
                    isEvent ? styles.iconEvent : styles.iconPoll,
                  ]}
                >
                  <Text style={styles.iconText}>
                    {isEvent ? "📅" : "📊"}
                  </Text>
                </View>

                <View style={styles.rowTextBlock}>
                  <Text style={styles.rowTitle} numberOfLines={1}>
                    {titleText}
                  </Text>
                  {subtitle.length > 0 && (
                    <Text style={styles.rowSubtitle} numberOfLines={1}>
                      {subtitle}
                    </Text>
                  )}
                  <View style={styles.rowMetaRow}>
                    {extra.length > 0 && (
                      <Text style={styles.rowMeta}>{extra}</Text>
                    )}
                    {dateText.length > 0 && (
                      <Text style={styles.rowMeta}>
                        {extra.length > 0 ? " · " : ""}
                        {dateText}
                      </Text>
                    )}
                  </View>
                </View>
              </View>
            </View>
          );
        }}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text style={styles.emptyTitle}>No events or polls yet</Text>
            <Text style={styles.emptyText}>
              Any events or polls shared in this chat will appear here.
            </Text>
          </View>
        }
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#020617",
    position: "absolute",
    top: 0,
    right: 0,
    bottom: 0,
    left: 0,
    zIndex: 100,
    elevation: 100,
  },
  header: {
    height: 48,
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(15,23,42,0.8)",
  },
  backBtn: {
    minWidth: 48,
    paddingVertical: 4,
  },
  backText: {
    fontSize: 12,
    color: "#93c5fd",
    textTransform: "uppercase",
    letterSpacing: 0.8,
  },
  headerTitle: {
    flex: 1,
    textAlign: "center",
    fontSize: 15,
    fontWeight: "600",
    color: "#e5e7eb",
  },
  filterBlock: {
    paddingHorizontal: 16,
    paddingTop: 10,
  },
  filterLabel: {
    fontSize: 11,
    color: "#9ca3af",
    marginBottom: 6,
    textTransform: "uppercase",
    letterSpacing: 0.8,
  },
  chipRowHorizontal: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(96,165,250,0.7)",
    backgroundColor: "#020617",
  },
  chipSelected: {
    borderColor: "rgba(59,130,246,1)",
    backgroundColor: "#0b1120",
  },
  chipText: {
    fontSize: 11,
    color: "#e5e7eb",
    textTransform: "uppercase",
    letterSpacing: 0.8,
  },
  chipTextSelected: {
    color: "#bfdbfe",
  },
  listContent: {
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: 24,
    gap: 8,
  },
  row: {
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "rgba(30,64,175,0.6)",
    backgroundColor: "#020617",
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
  rowSender: {
    fontSize: 11,
    color: "#9ca3af",
    marginBottom: 4,
  },
  rowMain: {
    flexDirection: "row",
    alignItems: "center",
  },
  iconCircle: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 8,
  },
  iconEvent: {
    backgroundColor: "rgba(34,197,94,0.2)",
  },
  iconPoll: {
    backgroundColor: "rgba(59,130,246,0.2)",
  },
  iconText: {
    fontSize: 18,
  },
  rowTextBlock: {
    flex: 1,
  },
  rowTitle: {
    fontSize: 13,
    color: "#e5e7eb",
    fontWeight: "600",
  },
  rowSubtitle: {
    fontSize: 12,
    color: "#a5b4fc",
    marginTop: 1,
  },
  rowMetaRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    alignItems: "center",
    marginTop: 2,
  },
  rowMeta: {
    fontSize: 11,
    color: "#9ca3af",
  },
  emptyState: {
    paddingTop: 40,
    alignItems: "center",
  },
  emptyTitle: {
    fontSize: 14,
    color: "#e5e7eb",
    marginBottom: 4,
  },
  emptyText: {
    fontSize: 12,
    color: "#9ca3af",
  },
});
