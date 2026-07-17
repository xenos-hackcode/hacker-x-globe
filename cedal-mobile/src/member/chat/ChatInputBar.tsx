// src/member/chat/ChatInputBar.tsx
import React from "react";
import {
  View,
  TextInput,
  Text,
  TouchableOpacity,
  StyleSheet,
} from "react-native";

type Props = {
  value: string;
  onChangeText: (text: string) => void;
  onSend: () => void;
  disabled?: boolean;
  placeholder?: string;
  onOpenTools?: () => void;
};

export default function ChatInputBar({
  value,
  onChangeText,
  onSend,
  disabled = false,
  placeholder = "Type a neural transmission...",
  onOpenTools,
}: Props) {
  const hasText = value.trim().length > 0;

  function handleSendPress() {
    if (!hasText || disabled) return;
    onSend();
  }

  return (
    <View style={styles.wrapper}>
      <View style={styles.inner}>
        {/* prompt arrow as tools trigger */}
        <TouchableOpacity
          onPress={onOpenTools}
          activeOpacity={0.7}
          style={styles.promptWrapper}
        >
          <Text style={styles.prompt}>{">"}</Text>
        </TouchableOpacity>

        <TextInput
          style={styles.input}
          value={value}
          onChangeText={onChangeText}
          placeholder={placeholder}
          placeholderTextColor="#6b7280"
          editable={!disabled}
          multiline
        />

        {/* neural indicator dot */}
        <View
          style={[
            styles.dot,
            hasText ? styles.dotActive : styles.dotIdle,
          ]}
        />
      </View>

      {/* send button */}
      <TouchableOpacity
        style={[
          styles.sendButton,
          (!hasText || disabled) && styles.sendButtonDisabled,
        ]}
        activeOpacity={0.8}
        onPress={handleSendPress}
        disabled={!hasText || disabled}
      >
        <Text style={styles.sendIcon}>
          {hasText && !disabled ? "⇱" : "➤"}
        </Text>
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  wrapper: {
    paddingHorizontal: 10,
    paddingVertical: 8,
    backgroundColor: "rgba(15,23,42,0.95)",
    borderTopWidth: 1,
    borderTopColor: "rgba(56,189,248,0.3)",
    flexDirection: "row",
    alignItems: "flex-end",
    gap: 8,
  },
  inner: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    borderRadius: 999,
    paddingHorizontal: 14,
    paddingVertical: 8,
    backgroundColor: "rgba(30,41,59,0.7)",
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.4)",
  },
  promptWrapper: {
    paddingRight: 6,
    paddingVertical: 4,
  },
  prompt: {
    fontFamily: "Menlo",
    fontSize: 14,
    color: "#38bdf8",
  },
  input: {
    flex: 1,
    color: "#e5e7eb",
    fontSize: 15,
    paddingVertical: 0,
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginLeft: 8,
  },
  dotIdle: {
    backgroundColor: "rgba(156,163,175,0.4)",
  },
  dotActive: {
    backgroundColor: "#00ff41",
  },
  sendButton: {
    width: 44,
    height: 44,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: "rgba(0,255,65,0.8)",
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "rgba(0,255,65,0.9)",
  },
  sendButtonDisabled: {
    borderColor: "rgba(55,65,81,0.8)",
    backgroundColor: "rgba(55,65,81,0.6)",
  },
  sendIcon: {
    fontSize: 18,
    fontWeight: "800",
    color: "#020617",
  },
});
