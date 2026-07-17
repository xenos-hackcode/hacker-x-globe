// src/member/work/banking/screens/TradeScreen.tsx
import { db } from "@/src/api/firebase";
import { useUserProfile } from "@/src/hooks/useUserProfile";
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import { addDoc, collection, serverTimestamp } from "firebase/firestore";
import React, { useState } from "react";
import {
    KeyboardAvoidingView,
    Platform,
    ScrollView,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    View,
} from "react-native";

type Props = {
  onBackHome: () => void;
};

export default function TradeScreen({ onBackHome }: Props) {
  const { colors } = useTheme();
  const { profile, user } = useUserProfile();
  const [amountText, setAmountText] = useState("");
  const [plea, setPlea] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const scBalance = profile?.scBalance ?? 0;
  const parsedAmount = Number(amountText.replace(/[^0-9.]/g, ""));
  const isValidAmount = parsedAmount > 0;
  const canSubmit = isValidAmount && !!user && !submitting;

  const handleSubmit = async () => {
    if (!canSubmit || !user) return;

    const cleanPlea = plea.trim();
    const creatorName = profile?.nickname || profile?.email || "Unknown user";

    try {
      setSubmitting(true);

      // 1) write trade request
      const tradeRef = await addDoc(collection(db, "trades"), {
        userId: user.uid,
        amount: parsedAmount,
        plea: cleanPlea,
        createdAt: serverTimestamp(),
        creatorName,
      });

      // 2) mirror into notifications/history (now includes amount)
      await addDoc(collection(db, "notifications"), {
        type: "trade",
        tradeId: tradeRef.id,
        title: `Trade request: ${parsedAmount} SC`,
        body:
          cleanPlea ||
          `${creatorName} requested ${parsedAmount} Star Coins.`,
        createdAt: serverTimestamp(),
        fromUserId: user.uid,
        fromUserName: creatorName,
        amount: parsedAmount,
      });

      onBackHome();
    } catch (e) {
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={{ flex: 1 }}
      behavior={Platform.OS === "ios" ? "padding" : undefined}
    >
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
              Create Trade
            </Text>
            <Text style={[styles.subTitle, { color: colors.textSecondary }]}>
              Post a global Star Coin trade request with an optional message.
            </Text>
          </View>
        </View>

        {/* Balance info */}
        <View style={styles.balanceRow}>
          <Text style={[styles.balanceLabel, { color: colors.textSecondary }]}>
            Your balance
          </Text>
          <Text style={[styles.balanceValue, { color: colors.textPrimary }]}>
            {scBalance.toLocaleString("en-GB")} SC
          </Text>
        </View>

        {/* Amount input */}
        <View style={styles.card}>
          <Text style={[styles.cardLabel, { color: colors.textSecondary }]}>
            Amount to request
          </Text>
          <View style={styles.amountRow}>
            <Text style={styles.amountSuffix}>SC</Text>
            <TextInput
              style={[styles.amountInput, { color: colors.textPrimary }]}
              placeholder="0"
              placeholderTextColor="#4b5563"
              keyboardType="numeric"
              value={amountText}
              onChangeText={(text) => {
                const cleaned = text.replace(/[^0-9]/g, "");
                setAmountText(cleaned);
              }}
            />
          </View>
          <Text style={[styles.helperText, { color: colors.textSecondary }]}>
            This trade is global. You can request more than your current
            balance.
          </Text>
        </View>

        {/* Plea message */}
        <View style={styles.card}>
          <Text style={[styles.cardLabel, { color: colors.textSecondary }]}>
            Plea message (optional)
          </Text>
          <TextInput
            style={[
              styles.pleaInput,
              { color: colors.textPrimary, borderColor: colors.border },
            ]}
            placeholder="Explain why you need these Star Coins..."
            placeholderTextColor="#4b5563"
            multiline
            value={plea}
            onChangeText={setPlea}
            maxLength={280}
          />
          <Text style={[styles.charCount, { color: colors.textSecondary }]}>
            {plea.length}/280
          </Text>
        </View>

        {/* Submit */}
        <TouchableOpacity
          style={[
            styles.submitBtn,
            {
              backgroundColor: canSubmit ? "#22c55e" : "#374151",
            },
          ]}
          disabled={!canSubmit}
          onPress={handleSubmit}
        >
          <Text style={styles.submitText}>
            {submitting ? "Posting..." : "Post trade"}
          </Text>
        </TouchableOpacity>
      </ScrollView>
    </KeyboardAvoidingView>
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
  balanceRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  balanceLabel: {
    fontSize: 12,
    textTransform: "uppercase",
    letterSpacing: 1,
  },
  balanceValue: {
    fontSize: 14,
    fontWeight: "600",
  },
  card: {
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 14,
    padding: 16,
    backgroundColor: "#020617",
    borderColor: "#1f2937",
  },
  cardLabel: {
    fontSize: 12,
    textTransform: "uppercase",
    letterSpacing: 1,
    marginBottom: 8,
  },
  amountRow: {
    flexDirection: "row-reverse",
    alignItems: "center",
    marginBottom: 6,
  },
  amountSuffix: {
    fontSize: 18,
    fontWeight: "600",
    color: "#9ca3af",
    marginLeft: 8,
  },
  amountInput: {
    flex: 1,
    fontSize: 28,
    fontWeight: "700",
    textAlign: "right",
  },
  helperText: {
    fontSize: 11,
    marginTop: 4,
  },
  pleaInput: {
    marginTop: 4,
    minHeight: 80,
    borderWidth: StyleSheet.hairlineWidth,
    borderRadius: 10,
    padding: 10,
    fontSize: 13,
    textAlignVertical: "top",
  },
  charCount: {
    fontSize: 11,
    textAlign: "right",
    marginTop: 4,
  },
  submitBtn: {
    marginTop: 8,
    paddingVertical: 14,
    borderRadius: 999,
    alignItems: "center",
  },
  submitText: {
    color: "#f9fafb",
    fontSize: 15,
    fontWeight: "600",
  },
});
