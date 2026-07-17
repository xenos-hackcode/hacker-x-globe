// src/member/work/banking/screens/DebtScreen.tsx
import { useUserProfile } from "@/src/hooks/useUserProfile";
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

const MIN_DEBT = -30;

type Props = {
  onBackHome: () => void;
};

export default function DebtScreen({ onBackHome }: Props) {
  const { colors } = useTheme();
  const { profile } = useUserProfile();

  const balance = profile?.scBalance ?? 0;
  const maxDebt = Math.abs(MIN_DEBT);

  // Progress + color logic
  let progress = 0;
  let barColor = "#3b82f6"; // blue default for zero

  if (balance > 0) {
    // Positive: green, scaled vs maxDebt
    progress = Math.min(balance / maxDebt, 1);
    barColor = "#22c55e";
  } else if (balance < 0) {
    // Negative: red, scaled vs maxDebt depth
    const debt = Math.abs(balance);
    progress = Math.min(debt / maxDebt, 1);
    barColor = "#ef4444";
  } else {
    // Exactly zero: blue at 50% width (adjust if you want)
    progress = 0.5;
    barColor = "#3b82f6";
  }

  const isInDebt = balance < 0;
  const debt = isInDebt ? Math.abs(balance) : 0;
  const remainingRoom = isInDebt ? maxDebt - debt : maxDebt;

  return (
    <ScrollView
      style={[styles.root, { backgroundColor: colors.background }]}
      contentContainerStyle={styles.scrollContent}
    >
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity style={styles.backBtn} onPress={onBackHome}>
          <Ionicons name="chevron-back" size={18} color="#60a5fa" />
          <Text style={[styles.backText, { color: "#60a5fa" }]}>Back</Text>
        </TouchableOpacity>

        <View style={styles.headerTextBlock}>
          <Text style={[styles.title, { color: colors.textPrimary }]}>
            Debt overview
          </Text>
          <Text style={[styles.subTitle, { color: colors.textSecondary }]}>
            See how much Star Coin debt you have and your limit.
          </Text>
        </View>
      </View>

      {/* Debt summary card */}
      <View style={[styles.card, { borderColor: colors.border }]}>
        <Text style={[styles.cardLabel, { color: colors.textSecondary }]}>
          Current balance
        </Text>
        <Text
          style={[
            styles.balanceText,
            { color: balance < 0 ? "#ef4444" : "#22c55e" },
          ]}
        >
          {balance.toLocaleString("en-GB")} SC
        </Text>

        <View style={styles.row}>
          <Text style={[styles.caption, { color: colors.textSecondary }]}>
            Maximum allowed debt
          </Text>
          <Text style={[styles.captionValue, { color: colors.textPrimary }]}>
            -{maxDebt} SC
          </Text>
        </View>

        <View style={styles.row}>
          <Text style={[styles.caption, { color: colors.textSecondary }]}>
            Remaining before limit
          </Text>
          <Text style={[styles.captionValue, { color: colors.textPrimary }]}>
            {remainingRoom.toLocaleString("en-GB")} SC
          </Text>
        </View>

        {/* Progress bar */}
        <View style={styles.progressBarOuter}>
          <View
            style={[
              styles.progressBarInner,
              {
                width: `${progress * 100}%`,
                backgroundColor: barColor,
              },
            ]}
          />
        </View>

        <Text style={[styles.helper, { color: colors.textSecondary }]}>
          You can go down to a maximum of -{maxDebt} SC. Spending from 0 will
          put you into debt, but you cannot go past this limit.
        </Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  scrollContent: {
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 24,
    gap: 16,
  },
  header: {
    marginBottom: 8,
    flexDirection: "row",
    alignItems: "center",
  },
  backBtn: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    paddingRight: 8,
    paddingVertical: 4,
  },
  backText: {
    fontSize: 13,
    fontWeight: "500",
  },
  headerTextBlock: {
    flex: 1,
  },
  title: { fontSize: 18, fontWeight: "600" },
  subTitle: { fontSize: 12, marginTop: 2 },
  card: {
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 14,
    padding: 16,
    backgroundColor: "#020617",
  },
  cardLabel: {
    fontSize: 11,
    textTransform: "uppercase",
    letterSpacing: 1,
    marginBottom: 6,
  },
  balanceText: {
    fontSize: 28,
    fontWeight: "800",
    marginBottom: 8,
  },
  row: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 4,
  },
  caption: {
    fontSize: 12,
  },
  captionValue: {
    fontSize: 12,
    fontWeight: "600",
  },
  progressBarOuter: {
    marginTop: 10,
    height: 8,
    borderRadius: 999,
    backgroundColor: "#111827",
    overflow: "hidden",
  },
  progressBarInner: {
    height: "100%",
    borderRadius: 999,
  },
  helper: {
    fontSize: 12,
    marginTop: 8,
  },
});
