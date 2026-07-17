// src/member/profile/components/ProfileAvatarBlock.tsx
import React from "react";
import { View, Text, StyleSheet, TouchableOpacity, Image } from "react-native";

type Props = {
  avatarUri: string | null;
  initial: string;
  nickname: string;
  handle: string;
  presence: "online" | "offline";
  onPickAvatar: () => void;
};

export function ProfileAvatarBlock({
  avatarUri,
  initial,
  nickname,
  handle,
  presence,
  onPickAvatar,
}: Props) {
  return (
    <View style={styles.profileBlock}>
      <TouchableOpacity
        style={styles.avatar}
        activeOpacity={0.8}
        onPress={onPickAvatar}
      >
        {avatarUri ? (
          <Image source={{ uri: avatarUri }} style={styles.avatarImage} />
        ) : (
          <Text style={styles.avatarInitial}>{initial}</Text>
        )}
      </TouchableOpacity>

      <Text style={styles.name}>{nickname}</Text>
      <Text style={styles.handle}>{handle}</Text>

      <View style={styles.presenceRow}>
        <View
          style={[
            styles.presenceDot,
            presence === "online"
              ? styles.presenceOnline
              : styles.presenceOffline,
          ]}
        />
        <Text style={styles.presenceText}>
          {presence === "online"
            ? "Online in Cedal mesh"
            : "Offline · shell in idle mode"}
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  profileBlock: { alignItems: "center", marginBottom: 16 },
  avatar: {
    width: 88,
    height: 88,
    borderRadius: 44,
    borderWidth: 2,
    borderColor: "rgba(56,189,248,0.9)",
    backgroundColor: "#0f172a",
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 8,
  },
  avatarImage: { width: "100%", height: "100%", borderRadius: 44 },
  avatarInitial: { fontSize: 36, color: "#e0f2fe" },
  name: { fontSize: 18, color: "#e5e7eb", fontWeight: "600" },
  handle: { fontSize: 12, color: "#9ca3af", marginTop: 2 },

  presenceRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 8,
    gap: 6,
  },
  presenceDot: { width: 8, height: 8, borderRadius: 4 },
  presenceOnline: { backgroundColor: "#22c55e" },
  presenceOffline: { backgroundColor: "#f97373" },
  presenceText: { fontSize: 11, color: "#a7f3d0" },
});
