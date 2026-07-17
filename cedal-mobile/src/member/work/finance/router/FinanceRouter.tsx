// src/member/work/finance/router/FinanceRouter.tsx
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
import AssetScreen from "../screens/AssetScreen";
import LearnScreen from "../screens/LearnScreen";
import OverviewScreen from "../screens/OverviewScreen";
import TradeScreen from "../screens/TradeScreen"; // NEW
import WatchlistScreen from "../screens/WatchlistScreen";

type FinanceTabKey = "overview" | "watchlist" | "trade" | "learn";

type Route =
  | { name: "overview" }
  | { name: "watchlist" }
  | { name: "asset"; symbol: string }
  | { name: "trade" }
  | { name: "learn" };

export default function FinanceRouter() {
  const insets = useSafeAreaInsets();
  const { colors } = useTheme();
  const [activeTab, setActiveTab] = useState<FinanceTabKey>("overview");
  const [route, setRoute] = useState<Route>({ name: "overview" });

  const goOverview = () => {
    setActiveTab("overview");
    setRoute({ name: "overview" });
  };

  const goWatchlist = () => {
    setActiveTab("watchlist");
    setRoute({ name: "watchlist" });
  };

  const goTrade = () => {
    setActiveTab("trade");
    setRoute({ name: "trade" });
  };

  const goLearn = () => {
    setActiveTab("learn");
    setRoute({ name: "learn" });
  };

  const goAsset = (symbol: string) => {
    setRoute({ name: "asset", symbol });
  };

  let screen = null;
  if (route.name === "overview") {
    screen = (
      <OverviewScreen
        onOpenWatchlist={goWatchlist}
        onOpenAsset={goAsset}
      />
    );
  } else if (route.name === "watchlist") {
    screen = (
      <WatchlistScreen
        onBack={goOverview}
        onOpenAsset={goAsset}
      />
    );
  } else if (route.name === "asset") {
    screen = (
      <AssetScreen
        symbol={route.symbol}
        onBack={goOverview}
      />
    );
  } else if (route.name === "trade") {
    screen = (
      <TradeScreen
        onBack={goOverview}
        onOpenAsset={goAsset}
      />
    );
  } else if (route.name === "learn") {
  screen = (
    <LearnScreen
      onBack={goOverview}
    />
  );
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
          <FinanceTab
            label="Overview"
            icon="stats-chart-outline"
            active={activeTab === "overview"}
            onPress={goOverview}
          />
          <FinanceTab
            label="Watchlist"
            icon="star-outline"
            active={activeTab === "watchlist"}
            onPress={goWatchlist}
          />
          <FinanceTab
            label="Trade"
            icon="swap-horizontal-outline"
            active={activeTab === "trade"}
            onPress={goTrade}
          />
          <FinanceTab
            label="Learn"
            icon="book-outline"
            active={activeTab === "learn"}
            onPress={goLearn}
          />
        </View>
      </View>
    </SafeAreaView>
  );
}

type FinanceTabProps = {
  label: string;
  icon: keyof typeof Ionicons.glyphMap;
  active?: boolean;
  onPress?: () => void;
};

function FinanceTab({ label, icon, active, onPress }: FinanceTabProps) {
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
