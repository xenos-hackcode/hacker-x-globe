// src/member/chat/MessageRow.tsx
import React, { memo, useState, useCallback } from "react";
import {
  View,
  Text,
  Image,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Linking,
} from "react-native";
import { Swipeable } from "react-native-gesture-handler";
import { Audio } from "expo-av";
import type { ViewOnceConfig } from "@/src/member/utils/ViewOnceConfigSheet";
import { LoopingStickerVideo } from "@/src/member/chat/LoopingStickerVideo";
import { CedalMenu } from "@/src/member/ai/CedalMenu";

type PollOption = {
  id: string;
  label: string;
};

export type Message = {
  id: string;
  text: string;
  createdAt: Date | null;
  userId: string;
  userName: string;
  savedBy?: string[];

  imageUri?: string | null;
  videoUri?: string | null;
  audioUri?: string | null;

  fileUri?: string | null;
  fileName?: string | null;
  fileSize?: number | null;
  fileMime?: string | null;

  type?:
    | "text"
    | "sticker"
    | "document"
    | "menu"
    | "contact"
    | "poll"
    | "event"
    | "emoji";
  question?: string | null;
  options?: PollOption[];
  allowMulti?: boolean;
  votesByUser?: Record<string, string[]>;

  stickerUri?: string | null;
  stickerKind?: "image" | "video";
  stickerReactions?: { [userId: string]: string };

  viewOnce?: boolean;
  viewConfig?: ViewOnceConfig | null;
  perUserViews?: Record<string, number>;
  firstViewAt?: any;

  contactUserId?: string | null;
  contactUserName?: string | null;

  eventTitle?: string | null;
  eventStartsAt?: any;
  eventEndsAt?: any;
  eventLocation?: string | null;
  eventNotes?: string | null;

  replyToId?: string | null;
  replyToText?: string | null;
  replyToUserName?: string | null;
  replyToType?: string | null;
  replyToStickerUri?: string | null;
  replyToViewOnce?: boolean | null;

  reactions?: Record<string, string>; // userId -> emoji

  deleted?: boolean; // tombstone
};

type Props = {
  item: Message;
  currentUserId: string;
  nowTick: number;
  onOpenViewOnce: (msg: Message) => void;
  searchQuery?: string;
  onReply?: (m: Message) => void;
  onDeleteMessage?: (m: Message) => Promise<void> | void;
  onOpenMessageActions?: (m: Message) => void;
  onJumpToMessage?: (id: string) => void;
  highlighted?: boolean;
  onPollVote?: (m: Message, optionId: string) => void;
  onReactionPress?: (
    m: Message,
    kind: "emoji" | "sticker",
    userId: string,
  ) => void;
};

// highlight @corneal in green
function renderWithCornealHighlight(text: string) {
  const parts = text.split(/(@corneal)/gi);

  return (
    <>
      {parts.map((part, idx) => {
        if (/^@corneal$/i.test(part)) {
          return (
            <Text
              key={idx}
              style={[
                styles.bubbleText,
                { color: "#22c55e", fontWeight: "600" },
              ]}
            >
              {part}
            </Text>
          );
        }
        return (
          <Text key={idx} style={styles.bubbleText}>
            {part}
          </Text>
        );
      })}
    </>
  );
}

// wrap text and also highlight search query in red
function renderHighlightedText(text: string, searchQuery?: string) {
  if (!searchQuery || !searchQuery.trim()) {
    return renderWithCornealHighlight(text);
  }

  const lowerText = text.toLowerCase();
  const lowerQuery = searchQuery.toLowerCase();
  const parts: React.ReactNode[] = [];
  let index = 0;

  while (true) {
    const matchIndex = lowerText.indexOf(lowerQuery, index);
    if (matchIndex === -1) {
      if (index < text.length) {
        const remaining = text.slice(index);
        parts.push(
          <Text key={index} style={styles.bubbleText}>
            {remaining}
          </Text>,
        );
      }
      break;
    }

    if (matchIndex > index) {
      const before = text.slice(index, matchIndex);
      parts.push(
        <Text key={index} style={styles.bubbleText}>
          {before}
        </Text>,
      );
    }

    const matchText = text.slice(matchIndex, matchIndex + searchQuery.length);
    parts.push(
      <Text
        key={`${matchIndex}-${matchText}`}
        style={[styles.bubbleText, { color: "red" }]}
      >
        {matchText}
      </Text>,
    );

    index = matchIndex + searchQuery.length;
  }

  return <>{parts}</>;
}

