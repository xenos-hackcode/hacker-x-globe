// src/calls/useRtcStats.ts
import { useEffect, useState } from "react";
// If you want typing, import from react-native-webrtc:
// import { RTCPeerConnection } from "react-native-webrtc";

export type RtcStats = {
  pingMs: number | null;
  jitterMs: number | null;
  audioLevel: number; // 0–1
};

export function useRtcStats(peer: RTCPeerConnection | null): RtcStats {
  const [stats, setStats] = useState<RtcStats>({
    pingMs: null,
    jitterMs: null,
    audioLevel: 0,
  });

  useEffect(() => {
    if (!peer) return;

    let cancelled = false;
    let lastAudioLevel = 0;

    const intervalId = setInterval(async () => {
      try {
        const report = await peer.getStats(); // WebRTC stats API[web:506][web:521]

        let pingMs: number | null = null;
        let jitterMs: number | null = null;
        let audioLevel = lastAudioLevel;

        report.forEach((stat: any) => {
          // RTT from candidate-pair
          if (
            stat.type === "candidate-pair" &&
            stat.state === "succeeded" &&
            typeof stat.currentRoundTripTime === "number"
          ) {
            pingMs = stat.currentRoundTripTime * 1000; // seconds → ms[web:506]
          }

          // Audio RTP stats (for jitter + level)
          if (
            (stat.type === "remote-inbound-rtp" ||
              stat.type === "inbound-rtp") &&
            stat.kind === "audio"
          ) {
            if (typeof stat.jitter === "number") {
              jitterMs = stat.jitter * 1000; // seconds → ms[web:506][web:520]
            }

            // Some stacks expose audioInputLevel/audioOutputLevel 0–32767[web:513]
            const rawLevel =
              (stat.audioInputLevel as number | undefined) ??
              (stat.audioOutputLevel as number | undefined);
            if (typeof rawLevel === "number") {
              audioLevel = Math.min(1, Math.max(0, rawLevel / 32767));
            }
          }
        });

        lastAudioLevel = audioLevel;

        if (!cancelled) {
          setStats((prev) => ({
            pingMs: pingMs ?? prev.pingMs ?? null,
            jitterMs: jitterMs ?? prev.jitterMs ?? null,
            audioLevel,
          }));
        }
      } catch {
        // ignore stats errors
      }
    }, 1000); // 1s polling is a common compromise[web:513][web:517]

    return () => {
      cancelled = true;
      clearInterval(intervalId);
    };
  }, [peer]);

  return stats;
}
