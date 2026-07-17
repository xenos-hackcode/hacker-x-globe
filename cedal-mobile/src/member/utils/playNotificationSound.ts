// src/member/utils/playNotificationSound.ts
import { Audio } from "expo-av";

export async function playNotificationSound() {
  try {
    const { sound } = await Audio.Sound.createAsync(
      require("../../../assets/notification.wav")
    );

    await sound.playAsync();

    sound.setOnPlaybackStatusUpdate((status) => {
      if (!status.isLoaded) return;
      if (status.didJustFinish) {
        sound.unloadAsync();
      }
    });
  } catch (e) {
    console.log("Failed to play notification sound", e);
  }
}
