// src/member/utils/uploadChatImage.ts
import { getStorage, ref, uploadBytes, getDownloadURL } from "firebase/storage";

export async function uploadChatImage(uri: string, path: string): Promise<string> {
  const storage = getStorage();
  const res = await fetch(uri);
  const blob = await res.blob();

  const storageRef = ref(storage, path);
  await uploadBytes(storageRef, blob);

  return getDownloadURL(storageRef);
}
