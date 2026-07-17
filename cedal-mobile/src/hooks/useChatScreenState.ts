// src/member/chat/hooks/useChatScreenState.ts
import { useEffect, useState, useCallback, useMemo } from "react";
import { useFocusEffect } from "expo-router";
import { auth, db } from "@/src/api/firebase";
import {
  addDoc,
  collection,
  doc,
  getDoc,
  increment,
  onSnapshot,
  serverTimestamp,
  setDoc,
  where,
  query,
  updateDoc,
  Timestamp,
  type QuerySnapshot,
  type DocumentData,
} from "firebase/firestore";

import type { ChatScreenProps } from "@/src/member/chat/ChatScreen";
import { useChatMessages } from "@/src/hooks/useChatMessages";
import { askCorneal, CornealMessage } from "@/src/api/cornealAssistant";
import { uploadChatImage } from "@/src/member/utils/uploadChatImage";
import { uploadChatVideo } from "@/src/member/utils/uploadChatVideo";
import { uploadChatAudio } from "@/src/member/utils/uploadChatAudio";
import { openChatCamera } from "@/src/member/utils/ChatCamera";
import { openChatVideo } from "@/src/member/utils/chatVideo";
import { pickAndUploadDocumentForChat } from "@/src/member/utils/pickAndUploadDocument";
import type { Message } from "@/src/member/chat/MessageRow";
import type { Group } from "@/src/member/fun/groups/GroupsHub";
import type { ViewOnceConfig } from "@/src/member/utils/ViewOnceConfigSheet";
import type { ChatTheme } from "@/src/types/ChatTheme";
import { loadChatTheme, saveChatTheme } from "@/src/api/userStorage";
import Clipboard from "@react-native-clipboard/clipboard";
import { scheduleCountdownAlarm } from "@/src/member/utils/countdownAlarm";

type GalleryItem = {
  id: string;
  uri: string;
  kind: "image" | "video" | "stickerImage" | "stickerVideo";
};

type ContactUser = { id: string; name: string; handle?: string };

export function useChatScreenState({
  chatId,
  onBack,
  chatName,
  otherUserId: otherUserIdFromRoute,
}: ChatScreenProps) {
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);

  const [othersTyping, setOthersTyping] = useState(false);
  const [isTyping, setIsTyping] = useState(false);
  const [toolsOpen, setToolsOpen] = useState(false);

  const [stickerSheetOpen, setStickerSheetOpen] = useState(false);
  const [pollSheetOpen, setPollSheetOpen] = useState(false);
  const [emojiPickerTarget, setEmojiPickerTarget] =
    useState<Message | null>(null);
  const [emojiSheetOpen, setEmojiSheetOpen] = useState(false);
  const [stickerReactionTarget, setStickerReactionTarget] =
    useState<Message | null>(null);

  const [previewUri, setPreviewUri] = useState<string | null>(null);
  const [previewType, setPreviewType] = useState<"image" | "video">("image");
  const [previewViewOnce, setPreviewViewOnce] = useState(false);
  const [metaOpen, setMetaOpen] = useState(false);
  const [viewOnceConfig, setViewOnceConfig] = useState<ViewOnceConfig>({
    mode: "views",
    maxViews: 1,
    timeLimitSeconds: null,
  });

  // pin sheet + target
  const [pinSheetOpen, setPinSheetOpen] = useState(false);
  const [pinTargetMessage, setPinTargetMessage] =
    useState<Message | null>(null);

  const confirmPinWithDuration = useCallback(
    async (durationMs: number) => {
      if (!chatId || !pinTargetMessage) return;

      const roomRef = doc(db, "rooms", String(chatId));
      const now = Date.now();
      const expiresAt =
        durationMs > 0 ? Timestamp.fromMillis(now + durationMs) : null;

      try {
        const snap = await getDoc(roomRef);
        const data = (snap.data() as any) || {};
        const prev: any[] = data.pinnedMessages ?? [];

        const nowMs = Date.now();
        const cleaned = prev.filter((p) => {
          if (!p?.expiresAt?.toMillis) return true;
          return p.expiresAt.toMillis() > nowMs;
        });

        const withoutDup = cleaned.filter(
          (p) => p.messageId !== String(pinTargetMessage.id),
        );

        const next = [
          ...withoutDup,
          {
            messageId: String(pinTargetMessage.id),
            createdAt: Timestamp.fromMillis(now),
            expiresAt,
          },
        ];

        if (next.length > 4) {
          next.sort((a, b) => {
            const aMs = a.createdAt?.toMillis ? a.createdAt.toMillis() : 0;
            const bMs = b.createdAt?.toMillis ? b.createdAt.toMillis() : 0;
            return aMs - bMs;
          });
          // keep newest 4
          next.splice(0, next.length - 4);
        }

        await updateDoc(roomRef, { pinnedMessages: next });
      } catch (e: any) {
        console.warn("confirmPinWithDuration error", e?.message);
      } finally {
        setPinTargetMessage(null);
        setPinSheetOpen(false);
      }
    },
    [chatId, pinTargetMessage],
  );

  const unpinMessage = useCallback(
    async (m: Message) => {
      if (!chatId || !m?.id) return;

      const roomRef = doc(db, "rooms", String(chatId));

      try {
        const snap = await getDoc(roomRef);
        const data = (snap.data() as any) || {};
        const prev: any[] = data.pinnedMessages ?? [];

        const next = prev.filter(
          (p) => String(p.messageId) !== String(m.id),
        );

        await updateDoc(roomRef, { pinnedMessages: next });
      } catch (e: any) {
        console.warn("unpinMessage error", e?.message);
      }
    },
    [chatId],
  );

  const [reportSheetOpen, setReportSheetOpen] = useState(false);
  const [reportsClearedAt, setReportsClearedAt] =
    useState<number | null>(null);
  const [themeSheetOpen, setThemeSheetOpen] = useState(false);
  const [chatTheme, setChatTheme] = useState<ChatTheme>({
    kind: "none",
    backgroundImageUrl: null,
    backgroundColor: null,
    textColor: null,
    textFontFamily: null,
    textSize: null,
    bubbleMeColor: null,
    bubbleOtherColor: null,
    tabAccentColor: null,
    tabLabelColor: null,
    presetId: null,
  });
  // load persisted chat theme once
useEffect(() => {
  (async () => {
    try {
      const stored = await loadChatTheme();
      if (stored) {
        setChatTheme(stored);
      }
    } catch (e: any) {
      console.warn("loadChatTheme error", e?.message);
    }
  })();
}, []);

