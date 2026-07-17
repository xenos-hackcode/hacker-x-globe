// app/(auth)/(member)/rules.tsx
import { useTheme } from "@/src/themes/ThemeContext";
import { useRouter } from "expo-router";
import React from "react";
import {
  ScrollView,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from "react-native";

export default function RulesScreen() {
  const router = useRouter();
  const { colors } = useTheme();

  function handleBack() {
    router.back();
  }

  return (
    <View style={[styles.root, { backgroundColor: colors.background }]}>
      {/* Top bar */}
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
          Rules
        </Text>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        {/* Philosophy card */}
        <View
          style={[
            styles.card,
            { borderColor: colors.border, backgroundColor: colors.background },
          ]}
        >
          <Text style={[styles.cardTitle, { color: colors.textPrimary }]}>
            Freedom first. Safety always.
          </Text>
          <Text style={[styles.cardBody, { color: colors.textSecondary }]}>
            Cedal is built for heavy freedom: experiments, wild ideas, late‑night
            chats. But if your freedom harms under‑age users, the law kicks in
            fast, not vibes.
          </Text>
        </View>

        {/* Core rules */}
        <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>
          Core rules
        </Text>
        <View
          style={[
            styles.card,
            { borderColor: colors.border, backgroundColor: colors.background },
          ]}
        >
          <RuleRow
            title="No explicit content with minors"
            body="Do not send sexual, nude, or otherwise explicit content to anyone under 18. Do not ask for it, trade it, or store it. That includes images, videos, and text describing sexual acts with minors."
          />
          <RuleRow
            title="No grooming or targeting under‑age users"
            body="Do not try to befriend, manipulate, or pressure under‑age users into sexual chats, sharing nudes, or meeting up. That behaviour is grooming and can be criminal."
          />
          <RuleRow
            title="No sharing of illegal material"
            body="Do not share content that is illegal to possess or distribute where you live (for example: child sexual abuse material, non‑consensual intimate images, or extreme violence)."
          />
          <RuleRow
            title="Respect boundaries and consent"
            body="If someone says no, stops replying, or seems uncomfortable, back off. Harassment, stalking, and doxxing are not tolerated."
          />
          <RuleRow
            title="No serious threats or targeted hate"
            body="Do not issue credible threats of harm or encourage real‑world violence against any person or group."
            last
          />
        </View>

        {/* Under‑age safety */}
        <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>
          If you’re under 18
        </Text>
        <View
          style={[
            styles.card,
            { borderColor: colors.border, backgroundColor: colors.background },
          ]}
        >
          <RuleRow
            title="You never have to send anything"
            body="If anyone asks you for nudes, explicit pics, or sexual favours, you can ignore, block, or leave. You do not owe anyone content, ever."
          />
          <RuleRow
            title="Talk to someone you trust"
            body="If a chat makes you feel weird, pressured, or unsafe, close it and talk to a trusted adult or local helpline. Saving evidence (screenshots, links) can help if you report it."
            last
          />
        </View>

        {/* Consequences */}
        <Text style={[styles.sectionLabel, { color: colors.textSecondary }]}>
          What can happen
        </Text>
        <View
          style={[
            styles.card,
            { borderColor: colors.border, backgroundColor: colors.background },
          ]}
        >
          <RuleRow
            title="Legal consequences"
            body="In many places, creating, possessing, or sharing sexual images of anyone under 18 is a criminal offence, even if they agreed or sent the image themselves. Courts can impose fines, restrictions, and even prison time."
          />
          <RuleRow
            title="Account consequences"
            body="We reserve the right to lock or remove accounts involved in harming or exploiting minors, or in clearly illegal activity, to protect other users."
            last
          />
        </View>
      </ScrollView>
    </View>
  );
}

function RuleRow({
  title,
  body,
  last,
}: {
  title: string;
  body: string;
  last?: boolean;
}) {
  return (
    <View style={[styles.ruleRow, last && styles.ruleRowLast]}>
      <Text style={styles.ruleTitle}>{title}</Text>
      <Text style={styles.ruleBody}>{body}</Text>
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
  },
  content: {
    paddingHorizontal: 16,
    paddingVertical: 16,
    paddingBottom: 32,
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
    paddingHorizontal: 14,
    paddingVertical: 12,
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: "600",
    marginBottom: 4,
  },
  cardBody: {
    fontSize: 12,
  },
  ruleRow: {
    paddingVertical: 8,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: "rgba(31,41,55,0.9)",
  },
  ruleRowLast: {
    borderBottomWidth: 0,
  },
  ruleTitle: {
    fontSize: 13,
    fontWeight: "500",
    color: "#e5e7eb",
    marginBottom: 2,
  },
  ruleBody: {
    fontSize: 11,
    color: "#9ca3af",
  },
});
