// src/auth/biometricSession.ts
import * as SecureStore from "expo-secure-store";

export async function bindBiometricSession(userId: string) {
  try {
    await SecureStore.setItemAsync("cedal_session_token", userId);
    await SecureStore.setItemAsync("cedal_biometric_enabled", "true");
  } catch (e) {
    // optional: log error, but don't block login
    console.warn("Failed to bind biometric session", e);
  }
}
