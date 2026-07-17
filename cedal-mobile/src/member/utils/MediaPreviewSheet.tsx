// src/member/utils/MediaPreviewSheet.tsx
import React from "react";
import {
  View,
  Text,
  StyleSheet,
  Image,
  TouchableOpacity,
} from "react-native";

type Props = {
  uri: string;
  type?: "image" | "video";
  viewOnce: boolean;
  onToggleViewOnce: () => void;
  onSend: () => Promise<void>;
  onCancel: () => void;
};

export function MediaPreviewSheet({
  uri,
  viewOnce,
  onToggleViewOnce,
  onSend,
  onCancel,
}: Props) {
  return (
    <View style={styles.backdrop}>
      <View style={styles.panel}>
        <Image source={{ uri }} style={styles.image} resizeMode="contain" />

        <View style={styles.optionsRow}>
          <TouchableOpacity onPress={onToggleViewOnce}>
            <Text
              style={[
                styles.viewOnceText,
                viewOnce && styles.viewOnceActive,
              ]}
            >
              {viewOnce ? "✓ View once" : "View once"}
            </Text>
          </TouchableOpacity>
        </View>

        <View style={styles.buttonsRow}>
          <TouchableOpacity onPress={onCancel}>
            <Text style={styles.cancelText}>Cancel</Text>
          </TouchableOpacity>
          <TouchableOpacity onPress={onSend}>
            <Text style={styles.sendText}>Send</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    position: "absolute",
    left: 0,
    right: 0,
    bottom: 0,
    top: 0,
    backgroundColor: "rgba(15,23,42,0.85)",
    justifyContent: "flex-end",
  },
  panel: {
    backgroundColor: "#020617",
    padding: 12,
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.4)",
  },
  image: {
    width: "100%",
    height: 260,
    borderRadius: 12,
    marginBottom: 10,
  },
  optionsRow: {
    flexDirection: "row",
    justifyContent: "flex-start",
    marginBottom: 8,
  },
  viewOnceText: {
    fontSize: 12,
    color: "#9ca3af",
  },
  viewOnceActive: {
    color: "#22c55e",
  },
  buttonsRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  cancelText: {
    fontSize: 14,
    color: "#f97373",
  },
  sendText: {
    fontSize: 14,
    color: "#22c55e",
    fontWeight: "600",
  },
});
