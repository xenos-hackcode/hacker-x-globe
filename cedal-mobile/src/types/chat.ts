// A pack of stickers a user can browse
export type StickerPack = {
  id: string;
  ownerId: string;
  name: string;
  coverUri: string | null; // small preview sticker
  isGlobal: boolean;       // true = visible to everyone
  createdAt: Date | null;
};

// Single sticker image
export type Sticker = {
  id: string;
  packId: string;
  imageUri: string;
  creatorId: string;
  createdAt: Date | null;
  isAnimated?: boolean;
};

// A pack of custom emoji (small stickers)
export type EmojiPack = {
  id: string;
  ownerId: string;
  name: string;
  iconUri: string | null;
  isGlobal: boolean;
  createdAt: Date | null;
};

export type CustomEmoji = {
  id: string;
  packId: string;
  imageUri: string;
  creatorId: string;
  shortcode: string; // ":vibes:", ":fire_me:" etc
  createdAt: Date | null;
};
