// src/member/chat/ChatAudioGallery.tsx
import React, { useState } from "react";
import {
  Modal,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  FlatList,
} from "react-native";
import { Audio } from "expo-av";
import { useSafeAreaInsets } from "react-native-safe-area-context";

type Item = {
  id: string;
  uri: string;
  label: string;
};

type Props = {
  visible: boolean;
  items: Item[];
  onClose: () => void;
};

export function ChatAudioGallery({ visible, items, onClose }: Props) {
  const insets = useSafeAreaInsets();
  const [currentId, setCurrentId] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [sound, setSound] = useState<Audio.Sound | null>(null);

  if (!visible) return null;

  const handlePlay = async (item: Item) => {
    try {
      if (currentId === item.id && sound) {
        await sound.stopAsync();
        await sound.unloadAsync();
        setSound(null);
        setCurrentId(null);
        return;
      }

      setLoading(true);

      if (sound) {
        await sound.stopAsync();
        await sound.unloadAsync();
      }

      const s = new Audio.Sound();
      await s.loadAsync({ uri: item.uri });
      await s.playAsync();
      setSound(s);
      setCurrentId(item.id);
    } catch (e: any) {
      console.warn("audio gallery play error", e?.message);
      setCurrentId(null);
    } finally {
      setLoading(false);
    }
  };

  const handleClose = async () => {
    if (sound) {
      try {
        await sound.stopAsync();
        await sound.unloadAsync();
      } catch {}
    }
    setSound(null);
    setCurrentId(null);
    onClose();
  };

  return (
    <Modal
      transparent
      animationType="slide"
      visible={visible}
      onRequestClose={handleClose}
    >
      <View style={styles.backdrop}>
        <View
          style={[
            styles.sheet,
            { paddingBottom: 16 + insets.bottom }, // safe‑area padding
          ]}
        >
          <View style={styles.headerRow}>
            <Text style={styles.title}>Chat audio</Text>
            <TouchableOpacity onPress={handleClose} style={styles.closeBtn}>
              <Text style={styles.closeText}>✕</Text>
            </TouchableOpacity>
          </View>

          <FlatList
            data={items}
            keyExtractor={(item) => item.id}
            contentContainerStyle={styles.list}
            ItemSeparatorComponent={() => <View style={styles.separator} />}
            renderItem={({ item }) => {
              const playing = currentId === item.id;
              return (
                <TouchableOpacity
                  style={styles.row}
                  activeOpacity={0.8}
                  onPress={() => handlePlay(item)}
                >
                  <View style={styles.iconCircle}>
                    <Text style={styles.iconText}>
                      {loading && playing ? "…" : playing ? "⏸" : "▶"}
                    </Text>
                  </View>
                  <View style={styles.textBlock}>
                    <Text style={styles.label} numberOfLines={1}>
                      {item.label}
                    </Text>
                    <Text style={styles.subLabel}>
                      Tap to {playing ? "pause" : "play"}
                    </Text>
                  </View>
                </TouchableOpacity>
              );
            }}
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
    maxHeight: "70%",
    backgroundColor: "#020617",
    borderTopLeftRadius: 18,
    borderTopRightRadius: 18,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.4)",
    paddingBottom: 16,
  },
  headerRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: 6,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(51,65,85,0.8)",
  },
  title: { color: "#e5e7eb", fontSize: 14, fontWeight: "600" },
  closeBtn: {
    width: 26,
    height: 26,
    borderRadius: 13,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    alignItems: "center",
    justifyContent: "center",
  },
  closeText: { color: "#9ca3af", fontSize: 13 },
  list: {
    paddingHorizontal: 12,
    paddingTop: 8,
    paddingBottom: 12,
  },
  separator: {
    height: StyleSheet.hairlineWidth,
    backgroundColor: "rgba(30,64,175,0.5)",
    marginLeft: 12,
  },
  row: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 8,
  },
  iconCircle: {
    width: 32,
    height: 32,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.7)",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 10,
    backgroundColor: "rgba(15,23,42,0.9)",
  },
  iconText: { color: "#e0f2fe", fontSize: 14, fontWeight: "700" },
  textBlock: { flex: 1 },
  label: { color: "#e5e7eb", fontSize: 13 },
  subLabel: { color: "#9ca3af", fontSize: 11, marginTop: 2 },
});
