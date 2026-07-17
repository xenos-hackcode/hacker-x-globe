// src/member/ai/LeoAssistantPanel.tsx
import React from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  TextInput,
  ViewStyle,
  StyleProp,
  ScrollView,
} from "react-native";
import { askBotAssistant, BotMessage } from "@/src/api/botAssistant";

type Props = {
  onClose: () => void;
  style?: StyleProp<ViewStyle>;
  userId?: string | null;
  currentBotJson: string;
  onApplyBotJson?: (json: string) => void;
};

export default function LeoAssistantPanel({
  onClose,
  style,
  userId,
  currentBotJson,
  onApplyBotJson,
}: Props) {
  const [input, setInput] = React.useState("");
  const [messages, setMessages] = React.useState<BotMessage[]>([]);
  const [loading, setLoading] = React.useState(false);
  const scrollRef = React.useRef<ScrollView | null>(null);

  function append(msg: BotMessage) {
    setMessages((prev) => [...prev, msg]);
  }

  async function handleSend() {
    const question = input.trim();
    if (!question || loading) return;

    const userMsg: BotMessage = {
      id: Date.now().toString(),
      role: "user",
      content: question,
    };
    append(userMsg);
    setInput("");
    setLoading(true);

    const payloadContent = `
You are Leo, a bot persona designer for the Cedal app.

Here is the CURRENT bot definition as JSON (name, age, gender, character, personality, bio, occupation, lifeStory, description):

\`\`\`json
${currentBotJson}
\`\`\`

The user says:
${question}

IMPORTANT:
- Keep your answers focused on improving or creating this AI persona.
- If you propose a full replacement bot definition, respond with ONLY one fenced code block containing valid JSON for the bot, tagged as \`\`\`json.
`.trim();

    const backendMsg: BotMessage = {
      id: Date.now().toString(),
      role: "user",
      content: payloadContent,
    };

    try {
      const answer = await askBotAssistant({
        userId: userId ?? null,
        messages: messages.concat(backendMsg),
      });

      const assistantMsg: BotMessage = {
        id: (Date.now() + 1).toString(),
        role: "assistant",
        content: answer,
      };
      append(assistantMsg);

      const codeMatch = answer.match(/```json([\s\S]*?)```/);
      if (codeMatch && codeMatch[1] && onApplyBotJson) {
        onApplyBotJson(codeMatch[1].trim());
      }
    } catch (e) {
      console.error("leo assistant error", e);
      append({
        id: (Date.now() + 2).toString(),
        role: "assistant",
        content: "Sorry, Leo ran into an error helping with this persona.",
      });
    } finally {
      setLoading(false);
    }
  }

  return (
    <View style={[styles.panel, style]}>
      <View className="header" style={styles.header}>
        <Text style={styles.title}>CEDAL · Leo (bot designer)</Text>
        <TouchableOpacity onPress={onClose}>
          <Text style={styles.close}>✕</Text>
        </TouchableOpacity>
      </View>

      <ScrollView
        style={styles.messages}
        contentContainerStyle={{ padding: 10 }}
        ref={scrollRef}
        onContentSizeChange={() =>
          scrollRef.current?.scrollToEnd({ animated: true })
        }
      >
        {messages.length === 0 && (
          <Text style={styles.hint}>
            Ask Leo to tweak personality, bio, or life story for this AI.
          </Text>
        )}

        {messages.map((m) => (
          <View
            key={m.id}
            style={[
              styles.messageRow,
              m.role === "user"
                ? styles.messageRowUser
                : styles.messageRowAssistant,
            ]}
          >
            <View
              style={[
                styles.messageBubble,
                m.role === "user"
                  ? styles.messageBubbleUser
                  : styles.messageBubbleAssistant,
              ]}
            >
              <Text style={styles.messageText}>{m.content}</Text>
            </View>
          </View>
        ))}

        {loading && (
          <Text style={styles.loading}>Leo is thinking...</Text>
        )}
      </ScrollView>

      <View style={styles.inputRow}>
        <TextInput
          value={input}
          onChangeText={setInput}
          placeholder="Ask Leo to sharpen this bot’s vibe…"
          placeholderTextColor="#6b7280"
          style={styles.input}
          multiline
        />
        <TouchableOpacity
          style={[
            styles.sendBtn,
            (!input.trim() || loading) && styles.sendBtnDisabled,
          ]}
          disabled={!input.trim() || loading}
          onPress={handleSend}
        >
          <Text style={styles.sendText}>{loading ? "..." : "Send"}</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  panel: {
    width: 280,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: "rgba(96,165,250,0.6)",
    backgroundColor: "rgba(15,23,42,0.98)",
    overflow: "hidden",
  },
  header: {
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(31,41,55,0.9)",
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  title: {
    color: "#e5e7eb",
    fontSize: 13,
    fontWeight: "600",
    letterSpacing: 1,
  },
  close: {
    color: "#9ca3af",
    fontSize: 14,
  },
  messages: {
    maxHeight: 220,
  },
  messageRow: {
    marginBottom: 6,
    flexDirection: "row",
  },
  messageRowUser: {
    justifyContent: "flex-end",
  },
  messageRowAssistant: {
    justifyContent: "flex-start",
  },
  messageBubble: {
    maxWidth: "80%",
    paddingHorizontal: 8,
    paddingVertical: 6,
    borderRadius: 10,
  },
  messageBubbleUser: {
    backgroundColor: "rgba(59,130,246,0.25)",
  },
  messageBubbleAssistant: {
    backgroundColor: "rgba(15,23,42,0.9)",
    borderWidth: 1,
    borderColor: "rgba(96,165,250,0.7)",
  },
  messageText: {
    color: "#e5e7eb",
    fontSize: 12,
  },
  loading: {
    color: "#9ca3af",
    fontSize: 11,
    marginTop: 4,
  },
  hint: {
    color: "#9ca3af",
    fontSize: 11,
  },
  inputRow: {
    flexDirection: "row",
    alignItems: "flex-end",
    paddingHorizontal: 10,
    paddingVertical: 8,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: "rgba(31,41,55,0.9)",
    backgroundColor: "#020617",
  },
  input: {
    flex: 1,
    color: "#e5e7eb",
    fontSize: 12,
    paddingHorizontal: 8,
    paddingVertical: 6,
    maxHeight: 80,
  },
  sendBtn: {
    marginLeft: 8,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "#60a5fa",
  },
  sendBtnDisabled: {
    backgroundColor: "#4b5563",
  },
  sendText: {
    color: "#020617",
    fontSize: 11,
    fontWeight: "600",
  },
});
