// app/(auth)/sign-up.tsx
import React, { useState, useEffect } from "react";
import {
  Alert,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  Platform,
} from "react-native";
import { useRouter } from "expo-router";
import { signUpWithEmail, signInAsGuest } from "@/src/api/auth";
import { db, ensureUserProfile, auth } from "@/src/api/firebase";
import { doc, setDoc, getDoc, serverTimestamp } from "firebase/firestore";
import { sendEmailVerification } from "firebase/auth";
import { notifyVerificationSent } from "@/src/member/utils/notifications";
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withSpring,
  withRepeat,
  withTiming,
} from "react-native-reanimated";

import {
  Canvas,
  Rect,
  LinearGradient as SkiaLinearGradient,
  vec,
  Blur,
} from "@shopify/react-native-skia";

export default function SignUpScreen() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [termsAccepted, setTermsAccepted] = useState(false);

  const [waitingVerify, setWaitingVerify] = useState(false);

  const cardOpacity = useSharedValue(0);
  const cardTranslateY = useSharedValue(30);
  const orbPulse = useSharedValue(0);

  useEffect(() => {
    cardOpacity.value = withSpring(1, { damping: 14 });
    cardTranslateY.value = withSpring(0, { damping: 14 });
    orbPulse.value = withRepeat(withTiming(1, { duration: 1600 }), -1, true);
  }, []);

  useEffect(() => {
    (async () => {
      const user = auth.currentUser;
      if (!user) return;

      try {
        const ref = doc(db, "users", user.uid);
        const snap = await getDoc(ref);
        if (snap.exists()) {
          const data = snap.data() as any;
          if (data.acceptedTerms === true) {
            setTermsAccepted(true);
          }
        }
      } catch {
        // ignore
      }
    })();
  }, []);

  const cardAnimatedStyle = useAnimatedStyle(() => ({
    opacity: cardOpacity.value,
    transform: [{ translateY: cardTranslateY.value }],
  }));

  const orbStyle = useAnimatedStyle(() => ({
    opacity: 0.25 + 0.45 * orbPulse.value,
    transform: [{ scale: 1 + 0.05 * orbPulse.value }],
  }));

  const disabledByTerms = !termsAccepted || busy;

  async function handleEmailCreate() {
    if (!termsAccepted) {
      Alert.alert("Terms required", "Bind terms to spawn a node.");
      return;
    }

    if (!email || !password) {
      Alert.alert("Missing info", "Enter email and password.");
      return;
    }

    try {
      setBusy(true);

      const newUser = await signUpWithEmail(email.trim(), password);
      const uid = newUser.uid;

      await ensureUserProfile(uid);

      const ref = doc(db, "users", uid);
      await setDoc(
        ref,
        {
          email: newUser.email,
          online: true,
          lastSeen: serverTimestamp(),
          acceptedTerms: true,
        },
        { merge: true },
      );

      // send verification email ONCE
      if (auth.currentUser) {
        await sendEmailVerification(auth.currentUser);
        await notifyVerificationSent(email.trim());
      }

      Alert.alert(
        "Verify your email",
        "Open your Gmail and find the Cedal verification email. Check both your inbox and spam folder. Once you click the link, return here and tap 'I VERIFIED MY EMAIL'.",
      );

      // stay on this screen, show verify button
      setWaitingVerify(true);
    } catch (e: any) {
      if (e?.code === "auth/too-many-requests") {
        Alert.alert(
          "Too many attempts",
          "We detected too many sign-up or verification attempts from this device. Please wait a few minutes and try again.",
        );
      } else {
        Alert.alert("Sign up error", e?.message ?? "Something went wrong.");
      }
    } finally {
      setBusy(false);
    }
  }

  async function handleCheckVerified() {
    try {
      const user = auth.currentUser;
      if (!user) {
        Alert.alert("Not signed in", "Sign in again to continue.");
        return;
      }

      await user.reload();

      if (user.emailVerified) {
        router.replace("/(auth)/(member)/create-passcode");
      } else {
        Alert.alert(
          "Not verified yet",
          "We still can’t see the verification. Make sure you clicked the link in your Gmail (inbox or spam), then wait a few seconds and tap again.",
        );
      }
    } catch (e: any) {
      Alert.alert(
        "Check failed",
        e?.message ?? "Could not refresh verification status.",
      );
    }
  }

  async function handleGuestCreate() {
    if (!termsAccepted) {
      Alert.alert("Terms required", "Bind terms to spawn a node.");
      return;
    }

    try {
      setBusy(true);

      const guest = await signInAsGuest();
      const uid = guest.uid;

      await ensureUserProfile(uid);

      const userRef = doc(db, "users", uid);
      await setDoc(
        userRef,
        {
          email: null,
          online: true,
          lastSeen: serverTimestamp(),
          acceptedTerms: true,
        },
        { merge: true },
      );

      router.replace("/(auth)/(member)/create-passcode");
    } catch (e: any) {
      Alert.alert(
        "Guest node error",
        e?.message ?? "Could not spawn guest node.",
      );
    } finally {
      setBusy(false);
    }
  }

  return (
    <View style={styles.root}>
      {/* background blobs */}
      <View style={styles.backgroundLayerTop} />
      <View style={styles.backgroundLayerBottom} />

      {/* top HUD bar */}
      <View style={styles.topHudBar}>
        <Text style={styles.topHudText}>CEDAL NODE · NEW REGISTRY</Text>
      </View>

      {/* creation orb behind card */}
      <Animated.View style={[styles.orb, orbStyle]} />

      <Animated.View style={[styles.card, cardAnimatedStyle]}>
        {/* neon grid overlay */}
        <View style={styles.cardGridOverlay}>
          <View
            style={[
              styles.cardGridLine,
              { left: "15%", top: 0, bottom: 0, width: 0.5 },
            ]}
          />
          <View
            style={[
              styles.cardGridLine,
              { left: "50%", top: 0, bottom: 0, width: 0.5 },
            ]}
          />
          <View
            style={[
              styles.cardGridLine,
              { left: "85%", top: 0, bottom: 0, width: 0.5 },
            ]}
          />
          <View
            style={[
              styles.cardGridLine,
              { top: "38%", left: 0, right: 0, height: 0.5 },
            ]}
          />
          <View
            style={[
              styles.cardGridLine,
              { top: "78%", left: 0, right: 0, height: 0.5 },
            ]}
          />
        </View>

        {/* Skia glass wash */}
        <Canvas style={styles.cardCanvas} pointerEvents="none">
          <Rect x={0} y={0} width={500} height={500}>
            <SkiaLinearGradient
              start={vec(0, 0)}
              end={vec(500, 500)}
              colors={[
                "rgba(129,140,248,0.28)",
                "rgba(56,189,248,0.24)",
                "rgba(15,23,42,0.0)",
              ]}
            />
            <Blur blur={20} />
          </Rect>
        </Canvas>

        {/* header */}
        <View style={styles.logoRow}>
          <View style={styles.logoChip}>
            <Text style={styles.logoChipText}>◎</Text>
          </View>
          <View>
            <Text style={styles.logoTitle}>CEDAL NODE</Text>
            <Text style={styles.logoSubtitle}>PROFILE CREATION CONSOLE</Text>
          </View>
        </View>

        <View style={styles.accentStrip} />

        {/* email */}
        <View style={styles.fieldLabelRow}>
          <Text style={styles.fieldLabel}>PRIMARY LINK</Text>
          <View style={styles.fieldLabelChip}>
            <Text style={styles.fieldLabelChipText}>EMAIL</Text>
          </View>
        </View>
        <View style={styles.inputShell}>
          <View style={styles.inputInnerRow}>
            <Text style={styles.inputPrefix}>⧉</Text>
            <TextInput
              value={email}
              onChangeText={setEmail}
              placeholder="enter your access email"
              placeholderTextColor="#9ca3af"
              autoCapitalize="none"
              keyboardType="email-address"
              style={styles.input}
            />
          </View>
        </View>

        {/* password with show/hide */}
        <View style={styles.fieldLabelRow}>
          <Text style={styles.fieldLabel}>MASTER KEY</Text>
          <View style={styles.fieldLabelChip}>
            <Text style={styles.fieldLabelChipText}>MIN 6 CHARS</Text>
          </View>
        </View>
        <View style={styles.inputShell}>
          <View style={styles.inputInnerRow}>
            <Text style={styles.inputPrefix}>★</Text>
            <TextInput
              value={password}
              onChangeText={setPassword}
              placeholder="create a secure pattern"
              placeholderTextColor="#9ca3af"
              secureTextEntry={!showPassword}
              style={styles.input}
            />
            <TouchableOpacity
              onPress={() => setShowPassword((v) => !v)}
              activeOpacity={0.7}
              style={styles.eyeBtn}
            >
              <Text style={styles.eyeTxt}>
                {showPassword ? "HIDE" : "SHOW"}
              </Text>
            </TouchableOpacity>
          </View>
        </View>

        <View style={styles.termsRow}>
          <TouchableOpacity
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
          </TouchableOpacity>

          <TouchableOpacity
            activeOpacity={0.8}
            onPress={() => router.push("/(auth)/(legal)/terms-full")}
            style={styles.termsTextBlock}
          >
            <Text style={styles.termsLabel}>TERMS & CONDITIONS</Text>
            <Text style={styles.termsSub}>
              Tap to read Cedal system rules and data handling.
            </Text>
          </TouchableOpacity>
        </View>

        <View style={styles.chipRow}>
          <View style={styles.tinyChip}>
            <Text style={styles.tinyChipText}>
              {busy ? "WRITING NODE SEED…" : "NODE SEED READY"}
            </Text>
          </View>
          <View style={styles.tinyChip}>
            <Text style={styles.tinyChipText}>
              {termsAccepted ? "TERMS ACCEPTED" : "TERMS REQUIRED"}
            </Text>
          </View>
        </View>

        {/* primary: email node */}
        <TouchableOpacity
          onPress={handleEmailCreate}
          disabled={disabledByTerms || waitingVerify}
          style={[
            styles.primaryBtn,
            (disabledByTerms || waitingVerify) && { opacity: 0.4 },
          ]}
          activeOpacity={0.85}
        >
          <Text style={styles.primaryBtnText}>
            {busy ? "SPAWNING NODE…" : "CREATE NODE (EMAIL)"}
          </Text>
        </TouchableOpacity>

        {/* “I VERIFIED MY EMAIL” button appears once email was sent */}
        {waitingVerify && (
          <TouchableOpacity
            onPress={handleCheckVerified}
            style={styles.altGhostBtn}
            activeOpacity={0.85}
          >
            <Text style={styles.altGhostBtnText}>I VERIFIED MY EMAIL</Text>
          </TouchableOpacity>
        )}

        {/* guest node as secondary action */}
        <TouchableOpacity
          onPress={handleGuestCreate}
          disabled={disabledByTerms}
          style={[
            styles.altGhostBtn,
            disabledByTerms && { opacity: 0.4 },
          ]}
          activeOpacity={0.85}
        >
          <Text style={styles.altGhostBtnText}>SPAWN GUEST NODE</Text>
        </TouchableOpacity>

        {/* link back to sign-in portal */}
        <TouchableOpacity
          onPress={() => router.push("/(auth)/sign-in")}
          style={styles.tertiaryLink}
        >
          <Text style={styles.tertiaryLinkText}>
            Already have a node? Access console
          </Text>
        </TouchableOpacity>
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
  backgroundLayerTop: {
    position: "absolute",
    top: -120,
    left: -80,
    width: 260,
    height: 260,
    borderRadius: 999,
    backgroundColor: "#0f172a",
  },
  backgroundLayerBottom: {
    position: "absolute",
    bottom: -140,
    right: -100,
    width: 300,
    height: 300,
    borderRadius: 999,
    backgroundColor: "#020617",
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
  orb: {
    position: "absolute",
    width: 260,
    height: 260,
    borderRadius: 999,
    backgroundColor: "rgba(56,189,248,0.55)",
    top: "25%",
    alignSelf: "center",
    shadowColor: "#38bdf8",
    shadowOpacity: 0.9,
    shadowRadius: 80,
    shadowOffset: { width: 0, height: 0 },
  },
  card: {
    width: "88%",
    maxWidth: 440,
    borderRadius: 24,
    borderWidth: 1.5,
    borderColor: "rgba(56,189,248,0.9)",
    backgroundColor: "rgba(15,23,42,0.97)",
    padding: 20,
    paddingTop: 26,
    paddingBottom: 18,
    overflow: "hidden",
    shadowColor: "#0ea5e9",
    shadowOpacity: 0.75,
    shadowRadius: 40,
    shadowOffset: { width: 0, height: 0 },
    elevation: 20,
  },
  cardGridOverlay: {
    ...StyleSheet.absoluteFillObject,
    pointerEvents: "none",
  },
  cardGridLine: {
    position: "absolute",
    backgroundColor: "rgba(148,163,184,0.25)",
  },
  cardCanvas: {
    ...StyleSheet.absoluteFillObject,
  },
  logoRow: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 10,
  },
  logoChip: {
    width: 32,
    height: 32,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.9)",
    backgroundColor: "rgba(15,23,42,0.95)",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 10,
  },
  logoChipText: {
    fontSize: 18,
    color: "#22d3ee",
  },
  logoTitle: {
    fontSize: 18,
    fontWeight: "800",
    letterSpacing: 3,
    color: "#e5e7eb",
  },
  logoSubtitle: {
    fontSize: 10,
    color: "#93c5fd",
    letterSpacing: 2,
    marginTop: 2,
  },
  accentStrip: {
    height: 2,
    borderRadius: 999,
    backgroundColor: "#22d3ee",
    marginTop: 14,
    marginBottom: 16,
    shadowColor: "#22d3ee",
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 0.7,
    shadowRadius: 10,
  },
  fieldLabelRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginBottom: 4,
  },
  fieldLabel: {
    fontSize: 11,
    color: "#9ca3af",
    letterSpacing: 2,
  },
  fieldLabelChip: {
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
    backgroundColor: "rgba(15,23,42,0.9)",
  },
  fieldLabelChipText: {
    fontSize: 10,
    color: "#cbd5f5",
    letterSpacing: 1.5,
  },
  inputShell: {
    borderRadius: 14,
    borderWidth: 1.5,
    borderColor: "rgba(148,163,184,0.9)",
    backgroundColor: "#020617",
    paddingHorizontal: 10,
    paddingVertical: 6,
    marginBottom: 10,
  },
  inputInnerRow: {
    flexDirection: "row",
    alignItems: "center",
  },
  inputPrefix: {
    fontSize: 13,
    color: "#6b7280",
    marginRight: 8,
  },
  input: {
    flex: 1,
    color: "#e5e7eb",
    fontSize: 14,
  },
  eyeBtn: {
    paddingHorizontal: 6,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
    backgroundColor: "rgba(15,23,42,0.9)",
    marginLeft: 6,
  },
  eyeTxt: {
    fontSize: 10,
    color: "#e5e7eb",
    letterSpacing: 1.5,
  },
  termsRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 4,
    marginBottom: 8,
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
  termsTextBlock: {
    marginLeft: 10,
    flex: 1,
  },
  termsLabel: {
    fontSize: 10,
    letterSpacing: 2,
    color: "#e5e7eb",
  },
  termsSub: {
    fontSize: 10,
    color: "#64748b",
    marginTop: 2,
  },
  chipRow: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    marginTop: 2,
    marginBottom: 10,
  },
  tinyChip: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    backgroundColor: "rgba(15,23,42,0.9)",
    maxWidth: "48%",
  },
  tinyChipText: {
    fontSize: 9,
    color: "#a5b4fc",
    letterSpacing: 1.5,
  },
  primaryBtn: {
    borderRadius: 999,
    paddingVertical: 11,
    alignItems: "center",
    backgroundColor: "#22d3ee",
    shadowColor: "#22d3ee",
    shadowOffset: { width: 0, height: 0 },
    shadowOpacity: 1,
    shadowRadius: 22,
    marginBottom: 8,
  },
  primaryBtnText: {
    color: "#020617",
    fontSize: 13,
    fontWeight: "800",
    letterSpacing: 3,
  },
  altGhostBtn: {
    borderRadius: 999,
    paddingVertical: 9,
    alignItems: "center",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
    backgroundColor: "rgba(2,6,23,0.95)",
    marginBottom: 8,
  },
  altGhostBtnText: {
    color: "#93c5fd",
    fontSize: 11,
    letterSpacing: 2,
    textTransform: "uppercase",
  },
  tertiaryLink: {
    alignItems: "center",
    marginTop: 4,
  },
  tertiaryLinkText: {
    color: "#93c5fd",
    fontSize: 11,
    letterSpacing: 1.5,
    textTransform: "uppercase",
  },
});
