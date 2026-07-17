// src/member/fun/games/GameCreate.tsx
import { createGame, GameGenre } from "@/src/api/games/createGame";
import LeoAssistantBubble from "@/src/member/ai/LeoAssistantBubble";
import LeoAssistantPanel from "@/src/member/ai/LeoAssistantPanel";
import { useTheme } from "@/src/themes/ThemeContext";
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

type Props = {
  onClose: () => void;
};

const GENRES: GameGenre[] = ["Action", "Casual", "RPG", "Simulation"];

export default function GameCreate({ onClose }: Props) {
  const { colors } = useTheme();

  const [name, setName] = useState("");
  const [genre, setGenre] = useState<GameGenre>("Action");
  const [tagline, setTagline] = useState("");
  const [description, setDescription] = useState("");
  const [difficulty, setDifficulty] = useState("");
  const [maxPlayers, setMaxPlayers] = useState("");
  const [tier, setTier] = useState("");            // NEW
  const [charge, setCharge] = useState("");        // NEW: amount user wants to charge

  const [saving, setSaving] = useState(false);

  // --- AI assistant shared with BotsScreen ---
  const [leoOpen, setLeoOpen] = useState(false);

  const gameJson = useMemo(
    () =>
      JSON.stringify(
        {
          name,
          genre,
          tagline,
          description,
          difficulty,
          maxPlayers,
          tier,
          charge,
        },
        null,
        2
      ),
    [name, genre, tagline, description, difficulty, maxPlayers, tier, charge]
  );

  function applyGameJson(json: string) {
    try {
      const parsed = JSON.parse(json);
      if (!parsed || typeof parsed !== "object") return;

      setName(parsed.name ?? "");
      setGenre(
        GENRES.includes(parsed.genre) ? parsed.genre : ("Action" as GameGenre)
      );
      setTagline(parsed.tagline ?? "");
      setDescription(parsed.description ?? "");
      setDifficulty(parsed.difficulty ?? "");
      setMaxPlayers(
        parsed.maxPlayers !== undefined ? String(parsed.maxPlayers) : ""
      );
      setTier(parsed.tier ?? "");
      setCharge(
        parsed.charge !== undefined ? String(parsed.charge) : ""
      );
    } catch {
      // ignore invalid JSON
    }
  }

  // draggable Leo
  const leoPos = useRef(new Animated.ValueXY({ x: 0, y: 0 })).current;
  const isDraggingLeo = useRef(false);

  const leoPanResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => false,
      onMoveShouldSetPanResponder: (_e, gesture) => {
        const moved = Math.abs(gesture.dx) > 5 || Math.abs(gesture.dy) > 5;
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

  async function handleSave() {
    if (!name.trim()) {
      Alert.alert("Missing name", "Give your game a name first.");
      return;
    }

    const max = maxPlayers.trim() ? Number(maxPlayers.trim()) : 4;
    if (Number.isNaN(max) || max <= 0) {
      Alert.alert("Invalid players", "Max players must be a positive number.");
      return;
    }

    const chargeNumber = charge.trim() ? Number(charge.trim()) : 0;
    if (charge.trim() && (Number.isNaN(chargeNumber) || chargeNumber < 0)) {
      Alert.alert("Invalid charge", "Charge must be a non‑negative number.");
      return;
    }

    try {
      setSaving(true);
      await createGame({
        name: name.trim(),
        genre,
        createdBy: "user",
        players: 0,
        likes: 0,
        // you can extend GameDoc later with tier/charge fields
      } as any);
      Alert.alert(
        "Created",
        "Your game shell has been created. Remember: Cedal receives 50% of what you make from this game."
      );
      onClose();
    } catch (e) {
      Alert.alert("Error", "Could not create your game.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      {/* top bar */}
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
          onPress={onClose}
          activeOpacity={0.7}
          style={styles.backBtn}
          disabled={saving}
        >
          <Text style={[styles.backText, { color: colors.textPrimary }]}>
            {saving ? "Saving…" : "Cancel"}
          </Text>
        </TouchableOpacity>
        <Text style={[styles.topTitle, { color: colors.textPrimary }]}>
          Create game
        </Text>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        {/* intro with revenue line */}
        <View
          style={[
            styles.card,
            { borderColor: colors.border, backgroundColor: colors.background },
          ]}
        >
          <Text style={[styles.cardTitle, { color: colors.textPrimary }]}>
            Shape a new world
          </Text>
          <Text style={[styles.cardBody, { color: colors.textSecondary }]}>
            When you publish a game through Cedal, we receive 50% of however
            much you make from it. The rest is yours to keep.
          </Text>
        </View>

        {/* form */}
        <View
          style={[
            styles.card,
            { borderColor: colors.border, backgroundColor: colors.background },
          ]}
        >
          <Field
            label="Name"
            required
            placeholder="Neon Arena, Void Drift, etc."
            value={name}
            onChangeText={setName}
          />

          {/* genre picker */}
          <View style={styles.field}>
            <View style={styles.fieldLabelRow}>
              <Text style={styles.fieldLabel}>Genre</Text>
              <Text style={styles.fieldTag}>Required</Text>
            </View>
            <View style={styles.genreRow}>
              {GENRES.map((g) => {
                const active = genre === g;
                return (
                  <TouchableOpacity
                    key={g}
                    onPress={() => setGenre(g)}
                    activeOpacity={0.8}
                    style={[
                      styles.genreChip,
                      active && styles.genreChipActive,
                    ]}
                  >
                    <Text
                      style={[
                        styles.genreText,
                        active && styles.genreTextActive,
                      ]}
                    >
                      {g}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </View>
          </View>

          <Field
            label="Tagline"
            optional
            placeholder="One‑line pitch for the lobby."
            value={tagline}
            onChangeText={setTagline}
          />

          <Field
            label="World description"
            required
            placeholder="Describe the arena, rules, and what players actually do."
            value={description}
            onChangeText={setDescription}
            multiline
          />

          <Field
            label="Difficulty"
            optional
            placeholder="Chill, ranked, punishing, etc."
            value={difficulty}
            onChangeText={setDifficulty}
            multiline
          />

          <Field
            label="Max players"
            optional
            placeholder="4"
            value={maxPlayers}
            onChangeText={setMaxPlayers}
          />

          {/* NEW: tier */}
          <Field
            label="Tier"
            optional
            placeholder="Bronze, premium, supporter pack…"
            value={tier}
            onChangeText={setTier}
          />

          {/* NEW: charge amount */}
          <Field
            label="Charge per player"
            optional
            placeholder="Amount users pay to join (any amount)."
            value={charge}
            onChangeText={setCharge}
          />
        </View>

        {/* save */}
        <TouchableOpacity
          style={styles.saveBtn}
          activeOpacity={0.85}
          onPress={handleSave}
          disabled={saving}
        >
          <Text style={styles.saveText}>
            {saving ? "Saving…" : "Save game shell"}
          </Text>
        </TouchableOpacity>
      </ScrollView>

      {/* Draggable Leo, same AI as BotsScreen */}
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
            currentBotJson={gameJson}
            onApplyBotJson={applyGameJson}
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
};

function Field({
  label,
  required,
  optional,
  placeholder,
  value,
  onChangeText,
  multiline,
}: FieldProps) {
  return (
    <View style={styles.field}>
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
  root: { flex: 1 },
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
  genreRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginTop: 4,
  },
  genreChip: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(55,65,81,0.9)",
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  genreChipActive: {
    borderColor: "rgba(56,189,248,0.9)",
    backgroundColor: "#0b1120",
  },
  genreText: {
    fontSize: 11,
    color: "#9ca3af",
    letterSpacing: 0.6,
    textTransform: "uppercase",
  },
  genreTextActive: {
    color: "#e5e7eb",
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
