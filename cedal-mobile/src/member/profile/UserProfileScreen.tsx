// src/member/profile/UserProfileScreen.tsx
import React, { useEffect, useState } from "react";
import { Clipboard, ScrollView, StyleSheet } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useRouter } from "expo-router";
import { useUserProfileById } from "@/src/hooks/useUserProfileById";
import { ChatMetaSection } from "./components/ChatMetaSection";
import { NodeIdentitySection } from "./components/NodeIdentitySection";
import { ProfileHeader } from "./components/ProfileHeader";
import { QuickActionsSection } from "./components/QuickActionsSection";
import { GroupsPanel, UserGroupRow } from "./components/GroupsPanel";
import { BlockUserPanel } from "./components/BlockUserPanel";
import { auth, db } from "@/src/api/firebase";
import { SavedMessagesScreen } from "./SavedMessagesScreen";
import { MediaScreen } from "./MediaScreen";
import { DocumentsScreen } from "./DocumentsScreen";
import { ActivityScreen } from "./ActivityScreen";
import { PinnedMessagesScreen } from "./PinnedMessagesScreen";
import { ChatLockSheet } from "@/src/member/chat/partials/ChatLockSheet"
import VoiceCallScreen from "@/src/member/calls/VoiceCallScreen";
import VideoCallScreen from "@/src/member/calls/VideoCallScreen";

import {
  collection,
  doc,
  onSnapshot,
  query,
  updateDoc,
  arrayUnion,
} from "firebase/firestore";
import { View } from "react-native";

type Props = {
  userId: string; // target user
  onBack?: () => void;
};

type RoomPinMeta = {
  messageId: string;
  createdAt?: any;
  expiresAt?: any;
};

type SavedMessage = {
  id: string;
  text?: string | null;
  imageUri?: string | null;
  videoUri?: string | null;
  userId: string;
  userName?: string;
  label?: string | null;
  type?: string | null;
  timestamp?: number;
};

type RoomMessage = {
  id: string;
  text?: string | null;
  imageUri?: string | null;
  videoUri?: string | null;
  fileUri?: string | null;
  fileName?: string | null;
  fileSize?: number | null;
  fileMime?: string | null;
  type?: string | null;
  userId: string;
  userName?: string;
  timestamp?: number;
  savedBy?: string[];
  eventTitle?: string | null;
  eventStartsAt?: string | null;
  eventLocation?: string | null;
  eventNotes?: string | null;
  question?: string | null;
  options?: { id: string; label: string }[];
  allowMulti?: boolean;
  isPinned?: boolean;
};

type PinnedMessageSummary = {
  id: string;
  text: string | null;
  type: string | null;
  imageUri: string | null;
  videoUri: string | null;
  fileUri: string | null;
  fileName: string | null;
  userId: string;
  userName?: string;
  timestamp?: number;
};

