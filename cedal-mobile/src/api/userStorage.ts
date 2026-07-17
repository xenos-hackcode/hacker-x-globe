// src/api/userStorage.ts
import { auth, db } from "@/src/api/firebase";
import { doc, getDoc, setDoc } from "firebase/firestore";

// reuse your existing type here
export type ChatThemeKind = "none" | "userPhoto" | "preset";

export type ChatTheme = {
  kind: ChatThemeKind;
  backgroundImageUrl?: string | null;
  backgroundColor?: string | null;
  textColor?: string | null;
  textFontFamily?: string | null;
  textSize?: number | null;
  bubbleMeColor?: string | null;
  bubbleOtherColor?: string | null;
  tabAccentColor?: string | null;
  tabLabelColor?: string | null;
  presetId?: string | null;
};

// SAVE to users/{uid}.chatTheme
export async function saveChatTheme(theme: ChatTheme) {
  const user = auth.currentUser;
  if (!user) throw new Error("No user signed in");

  await setDoc(
    doc(db, "users", user.uid),
    { chatTheme: theme },
    { merge: true }
  ); // [web:368]
}

// LOAD from users/{uid}.chatTheme
export async function loadChatTheme(): Promise<ChatTheme | null> {
  const user = auth.currentUser;
  if (!user) return null;

  const snap = await getDoc(doc(db, "users", user.uid));
  if (!snap.exists()) return null;

  const data = snap.data() as any;
  return (data.chatTheme as ChatTheme) ?? null;
}
