// src/member/chat/EmojiPanel.tsx
import React, { useMemo, useState } from "react";
import {
  Modal,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  TextInput,
} from "react-native";

type Props = {
  visible: boolean;
  onClose: () => void;
  onSelectEmoji: (emoji: string) => void;
};

// EMOJIS: includes your long list + some tech ones for vibe
const EMOJIS = [
  // Tech / system-ish
  "🤖","👾","🧠","💻","⌨️","🖱️","🛰️","📡","📱","⚙️","🧬","🧪","⚡","🚀",

  // Faces – big set you pasted (deduped)
  "😀","😃","😄","😁","😆","😅","😂","🤣","🥲","🥹","☺️","😊","😇","🙂","🙃","😉",
  "😌","😍","🥰","😘","😗","😙","😚","😋","😛","😝","😜","🤪","🤨","🧐","🤓","😎",
  "🥸","🤩","🥳","🙂‍↕️","😏","😒","🙂‍↔️","😞","😔","😟","😕","🙁","☹️","😣","😖",
  "😫","😩","🥺","😢","😭","😮‍💨","😤","😠","😡","🤬","🤯","😳","🥵","🥶","😱","😨",
  "😰","😥","😓","🫣","🫡","🤔","🫢","🤭","🤫","🤥","😶","😶‍🌫️","😐","😑","😬","😪",
  "😴","🫩","😷","🤒","🤕","🤢","🤮","🤧","🥴","😵","😵‍💫","🤑","🤗","🤤","🤠","🫨",
  "🫠","🙄","😯","😦","😧","😮","😲","🫥","🙈","🙉","🙊",

  // Monsters / spooky / cats
  "😈","👿","👹","👺","🤡","💩","👻","💀","☠️","👽","👾","🤖","🎃",
  "😺","😸","😹","😻","😼","😽","🙀","😿","😾",

  // Hands / gestures
  "👋","🤚","🖐","✋","🖖","👌","🤌","🤏","✌️","🤞","🫰","🤟","🤘","🤙","🫵",
  "🫱","🫲","🫸","🫷","🫳","🫴","👈","👉","👆","🖕","👇","☝️","👍","👎","✊","👊",
  "🤛","🤜","👏","🫶","🙌","👐","🤲","🤝","🙏","✍️","💅","🤳",

  // Body / organs
  "💪","🦾","🦵","🦿","🦶","👣","🫆","👂","🦻","👃","🫀","🫁","🧠","🦷","🦴",
  "👀","👁","👅","👄","🫦","💋","🩸",

  // Hearts and hype
  "❤️","🩷","🧡","💛","💚","💙","💜","🖤","🤍","🤎",
  "💔","❣️","💕","💞","💓","💗","💖","💘","💝","💟","💯","💌",
  "🔥","✨","⭐","🌟","💥","⚡","🌈","🎉","🎊","🪄",
];

