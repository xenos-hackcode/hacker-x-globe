// src/member/profile/components/ProfileHeader.tsx
import React from "react";
import { View, Text, TouchableOpacity, StyleSheet } from "react-native";

type Props = {
  title?: string;
  role?: string;
  backLabel?: string;
  onBack?: () => void;
};

export function ProfileHeader({
  title = "My Profile",
  role = "",
  backLabel = "Back",
  onBack,
}: Props) {
  return (
    <View style={styles.topBar}>
      {onBack && (
        <TouchableOpacity
          onPress={onBack}
          activeOpacity={0.7}
          style={styles.backBtn}
        >
          <Text style={styles.backText}>{backLabel}</Text>
        </TouchableOpacity>
      )}

      <View style={styles.topTextWrap}>
        <Text style={styles.topMeta}>{title}</Text>
        {!!role && <Text style={styles.topRole}>{role}</Text>}
      </View>
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
});
