// app/(auth)/(member)/about.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import { useRouter } from "expo-router";
import React from "react";
import {
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

export default function AboutScreen() {
  const router = useRouter();
  const { colors } = useTheme();

  function handleBack() {
    router.back();
  }

  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      {/* Top bar */}
      <View
        style={[
          styles.topBar,
          {
            borderBottomColor: colors.border,
            backgroundColor:
              (colors as any).headerBackground ?? colors.background,
          },
        ]}
      >
        <TouchableOpacity
          onPress={handleBack}
          activeOpacity={0.7}
          style={styles.backBtn}
        >
          <Text style={[styles.backText, { color: colors.textPrimary }]}>
            Back
          </Text>
        </TouchableOpacity>
        <Text style={[styles.topTitle, { color: colors.textPrimary }]}>
          About
        </Text>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        {/* Hero card */}
        <View
          style={[
            styles.card,
            { borderColor: colors.border, backgroundColor: colors.background },
          ]}
        >
          <View style={styles.heroTop}>
            <Text style={[styles.heroTitle, { color: colors.textPrimary }]}>
              Cedal mesh node client
            </Text>
            <Text style={[styles.heroTag, { color: colors.textSecondary }]}>
              Alpha build · Experimental
            </Text>
          </View>

          <Text style={[styles.heroBody, { color: colors.textSecondary }]}>
            Cedal is a cyber‑native chat and work node. It blends chats, work
            lanes, calls, and fantasy spaces into one mesh so you can stay in
            flow instead of juggling apps.
          </Text>
        </View>

        {/* Version & build info */}
        <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>
          Build
        </Text>
        <View
          style={[
            styles.card,
            { borderColor: colors.border, backgroundColor: colors.background },
          ]}
        >
          <View style={styles.row}>
            <View style={styles.rowTextBlock}>
              <Text style={[styles.rowLabel, { color: colors.textPrimary }]}>
                Version
              </Text>
              <Text
                style={[
                  styles.rowDescription,
                  { color: colors.textSecondary },
                ]}
              >
                0.1.0 (alpha channel)
              </Text>
            </View>
          </View>
          <View style={styles.row}>
            <View style={styles.rowTextBlock}>
              <Text style={[styles.rowLabel, { color: colors.textPrimary }]}>
                Mesh ID
              </Text>
              <Text
                style={[
                  styles.rowDescription,
                  { color: colors.textSecondary },
                ]}
              >
                Local dev mesh · not yet federated
              </Text>
            </View>
          </View>
          <View style={[styles.row, styles.rowLast]}>
            <View style={styles.rowTextBlock}>
              <Text style={[styles.rowLabel, { color: colors.textPrimary }]}>
                Build channel
              </Text>
              <Text
                style={[
                  styles.rowDescription,
                  { color: colors.textSecondary },
                ]}
              >
                Internal preview · subject to breaking changes
              </Text>
            </View>
          </View>
        </View>

        {/* Credits */}
        <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>
          Credits
        </Text>
        <View
          style={[
            styles.card,
            { borderColor: colors.border, backgroundColor: colors.background },
          ]}
        >
          <View style={styles.row}>
            <View style={styles.rowTextBlock}>
              <Text style={[styles.rowLabel, { color: colors.textPrimary }]}>
                Crafted by
              </Text>
              <Text
                style={[
                  styles.rowDescription,
                  { color: colors.textSecondary },
                ]}
              >
                Solo node operator, custom mesh client, cyber‑night UI.
              </Text>
            </View>
          </View>
          <View style={[styles.row, styles.rowLast]}>
            <View style={styles.rowTextBlock}>
              <Text style={[styles.rowLabel, { color: colors.textPrimary }]}>
                Stack
              </Text>
              <Text
                style={[
                  styles.rowDescription,
                  { color: colors.textSecondary },
                ]}
              >
                Expo, React Native, Expo Router, Firebase, and AI‑powered
                assistance.
              </Text>
            </View>
          </View>
        </View>

        {/* Legal / small print */}
        <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>
          Legal
        </Text>
        <View
          style={[
            styles.card,
            { borderColor: colors.border, backgroundColor: colors.background },
          ]}
        >
          <View style={[styles.row, styles.rowLast]}>
            <View style={styles.rowTextBlock}>
              <Text
                style={[
                  styles.rowDescription,
                  { color: colors.textSecondary },
                ]}
              >
                This build is experimental. Do not store sensitive production
                data. Features, visuals, and behavior may change without notice.
              </Text>
            </View>
          </View>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
  },
  topBar: {
    paddingTop: 48,
    paddingHorizontal: 16,
    paddingBottom: 12,
    flexDirection: "row",
    alignItems: "center",
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  backBtn: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
    marginRight: 12,
  },
  backText: {
    fontSize: 13,
    letterSpacing: 1.5,
  },
  topTitle: {
    fontSize: 16,
    fontWeight: "600",
    letterSpacing: 2,
    textTransform: "uppercase",
  },
  content: {
    paddingHorizontal: 16,
    paddingVertical: 16,
    paddingBottom: 32,
  },
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
    paddingHorizontal: 14,
    paddingVertical: 12,
  },
  heroTop: {
    marginBottom: 6,
  },
  heroTitle: {
    fontSize: 16,
    fontWeight: "600",
  },
  heroTag: {
    fontSize: 11,
  },
  heroBody: {
    fontSize: 12,
    marginTop: 4,
  },
  row: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 8,
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
});
