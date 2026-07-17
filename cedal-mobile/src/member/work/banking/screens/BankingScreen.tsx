// src/member/work/banking/screens/BankingScreen.tsx
import { db } from "@/src/api/firebase";
import { useUserProfile } from "@/src/hooks/useUserProfile";
import { useBankSettings } from "@/src/member/work/banking/BankSettingsContext";
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";
import {
    collection,
    getDocs,
    limit,
    orderBy,
    query,
    where,
} from "firebase/firestore";
import React, { useEffect, useState } from "react";
import {
    ActivityIndicator,
    ScrollView,
    StyleSheet,
    Text,
    TouchableOpacity,
    View,
} from "react-native";
import Animated, {
    Easing,
    useAnimatedStyle,
    useSharedValue,
    withRepeat,
    withTiming,
} from "react-native-reanimated";

type Props = {
  onOpenTransactions: () => void;
  onOpenDebt: () => void;
  onOpenTrade: () => void;
  onOpenNotifications: () => void;
};

type RecentTx = {
  id: string;
  desc: string;
  amount: number; // + for in, - for out
  time: string;
};

export default function BankingScreen({
  onOpenTransactions,
  onOpenDebt,
  onOpenTrade,
  onOpenNotifications,
}: Props) {
  const { colors } = useTheme();
  const router = useRouter();
  const { profile, user } = useUserProfile();
  const { muteAllBankNotifications } = useBankSettings();
  const [buying, setBuying] = useState(false);
  const glow = useSharedValue(0);

  const [recent, setRecent] = useState<RecentTx[]>([]);
  const [loadingRecent, setLoadingRecent] = useState(true);
  const [hasBankNotifications, setHasBankNotifications] = useState(false);

  const scBalance = profile?.scBalance ?? 0;

  useEffect(() => {
    glow.value = withRepeat(
      withTiming(1, { duration: 2000, easing: Easing.inOut(Easing.ease) }),
      -1,
      true
    );
  }, []);

  // Load recent transactions for this user
  useEffect(() => {
    if (!user) {
      setRecent([]);
      setLoadingRecent(false);
      return;
    }

    let active = true;

    async function loadRecent() {
      try {
        setLoadingRecent(true);

        const q = query(
          collection(db, "walletTransactions"),
          where("userId", "==", user!.uid),
          orderBy("createdAt", "desc"),
          limit(3)
        );

        const snap = await getDocs(q);
        const items: RecentTx[] = snap.docs.map((docSnap) => {
          const data: any = docSnap.data();
          const ts: Date = data.createdAt?.toDate?.() ?? new Date();

          let desc = "Wallet activity";
          if (data.type === "topup") {
            desc = "Top‑up";
          } else if (data.type === "credit_out") {
            const name = data.toUserName || data.peerUserId || "Friend";
            desc = `To ${name}`;
          } else if (data.type === "credit_in") {
            const name = data.fromUserName || data.peerUserId || "Friend";
            desc = `From ${name}`;
          }

          return {
            id: docSnap.id,
            desc,
            amount: Number(data.amount ?? 0),
            time: ts.toLocaleTimeString("en-GB", {
              hour: "2-digit",
              minute: "2-digit",
            }),
          };
        });

        if (!active) return;
        setRecent(items);
      } catch (e) {
        if (!active) return;
        setRecent([]);
      } finally {
        if (active) setLoadingRecent(false);
      }
    }

    loadRecent();
    return () => {
      active = false;
    };
  }, [user?.uid]);

  // Check if there is at least one banking notification
  useEffect(() => {
    let active = true;

    async function checkBankNotifications() {
      try {
        if (!user) {
          if (active) setHasBankNotifications(false);
          return;
        }

        const q = query(
          collection(db, "notifications"),
          // Add filters if needed, e.g. target user or type:
          // where("targetUserId", "==", user.uid),
          // where("type", "==", "trade"),
          limit(1)
        );

        const snap = await getDocs(q);
        if (!active) return;

        setHasBankNotifications(!snap.empty);
      } catch (e) {
        if (!active) return;
        setHasBankNotifications(false);
      }
    }

    checkBankNotifications();
    return () => {
      active = false;
    };
  }, [user?.uid]);

  const animatedGlow = useAnimatedStyle(() => ({
    shadowColor: "#3b82f6",
    shadowOpacity: 0.6 + glow.value * 0.4,
    shadowRadius: 20 + glow.value * 10,
    shadowOffset: { width: 0, height: 0 },
  }));

  const showDrop = hasBankNotifications && !muteAllBankNotifications;

  return (
    <ScrollView
      style={[styles.root, { backgroundColor: colors.background }]}
      contentContainerStyle={styles.scrollContent}
    >
      {/* Header with back button + settings */}
      <View style={styles.header}>
        <TouchableOpacity
          style={styles.backBtn}
          onPress={() => router.back()}
        >
          <Ionicons name="chevron-back" size={18} color="#60a5fa" />
          <Text style={[styles.backText, { color: "#60a5fa" }]}>Back</Text>
        </TouchableOpacity>

        <View style={styles.headerTextBlock}>
          <Text style={[styles.title, { color: colors.textPrimary }]}>
            Banking Hub
          </Text>
          <Text style={[styles.subTitle, { color: colors.textSecondary }]}>
            Star Coins balance and transactions.
          </Text>
        </View>

        <TouchableOpacity
          style={{ padding: 6 }}
          onPress={() => {
            router.push("/(auth)/(member)/settings");
          }}
        >
          <Ionicons
            name="settings-outline"
            size={20}
            color={colors.textSecondary}
          />
        </TouchableOpacity>
      </View>

      {/* Hero Balance Card */}
      <Animated.View style={[styles.balanceCard, animatedGlow]}>
        <View style={styles.balanceHeader}>
          <Text style={[styles.balanceLabel, { color: colors.textSecondary }]}>
            Star Coins balance
          </Text>
          <TouchableOpacity
            style={styles.connectBtn}
            onPress={async () => {
              if (buying) return;
              try {
                setBuying(true);
              } catch (e) {
              } finally {
                setBuying(false);
              }
            }}
          >
            {buying ? (
              <ActivityIndicator size="small" color="#10b981" />
            ) : (
              <>
                <Text style={styles.connectText}>Buy 50 SC (£1)</Text>
                <Ionicons name="add-circle" size={16} color="#10b981" />
              </>
            )}
          </TouchableOpacity>
        </View>

        <Text style={styles.balanceAmount}>
          {scBalance.toLocaleString("en-GB")} SC
        </Text>
        <Text style={styles.balanceSub}>In‑game currency only</Text>
      </Animated.View>

      {/* Quick Actions */}
      <View style={styles.actionsRow}>
        <ActionTile
          icon="trending-down-outline"
          label="Debt"
          onPress={onOpenDebt}
        />
        <ActionTile
          icon="swap-horizontal"
          label="Trade"
          onPress={onOpenTrade}
        />
        <NotificationTile
          hasNotifications={showDrop}
          onPress={onOpenNotifications}
        />
      </View>

      {/* Recent Transactions */}
      <View style={[styles.card, { borderColor: colors.border }]}>
        <View style={styles.sectionHeader}>
          <Text style={[styles.cardLabel, { color: colors.textSecondary }]}>
            Recent Transactions
          </Text>
          <TouchableOpacity onPress={onOpenTransactions}>
            <Text style={[styles.link, { color: "#60a5fa" }]}>See all</Text>
          </TouchableOpacity>
        </View>

        {loadingRecent ? (
          <ActivityIndicator size="small" color="#60a5fa" />
        ) : recent.length === 0 ? (
          <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
            When you send, receive, or buy Star Coins, transactions will show here.
          </Text>
        ) : (
          recent.map((tx) => (
            <TransactionRow
              key={tx.id}
              desc={tx.desc}
              amount={tx.amount}
              time={tx.time}
              colors={colors}
            />
          ))
        )}
      </View>
    </ScrollView>
  );
}

