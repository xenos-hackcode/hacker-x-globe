// src/member/utils/pickAndUploadDocument.ts
import * as DocumentPicker from "expo-document-picker";
import * as FileSystem from "expo-file-system";
import { storage } from "@/src/api/firebase";
import { ref, uploadBytes, getDownloadURL } from "firebase/storage";
import { Buffer } from "buffer";

/** tiny base64→Uint8Array helper */
function base64ToUint8Array(base64: string): Uint8Array {
  const binary = Buffer.from(base64, "base64").toString("binary");
  const len = binary.length;
  const bytes = new Uint8Array(len);
  for (let i = 0; i < len; i++) bytes[i] = binary.charCodeAt(i);
  return bytes;
}

export async function pickAndUploadDocumentForChat(
  chatId: string,
  currentUserId: string,
) {
  try {
    const res = await DocumentPicker.getDocumentAsync({
      type: [
        "application/pdf",
        "text/plain",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
      ],
      multiple: false,
      copyToCacheDirectory: true,
    });

    if (res.canceled || !res.assets || res.assets.length === 0) {
      return null;
    }

    const file = res.assets[0];
    const uri = file.uri;
    if (!uri) return null;

    const name = file.name || "document";
    const mimeType = file.mimeType ?? "application/octet-stream";
    const size = typeof file.size === "number" ? file.size : null;

    // Read as base64 string
    const base64 = await FileSystem.readAsStringAsync(uri, {
      encoding: "base64",
    });

    const buffer = base64ToUint8Array(base64);

    const fileName = `${currentUserId}_${Date.now()}_${name}`;
    const path = `chats/${chatId}/documents/${fileName}`;
    const fileRef = ref(storage, path);

    await uploadBytes(fileRef, buffer, { contentType: mimeType });
    const downloadUrl = await getDownloadURL(fileRef);

    return { downloadUrl, name, size, mimeType };
  } catch (e: any) {
    console.warn("document pick/upload error", e?.message);
    return null;
  }
}
