// src/member/chat/LinkPickerSheet.tsx
import React from "react";
import {
  Modal,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
} from "react-native";
import type { Group } from "@/src/member/fun/groups/GroupsHub";

type Props = {
  visible: boolean;
  groups: Group[];
  onClose: () => void;
  onPickLink: (link: string) => void;
};

export function LinkPickerSheet({ visible, groups, onClose, onPickLink }: Props) {
  if (!visible) return null;

  return (
    <Modal
      transparent
      animationType="slide"
      visible={visible}
      onRequestClose={onClose}
    >
      <View style={styles.backdrop}>
        <View style={styles.sheet}>
          <View style={styles.headerRow}>
            <Text style={styles.title}>Links</Text>
            <TouchableOpacity
              activeOpacity={0.8}
              onPress={onClose}
              style={styles.closeBtn}
            >
              <Text style={styles.closeText}>Close</Text>
            </TouchableOpacity>
          </View>

          <View style={{ height: 320 }}>
            <ScrollView
              showsVerticalScrollIndicator={false}
              contentContainerStyle={styles.listContent}
            >
              {groups.map((g) => {
                const link =
                  g.isPrivate && g.inviteCode
                    ? `cedal.app/join/${g.inviteCode}`
                    : `cedal.app/group/${g.id}`;
                return (
                  <TouchableOpacity
                    key={g.id}
                    style={styles.item}
                    activeOpacity={0.8}
                    onPress={() => {
                      onPickLink(link);
                      onClose();
                    }}
                  >
                    <Text style={styles.name}>{g.name}</Text>
                    <Text style={styles.link}>{link}</Text>
                  </TouchableOpacity>
                );
              })}

              {groups.length === 0 && (
                <View style={styles.emptyState}>
                  <Text style={styles.emptyText}>
                    No groups yet. Create one from the Fun tab.
                  </Text>
                </View>
              )}
            </ScrollView>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    justifyContent: "flex-end",
    backgroundColor: "rgba(15,23,42,0.75)",
  },
  sheet: {
    backgroundColor: "#020617",
    borderTopLeftRadius: 18,
    borderTopRightRadius: 18,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.4)",
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: 24,
    flexShrink: 0,
    maxHeight: 700,
  },
  headerRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 8,
  },
  title: {
    color: "#e5e7eb",
    fontSize: 14,
    fontWeight: "700",
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
  listContent: {
    paddingBottom: 8,
  },
  item: {
    paddingVertical: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(31,41,55,0.8)",
  },
  name: {
    fontSize: 13,
    color: "#e5e7eb",
  },
  link: {
    fontSize: 11,
    color: "#38bdf8",
    marginTop: 2,
  },
  emptyState: {
    width: "100%",
    paddingVertical: 20,
    alignItems: "center",
  },
  emptyText: {
    color: "#6b7280",
    fontSize: 12,
  },
});