export default function UserProfileScreen({ userId, onBack }: Props) {
  // HOOKS (must stay at top, same order)
  const { profile, loading } = useUserProfileById(userId);
  const router = useRouter();

  const [groupsOpen, setGroupsOpen] = useState(false);
  const [groupRows, setGroupRows] = useState<UserGroupRow[]>([]);
  const [voiceCallOpen, setVoiceCallOpen] = useState(false);
  const [videoCallOpen, setVideoCallOpen] = useState(false);
  const [passwordOpen, setPasswordOpen] = useState(false);
  const [mediaOpen, setMediaOpen] = useState(false);
  const [savedOpen, setSavedOpen] = useState(false);
  const [docsOpen, setDocsOpen] = useState(false);
  const [linksOpen, setLinksOpen] = useState(false);
  const [reportOpen, setReportOpen] = useState(false);
  const [blockOpen, setBlockOpen] = useState(false);
  const [activityOpen, setActivityOpen] = useState(false);
  const [pinScreenOpen, setPinScreenOpen] = useState(false); // if you still use it elsewhere
  const [pinnedOpen, setPinnedOpen] = useState(false);
  const [roomPinnedMeta, setRoomPinnedMeta] = useState<RoomPinMeta[]>([]);

  const [savedMessages, setSavedMessages] = useState<SavedMessage[]>([]);
  const [roomMessages, setRoomMessages] = useState<RoomMessage[]>([]);

  const viewerId = auth.currentUser?.uid ?? null;
  const dmRoomId =
    viewerId && userId ? [viewerId, userId].sort().join("_") : null;

  // live groups list for this user (all groups userId is in)
  useEffect(() => {
    const targetId = userId;
    if (!targetId) {
      setGroupRows([]);
      return;
    }

    const q = query(collection(db, "groups"));
    const unsub = onSnapshot(q, (snap) => {
      const rows: UserGroupRow[] = [];
      snap.forEach((docSnap) => {
        const d = docSnap.data() as any;
        const members: string[] = d.members ?? [];
        if (!members.includes(targetId)) return;

        rows.push({
          id: docSnap.id,
          name: d.name ?? docSnap.id,
        });
      });
      setGroupRows(rows);
    });

    return () => unsub();
  }, [userId]);

  type ChatStats = { messagesSent: number; stickersSent: number };

const [chatStats, setChatStats] = useState<ChatStats>({
  messagesSent: 0,
  stickersSent: 0,
});

function handleStartVoiceCall() {
  setVoiceCallOpen(true);
}

function handleStartVideoCall() {
  setVideoCallOpen(true);
}

  // ALL messages in this DM (for Media + Docs + Saved + Pinned)
  useEffect(() => {
    if (!viewerId || !dmRoomId) {
      setRoomMessages([]);
      return;
    }

    const msgsRef = collection(db, "rooms", dmRoomId, "messages");
    const unsub = onSnapshot(msgsRef, (snap) => {
      const next: RoomMessage[] = [];
      snap.forEach((docSnap) => {
        const m = docSnap.data() as any;

        next.push({
          id: docSnap.id,
          text: m.text ?? null,
          imageUri: m.imageUri ?? null,
          videoUri: m.videoUri ?? null,
          fileUri: m.fileUri ?? null,
          fileName: m.fileName ?? null,
          fileSize: m.fileSize ?? null,
          fileMime: m.fileMime ?? null,
          type: m.type ?? null,
          userId: m.userId,
          userName: m.userName ?? null,
          timestamp: m.createdAt?.toMillis
            ? m.createdAt.toMillis()
            : undefined,
          savedBy: m.savedBy ?? [],
          isPinned: m.isPinned ?? false,
          eventTitle: m.eventTitle ?? null,
          eventStartsAt: m.eventStartsAt ?? null,
          eventLocation: m.eventLocation ?? null,
          eventNotes: m.eventNotes ?? null,
          question: m.question ?? null,
          options: m.options ?? [],
          allowMulti: m.allowMulti ?? false,
        });
      });
      setRoomMessages(next);
    });

    return () => unsub();
  }, [viewerId, dmRoomId]);

  // Pinned meta for this DM (for profile Pinned overlay)
  useEffect(() => {
  if (!dmRoomId) {
    setRoomPinnedMeta([]);
    return;
  }

  const roomRef = doc(db, "rooms", dmRoomId);
  const unsub = onSnapshot(roomRef, (snap) => {
    const data = snap.data() as any;
    const arr: any[] = data?.pinnedMessages ?? [];
    const nowMs = Date.now();

    const cleaned: RoomPinMeta[] = arr
      .filter((p) => {
        if (!p?.expiresAt?.toMillis) return true; // no expiry -> keep
        return p.expiresAt.toMillis() > nowMs;    // only keep not yet expired
      })
      .map((p) => ({
        messageId: String(p.messageId),
        createdAt: p.createdAt ?? null,
        expiresAt: p.expiresAt ?? null,
      }));

    setRoomPinnedMeta(cleaned);
  });

  return () => unsub();
}, [dmRoomId]);

  // Saved messages for THIS DM (viewer ↔ this user)
  useEffect(() => {
    if (!viewerId || !dmRoomId) {
      setSavedMessages([]);
      return;
    }

    const msgsRef = collection(db, "rooms", dmRoomId, "messages");
    const unsub = onSnapshot(msgsRef, (snap) => {
      const next: SavedMessage[] = [];
      snap.forEach((docSnap) => {
        const m = docSnap.data() as any;
        const savedBy: string[] = m.savedBy ?? [];
        if (!savedBy.includes(viewerId)) return;

        let label: string | null = null;
        if (m.text && m.text.trim().length > 0) {
          label = m.text.trim();
        } else if (m.imageUri) {
          label = "Photo";
        } else if (m.videoUri) {
          label = "Video";
        } else if (m.audioUri) {
          label = "Voice message";
        } else if (m.type === "sticker") {
          label = "Sticker";
        } else {
          label = "Message";
        }

        next.push({
          id: docSnap.id,
          text: m.text ?? null,
          imageUri: m.imageUri ?? null,
          videoUri: m.videoUri ?? null,
          userId: m.userId,
          userName: m.userName ?? null,
          label,
          type: m.type ?? null,
          timestamp: m.createdAt?.toMillis
            ? m.createdAt.toMillis()
            : undefined,
        });
      });
      setSavedMessages(next);
    });

    return () => unsub();
  }, [viewerId, dmRoomId]);

  // loading guard AFTER all hooks
  if (loading || !profile) {
    return <SafeAreaView style={styles.screen} />;
  }

  const baseName = profile.email?.split("@")[0] || "Design lab";
  const handle = profile.email || "no-email@cedal.dev";
  const presence: "online" | "offline" = profile.online ? "online" : "offline";
  const level = profile.level ?? 0;
  const points = profile.points ?? 0;
  const messages = chatStats.messagesSent;        // was profile.messagesSent
  const stickers = chatStats.stickersSent;        // was profile.stickersSent
  const streak = profile.streakDays ?? 0;
  const reputation = profile.reputation ?? 0;

  const nickname = profile.nickname || baseName;
  const customLink = profile.customLink || `act://node/${handle}`;
  const randomLink = profile.randomLink || "https://cedal.dev/xxxxxx";
  const bio =
    profile.bio ||
    "Grinding ranked, clipping shorts, and dropping VODs for the node. Uploads from streams will surface here.";
  const age = profile.age ? String(profile.age) : "16";
  const occupation = profile.occupation || "Student, dev";
  const hobby = profile.hobby || "Gaming, coding, streaming";
  const gender = profile.gender || "Male";
  const avatarUrl = profile.avatarUrl ?? null;
  const initial = baseName.charAt(0).toUpperCase();

  function copyToClipboard(text: string) {
    try {
      Clipboard.setString(text);
    } catch {}
  }

  async function handleBlockUser() {
    const viewerIdInner = auth.currentUser?.uid;
    if (!viewerIdInner) return;
    const userDocRef = doc(db, "users", viewerIdInner);
    await updateDoc(userDocRef, {
      blockedUserIds: arrayUnion(userId),
    });
  }

  // Media list = all photos & videos in this DM, excluding stickers/emojis
  const mediaMessages = roomMessages.filter((m) => {
    const isStickerOrEmoji = m.type === "sticker" || m.type === "emoji";
    const isRealImage = !!m.imageUri && !isStickerOrEmoji;
    const isVideo = !!m.videoUri;
    return isRealImage || isVideo;
  });

  // Documents list = all document messages in this DM
  const documentMessages = roomMessages.filter(
    (m) => m.type === "document" && !!m.fileUri,
  );

  // Events + polls list
  const activityMessages = roomMessages
    .filter((m) => m.type === "event" || m.type === "poll")
    .map((m) => ({
      id: m.id,
      type: m.type as "event" | "poll",
      eventTitle: m.eventTitle ?? null,
      eventStartsAt: m.eventStartsAt ?? null,
      eventLocation: m.eventLocation ?? null,
      eventNotes: m.eventNotes ?? null,
      question: m.question ?? null,
      options: m.options ?? [],
      allowMulti: m.allowMulti ?? false,
      userId: m.userId,
      userName: m.userName ?? undefined,
      timestamp: m.timestamp,
    }));

  // Pinned messages for profile overlay
  const pinnedMessagesForProfile: PinnedMessageSummary[] = roomPinnedMeta
    .map((pin) => {
      const msg = roomMessages.find((m) => m.id === pin.messageId);
      if (!msg) return null;

      const summary: PinnedMessageSummary = {
        id: msg.id,
        text: msg.text ?? null,
        type: msg.type ?? null,
        imageUri: msg.imageUri ?? null,
        videoUri: msg.videoUri ?? null,
        fileUri: msg.fileUri ?? null,
        fileName: msg.fileName ?? null,
        userId: msg.userId,
        userName: msg.userName ?? undefined,
        timestamp: msg.timestamp,
      };

      return summary;
    })
    .filter((m): m is PinnedMessageSummary => m !== null);

  return (
    <SafeAreaView
      style={styles.screen}
      edges={["top", "bottom", "left", "right"]}
    >
      {/* Fixed header with avatar + call + video */}
      <ProfileHeader
  nickname={nickname}
  handle={handle}
  presence={presence}
  avatarUrl={avatarUrl}
  initial={initial}
  onBack={onBack}
  onVoiceCall={handleStartVoiceCall}
  onVideoCall={handleStartVideoCall}
/>

      {/* Scrollable content below */}
      <ScrollView contentContainerStyle={styles.content}>
        <NodeIdentitySection
          handle={handle}
          nickname={nickname}
          onChangeNickname={() => {}}
          customLink={customLink}
          age={age}
          occupation={occupation}
          hobby={hobby}
          gender={gender}
          bio={bio}
          onCopy={copyToClipboard}
        />

        <QuickActionsSection
          randomLink={randomLink}
          onCopyRandom={() => copyToClipboard(randomLink)}
          sharedGroups={groupRows}
          onOpenSharedGroups={() => setGroupsOpen(true)}
        />

        <ChatMetaSection
          onChatPassword={() => setPasswordOpen(true)}
          onSavedMessages={() => setSavedOpen(true)}
          onMedia={() => setMediaOpen(true)}
          onLinks={() => setLinksOpen(true)}
          onDocs={() => setDocsOpen(true)}
          onPollsEvents={() => setActivityOpen(true)}
          onFavorite={() => setPinnedOpen(true)}
          onBlock={() => setBlockOpen(true)}
          onClearChat={() => {
            // TODO
          }}
        />
      </ScrollView>

      <GroupsPanel
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

      {/* Full-screen Saved overlay */}
      {savedOpen && (
        <SavedMessagesScreen
          title="Saved"
          otherDisplayName={nickname}
          currentUserId={viewerId ?? "anon"}
          savedMessages={savedMessages}
          onBack={() => setSavedOpen(false)}
        />
      )}

      {passwordOpen && dmRoomId && (
  <ChatLockSheet
    chatId={dmRoomId}
    open={passwordOpen}
    onClose={() => setPasswordOpen(false)}
  />
)}

      {/* Full-screen Media overlay */}
      {mediaOpen && (
        <MediaScreen
          title="Media"
          otherDisplayName={nickname}
          currentUserId={viewerId ?? "anon"}
          mediaMessages={mediaMessages}
          onBack={() => setMediaOpen(false)}
        />
      )}

      {/* Full-screen Documents overlay */}
      {docsOpen && (
        <DocumentsScreen
          title="Documents"
          otherDisplayName={nickname}
          currentUserId={viewerId ?? "anon"}
          documentMessages={documentMessages}
          onBack={() => setDocsOpen(false)}
        />
      )}

      {/* Full-screen Events & Polls overlay */}
      {activityOpen && (
        <ActivityScreen
          title="Events & polls"
          otherDisplayName={nickname}
          currentUserId={viewerId ?? "anon"}
          activityMessages={activityMessages}
          onBack={() => setActivityOpen(false)}
        />
      )}

      {/* Full-screen Pinned messages overlay */}
      {pinnedOpen && (
        <PinnedMessagesScreen
          title="Pinned messages"
          otherDisplayName={nickname}
          currentUserId={viewerId ?? "anon"}
          pinnedMessages={pinnedMessagesForProfile}
          onBack={() => setPinnedOpen(false)}
        />
      )}

      {voiceCallOpen && (
  <View style={StyleSheet.absoluteFill}>
    <VoiceCallScreen
      calleeId={userId}
      onSwitchToVideo={() => {
        setVoiceCallOpen(false);
        setVideoCallOpen(true);
      }}
      onEnd={() => setVoiceCallOpen(false)}
    />
  </View>
)}

{videoCallOpen && (
  <View style={StyleSheet.absoluteFill}>
    <VideoCallScreen
      calleeId={userId}
      onSwitchToVoice={() => {
        setVideoCallOpen(false);
        setVoiceCallOpen(true);
      }}
      onEnd={() => setVideoCallOpen(false)}
    />
  </View>
)}

      <BlockUserPanel
        open={blockOpen}
        onClose={() => setBlockOpen(false)}
        targetName={nickname}
        onConfirm={handleBlockUser}
      />

      {/* TODO: plug in sheets for other meta actions */}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: "#020617" },
  content: { padding: 16, paddingBottom: 24 },
});
