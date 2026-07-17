// src/member/chat/ChatToolsPanel.tsx
import React from "react";
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  Modal,
} from "react-native";

type ToolId =
  | "camera"
  | "cameraVideo"
  | "voice"
  | "sticker"
  | "gallery"
  | "audio"
  | "document"
  | "contact"
  | "poll"
  | "links"
  | "event"
  | "emoji"; 

type ToolAction = {
  tool: ToolId;
  type: "select";
};

type Props = {
  visible: boolean;
  onClose: () => void;
  onToolAction?: (action: ToolAction) => void;
};

const TOOLS: Array<{
  id: ToolId;
  label: string;
  glyph: string;
  group: "capture" | "express" | "attach";
}> = [
  { id: "camera", label: "Camera", glyph: "📷", group: "capture" },
  { id: "cameraVideo", label: "Video camera", glyph: "📹", group: "capture" },
  { id: "voice", label: "Voice record", glyph: "🎙️", group: "capture" },
  { id: "sticker", label: "Sticker", glyph: "🩹", group: "express" },
  { id: "gallery", label: "Gallery", glyph: "🖼️", group: "express" },
  { id: "audio", label: "Audio", glyph: "🎧", group: "express" },

  // put emoji in Attach
  { id: "emoji", label: "Emoji", glyph: "😊", group: "attach" },

  { id: "document", label: "Document", glyph: "📄", group: "attach" },
  { id: "contact", label: "Contact", glyph: "👤", group: "attach" },
  { id: "poll", label: "Poll", glyph: "📊", group: "attach" },
  { id: "links", label: "Links", glyph: "🔗", group: "attach" },
  { id: "event", label: "Event", glyph: "📅", group: "attach" },
];

const GROUP_LABELS: Record<"capture" | "express" | "attach", string> = {
  capture: "Capture",
  express: "Express",
  attach: "Attach",
};

export default function ChatToolsPanel({
  visible,
  onClose,
  onToolAction,
}: Props) {
  if (!visible) return null;

  const handleTool = (toolId: ToolId) => {
    onToolAction?.({ tool: toolId, type: "select" });
    onClose();
  };

  const groups: Array<"capture" | "express" | "attach"> = [
    "capture",
    "express",
    "attach",
  ];

  return (
    <Modal
      transparent
      animationType="fade"
      visible={visible}
      onRequestClose={onClose}
    >
      <View style={styles.backdrop}>
        <View style={styles.panel}>
          {/* Header */}
          <View style={styles.headerRow}>
            <View style={styles.headerLeft}>
              <View style={styles.headerDot} />
              <Text style={styles.headerTitle}>Neural Rail</Text>
            </View>
            <View style={styles.headerRight}>
              <Text style={styles.headerBadge}>{TOOLS.length} nodes</Text>
              <TouchableOpacity onPress={onClose} style={styles.headerCloseBtn}>
                <Text style={styles.headerCloseText}>✕</Text>
              </TouchableOpacity>
            </View>
          </View>

          {/* Groups */}
          {groups.map((groupKey) => {
            const groupTools = TOOLS.filter((t) => t.group === groupKey);
            return (
              <View key={groupKey} style={styles.groupBlock}>
                {/* Group header */}
                <View style={styles.groupHeader}>
                  <View style={styles.groupHeaderLeft}>
                    <View
                      style={[
                        styles.groupBar,
                        groupKey === "capture"
                          ? styles.groupBarCapture
                          : groupKey === "express"
                          ? styles.groupBarExpress
                          : styles.groupBarAttach,
                      ]}
                    />
                    <Text style={styles.groupLabel}>
                      {GROUP_LABELS[groupKey]}
                    </Text>
                  </View>
                  <Text style={styles.groupCount}>
                    {groupTools.length} nodes
                  </Text>
                </View>

                {/* Tool grid */}
                <View style={styles.toolsGrid}>
                  {groupTools.map((tool) => (
                    <TouchableOpacity
                      key={tool.id}
                      style={styles.toolButton}
                      activeOpacity={0.8}
                      onPress={() => handleTool(tool.id)}
                    >
                      <Text style={styles.toolGlyph}>{tool.glyph}</Text>
                      <Text style={styles.toolLabel}>{tool.label}</Text>
                    </TouchableOpacity>
                  ))}
                </View>
              </View>
            );
          })}
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: "rgba(15,23,42,0.8)",
    justifyContent: "flex-end",
    paddingBottom: 90,
  },
  panel: {
    alignSelf: "center",
    width: 280,
    backgroundColor: "#020617",
    borderRadius: 20,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.4)",
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  headerRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 10,
    paddingBottom: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(56,189,248,0.4)",
  },
  headerLeft: {
    flexDirection: "row",
    alignItems: "center",
  },
  headerDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: "#56bdf0",
    marginRight: 6,
  },
  headerTitle: {
    fontSize: 13,
    fontWeight: "700",
    color: "#e5e7eb",
    letterSpacing: 0.3,
  },
  headerRight: {
    flexDirection: "row",
    alignItems: "center",
  },
  headerBadge: {
    fontSize: 9,
    color: "#a5b4fc",
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "rgba(129,140,248,0.5)",
    marginRight: 6,
    textTransform: "uppercase",
  },
  headerCloseBtn: {
    width: 24,
    height: 24,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.4)",
    alignItems: "center",
    justifyContent: "center",
  },
  headerCloseText: {
    fontSize: 12,
    color: "#9ca3af",
  },
  groupBlock: {
    marginTop: 8,
  },
  groupHeader: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 6,
  },
  groupHeaderLeft: {
    flexDirection: "row",
    alignItems: "center",
  },
  groupBar: {
    width: 20,
    height: 2,
    borderRadius: 999,
    marginRight: 6,
  },
  groupBarCapture: {
    backgroundColor: "#56bdf0",
  },
  groupBarExpress: {
    backgroundColor: "#ec489f",
  },
  groupBarAttach: {
    backgroundColor: "#10b981",
  },
  groupLabel: {
    fontSize: 10,
    color: "#9ca3af",
    textTransform: "uppercase",
    letterSpacing: 0.2,
  },
  groupCount: {
    fontSize: 9,
    color: "#9ca3af",
    opacity: 0.8,
  },
  toolsGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    justifyContent: "space-between",
  },
  toolButton: {
    width: "31%",
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.3)",
    paddingVertical: 8,
    alignItems: "center",
    marginBottom: 6,
    backgroundColor: "#020617",
  },
  toolGlyph: {
    fontSize: 18,
    marginBottom: 2,
  },
  toolLabel: {
    fontSize: 9,
    color: "#e5e7eb",
    textAlign: "center",
    fontWeight: "600",
  },
});
