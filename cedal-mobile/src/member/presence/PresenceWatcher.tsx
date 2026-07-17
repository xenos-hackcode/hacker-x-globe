// src/member/presence/PresenceWatcher.tsx
import { useEffect } from "react";
import { onAuthStateChanged } from "firebase/auth";
import { auth } from "@/src/api/firebase";
import { setupRealtimePresence } from "@/src/member/presence/setupRealtimePresence";

export function PresenceWatcher() {
  useEffect(() => {
    const unsub = onAuthStateChanged(auth, (user) => {
      if (user) {
        setupRealtimePresence();
      }
    });
    return () => unsub();
  }, []);

  return null;
}
