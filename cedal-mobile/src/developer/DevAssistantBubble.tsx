// src/developer/DevAssistantBubble.tsx
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

export default function DevAssistantBubble({ onPress, style }: Props) {
  return (
    <TouchableOpacity
      activeOpacity={0.8}
      onPress={onPress}
      style={[styles.circleWrapper, style]}
    >
      <Text style={styles.circleText}>A</Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  circleWrapper: {
    width: 50,
    height: 50,
    borderRadius: 25,
    backgroundColor: "rgba(34,211,238,0.18)",
    borderWidth: 1,
    borderColor: "#22d3ee",
    alignItems: "center",
    justifyContent: "center",
    shadowColor: "#22d3ee",
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

