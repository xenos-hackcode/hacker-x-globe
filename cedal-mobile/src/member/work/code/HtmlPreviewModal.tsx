// src/member/work/code/HtmlPreviewModal.tsx
import React from "react";
import { Modal, View, Text, TouchableOpacity, StyleSheet } from "react-native";
import { WebView } from "react-native-webview";

type Props = {
  visible: boolean;
  html: string;
  onClose: () => void;
};

export default function HtmlPreviewModal({ visible, html, onClose }: Props) {
  return (
    <Modal visible={visible} animationType="slide" onRequestClose={onClose}>
      <View style={styles.root}>
        <View style={styles.header}>
          <Text style={styles.title}>Preview</Text>
          <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
            <Text style={styles.closeText}>Close</Text>
          </TouchableOpacity>
        </View>
        <WebView originWhitelist={["*"]} source={{ html }} style={styles.webview} />
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: "#020617" },
  header: {
    height: 48,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 14,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(148,163,184,0.3)",
  },
  title: { color: "#e5e7eb", fontSize: 14, fontWeight: "600" },
  closeBtn: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.5)",
  },
  closeText: { color: "#9ca3af", fontSize: 12 },
  webview: { flex: 1, backgroundColor: "#fff" },
});
