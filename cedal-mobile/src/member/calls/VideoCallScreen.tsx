// src/member/calls/VideoCallScreen.tsx
import { useChatFriends } from "@/src/member/chat/useChatFriends";
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import React, { useEffect, useState } from "react";
import { Image, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context"; 

type CallState = "calling" | "ringing" | "in_call" | "ended";

type Props = {
  calleeId: string;
  onSwitchToVoice?: () => void;
  onEnd?: () => void;
};

export default function VideoCallScreen({
  calleeId,
  onSwitchToVoice,
  onEnd,
}: Props) {
  const { colors, isDark } = useTheme();
  const insets = useSafeAreaInsets(); 
  const { friends } = useChatFriends();
  const styles = makeStyles(colors, isDark, insets); 

  const callee = friends.find((f) => f.id === calleeId);

  const [callState, setCallState] = useState<CallState>("calling");
  const [muted, setMuted] = useState(false);
  const [speakerOn, setSpeakerOn] = useState(false);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [recording, setRecording] = useState(false);
  const [cameraOff, setCameraOff] = useState(false);

  useEffect(() => {
    if (callState !== "calling") return;
    const t = setTimeout(() => setCallState("in_call"), 2000);
    return () => clearTimeout(t);
  }, [callState]);

  useEffect(() => {
    if (callState !== "in_call") return;
    const timer = setInterval(() => setElapsedSeconds((s) => s + 1), 1000);
    return () => clearInterval(timer);
  }, [callState]);

  const handleEnd = () => {
    setCallState("ended");
    onEnd?.();
  };

  const formatDuration = (total: number) => {
    const m = Math.floor(total / 60)
      .toString()
      .padStart(2, "0");
    const s = (total % 60).toString().padStart(2, "0");
    return `${m}:${s}`;
  };

  const title = callee?.name ?? "Unknown";
  const stateLabel =
    callState === "calling"
      ? "Starting video..."
      : callState === "in_call"
      ? formatDuration(elapsedSeconds)
      : callState === "ringing"
      ? "Ringing..."
      : "Call ended";

  return (
    <View style={styles.root}>
      {/* Top title like old version */}
      <View style={styles.topRow}>
        <Text style={styles.appName}>cedal Video Link</Text>
      </View>

      {/* Video area */}
      <View style={styles.videoSection}>
        <View style={styles.remoteVideo}>
          {cameraOff ? (
            <View style={styles.remotePlaceholder}>
              <Ionicons name="videocam-off" size={40} color="#e5e7eb" />
              <Text style={styles.remoteInitial}>
                {title.charAt(0).toUpperCase()}
              </Text>
            </View>
          ) : callee?.avatarUri ? (
            <Image
              source={{ uri: callee.avatarUri }}
              style={styles.remoteVideoImage}
            />
          ) : (
            <View style={styles.remotePlaceholder}>
              <Text style={styles.remoteInitial}>
                {title.charAt(0).toUpperCase()}
              </Text>
            </View>
          )}

          <View style={styles.remoteOverlay}>
            <Text style={styles.remoteName}>{title}</Text>
            <Text style={styles.remoteState}>{stateLabel}</Text>
          </View>
        </View>

        <View style={styles.localPreview}>
          <View style={styles.localPreviewInner}>
            <Text style={styles.localPreviewText}>You</Text>
          </View>
        </View>
      </View>

      {/* Controls */}
      <View style={styles.controlsSection}>
        <View style={styles.controlsRow}>
          {/* Mute mic */}
          <TouchableOpacity
            onPress={() => setMuted((m) => !m)}
            style={[styles.controlBtn, muted && styles.controlBtnActive]}
            activeOpacity={0.8}
          >
            <Ionicons
              name={muted ? "mic-off" : "mic"}
              size={24}
              color={muted ? "#f97373" : "#e5e7eb"}
            />
            <Text style={styles.controlLabel}>
              {muted ? "Unmute" : "Mute"}
            </Text>
          </TouchableOpacity>

          {/* Speaker */}
          <TouchableOpacity
            onPress={() => setSpeakerOn((s) => !s)}
            style={[styles.controlBtn, speakerOn && styles.controlBtnActive]}
            activeOpacity={0.8}
          >
            <Ionicons
              name={speakerOn ? "volume-high" : "volume-medium"}
              size={24}
              color={speakerOn ? "#22c55e" : "#e5e7eb"}
            />
            <Text style={styles.controlLabel}>
              {speakerOn ? "Speaker" : "Earpiece"}
            </Text>
          </TouchableOpacity>

          {/* Switch to voice */}
          <TouchableOpacity
            onPress={onSwitchToVoice}
            style={styles.controlBtnSmall}
            activeOpacity={0.8}
          >
            <Ionicons name="call-outline" size={22} color="#22c55e" />
            <Text style={styles.controlLabel}>Voice</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.controlsRow}>
          {/* Camera on/off */}
          <TouchableOpacity
            onPress={() => setCameraOff((c) => !c)}
            style={[
              styles.controlBtnSmall,
              cameraOff && styles.controlBtnActive,
            ]}
            activeOpacity={0.8}
          >
            <Ionicons
              name={cameraOff ? "camera-reverse-outline" : "camera-outline"}
              size={22}
              color={cameraOff ? "#f97373" : "#e5e7eb"}
            />
            <Text style={styles.controlLabel}>
              {cameraOff ? "Cam muted" : "Cam on"}
            </Text>
          </TouchableOpacity>

          {/* Record */}
          <TouchableOpacity
            onPress={() => setRecording((r) => !r)}
            style={[
              styles.controlBtnSmall,
              recording && styles.controlBtnRecordActive,
            ]}
            activeOpacity={0.8}
          >
            <Ionicons
              name={recording ? "stop" : "radio-button-on"}
              size={22}
              color={recording ? "#fee2e2" : "#f97373"}
            />
            <Text style={styles.controlLabel}>
              {recording ? "Stop" : "Record"}
            </Text>
          </TouchableOpacity>

          {/* Add */}
          <TouchableOpacity
            onPress={() => {}}
            style={styles.controlBtnSmall}
            activeOpacity={0.8}
          >
            <Ionicons name="add" size={24} color="#e5e7eb" />
            <Text style={styles.controlLabel}>Add</Text>
          </TouchableOpacity>
        </View>

        {/* End */}
        <View style={styles.bottomRow}>
          <TouchableOpacity
            onPress={handleEnd}
            style={styles.endCallBtn}
            activeOpacity={0.9}
          >
            <Ionicons name="call" size={26} color="#fee2e2" />
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
}

const makeStyles = (colors: any, isDark: boolean, insets: any) =>
  StyleSheet.create({
    root: {
  flex: 1,
  backgroundColor: isDark ? "#020617" : "#020617",
  paddingHorizontal: 16,
  paddingTop: insets.top,
  paddingBottom: Math.max(insets.bottom, 24),
  justifyContent: "space-between",
},
    topRow: {
      alignItems: "center",
    },
    appName: {
      fontSize: 12,
      letterSpacing: 2,
      textTransform: "uppercase",
      color: colors.textSecondary,
    },
    videoSection: {
      flex: 1,
      marginVertical: 12,
      borderRadius: 18,
      overflow: "hidden",
      backgroundColor: "#020617",
    },
    remoteVideo: {
      flex: 1,
      backgroundColor: "#020617",
      borderRadius: 18,
      overflow: "hidden",
      justifyContent: "flex-end",
    },
    remoteVideoImage: {
      ...StyleSheet.absoluteFillObject,
      width: "100%",
      height: "100%",
    },
    remotePlaceholder: {
      ...StyleSheet.absoluteFillObject,
      alignItems: "center",
      justifyContent: "center",
      backgroundColor: "#020617",
      gap: 8,
    },
    remoteInitial: {
      fontSize: 42,
      fontWeight: "700",
      color: "#e5f2ff",
    },
    remoteOverlay: {
      padding: 16,
      backgroundColor: "rgba(15,23,42,0.55)",
    },
    remoteName: {
      fontSize: 18,
      fontWeight: "600",
      color: "#e5e7eb",
    },
    remoteState: {
      fontSize: 12,
      color: "#cbd5f5",
      marginTop: 2,
    },
    localPreview: {
      position: "absolute",
      right: 12,
      top: 24,
      width: 96,
      height: 144,
      borderRadius: 12,
      overflow: "hidden",
      borderWidth: 1,
      borderColor: "rgba(148,163,184,0.8)",
      backgroundColor: "#020617",
    },
    localPreviewInner: {
      flex: 1,
      alignItems: "center",
      justifyContent: "center",
    },
    localPreviewText: {
      fontSize: 12,
      color: "#e5e7eb",
    },
    controlsSection: { paddingBottom: 4 },
    controlsRow: {
      flexDirection: "row",
      justifyContent: "space-evenly",
      flexWrap: "wrap",
      marginBottom: 16,
      gap: 10,
    },
    controlBtn: {
      width: 90,
      height: 90,
      borderRadius: 999,
      backgroundColor: "rgba(15,23,42,0.9)",
      borderWidth: 1,
      borderColor: "rgba(148,163,184,0.6)",
      alignItems: "center",
      justifyContent: "center",
      gap: 6,
    },
    controlBtnSmall: {
      width: 80,
      height: 80,
      borderRadius: 999,
      backgroundColor: "rgba(15,23,42,0.9)",
      borderWidth: 1,
      borderColor: "rgba(148,163,184,0.6)",
      alignItems: "center",
      justifyContent: "center",
      gap: 4,
    },
    controlBtnActive: {
      borderColor: "#22c55e",
      backgroundColor: "rgba(22,163,74,0.18)",
    },
    controlBtnRecordActive: {
      borderColor: "#f97373",
      backgroundColor: "rgba(248,113,113,0.18)",
    },
    controlLabel: {
      fontSize: 12,
      color: colors.textSecondary,
      textAlign: "center",
    },
    bottomRow: { alignItems: "center" },
    endCallBtn: {
      width: 76,
      height: 76,
      borderRadius: 999,
      backgroundColor: "#ef4444",
      alignItems: "center",
      justifyContent: "center",
      transform: [{ rotate: "135deg" }],
      shadowColor: "#000",
      shadowOffset: { width: 0, height: 4 },
      shadowOpacity: 0.4,
      shadowRadius: 8,
      elevation: 6,
    },
  });
