// src/member/work/finance/screens/OverviewScreen.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import React from "react";
import {
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

type Props = {
  onOpenWatchlist: () => void;
  onOpenAsset: (symbol: string) => void;
};

export default function OverviewScreen({ onOpenWatchlist, onOpenAsset }: Props) {
  const { colors } = useTheme();

  // later: pull this from Firestore / financeApi
  const totalValue = 12500.32;
  const dayChange = 245.87;
  const dayChangePct = 2.01;
  const bestMover = { symbol: "NVDA", changePct: 4.3 };
  const worstMover = { symbol: "AAPL", changePct: -1.2 };

  const watchlist = [
    { symbol: "AAPL", price: 221.34, changePct: -0.8 },
    { symbol: "NVDA", price: 932.1, changePct: 3.2 },
    { symbol: "MSFT", price: 421.88, changePct: 1.1 },
  ];

  return (
    <ScrollView
      style={[styles.root, { backgroundColor: colors.background }]}
      contentContainerStyle={styles.scrollContent}
    >
      {/* Header copy */}
      <View style={styles.header}>
        <Text style={[styles.title, { color: colors.textPrimary }]}>
          Portfolio overview
        </Text>
        <Text style={[styles.subTitle, { color: colors.textSecondary }]}>
          Snapshot of your positions, movers, and watchlist.
        </Text>
      </View>

      {/* Portfolio card */}
      <View style={[styles.card, { borderColor: colors.border }]}>
        <Text style={[styles.cardLabel, { color: colors.textSecondary }]}>
          Total value
        </Text>
        <Text style={[styles.value, { color: colors.textPrimary }]}>
          ${totalValue.toLocaleString()}
        </Text>
        <Text
          style={[
            styles.change,
            { color: dayChange >= 0 ? "#22c55e" : "#ef4444" },
          ]}
        >
          {dayChange >= 0 ? "+" : "-"}${Math.abs(dayChange).toFixed(2)} (
          {dayChange >= 0 ? "+" : "-"}
          {Math.abs(dayChangePct).toFixed(2)}%) today
        </Text>
      </View>

      {/* Movers row */}
      <View style={styles.row}>
        <View
          style={[
            styles.card,
            styles.rowCard,
            { borderColor: colors.border },
          ]}
        >
          <Text style={[styles.cardLabel, { color: colors.textSecondary }]}>
            Best mover
          </Text>
          <TouchableOpacity onPress={() => onOpenAsset(bestMover.symbol)}>
            <Text style={[styles.symbol, { color: colors.textPrimary }]}>
              {bestMover.symbol}
            </Text>
          </TouchableOpacity>
          <Text
            style={[
              styles.moverChange,
              { color: bestMover.changePct >= 0 ? "#22c55e" : "#ef4444" },
            ]}
          >
            {bestMover.changePct >= 0 ? "+" : "-"}
            {Math.abs(bestMover.changePct).toFixed(2)}%
          </Text>
        </View>

        <View
          style={[
            styles.card,
            styles.rowCard,
            { borderColor: colors.border },
          ]}
        >
          <Text style={[styles.cardLabel, { color: colors.textSecondary }]}>
            Worst mover
          </Text>
          <TouchableOpacity onPress={() => onOpenAsset(worstMover.symbol)}>
            <Text style={[styles.symbol, { color: colors.textPrimary }]}>
              {worstMover.symbol}
            </Text>
          </TouchableOpacity>
          <Text
            style={[
              styles.moverChange,
              { color: worstMover.changePct >= 0 ? "#22c55e" : "#ef4444" },
            ]}
          >
            {worstMover.changePct >= 0 ? "+" : "-"}
            {Math.abs(worstMover.changePct).toFixed(2)}%
          </Text>
        </View>
      </View>

      {/* Watchlist preview */}
      <View style={[styles.card, { borderColor: colors.border }]}>
        <View style={styles.watchlistHeader}>
          <Text style={[styles.cardLabel, { color: colors.textSecondary }]}>
            Watchlist
          </Text>
          <TouchableOpacity onPress={onOpenWatchlist}>
            <Text style={[styles.link, { color: "#60a5fa" }]}>View all</Text>
          </TouchableOpacity>
        </View>

        {watchlist.map((item) => (
          <TouchableOpacity
            key={item.symbol}
            style={styles.watchlistRow}
            onPress={() => onOpenAsset(item.symbol)}
          >
            <View>
              <Text style={[styles.symbol, { color: colors.textPrimary }]}>
                {item.symbol}
              </Text>
            </View>
            <View style={styles.watchlistRight}>
              <Text style={[styles.price, { color: colors.textPrimary }]}>
                ${item.price.toFixed(2)}
              </Text>
              <Text
                style={[
                  styles.rowChange,
                  { color: item.changePct >= 0 ? "#22c55e" : "#ef4444" },
                ]}
              >
                {item.changePct >= 0 ? "+" : "-"}
                {Math.abs(item.changePct).toFixed(2)}%
              </Text>
            </View>
          </TouchableOpacity>
        ))}
      </View>

      {/* later: add "Goals" or "Insights" card */}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
  },
  scrollContent: {
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 8, // tighter bottom so it meets the bar nicely
    gap: 12,
  },
  header: {
    marginBottom: 4,
  },
  title: {
    fontSize: 18,
    fontWeight: "600",
  },
  subTitle: {
    fontSize: 12,
    marginTop: 2,
  },
  card: {
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 14,
    padding: 12,
    backgroundColor: "#020617",
  },
  cardLabel: {
    fontSize: 11,
    textTransform: "uppercase",
    letterSpacing: 1,
    marginBottom: 4,
  },
  value: {
    fontSize: 22,
    fontWeight: "700",
  },
  change: {
    fontSize: 12,
    marginTop: 4,
  },
  row: {
    flexDirection: "row",
    columnGap: 10,
  },
  rowCard: {
    flex: 1,
  },
  symbol: {
    fontSize: 16,
    fontWeight: "600",
  },
  moverChange: {
    fontSize: 13,
    marginTop: 2,
  },
  watchlistHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 4,
  },
  link: {
    fontSize: 12,
    fontWeight: "500",
  },
  watchlistRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingVertical: 6,
  },
  watchlistRight: {
    alignItems: "flex-end",
  },
  price: {
    fontSize: 13,
  },
  rowChange: {
    fontSize: 11,
    marginTop: 2,
  },
});
