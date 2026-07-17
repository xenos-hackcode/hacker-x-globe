// src/member/fun/groups/GroupsHub.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import React, { useMemo, useState } from "react";
import {
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";

type VisibilityFilter = "all" | "public" | "private";
type TabKind = "all" | "joined";

export type Group = {
  id: string;
  name: string;
  description?: string;
  tags: string[];
  region: string;
  isPrivate: boolean;
  membersCount: number;
  members?: string[];
  ownerId?: string;
  inviteCode?: string;
};

type Props = {
  groups: Group[];
  currentUserId: string;
  onClose?: () => void;
  onCreateGroup?: () => void;
  onOpenSettings?: () => void;
  onOpenGroup?: (group: Group) => void;
};

export default function GroupsHub({
  groups,
  currentUserId,
  onClose,
  onCreateGroup,
  onOpenSettings,
  onOpenGroup,
}: Props) {
  const { colors } = useTheme();
  const styles = makeStyles(colors);

  const [search, setSearch] = useState("");
  const [topic, setTopic] = useState<string>("all");
  const [visibility, setVisibility] = useState<VisibilityFilter>("all");
  const [showVisibilityMenu, setShowVisibilityMenu] = useState(false);
  const [tab, setTab] = useState<TabKind>("all");

  const topicOptions = useMemo(() => {
    const tagSet = new Set<string>();
    groups.forEach((g) => {
      (g.tags || []).forEach((t) => {
        if (t) tagSet.add(t.toLowerCase());
      });
    });

    const tags = Array.from(tagSet).sort();
    return [
      { id: "all", label: "All" },
      ...tags.map((t) => ({
        id: t,
        label: t.charAt(0).toUpperCase() + t.slice(1),
      })),
    ];
  }, [groups]);

  const filteredGroups = useMemo(() => {
    let base = [...groups];

    const q = search.toLowerCase().trim();
    if (q) {
      base = base.filter((g) => {
        const name = g.name.toLowerCase();
        const tags = g.tags.join(" ").toLowerCase();
        const region = g.region.toLowerCase();
        return name.includes(q) || tags.includes(q) || region.includes(q);
      });
    }

    if (topic !== "all") {
      const topicLower = topic.toLowerCase();
      base = base.filter((g) =>
        g.tags.some((t) => t.toLowerCase() === topicLower),
      );
    }

    // visibility dropdown filter
    if (visibility === "public") {
      base = base.filter((g) => !g.isPrivate);
    } else if (visibility === "private") {
      base = base.filter((g) => g.isPrivate);
    }

    // tab filter
    if (tab === "joined") {
      base = base.filter((g) => {
        const inMembers =
          Array.isArray(g.members) && g.members.includes(currentUserId);
        const isOwner = g.ownerId === currentUserId;
        return inMembers || isOwner;
      });
    } else {
      // tab === "all": hide private groups from global browser
      base = base.filter((g) => !g.isPrivate);
    }

    return base;
  }, [groups, search, topic, visibility, tab, currentUserId]);

  return (
    <View style={styles.root}>
      {/* header + filters */}
      <View style={styles.headerBlock}>
        <View style={styles.headerTopRow}>
          <View style={styles.headerLeft}>
            <Text style={styles.headerTitle}>Groups</Text>
            <TouchableOpacity
              activeOpacity={0.9}
              onPress={onCreateGroup}
              style={styles.plusBtn}
            >
              <Ionicons name="add" size={16} color="#0f172a" />
            </TouchableOpacity>
          </View>

          <View style={styles.headerRightRow}>
            <TouchableOpacity
              activeOpacity={0.8}
              onPress={onOpenSettings}
              style={styles.iconBtn}
            >
              <Ionicons name="settings-outline" size={16} color="#9ca3af" />
            </TouchableOpacity>

            <TouchableOpacity
              activeOpacity={0.8}
              onPress={onClose}
              style={styles.closeBtn}
            >
              <Text style={styles.closeText}>Close</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* All / Joined tab row */}
        <View style={styles.tabRow}>
          <TouchableOpacity
            activeOpacity={0.8}
            onPress={() => setTab("all")}
            style={[styles.tabChip, tab === "all" && styles.tabChipActive]}
          >
            <Text
              style={[styles.tabText, tab === "all" && styles.tabTextActive]}
            >
              All
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            activeOpacity={0.8}
            onPress={() => setTab("joined")}
            style={[styles.tabChip, tab === "joined" && styles.tabChipActive]}
          >
            <Text
              style={[
                styles.tabText,
                tab === "joined" && styles.tabTextActive,
              ]}
            >
              Joined
            </Text>
          </TouchableOpacity>
        </View>

        {/* search + visibility */}
        <View style={styles.searchWrap}>
          <View style={styles.filterBox}>
            <TouchableOpacity
              activeOpacity={0.8}
              onPress={() => setShowVisibilityMenu((v) => !v)}
              style={styles.filterIconBtn}
            >
              <Ionicons name="filter-outline" size={14} color="#e5e7eb" />
            </TouchableOpacity>

            {showVisibilityMenu && (
              <View style={styles.filterDropdown}>
                {(["all", "public", "private"] as VisibilityFilter[]).map(
                  (v) => {
                    const active = visibility === v;
                    const label =
                      v === "all"
                        ? "All"
                        : v === "public"
                        ? "Public"
                        : "Private";
                    return (
                      <TouchableOpacity
                        key={v}
                        activeOpacity={0.8}
                        onPress={() => {
                          setVisibility(v);
                          setShowVisibilityMenu(false);
                        }}
                        style={[
                          styles.filterOption,
                          active && styles.filterOptionActive,
                        ]}
                      >
                        <Text
                          style={[
                            styles.filterOptionText,
                            active && styles.filterOptionTextActive,
                          ]}
                        >
                          {label}
                        </Text>
                      </TouchableOpacity>
                    );
                  },
                )}
              </View>
            )}
          </View>

          <Ionicons
            name="search-outline"
            size={14}
            color="#6b7280"
            style={{ marginRight: 6 }}
          />
          <TextInput
            value={search}
            onChangeText={setSearch}
            placeholder="Search groups by name, tags, or region…"
            placeholderTextColor="#6b7280"
            style={styles.searchInput}
          />
        </View>

        {/* dynamic tag filters */}
        <View style={styles.topicRow}>
          {topicOptions.map((t) => {
            const active = topic === t.id;
            return (
              <TouchableOpacity
                key={t.id}
                activeOpacity={0.8}
                onPress={() => setTopic(t.id)}
                style={[styles.topicChip, active && styles.topicChipActive]}
              >
                <Text
                  style={[
                    styles.topicText,
                    active && styles.topicTextActive,
                  ]}
                >
                  {t.label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>
      </View>

      {/* list */}
      <View style={styles.listArea}>
        <ScrollView
          style={styles.scroll}
          contentContainerStyle={styles.scrollContent}
        >
          {filteredGroups.length === 0 ? (
            <Text style={styles.emptyLabel}>
              No groups match these filters yet.
            </Text>
          ) : (
            filteredGroups.map((g) => (
              <TouchableOpacity
                key={g.id}
                activeOpacity={0.85}
                style={styles.groupCard}
                onPress={() => onOpenGroup?.(g)}
              >
                <View style={styles.groupHeaderRow}>
                  <Text style={styles.groupName}>{g.name}</Text>
                  <View
                    style={[
                      styles.privacyPill,
                      g.isPrivate
                        ? styles.privacyPrivate
                        : styles.privacyPublic,
                    ]}
                  >
                    <Text style={styles.privacyText}>
                      {g.isPrivate ? "Private" : "Public"}
                    </Text>
                  </View>
                </View>

                <View style={styles.groupMetaRow}>
                  <Text style={styles.metaText}>{g.region}</Text>
                  <Text style={styles.metaDot}>•</Text>
                  <Text style={styles.metaText}>
                    {g.membersCount} members
                  </Text>
                </View>

                <View style={styles.tagsRow}>
                  {g.tags.map((t) => (
                    <View key={t} style={styles.tagChip}>
                      <Text style={styles.tagText}>{t}</Text>
                    </View>
                  ))}
                </View>
              </TouchableOpacity>
            ))
          )}
        </ScrollView>
      </View>
    </View>
  );
}

const makeStyles = (colors: any) =>
  StyleSheet.create({
    root: {
      flex: 1,
      backgroundColor: "#020617",
    },
    headerBlock: {
      paddingHorizontal: 16,
      paddingTop: 35,
      paddingBottom: 8,
      backgroundColor: "#020617",
    },
    headerTopRow: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      marginBottom: 8,
    },
    headerLeft: {
      flexDirection: "row",
      alignItems: "center",
      gap: 8,
    },
    headerTitle: {
      fontSize: 18,
      fontWeight: "600",
      color: "#e5e7eb",
    },
    plusBtn: {
      width: 28,
      height: 28,
      borderRadius: 14,
      backgroundColor: "#38bdf8",
      alignItems: "center",
      justifyContent: "center",
    },
    headerRightRow: {
      flexDirection: "row",
      alignItems: "center",
      gap: 8,
    },
    iconBtn: {
      width: 30,
      height: 30,
      borderRadius: 15,
      borderWidth: 1,
      borderColor: "rgba(148,163,184,0.6)",
      alignItems: "center",
      justifyContent: "center",
      backgroundColor: "rgba(15,23,42,0.9)",
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
    tabRow: {
      flexDirection: "row",
      gap: 6,
      marginTop: 8,
    },
    tabChip: {
      borderRadius: 999,
      borderWidth: 1,
      borderColor: "#1f2937",
      paddingHorizontal: 10,
      paddingVertical: 4,
      backgroundColor: "#020617",
    },
    tabChipActive: {
      borderColor: "#38bdf8",
      backgroundColor: "rgba(30,64,175,0.4)",
    },
    tabText: {
      fontSize: 11,
      color: "#93c5fd",
      textTransform: "uppercase",
      letterSpacing: 0.7,
    },
    tabTextActive: {
      color: "#38bdf8",
    },
    searchWrap: {
      flexDirection: "row",
      alignItems: "center",
      borderRadius: 999,
      borderWidth: 1,
      borderColor: "rgba(15,23,42,0.9)",
      backgroundColor: "rgba(15,23,42,0.98)",
      paddingHorizontal: 10,
      paddingVertical: 6,
      marginTop: 6,
      gap: 6,
    },
    filterBox: {
      position: "relative",
      marginRight: 2,
    },
    filterIconBtn: {
      width: 24,
      height: 24,
      borderRadius: 12,
      alignItems: "center",
      justifyContent: "center",
      backgroundColor: "rgba(15,23,42,0.9)",
      borderWidth: 1,
      borderColor: "rgba(55,65,81,0.9)",
    },
    filterDropdown: {
      position: "absolute",
      top: 28,
      left: 0,
      borderRadius: 10,
      backgroundColor: "rgba(15,23,42,0.98)",
      borderWidth: 1,
      borderColor: "rgba(31,41,55,1)",
      paddingVertical: 4,
      minWidth: 90,
      zIndex: 20,
    },
    filterOption: {
      paddingHorizontal: 8,
      paddingVertical: 4,
    },
    filterOptionActive: {
      backgroundColor: "rgba(30,64,175,0.6)",
    },
    filterOptionText: {
      fontSize: 11,
      color: "#9ca3af",
    },
    filterOptionTextActive: {
      color: "#e5e7eb",
      fontWeight: "600",
    },
    searchInput: {
      flex: 1,
      fontSize: 13,
      color: "#e5e7eb",
    },
    topicRow: {
      marginTop: 8,
      flexDirection: "row",
      gap: 6,
      flexWrap: "wrap",
    },
    topicChip: {
      borderRadius: 999,
      borderWidth: 1,
      borderColor: "#1f2937",
      paddingHorizontal: 10,
      paddingVertical: 4,
      backgroundColor: "#020617",
    },
    topicChipActive: {
      borderColor: "#38bdf8",
      backgroundColor: "rgba(30,64,175,0.4)",
    },
    topicText: {
      fontSize: 11,
      color: "#93c5fd",
      textTransform: "uppercase",
      letterSpacing: 0.7,
    },
    topicTextActive: {
      color: "#38bdf8",
    },
    listArea: {
      flex: 1,
      paddingHorizontal: 16,
      paddingBottom: 12,
    },
    scroll: {
      flex: 1,
    },
    scrollContent: {
      paddingBottom: 24,
    },
    groupCard: {
      borderRadius: 14,
      borderWidth: 1,
      borderColor: "rgba(15,23,42,0.95)",
      backgroundColor: "rgba(15,23,42,0.98)",
      paddingHorizontal: 12,
      paddingVertical: 10,
      marginBottom: 8,
    },
    groupHeaderRow: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      marginBottom: 4,
    },
    groupName: {
      fontSize: 15,
      fontWeight: "600",
      color: "#e5e7eb",
      flexShrink: 1,
    },
    privacyPill: {
      borderRadius: 999,
      paddingHorizontal: 8,
      paddingVertical: 2,
    },
    privacyPrivate: {
      backgroundColor: "rgba(185,28,28,0.25)",
    },
    privacyPublic: {
      backgroundColor: "rgba(22,163,74,0.25)",
    },
    privacyText: {
      fontSize: 10,
      color: "#e5e7eb",
      textTransform: "uppercase",
      letterSpacing: 0.8,
    },
    groupMetaRow: {
      flexDirection: "row",
      alignItems: "center",
      marginBottom: 6,
    },
    metaText: {
      fontSize: 11,
      color: "#9ca3af",
    },
    metaDot: {
      fontSize: 11,
      color: "#4b5563",
      marginHorizontal: 4,
    },
    tagsRow: {
      flexDirection: "row",
      flexWrap: "wrap",
      gap: 4,
    },
    tagChip: {
      borderRadius: 999,
      backgroundColor: "#020617",
      paddingHorizontal: 8,
      paddingVertical: 2,
      borderWidth: 1,
      borderColor: "#1f2937",
    },
    tagText: {
      fontSize: 10,
      color: "#93c5fd",
      textTransform: "uppercase",
      letterSpacing: 0.6,
    },
    emptyLabel: {
      marginTop: 24,
      fontSize: 13,
      color: "#6b7280",
      textAlign: "center",
    },
  });