const applyChatTheme = useCallback(
  async (next: ChatTheme) => {
    setChatTheme(next);
    try {
      await saveChatTheme(next);
    } catch (e: any) {
      console.warn("saveChatTheme error", e?.message);
    }
  },
  [],
);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [reportSubmittedSheetOpen, setReportSubmittedSheetOpen] =
    useState(false);
  const [reportsForChat, setReportsForChat] = useState<any[]>([]);
  const [viewOnceConfigOpen, setViewOnceConfigOpen] = useState(false);
  const [clearSheetOpen, setClearSheetOpen] = useState(false);
  const [clearDurationMinutes, setClearDurationMinutes] = useState(60);
  const [linkPickerOpen, setLinkPickerOpen] = useState(false);
  const [groups, setGroups] = useState<Group[]>([]);
  const [openViewOnceMsg, setOpenViewOnceMsg] =
    useState<Message | null>(null);

  const [eventSheetOpen, setEventSheetOpen] = useState(false);
  
  const [selectedMessage, setSelectedMessage] =
    useState<Message | null>(null);
  const [messageActionsOpen, setMessageActionsOpen] = useState(false);

  const [selectedReplyMessage, setSelectedReplyMessage] =
    useState<Message | null>(null);

  const handleDeleteMessage = useCallback(
    async (m: Message) => {
      if (!chatId || !m?.id) return;
      if (m.deleted) return;
      try {
        const msgRef = doc(
          db,
          "rooms",
          String(chatId),
          "messages",
          String(m.id),
        );
        await updateDoc(msgRef, {
          text: "[deleted]",
          imageUri: null,
          videoUri: null,
          audioUri: null,
          type: "text",
          deleted: true,
        });
      } catch (e: any) {
        console.warn("delete message error", e?.message);
      }
    },
    [chatId],
  );

  const clearReportsForChat = useCallback(async () => {
    const currentUser = auth.currentUser;
    const currentUserId = currentUser?.uid ?? "anon";

    if (!chatId || !currentUserId) return;
    try {
      const now = Date.now();
      setReportsClearedAt(now);
      setReportsForChat([]);
      setReportSubmittedSheetOpen(false);

      const stateRef = doc(
        db,
        "rooms",
        String(chatId),
        "readState",
        currentUserId,
      );

      await setDoc(
        stateRef,
        { reportsClearedAt: now },
        { merge: true },
      );
    } catch (e: any) {
      console.warn("clear reports error", e?.message);
    }
  }, [chatId]);

  // voice
  const [voiceSheetOpen, setVoiceSheetOpen] = useState(false);
  const [audioPreviewOpen, setAudioPreviewOpen] = useState(false);
  const [pendingAudioUri, setPendingAudioUri] = useState<string | null>(null);
  const [editSheetOpen, setEditSheetOpen] = useState(false);
  const [editDurationMs, setEditDurationMs] = useState(1);
  const [editConfig, setEditConfig] = useState<any | null>(null);

  const [nowTick, setNowTick] = useState(Date.now());
  const [stickerPreviewUrl, setStickerPreviewUrl] =
    useState<string | null>(null);
  const [stickerPreviewViewOnce, setStickerPreviewViewOnce] =
    useState(false);

  // gallery
  const [galleryOpen, setGalleryOpen] = useState(false);
  const [galleryItems, setGalleryItems] = useState<GalleryItem[]>([]);

  // audio gallery
  const [audioPanelOpen, setAudioPanelOpen] = useState(false);
  const [audioItems, setAudioItems] = useState<
    { id: string; uri: string; label: string }[]
  >([]);

  const currentUser = auth.currentUser;
  const currentUserId = currentUser?.uid ?? "anon";
  const [currentUserName] = useState("You");

  const [blockedUserIds, setBlockedUserIds] = useState<string[]>([]);
  const [isBlocked, setIsBlocked] = useState(false);
  const [iBlockedThem, setIBlockedThem] = useState(false);

  const isGeneral = chatId === "general";
  const isCorneal = chatId === "corneal";

  const isXenoshacker =
  currentUser?.email?.toLowerCase() === "xenoshacker@gmail.com";

  const canActInGeneral = isGeneral ? isXenoshacker : true;
  const canUseDisappearing = isXenoshacker;

  const [otherUserId, setOtherUserId] = useState<string | null>(
    otherUserIdFromRoute ?? null,
  );
  const [otherAvatarUrl, setOtherAvatarUrl] = useState<string | null>(null);
  const [otherDisplayName, setOtherDisplayName] = useState("");

  // all accepted friends
const [friends, setFriends] = useState<ContactUser[]>([]);

const [contactPickerOpen, setContactPickerOpen] = useState(false);
const [dmContacts, setDmContacts] = useState<ContactUser[]>([]);

// load accepted friends from friendRequests
useEffect(() => {
  const current = auth.currentUser;
  if (!current) return;

  const baseRef = collection(db, "friendRequests");

  const qOut = query(
    baseRef,
    where("fromId", "==", current.uid),
    where("status", "==", "accepted"),
  );

  const qIn = query(
    baseRef,
    where("toId", "==", current.uid),
    where("status", "==", "accepted"),
  );

  const hydrate = async (otherId: string): Promise<ContactUser | null> => {
    const userSnap = await getDoc(doc(db, "users", otherId));
    if (!userSnap.exists()) return null;

    const u = userSnap.data() as any;
    const nickname = u.nickname || "";
    const email = u.email ?? null;

    return {
      id: otherId,
      name: (nickname || email || "Unknown") as string,
      handle: email ?? undefined,
    };
  };

  const unsubOut = onSnapshot(
    qOut,
    async (snap: QuerySnapshot<DocumentData>) => {
      const items = await Promise.all(
        snap.docs.map(async (d) => {
          const data = d.data() as any;
          return hydrate(data.toId as string);
        }),
      );

      const cleaned: ContactUser[] = items.filter(
        (x): x is ContactUser => x !== null,
      );

      setFriends((prev) => {
        const withoutThese = prev.filter(
          (c) => !cleaned.some((n) => n.id === c.id),
        );
        return [...withoutThese, ...cleaned];
      });
    },
  );

  const unsubIn = onSnapshot(
    qIn,
    async (snap: QuerySnapshot<DocumentData>) => {
      const items = await Promise.all(
        snap.docs.map(async (d) => {
          const data = d.data() as any;
          return hydrate(data.fromId as string);
        }),
      );

      const cleaned: ContactUser[] = items.filter(
        (x): x is ContactUser => x !== null,
      );

      setFriends((prev) => {
        const withoutThese = prev.filter(
          (c) => !cleaned.some((n) => n.id === c.id),
        );
        return [...withoutThese, ...cleaned];
      });
    },
  );

  return () => {
    unsubOut();
    unsubIn();
  };
}, []);

