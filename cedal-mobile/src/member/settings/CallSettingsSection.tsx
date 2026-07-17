// src/member/settings/CallSettingsSection.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import React, { useState } from "react";
import { StyleSheet, Text, TouchableOpacity, View } from "react-native";
import SettingsRow from "./SettingsRow";

const REGIONS = ["Auto", "EU-West", "US-East", "Asia-Pacific"];

export default function CallSettingsSection() {
  const { colors } = useTheme();

  const [aiModeUnsafe, setAiModeUnsafe] = useState(false);
  const [regionIndex, setRegionIndex] = useState(0);

  const currentRegion = REGIONS[regionIndex];

  function cycleRegion() {
    setRegionIndex((prev) => (prev + 1) % REGIONS.length);
  }

  return (
    <>
      <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>
        Call
      </Text>

      <View
        style={[
          styles.card,
          { borderColor: colors.border, backgroundColor: colors.background },
        ]}
      >
        {/* AI mode (less safe) */}
        <SettingsRow
          label="AI mode"
          description={
            aiModeUnsafe
              ? "Reduced IP tracking & noise filters. Not safe; expect unstable calls."
              : "Standard call safety. Turn on to experiment with looser AI call settings."
          }
          value={aiModeUnsafe}
          onValueChange={setAiModeUnsafe}
        />

        {/* Region VPN */}
        <View style={[styles.row, styles.rowLast]}>
          <View style={styles.rowTextBlock}>
            <Text style={[styles.rowLabel, { color: colors.textPrimary }]}>
              Region VPN
            </Text>
            <Text
              style={[
                styles.rowDescription,
                { color: colors.textSecondary },
              ]}
            >
              Switch between regional signal hubs if your current region is bad.
            </Text>
            <TouchableOpacity
              onPress={cycleRegion}
              activeOpacity={0.8}
              style={[
                styles.regionPill,
                { borderColor: colors.border },
              ]}
            >
              <Text style={[styles.regionText, { color: colors.textPrimary }]}>
                {currentRegion}
              </Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </>
  );
}

const styles = StyleSheet.create({
  sectionLabel: {
    fontSize: 11,
    letterSpacing: 2,
    textTransform: "uppercase",
    marginBottom: 6,
  },
  card: {
    borderRadius: 18,
    borderWidth: 1,
    marginBottom: 18,
    overflow: "hidden",
  },
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
  regionPill: {
    marginTop: 8,
    alignSelf: "flex-start",
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 999,
    borderWidth: 1,
    backgroundColor: "#020617",
  },
  regionText: {
    fontSize: 11,
    letterSpacing: 1,
  },
});
