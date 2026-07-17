// src/member/chat/partials/PinMessageSheet.tsx
import React, { useState } from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
} from "react-native";
import type { Message } from "@/src/member/chat/MessageRow";

type Props = {
  visible: boolean;
  message: Message | null;
  onClose: () => void;
  onConfirm: (durationMs: number) => void;
};

export function PinMessageSheet({ visible, message, onClose, onConfirm }: Props) {
  const [years, setYears] = useState("");
  const [months, setMonths] = useState("");
  const [weeks, setWeeks] = useState("");
  const [days, setDays] = useState("");
  const [hours, setHours] = useState("");
  const [minutes, setMinutes] = useState("");
  const [seconds, setSeconds] = useState("");

  if (!visible || !message) return null;

  const handleConfirm = () => {
    const y = Number(years) || 0;
    const mo = Number(months) || 0;
    const w = Number(weeks) || 0;
    const d = Number(days) || 0;
    const h = Number(hours) || 0;
    const mi = Number(minutes) || 0;
    const s = Number(seconds) || 0;

    const durationMs =
      y * 365 * 24 * 60 * 60 * 1000 +
      mo * 30 * 24 * 60 * 60 * 1000 +
      w * 7 * 24 * 60 * 60 * 1000 +
      d * 24 * 60 * 60 * 1000 +
      h * 60 * 60 * 1000 +
      mi * 60 * 1000 +
      s * 1000;

    if (durationMs <= 0) {
      onClose();
      return;
    }

    onConfirm(durationMs);
  };

  return (
    <View style={styles.backdrop}>
      <View style={styles.sheet}>
        <Text style={styles.title}>Pin message</Text>
        <Text style={styles.subtitle} numberOfLines={2}>
          “{message.text || "[media]"}”
        </Text>

        <Text style={styles.label}>Duration</Text>

        <View style={styles.row}>
          <TextInput
            style={styles.input}
            placeholder="Years"
            placeholderTextColor="#6b7280"
            keyboardType="numeric"
            value={years}
            onChangeText={setYears}
          />
          <TextInput
            style={styles.input}
            placeholder="Months"
            placeholderTextColor="#6b7280"
            keyboardType="numeric"
            value={months}
            onChangeText={setMonths}
          />
        </View>

        <View style={styles.row}>
          <TextInput
            style={styles.input}
            placeholder="Weeks"
            placeholderTextColor="#6b7280"
            keyboardType="numeric"
            value={weeks}
            onChangeText={setWeeks}
          />
          <TextInput
            style={styles.input}
            placeholder="Days"
            placeholderTextColor="#6b7280"
            keyboardType="numeric"
            value={days}
            onChangeText={setDays}
          />
        </View>

        <View style={styles.row}>
          <TextInput
            style={styles.input}
            placeholder="Hours"
            placeholderTextColor="#6b7280"
            keyboardType="numeric"
            value={hours}
            onChangeText={setHours}
          />
          <TextInput
            style={styles.input}
            placeholder="Minutes"
            placeholderTextColor="#6b7280"
            keyboardType="numeric"
            value={minutes}
            onChangeText={setMinutes}
          />
        </View>

        <View style={styles.row}>
          <TextInput
            style={styles.input}
            placeholder="Seconds"
            placeholderTextColor="#6b7280"
            keyboardType="numeric"
            value={seconds}
            onChangeText={setSeconds}
          />
        </View>

        <View style={styles.actionsRow}>
          <TouchableOpacity onPress={onClose} style={styles.cancelBtn}>
            <Text style={styles.cancelText}>Cancel</Text>
          </TouchableOpacity>
          <TouchableOpacity onPress={handleConfirm} style={styles.confirmBtn}>
            <Text style={styles.confirmText}>Pin</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(0,0,0,0.6)",
    justifyContent: "center",
    alignItems: "center",
  },
  sheet: {
    width: "90%",
    borderRadius: 16,
    padding: 16,
    backgroundColor: "#020617",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
  },
  title: {
    fontSize: 16,
    fontWeight: "600",
    color: "#e5e7eb",
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 13,
    color: "#9ca3af",
    marginBottom: 12,
  },
  label: {
    fontSize: 12,
    color: "#9ca3af",
    marginBottom: 4,
  },
  row: {
    flexDirection: "row",
    columnGap: 8,
    marginBottom: 6,
  },
  input: {
    flex: 1,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    paddingHorizontal: 8,
    paddingVertical: 4,
    color: "#e5e7eb",
    fontSize: 13,
  },
  actionsRow: {
    flexDirection: "row",
    justifyContent: "flex-end",
    marginTop: 12,
    columnGap: 8,
  },
  cancelBtn: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
  },
  cancelText: {
    fontSize: 13,
    color: "#9ca3af",
  },
  confirmBtn: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "#22c55e",
  },
  confirmText: {
    fontSize: 13,
    color: "#022c22",
    fontWeight: "600",
  },
});
