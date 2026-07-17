//src/member/chat/StickerPickerSheet.tsx
import React, { useEffect, useMemo, useState } from "react";
import {
  Modal,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Image,
  TextInput,
  ScrollView,
} from "react-native";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import * as ImagePicker from "expo-image-picker";
import * as DocumentPicker from "expo-document-picker";
import { auth, db } from "@/src/api/firebase";
import {
  collection,
  addDoc,
  query,
  orderBy,
  onSnapshot,
  serverTimestamp,
} from "firebase/firestore";

type Props = {
  visible: boolean;
  onClose: () => void;
  onSelectSticker?: (url: string) => void;
  onOpenChatMedia?: () => void;
};

type StickerItem = {
  id?: string;
  url: string;
  name: string;
  category: "sexy" | "love" | "funny" | "meme" | "other";
};

const CATEGORIES: Array<"all" | "sexy" | "love" | "funny" | "meme" | "other"> = [
  "all",
  "sexy",
  "love",
  "funny",
  "meme",
  "other",
];

export function StickerPickerSheet({
  visible,
  onClose,
  onSelectSticker,
  onOpenChatMedia,
}: Props) {
  const insets = useSafeAreaInsets();

  const [selectedCategory, setSelectedCategory] =
    useState<"all" | "sexy" | "love" | "funny" | "meme" | "other">("all");
  const [search, setSearch] = useState("");
  const [showFilterMenu, setShowFilterMenu] = useState(false);
  const [showCreateMenu, setShowCreateMenu] = useState(false);

  const [stickers, setStickers] = useState<StickerItem[]>([]);

  const currentUser = auth.currentUser;
  const currentUserId = currentUser?.uid ?? null;

  // load ALL shared stickers, newest first
  useEffect(() => {
    if (!currentUserId) return;
    const col = collection(db, "stickers");
    const q = query(col, orderBy("createdAt", "desc"));

    const unsub = onSnapshot(q, (snap) => {
      const list: StickerItem[] = [];
      snap.forEach((docSnap) => {
        const d = docSnap.data() as any;
        if (!d.url) return;
        list.push({
          id: docSnap.id,
          url: d.url,
          name: d.name || "My sticker",
          category: d.category || "other",
        });
      });
      setStickers(list);
    });

    return () => unsub();
  }, [currentUserId]);

  const saveStickerDoc = async (uri: string) => {
    if (!currentUserId) return;
    try {
      const col = collection(db, "stickers");
      await addDoc(col, {
        userId: currentUserId,
        url: uri,
        name: "My sticker",
        category: "other",
        createdAt: serverTimestamp(),
      });
    } catch (e: any) {
      console.warn("save sticker doc error", e?.message);
    }
  };

  const addStickerAndMaybeSend = async (uri: string | undefined | null) => {
    if (!uri) return;

    // optimistic add so it appears immediately in the grid
    setStickers((prev) => {
      if (prev.some((s) => s.url === uri)) return prev;
      return [{ url: uri, name: "My sticker", category: "other" }, ...prev];
    });

    // let ChatScreen decide what to do (open preview)
    onSelectSticker?.(uri);

    await saveStickerDoc(uri);
  };

  const handlePickFromGallery = async () => {
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      quality: 1,
    });
    if (result.canceled) return;
    await addStickerAndMaybeSend(result.assets?.[0]?.uri);
  };

  const handlePickFromCamera = async () => {
    const result = await ImagePicker.launchCameraAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      quality: 1,
    });
    if (result.canceled) return;
    await addStickerAndMaybeSend(result.assets?.[0]?.uri);
  };

  const handlePickFromVideo = async () => {
    const result = await ImagePicker.launchCameraAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Videos,
      videoMaxDuration: 10,
      quality: 1,
    });
    if (result.canceled) return;
    await addStickerAndMaybeSend(result.assets?.[0]?.uri);
  };

  const handlePickFromFiles = async () => {
    try {
      const res = await DocumentPicker.getDocumentAsync({
        type: ["image/*"],
        multiple: false,
        copyToCacheDirectory: true,
      });
      if (res.canceled) return;
      const asset = res.assets?.[0];
      await addStickerAndMaybeSend(asset?.uri);
    } catch (e: any) {
      console.warn("file pick error", e?.message);
    }
  };

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    const all = stickers;

    return all.filter((s) => {
      if (!s.url) return false;
      const categoryMatch =
        selectedCategory === "all" || s.category === selectedCategory;
      const searchMatch =
        !q ||
        s.name.toLowerCase().includes(q) ||
        s.category.toLowerCase().includes(q);
      return categoryMatch && searchMatch;
    });
  }, [selectedCategory, search, stickers]);

  const currentFilterLabel =
    selectedCategory === "all"
      ? "All"
      : selectedCategory.charAt(0).toUpperCase() + selectedCategory.slice(1);

  if (!visible) return null;

  return (
    <Modal
      transparent
      animationType="slide"
      visible={visible}
      onRequestClose={onClose}
    >
      <View style={styles.backdrop}>
        <View
          style={[
            styles.sheet,
            { paddingBottom: 24 + insets.bottom },
          ]}
        >
          <View style={styles.headerRow}>
            <Text style={styles.title}>Stickers</Text>
            <View style={styles.headerRight}>
              <View style={styles.createBox}>
                <TouchableOpacity
                  activeOpacity={0.8}
                  onPress={() => setShowCreateMenu((v) => !v)}
                  style={styles.createBtn}
                >
                  <Ionicons name="add" size={16} color="#0f172a" />
                </TouchableOpacity>
                {showCreateMenu && (
                  <View style={styles.createDropdown}>
                    {[
                      { id: "picture", label: "From picture" },
                      { id: "camera", label: "From camera" },
                      { id: "video", label: "From video frame" },
                      { id: "files", label: "From files" },
                      { id: "media", label: "From chat media" },
                    ].map((opt) => (
                      <TouchableOpacity
                        key={opt.id}
                        activeOpacity={0.8}
                        onPress={async () => {
                          setShowCreateMenu(false);
                          if (opt.id === "picture") {
                            await handlePickFromGallery();
                          } else if (opt.id === "camera") {
                            await handlePickFromCamera();
                          } else if (opt.id === "video") {
                            await handlePickFromVideo();
                          } else if (opt.id === "files") {
                            await handlePickFromFiles();
                          } else if (opt.id === "media") {
                            onOpenChatMedia?.();
                          }
                        }}
                        style={styles.createOption}
                      >
                        <Text style={styles.createOptionText}>{opt.label}</Text>
                      </TouchableOpacity>
                    ))}
                  </View>
                )}
              </View>

              <TouchableOpacity
                activeOpacity={0.8}
                onPress={onClose}
                style={styles.closeBtn}
              >
                <Text style={styles.closeText}>Close</Text>
              </TouchableOpacity>
            </View>
          </View>

          <View style={styles.searchWrap}>
            <View style={styles.filterBox}>
              <TouchableOpacity
                activeOpacity={0.8}
                onPress={() => setShowFilterMenu((v) => !v)}
                style={styles.filterIconBtn}
              >
                <Ionicons
                  name="filter-outline"
                  size={14}
                  color="#e5e7eb"
                />
              </TouchableOpacity>
              {showFilterMenu && (
                <View style={styles.filterDropdown}>
                  {CATEGORIES.map((cat) => {
                    const active = selectedCategory === cat;
                    const label =
                      cat === "all"
                        ? "All"
                        : cat.charAt(0).toUpperCase() + cat.slice(1);
                    return (
                      <TouchableOpacity
                        key={cat}
                        activeOpacity={0.8}
                        onPress={() => {
                          setSelectedCategory(cat);
                          setShowFilterMenu(false);
                        }}
                        style={[
                          styles.filterOption,
                          active && styles.filterOptionActive,
                        ]}
                      >
                        <Text
                          style={[
                            styles.filterOptionText,
                            active && styles.filterOptionTextActive,
                          ]}
                        >
                          {label}
                        </Text>
                      </TouchableOpacity>
                    );
                  })}
                </View>
              )}
            </View>

            <Ionicons
              name="search-outline"
              size={14}
              color="#6b7280"
              style={{ marginRight: 6 }}
            />
            <TextInput
              value={search}
              onChangeText={setSearch}
              placeholder="Search stickers by name or vibe…"
              placeholderTextColor="#6b7280"
              style={styles.searchInput}
            />
          </View>

          <Text style={styles.activeFilterText}>
            Filter: {currentFilterLabel}
          </Text>

          <View style={{ height: 320 }}>
            <ScrollView
              showsVerticalScrollIndicator={false}
              contentContainerStyle={styles.grid}
            >
              {filtered.map((sticker) => (
                <TouchableOpacity
                  key={sticker.id ?? sticker.url}
                  style={styles.stickerButton}
                  activeOpacity={0.8}
                  onPress={() => {
                    onClose();
                    onSelectSticker?.(sticker.url);
                  }}
                >
                  <Image
                    source={{ uri: sticker.url }}
                    style={styles.stickerImage}
                  />
                </TouchableOpacity>
              ))}

              {filtered.length === 0 && (
                <View style={styles.emptyState}>
                  <Text style={styles.emptyText}>
                    No stickers yet. Create one.
                  </Text>
                </View>
              )}
            </ScrollView>
          </View>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    justifyContent: "flex-end",
    backgroundColor: "rgba(15,23,42,0.75)",
  },
  sheet: {
    backgroundColor: "#020617",
    borderTopLeftRadius: 18,
    borderTopRightRadius: 18,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.4)",
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: 24,
    flexShrink: 0,
    maxHeight: 700,
  },
  headerRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 8,
  },
  title: {
    color: "#e5e7eb",
    fontSize: 14,
    fontWeight: "700",
  },
  headerRight: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  createBox: { position: "relative" },
  createBtn: {
    width: 28,
    height: 28,
    borderRadius: 14,
    backgroundColor: "#38bdf8",
    alignItems: "center",
    justifyContent: "center",
  },
  createDropdown: {
    position: "absolute",
    top: 32,
    right: 0,
    borderRadius: 10,
    backgroundColor: "rgba(15,23,42,0.98)",
    borderWidth: 1,
    borderColor: "rgba(31,41,55,1)",
    paddingVertical: 4,
    minWidth: 160,
    zIndex: 30,
  },
  createOption: {
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  createOptionText: {
    fontSize: 12,
    color: "#e5e7eb",
  },
  closeBtn: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.8)",
  },
  closeText: {
    fontSize: 11,
    color: "#9ca3af",
    letterSpacing: 1,
    textTransform: "uppercase",
  },
  searchWrap: {
    flexDirection: "row",
    alignItems: "center",
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(15,23,42,0.9)",
    backgroundColor: "rgba(15,23,42,0.98)",
    paddingHorizontal: 10,
    paddingVertical: 6,
    marginTop: 2,
    marginBottom: 4,
    gap: 6,
  },
  filterBox: { position: "relative", marginRight: 2 },
  filterIconBtn: {
    width: 24,
    height: 24,
    borderRadius: 12,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "rgba(15,23,42,0.9)",
    borderWidth: 1,
    borderColor: "rgba(55,65,81,0.9)",
  },
  filterDropdown: {
    position: "absolute",
    top: 28,
    left: 0,
    borderRadius: 10,
    backgroundColor: "rgba(15,23,42,0.98)",
    borderWidth: 1,
    borderColor: "rgba(31,41,55,1)",
    paddingVertical: 4,
    minWidth: 110,
    zIndex: 20,
  },
  filterOption: { paddingHorizontal: 8, paddingVertical: 4 },
  filterOptionActive: { backgroundColor: "rgba(30,64,175,0.6)" },
  filterOptionText: { fontSize: 11, color: "#9ca3af" },
  filterOptionTextActive: { color: "#e5e7eb", fontWeight: "600" },
  searchInput: {
    flex: 1,
    fontSize: 13,
    color: "#e5e7eb",
  },
  activeFilterText: {
    fontSize: 11,
    color: "#6b7280",
    marginBottom: 8,
  },
  grid: {
    flexDirection: "row",
    flexWrap: "wrap",
    justifyContent: "space-between",
    paddingBottom: 8,
  },
  stickerButton: {
    width: "30%",
    aspectRatio: 1,
    borderRadius: 12,
    overflow: "hidden",
    marginBottom: 10,
    backgroundColor: "#0f172a",
  },
  stickerImage: { width: "100%", height: "100%" },
  emptyState: {
    width: "100%",
    paddingVertical: 20,
    alignItems: "center",
  },
  emptyText: { color: "#6b7280", fontSize: 12 },
});
