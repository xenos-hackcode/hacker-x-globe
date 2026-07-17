// src/emoji/customEmojis.ts
export type CustomEmoji = {
  id: string;
  source: any; // require() result
};

export const CUSTOM_EMOJIS: CustomEmoji[] = [
  {
    id: "neon_crazy_smile_1",
    source: require("@/src/stickers/sticker1.png"),
  },
  // add more later: sticker2, sticker3, etc.
];
