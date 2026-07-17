// src/member/work/code/FilesPanel.tsx
import React, { useState } from "react";
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Modal,
} from "react-native";

export type FileItem = {
  name: string;          // leaf name
  tag: string;
  kind: "file" | "folder";
  path: string;          // parent folder, e.g. "/" or "/src"
};

type NewItemType = "file" | "folder";

type Props = {
  files: FileItem[];                       // only items in currentFolder
  search: string;
  onChangeSearch: (value: string) => void;
  borderColor: string;
  onAddItem?: (type: NewItemType, name: string) => void;

  currentFolder: string;
  onChangeFolder: (next: string) => void;

  selectedName?: string | null;           // open file
  actionTargetName?: string | null;       // copy/cut/delete target

  onSelect?: (name: string) => void;      // tap to open
  onLongSelect?: (name: string) => void;  // long-press to target
  onCopy?: () => void;
  onPaste?: () => void;
  onCut?: () => void;
  onDelete?: () => void;
};

function parentFolder(path: string): string {
  if (path === "/" || path === "") return "/";
  const parts = path.split("/").filter(Boolean);
  parts.pop();
  return parts.length === 0 ? "/" : `/${parts.join("/")}`;
}

export default function FilesPanel({
  files,
  search,
  onChangeSearch,
  borderColor,
  onAddItem,
  currentFolder,
  onChangeFolder,
  selectedName,
  actionTargetName,
  onSelect,
  onLongSelect,
  onCopy,
  onPaste,
  onCut,
  onDelete,
}: Props) {
  const [showNewModal, setShowNewModal] = useState(false);
  const [newType, setNewType] = useState<NewItemType>("file");
  const [newName, setNewName] = useState("");

  function openNewModal() {
    setNewType("file");
    setNewName("");
    setShowNewModal(true);
  }

  function closeNewModal() {
    setShowNewModal(false);
  }

  function handleCreate() {
    if (!newName.trim()) return;
    onAddItem?.(newType, newName.trim());
    setShowNewModal(false);
    setNewName("");
  }

  const filtered = files.filter((item) =>
    item.name.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <>
      <View
        style={[
          styles.filesHeader,
          { borderBottomColor: borderColor },
        ]}
      >
        <View style={styles.folderTitleRow}>
          {currentFolder !== "/" && (
            <TouchableOpacity
              onPress={() => onChangeFolder(parentFolder(currentFolder))}
              style={styles.upBtn}
              activeOpacity={0.7}
            >
              <Text style={styles.upBtnText}>↑</Text>
            </TouchableOpacity>
          )}
          <Text style={styles.filesTitle}>
            {currentFolder === "/" ? "ROOT" : currentFolder}
          </Text>
        </View>

        <View style={styles.filesToolbar}>
          <TouchableOpacity
            activeOpacity={0.7}
            onPress={onCopy}
            disabled={!onCopy}
          >
            <Text style={styles.toolbarText}>Copy</Text>
          </TouchableOpacity>
          <TouchableOpacity
            activeOpacity={0.7}
            onPress={onPaste}
            disabled={!onPaste}
          >
            <Text style={styles.toolbarText}>Paste</Text>
          </TouchableOpacity>
          <TouchableOpacity
            activeOpacity={0.7}
            onPress={onCut}
            disabled={!onCut}
          >
            <Text style={styles.toolbarText}>Cut</Text>
          </TouchableOpacity>
          <TouchableOpacity
            activeOpacity={0.7}
            onPress={onDelete}
            disabled={!onDelete}
          >
            <Text style={styles.toolbarText}>Delete</Text>
          </TouchableOpacity>
          <TouchableOpacity
            onPress={openNewModal}
            activeOpacity={0.7}
            hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
          >
            <Text style={styles.filesPlus}>+</Text>
          </TouchableOpacity>
        </View>
      </View>

      <View style={styles.filesSearchRow}>
        <View
          style={[
            styles.filesSearchShell,
            { borderColor },
          ]}
        >
          <Text style={styles.filesSearchIcon}>⌕</Text>
          <TextInput
            value={search}
            onChangeText={onChangeSearch}
            placeholder="search files…"
            placeholderTextColor="#6b7280"
            style={styles.filesSearchInput}
          />
        </View>
      </View>

      <FlatList
        data={filtered}
        keyExtractor={(item) => `${item.path}/${item.name}`}
        style={styles.filesList}
        contentContainerStyle={{
          paddingHorizontal: 8,
          paddingBottom: 8,
        }}
        renderItem={({ item }) => {
          const isFolder = item.kind === "folder";
          const isOpen = selectedName === item.name && !isFolder;
          const isActionTarget =
            actionTargetName === item.name && !isFolder;

          return (
            <TouchableOpacity
              onPress={() => {
                if (isFolder) {
                  const next =
                    currentFolder === "/"
                      ? `/${item.name}`
                      : `${currentFolder}/${item.name}`;
                  onChangeFolder(next);
                } else {
                  onSelect?.(item.name);       // open in editor
                }
              }}
              onLongPress={() => {
                if (!isFolder) {
                  onLongSelect?.(item.name);   // target for actions
                }
              }}
              delayLongPress={300}
              activeOpacity={0.8}
            >
              <View
                style={[
                  styles.fileRow,
                  (isOpen || isActionTarget) && styles.fileRowActive,
                ]}
              >
                <View style={styles.selectCircle}>
                  {isActionTarget && <View style={styles.selectDot} />}
                </View>

                <Text
                  style={[
                    styles.fileName,
                    (isOpen || isActionTarget) && styles.fileNameActive,
                  ]}
                  numberOfLines={1}
                >
                  {isFolder ? `📁 ${item.name}` : item.name}
                </Text>
                <View style={styles.fileTag}>
                  <Text style={styles.fileTagText}>{item.tag}</Text>
                </View>
              </View>
            </TouchableOpacity>
          );
        }}
      />

      {/* New file/folder modal */}
      <Modal
        visible={showNewModal}
        transparent
        animationType="fade"
        onRequestClose={closeNewModal}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>Create</Text>

            <View style={styles.modalTypeRow}>
              <TouchableOpacity
                onPress={() => setNewType("file")}
                style={[
                  styles.modalTypePill,
                  newType === "file" && styles.modalTypePillActive,
                ]}
              >
                <Text
                  style={[
                    styles.modalTypeText,
                    newType === "file" && styles.modalTypeTextActive,
                  ]}
                >
                  File
                </Text>
              </TouchableOpacity>
              <TouchableOpacity
                onPress={() => setNewType("folder")}
                style={[
                  styles.modalTypePill,
                  newType === "folder" && styles.modalTypePillActive,
                ]}
              >
                <Text
                  style={[
                    styles.modalTypeText,
                    newType === "folder" && styles.modalTypeTextActive,
                  ]}
                >
                  Folder
                </Text>
              </TouchableOpacity>
            </View>

            <View style={styles.modalField}>
              <Text style={styles.modalLabel}>
                {newType === "file" ? "File name" : "Folder name"}
              </Text>
              <TextInput
                value={newName}
                onChangeText={setNewName}
                placeholder={
                  newType === "file" ? "e.g. app" : "e.g. components"
                }
                placeholderTextColor="#6b7280"
                style={styles.modalInput}
                autoCapitalize="none"
              />
            </View>

            <View style={styles.modalActions}>
              <TouchableOpacity
                onPress={closeNewModal}
                style={styles.modalCancel}
                activeOpacity={0.8}
              >
                <Text style={styles.modalCancelText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity
                onPress={handleCreate}
                style={[
                  styles.modalCreate,
                  !newName.trim() && { opacity: 0.5 },
                ]}
                activeOpacity={0.8}
                disabled={!newName.trim()}
              >
                <Text style={styles.modalCreateText}>Create</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
    </>
  );
}

const styles = StyleSheet.create({
  filesHeader: {
    paddingHorizontal: 10,
    paddingVertical: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  folderTitleRow: {
    flexDirection: "row",
    alignItems: "center",
    columnGap: 6,
  },
  upBtn: {
    width: 18,
    height: 18,
    borderRadius: 9,
    borderWidth: 1,
    borderColor: "rgba(55,65,81,0.9)",
    alignItems: "center",
    justifyContent: "center",
  },
  upBtnText: {
    fontSize: 11,
    color: "#9ca3af",
  },
  filesTitle: {
    fontSize: 11,
    color: "#9ca3af",
    letterSpacing: 1.2,
  },
  filesToolbar: {
    flexDirection: "row",
    alignItems: "center",
    columnGap: 10,
  },
  toolbarText: {
    fontSize: 10,
    color: "#9ca3af",
  },
  filesPlus: {
    fontSize: 16,
    color: "#e5e7eb",
    paddingHorizontal: 2,
  },

  filesSearchRow: {
    paddingHorizontal: 10,
    paddingVertical: 6,
  },
  filesSearchShell: {
    flexDirection: "row",
    alignItems: "center",
    borderRadius: 999,
    borderWidth: 1,
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  filesSearchIcon: { fontSize: 11, color: "#6b7280", marginRight: 4 },
  filesSearchInput: {
    flex: 1,
    fontSize: 11,
    color: "#e5e7eb",
    paddingVertical: 0,
    paddingHorizontal: 0,
  },

  filesList: {
    maxHeight: 150,
  },
  fileRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 10,
  },
  fileRowActive: {
    backgroundColor: "rgba(34,197,94,0.12)",
    borderWidth: 1,
    borderColor: "rgba(34,197,94,0.6)",
  },
  selectCircle: {
    width: 14,
    height: 14,
    borderRadius: 7,
    borderWidth: 1,
    borderColor: "rgba(55,65,81,0.9)",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 6,
  },
  selectDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: "#22c55e",
  },
  fileName: {
    flex: 1,
    fontSize: 11,
    color: "#9ca3af",
  },
  fileNameActive: {
    color: "#e5e7eb",
  },
  fileTag: {
    paddingHorizontal: 6,
    paddingVertical: 2,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    backgroundColor: "rgba(15,23,42,0.95)",
  },
  fileTagText: {
    fontSize: 9,
    color: "#6b7280",
    letterSpacing: 1,
  },

  modalOverlay: {
    flex: 1,
    backgroundColor: "rgba(15,23,42,0.8)",
    justifyContent: "center",
    alignItems: "center",
  },
  modalCard: {
    width: "85%",
    borderRadius: 14,
    padding: 14,
    backgroundColor: "#020617",
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
  },
  modalTitle: {
    fontSize: 14,
    fontWeight: "600",
    color: "#e5e7eb",
    marginBottom: 10,
  },
  modalTypeRow: {
    flexDirection: "row",
    marginBottom: 10,
    columnGap: 8,
  },
  modalTypePill: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(55,65,81,0.9)",
    backgroundColor: "rgba(15,23,42,1)",
  },
  modalTypePillActive: {
    borderColor: "#22d3ee",
    backgroundColor: "rgba(34,211,238,0.12)",
  },
  modalTypeText: {
    fontSize: 11,
    color: "#9ca3af",
  },
  modalTypeTextActive: {
    color: "#e5e7eb",
    fontWeight: "600",
  },
  modalField: {
    marginBottom: 12,
  },
  modalLabel: {
    fontSize: 11,
    color: "#9ca3af",
    marginBottom: 4,
  },
  modalInput: {
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    paddingHorizontal: 8,
    paddingVertical: 6,
    fontSize: 12,
    color: "#e5e7eb",
    backgroundColor: "rgba(15,23,42,0.98)",
  },
  modalActions: {
    flexDirection: "row",
    justifyContent: "flex-end",
    columnGap: 8,
  },
  modalCancel: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(55,65,81,0.9)",
    backgroundColor: "rgba(15,23,42,1)",
  },
  modalCancelText: {
    fontSize: 11,
    color: "#9ca3af",
  },
  modalCreate: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#22c55e",
    backgroundColor: "#14532d",
  },
  modalCreateText: {
    fontSize: 11,
    color: "#bbf7d0",
    textTransform: "uppercase",
    letterSpacing: 1,
  },
});
