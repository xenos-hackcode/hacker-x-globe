// app/(auth)/sign-in.tsx
import { useRouter } from "expo-router";
import React, { useEffect, useState } from "react";
import {
  Alert,
  ImageBackground,
  Platform,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";

import Animated, {
  useAnimatedStyle,
  useDerivedValue,
  useSharedValue,
  withRepeat,
  withSpring,
  withTiming,
} from "react-native-reanimated";

import {
  Blur,
  Canvas,
  Rect,
  LinearGradient as SkiaLinearGradient,
  vec,
} from "@shopify/react-native-skia";

import * as LocalAuthentication from "expo-local-authentication";
import * as SecureStore from "expo-secure-store";

import { signInWithEmail } from "@/src/api/auth";
import { sendPasswordResetEmail } from "firebase/auth";
import { auth } from "@/src/api/firebase";

const bgImage = require("@/assets/logp.png");

// Xenos chip messages
const XENOS_MESSAGES = [
  "Booting neural link...",
  "Get ready to enjoy hacking.",
  "Chats are encrypted thoughts.",
  "It games on, welcome gamers.",
  "Welcome to Cedal, noobs.",
  "Every ping leaves a quantum trace.",
];

export default function SignInScreen() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [busy, setBusy] = useState(false);

  const [xenosIndex, setXenosIndex] = useState(0);
  const [showXenos, setShowXenos] = useState(false);

  const [termsAccepted, setTermsAccepted] = useState(false);
  const [biometricAvailable, setBiometricAvailable] = useState(false);

  const [rememberMe, setRememberMe] = useState(false);
  const [rememberTapCount, setRememberTapCount] = useState(0);

  // glitch / holo shared values
  const glitchPhase = useSharedValue(0);
  const scanPhase = useSharedValue(0);
  const circuitPulse = useSharedValue(0);
  const cardOpacity = useSharedValue(0);
  const cardTranslateY = useSharedValue(30);

  // start simple loops
  useEffect(() => {
    glitchPhase.value = withRepeat(withTiming(1, { duration: 900 }), -1, true);
    circuitPulse.value = withRepeat(
      withTiming(1, { duration: 1200 }),
      -1,
      true
    );
    // card entrance
    cardOpacity.value = withSpring(1, { damping: 14 });
    cardTranslateY.value = withSpring(0, { damping: 14 });
  }, []);

  // load remember-me state + stored email
  useEffect(() => {
    (async () => {
      try {
        const remembered = await SecureStore.getItemAsync("cedal_remember_me");
        const rememberedEmail = await SecureStore.getItemAsync(
          "cedal_remember_email"
        );
        if (remembered === "true" && rememberedEmail) {
          setRememberMe(true);
          setEmail(rememberedEmail);
        }
      } catch {
        // ignore
      }
    })();
  }, []);

  // check biometric support + whether user bound a session
  useEffect(() => {
    (async () => {
      try {
        const hasHardware = await LocalAuthentication.hasHardwareAsync();
        const isEnrolled = await LocalAuthentication.isEnrolledAsync();
        const bound = await SecureStore.getItemAsync(
          "cedal_biometric_enabled"
        );
        if (hasHardware && isEnrolled && bound === "true") {
          setBiometricAvailable(true);
        }
      } catch {
        setBiometricAvailable(false);
      }
    })();
  }, []);

  function handleXenosChipPress() {
    setShowXenos(true);
    setXenosIndex((prev) => (prev + 1) % XENOS_MESSAGES.length);
  }

  const disabledByTerms = !termsAccepted || busy;

  async function handleEmailSignIn() {
    if (!termsAccepted) {
      Alert.alert("Terms required", "Bind terms to access Cedal.");
      return;
    }
    try {
      setBusy(true);
      scanPhase.value = withRepeat(withTiming(1, { duration: 800 }), 1, false);
      if (!email || !password) {
        Alert.alert("Missing info", "Enter both email and passcode.");
        return;
      }

      // remember-me behavior (local only)
      if (rememberMe) {
        await SecureStore.setItemAsync("cedal_remember_me", "true");
        await SecureStore.setItemAsync("cedal_remember_email", email.trim());
      } else {
        await SecureStore.deleteItemAsync("cedal_remember_me");
        await SecureStore.deleteItemAsync("cedal_remember_email");
      }

      // READ-ONLY AUTH: just sign in, do not create/update Firestore here
      await signInWithEmail(email.trim(), password);

      // go straight to the node password screen
      router.replace("/(auth)/enter-password");
    } catch (e: any) {
      Alert.alert("Sign in error", e.message ?? "Failed to authorize session.");
    } finally {
      setBusy(false);
    }
  }

  async function handleBiometricSignIn() {
    if (!termsAccepted) {
      Alert.alert("Terms required", "Bind terms to access Cedal.");
      return;
    }
    try {
      const result = await LocalAuthentication.authenticateAsync({
        promptMessage: "Unlock Cedal node",
        fallbackLabel: "Use passcode",
      });
      if (!result.success) return;

      const token = await SecureStore.getItemAsync("cedal_session_token");
      if (!token) {
        Alert.alert(
          "No bound node",
          "Sign in once to bind this device to a node."
        );
        return;
      }

      // biometric login: same loader, with flag if you want special handling
      router.replace("/(auth)/(member)/loading-sign-in?from=biometric");
    } catch (e: any) {
      Alert.alert(
        "Biometric error",
        e?.message ?? "Could not unlock node via biometric."
      );
    }
  }

  // Forgot password with confirm
  function handleForgotPress() {
    if (!email.trim()) {
      Alert.alert("Reset password", "Enter your email first.");
      return;
    }
    Alert.alert(
      "Forgot password",
      "Are you sure you forgot your password?",
      [
        {
          text: "No",
          style: "cancel",
        },
        {
          text: "Yes",
          style: "destructive",
          onPress: () => handleForgotLoginPassword(),
        },
      ]
    );
  }

  async function handleForgotLoginPassword() {
    try {
      await sendPasswordResetEmail(auth, email.trim());
      Alert.alert(
        "Reset link sent",
        "Check your email for a Cedal password reset link."
      );
    } catch (e: any) {
      Alert.alert(
        "Reset error",
        e?.message ?? "Could not send reset email."
      );
    }
  }

  // remember-me hidden taps → creator passcode screen
  function handleRememberTap() {
    setRememberMe((v) => !v);
  }

  // glitch offsets for title
  const glitchBlue = useDerivedValue(() => ({
    x: (Math.random() - 0.5) * 1.5 * glitchPhase.value,
    y: (Math.random() - 0.5) * 1.5 * glitchPhase.value,
  }));
  const glitchPink = useDerivedValue(() => ({
    x: (Math.random() - 0.5) * 2.0 * glitchPhase.value,
    y: (Math.random() - 0.5) * 2.0 * glitchPhase.value,
  }));

  const titleBlueStyle = useAnimatedStyle(() => ({
    transform: [
      { translateX: glitchBlue.value.x },
      { translateY: glitchBlue.value.y },
    ],
    opacity: 0.6,
  }));
  const titlePinkStyle = useAnimatedStyle(() => ({
    transform: [
      { translateX: glitchPink.value.x },
      { translateY: glitchPink.value.y },
    ],
    opacity: 0.4,
  }));

  // simple scan bar over inputs
  const scanStyle = useAnimatedStyle(() => ({
    opacity: scanPhase.value > 0 && scanPhase.value < 1 ? 0.7 : 0,
    transform: [
      {
        translateX: scanPhase.value * 220 - 20,
      },
    ],
  }));

  // circuit pulse alpha
  const circuitStyle = useAnimatedStyle(() => ({
    opacity: 0.15 + 0.35 * circuitPulse.value,
  }));

  const cardAnimatedStyle = useAnimatedStyle(() => ({
    opacity: cardOpacity.value,
    transform: [{ translateY: cardTranslateY.value }],
  }));

  return (
    <ImageBackground source={bgImage} style={styles.bg} resizeMode="cover">
      {/* dark overlay */}
      <View style={styles.overlay} />

      {/* top HUD bar */}
      <View style={styles.topHudBar}>
        <Text style={styles.topHudText}>CEDAL NODE · SECURE LINK</Text>
      </View>

      {/* vertical neon rails */}
      <View style={styles.matrixStripeLeft} />
      <View style={styles.matrixStripeRight} />

      {/* center manhwa panel */}
      <View style={styles.centerWrap}>
        <Animated.View style={[styles.panel, cardAnimatedStyle]}>
          {/* Skia holographic shine on card */}
          <Canvas style={styles.cardCanvas} pointerEvents="none">
            <Rect x={0} y={0} width={500} height={500}>
              <SkiaLinearGradient
                start={vec(0, 0)}
                end={vec(500, 500)}
                colors={[
                  "rgba(56,189,248,0.32)",
                  "rgba(236,72,153,0.22)",
                  "rgba(15,23,42,0.0)",
                ]}
              />
              <Blur blur={22} />
            </Rect>
          </Canvas>

          {/* corner chips */}
          <View style={[styles.chip, styles.chipTopLeft]} />
          <View style={[styles.chip, styles.chipTopRight]} />
          <View style={[styles.chip, styles.chipBottomLeft]} />
          <View style={[styles.chip, styles.chipBottomRight]} />

          {/* circuit layer */}
          <Animated.View style={[styles.circuitLayer, circuitStyle]}>
            {/* horizontal wire */}
            <View
              style={{
                position: "absolute",
                left: 40,
                top: 80,
                width: 160,
                height: 1,
                backgroundColor: "rgba(56,189,248,0.7)",
              }}
            />
            {/* vertical drop */}
            <View
              style={{
                position: "absolute",
                left: 200,
                top: 80,
                width: 1,
                height: 80,
                backgroundColor: "rgba(56,189,248,0.7)",
              }}
            />
            {/* node */}
            <View
              style={{
                position: "absolute",
                left: 196,
                top: 76,
                width: 8,
                height: 8,
                borderRadius: 4,
                backgroundColor: "rgba(129,140,248,0.9)",
              }}
            />
          </Animated.View>

          {/* content */}
          <View style={styles.content}>
            {/* glitch title */}
            <View style={styles.titleWrap}>
              <Text style={styles.titleBase}>CEDAL ACCESS</Text>
              <Animated.Text style={[styles.titleBlue, titleBlueStyle]}>
                CEDAL ACCESS
              </Animated.Text>
              <Animated.Text style={[styles.titlePink, titlePinkStyle]}>
                CEDAL ACCESS
              </Animated.Text>
            </View>
            <Text style={styles.subtitle}>NEURAL SIGN IN PANEL</Text>

            {/* email */}
            <View style={styles.fieldBlock}>
              <Text style={styles.fieldLabel}>ACCESS ID</Text>
              <View style={styles.fieldShell}>
                <TextInput
                  value={email}
                  onChangeText={setEmail}
                  placeholder="you@cedal.app"
                  placeholderTextColor="#64748b"
                  autoCapitalize="none"
                  keyboardType="email-address"
                  style={styles.fieldInput}
                />
                {/* scan bar */}
                <Animated.View style={[styles.scanBar, scanStyle]} />
              </View>
            </View>

            {/* password */}
            <View style={styles.fieldBlock}>
              <Text style={styles.fieldLabel}>PASSWORD</Text>
              <View style={styles.fieldShell}>
                <View style={{ flexDirection: "row", alignItems: "center" }}>
                  <TextInput
                    value={password}
                    onChangeText={setPassword}
                    placeholder="••••••••"
                    placeholderTextColor="#64748b"
                    secureTextEntry={!showPassword}
                    style={[styles.fieldInput, { flex: 1 }]}
                  />
                  <TouchableOpacity
                    onPress={() => setShowPassword((v) => !v)}
                    activeOpacity={0.7}
                    style={{ paddingHorizontal: 6, paddingVertical: 4 }}
                  >
                    <Text
                      style={{
                        fontSize: 10,
                        color: "#e5e7eb",
                        letterSpacing: 1.5,
                      }}
                    >
                      {showPassword ? "HIDE" : "SHOW"}
                    </Text>
                  </TouchableOpacity>
                </View>
              </View>
            </View>

            {/* forgotten login password */}
            <TouchableOpacity
              onPress={handleForgotPress}
              style={{ alignSelf: "flex-end", marginTop: 6 }}
            >
              <Text
                style={{
                  fontSize: 11,
                  color: "#93c5fd",
                  letterSpacing: 1.5,
                  textTransform: "uppercase",
                }}
              >
                forgotten login password
              </Text>
            </TouchableOpacity>

            {/* terms + remember me row */}
            <View style={styles.dualRow}>
              {/* terms toggle */}
              <TouchableOpacity
                style={styles.termsRow}
                activeOpacity={0.8}
                onPress={() => setTermsAccepted((v) => !v)}
              >
                <View
                  style={[
                    styles.termsToggle,
                    termsAccepted && styles.termsToggleOn,
                  ]}
                >
                  <View
                    style={[
                      styles.termsKnob,
                      termsAccepted && styles.termsKnobOn,
                    ]}
                  />
                </View>
                <Text style={styles.termsLabelSmall}>TERMS</Text>
              </TouchableOpacity>

              {/* remember me toggle with hidden taps */}
              <TouchableOpacity
                style={styles.rememberRow}
                activeOpacity={0.8}
                onPress={handleRememberTap}
              >
                <View
                  style={[
                    styles.rememberToggle,
                    rememberMe && styles.rememberToggleOn,
                  ]}
                >
                  {rememberMe && <View style={styles.rememberDot} />}
                </View>
                <Text style={styles.rememberText}>Remember me</Text>
              </TouchableOpacity>
            </View>

            {/* status */}
            <Text style={styles.statusText}>
              {busy ? "SCANNING CREDENTIALS…" : "CHANNEL READY"}
            </Text>

            {/* main button */}
            <TouchableOpacity
              onPress={handleEmailSignIn}
              disabled={disabledByTerms}
              activeOpacity={0.8}
              style={[
                styles.primaryBtn,
                disabledByTerms && { opacity: 0.4 },
              ]}
            >
              <Text style={styles.primaryText}>
                {busy ? "AUTHORIZING…" : "ENTER NETWORK"}
              </Text>
            </TouchableOpacity>

            {/* biometric touch panel */}
            {biometricAvailable && (
              <TouchableOpacity
                onPress={handleBiometricSignIn}
                activeOpacity={0.9}
                style={styles.fingerprintBtn}
              >
                <Text style={styles.fingerprintGlyph}>▋▌▙▛</Text>
                <View style={{ marginLeft: 10 }}>
                  <Text style={styles.fingerprintTitle}>TOUCH PANEL</Text>
                  <Text style={styles.fingerprintSub}>
                    biometric access to bound node
                  </Text>
                </View>
              </TouchableOpacity>
            )}

            {/* sign up link */}
            <TouchableOpacity
              onPress={() => router.push("/(auth)/sign-up")}
              style={styles.linkBtn}
            >
              <Text style={styles.linkText}>create new identity</Text>
            </TouchableOpacity>

            {/* Xenos message chip */}
            <View style={styles.xenosRow}>
              <TouchableOpacity
                onPress={handleXenosChipPress}
                activeOpacity={0.8}
                style={styles.xenosChipMain}
              >
                <Text style={styles.xenosChipMainLabel}>TAP</Text>
                <Text
                  style={styles.xenosChipMainText}
                  numberOfLines={1}
                  ellipsizeMode="tail"
                >
                  {showXenos
                    ? XENOS_MESSAGES[xenosIndex]
                    : "Xenos has a message."}
                </Text>
              </TouchableOpacity>
            </View>

            {/* bottom strips */}
            <View style={styles.bottomStripRow}>
              <View style={styles.bottomStrip} />
              <View
                style={[styles.bottomStrip, { width: "25%", opacity: 0.6 }]}
              />
            </View>
          </View>
        </Animated.View>
      </View>
    </ImageBackground>
  );
}

