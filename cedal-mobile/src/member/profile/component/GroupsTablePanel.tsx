// src/member/profile/component/GroupsTablePanel.tsx
import React from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Modal,
} from "react-native";

export type GroupRow = {
  id: string;   // groupId
  role: string;
  name: string;
};

type Props = {
  open: boolean;
  onClose: () => void;
  rows: GroupRow[];
  onOpenGroup?: (groupId: string, groupName: string) => void;
};

export function GroupsTablePanel({ open, onClose, rows, onOpenGroup }: Props) {
  if (!open) return null;

  return (
    <Modal transparent animationType="fade" visible={open}>
      <View style={styles.overlay}>
        <View style={styles.card}>
          <View style={styles.headerRow}>
            <Text style={styles.headerTitle}>Group</Text>
            <TouchableOpacity
              onPress={onClose}
              style={styles.closePill}
              activeOpacity={0.8}
            >
              <Text style={styles.closeText}>Close</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.tableHeader}>
            <Text style={styles.colRole}>Role</Text>
            <Text style={styles.colName}>Name</Text>
          </View>

          {rows.map((row) => (
            <View key={row.id} style={styles.tableRow}>
              <Text style={styles.colRole}>{row.role}</Text>
              <TouchableOpacity
                style={styles.nameTouchable}
                activeOpacity={0.8}
                onPress={() => onOpenGroup?.(row.id, row.name)}
              >
                <Text style={styles.colName}>{row.name}</Text>
              </TouchableOpacity>
            </View>
          ))}
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
    width: 360,
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
  tableHeader: {
    flexDirection: "row",
    borderBottomWidth: 1,
    borderBottomColor: "#1f2937",
    paddingBottom: 6,
    marginBottom: 6,
  },
  tableRow: {
    flexDirection: "row",
    paddingVertical: 4,
  },
  colRole: {
    width: "40%",
    fontSize: 13,
    color: "#9ca3af",
  },
  nameTouchable: {
    width: "60%",
  },
  colName: {
    fontSize: 13,
    color: "#e5e7eb",
  },
});
