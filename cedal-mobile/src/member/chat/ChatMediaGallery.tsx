// src/member/chat/ChatMediaGallery.tsx
import React from "react";
import {
  Modal,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Image,
  FlatList,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

type Item = {
  id: string;
  uri: string;
  kind: "image" | "video" | "stickerImage" | "stickerVideo";
};

type Props = {
  visible: boolean;
  items: Item[];
  onClose: () => void;
};

export function ChatMediaGallery({ visible, items, onClose }: Props) {
  const insets = useSafeAreaInsets();

  if (!visible) return null;

  return (
    <Modal
      transparent
      animationType="slide"
      visible={visible}
      onRequestClose={onClose}
    >
      <View style={styles.backdrop}>
        <View
          style={[
            styles.sheet,
            { paddingBottom: 24 + insets.bottom }, // safe-area padding
          ]}
        >
          <View style={styles.headerRow}>
            <Text style={styles.title}>Chat media</Text>
            <TouchableOpacity onPress={onClose} style={styles.closeBtn}>
              <Text style={styles.closeText}>✕</Text>
            </TouchableOpacity>
          </View>

          <FlatList
            data={items}
            keyExtractor={(item) => item.id}
            numColumns={3}
            contentContainerStyle={styles.grid}
            renderItem={({ item }) => (
              <View style={styles.thumbWrapper}>
                <Image source={{ uri: item.uri }} style={styles.thumb} />
                {(item.kind === "video" || item.kind === "stickerVideo") && (
                  <View style={styles.playBadge}>
                    <Text style={styles.playText}>▶</Text>
                  </View>
                )}
              </View>
            )}
          />
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
    maxHeight: "75%",
    backgroundColor: "#020617",
    borderTopLeftRadius: 18,
    borderTopRightRadius: 18,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.4)",
    paddingTop: 10,
    paddingBottom: 24,
  },
  headerRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingHorizontal: 16,
    marginBottom: 8,
  },
  title: {
    color: "#e5e7eb",
    fontSize: 14,
    fontWeight: "600",
  },
  closeBtn: {
    width: 28,
    height: 28,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    alignItems: "center",
    justifyContent: "center",
  },
  closeText: {
    color: "#9ca3af",
    fontSize: 13,
  },
  grid: {
    paddingHorizontal: 8,
    paddingBottom: 16,
  },
  thumbWrapper: {
    width: "33.33%",
    aspectRatio: 1,
    padding: 4,
  },
  thumb: {
    flex: 1,
    borderRadius: 8,
    backgroundColor: "#020617",
  },
  playBadge: {
    position: "absolute",
    bottom: 8,
    right: 8,
    backgroundColor: "rgba(15,23,42,0.7)",
    borderRadius: 999,
    paddingHorizontal: 6,
    paddingVertical: 2,
  },
  playText: {
    color: "#f9fafb",
    fontSize: 10,
  },
});
