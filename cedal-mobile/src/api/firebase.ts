// src/api/firebase.ts
import { initializeApp, getApps, FirebaseApp } from "firebase/app";
import { getAuth, Auth } from "firebase/auth";
import { getFunctions, Functions } from "firebase/functions";
import {
  getFirestore,
  Firestore,
  doc,
  getDoc,
  setDoc,
} from "firebase/firestore";
import { getStorage, FirebaseStorage } from "firebase/storage";
import { getDatabase, Database } from "firebase/database";

const firebaseConfig = {
  apiKey: "AIzaSyDiOriQRpgIK_LHeFE8lwDkof44HlNfB8s",
  authDomain: "cedal-fd4a2.firebaseapp.com",
  projectId: "cedal-fd4a2",
  storageBucket: "cedal-fd4a2.firebasestorage.app",
  messagingSenderId: "717899371194",
  appId: "1:717899371194:web:7b4fd0a6e1368d3765a883",
};

let app: FirebaseApp;
let auth: Auth;
let db: Firestore;
let storage: FirebaseStorage;
let functions: Functions;
let euFunctions: Functions;
let rtdb: Database;

if (!getApps().length) {
  app = initializeApp(firebaseConfig);
} else {
  app = getApps()[0];
}

auth = getAuth(app);
db = getFirestore(app);
storage = getStorage(app);
functions = getFunctions(app);              // default (us-central1)
euFunctions = getFunctions(app, "europe-west2"); // alexReport region
rtdb = getDatabase(app);

// ---- devKey generator (7-char random) ----
const DEV_CHARS =
  "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";

function generateDevKey(length: number = 7): string {
  let result = "";
  for (let i = 0; i < length; i++) {
    const idx = Math.floor(Math.random() * DEV_CHARS.length);
    result += DEV_CHARS[idx];
  }
  return result;
}

// ---- ensure user profile ----
export async function ensureUserProfile(uid: string) {
  const publicRef  = doc(db, "users", uid);
  const privateRef = doc(db, "users", uid, "private", "profile");

  // Check if public profile already exists
  const snap = await getDoc(publicRef);
  if (!snap.exists()) {
    const devKey = generateDevKey(7);

    // 1) Public profile (NO devKey here)
    await setDoc(publicRef, {
      role: "user",
      devAccepted: false,
      createdAt: Date.now(), // or serverTimestamp() if you prefer
    });

    // 2) Private profile (devKey only owner can read)
    await setDoc(privateRef, {
      devKey,
    });
  }
}

export { app, auth, db, storage, functions, euFunctions, rtdb };
