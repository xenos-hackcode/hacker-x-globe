// src/member/work/hack/HackPanel.tsx
import { db } from "@/src/api/firebase";
import { useUserProfile } from "@/src/hooks/useUserProfile";
import HackCommunity, {
  Post as HackPost,
} from "@/src/member/work/hack/HackCommunity";
import HackCreatePostScreen from "@/src/member/work/hack/HackCreatePostScreen";
import HackGlobe from "@/src/member/work/hack/HackGlobe";
import HackNetworkScreen, {
  NetworkNode,
} from "@/src/member/work/hack/HackNetworkScreen";
import HackPostScreen from "@/src/member/work/hack/HackPostScreen";
import HackTradeChatScreen from "@/src/member/work/hack/HackTradeChatScreen";
import HackTradeScreen, {
  TradeItem,
} from "@/src/member/work/hack/HackTradeScreen";
import { HackLabConfig, HackTask } from "@/src/member/work/hack/HackTypes";
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import {
  doc,
  onSnapshot,
  serverTimestamp,
  setDoc,
} from "firebase/firestore";
import React, { useEffect, useState } from "react";
import {
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

type Props = {
  onBack?: () => void;
};

type HackTabKey = "globe" | "community" | "trade" | "network";

type OpenPostState = HackPost | null;
type CreatePostState =
  | { mode: "create"; roomId: string }
  | { mode: "edit"; post: HackPost }
  | null;
type PostActionsState = HackPost | null;
type OpenTradeThreadState = TradeItem | null;

type HackLogEntry = {
  id: string;
  at: string;
  message: string;
};

type HackTaskStatus = "pending" | "completed";

export default function HackPanel({ onBack }: Props) {
  const { colors } = useTheme();
  const { user } = useUserProfile();
  const uid = user?.uid ?? null;

  const [activeTab, setActiveTab] = useState<HackTabKey>("globe");

  const [posts, setPosts] = useState<HackPost[]>([]);
  const [trades, setTrades] = useState<TradeItem[]>([]);
  const [networkNodes, setNetworkNodes] = useState<NetworkNode[]>([]);
  const [labTitle, setLabTitle] = useState<string | null>(null);
  const [labDescription, setLabDescription] = useState<string | null>(null);
  const [labTasks, setLabTasks] = useState<HackTask[]>([]);
  const [taskStatus, setTaskStatus] = useState<Record<string, HackTaskStatus>>(
    {},
  );

  const [openPost, setOpenPost] = useState<OpenPostState>(null);
  const [createPost, setCreatePost] = useState<CreatePostState>(null);
  const [postActionsTarget, setPostActionsTarget] =
    useState<PostActionsState>(null);
  const [openTradeThread, setOpenTradeThread] =
    useState<OpenTradeThreadState>(null);

  const [hackLog, setHackLog] = useState<HackLogEntry[]>([]);
  const [showFullLog, setShowFullLog] = useState(false);

  const currentUserHandle = "@you"; // TODO: wire with real handle

  const pushLog = (message: string) => {
    setHackLog((prev) => [
      {
        id: `log-${Date.now()}-${Math.random().toString(16).slice(2, 6)}`,
        at: new Date().toISOString(),
        message,
      },
      ...prev,
    ]);
  };

  const markTaskCompleted = (taskId: string) => {
    setTaskStatus((prev) => {
      if (prev[taskId] === "completed") return prev;
      return { ...prev, [taskId]: "completed" };
    });
  };

  // Load per-user hack session
  useEffect(() => {
    if (!uid) return;
    const ref = doc(db, "users", uid, "hackSessions", "current");

    const unsub = onSnapshot(ref, (snap) => {
      if (!snap.exists()) return;
      const data = snap.data() as any;

      setLabTitle(data.labTitle ?? null);
      setLabDescription(data.labDescription ?? null);
      setLabTasks(data.labTasks ?? []);
      setTaskStatus(data.taskStatus ?? {});
      setNetworkNodes(data.networkNodes ?? []);
      setHackLog(data.hackLog ?? []);
    });

    return () => unsub();
  }, [uid]);

  const saveHackSession = async () => {
    if (!uid) return;
    const ref = doc(db, "users", uid, "hackSessions", "current");
    try {
      await setDoc(
        ref,
        {
          labTitle,
          labDescription,
          labTasks,
          taskStatus,
          networkNodes,
          hackLog,
          updatedAt: serverTimestamp(),
        },
        { merge: true },
      );
    } catch {
      // optional: log error
    }
  };

  const handleToggleLike = (postId: string) => {
    setPosts((prev) =>
      prev.map((p) => {
        if (p.id !== postId) return p;
        const likedByMe = !(p as any).likedByMe;
        const dislikedByMe = false;
        let likeCount = p.likeCount;
        let dislikeCount = (p as any).dislikeCount ?? 0;

        if (likedByMe) {
          likeCount += 1;
          if ((p as any).dislikedByMe) {
            dislikeCount = Math.max(0, dislikeCount - 1);
          }
        } else {
          likeCount = Math.max(0, likeCount - 1);
        }

        return {
          ...p,
          likeCount,
          dislikeCount,
          likedByMe,
          dislikedByMe,
        } as any;
      }),
    );
  };

  const handleToggleDislike = (postId: string) => {
    setPosts((prev) =>
      prev.map((p) => {
        if (p.id !== postId) return p;
        const dislikedByMe = !(p as any).dislikedByMe;
        const likedByMe = false;
        let dislikeCount = (p as any).dislikeCount ?? 0;
        let likeCount = p.likeCount;

        if (dislikedByMe) {
          dislikeCount += 1;
          if ((p as any).likedByMe) {
            likeCount = Math.max(0, likeCount - 1);
          }
        } else {
          dislikeCount = Math.max(0, dislikeCount - 1);
        }

        return {
          ...p,
          likeCount,
          dislikeCount,
          likedByMe,
          dislikedByMe,
        } as any;
      }),
    );
  };

  // 1) Viewing a community post (comments)
  if (openPost) {
    return (
      <View style={[styles.root, { backgroundColor: colors.background }]}>
        <HackPostScreen
          authorHandle={openPost.authorHandle}
          createdAt={openPost.createdAt}
          body={openPost.body}
          likeCount={openPost.likeCount}
          commentCount={openPost.commentCount}
          onBack={() => setOpenPost(null)}
        />
      </View>
    );
  }

  // 2) Viewing a trade thread chat
  if (openTradeThread) {
    return (
      <View style={[styles.root, { backgroundColor: colors.background }]}>
        <HackTradeChatScreen
          thread={openTradeThread}
          onBack={() => setOpenTradeThread(null)}
        />
      </View>
    );
  }

  // 3) Full-screen hack log
  if (showFullLog) {
    return (
      <View style={[styles.root, { backgroundColor: colors.background }]}>
        <View style={styles.header}>
          <TouchableOpacity
            onPress={() => setShowFullLog(false)}
            style={styles.backButton}
            activeOpacity={0.7}
          >
            <Ionicons
              name="chevron-back"
              size={20}
              color={colors.textPrimary}
            />
            <Text style={[styles.backText, { color: colors.textPrimary }]}>
              Back
            </Text>
          </TouchableOpacity>

          <View>
            <Text style={[styles.title, { color: colors.textPrimary }]}>
              Hack log
            </Text>
            <Text
              style={[
                styles.subTitle,
                { color: colors.textSecondary },
              ]}
            >
              Timeline of everything you tried in this lab.
            </Text>
          </View>
        </View>

        <View style={[styles.content, { paddingHorizontal: 16 }]}>
          {hackLog.length === 0 && (
            <View style={styles.centered}>
              <Text style={{ color: colors.textSecondary, fontSize: 12 }}>
                No actions recorded yet. Use the Network tab to start attacking.
              </Text>
            </View>
          )}

          {hackLog.map((entry) => (
            <View key={entry.id} style={{ marginBottom: 6 }}>
              <Text
                style={{
                  fontSize: 11,
                  color: "#6b7280",
                  marginBottom: 1,
                }}
              >
                {entry.at}
              </Text>
              <Text
                style={{
                  fontSize: 12,
                  color: colors.textPrimary,
                }}
              >
                • {entry.message}
              </Text>
            </View>
          ))}
        </View>
      </View>
    );
  }

  // 4) Creating or editing a post
  if (createPost) {
    const roomId =
      createPost.mode === "create"
        ? createPost.roomId
        : createPost.post.roomId;

    const roomName =
      roomId === "help"
        ? "Help & Debug"
        : roomId === "show"
        ? "Show & Tell"
        : roomId === "ideas"
        ? "Ideas"
        : "Collabs";

    return (
      <View style={[styles.root, { backgroundColor: colors.background }]}>
        <HackCreatePostScreen
          roomName={roomName}
          mode={createPost.mode}
          initialBody={createPost.mode === "edit" ? createPost.post.body : ""}
          onBack={() => setCreatePost(null)}
          onSubmit={({ body }) => {
            if (createPost.mode === "create") {
              const newPost: HackPost = {
                id: `local-${Date.now()}`,
                roomId,
                authorHandle: currentUserHandle,
                createdAt: "now",
                body,
                likeCount: 0,
                commentCount: 0,
              };
              setPosts((prev) => [newPost, ...prev]);
            } else {
              setPosts((prev) =>
                prev.map((p) =>
                  p.id === createPost.post.id ? { ...p, body } : p,
                ),
              );
            }
            setCreatePost(null);
          }}
        />
      </View>
    );
  }

  // 5) Main Hack session with tabs
  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity
          onPress={onBack}
          style={styles.backButton}
          activeOpacity={0.7}
        >
          <Ionicons
            name="chevron-back"
            size={20}
            color={colors.textPrimary}
          />
          <Text style={[styles.backText, { color: colors.textPrimary }]}>
            Back
          </Text>
        </TouchableOpacity>

        <View>
          <Text style={[styles.title, { color: colors.textPrimary }]}>
            Hack Session
          </Text>
          <Text
            style={[
              styles.subTitle,
              { color: colors.textSecondary },
            ]}
          >
            Short, aggressive sprints for experiments and wild ideas.
          </Text>
        </View>
      </View>

      {/* Main content */}
      <View style={styles.content}>
        {activeTab === "globe" && (
          <HackGlobe
            uid={uid}
            onLabConfig={async (config: HackLabConfig) => {
              setNetworkNodes(config.nodes);
              setLabTitle(config.title);
              setLabDescription(config.description);
              setLabTasks(config.tasks);
              setTaskStatus(
                Object.fromEntries(
                  config.tasks.map((t) => [t.id, "pending" as HackTaskStatus]),
                ),
              );
              pushLog(`New lab loaded: ${config.title}`);
              setActiveTab("network");
              await saveHackSession();
            }}
          />
        )}

        {activeTab === "community" && (
          <HackCommunity
            posts={posts}
            currentUserHandle={currentUserHandle}
            onOpenPost={(post) => setOpenPost(post)}
            onStartPost={(roomId) =>
              setCreatePost({ mode: "create", roomId })
            }
            onPostLongPress={(post) => setPostActionsTarget(post)}
            onToggleLike={handleToggleLike}
            onToggleDislike={handleToggleDislike}
          />
        )}

        {activeTab === "trade" && (
          <HackTradeScreen
            trades={trades}
            currentUserHandle={currentUserHandle}
            onOpenThread={(trade) => setOpenTradeThread(trade)}
            onCreateTrade={({ type, title, description }) => {
              const id = `trade-${Date.now()}`;
              const newTrade: TradeItem = {
                id,
                threadId: id,
                type,
                title,
                description,
                authorHandle: currentUserHandle,
                createdAt: "now",
                status: "open",
              };
              setTrades((prev) => [newTrade, ...prev]);
            }}
            onUpdateTrade={(updated) => {
              setTrades((prev) =>
                prev.map((t) => (t.id === updated.id ? updated : t)),
              );
            }}
            onDeleteTrade={(id) => {
              setTrades((prev) => prev.filter((t) => t.id !== id));
            }}
          />
        )}

        {activeTab === "network" && (
          <View style={{ flex: 1 }}>
            {(labTitle || labTasks.length > 0) && (
              <View style={{ paddingHorizontal: 4, marginBottom: 4 }}>
                {labTitle && (
                  <Text
                    style={{
                      fontSize: 13,
                      color: colors.textPrimary,
                      fontWeight: "600",
                    }}
                  >
                    {labTitle}
                  </Text>
                )}
                {labDescription && (
                  <Text
                    style={{
                      fontSize: 11,
                      color: colors.textSecondary,
                      marginTop: 2,
                    }}
                    numberOfLines={2}
                  >
                    {labDescription}
                  </Text>
                )}
                {labTasks.length > 0 && (
                  <View style={{ marginTop: 4 }}>
                    {labTasks.map((t) => {
                      const status = taskStatus[t.id] || "pending";
                      const done = status === "completed";
                      return (
                        <View
                          key={t.id}
                          style={{ flexDirection: "row", alignItems: "center" }}
                        >
                          <Ionicons
                            name={done ? "checkbox-outline" : "square-outline"}
                            size={12}
                            color={done ? "#22c55e" : "#6b7280"}
                            style={{ marginRight: 4 }}
                          />
                          <Text
                            style={{
                              fontSize: 11,
                              color: done
                                ? colors.textPrimary
                                : colors.textSecondary,
                            }}
                          >
                            {t.description}
                          </Text>
                        </View>
                      );
                    })}
                  </View>
                )}
              </View>
            )}

            <HackNetworkScreen
              nodes={networkNodes}
              onOpenNode={(node) => {
                pushLog(`Inspecting ${node.name} at ${node.ip}`);
              }}
              onAttackNode={async (node, attack) => {
                if (attack === "scan") {
                  pushLog(
                    `Scanning ${node.ip}... open service suggests: ${node.vulnHint}`,
                  );
                  await saveHackSession();
                  return;
                }

                if (attack === "bruteforce") {
                  if (node.difficulty <= 2 && node.status !== "compromised") {
                    setNetworkNodes((prev) =>
                      prev.map((n) =>
                        n.id === node.id
                          ? { ...n, status: "compromised", discovered: true }
                          : n,
                      ),
                    );
                    pushLog(`Bruteforce succeeded on ${node.ip}. Access gained.`);

                    labTasks
                      .filter(
                        (t) =>
                          (!t.requiredAction ||
                            t.requiredAction === "bruteforce") &&
                          t.targetIds.includes(node.id),
                      )
                      .forEach((t) => markTaskCompleted(t.id));
                  } else {
                    pushLog(
                      `Bruteforce failed on ${node.ip}. Try a different approach.`,
                    );
                  }
                  await saveHackSession();
                  return;
                }

                if (attack === "exploit") {
                  let updatedFlag: string | undefined;
                  setNetworkNodes((prev) =>
                    prev.map((n) => {
                      if (n.id !== node.id) return n;
                      updatedFlag = n.flag;
                      return {
                        ...n,
                        status: "compromised",
                        discovered: true,
                      };
                    }),
                  );
                  const flagMsg = updatedFlag
                    ? ` Flag captured: ${updatedFlag}`
                    : "";
                  pushLog(`Exploit fired against ${node.ip}.${flagMsg}`);

                  labTasks
                    .filter(
                      (t) =>
                        (!t.requiredAction ||
                          t.requiredAction === "exploit") &&
                        t.targetIds.includes(node.id),
                    )
                    .forEach((t) => markTaskCompleted(t.id));

                  await saveHackSession();
                }
              }}
            />

            <TouchableOpacity
              activeOpacity={0.8}
              onPress={() => setShowFullLog(true)}
              style={{
                maxHeight: 110,
                borderTopWidth: StyleSheet.hairlineWidth,
                borderTopColor: "rgba(148,163,184,0.4)",
                paddingBottom: 6,
              }}
            >
              <View
                style={{
                  flexDirection: "row",
                  alignItems: "center",
                  paddingHorizontal: 16,
                  paddingTop: 6,
                  marginBottom: 2,
                }}
              >
                <Text
                  style={{
                    fontSize: 11,
                    color: "#9ca3af",
                    marginRight: 6,
                  }}
                >
                  Hack log
                </Text>
                <Ionicons name="open-outline" size={12} color="#9ca3af" />
              </View>

              {hackLog.slice(0, 3).map((entry) => (
                <Text
                  key={entry.id}
                  style={{
                    fontSize: 11,
                    color: "#e5e7eb",
                    paddingHorizontal: 16,
                  }}
                  numberOfLines={1}
                >
                  • {entry.message}
                </Text>
              ))}
              {hackLog.length === 0 && (
                <Text
                  style={{
                    fontSize: 11,
                    color: "#6b7280",
                    paddingHorizontal: 16,
                  }}
                >
                  • Actions you take in the lab will show up here.
                </Text>
              )}
            </TouchableOpacity>
          </View>
        )}
      </View>

      <View style={[styles.bottomBar, { borderTopColor: colors.border }]}>
        <HackTab
          label="Globe"
          icon="globe-outline"
          active={activeTab === "globe"}
          onPress={() => setActiveTab("globe")}
        />
        <HackTab
          label="Community"
          icon="people-outline"
          active={activeTab === "community"}
          onPress={() => setActiveTab("community")}
        />
        <HackTab
          label="Trade"
          icon="swap-horizontal-outline"
          active={activeTab === "trade"}
          onPress={() => setActiveTab("trade")}
        />
        <HackTab
          label="Network"
          icon="radio-outline"
          active={activeTab === "network"}
          onPress={() => setActiveTab("network")}
        />
      </View>

      {postActionsTarget && (
        <View style={styles.sheetBackdrop}>
          <TouchableOpacity
            style={styles.sheetBackdropTouch}
            activeOpacity={1}
            onPress={() => setPostActionsTarget(null)}
          />
          <View style={styles.sheet}>
            <Text style={styles.sheetTitle}>Post actions</Text>

            <TouchableOpacity
              style={styles.sheetButton}
              onPress={() => {
                setCreatePost({ mode: "edit", post: postActionsTarget });
                setPostActionsTarget(null);
              }}
            >
              <Ionicons
                name="pencil-outline"
                size={16}
                color="#e5e7eb"
                style={{ marginRight: 8 }}
              />
              <Text style={styles.sheetButtonText}>Edit post</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[
                styles.sheetButton,
                { backgroundColor: "rgba(248,113,113,0.15)" },
              ]}
              onPress={() => {
                setPosts((prev) =>
                  prev.filter((p) => p.id !== postActionsTarget.id),
                );
                setPostActionsTarget(null);
              }}
            >
              <Ionicons
                name="trash-outline"
                size={16}
                color="#f87171"
                style={{ marginRight: 8 }}
              />
              <Text
                style={[styles.sheetButtonText, { color: "#fca5a5" }]}
              >
                Delete post
              </Text>
            </TouchableOpacity>
          </View>
        </View>
      )}
    </View>
  );
}

