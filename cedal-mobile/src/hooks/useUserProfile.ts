// src/hooks/useUserProfile.ts
import { auth, db } from "@/src/api/firebase";
import { onAuthStateChanged, User } from "firebase/auth";
import {
  doc,
  FirestoreError,
  onSnapshot,
  serverTimestamp,
  setDoc,
  Timestamp,
} from "firebase/firestore";
import { useEffect, useState } from "react";

export type UserProfile = {
  id: string;
  email: string | null;
  role: "user" | "owner" | "developer" | string;
  age?: number;
  createdAt?: any;      // Timestamp
  online?: boolean;
  lastSeen?: any;       // Timestamp

  // activity
  level?: number;       // 0–1000
  xp?: number;          // raw exp seconds
  points?: number;      // achievements count / score
  messagesSent?: number;
  stickersSent?: number;
  streakDays?: number;
  lastActiveAt?: any;

  // reputation
  reputation?: number;  // -100 .. 500
  vip?: boolean;        // true when reputation >= 500
  banned?: boolean;     // true when reputation <= -100
  lastResetAt?: any;    // last quarterly reset

  // profile fields
  nickname?: string;
  customLink?: string;
  randomLink?: string;
  bio?: string;
  occupation?: string;
  hobby?: string;
  gender?: string;
  avatarUrl?: string | null;

  // wallet
  scBalance?: number;   // Star Coins

  // groups / cooldowns
  nextGroupCreateAt?: Date | null;
};

type UseUserProfileState = {
  user: User | null;
  profile: UserProfile | null;
  loading: boolean;
  error: FirestoreError | null;
};

export function useUserProfile(): UseUserProfileState {
  const [user, setUser] = useState<User | null>(null);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<FirestoreError | null>(null);

  useEffect(() => {
    const unsubAuth = onAuthStateChanged(auth, async (firebaseUser) => {
      setUser(firebaseUser);
      setProfile(null);
      setError(null);

      if (!firebaseUser) {
        setLoading(false);
        return;
      }

      const uid = firebaseUser.uid;
      const ref = doc(db, "users", uid);

      const unsubProfile = onSnapshot(
        ref,
        async (snap) => {
          if (!snap.exists()) {
            const baseProfile: Omit<UserProfile, "id"> = {
              email: firebaseUser.email,
              role: "user",
              createdAt: serverTimestamp(),
              online: true,
              lastSeen: serverTimestamp(),

              // defaults for new fields
              level: 0,
              xp: 0,
              points: 0,
              messagesSent: 0,
              stickersSent: 0,
              streakDays: 0,
              reputation: 0,
              vip: false,
              banned: false,

              // wallet defaults
              scBalance: 0,
            };
            await setDoc(ref, baseProfile, { merge: true });
            return;
          }

          const data = snap.data() as any;

setProfile({
  id: snap.id,
  email: data.email ?? firebaseUser.email,
  role: data.role ?? "user",
  age: data.age,
  createdAt: data.createdAt,
  online: data.online,
  lastSeen: data.lastSeen,

  // activity
  level: data.level ?? 0,
  xp: data.xp ?? 0,
  points: data.points ?? 0,
  messagesSent: data.statsMessagesSent ?? 0,   // <-- change
  stickersSent: data.statsStickersSent ?? 0,   // <-- change
  streakDays: data.streakDays ?? 0,
  lastActiveAt: data.lastActiveAt,

  // reputation
  reputation: data.reputation ?? 0,
  vip: data.vip ?? false,
  banned: data.banned ?? false,
  lastResetAt: data.lastResetAt,

  // profile fields
  nickname: data.nickname,
  customLink: data.customLink,
  randomLink: data.randomLink,
  bio: data.bio,
  occupation: data.occupation,
  hobby: data.hobby,
  gender: data.gender,
  avatarUrl: data.avatarUrl ?? null,

  // wallet
  scBalance: data.scBalance ?? 0,

  // groups / cooldowns
  nextGroupCreateAt: data.nextGroupCreateAt
    ? (data.nextGroupCreateAt as Timestamp).toDate()
    : null,
});
          setLoading(false);
        },
        (err) => {
          setError(err);
          setLoading(false);
        },
      );

      return () => {
        unsubProfile();
      };
    });

    return () => {
      unsubAuth();
    };
  }, []);

  return { user, profile, loading, error };
}
