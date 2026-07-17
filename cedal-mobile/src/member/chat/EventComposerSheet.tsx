// src/member/chat/EventComposerSheet.tsx
import React, { useMemo, useState } from "react";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import {
  Modal,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
} from "react-native";

type CountdownDuration = {
  years: number;
  months: number;
  weeks: number;
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
};

type Props = {
  visible: boolean;
  onClose: () => void;
  onCreate: (payload: {
    title: string;
    startsAtText: string;
    location?: string;
    notes?: string;
    duration: CountdownDuration; // NEW: structured duration
  }) => void;
};

function parseIntSafe(v: string): number {
  const n = parseInt(v, 10);
  return Number.isFinite(n) && n > 0 ? n : 0;
}

export function EventComposerSheet({ visible, onClose, onCreate }: Props) {
  const insets = useSafeAreaInsets();
  const [title, setTitle] = useState("");
  const [location, setLocation] = useState("");
  const [notes, setNotes] = useState("");

  const [years, setYears] = useState("0");
  const [months, setMonths] = useState("0");
  const [weeks, setWeeks] = useState("0");
  const [days, setDays] = useState("0");
  const [hours, setHours] = useState("0");
  const [minutes, setMinutes] = useState("0");
  const [seconds, setSeconds] = useState("0");

  if (!visible) return null;

  const duration: CountdownDuration = useMemo(
    () => ({
      years: parseIntSafe(years),
      months: parseIntSafe(months),
      weeks: parseIntSafe(weeks),
      days: parseIntSafe(days),
      hours: parseIntSafe(hours),
      minutes: parseIntSafe(minutes),
      seconds: parseIntSafe(seconds),
    }),
    [years, months, weeks, days, hours, minutes, seconds],
  );

  const totalMs = useMemo(() => {
    const dayMs = 24 * 60 * 60 * 1000;
    const totalDays =
      duration.years * 365 +
      duration.months * 30 +
      duration.weeks * 7 +
      duration.days;

    return (
      totalDays * dayMs +
      duration.hours * 60 * 60 * 1000 +
      duration.minutes * 60 * 1000 +
      duration.seconds * 1000
    );
  }, [duration]);

  const hasDuration = totalMs > 0;
  const canSend = title.trim().length > 0 && hasDuration;

  const durationPreview = useMemo(() => {
    if (!hasDuration) return "No time set yet";

    const parts: string[] = [];
    const push = (n: number, label: string) => {
      if (n > 0) parts.push(`${n} ${label}`);
    };

    push(duration.years, "yr");
    push(duration.months, "mo");
    push(duration.weeks, "wk");
    push(duration.days, "day");
    push(duration.hours, "hr");
    push(duration.minutes, "min");
    push(duration.seconds, "sec");

    return `Triggers in ${parts.join(" ")}`;
  }, [duration, hasDuration]);

  function handleSend() {
    if (!canSend) return;

    const pretty = durationPreview.replace(/^Triggers in\s+/i, "");
    onCreate({
      title: title.trim(),
      startsAtText: pretty, // keep a human label for the message
      location: location.trim() || undefined,
      notes: notes.trim() || undefined,
      duration,
    });

    setTitle("");
    setLocation("");
    setNotes("");
    setYears("0");
    setMonths("0");
    setWeeks("0");
    setDays("0");
    setHours("0");
    setMinutes("0");
    setSeconds("0");
    onClose();
  }

  return (
    <Modal
      transparent
      animationType="slide"
      visible={visible}
      onRequestClose={onClose}
    >
      <View style={styles.backdrop}>
        <View
          style={[
            styles.sheet,
            { paddingBottom: 20 + insets.bottom },
          ]}
        >
          <View style={styles.headerRow}>
            <Text style={styles.title}>Create timer event</Text>
            <TouchableOpacity style={styles.closeBtn} onPress={onClose}>
              <Text style={styles.closeText}>Close</Text>
            </TouchableOpacity>
          </View>

          {/* Title */}
          <Text style={styles.label}>Title</Text>
          <View style={styles.inputBox}>
            <TextInput
              value={title}
              onChangeText={setTitle}
              placeholder="Eg. Take medicine"
              placeholderTextColor="#6b7280"
              style={styles.input}
            />
          </View>

          {/* Duration grid */}
          <Text style={[styles.label, { marginTop: 10 }]}>
            Countdown (year → sec)
          </Text>
          <View style={styles.durationGrid}>
            <DurationField
              label="Years"
              value={years}
              onChange={setYears}
            />
            <DurationField
              label="Months"
              value={months}
              onChange={setMonths}
            />
            <DurationField
              label="Weeks"
              value={weeks}
              onChange={setWeeks}
            />
            <DurationField
              label="Days"
              value={days}
              onChange={setDays}
            />
            <DurationField
              label="Hours"
              value={hours}
              onChange={setHours}
            />
            <DurationField
              label="Minutes"
              value={minutes}
              onChange={setMinutes}
            />
            <DurationField
              label="Seconds"
              value={seconds}
              onChange={setSeconds}
            />
          </View>

          {/* Live preview */}
          <Text style={styles.previewText}>{durationPreview}</Text>

          {/* Location */}
          <Text style={[styles.label, { marginTop: 10 }]}>
            Location (optional)
          </Text>
          <View style={styles.inputBox}>
            <TextInput
              value={location}
              onChangeText={setLocation}
              placeholder="Eg. Bedroom, Discord VC"
              placeholderTextColor="#6b7280"
              style={styles.input}
            />
          </View>

          {/* Notes */}
          <Text style={[styles.label, { marginTop: 10 }]}>
            Notes (optional)
          </Text>
          <View style={[styles.inputBox, { height: 70 }]}>
            <TextInput
              value={notes}
              onChangeText={setNotes}
              placeholder="Any extra details…"
              placeholderTextColor="#6b7280"
              style={[styles.input, { flex: 1 }]}
              multiline
            />
          </View>

          {/* Send */}
          <TouchableOpacity
            style={[styles.sendBtn, !canSend && styles.sendBtnDisabled]}
            disabled={!canSend}
            activeOpacity={0.8}
            onPress={handleSend}
          >
            <Text style={styles.sendText}>Set timer</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
}

type DurationFieldProps = {
  label: string;
  value: string;
  onChange: (v: string) => void;
};

function DurationField({ label, value, onChange }: DurationFieldProps) {
  return (
    <View style={styles.durationCell}>
      <Text style={styles.durationLabel}>{label}</Text>
      <View style={styles.durationInputBox}>
        <TextInput
          value={value}
          onChangeText={onChange}
          keyboardType="numeric"
          placeholder="0"
          placeholderTextColor="#6b7280"
          style={styles.durationInput}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    justifyContent: "flex-end",
    backgroundColor: "rgba(15,23,42,0.75)",
  },
  sheet: {
    backgroundColor: "#020617",
    borderTopLeftRadius: 18,
    borderTopRightRadius: 18,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.4)",
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: 20,
  },
  headerRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 8,
  },
  title: { fontSize: 14, color: "#e5e7eb", fontWeight: "700" },
  closeBtn: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.8)",
  },
  closeText: {
    fontSize: 11,
    color: "#9ca3af",
    letterSpacing: 1,
    textTransform: "uppercase",
  },
  label: { fontSize: 12, color: "#9ca3af", marginBottom: 4 },
  inputBox: {
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  input: { fontSize: 14, color: "#e5e7eb" },
  durationGrid: {
    marginTop: 4,
    flexDirection: "row",
    flexWrap: "wrap",
    justifyContent: "space-between",
  },
  durationCell: {
    width: "30%",
    marginBottom: 8,
  },
  durationLabel: {
    fontSize: 10,
    color: "#9ca3af",
    marginBottom: 2,
    textTransform: "uppercase",
    letterSpacing: 0.6,
  },
  durationInputBox: {
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  durationInput: {
    fontSize: 13,
    color: "#e5e7eb",
  },
  previewText: {
    marginTop: 4,
    fontSize: 11,
    color: "#a5b4fc",
  },
  sendBtn: {
    marginTop: 14,
    alignSelf: "flex-end",
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "#22c55e",
  },
  sendBtnDisabled: { backgroundColor: "#1f2937" },
  sendText: {
    fontSize: 12,
    color: "#022c22",
    fontWeight: "700",
    textTransform: "uppercase",
    letterSpacing: 0.8,
  },
});
