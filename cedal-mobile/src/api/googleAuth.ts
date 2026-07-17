import { GoogleAuthProvider, signInWithCredential } from "firebase/auth";
import * as Google from "expo-auth-session/providers/google";
import * as WebBrowser from "expo-web-browser";
import Constants from "expo-constants";
import { auth } from "./firebase";

type ExtraConfig = {
  googleAndroidClientId?: string;
};

const extra = (Constants.expoConfig?.extra ?? {}) as ExtraConfig;

const ANDROID_CLIENT_ID = extra.googleAndroidClientId ?? "";

if (!ANDROID_CLIENT_ID) {
  throw new Error("Missing googleAndroidClientId in app config.");
}

WebBrowser.maybeCompleteAuthSession();

export function useGoogleAuthRequest() {
  const [request, response, promptAsync] = Google.useAuthRequest({
    clientId: ANDROID_CLIENT_ID,
    scopes: ["openid", "profile", "email"],
  });

  async function handleResponse() {
    if (!response || response.type !== "success") {
      throw new Error("Google sign-in cancelled or failed.");
    }

    const authResult = response.authentication;
    if (!authResult || !authResult.idToken) {
      throw new Error("No idToken from Google.");
    }

    const credential = GoogleAuthProvider.credential(authResult.idToken);
    const cred = await signInWithCredential(auth, credential);
    return cred.user;
  }

  return { request, response, promptAsync, handleResponse };
}
