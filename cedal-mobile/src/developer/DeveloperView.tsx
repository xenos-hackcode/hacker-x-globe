// src/developer/DeveloperView.tsx
import React from "react";
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  Pressable,
  TextInput,
  ScrollView,
  Linking,
  Platform,
  Animated,
} from "react-native";

/** Cedal command type used by the dev console */
export type CedalCommand =
  | { type: "VIBRATE"; durationMs: number | number[] }
  | { type: "NOTIFY"; title: string; body: string }
  | { type: "OPEN_APP"; appId: string }
  | { type: "BUG"; body: string };

type Props = {
  code: string;
  language: string;
  onOpenCtesting: () => void;
  onOpenCgo: () => void;
  onOpenCmarket: () => void;
  onRunCedalCommand?: (cmd: CedalCommand) => void;
  pendingAppOpen?: string | null;
};

type AppTarget = {
  deeplink: string;
  androidStore: string;
  iosStore: string;
};

async function openExternalApp(target: AppTarget) {
  const storeUrl =
    Platform.OS === "android" ? target.androidStore : target.iosStore;

  try {
    const canOpen = await Linking.canOpenURL(target.deeplink);
    if (canOpen) {
      await Linking.openURL(target.deeplink);
    } else {
      await Linking.openURL(storeUrl);
    }
  } catch (e) {
    await Linking.openURL(storeUrl);
  }
}

const INTRO_LINES = [
  "oh...",
  "hi there, I am Cedal View.",
  "well I guess it's your first time meeting me.",
  "oh well, enough chit chat, I guess you wanna go into the real thing.",
  "here we go, remember we are here to help and also hmm have fun while you're at it.",
];

const C_APPS = [
  // core / special tools (internal screens)
  { id: "ctesting", label: "Ctesting" },
  { id: "cgo", label: "Cgo" },
  { id: "cmarket", label: "Cmarket" },

  // social / media
  { id: "cinstagram", label: "Cinstagram" },
  { id: "ctiktok", label: "Ctiktok" },

  // communication
  { id: "cchat", label: "Cchat" },

  // business & trading
  { id: "cbiz", label: "Cbiz" },
  { id: "ctrade", label: "Ctrade" },

  // education
  { id: "clearn", label: "Clearn" },

  // web / browsing
  { id: "cweb", label: "Cweb" },

  // gaming
  { id: "cplay", label: "Cplay" },

  // technology / tools
  { id: "ctech", label: "Ctech" },
  { id: "cdev", label: "Cdev" },

  // AI assistant – Perplexity
  { id: "cai", label: "Cperplexity" },
];