// EMOJI_TAGS: only for search; we don’t need one entry per emoji.
// Most similar variants will share meanings implicitly via search on base ones.
const EMOJI_TAGS: Record<string, string[]> = {
  // happy / positive (covers many smileys)
  "😀": ["happy","smile","positive"],
  "😃": ["happy","smile"],
  "😄": ["happy","smile"],
  "😁": ["happy","grin","smile"],
  "😆": ["happy","laugh"],
  "😅": ["happy","nervous"],
  "😂": ["happy","laugh","funny"],
  "🤣": ["happy","laugh","funny"],
  "😊": ["happy","smile","soft"],
  "😇": ["happy","angel"],
  "😉": ["happy","wink"],
  "😍": ["love","heart","happy"],
  "🥰": ["love","hearts","happy"],
  "😘": ["love","kiss"],
  "😎": ["cool","happy"],
  "🤩": ["star","eyes","excited"],
  "🥳": ["party","celebrate"],

  // neutral / thinking / meh
  "🙂": ["neutral","ok","soft"],
  "🙃": ["neutral","upside","weird"],
  "😐": ["neutral","meh"],
  "😑": ["neutral","blank"],
  "😶": ["mute","silent"],
  "🙄": ["annoyed","eyeroll"],
  "😏": ["smirk","sassy"],
  "🤔": ["think","question","hmm"],
  "🤨": ["suspicious","question"],
  "🫤": ["unsure","meh"],
  "🫥": ["faded","awkward"],
  "😴": ["sleep","tired"],
  "😪": ["sad","tired","sleepy"],
  "🫩": ["tired","burnout"],

  // sad / worried / scared
  "☹️": ["sad","unhappy"],
  "🙁": ["sad","unhappy"],
  "😟": ["sad","worried"],
  "😕": ["confused"],
  "😣": ["sad","struggle"],
  "😖": ["sad","frustrated"],
  "😫": ["sad","tired","exhausted"],
  "😩": ["tired","sad"],
  "😞": ["sad","disappointed"],
  "😔": ["sad","down"],
  "😢": ["sad","cry"],
  "😭": ["sad","cry"],
  "😥": ["sad","relief"],
  "😓": ["sad","sweat"],
  "🥺": ["sad","beg","puppy"],
  "🥹": ["sad","touching"],
  "😱": ["scared","shock"],
  "😰": ["anxious","scared"],
  "😨": ["scared","fear"],
  "😧": ["sad","shock"],
  "😦": ["sad","shock"],
  "😳": ["shock","embarrassed"],

  // angry / evil / spooky
  "😠": ["angry","mad"],
  "😡": ["angry","mad"],
  "🤬": ["angry","censored"],
  "😤": ["angry","frustrated"],
  "👿": ["angry","devil","evil"],
  "😈": ["devil","evil","smile"],
  "💢": ["angry","symbol"],
  "💀": ["skull","dead","scary"],
  "☠️": ["skull","bones","danger"],
  "👻": ["ghost","scary"],
  "👹": ["monster","scary"],
  "👺": ["ogre","mask","scary"],
  "🤡": ["clown","weird","scary"],

  // shock / surprise / dizzy
  "😮": ["shock","surprised"],
  "😯": ["shock","surprised"],
  "😲": ["shock","wow"],
  "😵": ["dizzy","confused"],
  "😵‍💫": ["dizzy","spinning"],
  "🫨": ["shake","shock"],

  // hearts / love / hype
  "❤️": ["love","heart"],
  "🩷": ["love","heart","pink"],
  "💔": ["sad","heartbreak"],
  "💕": ["love","hearts"],
  "💞": ["love","hearts"],
  "💖": ["love","sparkle"],
  "💘": ["love","arrow"],
  "💝": ["love","gift"],
  "💯": ["100","hype"],
  "🔥": ["lit","fire","hype"],
  "✨": ["sparkles","magic"],
  "🎉": ["party","celebrate"],
  "🎊": ["party","celebrate"],

  // tech
  "🤖": ["robot","tech"],
  "👾": ["alien","game","retro"],
  "🧠": ["brain","smart"],
  "💻": ["computer","tech"],
  "⌨️": ["keyboard","tech"],
  "📱": ["phone","mobile"],
  "⚙️": ["gear","settings"],

  // monkeys
  "🙈": ["monkey","see no evil"],
  "🙉": ["monkey","hear no evil"],
  "🙊": ["monkey","speak no evil"],

  // hands / actions
  "👍": ["ok","yes","good","like"],
  "👎": ["no","bad","dislike"],
  "👏": ["clap","applause"],
  "🙌": ["praise","celebrate"],
  "🙏": ["pray","thanks"],
  "👌": ["ok","perfect"],
  "✌️": ["peace","victory"],
  "🤞": ["luck","fingers"],
  "🤘": ["rock","metal"],
  "🤙": ["call","hang loose"],
  "🫰": ["money","snap","tiny"],
  "🫶": ["heart","hands"],
  "🤝": ["handshake","deal"],
  "🫵": ["you","point"],

  // body
  "👀": ["eyes","look"],
  "👁": ["eye","look"],
  "👂": ["ear","listen"],
  "🦻": ["ear","hearing aid"],
  "👃": ["nose","smell"],
  "👅": ["tongue"],
  "👄": ["mouth","lips"],
  "🫦": ["biting","lips"],
  "💋": ["kiss","lips"],
};

