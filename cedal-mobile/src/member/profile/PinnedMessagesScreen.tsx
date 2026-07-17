// src/member/profile/PinnedMessagesScreen.tsx
import React from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  FlatList,
  Image,
} from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

export type PinnedMessageSummary = {
  id: string;
  text?: string | null;
  type?: string | null;
  imageUri?: string | null;
  videoUri?: string | null;
  fileUri?: string | null;
  fileName?: string | null;
  userId: string;
  userName?: string;
  timestamp?: number;
};

type Props = {
  title: string;
  otherDisplayName: string;
  currentUserId: string;
  pinnedMessages: PinnedMessageSummary[];
  onBack?: () => void;
};

export function PinnedMessagesScreen({
  title,
  otherDisplayName,
  currentUserId,
  pinnedMessages,
  onBack,
}: Props) {
  function handleBack() {
    if (onBack) onBack();
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

      <FlatList
        data={pinnedMessages}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.listContent}
        renderItem={({ item }) => {
          const createdAt = item.timestamp ? new Date(item.timestamp) : null;
          const dateText = createdAt ? createdAt.toLocaleString() : "";
          const isMine = item.userId === currentUserId;

          const hasImage = !!item.imageUri;
          const hasVideo = !!item.videoUri;
          const hasFile = !!item.fileUri;

          let label: string;
          if (item.text && item.text.trim().length > 0) {
            label = item.text.trim();
          } else if (hasImage) {
            label = "Photo";
          } else if (hasVideo) {
            label = "Video";
          } else if (hasFile) {
            label = item.fileName ?? "Document";
          } else {
            label = "Message";
          }

          return (
            <View style={styles.row}>
              <Text style={styles.rowSender}>
                {isMine ? "You" : item.userName ?? otherDisplayName}
              </Text>

              <View style={styles.rowMain}>
                {/* Media thumbnail (image/video) */}
                {hasImage ? (
                  <Image
                    source={{ uri: item.imageUri! }}
                    style={styles.mediaThumb}
                  />
                ) : hasVideo ? (
                  <View style={styles.mediaThumbVideo}>
                    <Text style={styles.mediaThumbVideoIcon}>▶️</Text>
                  </View>
                ) : (
                  <View style={styles.mediaThumbFallback}>
                    <Text style={styles.mediaThumbFallbackText}>MSG</Text>
                  </View>
                )}

                {/* Text + meta */}
                <View style={styles.rowTextBlock}>
                  <View style={styles.rowTitleRow}>
                    <View style={styles.pinIconCircle}>
                      <Text style={styles.pinIcon}>📌</Text>
                    </View>
                    <Text style={styles.rowTitle} numberOfLines={2}>
                      {label}
                    </Text>
                  </View>

                  {dateText.length > 0 && (
                    <Text style={styles.rowMeta}>{dateText}</Text>
                  )}

                  {hasImage && (
                    <Text style={styles.rowMeta}>Photo attached</Text>
                  )}
                  {hasVideo && (
                    <Text style={styles.rowMeta}>Video attached</Text>
                  )}
                  {hasFile && (
                    <Text style={styles.rowMeta}>
                      File: {item.fileName ?? "Document"}
                    </Text>
                  )}
                </View>
              </View>
            </View>
          );
        }}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Text style={styles.emptyTitle}>No pinned messages</Text>
            <Text style={styles.emptyText}>
              Pin messages in this chat and they will appear here.
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
  mediaThumb: {
    width: 48,
    height: 48,
    borderRadius: 10,
    marginRight: 8,
    backgroundColor: "#020617",
  },
  mediaThumbVideo: {
    width: 48,
    height: 48,
    borderRadius: 10,
    marginRight: 8,
    backgroundColor: "#0f172a",
    alignItems: "center",
    justifyContent: "center",
  },
  mediaThumbVideoIcon: {
    fontSize: 18,
    color: "#e5e7eb",
  },
  mediaThumbFallback: {
    width: 48,
    height: 48,
    borderRadius: 10,
    marginRight: 8,
    backgroundColor: "#020617",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.5)",
    alignItems: "center",
    justifyContent: "center",
  },
  mediaThumbFallbackText: {
    fontSize: 11,
    color: "#9ca3af",
  },
  rowTextBlock: {
    flex: 1,
  },
  rowTitleRow: {
    flexDirection: "row",
    alignItems: "center",
  },
  pinIconCircle: {
    width: 24,
    height: 24,
    borderRadius: 12,
    backgroundColor: "rgba(59,130,246,0.2)",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 6,
  },
  pinIcon: {
    fontSize: 14,
  },
  rowTitle: {
    flex: 1,
    fontSize: 13,
    color: "#e5e7eb",
    fontWeight: "600",
  },
  rowMeta: {
    fontSize: 11,
    color: "#9ca3af",
    marginTop: 2,
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