export default function DeveloperView({
  code,
  language,
  onOpenCtesting,
  onOpenCgo,
  onOpenCmarket,
  onRunCedalCommand,
  pendingAppOpen,
}: Props) {
  const hasCode = code.trim().length > 0;
  const [poweredOn, setPoweredOn] = React.useState(false);
  const [introIndex, setIntroIndex] = React.useState(0);

  const introTimerRef = React.useRef<ReturnType<typeof setTimeout> | null>(
    null
  );

  const [usageMs, setUsageMs] = React.useState(0);

  // inline Ctesting state
  const [showCtesting, setShowCtesting] = React.useState(false);
  const [ctestOutput, setCtestOutput] = React.useState("");

  // dev Cedal command text (can be JSON or simple)
  const [devCommandText, setDevCommandText] = React.useState(
    '{ "type": "VIBRATE", "durationMs": 800 }'
  );

  const showIntro = !hasCode && poweredOn && introIndex < INTRO_LINES.length;

  React.useEffect(() => {
    const start = Date.now();
    const id = setInterval(() => {
      setUsageMs(Date.now() - start);
    }, 1000);
    return () => clearInterval(id);
  }, []);

  // auto-advance intro every 3 seconds
  React.useEffect(() => {
    if (!showIntro) {
      if (introTimerRef.current) {
        clearTimeout(introTimerRef.current);
        introTimerRef.current = null;
      }
      return;
    }

    introTimerRef.current = setTimeout(() => {
      setIntroIndex((prev) => prev + 1);
    }, 3000);

    return () => {
      if (introTimerRef.current) {
        clearTimeout(introTimerRef.current);
      }
    };
  }, [showIntro, introIndex]);

  function handlePhonePress() {
    if (showIntro) {
      setIntroIndex((prev) => prev + 1);
    }
  }

  function triggerInlineCtesting() {
    if (!showCtesting) {
      const timestamp = new Date().toLocaleTimeString();
      const fake = `▶ [${timestamp}] Ctesting ran your ${language} code.
(No real backend yet — this is a simulated console.)`;
      setCtestOutput(fake);
    }
    setShowCtesting((prev) => !prev);
  }

  /** Parse simple text like "vibrate: 2s", "notify: hello", "open: cinstagram", "bug: xenos" */
  function parseSimpleCommand(input: string): CedalCommand | null {
    const trimmed = input.trim();
    if (!trimmed) return null;

    const [rawType, ...rest] = trimmed.split(":");
    if (!rawType) return null;

    const type = rawType.trim().toLowerCase();
    const payload = rest.join(":").trim();

    if (type === "vibrate") {
      let ms = 0;
      if (payload.endsWith("ms")) {
        ms = parseInt(payload.replace("ms", "").trim(), 10);
      } else if (payload.endsWith("s")) {
        ms = parseFloat(payload.replace("s", "").trim()) * 1000;
      } else {
        ms = parseInt(payload, 10);
      }
      if (!Number.isFinite(ms) || ms <= 0) return null;
      return { type: "VIBRATE", durationMs: ms };
    }

    if (type === "notify") {
      if (!payload) return null;
      return {
        type: "NOTIFY",
        title: "Cedal",
        body: payload,
      };
    }

    if (type === "open") {
      if (!payload) return null;
      return {
        type: "OPEN_APP",
        appId: payload,
      };
    }

    if (type === "bug") {
      if (!payload) return null;
      return { type: "BUG", body: payload };
    }

    return null;
  }

  function sendDevCommand() {
    if (!onRunCedalCommand) return;

    const raw = devCommandText.trim();
    if (!raw) return;

    // Try JSON first
    try {
      const parsed = JSON.parse(raw);
      onRunCedalCommand(parsed as CedalCommand);
      return;
    } catch {
      // ignore, fall back to simple
    }

    // Try simple "vibrate: 2s" / "notify: hello" / "open: cinstagram" / "bug: xenos"
    const simple = parseSimpleCommand(raw);
    if (!simple) {
      console.warn("Invalid Cedal command (JSON or simple)");
      return;
    }
    onRunCedalCommand(simple);
  }

  return (
    <View style={styles.root}>
      <Text style={styles.viewLabel}>Preview</Text>

      <View style={styles.phoneOuter}>
        {/* notch */}
        <View style={styles.phoneNotchRow}>
          <View style={styles.phoneSpeaker} />
          <View style={styles.phoneCamera} />
        </View>

        <Pressable style={styles.phoneScreen} onPress={handlePhonePress}>
          {hasCode ? (
            <View style={styles.phoneScreenInner}>
              <Text style={styles.phoneScreenTitle}>App is running</Text>
              <Text style={styles.phoneScreenText}>
                Rendering preview for your current {language} work.
              </Text>

              {/* show current source */}
              <View style={styles.sourceBox}>
                <ScrollView
                  style={{ maxHeight: 120, width: "100%" }}
                  contentContainerStyle={{ paddingBottom: 4 }}
                >
                  <Text style={styles.sourceText}>
                    {code.length ? code : "// No code yet"}
                  </Text>
                </ScrollView>
              </View>

              {/* inline Ctesting toggle */}
              <TouchableOpacity
                style={styles.ctestToggleButton}
                onPress={triggerInlineCtesting}
              >
                <Text style={styles.ctestToggleText}>
                  {showCtesting ? "Hide Ctesting" : "Open Ctesting"}
                </Text>
              </TouchableOpacity>

              {showCtesting && (
                <View style={styles.ctestBox}>
                  <Text style={styles.ctestTitle}>Ctesting console</Text>
                  <ScrollView
                    style={{ maxHeight: 100, width: "100%" }}
                    contentContainerStyle={{ paddingBottom: 4 }}
                  >
                    <Text style={styles.ctestOutputText}>
                      {ctestOutput || "No output yet."}
                    </Text>
                  </ScrollView>
                </View>
              )}

              {/* Cedal remote command dev console */}
              <View style={{ marginTop: 12, width: "100%" }}>
                <Text
                  style={{
                    color: "#9ca3af",
                    fontSize: 11,
                    marginBottom: 4,
                  }}
                >
                  Cedal remote command (dev)
                </Text>
                <TextInput
                  style={{
                    borderWidth: 1,
                    borderColor: "rgba(31,41,55,0.9)",
                    borderRadius: 8,
                    paddingHorizontal: 8,
                    paddingVertical: 6,
                    color: "#e5e7eb",
                    fontSize: 11,
                    fontFamily: "monospace",
                  }}
                  value={devCommandText}
                  onChangeText={setDevCommandText}
                  multiline
                  placeholder={`{ "type": "VIBRATE", "durationMs": 800 } or vibrate: 2s or open: cinstagram or bug: xenos`}
                  placeholderTextColor="#6b7280"
                />
                <TouchableOpacity
                  style={[styles.ctestToggleButton, { marginTop: 6 }]}
                  onPress={sendDevCommand}
                >
                  <Text style={styles.ctestToggleText}>Run command</Text>
                </TouchableOpacity>
              </View>
            </View>
          ) : !poweredOn ? (
            <View style={styles.phoneScreenOff}>
              <TouchableOpacity
                style={styles.powerButton}
                onPress={() => {
                  setPoweredOn(true);
                  setIntroIndex(0);
                }}
              >
                <Text style={styles.powerButtonText}>Power on</Text>
              </TouchableOpacity>
            </View>
          ) : showIntro ? (
            <View style={styles.phoneScreenInner}>
              <Text style={styles.phoneIntroText}>
                {INTRO_LINES[introIndex]}
              </Text>
              <Text style={styles.phoneIntroHint}>
                (tap to skip • auto next in 3s)
              </Text>
            </View>
          ) : (
            <CedalHomeScreen
              usageMs={usageMs}
              onOpenCtesting={triggerInlineCtesting}
              onOpenCgo={onOpenCgo}
              onOpenCmarket={onOpenCmarket}
              pendingAppOpen={pendingAppOpen}
            />
          )}
        </Pressable>

        <View style={styles.phoneHomeBar} />
      </View>
    </View>
  );
}

