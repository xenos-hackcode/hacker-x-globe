// src/member/work/finance/screens/AssetScreen.tsx
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
  symbol: string;
  onBack: () => void;
};

export default function AssetScreen({ symbol, onBack }: Props) {
  const { colors } = useTheme();

  // later: fetch real quote + stats for symbol
  const name = symbol === "AAPL" ? "Apple Inc." : `${symbol} Corp.`;
  const price = 221.34;
  const change = -1.23;
  const changePct = -0.55;
  const dayHigh = 225.1;
  const dayLow = 219.8;
  const volume = "54.2M";
  const marketCap = "3.4T";

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
            Back
          </Text>
        </TouchableOpacity>

        <View>
          <Text style={[styles.symbol, { color: colors.textPrimary }]}>
            {symbol}
          </Text>
          <Text style={[styles.name, { color: colors.textSecondary }]}>
            {name}
          </Text>
        </View>
      </View>

      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.scrollContent}
      >
        {/* Price block */}
        <View style={[styles.card, { borderColor: colors.border }]}>
          <Text style={[styles.price, { color: colors.textPrimary }]}>
            ${price.toFixed(2)}
          </Text>
          <Text
            style={[
              styles.change,
              { color: change >= 0 ? "#22c55e" : "#ef4444" },
            ]}
          >
            {change >= 0 ? "+" : "-"}${Math.abs(change).toFixed(2)} (
            {changePct >= 0 ? "+" : "-"}
            {Math.abs(changePct).toFixed(2)}%) today
          </Text>
        </View>

        {/* Chart placeholder */}
        <View style={[styles.card, { borderColor: colors.border }]}>
          <View style={styles.chartHeader}>
            <Text
              style={[styles.sectionLabel, { color: colors.textSecondary }]}
            >
              Price action
            </Text>
            <View style={styles.chartRangeRow}>
              <RangeChip label="1D" active />
              <RangeChip label="1W" />
              <RangeChip label="1M" />
              <RangeChip label="1Y" />
            </View>
          </View>
          <View style={styles.chartPlaceholder}>
            <Text style={[styles.chartPlaceholderText, { color: colors.textSecondary }]}>
              Mini chart coming soon
            </Text>
          </View>
        </View>

        {/* Key stats */}
        <View style={[styles.card, { borderColor: colors.border }]}>
          <Text
            style={[styles.sectionLabel, { color: colors.textSecondary }]}
          >
            Today’s range
          </Text>
          <View style={styles.statRow}>
            <Text style={[styles.statLabel, { color: colors.textSecondary }]}>
              Low
            </Text>
            <Text style={[styles.statValue, { color: colors.textPrimary }]}>
              ${dayLow.toFixed(2)}
            </Text>
            <View style={styles.rangeBarTrack}>
              <View style={styles.rangeBarFill} />
            </View>
            <Text style={[styles.statLabel, { color: colors.textSecondary }]}>
              High
            </Text>
            <Text style={[styles.statValue, { color: colors.textPrimary }]}>
              ${dayHigh.toFixed(2)}
            </Text>
          </View>

          <View style={styles.statsGrid}>
            <StatItem label="Volume" value={volume} />
            <StatItem label="Market cap" value={marketCap} />
          </View>
        </View>

        {/* Actions */}
        <View style={styles.actionsRow}>
          <TouchableOpacity
            style={[styles.actionButton, { backgroundColor: "#22c55e" }]}
            activeOpacity={0.8}
          >
            <Ionicons name="add-outline" size={16} color="#0b1120" />
            <Text style={styles.actionText}>Add to watchlist</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.actionButton, { backgroundColor: "#1f2937" }]}
            activeOpacity={0.8}
          >
            <Ionicons name="swap-horizontal-outline" size={16} color="#e5e7eb" />
            <Text style={[styles.actionText, { color: "#e5e7eb" }]}>
              Trade
            </Text>
          </TouchableOpacity>
        </View>

        {/* later: news / notes / alerts */}
      </ScrollView>
    </View>
  );
}

type StatItemProps = { label: string; value: string };
function StatItem({ label, value }: StatItemProps) {
  return (
    <View style={styles.statItem}>
      <Text style={styles.statItemLabel}>{label}</Text>
      <Text style={styles.statItemValue}>{value}</Text>
    </View>
  );
}

type RangeChipProps = { label: string; active?: boolean };
function RangeChip({ label, active }: RangeChipProps) {
  return (
    <View
      style={[
        styles.rangeChip,
        active && { backgroundColor: "rgba(37,99,235,0.3)", borderColor: "#60a5fa" },
      ]}
    >
      <Text
        style={[
          styles.rangeChipText,
          active && { color: "#e5e7eb", fontWeight: "500" },
        ]}
      >
        {label}
      </Text>
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
  symbol: {
    fontSize: 18,
    fontWeight: "700",
  },
  name: {
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
  card: {
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 14,
    padding: 12,
    backgroundColor: "#020617",
  },
  price: {
    fontSize: 24,
    fontWeight: "700",
  },
  change: {
    fontSize: 12,
    marginTop: 4,
  },
  chartHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 6,
  },
  sectionLabel: {
    fontSize: 11,
    textTransform: "uppercase",
    letterSpacing: 1,
  },
  chartRangeRow: {
    flexDirection: "row",
    columnGap: 6,
  },
  chartPlaceholder: {
    height: 120,
    borderRadius: 10,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: "#1f2937",
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#020617",
  },
  chartPlaceholderText: {
    fontSize: 11,
  },
  statRow: {
    flexDirection: "row",
    alignItems: "center",
    columnGap: 6,
    marginTop: 6,
    marginBottom: 8,
  },
  statLabel: {
    fontSize: 11,
  },
  statValue: {
    fontSize: 12,
    fontWeight: "500",
  },
  rangeBarTrack: {
    flex: 1,
    height: 5,
    borderRadius: 999,
    backgroundColor: "#020617",
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: "#1f2937",
    overflow: "hidden",
  },
  rangeBarFill: {
    width: "50%", // placeholder position
    height: "100%",
    backgroundColor: "#22c55e",
  },
  statsGrid: {
    flexDirection: "row",
    columnGap: 12,
    marginTop: 4,
  },
  statItem: {
    flex: 1,
  },
  statItemLabel: {
    fontSize: 11,
    color: "#9ca3af",
  },
  statItemValue: {
    fontSize: 13,
    color: "#e5e7eb",
    marginTop: 2,
  },
  actionsRow: {
    flexDirection: "row",
    columnGap: 8,
    marginTop: 2,
  },
  actionButton: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "center",
    columnGap: 6,
    borderRadius: 999,
    paddingVertical: 8,
  },
  actionText: {
    fontSize: 13,
    fontWeight: "600",
    color: "#0b1120",
  },
  rangeChip: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 999,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: "#1f2937",
    backgroundColor: "#020617",
  },
  rangeChipText: {
    fontSize: 10,
    color: "#9ca3af",
  },
});
