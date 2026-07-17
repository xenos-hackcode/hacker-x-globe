// src/member/profile/MediaScreen.tsx
import React, { useMemo, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  FlatList,
  Image,
  ScrollView,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useRouter } from "expo-router";

type MediaTypeFilter = "all" | "images" | "videos";

export type MediaMessage = {
  id: string;
  imageUri?: string | null;
  videoUri?: string | null;
  userId: string;
  userName?: string;
  timestamp?: number;
};

type Props = {
  title: string;
  otherDisplayName: string;
  currentUserId: string;
  mediaMessages: MediaMessage[]; // IMPORTANT: pass all messages here (saved or not)
  onBack?: () => void;
};

export function MediaScreen({
  title,
  otherDisplayName,
  currentUserId,
  mediaMessages,
  onBack,
}: Props) {
  const router = useRouter();
  const [typeFilter, setTypeFilter] = useState<MediaTypeFilter>("all");

  const filtered = useMemo(() => {
    return mediaMessages.filter((m) => {
      const isImage = !!m.imageUri;
      const isVideo = !!m.videoUri;

      if (!isImage && !isVideo) return false;

      if (typeFilter === "images" && !isImage) return false;
      if (typeFilter === "videos" && !isVideo) return false;

      return true;
    });
  }, [mediaMessages, typeFilter]);

  const typeChips: { key: MediaTypeFilter; label: string }[] = [
    { key: "all", label: "All" },
    { key: "images", label: "Images" },
    { key: "videos", label: "Videos" },
  ];

  function handleBack() {
    if (onBack) onBack();
    else router.back();
  }

  return (
    <SafeAreaView style={styles.screen} edges={["top", "bottom"]}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity
          onPress={handleBack}
          hitSlop={16}
          style={styles.backBtn}
        >
          <Text style={styles.backText}>Back</Text>
        </TouchableOpacity>
        <Text style={styles.headerTitle}>{title}</Text>
        <View style={{ width: 48 }} />
      </View>

      {/* Filter */}
      <View style={styles.filterBlock}>
        <Text style={styles.filterLabel}>Content</Text>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.chipRowHorizontal}
        >
          {typeChips.map((chip) => {
            const selected = chip.key === typeFilter;
            return (
              <TouchableOpacity
                key={chip.key}
                style={[styles.chip, selected && styles.chipSelected]}
                activeOpacity={0.85}
                onPress={() => setTypeFilter(chip.key)}
              >
                <Text
                  style={[
                    styles.chipText,
                    selected && styles.chipTextSelected,
                  ]}
                >
                  {chip.label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>
      </View>

      {/* List */}
      <FlatList
        data={filtered}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.listContent}
        renderItem={({ item }) => {
          const createdAt = item.timestamp ? new Date(item.timestamp) : null;
          const dateText = createdAt ? createdAt.toLocaleString() : "";

          return (
            <View style={styles.row}>
              <Text style={styles.rowSender}>
                {item.userId === currentUserId
                  ? "You"
                  : item.userName ?? otherDisplayName}
              </Text>

              {item.imageUri ? (
                <Image
                  source={{ uri: item.imageUri }}
                  style={styles.mediaImage}
                  resizeMode="cover"
                />
              ) : (
                <View style={styles.videoThumb}>
                  <Text style={styles.videoLabel}>Video</Text>
                </View>
              )}

              {dateText.length > 0 && (
                <Text style={styles.rowMeta}>{dateText}</Text>
              )}
            </View>
          );
        }}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text style={styles.emptyTitle}>No media yet</Text>
            <Text style={styles.emptyText}>
              Photos and videos from this chat will appear here.
            </Text>
          </View>
        }
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#020617",
    position: "absolute",
    top: 0,
    right: 0,
    bottom: 0,
    left: 0,
    zIndex: 100,
    elevation: 100,
  },
  header: {
    height: 48,
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(15,23,42,0.8)",
  },
  backBtn: {
    minWidth: 48,
    paddingVertical: 4,
  },
  backText: {
    fontSize: 12,
    color: "#93c5fd",
    textTransform: "uppercase",
    letterSpacing: 0.8,
  },
  headerTitle: {
    flex: 1,
    textAlign: "center",
    fontSize: 15,
    fontWeight: "600",
    color: "#e5e7eb",
  },
  filterBlock: {
    paddingHorizontal: 16,
    paddingTop: 10,
  },
  filterLabel: {
    fontSize: 11,
    color: "#9ca3af",
    marginBottom: 6,
    textTransform: "uppercase",
    letterSpacing: 0.8,
  },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(96,165,250,0.7)",
    backgroundColor: "#020617",
  },
  chipSelected: {
    borderColor: "rgba(59,130,246,1)",
    backgroundColor: "#0b1120",
  },
  chipText: {
    fontSize: 11,
    color: "#e5e7eb",
    textTransform: "uppercase",
    letterSpacing: 0.8,
  },
  chipTextSelected: {
    color: "#bfdbfe",
  },
  chipRowHorizontal: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  listContent: {
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: 24,
    gap: 8,
  },
  row: {
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "rgba(30,64,175,0.6)",
    backgroundColor: "#020617",
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
  rowSender: {
    fontSize: 11,
    color: "#9ca3af",
    marginBottom: 4,
  },
  mediaImage: {
    width: "100%",
    aspectRatio: 16 / 9,
    borderRadius: 8,
    marginBottom: 4,
  },
  videoThumb: {
    width: "100%",
    aspectRatio: 16 / 9,
    borderRadius: 8,
    marginBottom: 4,
    backgroundColor: "#020617",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    alignItems: "center",
    justifyContent: "center",
  },
  videoLabel: {
    color: "#e5e7eb",
    fontSize: 11,
  },
  rowMeta: {
    marginTop: 2,
    fontSize: 11,
    color: "#9ca3af",
  },
  emptyState: {
    paddingTop: 40,
    alignItems: "center",
  },
  emptyTitle: {
    fontSize: 14,
    color: "#e5e7eb",
    marginBottom: 4,
  },
  emptyText: {
    fontSize: 12,
    color: "#9ca3af",
  },
});
