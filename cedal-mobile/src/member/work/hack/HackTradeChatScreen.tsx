// src/member/work/hack/HackTradeChatScreen.tsx
import HackToolsPanel from "@/src/member/work/hack/HackToolsPanel";
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import React, { useState } from "react";
import {
    FlatList,
    KeyboardAvoidingView,
    Platform,
    StyleSheet,
    Text,
    TextInput,
    TouchableOpacity,
    View,
} from "react-native";
import { TradeItem } from "./HackTradeScreen";

type Props = {
  thread: TradeItem;
  onBack?: () => void;
};

type TradeChatMessage = {
  id: string;
  from: "me" | "them";
  text: string;
  createdAt: number;
};

export default function HackTradeChatScreen({ thread, onBack }: Props) {
  const { colors } = useTheme();

  const [messages, setMessages] = useState<TradeChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [toolsVisible, setToolsVisible] = useState(false);

  const handleSend = () => {
    const trimmed = input.trim();
    if (!trimmed) return;

    const msg: TradeChatMessage = {
      id: `${Date.now()}`,
      from: "me",
      text: trimmed,
      createdAt: Date.now(),
    };

    setMessages((prev) => [msg, ...prev]);
    setInput("");
  };

  const renderMessage = ({ item }: { item: TradeChatMessage }) => {
    const isMe = item.from === "me";
    const align = isMe ? "flex-end" : "flex-start";
    const bubbleBg = isMe
      ? "rgba(34,197,94,0.12)"
      : "rgba(56,189,248,0.15)";

    const words = item.text.split(/\s+/).filter(Boolean);

    return (
      <View
        style={{
          marginBottom: 6,
          alignSelf: align,
          maxWidth: "92%",
        }}
      >
        <View
          style={{
            flexDirection: "row",
            flexWrap: "wrap",
            backgroundColor: bubbleBg,
            borderRadius: 16,
            paddingHorizontal: 6,
            paddingVertical: 4,
          }}
        >
          {words.map((w, idx) => (
            <View
              key={`${item.id}-${idx}`}
              style={{
                flexDirection: "row",
                alignItems: "center",
                backgroundColor: "rgba(15,23,42,0.96)",
                borderRadius: 999,
                paddingHorizontal: 6,
                paddingVertical: 3,
                margin: 2,
              }}
            >
              {/* build logo becomes tool trigger */}
              <TouchableOpacity
                onPress={() => setToolsVisible(true)}
                activeOpacity={0.8}
                style={{
                  width: 10,
                  height: 10,
                  borderRadius: 3,
                  backgroundColor: isMe ? "#22c55e" : "#38bdf8",
                  marginRight: 4,
                }}
              />
              <Text
                style={{
                  color: "#e5e7eb",
                  fontSize: 12,
                }}
              >
                {w}
              </Text>
            </View>
          ))}
        </View>
      </View>
    );
  };

  return (
    <KeyboardAvoidingView
      style={[styles.root, { backgroundColor: colors.background }]}
      behavior={Platform.OS === "ios" ? "padding" : undefined}
    >
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
            {thread.title}
          </Text>
          <Text
            style={[styles.subTitle, { color: colors.textSecondary }]}
            numberOfLines={1}
          >
            {thread.type === "offer" ? "Offer" : "Request"} ·{" "}
            {thread.authorHandle}
          </Text>
        </View>
      </View>

      {/* Body */}
      <View style={styles.body}>
        <Text style={[styles.desc, { color: colors.textSecondary }]}>
          {thread.description}
        </Text>

        <FlatList
          data={messages}
          keyExtractor={(item) => item.id}
          inverted
          style={styles.list}
          contentContainerStyle={{ paddingBottom: 8 }}
          renderItem={renderMessage}
        />
      </View>

      {/* Input */}
      <View style={styles.inputBar}>
        <TextInput
          style={styles.input}
          placeholder="Type a reply..."
          placeholderTextColor="#6b7280"
          value={input}
          onChangeText={setInput}
          multiline
        />
        <TouchableOpacity
          style={[
            styles.sendButton,
            !input.trim() && { opacity: 0.4 },
          ]}
          onPress={handleSend}
          disabled={!input.trim()}
        >
          <Ionicons name="arrow-up" size={16} color="#020617" />
        </TouchableOpacity>
      </View>

      {/* Tools panel triggered by build logo */}
      <HackToolsPanel
        visible={toolsVisible}
        onClose={() => setToolsVisible(false)}
        onToolAction={(action) => {
          // hook for future behavior, e.g. apply reaction or open sketch
          // console.log("Tool picked in trade chat", action);
        }}
      />
    </KeyboardAvoidingView>
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
    fontSize: 16,
    fontWeight: "600",
  },
  subTitle: {
    fontSize: 12,
    marginTop: 2,
  },
  body: {
    flex: 1,
    paddingHorizontal: 16,
    paddingTop: 8,
  },
  desc: {
    fontSize: 12,
    marginBottom: 10,
  },
  list: {
    flex: 1,
  },
  inputBar: {
    flexDirection: "row",
    alignItems: "flex-end",
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: "rgba(148,163,184,0.4)",
    backgroundColor: "#020617",
  },
  input: {
    flex: 1,
    minHeight: 34,
    maxHeight: 90,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "rgba(15,23,42,1)",
    color: "#e5e7eb",
    fontSize: 13,
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
