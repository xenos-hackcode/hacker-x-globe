// src/api/account.ts
import { auth, db } from "./firebase";
import { doc, setDoc } from "firebase/firestore";
import * as SecureStore from "expo-secure-store";

export async function updatePasscode(newCode: string) {
  const user = auth.currentUser;
  if (!user) {
    throw new Error("Not signed in.");
  }

  const uid = user.uid;

  // 1) Update Firestore userKey (app-level passcode)
  const userRef = doc(db, "users", uid);
  await setDoc(
    userRef,
    { userKey: newCode },
    { merge: true }
  );

  // 2) Update local encrypted passcode
  await SecureStore.setItemAsync(`cedal_passcode_${uid}`, newCode);
}
