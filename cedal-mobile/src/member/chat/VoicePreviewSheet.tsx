// src/member/chat/VoicePreviewSheet.tsx
import React, { useEffect, useState } from "react";
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Modal,
} from "react-native";
import Slider from "@react-native-community/slider";
import { Audio } from "expo-av";
import type { VoiceEditConfig } from "./VoiceEditSheet";

type Props = {
  visible: boolean;
  uri: string | null;
  editConfig: VoiceEditConfig | null;
  viewOnce: boolean;
  onToggleViewOnce: () => void;
  onClose: () => void;
  onSend: (uri: string) => void | Promise<void>;
  onDiscard: () => void;
  onEdit: (durationMs: number) => void;
};

export function VoicePreviewSheet({
  visible,
  uri,
  editConfig,
  viewOnce,
  onToggleViewOnce,
  onClose,
  onSend,
  onDiscard,
  onEdit,
}: Props) {
  const [sound, setSound] = useState<Audio.Sound | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [positionMillis, setPositionMillis] = useState(0);
  const [durationMillis, setDurationMillis] = useState(1);

  useEffect(() => {
    let isMounted = true;
    const load = async () => {
      if (!visible || !uri) return;
      try {
        const { sound: s, status } = await Audio.Sound.createAsync(
          { uri },
          { shouldPlay: false },
          (playbackStatus) => {
            if (!playbackStatus.isLoaded || !isMounted) return;
            const pos = playbackStatus.positionMillis ?? 0;
            const dur = playbackStatus.durationMillis ?? 1;

            if (
              editConfig &&
              pos >= editConfig.endMs &&
              playbackStatus.isPlaying
            ) {
              (async () => {
                try {
                  await s.pauseAsync();
                  await s.setPositionAsync(editConfig.startMs);
                } catch {
                  /* ignore */
                }
              })();
              setIsPlaying(false);
              setPositionMillis(editConfig.startMs);
              setDurationMillis(dur);
              return;
            }

            setPositionMillis(pos);
            setDurationMillis(dur);
            setIsPlaying(playbackStatus.isPlaying ?? false);
          }
        );
        if (!isMounted) {
          await s.unloadAsync();
          return;
        }
        setSound(s);
        if (status.isLoaded && status.durationMillis != null) {
          setDurationMillis(status.durationMillis);
        }
      } catch (e: any) {
        console.warn("load preview sound error", e?.message);
      }
    };
    load();

    return () => {
      isMounted = false;
      if (sound) {
        sound.unloadAsync().catch(() => {});
      }
      setSound(null);
      setIsPlaying(false);
      setPositionMillis(0);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visible, uri]);

  const togglePlay = async () => {
    if (!sound) return;
    try {
      if (isPlaying) {
        await sound.pauseAsync();
        setIsPlaying(false);
      } else {
        if (editConfig) {
          const currentPos = positionMillis;
          if (
            currentPos < editConfig.startMs ||
            currentPos >= editConfig.endMs
          ) {
            await sound.setPositionAsync(editConfig.startMs);
            setPositionMillis(editConfig.startMs);
          }
        }
        await sound.playAsync();
        setIsPlaying(true);
      }
    } catch (e: any) {
      console.warn("preview play/pause error", e?.message);
    }
  };

  const handleSeek = async (value: number) => {
    if (!sound) return;
    try {
      if (editConfig) {
        const clamped = Math.min(
          Math.max(value, editConfig.startMs),
          editConfig.endMs
        );
        await sound.setPositionAsync(clamped);
        setPositionMillis(clamped);
      } else {
        await sound.setPositionAsync(value);
        setPositionMillis(value);
      }
    } catch (e: any) {
      console.warn("preview seek error", e?.message);
    }
  };

  const handleSend = () => {
    if (!uri) return;
    onSend(uri);
    onClose();
  };

  const handleDiscard = () => {
    onDiscard();
    onClose();
  };

  const handleEdit = () => {
    onEdit(durationMillis);
  };

  if (!visible || !uri) return null;

  const mm = (millis: number) =>
    String(Math.floor(millis / 1000 / 60)).padStart(2, "0");
  const ss = (millis: number) =>
    String(Math.floor((millis / 1000) % 60)).padStart(2, "0");

  const startLabel =
    editConfig?.startMs != null
      ? `${mm(editConfig.startMs)}:${ss(editConfig.startMs)}`
      : null;
  const endLabel =
    editConfig?.endMs != null
      ? `${mm(editConfig.endMs)}:${ss(editConfig.endMs)}`
      : null;

  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={onClose}
    >
      <View style={styles.backdrop}>
        <View style={styles.sheet}>
          <Text style={styles.title}>Review voice message</Text>

          {editConfig && (
            <Text style={styles.trimInfo}>
              Using segment {startLabel} – {endLabel}
            </Text>
          )}

          <View style={styles.playRow}>
            <TouchableOpacity
              onPress={togglePlay}
              style={styles.playButton}
              activeOpacity={0.8}
            >
              <Text style={styles.playIcon}>{isPlaying ? "⏸" : "▶"}</Text>
            </TouchableOpacity>

            <View style={styles.sliderColumn}>
              <Slider
                style={styles.slider}
                minimumValue={0}
                maximumValue={durationMillis}
                value={positionMillis}
                minimumTrackTintColor="#22c55e"
                maximumTrackTintColor="#4b5563"
                thumbTintColor="#22c55e"
                onSlidingComplete={handleSeek}
              />
              <View style={styles.timeRow}>
                <Text style={styles.timeText}>
                  {mm(positionMillis)}:{ss(positionMillis)}
                </Text>
                <Text style={styles.timeText}>
                  {mm(durationMillis)}:{ss(durationMillis)}
                </Text>
              </View>
            </View>
          </View>

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

          <View style={styles.actionsRow}>
            <TouchableOpacity
              style={[styles.actionButton, styles.discardButton]}
              onPress={handleDiscard}
            >
              <Text style={styles.actionText}>Discard</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.actionButton, styles.editButton]}
              onPress={handleEdit}
            >
              <Text style={styles.actionText}>Edit</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[styles.actionButton, styles.sendButton]}
              onPress={handleSend}
            >
              <Text style={styles.actionText}>Send</Text>
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
    height: "40%",
  },
  title: {
    fontSize: 14,
    fontWeight: "600",
    color: "#e5e7eb",
    marginBottom: 6,
  },
  trimInfo: {
    fontSize: 11,
    color: "#a5b4fc",
    marginBottom: 8,
  },
  playRow: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 12,
  },
  playButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: "#22c55e",
    justifyContent: "center",
    alignItems: "center",
    marginRight: 12,
  },
  playIcon: {
    fontSize: 18,
    color: "#bbf7d0",
  },
  sliderColumn: {
    flex: 1,
  },
  slider: {
    width: "100%",
    height: 24,
  },
  timeRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 2,
  },
  timeText: {
    fontSize: 10,
    color: "#9ca3af",
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
  actionsRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 6,
  },
  actionButton: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 999,
    alignItems: "center",
    marginHorizontal: 4,
  },
  discardButton: {
    backgroundColor: "#4b5563",
  },
  editButton: {
    backgroundColor: "#0ea5e9",
  },
  sendButton: {
    backgroundColor: "#22c55e",
  },
  actionText: {
    color: "#f9fafb",
    fontWeight: "600",
    fontSize: 13,
  },
});
