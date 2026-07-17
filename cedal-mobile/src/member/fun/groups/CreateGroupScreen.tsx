// src/member/fun/groups/CreateGroupScreen.tsx
import { generateGroupInviteCode } from "@/src/member/utils/groupInvite";
import { useTheme } from "@/src/themes/ThemeContext";
import React, { useEffect, useMemo, useState } from "react";
import {
  Alert,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
} from "react-native";

type Server = {
  id: string;
  label: string;
  basePing: number;
  url: string;
  region?: string;
};

type CreateGroupInput = {
  name: string;
  description: string;
  tags: string[];
  isPrivate: boolean;
  serverId: string;
  inviteLinkToken?: string; // token used in cedal.app/join/{token}
  inviteCode?: string;      // instant-join code (private only)
};

type Props = {
  currentUser: { uid: string; nextGroupCreateAt?: Date | null } | null;
  onCreate: (input: CreateGroupInput) => Promise<void> | void;
  onCancel: () => void;
};

// REAL servers: HTTPS Cloud Functions you deployed
const SERVERS: Server[] = [
  {
    id: "lon-01",
    label: "London · LON‑01",
    basePing: 20,
    url: "https://<project-id>.cloudfunctions.net/pingLon01",
    region: "EU",
  },
  {
    id: "fra-01",
    label: "Frankfurt · FRA‑01",
    basePing: 25,
    url: "https://<project-id>.cloudfunctions.net/pingFra01",
    region: "EU",
  },
  {
    id: "nyc-01",
    label: "New York · NYC‑01",
    basePing: 80,
    url: "https://<project-id>.cloudfunctions.net/pingNyc01",
    region: "NA",
  },
];

