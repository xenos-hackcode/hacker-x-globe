// src/member/profile/components/ProfileHeader.tsx
import React, { useState } from "react";
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Image,
  Modal,
} from "react-native";

type PresenceStatus = "offline" | "online" | "inChat";

type Props = {
  nickname: string;
  handle: string;
  presence: PresenceStatus;
  avatarUrl: string | null;
  initial: string;
  followers?: number;   // optional, default 0
  following?: number;   // optional, default 0
  onBack?: () => void;
  onVoiceCall?: () => void;
  onVideoCall?: () => void;
};

export function ProfileHeader({
  nickname,
  handle,
  presence,
  avatarUrl,
  initial,
  followers = 0,
  following = 0,
  onBack,
  onVoiceCall,
  onVideoCall,
}: Props) {
  const [showFullAvatar, setShowFullAvatar] = useState(false);

  let presenceDotStyle = styles.presenceOffline;
  let presenceLabel = "Offline · shell in idle mode";

  if (presence === "online") {
    presenceDotStyle = styles.presenceOnline;
    presenceLabel = "Online in Cedal mesh";
  } else if (presence === "inChat") {
    presenceDotStyle = styles.presenceInChat;
    presenceLabel = "Active in this chat";
  }

  return (
    <View>
      {/* top bar */}
      <View style={styles.topBar}>
        {onBack && (
          <TouchableOpacity
            onPress={onBack}
            activeOpacity={0.7}
            style={styles.backBtn}
          >
            <Text style={styles.backText}>Back</Text>
          </TouchableOpacity>
        )}

        <View style={styles.topTextWrap}>
          <Text style={styles.topMeta}>User Profile</Text>
          <Text style={styles.topRole}>Node</Text>
        </View>
      </View>

      {/* avatar + name + presence + calls + followers */}
      <View style={styles.headerInfo}>
        <TouchableOpacity
          style={styles.avatar}
          activeOpacity={0.8}
          onPress={() => setShowFullAvatar(true)}
        >
          {avatarUrl ? (
            <Image source={{ uri: avatarUrl }} style={styles.avatarImage} />
          ) : (
            <Text style={styles.avatarInitial}>{initial}</Text>
          )}
        </TouchableOpacity>

        <Text style={styles.name}>{nickname}</Text>
        <Text style={styles.handle}>{handle}</Text>

        <View style={styles.presenceRow}>
          <View style={[styles.presenceDot, presenceDotStyle]} />
          <Text style={styles.presenceText}>{presenceLabel}</Text>
        </View>

        {/* call buttons */}
        <View style={styles.callRow}>
  <TouchableOpacity
    style={[styles.callBtn, styles.callBtnVoice]}
    activeOpacity={0.8}
    onPress={onVoiceCall}
  >
    <Text style={styles.callBtnText}>📞 Call</Text>
  </TouchableOpacity>
  <TouchableOpacity
    style={[styles.callBtn, styles.callBtnVideo]}
    activeOpacity={0.8}
    onPress={onVideoCall}
  >
    <Text style={styles.callBtnText}>🎥 Video</Text>
  </TouchableOpacity>
</View>

        {/* followers / following */}
        <View style={styles.followRow}>
          <Text style={styles.followText}>
            {followers} followers · {following} following
          </Text>
        </View>
      </View>

      {/* full-screen avatar modal */}
      <Modal
        visible={showFullAvatar}
        transparent
        animationType="fade"
        onRequestClose={() => setShowFullAvatar(false)}
      >
        <TouchableOpacity
          activeOpacity={1}
          style={styles.fullscreenOverlay}
          onPress={() => setShowFullAvatar(false)}
        >
          <View style={styles.fullscreenCenter}>
            {avatarUrl ? (
              <Image
                source={{ uri: avatarUrl }}
                style={styles.fullscreenImage}
                resizeMode="contain"
              />
            ) : (
              <View style={styles.fullscreenInitialCircle}>
                <Text style={styles.fullscreenInitial}>{initial}</Text>
              </View>
            )}
          </View>
        </TouchableOpacity>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  topBar: { flexDirection: "row", alignItems: "center", marginBottom: 12 },
  backBtn: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
    marginRight: 12,
  },
  backText: { color: "#e5e7eb", fontSize: 13, letterSpacing: 1.5 },
  topTextWrap: { flexDirection: "column" },
  topMeta: {
    fontSize: 10,
    textTransform: "uppercase",
    letterSpacing: 2,
    color: "#9ca3af",
  },
  topRole: { fontSize: 11, color: "#a5b4fc", marginTop: 2 },

  headerInfo: { alignItems: "center", marginBottom: 12 },
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
  presenceInChat: { backgroundColor: "#ef4444" },
  presenceText: { fontSize: 11, color: "#a7f3d0" },

  callRow: { flexDirection: "row", gap: 8, marginTop: 10 },
  callBtn: {
    flex: 1,
    borderRadius: 999,
    paddingVertical: 7,
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 1,
  },
  callBtnVoice: {
    borderColor: "rgba(34,197,94,0.9)",
    backgroundColor: "#022c22",
  },
  callBtnVideo: {
    borderColor: "rgba(59,130,246,0.9)",
    backgroundColor: "#0b1120",
  },
  callBtnText: { fontSize: 11, color: "#e5e7eb" },

  followRow: { marginTop: 6 },
  followText: { fontSize: 11, color: "#9ca3af" },

  fullscreenOverlay: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.9)",
    justifyContent: "center",
    alignItems: "center",
  },
  fullscreenCenter: {
    width: "100%",
    height: "100%",
    justifyContent: "center",
    alignItems: "center",
  },
  fullscreenImage: {
    width: "92%",
    height: "92%",
  },
  fullscreenInitialCircle: {
    width: 220,
    height: 220,
    borderRadius: 110,
    backgroundColor: "#020617",
    borderWidth: 3,
    borderColor: "rgba(56,189,248,0.9)",
    justifyContent: "center",
    alignItems: "center",
  },
  fullscreenInitial: {
    fontSize: 110,
    color: "#e0f2fe",
  },
});
