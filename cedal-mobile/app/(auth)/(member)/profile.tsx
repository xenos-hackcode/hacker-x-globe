// app/(auth)/(member)/profile.tsx
import { updateUserProfile } from "@/src/api/profile";
import { uploadAvatar } from "@/src/api/storage";
import { useUserProfile } from "@/src/hooks/useUserProfile";
import MemberProfileScreen from "@/src/member/profile/MemberProfileScreen";
import * as ImagePicker from "expo-image-picker";
import { router } from "expo-router";
import React, { useEffect, useState } from "react";
import { ActivityIndicator, Alert, StyleSheet, Text, View } from "react-native";

export default function MemberProfileRoute() {
  const { user, profile, loading } = useUserProfile();

  const baseName = profile?.email?.split("@")[0] || "Design lab";
  const handle = profile?.email || "no-email@cedal.dev";
  const presence: "online" | "offline" = profile?.online ? "online" : "offline";
  const level = profile?.level ?? 0;
  const points = profile?.points ?? 0;
  const messages = profile?.messagesSent ?? 0;
  const stickers = profile?.stickersSent ?? 0;
  const streak = profile?.streakDays ?? 0;
  const reputation = profile?.reputation ?? 0;

  const [avatarUri, setAvatarUri] = useState<string | null>(null);
  const [nickname, setNickname] = useState("");
  const [customLink, setCustomLink] = useState("");
  const [randomLink, setRandomLink] = useState("");
  const [bio, setBio] = useState("");
  const [age, setAge] = useState<string>("");
  const [occupation, setOccupation] = useState("");
  const [hobby, setHobby] = useState("");
  const [gender, setGender] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!profile) return;
    const anyProfile = profile as any;
    setNickname(anyProfile.nickname || baseName);
    setCustomLink(anyProfile.customLink || `act://node/${handle}`);
    setRandomLink(anyProfile.randomLink || "https://cedal.dev/xxxxxx");
    setBio(
      anyProfile.bio ||
        "Grinding ranked, clipping shorts, and dropping VODs for the node. Uploads from streams will surface here."
    );
    setAge(
      anyProfile.age !== undefined && anyProfile.age !== null
        ? String(anyProfile.age)
        : ""
    );
    setOccupation(anyProfile.occupation || "");
    setHobby(anyProfile.hobby || "");
    setGender(anyProfile.gender || "");
    if (anyProfile.avatarUrl) {
      setAvatarUri(anyProfile.avatarUrl);
    }
  }, [profile, baseName, handle]);

  async function handlePickAvatar() {
    const permissionResult =
      await ImagePicker.requestMediaLibraryPermissionsAsync();

    if (!permissionResult.granted) {
      Alert.alert(
        "Permission needed",
        "Cedal needs access to your photos to set a profile picture."
      );
      return;
    }

    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [1, 1],
      quality: 0.9,
    });

    if (result.canceled) return;

    const asset = result.assets[0];
    if (!asset?.uri || !user) return;

    try {
      const downloadUrl = await uploadAvatar(user.uid, asset.uri);
      await updateUserProfile(user.uid, { avatarUrl: downloadUrl });
      setAvatarUri(downloadUrl);
    } catch (e) {
      Alert.alert("Upload failed", "Could not update your profile picture.");
    }
  }

  async function handleBack() {
    if (!user || !profile) {
      router.back();
      return;
    }

    try {
      setSaving(true);
      await updateUserProfile(user.uid, {
        nickname: nickname.trim() || baseName,
        customLink: customLink.trim() || `act://node/${handle}`,
        randomLink,
        bio: bio.trim(),
        age: age.trim() ? Number(age.trim()) : null,
        occupation: occupation.trim() || "",
        hobby: hobby.trim() || "",
        gender: gender.trim() || "",
      });
    } catch {
      // optional alert
    } finally {
      setSaving(false);
      router.back();
    }
  }

  if (loading) {
    return (
      <View style={styles.loadingScreen}>
        <ActivityIndicator color="#22c55e" />
        <Text style={styles.loadingText}>Loading profile…</Text>
      </View>
    );
  }

  if (!user || !profile) {
    return (
      <View style={styles.loadingScreen}>
        <Text style={styles.loadingText}>No profile found.</Text>
      </View>
    );
  }

  return (
    <MemberProfileScreen
      saving={saving}
      onBack={handleBack}
      onPickAvatar={handlePickAvatar}
      avatarUri={avatarUri}
      baseName={baseName}
      handle={handle}
      presence={presence}
      level={level}
      points={points}
      messages={messages}
      stickers={stickers}
      streak={streak}
      reputation={reputation}
      nickname={nickname}
      setNickname={setNickname}
      customLink={customLink}
      setCustomLink={setCustomLink}
      randomLink={randomLink}
      bio={bio}
      setBio={setBio}
      age={age}
      setAge={setAge}
      occupation={occupation}
      setOccupation={setOccupation}
      hobby={hobby}
      setHobby={setHobby}
      gender={gender}
      setGender={setGender}
    />
  );
}

const styles = StyleSheet.create({
  loadingScreen: {
    flex: 1,
    backgroundColor: "#020617",
    alignItems: "center",
    justifyContent: "center",
  },
  loadingText: { marginTop: 8, color: "#e5e7eb", fontSize: 13 },
});