useEffect(() => {
  if (!friends.length) {
    setDmContacts([]);
    return;
  }

  const filtered = friends.filter((f) => {
    if (f.id === currentUserId) return false; // exclude me
    if (otherUserId && f.id === otherUserId) return false; // exclude current chat peer
    return true;
  });

  setDmContacts(filtered);
}, [friends, currentUserId, otherUserId]);

  const [disappearingSheetOpen, setDisappearingSheetOpen] =
    useState(false);
  const [disappearingEnabled, setDisappearingEnabled] = useState(false);
  const [disappearingDurationSeconds, setDisappearingDurationSeconds] =
    useState(0);

  const messages = useChatMessages(
    chatId,
    currentUserId,
    currentUserName,
    isCorneal,
  );

  const [hiddenUntil, setHiddenUntil] = useState<number | null>(null);

  const visibleMessages = messages.filter((m) => {
  // In general, fully hide deleted messages
  if (isGeneral && m.deleted) {
    return false;
  }

  if (hiddenUntil && m.createdAt) {
    const ts = m.createdAt.getTime();
    if (ts <= hiddenUntil) {
      return false;
    }
  }

  if (disappearingEnabled && disappearingDurationSeconds && m.createdAt) {
    const createdMs = m.createdAt.getTime();
    const ageMs = nowTick - createdMs;
    if (ageMs >= disappearingDurationSeconds * 1000) {
      return false;
    }
  }

  return true;
});

    const savedMessagesForMe = useMemo(
  () =>
    visibleMessages
      .filter((m) => (m.savedBy ?? []).includes(currentUserId))
      .map((m) => ({
        id: m.id,
        text: m.text ?? null,
        imageUri: m.imageUri ?? null,
        videoUri: m.videoUri ?? null,
        userId: m.userId,
        userName: m.userName ?? null,
        timestamp: m.createdAt ? m.createdAt.getTime() : undefined,
      })),
  [visibleMessages, currentUserId],
);

// pinned state from room
type PinMeta = {
  messageId: string;
  createdAt?: any;
  expiresAt?: any;
};

type ContactUser = { id: string; name: string; handle?: string };

const [pinnedMetas, setPinnedMetas] = useState<PinMeta[]>([]);

useEffect(() => {
  if (!chatId) return;

  const roomRef = doc(db, "rooms", String(chatId));
  const unsub = onSnapshot(roomRef, (snap) => {
    const data = snap.data() as any;
    const arr: any[] = data?.pinnedMessages ?? [];
    const nowMs = Date.now();

    const cleaned = arr.filter((p) => {
      if (!p?.expiresAt?.toMillis) return true; // no expiry -> keep
      return p.expiresAt.toMillis() > nowMs;    // only keep not expired
    });

    cleaned.sort((a, b) => {
      const aMs = a.createdAt?.toMillis ? a.createdAt.toMillis() : 0;
      const bMs = b.createdAt?.toMillis ? b.createdAt.toMillis() : 0;
      return aMs - bMs;
    });

    setPinnedMetas(cleaned);
  });

  return () => unsub();
}, [chatId]);