type HackTabProps = {
  label: string;
  icon: keyof typeof Ionicons.glyphMap;
  active?: boolean;
  onPress?: () => void;
};

function HackTab({ label, icon, active, onPress }: HackTabProps) {
  return (
    <TouchableOpacity
      style={styles.tabItem}
      activeOpacity={0.7}
      onPress={onPress}
    >
      <Ionicons
        name={icon}
        size={18}
        color={active ? "#22c55e" : "#6b7280"}
      />
      <Text style={[styles.tabLabel, active && styles.tabLabelActive]}>
        {label}
      </Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
  },
  header: {
    paddingTop: 40,
    paddingHorizontal: 16,
    paddingBottom: 12,
    flexDirection: "row",
    alignItems: "center",
    columnGap: 12,
  },
  backButton: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 6,
    paddingRight: 8,
    paddingLeft: 0,
  },
  backText: {
    fontSize: 13,
    marginLeft: 2,
  },
  title: {
    fontSize: 18,
    fontWeight: "600",
  },
  subTitle: {
    fontSize: 12,
    marginTop: 2,
  },
  content: {
    flex: 1,
    paddingHorizontal: 16,
    paddingTop: 8,
  },
  centered: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
  },
  bottomBar: {
    height: 60,
    borderTopWidth: StyleSheet.hairlineWidth,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-around",
    backgroundColor: "#020617",
  },
  tabItem: {
    alignItems: "center",
    justifyContent: "center",
  },
  tabLabel: {
    marginTop: 3,
    fontSize: 11,
    color: "#6b7280",
  },
  tabLabelActive: {
    color: "#e5e7eb",
    fontWeight: "600",
  },
  sheetBackdrop: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: "flex-end",
  },
  sheetBackdropTouch: {
    flex: 1,
  },
  sheet: {
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: 16,
    backgroundColor: "rgba(15,23,42,0.98)",
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: "rgba(148,163,184,0.4)",
  },
  sheetTitle: {
    fontSize: 13,
    color: "#9ca3af",
    marginBottom: 8,
  },
  sheetButton: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 8,
    paddingHorizontal: 8,
    borderRadius: 8,
    marginTop: 4,
  },
  sheetButtonText: {
    fontSize: 14,
    color: "#e5e7eb",
  },
});
