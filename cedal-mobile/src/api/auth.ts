// src/api/auth.ts
import * as SecureStore from "expo-secure-store";
import {
    createUserWithEmailAndPassword,
    signInAnonymously,
    signInWithEmailAndPassword,
    signOut,
    UserCredential,
} from "firebase/auth";
import { auth } from "./firebase";

export type AuthUser = {
  uid: string;
  email: string | null;
  displayName: string | null;
  photoURL: string | null;
};

function mapUser(cred: UserCredential): AuthUser {
  const u = cred.user;
  return {
    uid: u.uid,
    email: u.email,
    displayName: u.displayName,
    photoURL: u.photoURL,
  };
}

async function bindBiometricSession(uid: string) {
  try {
    await SecureStore.setItemAsync("cedal_session_token", uid);
    await SecureStore.setItemAsync("cedal_biometric_enabled", "true");
  } catch (e) {
    console.warn("Failed to bind biometric session", e);
  }
}

// Email sign up (creation)
export async function signUpWithEmail(email: string, password: string) {
  const cred = await createUserWithEmailAndPassword(auth, email, password);
  if (cred.user?.uid) {
    await bindBiometricSession(cred.user.uid);
  }
  return mapUser(cred);
}

// Email sign in
export async function signInWithEmail(email: string, password: string) {
  const cred = await signInWithEmailAndPassword(auth, email, password);
  if (cred.user?.uid) {
    await bindBiometricSession(cred.user.uid);
  }
  return mapUser(cred);
}

// Guest / anonymous sign-in
export async function signInAsGuest() {
  const cred = await signInAnonymously(auth);
  if (cred.user?.uid) {
    await bindBiometricSession(cred.user.uid);
  }
  return mapUser(cred);
}

// Logout
export async function logout() {
  await signOut(auth);
  try {
    await SecureStore.deleteItemAsync("cedal_session_token");
    await SecureStore.deleteItemAsync("cedal_biometric_enabled");
  } catch (e) {
    console.warn("Failed to clear biometric session", e);
  }
}