const styles = StyleSheet.create({
  bg: { flex: 1, backgroundColor: "#020617" },
  overlay: {
    ...StyleSheet.absoluteFillObject,
    backgroundColor: "rgba(2,6,23,0.9)",
  },
  topHudBar: {
    position: "absolute",
    top: Platform.select({ ios: 50, android: 30 }),
    left: 32,
    right: 32,
    height: 32,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
    backgroundColor: "rgba(15,23,42,0.75)",
    alignItems: "center",
    justifyContent: "center",
  },
  topHudText: {
    fontSize: 10,
    letterSpacing: 2,
    color: "#94a3b8",
  },
  matrixStripeLeft: {
    position: "absolute",
    left: 24,
    top: -60,
    bottom: -60,
    width: 3,
    backgroundColor: "#00ffbf",
    opacity: 0.35,
    shadowColor: "#00ffbf",
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.9,
    shadowRadius: 24,
  },
  matrixStripeRight: {
    position: "absolute",
    right: 24,
    top: -40,
    bottom: -40,
    width: 3,
    backgroundColor: "#38bdf8",
    opacity: 0.3,
    shadowColor: "#38bdf8",
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.9,
    shadowRadius: 24,
  },
  centerWrap: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 20,
  },
  panel: {
    width: "100%",
    maxWidth: 440,
    borderRadius: 24,
    borderWidth: 1.5,
    borderColor: "rgba(56,189,248,0.9)",
    backgroundColor: "rgba(15,23,42,0.97)",
    overflow: "hidden",
    shadowColor: "#0ea5e9",
    shadowOpacity: 0.75,
    shadowRadius: 40,
    shadowOffset: { width: 0, height: 0 },
    elevation: 20,
  },
  cardCanvas: {
    ...StyleSheet.absoluteFillObject,
  },
  chip: {
    position: "absolute",
    width: 22,
    height: 22,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: "#67e8f9",
    backgroundColor: "rgba(15,23,42,0.98)",
  },
  chipTopLeft: { top: -2, left: -2 },
  chipTopRight: { top: -2, right: -2 },
  chipBottomLeft: { bottom: -2, left: -2 },
  chipBottomRight: { bottom: -2, right: -2 },
  circuitLayer: {
    ...StyleSheet.absoluteFillObject,
  },
  content: {
    padding: 20,
    paddingTop: 26,
    paddingBottom: 18,
  },
  titleWrap: {
    alignItems: "center",
    marginBottom: 6,
  },
  titleBase: {
    fontSize: 20,
    fontWeight: "700",
    color: "#e5e7eb",
    letterSpacing: 3,
  },
  titleBlue: {
    position: "absolute",
    fontSize: 20,
    fontWeight: "700",
    color: "#38bdf8",
  },
  titlePink: {
    position: "absolute",
    fontSize: 20,
    fontWeight: "700",
    color: "#f472b6",
  },
  subtitle: {
    fontSize: 11,
    color: "#93c5fd",
    letterSpacing: 2,
    textAlign: "center",
    marginBottom: 20,
  },
  fieldBlock: { marginBottom: 12 },
  fieldLabel: {
    fontSize: 11,
    color: "#9ca3af",
    letterSpacing: 2,
    marginBottom: 4,
  },
  fieldShell: {
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: "rgba(148,163,184,0.9)",
    backgroundColor: "#020617",
    paddingHorizontal: 10,
    paddingVertical: 6,
    overflow: "hidden",
  },
  fieldInput: {
    color: "#e5e7eb",
    fontSize: 14,
  },
  scanBar: {
    position: "absolute",
    top: "50%",
    width: 40,
    height: 1,
    backgroundColor: "#22d3ee",
  },
  dualRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginTop: 4,
    marginBottom: 6,
  },
  termsRow: {
    flexDirection: "row",
    alignItems: "center",
  },
  termsToggle: {
    width: 42,
    height: 22,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
    backgroundColor: "rgba(15,23,42,0.9)",
    padding: 2,
    justifyContent: "center",
    marginRight: 6,
  },
  termsToggleOn: {
    borderColor: "#22c55e",
    backgroundColor: "rgba(22,163,74,0.4)",
  },
  termsKnob: {
    width: 16,
    height: 16,
    borderRadius: 999,
    backgroundColor: "#64748b",
  },
  termsKnobOn: {
    alignSelf: "flex-end",
    backgroundColor: "#bbf7d0",
  },
  termsLabelSmall: {
    fontSize: 10,
    letterSpacing: 1.5,
    color: "#e5e7eb",
  },
  rememberRow: {
    flexDirection: "row",
    alignItems: "center",
  },
  rememberToggle: {
    width: 18,
    height: 18,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.8)",
    backgroundColor: "rgba(15,23,42,0.9)",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 6,
  },
  rememberToggleOn: {
    borderColor: "#22c55e",
    backgroundColor: "rgba(22,163,74,0.4)",
  },
  rememberDot: {
    width: 10,
    height: 10,
    borderRadius: 2,
    backgroundColor: "#bbf7d0",
  },
  rememberText: {
    fontSize: 11,
    color: "#e5e7eb",
    letterSpacing: 1.2,
  },
  statusText: {
    fontSize: 10,
    color: "#22c55e",
    letterSpacing: 2,
    marginTop: 4,
    marginBottom: 10,
    textTransform: "uppercase",
  },
  primaryBtn: {
    borderRadius: 999,
    paddingVertical: 12,
    alignItems: "center",
    backgroundColor: "#22d3ee",
    marginBottom: 10,
    shadowColor: "#22d3ee",
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 1,
    shadowRadius: 26,
  },
  primaryText: {
    color: "#020617",
    fontSize: 13,
    fontWeight: "800",
    letterSpacing: 3,
  },
  linkBtn: {
    alignItems: "center",
    marginTop: 4,
  },
  linkText: {
    color: "#93c5fd",
    fontSize: 11,
    letterSpacing: 2,
    textTransform: "uppercase",
  },
  fingerprintBtn: {
    flexDirection: "row",
    alignItems: "center",
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.9)",
    backgroundColor: "rgba(15,23,42,0.95)",
    paddingVertical: 8,
    paddingHorizontal: 12,
    marginTop: 10,
  },
  fingerprintGlyph: {
    fontSize: 20,
    color: "#22d3ee",
  },
  fingerprintTitle: {
    fontSize: 11,
    color: "#e5e7eb",
    letterSpacing: 2,
  },
  fingerprintSub: {
    fontSize: 10,
    color: "#64748b",
  },
  xenosRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 14,
  },
  xenosChipMain: {
    flex: 1,
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.9)",
    backgroundColor: "rgba(15,23,42,0.95)",
    flexDirection: "row",
    alignItems: "center",
  },
  xenosChipMainLabel: {
    fontSize: 10,
    color: "#22d3ee",
    marginRight: 8,
    letterSpacing: 2,
  },
  xenosChipMainText: {
    flex: 1,
    fontSize: 11,
    color: "#e5e7eb",
  },
  bottomStripRow: {
    marginTop: 18,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "flex-start",
  },
  bottomStrip: {
    height: 3,
    width: "40%",
    borderRadius: 999,
    backgroundColor: "#22d3ee",
    shadowColor: "#22d3ee",
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.7,
    shadowRadius: 14,
    marginRight: 6,
  },
});
