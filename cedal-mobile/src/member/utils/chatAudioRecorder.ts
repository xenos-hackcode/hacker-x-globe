// src/member/utils/chatAudioRecorder.ts
import { Audio } from "expo-av";

let currentRecording: Audio.Recording | null = null;
let isPreparing = false;

export async function startChatRecording(): Promise<{
  recording: Audio.Recording;
} | null> {
  // prevent double‑tap starting two recordings
  if (isPreparing || currentRecording) {
    return currentRecording ? { recording: currentRecording } : null;
  }

  const { status } = await Audio.requestPermissionsAsync();
  if (status !== "granted") return null;

  await Audio.setAudioModeAsync({
    allowsRecordingIOS: true,
    playsInSilentModeIOS: true,
    staysActiveInBackground: false,
    shouldDuckAndroid: true,
  });

  isPreparing = true;
  try {
    const recording = new Audio.Recording();
    await recording.prepareToRecordAsync(
      Audio.RecordingOptionsPresets.HIGH_QUALITY,
    );
    await recording.startAsync();
    currentRecording = recording;
    return { recording };
  } finally {
    isPreparing = false;
  }
}

export async function stopChatRecording(
  recording?: Audio.Recording,
): Promise<string | null> {
  // prefer the passed recording, else fall back to global
  const rec = recording ?? currentRecording;
  if (!rec) return null;

  try {
    await rec.stopAndUnloadAsync();
  } catch {
    // already stopped/unloaded, ignore
  }

  const uri = rec.getURI() ?? null;
  if (rec === currentRecording) {
    currentRecording = null;
  }
  return uri;
}
