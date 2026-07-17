// src/member/work/hack/HackPostScreen.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import React, { useState } from "react";
import {
  FlatList,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";

type Comment = {
  id: string;
  authorHandle: string;
  body: string;
  createdAt: string;
};

type Props = {
  authorHandle: string;
  createdAt: string;
  body: string;
  likeCount: number;
  commentCount: number;
  comments?: Comment[];                 // NEW: pass real comments
  onBack?: () => void;
  onSendComment?: (body: string) => void; // NEW: send up to parent
};

export default function HackPostScreen({
  authorHandle,
  createdAt,
  body,
  likeCount,
  commentCount,
  comments: initialComments = [],
  onBack,
  onSendComment,
}: Props) {
  const { colors } = useTheme();
  const [comment, setComment] = useState("");
  const [comments, setComments] = useState<Comment[]>(initialComments);

  const handleSend = () => {
    const text = comment.trim();
    if (!text) return;

    const newComment: Comment = {
      id: `local-${Date.now()}`,
      authorHandle: "@you", // later: current user handle
      body: text,
      createdAt: "now",
    };

    setComments((prev) => [newComment, ...prev]);
    onSendComment?.(text); // notify parent if needed
    setComment("");
  };

  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      {/* Header */}
      <View style={styles.header}>
        <TouchableOpacity
          onPress={onBack}
          style={styles.backButton}
          activeOpacity={0.7}
        >
          <Ionicons
            name="chevron-back"
            size={20}
            color={colors.textPrimary}
          />
          <Text style={[styles.backText, { color: colors.textPrimary }]}>
            Back
          </Text>
        </TouchableOpacity>

        <Text style={[styles.headerTitle, { color: colors.textPrimary }]}>
          Community post
        </Text>
      </View>

      {/* Post content */}
      <View style={styles.postContainer}>
        <View style={styles.postHeaderRow}>
          <Text style={styles.postAuthor}>{authorHandle}</Text>
          <Text style={styles.postTime}>{createdAt}</Text>
        </View>
        <Text style={styles.postBody}>{body}</Text>
        <View style={styles.postMetaRow}>
          <View style={styles.metaGroup}>
            <Ionicons
              name="heart-outline"
              size={14}
              color="#f97316"
              style={{ marginRight: 3 }}
            />
            <Text style={styles.postMetaText}>{likeCount}</Text>
          </View>
          <View style={styles.metaGroup}>
            <Ionicons
              name="chatbubble-ellipses-outline"
              size={14}
              color="#6b7280"
              style={{ marginRight: 3 }}
            />
            <Text style={styles.postMetaText}>
              {Math.max(commentCount, comments.length)}
            </Text>
          </View>
        </View>
      </View>

      {/* Comments */}
      <FlatList
        style={styles.commentsList}
        contentContainerStyle={{ paddingBottom: 12 }}
        data={comments}
        keyExtractor={(item) => item.id}
        inverted // newest at bottom visually
        renderItem={({ item }) => (
          <View style={styles.commentBubble}>
            <View style={styles.commentHeader}>
              <Text style={styles.commentAuthor}>
                {item.authorHandle}
              </Text>
              <Text style={styles.commentTime}>{item.createdAt}</Text>
            </View>
            <Text style={styles.commentBody}>{item.body}</Text>
          </View>
        )}
        ListEmptyComponent={
          <View style={{ paddingHorizontal: 16, paddingVertical: 8 }}>
            <Text style={{ fontSize: 12, color: "#6b7280" }}>
              No comments yet. Be the first to reply.
            </Text>
          </View>
        }
      />

      {/* Comment composer */}
      <View style={styles.composer}>
        <TextInput
          style={styles.composerInput}
          placeholder="Add a comment..."
          placeholderTextColor="#6b7280"
          value={comment}
          onChangeText={setComment}
          multiline
        />
        <TouchableOpacity
          style={[
            styles.sendButton,
            !comment.trim() && { opacity: 0.4 },
          ]}
          disabled={!comment.trim()}
          onPress={handleSend}
        >
          <Ionicons name="send" size={18} color="#020617" />
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  header: {
    paddingTop: 40,
    paddingHorizontal: 16,
    paddingBottom: 8,
    flexDirection: "row",
    alignItems: "center",
    columnGap: 10,
  },
  backButton: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 6,
    paddingRight: 8,
  },
  backText: { fontSize: 13, marginLeft: 2 },
  headerTitle: { fontSize: 16, fontWeight: "600" },
  postContainer: {
    marginHorizontal: 16,
    marginTop: 8,
    marginBottom: 8,
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 12,
    backgroundColor: "rgba(15,23,42,0.95)",
  },
  postHeaderRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: 4,
  },
  postAuthor: {
    fontSize: 13,
    color: "#e5e7eb",
    fontWeight: "500",
  },
  postTime: { fontSize: 11, color: "#6b7280" },
  postBody: {
    fontSize: 13,
    color: "#e5e7eb",
    marginTop: 2,
  },
  postMetaRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 6,
    columnGap: 12,
  },
  metaGroup: { flexDirection: "row", alignItems: "center" },
  postMetaText: { fontSize: 11, color: "#9ca3af" },
  commentsList: { flex: 1, paddingHorizontal: 16 },
  commentBubble: {
    paddingVertical: 8,
    paddingHorizontal: 10,
    borderRadius: 10,
    marginBottom: 6,
    backgroundColor: "rgba(15,23,42,0.95)",
  },
  commentHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: 2,
  },
  commentAuthor: {
    fontSize: 12,
    fontWeight: "500",
    color: "#e5e7eb",
  },
  commentTime: { fontSize: 10, color: "#6b7280" },
  commentBody: { fontSize: 13, color: "#e5e7eb" },
  composer: {
    flexDirection: "row",
    alignItems: "center",
    marginHorizontal: 16,
    marginBottom: 16,
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "rgba(15,23,42,0.9)",
  },
  composerInput: {
    flex: 1,
    fontSize: 13,
    color: "#e5e7eb",
    paddingVertical: 4,
    paddingHorizontal: 6,
  },
  sendButton: {
    marginLeft: 8,
    width: 32,
    height: 32,
    borderRadius: 999,
    backgroundColor: "#22c55e",
    alignItems: "center",
    justifyContent: "center",
  },
});
