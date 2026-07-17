// src/member/utils/chatCamera.ts
import * as ImagePicker from "expo-image-picker";

export async function openChatCamera(): Promise<string | null> {
  const { status } = await ImagePicker.requestCameraPermissionsAsync();
  if (status !== "granted") return null;

  const result = await ImagePicker.launchCameraAsync({
    mediaTypes: ImagePicker.MediaTypeOptions.Images,
    allowsEditing: true,
    aspect: [3, 4],
    quality: 0.8,
  });

  if (result.canceled) return null;
  return result.assets[0].uri;
}
