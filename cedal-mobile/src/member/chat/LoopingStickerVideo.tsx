// src/member/chat/LoopingStickerVideo.tsx
import React, { useCallback, useRef, useState } from "react";
import { Video, ResizeMode, AVPlaybackStatus } from "expo-av";

type Props = {
  uri: string;
};

export function LoopingStickerVideo({ uri }: Props) {
  const videoRef = useRef<Video | null>(null);
  const [loops, setLoops] = useState(0);

  const handleStatusUpdate = useCallback(
    (status: AVPlaybackStatus) => {
      if (!status.isLoaded) return;
      if (!status.isPlaying) return;

      // stop at 10 seconds
      if (status.positionMillis >= 10000) {
        if (loops < 2) {
          // play 3 times total (0,1,2)
          setLoops((prev) => prev + 1);
          videoRef.current?.setPositionAsync(0);
        } else {
          videoRef.current?.pauseAsync();
        }
      }
    },
    [loops],
  );

  return (
    <Video
  ref={videoRef}
  source={{ uri }}
  style={{ width: 120, height: 120, borderRadius: 12 }}
  isMuted
  resizeMode={ResizeMode.COVER}   // ✅ enum, not "cover"
  shouldPlay
  onPlaybackStatusUpdate={handleStatusUpdate}
/>
  );
}
