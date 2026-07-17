// src/developer/DeveloperFiles.tsx
import React, { useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Modal,
} from "react-native";

type FileKind = "folder" | "file";

export type DevFileItem = {
  id: string;
  name: string;
  kind: FileKind;
  ext?: "ts" | "tsx" | "js" | "py" | "html" | "json" | "md";
  size?: string;
};

type Props = {
  pathLabel: string; // e.g. "/src"
  items: DevFileItem[];

  onOpenFolder: (item: DevFileItem) => void;
  onGoUp: () => void;
  canGoUp: boolean;

  onSelectFile: (file: DevFileItem) => void;
  onCreateFolder: () => void;

  // context menu / toolbar actions
  onDelete: (item: DevFileItem) => void;
  onRename: (item: DevFileItem) => void;
  onCopy: (item: DevFileItem) => void;
  onCut: (item: DevFileItem) => void;
  onPaste: () => void;

  // drag & drop (move into folder)
  onDragStart: (item: DevFileItem) => void;
  onDropOnFolder: (targetFolder: DevFileItem) => void;

  canPaste: boolean;
};

export default function DeveloperFiles({
  pathLabel,
  items,
  onOpenFolder,
  onGoUp,
  canGoUp,
  onSelectFile,
  onCreateFolder,
  onDelete,
  onRename,
  onCopy,
  onCut,
  onPaste,
  onDragStart,
  onDropOnFolder,
  canPaste,
}: Props) {
  const [contextItem, setContextItem] = useState<DevFileItem | null>(null);
  const [lastSelectedItem, setLastSelectedItem] = useState<DevFileItem | null>(
    null
  );

  function openContext(item: DevFileItem) {
    setContextItem(item);
  }

  function closeContext() {
    setContextItem(null);
  }

  function handlePress(item: DevFileItem) {
    setLastSelectedItem(item);
    if (item.kind === "folder") onOpenFolder(item);
    else onSelectFile(item);
  }

  function handleLongPress(item: DevFileItem) {
    setLastSelectedItem(item);
    openContext(item);
  }

  function renderItem({ item }: { item: DevFileItem }) {
    const isFolder = item.kind === "folder";

    return (
      <TouchableOpacity
        style={styles.row}
        activeOpacity={0.7}
        onPress={() => handlePress(item)}
        onLongPress={() => handleLongPress(item)}
        delayLongPress={300}
        onPressIn={() => onDragStart(item)}
      >
        <View style={styles.iconBox}>
          <Text style={styles.iconText}>{isFolder ? "📁" : "📄"}</Text>
        </View>

        <View style={styles.main}>
          <View style={styles.nameRow}>
            <Text numberOfLines={1} style={styles.name}>
              {item.name}
            </Text>
            {item.size && <Text style={styles.size}>{item.size}</Text>}
          </View>

          <Text style={styles.meta} numberOfLines={1}>
            {isFolder
              ? "Folder"
              : item.ext
              ? `${item.ext.toUpperCase()} file`
              : "File"}
          </Text>
        </View>
      </TouchableOpacity>
    );
  }

  function renderContextMenu() {
    if (!contextItem) return null;

    const isFolder = contextItem.kind === "folder";

    return (
      <Modal
        visible
        transparent
        animationType="fade"
        onRequestClose={closeContext}
      >
        <View style={styles.menuOverlay}>
          <View style={styles.menu}>
            <Text style={styles.menuTitle}>{contextItem.name}</Text>

            <TouchableOpacity
              style={styles.menuItem}
              onPress={() => {
                onRename(contextItem);
                closeContext();
              }}
            >
              <Text style={styles.menuItemText}>Rename</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.menuItem}
              onPress={() => {
                onDelete(contextItem);
                closeContext();
              }}
            >
              <Text style={[styles.menuItemText, styles.menuDanger]}>
                Delete
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.menuItem}
              onPress={() => {
                onCopy(contextItem);
                closeContext();
              }}
            >
              <Text style={styles.menuItemText}>Copy</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={styles.menuItem}
              onPress={() => {
                onCut(contextItem);
                closeContext();
              }}
            >
              <Text style={styles.menuItemText}>Cut</Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[styles.menuItem, !canPaste && styles.menuItemDisabled]}
              disabled={!canPaste}
              onPress={() => {
                if (canPaste) {
                  onPaste();
                  closeContext();
                }
              }}
            >
              <Text
                style={[
                  styles.menuItemText,
                  !canPaste && styles.menuItemTextDisabled,
                ]}
              >
                Paste
              </Text>
            </TouchableOpacity>

            {isFolder && (
              <TouchableOpacity
                style={styles.menuItem}
                onPress={() => {
                  onDropOnFolder(contextItem);
                  closeContext();
                }}
              >
                <Text style={styles.menuItemText}>Move here</Text>
              </TouchableOpacity>
            )}

            <TouchableOpacity style={styles.menuCancel} onPress={closeContext}>
              <Text style={styles.menuCancelText}>Cancel</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    );
  }

  function handleToolbarCopy() {
    if (!lastSelectedItem) return;
    onCopy(lastSelectedItem);
  }

  function handleToolbarCut() {
    if (!lastSelectedItem) return;
    onCut(lastSelectedItem);
  }

  function handleToolbarPaste() {
    if (!canPaste) return;
    onPaste();
  }

  return (
    <View style={styles.root}>
      <View style={styles.pathRow}>
        <Text style={styles.pathText}>{pathLabel}</Text>

        <View style={styles.pathRight}>
          {canGoUp && (
            <TouchableOpacity onPress={onGoUp}>
              <Text style={styles.upText}>Up</Text>
            </TouchableOpacity>
          )}

          {/* Copy / Cut / Paste toolbar */}
          <View style={styles.toolbarGroup}>
            <TouchableOpacity
              style={[
                styles.toolbarButton,
                !lastSelectedItem && styles.toolbarButtonDisabled,
              ]}
              disabled={!lastSelectedItem}
              onPress={handleToolbarCopy}
            >
              <Text
                style={[
                  styles.toolbarText,
                  !lastSelectedItem && styles.toolbarTextDisabled,
                ]}
              >
                Copy
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[
                styles.toolbarButton,
                !lastSelectedItem && styles.toolbarButtonDisabled,
              ]}
              disabled={!lastSelectedItem}
              onPress={handleToolbarCut}
            >
              <Text
                style={[
                  styles.toolbarText,
                  !lastSelectedItem && styles.toolbarTextDisabled,
                ]}
              >
                Cut
              </Text>
            </TouchableOpacity>

            <TouchableOpacity
              style={[
                styles.toolbarButton,
                !canPaste && styles.toolbarButtonDisabled,
              ]}
              disabled={!canPaste}
              onPress={handleToolbarPaste}
            >
              <Text
                style={[
                  styles.toolbarText,
                  !canPaste && styles.toolbarTextDisabled,
                ]}
              >
                Paste
              </Text>
            </TouchableOpacity>
          </View>

          <TouchableOpacity
            style={styles.newFolderButton}
            onPress={onCreateFolder}
          >
            <Text style={styles.newFolderPlus}>＋</Text>
            <Text style={styles.newFolderText}>Folder</Text>
          </TouchableOpacity>
        </View>
      </View>

      <FlatList
        data={items}
        keyExtractor={(item) => item.id}
        renderItem={renderItem}
        ItemSeparatorComponent={() => <View style={styles.separator} />}
        contentContainerStyle={styles.listContent}
      />

      {renderContextMenu()}
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    paddingTop: 4,
  },
  pathRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 8,
  },
  pathText: {
    color: "#9ca3af",
    fontSize: 11,
  },
  pathRight: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  upText: {
    color: "#22d3ee",
    fontSize: 11,
  },
  toolbarGroup: {
    flexDirection: "row",
    alignItems: "center",
    gap: 4,
  },
  toolbarButton: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    backgroundColor: "rgba(15,23,42,0.98)",
  },
  toolbarButtonDisabled: {
    opacity: 0.4,
  },
  toolbarText: {
    color: "#e5e7eb",
    fontSize: 10,
  },
  toolbarTextDisabled: {
    color: "#6b7280",
  },
  newFolderButton: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    backgroundColor: "rgba(15,23,42,0.98)",
  },
  newFolderPlus: {
    color: "#e5e7eb",
    fontSize: 12,
    marginRight: 4,
  },
  newFolderText: {
    color: "#e5e7eb",
    fontSize: 11,
  },
  listContent: {
    paddingVertical: 4,
  },
  row: {
    flexDirection: "row",
    alignItems: "center",
    paddingVertical: 8,
  },
  iconBox: {
    width: 32,
    alignItems: "center",
    marginRight: 10,
  },
  iconText: {
    fontSize: 18,
  },
  main: {
    flex: 1,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(31,41,55,0.9)",
    paddingBottom: 8,
  },
  nameRow: {
    flexDirection: "row",
    alignItems: "center",
  },
  name: {
    flex: 1,
    color: "#e5e7eb",
    fontSize: 13,
    fontWeight: "500",
  },
  size: {
    color: "#6b7280",
    fontSize: 11,
    marginLeft: 8,
  },
  meta: {
    color: "#6b7280",
    fontSize: 11,
    marginTop: 2,
  },
  separator: {
    height: 4,
  },
  menuOverlay: {
    flex: 1,
    backgroundColor: "rgba(0,0,0,0.6)",
    justifyContent: "center",
    alignItems: "center",
  },
  menu: {
    width: 220,
    borderRadius: 12,
    padding: 12,
    backgroundColor: "#020617",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
  },
  menuTitle: {
    color: "#e5e7eb",
    fontSize: 13,
    fontWeight: "600",
    marginBottom: 8,
  },
  menuItem: {
    paddingVertical: 6,
  },
  menuItemDisabled: {
    opacity: 0.4,
  },
  menuItemText: {
    color: "#e5e7eb",
    fontSize: 12,
  },
  menuItemTextDisabled: {
    color: "#6b7280",
  },
  menuDanger: {
    color: "#f97373",
  },
  menuCancel: {
    marginTop: 8,
    paddingVertical: 6,
    alignItems: "center",
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: "rgba(31,41,55,0.9)",
  },
  menuCancelText: {
    color: "#9ca3af",
    fontSize: 12,
  },
});
