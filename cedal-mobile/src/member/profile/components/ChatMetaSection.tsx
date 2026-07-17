// src/member/profile/components/ChatMetaSection.tsx
import React from "react";
import { View, Text, StyleSheet, TouchableOpacity } from "react-native";

type Props = {
  onChatPassword?: () => void;
  onDisappearingChat?: () => void;
  onSavedMessages?: () => void;
  onMedia?: () => void;
  onLinks?: () => void;
  onDocs?: () => void;
  onPollsEvents?: () => void; // NEW
  onFavorite?: () => void;
  onBlock?: () => void;
  onClearChat?: () => void;
};

export function ChatMetaSection({
  onChatPassword,
  onSavedMessages,
  onMedia,
  onLinks,
  onDocs,
  onPollsEvents,   // NEW
  onFavorite,
  onBlock,
  onClearChat,
}: Props) {
  return (
    <View style={styles.section}>
      <Text style={styles.sectionTitle}>Chat controls</Text>

      <View style={styles.grid}>
        <MetaCard
          label="Chat password"
          value="Lock this chat"
          onPress={onChatPassword}
        />
        <MetaCard
          label="Saved messages"
          value="Starred items"
          onPress={onSavedMessages}
        />
        <MetaCard
          label="Media"
          value="Photos & videos"
          onPress={onMedia}
        />
        <MetaCard
          label="Links"
          value="All URLs"
          onPress={onLinks}
        />
        <MetaCard
          label="Docs"
          value="Shared files"
          onPress={onDocs}
        />
        <MetaCard
          label="Polls & events"
          value="Votes & schedules"
          onPress={onPollsEvents}      // NEW
        />
        <MetaCard
          label="Favorite"
          value="Pin this chat"
          onPress={onFavorite}
        />
        <MetaCard
          label="Block"
          value="Stop messages"
          danger
          onPress={onBlock}
        />
        
      </View>
    </View>
  );
}

type CardProps = {
  label: string;
  value: string;
  danger?: boolean;
  onPress?: () => void;
};

function MetaCard({ label, value, danger, onPress }: CardProps) {
  const clickable = !!onPress;
  const Wrapper: React.ComponentType<any> = clickable ? TouchableOpacity : View;

  return (
    <Wrapper
      style={[
        styles.card,
        danger && { borderColor: "rgba(248,113,113,0.7)" },
      ]}
      {...(clickable ? { activeOpacity: 0.7, onPress } : {})}
    >
      <Text
        style={[
          styles.cardLabel,
          danger && { color: "#fca5a5" },
        ]}
      >
        {label}
      </Text>
      <Text style={styles.cardValue}>{value}</Text>
    </Wrapper>
  );
}

const styles = StyleSheet.create({
  section: { marginBottom: 16 },
  sectionTitle: {
    fontSize: 13,
    color: "#e5e7eb",
    fontWeight: "600",
    marginBottom: 8,
  },
  grid: { flexDirection: "row", flexWrap: "wrap", gap: 8 },
  card: {
    flexBasis: "48%",
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(30,64,175,0.7)",
    backgroundColor: "#020617",
    padding: 8,
  },
  cardLabel: {
    fontSize: 9,
    color: "#9ca3af",
    textTransform: "uppercase",
  },
  cardValue: {
    fontSize: 11,
    color: "#e5e7eb",
    marginTop: 2,
  },
});
