// src/api/games/createGame.ts
import { addDoc, collection, serverTimestamp } from "firebase/firestore";
import { db } from "@/src/api/firebase"; // adjust to your firebase init

export type GameGenre = "Action" | "Casual" | "RPG" | "Simulation";

export type GameDoc = {
  name: string;
  genre: GameGenre;
  createdBy: "app" | "ai" | "user";
  players: number;
  likes: number;
  createdAt: any;
};

export async function createGame(payload: Omit<GameDoc, "createdAt">) {
  const ref = collection(db, "games");
  const docRef = await addDoc(ref, {
    ...payload,
    createdAt: serverTimestamp(),
  });
  return docRef.id; // real Firestore id
}
