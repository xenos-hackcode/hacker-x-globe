// src/member/work/code/EditorPanel.tsx
import React from "react";
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TextInput,
  TouchableOpacity,
} from "react-native";

export const LANGUAGES = [
  "JavaScript",
  "TypeScript",
  "Python",
  "PHP",
  "Ruby",
  "Lua",
  "Bash",
  "C",
  "C++",
  "Go",
  "Rust",
  "Java",
  "C#",
  "HTML",
  "Kotlin",
] as const;
export type CodeLanguage = (typeof LANGUAGES)[number];

// Languages executed on the heavier compiled-tier runner (longer timeout, own region set).
export const COMPILED_LANGUAGES: readonly CodeLanguage[] = [
  "C",
  "C++",
  "Go",
  "Rust",
  "Java",
  "C#",
];

type Props = {
  code: string;
  onChangeCode: (value: string) => void;
  onRun: () => void;
  onDebug: () => void;
  onSave: () => void;
  borderColor: string;

  files: string[];             // file names in currentFolder
  activeFile?: string;
  onSelectFile?: (name: string) => void;

  currentFolder?: string | null;
  subFolders?: string[];
  onSelectSubfolder?: (name: string) => void;

  language?: CodeLanguage;
  onChangeLanguage?: (lang: CodeLanguage) => void;

  outputDestination?: "webview" | "browser";
  onChangeOutputDestination?: (dest: "webview" | "browser") => void;
};

