//src/member/chat/StickerPreviewSheet.tsx
import React from "react";
import {
  Modal,
  View,
  Image,
  Text,
  TouchableOpacity,
  StyleSheet,
  Switch,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

type Props = {
  url: string;
  viewOnce: boolean;
  onToggleViewOnce: () => void;
  onCancel: () => void;
  onSend: () => void;
};

export function StickerPreviewSheet({
  url,
  viewOnce,
  onToggleViewOnce,
  onCancel,
  onSend,
}: Props) {
  const insets = useSafeAreaInsets();

  return (
    <Modal transparent animationType="slide" visible={true}>
      <View style={styles.backdrop}>
        <View
          style={[
            styles.sheet,
            { paddingBottom: 32 + insets.bottom },
          ]}
        >
          <View style={styles.previewWrapper}>
            <Image source={{ uri: url }} style={styles.previewImage} />
          </View>

          <View style={styles.controlsRow}>
            <View style={styles.viewOnceRow}>
              <Text style={styles.viewOnceLabel}>View once</Text>
              <Switch
                value={viewOnce}
                onValueChange={onToggleViewOnce}
                thumbColor={viewOnce ? "#38bdf8" : "#1f2937"}
                trackColor={{ true: "#0ea5e9", false: "#4b5563" }}
              />
            </View>

            <View style={styles.actionsRow}>
              <TouchableOpacity style={styles.cancelBtn} onPress={onCancel}>
                <Text style={styles.cancelText}>Cancel</Text>
              </TouchableOpacity>

              <TouchableOpacity style={styles.sendBtn} onPress={onSend}>
                <Text style={styles.sendText}>Send</Text>
              </TouchableOpacity>
            </View>
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
    paddingBottom: 32,
    maxHeight: "92%",
  },
  previewWrapper: {
    alignItems: "center",
    justifyContent: "center",
    marginVertical: 16,
  },
  previewImage: {
    width: "80%",
    aspectRatio: 1,
    borderRadius: 16,
    backgroundColor: "#0f172a",
  },
  controlsRow: {
    marginTop: 8,
    gap: 12,
  },
  viewOnceRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 4,
  },
  viewOnceLabel: {
    color: "#e5e7eb",
    fontSize: 14,
    fontWeight: "500",
  },
  actionsRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 12,
  },
  cancelBtn: {
    flex: 1,
    marginRight: 8,
    paddingVertical: 10,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.8)",
    alignItems: "center",
    justifyContent: "center",
  },
  cancelText: {
    color: "#9ca3af",
    fontSize: 13,
    textTransform: "uppercase",
    letterSpacing: 1,
  },
  sendBtn: {
    flex: 1,
    marginLeft: 8,
    paddingVertical: 10,
    borderRadius: 999,
    backgroundColor: "#38bdf8",
    alignItems: "center",
    justifyContent: "center",
  },
  sendText: {
    color: "#0f172a",
    fontSize: 13,
    fontWeight: "600",
    textTransform: "uppercase",
    letterSpacing: 1,
  },
});