function ActionTile({
  icon,
  label,
  onPress,
}: {
  icon: string;
  label: string;
  onPress: () => void;
}) {
  return (
    <TouchableOpacity style={styles.actionTile} onPress={onPress}>
      <Ionicons name={icon as any} size={24} color="#3b82f6" />
      <Text style={styles.actionLabel}>{label}</Text>
    </TouchableOpacity>
  );
}

function NotificationTile({
  hasNotifications,
  onPress,
}: {
  hasNotifications: boolean;
  onPress: () => void;
}) {
  const fill = useSharedValue(hasNotifications ? 1 : 0);

  React.useEffect(() => {
    fill.value = withTiming(hasNotifications ? 1 : 0, { duration: 600 });
  }, [hasNotifications]);

  const dropStyle = useAnimatedStyle(() => ({
    transform: [
      { translateY: (1 - fill.value) * 6 },
      { scale: 0.6 + fill.value * 0.4 },
    ],
    opacity: 0.2 + fill.value * 0.8,
  }));

  return (
    <TouchableOpacity style={styles.actionTile} onPress={onPress}>
      <View style={styles.bellContainer}>
        {/* Water drop inside bell */}
        <Animated.View style={[styles.waterDrop, dropStyle]} />

        {/* Bell outline on top */}
        <Ionicons name="notifications-outline" size={24} color="#3b82f6" />
      </View>
      <Text style={styles.actionLabel}>Notifications</Text>
    </TouchableOpacity>
  );
}

