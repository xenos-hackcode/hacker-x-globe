// src/member/help/HelpAssistantBubble.tsx
import React from "react";
import {
  StyleSheet,
  Text,
  TouchableOpacity,
  ViewStyle,
  StyleProp,
} from "react-native";

type Props = {
  onPress?: () => void;
  style?: StyleProp<ViewStyle>;
};

export default function HelpAssistantBubble({ onPress, style }: Props) {
  return (
    <TouchableOpacity
      activeOpacity={0.8}
      onPress={onPress}
      style={[styles.circleWrapper, style]}
    >
      <Text style={styles.circleText}>H</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  circleWrapper: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: "rgba(59,130,246,0.18)",
    borderWidth: 1,
    borderColor: "#60a5fa",
    alignItems: "center",
    justifyContent: "center",
    shadowColor: "#60a5fa",
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.8,
    shadowRadius: 16,
  },
  circleText: {
    color: "#e5e7eb",
    fontSize: 20,
    fontWeight: "700",
  },
});
