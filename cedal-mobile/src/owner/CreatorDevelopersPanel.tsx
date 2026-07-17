// src/owner/CreatorDevelopersPanel.tsx
import React, { useEffect, useState, useMemo } from "react";
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  ActivityIndicator,
  TextInput,
  TouchableOpacity,
} from "react-native";
import { useRouter } from "expo-router";
import { collection, getDocs } from "firebase/firestore";
import { db } from "@/src/api/firebase";

type DeveloperUser = {
  id: string;
  displayName?: string | null;
  username?: string | null;
  email?: string | null;
  role?: string;
};

export function CreatorDevelopersPanel() {
  const router = useRouter();
  const [loading, setLoading] = useState(true);
  const [developers, setDevelopers] = useState<DeveloperUser[]>([]);
  const [search, setSearch] = useState("");

  useEffect(() => {
    let cancelled = false;

    async function loadDevelopers() {
      try {
        setLoading(true);

        // simple: load all users, then filter devs/owners in memory
        const snap = await getDocs(collection(db, "users"));
        if (cancelled) return;

        const items: DeveloperUser[] = snap.docs
          .map((docSnap) => {
            const data = docSnap.data() as any;
            return {
              id: docSnap.id,
              displayName:
                data.displayName ?? data.nickname ?? data.email ?? null,
              username: data.username ?? null,
              email: data.email ?? null,
              role: data.role ?? "user",
            };
          })
          // only users who have ever been dev: role === "developer" or "owner"
          .filter(
            (u) => u.role === "developer" || u.role === "owner"
          );

        setDevelopers(items);
      } catch (e) {
        console.error("Failed to load developers", e);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadDevelopers();
    return () => {
      cancelled = true;
    };
  }, []);

  const filtered = useMemo(() => {
    const term = search.trim().toLowerCase();
    return developers.filter((u) => {
      if (!term) return true;
      const name = (u.displayName || "").toLowerCase();
      const username = (u.username || "").toLowerCase();
      const email = (u.email || "").toLowerCase();

      return (
        name.includes(term) ||
        username.includes(term) ||
        email.includes(term)
      );
    });
  }, [developers, search]);

  const renderItem = ({ item }: { item: DeveloperUser }) => (
    <TouchableOpacity
      style={styles.row}
      onPress={() => {
        // later: detail view
        // router.push({ pathname: "/(auth)/(owner)/developer-detail", params: { uid: item.id } });
      }}
    >
      <View style={styles.avatar}>
        <Text style={styles.avatarText}>
          {(item.displayName || item.username || "?")
            .charAt(0)
            .toUpperCase()}
        </Text>
      </View>
      <View style={styles.rowText}>
        <Text style={styles.nameText}>
          {item.displayName || item.username || "Unknown developer"}
        </Text>
        {item.username ? (
          <Text style={styles.metaText}>@{item.username}</Text>
        ) : item.email ? (
          <Text style={styles.metaText}>{item.email}</Text>
        ) : null}
      </View>
      {(item.role === "developer" || item.role === "owner") && (
        <View style={styles.badge}>
          <Text style={styles.badgeText}>
            {item.role === "owner" ? "OWNER" : "DEV"}
          </Text>
        </View>
      )}
    </TouchableOpacity>
  );

  return (
    <View style={styles.root}>
      <View style={styles.header}>
        <Text style={styles.title}>Creator / Developers</Text>
        <Text style={styles.subtitle}>
          Everyone who has ever unlocked developer mode (including owners).
        </Text>
      </View>

      <View style={styles.controls}>
        <TextInput
          value={search}
          onChangeText={setSearch}
          placeholder="Search by name, username, or email"
          placeholderTextColor="#6b7280"
          style={styles.searchInput}
        />
      </View>

      {loading ? (
        <View style={styles.loadingBlock}>
          <ActivityIndicator color="#22d3ee" />
          <Text style={styles.loadingText}>Loading developers…</Text>
        </View>
      ) : (
        <FlatList
          data={filtered}
          keyExtractor={(item) => item.id}
          contentContainerStyle={
            filtered.length === 0 ? styles.emptyList : undefined
          }
          renderItem={renderItem}
          ListEmptyComponent={
            <Text style={styles.emptyText}>
              No developers found yet. Unlock dev mode at least once to appear here.
            </Text>
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: "#020617",
    paddingHorizontal: 16,
    paddingTop: 32,
  },
  header: {
    marginBottom: 16,
  },
  title: {
    color: "#e5e7eb",
    fontSize: 18,
    fontWeight: "700",
    letterSpacing: 2,
    textTransform: "uppercase",
  },
  subtitle: {
    color: "#9ca3af",
    fontSize: 12,
    marginTop: 4,
  },
  controls: {
    marginBottom: 12,
  },
  searchInput: {
    backgroundColor: "#020617",
    borderWidth: 1,
    borderColor: "#374151",
    borderRadius: 999,
    paddingHorizontal: 14,
    paddingVertical: 8,
    color: "#e5e7eb",
    fontSize: 13,
  },
  loadingBlock: {
    marginTop: 32,
    alignItems: "center",
  },
  loadingText: {
    marginTop: 8,
    color: "#9ca3af",
    fontSize: 12,
  },
  row: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "#1f2937",
  },
  avatar: {
    width: 32,
    height: 32,
    borderRadius: 999,
    backgroundColor: "#0f172a",
    borderWidth: 1,
    borderColor: "#374151",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 10,
  },
  avatarText: {
    color: "#e5e7eb",
    fontSize: 14,
    fontWeight: "600",
  },
  rowText: {
    flex: 1,
  },
  nameText: {
    color: "#e5e7eb",
    fontSize: 14,
    fontWeight: "500",
  },
  metaText: {
    color: "#6b7280",
    fontSize: 11,
    marginTop: 2,
  },
  badge: {
    borderRadius: 999,
    backgroundColor: "rgba(34,211,238,0.08)",
    borderWidth: 1,
    borderColor: "#22d3ee",
    paddingHorizontal: 8,
    paddingVertical: 2,
  },
  badgeText: {
    color: "#22d3ee",
    fontSize: 9,
    letterSpacing: 1.4,
  },
  emptyList: {
    flexGrow: 1,
    justifyContent: "center",
    alignItems: "center",
  },
  emptyText: {
    color: "#6b7280",
    fontSize: 12,
  },
});
