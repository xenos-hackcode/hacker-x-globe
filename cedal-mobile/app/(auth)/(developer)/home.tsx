// app/(auth)/(developer)/home.tsx
import { logout } from "@/src/api/auth";
import DevAssistantBubble from "@/src/developer/DevAssistantBubble";
import DevAssistantPanel from "@/src/developer/DevAssistantPanel";
import DeveloperEditor from "@/src/developer/DeveloperEditor";
import DeveloperFiles, { DevFileItem } from "@/src/developer/DeveloperFiles";
import * as Notifications from "expo-notifications";
import DeveloperView, { CedalCommand } from "@/src/developer/DeveloperView";
import { useUserProfile } from "@/src/hooks/useUserProfile";
import { useRouter } from "expo-router";
import React, { useRef, useState } from "react";
import {
  Alert,
  Animated,
  PanResponder,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  Vibration,
  ScrollView,
} from "react-native";

type DevTab = "work" | "files" | "view";
type CodeLanguage = "TypeScript" | "JavaScript" | "Python" | "HTML" | "JSON";

type NodeKind = "folder" | "file";

type FileNode = {
  id: string;
  kind: "file";
  name: string;
  parentId: string | null;
  language: CodeLanguage;
  ext: "ts" | "tsx" | "js" | "py" | "html" | "json" | "md";
  code: string;
  size?: string;
};

type FolderNode = {
  id: string;
  kind: "folder";
  name: string;
  parentId: string | null;
};

type FsNode = FileNode | FolderNode;

type ClipboardState = {
  mode: "copy" | "cut";
  nodeId: string;
} | null;

const ROOT_ID = "root";

// type guards so TS knows which node is which
function isFolderNode(n: FsNode): n is FolderNode {
  return n.kind === "folder";
}
function isFileNode(n: FsNode): n is FileNode {
  return n.kind === "file";
}

function makeFolder(name: string, parentId: string | null): FolderNode {
  return {
    id: `folder-${Date.now()}-${Math.random()}`,
    kind: "folder",
    name,
    parentId,
  };
}

function makeFile(
  name: string,
  parentId: string | null,
  language: CodeLanguage,
  code: string
): FileNode {
  const ext = (name.split(".").pop() || "tsx") as FileNode["ext"];
  return {
    id: `file-${Date.now()}-${Math.random()}`,
    kind: "file",
    name,
    parentId,
    language,
    ext,
    code,
    size: `${Math.max(1, Math.round(code.length / 50))} KB`,
  };
}

