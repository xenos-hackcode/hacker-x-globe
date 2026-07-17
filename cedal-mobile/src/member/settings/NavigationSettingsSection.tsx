// src/member/settings/NavigationSettingsSection.tsx
import SettingsRow from "@/src/member/settings/SettingsRow";
import { useTheme } from "@/src/themes/ThemeContext";
import { useRouter } from "expo-router";
import React, { useState } from "react";
import {
  Alert,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";

import { auth } from "@/src/api/firebase";
import {
  EmailAuthProvider,
  linkWithCredential,
} from "firebase/auth";

type LinkMode = "guest_to_email";

const SUPPORTED_LANGUAGES = [
  { code: "en", label: "English" },
  { code: "fr", label: "Français" },
] as const;

type AppLanguage = (typeof SUPPORTED_LANGUAGES)[number]["code"];

export default function NavigationSettingsSection() {
  const router = useRouter();
  const { colors, isDark, toggleTheme } = useTheme();

  const [language, setLanguage] = useState<AppLanguage>("en");

  const [toast, setToast] = useState<string | null>(null);

  const [linkMode, setLinkMode] = useState<LinkMode | null>(null);
  const [linkEmail, setLinkEmail] = useState("");
  const [linkPassword, setLinkPassword] = useState("");
  const [linkLoading, setLinkLoading] = useState(false);

  function showToast(message: string) {
    setToast(message);
    setTimeout(() => setToast(null), 4000);
  }

  async function handleLinkSubmit() {
    console.log("handleLinkSubmit start", { linkMode });

    try {
      if (!linkMode) {
        Alert.alert("Link account", "Pick what you want to link first.");
        return;
      }

      setLinkLoading(true);

      if (linkMode === "guest_to_email") {
        if (!linkEmail.trim() || !linkPassword.trim()) {
          Alert.alert(
            "Missing info",
            "Enter an email and password to bind this guest node."
          );
          return;
        }

        await linkGuestToEmailPassword(
          linkEmail.trim(),
          linkPassword.trim()
        );

        Alert.alert(
          "Guest upgraded",
          "You can now sign in with this email and password on any device."
        );
        showToast("Guest node bound to email.");
      }

      setLinkMode(null);
      setLinkEmail("");
      setLinkPassword("");
    } catch (e: any) {
      console.log("handleLinkSubmit error", e);
      if (e?.code === "auth/email-already-in-use") {
        Alert.alert(
          "Email already exists",
          "That email is already in use by another account. Try a different email or sign in with that account."
        );
      } else if (e?.code === "auth/invalid-email") {
        Alert.alert("Invalid email", "Enter a valid email address.");
      } else if (e?.code === "auth/weak-password") {
        Alert.alert(
          "Weak password",
          "Use a stronger password for this account."
        );
      } else {
        Alert.alert(
          "Link error",
          e?.message ?? "Could not link this account."
        );
      }
    } finally {
      setLinkLoading(false);
    }
  }

  return (
    <>
      {toast && (
        <View style={styles.toast}>
          <Text style={styles.toastText}>{toast}</Text>
        </View>
      )}

      <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>
        Navigation
      </Text>

      <View
        style={[
          styles.card,
          { borderColor: colors.border, backgroundColor: colors.background },
        ]}
      >
        {/* Theme */}
        <SettingsRow
          label="Theme"
          description={
            isDark
              ? "Cedal mesh is running in night mode."
              : "Switch to the cyber-night theme."
          }
          value={isDark}
          onValueChange={() => toggleTheme()}
        />

        {/* Language */}
        <View style={styles.row}>
          <View style={{ flex: 1 }}>
            <Text style={[styles.rowLabel, { color: colors.textPrimary }]}>
              Language
            </Text>
            <Text
              style={[
                styles.rowDescription,
                { color: colors.textSecondary },
              ]}
            >
              Choose the language Cedal uses.
            </Text>
          </View>

          <View style={{ flexDirection: "row", gap: 6 }}>
            {SUPPORTED_LANGUAGES.map((lang) => {
              const active = language === lang.code;
              return (
                <TouchableOpacity
                  key={lang.code}
                  style={[
                    styles.langChip,
                    active && styles.langChipActive,
                  ]}
                  onPress={() => {
                    setLanguage(lang.code);
                  }}
                  activeOpacity={0.8}
                >
                  <Text
                    style={[
                      styles.langChipText,
                      active && styles.langChipTextActive,
                    ]}
                  >
                    {lang.label}
                  </Text>
                </TouchableOpacity>
              );
            })}
          </View>
        </View>

        {/* Update passcode */}
        <TouchableOpacity
          style={styles.updatePasscodeRow}
          activeOpacity={0.8}
          onPress={() => {
            Alert.alert(
              "Update passcode",
              "Do you want to update your Cedal access passcode now?",
              [
                { text: "Cancel", style: "cancel" },
                {
                  text: "Yes",
                  style: "default",
                  onPress: () => router.push("/passcode-update"),
                },
              ]
            );
          }}
        >
          <View style={{ flex: 1 }}>
            <Text style={[styles.rowLabel, { color: colors.textPrimary }]}>
              Update passcode
            </Text>
            <Text
              style={[
                styles.rowDescription,
                { color: colors.textSecondary },
              ]}
            >
              Change the code you use to unlock Cedal.
            </Text>
          </View>
          <Text style={[styles.chevron, { color: colors.textSecondary }]}>
            ›
          </Text>
        </TouchableOpacity>

        {/* Link guest account */}
        <View
          style={[
            styles.row,
            styles.rowLast,
            { flexDirection: "column", alignItems: "flex-start" },
          ]}
        >
          <Text style={[styles.rowLabel, { color: colors.textPrimary }]}>
            Link guest node
          </Text>
          <Text
            style={[
              styles.rowDescription,
              { color: colors.textSecondary, marginBottom: 8 },
            ]}
          >
            Bind this guest node to an email so you can sign in or recover it later.
          </Text>

          {/* Link mode chooser (only guest) */}
          <View style={styles.linkModeRow}>
            <TouchableOpacity
              style={[
                styles.linkModeBtn,
                linkMode === "guest_to_email" && styles.linkModeBtnActive,
              ]}
              activeOpacity={0.8}
              onPress={() =>
                setLinkMode((prev) =>
                  prev === "guest_to_email" ? null : "guest_to_email"
                )
              }
            >
              <Text
                style={[
                  styles.linkModeText,
                  linkMode === "guest_to_email" &&
                    styles.linkModeTextActive,
                ]}
              >
                Guest → email + password
              </Text>
            </TouchableOpacity>
          </View>

          {/* Guest → email + password form */}
          {linkMode === "guest_to_email" && (
            <View style={styles.linkForm}>
              <Text style={styles.linkFormLabel}>Email</Text>
              <TextInput
                value={linkEmail}
                onChangeText={setLinkEmail}
                placeholder="you@example.com"
                placeholderTextColor="#6b7280"
                style={styles.linkInput}
                autoCapitalize="none"
                keyboardType="email-address"
              />

              <Text style={[styles.linkFormLabel, { marginTop: 6 }]}>
                Password
              </Text>
              <TextInput
                value={linkPassword}
                onChangeText={setLinkPassword}
                placeholder="Create a password"
                placeholderTextColor="#6b7280"
                style={styles.linkInput}
                secureTextEntry
              />

              <TouchableOpacity
                style={styles.linkSubmitBtn}
                activeOpacity={0.8}
                onPress={handleLinkSubmit}
                disabled={linkLoading}
              >
                <Text style={styles.linkSubmitText}>
                  {linkLoading ? "Binding…" : "Bind guest to email"}
                </Text>
              </TouchableOpacity>
            </View>
          )}
        </View>
      </View>
    </>
  );
}

/**
 * Guest → email+password
 * - Only call this if auth.currentUser.isAnonymous === true.
 * - Keeps the same UID and all existing Firestore data.[web:282]
 */
async function linkGuestToEmailPassword(
  email: string,
  password: string
) {
  const user = auth.currentUser;
  if (!user) throw new Error("No signed-in user to link.");
  if (!user.isAnonymous)
    throw new Error("This account is already linked.");

  const credential = EmailAuthProvider.credential(email, password);
  await linkWithCredential(user, credential); // [web:282]
}

const styles = StyleSheet.create({
  toast: {
    position: "absolute",
    top: 40,
    left: 16,
    right: 16,
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 999,
    backgroundColor: "#16a34a",
    zIndex: 20,
  },
  toastText: {
    color: "#ecfdf5",
    fontSize: 12,
    textAlign: "center",
  },
  sectionLabel: {
    fontSize: 11,
    letterSpacing: 2,
    textTransform: "uppercase",
    marginBottom: 6,
  },
  card: {
    borderRadius: 18,
    borderWidth: 1,
    marginBottom: 18,
    overflow: "hidden",
  },
  row: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(31,41,55,0.9)",
  },
  rowLast: {
    borderBottomWidth: 0,
  },
  rowLabel: {
    fontSize: 13,
    fontWeight: "500",
  },
  rowDescription: {
    fontSize: 11,
    marginTop: 2,
  },
  chevron: {
    fontSize: 18,
    marginLeft: 8,
  },
  updatePasscodeRow: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(31,41,55,0.9)",
  },
  langChip: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(55,65,81,0.8)",
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  langChipActive: {
    borderColor: "#3b82f6",
    backgroundColor: "rgba(59,130,246,0.15)",
  },
  langChipText: {
    fontSize: 11,
    color: "#9ca3af",
  },
  langChipTextActive: {
    color: "#dbeafe",
  },
  linkModeRow: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8,
    marginTop: 4,
  },
  linkModeBtn: {
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(55,65,81,0.8)",
    paddingHorizontal: 10,
    paddingVertical: 4,
  },
  linkModeBtnActive: {
    borderColor: "#22c55e",
    backgroundColor: "rgba(34,197,94,0.1)",
  },
  linkModeText: {
    fontSize: 11,
    color: "#9ca3af",
  },
  linkModeTextActive: {
    color: "#bbf7d0",
  },
  linkForm: {
    width: "100%",
    marginTop: 8,
  },
  linkFormLabel: {
    fontSize: 11,
    textTransform: "uppercase",
    letterSpacing: 0.12,
    color: "#9ca3af",
  },
  linkInput: {
    marginTop: 3,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(55,65,81,0.9)",
    backgroundColor: "#020617",
    paddingHorizontal: 10,
    paddingVertical: 6,
    fontSize: 12,
    color: "#e5e7eb",
  },
  linkSubmitBtn: {
    marginTop: 10,
    borderRadius: 999,
    paddingVertical: 7,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#22c55e",
  },
  linkSubmitText: {
    fontSize: 12,
    fontWeight: "600",
    color: "#022c22",
    letterSpacing: 0.5,
    textTransform: "uppercase",
  },
});
