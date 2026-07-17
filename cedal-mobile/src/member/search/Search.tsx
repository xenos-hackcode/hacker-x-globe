// src/member/search/Search.tsx
import { auth, db } from "@/src/api/firebase";
import type { UserProfile } from "@/src/hooks/useUserProfile";
import {
  addDoc,
  collection,
  doc,
  getDoc,
  getDocs,
  serverTimestamp,
} from "firebase/firestore";
import React, { useEffect, useState } from "react";
import {
  Alert,
  FlatList,
  Image,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";

type SearchUser = {
  id: string;
  name: string;
  email: string | null;
  avatar: string;
  occupation?: string;
  bio?: string;
  gender?: string;
  hobby?: string;
  age?: number;
};

function buildRoomId(a: string, b: string) {
  return a < b ? `${a}_${b}` : `${b}_${a}`;
}

export default function Search() {
  const [query, setQuery] = useState("");
  const [users, setUsers] = useState<SearchUser[]>([]);
  const [loading, setLoading] = useState(true);

  const [filterByGender, setFilterByGender] = useState(false);
  const [filterByOccupation, setFilterByOccupation] = useState(false);
  const [filterByHobby, setFilterByHobby] = useState(false);
  const [filterByAge, setFilterByAge] = useState(false);
  const [filterByBio, setFilterByBio] = useState(false);

  // load all users except current user and anyone we've already chatted with
  useEffect(() => {
    const load = async () => {
      try {
        const current = auth.currentUser;
        const currentUid = current?.uid;

        const snap = await getDocs(collection(db, "users"));
        const all: SearchUser[] = snap.docs.map((docSnap) => {
          const d = docSnap.data() as UserProfile | any;

          const nickname = d.nickname || "";
          const email = d.email ?? null;

          return {
            id: docSnap.id,
            name: nickname || email || "Unknown",
            email,
            avatar: d.avatarUrl || "",
            occupation: d.occupation,
            bio: d.bio,
            gender: d.gender,
            hobby: d.hobby,
            age: d.age,
          };
        });

        if (!currentUid) {
          setUsers(all);
          return;
        }

        const filtered: SearchUser[] = [];
        for (const u of all) {
          if (u.id === currentUid) continue;

          const roomId = buildRoomId(currentUid, u.id);
          const roomSnap = await getDoc(doc(db, "rooms", roomId));
          if (roomSnap.exists()) {
            // already have a chat with this user → skip from search
            continue;
          }

          filtered.push(u);
        }

        setUsers(filtered);
      } catch (e) {
        console.error("Error loading users", e);
      } finally {
        setLoading(false);
      }
    };

    load();
  }, []);

  // send friend request + remove from local list
  const handleAdd = async (target: SearchUser) => {
    const current = auth.currentUser;
    if (!current) {
      Alert.alert("Not signed in", "You must be signed in to send a request.");
      return;
    }

    const fromId = current.uid;
    const toId = target.id;

    try {
      await addDoc(collection(db, "friendRequests"), {
        fromId,
        toId,
        status: "pending",
        createdAt: serverTimestamp(),
      });

      // immediately remove this user from the search list
      setUsers((prev) => prev.filter((u) => u.id !== target.id));
    } catch (e) {
      console.error("Error sending request", e);
      Alert.alert("Error", "Could not send request. Please try again.");
    }
  };

  // SEARCH + FILTER LOGIC
  const data = users.filter((user) => {
    const q = query.trim().toLowerCase();

    const matchesText =
      !q ||
      (user.email ?? "").toLowerCase().includes(q) ||
      user.name.toLowerCase().includes(q) ||
      (user.occupation ?? "").toLowerCase().includes(q) ||
      (user.bio ?? "").toLowerCase().includes(q);

    if (!matchesText) return false;

    if (filterByOccupation && !user.occupation) return false;
    if (filterByGender && !user.gender) return false;
    if (filterByHobby && !user.hobby) return false;
    if (filterByAge && !user.age) return false;
    if (filterByBio && !user.bio) return false;

    return true;
  });

  return (
    <View style={styles.screen}>
      {/* Search input */}
      <View>
        <View style={styles.labelRow}>
          <Text style={styles.label}>Search</Text>
        </View>

        <View style={styles.box}>
          <TextInput
            value={query}
            onChangeText={setQuery}
            placeholder="Search by email, name or bio"
            placeholderTextColor="#6b7280"
            style={styles.input}
            autoCapitalize="none"
          />
        </View>
      </View>

      {/* Filter by row with horizontal scroll */}
      <View style={{ marginTop: 12 }}>
        <Text style={styles.filterLabel}>Filter by</Text>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.filterScrollContent}
        >
          <TouchableOpacity
            style={[styles.chip, filterByGender && styles.chipActive]}
            onPress={() => setFilterByGender((v) => !v)}
          >
            <Text style={styles.chipText}>Gender</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.chip, filterByOccupation && styles.chipActive]}
            onPress={() => setFilterByOccupation((v) => !v)}
          >
            <Text style={styles.chipText}>Occupation</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.chip, filterByHobby && styles.chipActive]}
            onPress={() => setFilterByHobby((v) => !v)}
          >
            <Text style={styles.chipText}>Hobby</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.chip, filterByAge && styles.chipActive]}
            onPress={() => setFilterByAge((v) => !v)}
          >
            <Text style={styles.chipText}>Age</Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[styles.chip, filterByBio && styles.chipActive]}
            onPress={() => setFilterByBio((v) => !v)}
          >
            <Text style={styles.chipText}>Bio</Text>
          </TouchableOpacity>
        </ScrollView>
      </View>

      <View style={styles.sectionHeader}>
        <Text style={styles.sectionTitle}>
          {query.trim() ? "Search results" : "Suggested for you"}
        </Text>
      </View>

      {loading ? (
        <Text style={styles.emptyText}>Loading users…</Text>
      ) : (
        <FlatList
          data={data}
          keyExtractor={(item) => item.id}
          contentContainerStyle={styles.listContent}
          ItemSeparatorComponent={() => <View style={styles.separator} />}
          renderItem={({ item }) => (
            <View style={styles.userRow}>
              <Image source={{ uri: item.avatar }} style={styles.userAvatar} />
              <View style={styles.userInfo}>
                <Text style={styles.userName}>{item.name}</Text>

                {item.email && (
                  <Text style={styles.userHandle}>{item.email}</Text>
                )}

                {item.occupation && (
                  <Text style={styles.userMeta}>{item.occupation}</Text>
                )}
              </View>
              <View style={styles.userAction}>
                <TouchableOpacity
                  style={styles.addBtn}
                  activeOpacity={0.8}
                  onPress={() => handleAdd(item)}
                >
                  <Text style={styles.addText}>Add</Text>
                </TouchableOpacity>
              </View>
            </View>
          )}
          ListEmptyComponent={
            <Text style={styles.emptyText}>No users found for that query.</Text>
          }
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#020617",
    paddingHorizontal: 16,
    paddingTop: 16,
  },
  labelRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 4,
  },
  label: {
    fontSize: 13,
    color: "#e5e7eb",
    fontWeight: "600",
  },
  box: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    paddingHorizontal: 14,
    paddingVertical: 8,
  },
  input: {
    color: "#e5e7eb",
    fontSize: 14,
    padding: 0,
  },
  filterLabel: {
    fontSize: 12,
    color: "#9ca3af",
    textTransform: "uppercase",
    letterSpacing: 0.4,
    marginBottom: 4,
  },
  filterScrollContent: {
    paddingVertical: 4,
    paddingRight: 8,
    columnGap: 8,
  },
  chip: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.8)",
    paddingHorizontal: 10,
    paddingVertical: 4,
    backgroundColor: "#020617",
  },
  chipActive: {
    backgroundColor: "#0f172a",
    borderColor: "#22c55e",
  },
  chipText: {
    fontSize: 11,
    color: "#e5e7eb",
    letterSpacing: 0.4,
    textTransform: "uppercase",
  },
  sectionHeader: {
    marginTop: 16,
    marginBottom: 4,
  },
  sectionTitle: {
    fontSize: 13,
    color: "#e5e7eb",
    fontWeight: "600",
  },
  listContent: {
    paddingBottom: 24,
  },
  separator: {
    height: StyleSheet.hairlineWidth,
    backgroundColor: "rgba(31,41,55,0.9)",
    marginLeft: 52,
  },
  userRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 10,
  },
  userAvatar: {
    width: 34,
    height: 34,
    borderRadius: 17,
    marginRight: 10,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.7)",
  },
  userInfo: { flex: 1 },
  userName: {
    fontSize: 13,
    color: "#e5e7eb",
    fontWeight: "500",
  },
  userHandle: {
    fontSize: 11,
    color: "#9ca3af",
    marginTop: 2,
  },
  userMeta: {
    fontSize: 11,
    color: "#6b7280",
    marginTop: 2,
  },
  userAction: { marginLeft: 8 },
  addBtn: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(34,197,94,0.8)",
    paddingHorizontal: 12,
    paddingVertical: 4,
    backgroundColor: "#022c22",
  },
  addText: {
    fontSize: 11,
    color: "#bbf7d0",
    letterSpacing: 0.4,
    textTransform: "uppercase",
  },
  emptyText: {
    marginTop: 24,
    fontSize: 12,
    color: "#6b7280",
    textAlign: "center",
  },
});