export default function DeveloperHome() {
  const router = useRouter();
  const [activeTab, setActiveTab] = useState<DevTab>("work");
  const [showAssistant, setShowAssistant] = useState(false);
  const { user } = useUserProfile();

  // editor state
  const [fileName, setFileName] = useState("main.tsx");
  const [language, setLanguage] = useState<CodeLanguage>("TypeScript");
  const [code, setCode] = useState("");
  const [currentFileId, setCurrentFileId] = useState<string | null>(null);

  // filesystem
  const [nodes, setNodes] = useState<FsNode[]>([
    { id: ROOT_ID, kind: "folder", name: "/", parentId: null },
    makeFile("main.tsx", ROOT_ID, "TypeScript", ""),
  ]);
  const [currentFolderId, setCurrentFolderId] = useState<string>(ROOT_ID);

  // clipboard + drag
  const [clipboard, setClipboard] = useState<ClipboardState>(null);
  const [draggingId, setDraggingId] = useState<string | null>(null);

  // rename
  const [renameTarget, setRenameTarget] = useState<FsNode | null>(null);
  const [renameValue, setRenameValue] = useState("");

  // Cedal OPEN_APP from commands
  const [pendingAppOpen, setPendingAppOpen] = useState<string | null>(null);

  // BUG / matrix overlay
  const [bugVisible, setBugVisible] = useState(false);
  const [bugText, setBugText] = useState<string>("SYSTEM BUG");

  // helper: safely get folder by id
  function getFolderById(id: string | null | undefined): FolderNode | undefined {
    if (!id) return undefined;
    const node = nodes.find((n) => n.id === id);
    return node && isFolderNode(node) ? node : undefined;
  }

  // current folder + children
  const currentFolder = getFolderById(currentFolderId)!;
  const children = nodes.filter((n) => n.parentId === currentFolderId);

  const visibleItems: DevFileItem[] = children.map((n) => ({
    id: n.id,
    name: n.name,
    kind: n.kind,
    ext: isFileNode(n) ? n.ext : undefined,
    size: isFileNode(n) ? n.size : undefined,
  }));

  // path helpers
  function buildPathLabel(folderId: string): string {
    let curr = getFolderById(folderId);
    const parts: string[] = [];

    while (curr) {
      if (curr.name !== "/") {
        parts.push(curr.name);
      }
      if (!curr.parentId) break;
      curr = getFolderById(curr.parentId);
    }

    parts.reverse();
    const label = "/" + parts.join("/");
    return label === "/" ? "/" : label;
  }

  function buildFolderPath(folder: FolderNode): string {
    if (folder.id === ROOT_ID) return "/";
    const parts: string[] = [folder.name];
    let curr: FolderNode | undefined = folder;

    while (curr && curr.parentId) {
      const parent = getFolderById(curr.parentId);
      if (!parent) break;
      if (parent.id !== ROOT_ID) {
        parts.push(parent.name);
      }
      curr = parent;
    }

    return "/" + parts.reverse().join("/");
  }

  function findFolderIdByPath(path: string): string | null {
    if (path === "/" || path.trim() === "") return ROOT_ID;
    const parts = path.split("/").filter(Boolean);
    let curr = getFolderById(ROOT_ID);

    for (const part of parts) {
      if (!curr) return null;

      const parentId = curr.id;

      const child = nodes.find(
        (n): n is FolderNode =>
          isFolderNode(n) && n.parentId === parentId && n.name === part
      );

      if (!child) return null;
      curr = child;
    }
    return curr ? curr.id : null;
  }

  const pathLabel = buildPathLabel(currentFolderId);
  const canGoUp = currentFolder.parentId !== null;

  const folderNodes = nodes.filter(isFolderNode) as FolderNode[];
  const folderPaths = folderNodes.map((f) => buildFolderPath(f));

  const subFolders = children.filter(isFolderNode).map((n) => n.name);

  // navigation
  function handleOpenFolder(item: DevFileItem) {
    const folderNode = getFolderById(item.id);
    if (!folderNode) return;
    setCurrentFolderId(folderNode.id);
  }

  function handleGoUp() {
    if (!canGoUp) return;
    const parent = getFolderById(currentFolder.parentId);
    if (parent) setCurrentFolderId(parent.id);
  }

  // editor: open file
  function handleSelectFile(item: DevFileItem) {
    const node = nodes.find(
      (n) => n.id === item.id && isFileNode(n)
    ) as FileNode | undefined;
    if (!node) return;
    setCurrentFileId(node.id);
    setFileName(node.name);
    setLanguage(node.language);
    setCode(node.code);
  }

  // editor: save file
  function handleSave() {
    setNodes((prev) => {
      const next = [...prev];
      if (currentFileId) {
        const idx = next.findIndex((n) => n.id === currentFileId);
        if (idx >= 0 && isFileNode(next[idx])) {
          const f = next[idx] as FileNode;
          f.name = fileName;
          f.language = language;
          f.code = code;
          f.ext = (fileName.split(".").pop() || "tsx") as FileNode["ext"];
          f.size = `${Math.max(1, Math.round(code.length / 50))} KB`;
        }
        return next;
      }
      const newFile = makeFile(fileName, currentFolderId, language, code);
      next.push(newFile);
      setCurrentFileId(newFile.id);
      return next;
    });
  }

  // unique name per folder
  function getUniqueNameInFolder(
    baseName: string,
    parentId: string,
    list: FsNode[]
  ): string {
    const existing = new Set(
      list.filter((n) => n.parentId === parentId).map((n) => n.name)
    );

    if (!existing.has(baseName)) return baseName;

    const match = baseName.match(/^(.*) \((\d+)\)$/);
    let stem = baseName;
    let num = 1;

    if (match) {
      stem = match[1];
      num = parseInt(match[2], 10);
    }

    let candidate = "";
    do {
      num += 1;
      candidate = `${stem} (${num})`;
    } while (existing.has(candidate));

    return candidate;
  }

  // create folder
  function handleCreateFolder() {
    setNodes((prev) => {
      const name = getUniqueNameInFolder("New folder", currentFolderId, prev);
      const folder = makeFolder(name, currentFolderId);
      return [...prev, folder];
    });
  }

  // delete
  function deleteFolderRecursive(folderId: string, list: FsNode[]): FsNode[] {
    const toDeleteIds = new Set<string>([folderId]);

    let changed = true;
    while (changed) {
      changed = false;
      for (const node of list) {
        if (node.parentId && toDeleteIds.has(node.parentId)) {
          if (!toDeleteIds.has(node.id)) {
            toDeleteIds.add(node.id);
            changed = true;
          }
        }
      }
    }

    return list.filter((n) => !toDeleteIds.has(n.id));
  }

  function handleDelete(item: DevFileItem) {
    const node = nodes.find((n) => n.id === item.id);
    if (!node) return;

    if (isFolderNode(node)) {
      Alert.alert(
        "Delete folder",
        `Delete folder "${node.name}" and all contents?`,
        [
          { text: "Cancel", style: "cancel" },
          {
            text: "Delete",
            style: "destructive",
            onPress: () => {
              setNodes((prev) => deleteFolderRecursive(node.id, prev));
              if (currentFolderId === node.id) handleGoUp();
            },
          },
        ]
      );
    } else {
      Alert.alert("Delete file", `Delete file "${node.name}"?`, [
        { text: "Cancel", style: "cancel" },
        {
          text: "Delete",
          style: "destructive",
          onPress: () => {
            setNodes((prev) => prev.filter((n) => n.id !== node.id));
            if (currentFileId === node.id) {
              setCurrentFileId(null);
              setFileName("main.tsx");
              setCode("");
            }
          },
        },
      ]);
    }
  }

  // rename
  function handleRename(item: DevFileItem) {
    const node = nodes.find((n) => n.id === item.id);
    if (!node) return;
    setRenameTarget(node);
    setRenameValue(node.name);
  }

  function closeRename() {
    setRenameTarget(null);
    setRenameValue("");
  }

  function confirmRename() {
    if (!renameTarget) return;
    const newName = renameValue.trim();
    if (!newName) {
      closeRename();
      return;
    }

    setNodes((prev) => {
      const next = [...prev];
      const idx = next.findIndex((n) => n.id === renameTarget.id);
      if (idx >= 0) {
        next[idx] = { ...next[idx], name: newName } as FsNode;
      }
      return next;
    });

    if (currentFileId === renameTarget.id) {
      setFileName(newName);
    }

    closeRename();
  }

  // copy / cut / paste
  function handleCopy(item: DevFileItem) {
    const node = nodes.find((n) => n.id === item.id && isFileNode(n));
    if (!node) return;
    setClipboard({ mode: "copy", nodeId: node.id });
  }

  function handleCut(item: DevFileItem) {
    const node = nodes.find((n) => n.id === item.id && isFileNode(n));
    if (!node) return;
    setClipboard({ mode: "cut", nodeId: node.id });
  }

  function handlePaste() {
    if (!clipboard) return;
    const { mode, nodeId } = clipboard;

    setNodes((prev) => {
      const next = [...prev];
      const src = next.find((n) => n.id === nodeId && isFileNode(n)) as
        | FileNode
        | undefined;
      if (!src) return next;

      if (mode === "copy") {
        const uniqueName = getUniqueNameInFolder(
          src.name,
          currentFolderId,
          next
        );
        const copy: FileNode = {
          ...src,
          id: `file-${Date.now()}-${Math.random()}`,
          parentId: currentFolderId,
          name: uniqueName,
        };
        next.push(copy);
      } else {
        const idx = next.findIndex((n) => n.id === src.id);
        if (idx >= 0 && isFileNode(next[idx])) {
          const moved = next[idx] as FileNode;
          const uniqueName = getUniqueNameInFolder(
            moved.name,
            currentFolderId,
            next
          );
          moved.parentId = currentFolderId;
          moved.name = uniqueName;
        }
        setClipboard(null);
      }

      return next;
    });
  }

  // drag / move here (files only)
  function handleDragStart(item: DevFileItem) {
    const node = nodes.find((n) => n.id === item.id && isFileNode(n));
    if (!node) {
      setDraggingId(null);
      return;
    }
    setDraggingId(node.id);
  }

  function handleDropOnFolder(targetFolderItem: DevFileItem) {
    if (!draggingId) return;
    const folderNode = getFolderById(targetFolderItem.id);
    if (!folderNode) return;

    setNodes((prev) => {
      const next = [...prev];
      const idx = next.findIndex((n) => n.id === draggingId);
      if (idx >= 0 && isFileNode(next[idx])) {
        const moved = next[idx] as FileNode;
        const uniqueName = getUniqueNameInFolder(
          moved.name,
          folderNode.id,
          next
        );
        moved.parentId = folderNode.id;
        moved.name = uniqueName;
      }
      return next;
    });

    setDraggingId(null);
    setCurrentFolderId(folderNode.id);
  }

  // assistant drag
  const assistantPos = useRef(
    new Animated.ValueXY({ x: 0, y: 0 })
  ).current;
  const isDraggingAssistant = useRef(false);

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => false,
      onMoveShouldSetPanResponder: (_e, gesture) => {
        const moved =
          Math.abs(gesture.dx) > 5 || Math.abs(gesture.dy) > 5;
        if (moved) {
          isDraggingAssistant.current = true;
        }
        return moved;
      },
      onPanResponderMove: Animated.event(
        [null, { dx: assistantPos.x, dy: assistantPos.y }],
        { useNativeDriver: false }
      ),
      onPanResponderRelease: () => {
        assistantPos.extractOffset();
        setTimeout(() => {
          isDraggingAssistant.current = false;
        }, 0);
      },
      onPanResponderTerminate: () => {
        assistantPos.extractOffset();
        isDraggingAssistant.current = false;
      },
    })
  ).current;

  async function handleSignOut() {
    await logout();
    router.replace("/(auth)/sign-in");
  }

  const handleOpenCtesting = () => {
    router.push({
      pathname: "/ctesting",
      params: {
        language,
        code,
        fileName,
      },
    });
  };

  const handleOpenCgo = () => {
    router.push("/cgo");
  };

  const handleOpenCmarket = () => {
    router.push("/cmarket");
  };

  // OPEN_APP from Cedal command
  function handleOpenAppCommand(appId: string) {
    setActiveTab("view");
    setPendingAppOpen(appId);
  }

  // central Cedal command handler
  function runCedalCommand(cmd: CedalCommand) {
    switch (cmd.type) {
      case "VIBRATE": {
        const pattern = cmd.durationMs;
        if (Array.isArray(pattern)) {
          Vibration.vibrate(pattern);
        } else {
          const ms = Math.max(100, Math.min(pattern, 5000));
          Vibration.vibrate(ms);
        }
        return;
      }

      case "NOTIFY": {
        Notifications.scheduleNotificationAsync({
          content: {
            title: cmd.title || "Cedal",
            body: cmd.body,
          },
          trigger: null,
        });
        return;
      }

      case "OPEN_APP": {
        handleOpenAppCommand(cmd.appId);
        return;
      }

      case "BUG": {
        setBugText(cmd.body);
        setBugVisible(true);
        return;
      }

      default:
        return;
    }
  }

  function renderContent() {
    if (activeTab === "files") {
      return (
        <DeveloperFiles
          pathLabel={pathLabel}
          items={visibleItems}
          onOpenFolder={handleOpenFolder}
          onGoUp={handleGoUp}
          canGoUp={canGoUp}
          onSelectFile={handleSelectFile}
          onCreateFolder={handleCreateFolder}
          onDelete={handleDelete}
          onRename={handleRename}
          onCopy={handleCopy}
          onCut={handleCut}
          onPaste={handlePaste}
          onDragStart={handleDragStart}
          onDropOnFolder={handleDropOnFolder}
          canPaste={clipboard !== null}
        />
      );
    }

    if (activeTab === "view") {
      return (
        <DeveloperView
          code={code}
          language={language}
          onOpenCtesting={handleOpenCtesting}
          onOpenCgo={handleOpenCgo}
          onOpenCmarket={handleOpenCmarket}
          onRunCedalCommand={runCedalCommand}
          pendingAppOpen={pendingAppOpen}
        />
      );
    }

    return (
      <DeveloperEditor
        fileName={fileName}
        onChangeFileName={setFileName}
        language={language}
        onChangeLanguage={setLanguage}
        code={code}
        onChangeCode={setCode}
        currentFolder={pathLabel === "/" ? null : pathLabel}
        availableFolders={folderPaths}
        subFolders={subFolders}
        onChangeFolder={(folderPath) => {
          const targetId = folderPath ? findFolderIdByPath(folderPath) : ROOT_ID;
          if (targetId) {
            setCurrentFolderId(targetId);
          }
        }}
        onSave={handleSave}
      />
    );
  }

  function handleAssistantPress() {
    if (!isDraggingAssistant.current) {
      setShowAssistant((prev) => !prev);
    }
  }

  return (
    <View style={styles.root}>
      <View style={styles.header}>
        <View>
          <Text style={styles.title}>CEDAL DEVELOPER</Text>
          <Text style={styles.sub}>Workbench</Text>
        </View>

        <TouchableOpacity style={styles.signOutBtn} onPress={handleSignOut}>
          <Text style={styles.signOutText}>Sign out</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.content}>{renderContent()}</View>

      <View style={styles.bottom}>
        <TouchableOpacity
          style={styles.tab}
          onPress={() => setActiveTab("work")}
        >
          <Text
            style={[
              styles.tabLabel,
              activeTab === "work" && styles.tabLabelActive,
            ]}
          >
            Work
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.tab}
          onPress={() => setActiveTab("files")}
        >
          <Text
            style={[
              styles.tabLabel,
              activeTab === "files" && styles.tabLabelActive,
            ]}
          >
            Files
          </Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.tab}
          onPress={() => setActiveTab("view")}
        >
          <Text
            style={[
              styles.tabLabel,
              activeTab === "view" && styles.tabLabelActive,
            ]}
          >
            View
          </Text>
        </TouchableOpacity>
      </View>

      <Animated.View
        {...panResponder.panHandlers}
        style={[
          styles.assistantAnchor,
          { transform: assistantPos.getTranslateTransform() },
        ]}
      >
        <DevAssistantBubble onPress={handleAssistantPress} />

        {showAssistant && (
          <DevAssistantPanel
            onClose={() => setShowAssistant(false)}
            style={styles.assistantPanelOffset}
            userId={user?.uid ?? null}
            currentFile={fileName}
            currentLanguage={language}
            currentCode={code}
            onApplyCode={(newCode) => setCode(newCode)}
          />
        )}
      </Animated.View>

      {renameTarget && (
        <View style={styles.renameOverlay}>
          <View style={styles.renameCard}>
            <Text style={styles.renameTitle}>
              Rename {renameTarget.kind === "folder" ? "folder" : "file"}
            </Text>
            <View style={styles.renameInputBox}>
              <Text style={styles.renameLabel}>New name</Text>
              <TextInput
                style={styles.renameInput}
                value={renameValue}
                onChangeText={setRenameValue}
                autoFocus
                autoCapitalize="none"
              />
            </View>
            <View style={styles.renameButtonsRow}>
              <TouchableOpacity onPress={closeRename}>
                <Text style={styles.renameCancel}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity onPress={confirmRename}>
                <Text style={styles.renameSave}>Save</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      )}

      {/* FULL-SCREEN MATRIX BUG OVERLAY */}
      {bugVisible && (
        <TouchableOpacity
          activeOpacity={1}
          style={styles.bugOverlay}
          onPress={() => setBugVisible(false)}
        >
          <View style={styles.bugMatrixLayer}>
            <ScrollView
              style={{ flex: 1, width: "100%" }}
              contentContainerStyle={styles.bugScrollContent}
              showsVerticalScrollIndicator={false}
            >
              {Array.from({ length: 40 }).map((_, i) => (
                <Text key={i} style={styles.bugMatrixText}>
                  {bugText.toUpperCase()} {bugText.toUpperCase()}{" "}
                  {bugText.toUpperCase()}
                </Text>
              ))}
            </ScrollView>
          </View>

          <View style={styles.bugCenter}>
            <Text style={styles.bugCenterText}>{bugText.toUpperCase()}</Text>
            <Text style={styles.bugHintText}>tap to cancel bug</Text>
          </View>
        </TouchableOpacity>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: "#020617",
  },
  header: {
    paddingTop: 48,
    paddingHorizontal: 20,
    paddingBottom: 14,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(148,163,184,0.3)",
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  title: {
    color: "#e5e7eb",
    fontSize: 16,
    letterSpacing: 3,
    fontWeight: "700",
  },
  sub: {
    color: "#9ca3af",
    fontSize: 11,
    marginTop: 4,
  },
  signOutBtn: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
  },
  signOutText: {
    color: "#e5e7eb",
    fontSize: 12,
  },
  content: {
    flex: 1,
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 90,
  },
  bottom: {
    position: "absolute",
    left: 16,
    right: 16,
    bottom: 30,
    height: 52,
    borderRadius: 999,
    paddingHorizontal: 20,
    paddingVertical: 8,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: "rgba(15,23,42,0.9)",
    backgroundColor: "rgba(15,23,42,0.98)",
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
  },
  tab: {
    flex: 1,
    alignItems: "center",
  },
  tabLabel: {
    color: "#6b7280",
    fontSize: 12,
    fontWeight: "500",
  },
  tabLabelActive: {
    color: "#e5e7eb",
  },
  assistantAnchor: {
    position: "absolute",
    right: 20,
    bottom: 110,
    zIndex: 40,
  },
  assistantPanelOffset: {
    marginTop: 8,
  },
  renameOverlay: {
    position: "absolute",
    left: 0,
    right: 0,
    top: 0,
    bottom: 0,
    backgroundColor: "rgba(0,0,0,0.6)",
    justifyContent: "center",
    alignItems: "center",
  },
  renameCard: {
    width: 260,
    borderRadius: 12,
    padding: 12,
    backgroundColor: "#020617",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
  },
  renameTitle: {
    color: "#e5e7eb",
    fontSize: 13,
    fontWeight: "600",
    marginBottom: 8,
  },
  renameInputBox: {
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    paddingHorizontal: 8,
    paddingVertical: 4,
    marginBottom: 10,
  },
  renameLabel: {
    color: "#9ca3af",
    fontSize: 11,
    marginBottom: 2,
  },
  renameInput: {
    color: "#e5e7eb",
    fontSize: 13,
    padding: 0,
  },
  renameButtonsRow: {
    flexDirection: "row",
    justifyContent: "flex-end",
    columnGap: 12,
  },
  renameCancel: {
    color: "#9ca3af",
    fontSize: 12,
  },
  renameSave: {
    color: "#22d3ee",
    fontSize: 12,
  },

  // BUG / matrix overlay
  bugOverlay: {
    position: "absolute",
    left: 0,
    right: 0,
    top: 0,
    bottom: 0,
    backgroundColor: "rgba(0,0,0,0.95)",
    justifyContent: "center",
    alignItems: "center",
    zIndex: 999,
  },
  bugMatrixLayer: {
    position: "absolute",
    left: 0,
    right: 0,
    top: 0,
    bottom: 0,
    paddingHorizontal: 16,
    paddingVertical: 40,
  },
  bugScrollContent: {
    justifyContent: "center",
  },
  bugMatrixText: {
    color: "#22c55e",
    fontSize: 11,
    fontFamily: "monospace",
    opacity: 0.6,
  },
  bugCenter: {
    alignItems: "center",
    paddingHorizontal: 20,
  },
  bugCenterText: {
    color: "#22c55e",
    fontSize: 26,
    fontWeight: "800",
    letterSpacing: 4,
    textAlign: "center",
  },
  bugHintText: {
    color: "#16a34a",
    fontSize: 11,
    marginTop: 8,
  },
});