const headerPinnedMessages = (messages ?? []).filter((m: Message) =>
  pinnedMetas.some((p) => String(p.messageId) === String(m.id)),
);

  // read state
  useFocusEffect(
  useCallback(() => {
    if (!chatId || !currentUser) return;

    const stateRef = doc(
      db,
      "rooms",
      String(chatId),
      "readState",
      currentUser.uid,
    );

    setDoc(
      stateRef,
      { lastReadAt: serverTimestamp() },
      { merge: true },
    ).catch((e: any) =>
      console.warn("lastReadAt update error", e?.message),
    );

    return undefined;
  }, [chatId, currentUser]),
);

  // infer otherUserId in DM
  useEffect(() => {
    if (!chatId || !currentUserId || isCorneal || isGeneral) return;
    if (otherUserIdFromRoute) return;

    (async () => {
      try {
        const roomSnap = await getDoc(doc(db, "rooms", String(chatId)));
        if (!roomSnap.exists()) return;
        const roomData = roomSnap.data() as any;
        const participants: string[] = roomData.participants || [];
        const other =
          participants.find((uid) => uid !== currentUserId) || null;
        setOtherUserId(other);
      } catch (e: any) {
        console.warn("load room error", e?.message);
      }
    })();
  }, [chatId, isGeneral, isCorneal, currentUserId, otherUserIdFromRoute]);

  useEffect(() => {
  if (!messages.length) {
    setGalleryItems([]);
    return;
  }

  const items: GalleryItem[] = [];
  messages.forEach((m) => {
    // skip any view‑once media
    if (m.viewOnce) {
      return;
    }

    if (m.imageUri) {
      items.push({
        id: `${m.id}-img`,
        uri: m.imageUri,
        kind: "image",
      });
    }

    if (m.videoUri) {
      items.push({
        id: `${m.id}-vid`,
        uri: m.videoUri,
        kind: "video",
      });
    }

    // if you also want to hide view‑once stickers, keep this under the same guard
    if (m.type === "sticker" && m.stickerUri) {
      items.push({
        id: `${m.id}-stimg`,
        uri: m.stickerUri,
        kind: "stickerImage",
      });
    }
  });

  setGalleryItems(items);
}, [messages]);

  // other user profile + block flags
  useEffect(() => {
    if (!otherUserId) {
      setIsBlocked(false);
      setIBlockedThem(false);
      return;
    }

    (async () => {
      try {
        const userSnap = await getDoc(doc(db, "users", otherUserId));
        if (!userSnap.exists()) {
          setIsBlocked(false);
          setIBlockedThem(false);
          return;
        }
        const u = userSnap.data() as any;
        setOtherAvatarUrl(u.avatarUrl || null);
        setOtherDisplayName(u.nickname || u.email || "Node");

        setIBlockedThem(blockedUserIds.includes(otherUserId));
        const theirBlocked: string[] = u.blockedUserIds ?? [];
        setIsBlocked(theirBlocked.includes(currentUserId));
      } catch (e: any) {
        console.warn("load other user profile error", e?.message);
        setIsBlocked(false);
        setIBlockedThem(false);
      }
    })();
  }, [otherUserId, blockedUserIds, currentUserId]);

  // typing indicator
  useEffect(() => {
    if (!chatId || !currentUserId || isCorneal || isGeneral) return;

    const typingRef = collection(db, "rooms", String(chatId), "typing");
    const unsub = onSnapshot(typingRef, (snap) => {
      let someoneElseTyping = false;
      snap.forEach((docSnap) => {
        const data = docSnap.data() as any;
        if (!data?.isTyping) return;
        if (docSnap.id === currentUserId) return;
        someoneElseTyping = true;
      });
      setOthersTyping(someoneElseTyping);
    });

    return () => unsub();
  }, [chatId, currentUserId, isCorneal, isGeneral]);

  const setTyping = useCallback(
    async (typing: boolean) => {
      if (!chatId || !currentUserId || isCorneal || isGeneral) return;
      setIsTyping(typing);
      try {
        const typingDocRef = doc(
          db,
          "rooms",
          String(chatId),
          "typing",
          currentUserId,
        );
        await setDoc(
          typingDocRef,
          {
            userId: currentUserId,
            userName: currentUserName,
            isTyping: typing,
            updatedAt: serverTimestamp(),
          },
          { merge: true },
        );
      } catch (e: any) {
        console.warn("typing update error", e?.message);
      }
    },
    [chatId, currentUserId, currentUserName, isCorneal, isGeneral],
  );

  // audio items list
  useEffect(() => {
    if (!messages.length) {
      setAudioItems([]);
      return;
    }

    const items: { id: string; uri: string; label: string }[] = [];
    messages.forEach((m) => {
      if (m.audioUri) {
        const label =
          m.text && m.text.length > 0
            ? m.text
            : m.userId === currentUserId
            ? "Your voice note"
            : `${m.userName}'s voice note`;

        items.push({ id: m.id, uri: m.audioUri, label });
      }
    });
    setAudioItems(items);
  }, [messages, currentUserId]);

  // Corneal reply helper
  const sendCornealReply = useCallback(
    async (userText: string, sourceChatId?: string) => {
      if (!currentUser) return;

      const aiMsgsCol = collection(
        db,
        "users",
        currentUserId,
        "aiChats",
        "corneal",
        "messages",
      );

      const userMsgDoc = await addDoc(aiMsgsCol, {
        role: "user",
        text: userText,
        createdAt: serverTimestamp(),
      });

      const aiRoomRef = doc(db, "rooms", `ai_${currentUserId}_corneal`);
      await setDoc(
        aiRoomRef,
        {
          type: "ai",
          participants: [currentUserId, "corneal"],
          lastMessage: userText,
          lastMessageAt: serverTimestamp(),
        },
        { merge: true },
      );

      const recentForCorneal: CornealMessage[] = [];
      const lastFew = messages.slice(0, 8).reverse();
      lastFew.forEach((m) => {
  const parts: string[] = [];

  // who + type
  const who = m.userId === currentUserId ? "You" : (m.userName ?? "Friend");
  const kind = m.type ?? "text";
  parts.push(`Message ID: ${m.id}. ${who} sent a ${kind} message.`);

  // main text
  if (m.text?.trim()) {
    parts.push(`Text: "${m.text.trim()}"`);
  }

  // media
  if (m.imageUri) {
    parts.push(`Image URL: ${m.imageUri}`);
  }
  if (m.videoUri) {
    parts.push(`Video URL: ${m.videoUri}`);
  }
  if (m.audioUri) {
    parts.push(`Audio URL: ${m.audioUri}`);
  }
  if (kind === "sticker" && m.stickerUri) {
    parts.push(`Sticker URL: ${m.stickerUri}`);
  }

  // reply/tag info
  if (m.replyToId) {
    const rtType = m.replyToType ?? "text";
    const rtText = m.replyToText ?? "";
    parts.push(
      `This message is replying to message ${m.replyToId} of type ${rtType} with text "${rtText}".`
    );
    if (m.replyToStickerUri) {
      parts.push(`Original sticker URL: ${m.replyToStickerUri}`);
    }
  }

  recentForCorneal.push({
    id: m.id,
    role: m.userId === currentUserId ? "user" : "assistant",
    content: parts.join(" "),
  });
});

      recentForCorneal.push({
        id: userMsgDoc.id,
        role: "user",
        content: userText,
      });

      let aiAnswer = "";
      try {
        aiAnswer = await askCorneal({ messages: recentForCorneal });
      } catch (err: any) {
        console.warn("Corneal call failed", err?.message);
        aiAnswer =
          "I had trouble answering just now. Try again.";
      }
      if (!aiAnswer) return;

      await addDoc(aiMsgsCol, {
        role: "assistant",
        text: aiAnswer,
        createdAt: serverTimestamp(),
      });

      await setDoc(
        aiRoomRef,
        {
          lastMessage: aiAnswer,
          lastMessageAt: serverTimestamp(),
        },
        { merge: true },
      );

      try {
        if (sourceChatId && !isCorneal && !isGeneral) {
          const dmRoomRef = doc(db, "rooms", String(sourceChatId));
          const dmMsgsRef = collection(dmRoomRef, "messages");

          await addDoc(dmMsgsRef, {
            text: aiAnswer,
            userId: "corneal",
            userName: "Corneal",
            type: "text",
            imageUri: null,
            videoUri: null,
            audioUri: null,
            createdAt: serverTimestamp(),
          });

          await setDoc(
            dmRoomRef,
            {
              lastMessage: aiAnswer,
              lastMessageAt: serverTimestamp(),
            },
            { merge: true },
          );
        }
      } catch (e: any) {
        console.warn(
          "failed to echo Corneal reply into DM",
          e?.message,
        );
      }
    },
    [currentUser, currentUserId, messages, isCorneal, isGeneral],
  );

  // handleSend with reply + block
  const handleSend = useCallback(
    async () => {
      if (!input.trim() || !chatId || !currentUser) return;

      if (isBlocked || iBlockedThem) {
        return;
      }

      if (isGeneral && !canActInGeneral) {
      setSending(false);
      return;
      }


      try {
        setSending(true);

        const rawText = input;
        const cleanedText = input.trim();

        const mentionMatch = rawText.match(
          /^\s*(?:@corneal|\.corneal)\s+(.+)$/i,
        );
        const isMention = !!mentionMatch;
        const mentionQuestion = mentionMatch?.[1]?.trim() ?? "";
        const lower = cleanedText.toLowerCase();
        const isMenuCommand =
          lower === "@menu" || lower === ".menu";

        if (isMenuCommand) {
          const roomRef = doc(db, "rooms", String(chatId));
          const msgsRef = collection(roomRef, "messages");
          const replyTo = selectedReplyMessage;

          await addDoc(msgsRef, {
            text: "",
            type: "menu",
            userId: "corneal",
            userName: "Corneal",
            imageUri: null,
            videoUri: null,
            audioUri: null,
            createdAt: serverTimestamp(),
            replyToId: replyTo ? replyTo.id : null,
            replyToText: replyTo ? replyTo.text : null,
            replyToUserName: replyTo ? replyTo.userName : null,
          });

          await setDoc(
            roomRef,
            {
              type: isGeneral ? "updates" : isCorneal ? "ai" : "dm",
              updatedAt: serverTimestamp(),
              lastMessage: "[Corneal menu]",
              lastMessageAt: serverTimestamp(),
            },
            { merge: true },
          );

          setInput("");
          setSelectedReplyMessage(null);
          if (isTyping) setTyping(false);
          setSending(false);
          return;
        }

        const isXenoshacker =
  currentUser?.email?.toLowerCase() === "xenoshacker@gmail.com";

// In general, only Xenoshacker can send anything.
if (isGeneral && !isXenoshacker) {
  setSending(false);
  return;
}

        if (isCorneal) {
          setInput("");
          await sendCornealReply(cleanedText, chatId);
          setSending(false);
          return;
        }

        const roomRef = doc(db, "rooms", String(chatId));
        const msgsRef = collection(roomRef, "messages");

        const ids = String(chatId).split("_");
        const myId = currentUserId;
        const otherId =
          ids.find((id) => id !== myId) || null;

        await setDoc(
          roomRef,
          {
            type: isGeneral ? "updates" : "dm",
            participants: otherId ? [myId, otherId] : [myId],
            updatedAt: serverTimestamp(),
            lastMessage: cleanedText,
            lastMessageAt: serverTimestamp(),
          },
          { merge: true },
        );

        const replyTo = selectedReplyMessage;

        let replyToText: string | null = null;
        let replyToType: string | null = null;
        let replyToStickerUri: string | null = null;
        let replyToViewOnce: boolean | null = null;

        if (replyTo) {
          replyToType = replyTo.type ?? null;
          replyToStickerUri = replyTo.stickerUri ?? null;
          replyToViewOnce = !!replyTo.viewOnce;

          if (replyToViewOnce) {
            // any view-once media → generic label
            replyToText = "View once";
          } else {
            const directText = replyTo.text ?? "";

            if (directText.trim().length > 0) {
              replyToText = directText.trim();
            } else if (replyTo.imageUri) {
              replyToText = "Photo";
            } else if (replyTo.videoUri) {
              replyToText = "Video";
            } else if (replyTo.audioUri) {
              replyToText = "Voice message";
            } else if (replyTo.type === "sticker") {
              replyToText = "Sticker";
            } else {
              replyToText = "Message";
            }
          }
        }

        await addDoc(msgsRef, {
  text: cleanedText,
  userId: currentUserId,
  userName: currentUserName,
  createdAt: serverTimestamp(),
  replyToId: replyTo ? replyTo.id : null,
  replyToText,
  replyToUserName: replyTo ? replyTo.userName : null,
  replyToType,
  replyToStickerUri,
  replyToViewOnce,
});

// count this as one message sent by the user
try {
  const userRef = doc(db, "users", currentUserId);
  const userSnap = await getDoc(userRef);
  const now = new Date();

  let streakDays = 0;
  let lastActiveAt: Date | null = null;

  if (userSnap.exists()) {
    const data = userSnap.data() as any;
    streakDays = data.streakDays ?? 0;
    const rawLast = data.lastActiveAt;
    lastActiveAt =
      rawLast && typeof rawLast.toDate === "function"
        ? rawLast.toDate()
        : null;
  }

  const today = new Date(Date.UTC(
    now.getUTCFullYear(),
    now.getUTCMonth(),
    now.getUTCDate(),
  ));

  let newStreak = 1;

  if (lastActiveAt) {
    const last = new Date(Date.UTC(
      lastActiveAt.getUTCFullYear(),
      lastActiveAt.getUTCMonth(),
      lastActiveAt.getUTCDate(),
    ));
    const diffDays =
      (today.getTime() - last.getTime()) / (1000 * 60 * 60 * 24);

    if (diffDays === 0) {
      newStreak = streakDays || 1;
    } else if (diffDays === 1) {
      newStreak = streakDays + 1;
    } else if (diffDays > 1) {
      newStreak = 1;
    }
  }

  await updateDoc(userRef, {
    statsMessagesSent: increment(1),
    streakDays: newStreak,
    lastActiveAt: serverTimestamp(),
  });
} catch (e: any) {
  console.warn("increment stats / streak error", e?.message);
}

        setInput("");
        setSelectedReplyMessage(null);
        if (isTyping) setTyping(false);

        if (isMention && mentionQuestion) {
          await sendCornealReply(mentionQuestion, chatId);
        }
      } catch (e: any) {
        console.warn("send error", e?.message);
      } finally {
        setSending(false);
      }
    },
    [
      input,
      chatId,
      currentUser,
      currentUserId,
      currentUserName,
      isCorneal,
      isGeneral,
      isTyping,
      setTyping,
      sendCornealReply,
      isBlocked,
      iBlockedThem,
      selectedReplyMessage,
    ],
  );

  // TOOL ACTIONS
  const handleToolAction = useCallback(
    async (action: { tool: string; type: string }) => {
      if (!chatId || !currentUser) return;

      const isXenoshacker =
  currentUser?.email?.toLowerCase() === "xenoshacker@gmail.com";

// In general, only Xenoshacker can use any tools at all.
if (isGeneral && !isXenoshacker) {
  return;
}

      if (isGeneral && !canActInGeneral) {
      return;
    }

      if (action.tool === "camera") {
        const uri = await openChatCamera();
        if (!uri) return;
        setPreviewUri(uri);
        setPreviewType("image");
        setPreviewViewOnce(false);
        return;
      }

      if (action.tool === "cameraVideo") {
        const uri = await openChatVideo();
        if (!uri) return;
        setPreviewUri(uri);
        setPreviewType("video");
        setPreviewViewOnce(false);
        return;
      }

      if (action.tool === "voice") {
        setVoiceSheetOpen(true);
        return;
      }

      if (action.tool === "sticker") {
        setStickerReactionTarget(null); // normal mode
        setStickerSheetOpen(true);
        return;
      }

      if (action.tool === "gallery") {
        setGalleryOpen(true);
        return;
      }

      if (action.tool === "audio") {
        setAudioPanelOpen(true);
        return;
      }

      if (action.tool === "links") {
        setLinkPickerOpen(true);
        return;
      }

      if (action.tool === "poll") {
        setPollSheetOpen(true);
        return;
      }

      if (action.tool === "contact") {
        setContactPickerOpen(true);
        return;
      }

      if (action.tool === "event") {
        setEventSheetOpen(true);
        return;
      }

      if (action.tool === "emoji") {
        setEmojiSheetOpen(true);
        return;
      }

      if (action.tool === "document") {
        const result = await pickAndUploadDocumentForChat(
          chatId,
          currentUserId,
        );
        if (!result) return;

        const { downloadUrl, name, size, mimeType } = result;
        const roomRef = doc(db, "rooms", String(chatId));
        const msgsRef = collection(roomRef, "messages");

        await addDoc(msgsRef, {
          type: "document",
          text: "",
          fileUri: downloadUrl,
          fileName: name,
          fileSize: size,
          fileMime: mimeType,
          imageUri: null,
          videoUri: null,
          audioUri: null,
          userId: currentUserId,
          userName: currentUserName,
          createdAt: serverTimestamp(),
        });

        await setDoc(
          roomRef,
          {
            lastMessage: name,
            lastMessageAt: serverTimestamp(),
          },
          { merge: true },
        );

        return;
      }
    },
    [chatId, currentUser, currentUserId, currentUserName],
  );

  // preview send / cancel
  const handlePreviewSend = useCallback(
    async () => {
      if (!previewUri || !chatId || !currentUser) return;

      try {
        const roomRef = doc(db, "rooms", String(chatId));
        const msgsRef = collection(roomRef, "messages");

        if (previewType === "image") {
          const fileName = `${currentUserId}_${Date.now()}.jpg`;
          const path = `chats/${chatId}/${fileName}`;
          const downloadUrl = await uploadChatImage(previewUri, path);

          await addDoc(msgsRef, {
            text: "",
            imageUri: downloadUrl,
            videoUri: null,
            audioUri: null,
            userId: currentUserId,
            userName: currentUserName,
            createdAt: serverTimestamp(),
            viewOnce: previewViewOnce,
            viewConfig: previewViewOnce ? viewOnceConfig : null,
          });
        } else {
          const fileName = `${currentUserId}_${Date.now()}.mp4`;
          const path = `chats/${chatId}/${fileName}`;
          const downloadUrl = await uploadChatVideo(previewUri, path);

          await addDoc(msgsRef, {
            text: "",
            imageUri: null,
            videoUri: downloadUrl,
            audioUri: null,
            userId: currentUserId,
            userName: currentUserName,
            createdAt: serverTimestamp(),
            viewOnce: previewViewOnce,
            viewConfig: previewViewOnce ? viewOnceConfig : null,
          });
        }

        setPreviewUri(null);
        setPreviewViewOnce(false);
      } catch (e: any) {
        console.warn("preview send error", e?.message);
      }
    },
    [
      previewUri,
      previewType,
      chatId,
      currentUser,
      currentUserId,
      currentUserName,
      previewViewOnce,
      viewOnceConfig,
    ],
  );

  const handlePreviewCancel = useCallback(() => {
    setPreviewUri(null);
    setPreviewViewOnce(false);
  }, []);

  // audio helpers
  const handleAudioRecorded = useCallback((uri: string) => {
    if (!uri) return;
    setPendingAudioUri(uri);
    setAudioPreviewOpen(true);
  }, []);

  const handleAudioSend = useCallback(
    async (uri: string) => {
      if (!chatId || !currentUser) return;
      try {
        const roomRef = doc(db, "rooms", String(chatId));
        const msgsRef = collection(roomRef, "messages");

        const fileName = `${currentUserId}_${Date.now()}.m4a`;
        const path = `chats/${chatId}/audio/${fileName}`;

        const downloadUrl = await uploadChatAudio(uri, path);

        await addDoc(msgsRef, {
          text: "",
          imageUri: null,
          videoUri: null,
          audioUri: downloadUrl,
          userId: currentUserId,
          userName: currentUserName,
          createdAt: serverTimestamp(),
          viewOnce: previewViewOnce,
          viewConfig: previewViewOnce ? viewOnceConfig : null,
        });
      } catch (e: any) {
        console.warn("audio send error", e?.message);
      } finally {
        setPendingAudioUri(null);
        setEditConfig(null);
        setPreviewViewOnce(false);
        setViewOnceConfig({
          mode: "views",
          maxViews: 1,
          timeLimitSeconds: null,
        });
      }
    },
    [
      chatId,
      currentUser,
      currentUserId,
      currentUserName,
      previewViewOnce,
      viewOnceConfig,
    ],
  );

  const handleAudioDiscard = useCallback(() => {
    setPendingAudioUri(null);
    setEditConfig(null);
  }, []);

  const handleAudioEdit = useCallback((durationMs: number) => {
    setEditDurationMs(durationMs);
    setEditSheetOpen(true);
  }, []);

  const handleApplyEdit = useCallback((cfg: any) => {
    setEditConfig(cfg);
  }, []);

  // view-once consume
  const handleViewOnceClose = useCallback(
    async (msg: Message | null) => {
      const toConsume = msg;
      setOpenViewOnceMsg(null);
      if (!toConsume || !toConsume.viewOnce || !chatId) return;

      try {
        const msgRef = doc(
          db,
          "rooms",
          String(chatId),
          "messages",
          toConsume.id,
        );

        const updates: any = {};
        if (!toConsume.firstViewAt) {
          updates.firstViewAt = serverTimestamp();
        }

        const cfg = toConsume.viewConfig;
        const isViewsMode =
          cfg?.mode === "views" || cfg?.mode === "both";
        const rawMaxViews = cfg?.maxViews ?? 1;
        const maxViews = isViewsMode ? rawMaxViews : 0;
        const noViewCap = !maxViews || maxViews <= 0;

        if (!noViewCap) {
          updates[`perUserViews.${currentUserId}`] = increment(1);
        }

        await updateDoc(msgRef, updates);
      } catch (e: any) {
        console.warn("viewOnce consume error", e?.message);
      }
    },
    [chatId, currentUserId],
  );

  const updateMessage = useCallback(
    async (m: Message, newText: string) => {
      if (!chatId || !m?.id) return;
      if (m.deleted) return;
      try {
        const msgRef = doc(
          db,
          "rooms",
          String(chatId),
          "messages",
          String(m.id),
        );
        await updateDoc(msgRef, {
          text: newText,
        });
      } catch (e: any) {
        console.warn("update message error", e?.message);
      }
    },
    [chatId],
  );

  const handleDeleteForMe = useCallback(
    async (m: Message) => {
      if (!chatId || !m?.id || !currentUserId) return;
      if (m.deleted) return;
      try {
        const stateRef = doc(
          db,
          "rooms",
          String(chatId),
          "readState",
          currentUserId,
        );

        const snap = await getDoc(stateRef);
        const data = (snap.data() as any) || {};
        const deleted: string[] = data.deletedMessageIds ?? [];

        if (deleted.includes(m.id)) return;

        const next = [...deleted, m.id];

        await setDoc(
          stateRef,
          { deletedMessageIds: next },
          { merge: true },
        );
      } catch (e: any) {
        console.warn("delete for me error", e?.message);
      }
    },
    [chatId, currentUserId],
  );

  const copyMessageText = useCallback((m: Message) => {
    if (!m.text) return;
    if (m.deleted) return;
    Clipboard.setString(m.text);
  }, []);

  // ⭐ Saved toggle
  const saveMessageMedia = useCallback(
    async (m: Message) => {
      const currentUser = auth.currentUser;
      const currentUserId = currentUser?.uid ?? "anon";
      if (!chatId || !m?.id || !currentUserId) return;
      if (m.deleted) return;
      try {
        const msgRef = doc(
          db,
          "rooms",
          String(chatId),
          "messages",
          String(m.id),
        );

        const existing: string[] = m.savedBy ?? [];
        const set = new Set(existing);

        if (set.has(currentUserId)) {
          set.delete(currentUserId); // un-save
        } else {
          set.add(currentUserId); // save
        }

        const next = Array.from(set);
        await updateDoc(msgRef, { savedBy: next });
      } catch (e: any) {
        console.warn("save media error", e?.message);
      }
    },
    [chatId],
  );

  const togglePinMessage = useCallback((m: Message) => {
    if (m.deleted) return;
    // TODO: implement pin logic
  }, []);

  const openForwardSheet = useCallback((m: Message) => {
    if (m.deleted) return;
    // TODO: open forward UI
  }, []);

  const openStickerPickerForMessage = useCallback((m: Message) => {
    setStickerSheetOpen(true);
    if (m.deleted) return;
  }, []);

  const openEmojiPickerForMessage = useCallback((m: Message) => {
    setEmojiPickerTarget(m);
    setEmojiSheetOpen(true);
    if (m.deleted) return;
  }, []);

  const sendEmojiReaction = useCallback(
  async (emoji: string) => {
    if (!chatId || !currentUserId || !emojiPickerTarget?.id) return;
    if (emojiPickerTarget.deleted) return; // check early

    const msgRef = doc(
      db,
      "rooms",
      String(chatId),
      "messages",
      emojiPickerTarget.id,
    );

    const newReactions = {
      ...(emojiPickerTarget.reactions ?? {}),
      [currentUserId]: emoji, // one reaction per user
    };

    try {
      await updateDoc(msgRef, { reactions: newReactions });
    } catch (e: any) {
      console.warn("emoji reaction error", e?.message);
    } finally {
      setEmojiPickerTarget(null);
    }
  },
  [chatId, currentUserId, emojiPickerTarget],
);

  const removeReaction = useCallback(
    async (m: Message, kind: "emoji" | "sticker", userId: string) => {
      if (!chatId || !m?.id) return;
      if (m.deleted) return;
      const msgRef = doc(
        db,
        "rooms",
        String(chatId),
        "messages",
        String(m.id),
      );

      const updates: any = {};

      if (kind === "emoji") {
        const copy = { ...(m.reactions ?? {}) };
        delete copy[userId];
        updates.reactions = copy;
      } else {
        const copy = { ...(m.stickerReactions ?? {}) };
        delete copy[userId];
        updates.stickerReactions = copy;
      }

      try {
        await updateDoc(msgRef, updates);
      } catch (e: any) {
        console.warn("remove reaction error", e?.message);
      }
    },
    [chatId],
  );

  const setSelectedMessageForReport = useCallback(
    (m: Message | null) => {
      if (m && m.deleted) return;
      setSelectedMessage(m);
    },
    [setSelectedMessage],
  );

  const headerTitle = isGeneral
    ? "UPDATES"
    : isCorneal
    ? "CORNEAL"
    : otherDisplayName || chatName || chatId;

  const headerSubtitle = othersTyping
    ? "Typing..."
    : isGeneral
    ? "Important updates from the node"
    : isCorneal
    ? "AI link channel"
    : "Direct chat";

  const presence: "online" | "offline" =
    isGeneral || isCorneal ? "offline" : "online";

  const clearLocalHistory = useCallback(async () => {
    if (!chatId || !currentUserId) return;
    const now = Date.now();
    setHiddenUntil(now);

    const stateRef = doc(
      db,
      "rooms",
      String(chatId),
      "readState",
      currentUserId,
    );
    await setDoc(
      stateRef,
      { clearedAt: now },
      { merge: true },
    );
  }, [chatId, currentUserId]);

  // poll vote
