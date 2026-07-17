// src/member/chat/partials/MessageActionsSheet.tsx
import React, { useMemo } from "react";
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
} from "react-native";
import type { Message } from "@/src/member/chat/MessageRow";

type Props = {
  visible: boolean;
  message: Message | null;
  currentUserId: string;

  onClose: () => void;

  onDeleteForEveryone: (m: Message) => void;
  onDeleteForMe: (m: Message) => void;
  onEditMessage: (m: Message) => void;
  onForwardMessage: (m: Message) => void;
  onPinMessage: (m: Message) => void;
  onSaveMessage: (m: Message) => void;
  onCopyText: (m: Message) => void;
  onOpenStickerPicker: (m: Message) => void;
  onOpenEmojiPicker: (m: Message) => void;
  onReportMessage: (m: Message) => void;
};

type ActionKey =
  | "emoji"
  | "forward"
  | "pin"
  | "copy"
  | "save"
  | "edit"
  | "sticker"
  | "deleteForEveryone"
  | "deleteForMe"
  | "report";

type ActionItem = {
  key: ActionKey;
  label: string;
  danger?: boolean;
};

export function MessageActionsSheet({
  visible,
  message,
  currentUserId,
  onClose,
  onDeleteForEveryone,
  onDeleteForMe,
  onEditMessage,
  onForwardMessage,
  onPinMessage,
  onSaveMessage,
  onCopyText,
  onOpenStickerPicker,
  onOpenEmojiPicker,
  onReportMessage,
}: Props) {
  if (!visible || !message) return null;

  const actions = useMemo<ActionItem[]>(() => {
    const isOwner = message.userId === currentUserId;

    const within5min =
      !!message.createdAt &&
      Date.now() - message.createdAt.getTime() < 5 * 60 * 1000;

    if (isOwner) {
      const base: ActionItem[] = [
        { key: "emoji", label: "Emoji" },
        { key: "forward", label: "Forward" },
        { key: "pin", label: "Pin" },
        { key: "copy", label: "Copy" },
        { key: "save", label: "Save" },
        { key: "sticker", label: "Sticker" },
      ];

      if (within5min) {
        base.push({ key: "edit", label: "Edit" });
      }

      return [{ key: "deleteForEveryone", label: "Delete", danger: true }, ...base];
    }

    return [
      { key: "report", label: "Report", danger: true },
      { key: "deleteForMe", label: "Delete", danger: true },
      { key: "emoji", label: "Emoji" },
      { key: "sticker", label: "Sticker" },
      { key: "copy", label: "Copy" },
      { key: "save", label: "Save" },
      { key: "forward", label: "Forward" },
      { key: "pin", label: "Pin" },
    ];
  }, [message, currentUserId]);

  const handlePress = (key: ActionKey) => {
    switch (key) {
      case "emoji":
        onOpenEmojiPicker(message);
        break;
      case "forward":
        onForwardMessage(message);
        break;
      case "pin":
        onPinMessage(message);
        break;
      case "copy":
        onCopyText(message);
        break;
      case "save":
        onSaveMessage(message);
        break;
      case "edit":
        onEditMessage(message);
        break;
      case "sticker":
        onOpenStickerPicker(message);
        break;
      case "deleteForEveryone":
        onDeleteForEveryone(message);
        break;
      case "deleteForMe":
        onDeleteForMe(message);
        break;
      case "report":
        onReportMessage(message);
        break;
    }

    onClose();
  };

  return (
    <View style={styles.overlay}>
      <View style={styles.sheet}>
        {message.text ? (
          <View style={styles.previewBox}>
            <Text numberOfLines={2} style={styles.previewText}>
              {message.text}
            </Text>
          </View>
        ) : null}

        {actions.map((a) => (
          <TouchableOpacity
            key={a.key}
            style={styles.actionRow}
            activeOpacity={0.7}
            onPress={() => handlePress(a.key)}
          >
            <Text
              style={[
                styles.actionRowText,
                a.danger && styles.actionRowTextDanger,
              ]}
            >
              {a.label}
            </Text>
          </TouchableOpacity>
        ))}

        <TouchableOpacity
          style={[styles.actionRow, styles.cancelRow]}
          activeOpacity={0.7}
          onPress={onClose}
        >
          <Text style={[styles.actionRowText, { fontWeight: "600" }]}>
            Cancel
          </Text>
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
  previewBox: {
    marginBottom: 10,
    paddingHorizontal: 10,
    paddingVertical: 8,
    borderRadius: 10,
    backgroundColor: "rgba(15,23,42,0.95)",
  },
  previewText: {
    fontSize: 13,
    color: "#e5e7eb",
  },
  actionRow: {
    paddingVertical: 10,
    paddingHorizontal: 12,
    borderRadius: 10,
    backgroundColor: "#020617",
    borderWidth: 1,
    borderColor: "rgba(51,65,85,0.7)",
    marginTop: 6,
  },
  cancelRow: {
    marginTop: 10,
  },
  actionRowText: {
    fontSize: 14,
    color: "#e5e7eb",
  },
  actionRowTextDanger: {
    color: "#f97373",
  },
});
