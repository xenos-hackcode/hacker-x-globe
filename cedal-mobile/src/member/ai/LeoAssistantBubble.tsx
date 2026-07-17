// src/member/ai/LeoAssistantBubble.tsx
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

export default function LeoAssistantBubble({ onPress, style }: Props) {
  return (
    <TouchableOpacity
      activeOpacity={0.8}
      onPress={onPress}
      style={[styles.circleWrapper, style]}
    >
      <Text style={styles.circleText}>L</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  circleWrapper: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: "rgba(147,197,253,0.18)",
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
