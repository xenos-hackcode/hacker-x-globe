// src/member/chat/ChatScreenHeader.tsx
import React, { useState } from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Platform,
  Image,
} from "react-native";
import { useRouter } from "expo-router";

type PresenceStatus = "offline" | "online" | "inChat";

type Props = {
  title: string;
  subtitle: string;
  onBack: () => void;
  onStartVideoCall?: () => void;
  avatarInitial?: string;
  avatarUri?: string;
  avatarSource?: any;
  userId?: string;
  presence?: PresenceStatus;
  onClearChat?: () => void;      
  onOpenInfo?: () => void;         
  onOpenDisappearing?: () => void; 
  onOpenSearch?: () => void;  
  onOpenTheme?: () => void; 
  onOpenReport?: () => void;   
  onOpenSaved?: () => void;  
};

export default function ChatScreenHeader({
  title,
  subtitle,
  onBack,
  onStartVideoCall,
  avatarInitial,
  avatarUri,
  avatarSource,
  userId,
  presence = "offline",
  onClearChat,
  onOpenInfo,
  onOpenDisappearing,
  onOpenSearch,
  onOpenTheme,
  onOpenReport, 
  onOpenSaved,
}: Props) {
  const router = useRouter();
  const [menuOpen, setMenuOpen] = useState(false);

  const initial = avatarInitial || title?.charAt(0)?.toUpperCase() || "C";

  function onAvatarPress() {
    if (!userId) return;
    router.push({
      pathname: "/(auth)/(member)/user/[userId]",
      params: { userId },
    } as any);
  }

  let dotColor = "#000000";
  if (presence === "online") dotColor = "#22c55e";
  if (presence === "inChat") dotColor = "#ef4444";

  function handleMenuPress(key: string) {
  setMenuOpen(false);

  if (key === "clear" && onClearChat) {
    onClearChat();
    return;
  }
  if (key === "info" && onOpenInfo) {
    onOpenInfo();
    return;
  }
  if (key === "disappearing" && onOpenDisappearing) {
    onOpenDisappearing();
    return;
  }
  if (key === "search" && onOpenSearch) {
    onOpenSearch();
    return;
  }
    if (key === "theme" && onOpenTheme) {
    onOpenTheme();
    return;
  }
  if (key === "report" && onOpenReport) {
    onOpenReport();          // ← this was missing
    return;
  }
  // other keys can stay TODO for now
}

  const menuItems = [
    { key: "info", label: "Chat info" },
    { key: "disappearing", label: "Msg disappearing" },
    { key: "theme", label: "Chat theme" },
    { key: "search", label: "Search" },
    { key: "clear", label: "Clear chat" },
    { key: "report", label: "Report" },
    // no "exit" here for DMs
  ];

  return (
    <View style={styles.header}>
      <TouchableOpacity
        onPress={onBack}
        activeOpacity={0.7}
        style={styles.backButton}
      >
        <Text style={styles.backIcon}>◀</Text>
      </TouchableOpacity>

      <View style={styles.centerRow}>
        <TouchableOpacity
          activeOpacity={0.8}
          style={styles.avatar}
          onPress={onAvatarPress}
        >
          {avatarSource ? (
            <Image source={avatarSource} style={styles.avatarImage} />
          ) : avatarUri && avatarUri.startsWith("http") ? (
            <Image source={{ uri: avatarUri }} style={styles.avatarImage} />
          ) : (
            <Text style={styles.avatarInitial}>{initial}</Text>
          )}
        </TouchableOpacity>

        <View style={styles.textBlock}>
          <Text numberOfLines={1} style={styles.title}>
            {title}
          </Text>
          <View style={styles.sublineRow}>
            <View style={[styles.statusDot, { backgroundColor: dotColor }]} />
            <Text numberOfLines={1} style={styles.subtitle}>
              {subtitle}
            </Text>
          </View>
        </View>
      </View>

      {/* Right actions: camera + 3-dot menu with dropdown */}
      <View style={styles.rightActions}>
        <TouchableOpacity
          activeOpacity={0.8}
          style={styles.iconButton}
          onPress={onStartVideoCall}
        >
          <Text style={styles.iconText}>🎥</Text>
        </TouchableOpacity>

        <View style={styles.menuWrapper}>
          <TouchableOpacity
            activeOpacity={0.8}
            style={[styles.iconButton, { marginLeft: 8 }]}
            onPress={() => setMenuOpen((v) => !v)}
          >
            <Text style={styles.iconText}>⋮</Text>
          </TouchableOpacity>

          {menuOpen && (
            <View style={styles.menu}>
              {menuItems.map((item) => (
                <TouchableOpacity
                  key={item.key}
                  activeOpacity={0.8}
                  style={styles.menuItem}
                  onPress={() => handleMenuPress(item.key)}
                >
                  <Text
                    style={[
                      styles.menuText,
                      item.key === "report" && styles.menuReportText,
                    ]}
                  >
                    {item.label}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          )}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    paddingTop: Platform.select({ ios: 50, android: 30 }),
    paddingBottom: 14,
    paddingHorizontal: 12,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    borderBottomWidth: 1,
    borderBottomColor: "rgba(15,23,42,1)",
    backgroundColor: "rgba(2,6,23,0.98)",
  },
  backButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.3)",
    backgroundColor: "rgba(15,23,42,0.8)",
    alignItems: "center",
    justifyContent: "center",
  },
  backIcon: { fontSize: 16, color: "#e5e7eb" },
  centerRow: {
    flex: 1,
    flexDirection: "row",
    alignItems: "center",
    marginHorizontal: 12,
  },
  avatar: {
    width: 40,
    height: 40,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.7)",
    backgroundColor: "rgba(15,23,42,1)",
    alignItems: "center",
    justifyContent: "center",
    overflow: "hidden",
    marginRight: 10,
  },
  avatarImage: { width: "100%", height: "100%", borderRadius: 12 },
  avatarInitial: { fontSize: 18, color: "#e0f2fe", fontWeight: "600" },
  textBlock: { flex: 1 },
  title: {
    fontSize: 15,
    fontWeight: "600",
    letterSpacing: 1,
    textTransform: "uppercase",
    color: "#e5e7eb",
  },
  sublineRow: { flexDirection: "row", alignItems: "center", marginTop: 2 },
  statusDot: {
    width: 7,
    height: 7,
    borderRadius: 3.5,
    marginRight: 6,
    backgroundColor: "#22c55e",
  },
  subtitle: { fontSize: 12, color: "#9ca3af" },

  rightActions: {
    flexDirection: "row",
    alignItems: "center",
  },
  iconButton: {
    width: 36,
    height: 36,
    borderRadius: 12,
    backgroundColor: "rgba(15,23,42,0.9)",
    alignItems: "center",
    justifyContent: "center",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.3)",
  },
  iconText: {
    fontSize: 16,
    color: "#e0f2fe",
  },

  menuWrapper: {
    marginLeft: 0,
    position: "relative",
  },
  menu: {
    position: "absolute",
    top: 40,
    right: 0,
    borderRadius: 12,
    paddingVertical: 4,
    minWidth: 170,
    backgroundColor: "rgba(15,23,42,0.98)",
    borderWidth: 1,
    borderColor: "rgba(30,64,175,0.9)",
    shadowColor: "#000",
    shadowOpacity: 0.6,
    shadowRadius: 16,
    shadowOffset: { width: 0, height: 8 },
    zIndex: 40,
    elevation: 30,
  },
  menuItem: {
    paddingVertical: 8,
    paddingHorizontal: 12,
  },
  menuText: {
    fontSize: 13,
    color: "#e5e7eb",
  },
  menuReportText: {
    color: "#fb923c",
  },
});
