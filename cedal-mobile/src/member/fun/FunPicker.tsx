// src/member/fun/FunPicker.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import React from "react";
import {
  ImageBackground,
  ImageSourcePropType,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

export type FunMode = "guild" | "group" | "games" | "streams";

type Props = {
  selectedMode: FunMode | null;
  onSelectMode?: (mode: FunMode) => void;
  footer?: React.ReactNode; // NEW
};

const guildImg = require("./images/guild.png");
const groupImg = require("./images/group.png");
const gamesImg = require("./images/games.png");
const streamsImg = require("./images/streams.png");

const modeImages: Record<FunMode, ImageSourcePropType> = {
  guild: guildImg,
  group: groupImg,
  games: gamesImg,
  streams: streamsImg,
};

export default function FunPicker({ selectedMode, onSelectMode, footer }: Props) {
  const { colors, isDark } = useTheme();
  const styles = makeStyles(colors, isDark);

  function handleSelect(mode: FunMode) {
    onSelectMode?.(mode);
  }

  return (
    <View style={styles.wrapper}>
      <View style={styles.overlayTop} />
      <View style={styles.overlayLine} />

      {/* header */}
      <View style={styles.header}>
        <Text style={styles.headerMeta}>Node shield selector</Text>
        <Text style={styles.headerTitle}>Welcome to a world of fantasy.</Text>
        <Text style={styles.headerSub}>
          Guilds and Groups are social shells. Games and Streams are active
          surfaces you can dock to them.
        </Text>
      </View>

      {/* scrollable column */}
      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.column}
        showsVerticalScrollIndicator={false}
      >
        <FunCard
          title="Guilds"
          mode="guild"
          description="Persistent hubs for clans, studios, and long‑running crews. Multi‑room, role‑aware, built for seasons not sessions."
          badge="MULTI‑ROOM"
          pill="STABLE · PERSISTENT"
          active={selectedMode === "guild"}
          onPress={handleSelect}
          styles={styles}
          image={modeImages.guild}
        />
        <FunCard
          title="Groups"
          mode="group"
          description="Tight circles anchored to a main chat. Spin up calls, share drops, and pin media without heavy structure."
          badge="LIGHTWEIGHT"
          pill="FAST · EPHEMERAL"
          active={selectedMode === "group"}
          onPress={handleSelect}
          styles={styles}
          image={modeImages.group}
        />
        <FunCard
          title="Games"
          mode="games"
          description="Shared worlds plugged into your chats. Start tiny party games now, grow into bigger arenas later."
          badge="INTERACTIVE"
          pill="STATE · SYNCED"
          active={selectedMode === "games"}
          onPress={handleSelect}
          styles={styles}
          image={modeImages.games}
        />
        <FunCard
          title="Streams"
          mode="streams"
          description="Live rooms, watch‑together sessions, and short broadcasts that reuse your call stack and presence."
          badge="LIVE"
          pill="REAL‑TIME · HOSTED"
          active={selectedMode === "streams"}
          onPress={handleSelect}
          styles={styles}
          image={modeImages.streams}
        />
      </ScrollView>

      {footer ? <View style={styles.footerWrap}>{footer}</View> : null}
    </View>
  );
}

type FunCardProps = {
  title: string;
  description: string;
  badge: string;
  pill: string;
  mode: FunMode;
  active: boolean;
  onPress: (mode: FunMode) => void;
  styles: ReturnType<typeof makeStyles>;
  image: ImageSourcePropType;
};

