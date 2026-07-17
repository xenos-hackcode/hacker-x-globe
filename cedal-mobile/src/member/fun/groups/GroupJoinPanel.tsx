// src/member/fun/groups/GroupJoinPanel.tsx
import React, { useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  TouchableOpacity,
} from "react-native";

type Group = {
  id?: string;
  name?: string;
};

type Mode = "join" | "request";

type Props = {
  mode: Mode;                    // "join" = join now, "request" = send request
  group: Group | null;
  requireInviteCode: boolean;    // true for private groups
  onCancel: () => void;
  onSubmit: (params: { code?: string; reason: string }) => Promise<void>;
};

const INVITE_PREFIX = "cedal-";

export default function GroupJoinPanel({
  mode,
  group,
  requireInviteCode,
  onCancel,
  onSubmit,
}: Props) {
  const [reason, setReason] = useState("");
  const [codeSuffix, setCodeSuffix] = useState(""); // only after "cedal-"
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const isJoin = mode === "join";

  if (!group) return null;

  const handleSubmit = async () => {
    const trimmedReason = reason.trim();
    const trimmedSuffix = codeSuffix.trim();

    if (requireInviteCode && !trimmedSuffix) {
      setError("Invite code is required for this group.");
      return;
    }

    const fullCode =
      requireInviteCode && trimmedSuffix
        ? `${INVITE_PREFIX}${trimmedSuffix}`
        : undefined;

    setLoading(true);
    setError(null);
    try {
      await onSubmit({
        code: fullCode,
        reason: trimmedReason,
      });
      onCancel(); // close on success
    } catch (e: any) {
      setError(e?.message || "Could not join group.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.overlay}>
      <View style={styles.card}>
        {/* header + back pill */}
        <View style={styles.headerRow}>
          <View>
            <Text style={styles.headerMeta}>
              {isJoin ? "Join group" : "Request access"}
            </Text>
            <Text style={styles.headerTitle}>{group.name}</Text>
          </View>

          <TouchableOpacity
            activeOpacity={0.8}
            onPress={onCancel}
            style={styles.backPill}
          >
            <Text style={styles.backPillText}>◀</Text>
          </TouchableOpacity>
        </View>

        {/* invite code for private groups */}
        {requireInviteCode && (
          <>
            <Text style={styles.label}>Invite link or code</Text>
            <View style={styles.codeRow}>
              <Text style={styles.codePrefix}>{INVITE_PREFIX}</Text>
              <TextInput
                style={styles.codeInput}
                value={codeSuffix}
                onChangeText={setCodeSuffix}
                placeholder="XXXXXXXX"
                placeholderTextColor="#6b7280"
                autoCapitalize="none"
                autoCorrect={false}
              />
            </View>

            <Text style={styles.privateHint}>
              Enter a valid invite code to join instantly. If you do not have a
              code, you can send an optional note below and the owner can
              approve your request.
            </Text>
          </>
        )}

        {/* reason textarea */}
        <Text style={styles.bodyText}>
          Optional note for the group. You can leave this empty and continue.
        </Text>

        <TextInput
          multiline
          numberOfLines={4}
          value={reason}
          onChangeText={setReason}
          placeholder="Optional: Who you are, when you're active, what you’re looking for…"
          placeholderTextColor="#6b7280"
          style={styles.textarea}
        />

        {error && <Text style={styles.error}>{error}</Text>}

        <View style={styles.footerRow}>
          <TouchableOpacity
            activeOpacity={0.8}
            onPress={onCancel}
            style={styles.cancelBtn}
          >
            <Text style={styles.cancelText}>Cancel</Text>
          </TouchableOpacity>

          <TouchableOpacity
            activeOpacity={0.9}
            onPress={handleSubmit}
            style={[
              styles.submitBtn,
              isJoin ? styles.submitJoin : styles.submitRequest,
            ]}
            disabled={loading}
          >
            <Text style={styles.submitText}>
              {loading
                ? isJoin
                  ? "Joining..."
                  : "Sending..."
                : isJoin
                ? "Join group"
                : "Send request"}
            </Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    position: "absolute",
    inset: 0,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "rgba(15,23,42,0.9)",
  },
  card: {
    width: 420,
    maxWidth: "90%",
    borderRadius: 18,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.5)",
    backgroundColor: "rgba(15,23,42,0.98)",
    padding: 18,
    alignSelf: "center",
  },
  headerRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 10,
    gap: 8,
  },
  headerMeta: {
    fontSize: 11,
    textTransform: "uppercase",
    letterSpacing: 1.2,
    color: "#6b7280",
  },
  headerTitle: {
    fontSize: 16,
    fontWeight: "600",
    color: "#f9a8d4",
  },
  backPill: {
    width: 32,
    height: 32,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.3)",
    backgroundColor: "rgba(15,23,42,0.8)",
    alignItems: "center",
    justifyContent: "center",
  },
  backPillText: {
    fontSize: 16,
    color: "#e5e7eb",
  },
  label: {
    fontSize: 12,
    color: "#9ca3af",
    marginBottom: 4,
  },
  codeRow: {
    flexDirection: "row",
    alignItems: "center",
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    backgroundColor: "rgba(15,23,42,0.95)",
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  codePrefix: {
    fontSize: 13,
    color: "#e5e7eb",
    marginRight: 4,
  },
  codeInput: {
    flex: 1,
    fontSize: 13,
    color: "#e5e7eb",
    paddingVertical: 0,
  },
  privateHint: {
    marginTop: 6,
    fontSize: 11,
    color: "#9ca3af",
  },
  bodyText: {
    fontSize: 13,
    color: "#9ca3af",
    marginTop: 10,
    marginBottom: 8,
  },
  textarea: {
    width: "100%",
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    backgroundColor: "rgba(15,23,42,0.95)",
    color: "#e5e7eb",
    fontSize: 13,
    padding: 10,
    textAlignVertical: "top",
  },
  error: {
    fontSize: 12,
    color: "#f97373",
    marginTop: 6,
  },
  footerRow: {
    flexDirection: "row",
    justifyContent: "flex-end",
    gap: 8,
    marginTop: 12,
  },
  cancelBtn: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#1f2937",
    backgroundColor: "transparent",
  },
  cancelText: {
    fontSize: 12,
    color: "#9ca3af",
  },
  submitBtn: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 999,
  },
  submitJoin: {
    backgroundColor: "#f472b6",
  },
  submitRequest: {
    backgroundColor: "#fb7185",
  },
  submitText: {
    fontSize: 12,
    color: "#020617",
    fontWeight: "600",
  },
});
