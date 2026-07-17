// src/hooks/useChatMessages.ts
import { useEffect, useState } from "react";
import {
  collection,
  onSnapshot,
  orderBy,
  query,
} from "firebase/firestore";
import { db } from "@/src/api/firebase";
import { Message } from "@/src/member/chat/MessageRow";

export function useChatMessages(
  chatId: string,
  currentUserId: string,
  currentUserName: string,
  isCorneal: boolean,
) {
  const [messages, setMessages] = useState<Message[]>([]);

  useEffect(() => {
    if (!chatId || !currentUserId) return;

    // Corneal AI chat under users/{uid}/aiChats/corneal/messages
    if (isCorneal) {
      const aiMsgsCol = collection(
        db,
        "users",
        currentUserId,
        "aiChats",
        "corneal",
        "messages",
      );
      const qMsgs = query(aiMsgsCol, orderBy("createdAt", "asc"));

      const unsub = onSnapshot(qMsgs, (snap) => {
        const list: Message[] = snap.docs.map((d) => {
          const data = d.data() as any;
          const role = data.role || "user";
          const isAssistant = role === "assistant";

          return {
            id: d.id,
            text: data.text ?? "",
            createdAt: data.createdAt?.toDate?.() ?? null,
            userId: isAssistant ? "corneal" : currentUserId,
            userName: isAssistant ? "Corneal" : currentUserName,

            type: data.type ?? "text",

            // reply fields so Corneal can also quote previous messages
            replyToId: data.replyToId ?? null,
            replyToText: data.replyToText ?? null,
            replyToUserName: data.replyToUserName ?? null,

            // contact (if you ever send contacts in AI chat)
            contactUserId: data.contactUserId ?? null,
            contactUserName: data.contactUserName ?? null,
          };
        });
        setMessages(list);
      });

      return () => unsub();
    }

   // Normal rooms/* chat
const msgsRef = collection(db, "rooms", String(chatId), "messages");
const q = query(msgsRef, orderBy("createdAt", "desc"));

const unsub = onSnapshot(q, (snap) => {
  const list: Message[] = [];
  snap.forEach((docSnap) => {
    const data = docSnap.data() as any;
    list.push({
      id: docSnap.id,
      text: data.text ?? "",
      createdAt: data.createdAt?.toDate?.() ?? null,
      userId: data.userId ?? "unknown",
      userName: data.userName ?? "Unknown",

      imageUri: data.imageUri ?? null,
      videoUri: data.videoUri ?? null,
      audioUri: data.audioUri ?? null,

      type: data.type ?? "text",
      stickerUri: data.stickerUri ?? null,
      stickerKind: data.stickerKind ?? undefined,

      fileUri: data.fileUri ?? null,
      fileName: data.fileName ?? null,
      fileSize: data.fileSize ?? null,
      fileMime: data.fileMime ?? null,

      viewOnce: data.viewOnce ?? false,
      viewConfig: data.viewConfig ?? null,
      perUserViews: data.perUserViews ?? {},
      firstViewAt: data.firstViewAt ?? null,

      replyToId: data.replyToId ?? null,
      replyToText: data.replyToText ?? null,
      replyToUserName: data.replyToUserName ?? null,
      replyToType: data.replyToType ?? null,
      replyToStickerUri: data.replyToStickerUri ?? null,
      replyToViewOnce: data.replyToViewOnce ?? null,

      // 👇 add this line for reactions
      reactions: data.reactions ?? {},
      stickerReactions: data.stickerReactions ?? {},

      question: data.question ?? null,
      options: data.options ?? [],
      allowMulti: data.allowMulti ?? true,
      votesByUser: data.votesByUser ?? {},

      eventTitle: data.eventTitle ?? null,
      eventStartsAt: data.eventStartsAt ?? null,
      eventEndsAt: data.eventEndsAt ?? null,
      eventLocation: data.eventLocation ?? null,
      eventNotes: data.eventNotes ?? null,

      contactUserId: data.contactUserId ?? null,
      contactUserName: data.contactUserName ?? null,

      savedBy: data.savedBy ?? [],

      deleted: data.deleted ?? false,
    });
  });
  setMessages(list);
});

    return () => unsub();
  }, [chatId, currentUserId, currentUserName, isCorneal]);

  return messages;
}
