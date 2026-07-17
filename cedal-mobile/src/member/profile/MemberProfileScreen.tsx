// src/member/profile/MemberProfileScreen.tsx
import React, { useEffect, useState } from "react";
import { View, ScrollView, StyleSheet, Text } from "react-native";
import { useRouter } from "expo-router";

import { ProfileHeader } from "./component/ProfileHeader";
import { ProfileAvatarBlock } from "./component/ProfileAvatarBlock";
import { NodeIdentitySection } from "./component/NodeIdentitySection";
import { QuickActionsSection } from "./component/QuickActionsSection";
import { ActivitySection } from "./component/ActivitySection";
import {
  GroupsTablePanel,
  GroupRow,
} from "./component/GroupsTablePanel";

import { db, auth } from "@/src/api/firebase";
import { collection, onSnapshot, query } from "firebase/firestore";

type Props = {
  saving: boolean;
  onBack: () => void;
  onPickAvatar: () => void;
  avatarUri: string | null;
  baseName: string;
  handle: string;
  presence: "online" | "offline";
  level: number;
  points: number;
  messages: number;
  stickers: number;
  streak: number;
  reputation: number;

  nickname: string;
  setNickname: (v: string) => void;
  customLink: string;
  setCustomLink: (v: string) => void;
  randomLink: string;
  bio: string;
  setBio: (v: string) => void;
  age: string;
  setAge: (v: string) => void;
  occupation: string;
  setOccupation: (v: string) => void;
  hobby: string;
  setHobby: (v: string) => void;
  gender: string;
  setGender: (v: string) => void;
};

export default function MemberProfileScreen(props: Props) {
  const {
    saving,
    onBack,
    onPickAvatar,
    avatarUri,
    baseName,
    handle,
    presence,
    level,
    points,
    messages,
    stickers,
    streak,
    reputation,
    nickname,
    setNickname,
    customLink,
    setCustomLink,
    randomLink,
    bio,
    setBio,
    age,
    setAge,
    occupation,
    setOccupation,
    hobby,
    setHobby,
    gender,
    setGender,
  } = props;

  const displayName = nickname || baseName;
  const initial = baseName.charAt(0).toUpperCase();

  const router = useRouter();
  const [groupsOpen, setGroupsOpen] = useState(false);
  const [groupRows, setGroupRows] = useState<GroupRow[]>([]);

  // live groups list for this user (role + name)
  useEffect(() => {
    const uid = auth.currentUser?.uid;
    if (!uid) {
      setGroupRows([]);
      return;
    }

    const q = query(collection(db, "groups"));
    const unsub = onSnapshot(q, (snap) => {
      const rows: GroupRow[] = [];
      snap.forEach((docSnap) => {
        const d = docSnap.data() as any;
        const members: string[] = d.members ?? [];
        if (!members.includes(uid)) return;

        let role = "Member";
        if (d.ownerId === uid || d.createdBy === uid) {
          role = "Owner";
        }

        rows.push({
          id: docSnap.id,
          role,
          name: d.name ?? docSnap.id,
        });
      });
      setGroupRows(rows);
    });

    return () => unsub();
  }, []);

  return (
    <View style={styles.screen}>
      <ScrollView contentContainerStyle={styles.content}>
        <ProfileHeader
          title="My Profile"
          role=""
          backLabel={saving ? "Saving…" : "Back"}
          onBack={saving ? undefined : onBack}
        />

        <ProfileAvatarBlock
          avatarUri={avatarUri}
          initial={initial}
          nickname={displayName}
          handle={handle}
          presence={presence}
          onPickAvatar={onPickAvatar}
        />

        {/* followers strip 
        <View style={styles.statsStrip}>
          <View style={styles.statsBox}>
            <Text style={styles.statsLabel}>Followers</Text>
            <Text style={styles.statsValue}>1,248</Text>
          </View>
          <View style={styles.statsBox}>
            <Text style={styles.statsLabel}>Following</Text>
            <Text style={styles.statsValue}>312</Text>
          </View>
          <View style={styles.statsBox}>
            <Text style={styles.statsLabel}>Reputation</Text>
            <Text style={styles.statsValue}>{reputation}</Text>
          </View>
        </View>*/}

        <NodeIdentitySection
          handle={handle}
          nickname={nickname}
          onChangeNickname={setNickname}
          customLink={customLink}
          onChangeCustomLink={setCustomLink}
          age={age}
          onChangeAge={setAge}
          occupation={occupation}
          onChangeOccupation={setOccupation}
          hobby={hobby}
          onChangeHobby={setHobby}
          gender={gender}
          onChangeGender={setGender}
          bio={bio}
          onChangeBio={setBio}
        />

        <QuickActionsSection
          randomLink={randomLink}
          onOpenGroups={() => setGroupsOpen(true)}
        />

        <ActivitySection
          level={level}
          points={points}
          messages={messages}
          stickers={stickers}
          streak={streak}
          reputation={reputation}
        />
      </ScrollView>

     <GroupsTablePanel
  open={groupsOpen}
  onClose={() => setGroupsOpen(false)}
  rows={groupRows}
  onOpenGroup={(groupId, groupName) => {
    setGroupsOpen(false);
    router.push({
      pathname: "/(auth)/(member)/group-chat",
      params: { groupId, groupName },
    });
  }}
/>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: "#020617" },
  content: { padding: 16, paddingBottom: 24 },
  statsStrip: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: 16,
  },
  statsBox: { flex: 1, alignItems: "center" },
  statsLabel: { fontSize: 11, color: "#9ca3af" },
  statsValue: {
    fontSize: 14,
    fontWeight: "600",
    color: "#e5e7eb",
    marginTop: 2,
  },
});
