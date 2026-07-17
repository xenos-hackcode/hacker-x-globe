// src/member/settings/SettingsRow.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import React from "react";
import { StyleSheet, Switch, Text, View } from "react-native";

type Props = {
  label: string;
  description: string;
  value: boolean;
  onValueChange: (val: boolean) => void;
  last?: boolean;
};

export default function SettingsRow({
  label,
  description,
  value,
  onValueChange,
  last,
}: Props) {
  const { colors } = useTheme();

  return (
    <View style={[styles.row, last && styles.rowLast]}>
      <View style={styles.rowTextBlock}>
        <Text style={[styles.rowLabel, { color: colors.textPrimary }]}>
          {label}
        </Text>
        <Text
          style={[styles.rowDescription, { color: colors.textSecondary }]}
        >
          {description}
        </Text>
      </View>
      <Switch
        value={value}
        onValueChange={onValueChange}
        thumbColor={value ? "#22c55e" : "#f97373"}
        trackColor={{ true: "#065f46", false: "#111827" }}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(31,41,55,0.9)",
  },
  rowLast: {
    borderBottomWidth: 0,
  },
  rowTextBlock: {
    flex: 1,
    marginRight: 10,
  },
  rowLabel: {
    fontSize: 13,
    fontWeight: "500",
  },
  rowDescription: {
    fontSize: 11,
    marginTop: 2,
  },
});