const handlePollVote = useCallback(
  async (msg: Message, optionId: string) => {
    if (!chatId || !currentUserId || msg.type !== "poll" || !msg.id) return;
    if (msg.deleted) return;
    const msgRef = doc(db, "rooms", String(chatId), "messages", msg.id);

    const currentVotes = msg.votesByUser?.[currentUserId] ?? [];

    let nextVotes: string[];
    if (msg.allowMulti) {
      if (currentVotes.includes(optionId)) {
        nextVotes = currentVotes.filter((id) => id !== optionId);
      } else {
        nextVotes = [...currentVotes, optionId];
      }
    } else {
      nextVotes = [optionId];
    }

    const newVotesByUser = {
      ...(msg.votesByUser ?? {}),
      [currentUserId]: nextVotes,
    };

    try {
      await updateDoc(msgRef, { votesByUser: newVotesByUser });
    } catch (e: any) {
      console.warn("poll vote error", e?.message);
    }
  },
  [chatId, currentUserId],
);

// sticker preview send (message + reaction modes)
const sendStickerFromPreview = useCallback(
  async () => {
    if (!chatId || !currentUser || !stickerPreviewUrl) return;

    try {
      // 1) Reaction path – unchanged
      if (stickerReactionTarget) {
        const msgRef = doc(
          db,
          "rooms",
          String(chatId),
          "messages",
          String(stickerReactionTarget.id),
        );

        const prevReactions = stickerReactionTarget.stickerReactions ?? {};
        const newReactions = {
          ...prevReactions,
          [currentUserId]: stickerPreviewUrl,
        };

        await updateDoc(msgRef, { stickerReactions: newReactions });

        setStickerReactionTarget(null);
        setStickerPreviewUrl(null);
        setStickerPreviewViewOnce(false);
        return;
      }

      const roomRef = doc(db, "rooms", String(chatId));
      const msgsRef = collection(roomRef, "messages");

      // 2) NEW: upload local file:// sticker and get a stable URL
      let finalStickerUri = stickerPreviewUrl;
      if (stickerPreviewUrl.startsWith("file://")) {
        const fileName = `${currentUserId}-${Date.now()}.png`;
        const path = `stickers/${chatId}/${fileName}`;
        finalStickerUri = await uploadChatImage(stickerPreviewUrl, path);
      }

      // 3) Save message with stable URL
      await addDoc(msgsRef, {
        type: "sticker",
        stickerKind: "image",
        text: "",
        stickerUri: finalStickerUri,
        imageUri: null,
        videoUri: null,
        audioUri: null,
        userId: currentUserId,
        userName: currentUserName,
        createdAt: serverTimestamp(),
        viewOnce: stickerPreviewViewOnce,
        viewConfig: stickerPreviewViewOnce ? viewOnceConfig : null,
      });

      try {
        const userRef = doc(db, "users", currentUserId);
        await updateDoc(userRef, {
          statsStickersSent: increment(1),
        });
      } catch (e: any) {
        console.warn(
          "increment statsStickersSent error",
          (e as any)?.message,
        );
      }

      setStickerPreviewUrl(null);
      setStickerPreviewViewOnce(false);
    } catch (e: any) {
      console.warn("sticker preview send error", e?.message);
    }
  },
  [
    chatId,
    currentUser,
    stickerPreviewUrl,
    stickerPreviewViewOnce,
    viewOnceConfig,
    currentUserId,
    currentUserName,
    stickerReactionTarget,
  ],
);

