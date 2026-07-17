// app/(auth)/(member)/settings.tsx
//import BankSettingsSection from "@/src/member/settings/BankSettingsSection";
import CallSettingsSection from "@/src/member/settings/CallSettingsSection";
import ChatSettingsSection from "@/src/member/settings/ChatSettingsSection";
import GroupSettingsSection from "@/src/member/settings/GroupSettingsSection";
import NavigationSettingsSection from "@/src/member/settings/NavigationSettingsSection";
import SecuritySettingsSection from "@/src/member/settings/SecuritySettingsSection";
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";
import React, { useMemo, useState } from "react";
import {
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";

export default function SettingsScreen() {
  const router = useRouter();
  const { colors } = useTheme();

  const [searchOpen, setSearchOpen] = useState(false);
  const [search, setSearch] = useState("");

  function handleBack() {
    router.back();
  }

  const hasQuery = search.trim().length > 0;
  const query = search.trim().toLowerCase();

  // Which sections should be searched and how to label them
  const sections = useMemo(
    () => [
      { key: "chat", title: "Chat", component: ChatSettingsSection },
      { key: "groups", title: "Groups", component: GroupSettingsSection },
      { key: "nav", title: "Navigation", component: NavigationSettingsSection },
      { key: "call", title: "Calls", component: CallSettingsSection },
      { key: "security", title: "Security", component: SecuritySettingsSection },
      //{ key: "bank", title: "Bank", component: BankSettingsSection },
    ],
    []
  );

  // For now, we keep it simple: search only by section title.
  // Later you can expose per-row metadata and filter inside each section.
  const filteredSections = useMemo(() => {
    if (!hasQuery) return sections;
    return sections.filter((s) =>
      s.title.toLowerCase().includes(query)
    );
  }, [sections, hasQuery, query]);

  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      <View
        style={[
          styles.topBar,
          {
            borderBottomColor: colors.border,
            backgroundColor:
              (colors as any).headerBackground ?? colors.background,
          },
        ]}
      >
        <TouchableOpacity
          onPress={handleBack}
          activeOpacity={0.7}
          style={styles.backBtn}
        >
          <Text style={[styles.backText, { color: colors.textPrimary }]}>
            Back
          </Text>
        </TouchableOpacity>

        <Text style={[styles.topTitle, { color: colors.textPrimary }]}>
          Settings
        </Text>

        <View style={styles.topBarRight}>
          <TouchableOpacity
            activeOpacity={0.8}
            onPress={() => {
              if (searchOpen) {
                setSearch("");
              }
              setSearchOpen((v) => !v);
            }}
            style={styles.iconBtn}
          >
            <Ionicons
              name={searchOpen ? "close" : "search-outline"}
              size={16}
              color={colors.textPrimary}
            />
          </TouchableOpacity>
        </View>
      </View>

      {searchOpen && (
        <View
          style={[
            styles.searchBar,
            {
              borderBottomColor: colors.border,
              backgroundColor:
                (colors as any).headerBackground ?? colors.background,
            },
          ]}
        >
          <Ionicons
            name="search-outline"
            size={14}
            color="#6b7280"
            style={{ marginRight: 6 }}
          />
          <TextInput
            value={search}
            onChangeText={setSearch}
            placeholder="Search settings (e.g. notification, groups)…"
            placeholderTextColor="#6b7280"
            style={[styles.searchInput, { color: colors.textPrimary }]}
          />
        </View>
      )}

      <ScrollView contentContainerStyle={styles.content}>
        {filteredSections.map((s) => {
          const SectionComp = s.component;
          return <SectionComp key={s.key} />;
        })}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
  },
  topBar: {
    paddingTop: 48,
    paddingHorizontal: 16,
    paddingBottom: 12,
    flexDirection: "row",
    alignItems: "center",
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  backBtn: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
    marginRight: 12,
  },
  backText: {
    fontSize: 13,
    letterSpacing: 1.5,
  },
  topTitle: {
    fontSize: 16,
    fontWeight: "600",
    letterSpacing: 2,
    textTransform: "uppercase",
    flex: 1,
  },
  topBarRight: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8,
  },
  iconBtn: {
    paddingHorizontal: 10,
    paddingVertical: 6,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.7)",
  },
  searchBar: {
    flexDirection: "row",
    alignItems: "center",
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  searchInput: {
    flex: 1,
    fontSize: 13,
  },
  content: {
    paddingHorizontal: 16,
    paddingVertical: 16,
    paddingBottom: 32,
  },
});
