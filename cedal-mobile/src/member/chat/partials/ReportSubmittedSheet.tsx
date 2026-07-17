// src/member/chat/partials/ReportSubmittedSheet.tsx
import React, { useMemo, useState } from "react";
import { View, Text, StyleSheet, ScrollView } from "react-native";

type Props = {
  state: any;
};

export function ReportSubmittedSheet({ state }: Props) {
  const {
    reportSubmittedSheetOpen,
    setReportSubmittedSheetOpen,
    reportsForChat,
    clearReportsForChat,
  } = state;

  const [showLastOnly, setShowLastOnly] = useState(true);

  if (!reportSubmittedSheetOpen) return null;

  const sortedReports = useMemo(() => {
    const base = Array.isArray(reportsForChat) ? [...reportsForChat] : [];
    base.sort((a: any, b: any) => {
      const ta = a.createdAt?.toMillis ? a.createdAt.toMillis() : a.createdAt;
      const tb = b.createdAt?.toMillis ? b.createdAt.toMillis() : b.createdAt;
      return (tb || 0) - (ta || 0); // newest first
    });
    return base;
  }, [reportsForChat]);

  const visibleReports =
    showLastOnly && sortedReports.length > 0
      ? [sortedReports[0]]
      : sortedReports;

  const formatTime = (r: any) => {
    const raw = r.createdAt;
    if (!raw) return "";
    const d = raw.toDate ? raw.toDate() : new Date(raw);
    return d.toLocaleString();
  };

  return (
    <View style={styles.overlay}>
      <View style={styles.sheet}>
        <View style={styles.headerRow}>
          <Text style={styles.title}>Your reports for this chat</Text>
          <Text
            style={styles.filter}
            onPress={() => setShowLastOnly((v) => !v)}
          >
            {showLastOnly ? "Show all" : "Show last"}
          </Text>
        </View>

        <ScrollView style={styles.list}>
          {visibleReports.map((r: any) => (
            <View key={r.id} style={styles.item}>
              <Text style={styles.time}>{formatTime(r)}</Text>

              <Text style={styles.label}>Reason</Text>
              <Text style={styles.value}>{r.reason}</Text>

              <Text style={styles.label}>Status</Text>
              <Text style={styles.value}>{r.status}</Text>

              {r.alexInitial?.summary && (
                <>
                  <Text style={styles.label}>Alex summary</Text>
                  <Text style={styles.value}>{r.alexInitial.summary}</Text>
                </>
              )}
            </View>
          ))}

          {visibleReports.length === 0 && (
            <Text style={styles.empty}>No reports yet.</Text>
          )}
        </ScrollView>

         <Text
          style={styles.clear}
          onPress={clearReportsForChat}
        >
          Clear from this device
        </Text>

        <Text
          style={styles.cancel}
          onPress={() => setReportSubmittedSheetOpen(false)}
        >
          Cancel
        </Text>
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
    backgroundColor: "rgba(0,0,0,0.7)",
    justifyContent: "flex-end",
    zIndex: 9999,
    elevation: 9999,
  },
  sheet: {
    padding: 16,
    backgroundColor: "#020617",
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    maxHeight: "60%",
  },
  clear: {
    marginTop: 8,
    textAlign: "center",
    color: "#9ca3af",
    fontSize: 13,
  },
  headerRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 4,
  },
  title: {
    fontSize: 16,
    fontWeight: "600",
    color: "#e5e7eb",
  },
  filter: {
    fontSize: 12,
    color: "#38bdf8",
  },
  list: {
    marginTop: 4,
  },
  item: {
    paddingVertical: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(148,163,184,0.3)",
  },
  time: {
    fontSize: 11,
    color: "#93c5fd",
    marginBottom: 4,
  },
  label: {
    fontSize: 11,
    color: "#9ca3af",
  },
  value: {
    fontSize: 13,
    color: "#e5e7eb",
    marginBottom: 4,
  },
  empty: {
    fontSize: 13,
    color: "#9ca3af",
    marginTop: 10,
  },
  cancel: {
    marginTop: 12,
    textAlign: "center",
    color: "#f97316",
    fontSize: 14,
    fontWeight: "500",
  },
});
