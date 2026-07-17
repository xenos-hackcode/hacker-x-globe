// src/member/chat/VoiceEditSheet.tsx
import React from "react";
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Modal,
} from "react-native";

export type VoiceEditConfig = {
  startMs: number;
  endMs: number;
  noiseReduction: boolean;
  normalize: boolean;
};

type Props = {
  visible: boolean;
  durationMs: number;
  onClose: () => void;
  onApply: (config: VoiceEditConfig) => void;
};

export function VoiceEditSheet({
  visible,
  durationMs,
  onClose,
  onApply,
}: Props) {
  if (!visible) return null;

  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={onClose}
    >
      <View style={styles.backdrop}>
        <View style={styles.sheet}>
          <Text style={styles.title}>Advanced edit</Text>
          <Text style={[styles.label, { marginBottom: 16 }]}>
            Coming soon: trim, noise reduction, and normalize for voice messages.
          </Text>

          <View style={styles.actionsRow}>
            <TouchableOpacity
              style={[styles.actionButton, styles.cancelButton]}
              onPress={onClose}
            >
              <Text style={styles.actionText}>Close</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: "rgba(15,23,42,0.7)",
    justifyContent: "flex-end",
  },
  sheet: {
    backgroundColor: "#020617",
    paddingHorizontal: 20,
    paddingVertical: 18,
    borderTopLeftRadius: 18,
    borderTopRightRadius: 18,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.4)",
    height: "30%", // or "80%", etc.
  },
  title: {
    fontSize: 14,
    fontWeight: "600",
    color: "#e5e7eb",
    marginBottom: 12,
  },
  label: {
    fontSize: 11,
    color: "#9ca3af",
    marginBottom: 6,
  },
  actionsRow: {
    flexDirection: "row",
    justifyContent: "flex-end",
    marginTop: 4,
  },
  actionButton: {
    paddingVertical: 10,
    paddingHorizontal: 18,
    borderRadius: 999,
    alignItems: "center",
    marginHorizontal: 4,
  },
  cancelButton: {
    backgroundColor: "#4b5563",
  },
  actionText: {
    color: "#f9fafb",
    fontWeight: "600",
    fontSize: 13,
  },
});
