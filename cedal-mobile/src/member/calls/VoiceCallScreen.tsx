// src/member/calls/VoiceCallScreen.tsx
import { useChatFriends } from "@/src/member/chat/useChatFriends";
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import React, { useEffect, useState } from "react";
import { Image, StyleSheet, Text, TouchableOpacity, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context"; 

type CallState = "calling" | "ringing" | "in_call" | "ended";

type Props = {
  calleeId: string;
  onSwitchToVideo?: () => void;
  onEnd?: () => void;
};

export default function VoiceCallScreen({
  calleeId,
  onSwitchToVideo,
  onEnd,
}: Props) {
  const { colors, isDark } = useTheme();
  const insets = useSafeAreaInsets(); 
  const styles = makeStyles(colors, isDark, insets);
  const { friends } = useChatFriends();
  

  const callee = friends.find((f) => f.id === calleeId);

  const [callState, setCallState] = useState<CallState>("calling");
  const [muted, setMuted] = useState(false);
  const [speakerOn, setSpeakerOn] = useState(false);
  const [elapsedSeconds, setElapsedSeconds] = useState(0);
  const [recording, setRecording] = useState(false);

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
      ? "Calling..."
      : callState === "in_call"
      ? formatDuration(elapsedSeconds)
      : callState === "ringing"
      ? "Ringing..."
      : "Call ended";

  return (
    <View style={styles.root}>
      {/* Top label like old version */}
      <View style={styles.topRow}>
        <Text style={styles.appName}>cedal voice Link</Text>
      </View>

      {/* Center: avatar + info */}
      <View style={styles.centerSection}>
        <View style={styles.avatarWrapper}>
          <View style={styles.avatar}>
            {callee?.avatarUri ? (
              <Image
                source={{ uri: callee.avatarUri }}
                style={styles.avatarImage}
              />
            ) : (
              <Text style={styles.avatarInitial}>
                {title.charAt(0).toUpperCase()}
              </Text>
            )}
          </View>
        </View>

        <Text style={styles.calleeName}>{title}</Text>
        <Text style={styles.callStateText}>{stateLabel}</Text>
        <Text style={styles.callModeText}>Voice call</Text>
      </View>

      {/* Controls */}
      <View style={styles.controlsSection}>
        <View style={styles.controlsRow}>
          {/* Mute */}
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

          {/* Switch to video */}
          <TouchableOpacity
            onPress={onSwitchToVideo}
            style={styles.controlBtnSmall}
            activeOpacity={0.8}
          >
            <Ionicons name="videocam-outline" size={22} color="#22c55e" />
            <Text style={styles.controlLabel}>Video</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.controlsRow}>
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
      paddingTop: insets.top,  // ← Add this
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
    centerSection: {
      alignItems: "center",
    },
    avatarWrapper: {
      marginBottom: 18,
      padding: 4,
      borderRadius: 999,
      borderWidth: 1,
      borderColor: "rgba(59,130,246,0.7)",
    },
    avatar: {
      width: 96,
      height: 96,
      borderRadius: 48,
      overflow: "hidden",
      alignItems: "center",
      justifyContent: "center",
      backgroundColor: isDark ? "#0f172a" : "#e5f2ff",
    },
    avatarImage: { width: "100%", height: "100%" },
    avatarInitial: {
      fontSize: 40,
      fontWeight: "700",
      color: isDark ? "#e0f2fe" : "#0f172a",
    },
    calleeName: {
      fontSize: 20,
      fontWeight: "600",
      color: colors.textPrimary,
      marginBottom: 4,
    },
    callStateText: {
      fontSize: 14,
      color: colors.textSecondary,
      marginBottom: 2,
    },
    callModeText: {
      fontSize: 12,
      color: colors.textSecondary,
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
