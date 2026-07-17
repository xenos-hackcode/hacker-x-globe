// src/member/work/finance/screens/LearnScreen.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import React from "react";
import {
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

type Props = {
  onBack: () => void;
};

type Lesson = {
  id: string;
  title: string;
  level: "Beginner" | "Intermediate" | "Advanced";
  duration: string; // "5 min"
  tag: string;      // "Investing", "Risk", etc.
};

export default function LearnScreen({ onBack }: Props) {
  const { colors } = useTheme();

  const lessons: Lesson[] = [
    {
      id: "1",
      title: "What is an index fund?",
      level: "Beginner",
      duration: "4 min",
      tag: "Investing basics",
    },
    {
      id: "2",
      title: "Risk vs. volatility (and why it matters)",
      level: "Intermediate",
      duration: "6 min",
      tag: "Risk",
    },
    {
      id: "3",
      title: "Dollar-cost averaging in plain English",
      level: "Beginner",
      duration: "5 min",
      tag: "Strategy",
    },
    {
      id: "4",
      title: "How to read a candlestick chart",
      level: "Intermediate",
      duration: "8 min",
      tag: "Trading",
    },
  ];

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
            Overview
          </Text>
        </TouchableOpacity>

        <View>
          <Text style={[styles.title, { color: colors.textPrimary }]}>
            Learn
          </Text>
          <Text
            style={[styles.subTitle, { color: colors.textSecondary }]}
          >
            Short, focused lessons on investing and trading.
          </Text>
        </View>
      </View>

      {/* Content */}
      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.scrollContent}
      >
        {/* Featured banner */}
        <View style={[styles.banner, { borderColor: colors.border }]}>
          <View style={styles.bannerIconWrap}>
            <Ionicons name="school-outline" size={18} color="#bfdbfe" />
          </View>
          <View style={{ flex: 1 }}>
            <Text style={[styles.bannerTitle, { color: colors.textPrimary }]}>
              Daily 5‑minute briefing
            </Text>
            <Text
              style={[styles.bannerSub, { color: colors.textSecondary }]}
            >
              One quick concept a day to slowly level up your finance brain.
            </Text>
          </View>
        </View>

        {/* Lessons list */}
        <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>
          Saved lessons
        </Text>

        {lessons.map((lesson) => (
          <TouchableOpacity
            key={lesson.id}
            style={[styles.lessonCard, { borderColor: colors.border }]}
            activeOpacity={0.8}
            // later: onPress={() => openLesson(lesson)}
          >
            <View style={styles.lessonTopRow}>
              <Text
                style={[styles.lessonTitle, { color: colors.textPrimary }]}
                numberOfLines={2}
              >
                {lesson.title}
              </Text>
            </View>

            <View style={styles.lessonMetaRow}>
              <MetaPill label={lesson.level} icon="speedometer-outline" />
              <MetaPill label={lesson.duration} icon="time-outline" />
              <MetaPill label={lesson.tag} icon="pricetag-outline" />
            </View>
          </TouchableOpacity>
        ))}
      </ScrollView>
    </View>
  );
}

type MetaPillProps = {
  label: string;
  icon: keyof typeof Ionicons.glyphMap;
};

function MetaPill({ label, icon }: MetaPillProps) {
  return (
    <View style={styles.metaPill}>
      <Ionicons name={icon} size={11} color="#9ca3af" />
      <Text style={styles.metaPillText}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
  },
  header: {
    paddingTop: 12,
    paddingHorizontal: 16,
    paddingBottom: 8,
    flexDirection: "row",
    alignItems: "center",
    columnGap: 12,
  },
  backButton: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 6,
    paddingRight: 8,
    paddingLeft: 0,
  },
  backText: {
    fontSize: 13,
    marginLeft: 2,
  },
  title: {
    fontSize: 18,
    fontWeight: "600",
  },
  subTitle: {
    fontSize: 12,
    marginTop: 2,
  },
  scroll: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: 16,
    paddingBottom: 8,
    gap: 10,
  },
  banner: {
    flexDirection: "row",
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 14,
    padding: 12,
    backgroundColor: "#020617",
    columnGap: 10,
  },
  bannerIconWrap: {
    width: 32,
    height: 32,
    borderRadius: 999,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "rgba(37,99,235,0.35)",
  },
  bannerTitle: {
    fontSize: 14,
    fontWeight: "600",
    marginBottom: 2,
  },
  bannerSub: {
    fontSize: 11,
  },
  sectionLabel: {
    fontSize: 11,
    textTransform: "uppercase",
    letterSpacing: 1,
    marginTop: 4,
    marginBottom: 2,
  },
  lessonCard: {
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 14,
    padding: 12,
    backgroundColor: "#020617",
  },
  lessonTopRow: {
    marginBottom: 6,
  },
  lessonTitle: {
    fontSize: 14,
    fontWeight: "600",
  },
  lessonMetaRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    columnGap: 6,
    rowGap: 4,
  },
  metaPill: {
    flexDirection: "row",
    alignItems: "center",
    columnGap: 4,
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 999,
    backgroundColor: "#020617",
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: "#1f2937",
  },
  metaPillText: {
    fontSize: 10,
    color: "#9ca3af",
  },
});
