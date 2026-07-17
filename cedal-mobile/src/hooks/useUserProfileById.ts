// src/hooks/useUserProfileById.ts
import { useEffect, useState } from "react";
import { doc, onSnapshot, FirestoreError } from "firebase/firestore";
import { db } from "@/src/api/firebase";
import type { UserProfile } from "./useUserProfile";

type State = {
  profile: UserProfile | null;
  loading: boolean;
  error: FirestoreError | null;
};

export function useUserProfileById(userId: string | null): State {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<FirestoreError | null>(null);

  useEffect(() => {
    if (!userId) {
      setProfile(null);
      setLoading(false);
      return;
    }

    const ref = doc(db, "users", userId);
    const unsub = onSnapshot(
      ref,
       (snap) => {
        if (!snap.exists()) {
          setProfile(null);
        } else {
          const data = snap.data() as any;

          setProfile({
            id: snap.id,
            email: data.email ?? null,
            role: data.role ?? "user",
            age: data.age,
            createdAt: data.createdAt,
            online: data.online,
            lastSeen: data.lastSeen,

            // activity (GLOBAL)
            level: data.level ?? 0,
            xp: data.xp ?? 0,
            points: data.points ?? 0,
            messagesSent: data.statsMessagesSent ?? 0,
            stickersSent: data.statsStickersSent ?? 0,
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

            // if you want global wallet here too:
            scBalance: data.scBalance ?? 0,
          });
        }
        setLoading(false);
      },
      (err) => {
        setError(err);
        setLoading(false);
      }
    );

    return () => unsub();
  }, [userId]);

  return { profile, loading, error };
}
