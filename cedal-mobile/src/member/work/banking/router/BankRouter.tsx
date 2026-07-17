// src/member/work/banking/router/BankRouter.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import { Ionicons } from "@expo/vector-icons";
import React, { useState } from "react";
import {
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";
import {
  SafeAreaView,
  useSafeAreaInsets,
} from "react-native-safe-area-context";
import BankingScreen from "../screens/BankingScreen";
import DebtScreen from "../screens/DebtScreen";
import NotificationsScreen from "../screens/NotificationsScreen";
import SendScreen from "../screens/SendScreen";
import TasksScreen from "../screens/TasksScreen";
import TradeScreen from "../screens/TradeScreen";
import TransactionsScreen from "../screens/TransactionsScreen";

type BankTabKey = "home" | "tasks" | "credit" | "transactions";

type Route =
  | { name: "home" }
  | { name: "tasks" }
  | { name: "credit" }
  | { name: "transactions" }
  | { name: "trade" }
  | { name: "notifications" }
  | { name: "debt" };

export default function BankRouter() {
  const insets = useSafeAreaInsets();
  const { colors } = useTheme();
  const [activeTab, setActiveTab] = useState<BankTabKey>("home");
  const [route, setRoute] = useState<Route>({ name: "home" });

  const goHome = () => {
    setActiveTab("home");
    setRoute({ name: "home" });
  };

  const goTasks = () => {
    setActiveTab("tasks");
    setRoute({ name: "tasks" });
  };

  const goCredit = () => {
    setActiveTab("credit");
    setRoute({ name: "credit" });
  };

  const goTransactions = () => {
    setActiveTab("transactions");
    setRoute({ name: "transactions" });
  };

  const goTrade = () => {
    setRoute({ name: "trade" });
  };

  const goNotifications = () => {
    setRoute({ name: "notifications" });
  };

  const goDebt = () => {
    setRoute({ name: "debt" });
  };

  let screen = null;
  if (route.name === "home") {
    screen = (
      <BankingScreen
        onOpenTransactions={goTransactions}
        onOpenDebt={goDebt}
        onOpenTrade={goTrade}
        onOpenNotifications={goNotifications}
      />
    );
  } else if (route.name === "tasks") {
    screen = <TasksScreen onBackHome={goHome} />;
  } else if (route.name === "credit") {
    screen = <SendScreen onBackHome={goHome} />;
  } else if (route.name === "transactions") {
    screen = <TransactionsScreen onBack={goHome} />;
  } else if (route.name === "trade") {
    screen = <TradeScreen onBackHome={goHome} />;
  } else if (route.name === "notifications") {
    screen = <NotificationsScreen onBackHome={goHome} />;
  } else if (route.name === "debt") {
    screen = <DebtScreen onBackHome={goHome} />;
  }

  return (
    <SafeAreaView style={{ flex: 1 }}>
      <View style={styles.root}>
        <View style={styles.content}>{screen}</View>

        <View
          style={[
            styles.bottomBar,
            {
              borderTopColor: colors.border,
              marginBottom: insets.bottom + 4,
            },
          ]}
        >
          <BankTab
            label="Home"
            icon="home-outline"
            active={activeTab === "home"}
            onPress={goHome}
          />
          <BankTab
            label="Tasks"
            icon="briefcase"
            active={activeTab === "tasks"}
            onPress={goTasks}
          />
          <BankTab
            label="Credit"
            icon="send-outline"
            active={activeTab === "credit"}
            onPress={goCredit}
          />
          <BankTab
            label="Transactions"
            icon="time-outline"
            active={activeTab === "transactions"}
            onPress={goTransactions}
          />
        </View>
      </View>
    </SafeAreaView>
  );
}

type BankTabProps = {
  label: string;
  icon: keyof typeof Ionicons.glyphMap;
  active?: boolean;
  onPress?: () => void;
};

function BankTab({ label, icon, active, onPress }: BankTabProps) {
  return (
    <TouchableOpacity
      style={styles.tabItem}
      activeOpacity={0.7}
      onPress={onPress}
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
  root: {
    flex: 1,
  },
  content: {
    flex: 1,
  },
  centered: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
  },
  bottomBar: {
    height: 60,
    borderTopWidth: StyleSheet.hairlineWidth,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-around",
    backgroundColor: "#020617",
    marginHorizontal: 1,
    borderRadius: 16,
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
