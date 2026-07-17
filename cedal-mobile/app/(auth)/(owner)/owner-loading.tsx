// app/(auth)/(owner)/owner-loading.tsx
import React, { useEffect, useState } from "react";
import { View, Text, StyleSheet, Platform } from "react-native";
import { useRouter } from "expo-router";
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withRepeat,
} from "react-native-reanimated";

const PHASES = [
  "Spinning up developer workspace...",
  "Loading tools and panels...",
  "Syncing code context...",
  "Preparing dev assistant...",
  "Developer environment ready.",
];

export default function DeveloperLoadingScreen() {
  const router = useRouter();
  const [phase, setPhase] = useState(0);
  const [progress, setProgress] = useState(0);

  const pulse = useSharedValue(0);

  useEffect(() => {
    pulse.value = withRepeat(withTiming(1, { duration: 1200 }), -1, true);
  }, [pulse]);

  useEffect(() => {
    const interval = setInterval(() => {
      setPhase((prev) => {
        if (prev >= PHASES.length - 1) return prev;
        return prev + 1;
      });
      setProgress((prev) =>
        Math.min(prev + Math.round(100 / PHASES.length), 100)
      );
    }, 1200);

    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (phase === PHASES.length - 1) {
      const t = setTimeout(() => {
        // after developer boot, route into dev area
        router.replace("/(auth)/(owner)/creator-developers");
      }, 1600);
      return () => clearTimeout(t);
    }
  }, [phase, router]);

  const orbStyle = useAnimatedStyle(() => ({
    opacity: 0.3 + 0.5 * pulse.value,
    transform: [{ scale: 0.95 + 0.1 * pulse.value }],
  }));

  const barStyle = useAnimatedStyle(() => ({
    width: `${progress}%`,
  }));

  return (
    <View style={styles.root}>
      <Animated.View style={[styles.orb, orbStyle]} />

      <View style={styles.card}>
        <Text style={styles.heading}>Developer node booting</Text>
        <Text style={styles.subheading}>CEDAL / DEVELOPER CHANNEL</Text>

        <View style={styles.statusBlock}>
          <Text style={styles.statusLabel}>
            STATUS:{" "}
            <Text style={styles.statusValue}>
              {phase < PHASES.length - 1 ? "INITIALIZING" : "READY"}
            </Text>
          </Text>
          <Text style={styles.phaseText}>{PHASES[phase]}</Text>
        </View>

        <View style={styles.progressShell}>
          <View style={styles.progressTrack}>
            <Animated.View style={[styles.progressFill, barStyle]} />
          </View>
          <View style={styles.progressMeta}>
            <Text style={styles.progressMetaText}>PROGRESS</Text>
            <Text style={styles.progressMetaText}>{progress}%</Text>
          </View>
        </View>

        <View style={styles.logsShell}>
          {[
            "Linking dev assistant ↔ workspace",
            "Loading code navigation context",
            "Syncing recent error logs",
            "Priming code actions",
          ].map((text, idx) => (
            <Text key={idx} style={styles.logLine}>
              ▸ {text}
            </Text>
          ))}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: "#020617",
    alignItems: "center",
    justifyContent: "center",
    padding: 24,
  },
  orb: {
    position: "absolute",
    width: 260,
    height: 260,
    borderRadius: 999,
    backgroundColor: "rgba(56,189,248,0.35)",
    top: "18%",
  },
  card: {
    width: "100%",
    maxWidth: 480,
    borderRadius: 24,
    padding: 20,
    backgroundColor: "rgba(15,23,42,0.98)",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
  },
  heading: {
    fontSize: 16,
    color: "#e5e7eb",
    fontWeight: "600",
    letterSpacing: 2,
    textTransform: "uppercase",
    textAlign: "center",
  },
  subheading: {
    fontSize: 11,
    color: "#93c5fd",
    letterSpacing: 2,
    textAlign: "center",
    marginTop: 4,
    marginBottom: 20,
  },
  statusBlock: {
    marginBottom: 16,
  },
  statusLabel: {
    fontSize: 11,
    color: "#9ca3af",
    textAlign: "center",
    letterSpacing: 1.3,
    marginBottom: 6,
    textTransform: "uppercase",
  },
  statusValue: {
    color: "#22d3ee",
    fontWeight: "700",
  },
  phaseText: {
    fontSize: 14,
    fontWeight: "500",
    color: "#e5e7eb",
    textAlign: "center",
  },
  progressShell: {
    marginTop: 10,
  },
  progressTrack: {
    width: "100%",
    height: 4,
    borderRadius: 999,
    backgroundColor: "rgba(31,41,55,0.9)",
    overflow: "hidden",
  },
  progressFill: {
    height: "100%",
    borderRadius: 999,
    backgroundColor: "#22d3ee",
  },
  progressMeta: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 6,
  },
  progressMetaText: {
    fontSize: 10,
    color: "#9ca3af",
  },
  logsShell: {
    marginTop: 20,
    borderRadius: 12,
    backgroundColor: "rgba(15,23,42,0.9)",
    padding: 10,
    borderWidth: 1,
    borderColor: "rgba(37,99,235,0.4)",
  },
  logLine: {
    fontSize: 10,
    color: "#93c5fd",
    fontFamily: Platform.select({ ios: "Menlo", android: "monospace" }),
    marginBottom: 2,
  },
});
