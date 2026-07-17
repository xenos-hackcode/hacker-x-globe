// src/member/profile/DocumentsScreen.tsx
import React, { useMemo, useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  FlatList,
  ScrollView,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";
import { useRouter } from "expo-router";
import { Linking } from "react-native";

type SavedFilter = "all" | "saved";

export type DocumentMessage = {
  id: string;
  fileUri?: string | null;
  fileName?: string | null;
  fileSize?: number | null;
  fileMime?: string | null;
  userId: string;
  userName?: string;
  timestamp?: number;
  savedBy?: string[]; // so we can know if YOU saved it
};

type Props = {
  title: string;
  otherDisplayName: string;
  currentUserId: string;
  documentMessages: DocumentMessage[]; // pass ALL docs from this chat
  onBack?: () => void;
};

export function DocumentsScreen({
  title,
  otherDisplayName,
  currentUserId,
  documentMessages,
  onBack,
}: Props) {
  const router = useRouter();
  const [savedFilter, setSavedFilter] = useState<SavedFilter>("all");

  const filtered = useMemo(() => {
    return documentMessages.filter((m) => {
      const isDoc = !!m.fileUri;
      if (!isDoc) return false;

      if (savedFilter === "saved") {
        const savedBy = m.savedBy ?? [];
        return savedBy.includes(currentUserId);
      }

      // "all"
      return true;
    });
  }, [documentMessages, savedFilter, currentUserId]);

  const savedChips: { key: SavedFilter; label: string }[] = [
    { key: "all", label: "All documents" },
    { key: "saved", label: "Saved only" },
  ];

  function handleBack() {
    if (onBack) onBack();
    else router.back();
  }

  function handleOpenDoc(uri: string | null | undefined) {
    if (!uri) return;
    Linking.openURL(uri).catch(() => {});
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
        <Text style={styles.filterLabel}>Saved</Text>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.chipRowHorizontal}
        >
          {savedChips.map((chip) => {
            const selected = chip.key === savedFilter;
            return (
              <TouchableOpacity
                key={chip.key}
                style={[styles.chip, selected && styles.chipSelected]}
                activeOpacity={0.85}
                onPress={() => setSavedFilter(chip.key)}
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
          const sizeKb =
            typeof item.fileSize === "number"
              ? Math.round(item.fileSize / 1024)
              : null;
          const savedBy = item.savedBy ?? [];
          const isSavedByMe = savedBy.includes(currentUserId);

          return (
            <TouchableOpacity
              style={styles.row}
              activeOpacity={0.85}
              onPress={() => handleOpenDoc(item.fileUri)}
            >
              <Text style={styles.rowSender}>
                {item.userId === currentUserId
                  ? "You"
                  : item.userName ?? otherDisplayName}
              </Text>

              <View style={styles.rowMain}>
                <View style={styles.docIconCircle}>
                  <Text style={styles.docIcon}>📄</Text>
                </View>
                <View style={styles.rowTextBlock}>
                  <Text style={styles.docTitle} numberOfLines={1}>
                    {item.fileName ?? "Document"}
                  </Text>
                  <View style={styles.rowMetaRow}>
                    {item.fileMime && (
                      <Text style={styles.rowMeta}>
                        {item.fileMime}
                      </Text>
                    )}
                    {sizeKb != null && (
                      <Text style={styles.rowMeta}>
                        · {sizeKb} KB
                      </Text>
                    )}
                    {dateText.length > 0 && (
                      <Text style={styles.rowMeta}> · {dateText}</Text>
                    )}
                    {isSavedByMe && (
                      <Text style={styles.rowSavedTag}>Saved</Text>
                    )}
                  </View>
                </View>
              </View>
            </TouchableOpacity>
          );
        }}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text style={styles.emptyTitle}>No documents yet</Text>
            <Text style={styles.emptyText}>
              Any documents shared in this chat will appear here.
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
  chipRowHorizontal: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
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
  rowMain: {
    flexDirection: "row",
    alignItems: "center",
  },
  docIconCircle: {
    width: 32,
    height: 32,
    borderRadius: 16,
    backgroundColor: "rgba(59,130,246,0.2)",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 8,
  },
  docIcon: {
    fontSize: 18,
  },
  rowTextBlock: {
    flex: 1,
  },
  docTitle: {
    fontSize: 13,
    color: "#e5e7eb",
    fontWeight: "600",
  },
  rowMetaRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    alignItems: "center",
    marginTop: 2,
  },
  rowMeta: {
    fontSize: 11,
    color: "#9ca3af",
  },
  rowSavedTag: {
    fontSize: 11,
    color: "#facc15",
    marginLeft: 4,
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
