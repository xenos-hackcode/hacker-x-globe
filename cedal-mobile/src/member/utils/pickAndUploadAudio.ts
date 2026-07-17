// src/member/utils/pickAndUploadAudio.ts
import * as DocumentPicker from "expo-document-picker";
import { storage } from "@/src/api/firebase";
import { ref, uploadBytes, getDownloadURL } from "firebase/storage";

export async function pickAndUploadAudioForChat(
  chatId: string,
  currentUserId: string,
): Promise<string | null> {
  try {
    const res = await DocumentPicker.getDocumentAsync({
      type: "audio/*",
      multiple: false,
      copyToCacheDirectory: true,
    });

    // Expo: cancelled is res.canceled, files are in res.assets
    if (res.canceled || !res.assets || res.assets.length === 0) {
      return null;
    }

    const file = res.assets[0];
    const uri = file.uri;
    if (!uri) return null;

    const name = file.name || "audio";
    const mimeType = file.mimeType ?? "audio/mpeg";

    const response = await fetch(uri);
    const blob = await response.blob();

    const fileName = `${currentUserId}_${Date.now()}_${name}`;
    const path = `chats/${chatId}/audioFiles/${fileName}`;
    const fileRef = ref(storage, path);

    await uploadBytes(fileRef, blob, { contentType: mimeType });
    const downloadUrl = await getDownloadURL(fileRef);
    return downloadUrl;
  } catch (e: any) {
    console.warn("audio file pick/upload error", e?.message);
    return null;
  }
}