// contact send
const sendContact = useCallback(
  async (user: { id: string; name: string }) => {
    if (!chatId || !currentUser) return;

    const roomRef = doc(db, "rooms", String(chatId));
    const msgsRef = collection(roomRef, "messages");

    await addDoc(msgsRef, {
      type: "contact",
      contactUserId: user.id,
      contactUserName: user.name,
      text: "",
      imageUri: null,
      videoUri: null,
      audioUri: null,
      userId: currentUserId,
      userName: currentUserName,
      createdAt: serverTimestamp(),
    });

    setContactPickerOpen(false);
  },
  [chatId, currentUser, currentUserId, currentUserName],
);

// disappearing config
const saveDisappearingConfig = useCallback(
  async (enabled: boolean, durationSeconds: number) => {
    if (!chatId) return;
    if (!canUseDisappearing) return;
    try {
      const roomRef = doc(db, "rooms", String(chatId));
      await setDoc(
        roomRef,
        {
          disappearing: {
            enabled,
            durationSeconds,
          },
        },
        { merge: true },
      );
      setDisappearingEnabled(enabled);
      setDisappearingDurationSeconds(durationSeconds);
    } catch (e: any) {
      console.warn("save disappearing config error", e?.message);
    }
  },
  [chatId],
);

// poll send
const sendPoll = useCallback(
  async ({
    question,
    options,
    allowMulti,
  }: {
    question: string;
    options: string[];
    allowMulti: boolean;
  }) => {
    if (!chatId || !currentUser) return;

    const roomRef = doc(db, "rooms", String(chatId));
    const msgsRef = collection(roomRef, "messages");

    await addDoc(msgsRef, {
      type: "poll",
      question,
      options: options.map((label, index) => ({
        id: String(index),
        label,
      })),
      allowMulti,
      votesByUser: {},
      text: "",
      imageUri: null,
      videoUri: null,
      audioUri: null,
      userId: currentUserId,
      userName: currentUserName,
      createdAt: serverTimestamp(),
    });

    await setDoc(
      roomRef,
      {
        lastMessage: `[Poll] ${question}`,
        lastMessageAt: serverTimestamp(),
      },
      { merge: true },
    );

    setPollSheetOpen(false);
  },
  [chatId, currentUser, currentUserId, currentUserName],
);

