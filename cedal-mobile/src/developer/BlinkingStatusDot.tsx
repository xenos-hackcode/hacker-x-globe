// BlinkingStatusDot.tsx
import React, { useEffect, useRef } from "react";
import { Animated, StyleSheet } from "react-native";

type Props = {
  color: "green" | "red";
};

export default function BlinkingStatusDot({ color }: Props) {
  const opacity = useRef(new Animated.Value(1)).current;

  useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(opacity, {
          toValue: 0.2,
          duration: 600,
          useNativeDriver: true,
        }),
        Animated.timing(opacity, {
          toValue: 1,
          duration: 600,
          useNativeDriver: true,
        }),
      ])
    );
    loop.start();
    return () => loop.stop();
  }, [opacity]);

  return (
    <Animated.View
      style={[
        styles.dot,
        { opacity },
        color === "green" ? styles.green : styles.red,
      ]}
    />
  );
}

const styles = StyleSheet.create({
  dot: {
    width: 10,
    height: 10,
    borderRadius: 5,
  },
  green: {
    backgroundColor: "#22c55e",
  },
  red: {
    backgroundColor: "#ef4444",
  },
});
