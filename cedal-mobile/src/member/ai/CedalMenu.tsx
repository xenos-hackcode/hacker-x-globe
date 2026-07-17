// src/member/ai/CedalMenu.tsx
import React from "react";
import { View, Text, StyleSheet } from "react-native";

type Props = {
  senderName?: string;
};

export function CedalMenu({ senderName = "User" }: Props) {
  const name = senderName || "User";

  return (
    <View style={styles.container}>
      {/* Header */}
      <Text style={styles.line}>
        <Text style={styles.blue}>╭───── </Text>
        <Text style={styles.red}>ℂ 𝔼 𝔻 𝔸 𝕃</Text>
        <Text style={styles.blue}>{` }─────★`}</Text>
      </Text>

      <Text style={styles.line}>
        <Text style={styles.blue}>┊★╭━━━━━━━━━━━━━━━━━━━━━━━*</Text>
      </Text>

      <Text style={styles.line}>
        <Text style={styles.blue}>┊✺┊ Hello </Text>
        <Text style={styles.red}>{name}</Text>
        <Text style={styles.blue}> ┊👋</Text>
      </Text>

      <Text style={styles.line}>
        <Text style={styles.blue}>┊✺┊ Status  : </Text>
        <Text style={styles.green}>ONLINE ⚡</Text>
      </Text>

      <Text style={styles.line}>
        <Text style={styles.blue}>┊✺┊ Version : v1.0</Text>
      </Text>

      <Text style={styles.line}>
        <Text style={styles.blue}>┊✺┊ Owner   : </Text>
        <Text style={styles.red}>Cedal</Text>
      </Text>

      <Text style={styles.line}>
        <Text style={styles.blue}>┊★╰━━━━━━━━━━━━━━━━━━━━━━━*</Text>
      </Text>

      <Text style={styles.line}>
        <Text style={styles.blue}>╰━━━━━━━━━━━━━━━━━━━━━━━★</Text>
      </Text>

      <Text style={styles.line}>
        <Text style={styles.red}>ℂ 𝔼 𝔻 𝔸 𝕃</Text>
        <Text style={styles.blue}> • </Text>
        <Text style={styles.green}>ℂ 𝔼 𝔻 𝔸 𝕃 𝕮𝕺ℕ𝕊𝕺𝕃𝔼</Text>
      </Text>

      <Text style={[styles.line, styles.spacedTop]}>
        <Text style={styles.blue}>
          ╭═══════════[ ✦ 𝙊𝙒𝙉𝙀𝙍 𝙈𝙀𝙉𝙐 ✦ ]═══════════*
        </Text>
      </Text>

      {[
        "⚡ .broadcast £",
        "🔒 .block",
        "🔓 .unblock",
        "☣️ .eval",
        "🧬 .enc",
        "⏱️ .runtime",
        "📡 .ping",
        "💠 .alive",
        "🛰️ .setppbot £",
        "🌐 .tagall",
        "🛑 .enough",
        "⚖️ .enforce",
        "🟢 .online",
        "🔴 .offline",
        "💚 like status",
        "💙 view status",
        "📝 .change name <newname>",
        "❌ .remove <number>",
        "🛡️ admin only / all can talk",
        "🎙️ .voice <prompt>",
      ].map((cmd) => (
        <Text key={cmd} style={styles.line}>
          <Text style={styles.blue}>┊  {cmd}</Text>
        </Text>
      ))}

      <Text style={styles.line}>
        <Text style={styles.blue}>┊</Text>
      </Text>
      <Text style={styles.line}>
        <Text style={styles.blue}>
          ╰═════════════════════════════════════*
        </Text>
      </Text>
      <Text style={styles.line}>
        <Text style={styles.blue}>      𝓒𝓡𝓔𝓐𝓣𝓔𝓓  𝓑𝓨 '</Text>
        <Text style={styles.red}>ℂ𝖊𝖉𝖆𝖑</Text>
        <Text style={styles.blue}>'</Text>
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    padding: 10,
    borderRadius: 14,
    backgroundColor: "#020617",
    borderWidth: 1,
    borderColor: "#22d3ee",
    shadowColor: "#22d3ee",
    shadowOpacity: 0.45,
    shadowRadius: 12,
    shadowOffset: { width: 0, height: 0 },
  },
  line: {
    fontFamily: "monospace",
    fontSize: 11,
    color: "#22d3ee",
    lineHeight: 14,
  },
  spacedTop: {
    marginTop: 4,
  },
  blue: { color: "#22d3ee" },
  red: { color: "#fb7185" },
  green: { color: "#4ade80" },
});
