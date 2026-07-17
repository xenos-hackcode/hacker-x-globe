// src/member/chat/PollComposerSheet.tsx
import React, { useState } from "react";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import {
  Modal,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  Switch,
} from "react-native";

type Props = {
  visible: boolean;
  onClose: () => void;
  onCreate: (payload: {
    question: string;
    options: string[];
    allowMulti: boolean;
  }) => void;
};

export function PollComposerSheet({ visible, onClose, onCreate }: Props) {
  const insets = useSafeAreaInsets();
  const [question, setQuestion] = useState("");
  const [options, setOptions] = useState<string[]>(["", ""]);
  const [allowMulti, setAllowMulti] = useState(true);

  if (!visible) return null;

  const canSend =
    question.trim().length > 0 &&
    options.filter((o) => o.trim().length > 0).length >= 2;

  const updateOption = (index: number, value: string) => {
    setOptions((prev) => {
      const copy = [...prev];
      copy[index] = value;
      return copy;
    });
  };

  const addOption = () => {
    setOptions((prev) => [...prev, ""]);
  };

  return (
    <Modal
      transparent
      animationType="slide"
      visible={visible}
      onRequestClose={onClose}
    >
      <View style={styles.backdrop}>
        <View
          style={[
            styles.sheet,
            { paddingBottom: 24 + insets.bottom }, // key line
          ]}
        >
          <View style={styles.headerRow}>
            <Text style={styles.title}>Create poll</Text>
            <TouchableOpacity
              style={styles.closeBtn}
              onPress={onClose}
              activeOpacity={0.8}
            >
              <Text style={styles.closeText}>Close</Text>
            </TouchableOpacity>
          </View>

          <Text style={styles.label}>Question</Text>
          <View style={styles.inputBox}>
            <TextInput
              value={question}
              onChangeText={setQuestion}
              placeholder="What do you want to ask?"
              placeholderTextColor="#6b7280"
              style={styles.input}
            />
          </View>

          <Text style={[styles.label, { marginTop: 12 }]}>Options</Text>

          <ScrollView
            style={{ maxHeight: 260 }}
            keyboardShouldPersistTaps="handled"
          >
            {options.map((opt, idx) => (
              <View key={idx} style={styles.optionRow}>
                <Text style={styles.optionIndex}>{idx + 1}.</Text>
                <TextInput
                  value={opt}
                  onChangeText={(text) => updateOption(idx, text)}
                  placeholder="Add option"
                  placeholderTextColor="#6b7280"
                  style={styles.optionInput}
                />
              </View>
            ))}

            <TouchableOpacity
              style={styles.addOptionBtn}
              activeOpacity={0.8}
              onPress={addOption}
            >
              <Text style={styles.addOptionText}>Add option</Text>
            </TouchableOpacity>
          </ScrollView>

          <View style={styles.multiRow}>
            <Text style={styles.multiLabel}>Allow multiple answers</Text>
            <Switch
              value={allowMulti}
              onValueChange={setAllowMulti}
              thumbColor={allowMulti ? "#22c55e" : "#6b7280"}
              trackColor={{ true: "#065f46", false: "#111827" }}
            />
          </View>

          <TouchableOpacity
            style={[styles.sendBtn, !canSend && styles.sendBtnDisabled]}
            activeOpacity={0.8}
            disabled={!canSend}
            onPress={() => {
              const cleanOptions = options
                .map((o) => o.trim())
                .filter((o) => o.length > 0);
              if (cleanOptions.length < 2) return;
              onCreate({
                question: question.trim(),
                options: cleanOptions,
                allowMulti,
              });
              setQuestion("");
              setOptions(["", ""]);
              setAllowMulti(true);
              onClose();
            }}
          >
            <Text style={styles.sendText}>Send poll</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    justifyContent: "flex-end",
    backgroundColor: "rgba(15,23,42,0.75)",
  },
  sheet: {
    backgroundColor: "#020617",
    borderTopLeftRadius: 18,
    borderTopRightRadius: 18,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.4)",
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: 24,
    flexShrink: 0,
    maxHeight: 700,
  },
  headerRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 8,
  },
  title: {
    fontSize: 14,
    color: "#e5e7eb",
    fontWeight: "700",
  },
  closeBtn: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.8)",
  },
  closeText: {
    fontSize: 11,
    color: "#9ca3af",
    letterSpacing: 1,
    textTransform: "uppercase",
  },
  label: {
    fontSize: 12,
    color: "#9ca3af",
    marginBottom: 4,
  },
  inputBox: {
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  input: {
    fontSize: 14,
    color: "#e5e7eb",
  },
  optionRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 6,
  },
  optionIndex: {
    fontSize: 13,
    color: "#9ca3af",
    width: 18,
  },
  optionInput: {
    flex: 1,
    fontSize: 14,
    color: "#e5e7eb",
    borderBottomWidth: 1,
    borderBottomColor: "rgba(55,65,81,0.9)",
    paddingVertical: 2,
  },
  addOptionBtn: {
    marginTop: 10,
    alignSelf: "flex-start",
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.8)",
  },
  addOptionText: {
    fontSize: 11,
    color: "#38bdf8",
    textTransform: "uppercase",
    letterSpacing: 0.6,
  },
  multiRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginTop: 10,
  },
  multiLabel: {
    fontSize: 12,
    color: "#e5e7eb",
  },
  sendBtn: {
    marginTop: 12,
    alignSelf: "flex-end",
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "#22c55e",
  },
  sendBtnDisabled: {
    backgroundColor: "#1f2937",
  },
  sendText: {
    fontSize: 12,
    color: "#022c22",
    fontWeight: "700",
    textTransform: "uppercase",
    letterSpacing: 0.8,
  },
});
