// app/(auth)/(member)/bank.tsx
import BankRouter from "@/src/member/work/banking/router/BankRouter";
import { useTheme } from "@/src/themes/ThemeContext";
import React from "react";
import { StyleSheet, View } from "react-native";

export default function BankScreen() {
  const { colors } = useTheme();

  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      <BankRouter />
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
});
