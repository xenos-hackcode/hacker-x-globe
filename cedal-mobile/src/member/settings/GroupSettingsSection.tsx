// src/member/settings/GroupSettingsSection.tsx
import SettingsRow from "@/src/member/settings/SettingsRow";
import { useTheme } from "@/src/themes/ThemeContext";
import React, { useState } from "react";
import { Alert, StyleSheet, Text, TouchableOpacity, View } from "react-native";

export default function GroupSettingsSection() {
  const { colors } = useTheme();

  // Global defaults for how group notifications behave across all groups.
  const [defaultMuteNewGroups, setDefaultMuteNewGroups] = useState(false);
  const [defaultMentionsOnly, setDefaultMentionsOnly] = useState(false);
  const [showJoinLeaveSystemMessages, setShowJoinLeaveSystemMessages] =
    useState(true);
  const [showTypingIndicators, setShowTypingIndicators] = useState(true);
  const [autoPinOwnedGroups, setAutoPinOwnedGroups] = useState(true);

  return (
    <>
      <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>
        Groups
      </Text>

      <View
        style={[
          styles.card,
          { borderColor: colors.border, backgroundColor: colors.background },
        ]}
      >
        {/* Default notifications for new groups */}
        <SettingsRow
          label="Mute new groups"
          description={
            defaultMuteNewGroups
              ? "Newly joined groups start muted."
              : "New groups follow your normal notification settings."
          }
          value={defaultMuteNewGroups}
          onValueChange={(val: boolean) => {
            setDefaultMuteNewGroups(val);
            if (val) setDefaultMentionsOnly(false);
          }}
        />

        <SettingsRow
          label="Mentions only by default"
          description="Only notify for @mentions when joining a new group."
          value={defaultMentionsOnly}
          onValueChange={(val: boolean) => {
            setDefaultMentionsOnly(val);
            if (val) setDefaultMuteNewGroups(false);
          }}
        />

        {/* System messages */}
        <SettingsRow
          label="Join & leave messages"
          description="Show small system messages when members join or leave groups."
          value={showJoinLeaveSystemMessages}
          onValueChange={setShowJoinLeaveSystemMessages}
        />

        {/* Typing indicators */}
        <SettingsRow
          label="Typing indicators in groups"
          description="Show when members are typing in any group chat."
          value={showTypingIndicators}
          onValueChange={setShowTypingIndicators}
        />

        {/* Owner quality-of-life */}
        <SettingsRow
          label="Auto‑pin owned groups"
          description="Automatically keep groups you create at the top of your list."
          value={autoPinOwnedGroups}
          onValueChange={setAutoPinOwnedGroups}
        />

        <View style={styles.footerRow}>
          <TouchableOpacity
            activeOpacity={0.8}
            style={styles.footerBtn}
            onPress={() => {
              // later: reset to persisted defaults
              setDefaultMuteNewGroups(false);
              setDefaultMentionsOnly(false);
              setShowJoinLeaveSystemMessages(true);
              setShowTypingIndicators(true);
              setAutoPinOwnedGroups(true);
              Alert.alert("Groups", "Group settings reset to defaults.");
            }}
          >
            <Text style={styles.footerBtnText}>Reset group settings</Text>
          </TouchableOpacity>
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
  footerRow: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: "rgba(31,41,55,0.9)",
    alignItems: "flex-start",
  },
  footerBtn: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
  },
  footerBtnText: {
    fontSize: 11,
    color: "#e5e7eb",
    letterSpacing: 1,
    textTransform: "uppercase",
  },
});
