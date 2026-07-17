// app/(auth)/(member)/loading-sign-in.tsx
import React, { useEffect, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  Platform,
  Dimensions,
} from "react-native";
import { useRouter, useLocalSearchParams } from "expo-router";
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withRepeat,
  withSequence,
} from "react-native-reanimated";
import {
  Canvas,
  Rect,
  LinearGradient as SkiaLinearGradient,
  vec,
  Blur,
} from "@shopify/react-native-skia";

const { width } = Dimensions.get("window");

const PHASES = [
  "Authenticating credentials...",
  "Checking account status...",
  "Profile verification...",
  "Loading user profile...",
  "Syncing session data...",
  "Access granted!",
];

export default function MemberLoadingSignInScreen() {
  const router = useRouter();
  const { from } = useLocalSearchParams<{ from?: string }>();
  const [phase, setPhase] = useState(0);
  const [progress, setProgress] = useState(0);

  const bgShift = useSharedValue(0);
  const panelPulse = useSharedValue(0);
  const orbPulse = useSharedValue(0);

  useEffect(() => {
    bgShift.value = withRepeat(
      withTiming(1, { duration: 20000 }),
      -1,
      false
    );
    panelPulse.value = withRepeat(
      withSequence(
        withTiming(1, { duration: 1800 }),
        withTiming(0, { duration: 1800 })
      ),
      -1,
      false
    );
    orbPulse.value = withRepeat(
      withSequence(
        withTiming(1, { duration: 2200 }),
        withTiming(0, { duration: 2200 })
      ),
      -1,
      false
    );
  }, [bgShift, panelPulse, orbPulse]);

  useEffect(() => {
    const interval = setInterval(() => {
      setPhase((prev) => {
        if (prev >= PHASES.length - 1) return prev;
        return prev + 1;
      });
      setProgress((prev) =>
        Math.min(prev + Math.round(100 / PHASES.length), 100)
      );
    }, 1400);

    return () => clearInterval(interval);
  }, []);

  // in loading-sign-in.tsx
useEffect(() => {
  if (phase === PHASES.length - 1) {
    const t = setTimeout(() => {
      router.replace("/(auth)/(member)" as any);
    }, 2000);
    return () => clearTimeout(t);
  }
}, [phase, router, from]);

  const orbStyle = useAnimatedStyle(() => ({
    opacity: 0.2 + 0.4 * orbPulse.value,
    transform: [{ scale: 1 + 0.05 * orbPulse.value }],
  }));

  const panelAnimatedStyle = useAnimatedStyle(() => ({
    transform: [{ scale: 1 + 0.01 * panelPulse.value }],
    shadowOpacity: 0.7 + 0.3 * panelPulse.value,
  }));

  const progressBarStyle = useAnimatedStyle(() => ({
    width: `${progress}%`,
  }));

  return (
    <View style={styles.root}>
      {/* radial gradient background via View layers */}
      <View style={styles.bgLayer} />
      <View style={[styles.bgOrb, styles.bgOrbLeft]} />
      <View style={[styles.bgOrb, styles.bgOrbRight]} />

      {/* Skia subtle wash */}
      <Canvas style={StyleSheet.absoluteFillObject} pointerEvents="none">
        <Rect x={0} y={0} width={width} height={800}>
          <SkiaLinearGradient
            start={vec(0, 0)}
            end={vec(width, 800)}
            colors={[
              "rgba(56,189,248,0.18)",
              "rgba(129,140,248,0.22)",
              "rgba(15,23,42,0.0)",
            ]}
          />
          <Blur blur={26} />
        </Rect>
      </Canvas>

      {/* main panel */}
      <Animated.View style={[styles.panelOuter, panelAnimatedStyle]}>
        <View style={styles.panelInner}>
          {/* inner grid */}
          <View style={styles.gridOverlay} pointerEvents="none" />

          {/* logo */}
          <View style={styles.logoBlock}>
            <Animated.View style={[styles.logoOrb, orbStyle]} />
            <View style={styles.logoGlyph}>
              <Text style={styles.logoGlyphText}>◈</Text>
            </View>
            <Text style={styles.logoTitle}>CEDAL / MEMBER NODE</Text>
            <Text style={styles.logoSubtitle}>SIGN-IN CHANNEL</Text>
          </View>

          {/* status */}
          <View style={styles.statusBlock}>
            <Text style={styles.statusLabel}>
              STATUS:{" "}
              <Text style={styles.statusValue}>
                {phase < PHASES.length - 1 ? "VERIFYING" : "READY"}
              </Text>
            </Text>
            <Text style={styles.phaseText}>{PHASES[phase]}</Text>
          </View>

          {/* progress bar */}
          <View style={styles.progressShell}>
            <View style={styles.progressTrack}>
              <Animated.View
                style={[styles.progressFill, progressBarStyle]}
              />
            </View>
            <View style={styles.progressMeta}>
              <Text style={styles.progressMetaText}>PROGRESS</Text>
              <Text style={styles.progressMetaText}>{progress}%</Text>
            </View>
          </View>

          {/* live logs */}
          <View style={styles.logsShell}>
            <Text style={styles.logsBadge}>LIVE</Text>
            <View style={styles.logsList}>
              {[
                { text: "Auth handshake", minPhase: 1 },
                { text: "Credentials validated", minPhase: 2 },
                { text: "Session deployed", minPhase: 4 },
                { text: "Access granted → LON-01", minPhase: 5 },
              ].map((log, idx) => {
                const complete = phase >= log.minPhase;
                return (
                  <Text
                    key={idx}
                    style={[
                      styles.logLine,
                      complete
                        ? styles.logLineComplete
                        : styles.logLinePending,
                    ]}
                  >
                    ▸ [{new Date().toLocaleTimeString()}] {log.text}{" "}
                    {complete ? "✓" : "⏳"}
                  </Text>
                );
              })}
            </View>
          </View>

          {/* region information */}
          <View style={styles.regionBlock}>
            <Text style={styles.regionText}>
              REGION: LON-01  ·  CHANNEL: AUTH-MEMBER-01
            </Text>
          </View>
        </View>
      </Animated.View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: "#020617",
    alignItems: "center",
    justifyContent: "center",
  },
  bgLayer: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "#020617",
  },
  bgOrb: {
    position: "absolute",
    borderRadius: 999,
    backgroundColor: "rgba(56,189,248,0.28)",
    shadowColor: "#38bdf8",
    shadowOpacity: 0.9,
    shadowRadius: 80,
    shadowOffset: { width: 0, height: 0 },
  },
  bgOrbLeft: {
    width: 220,
    height: 220,
    top: "18%",
    left: -60,
  },
  bgOrbRight: {
    width: 180,
    height: 180,
    bottom: "12%",
    right: -40,
    backgroundColor: "rgba(236,72,153,0.28)",
    shadowColor: "#ec489f",
  },
  panelOuter: {
    width: "90%",
    maxWidth: 520,
    borderRadius: 28,
    padding: 4,
    backgroundColor: "transparent",
    shadowColor: "#38bdf8",
    shadowRadius: 40,
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.8,
  },
  panelInner: {
    borderRadius: 24,
    padding: 24,
    backgroundColor: "rgba(15,23,42,0.98)",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.5)",
    overflow: "hidden",
  },
  gridOverlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "transparent",
  },
  logoBlock: {
    alignItems: "center",
    marginBottom: 20,
  },
  logoOrb: {
    position: "absolute",
    width: 120,
    height: 120,
    borderRadius: 999,
    backgroundColor: "rgba(56,189,248,0.28)",
    top: -40,
  },
  logoGlyph: {
    width: 50,
    height: 50,
    borderRadius: 999,
    borderWidth: 2,
    borderColor: "rgba(15,23,42,0.9)",
    backgroundColor: "rgba(15,23,42,0.9)",
    alignItems: "center",
    justifyContent: "center",
    marginBottom: 10,
  },
  logoGlyphText: {
    fontSize: 22,
    color: "#e0f2fe",
  },
  logoTitle: {
    fontSize: 13,
    letterSpacing: 3,
    textTransform: "uppercase",
    color: "#60a5fa",
    fontWeight: "700",
  },
  logoSubtitle: {
    fontSize: 10,
    letterSpacing: 2,
    color: "#9ca3af",
    marginTop: 4,
    textTransform: "uppercase",
  },
  statusBlock: {
    marginBottom: 20,
  },
  statusLabel: {
    fontSize: 11,
    color: "#9ca3af",
    textAlign: "center",
    letterSpacing: 1.4,
    textTransform: "uppercase",
    marginBottom: 6,
  },
  statusValue: {
    color: "#22c55e",
    fontWeight: "700",
  },
  phaseText: {
    fontSize: 15,
    fontWeight: "600",
    color: "#e5e7eb",
    textAlign: "center",
    letterSpacing: 0.5,
    lineHeight: 20,
  },
  progressShell: {
    marginTop: 10,
  },
  progressTrack: {
    width: "100%",
    height: 4,
    borderRadius: 2,
    backgroundColor: "rgba(55,65,81,0.6)",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.4)",
    overflow: "hidden",
  },
  progressFill: {
    height: "100%",
    borderRadius: 2,
    backgroundColor: "#38bdf8",
    shadowColor: "#38bdf8",
    shadowOpacity: 0.8,
    shadowRadius: 10,
    shadowOffset: { width: 0, height: 0 },
  },
  progressMeta: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginTop: 6,
  },
  progressMetaText: {
    fontSize: 10,
    color: "#9ca3af",
    letterSpacing: 0.6,
  },
  logsShell: {
    marginTop: 22,
    borderRadius: 12,
    backgroundColor: "rgba(15,23,42,0.8)",
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.3)",
    padding: 10,
    minHeight: 80,
    position: "relative",
  },
  logsBadge: {
    position: "absolute",
    top: 6,
    right: 10,
    fontSize: 9,
    color: "#22c55e",
    letterSpacing: 1.5,
  },
  logsList: {
    marginTop: 4,
  },
  logLine: {
    fontSize: 10,
    fontFamily: Platform.select({ ios: "Menlo", android: "monospace" }),
    marginBottom: 2,
  },
  logLineComplete: {
    color: "#22c55e",
  },
  logLinePending: {
    color: "#93c5fd",
    opacity: 0.5,
  },
  regionBlock: {
    marginTop: 18,
    alignItems: "center",
  },
  regionText: {
    fontSize: 11,
    color: "#6b7280",
    letterSpacing: 1.4,
  },
});
