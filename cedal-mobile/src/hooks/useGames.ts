// src/hooks/useGames.ts
import { useEffect, useState } from "react";
import {
  collection,
  onSnapshot,
  orderBy,
  query,
} from "firebase/firestore";
import { db } from "@/src/api/firebase";
import { GameDoc } from "@/src/api/games/createGame";

export type Game = GameDoc & { id: string };

export function useGames() {
  const [games, setGames] = useState<Game[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const q = query(collection(db, "games"), orderBy("createdAt", "desc"));
    const unsub = onSnapshot(
      q,
      (snap) => {
        const next: Game[] = snap.docs.map((d) => ({
          id: d.id,
          ...(d.data() as GameDoc),
        }));
        setGames(next);
        setLoading(false);
      },
      () => setLoading(false)
    );
    return unsub;
  }, []);

  return { games, loading };
}
