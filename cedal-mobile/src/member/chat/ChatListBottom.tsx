// src/member/chat/ChatListBottom.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";
import React from "react";
import { StyleSheet, Text, TouchableOpacity, View } from "react-native";

type TabKey = "chats" |"work" | "calls" | "fun";

type Props = {
  active: TabKey;
};

export default function ChatListBottom({ active }: Props) {
  const { colors } = useTheme();
  const router = useRouter();

  function go(tab: TabKey) {
    if (tab === "chats") {
      router.push("/(auth)/(member)/home" as any);
      } else if (tab === "work") {
        router.push("/(auth)/(member)/work" as any);
    } else if (tab === "calls") {
      router.push("/(auth)/(member)/calls" as any);
    } else if (tab === "fun") {
      router.push("/(auth)/(member)/fun" as any);
    }
  }

  return (
    <View
      style={[
        styles.bar,
        {
          borderTopColor: colors.border,
          backgroundColor: "#020617",
        },
      ]}
    >
      <TabButton
        label="Chats"
        icon="chatbubbles-outline"
        active={active === "chats"}
        onPress={() => go("chats")}
      />

      {
      <TabButton
        label="Work"
        icon="briefcase-outline"
        active={active === "work"}
        onPress={() => go("work")}
      />
      }

      <TabButton
        label="Calls"
        icon="call-outline"
        active={active === "calls"}
        onPress={() => go("calls")}
      />
      <TabButton
        label="Fun"
        icon="game-controller-outline"
        active={active === "fun"}
        onPress={() => go("fun")}
      />
    </View>
  );
}

type TabButtonProps = {
  label: string;
  icon: keyof typeof Ionicons.glyphMap;
  active: boolean;
  onPress: () => void;
};

function TabButton({ label, icon, active, onPress }: TabButtonProps) {
  return (
    <TouchableOpacity
      style={styles.tabItem}
      onPress={onPress}
      activeOpacity={0.7}
    >
      <Ionicons
        name={icon}
        size={18}
        color={active ? "#22c55e" : "#6b7280"}
      />
      <Text style={[styles.tabLabel, active && styles.tabLabelActive]}>
        {label}
      </Text>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  bar: {
    height: 60,
    borderTopWidth: StyleSheet.hairlineWidth,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-around",
  },
  tabItem: {
    alignItems: "center",
    justifyContent: "center",
  },
  tabLabel: {
    marginTop: 3,
    fontSize: 11,
    color: "#6b7280",
  },
  tabLabelActive: {
    color: "#e5e7eb",
    fontWeight: "600",
  },
});
