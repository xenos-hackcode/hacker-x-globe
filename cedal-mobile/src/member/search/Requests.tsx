// src/member/search/Requests.tsx
import { auth, db } from "@/src/api/firebase";
import type { UserProfile } from "@/src/hooks/useUserProfile";
import {
  collection,
  deleteDoc,
  doc,
  getDoc,
  onSnapshot,
  query,
  updateDoc,
  where,
} from "firebase/firestore";
import React, { useEffect, useState } from "react";
import {
  FlatList,
  Image,
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

type RequestStatus = "pending" | "accepted" | "declined";
type Direction = "outgoing" | "incoming";
type RequestFilter = "all" | "pending" | "accepted" | "declined";

type RequestItem = {
  id: string; // request doc id
  fromId: string;
  toId: string;
  status: RequestStatus;
  direction: Direction;
  // display fields
  name: string;
  email: string | null;
  avatar: string;
};

function buildRoomId(a: string, b: string) {
  return a < b ? `${a}_${b}` : `${b}_${a}`;
}

export default function Requests() {
  const [filter, setFilter] = useState<RequestFilter>("all");
  const [requests, setRequests] = useState<RequestItem[]>([]);

  useEffect(() => {
    const current = auth.currentUser;
    if (!current) return;

    const baseRef = collection(db, "friendRequests");

    const qOutgoing = query(baseRef, where("fromId", "==", current.uid));
    const qIncoming = query(baseRef, where("toId", "==", current.uid));

    const hydrate = async (
      reqId: string,
      data: any,
      direction: Direction
    ): Promise<RequestItem | null> => {
      const otherUserId =
        direction === "outgoing" ? data.toId : data.fromId;

      try {
        // if accepted and room already exists, hide it
        if (data.status === "accepted") {
          const roomId = buildRoomId(current.uid, otherUserId);
          const roomSnap = await getDoc(doc(db, "rooms", roomId));
          if (roomSnap.exists()) {
            return null;
          }
        }

        const userSnap = await getDoc(doc(db, "users", otherUserId));
        const u = userSnap.exists()
          ? (userSnap.data() as UserProfile | any)
          : ({} as any);

        const nickname = u.nickname || "";
        const email = u.email ?? null;

        return {
          id: reqId,
          fromId: data.fromId,
          toId: data.toId,
          status: data.status as RequestStatus,
          direction,
          name: nickname || email || "Unknown",
          email,
          avatar: u.avatarUrl || "",
        };
      } catch (e) {
        console.error("Error loading user for request", e);
        return null;
      }
    };

    const unsubOut = onSnapshot(qOutgoing, async (snap) => {
      const items = await Promise.all(
        snap.docs.map(async (d) => {
          const data = d.data();
          return hydrate(d.id, data, "outgoing");
        })
      );
      const cleaned = items.filter((x): x is RequestItem => !!x);
      setRequests((prev) => {
        const incomingOnly = prev.filter((r) => r.direction === "incoming");
        return [...incomingOnly, ...cleaned];
      });
    });

    const unsubIn = onSnapshot(qIncoming, async (snap) => {
      const items = await Promise.all(
        snap.docs.map(async (d) => {
          const data = d.data();
          return hydrate(d.id, data, "incoming");
        })
      );
      const cleaned = items.filter((x): x is RequestItem => !!x);
      setRequests((prev) => {
        const outgoingOnly = prev.filter((r) => r.direction === "outgoing");
        return [...outgoingOnly, ...cleaned];
      });
    });

    return () => {
      unsubOut();
      unsubIn();
    };
  }, []);

  const handleAccept = async (req: RequestItem) => {
    await updateDoc(doc(db, "friendRequests", req.id), {
      status: "accepted",
    });
  };

  const handleDecline = async (req: RequestItem) => {
    await updateDoc(doc(db, "friendRequests", req.id), {
      status: "declined",
    });
  };

  const handleCancel = async (req: RequestItem) => {
    await deleteDoc(doc(db, "friendRequests", req.id));
  };

  const filtered = requests.filter((r) =>
    filter === "all" ? true : r.status === filter
  );

  return (
    <View style={styles.container}>
      {/* Filter by row */}
      <View style={styles.filterLabelRow}>
        <Text style={styles.filterLabel}>Filter by</Text>
      </View>

      <ScrollView
        horizontal
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.filterScrollContent}
      >
        {(["all", "pending", "accepted", "declined"] as RequestFilter[]).map(
          (key) => {
            const label =
              key === "all"
                ? "All"
                : key === "pending"
                ? "Pending"
                : key === "accepted"
                ? "Accepted"
                : "Declined";
            const active = filter === key;
            return (
              <TouchableOpacity
                key={key}
                style={[styles.chip, active && styles.chipActive]}
                onPress={() => setFilter(key)}
              >
                <Text style={styles.chipText}>{label}</Text>
              </TouchableOpacity>
            );
          }
        )}
      </ScrollView>

      {filtered.length === 0 ? (
        <Text style={styles.emptyText}>No requests here yet.</Text>
      ) : (
        <FlatList
          data={filtered}
          keyExtractor={(item) => item.id}
          ItemSeparatorComponent={() => <View style={styles.separator} />}
          contentContainerStyle={{ paddingTop: 8, paddingBottom: 24 }}
          renderItem={({ item }) => (
            <View style={styles.row}>
              <Image source={{ uri: item.avatar }} style={styles.avatar} />
              <View style={styles.info}>
                <Text style={styles.name}>{item.name}</Text>
                {item.email && (
                  <Text style={styles.handle}>{item.email}</Text>
                )}
                <Text style={styles.meta}>
                  {item.direction === "outgoing" ? "You sent" : "To you"}
                </Text>
              </View>

              <View style={styles.actions}>
                {item.status === "pending" && (
                  <View style={{ alignItems: "flex-end" }}>
                    {item.direction === "incoming" && (
                      <TouchableOpacity
                        style={styles.acceptBtn}
                        onPress={() => handleAccept(item)}
                      >
                        <Text style={styles.acceptText}>Accept</Text>
                      </TouchableOpacity>
                    )}
                    <TouchableOpacity
                      style={[
                        styles.declineBtn,
                        item.direction === "incoming" && { marginTop: 4 },
                      ]}
                      onPress={() =>
                        item.direction === "incoming"
                          ? handleDecline(item) // receiver: Decline
                          : handleCancel(item)  // sender: Cancel
                      }
                    >
                      <Text style={styles.declineText}>
                        {item.direction === "incoming" ? "Decline" : "Cancel"}
                      </Text>
                    </TouchableOpacity>
                  </View>
                )}

                {item.status === "accepted" && (
                  <View style={styles.acceptedPill}>
                    <Text style={styles.acceptedText}>Accepted</Text>
                    <Text style={styles.subText}>
                      Leaves here after first chat
                    </Text>
                  </View>
                )}

                {item.status === "declined" && (
                  <View style={styles.declinedPill}>
                    <Text style={styles.declinedPillText}>Declined</Text>
                  </View>
                )}
              </View>
            </View>
          )}
        />
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: "#020617", paddingHorizontal: 16, paddingTop: 16 },

  filterLabelRow: {
    marginTop: 12,
    marginBottom: 4,
  },
  filterLabel: {
    fontSize: 12,
    color: "#9ca3af",
    textTransform: "uppercase",
    letterSpacing: 0.4,
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

  separator: {
    height: StyleSheet.hairlineWidth,
    backgroundColor: "rgba(31,41,55,0.9)",
    marginLeft: 52,
  },
  row: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 10,
  },
  avatar: {
    width: 34,
    height: 34,
    borderRadius: 17,
    marginRight: 10,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.7)",
  },
  info: { flex: 1 },
  name: {
    fontSize: 13,
    color: "#e5e7eb",
    fontWeight: "500",
  },
  handle: {
    fontSize: 11,
    color: "#9ca3af",
    marginTop: 2,
  },
  meta: {
    fontSize: 11,
    color: "#6b7280",
    marginTop: 2,
  },
  actions: { marginLeft: 8, alignItems: "flex-end" },
  acceptBtn: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(34,197,94,0.8)",
    paddingHorizontal: 12,
    paddingVertical: 4,
    backgroundColor: "#022c22",
  },
  acceptText: {
    fontSize: 11,
    color: "#bbf7d0",
    letterSpacing: 0.4,
    textTransform: "uppercase",
  },
  declineBtn: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(248,113,113,0.9)",
    paddingHorizontal: 10,
    paddingVertical: 3,
    backgroundColor: "#450a0a",
  },
  declineText: {
    fontSize: 10,
    color: "#fecaca",
    letterSpacing: 0.4,
    textTransform: "uppercase",
  },
  acceptedPill: {
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(34,197,94,0.9)",
    paddingHorizontal: 10,
    paddingVertical: 4,
    backgroundColor: "#022c22",
    alignItems: "flex-start",
  },
  acceptedText: {
    fontSize: 11,
    color: "#bbf7d0",
    letterSpacing: 0.4,
    textTransform: "uppercase",
  },
  subText: {
    fontSize: 9,
    color: "#9ca3af",
    marginTop: 2,
  },
  declinedPill: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(248,113,113,0.9)",
    paddingHorizontal: 10,
    paddingVertical: 4,
    backgroundColor: "#450a0a",
  },
  declinedPillText: {
    fontSize: 11,
    color: "#fecaca",
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
