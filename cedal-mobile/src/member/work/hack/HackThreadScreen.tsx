// src/member/work/hack/HackThreadScreen.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import React, { useState } from "react";
import {
  FlatList,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";

type Message = {
  id: string;
  authorHandle: string;
  body: string;
  createdAt: string;
};

type Props = {
  title: string;
  messages: Message[];      // pass real messages from parent
  onBack?: () => void;
  onSend?: (body: string) => void; // optional send handler
};

export default function HackThreadScreen({
  title,
  messages,
  onBack,
  onSend,
}: Props) {
  const { colors } = useTheme();
  const [message, setMessage] = useState("");

  const handleSend = () => {
    const text = message.trim();
    if (!text) return;
    onSend?.(text);
    setMessage("");
  };

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

        <View style={{ flex: 1 }}>
          <Text
            style={[styles.title, { color: colors.textPrimary }]}
            numberOfLines={2}
          >
            {title}
          </Text>
          <Text
            style={[styles.subtitle, { color: colors.textSecondary }]}
          >
            Hacker thread
          </Text>
        </View>
      </View>

      {/* Messages */}
      <FlatList
        style={styles.messagesList}
        contentContainerStyle={{ paddingBottom: 12 }}
        data={messages}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <View style={styles.messageBubble}>
            <View style={styles.messageHeader}>
              <Text style={styles.messageAuthor}>
                {item.authorHandle}
              </Text>
              <Text style={styles.messageTime}>{item.createdAt}</Text>
            </View>
            <Text style={styles.messageBody}>{item.body}</Text>
          </View>
        )}
        ListEmptyComponent={
          <View style={{ padding: 16 }}>
            <Text style={{ color: "#6b7280", fontSize: 12 }}>
              No messages yet. Be the first to reply.
            </Text>
          </View>
        }
      />

      {/* Composer */}
      <View className="composer">
        <TextInput
          style={styles.composerInput}
          placeholder="Reply to this thread..."
          placeholderTextColor="#6b7280"
          value={message}
          onChangeText={setMessage}
          multiline
        />
        <TouchableOpacity
          style={[
            styles.sendButton,
            !message.trim() && { opacity: 0.4 },
          ]}
          disabled={!message.trim()}
          onPress={handleSend}
        >
          <Ionicons name="send" size={18} color="#020617" />
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
  },
  header: {
    paddingTop: 40,
    paddingHorizontal: 16,
    paddingBottom: 8,
    flexDirection: "row",
    alignItems: "center",
    columnGap: 10,
  },
  backButton: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 6,
    paddingRight: 8,
  },
  backText: {
    fontSize: 13,
    marginLeft: 2,
  },
  title: {
    fontSize: 16,
    fontWeight: "600",
  },
  subtitle: {
    fontSize: 11,
    marginTop: 2,
  },
  messagesList: {
    flex: 1,
    paddingHorizontal: 16,
  },
  messageBubble: {
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderRadius: 10,
    marginBottom: 6,
    backgroundColor: "rgba(15,23,42,0.95)",
  },
  messageHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: 2,
  },
  messageAuthor: {
    fontSize: 12,
    fontWeight: "500",
    color: "#e5e7eb",
  },
  messageTime: {
    fontSize: 10,
    color: "#6b7280",
  },
  messageBody: {
    fontSize: 13,
    color: "#e5e7eb",
  },
  composer: {
    flexDirection: "row",
    alignItems: "center",
    marginHorizontal: 16,
    marginBottom: 16,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "rgba(15,23,42,0.9)",
  },
  composerInput: {
    flex: 1,
    fontSize: 13,
    color: "#e5e7eb",
    paddingVertical: 4,
    paddingHorizontal: 6,
  },
  sendButton: {
    marginLeft: 8,
    width: 32,
    height: 32,
    borderRadius: 999,
    backgroundColor: "#22c55e",
    alignItems: "center",
    justifyContent: "center",
  },
});
