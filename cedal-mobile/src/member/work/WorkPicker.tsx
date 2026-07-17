// src/member/work/WorkPicker.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import { useRouter } from "expo-router";
import React from "react";
import {
  ImageBackground,
  ImageSourcePropType,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

export type WorkMode = "bank" | "investor" | "code" | "hack";

type Props = {
  selectedMode: WorkMode | null;
  onSelectMode?: (mode: WorkMode) => void;
};

const bankImg = require("./images/bank.png");
const investorImg = require("./images/investor.png");
const codeImg = require("./images/code.png");
const hackImg = require("./images/hack.png");

const modeImages: Record<WorkMode, ImageSourcePropType> = {
  bank: bankImg,
  investor: investorImg,
  code: codeImg,
  hack: hackImg,
};

export default function WorkPicker({ selectedMode, onSelectMode }: Props) {
  const { colors, isDark } = useTheme();
  const router = useRouter();
  const themedStyles = makeStyles(colors, isDark);

  function handleSelect(mode: WorkMode) {
  onSelectMode?.(mode);

  if (mode === "bank") {
    router.push("/(auth)/(member)/bank" as any);  // New bank screen
  } else if (mode === "code") {
    router.push("/(auth)/(member)/code" as any);
  } else if (mode === "hack") {
    router.push("/(auth)/(member)/hack" as any);
  } else if (mode === "investor") {
    router.push("/(auth)/(member)/finance" as any);
  }
}

  return (
    <View style={themedStyles.wrapper}>
      <View style={themedStyles.overlayTop} />
      <View style={themedStyles.overlayLine} />

      <View style={themedStyles.header}>
        <Text style={themedStyles.headerMeta}>Work mode selector</Text>
        <Text style={themedStyles.headerTitle}>Pick how you want to grind.</Text>
        <Text style={themedStyles.headerSub}>
          Bank, Invest, Code, and Hack are different shells for the same session.
        </Text>
      </View>

      <View style={themedStyles.column}>
        <WorkCard
          title="Bank"
          mode="bank"
          description="Treat your time like currency. Allocate focus, protect energy, and track where every hour goes."
          badge="FOCUS BUDGET"
          pill="SAFE · STRUCTURED"
          active={selectedMode === "bank"}
          onPress={handleSelect}
          styles={themedStyles}
          image={modeImages.bank}
        />
        <WorkCard
          title="Invest"
          mode="investor"
          description="Aim at long‑term gains. Skills, projects, and habits that pay off over months, not minutes."
          badge="LONG GAME"
          pill="GROWTH · STACKING"
          active={selectedMode === "investor"}
          onPress={handleSelect}
          styles={themedStyles}
          image={modeImages.investor}
        />
        <WorkCard
          title="Code"
          mode="code"
          description="Deep work lane for building, debugging, and shipping. Minimal noise, maximum throughput."
          badge="BUILD"
          pill="QUIET · PRECISE"
          active={selectedMode === "code"}
          onPress={handleSelect}
          styles={themedStyles}
          image={modeImages.code}
        />
        <WorkCard
          title="Hack"
          mode="hack"
          description="Short, aggressive sprints for experiments, refactors, and wild ideas. Move fast, then cool down."
          badge="SPRINT"
          pill="LOUD · RAPID"
          active={selectedMode === "hack"}
          onPress={handleSelect}
          styles={themedStyles}
          image={modeImages.hack}
        />
      </View>
    </View>
  );
}

type WorkCardProps = {
  title: string;
  description: string;
  badge: string;
  pill: string;
  mode: WorkMode;
  active: boolean;
  onPress: (mode: WorkMode) => void;
  styles: ReturnType<typeof makeStyles>;
  image: ImageSourcePropType;
};

function WorkCard({
  title,
  description,
  badge,
  pill,
  mode,
  active,
  onPress,
  styles,
  image,
}: WorkCardProps) {
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
    },
    overlayTop: {
      position: "absolute",
      inset: 0,
      backgroundColor: isDark
        ? "rgba(15,23,42,0.9)"
        : "rgba(248,250,252,0.9)",
    },
    overlayLine: {
      position: "absolute",
      top: 0,
      left: 40,
      right: 40,
      height: 1,
      backgroundColor: isDark
        ? "rgba(45,212,191,0.8)"
        : "rgba(59,130,246,0.7)",
      opacity: 0.6,
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
    column: {
      gap: 12,
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
      shadowColor: "#22c55e",
      shadowOpacity: 0.4,
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
  });
