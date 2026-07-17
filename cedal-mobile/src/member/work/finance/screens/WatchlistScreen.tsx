// src/member/work/finance/screens/WatchlistScreen.tsx
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
  onOpenAsset: (symbol: string) => void;
};

type WatchItem = {
  symbol: string;
  name: string;
  price: number;
  changePct: number;
  market: string;
};

export default function WatchlistScreen({ onBack, onOpenAsset }: Props) {
  const { colors } = useTheme();

  // later: replace with real Firestore watchlist
  const items: WatchItem[] = [
    {
      symbol: "AAPL",
      name: "Apple Inc.",
      price: 221.34,
      changePct: -0.8,
      market: "NASDAQ",
    },
    {
      symbol: "NVDA",
      name: "NVIDIA Corp.",
      price: 932.1,
      changePct: 3.2,
      market: "NASDAQ",
    },
    {
      symbol: "MSFT",
      name: "Microsoft Corp.",
      price: 421.88,
      changePct: 1.1,
      market: "NASDAQ",
    },
    {
      symbol: "TSLA",
      name: "Tesla Inc.",
      price: 188.42,
      changePct: -2.4,
      market: "NASDAQ",
    },
  ];

  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      {/* Header with back + title */}
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
            Watchlist
          </Text>
          <Text
            style={[styles.subTitle, { color: colors.textSecondary }]}
          >
            Assets you’re tracking across sessions.
          </Text>
        </View>
      </View>

      {/* Filters row (static for now) */}
      <View style={styles.filtersRow}>
        <FilterChip label="All" active />
        <FilterChip label="Stocks" />
        <FilterChip label="Crypto" />
        <FilterChip label="Funds" />
      </View>

      {/* List */}
      <ScrollView
        style={styles.list}
        contentContainerStyle={styles.listContent}
      >
        {items.map((item) => (
          <TouchableOpacity
            key={item.symbol}
            style={styles.row}
            activeOpacity={0.8}
            onPress={() => onOpenAsset(item.symbol)}
          >
            <View style={styles.left}>
              <View style={styles.symbolRow}>
                <Text style={[styles.symbol, { color: colors.textPrimary }]}>
                  {item.symbol}
                </Text>
                <View style={styles.marketPill}>
                  <Text style={styles.marketPillText}>{item.market}</Text>
                </View>
              </View>
              <Text style={[styles.name, { color: colors.textSecondary }]}>
                {item.name}
              </Text>
            </View>

            <View style={styles.right}>
              <Text style={[styles.price, { color: colors.textPrimary }]}>
                ${item.price.toFixed(2)}
              </Text>
              <Text
                style={[
                  styles.change,
                  {
                    color:
                      item.changePct >= 0 ? "#22c55e" : "#ef4444",
                  },
                ]}
              >
                {item.changePct >= 0 ? "+" : "-"}
                {Math.abs(item.changePct).toFixed(2)}%
              </Text>
            </View>
          </TouchableOpacity>
        ))}
      </ScrollView>
    </View>
  );
}

type FilterChipProps = {
  label: string;
  active?: boolean;
};

function FilterChip({ label, active }: FilterChipProps) {
  return (
    <View
      style={[
        styles.chip,
        active && styles.chipActive,
      ]}
    >
      <Text
        style={[
          styles.chipText,
          active && styles.chipTextActive,
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
  title: {
    fontSize: 18,
    fontWeight: "600",
  },
  subTitle: {
    fontSize: 12,
    marginTop: 2,
  },
  filtersRow: {
    flexDirection: "row",
    paddingHorizontal: 16,
    paddingBottom: 4,
    columnGap: 8,
  },
  chip: {
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: "#374151",
    backgroundColor: "#020617",
  },
  chipActive: {
    borderColor: "#60a5fa",
    backgroundColor: "rgba(37,99,235,0.25)",
  },
  chipText: {
    fontSize: 11,
    color: "#9ca3af",
  },
  chipTextActive: {
    color: "#e5e7eb",
    fontWeight: "500",
  },
  list: {
    flex: 1,
  },
  listContent: {
    paddingHorizontal: 16,
    paddingBottom: 8,
  },
  row: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "#1f2933",
  },
  left: {
    flexShrink: 1,
    paddingRight: 8,
  },
  symbolRow: {
    flexDirection: "row",
    alignItems: "center",
    columnGap: 6,
  },
  symbol: {
    fontSize: 16,
    fontWeight: "600",
  },
  marketPill: {
    paddingHorizontal: 6,
    paddingVertical: 1,
    borderRadius: 999,
    backgroundColor: "#111827",
  },
  marketPillText: {
    fontSize: 9,
    color: "#9ca3af",
    textTransform: "uppercase",
  },
  name: {
    fontSize: 12,
    marginTop: 2,
  },
  right: {
    alignItems: "flex-end",
  },
  price: {
    fontSize: 14,
    fontWeight: "500",
  },
  change: {
    fontSize: 11,
    marginTop: 2,
  },
});
