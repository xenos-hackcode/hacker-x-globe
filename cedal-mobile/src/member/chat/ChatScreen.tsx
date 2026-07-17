// src/member/chat/ChatScreen.tsx
import React, { useRef, useState } from "react";
import {
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  View,
  ImageBackground,
  TextInput,
  TouchableOpacity,
  Text,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { router } from "expo-router";
import { ReportSubmittedSheet } from "./partials/ReportSubmittedSheet";
import ChatInputBar from "@/src/member/chat/ChatInputBar";
import { ChatScreenHeaderSection } from "./ChatScreenHeaderSection";
import { ChatMessageListSection } from "./ChatMessageListSection";
import ChatToolsPanel from "@/src/member/chat/ChatToolsPanel";
import { useChatScreenState } from "@/src/hooks/useChatScreenState";
import { ChatSheets } from "./partials/ChatSheets";
import { ClearChatSheet } from "./partials/ClearChatSheet";
import { ChatThemeSheet } from "./partials/ChatThemeSheet";
import { DisappearingSheet } from "./partials/DisappearingSheet";
import { ReportSheet } from "./partials/ReportSheet";
import { MessageActionsSheet } from "./partials/MessageActionsSheet";
import type { Message } from "@/src/member/chat/MessageRow";
import { FlatList } from "react-native-gesture-handler";
import { EmojiPanel } from "./EmojiPanel";
import { ContactPickerSheet } from "./ContactPickerSheet";
import { PinMessageSheet } from "./partials/PinMessageSheet";
import { SavedMessagesScreen } from "@/src/member/profile/SavedMessagesScreen";

export type ChatScreenProps = {
  chatId: string;
  onBack: () => void;
  chatName?: string;
  otherUserId?: string;
  otherUserEmail?: string | null;
};

const PRESET_BACKGROUND_MAP: Record<string, any> = {
  "1": require("@/src/themes/1.png"),
  "2": require("@/src/themes/2.png"),
  "3": require("@/src/themes/3.png"),
  "4": require("@/src/themes/4.png"),
  "5": require("@/src/themes/5.png"),
  "6": require("@/src/themes/6.png"),
  "7": require("@/src/themes/7.png"),
  "8": require("@/src/themes/8.png"),
  "9": require("@/src/themes/9.png"),
  "10": require("@/src/themes/10.png"),
  "11": require("@/src/themes/11.png"),
  "12": require("@/src/themes/12.png"),
  "13": require("@/src/themes/13.png"),
  "14": require("@/src/themes/14.png"),
  "15": require("@/src/themes/15.png"),
  "16": require("@/src/themes/16.png"),
  "17": require("@/src/themes/17.png"),
};

export default function ChatScreen(props: ChatScreenProps) {
  const state = useChatScreenState(props);

  const {
    headerTitle,
    headerSubtitle,
    presence,
    onBack,
    otherAvatarUrl,
    otherUserId,
    emojiSheetOpen,
    setEmojiSheetOpen,
    messages,
    headerPinnedMessages,
    currentUserId,
    nowTick,
    isCorneal,
    input,
    setInput,
    sending,
    isGeneral,
    toolsOpen,
    setToolsOpen,
    handleSend,
    handleToolAction,
    setClearSheetOpen,
    setThemeSheetOpen,
    chatTheme,
    searchOpen,
    setSearchOpen,
    searchQuery,
    setSearchQuery,
    setDisappearingSheetOpen,
    setReportSheetOpen,
    selectedReplyMessage,
    setSelectedReplyMessage,
    pinnedMessages,
    unpinMessage,
    otherDisplayName,
    savedMessagesForMe,
  } = state as any;

  const listRef = useRef<FlatList<Message>>(null);
  const [highlightedMessageId, setHighlightedMessageId] =
    useState<string | null>(null);

  const [editingMessage, setEditingMessage] = useState<Message | null>(null);
  const [showSaved, setShowSaved] = useState(false);
  const handleOpenSaved = () => {
  setShowSaved(true);
};


  const [reactionToRemove, setReactionToRemove] = useState<{
    msg: Message;
    kind: "emoji" | "sticker";
    userId: string;
  } | null>(null);

  const handleJumpToMessage = (msgId: string) => {
    if (!messages || !messages.length) return;
    const index = messages.findIndex((m: Message) => m.id === msgId);
    if (index === -1) return;

    try {
      listRef.current?.scrollToIndex({ index, animated: true });
    } catch (e) {
      console.warn("scrollToIndex error", (e as any)?.message);
    }

    setHighlightedMessageId(msgId);
    setTimeout(() => setHighlightedMessageId(null), 1500);
  };

  const handleOpenChatInfo = () => {
    if (!otherUserId) return;
    router.push({
      pathname: "/(auth)/(member)/user/[userId]",
      params: { userId: otherUserId },
    } as any);
  };

  const handleSendWrapper = async () => {
  if (!input.trim()) return;

  if (editingMessage) {
    try {
      await state.updateMessage?.(editingMessage, input.trim());
    } catch (e: any) {
      console.warn("updateMessage error", e?.message);
    } finally {
      setEditingMessage(null);
      setInput("");
    }
    return;
  }

  // log for debugging
  console.log("handleSendWrapper text:", input.trim());
  console.log("reply at send:", selectedReplyMessage);

  try {
    await handleSend();
  } finally {
    // clear input + reply UI after send
    setInput("");
    setSelectedReplyMessage(null);
  }
};

  const content = (
    <>
      {selectedReplyMessage && (
        <View style={styles.replyPreview}>
          <View style={styles.replyBar} />
          <View style={styles.replyTextBlock}>
            <Text style={styles.replyLabel}>
              Replying to {selectedReplyMessage.userName}
            </Text>
            <Text numberOfLines={1} style={styles.replySnippet}>
              {selectedReplyMessage.text}
            </Text>
          </View>
          <TouchableOpacity
            onPress={() => setSelectedReplyMessage(null)}
            style={styles.replyClose}
          >
            <Text style={styles.replyCloseText}>✕</Text>
          </TouchableOpacity>
        </View>
      )}

{headerPinnedMessages && headerPinnedMessages.length > 0 && (
  <View style={styles.pinnedContainer}>
    {headerPinnedMessages.map((m: Message) => (
      <TouchableOpacity
        key={m.id}
        style={styles.pinnedBanner}
        onPress={() => handleJumpToMessage(m.id)}
      >
        <Text style={styles.pinnedLabel}>Pinned</Text>
        <Text style={styles.pinnedText} numberOfLines={1}>
          {m.text || "[media]"}
        </Text>
        <TouchableOpacity
          style={styles.pinnedClose}
          onPress={() => unpinMessage(m)}
        >
          <Text style={styles.pinnedCloseText}>✕</Text>
        </TouchableOpacity>
      </TouchableOpacity>
    ))}
  </View>
)}

      <ChatMessageListSection
        ref={listRef}
        messages={messages}
        currentUserId={currentUserId}
        nowTick={nowTick}
        isCorneal={isCorneal}
        searchQuery={searchQuery}
        onReply={(m) => {
          setSelectedReplyMessage(m);
        }}
        onDeleteMessage={(m) => state.handleDeleteMessage?.(m)}
        onOpenMessageActions={(m) => {
          state.setSelectedMessage(m);
          state.setMessageActionsOpen(true);
        }}
        onOpenViewOnce={state.setOpenViewOnceMsg}
        onJumpToMessage={handleJumpToMessage}
        onPollVote={state.handlePollVote}
        highlightedMessageId={highlightedMessageId}
        onReactionPress={(m, kind, userId) => {
          if (userId !== currentUserId) return;
          setReactionToRemove({ msg: m, kind, userId });
        }}
      />

      <ChatInputBar
        value={input}
        onChangeText={setInput}
        onSend={handleSendWrapper}
        disabled={sending || state.isBlocked || state.iBlockedThem}
        placeholder={
          state.isBlocked || state.iBlockedThem
            ? "You can’t send messages to this user"
            : editingMessage
            ? "Edit message"
            : "Message"
        }
        onOpenTools={() => setToolsOpen(true)}
      />

      <ChatToolsPanel
        visible={toolsOpen}
        onClose={() => setToolsOpen(false)}
        onToolAction={handleToolAction}
      />

      <ChatSheets state={state} />
      <ClearChatSheet state={state} />
      <ChatThemeSheet state={state} />
      <DisappearingSheet state={state} />
      <ReportSheet state={state} />
      <ReportSubmittedSheet state={state} />

      <MessageActionsSheet
        visible={state.messageActionsOpen}
        message={state.selectedMessage}
        currentUserId={currentUserId}
        onClose={() => state.setMessageActionsOpen(false)}
        onDeleteForEveryone={(m) => state.handleDeleteMessage?.(m)}
        onDeleteForMe={(m) => state.handleDeleteForMe?.(m)}
        onEditMessage={(m) => {
          setEditingMessage(m);
          setInput(m.text ?? "");
        }}
        onForwardMessage={(m) => {
          state.openForwardSheet?.(m);
        }}
        onPinMessage={(m) => {
          state.setPinTargetMessage?.(m);
          state.setPinSheetOpen?.(true);
        }}
        onSaveMessage={(m) => {
          state.saveMessageMedia?.(m);
        }}
        onCopyText={(m) => {
          state.copyMessageText?.(m);
        }}
        onOpenStickerPicker={(m) => {
          // sticker react mode
          state.setStickerReactionTarget?.(m);
          state.setStickerSheetOpen?.(true);
        }}
        onOpenEmojiPicker={(m) => {
          state.openEmojiPickerForMessage?.(m);
        }}
        onReportMessage={(m) => {
          state.setSelectedMessageForReport?.(m);
          state.setReportSheetOpen(true);
        }}
      />
    </>
  );

  const rootStyle = [
    styles.root,
    chatTheme?.backgroundColor && { backgroundColor: chatTheme.backgroundColor },
  ];

  const presetImageSource =
    chatTheme?.kind === "preset" && chatTheme.presetId
      ? PRESET_BACKGROUND_MAP[chatTheme.presetId]
      : undefined;

  const userImageSource =
    chatTheme?.kind === "userPhoto" && chatTheme.backgroundImageUrl
      ? { uri: chatTheme.backgroundImageUrl }
      : undefined;

  const bgSource = /* userImageSource || */ presetImageSource;

  return (
    <>
      <SafeAreaView
        style={styles.safeRoot}
        edges={["bottom", "left", "right"]}
      >
        <KeyboardAvoidingView
          style={styles.root}
          behavior={Platform.select({ ios: "padding", android: "height" })}
          keyboardVerticalOffset={Platform.select({ ios: 80, android: 0 })}
        >
          <ChatScreenHeaderSection
            onBack={onBack}
            headerTitle={headerTitle}
            headerSubtitle={headerSubtitle}
            isGeneral={isGeneral}
            isCorneal={isCorneal}
            otherAvatarUrl={otherAvatarUrl}
            otherUserId={otherUserId}
            presence={presence}
            onClearChat={() => setClearSheetOpen(true)}
            onOpenTheme={() => setThemeSheetOpen(true)}
            onOpenSearch={() => setSearchOpen(true)}
            onOpenDisappearing={() => setDisappearingSheetOpen(true)}
            onOpenChatInfo={handleOpenChatInfo}
            onOpenReport={() => setReportSheetOpen(true)}
            onOpenSaved={handleOpenSaved}  
          />

          {searchOpen && (
            <View style={styles.searchBar}>
              <TextInput
                value={searchQuery}
                onChangeText={setSearchQuery}
                placeholder="Search in chat"
                placeholderTextColor="#6b7280"
                style={styles.searchInput}
                autoFocus
              />
              <TouchableOpacity
                onPress={() => {
                  setSearchQuery("");
                  setSearchOpen(false);
                }}
              >
                <Text style={styles.searchClose}>Close</Text>
              </TouchableOpacity>
            </View>
          )}

          {bgSource ? (
            <ImageBackground
              source={bgSource}
              style={rootStyle}
              imageStyle={{ opacity: 0.7 }}
            >
              {content}
            </ImageBackground>
          ) : (
            <View style={rootStyle}>{content}</View>
          )}

          <EmojiPanel
            visible={emojiSheetOpen}
            onClose={() => {
              state.setEmojiPickerTarget?.(null);
              setEmojiSheetOpen(false);
            }}
            onSelectEmoji={async (emoji) => {
              if (state.emojiPickerTarget) {
                await state.sendEmojiReaction?.(emoji);
                return;
              }
              setInput(input + emoji);
            }}
          />

          <PinMessageSheet
            visible={state.pinSheetOpen}
            message={state.pinTargetMessage}
            onClose={() => {
              state.setPinSheetOpen?.(false);
              state.setPinTargetMessage?.(null);
            }}
            onConfirm={(durationMs) => {
              state.confirmPinWithDuration?.(durationMs);
            }}
          />
        </KeyboardAvoidingView>
      </SafeAreaView>

      {reactionToRemove && (
        <View style={styles.removeReactionSheet}>
          <View style={styles.removeReactionBox}>
            <Text style={styles.removeReactionTitle}>Remove reaction?</Text>
            <View style={styles.removeReactionRow}>
              <TouchableOpacity
                style={styles.removeReactionCancel}
                onPress={() => setReactionToRemove(null)}
              >
                <Text style={styles.removeReactionCancelText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.removeReactionConfirm}
                onPress={async () => {
                  await state.removeReaction?.(
                    reactionToRemove.msg,
                    reactionToRemove.kind,
                    reactionToRemove.userId,
                  );
                  setReactionToRemove(null);
                }}
              >
                <Text style={styles.removeReactionConfirmText}>Remove</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      )}
      {showSaved && (
        <SavedMessagesScreen
          title="Saved"
          otherDisplayName={otherDisplayName}
          currentUserId={currentUserId}
          savedMessages={savedMessagesForMe}
          onBack={() => setShowSaved(false)}
        />
      )}
    </>
  );
}

const styles = StyleSheet.create({
  safeRoot: { flex: 1, backgroundColor: "#020617" },
  root: { flex: 1, backgroundColor: "#020617" },

  searchBar: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 12,
    paddingVertical: 6,
    backgroundColor: "#020617",
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(148,163,184,0.4)",
  },
  searchInput: {
    flex: 1,
    color: "#e5e7eb",
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 8,
    backgroundColor: "#020617",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    fontSize: 13,
  },
  searchClose: {
    marginLeft: 10,
    color: "#9ca3af",
    fontSize: 13,
  },

  replyPreview: {
    flexDirection: "row",
    alignItems: "center",
    marginHorizontal: 12,
    marginBottom: 4,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 12,
    backgroundColor: "rgba(15,23,42,0.95)",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
  },
  replyBar: {
    width: 3,
    alignSelf: "stretch",
    borderRadius: 999,
    backgroundColor: "#22c55e",
    marginRight: 8,
  },
  replyTextBlock: {
    flex: 1,
  },
  replyLabel: {
    fontSize: 11,
    color: "#9ca3af",
    marginBottom: 2,
  },
  replySnippet: {
    fontSize: 13,
    color: "#e5e7eb",
  },
  replyClose: {
    paddingLeft: 8,
    paddingVertical: 4,
  },
  replyCloseText: {
    fontSize: 14,
    color: "#9ca3af",
  },

  pinnedContainer: {
    marginHorizontal: 12,
    marginBottom: 4,
    rowGap: 4,
  },
  pinnedBanner: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 12,
    backgroundColor: "rgba(15,23,42,0.95)",
    borderWidth: 1,
    borderColor: "rgba(248,250,252,0.16)",
  },
  pinnedLabel: {
    fontSize: 11,
    color: "#22c55e",
    fontWeight: "600",
    marginRight: 8,
  },
  pinnedText: {
    flex: 1,
    fontSize: 13,
    color: "#e5e7eb",
  },
  pinnedClose: {
    marginLeft: 8,
    paddingHorizontal: 4,
    paddingVertical: 2,
  },
  pinnedCloseText: {
    fontSize: 13,
    color: "#9ca3af",
  },

  removeReactionSheet: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: "center",
    alignItems: "center",
    backgroundColor: "rgba(0,0,0,0.5)",
  },
  removeReactionBox: {
    width: "80%",
    padding: 16,
    borderRadius: 12,
    backgroundColor: "#020617",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
  },
  removeReactionTitle: {
    fontSize: 14,
    color: "#e5e7eb",
    marginBottom: 12,
  },
  removeReactionRow: {
    flexDirection: "row",
    justifyContent: "flex-end",
    columnGap: 8,
  },
  removeReactionCancel: {
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  removeReactionCancelText: {
    fontSize: 13,
    color: "#9ca3af",
  },
  removeReactionConfirm: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "#ef4444",
  },
  removeReactionConfirmText: {
    fontSize: 13,
    color: "#f9fafb",
    fontWeight: "600",
  },
});
