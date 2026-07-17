// app/(auth)/(member)/bots.tsx
import LeoAssistantBubble from "@/src/member/ai/LeoAssistantBubble";
import LeoAssistantPanel from "@/src/member/ai/LeoAssistantPanel";
import { useTheme } from "@/src/themes/ThemeContext";
import { useRouter } from "expo-router";
import React, { useMemo, useRef, useState } from "react";
import {
  Alert,
  Animated,
  PanResponder,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";

export default function BotsScreen() {
  const router = useRouter();
  const { colors } = useTheme();

  const [name, setName] = useState("");
  const [age, setAge] = useState("");
  const [gender, setGender] = useState("");
  const [character, setCharacter] = useState("");
  const [personality, setPersonality] = useState("");
  const [bio, setBio] = useState("");
  const [occupation, setOccupation] = useState("");
  const [lifeStory, setLifeStory] = useState("");
  const [description, setDescription] = useState("");

  const [leoOpen, setLeoOpen] = useState(false);

  const botJson = useMemo(
    () =>
      JSON.stringify(
        {
          name,
          age,
          gender,
          character,
          personality,
          bio,
          occupation,
          lifeStory,
          description,
        },
        null,
        2
      ),
    [
      name,
      age,
      gender,
      character,
      personality,
      bio,
      occupation,
      lifeStory,
      description,
    ]
  );

  function applyBotJson(json: string) {
    try {
      const parsed = JSON.parse(json);
      if (!parsed || typeof parsed !== "object") return;

      setName(parsed.name ?? "");
      setAge(parsed.age ?? "");
      setGender(parsed.gender ?? "");
      setCharacter(parsed.character ?? "");
      setPersonality(parsed.personality ?? "");
      setBio(parsed.bio ?? "");
      setOccupation(parsed.occupation ?? "");
      setLifeStory(parsed.lifeStory ?? "");
      setDescription(parsed.description ?? "");
    } catch {
      // ignore invalid JSON
    }
  }

  function handleBack() {
    router.back();
  }

  function handleSave() {
    if (!name.trim()) {
      Alert.alert("Missing name", "Give your AI a name first.");
      return;
    }

    const payload = {
      name: name.trim(),
      age: age.trim(),
      gender: gender.trim(),
      character: character.trim(),
      personality: personality.trim(),
      bio: bio.trim(),
      occupation: occupation.trim(),
      lifeStory: lifeStory.trim(),
      description: description.trim(),
    };

    console.log("BOT_FORM", payload);
    Alert.alert("Saved", "Your AI template has been saved (stub).");
  }

  // shared drag for bubble + panel
  const leoPos = useRef(new Animated.ValueXY({ x: 0, y: 0 })).current;
  const isDraggingLeo = useRef(false);

  const leoPanResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => false,
      onMoveShouldSetPanResponder: (_e, gesture) => {
        const moved =
          Math.abs(gesture.dx) > 5 || Math.abs(gesture.dy) > 5;
        if (moved) {
          isDraggingLeo.current = true;
        }
        return moved;
      },
      onPanResponderMove: Animated.event(
        [null, { dx: leoPos.x, dy: leoPos.y }],
        { useNativeDriver: false }
      ),
      onPanResponderRelease: () => {
        leoPos.extractOffset();
        setTimeout(() => {
          isDraggingLeo.current = false;
        }, 0);
      },
      onPanResponderTerminate: () => {
        leoPos.extractOffset();
        isDraggingLeo.current = false;
      },
    })
  ).current;

  function handleLeoPress() {
    if (!isDraggingLeo.current) {
      setLeoOpen((prev) => !prev);
    }
  }

  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      {/* Top bar */}
      <View
        style={[
          styles.topBar,
          {
            borderBottomColor: colors.border,
            backgroundColor:
              (colors as any).headerBackground ?? colors.background,
          },
        ]}
      >
        <TouchableOpacity
          onPress={handleBack}
          activeOpacity={0.7}
          style={styles.backBtn}
        >
          <Text style={[styles.backText, { color: colors.textPrimary }]}>
            Back
          </Text>
        </TouchableOpacity>
        <Text style={[styles.topTitle, { color: colors.textPrimary }]}>
          Bots
        </Text>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        {/* Intro */}
        <View
          style={[
            styles.card,
            { borderColor: colors.border, backgroundColor: colors.background },
          ]}
        >
          <Text style={[styles.cardTitle, { color: colors.textPrimary }]}>
            Build your own AI
          </Text>
          <Text style={[styles.cardBody, { color: colors.textSecondary }]}>
            Fill this out like you’re writing a character sheet. The more human
            the details, the easier it is for the AI to stay in‑role.
          </Text>
        </View>

        {/* Form card */}
        <View
          style={[
            styles.card,
            { borderColor: colors.border, backgroundColor: colors.background },
          ]}
        >
          <Field
            label="Name"
            required
            placeholder="Nova, Corneal, Ghost, etc."
            value={name}
            onChangeText={setName}
          />
          <Field
            label="Age"
            optional
            placeholder="22, timeless, ageless entity…"
            value={age}
            onChangeText={setAge}
          />
          <Field
            label="Gender"
            optional
            placeholder="Female, male, non‑binary, AI, none…"
            value={gender}
            onChangeText={setGender}
          />
          <Field
            label="Character"
            required
            placeholder="Role in the story: mentor, chaos hacker, soft therapist…"
            value={character}
            onChangeText={setCharacter}
            multiline
          />
          <Field
            label="Personality"
            required
            placeholder="How they talk, react, and vibe. Calm, chaotic, playful, ruthless…"
            value={personality}
            onChangeText={setPersonality}
            multiline
          />
          <Field
            label="Bio"
            required
            placeholder="Short bio you’d see on their profile."
            value={bio}
            onChangeText={setBio}
            multiline
          />
          <Field
            label="Occupation"
            optional
            placeholder="What they ‘do’ in the world: engineer, mercenary, archivist…"
            value={occupation}
            onChangeText={setOccupation}
            multiline
          />
          <Field
            label="Life story"
            optional
            placeholder="Key events, scars, victories, how they ended up here."
            value={lifeStory}
            onChangeText={setLifeStory}
            multiline
          />
          <Field
            label="Description"
            required
            placeholder="Visual + energy description like you’d brief an artist."
            value={description}
            onChangeText={setDescription}
            multiline
            last
          />
        </View>

        {/* Save button */}
        <TouchableOpacity
          style={styles.saveBtn}
          activeOpacity={0.85}
          onPress={handleSave}
        >
          <Text style={styles.saveText}>Save AI</Text>
        </TouchableOpacity>
      </ScrollView>

      {/* Draggable Leo: bubble + panel move together */}
      <Animated.View
        {...leoPanResponder.panHandlers}
        style={[
          styles.leoAnchor,
          { transform: leoPos.getTranslateTransform() },
        ]}
      >
        <LeoAssistantBubble onPress={handleLeoPress} />

        {leoOpen && (
          <LeoAssistantPanel
            onClose={() => setLeoOpen(false)}
            currentBotJson={botJson}
            onApplyBotJson={applyBotJson}
          />
        )}
      </Animated.View>
    </View>
  );
}

