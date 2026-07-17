// src/member/chat/partials/ClearChatSheet.tsx
import React from "react";
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  TextInput,
} from "react-native";
import { useChatScreenState } from "@/src/hooks/useChatScreenState";

type Props = {
  state: ReturnType<typeof useChatScreenState>;
};

export function ClearChatSheet({ state }: Props) {
  const {
    clearSheetOpen,
    setClearSheetOpen,
    clearDurationMinutes,
    setClearDurationMinutes,
    clearLocalHistory, // ← pulled from hook
  } = state as any;

  if (!clearSheetOpen) return null;

  const handleClose = () => setClearSheetOpen(false);

  // Clear everything and persist clearedAt
  const handleClearAll = async () => {
    try {
      await clearLocalHistory(); // writes clearedAt + sets hiddenUntil
    } finally {
      handleClose();
    }
  };

  // Local-only time window clear (optional)
  const handleClearByTime = () => {
    const minutes = Number(clearDurationMinutes) || 0;
    if (minutes <= 0) {
      handleClose();
      return;
    }
    const cutoff = Date.now() - minutes * 60 * 1000;
    state.setHiddenUntil?.(cutoff); // just client-side filter
    handleClose();
  };

  return (
    <View style={styles.overlay}>
      <View style={styles.sheet}>
        <Text style={styles.title}>Clear chat</Text>

        <TouchableOpacity style={styles.button} onPress={handleClearAll}>
          <Text style={styles.buttonText}>Clear all messages</Text>
        </TouchableOpacity>

        <View style={styles.divider} />

        <Text style={styles.label}>Clear messages older than</Text>
        <View style={styles.row}>
          <TextInput
            style={styles.input}
            keyboardType="numeric"
            value={String(clearDurationMinutes)}
            onChangeText={(txt) =>
              setClearDurationMinutes(Number(txt) || 0)
            }
          />
          <Text style={styles.unit}>minutes</Text>
        </View>

        <TouchableOpacity
          style={[styles.button, styles.primary]}
          onPress={handleClearByTime}
        >
          <Text style={styles.buttonText}>Apply</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.cancel} onPress={handleClose}>
          <Text style={styles.cancelText}>Cancel</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    position: "absolute",
    left: 0,
    right: 0,
    bottom: 0,
    top: 0,
    backgroundColor: "rgba(0,0,0,0.6)",
    justifyContent: "flex-end",
  },
  sheet: {
    padding: 16,
    backgroundColor: "#020617",
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
  },
  title: {
    fontSize: 16,
    fontWeight: "600",
    color: "#e5e7eb",
    marginBottom: 12,
  },
  button: {
    paddingVertical: 10,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.5)",
    alignItems: "center",
    marginBottom: 12,
  },
  primary: {
    borderColor: "rgba(59,130,246,0.9)",
    backgroundColor: "#0b1120",
  },
  buttonText: { color: "#e5e7eb", fontSize: 13 },
  divider: {
    height: 1,
    backgroundColor: "rgba(30,64,175,0.7)",
    marginVertical: 12,
  },
  label: { fontSize: 12, color: "#9ca3af", marginBottom: 6 },
  row: { flexDirection: "row", alignItems: "center" },
  input: {
    flex: 0,
    width: 70,
    borderRadius: 6,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.5)",
    paddingHorizontal: 8,
    paddingVertical: 4,
    color: "#e5e7eb",
    fontSize: 13,
  },
  unit: { marginLeft: 8, color: "#9ca3af", fontSize: 13 },
  cancel: { marginTop: 8, alignItems: "center" },
  cancelText: { fontSize: 13, color: "#9ca3af" },
});
