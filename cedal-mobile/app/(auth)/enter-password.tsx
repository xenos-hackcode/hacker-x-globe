// app/(auth)/enter-password.tsx
import React, { useState, useEffect } from "react";
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  Alert,
} from "react-native";
import { useRouter } from "expo-router";
import { auth, db } from "@/src/api/firebase";
import { doc, getDoc, updateDoc } from "firebase/firestore";
import * as SecureStore from "expo-secure-store";

type Mode = "dev" | "user";

// ---- lockout helpers (local, per-uid) ----
const FAIL_KEY = (uid: string) => `cedal_fail_count_${uid}`;
const LOCK_KEY = (uid: string) => `cedal_lock_until_${uid}`;
const LOCKDOWN_MODE = true; // or read from remote config later

type LockoutState = {
  locked: boolean;
  failCount: number;
  lockUntil: number | null; // ms since epoch
};

async function getLockoutState(uid: string): Promise<LockoutState> {
  const failRaw = await SecureStore.getItemAsync(FAIL_KEY(uid));
  const lockRaw = await SecureStore.getItemAsync(LOCK_KEY(uid));

  const failCount = failRaw ? Number(failRaw) || 0 : 0;
  const lockUntil = lockRaw ? Number(lockRaw) || 0 : 0;
  const now = Date.now();

  return {
    locked: lockUntil > 0 && now < lockUntil,
    failCount,
    lockUntil: lockUntil > 0 ? lockUntil : null,
  };
}

async function registerSuccess(uid: string) {
  await SecureStore.deleteItemAsync(FAIL_KEY(uid));
  await SecureStore.deleteItemAsync(LOCK_KEY(uid));
}

async function registerFailure(uid: string): Promise<LockoutState> {
  const state = await getLockoutState(uid);
  const nextFail = state.failCount + 1;

  let lockUntil: number | null = null;

  if (nextFail === 2) {
    // 2nd wrong → 15 minutes
    lockUntil = Date.now() + 15 * 60 * 1000;
  } else if (nextFail === 3) {
    // 3rd wrong → 30 minutes
    lockUntil = Date.now() + 30 * 60 * 1000;
  } else if (nextFail >= 4) {
    // 4th+ wrong → 2 hours
    lockUntil = Date.now() + 2 * 60 * 60 * 1000;
  }

  await SecureStore.setItemAsync(FAIL_KEY(uid), String(nextFail));

  if (lockUntil) {
    await SecureStore.setItemAsync(LOCK_KEY(uid), String(lockUntil));
    return { locked: true, failCount: nextFail, lockUntil };
  }

  // still not locked (only first wrong attempt)
  return {
    locked: false,
    failCount: nextFail,
    lockUntil: state.lockUntil,
  };
}