type FieldProps = {
  label: string;
  required?: boolean;
  optional?: boolean;
  placeholder?: string;
  value: string;
  onChangeText: (text: string) => void;
  multiline?: boolean;
  last?: boolean;
};

function Field({
  label,
  required,
  optional,
  placeholder,
  value,
  onChangeText,
  multiline,
  last,
}: FieldProps) {
  return (
    <View style={[styles.field, last && styles.fieldLast]}>
      <View style={styles.fieldLabelRow}>
        <Text style={styles.fieldLabel}>{label}</Text>
        {required && <Text style={styles.fieldTag}>Required</Text>}
        {optional && <Text style={styles.fieldTag}>Optional</Text>}
      </View>
      <TextInput
        value={value}
        onChangeText={onChangeText}
        placeholder={placeholder}
        placeholderTextColor="#6b7280"
        style={[styles.input, multiline && styles.inputMultiline]}
        multiline={multiline}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
  },
  topBar: {
    paddingTop: 48,
    paddingHorizontal: 16,
    paddingBottom: 12,
    flexDirection: "row",
    alignItems: "center",
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  backBtn: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
    marginRight: 12,
  },
  backText: {
    fontSize: 13,
    letterSpacing: 1.5,
  },
  topTitle: {
    fontSize: 16,
    fontWeight: "600",
    letterSpacing: 2,
    textTransform: "uppercase",
  },
  content: {
    paddingHorizontal: 16,
    paddingVertical: 16,
    paddingBottom: 32,
  },
  card: {
    borderRadius: 18,
    borderWidth: 1,
    marginBottom: 18,
    overflow: "hidden",
    paddingHorizontal: 14,
    paddingVertical: 12,
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: "600",
    marginBottom: 4,
  },
  cardBody: {
    fontSize: 12,
  },
  field: {
    paddingVertical: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(31,41,55,0.9)",
  },
  fieldLast: {
    borderBottomWidth: 0,
  },
  fieldLabelRow: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 4,
    gap: 6,
  },
  fieldLabel: {
    fontSize: 13,
    fontWeight: "500",
    color: "#e5e7eb",
  },
  fieldTag: {
    fontSize: 10,
    textTransform: "uppercase",
    letterSpacing: 1,
    color: "#9ca3af",
  },
  input: {
    fontSize: 12,
    color: "#e5e7eb",
    paddingHorizontal: 0,
    paddingVertical: 4,
  },
  inputMultiline: {
    minHeight: 60,
    textAlignVertical: "top",
  },
  saveBtn: {
    marginTop: 4,
    alignSelf: "flex-end",
    paddingHorizontal: 18,
    paddingVertical: 8,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#22c55e",
    backgroundColor: "#16a34a",
  },
  saveText: {
    fontSize: 12,
    letterSpacing: 1,
    textTransform: "uppercase",
    color: "#020617",
    fontWeight: "600",
  },
  leoAnchor: {
    position: "absolute",
    right: 20,
    bottom: 40,
    zIndex: 40,
  },
});
