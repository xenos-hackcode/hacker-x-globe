// app/(auth)/(member)/shop.tsx
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

type VipInfo = {
  level: number;
  exp: number;
  nextLevelExp: number;
};

// £1 = 100 exp, VIP n = n * 100 exp
function computeVip(exp: number): VipInfo {
  const level = Math.floor(exp / 100); // 0,1,2,...
  const nextLevelExp = (level + 1) * 100; // 100, 200, 300,...
  return { level, exp, nextLevelExp };
}

export default function ShopScreen() {
  const router = useRouter();
  const { colors } = useTheme();

  // later you’ll pull this from user billing history
  const totalSpentGBP = 0; // e.g. 12.34
  const vipExp = Math.floor(totalSpentGBP * 100); // £1 = 100 exp
  const vip = computeVip(vipExp);

  const progress =
    vip.nextLevelExp > 0
      ? Math.min(1, vip.exp / vip.nextLevelExp)
      : 1;

  function handleBack() {
    router.back();
  }

  function handleSubscribe(plan: "basic" | "pro" | "god") {
    // TODO: hook into your billing / Stripe etc.
  }

  function handleAddPaymentMethod() {
    // TODO: open payment method / card details flow
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
          Shop
        </Text>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        {/* VIP card */}
        <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>
          VIP status
        </Text>
        <View
          style={[
            styles.card,
            { borderColor: colors.border, backgroundColor: colors.background },
          ]}
        >
          <Text style={[styles.vipLabel, { color: colors.textSecondary }]}>
            Total spent: £{totalSpentGBP.toFixed(2)}
          </Text>
          <Text style={[styles.vipTitle, { color: colors.textPrimary }]}>
            VIP {vip.level || 0}
          </Text>
          <Text style={[styles.vipSub, { color: colors.textSecondary }]}>
            {vip.level === 0
              ? "Spend £1 to reach VIP 1 and start climbing."
              : `You have ${vip.exp} exp. Next level at ${vip.nextLevelExp} exp.`}
          </Text>

          {/* Progress bar */}
          <View style={styles.progressTrack}>
            <View
              style={[
                styles.progressFill,
                { width: `${Math.round(progress * 100)}%` },
              ]}
            />
          </View>

          <Text style={[styles.vipHint, { color: colors.textSecondary }]}>
            Every £1 you spend in Cedal gives you 100 VIP exp. Higher VIP tiers
            will unlock cosmetics and perks later.
          </Text>

          <TouchableOpacity
            style={styles.primaryButton}
            activeOpacity={0.8}
            onPress={handleAddPaymentMethod}
          >
            <Text style={styles.primaryButtonText}>Add payment method</Text>
          </TouchableOpacity>
        </View>

        {/* Subscriptions */}
        <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>
          Subscriptions
        </Text>
        <View
          style={[
            styles.card,
            { borderColor: colors.border, backgroundColor: colors.background },
          ]}
        >
          <PlanRow
            name="Cedal Overlord"
            price="£3 / month"
            blurb="Unlock extended history and higher message limits."
            onPress={() => handleSubscribe("basic")}
          />
          <PlanRow
            name="Cedal Celestial"
            price="£8 / month"
            blurb="Adds priority routing, more storage, and early feature flags."
            onPress={() => handleSubscribe("pro")}
          />
          <PlanRow
            name="Cedal Void"
            price="£20 / month"
            blurb="Maxed‑out limits, cosmetic packs, and highest VIP acceleration."
            last
            onPress={() => handleSubscribe("god")}
          />
        </View>

        {/* Small print */}
        <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>
          Notes
        </Text>
        <View
          style={[
            styles.card,
            { borderColor: colors.border, backgroundColor: colors.background },
          ]}
        >
          <Text style={[styles.notes, { color: colors.textSecondary }]}>
            VIP is purely progression and perks. It does not make you above the
            rules. Refunds or chargebacks may reduce your VIP exp.
          </Text>
        </View>
      </ScrollView>
    </View>
  );
}

function PlanRow({
  name,
  price,
  blurb,
  last,
  onPress,
}: {
  name: string;
  price: string;
  blurb: string;
  last?: boolean;
  onPress: () => void;
}) {
  return (
    <View style={[styles.planRow, last && styles.planRowLast]}>
      <View style={styles.planTextBlock}>
        <Text style={styles.planName}>{name}</Text>
        <Text style={styles.planPrice}>{price}</Text>
        <Text style={styles.planBlurb}>{blurb}</Text>
        <Text style={styles.planVipNote}>
          Subscriptions give VIP exp at 50% rate (every £1 = 50 exp).
        </Text>
      </View>
      <TouchableOpacity
        style={styles.planBuyButton}
        activeOpacity={0.8}
        onPress={onPress}
      >
        <Text style={styles.planBuyText}>Buy</Text>
      </TouchableOpacity>
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
  vipLabel: {
    fontSize: 11,
    marginBottom: 4,
  },
  vipTitle: {
    fontSize: 18,
    fontWeight: "600",
    marginBottom: 2,
  },
  vipSub: {
    fontSize: 12,
    marginBottom: 8,
  },
  progressTrack: {
    height: 8,
    borderRadius: 999,
    backgroundColor: "rgba(31,41,55,0.9)",
    overflow: "hidden",
    marginBottom: 6,
  },
  progressFill: {
    height: "100%",
    borderRadius: 999,
    backgroundColor: "#22c55e",
  },
  vipHint: {
    fontSize: 11,
    marginTop: 4,
  },
  primaryButton: {
    marginTop: 10,
    paddingVertical: 10,
    borderRadius: 999,
    backgroundColor: "#22c55e",
    alignItems: "center",
  },
  primaryButtonText: {
    color: "#020617",
    fontSize: 13,
    fontWeight: "600",
  },
  planRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(31,41,55,0.9)",
  },
  planRowLast: {
    borderBottomWidth: 0,
  },
  planTextBlock: {
    flex: 1,
    marginRight: 8,
  },
  planName: {
    fontSize: 13,
    fontWeight: "500",
    color: "#e5e7eb",
  },
  planPrice: {
    fontSize: 12,
    color: "#a5b4fc",
    marginTop: 2,
  },
  planBlurb: {
    fontSize: 11,
    color: "#9ca3af",
    marginTop: 2,
  },
  planVipNote: {
    fontSize: 10,
    color: "#9ca3af",
    marginTop: 4,
  },
  planBuyButton: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.8)",
  },
  planBuyText: {
    fontSize: 12,
    fontWeight: "600",
    color: "#e5e7eb",
  },
  planChevron: {
    fontSize: 18,
    color: "#6b7280",
  },
  notes: {
    fontSize: 11,
  },
});
