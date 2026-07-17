// src/member/chat/partials/DisappearingSheet.tsx
import React, { useState, useEffect } from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  Switch,
} from "react-native";
import { useChatScreenState } from "@/src/hooks/useChatScreenState";

type Props = {
  state: ReturnType<typeof useChatScreenState>;
};

function toParts(totalSeconds: number) {
  let remaining = totalSeconds;

  const years = Math.floor(remaining / (365 * 24 * 3600));
  remaining -= years * 365 * 24 * 3600;

  const months = Math.floor(remaining / (30 * 24 * 3600));
  remaining -= months * 30 * 24 * 3600;

  const days = Math.floor(remaining / (24 * 3600));
  remaining -= days * 24 * 3600;

  const hours = Math.floor(remaining / 3600);
  remaining -= hours * 3600;

  const minutes = Math.floor(remaining / 60);
  remaining -= minutes * 60;

  const seconds = remaining;

  return { years, months, days, hours, minutes, seconds };
}

function toSeconds(parts: {
  years: number;
  months: number;
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
}) {
  return (
    parts.years * 365 * 24 * 3600 +
    parts.months * 30 * 24 * 3600 +
    parts.days * 24 * 3600 +
    parts.hours * 3600 +
    parts.minutes * 60 +
    parts.seconds
  );
}

export function DisappearingSheet({ state }: Props) {
  const {
    disappearingSheetOpen,
    setDisappearingSheetOpen,
    disappearingEnabled,
    disappearingDurationSeconds,
    saveDisappearingConfig,
  } = state as any;

  const [enabled, setEnabled] = useState(disappearingEnabled);
  const [years, setYears] = useState("0");
  const [months, setMonths] = useState("0");
  const [days, setDays] = useState("0");
  const [hours, setHours] = useState("0");
  const [minutes, setMinutes] = useState("0");
  const [seconds, setSeconds] = useState("0");

  useEffect(() => {
    setEnabled(disappearingEnabled);
    const p = toParts(disappearingDurationSeconds || 0);
    setYears(String(p.years));
    setMonths(String(p.months));
    setDays(String(p.days));
    setHours(String(p.hours));
    setMinutes(String(p.minutes));
    setSeconds(String(p.seconds));
  }, [disappearingEnabled, disappearingDurationSeconds]);

  if (!disappearingSheetOpen) return null;

  const handleClose = () => setDisappearingSheetOpen(false);

  const handleSave = async () => {
    const parts = {
      years: Number(years) || 0,
      months: Number(months) || 0,
      days: Number(days) || 0,
      hours: Number(hours) || 0,
      minutes: Number(minutes) || 0,
      seconds: Number(seconds) || 0,
    };
    const totalSeconds = toSeconds(parts);

    await saveDisappearingConfig(enabled && totalSeconds > 0, totalSeconds);
    handleClose();
  };

  const renderField = (
    label: string,
    value: string,
    onChange: (v: string) => void,
  ) => (
    <View style={styles.field}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <TextInput
        value={value}
        onChangeText={(t) => onChange(t.replace(/[^0-9]/g, ""))}
        keyboardType="number-pad"
        style={styles.fieldInput}
        placeholder="0"
        placeholderTextColor="#6b7280"
      />
    </View>
  );

  return (
    <View style={styles.overlay}>
      <View style={styles.sheet}>
        <View style={styles.headerRow}>
          <Text style={styles.title}>Message disappearing</Text>
          <TouchableOpacity onPress={handleClose}>
            <Text style={styles.closeText}>Close</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.switchRow}>
          <Text style={styles.switchLabel}>Enable for this chat</Text>
          <Switch
            value={enabled}
            onValueChange={setEnabled}
            thumbColor={enabled ? "#22c55e" : "#6b7280"}
            trackColor={{ false: "#1f2937", true: "#047857" }}
          />
        </View>

        <Text style={styles.helpText}>
          Choose how long messages stay before they disappear.
        </Text>

        <View style={styles.grid}>
          {renderField("Years", years, setYears)}
          {renderField("Months", months, setMonths)}
          {renderField("Days", days, setDays)}
          {renderField("Hours", hours, setHours)}
          {renderField("Minutes", minutes, setMinutes)}
          {renderField("Seconds", seconds, setSeconds)}
        </View>

        <TouchableOpacity style={styles.saveBtn} onPress={handleSave}>
          <Text style={styles.saveText}>Save</Text>
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
  headerRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 12,
  },
  title: { fontSize: 16, fontWeight: "600", color: "#e5e7eb" },
  closeText: { fontSize: 13, color: "#9ca3af" },
  switchRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 8,
  },
  switchLabel: { fontSize: 13, color: "#e5e7eb" },
  helpText: { fontSize: 11, color: "#9ca3af", marginBottom: 12 },
  grid: {
    flexDirection: "row",
    flexWrap: "wrap",
    columnGap: 8,
    rowGap: 8,
  },
  field: {
    width: "31%",
  },
  fieldLabel: {
    fontSize: 13,
    color: "#9ca3af",
    marginBottom: 2,
  },
  fieldInput: {
    height: 40,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    paddingHorizontal: 8,
    color: "#e5e7eb",
    fontSize: 13,
    backgroundColor: "#020617",
  },
  saveBtn: {
    marginTop: 12,
    paddingVertical: 10,
    borderRadius: 8,
    backgroundColor: "#22c55e",
    alignItems: "center",
  },
  saveText: {
    fontSize: 14,
    fontWeight: "600",
    color: "#020617",
  },
});
