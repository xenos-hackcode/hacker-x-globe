// src/member/work/banking/screens/TransactionsScreen.tsx
import { auth, db } from "@/src/api/firebase";
import { useTheme } from "@/src/themes/ThemeContext";
import {
    collection,
    onSnapshot,
    orderBy,
    query,
    where,
} from "firebase/firestore";
import React, { useEffect, useMemo, useState } from "react";
import {
    ActivityIndicator,
    ScrollView,
    StyleSheet,
    Text,
    TouchableOpacity,
    View,
} from "react-native";

type Props = {
  onBack: () => void;
};

type TxKind = "debit" | "credit";

export type TransactionRecord = {
  id: string;
  kind: TxKind;            // "debit" = you sent, "credit" = you received
  amount: number;          // in SC, always positive
  fromUserName?: string | null;
  toUserName?: string | null;
  source?: "send" | "task" | "game" | "system";
  createdAt: Date;
};

export default function TransactionsScreen({ onBack }: Props) {
  const { colors } = useTheme();
  const [filter, setFilter] = useState<"all" | TxKind>("all");
  const [transactions, setTransactions] = useState<TransactionRecord[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const current = auth.currentUser;
    if (!current) {
      setTransactions([]);
      setLoading(false);
      return;
    }

    const q = query(
      collection(db, "walletTransactions"),
      where("userId", "==", current.uid),
      orderBy("createdAt", "desc")
    );

    const unsub = onSnapshot(
      q,
      (snap) => {
        const list: TransactionRecord[] = [];
        snap.forEach((docSnap) => {
          const data: any = docSnap.data();

          const type = data.type as "topup" | "credit_out" | "credit_in";
          const rawAmount = Number(data.amount ?? 0);
          const createdAt: Date =
            data.createdAt?.toDate?.() ?? new Date();

          if (type === "topup") {
            list.push({
              id: docSnap.id,
              kind: "credit",
              amount: Math.abs(rawAmount),
              fromUserName: "Top‑up",
              source: "system",
              createdAt,
            });
          } else if (type === "credit_in") {
            list.push({
              id: docSnap.id,
              kind: "credit",
              amount: Math.abs(rawAmount),
              fromUserName:
                data.fromUserName ||
                data.peerUserId ||
                "Friend",
              source: "send",
              createdAt,
            });
          } else if (type === "credit_out") {
            list.push({
              id: docSnap.id,
              kind: "debit",
              amount: Math.abs(rawAmount),
              toUserName:
                data.toUserName ||
                data.peerUserId ||
                "Friend",
              source: "send",
              createdAt,
            });
          }
        });

        setTransactions(list);
        setLoading(false);
      },
      (err) => {
        setLoading(false);
      }
    );

    return () => unsub();
  }, []);

  const filtered = useMemo(() => {
    if (filter === "all") return transactions;
    return transactions.filter((t) => t.kind === filter);
  }, [filter, transactions]);

  return (
    <ScrollView
      style={[styles.root, { backgroundColor: colors.background }]}
      contentContainerStyle={styles.scrollContent}
    >
      <TouchableOpacity style={styles.backBtn} onPress={onBack}>
        <Text style={styles.backText}>← Back</Text>
      </TouchableOpacity>

      <Text style={[styles.title, { color: colors.textPrimary }]}>
        Star Coin Activity
      </Text>

      <View style={styles.filtersRow}>
        <FilterChip
          label="All"
          active={filter === "all"}
          onPress={() => setFilter("all")}
        />
        <FilterChip
          label="Credit"
          active={filter === "credit"}
          onPress={() => setFilter("credit")}
        />
        <FilterChip
          label="Debit"
          active={filter === "debit"}
          onPress={() => setFilter("debit")}
        />
      </View>

      {loading ? (
        <View style={{ marginTop: 16, alignItems: "center" }}>
          <ActivityIndicator size="small" color="#60a5fa" />
        </View>
      ) : filtered.length === 0 ? (
        <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
          No Star Coin activity yet. Credits and debits will show here.
        </Text>
      ) : (
        filtered.map((tx) => {
          const isCredit = tx.kind === "credit";
          const primaryLine = isCredit
            ? tx.fromUserName || "Received SC"
            : tx.toUserName || "Sent SC";

          const secondaryLine = isCredit
            ? tx.source === "task"
              ? "Task reward"
              : tx.source === "game"
              ? "Game reward"
              : tx.source === "send"
              ? "From friend"
              : "Credit"
            : tx.source === "send"
            ? "To friend"
            : "Debit";

          const dateText = tx.createdAt.toLocaleDateString("en-GB", {
            year: "numeric",
            month: "short",
            day: "2-digit",
          });
          const timeText = tx.createdAt.toLocaleTimeString("en-GB", {
            hour: "2-digit",
            minute: "2-digit",
          });

          return (
            <View
              key={tx.id}
              style={[styles.txCard, { borderColor: colors.border }]}
            >
              <View>
                <Text
                  style={[styles.txDesc, { color: colors.textPrimary }]}
                  numberOfLines={1}
                >
                  {primaryLine}
                </Text>
                <Text
                  style={[styles.txMeta, { color: colors.textSecondary }]}
                  numberOfLines={1}
                >
                  {secondaryLine}
                </Text>
                <Text
                  style={[styles.txDate, { color: colors.textSecondary }]}
                >
                  {dateText} · {timeText}
                </Text>
              </View>

              <Text
                style={[
                  styles.txAmount,
                  { color: isCredit ? "#22c55e" : "#ef4444" },
                ]}
              >
                {isCredit ? "+" : "-"}
                {tx.amount.toFixed(0)} SC
              </Text>
            </View>
          );
        })
      )}
    </ScrollView>
  );
}

function FilterChip({
  label,
  active,
  onPress,
}: {
  label: string;
  active: boolean;
  onPress: () => void;
}) {
  return (
    <TouchableOpacity onPress={onPress}>
      <View style={[styles.chip, active && styles.chipActive]}>
        <Text style={[styles.chipText, active && styles.chipTextActive]}>
          {label}
        </Text>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  scrollContent: { padding: 16, paddingTop: 8, gap: 8 },
  backBtn: { paddingVertical: 12, paddingHorizontal: 8 },
  backText: { fontSize: 16, fontWeight: "600", color: "#60a5fa" },
  title: { fontSize: 20, fontWeight: "700", marginBottom: 8 },
  filtersRow: {
    flexDirection: "row",
    gap: 8,
    marginBottom: 8,
  },
  chip: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: "#4b5563",
    backgroundColor: "rgba(15,23,42,0.8)",
  },
  chipActive: {
    borderColor: "#60a5fa",
    backgroundColor: "rgba(37,99,235,0.25)",
  },
  chipText: {
    fontSize: 11,
    color: "#9ca3af",
    textTransform: "uppercase",
    letterSpacing: 1,
  },
  chipTextActive: {
    color: "#e5e7eb",
    fontWeight: "600",
  },
  emptyText: {
    fontSize: 13,
    marginTop: 8,
  },
  txCard: {
    backgroundColor: "#020617",
    padding: 16,
    borderRadius: 12,
    borderWidth: 1,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  txDesc: { fontSize: 15, fontWeight: "500" },
  txMeta: { fontSize: 12, marginTop: 2 },
  txDate: { fontSize: 11, marginTop: 2 },
  txAmount: { fontSize: 18, fontWeight: "700", marginLeft: 12 },
});
