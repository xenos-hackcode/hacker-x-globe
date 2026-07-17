// app/(auth)/(owner)/home.tsx
import React from "react";
import { View, Text, TouchableOpacity, StyleSheet } from "react-native";
import { useRouter } from "expo-router";
import { logout } from "@/src/api/auth";

export default function OwnerHome() {
  const router = useRouter();

  async function handleSignOut() {
    await logout();
    router.replace("/(auth)/sign-in");
  }

  return (
    <View style={styles.root}>
      <Text style={styles.title}>OWNER HOME</Text>
      {/* owner-only UI here */}
      <TouchableOpacity onPress={handleSignOut} style={styles.btn}>
        <Text style={styles.btnText}>Sign out</Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: "#020617", alignItems: "center", justifyContent: "center" },
  title: { color: "#e5e7eb" },
  btn: { marginTop: 16, paddingHorizontal: 16, paddingVertical: 10, borderRadius: 999, borderWidth: 1, borderColor: "rgba(148,163,184,0.6)" },
  btnText: { color: "#e5e7eb" },
});