export default function CreateGroupScreen({
  currentUser,
  onCreate,
  onCancel,
}: Props) {
  const { colors } = useTheme();
  const styles = useMemo(() => makeStyles(colors), [colors]);

  const [name, setName] = useState("");
  const [purpose, setPurpose] = useState("");
  const [tags, setTags] = useState("");
  const [visibility, setVisibility] = useState<"public" | "private">("public");
  const [loading, setLoading] = useState(false);

  const [selectedServer, setSelectedServer] = useState<string | null>(
    SERVERS[0]?.id ?? null,
  );
  const [pings, setPings] = useState<Record<string, number>>({});
  const [showServerList, setShowServerList] = useState(false);

  // NEW: split link token vs instant code
  const [inviteLinkToken, setInviteLinkToken] = useState<string | null>(null);
  const [instantCode, setInstantCode] = useState<string | null>(null);

  // cooldown tick (for owner leave cooldown)
  const [nowTick, setNowTick] = useState(Date.now());
  useEffect(() => {
    const id = setInterval(() => setNowTick(Date.now()), 1000);
    return () => clearInterval(id);
  }, []);

  const cooldownTs = currentUser?.nextGroupCreateAt
    ? new Date(currentUser.nextGroupCreateAt).getTime()
    : 0;
  const remainingMs = Math.max(0, cooldownTs - nowTick);
  const onCooldown = remainingMs > 0;
  let countdownLabel = "";
  if (onCooldown) {
    const h = Math.floor(remainingMs / (1000 * 60 * 60));
    const m = Math.floor((remainingMs % (1000 * 60 * 60)) / (1000 * 60));
    const s = Math.floor((remainingMs % (1000 * 60)) / 1000);
    countdownLabel = `Create cooldown: ${h}h ${m}m ${s}s`;
  }

  // when switching visibility, generate link/code appropriately
  function handleSetVisibility(next: "public" | "private") {
    setVisibility(next);

    if (!inviteLinkToken) {
      setInviteLinkToken(generateGroupInviteCode());
    }

    if (next === "public") {
      // public: invite link only
      setInstantCode(null);
    } else {
      // private: invite link + instant code
      if (!instantCode) {
        setInstantCode(generateGroupInviteCode());
      }
    }
  }

  // Measure real ping periodically via fetch to your Cloud Functions
  useEffect(() => {
    if (SERVERS.length === 0) return;

    let cancelled = false;

    async function pingOnce(server: Server) {
      const start = Date.now();
      try {
        await fetch(server.url, { method: "GET" });
        const elapsed = Date.now() - start;
        if (!cancelled) {
          setPings((prev) => ({
            ...prev,
            [server.id]: elapsed,
          }));
        }
      } catch {
        if (!cancelled) {
          setPings((prev) => ({
            ...prev,
            [server.id]: (prev[server.id] ?? server.basePing) + 50,
          }));
        }
      }
    }

    SERVERS.forEach((s) => pingOnce(s));

    const interval = setInterval(() => {
      SERVERS.forEach((s) => pingOnce(s));
    }, 5000);

    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, []);

  function pingStatus(ping: number) {
    if (ping <= 60) return { label: "Stable", color: "#22c55e" };
    if (ping <= 130) return { label: "Fair", color: "#eab308" };
    return { label: "Bad", color: "#ef4444" };
  }

  async function handleSubmit() {
    if (!currentUser?.uid) {
      Alert.alert("Not signed in", "You must be signed in to create a group.");
      return;
    }

    if (onCooldown) {
      const h = Math.floor(remainingMs / (1000 * 60 * 60));
      const m = Math.floor((remainingMs % (1000 * 60 * 60)) / (1000 * 60));
      Alert.alert(
        "Create on cooldown",
        `You left a group as owner recently. You can create a new group in ${h}h ${m}m.`,
      );
      return;
    }

    if (!name.trim() || loading) return;
    if (!selectedServer) {
      Alert.alert("Relay", "Select a relay server before creating a group.");
      return;
    }
    if (!inviteLinkToken) {
      Alert.alert(
        "Invite link",
        "Failed to generate an invite link. Try toggling visibility.",
      );
      return;
    }
    if (visibility === "private" && !instantCode) {
      Alert.alert(
        "Invite code",
        "Failed to generate an invite code. Try toggling visibility.",
      );
      return;
    }

    try {
      setLoading(true);
      await onCreate({
        name: name.trim(),
        description: purpose.trim(),
        tags: tags
          .split(",")
          .map((t) => t.trim())
          .filter(Boolean),
        isPrivate: visibility === "private",
        serverId: selectedServer,
        inviteLinkToken: inviteLinkToken ?? undefined,
        inviteCode: visibility === "private" ? instantCode ?? undefined : undefined,
      });
    } catch (e: any) {
      console.log("Create group failed", e?.code, e?.message);
      Alert.alert("Error", e?.message ?? "Could not create group.");
    } finally {
      setLoading(false);
    }
  }

  const uidShort = currentUser?.uid?.slice(0, 6) || "offline";

  return (
    <View style={styles.root}>
      <ScrollView
        contentContainerStyle={styles.scrollContent}
        keyboardShouldPersistTaps="handled"
      >
        {/* HEADER */}
        <View style={styles.headerRow}>
          <View>
            <Text style={styles.headerMeta}>Group setup</Text>
            <Text style={styles.headerTitle}>Create new group</Text>
            <Text style={styles.headerSub}>
              Spin up a focused space for squads, friends, or events.
            </Text>
          </View>
          <View style={styles.headerRight}>
            <Text style={styles.badge}>Session config</Text>
            <Text style={styles.uidLine}>UID: {uidShort}</Text>
          </View>
        </View>

        {/* GROUP NAME */}
        <View style={styles.fieldBlock}>
          <Text style={styles.label}>Group name</Text>
          <View style={styles.inputShell}>
            <TextInput
              value={name}
              onChangeText={setName}
              maxLength={40}
              placeholder="Clan hub, Strategy room..."
              placeholderTextColor="#6b7280"
              style={styles.input}
            />
          </View>
          <View style={styles.counterRow}>
            <Text style={styles.counterText}>{name.length}/40</Text>
          </View>
        </View>

        {/* PURPOSE */}
        <View style={styles.fieldBlock}>
          <Text style={styles.label}>Purpose</Text>
          <View style={styles.inputShell}>
            <TextInput
              value={purpose}
              onChangeText={setPurpose}
              maxLength={220}
              placeholder="What is this group coordinating or focusing on?"
              placeholderTextColor="#6b7280"
              style={[styles.input, styles.textarea]}
              multiline
            />
          </View>
          <View style={styles.counterRow}>
            <Text style={styles.counterText}>{purpose.length}/220</Text>
          </View>
        </View>

        {/* TAGS */}
        <View style={styles.fieldBlock}>
          <Text style={styles.label}>Tags</Text>
          <View style={styles.tagsShell}>
            <View style={styles.tagsInner}>
              <TextInput
                value={tags}
                onChangeText={setTags}
                placeholder="scrim, VOD review, comp, chill..."
                placeholderTextColor="#6b7280"
                style={styles.tagsInput}
              />
            </View>
          </View>
          <Text style={styles.tagsHelp}>
            Comma separated. Used for discovery inside the browser.
          </Text>
        </View>

        {/* SERVER PICKER */}
        <View style={styles.fieldBlock}>
          <Text style={styles.label}>Relay server</Text>

          <View style={styles.serverPanel}>
            <TouchableOpacity
              activeOpacity={0.8}
              style={styles.serverButton}
              onPress={() => setShowServerList((prev) => !prev)}
              disabled={SERVERS.length === 0}
            >
              <Text style={styles.serverButtonText}>
                {SERVERS.length === 0
                  ? "No servers configured"
                  : SERVERS.find((s) => s.id === selectedServer)?.label ??
                    "Select relay"}
              </Text>
              <Text
                style={[
                  styles.serverChevron,
                  showServerList && { transform: [{ rotate: "180deg" }] },
                ]}
              >
                ▼
              </Text>
            </TouchableOpacity>

            {showServerList && SERVERS.length > 0 && (
              <View style={styles.serverListOuter}>
                <ScrollView
                  nestedScrollEnabled
                  showsVerticalScrollIndicator={false}
                >
                  {SERVERS.map((s) => {
                    const ping = pings[s.id] ?? s.basePing;
                    const status = pingStatus(ping);
                    const barWidth = Math.min(
                      100,
                      Math.max(10, (ping / 220) * 100),
                    );
                    const selected = selectedServer === s.id;

                    return (
                      <TouchableOpacity
                        key={s.id}
                        activeOpacity={0.85}
                        onPress={() => {
                          setSelectedServer(s.id);
                          setShowServerList(false);
                        }}
                        style={[
                          styles.serverRow,
                          selected && styles.serverRowSelected,
                        ]}
                      >
                        <View style={styles.serverRowTop}>
                          <Text style={styles.serverLabel}>{s.label}</Text>
                          <Text
                            style={[
                              styles.serverPing,
                              { color: status.color },
                            ]}
                          >
                            {ping} ms
                          </Text>
                        </View>

                        <View style={styles.serverBarShell}>
                          <View
                            style={[
                              styles.serverBar,
                              { width: `${barWidth}%` },
                            ]}
                          />
                        </View>

                        <View style={styles.serverStatusRow}>
                          <Text style={styles.serverStatusLabel}>
                            Stability:
                          </Text>
                          <Text
                            style={[
                              styles.serverStatusValue,
                              { color: status.color },
                            ]}
                          >
                            {status.label}
                          </Text>
                        </View>
                      </TouchableOpacity>
                    );
                  })}
                </ScrollView>
              </View>
            )}

            <Text style={styles.serverHelp}>
              Groups inherit this relay for voice, video, and activity metrics.
            </Text>
          </View>
        </View>

        {/* VISIBILITY + invite block */}
        <View style={styles.fieldBlock}>
          <Text style={styles.label}>Visibility</Text>
          <View style={styles.visibilityShell}>
            <View style={styles.visibilityInner}>
              <TouchableOpacity
                activeOpacity={0.8}
                style={styles.visibilityRow}
                onPress={() => handleSetVisibility("public")}
              >
                <View style={styles.radioOuter}>
                  {visibility === "public" && <View style={styles.radioDot} />}
                </View>
                <Text style={styles.visibilityLabel}>Public</Text>
                <Text style={styles.visibilitySub}>
                  · Anyone in the guild can join
                </Text>
              </TouchableOpacity>

              <TouchableOpacity
                activeOpacity={0.8}
                style={styles.visibilityRow}
                onPress={() => handleSetVisibility("private")}
              >
                <View style={styles.radioOuter}>
                  {visibility === "private" && <View style={styles.radioDot} />}
                </View>
                <Text style={styles.visibilityLabel}>Private</Text>
                <Text style={styles.visibilitySub}>
                  · Invite link + code
                </Text>
              </TouchableOpacity>

              {/* PUBLIC: link only */}
              {visibility === "public" && inviteLinkToken && (
                <View style={styles.inviteBlock}>
                  <Text style={styles.inviteLabel}>Invite link</Text>
                  <Text style={styles.inviteValue} numberOfLines={1}>
                    cedal.app/join/{inviteLinkToken}
                  </Text>
                  <TouchableOpacity
                    style={styles.inviteRefreshBtn}
                    activeOpacity={0.8}
                    onPress={() =>
                      setInviteLinkToken(generateGroupInviteCode())
                    }
                  >
                    <Text style={styles.inviteRefreshText}>Regenerate</Text>
                  </TouchableOpacity>
                </View>
              )}

              {/* PRIVATE: link + instant code */}
              {visibility === "private" && (
                <View style={styles.inviteBlock}>
                  <Text style={styles.inviteLabel}>Invite link</Text>
                  {inviteLinkToken ? (
                    <Text style={styles.inviteValue} numberOfLines={1}>
                      cedal.app/join/{inviteLinkToken}
                    </Text>
                  ) : (
                    <Text style={styles.inviteValue}>
                      Failed to generate link
                    </Text>
                  )}
                  <TouchableOpacity
                    style={styles.inviteRefreshBtn}
                    activeOpacity={0.8}
                    onPress={() =>
                      setInviteLinkToken(generateGroupInviteCode())
                    }
                  >
                    <Text style={styles.inviteRefreshText}>
                      Regenerate link
                    </Text>
                  </TouchableOpacity>

                  <View style={{ height: 8 }} />

                  <Text style={styles.inviteLabel}>Instant join code</Text>
                  {instantCode ? (
                    <Text style={styles.inviteValue}>{instantCode}</Text>
                  ) : (
                    <Text style={styles.inviteValue}>
                      Failed to generate code
                    </Text>
                  )}
                  <TouchableOpacity
                    style={styles.inviteRefreshBtn}
                    activeOpacity={0.8}
                    onPress={() => setInstantCode(generateGroupInviteCode())}
                  >
                    <Text style={styles.inviteRefreshText}>
                      Regenerate code
                    </Text>
                  </TouchableOpacity>
                </View>
              )}
            </View>
          </View>
        </View>

        {/* FOOTER */}
        <View style={styles.footerRow}>
          <Text style={styles.footerMeta}>
            Relay: {selectedServer ?? "N/A"} ·{" "}
            {visibility === "public"
              ? "Visible inside guild"
              : "Hidden · Invite only"}
          </Text>

          {onCooldown && (
            <Text style={[styles.footerMeta, { color: "#f97373" }]}>
              {countdownLabel}
            </Text>
          )}

          <View style={styles.footerButtons}>
            <TouchableOpacity
              activeOpacity={0.8}
              onPress={onCancel}
              style={[styles.btnCancel, loading && styles.btnDisabled]}
              disabled={loading}
            >
              <Text style={styles.btnCancelText}>Cancel</Text>
            </TouchableOpacity>

            <TouchableOpacity
              activeOpacity={0.9}
              onPress={handleSubmit}
              style={[
                styles.btnCreate,
                (loading || onCooldown) && styles.btnDisabled,
              ]}
              disabled={loading || onCooldown}
            >
              <Text style={styles.btnCreateText}>
                {loading ? "Creating..." : "Create group"}
              </Text>
            </TouchableOpacity>
          </View>
        </View>
      </ScrollView>
    </View>
  );
}

const makeStyles = (colors: any) =>
  StyleSheet.create({
    root: {
      flex: 1,
      borderRadius: 22,
      borderWidth: 1,
      borderColor: "rgba(15,23,42,0.9)",
      backgroundColor: "#020617",
      overflow: "hidden",
    },
    scrollContent: {
      paddingHorizontal: 16,
      paddingVertical: 18,
      paddingBottom: 28,
      gap: 16,
    },
    headerRow: {
      flexDirection: "row",
      justifyContent: "space-between",
      alignItems: "flex-start",
      marginBottom: 4,
    },
    headerMeta: {
      fontSize: 10,
      letterSpacing: 2,
      textTransform: "uppercase",
      color: "#64748b",
      marginBottom: 4,
    },
    headerTitle: {
      fontWeight: "700",
      fontSize: 20,
      color: "#e5e7eb",
    },
    headerSub: {
      fontSize: 12,
      color: "#9ca3af",
      marginTop: 4,
    },
    headerRight: {
      alignItems: "flex-end",
    },
    badge: {
      fontSize: 10,
      textTransform: "uppercase",
      letterSpacing: 1,
      paddingHorizontal: 9,
      paddingVertical: 2,
      borderRadius: 999,
      borderWidth: 1,
      borderColor: "rgba(31,41,55,1)",
      backgroundColor: "rgba(15,23,42,1)",
      color: "#a5b4fc",
    },
    uidLine: {
      marginTop: 6,
      fontSize: 9,
      color: "#6b7280",
    },
    fieldBlock: {
      marginTop: 4,
    },
    label: {
      fontWeight: "600",
      fontSize: 11,
      textTransform: "uppercase",
      letterSpacing: 1.3,
      color: "#94a3b8",
      marginBottom: 6,
    },
    inputShell: {
      borderRadius: 14,
      borderWidth: 1,
      borderColor: "rgba(30,64,175,0.65)",
      backgroundColor: "rgba(15,23,42,0.98)",
    },
    input: {
      borderRadius: 13,
      color: "#e5e7eb",
      paddingHorizontal: 12,
      paddingVertical: 9,
      fontSize: 14,
    },
    textarea: {
      minHeight: 80,
      textAlignVertical: "top",
    },
    counterRow: {
      marginTop: 4,
      alignItems: "flex-end",
    },
    counterText: {
      fontSize: 10,
      color: "#6b7280",
    },
    tagsShell: {
      borderRadius: 14,
      borderWidth: 1,
      borderColor: "rgba(30,64,175,0.65)",
      backgroundColor: "rgba(15,23,42,0.98)",
    },
    tagsInner: {
      borderRadius: 13,
      paddingHorizontal: 11,
      paddingVertical: 7,
      backgroundColor: "rgba(15,23,42,1)",
    },
    tagsInput: {
      width: "100%",
      color: "#f9fafb",
      fontSize: 14,
    },
    tagsHelp: {
      marginTop: 4,
      fontSize: 11,
      color: "#9ca3af",
    },
    serverPanel: {
      borderRadius: 14,
      borderWidth: 1,
      borderColor: "rgba(30,64,175,0.8)",
      backgroundColor: "rgba(15,23,42,1)",
      padding: 8,
    },
    serverButton: {
      width: "100%",
      borderRadius: 10,
      paddingVertical: 6,
      paddingHorizontal: 8,
      borderWidth: 1,
      borderColor: "rgba(30,64,175,0.6)",
      backgroundColor: "rgba(15,23,42,1)",
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
    },
    serverButtonText: {
      fontSize: 12,
      color: "#e5e7eb",
    },
    serverChevron: {
      fontSize: 11,
      color: "#9ca3af",
    },
    serverListOuter: {
      marginTop: 6,
      borderRadius: 12,
      overflow: "hidden",
      backgroundColor: "rgba(15,23,42,1)",
      borderWidth: 1,
      borderColor: "rgba(31,41,55,1)",
      maxHeight: 180,
    },
    serverRow: {
      borderRadius: 10,
      borderWidth: 1,
      borderColor: "rgba(31,41,55,0.95)",
      padding: 8,
      marginBottom: 6,
      marginHorizontal: 6,
      backgroundColor: "#020617",
    },
    serverRowSelected: {
      borderColor: "#38bdf8",
      backgroundColor: "rgba(15,23,42,0.9)",
    },
    serverRowTop: {
      flexDirection: "row",
      justifyContent: "space-between",
      alignItems: "center",
    },
    serverLabel: {
      fontSize: 12,
      color: "#e5e7eb",
    },
    serverPing: {
      fontSize: 11,
      fontWeight: "700",
    },
    serverBarShell: {
      marginTop: 4,
      height: 6,
      borderRadius: 999,
      backgroundColor: "rgba(15,23,42,1)",
      overflow: "hidden",
    },
    serverBar: {
      height: "100%",
      borderRadius: 999,
      backgroundColor: "#22c55e",
    },
    serverStatusRow: {
      marginTop: 2,
      flexDirection: "row",
      justifyContent: "space-between",
    },
    serverStatusLabel: {
      fontSize: 10,
      color: "#9ca3af",
    },
    serverStatusValue: {
      fontSize: 10,
      fontWeight: "600",
    },
    serverHelp: {
      marginTop: 6,
      fontSize: 11,
      color: "#6b7280",
    },
    visibilityShell: {
      borderRadius: 14,
      borderWidth: 1,
      borderColor: "rgba(30,64,175,0.7)",
      backgroundColor: "rgba(15,23,42,0.98)",
    },
    visibilityInner: {
      borderRadius: 13,
      paddingHorizontal: 11,
      paddingVertical: 8,
      backgroundColor: "rgba(15,23,42,1)",
    },
    visibilityRow: {
      flexDirection: "row",
      alignItems: "center",
      marginBottom: 4,
    },
    radioOuter: {
      width: 16,
      height: 16,
      borderRadius: 8,
      borderWidth: 1,
      borderColor: "#38bdf8",
      alignItems: "center",
      justifyContent: "center",
      marginRight: 6,
    },
    radioDot: {
      width: 8,
      height: 8,
      borderRadius: 4,
      backgroundColor: "#38bdf8",
    },
    visibilityLabel: {
      fontSize: 13,
      color: "#f9fafb",
    },
    visibilitySub: {
      fontSize: 11,
      color: "#9ca3af",
      marginLeft: 4,
    },
    inviteBlock: {
      marginTop: 8,
      padding: 8,
      borderRadius: 10,
      borderWidth: 1,
      borderColor: "rgba(55,65,81,0.9)",
      backgroundColor: "#020617",
    },
    inviteLabel: {
      fontSize: 10,
      color: "#9ca3af",
      textTransform: "uppercase",
      letterSpacing: 1,
      marginBottom: 2,
    },
    inviteValue: {
      fontSize: 12,
      color: "#e5e7eb",
    },
    inviteRefreshBtn: {
      alignSelf: "flex-start",
      marginTop: 4,
      paddingHorizontal: 10,
      paddingVertical: 4,
      borderRadius: 999,
      borderWidth: 1,
      borderColor: "rgba(148,163,184,0.7)",
    },
    inviteRefreshText: {
      fontSize: 11,
      color: "#e5e7eb",
      letterSpacing: 0.5,
      textTransform: "uppercase",
    },
    footerRow: {
      marginTop: 10,
      flexDirection: "row",
      alignItems: "center",
      justifyContent: "space-between",
    },
    footerMeta: {
      fontSize: 10,
      color: "#6b7280",
      flex: 1,
      marginRight: 12,
    },
    footerButtons: {
      flexDirection: "row",
      alignItems: "center",
      gap: 8,
    },
    btnCancel: {
      paddingHorizontal: 16,
      paddingVertical: 8,
      borderRadius: 999,
      borderWidth: 1,
      borderColor: "rgba(31,41,55,1)",
      backgroundColor: "#020617",
    },
    btnCancelText: {
      fontSize: 12,
      color: "#e5e7eb",
      fontWeight: "500",
    },
    btnCreate: {
      paddingHorizontal: 20,
      paddingVertical: 9,
      borderRadius: 999,
      backgroundColor: "#38bdf8",
    },
    btnCreateText: {
      fontSize: 12,
      color: "#0f172a",
      fontWeight: "700",
      textTransform: "uppercase",
      letterSpacing: 1,
    },
    btnDisabled: {
      opacity: 0.7,
    },
  });
