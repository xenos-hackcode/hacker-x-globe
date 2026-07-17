// app/(auth)/(member)/question-panel.tsx
import React, { useEffect, useState } from "react";
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
import { doc, getDoc } from "firebase/firestore";

type QuestionKey = "favoriteColor" | "age";

export default function QuestionPanelScreen() {
  const router = useRouter();
  const [activeQuestion, setActiveQuestion] =
    useState<QuestionKey>("favoriteColor");
  const [answer, setAnswer] = useState("");
  const [busy, setBusy] = useState(false);

  const [storedFavoriteColor, setStoredFavoriteColor] = useState<string | null>(
    null
  );
  const [storedAge, setStoredAge] = useState<number | null>(null);

  // Load profile answers from Firestore
  useEffect(() => {
    (async () => {
      const user = auth.currentUser;
      if (!user) {
        Alert.alert("Session", "Sign in first.");
        router.replace("/(auth)/sign-in");
        return;
      }

      try {
        const ref = doc(db, "users", user.uid);
        const snap = await getDoc(ref);
        if (!snap.exists()) {
          Alert.alert("Profile", "No profile found for this node.");
          return;
        }
        const data = snap.data() as any;
        setStoredFavoriteColor(data.favoriteColor ?? null);
        setStoredAge(
          typeof data.age === "number" ? data.age : Number(data.age) || null
        );
      } catch (e: any) {
        Alert.alert(
          "Error",
          e?.message ?? "Failed to load recovery questions."
        );
      }
    })();
  }, [router]);

  function switchQuestion(next: QuestionKey) {
    if (next === activeQuestion) return;
    setActiveQuestion(next);
    setAnswer("");
  }

  function getLabel() {
    if (activeQuestion === "favoriteColor")
      return "WHAT IS YOUR FAVORITE COLOR";
    return "WHAT IS YOUR AGE";
  }

  function getPlaceholder() {
    if (activeQuestion === "favoriteColor") return "e.g. electric blue";
    return "e.g. 24";
  }

  function normalize(value: string) {
    return value.trim().toLowerCase();
  }

  async function handleConfirm() {
    if (!answer.trim()) {
      Alert.alert("Answer", "Please provide an answer before continuing.");
      return;
    }

    setBusy(true);
    try {
      let isMatch = false;

      if (activeQuestion === "favoriteColor") {
        if (!storedFavoriteColor) {
          Alert.alert(
            "No record",
            "Favorite color is not set for this node."
          );
          return;
        }
        isMatch =
          normalize(answer) === normalize(String(storedFavoriteColor || ""));
      } else {
        if (storedAge == null) {
          Alert.alert("No record", "Age is not set for this node.");
          return;
        }
        const numAnswer = Number(answer.trim());
        if (Number.isNaN(numAnswer)) {
          Alert.alert("Answer", "Age must be a number.");
          return;
        }
        isMatch = numAnswer === storedAge;
      }

      if (!isMatch) {
        Alert.alert("Mismatch", "Your answer does not match our records.");
        return;
      }

      // Correct answer → ask whether to update passcode
      Alert.alert(
        "Identity confirmed",
        "Your answer matches. Do you want to update your Cedal passcode now?",
        [
          {
            text: "No",
            onPress: () => {
              router.replace("/home");
            },
            style: "cancel",
          },
          {
            text: "Yes",
            onPress: () => {
              // TODO: replace with your real update screen route
              router.replace("/(auth)/(member)/update-passcode-simple");
            },
          },
        ]
      );
    } catch (e: any) {
      Alert.alert("Error", e?.message ?? "Failed to verify answer.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <View style={styles.root}>
      <View style={styles.card}>
        <Text style={styles.title}>ANSWER ONE QUESTION</Text>

        {/* question toggle panel */}
        <View style={styles.modeRow}>
          <TouchableOpacity
            onPress={() => switchQuestion("favoriteColor")}
            style={[
              styles.modeBtn,
              activeQuestion === "favoriteColor" && styles.modeBtnActive,
            ]}
          >
            <Text
              style={[
                styles.modeText,
                activeQuestion === "favoriteColor" && styles.modeTextActive,
              ]}
            >
              FAVORITE COLOR
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            onPress={() => switchQuestion("age")}
            style={[
              styles.modeBtn,
              activeQuestion === "age" && styles.modeBtnActive,
            ]}
          >
            <Text
              style={[
                styles.modeText,
                activeQuestion === "age" && styles.modeTextActive,
              ]}
            >
              AGE
            </Text>
          </TouchableOpacity>
        </View>

        {/* Single dynamic field */}
        <Text style={styles.label}>{getLabel()}</Text>
        <TextInput
          value={answer}
          onChangeText={setAnswer}
          placeholder={getPlaceholder()}
          placeholderTextColor="#64748b"
          keyboardType={activeQuestion === "age" ? "number-pad" : "default"}
          style={styles.input}
        />

        <TouchableOpacity
          onPress={handleConfirm}
          disabled={busy}
          style={[styles.btn, busy && { opacity: 0.5 }]}
        >
          <Text style={styles.btnText}>
            {busy ? "SAVING…" : "CONFIRM ANSWER"}
          </Text>
        </TouchableOpacity>

        <TouchableOpacity onPress={() => router.back()} style={styles.backBtn}>
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
    marginBottom: 18,
  },
  modeRow: {
    flexDirection: "row",
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.9)",
    overflow: "hidden",
    marginBottom: 16,
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
  input: {
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: "rgba(148,163,184,0.9)",
    backgroundColor: "#020617",
    color: "#e5e7eb",
    paddingHorizontal: 12,
    paddingVertical: 8,
    fontSize: 14,
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