export default function EditorPanel({
  code,
  onChangeCode,
  onRun,
  onDebug,
  onSave,
  borderColor,

  files,
  activeFile,
  onSelectFile,

  currentFolder = null,
  subFolders = [],
  onSelectSubfolder,

  language = "JavaScript",
  onChangeLanguage,

  outputDestination = "webview",
  onChangeOutputDestination,
}: Props) {
  const activeFileSafe = activeFile ?? files[0] ?? "untitled";

  const pathLabel =
    currentFolder && currentFolder.length > 0
      ? `Workspace ${currentFolder}`
      : "Workspace /";

  return (
    <View style={styles.root}>
      {/* Path row */}
      <View style={styles.pathRow}>
        <Text style={styles.pathText}>{pathLabel}</Text>
      </View>

      {/* FILES box */}
      <View style={styles.filesBox}>
        <Text style={styles.filesLabel}>FILES</Text>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.filesRow}
        >
          {files.map((name) => {
            const active = name === activeFileSafe;
            return (
              <TouchableOpacity
                key={name}
                onPress={() => onSelectFile && onSelectFile(name)}
                style={[
                  styles.filePill,
                  active && styles.filePillActive,
                ]}
                activeOpacity={onSelectFile ? 0.85 : 1}
              >
                <Text
                  style={[
                    styles.filePillText,
                    active && styles.filePillTextActive,
                  ]}
                >
                  {name}
                </Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>
      </View>

      {/* SUBFOLDER box */}
      <View style={styles.subFolderBox}>
        <Text style={styles.subFolderLabel}>SUBFOLDER</Text>
        {subFolders.length === 0 ? (
          <Text style={styles.subFolderEmpty}>No subfolders</Text>
        ) : (
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.folderPillsRow}
          >
            {subFolders.map((name) => (
              <TouchableOpacity
                key={name}
                onPress={() => onSelectSubfolder && onSelectSubfolder(name)}
                style={styles.folderPill}
                activeOpacity={onSelectSubfolder ? 0.85 : 1}
              >
                <Text style={styles.folderPillText}>{name}</Text>
              </TouchableOpacity>
            ))}
          </ScrollView>
        )}
      </View>

      {/* LANGUAGE box */}
      <View style={styles.languageBox}>
        <Text style={styles.languageLabel}>LANGUAGE</Text>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.languagePillsRow}
        >
          {LANGUAGES.map((lang) => {
            const active = language === lang;
            return (
              <TouchableOpacity
                key={lang}
                onPress={() => onChangeLanguage && onChangeLanguage(lang)}
                style={[
                  styles.languagePill,
                  active && styles.languagePillActive,
                ]}
                activeOpacity={onChangeLanguage ? 0.85 : 1}
              >
                <Text
                  style={[
                    styles.languagePillText,
                    active && styles.languagePillTextActive,
                  ]}
                >
                  {lang}
                </Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>
      </View>

      {/* OUTPUT box (every language except Kotlin, which always installs to the phone) */}
      {language !== "Kotlin" && (
        <View style={styles.languageBox}>
          <Text style={styles.languageLabel}>SHOW OUTPUT IN</Text>
          <View style={styles.languagePillsRow}>
            {(["webview", "browser"] as const).map((dest) => {
              const active = outputDestination === dest;
              return (
                <TouchableOpacity
                  key={dest}
                  onPress={() =>
                    onChangeOutputDestination &&
                    onChangeOutputDestination(dest)
                  }
                  style={[
                    styles.languagePill,
                    active && styles.languagePillActive,
                  ]}
                  activeOpacity={onChangeOutputDestination ? 0.85 : 1}
                >
                  <Text
                    style={[
                      styles.languagePillText,
                      active && styles.languagePillTextActive,
                    ]}
                  >
                    {dest === "webview" ? "In-app" : "Browser"}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </View>
        </View>
      )}

      {/* USER POWERS box */}
      <View style={styles.powersBox}>
        <Text style={styles.powersLabel}>USER POWERS</Text>
        <View style={styles.powersRow}>
          <TouchableOpacity
            onPress={onRun}
            style={styles.runPill}
            activeOpacity={0.85}
          >
            <Text style={styles.runPillText}>Run</Text>
          </TouchableOpacity>
          <TouchableOpacity
            onPress={onDebug}
            style={styles.debugPill}
            activeOpacity={0.85}
          >
            <Text style={styles.debugPillText}>Debug</Text>
          </TouchableOpacity>
          <TouchableOpacity
            onPress={onSave}
            style={styles.savePill}
            activeOpacity={0.85}
          >
            <Text style={styles.savePillText}>Save</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Separator */}
      <View
        style={[
          styles.separator,
          { borderBottomColor: borderColor },
        ]}
      />

      {/* Editor container */}
      <View style={styles.editorContainer}>
        <ScrollView
          style={styles.editorScroll}
          contentContainerStyle={styles.editorScrollContent}
          keyboardShouldPersistTaps="handled"
        >
          <TextInput
            value={code}
            onChangeText={onChangeCode}
            style={styles.editorInput}
            placeholder={`Start typing ${language} for ${activeFileSafe}...`}
            placeholderTextColor="#4b5563"
            multiline
            autoCorrect={false}
            autoCapitalize="none"
            textAlignVertical="top"
          />
        </ScrollView>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1 },

  pathRow: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 8,
    paddingHorizontal: 10,
  },
  pathText: { color: "#9ca3af", fontSize: 11 },

  filesBox: {
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    backgroundColor: "rgba(15,23,42,0.98)",
    marginBottom: 8,
  },
  filesLabel: {
    color: "#6b7280",
    fontSize: 9,
    letterSpacing: 1,
    marginBottom: 4,
  },
  filesRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  filePill: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(55,65,81,0.9)",
    backgroundColor: "rgba(15,23,42,1)",
  },
  filePillActive: {
    borderColor: "#22d3ee",
    backgroundColor: "rgba(34,211,238,0.12)",
  },
  filePillText: {
    color: "#9ca3af",
    fontSize: 10,
  },
  filePillTextActive: {
    color: "#e5e7eb",
    fontWeight: "600",
  },

  subFolderBox: {
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    backgroundColor: "rgba(15,23,42,0.98)",
    marginBottom: 8,
  },
  subFolderLabel: {
    color: "#6b7280",
    fontSize: 9,
    letterSpacing: 1,
    marginBottom: 4,
  },
  subFolderEmpty: {
    color: "#4b5563",
    fontSize: 10,
  },
  folderPillsRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  folderPill: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(55,65,81,0.9)",
    backgroundColor: "rgba(15,23,42,1)",
  },
  folderPillText: {
    color: "#9ca3af",
    fontSize: 10,
  },

  languageBox: {
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    backgroundColor: "rgba(15,23,42,0.98)",
    marginBottom: 8,
  },
  languageLabel: {
    color: "#6b7280",
    fontSize: 9,
    letterSpacing: 1,
    marginBottom: 4,
  },
  languagePillsRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  languagePill: {
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(55,65,81,0.9)",
    backgroundColor: "rgba(15,23,42,1)",
  },
  languagePillActive: {
    borderColor: "#22d3ee",
    backgroundColor: "rgba(34,211,238,0.12)",
  },
  languagePillText: {
    color: "#9ca3af",
    fontSize: 10,
  },
  languagePillTextActive: {
    color: "#e5e7eb",
    fontWeight: "600",
  },

  powersBox: {
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    backgroundColor: "rgba(15,23,42,0.98)",
    marginBottom: 8,
  },
  powersLabel: {
    color: "#6b7280",
    fontSize: 9,
    letterSpacing: 1,
    marginBottom: 4,
  },
  powersRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  runPill: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#22c55e",
    backgroundColor: "#14532d",
  },
  runPillText: {
    color: "#bbf7d0",
    fontSize: 11,
    textTransform: "uppercase",
    letterSpacing: 1,
  },
  debugPill: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#60a5fa",
    backgroundColor: "#1d4ed8",
  },
  debugPillText: {
    color: "#dbeafe",
    fontSize: 11,
    textTransform: "uppercase",
    letterSpacing: 1,
  },
  savePill: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#eab308",
    backgroundColor: "#713f12",
  },
  savePillText: {
    color: "#facc15",
    fontSize: 11,
    textTransform: "uppercase",
    letterSpacing: 1,
  },

  separator: {
    borderBottomWidth: StyleSheet.hairlineWidth,
    marginHorizontal: 8,
    marginBottom: 8,
  },

  editorContainer: {
    flex: 1,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    backgroundColor: "rgba(15,23,42,0.98)",
    marginHorizontal: 8,
    overflow: "hidden",
  },
  editorScroll: { flex: 1 },
  editorScrollContent: {
    paddingHorizontal: 10,
    paddingVertical: 8,
  },
  editorInput: {
    minHeight: 200,
    color: "#e5e7eb",
    fontSize: 12,
    fontFamily: "monospace",
  },
});
