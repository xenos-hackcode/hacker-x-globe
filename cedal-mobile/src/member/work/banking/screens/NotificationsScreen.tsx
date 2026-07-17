// src/member/work/banking/screens/NotificationsScreen.tsx
import { db } from "@/src/api/firebase";
import { useUserProfile } from "@/src/hooks/useUserProfile";
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import {
    collection,
    deleteDoc,
    doc,
    getDocs,
    orderBy,
    query,
} from "firebase/firestore";
import React, { useEffect, useState } from "react";
import {
    ActivityIndicator,
    Alert,
    FlatList,
    StyleSheet,
    Text,
    TouchableOpacity,
    View,
} from "react-native";
import { MIN_DEBT } from "../constants";
import { creditUserFromTrade } from "../wallet";

type Props = {
  onBackHome: () => void;
};

export type GlobalNotification = {
  id: string;
  type: "trade" | "system";
  title: string;
  body: string;
  createdAt: Date;
  fromUserName?: string | null;
  fromUserId?: string | null;
  tradeId?: string | null;
  amount?: number | null;
};

export default function NotificationsScreen({ onBackHome }: Props) {
  const { colors } = useTheme();
  const { user, profile } = useUserProfile();
  const [loading, setLoading] = useState(true);
  const [items, setItems] = useState<GlobalNotification[]>([]);
  const [processingId, setProcessingId] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    async function loadHistory() {
      try {
        setLoading(true);
        const q = query(
          collection(db, "notifications"),
          orderBy("createdAt", "desc")
        );
        const snap = await getDocs(q);
        const docs: GlobalNotification[] = snap.docs.map((docSnap) => {
          const data: any = docSnap.data();
          return {
            id: docSnap.id,
            type: (data.type as "trade" | "system") ?? "system",
            title: data.title ?? "",
            body: data.body ?? "",
            createdAt: data.createdAt?.toDate
              ? data.createdAt.toDate()
              : new Date(),
            fromUserName: data.fromUserName,
            fromUserId: data.fromUserId,
            tradeId: data.tradeId,
            amount:
              typeof data.amount === "number" ? data.amount : null,
          };
        });
        if (!active) return;
        setItems(docs);
      } catch (e) {
        if (!active) return;
        setItems([]);
      } finally {
        if (active) setLoading(false);
      }
    }

    loadHistory();
    return () => {
      active = false;
    };
  }, []);

  const handleCredit = async (item: GlobalNotification) => {
    if (!user) {
      Alert.alert("Sign in required", "You must be signed in to credit.");
      return;
    }
    if (!item.fromUserId || item.amount == null || item.amount <= 0) {
      Alert.alert(
        "Trade error",
        "This trade is missing receiver or amount."
      );
      return;
    }

    const currentBalance = profile?.scBalance ?? 0;
    const newBalance = currentBalance - item.amount;

    if (newBalance < MIN_DEBT) {
      Alert.alert(
        "Limit reached",
        `You can’t credit this trade because it would put you past your debt limit of ${MIN_DEBT} SC.`
      );
      return;
    }

    try {
      setProcessingId(item.id);
      await creditUserFromTrade({
        fromUserId: user.uid,
        toUserId: item.fromUserId,
        amount: item.amount,
        tradeId: item.tradeId ?? undefined,
      });
      Alert.alert("Success", "Star Coins sent.");
    } catch (e: any) {
      Alert.alert("Error", e.message ?? "Failed to credit this trade.");
    } finally {
      setProcessingId(null);
    }
  };

  const handleDelete = async (item: GlobalNotification) => {
    if (!user) {
      Alert.alert("Sign in required", "You must be signed in to delete.");
      return;
    }
    if (!item.fromUserId || item.fromUserId !== user.uid) {
      Alert.alert("Not allowed", "Only the trade creator can delete this.");
      return;
    }

    try {
      setProcessingId(item.id);
      await deleteDoc(doc(db, "notifications", item.id));
      setItems((prev) => prev.filter((n) => n.id !== item.id));
    } catch (e: any) {
      Alert.alert("Error", e.message ?? "Failed to delete this trade.");
    } finally {
      setProcessingId(null);
    }
  };

  const renderItem = ({ item }: { item: GlobalNotification }) => {
    const dateText = item.createdAt.toLocaleDateString("en-GB", {
      year: "numeric",
      month: "short",
      day: "2-digit",
    });
    const timeText = item.createdAt.toLocaleTimeString("en-GB", {
      hour: "2-digit",
      minute: "2-digit",
    });

    const iconName =
      item.type === "trade"
        ? "swap-horizontal"
        : "information-circle-outline";

    const isTrade = item.type === "trade";
    const isProcessing = processingId === item.id;
    const isMine = !!user && item.fromUserId === user.uid;

    return (
      <View style={[styles.card, { borderColor: colors.border }]}>
        <View style={styles.cardIcon}>
          <Ionicons
            name={iconName as any}
            size={20}
            color={isTrade ? "#22c55e" : "#60a5fa"}
          />
        </View>
        <View style={styles.cardContent}>
          <Text
            style={[styles.titleText, { color: colors.textPrimary }]}
            numberOfLines={1}
          >
            {item.title}
          </Text>
          <Text
            style={[styles.bodyText, { color: colors.textSecondary }]}
            numberOfLines={2}
          >
            {item.body}
          </Text>
          <View style={styles.metaRow}>
            <Text style={[styles.metaText, { color: colors.textSecondary }]}>
              {item.fromUserName ? `${item.fromUserName} • ` : ""}
              {dateText} · {timeText}
            </Text>

            {isTrade && (
              <View style={{ flexDirection: "row", gap: 8 }}>
                {!isMine && (
                  <TouchableOpacity
                    style={[
                      styles.creditBtn,
                      isProcessing && { opacity: 0.6 },
                    ]}
                    disabled={isProcessing}
                    onPress={() => handleCredit(item)}
                  >
                    <Text style={styles.creditText}>
                      {isProcessing ? "Sending..." : "Credit"}
                    </Text>
                  </TouchableOpacity>
                )}

                {isMine && (
                  <TouchableOpacity
                    style={[
                      styles.deleteBtn,
                      isProcessing && { opacity: 0.6 },
                    ]}
                    disabled={isProcessing}
                    onPress={() => handleDelete(item)}
                  >
                    <Text style={styles.deleteText}>
                      {isProcessing ? "Deleting..." : "Delete"}
                    </Text>
                  </TouchableOpacity>
                )}
              </View>
            )}
          </View>
        </View>
      </View>
    );
  };

  return (
    <View style={styles.root}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity style={styles.backBtn} onPress={onBackHome}>
          <Ionicons name="chevron-back" size={18} color="#60a5fa" />
          <Text style={[styles.backText, { color: "#60a5fa" }]}>Back</Text>
        </TouchableOpacity>

        <View style={styles.headerTextBlock}>
          <Text style={styles.headerTitle}>Notifications</Text>
          <Text style={styles.headerSubtitle}>
            Global trades and business updates history.
          </Text>
        </View>
      </View>

      {/* List */}
      {loading ? (
        <View style={styles.loadingBox}>
          <ActivityIndicator size="small" color="#60a5fa" />
          <Text style={styles.loadingText}>Loading history...</Text>
        </View>
      ) : items.length === 0 ? (
        <View style={styles.emptyBox}>
          <Ionicons
            name="notifications-off-outline"
            size={26}
            color="#6b7280"
          />
          <Text style={styles.emptyTitle}>No notifications yet</Text>
          <Text style={styles.emptyText}>
            When global trades or business messages are created, they will
            appear here.
          </Text>
        </View>
      ) : (
        <FlatList
          data={items}
          keyExtractor={(item) => item.id}
          renderItem={renderItem}
          contentContainerStyle={styles.listContent}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: "#020617",
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 16,
  },
  header: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 12,
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
  headerTitle: {
    fontSize: 18,
    fontWeight: "600",
    color: "#e5e7eb",
  },
  headerSubtitle: {
    fontSize: 12,
    color: "#9ca3af",
    marginTop: 2,
  },
  loadingBox: {
    marginTop: 24,
    alignItems: "center",
    justifyContent: "center",
  },
  loadingText: {
    marginTop: 8,
    fontSize: 12,
    color: "#9ca3af",
  },
  emptyBox: {
    marginTop: 40,
    alignItems: "center",
    paddingHorizontal: 24,
  },
  emptyTitle: {
    marginTop: 12,
    fontSize: 16,
    fontWeight: "600",
    color: "#e5e7eb",
  },
  emptyText: {
    marginTop: 4,
    fontSize: 13,
    color: "#9ca3af",
    textAlign: "center",
  },
  listContent: {
    paddingTop: 8,
    paddingBottom: 16,
  },
  card: {
    flexDirection: "row",
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 14,
    padding: 12,
    backgroundColor: "#020617",
    marginBottom: 8,
  },
  cardIcon: {
    marginRight: 10,
    paddingTop: 4,
  },
  cardContent: {
    flex: 1,
  },
  titleText: {
    fontSize: 14,
    fontWeight: "600",
  },
  bodyText: {
    fontSize: 13,
    marginTop: 2,
  },
  metaRow: {
    marginTop: 6,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  metaText: {
    fontSize: 11,
  },
  creditBtn: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    backgroundColor: "rgba(34,197,94,0.12)",
  },
  creditText: {
    fontSize: 11,
    fontWeight: "600",
    color: "#22c55e",
    textTransform: "uppercase",
    letterSpacing: 0.5,
  },
  deleteBtn: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    backgroundColor: "rgba(248,113,113,0.16)",
  },
  deleteText: {
    fontSize: 11,
    fontWeight: "600",
    color: "#ef4444",
    textTransform: "uppercase",
    letterSpacing: 0.5,
  },
});
