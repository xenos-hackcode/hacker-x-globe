// src/member/settings/ChatSettingsSection.tsx
import SettingsRow from "@/src/member/settings/SettingsRow";
import { useTheme } from "@/src/themes/ThemeContext";
import * as DocumentPicker from "expo-document-picker";
import React, { useState } from "react";
import {
  Alert,
  StyleSheet,
  Text,
  TouchableOpacity,
  View
} from "react-native";

export default function ChatSettingsSection() {
  const { colors } = useTheme();

  const [notificationVolume, setNotificationVolume] = useState(0.7);
  const [soundsEnabled, setSoundsEnabled] = useState(true);
  const [soundFile, setSoundFile] = useState<string | null>(null);
  const [typingIndicators, setTypingIndicators] = useState(true);
  const [recordingStatus, setRecordingStatus] = useState<"idle" | "recording">(
    "idle"
  );

  async function startRecordingSnippet() {
    try {
      setRecordingStatus("recording");

      Alert.alert(
        "Recording",
        "Recording started. Play your song on speaker, then tap again to stop."
      );
    } catch (e: any) {
      Alert.alert(
        "Record error",
        e?.message ?? "Failed to start recording."
      );
    }
  }

  async function stopRecordingSnippet() {
    try {
      setRecordingStatus("idle");

      const fakeRecordingUri = "file:///fake/path/cedal-recording.m4a";
      setSoundFile(fakeRecordingUri);

      Alert.alert(
        "Recorded",
        "Sample recorded and set as current alert sound."
      );
    } catch (e: any) {
      Alert.alert(
        "Record error",
        e?.message ?? "Failed to stop recording."
      );
    }
  }

  const isRecording = recordingStatus === "recording";

  return (
    <>
      <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>
        Chat
      </Text>

      <View
        style={[
          styles.card,
          { borderColor: colors.border, backgroundColor: colors.background },
        ]}
      >
        {/* Notification volume */}
        <View style={styles.row}>
          <View style={styles.rowTextBlock}>
            <Text style={[styles.rowLabel, { color: colors.textPrimary }]}>
              Notification volume
            </Text>
            <Text
              style={[
                styles.rowDescription,
                { color: colors.textSecondary },
              ]}
            >
              Controls how loud Cedal alerts sound in the app.
            </Text>

            <View style={styles.volumeBarRow}>
              {/* Minus */}
              <TouchableOpacity
                style={styles.volumeStepBtn}
                activeOpacity={0.8}
                onPress={() =>
                  setNotificationVolume((prev) =>
                    Math.max(0, parseFloat((prev - 0.1).toFixed(1)))
                  )
                }
              >
                <Text style={styles.volumeStepText}>-</Text>
              </TouchableOpacity>

              {/* Bar */}
              <View
                style={[
                  styles.volumeBarTrack,
                  { backgroundColor: colors.border },
                ]}
              >
                <View
                  style={[
                    styles.volumeBarFill,
                    {
                      width: `${Math.round(notificationVolume * 100)}%`,
                    },
                  ]}
                />
              </View>

              {/* Plus */}
              <TouchableOpacity
                style={styles.volumeStepBtn}
                activeOpacity={0.8}
                onPress={() =>
                  setNotificationVolume((prev) =>
                    Math.min(1, parseFloat((prev + 0.1).toFixed(1)))
                  )
                }
              >
                <Text style={styles.volumeStepText}>+</Text>
              </TouchableOpacity>

              {/* Percent label */}
              <Text
                style={[
                  styles.volumeValue,
                  { color: colors.textPrimary },
                ]}
              >
                {Math.round(notificationVolume * 100)}%
              </Text>
            </View>
          </View>
        </View>

        {/* Sounds toggle */}
        <SettingsRow
          label="Sounds"
          description={
            soundFile
              ? "Custom alert sound selected."
              : "Play in‑app sounds for events."
          }
          value={soundsEnabled}
          onValueChange={(val: boolean) => setSoundsEnabled(val)}
        />

        {/* Typing indicators */}
        <SettingsRow
          label="Typing indicators"
          description="Show when you and others are typing."
          value={typingIndicators}
          onValueChange={(val: boolean) => setTypingIndicators(val)}
        />

        {/* Alert sound: Record / Pick */}
        <View
          style={[
            styles.row,
            styles.rowLast,
            { flexDirection: "column", alignItems: "flex-start" },
          ]}
        >
          <Text style={[styles.rowLabel, { color: colors.textPrimary }]}>
            Alert sound
          </Text>
          <Text
            style={[
              styles.rowDescription,
              { color: colors.textSecondary, marginBottom: 8 },
            ]}
          >
            Record a snippet from any song or pick an audio file. The last one
            you set becomes your alert sound.
          </Text>

          <View style={styles.alertActionsRow}>
            <TouchableOpacity
              style={[
                styles.alertBtn,
                isRecording && styles.alertBtnRecording,
              ]}
              activeOpacity={0.8}
              onPress={isRecording ? stopRecordingSnippet : startRecordingSnippet}
            >
              <Text style={styles.alertBtnText}>
                {isRecording ? "Stop" : "Record"}
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.alertBtn}
              activeOpacity={0.8}
              onPress={async () => {
                try {
                  const result = await DocumentPicker.getDocumentAsync({
                    type: "audio/*",
                    copyToCacheDirectory: true,
                  });
                  if (result.canceled) return;
                  const asset = result.assets?.[0];
                  if (!asset?.uri) return;

                  setSoundFile(asset.uri);
                } catch (e: any) {
                  Alert.alert(
                    "Sound picker",
                    e?.message ?? "Failed to pick a sound file."
                  );
                }
              }}
            >
              <Text style={styles.alertBtnText}>Pick file</Text>
            </TouchableOpacity>
          </View>

          {soundFile && (
            <Text
              style={[
                styles.rowDescription,
                { color: colors.textSecondary, marginTop: 4 },
              ]}
              numberOfLines={1}
            >
              Selected sound: {soundFile}
            </Text>
          )}
        </View>
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  sectionLabel: {
    fontSize: 11,
    letterSpacing: 2,
    textTransform: "uppercase",
    marginBottom: 6,
  },
  card: {
    borderRadius: 18,
    borderWidth: 1,
    marginBottom: 18,
    overflow: "hidden",
  },
  row: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(31,41,55,0.9)",
  },
  rowLast: {
    borderBottomWidth: 0,
  },
  rowTextBlock: {
    flex: 1,
    marginRight: 10,
  },
  rowLabel: {
    fontSize: 13,
    fontWeight: "500",
  },
  rowDescription: {
    fontSize: 11,
    marginTop: 2,
  },
  volumeBarRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 6,
    gap: 8,
  },
  volumeBarTrack: {
    flex: 1,
    height: 6,
    borderRadius: 999,
    overflow: "hidden",
  },
  volumeBarFill: {
    height: "100%",
    borderRadius: 999,
    backgroundColor: "#22c55e",
  },
  volumeValue: {
    fontSize: 13,
  },
  volumeStepBtn: {
    width: 28,
    height: 28,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
    alignItems: "center",
    justifyContent: "center",
  },
  volumeStepText: {
    color: "#e5e7eb",
    fontSize: 16,
    fontWeight: "600",
  },
  alertActionsRow: {
    flexDirection: "row",
    gap: 8,
    marginTop: 4,
  },
  alertBtn: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
    backgroundColor: "#020617",
  },
  alertBtnRecording: {
    borderColor: "#ef4444",
    backgroundColor: "#7f1d1d",
  },
  alertBtnText: {
    fontSize: 11,
    color: "#e5e7eb",
    letterSpacing: 1,
    textTransform: "uppercase",
  },
});
