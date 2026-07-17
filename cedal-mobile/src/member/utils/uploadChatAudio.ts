import { getStorage, ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { storage } from "@/src/api/firebase";

export async function uploadChatAudio(localUri: string, path: string) {
  const resp = await fetch(localUri);
  const blob = await resp.blob();

  const storageRef = ref(storage ?? getStorage(), path);
  const snapshot = await uploadBytes(storageRef, blob);
  const url = await getDownloadURL(snapshot.ref);
  return url;
}