// ---- screen ----
export default function EnterPasswordScreen() {
  const router = useRouter();
  const [devKey, setDevKey] = useState("");
  const [userKeyInput, setUserKeyInput] = useState("");
  const [busy, setBusy] = useState(false);
  const [profileUserKey, setProfileUserKey] = useState<string | null>(null);
  const [activeMode, setActiveMode] = useState<Mode>("user");

  const [showDevKey, setShowDevKey] = useState(false);
  const [showUserKey, setShowUserKey] = useState(false);

  const [lockRemainingMs, setLockRemainingMs] = useState<number | null>(null);
  const [lockChecked, setLockChecked] = useState(false);
  const [failCount, setFailCount] = useState(0);

  // hidden secret tap counter
  const [secretTapCount, setSecretTapCount] = useState(0);

  // load user profile + initial lock state
  useEffect(() => {
    (async () => {
      const user = auth.currentUser;
      if (!user) {
        Alert.alert("Session", "Sign in first.");
        router.replace("/(auth)/sign-in");
        return;
      }

      const ref = doc(db, "users", user.uid);
      const snap = await getDoc(ref);
      const data = snap.exists() ? (snap.data() as any) : null;
      setProfileUserKey(data?.userKey ?? null);

      const lockState = await getLockoutState(user.uid);
      if (lockState.locked && lockState.lockUntil) {
        setLockRemainingMs(lockState.lockUntil - Date.now());
      } else {
        setLockRemainingMs(null);
      }
      setFailCount(lockState.failCount);
      setLockChecked(true);
    })();
  }, [router]);

  // countdown updater
  useEffect(() => {
    if (lockRemainingMs == null) return;

    const interval = setInterval(() => {
      setLockRemainingMs((prev) => {
        if (prev == null) return null;
        const next = prev - 1000;
        return next > 0 ? next : 0;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [lockRemainingMs]);

  // if countdown hits 0, clear lock state in storage
  useEffect(() => {
    (async () => {
      if (lockRemainingMs === 0) {
        const user = auth.currentUser;
        if (!user) return;
        await SecureStore.deleteItemAsync(LOCK_KEY(user.uid));
        await SecureStore.deleteItemAsync(FAIL_KEY(user.uid));
        setLockRemainingMs(null);
        setFailCount(0);
      }
    })();
  }, [lockRemainingMs]);

  function switchMode(next: Mode) {
    if (next === activeMode) return;
    setActiveMode(next);
    if (next === "dev") {
      setUserKeyInput("");
    } else {
      setDevKey("");
    }
  }

  function formatRemaining(ms: number): string {
    const totalSeconds = Math.max(0, Math.floor(ms / 1000));
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    if (minutes <= 0) {
      return `${seconds}s`;
    }
    return `${minutes}m ${seconds}s`;
  }

  function attemptsMessage(count: number): string | null {
    const remaining = 3 - count;
    if (remaining <= 0) return null;
    if (remaining === 2) {
      return "2 attempts remaining before lock.";
    }
    if (remaining === 1) {
      return "last attempt before a 2 hour lock.";
    }
    return null;
  }

  async function handleConfirm() {
    const user = auth.currentUser;
    if (!user) {
      Alert.alert("Session", "Sign in first.");
      return;
    }

    // secret: 5 taps with no input opens creator area
    if (!devKey.trim() && !userKeyInput.trim()) {
      setSecretTapCount((prev) => {
        const next = prev + 1;
        if (next >= 5) {
          router.push("/(auth)/(owner)/creator-passcode");
          return 0;
        }
        return next;
      });
      // don't run normal password logic on empty input
      return;
    }

    const isLocked = lockRemainingMs != null && lockRemainingMs > 0;
    if (isLocked) {
      Alert.alert(
        "Locked",
        `Too many wrong attempts. Try again in about ${formatRemaining(
          lockRemainingMs!
        )} or use recovery.`
      );
      return;
    }

    setBusy(true);
    try {
      const ref = doc(db, "users", user.uid);
      const snap = await getDoc(ref);
      const data = snap.exists() ? (snap.data() as any) : null;

      if (!data) {
        Alert.alert("Profile", "No profile found for this node.");
        return;
      }

      const currentRole = data.role ?? "user";

      if (activeMode === "dev") {
        // DEV: must always type dev key, no biometric
        if (!devKey.trim()) {
          Alert.alert("Developer key", "Enter the developer password.");
          return;
        }

        const storedDevKey = data.devKey ?? null;

        if (!storedDevKey) {
          Alert.alert(
            "Developer key",
            "No developer key is registered for this node."
          );
          return;
        }

        if (devKey.trim() === storedDevKey) {
          // non-owner become / stay developer
          let nextRole = currentRole;
          if (currentRole !== "owner") {
            nextRole = "developer";
          }

          await updateDoc(ref, { role: nextRole });

          Alert.alert(
            "Access granted",
            "Developer mode unlocked for this node."
          );
          router.replace("/(auth)/(developer)/loading-developer" as any);
        } else {
          Alert.alert("Developer key", "Invalid developer key.");
        }
      } else {
        // USER mode: node password with lockout
        if (!userKeyInput.trim()) {
          Alert.alert("User key", "Enter your user password.");
          return;
        }

        const storedKey = data.userKey ?? null;
        const uid = user.uid;

        if (!storedKey) {
          Alert.alert("User key", "No user key is registered for this node.");
        } else if (userKeyInput === storedKey) {
          // successful user key → become / stay user (unless owner)
          let nextRole = currentRole;
          if (currentRole !== "owner") {
            nextRole = "user";
          }

          await updateDoc(ref, { role: nextRole });

          await registerSuccess(uid);
          setLockRemainingMs(null);
          setFailCount(0);
          Alert.alert("User key", "User key is valid.");
          router.replace("/(auth)/(member)/loading-sign-in" as any);
        } else {
          const failState = await registerFailure(uid);
          setFailCount(failState.failCount);

          if (failState.locked && failState.lockUntil) {
            const remaining = failState.lockUntil - Date.now();
            setLockRemainingMs(remaining);
            Alert.alert(
              "Locked",
              "Too many wrong attempts. This node is locked for 2 hours. Use recovery if this was you."
            );
          } else {
            const msg = attemptsMessage(failState.failCount);
            Alert.alert(
              "User key",
              msg
                ? `User key does not match. ${msg}`
                : "User key does not match."
            );
          }
        }
      }
    } catch (e: any) {
      Alert.alert("Error", e?.message ?? "Failed to verify password.");
    } finally {
      setBusy(false);
    }
  }

  function handleForgotUserKey() {
  if (LOCKDOWN_MODE) {
    Alert.alert(
      "Recovery disabled",
      "Password recovery is temporarily disabled while lockdown mode is active.",
    );
    return;
  }

  if (lockRemainingMs != null && lockRemainingMs > 0) {
    // optional: also block while this node is locally locked
    Alert.alert(
      "Locked",
      "This node is locked due to too many attempts. Try again later.",
    );
    return;
  }

  router.push("/(auth)/(member)/question-panel");
}

  const isLocked = lockRemainingMs != null && lockRemainingMs > 0;
  const attemptsHint = attemptsMessage(failCount);

  return (
    <View style={styles.root}>
      <View style={styles.card}>
        <Text style={styles.title}>ENTER NODE PASSWORD</Text>

        {lockChecked && isLocked && (
          <Text style={styles.lockWarning}>
            too many wrong attempts. node locked for{" "}
            {formatRemaining(lockRemainingMs!)}.
          </Text>
        )}

        {!isLocked && attemptsHint && (
          <Text style={styles.attemptsWarning}>{attemptsHint}</Text>
        )}

        {/* mode toggle panel */}
        <View style={styles.modeRow}>
          <TouchableOpacity
            onPress={() => switchMode("user")}
            style={[
              styles.modeBtn,
              activeMode === "user" && styles.modeBtnActive,
            ]}
          >
            <Text
              style={[
                styles.modeText,
                activeMode === "user" && styles.modeTextActive,
              ]}
            >
              USER
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            onPress={() => switchMode("dev")}
            style={[
              styles.modeBtn,
              activeMode === "dev" && styles.modeBtnActive,
            ]}
          >
            <Text
              style={[
                styles.modeText,
                activeMode === "dev" && styles.modeTextActive,
              ]}
            >
              DEVELOPER
            </Text>
          </TouchableOpacity>
        </View>

        {/* Developer password */}
        <Text style={styles.label}>DEVELOPER PASSWORD</Text>
        <View
          style={[
            styles.inputRow,
            activeMode !== "dev" && { opacity: 0.4 },
          ]}
        >
          <TextInput
            value={devKey}
            onChangeText={setDevKey}
            placeholder="••••••••"
            placeholderTextColor="#64748b"
            secureTextEntry={!showDevKey}
            editable={activeMode === "dev" && !isLocked}
            style={[styles.input, { flex: 1 }]}
          />
          <TouchableOpacity
            onPress={() => setShowDevKey((v) => !v)}
            disabled={activeMode !== "dev"}
            style={styles.toggleBtn}
          >
            <Text style={styles.toggleText}>
              {showDevKey ? "HIDE" : "SHOW"}
            </Text>
          </TouchableOpacity>
        </View>

        {/* User password / key */}
        <Text style={styles.label}>USER PASSWORD</Text>
        <View
          style={[
            styles.inputRow,
            activeMode !== "user" && { opacity: 0.4 },
          ]}
        >
          <TextInput
            value={userKeyInput}
            onChangeText={setUserKeyInput}
            placeholder={
              profileUserKey ? "Enter your node key" : "Enter your node key"
            }
            placeholderTextColor="#64748b"
            secureTextEntry={!showUserKey}
            editable={activeMode === "user" && !isLocked}
            style={[styles.input, { flex: 1 }]}
          />
          <TouchableOpacity
            onPress={() => setShowUserKey((v) => !v)}
            disabled={activeMode !== "user"}
            style={styles.toggleBtn}
          >
            <Text style={styles.toggleText}>
              {showUserKey ? "HIDE" : "SHOW"}
            </Text>
          </TouchableOpacity>
        </View>

        <TouchableOpacity
          onPress={handleForgotUserKey}
          style={styles.forgotBtn}
        >
          <Text style={styles.forgotText}>forgotten password</Text>
        </TouchableOpacity>

        <TouchableOpacity
          onPress={handleConfirm}
          disabled={busy || isLocked}
          style={[
            styles.btn,
            (busy || isLocked) && { opacity: 0.5 },
          ]}
        >
          <Text style={styles.btnText}>
            {busy ? "CHECKING…" : isLocked ? "LOCKED" : "CONFIRM PASSWORD"}
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          onPress={() => router.replace("/(auth)/sign-in")}
          style={styles.backBtn}
        >
          <Text style={styles.backText}>back</Text>
        </TouchableOpacity>
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
  card: {
    width: "100%",
    maxWidth: 420,
    borderRadius: 24,
    borderWidth: 1.5,
    borderColor: "rgba(56,189,248,0.9)",
    backgroundColor: "rgba(15,23,42,0.97)",
    padding: 20,
  },
  title: {
    fontSize: 16,
    color: "#e5e7eb",
    fontWeight: "700",
    letterSpacing: 3,
    textAlign: "center",
    marginBottom: 8,
  },
  lockWarning: {
    fontSize: 11,
    color: "#f97316",
    textAlign: "center",
    marginBottom: 8,
    textTransform: "uppercase",
    letterSpacing: 1.5,
  },
  attemptsWarning: {
    fontSize: 11,
    color: "#fbbf24",
    textAlign: "center",
    marginBottom: 8,
    textTransform: "uppercase",
    letterSpacing: 1.5,
  },
  modeRow: {
    flexDirection: "row",
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.9)",
    overflow: "hidden",
    marginBottom: 16,
    marginTop: 8,
  },
  modeBtn: {
    flex: 1,
    paddingVertical: 8,
    alignItems: "center",
    backgroundColor: "transparent",
  },
  modeBtnActive: {
    backgroundColor: "#22d3ee",
  },
  modeText: {
    fontSize: 11,
    letterSpacing: 2,
    color: "#9ca3af",
  },
  modeTextActive: {
    color: "#020617",
    fontWeight: "700",
  },
  label: {
    fontSize: 11,
    color: "#9ca3af",
    letterSpacing: 2,
    marginBottom: 4,
    marginTop: 8,
  },
  inputRow: {
    flexDirection: "row",
    alignItems: "center",
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: "rgba(148,163,184,0.9)",
    backgroundColor: "#020617",
    paddingHorizontal: 8,
  },
  input: {
    color: "#e5e7eb",
    paddingVertical: 8,
    fontSize: 14,
  },
  toggleBtn: {
    paddingHorizontal: 6,
    paddingVertical: 4,
  },
  toggleText: {
    fontSize: 10,
    color: "#e5e7eb",
    letterSpacing: 1.5,
  },
  forgotBtn: {
    alignItems: "flex-end",
    marginTop: 6,
  },
  forgotText: {
    fontSize: 11,
    color: "#93c5fd",
    letterSpacing: 1.5,
    textTransform: "uppercase",
  },
  btn: {
    borderRadius: 999,
    paddingVertical: 11,
    alignItems: "center",
    backgroundColor: "#22d3ee",
    marginTop: 18,
  },
  btnText: {
    color: "#020617",
    fontSize: 13,
    fontWeight: "800",
    letterSpacing: 2,
  },
  backBtn: {
    alignItems: "center",
    marginTop: 10,
  },
  backText: {
    fontSize: 11,
    color: "#64748b",
    letterSpacing: 1.5,
    textTransform: "uppercase",
  },
});
