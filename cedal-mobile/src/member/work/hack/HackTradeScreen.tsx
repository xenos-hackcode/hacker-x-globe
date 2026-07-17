// src/member/work/hack/HackTradeScreen.tsx
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

export type TradeItem = {
  id: string;
  threadId: string;
  type: "offer" | "request";
  title: string;
  description: string;
  authorHandle: string;
  createdAt: string;
  status: "open" | "in_progress" | "done";
};

type Props = {
  trades: TradeItem[];
  currentUserHandle: string;
  onOpenThread?: (trade: TradeItem) => void;
  onCreateTrade?: (t: {
    type: "offer" | "request";
    title: string;
    description: string;
  }) => void;
  onUpdateTrade?: (t: TradeItem) => void;
  onDeleteTrade?: (id: string) => void;
};

export default function HackTradeScreen({
  trades,
  currentUserHandle,
  onOpenThread,
  onCreateTrade,
  onUpdateTrade,
  onDeleteTrade,
}: Props) {
  const { colors } = useTheme();
  const [filter, setFilter] = useState<"all" | "offer" | "request">("all");
  const [composerOpen, setComposerOpen] = useState(false);
  const [mode, setMode] = useState<"offer" | "request">("offer");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");

  const [actionsTarget, setActionsTarget] = useState<TradeItem | null>(null);
  const [editDraftTitle, setEditDraftTitle] = useState("");
  const [editDraftDescription, setEditDraftDescription] = useState("");

  const filtered = trades.filter((t) =>
    filter === "all" ? true : t.type === filter,
  );

  const handleCreate = () => {
    const t = title.trim();
    const d = description.trim();
    if (!t || !d) return;
    onCreateTrade?.({ type: mode, title: t, description: d });
    setTitle("");
    setDescription("");
    setMode("offer");
    setComposerOpen(false);
  };

  const isOwner = (item: TradeItem) =>
    item.authorHandle === currentUserHandle;

  const openActionsFor = (item: TradeItem) => {
    if (!isOwner(item)) return; // only creator can edit/delete
    setActionsTarget(item);
    setEditDraftTitle(item.title);
    setEditDraftDescription(item.description);
  };

  const handleSaveEdit = () => {
    if (!actionsTarget) return;
    const t = editDraftTitle.trim();
    const d = editDraftDescription.trim();
    if (!t || !d) return;
    onUpdateTrade?.({
      ...actionsTarget,
      title: t,
      description: d,
    });
    setActionsTarget(null);
  };

  const handleDelete = () => {
    if (!actionsTarget) return;
    onDeleteTrade?.(actionsTarget.id);
    setActionsTarget(null);
  };

  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      {/* Filter row + plus */}
      <View style={styles.topRow}>
        <View style={styles.filterRow}>
          {(["all", "offer", "request"] as const).map((key) => {
            const label =
              key === "all" ? "All" : key === "offer" ? "Offers" : "Requests";
            const active = filter === key;
            return (
              <TouchableOpacity
                key={key}
                style={[styles.filterChip, active && styles.filterChipActive]}
                onPress={() => setFilter(key)}
                activeOpacity={0.8}
              >
                <Text
                  style={[
                    styles.filterChipText,
                    active && styles.filterChipTextActive,
                  ]}
                >
                  {label}
                </Text>
              </TouchableOpacity>
            );
          })}
        </View>

        <TouchableOpacity
          style={styles.plusButton}
          onPress={() => setComposerOpen((v) => !v)}
          activeOpacity={0.8}
        >
          <Ionicons name="add" size={18} color="#020617" />
        </TouchableOpacity>
      </View>

      {/* List of threads */}
      <FlatList
        data={filtered}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => {
          const owner = isOwner(item);
          return (
            <TouchableOpacity
              style={styles.card}
              activeOpacity={0.8}
              onPress={() => onOpenThread?.(item)}
              onLongPress={() => openActionsFor(item)}
            >
              <View style={styles.cardHeader}>
                <Text
                  style={[
                    styles.badge,
                    item.type === "offer"
                      ? styles.badgeOffer
                      : styles.badgeRequest,
                  ]}
                >
                  {item.type === "offer" ? "Offer" : "Request"}
                </Text>
                <Text style={styles.cardTime}>{item.createdAt}</Text>
              </View>
              <Text style={styles.cardTitle}>{item.title}</Text>
              <Text style={styles.cardDesc} numberOfLines={2}>
                {item.description}
              </Text>
              <View style={styles.cardFooter}>
                <Text style={styles.cardAuthor}>{item.authorHandle}</Text>
                <Text style={styles.cardStatus}>{item.status}</Text>
                {owner && <Text style={styles.ownerHint}>You</Text>}
              </View>
            </TouchableOpacity>
          );
        }}
        ListEmptyComponent={
          <View style={styles.empty}>
            <Ionicons
              name="swap-horizontal-outline"
              size={26}
              color="#4b5563"
            />
            <Text style={styles.emptyTitle}>No trades open yet.</Text>
            <Text style={styles.emptySubtitle}>
              Tap the plus to create an offer or request.
            </Text>
          </View>
        }
      />

      {/* Small create panel */}
      {composerOpen && (
        <View style={styles.composerPanel}>
          <View style={styles.modeRow}>
            <TouchableOpacity
              style={[
                styles.modeChip,
                mode === "offer" && styles.modeChipActive,
              ]}
              onPress={() => setMode("offer")}
            >
              <Text
                style={[
                  styles.modeChipText,
                  mode === "offer" && styles.modeChipTextActive,
                ]}
              >
                Offer
              </Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[
                styles.modeChip,
                mode === "request" && styles.modeChipActive,
              ]}
              onPress={() => setMode("request")}
            >
              <Text
                style={[
                  styles.modeChipText,
                  mode === "request" && styles.modeChipTextActive,
                ]}
              >
                Request
              </Text>
            </TouchableOpacity>
          </View>

          <TextInput
            style={styles.input}
            placeholder="Title"
            placeholderTextColor="#6b7280"
            value={title}
            onChangeText={setTitle}
          />
          <TextInput
            style={[styles.input, { marginTop: 6, height: 70 }]}
            placeholder="Description"
            placeholderTextColor="#6b7280"
            value={description}
            onChangeText={setDescription}
            multiline
          />

          <View style={styles.composerButtonsRow}>
            <TouchableOpacity
              style={styles.cancelButton}
              onPress={() => {
                setComposerOpen(false);
                setTitle("");
                setDescription("");
              }}
            >
              <Text style={styles.cancelButtonText}>Cancel</Text>
            </TouchableOpacity>
            <TouchableOpacity
              style={[
                styles.createButton,
                (!title.trim() || !description.trim()) && { opacity: 0.4 },
              ]}
              disabled={!title.trim() || !description.trim()}
              onPress={handleCreate}
              activeOpacity={0.8}
            >
              <Text style={styles.createButtonText}>
                {mode === "offer" ? "Create offer" : "Create request"}
              </Text>
            </TouchableOpacity>
          </View>
        </View>
      )}

      {/* Trade actions sheet (Edit / Delete) - only for owner */}
      {actionsTarget && isOwner(actionsTarget) && (
        <View style={styles.sheetBackdrop}>
          <TouchableOpacity
            style={styles.sheetBackdropTouch}
            activeOpacity={1}
            onPress={() => setActionsTarget(null)}
          />
          <View style={styles.sheet}>
            <Text style={styles.sheetTitle}>Trade actions</Text>

            {/* Edit fields */}
            <TextInput
              style={styles.input}
              placeholder="Title"
              placeholderTextColor="#6b7280"
              value={editDraftTitle}
              onChangeText={setEditDraftTitle}
            />
            <TextInput
              style={[styles.input, { marginTop: 6, height: 70 }]}
              placeholder="Description"
              placeholderTextColor="#6b7280"
              value={editDraftDescription}
              onChangeText={setEditDraftDescription}
              multiline
            />

            <TouchableOpacity
              style={[styles.sheetButton, { marginTop: 8 }]}
              onPress={handleSaveEdit}
            >
              <Ionicons
                name="pencil-outline"
                size={16}
                color="#e5e7eb"
                style={{ marginRight: 8 }}
              />
              <Text style={styles.sheetButtonText}>Save changes</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[
                styles.sheetButton,
                { backgroundColor: "rgba(248,113,113,0.15)", marginTop: 4 },
              ]}
              onPress={handleDelete}
            >
              <Ionicons
                name="trash-outline"
                size={16}
                color="#f87171"
                style={{ marginRight: 8 }}
              />
              <Text
                style={[styles.sheetButtonText, { color: "#fca5a5" }]}
              >
                Delete trade
              </Text>
            </TouchableOpacity>
          </View>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  topRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 12,
    paddingTop: 4,
    paddingBottom: 4,
  },
  filterRow: {
    flexDirection: "row",
    flex: 1,
  },
  plusButton: {
    width: 28,
    height: 28,
    borderRadius: 999,
    backgroundColor: "#22c55e",
    alignItems: "center",
    justifyContent: "center",
    marginLeft: 8,
  },
  filterChip: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    backgroundColor: "rgba(15,23,42,0.9)",
    marginRight: 4,
  },
  filterChipActive: {
    backgroundColor: "#22c55e",
  },
  filterChipText: { fontSize: 11, color: "#e5e7eb" },
  filterChipTextActive: { color: "#020617", fontWeight: "600" },
  list: {
    paddingHorizontal: 16,
    paddingTop: 6,
    paddingBottom: 80,
  },
  card: {
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderRadius: 12,
    marginBottom: 8,
    backgroundColor: "rgba(15,23,42,0.95)",
  },
  cardHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: 4,
  },
  badge: {
    fontSize: 10,
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 999,
    textTransform: "uppercase",
  },
  badgeOffer: {
    backgroundColor: "rgba(34,197,94,0.15)",
    color: "#4ade80",
  },
  badgeRequest: {
    backgroundColor: "rgba(59,130,246,0.15)",
    color: "#60a5fa",
  },
  cardTime: { fontSize: 10, color: "#6b7280" },
  cardTitle: {
    fontSize: 13,
    color: "#e5e7eb",
    fontWeight: "500",
  },
  cardDesc: { fontSize: 12, color: "#9ca3af", marginTop: 2 },
  cardFooter: {
    flexDirection: "row",
    alignItems: "center",
    marginTop: 6,
  },
  cardAuthor: { fontSize: 11, color: "#9ca3af" },
  cardStatus: {
    fontSize: 11,
    color: "#e5e7eb",
    marginLeft: 8,
    textTransform: "capitalize",
  },
  ownerHint: {
    fontSize: 10,
    color: "#f97316",
    marginLeft: "auto",
  },
  empty: {
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
  composerPanel: {
    position: "absolute",
    left: 0,
    right: 0,
    bottom: 0,
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: 16,
    backgroundColor: "#020617",
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: "rgba(148,163,184,0.4)",
  },
  modeRow: {
    flexDirection: "row",
    marginBottom: 6,
  },
  modeChip: {
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    backgroundColor: "rgba(15,23,42,0.9)",
    marginRight: 6,
  },
  modeChipActive: {
    backgroundColor: "#22c55e",
  },
  modeChipText: { fontSize: 11, color: "#e5e7eb" },
  modeChipTextActive: { color: "#020617", fontWeight: "600" },
  input: {
    fontSize: 12,
    color: "#e5e7eb",
    backgroundColor: "rgba(15,23,42,0.95)",
    borderRadius: 8,
    paddingHorizontal: 8,
    paddingVertical: 6,
  },
  composerButtonsRow: {
    flexDirection: "row",
    justifyContent: "flex-end",
    marginTop: 8,
  },
  cancelButton: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "rgba(148,163,184,0.2)",
    marginRight: 8,
  },
  cancelButtonText: { fontSize: 12, color: "#e5e7eb" },
  createButton: {
    borderRadius: 999,
    backgroundColor: "#22c55e",
    paddingHorizontal: 14,
    paddingVertical: 6,
  },
  createButtonText: {
    fontSize: 12,
    fontWeight: "600",
    color: "#020617",
  },
  sheetBackdrop: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: "flex-end",
  },
  sheetBackdropTouch: {
    flex: 1,
  },
  sheet: {
    paddingHorizontal: 16,
    paddingTop: 10,
    paddingBottom: 16,
    backgroundColor: "rgba(15,23,42,0.98)",
    borderTopLeftRadius: 16,
    borderTopRightRadius: 16,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: "rgba(148,163,184,0.4)",
  },
  sheetTitle: {
    fontSize: 13,
    color: "#9ca3af",
    marginBottom: 8,
  },
  sheetButton: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 8,
    paddingHorizontal: 8,
    borderRadius: 8,
  },
  sheetButtonText: {
    fontSize: 14,
    color: "#e5e7eb",
  },
});