export function EmojiPanel({ visible, onClose, onSelectEmoji }: Props) {
  const [query, setQuery] = useState("");

  if (!visible) return null;

  const handlePick = (emoji: string) => {
    onSelectEmoji(emoji);
    onClose();
  };

  const filteredEmojis = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return EMOJIS;
    return EMOJIS.filter((e) => {
      const tags = EMOJI_TAGS[e] || [];
      return tags.some((t) => t.includes(q));
    });
  }, [query]);

  return (
    <Modal transparent animationType="fade" visible={visible} onRequestClose={onClose}>
      <View style={styles.backdrop}>
        <View style={styles.panel}>
          <View style={styles.header}>
            <Text style={styles.title}>Emoji</Text>
            <TouchableOpacity onPress={onClose}>
              <Text style={styles.close}>✕</Text>
            </TouchableOpacity>
          </View>

          <View style={styles.searchRow}>
            <TextInput
              value={query}
              onChangeText={setQuery}
              placeholder="Search (e.g. sad, happy, angry, love)"
              placeholderTextColor="#6b7280"
              style={styles.searchInput}
            />
            {query.length > 0 && (
              <TouchableOpacity onPress={() => setQuery("")}>
                <Text style={styles.searchClear}>Clear</Text>
              </TouchableOpacity>
            )}
          </View>

          <ScrollView style={styles.scroll} contentContainerStyle={styles.grid}>
            {filteredEmojis.map((e, index) => (
              <TouchableOpacity
                key={`${e}-${index}`}
                style={styles.emojiButton}
                onPress={() => handlePick(e)}
              >
                <Text style={styles.emoji}>{e}</Text>
              </TouchableOpacity>
            ))}

            {filteredEmojis.length === 0 && (
              <View style={styles.emptyState}>
                <Text style={styles.emptyText}>No emojis for “{query}”</Text>
              </View>
            )}
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
    backgroundColor: "rgba(15,23,42,0.7)",
    justifyContent: "center",
    alignItems: "center",
  },
  panel: {
    width: 280,
    maxHeight: 360,
    backgroundColor: "#020617",
    borderRadius: 16,
    padding: 12,
    borderWidth: 1,
    borderColor: "rgba(56,189,248,0.5)",
  },
  header: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    marginBottom: 8,
  },
  title: {
    color: "#e5e7eb",
    fontWeight: "600",
    fontSize: 14,
  },
  close: {
    color: "#9ca3af",
    fontSize: 14,
  },
  searchRow: {
    flexDirection: "row",
    alignItems: "center",
    marginBottom: 8,
  },
  searchInput: {
    flex: 1,
    color: "#e5e7eb",
    paddingVertical: 6,
    paddingHorizontal: 8,
    borderRadius: 8,
    backgroundColor: "#020617",
    borderWidth: 1,
    borderColor: "rgba(148,163,184,0.6)",
    fontSize: 12,
  },
  searchClear: {
    marginLeft: 8,
    color: "#9ca3af",
    fontSize: 12,
  },
  scroll: {
    flexGrow: 0,
  },
  grid: {
    flexDirection: "row",
    flexWrap: "wrap",
    justifyContent: "space-between",
    paddingBottom: 4,
  },
  emojiButton: {
    width: "18%",
    paddingVertical: 6,
    alignItems: "center",
    marginVertical: 4,
  },
  emoji: {
    fontSize: 22,
  },
  emptyState: {
    paddingVertical: 16,
    width: "100%",
    alignItems: "center",
  },
  emptyText: {
    color: "#9ca3af",
    fontSize: 12,
  },
});
