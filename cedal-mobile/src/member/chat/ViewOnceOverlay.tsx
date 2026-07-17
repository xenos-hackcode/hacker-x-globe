// src/member/chat/ViewOnceOverlay.tsx
import React from "react";
import { View, Text, Image, StyleSheet, TouchableOpacity, ActivityIndicator } from "react-native";
import { Video, ResizeMode } from "expo-av";
import { Audio } from "expo-av";
import { Message } from "./MessageRow";

type Props = {
  message: Message;
  onClose: () => Promise<void> | void;
};

export function ViewOnceOverlay({ message, onClose }: Props) {
  const [sound, setSound] = React.useState<Audio.Sound | null>(null);
  const [playing, setPlaying] = React.useState(false);
  const [loading, setLoading] = React.useState(false);

  const headerTitle = message.videoUri
    ? "View once video"
    : message.imageUri
    ? "View once photo"
    : "View once voice";

  const handleAudioPress = async () => {
    if (!message.audioUri) return;
    try {
      if (playing && sound) {
        await sound.stopAsync();
        await sound.unloadAsync();
        setSound(null);
        setPlaying(false);
        return;
      }
      setLoading(true);
      if (!sound) {
        const newSound = new Audio.Sound();
        await newSound.loadAsync({ uri: message.audioUri });
        setSound(newSound);
        await newSound.playAsync();
        setPlaying(true);
      } else {
        await sound.playAsync();
        setPlaying(true);
      }
    } catch (e: any) {
      console.warn("view-once audio error", e?.message);
      setPlaying(false);
    } finally {
      setLoading(false);
    }
  };

  React.useEffect(() => {
    return () => {
      if (sound) {
        sound.unloadAsync().catch(() => {});
      }
    };
  }, [sound]);

  return (
    <View style={styles.backdrop}>
      <View style={styles.header}>
        <Text style={styles.title}>{headerTitle}</Text>
        <Text style={styles.close} onPress={onClose}>
          Close
        </Text>
      </View>
      <View style={styles.body}>
        {message.imageUri ? (
          <Image
            source={{ uri: message.imageUri }}
            style={styles.media}
            resizeMode="contain"
          />
        ) : message.videoUri ? (
          <Video
            style={styles.media}
            source={{ uri: message.videoUri }}
            useNativeControls
            resizeMode={ResizeMode.CONTAIN}
            shouldPlay
          />
        ) : message.audioUri ? (
          <TouchableOpacity
            style={styles.audioButton}
            onPress={handleAudioPress}
            activeOpacity={0.8}
          >
            <View style={styles.audioIconCircle}>
              {loading ? (
                <ActivityIndicator color="#bbf7d0" />
              ) : (
                <Text style={styles.audioIconText}>
                  {playing ? "⏸" : "▶"}
                </Text>
              )}
            </View>
            <View style={styles.audioTextBlock}>
              <Text style={styles.audioTitle}>Voice message</Text>
              <Text style={styles.audioSubtitle}>
                {playing ? "Playing..." : "Tap to play"}
              </Text>
            </View>
          </TouchableOpacity>
        ) : null}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    position: "absolute",
    left: 0,
    right: 0,
    top: 0,
    bottom: 0,
    backgroundColor: "rgba(0,0,0,0.95)",
    zIndex: 999,
    paddingTop: 40,
    paddingHorizontal: 12,
    paddingBottom: 24,
  },
  header: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 12,
  },
  title: { fontSize: 14, color: "#e5e7eb", fontWeight: "600" },
  close: { fontSize: 13, color: "#f97373" },
  body: { flex: 1, justifyContent: "center", alignItems: "center" },
  media: { width: "100%", height: "100%" },

  audioButton: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#22c55e",
    backgroundColor: "rgba(21,128,61,0.25)",
  },
  audioIconCircle: {
    width: 32,
    height: 32,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: "#22c55e",
    justifyContent: "center",
    alignItems: "center",
    marginRight: 10,
    backgroundColor: "rgba(21,128,61,0.35)",
  },
  audioIconText: {
    fontSize: 16,
    fontWeight: "700",
    color: "#bbf7d0",
  },
  audioTextBlock: {
    flexShrink: 1,
  },
  audioTitle: {
    fontSize: 14,
    color: "#e5e7eb",
  },
  audioSubtitle: {
    fontSize: 12,
    color: "#9ca3af",
    marginTop: 2,
  },
});
