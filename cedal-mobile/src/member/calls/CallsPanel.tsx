// src/member/calls/CallsPanel.tsx
import { useUserProfile } from "@/src/hooks/useUserProfile";
import { useChatFriends } from "@/src/member/chat/useChatFriends";
import { useTheme } from "@/src/themes/ThemeContext";
import React, { useEffect, useState } from "react";
import { Image, StyleSheet, Text, TouchableOpacity, View } from "react-native";

type Props = {
  onStartVoice?: (userId?: string) => void;
  onStartVideo?: (userId?: string) => void;
  onOpenSettings?: () => void;

  pingMs?: number; // live from useRtcStats
  jitterMs?: number; // live from useRtcStats
  audioLevel?: number; // 0–1 live from useRtcStats
};

export default function CallsPanel({
  onStartVoice,
  onStartVideo,
  onOpenSettings,
  pingMs = 40,
  jitterMs = 10,
  audioLevel = 0.4,
}: Props) {
  const { colors, isDark } = useTheme();
  const styles = makeStyles(colors, isDark);

  const { friends } = useChatFriends();
  const { user } = useUserProfile();

  // Only people in my chat list (and not me)
  const people = user ? friends.filter((f) => f.id !== user.uid) : friends;

  // Animated bar heights driven by real audioLevel
  const [barHeights, setBarHeights] = useState<number[]>([4, 8, 12, 10, 6, 9]);

  // Quality based on real ping + jitter thresholds
  const isBadSignal = pingMs > 150 || jitterMs > 60;
  const signalLabel = isBadSignal ? "Bad" : "Good";

  useEffect(() => {
    const id = setInterval(() => {
      const base = 4 + audioLevel * 14; // 4–18

      const next = Array.from({ length: 6 }).map((_, idx) => {
        const variance =
          ((Math.sin(Date.now() / 120 + idx) + 1) / 2) * (4 + audioLevel * 6);
        return Math.max(2, Math.min(20, base + variance));
      });

      setBarHeights(next);
    }, 100);

    return () => clearInterval(id);
  }, [audioLevel]);

  return (
    <View style={styles.wrapper}>
      <View style={styles.headerRow}>
        <Text style={styles.headerMeta}>Communication link</Text>
        <TouchableOpacity
          onPress={onOpenSettings}
          style={styles.settingsBtn}
          activeOpacity={0.8}
        >
          <Text style={styles.settingsIcon}>⚙️</Text>
        </TouchableOpacity>
      </View>

      <Text style={styles.headerTitle}>Choose who you want to call.</Text>
      <Text style={styles.headerSub}>
        Advanced Cedal call, uses advaced VPN and strong signal booster to get the most effective and best call experince you would love.
      </Text>

      {/* Live signal graph + ping/jitter */}
      <View style={styles.signalRow}>
        <View style={styles.graphRow}>
          {barHeights.map((h, idx) => {
            const goodEven = isDark ? "#4ade80" : "#16a34a";
            const goodOdd = isDark ? "#22d3ee" : "#0284c7";
            const badEven = "#f97373";
            const badOdd = "#3b82f6";

            const color = isBadSignal
              ? idx % 2 === 0
                ? badEven
                : badOdd
              : idx % 2 === 0
              ? goodEven
              : goodOdd;

            return (
              <View
                key={idx}
                style={[
                  styles.bar,
                  {
                    height: h,
                    backgroundColor: color,
                  },
                ]}
              />
            );
          })}
        </View>

        <View style={styles.signalMeta}>
          <Text style={styles.signalText}>
            Signal:{" "}
            <Text style={isBadSignal ? styles.signalBad : styles.signalGood}>
              {signalLabel}
            </Text>
          </Text>
          <Text style={styles.signalPing}>
            {Math.round(pingMs)} ms
            {jitterMs != null && ` · jitter ${Math.round(jitterMs)} ms`}
          </Text>
        </View>
      </View>

      {/* People list with call icons */}
      <View style={styles.listSection}>
        <Text style={styles.listHeader}>People</Text>

        {people.length === 0 ? (
          <Text style={styles.personStatus}>
            No chat friends yet. Start chatting to place calls.
          </Text>
        ) : (
          people.map((person) => (
            <View key={person.id} style={styles.personRow}>
              <View style={styles.personLeft}>
                <View style={styles.avatar}>
                  {person.avatarUri ? (
                    <Image
                      source={{ uri: person.avatarUri }}
                      style={styles.avatarImage}
                      resizeMode="cover"
                    />
                  ) : (
                    <Text style={styles.avatarInitial}>
                      {person.name?.charAt(0)?.toUpperCase() ?? "?"}
                    </Text>
                  )}
                </View>
                <View>
                  <Text style={styles.personName}>{person.name}</Text>
                  <Text style={styles.personStatus}>On your chat list</Text>
                </View>
              </View>

              <View style={styles.personActions}>
                <TouchableOpacity
                  style={[styles.iconBtn, styles.iconBtnVoice]}
                  activeOpacity={0.8}
                  onPress={() => onStartVoice?.(person.id)}
                >
                  <Text style={styles.iconBtnText}>📞</Text>
                </TouchableOpacity>
                <TouchableOpacity
                  style={[styles.iconBtn, styles.iconBtnVideo]}
                  activeOpacity={0.8}
                  onPress={() => onStartVideo?.(person.id)}
                >
                  <Text style={styles.iconBtnText}>🎥</Text>
                </TouchableOpacity>
              </View>
            </View>
          ))
        )}
      </View>
    </View>
  );
}

