// app/(auth)/(member)/fun.tsx
import FunPicker, { FunMode } from "@/src/member/fun/FunPicker";
import GameDescript from "@/src/member/fun/games/GameDescript";
import GameHub from "@/src/member/fun/games/GameHubNative";
import { useTheme } from "@/src/themes/ThemeContext";
import React, { useState } from "react";
import { ScrollView, StyleSheet, View } from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";

export default function FunScreen() {
  const { colors } = useTheme();
  const insets = useSafeAreaInsets();
  const [mode, setMode] = useState<FunMode | null>(null);
  const [selectedGame, setSelectedGame] = useState<any | null>(null);
  const [creating, setCreating] = useState(false);

  return (
    <View
      style={[
        styles.screen,
        { backgroundColor: colors.background, paddingBottom: insets.bottom },
      ]}
    >
      {/* main fun picker scroll */}
      <ScrollView contentContainerStyle={styles.content}>
        <FunPicker selectedMode={mode} onSelectMode={setMode} />
      </ScrollView>

      {/* full-screen games overlay (has its own scroll inside GameHub) */}
      {mode === "games" && (
        <View style={styles.overlay}>
          <GameHub
            onClose={() => setMode(null)}
            onSelectGame={(g) => setSelectedGame(g)}
            onCreateGame={() => setCreating(true)}
          />

          {selectedGame && (
            <View style={styles.detailOverlay}>
              <GameDescript
                game={selectedGame}
                onClose={() => setSelectedGame(null)}
              />
            </View>
          )}

          {/* later: mount your CreateGame overlay here when creating === true */}
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1 },
  content: { padding: 16, paddingBottom: 80 },
  overlay: {
    position: "absolute",
    inset: 0,
    padding: 16,
    backgroundColor: "rgba(2,6,23,0.96)",
    justifyContent: "center",
  },
  detailOverlay: {
    position: "absolute",
    inset: 0,
    backgroundColor: "rgba(15,23,42,0.92)",
    justifyContent: "center",
    padding: 16,
  },
});
