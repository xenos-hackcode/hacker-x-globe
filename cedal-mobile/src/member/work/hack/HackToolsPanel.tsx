// src/member/work/hack/HackToolsPanel.tsx
import React from "react";
import { View, Text, TouchableOpacity, StyleSheet, Modal } from "react-native";

type Props = {
  visible: boolean;
  onClose: () => void;
  onToolAction?: (action: { tool: string; type: string; [k: string]: any }) => void;
};

export default function ChatToolsPanel({ visible, onClose, onToolAction }: Props) {
  if (!visible) return null;

  const handleTool = (tool: string, type: string, extra?: object) => {
    onToolAction?.({ tool, type, ...extra });
    onClose();
  };

  return (
    <Modal transparent animationType="slide" visible={visible} onRequestClose={onClose}>
      <View style={styles.backdrop}>
        <View style={styles.sheet}>
          <View style={styles.handle} />

          <Text style={styles.title}>Neural tools</Text>
          <Text style={styles.subtitle}>
            Quick actions tied to this chat.
          </Text>

          <TouchableOpacity
            style={styles.row}
            onPress={() => handleTool("emoji", "reactionPanel")}
          >
            <Text style={styles.rowIcon}>🌌</Text>
            <View style={styles.rowTextBlock}>
              <Text style={styles.rowTitle}>Reaction field</Text>
              <Text style={styles.rowSubtitle}>Drop emoji reactions instead of typing.</Text>
            </View>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.row}
            onPress={() => handleTool("text", "messageActions")}
          >
            <Text style={styles.rowIcon}>🔗</Text>
            <View style={styles.rowTextBlock}>
              <Text style={styles.rowTitle}>Holo text layer</Text>
              <Text style={styles.rowSubtitle}>Reply, copy, pin, or delete messages.</Text>
            </View>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.row}
            onPress={() => handleTool("draw", "openSketch")}
          >
            <Text style={styles.rowIcon}>⚡</Text>
            <View style={styles.rowTextBlock}>
              <Text style={styles.rowTitle}>Cyber draw field</Text>
              <Text style={styles.rowSubtitle}>Sketch ideas or diagrams for this chat.</Text>
            </View>
          </TouchableOpacity>

          <TouchableOpacity style={styles.closeBtn} onPress={onClose}>
            <Text style={styles.closeText}>Close</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: "rgba(15,23,42,0.8)",
    justifyContent: "flex-end",
  },
  sheet: {
    backgroundColor: "#020617",
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 20,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    borderTopWidth: 1,
    borderColor: "rgba(56,189,248,0.3)",
  },
  handle: {
    alignSelf: "center",
    width: 40,
    height: 4,
    borderRadius: 999,
    backgroundColor: "#4b5563",
    marginBottom: 8,
  },
  title: {
    fontSize: 14,
    fontWeight: "600",
    color: "#e5e7eb",
  },
  subtitle: {
    fontSize: 11,
    color: "#9ca3af",
    marginBottom: 12,
  },
  row: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 10,
  },
  rowIcon: {
    fontSize: 18,
    marginRight: 10,
  },
  rowTextBlock: {
    flex: 1,
  },
  rowTitle: {
    fontSize: 13,
    fontWeight: "500",
    color: "#e5e7eb",
  },
  rowSubtitle: {
    fontSize: 11,
    color: "#9ca3af",
  },
  closeBtn: {
    marginTop: 10,
    alignSelf: "center",
    paddingHorizontal: 18,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
  },
  closeText: {
    fontSize: 11,
    color: "#e5e7eb",
  },
});
