// src/member/chat/ChatMessageListSection.tsx
import React, { forwardRef } from "react";
import { FlatList, StyleSheet } from "react-native";
import { MessageRow, Message } from "@/src/member/chat/MessageRow";

type Props = {
  messages: Message[];
  currentUserId: string;
  nowTick: number;
  isCorneal: boolean;
  onOpenViewOnce: (m: Message | null) => void;
  searchQuery?: string;
  onReply?: (m: Message) => void;
  onDeleteMessage?: (m: Message) => Promise<void> | void;
  onOpenMessageActions?: (m: Message) => void;
  onJumpToMessage?: (id: string) => void;
  onPollVote?: (m: Message, optionId: string) => void;
  highlightedMessageId?: string | null;
  onReactionPress?: (
    m: Message,
    kind: "emoji" | "sticker",
    userId: string,
  ) => void;
};

export const ChatMessageListSection = forwardRef<FlatList<Message>, Props>(
  (
    {
      messages,
      currentUserId,
      nowTick,
      isCorneal,
      onOpenViewOnce,
      searchQuery,
      onReply,
      onDeleteMessage,
      onOpenMessageActions,
      onJumpToMessage,
      highlightedMessageId,
      onPollVote,
      onReactionPress,
    },
    ref,
  ) => {
    const renderItem = ({ item }: { item: Message }) => (
      <MessageRow
        item={item}
        currentUserId={currentUserId}
        nowTick={nowTick}
        onOpenViewOnce={(m) => m && onOpenViewOnce(m)}
        searchQuery={searchQuery}
        onReply={onReply}
        onDeleteMessage={onDeleteMessage}
        onOpenMessageActions={onOpenMessageActions}
        onJumpToMessage={onJumpToMessage}
        onPollVote={onPollVote}
        highlighted={highlightedMessageId === item.id}
        onReactionPress={onReactionPress}
      />
    );

    return (
      <FlatList
        ref={ref}
        data={messages}
        keyExtractor={(item) => item.id}
        renderItem={renderItem}
        style={styles.list}
        contentContainerStyle={{ padding: 16, paddingBottom: 120 }}
        inverted={!isCorneal}
      />
    );
  },
);

const styles = StyleSheet.create({
  list: { flex: 1 },
});
