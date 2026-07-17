// src/member/profile/components/BlockUserPanel.tsx
import React, { useState } from "react";
import {
  Modal,
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ActivityIndicator,
} from "react-native";

type Props = {
  open: boolean;
  onClose: () => void;
  onConfirm: () => Promise<void>; // async block action
  targetName: string;
};

export function BlockUserPanel({ open, onClose, onConfirm, targetName }: Props) {
  const [loading, setLoading] = useState(false);

  if (!open) return null;

  const handleConfirm = async () => {
    if (loading) return;
    setLoading(true);
    try {
      await onConfirm();
      onClose();
    } catch (e) {
      // optional: show toast
    } finally {
      setLoading(false);
    }
  };

  return (
    <Modal transparent animationType="fade" visible={open} onRequestClose={onClose}>
      <View style={styles.overlay}>
        <View style={styles.card}>
          <Text style={styles.title}>Block {targetName}?</Text>
          <Text style={styles.body}>
            They will no longer be able to message you or add you to new groups. 
            Existing messages stay, but new chats are blocked.
          </Text>

          <View style={styles.actionsRow}>
            <TouchableOpacity
              style={styles.cancelBtn}
              onPress={onClose}
              disabled={loading}
              activeOpacity={0.8}
            >
              <Text style={styles.cancelText}>Cancel</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.blockBtn}
              onPress={handleConfirm}
              disabled={loading}
              activeOpacity={0.8}
            >
              {loading ? (
                <ActivityIndicator size="small" color="#0f172a" />
              ) : (
                <Text style={styles.blockText}>Block</Text>
              )}
            </TouchableOpacity>
          </View>
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
    borderColor: "rgba(248,113,113,0.6)",
    backgroundColor: "rgba(15,23,42,0.98)",
    padding: 16,
  },
  title: {
    fontSize: 16,
    fontWeight: "600",
    color: "#fee2e2",
    marginBottom: 8,
  },
  body: {
    fontSize: 13,
    color: "#e5e7eb",
    marginBottom: 14,
  },
  actionsRow: {
    flexDirection: "row",
    justifyContent: "flex-end",
    gap: 8,
  },
  cancelBtn: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#4b5563",
    backgroundColor: "#020617",
  },
  cancelText: {
    fontSize: 12,
    color: "#e5e7eb",
    textTransform: "uppercase",
    letterSpacing: 0.8,
  },
  blockBtn: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "#ef4444",
  },
  blockText: {
    fontSize: 12,
    color: "#0f172a",
    fontWeight: "600",
    textTransform: "uppercase",
    letterSpacing: 0.8,
  },
});