function MessageRowImpl({
  item,
  currentUserId,
  nowTick,
  onOpenViewOnce,
  searchQuery,
  onReply,
  onDeleteMessage,
  onOpenMessageActions,
  onJumpToMessage,
  onPollVote,
  highlighted,
  onReactionPress,
}: Props) {
  const mine = item.userId === currentUserId;
  const isCornealMsg = item.userId === "corneal";
  const iSaved = item.savedBy?.includes(currentUserId) ?? false;

  // 5‑minute edit window
  const canEditWindow =
    !!item.createdAt &&
    Date.now() - item.createdAt.getTime() < 5 * 60 * 1000;

  const isViewOnce = !!item.viewOnce;
  const cfg = item.viewConfig;

  const isViewsMode = cfg?.mode === "views" || cfg?.mode === "both";
  const isTimeMode = cfg?.mode === "time" || cfg?.mode === "both";

  const rawMaxViews = cfg?.maxViews ?? 1;
  const maxViews = isViewsMode ? rawMaxViews : 0;
  const noViewCap = !maxViews || maxViews <= 0;
  const myViews = item.perUserViews?.[currentUserId] ?? 0;

  let expiredByTime = false;
  let minutesLeft: number | null = null;

  if (
    isTimeMode &&
    cfg?.timeLimitSeconds != null &&
    item.firstViewAt?.toMillis
  ) {
    const firstMs = item.firstViewAt.toMillis();
    const endMs = firstMs + cfg.timeLimitSeconds * 1000;
    const now = nowTick;
    if (now >= endMs) {
      expiredByTime = true;
      minutesLeft = 0;
    } else {
      const diffSec = Math.max(0, Math.floor((endMs - now) / 1000));
      minutesLeft = Math.ceil(diffSec / 60);
    }
  }

  const viewsExhausted =
    isViewOnce && isViewsMode && !noViewCap && myViews >= maxViews;
  const timeExhausted = isViewOnce && isTimeMode && expiredByTime;

  const hasMedia = !!item.imageUri || !!item.videoUri || !!item.audioUri;
  const canView = isViewOnce && !viewsExhausted && !timeExhausted && hasMedia;

  let chipNumber = "1";
  if (cfg?.mode === "time" && cfg.timeLimitSeconds != null) {
    chipNumber = String(Math.round(cfg.timeLimitSeconds / 60));
  } else {
    chipNumber = maxViews && maxViews > 0 ? String(maxViews) : "∞";
  }

  let statusText = "Tap to open · disappears after viewing";

  if (viewsExhausted) {
    statusText = "Views used";
  } else if (timeExhausted) {
    statusText = "Time run out";
  } else if (isViewsMode) {
    const remaining = noViewCap ? "∞" : maxViews - myViews;
    statusText = `Views left: ${remaining}`;
  }

  if (isTimeMode && !timeExhausted && minutesLeft != null) {
    statusText = `Time left: ${minutesLeft} min`;
  }

  const showHiddenViewOnceTile =
    isViewOnce && canView && hasMedia && !item.deleted;
  const showUsedOrExpiredLabel =
    isViewOnce &&
    (!canView || viewsExhausted || timeExhausted) &&
    !item.deleted;

  // audio playback state
  const [audioLoading, setAudioLoading] = useState(false);
  const [audioPlaying, setAudioPlaying] = useState(false);
  const [sound, setSound] = useState<Audio.Sound | null>(null);

  const handleAudioPress = useCallback(async () => {
    if (!item.audioUri) return;

    try {
      if (audioPlaying && sound) {
        await sound.stopAsync();
        await sound.unloadAsync();
        setAudioPlaying(false);
        setSound(null);
        return;
      }

      setAudioLoading(true);

      if (!sound) {
        const newSound = new Audio.Sound();
        await newSound.loadAsync({ uri: item.audioUri });
        setSound(newSound);
        await newSound.playAsync();
        setAudioPlaying(true);
        setAudioLoading(false);
      } else {
        await sound.playAsync();
        setAudioPlaying(true);
        setAudioLoading(false);
      }
    } catch (e: any) {
      console.warn("audio play error", e?.message);
      setAudioLoading(false);
      setAudioPlaying(false);
    }
  }, [item.audioUri, sound, audioPlaying]);

  const viewOnceTitle =
    item.videoUri && !item.imageUri && !item.audioUri
      ? "View once video"
      : item.imageUri && !item.videoUri && !item.audioUri
      ? "View once photo"
      : "View once voice";

  // left swipe → reply
  const renderLeftActions = () => (
    <View style={styles.actionsContainer}>
      <TouchableOpacity
        style={[styles.actionButton, { backgroundColor: "#22c55e" }]}
        onPress={() => onReply?.(item)}
      >
        <Text style={styles.actionText}>Reply</Text>
      </TouchableOpacity>
    </View>
  );

  // right swipe → open actions sheet
  const renderRightActions = () => (
    <View style={styles.actionsContainer}>
      <TouchableOpacity
        style={[styles.actionButton, { backgroundColor: "#0ea5e9" }]}
        onPress={() => onOpenMessageActions?.(item)}
      >
        <Text style={styles.actionText}>More</Text>
      </TouchableOpacity>
    </View>
  );

  return (
    <Swipeable
      renderLeftActions={renderLeftActions}
      renderRightActions={renderRightActions}
      overshootFriction={8}
      onSwipeableOpen={(direction) => {
        if (direction === "left") {
          onReply?.(item);
        } else if (direction === "right") {
          onOpenMessageActions?.(item);
        }
      }}
    >
      <TouchableOpacity activeOpacity={0.9}>
        <View
          style={[
            styles.messageRow,
            mine ? styles.messageRowMe : styles.messageRowOther,
          ]}
        >
          {!mine && !isCornealMsg && (
            <Text style={styles.senderName}>{item.userName}</Text>
          )}

          <View
            style={[
              styles.bubble,
              mine
                ? styles.bubbleMe
                : isCornealMsg
                ? styles.bubbleCorneal
                : styles.bubbleOther,
              highlighted && styles.bubbleHighlighted,
            ]}
          >
            <View style={styles.bubbleContent}>
              {item.replyToId && (
                <TouchableOpacity
                  style={styles.replyHeader}
                  activeOpacity={0.7}
                  onPress={() => {
                    if (item.replyToId && onJumpToMessage) {
                      onJumpToMessage(item.replyToId);
                    }
                  }}
                >
                  <View style={styles.replyHeaderBar} />
                  <View style={styles.replyHeaderTextBlock}>
                    <Text style={styles.replyHeaderLabel}>
                      {item.replyToUserName || "Reply"}
                    </Text>

                    {item.replyToType === "sticker" &&
                    !item.replyToViewOnce &&
                    item.replyToStickerUri ? (
                      <Image
                        source={{ uri: item.replyToStickerUri }}
                        style={styles.replyStickerThumb}
                        resizeMode="cover"
                      />
                    ) : (
                      item.replyToText && (
                        <Text style={styles.replyHeaderSnippet}>
                          {item.replyToText}
                        </Text>
                      )
                    )}
                  </View>
                </TouchableOpacity>
              )}

              {showHiddenViewOnceTile ? (
                <TouchableOpacity
                  style={styles.viewOnceTileTouchable}
                  activeOpacity={0.7}
                  onPress={() => {
                    if (!canView) return;
                    onOpenViewOnce(item);
                  }}
                >
                  <View style={styles.viewOnceIconCircle}>
                    <Text style={styles.viewOnceIconText}>{chipNumber}</Text>
                  </View>
                  <View style={styles.viewOnceTextBlock}>
                    <Text style={styles.viewOnceTitle}>{viewOnceTitle}</Text>
                    <Text style={styles.viewOnceSubtitle}>{statusText}</Text>
                  </View>
                </TouchableOpacity>
              ) : showUsedOrExpiredLabel ? (
                <View style={styles.viewOnceUsedLabel}>
                  <Text style={styles.viewOnceTitle}>{statusText}</Text>
                </View>
              ) : !item.deleted &&
                item.type === "sticker" &&
                item.stickerUri ? (
                item.stickerKind === "video" ? (
                  <LoopingStickerVideo uri={item.stickerUri} />
                ) : (
                  <Image
                    source={{ uri: item.stickerUri }}
                    style={styles.stickerImage}
                    resizeMode="cover"
                  />
                )
              ) : !item.deleted && item.type === "emoji" && item.text ? (
                <Text style={[styles.bubbleText, { fontSize: 28 }]}>
                  {item.text}
                </Text>
              ) : !item.deleted &&
                item.type === "contact" &&
                item.contactUserId ? (
                <View style={styles.contactCard}>
                  <Text style={styles.contactTitle}>Contact</Text>
                  <Text style={styles.contactName}>
                    {item.contactUserName ?? "User"}
                  </Text>
                  <View style={styles.contactActionsRow}>
                    <TouchableOpacity
                      style={styles.contactAddBtn}
                      onPress={() => {}}
                    >
                      <Text style={styles.contactAddText}>Add</Text>
                    </TouchableOpacity>
                    <TouchableOpacity
                      style={styles.contactBlockBtn}
                      onPress={() => {}}
                    >
                      <Text style={styles.contactBlockText}>Block</Text>
                    </TouchableOpacity>
                  </View>
                </View>
              ) : !item.deleted && item.type === "menu" && isCornealMsg ? (
                <CedalMenu senderName={item.userName} />
              ) : !item.deleted &&
                item.type === "poll" &&
                item.question &&
                item.options?.length ? (
                <View style={styles.pollWrap}>
                  <Text style={styles.pollQuestion}>{item.question}</Text>
                  {item.options.map((opt) => {
                    const votedIds =
                      item.votesByUser?.[currentUserId] ?? [];
                    const isSelected = votedIds.includes(opt.id);
                    return (
                      <TouchableOpacity
                        key={opt.id}
                        style={[
                          styles.pollOptionRow,
                          isSelected && {
                            backgroundColor: "rgba(34,197,94,0.12)",
                          },
                        ]}
                        activeOpacity={0.8}
                        onPress={() => {
                          onPollVote?.(item, opt.id);
                        }}
                      >
                        <View
                          style={[
                            styles.pollOptionBullet,
                            isSelected && {
                              backgroundColor: "#22c55e",
                              borderColor: "#22c55e",
                            },
                          ]}
                        />
                        <Text
                          style={[
                            styles.pollOptionText,
                            isSelected && {
                              color: "#bbf7d0",
                              fontWeight: "600",
                            },
                          ]}
                        >
                          {opt.label}
                        </Text>
                      </TouchableOpacity>
                    );
                  })}
                </View>
              ) : !item.deleted && item.type === "event" && item.eventTitle ? (
                <View style={styles.eventWrap}>
                  <Text style={styles.eventTitle}>{item.eventTitle}</Text>
                  {item.eventStartsAt && (
                    <Text style={styles.eventLine}>
                      🕒 {String(item.eventStartsAt)}
                    </Text>
                  )}
                  {item.eventLocation && (
                    <Text style={styles.eventLine}>
                      📍 {item.eventLocation}
                    </Text>
                  )}
                  {item.eventNotes && (
                    <Text style={styles.eventNotes}>{item.eventNotes}</Text>
                  )}
                </View>
              ) : !item.deleted &&
  item.type === "document" &&
  item.fileUri ? (
  <TouchableOpacity
    style={styles.docBubble}
    activeOpacity={0.8}
    onPress={async () => {
      try {
        const url = item.fileUri!;
        const supported = await Linking.canOpenURL(url);
        if (supported) {
          await Linking.openURL(url);
        } else {
          console.warn("Cannot open URL", url);
        }
      } catch (e) {
        console.warn("open document error", (e as any)?.message);
      }
    }}
  >
    <View style={styles.docIconCircle}>
      <Text style={styles.docIconText}>📄</Text>
    </View>
    <View style={styles.docTextBlock}>
      <Text style={styles.docTitle} numberOfLines={1}>
        {item.fileName ?? "Document"}
      </Text>
      {item.fileMime ? (
        <Text style={styles.docMeta}>{item.fileMime}</Text>
      ) : null}
      {item.fileSize != null && (
        <Text style={styles.docMeta}>
          {Math.round(item.fileSize / 1024)} KB
        </Text>
      )}
    </View>
  </TouchableOpacity>
) : !item.deleted && item.imageUri && !isViewOnce ? (
                <Image
                  source={{ uri: item.imageUri }}
                  style={styles.bubbleImage}
                  resizeMode="cover"
                />
              ) : !item.deleted && item.videoUri && !isViewOnce ? (
                <View style={styles.viewOnceTileTouchable}>
                  <View style={styles.viewOnceIconCircle}>
                    <Text style={styles.viewOnceIconText}>▶</Text>
                  </View>
                  <View style={styles.viewOnceTextBlock}>
                    <Text style={styles.viewOnceTitle}>Video</Text>
                    <Text style={styles.viewOnceSubtitle}>Tap to play</Text>
                  </View>
                </View>
              ) : !item.deleted && item.audioUri ? (
                <TouchableOpacity
                  style={styles.audioBubble}
                  activeOpacity={0.8}
                  onPress={handleAudioPress}
                >
                  <View style={styles.audioLeft}>
                    <View style={styles.audioIconCircle}>
                      {audioLoading ? (
                        <ActivityIndicator size="small" color="#bbf7d0" />
                      ) : (
                        <Text style={styles.audioIconText}>
                          {audioPlaying ? "⏸" : "▶"}
                        </Text>
                      )}
                    </View>
                    <View style={styles.viewOnceTextBlock}>
                      <Text style={styles.viewOnceTitle}>Voice message</Text>
                      <Text style={styles.viewOnceSubtitle}>
                        {audioPlaying ? "Playing..." : "Tap to play"}
                      </Text>
                    </View>
                  </View>
                </TouchableOpacity>
              ) : null}

              {item.deleted ? (
                <Text
                  style={[
                    styles.bubbleText,
                    { fontStyle: "italic", color: "#9ca3af" },
                  ]}
                >
                  [deleted]
                </Text>
              ) : item.text ? (
                <Text
                  style={[
                    styles.bubbleText,
                    isCornealMsg && { color: "#e5e7ff" },
                  ]}
                >
                  {renderHighlightedText(item.text, searchQuery)}
                </Text>
              ) : null}
            </View>
          </View>

          {/* Emoji reactions row under the bubble */}
          {item.reactions && Object.keys(item.reactions).length > 0 && (
            <View
              style={[
                styles.reactionsRow,
                mine
                  ? { alignSelf: "flex-end" }
                  : { alignSelf: "flex-start" },
              ]}
            >
              {Object.entries(item.reactions).map(([userId, emoji]) => (
                <TouchableOpacity
                  key={userId}
                  style={styles.reactionChip}
                  activeOpacity={0.7}
                  onPress={() => onReactionPress?.(item, "emoji", userId)}
                >
                  <Text style={styles.reactionEmoji}>{emoji}</Text>
                </TouchableOpacity>
              ))}
            </View>
          )}

          {item.stickerReactions &&
            Object.keys(item.stickerReactions).length > 0 && (
              <View
                style={[
                  styles.reactionsRow,
                  mine
                    ? { alignSelf: "flex-end" }
                    : { alignSelf: "flex-start" },
                ]}
              >
                {Object.entries(item.stickerReactions).map(
                  ([userId, uri]) => (
                    <TouchableOpacity
                      key={userId}
                      activeOpacity={0.7}
                      onPress={() =>
                        onReactionPress?.(item, "sticker", userId)
                      }
                    >
                      <Image
                        source={{ uri }}
                        style={styles.stickerReactionThumb}
                        resizeMode="cover"
                      />
                    </TouchableOpacity>
                  ),
                )}
              </View>
            )}

          {item.createdAt && (
            <View style={styles.metaRow}>
              {iSaved && <Text style={styles.savedStar}>★</Text>}
              <Text style={styles.timeText}>
                {item.createdAt.toLocaleTimeString([], {
                  hour: "2-digit",
                  minute: "2-digit",
                })}
              </Text>
            </View>
          )}
        </View>
      </TouchableOpacity>
    </Swipeable>
  );
}

