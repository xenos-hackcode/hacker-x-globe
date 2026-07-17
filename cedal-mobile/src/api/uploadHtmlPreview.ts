// src/api/uploadHtmlPreview.ts
import { ref, uploadString, getDownloadURL } from "firebase/storage";
import { auth, storage } from "@/src/api/firebase";

export async function uploadHtmlPreview(code: string): Promise<string> {
  const uid = auth.currentUser?.uid ?? "anon";
  const path = `htmlPreviews/${uid}/${Date.now()}.html`;
  const fileRef = ref(storage, path);
  await uploadString(fileRef, code, "raw", { contentType: "text/html" });
  return getDownloadURL(fileRef);
}
