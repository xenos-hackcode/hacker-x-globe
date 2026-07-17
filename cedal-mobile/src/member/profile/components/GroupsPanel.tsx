// src/member/profile/components/GroupsPanel.tsx
import React from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Modal,
} from "react-native";

export type UserGroupRow = {
  id: string;   // groupId
  name: string;
};

type Props = {
  open: boolean;
  onClose: () => void;
  rows: UserGroupRow[];
  onOpenGroup?: (groupId: string, groupName: string) => void;
};

export function GroupsPanel({ open, onClose, rows, onOpenGroup }: Props) {
  if (!open) return null;

  return (
    <Modal
      transparent
      animationType="fade"
      visible={open}
      onRequestClose={onClose}
    >
      <View style={styles.overlay}>
        <View style={styles.card}>
          <View style={styles.headerRow}>
            <Text style={styles.headerTitle}>Groups</Text>
            <TouchableOpacity
              onPress={onClose}
              style={styles.closePill}
              activeOpacity={0.8}
            >
              <Text style={styles.closeText}>Close</Text>
            </TouchableOpacity>
          </View>

          {rows.length === 0 ? (
            <Text style={styles.emptyText}>
              This user is not in any groups yet.
            </Text>
          ) : (
            rows.map((row) => (
              <View key={row.id} style={styles.row}>
                <Text style={styles.groupName}>{row.name}</Text>
                <TouchableOpacity
                  style={styles.enterBtn}
                  activeOpacity={0.8}
                  onPress={() => onOpenGroup?.(row.id, row.name)}
                >
                  <Text style={styles.enterText}>Enter</Text>
                </TouchableOpacity>
              </View>
            ))
          )}
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  overlay: {
    flex: 1,
    backgroundColor: "rgba(15,23,42,0.9)",
    justifyContent: "center",
    alignItems: "center",
  },
  card: {
    width: 340,
    maxWidth: "90%",
    borderRadius: 16,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.5)",
    backgroundColor: "rgba(15,23,42,0.98)",
    padding: 16,
  },
  headerRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 10,
  },
  headerTitle: {
    fontSize: 16,
    fontWeight: "600",
    color: "#e5e7eb",
  },
  closePill: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#4b5563",
    backgroundColor: "#020617",
  },
  closeText: {
    fontSize: 11,
    color: "#e5e7eb",
    textTransform: "uppercase",
    letterSpacing: 0.8,
  },
  row: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingVertical: 6,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "#1f2937",
  },
  groupName: {
    flex: 1,
    fontSize: 13,
    color: "#e5e7eb",
  },
  enterBtn: {
    marginLeft: 10,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(52,211,153,0.8)",
    backgroundColor: "#022c22",
  },
  enterText: {
    fontSize: 11,
    color: "#bbf7d0",
    textTransform: "uppercase",
    letterSpacing: 0.6,
  },
  emptyText: {
    fontSize: 12,
    color: "#9ca3af",
  },
});