export const MessageRow = memo(MessageRowImpl);

const styles = StyleSheet.create({
  messageRow: { marginBottom: 10, maxWidth: "95%" },
  messageRowMe: { alignSelf: "flex-end", alignItems: "flex-end" },
  messageRowOther: { alignSelf: "flex-start", alignItems: "flex-start" },
  senderName: { fontSize: 10, color: "#93c5fd", marginBottom: 2 },
  bubble: {
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 16,
    maxWidth: "80%",
  },
  bubbleContent: {
    flexDirection: "column",
    alignItems: "flex-start",
    maxWidth: "100%",
  },
  bubbleMe: { backgroundColor: "#22d3ee", borderBottomRightRadius: 2 },
  bubbleOther: {
    backgroundColor: "rgba(15,23,42,0.95)",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
    borderBottomLeftRadius: 2,
  },
  bubbleCorneal: {
    backgroundColor: "rgba(15,23,42,0.98)",
    borderWidth: 1,
    borderColor: "#22c55e",
    borderBottomLeftRadius: 2,
  },
  bubbleHighlighted: {
    borderWidth: 2,
    borderColor: "#f87171",
  },
  bubbleText: {
    color: "#e5e7eb",
    fontSize: 14,
    flexShrink: 1,
    flexWrap: "wrap",
    maxWidth: "100%",
  },
  bubbleImage: {
    width: 180,
    height: 240,
    borderRadius: 12,
  },
  stickerImage: {
    width: 160,
    height: 160,
    borderRadius: 12,
  },

  metaRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 2,
  },
  timeText: { fontSize: 9, color: "#6b7280" },
  savedStar: {
    marginRight: 4,
    fontSize: 10,
    color: "#facc15",
  },

  viewOnceTileTouchable: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 6,
    paddingHorizontal: 8,
  },
  stickerReactionThumb: {
    width: 20,
    height: 20,
    borderRadius: 6,
    marginRight: 4,
  },
  viewOnceIconCircle: {
    width: 26,
    height: 26,
    borderRadius: 13,
    borderWidth: 1,
    borderColor: "#22c55e",
    justifyContent: "center",
    alignItems: "center",
    marginRight: 8,
    backgroundColor: "rgba(21,128,61,0.18)",
  },
  viewOnceIconText: {
    fontSize: 13,
    fontWeight: "600",
    color: "#bbf7d0",
  },
  viewOnceTextBlock: {
    flexShrink: 1,
  },
  viewOnceTitle: {
    fontSize: 13,
    color: "#e5e7eb",
  },
  viewOnceSubtitle: {
    fontSize: 11,
    color: "#9ca3af",
    marginTop: 1,
  },
  viewOnceUsedLabel: {
    paddingVertical: 6,
    paddingHorizontal: 8,
  },

  audioBubble: {
    paddingVertical: 6,
    paddingHorizontal: 8,
  },
  audioLeft: {
    flexDirection: "row",
    alignItems: "center",
  },
  audioIconCircle: {
    width: 26,
    height: 26,
    borderRadius: 13,
    borderWidth: 1,
    borderColor: "#22c55e",
    justifyContent: "center",
    alignItems: "center",
    marginRight: 8,
    backgroundColor: "rgba(21,128,61,0.18)",
  },
  audioIconText: {
    fontSize: 14,
    fontWeight: "700",
    color: "#bbf7d0",
  },

  contactCard: {
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    backgroundColor: "#020617",
  },
  contactTitle: {
    fontSize: 11,
    color: "#9ca3af",
    textTransform: "uppercase",
    letterSpacing: 0.6,
    marginBottom: 2,
  },
  contactName: {
    fontSize: 13,
    color: "#e5e7eb",
    fontWeight: "600",
    marginBottom: 6,
  },
  contactActionsRow: {
    flexDirection: "row",
    columnGap: 8,
  },
  contactAddBtn: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(34,197,94,0.8)",
    backgroundColor: "#022c22",
  },
  contactAddText: {
    fontSize: 11,
    color: "#bbf7d0",
    textTransform: "uppercase",
    letterSpacing: 0.6,
  },
  contactBlockBtn: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(248,113,113,0.8)",
    backgroundColor: "#451a1a",
  },
  contactBlockText: {
    fontSize: 11,
    color: "#fecaca",
    textTransform: "uppercase",
    letterSpacing: 0.6,
  },

  pollWrap: {
    maxWidth: 260,
  },
  pollQuestion: {
    fontSize: 14,
    color: "#22c55e",
    fontWeight: "600",
    marginBottom: 6,
  },
  pollOptionRow: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 4,
  },
  pollOptionBullet: {
    width: 10,
    height: 10,
    borderRadius: 5,
    borderWidth: 1,
    borderColor: "#38bdf8",
    marginRight: 6,
  },
  pollOptionText: {
    fontSize: 13,
    color: "#e5e7eb",
  },

  eventWrap: {
    maxWidth: 260,
  },
  eventTitle: {
    fontSize: 14,
    color: "#22c55e",
    fontWeight: "700",
    marginBottom: 4,
  },
  eventLine: {
    fontSize: 12,
    color: "#e5e7eb",
    marginBottom: 2,
  },
  eventNotes: {
    fontSize: 12,
    color: "#9ca3af",
    marginTop: 4,
  },

  docBubble: {
  flexDirection: "row",
  alignItems: "center",
  paddingVertical: 8,
  paddingHorizontal: 10,
  borderRadius: 16,
  borderWidth: 1,
  borderColor: "rgba(96,165,250,0.7)",
  backgroundColor: "#22d3ee",      // match your outgoing bubble
  maxWidth: 260,
},
docIconCircle: {
  width: 32,
  height: 32,
  borderRadius: 16,
  backgroundColor: "rgba(15,23,42,0.2)",
  alignItems: "center",
  justifyContent: "center",
  marginRight: 8,
},
docIconText: {
  fontSize: 18,
},
docTextBlock: {
  flexShrink: 1,
},
docTitle: {
  fontSize: 13,
  color: "#0f172a",
  fontWeight: "600",
},
docMeta: {
  fontSize: 11,
  color: "#0f172a",
  marginTop: 2,
},

  reactionsRow: {
    flexDirection: "row",
    marginTop: 4,
    columnGap: 4,
  },
  reactionChip: {
    paddingHorizontal: 4,
    paddingVertical: 2,
    borderRadius: 10,
    backgroundColor: "rgba(15,23,42,0.9)",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.5)",
  },
  reactionEmoji: {
    fontSize: 12,
  },

  replyHeader: {
    flexDirection: "row",
    alignItems: "flex-start",
    marginBottom: 4,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 10,
    backgroundColor: "rgba(15,23,42,0.9)",
    maxWidth: "100%",
  },
  replyHeaderBar: {
    width: 3,
    alignSelf: "stretch",
    borderRadius: 999,
    backgroundColor: "#22c55e",
    marginRight: 6,
  },
  replyHeaderTextBlock: {
    flexShrink: 1,
    maxWidth: "100%",
  },
  replyHeaderLabel: {
    fontSize: 11,
    color: "#9ca3af",
    marginBottom: 1,
  },
  replyHeaderSnippet: {
    fontSize: 12,
    color: "#e5e7eb",
    flexShrink: 1,
    flexWrap: "wrap",
  },

  actionsContainer: {
    justifyContent: "center",
    alignItems: "center",
    width: 72,
  },
  actionButton: {
    width: 56,
    height: 32,
    borderRadius: 16,
    alignItems: "center",
    justifyContent: "center",
  },
  actionText: {
    color: "#f9fafb",
    fontSize: 11,
  },

  replyStickerThumb: {
    width: 40,
    height: 40,
    borderRadius: 8,
    marginTop: 2,
  },
});
