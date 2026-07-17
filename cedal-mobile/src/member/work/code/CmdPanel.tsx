// src/member/work/code/CmdPanel.tsx
import React, { useCallback } from "react";
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TextInput,
  NativeSyntheticEvent,
  TextInputSubmitEditingEventData,
} from "react-native";

type Props = {
  cmdText: string;
  onChangeCmdText: (value: string) => void;
  onSubmit: () => void;
  borderColor: string;
};

export default function CmdPanel({
  cmdText,
  onChangeCmdText,
  onSubmit,
  borderColor,
}: Props) {
  const handleSubmit = useCallback(
    (_e: NativeSyntheticEvent<TextInputSubmitEditingEventData>) => {
      onSubmit();
    },
    [onSubmit]
  );

  return (
    <View
      style={[
        styles.cmdBox,
        { borderColor },
      ]}
    >
      <Text style={styles.cmdLabel}>cmd</Text>
      <ScrollView
        style={styles.cmdScroll}
        contentContainerStyle={{ padding: 6 }}
      >
        <TextInput
          value={cmdText}
          onChangeText={onChangeCmdText}
          multiline
          onSubmitEditing={handleSubmit}
          blurOnSubmit={false}
          style={styles.cmdInput}
        />
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  cmdBox: {
    flex: 1,
    borderRadius: 12,
    borderWidth: 1,
    margin: 8,
    overflow: "hidden",
  },
  cmdLabel: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    fontSize: 10,
    color: "#6b7280",
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(15,23,42,1)",
    letterSpacing: 1.2,
    textTransform: "uppercase",
  },
  cmdScroll: { flex: 1 },
  cmdInput: {
    fontFamily: "monospace",
    fontSize: 11,
    color: "#bbf7d0",
    minHeight: 80,
  },
});
