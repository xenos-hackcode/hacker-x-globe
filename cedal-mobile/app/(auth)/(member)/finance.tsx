// app/(auth)/(member)/finance.tsx
import FinanceRouter from "@/src/member/work/finance/router/FinanceRouter";
import { useTheme } from "@/src/themes/ThemeContext";
import React from "react";
import { StyleSheet, View } from "react-native";

export default function FinanceScreen() {
  const { colors } = useTheme();

  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      <FinanceRouter />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
});
