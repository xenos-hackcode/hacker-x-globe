// src/member/chat/chatData.ts
export type AvatarRemote = {
  type: "remote";
  uri: string;
};

export type AvatarLocal = {
  type: "local";
  source: any;
};

export type AvatarInitial = {
  type: "initial";
};

export type ChatAvatar = AvatarRemote | AvatarLocal | AvatarInitial;

export type ChatItem = {
  id: string;
  name: string;
  lastMessage: string;
  time: string;
  unread: number;
  avatar: ChatAvatar;
};

export const CHATS: ChatItem[] = [
  {
    id: "general",
    name: "Cedal System Feed",
    lastMessage: "System feed and global updates.",
    time: "09:24",
    unread: 0, // ✅ no notification by default
    avatar: {
      type: "local",
      source: require("./general.png"),
    },
  },
  {
    id: "corneal",
    name: "Corneal",
    lastMessage: "Corneal chat waiting for you.",
    time: "Yesterday",
    unread: 0,
    avatar: {
      type: "local",
      source: require("./corneal.png"),
    },
  },
];
