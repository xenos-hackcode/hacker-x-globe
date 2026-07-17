// src/member/fun/groups/GroupDetailsPanel.tsx
import React, { useState } from "react";
import { View, Text, StyleSheet, TouchableOpacity } from "react-native";

export type Group = {
  id?: string;
  name?: string;
  description?: string;
  tags?: string[];
  region?: string;
  isPrivate?: boolean;
  members?: string[];
};

type Props = {
  group: Group | null;
  isOwner: boolean;
  onClose: () => void;
  onEnterChat: () => void;
  onConfirmLeave: () => void;
};

export default function GroupDetailsPanel({
  group,
  isOwner,
  onClose,
  onEnterChat,
  onConfirmLeave,
}: Props) {
  const [confirmOpen, setConfirmOpen] = useState(false);

  if (!group) return null;

  const members = group.members?.length || 0;

  const confirmTitle = isOwner ? "Leave as owner?" : "Leave group?";
  const confirmBody = isOwner
    ? "Once you leave this group you will not be able to create a new group for the next 12 hours. You can still join other groups."
    : "Are you sure you want to leave this group?";

  return (
    <View style={styles.overlay}>
      <View style={styles.card}>
        {/* header */}
        <View style={styles.headerRow}>
          <View style={styles.headerLeft}>
            <Text style={styles.headerMeta}>Group profile</Text>
            <Text style={styles.headerTitle} numberOfLines={1}>
              {group.name}
            </Text>
          </View>

          <TouchableOpacity
            activeOpacity={0.85}
            onPress={onClose}
            style={styles.backPill}
          >
            <Text style={styles.backPillText}>Close</Text>
          </TouchableOpacity>
        </View>

        {/* description */}
        {!!group.description && (
          <View style={styles.section}>
            <Text style={styles.sectionLabel}>Overview</Text>
            <Text style={styles.description}>{group.description}</Text>
          </View>
        )}

        {/* tags */}
        {!!group.tags?.length && (
          <View style={styles.section}>
            <Text style={styles.sectionLabel}>Tags</Text>
            <View style={styles.tagsRow}>
              {group.tags.map((t) => (
                <View key={t} style={styles.tagChip}>
                  <Text style={styles.tagText}>{t}</Text>
                </View>
              ))}
            </View>
          </View>
        )}

        {/* meta */}
        <View style={styles.section}>
          <Text style={styles.sectionLabel}>Session meta</Text>
          <View style={styles.metaRow}>
            {group.region && (
              <View style={styles.metaPill}>
                <Text style={styles.metaLabel}>Region</Text>
                <Text style={styles.metaValue}>{group.region}</Text>
              </View>
            )}

            <View style={styles.metaPill}>
              <Text style={styles.metaLabel}>Members</Text>
              <Text style={styles.metaValue}>{members}</Text>
            </View>

            <View style={styles.metaPill}>
              <Text style={styles.metaLabel}>Visibility</Text>
              <Text style={styles.metaValue}>
                {group.isPrivate ? "Private" : "Public"}
              </Text>
            </View>
          </View>

          {/* note for private groups */}
          {group.isPrivate && (
            <Text style={styles.privateHint}>
              Private groups are hidden from the Groups hub and can only be
              joined via invite link or code.
            </Text>
          )}
        </View>

        {/* footer buttons */}
        <View style={styles.footerRow}>
          <TouchableOpacity
            activeOpacity={0.85}
            onPress={() => setConfirmOpen(true)}
            style={styles.leaveBtn}
          >
            <Text style={styles.leaveText}>Leave group</Text>
          </TouchableOpacity>

          <TouchableOpacity
            activeOpacity={0.9}
            onPress={onEnterChat}
            style={styles.chatBtn}
          >
            <Text style={styles.chatText}>Enter chat</Text>
          </TouchableOpacity>
        </View>

        {/* confirm overlay */}
        {confirmOpen && (
          <View style={styles.confirmOverlay}>
            <View style={styles.confirmCard}>
              <Text style={styles.confirmTitle}>{confirmTitle}</Text>
              <Text style={styles.confirmBody}>{confirmBody}</Text>

              <View style={styles.confirmRow}>
                <TouchableOpacity
                  style={styles.confirmCancel}
                  onPress={() => setConfirmOpen(false)}
                  activeOpacity={0.85}
                >
                  <Text style={styles.confirmCancelText}>Cancel</Text>
                </TouchableOpacity>

                <TouchableOpacity
                  style={styles.confirmLeave}
                  onPress={() => {
                    setConfirmOpen(false);
                    onConfirmLeave();
                  }}
                  activeOpacity={0.9}
                >
                  <Text style={styles.confirmLeaveText}>Yes, leave</Text>
                </TouchableOpacity>
              </View>
            </View>
          </View>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: "rgba(15,23,42,0.92)",
    alignItems: "center",
    justifyContent: "center",
    padding: 16,
  },
  card: {
    width: 460,
    maxWidth: "92%",
    borderRadius: 20,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.35)",
    backgroundColor: "rgba(15,23,42,0.98)",
    padding: 18,
  },
  headerRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    gap: 8,
    marginBottom: 10,
  },
  headerLeft: {
    flex: 1,
  },
  headerMeta: {
    fontSize: 10,
    textTransform: "uppercase",
    letterSpacing: 1.4,
    color: "#64748b",
    marginBottom: 2,
  },
  headerTitle: {
    fontSize: 18,
    fontWeight: "600",
    color: "#f9a8d4",
  },
  backPill: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.35)",
    backgroundColor: "rgba(15,23,42,0.9)",
  },
  backPillText: {
    fontSize: 11,
    letterSpacing: 1,
    textTransform: "uppercase",
    color: "#e5e7eb",
  },

  section: {
    marginTop: 10,
  },
  sectionLabel: {
    fontSize: 11,
    textTransform: "uppercase",
    letterSpacing: 1.2,
    color: "#9ca3af",
    marginBottom: 4,
  },
  description: {
    fontSize: 13,
    color: "#e5e7eb",
    lineHeight: 18,
  },

  tagsRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 6,
  },
  tagChip: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.7)",
    backgroundColor: "rgba(15,23,42,1)",
    paddingHorizontal: 8,
    paddingVertical: 3,
  },
  tagText: {
    fontSize: 10,
    color: "#7dd3fc",
    textTransform: "uppercase",
    letterSpacing: 0.7,
  },

  metaRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginTop: 2,
  },
  metaPill: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "rgba(15,23,42,1)",
    borderWidth: 1,
    borderColor: "rgba(30,64,175,0.7)",
  },
  metaLabel: {
    fontSize: 10,
    color: "#9ca3af",
    textTransform: "uppercase",
    letterSpacing: 0.8,
  },
  metaValue: {
    marginTop: 1,
    fontSize: 12,
    fontWeight: "500",
    color: "#e5e7eb",
  },

  privateHint: {
    marginTop: 6,
    fontSize: 11,
    color: "#9ca3af",
  },

  footerRow: {
    marginTop: 18,
    flexDirection: "row",
    justifyContent: "space-between",
    gap: 10,
  },
  leaveBtn: {
    flex: 1,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(199, 16, 59, 0.6)",
    backgroundColor: "rgba(184, 9, 24, 0.95)",
    paddingVertical: 9,
    alignItems: "center",
  },
  leaveText: {
    fontSize: 12,
    color: "#1b0303",
    fontWeight: "500",
  },
  chatBtn: {
    flex: 1,
    borderRadius: 999,
    backgroundColor: "#22c55e",
    paddingVertical: 9,
    alignItems: "center",
    shadowColor: "#22c55e",
    shadowOpacity: 0.4,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 0 },
    elevation: 4,
  },
  chatText: {
    fontSize: 12,
    color: "#022c22",
    fontWeight: "700",
    letterSpacing: 0.5,
    textTransform: "uppercase",
  },

  confirmOverlay: {
    position: "absolute",
    inset: 0,
    backgroundColor: "rgba(15,23,42,0.85)",
    alignItems: "center",
    justifyContent: "center",
  },
  confirmCard: {
    width: "92%",
    borderRadius: 16,
    padding: 14,
    backgroundColor: "#020617",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
  },
  confirmTitle: {
    fontSize: 14,
    fontWeight: "600",
    color: "#f97373",
    marginBottom: 6,
  },
  confirmBody: {
    fontSize: 12,
    color: "#e5e7eb",
    lineHeight: 18,
    marginBottom: 12,
  },
  confirmRow: {
    flexDirection: "row",
    justifyContent: "flex-end",
    gap: 8,
  },
  confirmCancel: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.5)",
  },
  confirmCancelText: {
    fontSize: 12,
    color: "#e5e7eb",
  },
  confirmLeave: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "#ef4444",
  },
  confirmLeaveText: {
    fontSize: 12,
    fontWeight: "600",
    color: "#0f172a",
  },
});
