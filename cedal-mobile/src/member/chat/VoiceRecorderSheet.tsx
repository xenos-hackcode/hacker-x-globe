// src/member/chat/VoiceRecorderSheet.tsx
import React, { useEffect, useState } from "react";
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Modal,
  Platform,
} from "react-native";
import { Audio } from "expo-av";
import {
  startChatRecording,
  stopChatRecording,
} from "@/src/member/utils/chatAudioRecorder";

type Props = {
  visible: boolean;
  onClose: () => void;
  onRecorded: (uri: string) => void;
};

export function VoiceRecorderSheet({ visible, onClose, onRecorded }: Props) {
  const [recording, setRecording] = useState<Audio.Recording | null>(null);
  const [status, setStatus] = useState<"idle" | "recording" | "paused">("idle");
  const [seconds, setSeconds] = useState(0);

  useEffect(() => {
    if (status !== "recording") return;
    const id = setInterval(() => {
      setSeconds((s) => s + 1);
    }, 1000);
    return () => clearInterval(id);
  }, [status]);

  const resetState = () => {
    setRecording(null);
    setStatus("idle");
    setSeconds(0);
  };

  const handleStart = async () => {
    if (Platform.OS === "web") return;

    if (recording) {
      try {
        await recording.startAsync();
        setStatus("recording");
      } catch (e: any) {
        console.warn("resume recording error", e?.message);
      }
      return;
    }

    const res = await startChatRecording();
    if (!res) return;
    setRecording(res.recording);
    setStatus("recording");
    setSeconds(0);
  };

  const handlePause = async () => {
    if (!recording) return;
    try {
      await recording.pauseAsync();
      setStatus("paused");
    } catch (e: any) {
      console.warn("pause recording error", e?.message);
    }
  };

  const handleStop = async () => {
    if (!recording) return;
    try {
      try {
        await recording.startAsync();
      } catch {
        // ignore if already recording
      }
      const uri = await stopChatRecording(recording);
      if (uri) {
        onRecorded(uri);
      }
    } catch (e: any) {
      console.warn("stop recording error", e?.message);
    }
    resetState();
    onClose();
  };

  const handleCancel = () => {
    resetState();
    onClose();
  };

  if (!visible) return null;

  const isRecording = status === "recording";
  const isPaused = status === "paused";
  const mm = String(Math.floor(seconds / 60)).padStart(2, "0");
  const ss = String(seconds % 60).padStart(2, "0");

  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={handleCancel}
    >
      <View style={styles.backdrop}>
        <View style={styles.sheet}>
          <Text style={styles.title}>Voice message</Text>
          <Text style={styles.subtitle}>
            {isRecording
              ? "Recording... you can pause or stop."
              : isPaused
              ? "Paused. Resume or stop to send."
              : "Tap record to start."}
          </Text>

          {(isRecording || isPaused) && (
            <Text style={styles.timerText}>
              {mm}:{ss}
            </Text>
          )}

          <View style={styles.controlsRow}>
            {status === "idle" && (
              <>
                <TouchableOpacity
                  style={[styles.button, styles.recordButton]}
                  onPress={handleStart}
                >
                  <Text style={styles.buttonText}>Record</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.button, styles.cancelButton]}
                  onPress={handleCancel}
                >
                  <Text style={styles.buttonText}>Cancel</Text>
                </TouchableOpacity>
              </>
            )}

            {isPaused && (
              <>
                <TouchableOpacity
                  style={[styles.button, styles.recordButton]}
                  onPress={handleStart}
                >
                  <Text style={styles.buttonText}>Resume</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.button, styles.stopButton]}
                  onPress={handleStop}
                >
                  <Text style={styles.buttonText}>Stop</Text>
                </TouchableOpacity>
              </>
            )}

            {isRecording && (
              <>
                <TouchableOpacity
                  style={[styles.button, styles.pauseButton]}
                  onPress={handlePause}
                >
                  <Text style={styles.buttonText}>Pause</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.button, styles.stopButton]}
                  onPress={handleStop}
                >
                  <Text style={styles.buttonText}>Stop</Text>
                </TouchableOpacity>
              </>
            )}
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
    height: "40%", // or "80%", etc.
  },
  title: {
    fontSize: 14,
    fontWeight: "600",
    color: "#e5e7eb",
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 11,
    color: "#9ca3af",
    marginBottom: 10,
  },
  timerText: {
    fontSize: 18,
    fontWeight: "600",
    color: "#f97316",
    textAlign: "center",
    marginBottom: 12,
  },
  controlsRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: 4,
  },
  button: {
    paddingHorizontal: 18,
    paddingVertical: 10,
    borderRadius: 999,
    minWidth: 110,
    alignItems: "center",
  },
  recordButton: {
    backgroundColor: "#ef4444",
  },
  pauseButton: {
    backgroundColor: "#f97316",
  },
  stopButton: {
    backgroundColor: "#22c55e",
  },
  cancelButton: {
    backgroundColor: "#4b5563",
  },
  buttonText: {
    color: "#f9fafb",
    fontWeight: "600",
    fontSize: 13,
  },
});
