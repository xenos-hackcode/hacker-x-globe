// src/member/chat/useChatFriends.ts
import { auth, db } from "@/src/api/firebase";
import type { UserProfile } from "@/src/hooks/useUserProfile";
import {
    collection,
    doc,
    query as fsQuery,
    getDoc,
    onSnapshot,
    where,
} from "firebase/firestore";
import { useEffect, useState } from "react";

export type ChatFriend = {
  id: string;
  name: string;
  avatarUri?: string | null;
};

export function useChatFriends() {
  const [friends, setFriends] = useState<ChatFriend[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const current = auth.currentUser;
    if (!current) {
      setFriends([]);
      setLoading(false);
      return;
    }

    const baseRef = collection(db, "friendRequests");

    const qOut = fsQuery(
      baseRef,
      where("fromId", "==", current.uid),
      where("status", "==", "accepted"),
    );

    const qIn = fsQuery(
      baseRef,
      where("toId", "==", current.uid),
      where("status", "==", "accepted"),
    );

    const loadUserProfile = async (uid: string): Promise<ChatFriend | null> => {
      try {
        const snap = await getDoc(doc(db, "users", uid));
        if (!snap.exists()) return null;
        const u = snap.data() as UserProfile | any;
        const nickname = u.nickname || "";
        const email = u.email ?? null;
        const name = nickname || email || "Unknown";

        const avatarUrl =
          u.avatarUrl ||
          `https://ui-avatars.com/api/?name=${encodeURIComponent(
            name,
          )}&background=0f172a&color=e5e7eb`;

        return {
          id: uid,
          name,
          avatarUri: avatarUrl,
        };
      } catch (e) {
        console.error("Error loading friend profile", e);
        return null;
      }
    };

    // I sent the request → friend is toId
    const handleOutSnap = async (snap: any) => {
      const items = await Promise.all(
        snap.docs.map(async (d: any) => {
          const data = d.data() as any;
          const otherId = data.toId as string;
          return loadUserProfile(otherId);
        }),
      );
      const cleaned = items.filter((x): x is ChatFriend => !!x);
      setFriends((prev) => {
        const withoutThese = prev.filter(
          (c) => !cleaned.find((n) => n.id === c.id),
        );
        return [...withoutThese, ...cleaned];
      });
    };

    // They sent the request → friend is fromId
    const handleInSnap = async (snap: any) => {
      const items = await Promise.all(
        snap.docs.map(async (d: any) => {
          const data = d.data() as any;
          const otherId = data.fromId as string;
          return loadUserProfile(otherId);
        }),
      );
      const cleaned = items.filter((x): x is ChatFriend => !!x);
      setFriends((prev) => {
        const withoutThese = prev.filter(
          (c) => !cleaned.find((n) => n.id === c.id),
        );
        return [...withoutThese, ...cleaned];
      });
    };

    const unsubOut = onSnapshot(qOut, handleOutSnap);
    const unsubIn = onSnapshot(qIn, handleInSnap);

    setLoading(false);

    return () => {
      unsubOut();
      unsubIn();
    };
  }, []);

  return { friends, loading };
}
