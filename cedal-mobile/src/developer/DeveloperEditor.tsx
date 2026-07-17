// src/developer/DeveloperEditor.tsx
import React from "react";
import {
  View,
  Text,
  StyleSheet,
  TextInput,
  ScrollView,
  TouchableOpacity,
} from "react-native";

const LANGUAGES = ["TypeScript", "JavaScript", "Python", "HTML", "JSON"] as const;
type CodeLanguage = (typeof LANGUAGES)[number];

type Props = {
  fileName: string;
  onChangeFileName: (name: string) => void;
  language: CodeLanguage;
  onChangeLanguage: (lang: CodeLanguage) => void;
  code: string;
  onChangeCode: (code: string) => void;

  // folder + save
  currentFolder: string | null; // e.g. "/src/sub"
  availableFolders: string[]; // e.g. ["/src", "/src/sub", ...]
  subFolders: string[]; // names of immediate subfolders of currentFolder
  onChangeFolder: (folder: string | null) => void;
  onSave: () => void;
};

export default function DeveloperEditor({
  fileName,
  onChangeFileName,
  language,
  onChangeLanguage,
  code,
  onChangeCode,
  currentFolder,
  availableFolders,
  subFolders,
  onChangeFolder,
  onSave,
}: Props) {
  const pathLabel =
    currentFolder && currentFolder.length > 0
      ? `Workspace ${currentFolder}`
      : "Workspace /";

  return (
    <View style={styles.root}>
      {/* Top row: Save button */}
      <View style={styles.topRow}>
        <View />
        <TouchableOpacity style={styles.saveButton} onPress={onSave}>
          <Text style={styles.saveButtonText}>Save</Text>
        </TouchableOpacity>
      </View>

      {/* Path */}
      <View style={styles.pathRow}>
        <Text style={styles.pathText}>{pathLabel}</Text>
      </View>

      {/* File + Folder row */}
      <View style={styles.metaRow}>
        {/* File name editable */}
        <View style={styles.fileNameBox}>
          <Text style={styles.fileNameLabel}>FILE</Text>
          <TextInput
            style={styles.fileNameInput}
            value={fileName}
            onChangeText={onChangeFileName}
            placeholder="Untitled.tsx"
            placeholderTextColor="#4b5563"
            autoCapitalize="none"
          />
        </View>

        {/* Folder dropdown (all folders) */}
        <View style={styles.folderBox}>
          <Text style={styles.folderLabel}>FOLDER</Text>
          <ScrollView
            horizontal
            showsHorizontalScrollIndicator={false}
            contentContainerStyle={styles.folderPillsRow}
          >
            <TouchableOpacity
              onPress={() => onChangeFolder(null)}
              style={[
                styles.folderPill,
                !currentFolder && styles.folderPillActive,
              ]}
            >
              <Text
                style={[
                  styles.folderPillText,
                  !currentFolder && styles.folderPillTextActive,
                ]}
              >
                /
              </Text>
            </TouchableOpacity>

            {availableFolders.map((folderPath) => {
              const active = currentFolder === folderPath;
              const label =
                folderPath === "/" ? "/" : folderPath.replace(/^\//, "");
              return (
                <TouchableOpacity
                  key={folderPath}
                  onPress={() => onChangeFolder(folderPath)}
                  style={[
                    styles.folderPill,
                    active && styles.folderPillActive,
                  ]}
                >
                  <Text
                    style={[
                      styles.folderPillText,
                      active && styles.folderPillTextActive,
                    ]}
                  >
                    {label}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </ScrollView>
        </View>
      </View>

      {/* Subfolder row */}
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
                onPress={() => {
                  // onChangeFolder expects full path; append to currentFolder
                  const base = currentFolder && currentFolder !== "/" ? currentFolder : "";
                  const nextPath =
                    base === "" ? `/${name}` : `${base}/${name}`;
                  onChangeFolder(nextPath);
                }}
                style={styles.folderPill}
              >
                <Text style={styles.folderPillText}>{name}</Text>
              </TouchableOpacity>
            ))}
          </ScrollView>
        )}
      </View>

      {/* Language row */}
      <View style={styles.languageBox}>
        <Text style={styles.languageLabel}>LANGUAGE</Text>
        <ScrollView
          horizontal
          showsHorizontalScrollIndicator={false}
          contentContainerStyle={styles.languagePillsRow}
        >
          {LANGUAGES.map((lang) => (
            <TouchableOpacity
              key={lang}
              onPress={() => onChangeLanguage(lang)}
              style={[
                styles.languagePill,
                language === lang && styles.languagePillActive,
              ]}
            >
              <Text
                style={[
                  styles.languagePillText,
                  language === lang && styles.languagePillTextActive,
                ]}
              >
                {lang}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      {/* Scrollable code editor */}
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
            placeholder={`Start typing ${language} here...`}
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
  root: {
    flex: 1,
  },
  topRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    marginBottom: 6,
  },
  saveButton: {
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#22d3ee",
    backgroundColor: "rgba(15,23,42,0.95)",
  },
  saveButtonText: {
    color: "#e5e7eb",
    fontSize: 11,
    fontWeight: "600",
  },
  pathRow: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 8,
  },
  pathText: {
    color: "#9ca3af",
    fontSize: 11,
  },
  metaRow: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 8,
    gap: 8,
  },
  fileNameBox: {
    flex: 1,
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    backgroundColor: "rgba(15,23,42,0.98)",
  },
  fileNameLabel: {
    color: "#6b7280",
    fontSize: 9,
    letterSpacing: 1,
  },
  fileNameInput: {
    color: "#e5e7eb",
    fontSize: 12,
    marginTop: 2,
  },
  folderBox: {
    flex: 1,
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    backgroundColor: "rgba(15,23,42,0.98)",
  },
  folderLabel: {
    color: "#6b7280",
    fontSize: 9,
    letterSpacing: 1,
    marginBottom: 4,
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
  folderPillActive: {
    borderColor: "#22d3ee",
    backgroundColor: "rgba(34,211,238,0.12)",
  },
  folderPillText: {
    color: "#9ca3af",
    fontSize: 10,
  },
  folderPillTextActive: {
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
    marginBottom: 10,
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
  languageBox: {
    paddingVertical: 6,
    paddingHorizontal: 10,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    backgroundColor: "rgba(15,23,42,0.98)",
    marginBottom: 10,
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
  editorContainer: {
    flex: 1,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    backgroundColor: "rgba(15,23,42,0.98)",
    overflow: "hidden",
  },
  editorScroll: {
    flex: 1,
  },
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
