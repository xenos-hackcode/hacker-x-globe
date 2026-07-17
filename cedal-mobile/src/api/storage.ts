// src/api/storage.ts
import { getStorage, ref, uploadBytes, getDownloadURL } from "firebase/storage";

export async function uploadAvatar(uid: string, uri: string): Promise<string> {
  const storage = getStorage();
  const response = await fetch(uri);
  const blob = await response.blob();

  const imageRef = ref(storage, `avatars/${uid}.jpg`);
  await uploadBytes(imageRef, blob);
  const downloadUrl = await getDownloadURL(imageRef); // public URL

  return downloadUrl;
}
