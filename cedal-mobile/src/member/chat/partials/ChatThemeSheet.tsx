// src/member/chat/partials/ChatThemeSheet.tsx
import React from "react";
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  Image,
} from "react-native";
import * as ImagePicker from "expo-image-picker";
import { doc, setDoc } from "firebase/firestore";
import { db } from "@/src/api/firebase";
import { uploadChatImage } from "@/src/member/utils/uploadChatImage";
import { useChatScreenState } from "@/src/hooks/useChatScreenState";
import type { ChatTheme } from "@/src/types/ChatTheme";

type Props = {
  state: ReturnType<typeof useChatScreenState>;
};

// local Cedal theme assets (1.png - 17.png)
const LOCAL_THEMES = [
  { id: "1", file: require("@/src/themes/1.png") },
  { id: "2", file: require("@/src/themes/2.png") },
  { id: "3", file: require("@/src/themes/3.png") },
  { id: "4", file: require("@/src/themes/4.png") },
  { id: "5", file: require("@/src/themes/5.png") },
  { id: "6", file: require("@/src/themes/6.png") },
  { id: "7", file: require("@/src/themes/7.png") },
  { id: "8", file: require("@/src/themes/8.png") },
  { id: "9", file: require("@/src/themes/9.png") },
  { id: "10", file: require("@/src/themes/10.png") },
  { id: "11", file: require("@/src/themes/11.png") },
  { id: "12", file: require("@/src/themes/12.png") },
  { id: "13", file: require("@/src/themes/13.png") },
  { id: "14", file: require("@/src/themes/14.png") },
  { id: "15", file: require("@/src/themes/15.png") },
  { id: "16", file: require("@/src/themes/16.png") },
  { id: "17", file: require("@/src/themes/17.png") },
];

// map local assets → ChatTheme presets
const PRESET_THEMES: {
  presetId: string;
  image: any;
  theme: ChatTheme;
}[] = LOCAL_THEMES.map(({ id, file }) => ({
  presetId: id,
  image: file,
  theme: {
    kind: "preset",
    presetId: id,
    backgroundImageUrl: null, // local image, handled in ChatScreen
    backgroundColor: "#020617",
    textColor: "#e5e7eb",
    bubbleMeColor: "#0ea5e9",
    bubbleOtherColor: "#1e293b",
    tabAccentColor: "#38bdf8",
    tabLabelColor: "#e5e7eb",
    textFontFamily: null,
    textSize: null,
  },
}));

export function ChatThemeSheet({ state }: Props) {
  const {
    chatId,
    themeSheetOpen,
    setThemeSheetOpen,
    chatTheme,
    setChatTheme,
    vipLevel,
  } = state as any;

  if (!themeSheetOpen) return null;

  const level = vipLevel ?? 0;
  const canUsePresets = level >= 10;
  const canUsePhoto = level >= 20;

  const handleClose = () => setThemeSheetOpen(false);

  async function saveTheme(next: ChatTheme) {
    if (!chatId) return;
    if (!canUsePresets) return; // safety

    const roomRef = doc(db, "rooms", String(chatId));
    await setDoc(
      roomRef,
      {
        theme: next,
      },
      { merge: true },
    );
    setChatTheme(next);
  }

  const handleUsePhoto = async () => {
    if (!canUsePhoto) return;

    const res = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      quality: 0.9,
    });
    if (res.canceled || !res.assets?.[0]?.uri) return;

    const uri = res.assets[0].uri;
    const fileName = `theme_${Date.now()}.jpg`;
    const path = `chats/${chatId}/theme/${fileName}`;
    const downloadUrl = await uploadChatImage(uri, path);

    const next: ChatTheme = {
      ...(chatTheme ?? { kind: "none" }),
      kind: "userPhoto",
      backgroundImageUrl: downloadUrl,
    };
    await saveTheme(next);
    handleClose();
  };

  const handleSelectPreset = async (preset: ChatTheme) => {
    if (!canUsePresets) return;
    const next: ChatTheme = {
      ...chatTheme,
      ...preset,
    };
    await saveTheme(next);
    handleClose();
  };

  const handleReset = async () => {
    if (!canUsePresets) return;
    const next: ChatTheme = {
      kind: "none",
      backgroundImageUrl: null,
      backgroundColor: "#020617",
      textColor: null,
      textFontFamily: null,
      textSize: null,
      bubbleMeColor: null,
      bubbleOtherColor: null,
      tabAccentColor: null,
      tabLabelColor: null,
      presetId: null,
    };
    await saveTheme(next);
    handleClose();
  };

  return (
    <View style={styles.overlay}>
      <View style={styles.sheet}>
        <Text style={styles.title}>Chat theme</Text>

        {!canUsePresets && (
          <Text style={{ color: "#f97316", fontSize: 11, marginTop: 6 }}>
            Cedal themes unlock at VIP 10. Custom photo backgrounds unlock at
            VIP 20. Open the Shop to level up your VIP.
          </Text>
        )}

        <TouchableOpacity
          style={[
            styles.button,
            !canUsePhoto && { opacity: 0.4 },
          ]}
          onPress={handleUsePhoto}
          disabled={!canUsePhoto}
        >
          <Text style={styles.buttonText}>Choose from photos (VIP 20)</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[
            styles.button,
            !canUsePresets && { opacity: 0.4 },
          ]}
          onPress={handleReset}
          disabled={!canUsePresets}
        >
          <Text style={styles.buttonText}>Remove theme</Text>
        </TouchableOpacity>

        <Text style={[styles.subtitle, { marginTop: 14 }]}>Cedal themes</Text>

        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          style={{ marginTop: 8 }}
        >
          {PRESET_THEMES.map((preset) => (
            <TouchableOpacity
              key={preset.presetId}
              style={[
                styles.presetCard,
                chatTheme?.presetId === preset.presetId && styles.presetSelected,
                !canUsePresets && { opacity: 0.4 },
              ]}
              onPress={() => canUsePresets && handleSelectPreset(preset.theme)}
              disabled={!canUsePresets}
            >
              <Image source={preset.image} style={styles.presetImage} />
              <Text style={styles.presetLabel}>Theme {preset.presetId}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>

        <TouchableOpacity style={styles.cancel} onPress={handleClose}>
          <Text style={styles.cancelText}>Close</Text>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  overlay: {
    position: "absolute",
    left: 0,
    right: 0,
    bottom: 0,
    top: 0,
    backgroundColor: "rgba(0,0,0,0.6)",
    justifyContent: "flex-end",
  },
  sheet: {
    padding: 16,
    backgroundColor: "#020617",
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
  },
  title: { fontSize: 16, fontWeight: "600", color: "#e5e7eb" },
  button: {
    marginTop: 10,
    paddingVertical: 10,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.5)",
    alignItems: "center",
  },
  buttonText: { color: "#e5e7eb", fontSize: 13 },
  subtitle: { fontSize: 12, color: "#9ca3af" },
  presetCard: {
    width: 90,
    marginRight: 8,
    borderRadius: 10,
    overflow: "hidden",
    borderWidth: 1,
    borderColor: "rgba(30,64,175,0.6)",
    backgroundColor: "#020617",
  },
  presetSelected: {
    borderColor: "rgba(59,130,246,0.9)",
  },
  presetImage: { width: "100%", height: 60 },
  presetLabel: { fontSize: 10, color: "#e5e7eb", padding: 4 },
  cancel: { marginTop: 12, alignItems: "center" },
  cancelText: { fontSize: 13, color: "#9ca3af" },
});
