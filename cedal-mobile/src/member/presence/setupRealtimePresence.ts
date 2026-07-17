// src/member/presence/setupRealtimePresence.ts
import { rtdb, auth } from "@/src/api/firebase";
import { ref, onDisconnect, onValue, set } from "firebase/database";

export function setupRealtimePresence() {
  const user = auth.currentUser;
  if (!user) return; // make sure you only call this AFTER login

  const uid = user.uid;
  const statusRef = ref(rtdb, "/status/" + uid);
  const connectedRef = ref(rtdb, ".info/connected");

  const isOfflineForDatabase = {
    state: "offline",
    last_changed: Date.now(),
  };

  const isOnlineForDatabase = {
    state: "online",
    last_changed: Date.now(),
  };

  onValue(connectedRef, (snap) => {
    // if still not connected, or auth dropped, bail
    if (snap.val() === false || !auth.currentUser) {
      return;
    }

    onDisconnect(statusRef)
      .set(isOfflineForDatabase)
      .then(() => {
        set(statusRef, isOnlineForDatabase);
      })
      .catch((err) => {
        console.warn("onDisconnect/set error", err);
      });
  });
}
