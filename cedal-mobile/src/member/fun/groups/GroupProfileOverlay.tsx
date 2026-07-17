// src/member/fun/groups/GroupProfileOverlay.tsx
import React from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  Image,
} from "react-native";

type GroupMember = {
  id: string;
  name: string;
  avatarUrl?: string | null;
};

type GroupProfile = {
  id?: string;
  name?: string;
  description?: string;
  tags?: string[];
  region?: string;
  isPrivate?: boolean;
  membersCount?: number;
  members?: GroupMember[];
  serverId?: string;
};

type Props = {
  open: boolean;
  group?: GroupProfile | null;
  onClose: () => void;
};

export default function GroupProfileOverlay({ open, group, onClose }: Props) {
  if (!open || !group) return null;

  const title = group.name || "Group";
  const membersCount = group.membersCount ?? group.members?.length ?? 0;
  const tags = group.tags ?? [];
  const visibilityLabel = group.isPrivate ? "Private group" : "Public group";

  return (
    <View style={styles.infoOverlay}>
      <View style={styles.infoFrame} />

      <View style={styles.infoCard}>
        {/* top bar: Close only */}
        <View style={styles.topBar}>
          <TouchableOpacity onPress={onClose} style={styles.infoClosePill}>
            <Text style={styles.infoClosePillText}>Close</Text>
          </TouchableOpacity>
        </View>

        {/* fixed header: avatar + name + subtitle */}
        <View style={styles.infoHeaderRow}>
          <View style={styles.infoHeroRow}>
            <View style={styles.infoAvatarOuter}>
              <View style={styles.infoAvatarGlow} />
              <View style={styles.infoAvatarInner}>
                <Text style={styles.infoAvatarLetter}>
                  {title.charAt(0).toUpperCase()}
                </Text>
              </View>
            </View>
            <View style={styles.infoHeroText}>
              <Text style={styles.infoHeroTitle}>{title}</Text>
              <Text style={styles.infoHeroSubtitle}>
                {visibilityLabel} · {membersCount} member
                {membersCount === 1 ? "" : "s"}
              </Text>
            </View>
          </View>
        </View>

        {/* scrolling content below fixed header */}
        <ScrollView
          style={styles.infoScroll}
          contentContainerStyle={styles.infoScrollContent}
        >
          {/* Group description */}
          <View style={styles.infoSection}>
            <Text style={styles.infoSectionLabel}>Group description</Text>
            <Text style={styles.infoSectionBody}>
              {group.description && group.description.trim().length > 0
                ? group.description
                : "No description has been added for this group yet."}
            </Text>
          </View>

          {/* Server */}
          <View style={styles.infoSection}>
            <Text style={styles.infoSectionLabel}>Server</Text>
            <Text style={styles.infoSectionBody}>
              {group.serverId || group.region || "Not set"}
            </Text>
          </View>

          {/* Chat theme (static for now) */}
          <View style={styles.infoSection}>
            <Text style={styles.infoSectionLabel}>Chat theme</Text>
            <Text style={styles.infoSectionBody}>Neon dark</Text>
          </View>

          {/* Chat color (static for now) */}
          <View style={styles.infoSection}>
            <Text style={styles.infoSectionLabel}>Chat color</Text>
            <Text style={styles.infoSectionBody}>Blue accent</Text>
          </View>

          {/* Bots */}
          <View style={styles.infoSection}>
            <Text style={styles.infoSectionLabel}>Bots</Text>
            <Text style={styles.infoSectionBody}>
              No bots connected yet.
            </Text>
          </View>

          {/* Tags */}
          <View style={styles.infoSection}>
            <Text style={styles.infoSectionLabel}>Tags</Text>
            {tags.length === 0 ? (
              <Text style={styles.infoSectionBody}>No tags added.</Text>
            ) : (
              <View style={styles.chipRow}>
                {tags.map((t) => (
                  <View key={t} style={styles.chip}>
                    <Text style={styles.chipText}>{t}</Text>
                  </View>
                ))}
              </View>
            )}
          </View>

          {/* Media */}
          <View style={styles.infoSection}>
            <Text style={styles.infoSectionLabel}>Media</Text>
            <Text style={styles.infoSectionBody}>
              Media gallery will appear here once chat is connected to storage.
            </Text>
          </View>

          {/* Members list */}
          <View style={styles.infoSection}>
            <Text style={styles.infoSectionLabel}>Members</Text>
            {group.members && group.members.length > 0 ? (
              <View style={styles.membersPanel}>
                {group.members.map((m) => (
                  <View key={m.id} style={styles.memberRow}>
                    <View style={styles.memberAvatar}>
                      {m.avatarUrl ? (
                        <Image
                          source={{ uri: m.avatarUrl }}
                          style={styles.memberAvatarImage}
                        />
                      ) : (
                        <Text style={styles.memberAvatarLetter}>
                          {m.name?.charAt(0).toUpperCase() || "?"}
                        </Text>
                      )}
                    </View>
                    <Text style={styles.memberName}>{m.name}</Text>
                  </View>
                ))}
              </View>
            ) : (
              <Text style={styles.infoSectionBody}>
                Members are hidden or not loaded yet.
              </Text>
            )}
          </View>

          {/* User power */}
          <View style={styles.infoSection}>
            <Text style={styles.infoSectionLabel}>User power</Text>
            <View style={styles.actionsGrid}>
              {[
                "Block group",
                "Report group",
                "Chat lock",
                "Disappearing messages",
                "Clear chat",
                "Add members",
                "Group link",
              ].map((label) => (
                <TouchableOpacity
                  key={label}
                  activeOpacity={0.85}
                  style={styles.actionBtn}
                  onPress={() => {
                    // hook actions later
                  }}
                >
                  <Text style={styles.actionText}>{label}</Text>
                </TouchableOpacity>
              ))}
            </View>
          </View>
        </ScrollView>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  infoOverlay: {
    position: "absolute",
    inset: 0,
    zIndex: 50,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "rgba(15,23,42,0.92)",
  },
  infoFrame: {
    position: "absolute",
    inset: 0,
    borderRadius: 0,
    borderWidth: 0,
    borderColor: "transparent",
  },
  infoCard: {
    flex: 1,
    width: "100%",
    borderRadius: 0,
    paddingHorizontal: 18,
    paddingTop: 18,
    paddingBottom: 10,
    backgroundColor: "rgba(15,23,42,0.97)",
    borderWidth: 0,
  },

  topBar: {
    flexDirection: "row",
    justifyContent: "flex-end",
    paddingBottom: 6,
  },

  // fixed header
  infoHeaderRow: {
    paddingBottom: 10,
    borderBottomWidth: 1,
    borderBottomColor: "#111827",
  },
  infoHeroRow: {
    flexDirection: "row",
    alignItems: "center",
  },
  infoAvatarOuter: {
    width: 72,
    height: 72,
    borderRadius: 36,
    alignItems: "center",
    justifyContent: "center",
    marginRight: 14,
  },
  infoAvatarGlow: {
    position: "absolute",
    width: 72,
    height: 72,
    borderRadius: 36,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.8)",
    shadowColor: "#22d3ee",
    shadowOpacity: 0.9,
    shadowRadius: 24,
    shadowOffset: { width: 0, height: 0 },
  },
  infoAvatarInner: {
    width: 60,
    height: 60,
    borderRadius: 30,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#020617",
    borderWidth: 2,
    borderColor: "#0ea5e9",
  },
  infoAvatarLetter: {
    fontSize: 24,
    fontWeight: "700",
    color: "#e5e7eb",
  },
  infoHeroText: {
    flex: 1,
  },
  infoHeroTitle: {
    fontSize: 18,
    fontWeight: "600",
    color: "#e5e7eb",
  },
  infoHeroSubtitle: {
    fontSize: 13,
    color: "#9ca3af",
    marginTop: 4,
  },

  infoClosePill: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#4b5563",
    backgroundColor: "#020617",
  },
  infoClosePillText: {
    fontSize: 12,
    color: "#e5e7eb",
  },

  // scrolling content
  infoScroll: {
    marginTop: 10,
  },
  infoScrollContent: {
    paddingBottom: 18,
  },
  infoSection: {
    marginBottom: 14,
  },
  infoSectionLabel: {
    fontSize: 13,
    fontWeight: "600",
    color: "#e5e7eb",
    marginBottom: 4,
  },
  infoSectionBody: {
    fontSize: 13,
    color: "#9ca3af",
  },

  chipRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 6,
    marginTop: 4,
  },
  chip: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.9)",
    backgroundColor: "rgba(15,23,42,1)",
  },
  chipText: {
    fontSize: 11,
    color: "#e5e7eb",
    textTransform: "uppercase",
    letterSpacing: 0.6,
  },

  membersPanel: {
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "#1f2937",
    backgroundColor: "#020617",
    padding: 8,
  },
  memberRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 4,
  },
  memberAvatar: {
    width: 26,
    height: 26,
    borderRadius: 13,
    backgroundColor: "#111827",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 8,
    overflow: "hidden",
  },
  memberAvatarImage: {
    width: "100%",
    height: "100%",
  },
  memberAvatarLetter: {
    fontSize: 12,
    color: "#e5e7eb",
    fontWeight: "600",
  },
  memberName: {
    fontSize: 13,
    color: "#e5e7eb",
  },

  actionsGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginTop: 4,
  },
  actionBtn: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#334155",
    backgroundColor: "#020617",
  },
  actionText: {
    fontSize: 11,
    color: "#e5e7eb",
    textTransform: "uppercase",
    letterSpacing: 0.6,
  },
});