const makeStyles = (colors: any, isDark: boolean) =>
  StyleSheet.create({
    wrapper: {
      borderRadius: 22,
      borderWidth: 1,
      borderColor: colors.border ?? "rgba(56,189,248,0.35)",
      backgroundColor: colors.cardBackground ?? colors.background,
      padding: 18,
    },
    headerRow: {
      flexDirection: "row",
      justifyContent: "space-between",
      alignItems: "center",
      marginBottom: 10,
    },
    headerMeta: {
      fontSize: 11,
      letterSpacing: 2,
      textTransform: "uppercase",
      color: colors.textSecondary,
    },
    settingsBtn: {
      width: 30,
      height: 30,
      borderRadius: 999,
      borderWidth: 1,
      borderColor: colors.border,
      alignItems: "center",
      justifyContent: "center",
      backgroundColor: colors.cardBackground ?? colors.background,
    },
    settingsIcon: {
      fontSize: 16,
      color: colors.textSecondary,
    },
    headerTitle: {
      fontSize: 17,
      fontWeight: "600",
      color: colors.textPrimary,
      marginBottom: 4,
    },
    headerSub: {
      fontSize: 12,
      color: colors.textSecondary,
      marginBottom: 10,
    },

    // Signal row
    signalRow: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      marginBottom: 14,
    },
    graphRow: {
      flexDirection: "row",
      alignItems: "flex-end",
      gap: 4,
      height: 20,
    },
    bar: {
      width: 3,
      borderRadius: 999,
    },
    signalMeta: {
      alignItems: "flex-end",
    },
    signalText: {
      fontSize: 11,
      color: colors.textSecondary,
    },
    signalGood: {
      color: "#22c55e",
      fontWeight: "600",
    },
    signalBad: {
      color: "#f97373",
      fontWeight: "600",
    },
    signalPing: {
      fontSize: 11,
      color: colors.textSecondary,
      marginTop: 2,
    },

    // People list
    listSection: {
      marginTop: 4,
      gap: 6,
    },
    listHeader: {
      fontSize: 12,
      fontWeight: "600",
      color: colors.textSecondary,
      textTransform: "uppercase",
      letterSpacing: 1,
      marginBottom: 4,
    },
    personRow: {
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
      paddingVertical: 8,
      gap: 8,
    },
    personLeft: {
      flexDirection: "row",
      alignItems: "center",
      gap: 8,
      flex: 1,
    },
    avatar: {
      width: 32,
      height: 32,
      borderRadius: 16,
      alignItems: "center",
      justifyContent: "center",
      overflow: "hidden",
      backgroundColor: isDark ? "#0f172a" : "#e5f2ff",
    },
    avatarImage: {
      width: "100%",
      height: "100%",
    },
    avatarInitial: {
      fontSize: 14,
      fontWeight: "600",
      color: isDark ? "#e0f2fe" : "#0f172a",
    },
    personName: {
      fontSize: 13,
      fontWeight: "500",
      color: colors.textPrimary,
    },
    personStatus: {
      fontSize: 11,
      color: colors.textSecondary,
    },
    personActions: {
      flexDirection: "row",
      alignItems: "center",
      gap: 8,
    },
    iconBtn: {
      width: 28,
      height: 28,
      borderRadius: 999,
      alignItems: "center",
      justifyContent: "center",
      borderWidth: 1,
    },
    iconBtnVoice: {
      borderColor: "rgba(34,197,94,0.8)",
      backgroundColor: isDark ? "#022c22" : "#dcfce7",
    },
    iconBtnVideo: {
      borderColor: "rgba(59,130,246,0.8)",
      backgroundColor: isDark ? "#0b1120" : "#dbeafe",
    },
    iconBtnText: {
      fontSize: 14,
      color: colors.textPrimary,
    },
  });
