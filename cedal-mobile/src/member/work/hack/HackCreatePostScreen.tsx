// src/member/work/hack/HackCreatePostScreen.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import * as ImagePicker from "expo-image-picker";
import React, { useState } from "react";
import {
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";

type Props = {
  roomName: string;
  onBack?: () => void;
  onSubmit?: (data: { body: string; videoUri?: string }) => void;
  initialBody?: string;
  mode?: "create" | "edit";
};

export default function HackCreatePostScreen({
  roomName,
  onBack,
  onSubmit,
  initialBody = "",
  mode = "create",
}: Props) {
  const { colors } = useTheme();
  const [body, setBody] = useState(initialBody);
  const [videoUri, setVideoUri] = useState<string | undefined>(undefined);

  const handlePost = () => {
    const text = body.trim();
    if (!text && !videoUri) return;
    onSubmit?.({ body: text, videoUri });
    setBody("");
    setVideoUri(undefined);
  };

  const pickVideoFromLibrary = async () => {
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== "granted") return;

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Videos,
      quality: 1,
    });

    if (!result.canceled && result.assets?.length) {
      setVideoUri(result.assets[0].uri);
    }
  };

  const recordVideoWithCamera = async () => {
    const { status } = await ImagePicker.requestCameraPermissionsAsync();
    if (status !== "granted") return;

    const result = await ImagePicker.launchCameraAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Videos,
      quality: 1,
    });

    if (!result.canceled && result.assets?.length) {
      setVideoUri(result.assets[0].uri);
    }
  };

  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity
          onPress={onBack}
          style={styles.backButton}
          activeOpacity={0.7}
        >
          <Ionicons
            name="chevron-back"
            size={20}
            color={colors.textPrimary}
          />
          <Text style={[styles.backText, { color: colors.textPrimary }]}>
            Back
          </Text>
        </TouchableOpacity>

        <View>
          <Text style={[styles.title, { color: colors.textPrimary }]}>
            {mode === "edit" ? "Edit post" : "Start a post"}
          </Text>
          <Text
            style={[styles.subtitle, { color: colors.textSecondary }]}
          >
            Posting in {roomName}
          </Text>
        </View>
      </View>

      {/* Body input */}
      <View style={styles.bodyContainer}>
        <TextInput
          style={styles.bodyInput}
          placeholder="Share an idea, question, or update..."
          placeholderTextColor="#6b7280"
          value={body}
          onChangeText={setBody}
          multiline
        />

        {videoUri && (
          <View style={styles.videoBadge}>
            <Ionicons
              name="videocam-outline"
              size={16}
              color="#e5e7eb"
              style={{ marginRight: 6 }}
            />
            <Text style={styles.videoBadgeText} numberOfLines={1}>
              Video attached
            </Text>
            <TouchableOpacity
              onPress={() => setVideoUri(undefined)}
              style={{ marginLeft: 8 }}
            >
              <Ionicons name="close-circle" size={18} color="#f97316" />
            </TouchableOpacity>
          </View>
        )}
      </View>

      {/* Media buttons */}
      <View style={styles.mediaRow}>
        <TouchableOpacity
          style={styles.mediaButton}
          onPress={pickVideoFromLibrary}
          activeOpacity={0.8}
        >
          <Ionicons
            name="folder-outline"
            size={16}
            color="#e5e7eb"
            style={{ marginRight: 6 }}
          />
          <Text style={styles.mediaButtonText}>Upload video</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.mediaButton}
          onPress={recordVideoWithCamera}
          activeOpacity={0.8}
        >
          <Ionicons
            name="videocam-outline"
            size={16}
            color="#e5e7eb"
            style={{ marginRight: 6 }}
          />
          <Text style={styles.mediaButtonText}>Record video</Text>
        </TouchableOpacity>
      </View>

      {/* Post button */}
      <View style={styles.footer}>
        <TouchableOpacity
          style={[
            styles.postButton,
            !body.trim() && !videoUri && { opacity: 0.4 },
          ]}
          disabled={!body.trim() && !videoUri}
          onPress={handlePost}
        >
          <Text style={styles.postButtonText}>
            {mode === "edit" ? "Save changes" : "Post"}
          </Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  header: {
    paddingTop: 40,
    paddingHorizontal: 16,
    paddingBottom: 12,
    flexDirection: "row",
    alignItems: "center",
    columnGap: 10,
  },
  backButton: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 6,
    paddingRight: 8,
  },
  backText: { fontSize: 13, marginLeft: 2 },
  title: { fontSize: 18, fontWeight: "600" },
  subtitle: { fontSize: 12, marginTop: 2 },
  bodyContainer: {
    flex: 1,
    marginHorizontal: 16,
    marginTop: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 12,
    backgroundColor: "rgba(15,23,42,0.95)",
  },
  bodyInput: {
    flex: 1,
    fontSize: 14,
    color: "#e5e7eb",
    textAlignVertical: "top",
  },
  videoBadge: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 10,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "rgba(15,23,42,0.9)",
  },
  videoBadgeText: {
    flex: 1,
    fontSize: 12,
    color: "#e5e7eb",
  },
  mediaRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    paddingHorizontal: 16,
    marginTop: 8,
  },
  mediaButton: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "rgba(15,23,42,0.9)",
  },
  mediaButtonText: {
    fontSize: 12,
    color: "#e5e7eb",
  },
  footer: {
    paddingHorizontal: 16,
    paddingBottom: 16,
    marginTop: 8,
  },
  postButton: {
    borderRadius: 999,
    backgroundColor: "#22c55e",
    alignItems: "center",
    justifyContent: "center",
    paddingVertical: 10,
  },
  postButtonText: {
    fontSize: 14,
    fontWeight: "600",
    color: "#020617",
  },
});
