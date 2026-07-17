// src/api/profile.ts
import { db } from "@/src/api/firebase";
import { doc, updateDoc } from "firebase/firestore";

export async function updateUserProfile(
  uid: string,
  data: {
    nickname?: string;
    customLink?: string;
    randomLink?: string;
    age?: number | null;
    occupation?: string;
    hobby?: string;
    bio?: string;
    gender?: string;
    avatarUrl?: string | null; 
  }
) {
  const ref = doc(db, "users", uid);
  await updateDoc(ref, data);
}
