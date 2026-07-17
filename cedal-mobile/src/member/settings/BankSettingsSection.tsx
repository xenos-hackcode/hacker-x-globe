// src/member/settings/BankSettingsSection.tsx
import SettingsRow from "@/src/member/settings/SettingsRow";
import { useBankSettings } from "@/src/member/work/banking/BankSettingsContext";
import { useTheme } from "@/src/themes/ThemeContext";
import React from "react";
import { StyleSheet, Text, Vibration, View } from "react-native";

export default function BankSettingsSection() {
  const { colors } = useTheme();
  const {
    muteAllBankNotifications,
    setMuteAllBankNotifications,
    securityAlertsEnabled,
    setSecurityAlertsEnabled,
    largeSpendAlertsEnabled,
    setLargeSpendAlertsEnabled,
  } = useBankSettings();

  const largeSpendThreshold = 1000; // fixed threshold for now

  return (
    <>
      <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>
        Banking
      </Text>

      <View
        style={[
          styles.card,
          { borderColor: colors.border, backgroundColor: colors.background },
        ]}
      >
        {/* Mute all banking notifications */}
        <SettingsRow
          label="Mute banking notifications"
          description="Turn off trade and wallet alerts from the Banking Hub."
          value={muteAllBankNotifications}
          onValueChange={setMuteAllBankNotifications}
        />

        {/* Security alerts (vibration) */}
        <SettingsRow
          label="Security alerts"
          description="Vibrate on unusual sends or login activity."
          value={securityAlertsEnabled}
          onValueChange={(next) => {
            Vibration.vibrate(80);
            setSecurityAlertsEnabled(next);
          }}
        />

        {/* Large spend alerts */}
        <SettingsRow
          label="Large spend alerts"
          description={`Vibrate when a single send is above ${largeSpendThreshold} SC.`}
          value={largeSpendAlertsEnabled}
          onValueChange={setLargeSpendAlertsEnabled}
        />
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
});
