// app/(auth)/(member)/fun.tsx
import ChatListBottom from "@/src/member/chat/ChatListBottom";
import FunPicker, { FunMode } from "@/src/member/fun/FunPicker";
import GameCreate from "@/src/member/fun/games/GameCreate";
import GameDescript from "@/src/member/fun/games/GameDescript";
import GameHub from "@/src/member/fun/games/GameHubNative";
import { useTheme } from "@/src/themes/ThemeContext";
import { useRouter } from "expo-router";
import React, { useEffect, useState } from "react";
import { ScrollView, StyleSheet, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

import {
  createGroupForUser,
  joinGroupByInviteCode,
  leaveGroupWithCooldown,
} from "@/src/api/groups";
import { useUserProfile } from "@/src/hooks/useUserProfile";
import CreateGroupScreen from "@/src/member/fun/groups/CreateGroupScreen";
import GroupChatScreen from "@/src/member/fun/groups/GroupChatScreen";
import GroupDetailsPanel from "@/src/member/fun/groups/GroupDetailsPanel";
import GroupJoinPanel from "@/src/member/fun/groups/GroupJoinPanel";
import GroupsHub, {
  Group as FunGroup,
} from "@/src/member/fun/groups/GroupsHub";

import { auth, db } from "@/src/api/firebase";
import {
  collection,
  doc,
  getDoc,
  onSnapshot,
  orderBy,
  query,
} from "firebase/firestore";

type GroupRoute =
  | { name: "hub" }
  | { name: "details"; group: FunGroup }
  | { name: "join"; mode: "join" | "request"; group: FunGroup }
  | { name: "create" }
  | { name: "chat"; group: FunGroup };

export default function FunScreen() {
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();
  const { user, profile } = useUserProfile();
  const router = useRouter();

  const [mode, setMode] = useState<FunMode | null>(null);

  // games
  const [selectedGame, setSelectedGame] = useState<any | null>(null);
  const [creatingGame, setCreatingGame] = useState(false);

  // groups
  const [groups, setGroups] = useState<FunGroup[]>([]);
  const [groupRoute, setGroupRoute] = useState<GroupRoute>({ name: "hub" });

  // Live Firestore listener for groups
  useEffect(() => {
    const qGroups = query(collection(db, "groups"), orderBy("createdAt", "desc"));
const unsub = onSnapshot(qGroups, (snap) => {
  const items: FunGroup[] = [];
  snap.forEach((docSnap) => {
    const d = docSnap.data() as any;
    items.push({
      id: docSnap.id,
      name: d.name,
      description: d.description,
      tags: d.tags ?? [],
      region: d.serverId ?? "EU",
      isPrivate: d.isPrivate ?? false,
      membersCount: d.membersCount ?? 0,
      members: d.members ?? [],
      ownerId: d.ownerId,
      inviteCode: d.inviteCode, // <- here
    });
  });
  setGroups(items);
});

    return () => unsub();
  }, []);

  // decide what to do when user presses "Enter chat" from details
  const enterGroupFromDetails = async (g: FunGroup) => {
    const uid = auth.currentUser?.uid;
    if (!uid) {
      return;
    }

    const snap = await getDoc(doc(db, "groups", g.id));
    const data = snap.data() as any | undefined;
    const members: string[] = data?.members ?? [];
    const isMember = members.includes(uid);

    if (g.isPrivate && !isMember) {
      setGroupRoute({ name: "join", mode: "join", group: g });
    } else {
      setGroupRoute({ name: "chat", group: g });
    }
  };

  return (
    <View
      style={[
        styles.screen,
        {
          backgroundColor: colors.background,
          paddingBottom: insets.bottom,
        },
      ]}
    >
      <ScrollView contentContainerStyle={styles.content}>
        <FunPicker selectedMode={mode} onSelectMode={setMode} />
      </ScrollView>

      <ChatListBottom active="fun" />

      {/* GAMES ROUTER */}
      {mode === "games" && (
        <View style={styles.routerRoot}>
          {selectedGame === null && !creatingGame && (
            <GameHub
              onClose={() => {
                setSelectedGame(null);
                setCreatingGame(false);
                setMode(null);
              }}
              onSelectGame={(g) => setSelectedGame(g)}
              onCreateGame={() => setCreatingGame(true)}
            />
          )}

          {selectedGame && !creatingGame && (
            <View style={styles.routerScreen}>
              <GameDescript
                game={selectedGame}
                onClose={() => setSelectedGame(null)}
              />
            </View>
          )}

          {creatingGame && (
            <View style={styles.routerScreen}>
              <GameCreate onClose={() => setCreatingGame(false)} />
            </View>
          )}
        </View>
      )}

      {/* GROUPS ROUTER */}
      {mode === "group" && (
        <View style={styles.routerRoot}>
          {groupRoute.name === "hub" && (
            <GroupsHub
              groups={groups}
              currentUserId={user?.uid ?? ""}
              onClose={() => {
                setMode(null);
                setGroupRoute({ name: "hub" });
              }}
              onOpenGroup={(g) => setGroupRoute({ name: "details", group: g })}
              onCreateGroup={() => setGroupRoute({ name: "create" })}
              onOpenSettings={() => router.push("/(auth)/(member)/settings")}
            />
          )}

          {groupRoute.name === "details" && (
            <View style={styles.routerScreen}>
              <GroupDetailsPanel
                group={{
                  id: groupRoute.group.id,
                  name: groupRoute.group.name,
                  description: groupRoute.group.description,
                  tags: groupRoute.group.tags,
                  region: groupRoute.group.region,
                  isPrivate: groupRoute.group.isPrivate,
                  members: groupRoute.group.members ?? [],
                }}
                isOwner={groupRoute.group.ownerId === user?.uid}
                onClose={() => setGroupRoute({ name: "hub" })}
                onEnterChat={() => enterGroupFromDetails(groupRoute.group)}
                onConfirmLeave={async () => {
                  if (!user?.uid) return;
                  await leaveGroupWithCooldown(
                    user.uid,
                    groupRoute.group.id,
                    groupRoute.group.ownerId === user.uid,
                  );
                  setGroupRoute({ name: "hub" });
                }}
              />
            </View>
          )}

          {groupRoute.name === "join" && (
            <View style={styles.routerScreen}>
              <GroupJoinPanel
                mode={groupRoute.mode}
                group={groupRoute.group}
                requireInviteCode={groupRoute.group.isPrivate === true}
                onCancel={() =>
                  setGroupRoute({ name: "details", group: groupRoute.group })
                }
                onSubmit={async ({ code, reason }) => {
                  const uid = auth.currentUser?.uid;
                  if (!uid) throw new Error("You must be signed in.");

                  if (groupRoute.group.isPrivate) {
                    if (!code) throw new Error("Invite code required.");
                    await joinGroupByInviteCode(uid, code);
                  }

                  // TODO: optionally store `reason`
                  setGroupRoute({ name: "chat", group: groupRoute.group });
                }}
              />
            </View>
          )}

          {groupRoute.name === "create" && (
            <View style={styles.routerScreen}>
              <CreateGroupScreen
                currentUser={
                  user
                    ? {
                        uid: user.uid,
                        nextGroupCreateAt: profile?.nextGroupCreateAt ?? null,
                      }
                    : null
                }
                onCancel={() => setGroupRoute({ name: "hub" })}
                onCreate={async (payload) => {
                  if (!user?.uid) {
                    throw new Error("User not signed in");
                  }

                  await createGroupForUser(user.uid, {
                    name: payload.name,
                    description: payload.description,
                    tags: payload.tags,
                    isPrivate: payload.isPrivate,
                    serverId: payload.serverId,
                    inviteCode: payload.inviteCode,
                  });

                  setGroupRoute({ name: "hub" });
                }}
              />
            </View>
          )}

          {groupRoute.name === "chat" && (
            <View style={styles.routerScreen}>
              <GroupChatScreen
                groupId={groupRoute.group.id}
                groupName={groupRoute.group.name}
                onBack={() =>
                  setGroupRoute({ name: "details", group: groupRoute.group })
                }
              />
            </View>
          )}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { padding: 16, paddingBottom: 80 },
  routerRoot: {
    position: "absolute",
    inset: 0,
    backgroundColor: "#020617",
  },
  routerScreen: {
    flex: 1,
    backgroundColor: "#020617",
  },
});
