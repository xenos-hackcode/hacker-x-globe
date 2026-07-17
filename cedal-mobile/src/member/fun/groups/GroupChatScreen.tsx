import React, { useEffect, useState, useCallback } from "react";
import {
  StyleSheet,
  FlatList,
  KeyboardAvoidingView,
  Platform,
  View,
  Text,
  TouchableOpacity,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

import { auth, db } from "@/src/api/firebase";
import {
  addDoc,
  collection,
  doc,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  updateDoc,
  increment,
  getDoc,
} from "firebase/firestore";

import ChatInputBar from "@/src/member/chat/ChatInputBar";
import ChatToolsPanel from "@/src/member/chat/ChatToolsPanel";
import { MessageRow, Message } from "@/src/member/chat/MessageRow";
import { ViewOnceOverlay } from "@/src/member/chat/ViewOnceOverlay";

import { openChatCamera } from "@/src/member/utils/ChatCamera";
import { openChatVideo } from "@/src/member/utils/chatVideo";
import { uploadChatImage } from "@/src/member/utils/uploadChatImage";
import { uploadChatVideo } from "@/src/member/utils/uploadChatVideo";
import { uploadChatAudio } from "@/src/member/utils/uploadChatAudio";
import { MediaPreviewSheet } from "@/src/member/utils/MediaPreviewSheet";
import {
  ViewOnceConfig,
  ViewOnceConfigSheet,
} from "@/src/member/utils/ViewOnceConfigSheet";
import { VoiceRecorderSheet } from "@/src/member/chat/VoiceRecorderSheet";
import { VoicePreviewSheet } from "@/src/member/chat/VoicePreviewSheet";
import {
  VoiceEditSheet,
  VoiceEditConfig,
} from "@/src/member/chat/VoiceEditSheet";
import { StickerPickerSheet } from "@/src/member/chat/StickerPickerSheet";
import { StickerPreviewSheet } from "@/src/member/chat/StickerPreviewSheet";

import GroupProfileOverlay from "@/src/member/fun/groups/GroupProfileOverlay";

export type GroupChatScreenProps = {
  groupId: string;
  groupName?: string;
  onBack: () => void;
};

export default function GroupChatScreen({
  groupId,
  groupName,
  onBack,
}: GroupChatScreenProps) {
  // core/chat state
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [nowTick, setNowTick] = useState(Date.now());

  const currentUser = auth.currentUser;
  const currentUserId = currentUser?.uid ?? "anon";
  const currentUserName =
    currentUser?.displayName || currentUser?.email || "You";

  // tools / media state
  const [toolsOpen, setToolsOpen] = useState(false);
  const [stickerSheetOpen, setStickerSheetOpen] = useState(false);

  const [previewUri, setPreviewUri] = useState<string | null>(null);
  const [previewType, setPreviewType] = useState<"image" | "video">("image");
  const [previewViewOnce, setPreviewViewOnce] = useState(false);
  const [viewOnceConfig, setViewOnceConfig] = useState<ViewOnceConfig>({
    mode: "views",
    maxViews: 1,
    timeLimitSeconds: null,
  });
  const [viewOnceConfigOpen, setViewOnceConfigOpen] = useState(false);

  const [openViewOnceMsg, setOpenViewOnceMsg] = useState<Message | null>(null);

  // voice
  const [voiceSheetOpen, setVoiceSheetOpen] = useState(false);
  const [audioPreviewOpen, setAudioPreviewOpen] = useState(false);
  const [pendingAudioUri, setPendingAudioUri] = useState<string | null>(null);
  const [editSheetOpen, setEditSheetOpen] = useState(false);
  const [editDurationMs, setEditDurationMs] = useState(1);
  const [editConfig, setEditConfig] = useState<VoiceEditConfig | null>(null);

  // stickers
  const [stickerPreviewUrl, setStickerPreviewUrl] = useState<string | null>(
    null,
  );
  const [stickerPreviewViewOnce, setStickerPreviewViewOnce] =
    useState(false);

  // group meta / overlay
  const [infoOpen, setInfoOpen] = useState(false);
  const [groupMeta, setGroupMeta] = useState<any | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);

  // tick for view-once timers
  useEffect(() => {
    const id = setInterval(() => setNowTick(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);

  // load group messages: groups/{groupId}/messages
  useEffect(() => {
    if (!groupId) return;

    const msgsRef = collection(db, "groups", groupId, "messages");
    const q = query(msgsRef, orderBy("createdAt", "desc"));

    const unsub = onSnapshot(q, (snap) => {
      const list: Message[] = [];
      snap.forEach((docSnap) => {
        const data = docSnap.data() as any;
        list.push({
          id: docSnap.id,
          text: data.text ?? "",
          createdAt: data.createdAt?.toDate?.() ?? null,
          userId: data.userId ?? "unknown",
          userName: data.userName ?? "Unknown",

          imageUri: data.imageUri ?? null,
          videoUri: data.videoUri ?? null,
          audioUri: data.audioUri ?? null,

          type: data.type ?? "text",
          stickerUri: data.stickerUri ?? null,

          viewOnce: data.viewOnce ?? false,
          viewConfig: data.viewConfig ?? null,
          perUserViews: data.perUserViews ?? {},
          firstViewAt: data.firstViewAt ?? null,
        });
      });
      setMessages(list);
    });

    return () => unsub();
  }, [groupId]);

  // load group meta (you already had this)
  useEffect(() => {
    if (!groupId) return;
    let cancelled = false;

    const loadGroup = async () => {
      try {
        const ref = doc(db, "groups", groupId);
        const snap = await getDoc(ref);
        if (!snap.exists() || cancelled) return;
        const data = snap.data() as any;

        const memberIds: string[] = Array.isArray(data.members)
          ? data.members
          : [];

        const memberDocs = await Promise.all(
          memberIds.map(async (uid) => {
            try {
              const userSnap = await getDoc(doc(db, "users", uid));
              if (!userSnap.exists()) {
                return {
                  id: uid,
                  name: uid,
                  avatarUrl: null,
                };
              }
              const u = userSnap.data() as any;
              const name =
                u.nickname ||
                u.customLink ||
                u.randomLink ||
                u.email ||
                uid;

              return {
                id: uid,
                name,
                avatarUrl: u.avatarUrl ?? null,
              };
            } catch {
              return {
                id: uid,
                name: uid,
                avatarUrl: null,
              };
            }
          }),
        );

        if (cancelled) return;

        setGroupMeta({
          id: snap.id,
          name: data.name,
          description: data.description,
          tags: data.tags ?? [],
          region: data.serverId ?? "Unknown",
          isPrivate: data.isPrivate ?? false,
          membersCount: data.membersCount ?? memberIds.length,
          members: memberDocs,
          serverId: data.serverId ?? "Unknown",
        });
      } catch (e) {
        console.log("Failed to load group meta", e);
      }
    };

    loadGroup();
    return () => {
      cancelled = true;
    };
  }, [groupId]);

  // sending plain text to groups/{groupId}/messages
  const handleSend = useCallback(async () => {
    if (!input.trim() || !groupId || !currentUser) return;

    try {
      setSending(true);
      const text = input.trim();

      const groupRef = doc(db, "groups", groupId);
      const msgsRef = collection(groupRef, "messages");

      await addDoc(msgsRef, {
        text,
        userId: currentUserId,
        userName: currentUserName,
        createdAt: serverTimestamp(),
      });

      setInput("");
    } catch (e: any) {
      console.warn("group send error", e?.message);
    } finally {
      setSending(false);
    }
  }, [input, groupId, currentUser, currentUserId, currentUserName]);

  // tools actions
  const handleToolAction = useCallback(
    async (action: { tool: string; type: string }) => {
      if (!groupId || !currentUser) return;

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
        setStickerSheetOpen(true);
        return;
      }

      console.log("group tool action", action);
    },
    [groupId, currentUser],
  );

  // media preview send (image / video)
  const handlePreviewSend = useCallback(async () => {
    if (!previewUri || !groupId || !currentUser) return;

    try {
      const groupRef = doc(db, "groups", groupId);
      const msgsRef = collection(groupRef, "messages");

      if (previewType === "image") {
        const fileName = `${currentUserId}_${Date.now()}.jpg`;
        const path = `groups/${groupId}/${fileName}`;
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
        const path = `groups/${groupId}/${fileName}`;
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
      console.warn("group preview send error", e?.message);
    }
  }, [
    previewUri,
    previewType,
    groupId,
    currentUser,
    currentUserId,
    currentUserName,
    previewViewOnce,
    viewOnceConfig,
  ]);

  const handlePreviewCancel = useCallback(() => {
    setPreviewUri(null);
    setPreviewViewOnce(false);
  }, []);

  // voice
  const handleAudioRecorded = useCallback((uri: string) => {
    if (!uri) return;
    setPendingAudioUri(uri);
    setAudioPreviewOpen(true);
  }, []);

  const handleAudioSend = useCallback(
    async (uri: string) => {
      if (!groupId || !currentUser) return;
      try {
        const groupRef = doc(db, "groups", groupId);
        const msgsRef = collection(groupRef, "messages");

        const fileName = `${currentUserId}_${Date.now()}.m4a`;
        const path = `groups/${groupId}/audio/${fileName}`;

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
        console.warn("group audio send error", e?.message);
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
      groupId,
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

  const handleApplyEdit = useCallback((cfg: VoiceEditConfig) => {
    setEditConfig(cfg);
  }, []);

  // view once consume for group messages
  const handleViewOnceClose = useCallback(
    async (msg: Message | null) => {
      const toConsume = msg;
      setOpenViewOnceMsg(null);
      if (!toConsume || !toConsume.viewOnce || !groupId) return;

      try {
        const msgRef = doc(
          db,
          "groups",
          String(groupId),
          "messages",
          toConsume.id,
        );

        const updates: any = {};
        if (!toConsume.firstViewAt) {
          updates.firstViewAt = serverTimestamp();
        }

        const cfg = toConsume.viewConfig;
        const isViewsMode = cfg?.mode === "views" || cfg?.mode === "both";
        const rawMaxViews = cfg?.maxViews ?? 1;
        const maxViews = isViewsMode ? rawMaxViews : 0;
        const noViewCap = !maxViews || maxViews <= 0;

        if (!noViewCap) {
          updates[`perUserViews.${currentUserId}`] = increment(1);
        }

        if (Object.keys(updates).length > 0) {
          await updateDoc(msgRef, updates);
        }
      } catch (err: any) {
        console.warn("group view-once consume error", err?.message);
      }
    },
    [groupId, currentUserId],
  );

  // stickers
  const handleStickerSelected = useCallback((downloadUrl: string) => {
    setStickerPreviewUrl(downloadUrl);
    setStickerPreviewViewOnce(false);
  }, []);

  const headerTitle = groupName || groupMeta?.name || "Group chat";

  function handleMenuPress(item: string) {
    setMenuOpen(false);
    if (item === "exit") {
      onBack();
    }
    if (item === "clear") {
      // purely client-side clear for now; you can later add a server-side archive
      setMessages([]);
    }
    if (item === "info") {
      setInfoOpen(true);
    }
  }

  function openInfoPanel() {
    setInfoOpen(true);
  }

  const renderItem = ({ item }: { item: Message }) => {
    return (
      <MessageRow
        item={item}
        currentUserId={currentUserId}
        nowTick={nowTick}
        onOpenViewOnce={setOpenViewOnceMsg}
      />
    );
  };

  return (
    <SafeAreaView style={styles.safeRoot} edges={["bottom", "left", "right"]}>
      <KeyboardAvoidingView
        style={styles.root}
        behavior={Platform.select({ ios: "padding", android: "height" })}
        keyboardVerticalOffset={Platform.select({ ios: 80, android: 0 })}
      >
        {/* header */}
        <View style={styles.headerShell}>
          <View style={styles.headerCapsule}>
            <View style={styles.headerLeft}>
              <TouchableOpacity
                onPress={onBack}
                activeOpacity={0.8}
                style={styles.backPill}
              >
                <Text style={styles.backPillText}>Back</Text>
              </TouchableOpacity>

              <TouchableOpacity
                activeOpacity={0.8}
                onPress={openInfoPanel}
                style={styles.headerAvatarWrap}
              >
                <Text style={styles.headerAvatar}>
                  {headerTitle.charAt(0).toUpperCase()}
                </Text>
                <View style={styles.headerAvatarGlow} />
              </TouchableOpacity>

              <View style={styles.headerTextBlock}>
                <Text style={styles.headerTitle}>{headerTitle}</Text>
                <View style={styles.headerStatusRow}>
                  <View style={styles.headerStatusDot} />
                  <Text style={styles.headerSubtitle}>Neural channel live</Text>
                </View>
              </View>
            </View>

            <View style={styles.headerRight}>
              <TouchableOpacity
                activeOpacity={0.7}
                onPress={() => setMenuOpen((v) => !v)}
              >
                <Text style={styles.headerMenuText}>⋮</Text>
              </TouchableOpacity>

              {menuOpen && (
                <View style={styles.menu}>
                  {[
                    { key: "info", label: "Group info" },
                    { key: "disappearing", label: "Msg disappearing" },
                    { key: "theme", label: "Chat theme" },
                    { key: "search", label: "Search" },
                    { key: "clear", label: "Clear chat" },
                    { key: "report", label: "Report" },
                    { key: "exit", label: "Exit group" },
                  ].map((item) => (
                    <TouchableOpacity
                      key={item.key}
                      activeOpacity={0.8}
                      style={styles.menuItem}
                      onPress={() => handleMenuPress(item.key)}
                    >
                      <Text
                        style={[
                          styles.menuText,
                          item.key === "exit" && styles.menuExitText,
                          item.key === "report" && styles.menuReportText,
                        ]}
                      >
                        {item.label}
                      </Text>
                    </TouchableOpacity>
                  ))}
                </View>
              )}
            </View>
          </View>
        </View>

        {/* messages */}
        <FlatList
          data={messages}
          keyExtractor={(item) => item.id}
          renderItem={renderItem}
          style={styles.list}
          contentContainerStyle={styles.listContent}
          inverted
        />

        {/* input */}
        <ChatInputBar
          value={input}
          onChangeText={setInput}
          onSend={handleSend}
          disabled={sending}
          placeholder="Transmit to the group..."
          onOpenTools={() => setToolsOpen(true)}
        />

        {/* tools panel */}
        <ChatToolsPanel
          visible={toolsOpen}
          onClose={() => setToolsOpen(false)}
          onToolAction={handleToolAction}
        />

        {/* media preview */}
        {previewUri && (
          <MediaPreviewSheet
            uri={previewUri}
            viewOnce={previewViewOnce}
            onToggleViewOnce={() => {
              setPreviewViewOnce(true);
              setViewOnceConfigOpen(true);
            }}
            onSend={handlePreviewSend}
            onCancel={handlePreviewCancel}
          />
        )}

        {/* view-once config */}
        {viewOnceConfigOpen && (
          <ViewOnceConfigSheet
            visible={viewOnceConfigOpen}
            value={viewOnceConfig}
            onChange={setViewOnceConfig}
            onClose={() => setViewOnceConfigOpen(false)}
            onCancelViewOnce={() => {
              setPreviewViewOnce(false);
              setStickerPreviewViewOnce(false);
              setViewOnceConfig({
                mode: "views",
                maxViews: 1,
                timeLimitSeconds: null,
              });
            }}
          />
        )}

        {/* view-once viewer */}
        {openViewOnceMsg &&
          (openViewOnceMsg.imageUri ||
            openViewOnceMsg.videoUri ||
            openViewOnceMsg.audioUri) && (
            <ViewOnceOverlay
              message={openViewOnceMsg}
              onClose={() => handleViewOnceClose(openViewOnceMsg)}
            />
          )}

        {/* voice recorder / preview / edit */}
        <VoiceRecorderSheet
          visible={voiceSheetOpen}
          onClose={() => setVoiceSheetOpen(false)}
          onRecorded={handleAudioRecorded}
        />

        <VoicePreviewSheet
          visible={audioPreviewOpen}
          uri={pendingAudioUri}
          editConfig={editConfig}
          viewOnce={previewViewOnce}
          onToggleViewOnce={() => {
            setPreviewViewOnce(true);
            setViewOnceConfigOpen(true);
          }}
          onClose={() => setAudioPreviewOpen(false)}
          onSend={handleAudioSend}
          onDiscard={handleAudioDiscard}
          onEdit={handleAudioEdit}
        />

        <VoiceEditSheet
          visible={editSheetOpen}
          durationMs={editDurationMs}
          onClose={() => setEditSheetOpen(false)}
          onApply={handleApplyEdit}
        />

        {/* stickers */}
        <StickerPickerSheet
          visible={stickerSheetOpen}
          onClose={() => setStickerSheetOpen(false)}
          onSelectSticker={(url) => {
            setStickerSheetOpen(false);
            setStickerPreviewUrl(url);
            setStickerPreviewViewOnce(false);
          }}
          onOpenChatMedia={() => {}}
        />

        {stickerPreviewUrl && (
          <StickerPreviewSheet
            url={stickerPreviewUrl}
            viewOnce={stickerPreviewViewOnce}
            onToggleViewOnce={() => {
              setStickerPreviewViewOnce(true);
              setViewOnceConfigOpen(true);
            }}
            onCancel={() => {
              setStickerPreviewUrl(null);
              setStickerPreviewViewOnce(false);
            }}
            onSend={async () => {
              if (!groupId || !currentUser) return;

              try {
                const groupRef = doc(db, "groups", groupId);
                const msgsRef = collection(groupRef, "messages");

                await addDoc(msgsRef, {
                  type: "sticker",
                  text: "",
                  stickerUri: stickerPreviewUrl,
                  imageUri: null,
                  videoUri: null,
                  audioUri: null,
                  userId: currentUserId,
                  userName: currentUserName,
                  createdAt: serverTimestamp(),
                  viewOnce: stickerPreviewViewOnce,
                  viewConfig: stickerPreviewViewOnce
                    ? viewOnceConfig
                    : null,
                });

                setStickerPreviewUrl(null);
                setStickerPreviewViewOnce(false);
              } catch (e: any) {
                console.warn("group sticker send error", e?.message);
              }
            }}
          />
        )}

        {/* group info overlay */}
        <GroupProfileOverlay
          open={infoOpen}
          group={
            groupMeta || {
              name: groupName ?? headerTitle,
            }
          }
          onClose={() => setInfoOpen(false)}
        />
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeRoot: { flex: 1, backgroundColor: "#020617" },
  root: { flex: 1, backgroundColor: "#020617" },

  headerShell: {
    paddingHorizontal: 12,
    paddingTop: 10,
    paddingBottom: 6,
    backgroundColor: "#020617",
  },
  headerCapsule: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(30,64,175,0.8)",
    backgroundColor: "rgba(15,23,42,0.98)",
  },
  headerLeft: {
    flexDirection: "row",
    alignItems: "center",
    flex: 1,
    gap: 10,
  },
  backPill: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.5)",
    backgroundColor: "rgba(15,23,42,1)",
  },
  backPillText: {
    fontSize: 11,
    letterSpacing: 1,
    textTransform: "uppercase",
    color: "#e5e7eb",
  },
  headerAvatarWrap: {
    width: 36,
    height: 36,
    borderRadius: 18,
    alignItems: "center",
    justifyContent: "center",
    marginLeft: 4,
  },
  headerAvatarGlow: {
    position: "absolute",
    width: 36,
    height: 36,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.9)",
    shadowColor: "#22d3ee",
    shadowOpacity: 0.7,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 0 },
  },
  headerAvatar: {
    width: 28,
    height: 28,
    borderRadius: 14,
    textAlign: "center",
    textAlignVertical: "center",
    color: "#e5e7eb",
    fontWeight: "700",
    fontSize: 15,
    backgroundColor: "#020617",
  },
  headerTextBlock: {
    flexShrink: 1,
  },
  headerTitle: {
    fontSize: 14,
    fontWeight: "700",
    color: "#e5e7eb",
    letterSpacing: 0.4,
  },
  headerStatusRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 2,
  },
  headerStatusDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: "#22c55e",
    marginRight: 4,
  },
  headerSubtitle: {
    fontSize: 11,
    color: "#38bdf8",
  },
  headerRight: {
    paddingLeft: 10,
    alignItems: "flex-end",
    position: "relative",
  },
  headerMenuText: {
    fontSize: 18,
    color: "#9ca3af",
  },

  menu: {
    position: "absolute",
    top: 28,
    right: 0,
    borderRadius: 12,
    paddingVertical: 4,
    minWidth: 170,
    backgroundColor: "rgba(15,23,42,0.98)",
    borderWidth: 1,
    borderColor: "rgba(30,64,175,0.9)",
    shadowColor: "#000",
    shadowOpacity: 0.6,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 8 },
    zIndex: 40,
    elevation: 30,
  },
  menuItem: {
    paddingVertical: 8,
    paddingHorizontal: 12,
  },
  menuText: {
    fontSize: 13,
    color: "#e5e7eb",
  },
  menuExitText: {
    color: "#f97373",
    fontWeight: "600",
  },
  menuReportText: {
    color: "#fb923c",
  },

  list: {
    flex: 1,
    backgroundColor: "#020617",
  },
  listContent: {
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 90,
  },
});