// inside useChatScreenState

function durationToMs(d: {
  years: number;
  months: number;
  weeks: number;
  days: number;
  hours: number;
  minutes: number;
  seconds: number;
}) {
  const dayMs = 24 * 60 * 60 * 1000;
  const totalDays =
    d.years * 365 + d.months * 30 + d.weeks * 7 + d.days;

  return (
    totalDays * dayMs +
    d.hours * 60 * 60 * 1000 +
    d.minutes * 60 * 1000 +
    d.seconds * 1000
  );
}

const sendEvent = useCallback(
  async ({
    title,
    startsAtText,
    location,
    notes,
    duration, // <— NEW from EventComposerSheet
  }: {
    title: string;
    startsAtText: string;
    location?: string | null;
    notes?: string | null;
    duration: {
      years: number;
      months: number;
      weeks: number;
      days: number;
      hours: number;
      minutes: number;
      seconds: number;
    };
  }) => {
    if (!chatId || !currentUser) return;

    const roomRef = doc(db, "rooms", String(chatId));
    const msgsRef = collection(roomRef, "messages");

    const durationMs = durationToMs(duration);
    const targetTs = Date.now() + durationMs;

    await addDoc(msgsRef, {
      type: "event",
      eventTitle: title,
      eventStartsAt: startsAtText,
      eventLocation: location ?? null,
      eventNotes: notes ?? null,
      countdownDuration: duration, // store full breakdown
      targetTs,                    // absolute target in ms
      text: "",
      userId: currentUserId,
      userName: currentUserName,
      createdAt: serverTimestamp(),
    });

    await setDoc(
  roomRef,
  {
    lastMessage: `[Event] ${title}`,
    lastMessageAt: serverTimestamp(),
  },
  { merge: true },
);

setEventSheetOpen(false);

// schedule vibration alarm on this device
scheduleCountdownAlarm(targetTs, title).catch(() => {});
  },
  [chatId, currentUser, currentUserId, currentUserName],
);

  return {
    chatId,
    onBack,
    headerTitle,
    removeReaction,
    headerSubtitle,
    presence,
    otherAvatarUrl,
    otherUserId,
    messages: visibleMessages,
    pinSheetOpen,
    setPinSheetOpen,
    pinTargetMessage,
    setPinTargetMessage,
    confirmPinWithDuration,
    savedMessagesForMe,

    hiddenUntil,
    setHiddenUntil,
    clearLocalHistory,
    clearSheetOpen,
    setClearSheetOpen,
    clearDurationMinutes,
    setClearDurationMinutes,

    currentUserId,
    nowTick,
    isCorneal,
    isGeneral,
    canActInGeneral,
    input,
    setInput,
    sending,
    isTyping,
    setTyping,
    toolsOpen,
    setToolsOpen,
    handleSend,
    handleToolAction,
    chatTheme,
    themeSheetOpen,
    setThemeSheetOpen,
    setChatTheme,
    headerPinnedMessages,
    unpinMessage,
    otherDisplayName,

    selectedMessage,
    setSelectedMessage,
    messageActionsOpen,
    setMessageActionsOpen,
    handleDeleteMessage,
    selectedReplyMessage,
    setSelectedReplyMessage,
    updateMessage,
    handleDeleteForMe,
    copyMessageText,
    saveMessageMedia,
    togglePinMessage,
    openForwardSheet,
    openStickerPickerForMessage,
    openEmojiPickerForMessage,
    setSelectedMessageForReport,
    stickerReactionTarget,
    setStickerReactionTarget,

    previewUri,
    setPreviewUri,
    previewType,
    setPreviewType,
    previewViewOnce,
    setPreviewViewOnce,
    viewOnceConfig,
    setViewOnceConfig,
    viewOnceConfigOpen,
    setViewOnceConfigOpen,
    openViewOnceMsg,
    setOpenViewOnceMsg,
    galleryOpen,
    setGalleryOpen,
    galleryItems,
    audioPanelOpen,
    setAudioPanelOpen,
    audioItems,
    voiceSheetOpen,
    setVoiceSheetOpen,
    audioPreviewOpen,
    setAudioPreviewOpen,
    pendingAudioUri,
    setPendingAudioUri,
    editSheetOpen,
    setEditSheetOpen,
    editDurationMs,
    setEditDurationMs,
    editConfig,
    setEditConfig,
    stickerSheetOpen,
    setStickerSheetOpen,
    stickerPreviewUrl,
    setStickerPreviewUrl,
    stickerPreviewViewOnce,
    setStickerPreviewViewOnce,
    linkPickerOpen,
    setLinkPickerOpen,
    groups,
    setGroups,
    contactPickerOpen,
    setContactPickerOpen,
    dmContacts,
    setDmContacts,
    eventSheetOpen,
    setEventSheetOpen,
    pollSheetOpen,
    setPollSheetOpen,
    emojiSheetOpen,
    setEmojiSheetOpen,
    emojiPickerTarget,
    setEmojiPickerTarget,
    sendEmojiReaction,

    handlePreviewSend,
    handlePreviewCancel,
    handleAudioRecorded,
    handleAudioSend,
    handleAudioDiscard,
    handleAudioEdit,
    handleApplyEdit,
    handleViewOnceClose,
    sendStickerFromPreview,
    sendContact,
    sendPoll,
    handlePollVote,
    sendEvent,
    disappearingSheetOpen,
    setDisappearingSheetOpen,
    disappearingEnabled,
    disappearingDurationSeconds,
    saveDisappearingConfig,
    reportSheetOpen,
    setReportSheetOpen,
    reportSubmittedSheetOpen,
    setReportSubmittedSheetOpen,
    reportsForChat,
    clearReportsForChat,

    isBlocked,
    iBlockedThem,
    searchOpen,
    setSearchOpen,
    searchQuery,
    setSearchQuery,
  };
}
