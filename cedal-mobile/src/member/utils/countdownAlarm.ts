// src/member/utils/countdownAlarm.ts
import * as Notifications from "expo-notifications";
import { Vibration } from "react-native";

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: false,
    shouldShowBanner: true,
    shouldShowList: true,
  }),
});

export async function scheduleCountdownAlarm(targetTs: number, title: string) {
  const trigger = {
    type: "date" as const,
    date: new Date(targetTs),
  };

  await Notifications.scheduleNotificationAsync({
    content: {
      title: "Timer finished",
      body: title,
    },
    trigger: trigger as any,
  });
}

export function startLongVibration() {
  const pattern: number[] = [];
  for (let i = 0; i < 60; i++) {
    pattern.push(5000, 5000);
  }
  Vibration.vibrate(pattern, false);
}

export function stopVibration() {
  Vibration.cancel();
}
