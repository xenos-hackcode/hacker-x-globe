// src/member/chat/partials/ReportSheet.tsx
import React, { useState } from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
} from "react-native";
import { useChatScreenState } from "@/src/hooks/useChatScreenState";
import { sendReportToAlex } from "@/src/api/alexReport";
import { showToast } from "@/src/member/utils/toast";
import { collection, addDoc, serverTimestamp } from "firebase/firestore";
import { db } from "@/src/api/firebase";

type Props = {
  state: ReturnType<typeof useChatScreenState>;
};

const PRESET_REASONS = [
  "Harassment or bullying",
  "Hate speech or symbols",
  "Spam or scam",
  "Sexual content",
  "Impersonation",
  "Other (write below)",
];

export function ReportSheet({ state }: Props) {
  const {
    reportSheetOpen,
    setReportSheetOpen,
    otherUserId,
    chatId,
    setReportSubmittedSheetOpen,
    reportsForChat,
    currentUserId,
  } = state as any;

  const [reason, setReason] = useState("");
  const [selectedReason, setSelectedReason] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  if (!reportSheetOpen) return null;

  const handleClose = () => {
    setReason("");
    setSelectedReason(null);
    setSubmitting(false);
    setReportSheetOpen(false);
  };

  const handleSubmit = async () => {
    const finalReason =
      selectedReason && selectedReason !== "Other (write below)"
        ? selectedReason + (reason ? ` — ${reason}` : "")
        : reason;

    if (!finalReason.trim() || !otherUserId || !chatId || submitting) {
      return;
    }

    try {
      setSubmitting(true);
      showToast("Report sent");

      // send to Alex backend
      const res = await sendReportToAlex({
        chatId,
        targetUserId: otherUserId,
        reason: finalReason,
      });

      // persist in per-chat reports collection: reports/{chatId}/items/{reportId}
      const reportsCol = collection(db, "reports", String(chatId), "items");
      await addDoc(reportsCol, {
        chatId,
        reporterId: currentUserId,
        targetUserId: otherUserId,
        reason: finalReason,
        status: "pending",
        createdAt: serverTimestamp(),
        alexInitial: res ?? null,
      });

      showToast("Report received");

      // close this form sheet
      handleClose();

      // open the submitted-report panel
      setReportSubmittedSheetOpen(true);
    } catch (e: any) {
      showToast("Report delivery failed");
      setSubmitting(false);
    }
  };

  return (
    <View style={styles.overlay}>
      <View style={styles.sheet}>
        <Text style={styles.title}>Report</Text>

        <Text style={styles.label}>What’s the main issue?</Text>
        <View style={styles.reasonList}>
          {PRESET_REASONS.map((item) => {
            const active = selectedReason === item;
            return (
              <TouchableOpacity
                key={item}
                style={[styles.reasonChip, active && styles.reasonChipActive]}
                onPress={() =>
                  setSelectedReason((prev) => (prev === item ? null : item))
                }
              >
                <Text
                  style={[
                    styles.reasonChipText,
                    active && styles.reasonChipTextActive,
                  ]}
                >
                  {item}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>

        <Text style={[styles.label, { marginTop: 10 }]}>
          Add details (optional)
        </Text>
        <TextInput
          value={reason}
          onChangeText={setReason}
          placeholder="Describe what happened or paste message text"
          placeholderTextColor="#6b7280"
          style={styles.input}
          multiline
        />

        <TouchableOpacity
          style={[styles.button, styles.primary, submitting && { opacity: 0.6 }]}
          onPress={handleSubmit}
          disabled={submitting}
        >
          <Text style={styles.buttonText}>
            {submitting ? "Sending..." : "Submit report"}
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.cancel}
          onPress={handleClose}
          disabled={submitting}
        >
          <Text style={styles.cancelText}>Cancel</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    position: "absolute",
    left: 0,
    right: 0,
    bottom: 0,
    top: 0,
    backgroundColor: "rgba(0,0,0,0.6)",
    justifyContent: "flex-end",
  },
  sheet: {
    padding: 16,
    backgroundColor: "#020617",
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
  },
  title: { fontSize: 16, fontWeight: "600", color: "#e5e7eb", marginBottom: 12 },
  label: { fontSize: 12, color: "#9ca3af", marginBottom: 6 },
  reasonList: {
    flexDirection: "row",
    flexWrap: "wrap",
    marginBottom: 8,
  },
  reasonChip: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    marginRight: 6,
    marginBottom: 6,
    backgroundColor: "#020617",
  },
  reasonChipActive: {
    borderColor: "rgba(239,68,68,0.9)",
    backgroundColor: "#111827",
  },
  reasonChipText: {
    fontSize: 11,
    color: "#e5e7eb",
  },
  reasonChipTextActive: {
    color: "#fecaca",
  },
  input: {
    minHeight: 80,
    maxHeight: 160,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    paddingHorizontal: 10,
    paddingVertical: 8,
    color: "#e5e7eb",
    fontSize: 13,
    marginBottom: 12,
  },
  button: {
    paddingVertical: 10,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.5)",
    alignItems: "center",
    marginBottom: 12,
  },
  primary: {
    borderColor: "rgba(239,68,68,0.9)",
    backgroundColor: "#0b1120",
  },
  buttonText: { color: "#e5e7eb", fontSize: 13 },
  cancel: { marginTop: 4, alignItems: "center" },
  cancelText: { fontSize: 13, color: "#9ca3af" },
});