function FunCard({
  title,
  description,
  badge,
  pill,
  mode,
  active,
  onPress,
  styles,
  image,
}: FunCardProps) {
  return (
    <TouchableOpacity
      activeOpacity={0.9}
      onPress={() => onPress(mode)}
      style={[styles.card, active && styles.cardActiveShadow]}
    >
      <ImageBackground
        source={image}
        style={styles.cardImage}
        imageStyle={styles.cardImageInner}
      >
        <View style={styles.cardOverlay} />

        <View style={styles.cardHeaderRow}>
          <View>
            <Text style={styles.cardTitle}>{title}</Text>
            <View style={styles.badge}>
              <Text style={styles.badgeText}>{badge}</Text>
            </View>
          </View>
          <View style={styles.pill}>
            <Text style={styles.pillText}>{pill}</Text>
          </View>
        </View>

        <Text style={styles.cardDescription}>{description}</Text>
      </ImageBackground>
    </TouchableOpacity>
  );
}

const makeStyles = (colors: any, isDark: boolean) =>
  StyleSheet.create({
    wrapper: {
      borderRadius: 18,
      borderWidth: 1,
      borderColor: colors.border,
      backgroundColor: colors.cardBackground ?? colors.background,
      padding: 18,
      overflow: "hidden",
      flex: 1,
    },
    overlayTop: {
      position: "absolute",
      inset: 0,
      backgroundColor: isDark
        ? "rgba(15,23,42,0.95)"
        : "rgba(248,250,252,0.9)",
    },
    overlayLine: {
      position: "absolute",
      top: 0,
      left: 40,
      right: 40,
      height: 1,
      backgroundColor: isDark
        ? "rgba(56,189,248,0.8)"
        : "rgba(59,130,246,0.8)",
      opacity: 0.8,
    },
    header: {
      marginBottom: 16,
    },
    headerMeta: {
      fontSize: 11,
      letterSpacing: 2,
      textTransform: "uppercase",
      color: colors.textSecondary,
      marginBottom: 4,
    },
    headerTitle: {
      fontSize: 18,
      fontWeight: "600",
      color: colors.textPrimary,
      marginBottom: 4,
    },
    headerSub: {
      fontSize: 12,
      color: colors.textSecondary,
    },
    scroll: {
      flex: 1,
    },
    column: {
      gap: 12,
      paddingBottom: 12,
    },
    card: {
      borderRadius: 16,
      borderWidth: 1,
      borderColor: colors.border,
      overflow: "hidden",
    },
    cardImage: {
      width: "100%",
      padding: 12,
      justifyContent: "flex-start",
    },
    cardImageInner: {
      resizeMode: "cover",
    },
    cardOverlay: {
      ...StyleSheet.absoluteFillObject,
      backgroundColor: isDark
        ? "rgba(15,23,42,0.7)"
        : "rgba(15,23,42,0.15)",
    },
    cardActiveShadow: {
      shadowColor: "#38bdf8",
      shadowOpacity: 0.5,
      shadowRadius: 12,
      shadowOffset: { width: 0, height: 8 },
    },
    cardHeaderRow: {
      flexDirection: "row",
      justifyContent: "space-between",
      alignItems: "flex-start",
      marginBottom: 6,
    },
    cardTitle: {
      fontSize: 16,
      fontWeight: "600",
      color: "#e5e7eb",
    },
    badge: {
      marginTop: 3,
      paddingHorizontal: 8,
      paddingVertical: 2,
      borderRadius: 999,
      borderWidth: 1,
      borderColor: "rgba(15,23,42,0.9)",
      backgroundColor: "rgba(15,23,42,0.85)",
    },
    badgeText: {
      fontSize: 10,
      color: "#9ca3af",
      letterSpacing: 1,
      textTransform: "uppercase",
    },
    pill: {
      paddingHorizontal: 8,
      paddingVertical: 2,
      borderRadius: 999,
      borderWidth: 1,
      borderColor: "rgba(148,163,184,0.9)",
      backgroundColor: "rgba(15,23,42,0.9)",
    },
    pillText: {
      fontSize: 10,
      color: "#e5e7eb",
    },
    cardDescription: {
      fontSize: 12,
      color: "#e5e7eb",
      marginTop: 4,
      minHeight: 36,
    },
    footerWrap: {
      marginTop: 12,
    },
  });
