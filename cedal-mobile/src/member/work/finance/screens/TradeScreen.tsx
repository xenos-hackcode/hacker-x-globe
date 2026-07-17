// src/member/work/finance/screens/TradeScreen.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import React, { useState } from "react";
import {
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    View,
} from "react-native";

type Props = {
  onBack: () => void;
  onOpenAsset: (symbol: string) => void;
};

type OrderSide = "buy" | "sell";
type OrderType = "market" | "limit";

export default function TradeScreen({ onBack, onOpenAsset }: Props) {
  const { colors } = useTheme();

  const [symbol, setSymbol] = useState("AAPL");
  const [side, setSide] = useState<OrderSide>("buy");
  const [type, setType] = useState<OrderType>("market");
  const [qty, setQty] = useState("1");
  const [limitPrice, setLimitPrice] = useState("220.00");

  // later: real recent orders from Firestore
  const recentOrders = [
    {
      id: "1",
      symbol: "AAPL",
      side: "buy" as OrderSide,
      qty: 2,
      price: 219.5,
      status: "Filled",
    },
    {
      id: "2",
      symbol: "NVDA",
      side: "sell" as OrderSide,
      qty: 1,
      price: 930.0,
      status: "Working",
    },
  ];

  const handleSubmit = () => {
    // TODO: validate + send order to backend
    console.log({ symbol, side, type, qty, limitPrice });
  };

  const activeSideColor = side === "buy" ? "#22c55e" : "#ef4444";

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
            Trade
          </Text>
          <Text
            style={[styles.subTitle, { color: colors.textSecondary }]}
          >
            Place quick orders on symbols you’re tracking.
          </Text>
        </View>
      </View>

      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
      >
        {/* Symbol row */}
        <View style={[styles.card, { borderColor: colors.border }]}>
          <Text style={[styles.cardLabel, { color: colors.textSecondary }]}>
            Symbol
          </Text>
          <View style={styles.symbolRow}>
            <TextInput
              value={symbol}
              onChangeText={setSymbol}
              placeholder="Ticker"
              placeholderTextColor={colors.textSecondary}
              style={[
                styles.symbolInput,
                { color: colors.textPrimary, borderColor: colors.border },
              ]}
              autoCapitalize="characters"
            />
            <TouchableOpacity
              style={styles.symbolGo}
              onPress={() => onOpenAsset(symbol.trim())}
            >
              <Text style={styles.symbolGoText}>Open</Text>
            </TouchableOpacity>
          </View>
        </View>

        {/* Side + type */}
        <View style={styles.row}>
          <View
            style={[styles.card, styles.rowCard, { borderColor: colors.border }]}
          >
            <Text style={[styles.cardLabel, { color: colors.textSecondary }]}>
              Side
            </Text>
            <View style={styles.toggleRow}>
              <ToggleChip
                label="Buy"
                active={side === "buy"}
                activeColor="#22c55e"
                onPress={() => setSide("buy")}
              />
              <ToggleChip
                label="Sell"
                active={side === "sell"}
                activeColor="#ef4444"
                onPress={() => setSide("sell")}
              />
            </View>
          </View>

          <View
            style={[styles.card, styles.rowCard, { borderColor: colors.border }]}
          >
            <Text style={[styles.cardLabel, { color: colors.textSecondary }]}>
              Type
            </Text>
            <View style={styles.toggleRow}>
              <ToggleChip
                label="Market"
                active={type === "market"}
                activeColor="#60a5fa"
                onPress={() => setType("market")}
              />
              <ToggleChip
                label="Limit"
                active={type === "limit"}
                activeColor="#a855f7"
                onPress={() => setType("limit")}
              />
            </View>
          </View>
        </View>

        {/* Quantity + limit price */}
        <View style={styles.row}>
          <View
            style={[styles.card, styles.rowCard, { borderColor: colors.border }]}
          >
            <Text style={[styles.cardLabel, { color: colors.textSecondary }]}>
              Quantity
            </Text>
            <TextInput
              value={qty}
              onChangeText={setQty}
              keyboardType="numeric"
              style={[
                styles.input,
                { color: colors.textPrimary, borderColor: colors.border },
              ]}
            />
          </View>

          {type === "limit" && (
            <View
              style={[
                styles.card,
                styles.rowCard,
                { borderColor: colors.border },
              ]}
            >
              <Text
                style={[styles.cardLabel, { color: colors.textSecondary }]}
              >
                Limit price
              </Text>
              <TextInput
                value={limitPrice}
                onChangeText={setLimitPrice}
                keyboardType="decimal-pad"
                style={[
                  styles.input,
                  { color: colors.textPrimary, borderColor: colors.border },
                ]}
              />
            </View>
          )}
        </View>

        {/* Submit button */}
        <TouchableOpacity
          style={[
            styles.submitButton,
            { backgroundColor: activeSideColor },
          ]}
          activeOpacity={0.8}
          onPress={handleSubmit}
        >
          <Text style={styles.submitText}>
            {side === "buy" ? "Buy" : "Sell"} {qty || "0"} {symbol || ""}
          </Text>
        </TouchableOpacity>

        {/* Recent orders */}
        <View style={[styles.card, { borderColor: colors.border }]}>
          <View style={styles.ordersHeader}>
            <Text style={[styles.cardLabel, { color: colors.textSecondary }]}>
              Recent orders
            </Text>
          </View>

          {recentOrders.map((o) => (
            <View key={o.id} style={styles.orderRow}>
              <View style={styles.orderLeft}>
                <Text
                  style={[
                    styles.orderSymbol,
                    { color: colors.textPrimary },
                  ]}
                >
                  {o.symbol}
                </Text>
                <Text
                  style={[
                    styles.orderMeta,
                    { color: colors.textSecondary },
                  ]}
                >
                  {o.side.toUpperCase()} · {o.qty} @ ${o.price.toFixed(2)}
                </Text>
              </View>
              <Text
                style={[
                  styles.orderStatus,
                  {
                    color:
                      o.status === "Filled"
                        ? "#22c55e"
                        : o.status === "Working"
                        ? "#eab308"
                        : colors.textSecondary,
                  },
                ]}
              >
                {o.status}
              </Text>
            </View>
          ))}
        </View>
      </ScrollView>
    </View>
  );
}