function CedalHomeScreen({
  usageMs,
  onOpenCtesting,
  onOpenCgo,
  onOpenCmarket,
  pendingAppOpen,
}: {
  usageMs: number;
  onOpenCtesting: () => void;
  onOpenCgo: () => void;
  onOpenCmarket: () => void;
  pendingAppOpen?: string | null;
}) {
  const [search, setSearch] = React.useState("");
  const [pulledDown, setPulledDown] = React.useState(false);
  const [time, setTime] = React.useState(new Date());

  React.useEffect(() => {
    const id = setInterval(() => setTime(new Date()), 1000);
    return () => clearInterval(id);
  }, []);

  const timeLabel = time.toLocaleTimeString([], {
    hour: "2-digit",
    minute: "2-digit",
  });

  const over24h = usageMs >= 24 * 60 * 60 * 1000;

  const blinkOpacity = React.useRef(new Animated.Value(1)).current;

  React.useEffect(() => {
    const loop = Animated.loop(
      Animated.sequence([
        Animated.timing(blinkOpacity, {
          toValue: 0.2,
          duration: 600,
          useNativeDriver: true,
        }),
        Animated.timing(blinkOpacity, {
          toValue: 1,
          duration: 600,
          useNativeDriver: true,
        }),
      ])
    );
    loop.start();
    return () => loop.stop();
  }, [blinkOpacity]);

  const dotStyle = over24h
    ? styles.homeStatusDotRed
    : styles.homeStatusDotGreen;

  function handleAppPress(appId: string) {
    switch (appId) {
      // internal Cedal screens – now toggle inline Ctesting
      case "ctesting":
        onOpenCtesting();
        return;
      case "cgo":
        onOpenCgo();
        return;
      case "cmarket":
        onOpenCmarket();
        return;

      // social / media
      case "cinstagram":
        openExternalApp({
          deeplink: "instagram://app",
          androidStore: "market://details?id=com.instagram.android",
          iosStore: "itms-apps://apps.apple.com/app/id389801252",
        });
        return;

      case "ctiktok":
        openExternalApp({
          deeplink: "snssdk1233://",
          androidStore: "market://details?id=com.zhiliaoapp.musically",
          iosStore: "itms-apps://apps.apple.com/app/id1235601864",
        });
        return;

      // communication
      case "cchat":
        openExternalApp({
          deeplink: "whatsapp://send",
          androidStore: "market://details?id=com.whatsapp",
          iosStore: "itms-apps://apps.apple.com/app/id310633997",
        });
        return;

      // business & trading
      case "cbiz":
        openExternalApp({
          deeplink: "mailto:",
          androidStore: "market://details?id=com.google.android.gm",
          iosStore: "itms-apps://apps.apple.com/app/id422689480",
        });
        return;

      case "ctrade":
        openExternalApp({
          deeplink: "binance://home",
          androidStore: "market://details?id=com.binance.dev",
          iosStore: "itms-apps://apps.apple.com/app/id1436799971",
        });
        return;

      // education
      case "clearn":
        openExternalApp({
          deeplink: "coursera://app",
          androidStore: "market://details?id=org.coursera.android",
          iosStore: "itms-apps://apps.apple.com/app/id736535961",
        });
        return;

      // web / browsing
      case "cweb":
        openExternalApp({
          deeplink: "googlechrome://",
          androidStore: "market://details?id=com.android.chrome",
          iosStore: "itms-apps://apps.apple.com/app/id535886823",
        });
        return;

      // gaming
      case "cplay":
        openExternalApp({
          deeplink: "playgames://",
          androidStore: "market://details?id=com.google.android.play.games",
          iosStore: "itms-apps://apps.apple.com/app/id1114922065",
        });
        return;

      // tech / news
      case "ctech":
        openExternalApp({
          deeplink: "twitter://",
          androidStore: "market://details?id=com.twitter.android",
          iosStore: "itms-apps://apps.apple.com/app/id333903271",
        });
        return;

      // dev tools
      case "cdev":
        openExternalApp({
          deeplink: "github://",
          androidStore: "market://details?id=com.github.android",
          iosStore: "itms-apps://apps.apple.com/app/id1477376905",
        });
        return;

      // AI – Perplexity
      case "cai":
        openExternalApp({
          deeplink: "https://www.perplexity.ai",
          androidStore: "market://details?id=ai.perplexity.app.android",
          iosStore: "itms-apps://apps.apple.com",
        });
        return;

      default:
        console.log("Unknown app:", appId);
    }
  }

  // Auto-open requested app when pendingAppOpen changes
  React.useEffect(() => {
    if (!pendingAppOpen) return;
    handleAppPress(pendingAppOpen);
  }, [pendingAppOpen]);

  const filteredApps = C_APPS.filter((app) =>
    app.label.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <View style={styles.homeRoot}>
      {/* pull-down status area */}
      <View
        style={[
          styles.homeStatusBar,
          pulledDown && styles.homeStatusBarExpanded,
        ]}
      >
        <View style={styles.homeStatusRow}>
          <View style={styles.homeStatusLeft}>
            <Animated.View
              style={[styles.homeStatusDot, dotStyle, { opacity: blinkOpacity }]}
            />
            <Text style={styles.homeStatusTime}>{timeLabel}</Text>
          </View>
          <Text style={styles.homeStatusBrand}>CEDAL OS</Text>
        </View>
        {pulledDown && (
          <Text style={styles.homeStatusHint}>
            More system info and notifications will appear here later.
          </Text>
        )}
        <View
          style={styles.homePullHandle}
          onTouchStart={() => setPulledDown((prev) => !prev)}
        />
      </View>

      {/* search bar */}
      <View style={styles.homeSearchBar}>
        <Text style={styles.homeSearchIcon}>⌕</Text>
        <TextInput
          style={styles.homeSearchInput}
          value={search}
          onChangeText={setSearch}
          placeholder="Search C‑apps..."
          placeholderTextColor="#6b7280"
        />
      </View>

      {/* grid of apps */}
      <ScrollView
        style={styles.homeAppsScroll}
        contentContainerStyle={styles.homeAppsGrid}
        showsVerticalScrollIndicator={false}
      >
        {filteredApps.map((app) => (
          <TouchableOpacity
            key={app.id}
            style={styles.homeAppIcon}
            onPress={() => handleAppPress(app.id)}
          >
            <View style={styles.homeAppBubble}>
              <Text style={styles.homeAppLetter}>
                {app.label.charAt(0)}
              </Text>
            </View>
            <Text style={styles.homeAppLabel}>{app.label}</Text>
          </TouchableOpacity>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    paddingHorizontal: 16,
  },
  viewLabel: {
    color: "#9ca3af",
    fontSize: 12,
    marginBottom: 10,
  },
  phoneOuter: {
    width: 280,
    aspectRatio: 9 / 19.5,
    borderRadius: 32,
    borderWidth: 2,
    borderColor: "rgba(148,163,184,0.8)",
    backgroundColor: "#020617",
    padding: 8,
  },
  phoneNotchRow: {
    height: 18,
    alignItems: "center",
    justifyContent: "center",
  },
  phoneSpeaker: {
    width: 50,
    height: 4,
    borderRadius: 999,
    backgroundColor: "#1f2937",
  },
  phoneCamera: {
    position: "absolute",
    right: 40,
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: "#0f172a",
  },
  phoneScreen: {
    flex: 1,
    borderRadius: 24,
    overflow: "hidden",
    backgroundColor: "black",
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
  },
  phoneScreenInner: {
    flex: 1,
    padding: 16,
    alignItems: "center",
    justifyContent: "flex-start",
    backgroundColor: "rgba(15,23,42,0.98)",
  },
  phoneScreenTitle: {
    color: "#e5e7eb",
    fontSize: 13,
    fontWeight: "600",
    marginBottom: 6,
  },
  phoneScreenText: {
    color: "#9ca3af",
    fontSize: 11,
    textAlign: "center",
  },
  phoneScreenOff: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "black",
  },
  powerButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#22d3ee",
    backgroundColor: "rgba(15,23,42,0.9)",
  },
  powerButtonText: {
    color: "#e5e7eb",
    fontSize: 12,
    fontWeight: "600",
  },
  phoneIntroText: {
    color: "#e5e7eb",
    fontSize: 12,
    textAlign: "center",
    marginBottom: 6,
  },
  phoneIntroHint: {
    color: "#6b7280",
    fontSize: 10,
  },
  phoneHomeBar: {
    height: 12,
    alignItems: "center",
    justifyContent: "center",
  },
  sourceBox: {
    marginTop: 12,
    maxHeight: 120,
    width: "100%",
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    backgroundColor: "rgba(3,7,18,0.98)",
    paddingHorizontal: 6,
    paddingVertical: 4,
  },
  sourceText: {
    color: "#e5e7eb",
    fontSize: 11,
    fontFamily: "monospace",
  },
  ctestToggleButton: {
    marginTop: 10,
    paddingHorizontal: 12,
    paddingVertical: 4,
    borderRadius: 999,
    borderWidth: 1,
    borderColor: "#22d3ee",
    backgroundColor: "rgba(15,23,42,0.95)",
  },
  ctestToggleText: {
    color: "#e5e7eb",
    fontSize: 11,
    fontWeight: "600",
  },
  ctestBox: {
    marginTop: 8,
    width: "100%",
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    backgroundColor: "rgba(0,0,0,0.9)",
    paddingHorizontal: 8,
    paddingVertical: 6,
  },
  ctestTitle: {
    color: "#e5e7eb",
    fontSize: 11,
    fontWeight: "600",
    marginBottom: 4,
  },
  ctestOutputText: {
    color: "#a5b4fc",
    fontSize: 11,
    fontFamily: "monospace",
  },

  // home screen
  homeRoot: {
    flex: 1,
    paddingHorizontal: 10,
    paddingTop: 10,
  },
  homeStatusBar: {
    borderRadius: 12,
    paddingHorizontal: 10,
    paddingTop: 6,
    paddingBottom: 4,
    backgroundColor: "rgba(15,23,42,0.95)",
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    marginBottom: 8,
  },
  homeStatusBarExpanded: {
    paddingBottom: 10,
  },
  homeStatusRow: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
  },
  homeStatusLeft: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
  },
  homeStatusDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
  },
  homeStatusDotGreen: {
    backgroundColor: "#22c55e",
  },
  homeStatusDotRed: {
    backgroundColor: "#ef4444",
  },
  homeStatusTime: {
    color: "#e5e7eb",
    fontSize: 13,
    fontWeight: "600",
  },
  homeStatusBrand: {
    color: "#9ca3af",
    fontSize: 11,
  },
  homeStatusHint: {
    color: "#6b7280",
    fontSize: 10,
    marginTop: 4,
  },
  homePullHandle: {
    marginTop: 6,
    alignSelf: "center",
    width: 36,
    height: 4,
    borderRadius: 999,
    backgroundColor: "#1f2937",
  },
  homeSearchBar: {
    flexDirection: "row",
    alignItems: "center",
    borderRadius: 999,
    paddingHorizontal: 10,
    paddingVertical: 4,
    backgroundColor: "rgba(15,23,42,0.98)",
    borderWidth: 1,
    borderColor: "rgba(31,41,55,0.9)",
    marginBottom: 12,
  },
  homeSearchIcon: {
    color: "#6b7280",
    fontSize: 12,
    marginRight: 6,
  },
  homeSearchInput: {
    flex: 1,
    color: "#e5e7eb",
    fontSize: 11,
  },
  homeAppsScroll: {
    flex: 1,
  },
  homeAppsGrid: {
    flexDirection: "row",
    flexWrap: "wrap",
    justifyContent: "flex-start",
    paddingVertical: 4,
  },
  homeAppIcon: {
    width: "25%",
    paddingVertical: 10,
    alignItems: "center",
  },
  homeAppBubble: {
    width: 44,
    height: 44,
    borderRadius: 16,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "rgba(34,211,238,0.18)",
  },
  homeAppLetter: {
    color: "#e5e7eb",
    fontSize: 20,
    fontWeight: "700",
  },
  homeAppLabel: {
    color: "#e5e7eb",
    fontSize: 10,
    marginTop: 4,
  },
});
