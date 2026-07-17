import React, { useState, useEffect } from "react";
import { View, Text, TouchableOpacity, StyleSheet } from "react-native";
import { router } from "expo-router";
import Search from "@/src/member/search/Search";
import Requests from "@/src/member/search/Requests";
import { auth, db } from "@/src/api/firebase";
import { collection, onSnapshot, query, where } from "firebase/firestore";

type TopTab = "search" | "requests";

export default function SearchAndRequestsRoute() {
  const [activeTab, setActiveTab] = useState<TopTab>("search");
  const [hasRequests, setHasRequests] = useState(false);

  useEffect(() => {
    const current = auth.currentUser;
    if (!current) return;

    const baseRef = collection(db, "friendRequests");
    const q = query(
      baseRef,
      where("toId", "==", current.uid),
    );

    const unsub = onSnapshot(q, (snap) => {
      setHasRequests(!snap.empty);
    });

    return () => unsub();
  }, []);

  function handleBack() {
    router.back();
  }

  return (
    <View style={styles.screen}>
      <View className="flex-row items-center mb-3" style={styles.topBar}>
        <TouchableOpacity
          onPress={handleBack}
          activeOpacity={0.7}
          style={styles.backBtn}
        >
          <Text style={styles.backText}>Back</Text>
        </TouchableOpacity>

        <View style={styles.tabRow}>
          <TouchableOpacity
            style={[
              styles.tabBtn,
              activeTab === "search" && styles.tabBtnActive,
            ]}
            activeOpacity={0.8}
            onPress={() => setActiveTab("search")}
          >
            <Text
              style={[
                styles.tabText,
                activeTab === "search" && styles.tabTextActive,
              ]}
            >
              Search
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[
              styles.tabBtn,
              activeTab === "requests" && styles.tabBtnActive,
            ]}
            activeOpacity={0.8}
            onPress={() => setActiveTab("requests")}
          >
            <View style={{ flexDirection: "row", alignItems: "center" }}>
              <Text
                style={[
                  styles.tabText,
                  activeTab === "requests" && styles.tabTextActive,
                ]}
              >
                Requests
              </Text>
              {hasRequests && (
                <View style={styles.dot} />
              )}
            </View>
          </TouchableOpacity>
        </View>
      </View>

      <View style={styles.body}>
        {activeTab === "search" ? <Search /> : <Requests />}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#020617",
    paddingTop: 32,
    paddingHorizontal: 16,
  },
  topBar: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 12,
  },
  backBtn: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
    marginRight: 12,
  },
  backText: {
    color: "#e5e7eb",
    fontSize: 13,
    letterSpacing: 1.5,
  },
  tabRow: {
    flexDirection: "row",
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    padding: 2,
    flex: 1,
    maxWidth: 230,
  },
  tabBtn: {
    flex: 1,
    paddingVertical: 4,
    borderRadius: 999,
    alignItems: "center",
    justifyContent: "center",
  },
  tabBtnActive: {
    backgroundColor: "#0f172a",
  },
  tabText: {
    fontSize: 12,
    color: "#9ca3af",
    textTransform: "uppercase",
    letterSpacing: 0.8,
  },
  tabTextActive: {
    color: "#e5e7eb",
  },
  dot: {
    width: 7,
    height: 7,
    borderRadius: 3.5,
    backgroundColor: "#f97373",
    marginLeft: 6,
  },
  body: {
    flex: 1,
  },
});
