// src/member/utils/ViewOnceConfigSheet.tsx
import React from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  Modal,
} from "react-native";

export type ViewOnceMode = "views" | "time" | "both";

export type ViewOnceConfig = {
  mode: ViewOnceMode;
  maxViews: number | null;         // null => unlimited
  timeLimitSeconds: number | null; // null => no time limit
};

type Props = {
  visible: boolean;
  value: ViewOnceConfig;
  onChange: (cfg: ViewOnceConfig) => void;
  onClose: () => void;
  onCancelViewOnce?: () => void;   // NEW: for Back button
};

const MODE_OPTIONS: Array<{ label: string; mode: ViewOnceMode }> = [
  { label: "Max views only", mode: "views" },
  { label: "Time limit only", mode: "time" },
  { label: "Views + time", mode: "both" },
];

const TIME_OPTIONS: Array<{ label: string; seconds: number | null }> = [
  { label: "No time limit", seconds: null },
  { label: "5 minutes", seconds: 5 * 60 },
  { label: "1 hour", seconds: 60 * 60 },
  { label: "24 hours", seconds: 24 * 60 * 60 },
];

export function ViewOnceConfigSheet({
  visible,
  value,
  onChange,
  onClose,
  onCancelViewOnce,
}: Props) {
  if (!visible) return null;

  const handleModeChange = (mode: ViewOnceMode) => {
    if (mode === "views") {
      onChange({
        ...value,
        mode,
        timeLimitSeconds: null,
      });
    } else if (mode === "time") {
      onChange({
        ...value,
        mode,
        maxViews: null,
      });
    } else {
      onChange({
        ...value,
        mode,
      });
    }
  };

  const handleMaxViewsInput = (text: string) => {
    const n = parseInt(text, 10);
    if (Number.isNaN(n) || n <= 0) {
      onChange({ ...value, maxViews: null });
    } else {
      onChange({ ...value, maxViews: n });
    }
  };

  const handleTimeCustomInput = (text: string) => {
    const n = parseInt(text, 10);
    if (Number.isNaN(n) || n <= 0) {
      onChange({ ...value, timeLimitSeconds: null });
    } else {
      onChange({ ...value, timeLimitSeconds: n * 60 });
    }
  };

  const maxViewsEnabled = value.mode === "views" || value.mode === "both";
  const timeLimitEnabled = value.mode === "time" || value.mode === "both";

  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={onClose}
    >
      <View style={styles.backdrop}>
        <View style={styles.panel}>
          {/* Header with Back + Title */}
          <View style={styles.headerRow}>
            <TouchableOpacity
              onPress={() => {
                onCancelViewOnce?.();
                onClose();
              }}
            >
              <Text style={styles.backText}>Back</Text>
            </TouchableOpacity>
            <Text style={styles.title}>View once options</Text>
            <View style={{ width: 40 }} />
          </View>

          {/* Mode selection */}
          <Text style={styles.sectionLabel}>Mode</Text>
          <View style={styles.chipRow}>
            {MODE_OPTIONS.map((opt) => {
              const active = opt.mode === value.mode;
              return (
                <TouchableOpacity
                  key={opt.mode}
                  style={[styles.chip, active && styles.chipActive]}
                  onPress={() => handleModeChange(opt.mode)}
                >
                  <Text
                    style={[
                      styles.chipText,
                      active && styles.chipTextActive,
                    ]}
                  >
                    {opt.label}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </View>

          {/* Max views */}
          <Text style={styles.sectionLabel}>Max views</Text>
          <View
            style={[
              styles.sectionBox,
              !maxViewsEnabled && styles.sectionDisabled,
            ]}
          >
            <Text style={styles.helperText}>
              {maxViewsEnabled
                ? "Set how many times this media can be opened."
                : "Enable a mode that includes views to edit."}
            </Text>

            <View style={styles.maxViewsRow}>
              <TextInput
                style={[
                  styles.numberInput,
                  !maxViewsEnabled && styles.inputDisabled,
                ]}
                editable={maxViewsEnabled}
                keyboardType="number-pad"
                placeholder="Custom"
                placeholderTextColor="#6b7280"
                value={
                  value.maxViews != null && value.maxViews > 0
                    ? String(value.maxViews)
                    : ""
                }
                onChangeText={handleMaxViewsInput}
              />
              <Text style={styles.maxViewsNote}>
                Empty = unlimited views
              </Text>
            </View>
          </View>

          {/* Time limit */}
          <Text style={styles.sectionLabel}>Time limit</Text>
          <View
            style={[
              styles.sectionBox,
              !timeLimitEnabled && styles.sectionDisabled,
            ]}
          >
            <Text style={styles.helperText}>
              {timeLimitEnabled
                ? "How long after first open it stays viewable."
                : "Enable a mode that includes time to edit."}
            </Text>

            <View style={styles.chipRow}>
              {TIME_OPTIONS.map((opt) => {
                const active = opt.seconds === value.timeLimitSeconds;
                return (
                  <TouchableOpacity
                    key={opt.label}
                    disabled={!timeLimitEnabled}
                    style={[
                      styles.chip,
                      active && styles.chipActive,
                      !timeLimitEnabled && styles.chipDisabled,
                    ]}
                    onPress={() =>
                      onChange({
                        ...value,
                        timeLimitSeconds: opt.seconds,
                      })
                    }
                  >
                    <Text
                      style={[
                        styles.chipText,
                        active && styles.chipTextActive,
                        !timeLimitEnabled && styles.chipTextDisabled,
                      ]}
                    >
                      {opt.label}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </View>

            <View style={styles.customTimeRow}>
              <TextInput
                style={[
                  styles.numberInput,
                  !timeLimitEnabled && styles.inputDisabled,
                ]}
                editable={timeLimitEnabled}
                keyboardType="number-pad"
                placeholder="Custom minutes"
                placeholderTextColor="#6b7280"
                onChangeText={handleTimeCustomInput}
              />
            </View>
          </View>

          <View style={styles.footerRow}>
            <TouchableOpacity onPress={onClose}>
              <Text style={styles.closeText}>Done</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    position: "absolute",
    left: 0,
    right: 0,
    bottom: 0,
    top: 0,
    backgroundColor: "rgba(15,23,42,0.8)",
    justifyContent: "flex-end",
  },
  panel: {
    backgroundColor: "#020617",
    padding: 14,
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.4)",
    height: "85%", // adjust as you like
  },
  headerRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 8,
  },
  backText: {
    fontSize: 13,
    color: "#f97373",
    fontWeight: "600",
  },
  title: {
    fontSize: 14,
    fontWeight: "600",
    color: "#e5e7eb",
  },
  sectionLabel: {
    fontSize: 11,
    color: "#9ca3af",
    marginTop: 8,
    marginBottom: 4,
  },
  sectionBox: {
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(30,64,175,0.8)",
    padding: 8,
    marginBottom: 6,
  },
  sectionDisabled: {
    opacity: 0.45,
  },
  helperText: {
    fontSize: 11,
    color: "#9ca3af",
    marginBottom: 6,
  },
  chipRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 6,
    marginBottom: 6,
  },
  chip: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
  },
  chipActive: {
    borderColor: "#22c55e",
    backgroundColor: "#022c22",
  },
  chipDisabled: {
    borderColor: "rgba(55,65,81,0.7)",
  },
  chipText: {
    fontSize: 11,
    color: "#9ca3af",
  },
  chipTextActive: {
    color: "#bbf7d0",
  },
  chipTextDisabled: {
    color: "#6b7280",
  },
  maxViewsRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 4,
    gap: 8,
  },
  numberInput: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
    paddingHorizontal: 10,
    paddingVertical: 4,
    minWidth: 80,
    fontSize: 12,
    color: "#e5e7eb",
  },
  inputDisabled: {
    borderColor: "rgba(55,65,81,0.8)",
    color: "#6b7280",
  },
  maxViewsNote: {
    fontSize: 10,
    color: "#9ca3af",
  },
  customTimeRow: {
    marginTop: 4,
  },
  footerRow: {
    marginTop: 10,
    alignItems: "flex-end",
  },
  closeText: {
    fontSize: 13,
    color: "#22c55e",
    fontWeight: "600",
  },
});
