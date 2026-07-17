// src/member/work/hack/HackCommunity.tsx
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

type Room = {
  id: string;
  name: string;
};

export type Post = {
  id: string;
  roomId: string;
  authorHandle: string;
  createdAt: string;
  body: string;
  likeCount: number;
  commentCount: number;
  // dislikeCount is added dynamically via state in HackPanel
};

type HackCommunityProps = {
  posts?: Post[];
  onOpenPost?: (post: Post) => void;
  onStartPost?: (roomId: string) => void;
  onPostLongPress?: (post: Post) => void;
  currentUserHandle?: string;
  onToggleLike?: (postId: string) => void;
  onToggleDislike?: (postId: string) => void;
};

const ROOMS: Room[] = [
  { id: "help", name: "Help & Debug" },
  { id: "show", name: "Show & Tell" },
  { id: "ideas", name: "Ideas" },
  { id: "collab", name: "Collabs" },
];

export default function HackCommunity({
  posts = [],
  onOpenPost,
  onStartPost,
  onPostLongPress,
  currentUserHandle,
  onToggleLike,
  onToggleDislike,
}: HackCommunityProps) {
  const { colors } = useTheme();
  const [activeRoomId, setActiveRoomId] = useState<string>("help");
  const [search, setSearch] = useState("");

  const filteredPosts = posts.filter((p) => {
    if (p.roomId !== activeRoomId) return false;
    if (!search.trim()) return true;
    const q = search.toLowerCase();
    return (
      p.body.toLowerCase().includes(q) ||
      p.authorHandle.toLowerCase().includes(q)
    );
  });

  return (
    <View style={styles.root}>
      {/* Room chips row */}
      <FlatList
        horizontal
        data={ROOMS}
        keyExtractor={(item) => item.id}
        showsHorizontalScrollIndicator={false}
        contentContainerStyle={styles.roomsRow}
        renderItem={({ item }) => {
          const active = item.id === activeRoomId;
          return (
            <TouchableOpacity
              style={[
                styles.roomChip,
                active && styles.roomChipActive,
              ]}
              activeOpacity={0.8}
              onPress={() => setActiveRoomId(item.id)}
            >
              <Text
                style={[
                  styles.roomChipText,
                  active && styles.roomChipTextActive,
                ]}
              >
                {item.name}
              </Text>
            </TouchableOpacity>
          );
        }}
      />

      {/* Search + start post bar */}
      <View style={styles.searchRow}>
        <View style={styles.searchBox}>
          <Ionicons
            name="search"
            size={15}
            color="#6b7280"
            style={{ marginRight: 6 }}
          />
          <TextInput
            style={styles.searchInput}
            placeholder="Search posts..."
            placeholderTextColor="#6b7280"
            value={search}
            onChangeText={setSearch}
          />
        </View>

        <TouchableOpacity
          style={styles.startPostButton}
          activeOpacity={0.8}
          onPress={() => onStartPost?.(activeRoomId)}
        >
          <Ionicons
            name="add-circle-outline"
            size={16}
            color="#e5e7eb"
            style={{ marginRight: 4 }}
          />
          <Text style={styles.startPostText}>Start a post</Text>
        </TouchableOpacity>
      </View>

      {/* Posts list */}
      <FlatList
        data={filteredPosts}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.postsList}
        renderItem={({ item }) => {
          const isOwner = item.authorHandle === currentUserHandle;
          const likedByMe = (item as any).likedByMe === true;
          const dislikedByMe = (item as any).dislikedByMe === true;
          const dislikeCount = (item as any).dislikeCount ?? 0;

          return (
            <TouchableOpacity
              style={styles.postCard}
              activeOpacity={0.8}
              onPress={() => onOpenPost?.(item)}
              onLongPress={() => {
                if (!isOwner) return;
                onPostLongPress?.(item);
              }}
            >
              <View style={styles.postHeaderRow}>
                <Text style={styles.postAuthor}>{item.authorHandle}</Text>
                <Text style={styles.postTime}>{item.createdAt}</Text>
              </View>

              <Text style={styles.postBody} numberOfLines={3}>
                {item.body}
              </Text>

              <View style={styles.postMetaRow}>
                {/* Like */}
                <TouchableOpacity
                  style={styles.metaGroup}
                  activeOpacity={0.7}
                  onPress={() => onToggleLike?.(item.id)}
                >
                  <Ionicons
                    name={likedByMe ? "heart" : "heart-outline"}
                    size={14}
                    color={likedByMe ? "#f97316" : "#6b7280"}
                    style={{ marginRight: 3 }}
                  />
                  <Text style={styles.postMetaText}>{item.likeCount}</Text>
                </TouchableOpacity>

                {/* Dislike */}
                <TouchableOpacity
                  style={styles.metaGroup}
                  activeOpacity={0.7}
                  onPress={() => onToggleDislike?.(item.id)}
                >
                  <Ionicons
                    name={dislikedByMe ? "thumbs-down" : "thumbs-down-outline"}
                    size={14}
                    color={dislikedByMe ? "#f97316" : "#6b7280"}
                    style={{ marginRight: 3 }}
                  />
                  <Text style={styles.postMetaText}>{dislikeCount}</Text>
                </TouchableOpacity>

                {/* Comments count */}
                <View style={styles.metaGroup}>
                  <Ionicons
                    name="chatbubble-ellipses-outline"
                    size={14}
                    color="#6b7280"
                    style={{ marginRight: 3 }}
                  />
                  <Text style={styles.postMetaText}>
                    {item.commentCount}
                  </Text>
                </View>

                {isOwner && (
                  <Text
                    style={[
                      styles.postMetaText,
                      { marginLeft: "auto", fontSize: 10 },
                    ]}
                  >
                    Hold for actions
                  </Text>
                )}
              </View>
            </TouchableOpacity>
          );
        }}
        ListEmptyComponent={
          <View style={styles.emptyState}>
            <Ionicons
              name="chatbubble-ellipses-outline"
              size={28}
              color="#4b5563"
            />
            <Text style={styles.emptyTitle}>
              No posts yet in this room.
            </Text>
            <Text style={styles.emptySubtitle}>
              Start a post to kick off the first hacker thread here.
            </Text>
          </View>
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
  },
  roomsRow: {
    paddingVertical: 4,
    paddingHorizontal: 4,
  },
  roomChip: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "rgba(15,23,42,0.9)",
    marginHorizontal: 4,
  },
  roomChipActive: {
    backgroundColor: "#22c55e",
  },
  roomChipText: {
    fontSize: 12,
    color: "#e5e7eb",
  },
  roomChipTextActive: {
    color: "#020617",
    fontWeight: "600",
  },
  searchRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 8,
    columnGap: 8,
  },
  searchBox: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "rgba(15,23,42,0.9)",
  },
  searchInput: {
    flex: 1,
    fontSize: 13,
    color: "#e5e7eb",
    paddingVertical: 2,
  },
  startPostButton: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "#22c55e",
  },
  startPostText: {
    fontSize: 12,
    color: "#020617",
    fontWeight: "600",
  },
  postsList: {
    paddingTop: 10,
    paddingBottom: 12,
  },
  postCard: {
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderRadius: 12,
    marginBottom: 8,
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
  postTime: {
    fontSize: 11,
    color: "#6b7280",
  },
  postBody: {
    fontSize: 13,
    color: "#e5e7eb",
  },
  postMetaRow: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 6,
    columnGap: 12,
  },
  metaGroup: {
    flexDirection: "row",
    alignItems: "center",
  },
  postMetaText: {
    fontSize: 11,
    color: "#9ca3af",
  },
  emptyState: {
    marginTop: 40,
    alignItems: "center",
    paddingHorizontal: 24,
  },
  emptyTitle: {
    color: "#e5e7eb",
    fontSize: 14,
    fontWeight: "500",
    marginTop: 8,
    textAlign: "center",
  },
  emptySubtitle: {
    color: "#9ca3af",
    fontSize: 12,
    marginTop: 4,
    textAlign: "center",
  },
});
