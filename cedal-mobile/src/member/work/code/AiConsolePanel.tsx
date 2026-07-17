// src/member/work/code/AiConsolePanel.tsx
import React from "react";
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TextInput,
  TouchableOpacity,
} from "react-native";

type Props = {
  lines: string[];
  input: string;
  onChangeInput: (value: string) => void;
  onSend: () => void;
  borderColor: string;
  onRestructure: () => void;
  onDebug: () => void;
  onErrorFinder: () => void;
};

export default function AiConsolePanel({
  lines,
  input,
  onChangeInput,
  onSend,
  borderColor,
  onRestructure,
  onDebug,
  onErrorFinder,
}: Props) {
  return (
    <>
      {/* top tools bar */}
      <View
        style={[
          styles.toolsRow,
          { borderBottomColor: borderColor },
        ]}
      >
        <Text style={styles.toolsTitle}>Coder tools</Text>
        <View style={styles.toolsButtons}>
          <ToolButton label="Restructure" onPress={onRestructure} />
          <ToolButton label="Debug" onPress={onDebug} />
          <ToolButton label="Error finder" onPress={onErrorFinder} />
        </View>
      </View>

      {/* log */}
      <ScrollView
        style={styles.aiScroll}
        contentContainerStyle={{ padding: 10 }}
      >
        <Text style={styles.aiLine}>
          &gt; boot: attached to{" "}
          <Text style={styles.aiAccent}>cedal://coder</Text>
        </Text>
        <Text style={styles.aiLine}>
          &gt; ai: Coder is online.
        </Text>
        {lines.map((line, idx) => (
          <Text key={idx} style={styles.aiLine}>
            {line}
          </Text>
        ))}
      </ScrollView>

      {/* input */}
      <View
        style={[
          styles.aiInputRow,
          { borderTopColor: borderColor },
        ]}
      >
        <TextInput
          value={input}
          onChangeText={onChangeInput}
          placeholder="ask Coder about this file, error, or refactor…"
          placeholderTextColor="#6b7280"
          style={styles.aiInput}
          onSubmitEditing={onSend}
        />
        <TouchableOpacity
          onPress={onSend}
          style={styles.aiSendBtn}
          activeOpacity={0.8}
        >
          <Text style={styles.aiSendText}>▶</Text>
        </TouchableOpacity>
      </View>
    </>
  );
}

function ToolButton({
  label,
  onPress,
}: {
  label: string;
  onPress: () => void;
}) {
  return (
    <TouchableOpacity
      style={styles.toolBtn}
      onPress={onPress}
      activeOpacity={0.8}
    >
      <Text style={styles.toolBtnText}>{label}</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  toolsRow: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  toolsTitle: {
    fontSize: 11,
    color: "#9ca3af",
    marginBottom: 4,
    letterSpacing: 1.2,
    textTransform: "uppercase",
  },
  toolsButtons: {
    flexDirection: "row",
    gap: 6,
  },
  toolBtn: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(59,130,246,0.8)",
    backgroundColor: "rgba(15,23,42,0.9)",
  },
  toolBtnText: {
    fontSize: 11,
    color: "#dbeafe",
  },

  aiScroll: { flex: 1 },
  aiLine: {
    fontSize: 11,
    fontFamily: "monospace",
    color: "#cbd5f5",
    marginBottom: 4,
  },
  aiAccent: { color: "#22c55e" },

  aiInputRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 8,
    paddingVertical: 6,
    borderTopWidth: StyleSheet.hairlineWidth,
  },
  aiInput: {
    flex: 1,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,1)",
    paddingHorizontal: 10,
    paddingVertical: 4,
    fontSize: 11,
    color: "#e5e7eb",
  },
  aiSendBtn: {
    width: 30,
    height: 30,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#38bdf8",
    alignItems: "center",
    justifyContent: "center",
    marginLeft: 6,
  },
  aiSendText: { color: "#38bdf8", fontSize: 14 },
});