type ToggleChipProps = {
  label: string;
  active?: boolean;
  activeColor: string;
  onPress: () => void;
};

function ToggleChip({ label, active, activeColor, onPress }: ToggleChipProps) {
  return (
    <TouchableOpacity
      onPress={onPress}
      activeOpacity={0.8}
      style={[
        styles.toggleChip,
        active && {
          backgroundColor: activeColor,
          borderColor: activeColor,
        },
      ]}
    >
      <Text
        style={[
          styles.toggleChipText,
          active && { color: "#0b1120" },
        ]}
      >
        {label}
      </Text>
    </TouchableOpacity>
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
  symbolRow: {
    flexDirection: "row",
    columnGap: 8,
    alignItems: "center",
  },
  symbolInput: {
    flex: 1,
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 6,
    fontSize: 14,
  },
  symbolGo: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "#111827",
  },
  symbolGoText: {
    fontSize: 12,
    color: "#e5e7eb",
  },
  row: {
    flexDirection: "row",
    columnGap: 10,
  },
  rowCard: {
    flex: 1,
  },
  toggleRow: {
    flexDirection: "row",
    columnGap: 8,
    marginTop: 2,
  },
  toggleChip: {
    flex: 1,
    borderRadius: 999,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: "#374151",
    paddingVertical: 6,
    alignItems: "center",
  },
  toggleChipText: {
    fontSize: 12,
    color: "#e5e7eb",
  },
  input: {
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 10,
    paddingHorizontal: 10,
    paddingVertical: 6,
    fontSize: 14,
  },
  submitButton: {
    marginTop: 4,
    borderRadius: 999,
    paddingVertical: 10,
    alignItems: "center",
  },
  submitText: {
    fontSize: 14,
    fontWeight: "600",
    color: "#0b1120",
  },
  ordersHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 4,
  },
  orderRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingVertical: 6,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: "#111827",
  },
  orderLeft: {
    flexShrink: 1,
    paddingRight: 8,
  },
  orderSymbol: {
    fontSize: 14,
    fontWeight: "600",
  },
  orderMeta: {
    fontSize: 11,
    marginTop: 1,
  },
  orderStatus: {
    fontSize: 11,
    fontWeight: "500",
  },
});
