// src/utils/notifications.ts
import * as Notifications from "expo-notifications";

// Call this once at app start (e.g. in App.tsx)
export async function initNotifications() {
  const { status } = await Notifications.requestPermissionsAsync();
  if (status !== "granted") return;

  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldShowAlert: true,
      shouldPlaySound: false,
      shouldSetBadge: false,
      shouldShowBanner: true, // iOS 15+ notification center banner[web:355]
      shouldShowList: true, 
    }),
  });
}

// For email verification sent
export async function notifyVerificationSent(email: string) {
  await Notifications.scheduleNotificationAsync({
    content: {
      title: "CEDAL · VERIFY NODE",
      body: `Verification ping sent to ${email}. Check your Gmail inbox or spam to unlock full access.`,
    },
    trigger: null, // fire immediately[web:355][web:357]
  });
}
