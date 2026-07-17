// src/member/chat/ChatListHeader.tsx
import { auth } from "@/src/api/firebase";
import { useUserProfile } from "@/src/hooks/useUserProfile";
import { useTheme } from "@/src/themes/ThemeContext";
import { useRouter } from "expo-router";
import { signOut } from "firebase/auth";
import React, { useState } from "react";
import {
  Image,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";

export default function ChatListHeader() {
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);
  const { colors } = useTheme();
  const { user, profile } = useUserProfile();

  const baseName = profile?.email?.split("@")[0] || "Design lab";
  const initial = baseName.charAt(0).toUpperCase();
  const avatarUrl = (profile as any)?.avatarUrl || null;

  function openProfile() {
    router.push("/(auth)/(member)/profile" as any);
  }

  function openSearch() {
    router.push("/(auth)/(member)/search" as any);
  }

  function toggleMenu() {
    setMenuOpen((v) => !v);
  }

  async function handleSignOut() {
  setMenuOpen(false);
  try {
    await signOut(auth);
    router.replace("/(auth)/sign-in" as any);
  } catch (e) {
    console.error("sign out error", e);
  }
}

  function handleMenuPress(item: string) {
    setMenuOpen(false);
    if (item === "settings") {
      router.push("/(auth)/(member)/settings" as any);
      return;
    }
    if (item === "about") {
      router.push("/(auth)/(member)/about" as any);
      return;
    }
    if (item === "rules") {
      router.push("/(auth)/(member)/rules" as any);
      return;
    }
     if (item === "shop") {
    router.push("/(auth)/(member)/shop" as any);
    return;
  }
    if (item === "history") {
      router.push("/(auth)/(member)/history" as any);
      return;
    }
    if (item === "bots") {
      router.push("/(auth)/(member)/bots" as any);
      return;
    }
  }

  return (
    <View
      style={[
        styles.header,
        {
          backgroundColor: colors.headerBackground,
          borderBottomColor: colors.border,
        },
      ]}
    >
      <View style={styles.left}>
        <TouchableOpacity onPress={openProfile} activeOpacity={0.7}>
          {avatarUrl ? (
            <Image source={{ uri: avatarUrl }} style={styles.avatar} />
          ) : (
            <View
              style={[
                styles.avatar,
                { alignItems: "center", justifyContent: "center" },
              ]}
            >
              <Text style={styles.avatarInitial}>{initial}</Text>
            </View>
          )}
        </TouchableOpacity>

        <View
          style={[
            styles.searchContainer,
            { borderColor: colors.border },
          ]}
        >
          <TextInput
            placeholder="Search chats"
            placeholderTextColor={colors.textSecondary}
            style={[styles.searchInput, { color: colors.textPrimary }]}
          />
        </View>
      </View>

      <View style={styles.actionsWrapper}>
        <View style={styles.actions}>
          <TouchableOpacity
            style={[styles.iconBtn, { borderColor: colors.border }]}
            onPress={openSearch}
          >
            <Text style={[styles.iconText, { color: colors.textPrimary }]}>
              ✚
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.iconBtn, { borderColor: colors.border }]}
            onPress={toggleMenu}
          >
            <Text style={[styles.iconText, { color: colors.textPrimary }]}>
              ⋮
            </Text>
          </TouchableOpacity>
        </View>

        {menuOpen && (
  <View
    style={[
      styles.menu,
      {
        backgroundColor: colors.headerBackground,
        borderColor: colors.border,
      },
    ]}
  >
    {["Settings", "About", "Rules", "Bots", "History", "Shop"].map(
      (label) => (
        <TouchableOpacity
          key={label}
          style={styles.menuItem}
          activeOpacity={0.8}
          onPress={() => handleMenuPress(label.toLowerCase())}
        >
          <Text
            style={[
              styles.menuText,
              { color: colors.textPrimary },
            ]}
          >
            {label}
          </Text>
        </TouchableOpacity>
      ),
    )}

    {/* Sign out item */}
    <TouchableOpacity
      style={styles.menuItem}
      activeOpacity={0.8}
      onPress={handleSignOut}
    >
      <Text style={[styles.menuText, styles.signOutText]}>
        Sign out
      </Text>
    </TouchableOpacity>
  </View>
)}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    paddingTop: 48,
    paddingHorizontal: 16,
    paddingBottom: 12,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    borderBottomWidth: StyleSheet.hairlineWidth,
    zIndex: 20,
    elevation: 20,
  },
  left: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
  },
  avatar: {
    width: 34,
    height: 34,
    borderRadius: 17,
    marginRight: 10,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.7)",
    backgroundColor: "#020617",
  },
  avatarInitial: {
    fontSize: 16,
    color: "#e0f2fe",
  },
  searchContainer: {
    flex: 1,
    borderRadius: 999,
    borderWidth: 1,
    paddingHorizontal: 12,
    paddingVertical: 6,
    justifyContent: "center",
  },
  searchInput: {
    fontSize: 14,
    padding: 0,
  },
  actionsWrapper: {
    position: "relative",
    marginLeft: 12,
    zIndex: 21,
  },
  actions: {
    flexDirection: "row",
    gap: 8,
  },
  iconBtn: {
    width: 30,
    height: 30,
    borderRadius: 999,
    borderWidth: 1,
    alignItems: "center",
    justifyContent: "center",
  },
  iconText: {
    fontSize: 16,
  },
  menu: {
    position: "absolute",
    top: 38,
    right: 0,
    borderRadius: 12,
    paddingVertical: 4,
    minWidth: 140,
    shadowColor: "#000",
    shadowOpacity: 0.6,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 8 },
    elevation: 30,
    zIndex: 30,
    borderWidth: 1,
  },
  menuItem: {
    paddingVertical: 8,
    paddingHorizontal: 12,
  },
  menuText: {
    fontSize: 13,
    letterSpacing: 1,
    textTransform: "uppercase",
  },
  signOutText: {
    color: "#f97373",
  },
});

export { };

