// src/member/work/code/CodePanel.tsx
import {
  askCoderAssistant,
  CoderMessage,
} from "@/src/api/coderAssistant";
import { requestAndroidBuild } from "@/src/api/requestAndroidBuild";
import { runCode } from "@/src/api/runCode";
import { uploadHtmlPreview } from "@/src/api/uploadHtmlPreview";
import { uploadRunOutputPreview } from "@/src/api/uploadRunOutputPreview";
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import AsyncStorage from "@react-native-async-storage/async-storage";
import * as WebBrowser from "expo-web-browser";
import React, { useEffect, useState } from "react";
import {
  KeyboardAvoidingView,
  Platform,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import AiConsolePanel from "./AiConsolePanel";
import CmdPanel from "./CmdPanel";
import EditorPanel, { CodeLanguage } from "./EditorPanel";
import FilesPanel, { FileItem } from "./FilesPanel";
import HtmlPreviewModal from "./HtmlPreviewModal";
import KotlinBuildModal from "./KotlinBuildModal";

const OUTPUT_DEST_KEY = "cedal.codePanel.outputDestination";

type Props = {
  onBack: () => void;
};

type CodeTab = "files" | "cmd" | "editor" | "console";

export default function CodePanel({ onBack }: Props) {
  const { colors } = useTheme();

  const [activeTab, setActiveTab] = useState<CodeTab>("editor");
  const [fileSearch, setFileSearch] = useState("");

  const [code, setCode] = useState(
    `function CedalPanel() {
  const [state, setState] = useState(() => bootState());
  const [logs, pushLog] = useLogs("cedal-coder");

  useEffect(() => {
    pushLog("session-online", { at: Date.now() });
  }, []);

  return (
    <Workspace>
      <Sidebar state={state} />
      <Editor
        doc={state.doc}
        onChange={(next) => setState((s) => ({ ...s, doc: next }))}
      />
      <Console logs={logs} />
    </Workspace>
  );
}`
  );

  const [cmdText, setCmdText] = useState("C:\\\\Users> ");
  const [aiLines, setAiLines] = useState<string[]>([]);
  const [aiInput, setAiInput] = useState("");

  // file tree + selection
  const [filesAll, setFilesAll] = useState<FileItem[]>([]);
  const [currentFolder, setCurrentFolder] = useState<string>("/");
  const [selectedFileName, setSelectedFileName] = useState<string | null>(null); // open in editor
  const [actionTargetName, setActionTargetName] = useState<string | null>(null); // copy/cut/delete target
  const [clipboard, setClipboard] = useState<FileItem | null>(null);
  const [clipboardMode, setClipboardMode] = useState<"copy" | "cut" | null>(
    null
  );

  const [fileCodes, setFileCodes] = useState<Record<string, string>>({});
  const [language, setLanguage] = useState<CodeLanguage>("JavaScript");

  const [outputDestination, setOutputDestination] = useState<
    "webview" | "browser"
  >("webview");
  const [htmlPreviewVisible, setHtmlPreviewVisible] = useState(false);
  const [previewHtml, setPreviewHtml] = useState("");

  const [kotlinJobId, setKotlinJobId] = useState<string | null>(null);
  const [kotlinModalVisible, setKotlinModalVisible] = useState(false);

  useEffect(() => {
    AsyncStorage.getItem(OUTPUT_DEST_KEY).then((saved) => {
      if (saved === "webview" || saved === "browser") {
        setOutputDestination(saved);
      }
    });
  }, []);

  function handleChangeOutputDestination(dest: "webview" | "browser") {
    setOutputDestination(dest);
    void AsyncStorage.setItem(OUTPUT_DEST_KEY, dest);
  }

  function inferExtFromLanguage(lang: CodeLanguage): string {
    switch (lang) {
      case "JavaScript":
        return ".js";
      case "TypeScript":
        return ".ts";
      case "Python":
        return ".py";
      case "PHP":
        return ".php";
      case "Ruby":
        return ".rb";
      case "Lua":
        return ".lua";
      case "Bash":
        return ".sh";
      case "C":
        return ".c";
      case "C++":
        return ".cpp";
      case "Go":
        return ".go";
      case "Rust":
        return ".rs";
      case "Java":
        return ".java";
      case "C#":
        return ".cs";
      case "HTML":
        return ".html";
      case "Kotlin":
        return ".kt";
      default:
        return "";
    }
  }

  function handleAddFileOrFolder(
    type: "file" | "folder",
    rawName: string
  ) {
    let name = rawName.trim();
    if (!name) return;

    if (type === "file" && !name.includes(".")) {
      name = name + inferExtFromLanguage(language);
    }

    const lower = name.toLowerCase();
    const tag =
      type === "folder"
        ? "DIR"
        : lower.endsWith(".md")
        ? "MD"
        : lower.endsWith(".ts") || lower.endsWith(".tsx")
        ? "TS"
        : lower.endsWith(".js") || lower.endsWith(".jsx")
        ? "JS"
        : lower.endsWith(".py")
        ? "PY"
        : "FILE";

    const item: FileItem = {
      name,
      tag,
      kind: type,
      path: currentFolder,
    };

    setFilesAll((prev) => [...prev, item]);

    if (type === "file") {
      setSelectedFileName(name);
      setActionTargetName(null);
      setCode(fileCodes[name] ?? "");
    }
  }

  function itemsInCurrentFolder(): FileItem[] {
    return filesAll
      .filter((f) => f.path === currentFolder)
      .filter((f) =>
        f.name.toLowerCase().includes(fileSearch.toLowerCase())
      );
  }

  function filesInCurrentFolderNames(): string[] {
    return filesAll
      .filter((f) => f.path === currentFolder && f.kind === "file")
      .map((f) => f.name);
  }

  // item selected for copy/cut/delete (from long press)
  function getActionTarget(): FileItem | null {
    if (!actionTargetName) return null;
    return (
      filesAll.find(
        (f) => f.name === actionTargetName && f.path === currentFolder
      ) ?? null
    );
  }

  function handleCopy() {
    const sel = getActionTarget();
    if (!sel) return;
    setClipboard(sel);
    setClipboardMode("copy");
  }

  function handleCut() {
    const sel = getActionTarget();
    if (!sel) return;
    setClipboard(sel);
    setClipboardMode("cut");
  }

  function handlePaste() {
    if (!clipboard) return;

    let newName = clipboard.name;
    if (
      filesAll.some(
        (f) => f.name === newName && f.path === currentFolder
      )
    ) {
      let i = 1;
      while (
        filesAll.some(
          (f) =>
            f.name === `${clipboard.name} (${i})` &&
            f.path === currentFolder
        )
      ) {
        i += 1;
      }
      newName = `${clipboard.name} (${i})`;
    }

    const newItem: FileItem = {
      ...clipboard,
      name: newName,
      path: currentFolder,
    };

    setFilesAll((prev) => {
      if (clipboardMode === "cut") {
        return [
          ...prev.filter(
            (f) =>
              !(
                f.name === clipboard.name &&
                f.path === clipboard.path
              )
          ),
          newItem,
        ];
      }
      return [...prev, newItem];
    });

    setFileCodes((prev) => {
      const srcKey = clipboard.name;
      const newCode = prev[srcKey] ?? "";
      const next = { ...prev, [newName]: newCode };
      if (clipboardMode === "cut") {
        delete next[srcKey];
      }
      return next;
    });

    setSelectedFileName(newName);
    setActionTargetName(null);
    setCode(fileCodes[clipboard.name] ?? "");

    if (clipboardMode === "cut") {
      setClipboard(null);
      setClipboardMode(null);
    }
  }

  function handleDelete() {
    const sel = getActionTarget();
    if (!sel) return;

    setFilesAll((prev) =>
      prev.filter(
        (f) =>
          !(f.name === sel.name && f.path === sel.path)
      )
    );

    setFileCodes((prev) => {
      const next = { ...prev };
      delete next[sel.name];
      return next;
    });

    if (selectedFileName === sel.name) {
      setSelectedFileName(null);
      setCode("");
    }
    setActionTargetName(null);
  }

  // tap: open
  function handleSelectFile(name: string) {
    setSelectedFileName(name);
    setActionTargetName(null);
    setCode(fileCodes[name] ?? "");
  }

  // long press: mark for actions
  function handleLongSelectFile(name: string) {
    setActionTargetName(name);
  }

  function handleChangeCode(next: string) {
    setCode(next);
    if (selectedFileName) {
      setFileCodes((prev) => ({
        ...prev,
        [selectedFileName]: next,
      }));
    }
  }

  function handleCmdSubmit() {
    const prompt = "C:\\\\Users>";
    const lines = cmdText.split("\n");
    const last = lines[lines.length - 1] || "";
    const command = last.startsWith(prompt)
      ? last.slice(prompt.length).trim()
      : last.trim();
    if (!command) {
      setCmdText((prev) => prev + "\n" + prompt + " ");
      return;
    }
    setCode((prev) => prev + `\n// cmd: ${command}`);
    setCmdText((prev) => prev + "\n" + prompt + " ");
  }

  async function sendToCoder(
    prompt: string,
    mode: "chat" | "rewrite" = "chat"
  ) {
    const userMessage: CoderMessage = {
      id: String(Date.now()),
      role: "user",
      content: prompt,
    };

    setAiLines((prev) => [...prev, `you: ${prompt}`]);

    try {
      const answer = await askCoderAssistant({
        messages: [userMessage],
      });

      if (mode === "rewrite") {
        setCode(answer);
        if (selectedFileName) {
          setFileCodes((prev) => ({
            ...prev,
            [selectedFileName]: answer,
          }));
        }
        setAiLines((prev) => [
          ...prev,
          "coder: (restructured code applied to editor)",
        ]);
      } else {
        setAiLines((prev) => [...prev, `coder: ${answer}`]);
      }
    } catch (e: any) {
      setAiLines((prev) => [
        ...prev,
        "system: Coder failed to answer. Check connection or config.",
      ]);
    }
  }

  function handleAiSend() {
    if (!aiInput.trim()) return;
    const text = aiInput.trim();
    setAiInput("");
    void sendToCoder(text, "chat");
  }

  async function handleToolRestructure() {
    const snippet = code.slice(0, 4000);
    const hint =
      aiInput.trim() || "Restructure this code for clarity and readability.";

    setAiInput("");

    const prompt =
      `${hint}\n` +
      "You MUST respond with ONLY the full rewritten code, no explanations, no markdown, no backticks:\n\n" +
      snippet;

    void sendToCoder(prompt, "rewrite");
  }

  function handleToolDebug() {
    const snippet = code.slice(0, 4000);
    const prompt =
      "Debug this code: find likely bugs or suspicious patterns and explain fixes. Do NOT change the code, just explain:\n\n" +
      snippet;
    void sendToCoder(prompt, "chat");
  }

  function handleToolErrorFinder() {
    const snippet = code.slice(0, 4000);
    const prompt =
      "Act as an error finder. Point out runtime or type errors you see and how to fix them. Do NOT rewrite the code, just list issues:\n\n" +
      snippet;
    void sendToCoder(prompt, "chat");
  }

  async function handleRunHtml() {
    if (outputDestination === "webview") {
      setPreviewHtml(code);
      setHtmlPreviewVisible(true);
      return;
    }
    setAiLines((prev) => [...prev, "$ run (HTML) - opening in browser..."]);
    try {
      const url = await uploadHtmlPreview(code);
      await WebBrowser.openBrowserAsync(url);
    } catch (e: any) {
      setAiLines((prev) => [
        ...prev,
        `system: Preview failed - ${e?.message ?? "unknown error"}`,
      ]);
    }
  }

  async function handleRunKotlin() {
    setKotlinJobId(null);
    setKotlinModalVisible(true);
    try {
      const { jobId } = await requestAndroidBuild(code);
      setKotlinJobId(jobId);
    } catch (e: any) {
      setAiLines((prev) => [
        ...prev,
        `system: Android build request failed - ${e?.message ?? "unknown error"}`,
      ]);
      setKotlinModalVisible(false);
    }
  }

  async function handleRunEditor() {
    if (language === "HTML") {
      void handleRunHtml();
      return;
    }
    if (language === "Kotlin") {
      void handleRunKotlin();
      return;
    }
    setAiLines((prev) => [...prev, `$ run (${language})`]);
    try {
      const result = await runCode({ language, code });

      if (outputDestination === "browser") {
        setAiLines((prev) => [...prev, "system: opening result in browser..."]);
        const url = await uploadRunOutputPreview(language, result);
        await WebBrowser.openBrowserAsync(url);
        return;
      }

      setAiLines((prev) => {
        const next = [...prev];
        if (result.stdout) next.push(...result.stdout.split("\n"));
        if (result.stderr) next.push(...result.stderr.split("\n"));
        next.push(
          result.timedOut
            ? "[timed out]"
            : `[exit ${result.exitCode}] (${result.durationMs}ms)`
        );
        return next;
      });
    } catch (e: any) {
      setAiLines((prev) => [
        ...prev,
        `system: Run failed - ${e?.message ?? "unknown error"}`,
      ]);
    }
  }

  function handleDebugEditor() {
    const snippet = code.slice(0, 4000);
    const prompt =
      "Debug this code like a senior engineer: explain likely bugs or issues and how to fix them:\n\n" +
      snippet;
    void sendToCoder(prompt, "chat");
  }

  function handleSaveEditor() {
    setAiLines((prev) => [...prev, "system: Code saved successfully"]);
  }

  return (
    <KeyboardAvoidingView
      style={[styles.root, { backgroundColor: colors.background }]}
      behavior={Platform.OS === "ios" ? "padding" : undefined}
    >
      {/* Header */}
      <View style={[styles.header, { borderBottomColor: colors.border }]}>
        <TouchableOpacity
          onPress={onBack}
          style={styles.backBtn}
          activeOpacity={0.7}
        >
          <Text style={styles.backText}>◀</Text>
        </TouchableOpacity>
        <View>
          <Text style={[styles.headerTitle, { color: colors.textPrimary }]}>
            Cedal Coder
          </Text>
          <Text style={styles.headerSub}>workspace / code</Text>
        </View>
        <View style={{ flex: 1 }} />
        <View style={styles.headerStatus}>
          <View style={styles.liveDot} />
          <Text style={styles.headerStatusText}>LIVE LINK</Text>
        </View>
      </View>

      {/* Body */}
      <View style={styles.body}>
        <View
          style={[
            styles.mainArea,
            { borderColor: colors.border },
          ]}
        >
          {activeTab === "files" && (
            <FilesPanel
              files={itemsInCurrentFolder()}
              search={fileSearch}
              onChangeSearch={setFileSearch}
              borderColor={colors.border}
              onAddItem={handleAddFileOrFolder}
              currentFolder={currentFolder}
              onChangeFolder={setCurrentFolder}
              selectedName={selectedFileName}
              actionTargetName={actionTargetName}
              onSelect={handleSelectFile}
              onLongSelect={handleLongSelectFile}
              onCopy={handleCopy}
              onPaste={handlePaste}
              onCut={handleCut}
              onDelete={handleDelete}
            />
          )}

          {activeTab === "cmd" && (
            <CmdPanel
              cmdText={cmdText}
              onChangeCmdText={setCmdText}
              onSubmit={handleCmdSubmit}
              borderColor={colors.border}
            />
          )}

          {activeTab === "editor" && (
            <EditorPanel
              code={code}
              onChangeCode={handleChangeCode}
              onRun={handleRunEditor}
              onDebug={handleDebugEditor}
              onSave={handleSaveEditor}
              borderColor={colors.border}
              files={filesInCurrentFolderNames()}
              activeFile={selectedFileName ?? undefined}
              onSelectFile={handleSelectFile}
              currentFolder={currentFolder}
              language={language}
              onChangeLanguage={setLanguage}
              outputDestination={outputDestination}
              onChangeOutputDestination={handleChangeOutputDestination}
            />
          )}

          {activeTab === "console" && (
            <AiConsolePanel
              lines={aiLines}
              input={aiInput}
              onChangeInput={setAiInput}
              onSend={handleAiSend}
              borderColor={colors.border}
              onRestructure={handleToolRestructure}
              onDebug={handleToolDebug}
              onErrorFinder={handleToolErrorFinder}
            />
          )}
        </View>

        {/* Bottom tabs */}
        <View
          style={[
            styles.localTabsBar,
            { borderTopColor: colors.border },
          ]}
        >
          <CodeTabButton
            label="Files"
            icon="document-text-outline"
            active={activeTab === "files"}
            onPress={() => setActiveTab("files")}
          />
          <CodeTabButton
            label="Cmd"
            icon="terminal-outline"
            active={activeTab === "cmd"}
            onPress={() => setActiveTab("cmd")}
          />
          <CodeTabButton
            label="Editor"
            icon="code-slash-outline"
            active={activeTab === "editor"}
            onPress={() => setActiveTab("editor")}
          />
          <CodeTabButton
            label="Console"
            icon="chatbox-ellipses-outline"
            active={activeTab === "console"}
            onPress={() => setActiveTab("console")}
          />
        </View>
      </View>

      <HtmlPreviewModal
        visible={htmlPreviewVisible}
        html={previewHtml}
        onClose={() => setHtmlPreviewVisible(false)}
      />

      <KotlinBuildModal
        visible={kotlinModalVisible}
        jobId={kotlinJobId}
        onClose={() => setKotlinModalVisible(false)}
      />
    </KeyboardAvoidingView>
  );
}

type CodeTabButtonProps = {
  label: string;
  icon: keyof typeof Ionicons.glyphMap;
  active: boolean;
  onPress: () => void;
};

function CodeTabButton({
  label,
  icon,
  active,
  onPress,
}: CodeTabButtonProps) {
  return (
    <TouchableOpacity
      style={styles.localTabBtn}
      onPress={onPress}
      activeOpacity={0.8}
    >
      <Ionicons
        name={icon}
        size={18}
        color={active ? "#22c55e" : "#6b7280"}
      />
      <Text
        style={[
          styles.localTabText,
          active && styles.localTabTextActive,
        ]}
      >
        {label}
      </Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },

  header: {
    height: 56,
    paddingHorizontal: 16,
    flexDirection: "row",
    alignItems: "center",
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  backBtn: {
    width: 32,
    height: 32,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.5)",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 10,
  },
  backText: { color: "#9ca3af", fontSize: 16 },
  headerTitle: {
    fontSize: 15,
    fontWeight: "600",
  },
  headerSub: {
    fontSize: 10,
    color: "#6b7280",
    textTransform: "uppercase",
    letterSpacing: 1,
  },
  headerStatus: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(30,64,175,0.9)",
    backgroundColor: "rgba(15,23,42,0.9)",
  },
  liveDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: "#22c55e",
    marginRight: 6,
  },
  headerStatusText: {
    fontSize: 9,
    color: "#9ca3af",
    letterSpacing: 1,
  },

  body: {
    flex: 1,
    padding: 10,
    gap: 8,
  },

  mainArea: {
    flex: 1,
    borderRadius: 18,
    borderWidth: 1,
    overflow: "hidden",
    backgroundColor: "#020617",
  },

  localTabsBar: {
    height: 60,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-around",
    borderTopWidth: StyleSheet.hairlineWidth,
    backgroundColor: "#020617",
    paddingHorizontal: 8,
    borderRadius: 999,
  },
  localTabBtn: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
  },
  localTabText: {
    marginTop: 3,
    fontSize: 11,
    color: "#64748b",
  },
  localTabTextActive: {
    color: "#e5e7eb",
    fontWeight: "600",
  },
});
