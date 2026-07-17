// src/member/chat/ChatList.tsx

import { auth, db } from "@/src/api/firebase";
import type { UserProfile } from "@/src/hooks/useUserProfile";
import ChatListBottom from "@/src/member/chat/ChatListBottom";
import ChatListHeader from "@/src/member/chat/ChatListHeader";
import HelpAssistantBubble from "@/src/member/help/HelpAssistantBubble";
import HelpAssistantPanel from "@/src/member/help/HelpAssistantPanel";
import { useTheme } from "@/src/themes/ThemeContext";
import { router, useSegments } from "expo-router";
import {
  collection,
  doc,
  query as fsQuery,
  getCountFromServer,
  getDoc,
  getDocs,
  limit,
  onSnapshot,
  orderBy,
  where,
} from "firebase/firestore";
import React, { useEffect, useRef, useState } from "react";
import {
  Animated,
  FlatList,
  Image,
  PanResponder,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

type AvatarRemote = {
  type: "remote";
  uri: string;
};

type AvatarLocal = {
  type: "local";
  source: any;
};

type AvatarInitial = {
  type: "initial";
};

type ChatAvatar = AvatarRemote | AvatarLocal | AvatarInitial;

export type ChatItem = {
  id: string; // friend uid or "general"/"corneal"
  name: string;
  lastMessage: string;
  time: string;
  unread: number;
  avatar: ChatAvatar;
  email?: string | null;
  lastMessageAt?: number;
};

// static metadata only (name + avatar)
export const CHATS_META: ChatItem[] = [
  {
    id: "general",
    name: "Cedal System Feed",
    lastMessage: "",
    time: "",
    unread: 0,
    avatar: {
      type: "local",
      source: require("./general.png"),
    },
  },
  {
    id: "corneal",
    name: "Corneal",
    lastMessage: "",
    time: "",
    unread: 0,
    avatar: {
      type: "local",
      source: require("./corneal.png"),
    },
  },
];

function buildRoomId(a: string, b: string) {
  return a < b ? `${a}_${b}` : `${b}_${a}`;
}

function sortByLastMessageDesc(list: ChatItem[]): ChatItem[] {
  return [...list].sort(
    (a, b) => (b.lastMessageAt || 0) - (a.lastMessageAt || 0),
  );
}

export default function ChatList() {
  const { colors } = useTheme();
  const segments = useSegments();
  const currentPath = "/" + segments.join("/");

  const [items, setItems] = useState<ChatItem[]>([]);

  // help assistant state
  const [showHelp, setShowHelp] = useState(false);
  const helpPos = useRef(new Animated.ValueXY({ x: 0, y: 0 })).current;
  const isDraggingHelp = useRef(false);

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => false,
      onMoveShouldSetPanResponder: (_e, gesture) => {
        const moved =
          Math.abs(gesture.dx) > 5 || Math.abs(gesture.dy) > 5;
        if (moved) {
          isDraggingHelp.current = true;
        }
        return moved;
      },
      onPanResponderMove: Animated.event(
        [null, { dx: helpPos.x, dy: helpPos.y }],
        { useNativeDriver: false },
      ),
      onPanResponderRelease: () => {
        helpPos.extractOffset();
        setTimeout(() => {
          isDraggingHelp.current = false;
        }, 0);
      },
      onPanResponderTerminate: () => {
        helpPos.extractOffset();
        isDraggingHelp.current = false;
      },
    }),
  ).current;

  // helper: DM chat row (friend)
  const loadUserChatItem = async (friendUid: string): Promise<ChatItem | null> => {
    const current = auth.currentUser;
    if (!current) return null;

    try {
      // 1) profile
      const userSnap = await getDoc(doc(db, "users", friendUid));
      if (!userSnap.exists()) return null;
      const u = userSnap.data() as UserProfile | any;
      const nickname = u.nickname || "";
      const email = u.email ?? null;
      const name = nickname || email || "Unknown";

      const avatarUrl =
        u.avatarUrl ||
        `https://ui-avatars.com/api/?name=${encodeURIComponent(
          name,
        )}&background=0f172a&color=e5e7eb`;

      // 2) room + messages
      const roomId = buildRoomId(current.uid, friendUid);
      const roomRef = doc(db, "rooms", roomId);
      const msgsRef = collection(roomRef, "messages");

      // last message
      const lastMsgQuery = fsQuery(
        msgsRef,
        orderBy("createdAt", "desc"),
        limit(1),
      );
      const lastMsgSnap = await getDocs(lastMsgQuery);

      let lastMessage = "Say hi 👋";
      let lastMessageAt: Date | null = null;

      if (!lastMsgSnap.empty) {
        const d = lastMsgSnap.docs[0];
        const data = d.data() as any;
        lastMessage = data.text || "Attachment";
        lastMessageAt = data.createdAt?.toDate?.() ?? null;
      }

      // 3) read state
      const stateRef = doc(roomRef, "readState", current.uid);
      const stateSnap = await getDoc(stateRef);
      const rawLastRead = stateSnap.exists()
        ? (stateSnap.data() as any).lastReadAt
        : null;
      const lastReadAt: Date | null =
        rawLastRead && typeof rawLastRead.toDate === "function"
          ? rawLastRead.toDate()
          : null;

      // 4) unread count (messages from friend after lastReadAt)
      let unread = 0;
      let unreadQuery;

      if (lastReadAt) {
        unreadQuery = fsQuery(
          msgsRef,
          where("createdAt", ">", lastReadAt),
          where("userId", "==", friendUid),
        );
      } else {
        unreadQuery = fsQuery(
          msgsRef,
          where("userId", "==", friendUid),
        );
      }

      const countSnap = await getCountFromServer(unreadQuery);
      unread = countSnap.data().count;

      // 5) time label
      const time = lastMessageAt
        ? lastMessageAt.toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
          })
        : "";

      return {
        id: friendUid,
        name,
        lastMessage,
        time,
        unread,
        avatar: { type: "remote", uri: avatarUrl },
        email,
        lastMessageAt: lastMessageAt ? lastMessageAt.getTime() : 0,
      };
    } catch (e) {
      console.error("Error loading friend chat item", e);
      return null;
    }
  };

  // helper: system rooms general/corneal
  async function loadSystemChatItem(
    roomId: "general" | "corneal",
  ): Promise<ChatItem | null> {
    const current = auth.currentUser;
    if (!current) return null;

    try {
      const roomRef = doc(db, "rooms", roomId);
      const msgsRef = collection(roomRef, "messages");

      // last message
      const lastMsgQuery = fsQuery(
        msgsRef,
        orderBy("createdAt", "desc"),
        limit(1),
      );
      const lastMsgSnap = await getDocs(lastMsgQuery);

      const meta = CHATS_META.find((c) => c.id === roomId);
      let lastMessage =
        meta?.lastMessage ||
        (roomId === "general"
          ? "System feed and global updates."
          : "Corneal chat waiting for you.");
      let lastMessageAt: Date | null = null;

      if (!lastMsgSnap.empty) {
        const d = lastMsgSnap.docs[0];
        const data = d.data() as any;
        lastMessage = data.text || "Attachment";
        lastMessageAt = data.createdAt?.toDate?.() ?? null;
      }

      // read state
      const stateRef = doc(roomRef, "readState", current.uid);
      const stateSnap = await getDoc(stateRef);
      const rawLastRead = stateSnap.exists()
        ? (stateSnap.data() as any).lastReadAt
        : null;
      const lastReadAt: Date | null =
        rawLastRead && typeof rawLastRead.toDate === "function"
          ? rawLastRead.toDate()
          : null;

      // unread = messages not sent by me after lastReadAt
      let unread = 0;
      let unreadQuery;

      if (lastReadAt) {
        unreadQuery = fsQuery(
          msgsRef,
          where("createdAt", ">", lastReadAt),
          where("userId", "!=", current.uid),
        );
      } else {
        unreadQuery = fsQuery(
          msgsRef,
          where("userId", "!=", current.uid),
        );
      }

      const countSnap = await getCountFromServer(unreadQuery);
      unread = countSnap.data().count;

      const time = lastMessageAt
        ? lastMessageAt.toLocaleTimeString([], {
            hour: "2-digit",
            minute: "2-digit",
          })
        : "";

      const avatar = meta?.avatar ?? { type: "initial" as const };

      return {
        id: roomId,
        name: meta?.name ?? roomId,
        lastMessage,
        time,
        unread,
        avatar,
        lastMessageAt: lastMessageAt ? lastMessageAt.getTime() : 0,
      };
    } catch (e) {
      console.error("Error loading system chat item", e);
      return null;
    }
  }

  // load accepted friends + general/corneal
  useEffect(() => {
    const current = auth.currentUser;
    if (!current) return;

    const baseRef = collection(db, "friendRequests");

    const qOut = fsQuery(
      baseRef,
      where("fromId", "==", current.uid),
      where("status", "==", "accepted"),
    );

    const qIn = fsQuery(
      baseRef,
      where("toId", "==", current.uid),
      where("status", "==", "accepted"),
    );

    const unsubOut = onSnapshot(qOut, async (snap) => {
      const itemsSnap = await Promise.all(
        snap.docs.map(async (d) => {
          const data = d.data() as any;
          const otherId = data.toId as string;
          return loadUserChatItem(otherId);
        }),
      );
      const cleaned = itemsSnap.filter(
        (x): x is ChatItem => !!x,
      );
      setItems((prev) => {
        const withoutThese = prev.filter(
          (c) => !cleaned.find((n) => n.id === c.id),
        );
        return sortByLastMessageDesc([...withoutThese, ...cleaned]);
      });
    });

    const unsubIn = onSnapshot(qIn, async (snap) => {
      const itemsSnap = await Promise.all(
        snap.docs.map(async (d) => {
          const data = d.data() as any;
          const otherId = data.fromId as string;
          return loadUserChatItem(otherId);
        }),
      );
      const cleaned = itemsSnap.filter(
        (x): x is ChatItem => !!x,
      );
      setItems((prev) => {
        const withoutThese = prev.filter(
          (c) => !cleaned.find((n) => n.id === c.id),
        );
        return sortByLastMessageDesc([...withoutThese, ...cleaned]);
      });
    });

    // listen to general + corneal
    const roomIds: ("general" | "corneal")[] = ["general", "corneal"];
    const unsubsRooms = roomIds.map((roomId) => {
      const roomRef = doc(db, "rooms", roomId);
      const msgsRef = collection(roomRef, "messages");
      return onSnapshot(msgsRef, async () => {
        const item = await loadSystemChatItem(roomId);
        if (!item) return;
        setItems((prev) => {
          const without = prev.filter((c) => c.id !== roomId);
          return sortByLastMessageDesc([...without, item]);
        });
      });
    });

    // initial load of system rooms (in case no messages yet)
    (async () => {
      const initialSystem = await Promise.all(
        roomIds.map((id) => loadSystemChatItem(id)),
      );
      const cleaned = initialSystem.filter(
        (x): x is ChatItem => !!x,
      );
      if (cleaned.length) {
        setItems((prev) =>
          sortByLastMessageDesc([
            ...prev.filter(
              (c) => !cleaned.find((n) => n.id === c.id),
            ),
            ...cleaned,
          ]),
        );
      }
    })();

    return () => {
      unsubOut();
      unsubIn();
      unsubsRooms.forEach((u) => u());
    };
  }, []);

  function openChat(chat: ChatItem) {
    const current = auth.currentUser;

    let effectiveChatId = chat.id;

    if (
      current &&
      chat.id !== "general" &&
      chat.id !== "corneal"
    ) {
      effectiveChatId = buildRoomId(current.uid, chat.id);
    }

    // optimistic: clear unread locally
    setItems((prev) =>
      prev.map((c) =>
        c.id === chat.id ? { ...c, unread: 0 } : c,
      ),
    );

    const params: any = {
      chatId: effectiveChatId,
      chatName: chat.name,
      otherUserEmail: chat.email ?? null,
    };

    if (chat.avatar.type === "remote") {
      params.chatAvatarType = "remote";
      params.chatAvatarUri = chat.avatar.uri;
    } else if (chat.avatar.type === "local") {
      params.chatAvatarType = "local";
    } else if (chat.avatar.type === "initial") {
      params.chatAvatarType = "initial";
    }

    router.push({
      pathname: "/(auth)/(member)/(chat)/[chatId]",
      params,
    } as any);
  }

  function renderItem({ item }: { item: ChatItem }) {
    let imageSource: any = null;

    if (item.avatar.type === "local") {
      imageSource = item.avatar.source;
    } else if (item.avatar.type === "remote") {
      imageSource = { uri: item.avatar.uri };
    }

    const initial = item.name.charAt(0).toUpperCase();

    return (
      <TouchableOpacity
        style={[
          styles.row,
          { backgroundColor: colors.chatRowBackground },
        ]}
        activeOpacity={0.7}
        onPress={() => openChat(item)}
      >
        {imageSource ? (
          <Image source={imageSource} style={styles.avatar} />
        ) : (
          <View style={styles.initialAvatar}>
            <Text style={styles.initialText}>{initial}</Text>
          </View>
        )}

        <View
          style={[
            styles.middle,
            { borderBottomColor: colors.border },
          ]}
        >
          <View style={styles.nameRow}>
            <Text
              numberOfLines={1}
              style={[
                styles.name,
                { color: colors.textPrimary },
              ]}
            >
              {item.name}
            </Text>
            <Text
              style={[
                styles.time,
                { color: colors.textSecondary },
              ]}
            >
              {item.time}
            </Text>
          </View>

          <View style={styles.bottomRow}>
            <Text
              numberOfLines={1}
              style={[
                styles.lastMessage,
                { color: colors.textSecondary },
              ]}
            >
              {item.lastMessage}
            </Text>
            {item.unread > 0 && (
              <View style={styles.unreadBadge}>
                <Text style={styles.unreadText}>
                  {item.unread > 99 ? "99+" : item.unread}
                </Text>
              </View>
            )}
          </View>
        </View>
      </TouchableOpacity>
    );
  }

  function handleHelpBubblePress() {
    if (!isDraggingHelp.current) {
      setShowHelp((prev) => !prev);
    }
  }

  return (
    <View
      style={[styles.screen, { backgroundColor: colors.background }]}
    >
      <ChatListHeader />

      <FlatList
        data={items}
        keyExtractor={(item) => item.id}
        renderItem={renderItem}
        ItemSeparatorComponent={() => (
          <View style={styles.separator} />
        )}
        contentContainerStyle={{ paddingBottom: 80 }}
        style={styles.list}
      />

      {/* Draggable help assistant bubble + small panel */}
      <Animated.View
        {...panResponder.panHandlers}
        style={[
          styles.helpAnchor,
          { transform: helpPos.getTranslateTransform() },
        ]}
      >
        <HelpAssistantBubble onPress={handleHelpBubblePress} />

        {showHelp && (
          <HelpAssistantPanel
            onClose={() => setShowHelp(false)}
            style={styles.helpPanelOffset}
            userId={auth.currentUser?.uid ?? null}
          />
        )}
      </Animated.View>

      <ChatListBottom active="chats" />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  list: { flex: 1 },
  row: {
    flexDirection: "row",
    paddingHorizontal: 12,
    paddingVertical: 10,
    alignItems: "center",
  },
  avatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    marginRight: 10,
  },
  initialAvatar: {
    width: 48,
    height: 48,
    borderRadius: 24,
    marginRight: 10,
    backgroundColor: "#0f172a",
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
  },
  initialText: {
    fontSize: 20,
    color: "#e5e7eb",
    fontWeight: "600",
  },
  middle: {
    flex: 1,
    borderBottomWidth: StyleSheet.hairlineWidth,
    paddingBottom: 6,
  },
  nameRow: {
    flexDirection: "row",
    alignItems: "center",
  },
  name: {
    flex: 1,
    fontSize: 15,
    fontWeight: "600",
  },
  time: { fontSize: 11 },
  bottomRow: {
    marginTop: 2,
    flexDirection: "row",
    alignItems: "center",
  },
  lastMessage: {
    flex: 1,
    fontSize: 13,
  },
  unreadBadge: {
    minWidth: 20,
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 999,
    backgroundColor: "#22c55e",
    alignItems: "center",
    justifyContent: "center",
    marginLeft: 6,
  },
  unreadText: {
    color: "#020617",
    fontSize: 11,
    fontWeight: "700",
  },
  separator: { height: 0 },
  helpAnchor: {
    position: "absolute",
    right: 20,
    bottom: 110,
    zIndex: 40,
  },
  helpPanelOffset: {
    marginTop: 8,
  },
});
