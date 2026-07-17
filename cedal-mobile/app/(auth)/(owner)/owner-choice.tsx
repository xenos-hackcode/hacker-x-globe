// app/(auth)/(owner)/owner-choice.tsx
import { View, Text, TouchableOpacity, StyleSheet } from "react-native";
import { router } from "expo-router";

export default function OwnerChoiceScreen() {
  return (
    <View style={styles.root}>
      <Text style={styles.title}>Where do you want to go today?</Text>

      <TouchableOpacity
        style={styles.btnPrimary}
        onPress={() => router.replace("/(dev)/workbench")}
      >
        <Text style={styles.btnText}>Developer area</Text>
      </TouchableOpacity>

      <TouchableOpacity
        style={styles.btnSecondary}
        // send owners through the owner loading pipeline
        onPress={() => router.replace("/(auth)/(owner)/loading-owner")}
      >
        <Text style={styles.btnSecondaryText}>Member experience</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: "#020617", justifyContent: "center", padding: 24 },
  title: { color: "#e5e7eb", fontSize: 20, fontWeight: "600", textAlign: "center", marginBottom: 32 },
  btnPrimary: {
    borderRadius: 999,
    paddingVertical: 14,
    alignItems: "center",
    backgroundColor: "#22d3ee",
    marginBottom: 12,
  },
  btnText: { color: "#020617", fontWeight: "700", letterSpacing: 1.5 },
  btnSecondary: {
    borderRadius: 999,
    paddingVertical: 14,
    alignItems: "center",
    borderWidth: 1,
    borderColor: "#64748b",
  },
  btnSecondaryText: { color: "#e5e7eb", letterSpacing: 1.5 },
});
