// src/member/utils/chatVideo.ts
import * as ImagePicker from "expo-image-picker";

export async function openChatVideo(): Promise<string | null> {
  const { status } = await ImagePicker.requestCameraPermissionsAsync();
  if (status !== "granted") return null;

  const result = await ImagePicker.launchCameraAsync({
    mediaTypes: ImagePicker.MediaTypeOptions.Videos, // <- array form
    allowsEditing: false,
    quality: ImagePicker.UIImagePickerControllerQualityType.Medium,
    videoMaxDuration: 30,
  });

  if (result.canceled) return null;
  return result.assets[0].uri;
}
