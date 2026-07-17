// src/member/work/banking/screens/SendScreen.tsx
import { useUserProfile } from "@/src/hooks/useUserProfile";
import { useChatFriends } from "@/src/member/chat/useChatFriends";
import { useBankSettings } from "@/src/member/work/banking/BankSettingsContext";
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import React, { useState } from "react";
import {
    Alert,
    FlatList,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    Vibration,
    View,
} from "react-native";
import { MIN_DEBT } from "../constants";
import { creditUserFromTrade } from "../wallet";

type Props = {
  onBackHome: () => void;
};

export default function SendScreen({ onBackHome }: Props) {
  const { colors } = useTheme();
  const { friends } = useChatFriends();
  const { profile, user } = useUserProfile();
  const { largeSpendAlertsEnabled } = useBankSettings();

  const walletBalance = profile?.scBalance ?? 0;

  const [amount, setAmount] = useState("");
  const [reference, setReference] = useState("");
  const [toName, setToName] = useState("");
  const [toId, setToId] = useState<string | null>(null);
  const [showFriendPanel, setShowFriendPanel] = useState(false);
  const [sending, setSending] = useState(false);

  const handleSelectFriend = (friend: {
    id: string;
    name: string;
    avatarUri?: string | null;
  }) => {
    setToId(friend.id);
    setToName(friend.name);
    setShowFriendPanel(false);
  };

  const handleChangeAmount = (text: string) => {
    const cleaned = text.replace(/[^\d]/g, "");
    if (cleaned === "") {
      setAmount("");
      return;
    }
    const value = parseInt(cleaned, 10);
    if (Number.isNaN(value)) {
      setAmount("");
      return;
    }
    const clamped = Math.min(value, walletBalance);
    setAmount(String(clamped));
  };

  const numericAmount = parseInt(amount || "0", 10) || 0;
  const canSend =
    !!toId && numericAmount > 0 && numericAmount <= walletBalance && !!user;

  const friendItems = user
    ? friends.filter((f) => f.id !== user.uid)
    : friends;

  const handleSend = async () => {
    if (!canSend || !user || !toId) return;

    const currentBalance = walletBalance;
    const newBalance = currentBalance - numericAmount;

    if (newBalance < MIN_DEBT) {
      Alert.alert(
        "Limit reached",
        `You can’t send this amount because it would put you past your debt limit of ${MIN_DEBT} SC.`
      );
      return;
    }

    const LARGE_SPEND_THRESHOLD = 1000;

    Alert.alert(
      "Confirm send",
      `Send ${numericAmount} SC to ${toName}?`,
      [
        { text: "Cancel", style: "cancel" },
        {
          text: "Send",
          style: "default",
          onPress: async () => {
            try {
              setSending(true);

              // Large spend vibration: only if toggle is on and amount > threshold
              if (
                largeSpendAlertsEnabled &&
                numericAmount > LARGE_SPEND_THRESHOLD
              ) {
                Vibration.vibrate(200);
              }

              await creditUserFromTrade({
                fromUserId: user.uid,
                toUserId: toId,
                amount: numericAmount,
              });
              Alert.alert("Success", "Star Coins sent.");
              setAmount("");
              setReference("");
            } catch (e: any) {
              Alert.alert(
                "Error",
                e?.message ?? "Could not send Star Coins."
              );
            } finally {
              setSending(false);
            }
          },
        },
      ]
    );
  };

  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      <ScrollView
        style={styles.scroll}
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
      >
        <TouchableOpacity style={styles.backBtn} onPress={onBackHome}>
          <Text style={[styles.backText, { color: "#60a5fa" }]}>← Back</Text>
        </TouchableOpacity>

        <View style={styles.header}>
          <Text style={[styles.title, { color: colors.textPrimary }]}>
            Send Star Coins
          </Text>
          <Text style={[styles.subTitle, { color: colors.textSecondary }]}>
            Choose a friend and how many SC to send.
          </Text>
        </View>

        {/* Source info */}
        <View style={[styles.card, { borderColor: colors.border }]}>
          <View style={styles.sourceRow}>
            <Ionicons name="wallet-outline" size={18} color="#22c55e" />
            <Text style={[styles.sourceText, { color: colors.textPrimary }]}>
              From: main SC wallet
            </Text>
          </View>
          <Text style={[styles.sourceSub, { color: colors.textSecondary }]}>
            Available: {walletBalance.toLocaleString("en-GB")} SC
          </Text>
          <Text style={[styles.sourceSub, { color: colors.textSecondary }]}>
            You can only send SC to people on your chat list.
          </Text>
        </View>

        {/* Recipient */}
        <View style={[styles.card, { borderColor: colors.border }]}>
          <Text style={[styles.label, { color: colors.textSecondary }]}>
            To (chat friend)
          </Text>
          <TextInput
            placeholder="Pick from your chats"
            placeholderTextColor="#6b7280"
            style={[styles.input, { color: colors.textPrimary }]}
            value={toName}
            editable={false}
          />
          <TouchableOpacity
            style={styles.pickFriendRow}
            onPress={() => setShowFriendPanel(true)}
          >
            <Ionicons
              name="chatbubble-ellipses-outline"
              size={14}
              color="#60a5fa"
            />
            <Text style={styles.pickFriendText}>Select from chat list</Text>
          </TouchableOpacity>
        </View>

        {/* Amount in SC */}
        <View style={[styles.card, { borderColor: colors.border }]}>
          <Text style={[styles.label, { color: colors.textSecondary }]}>
            Amount (SC)
          </Text>
          <View style={styles.amountRow}>
            <Text style={styles.currency}>SC</Text>
            <TextInput
              placeholder="0"
              placeholderTextColor="#6b7280"
              keyboardType="number-pad"
              style={[styles.amountInput, { color: colors.textPrimary }]}
              value={amount}
              onChangeText={handleChangeAmount}
            />
          </View>
          <Text style={[styles.helperText, { color: colors.textSecondary }]}>
            You can’t send more than your current balance or below your debt
            limit.
          </Text>
        </View>

        {/* Note */}
        <View style={[styles.card, { borderColor: colors.border }]}>
          <Text style={[styles.label, { color: colors.textSecondary }]}>
            Note
          </Text>
          <TextInput
            placeholder="What is this for?"
            placeholderTextColor="#6b7280"
            style={[styles.input, { color: colors.textPrimary }]}
            value={reference}
            onChangeText={setReference}
          />
        </View>

        {/* Send button */}
        <TouchableOpacity
          style={[
            styles.sendBtn,
            (!canSend || sending) && { opacity: 0.4 },
          ]}
          disabled={!canSend || sending}
          onPress={handleSend}
        >
          <Text style={styles.sendText}>
            {sending ? "Sending..." : "Send SC"}
          </Text>
        </TouchableOpacity>
      </ScrollView>

      {/* Friend picker panel */}
      {showFriendPanel && (
        <View style={styles.panelBackdrop}>
          <View style={[styles.panel, { borderColor: colors.border }]}>
            <View style={styles.panelHeader}>
              <Text style={[styles.panelTitle, { color: colors.textPrimary }]}>
                Select from chats
              </Text>
              <TouchableOpacity onPress={() => setShowFriendPanel(false)}>
                <Ionicons name="close" size={18} color={colors.textSecondary} />
              </TouchableOpacity>
            </View>

            {friendItems.length === 0 ? (
              <Text
                style={[styles.panelEmpty, { color: colors.textSecondary }]}
              >
                No chat friends yet. Add friends from the chat screen.
              </Text>
            ) : (
              <FlatList
                data={friendItems}
                keyExtractor={(f) => f.id}
                renderItem={({ item, index }) => {
                  return (
                    <TouchableOpacity
                      style={styles.friendRow}
                      onPress={() => handleSelectFriend(item)}
                    >
                      <Text style={{ color: "white" }}>{item.name}</Text>
                    </TouchableOpacity>
                  );
                }}
                ItemSeparatorComponent={() => (
                  <View style={styles.friendSeparator} />
                )}
              />
            )}
          </View>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  scroll: { flex: 1 },
  scrollContent: {
    paddingHorizontal: 16,
    paddingTop: 8,
    paddingBottom: 24,
    gap: 12,
  },
  backBtn: {
    paddingVertical: 8,
    paddingHorizontal: 4,
  },
  backText: {
    fontSize: 14,
    fontWeight: "600",
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
  sourceRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
    marginBottom: 4,
  },
  sourceText: {
    fontSize: 13,
    fontWeight: "500",
  },
  sourceSub: {
    fontSize: 11,
  },
  label: {
    fontSize: 11,
    textTransform: "uppercase",
    letterSpacing: 1,
    marginBottom: 6,
  },
  input: {
    fontSize: 14,
  },
  pickFriendRow: {
    marginTop: 6,
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
  },
  pickFriendText: {
    fontSize: 12,
    color: "#60a5fa",
  },
  amountRow: {
    flexDirection: "row",
    alignItems: "center",
  },
  currency: {
    fontSize: 18,
    color: "#e5e7eb",
    marginRight: 6,
  },
  amountInput: {
    fontSize: 28,
    fontWeight: "700",
    flex: 1,
  },
  helperText: {
    fontSize: 11,
    marginTop: 4,
  },
  sendBtn: {
    marginTop: 16,
    backgroundColor: "#22c55e",
    borderRadius: 999,
    paddingVertical: 12,
    alignItems: "center",
  },
  sendText: {
    fontSize: 15,
    fontWeight: "600",
    color: "#020617",
  },
  panelBackdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(15,23,42,0.8)",
    justifyContent: "flex-end",
  },
  panel: {
    maxHeight: "55%",
    borderTopLeftRadius: 18,
    borderTopRightRadius: 18,
    borderWidth: StyleSheet.hairlineWidth,
    backgroundColor: "#020617",
    padding: 12,
  },
  panelHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 8,
  },
  panelTitle: {
    fontSize: 14,
    fontWeight: "600",
  },
  panelEmpty: {
    fontSize: 12,
  },
  friendRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 6,
  },
  friendSeparator: {
    height: StyleSheet.hairlineWidth,
    backgroundColor: "rgba(55,65,81,0.6)",
  },
});