function TransactionRow({ desc, amount, time, colors }: any) {
  const isCredit = amount >= 0;
  return (
    <View style={styles.txRow}>
      <View>
        <Text style={[styles.txDesc, { color: colors.textPrimary }]}>
          {desc}
        </Text>
        <Text style={[styles.txTime, { color: colors.textSecondary }]}>
          {time}
        </Text>
      </View>
      <View style={styles.txRight}>
        <Text
          style={[
            styles.txAmount,
            { color: isCredit ? "#22c55e" : colors.textPrimary },
          ]}
        >
          {isCredit ? "+" : "-"}
          {Math.abs(amount).toFixed(0)} SC
        </Text>
      </View>
    </View>
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
  balanceCard: {
    backgroundColor: "#020617",
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 20,
    padding: 24,
    alignItems: "center",
    marginBottom: 16,
  },
  balanceHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    width: "100%",
    marginBottom: 12,
  },
  balanceLabel: {
    fontSize: 12,
    textTransform: "uppercase",
    letterSpacing: 1,
  },
  connectBtn: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
    paddingHorizontal: 12,
    paddingVertical: 6,
    backgroundColor: "rgba(16,185,129,0.1)",
    borderRadius: 20,
    borderWidth: 1,
    borderColor: "rgba(16,185,129,0.3)",
  },
  connectText: { fontSize: 12, color: "#10b981", fontWeight: "600" },
  balanceAmount: {
    fontSize: 36,
    fontWeight: "800",
    color: "#e5e7eb",
    letterSpacing: -1,
  },
  balanceSub: { fontSize: 12, color: "#94a3b8", marginTop: 4 },
  actionsRow: {
    flexDirection: "row",
    gap: 12,
    justifyContent: "space-around",
    marginBottom: 16,
  },
  actionTile: {
    backgroundColor: "#020617",
    padding: 20,
    borderRadius: 16,
    alignItems: "center",
    flex: 1,
    borderWidth: StyleSheet.hairlineWidth,
  },
  actionLabel: {
    fontSize: 13,
    color: "#e5e7eb",
    marginTop: 6,
    fontWeight: "500",
  },
  bellContainer: {
    width: 32,
    height: 32,
    justifyContent: "center",
    alignItems: "center",
  },
  waterDrop: {
    position: "absolute",
    bottom: 4,
    width: 14,
    height: 18,
    backgroundColor: "#3b82f6",
    borderRadius: 14,
    transform: [{ rotate: "180deg" }],
  },
  card: {
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 14,
    padding: 16,
    backgroundColor: "#020617",
  },
  sectionHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 12,
  },
  cardLabel: {
    fontSize: 11,
    textTransform: "uppercase",
    letterSpacing: 1,
  },
  link: { fontSize: 12, fontWeight: "500" },
  emptyText: {
    fontSize: 12,
    marginTop: 4,
  },
  txRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingVertical: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(75,85,99,0.3)",
  },
  txDesc: { fontSize: 14, fontWeight: "500" },
  txTime: { fontSize: 12, marginTop: 2 },
  txRight: { alignItems: "flex-end" },
  txAmount: { fontSize: 16, fontWeight: "600" },
});
