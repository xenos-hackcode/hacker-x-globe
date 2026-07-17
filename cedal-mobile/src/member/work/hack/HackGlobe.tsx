// src/member/work/hack/HackGlobe.tsx
import React, {
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import {
  Dimensions,
  GestureResponderEvent,
  PanResponder,
  StyleSheet,
  View,
  TextInput,
  TouchableOpacity,
  Text,
  FlatList,
} from "react-native";
import { Canvas, Circle, Line, Group, vec } from "@shopify/react-native-skia";
import {
  askHackGlobe,
  type HackGlobeMessage,
} from "@/src/api/hackGlobeAssistant";
import { HackLabConfig } from "@/src/member/work/hack/HackTypes";
import { db } from "@/src/api/firebase";
import {
  collection,
  addDoc,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
} from "firebase/firestore";

const { width: SCREEN_WIDTH, height: SCREEN_HEIGHT } = Dimensions.get("window");

// math helpers
type Point3D = { x: number; y: number; z: number };

function generateSpherePoints(count: number, radius: number): Point3D[] {
  const pts: Point3D[] = [];
  for (let i = 0; i < count; i++) {
    const u = Math.random();
    const v = Math.random();
    const theta = 2 * Math.PI * u;
    const phi = Math.acos(2 * v - 1);

    const x = radius * Math.sin(phi) * Math.cos(theta);
    const y = radius * Math.sin(phi) * Math.sin(theta);
    const z = radius * Math.cos(phi);

    pts.push({ x, y, z });
  }
  return pts;
}

function computeNeighbors(points: Point3D[], k: number): number[][] {
  const neighbors: number[][] = Array.from({ length: points.length }, () => []);
  for (let i = 0; i < points.length; i++) {
    const dists: { j: number; d2: number }[] = [];
    for (let j = 0; j < points.length; j++) {
      if (i === j) continue;
      const dx = points[i].x - points[j].x;
      const dy = points[i].y - points[j].y;
      const dz = points[i].z - points[j].z;
      const d2 = dx * dx + dy * dy + dz * dz;
      dists.push({ j, d2 });
    }
    dists.sort((a, b) => a.d2 - b.d2);
    neighbors[i] = dists.slice(0, k).map((d) => d.j);
  }
  return neighbors;
}

function project(
  p: Point3D,
  rotX: number,
  rotY: number,
  radiusPx: number,
): { x: number; y: number; depth: number } {
  const cosY = Math.cos(rotY);
  const sinY = Math.sin(rotY);
  const cosX = Math.cos(rotX);
  const sinX = Math.sin(rotX);

  const x1 = p.x * cosY - p.z * sinY;
  const z1 = p.x * sinY + p.z * cosY;

  const y2 = p.y * cosX - z1 * sinX;
  const z2 = p.y * sinX + z1 * cosX;

  const fov = 350;
  const scale = fov / (fov + z2 * radiusPx * 0.7);
  const x2d = x1 * radiusPx * scale;
  const y2d = y2 * radiusPx * scale;

  return { x: x2d, y: y2d, depth: z2 };
}

type HackGlobeProps = {
  uid: string | null;
  onLabConfig?: (config: HackLabConfig) => void;
};

export default function HackGlobe({ uid, onLabConfig }: HackGlobeProps) {
  const [input, setInput] = useState("");
  const [messages, setMessages] = useState<HackGlobeMessage[]>([]);
  const [isSending, setIsSending] = useState(false);

  const SIZE = Math.min(SCREEN_WIDTH * 0.8, SCREEN_HEIGHT * 0.5);
  const RADIUS = SIZE / 2;

  const pointCount = 200;
  const baseRadius = 1;

  const basePoints = useMemo(
    () => generateSpherePoints(pointCount, baseRadius),
    [],
  );
  const neighbors = useMemo(() => computeNeighbors(basePoints, 3), [basePoints]);

  const autoRotateRef = useRef(true);
  const rotXRef = useRef(0);
  const rotYRef = useRef(0);
  const scatterRef = useRef(1);

  const [breath, setBreath] = useState(1);
  const [frameId, setFrameId] = useState<number | null>(null);

  const [projected, setProjected] = useState<{
    pts: { x: number; y: number; depth: number }[];
    lines: {
      x1: number;
      y1: number;
      x2: number;
      y2: number;
      depth: number;
    }[];
  }>({ pts: [], lines: [] });

  // load per-user HackGlobe chat
  useEffect(() => {
    if (!uid) return;

    const messagesRef = collection(
      db,
      "users",
      uid,
      "aiChats",
      "hackGlobe",
      "messages",
    );
    const q = query(messagesRef, orderBy("createdAt", "desc"));

    const unsub = onSnapshot(q, (snap) => {
      const loaded: HackGlobeMessage[] = [];
      snap.forEach((docSnap) => {
        const d = docSnap.data() as any;
        loaded.push({
          id: docSnap.id,
          role: d.role,
          content: d.content,
        });
      });
      setMessages(loaded);
    });

    return () => unsub();
  }, [uid]);

  // animation loop
  useEffect(() => {
    let t = 0;
    const loop = () => {
      t += 0.016;

      if (autoRotateRef.current) {
        rotYRef.current += 0.01;
      }

      const b = 1 + 0.04 * Math.sin(t * 1.5);
      setBreath(b);

      const pts: { x: number; y: number; depth: number }[] = [];
      const lines: {
        x1: number;
        y1: number;
        x2: number;
        y2: number;
        depth: number;
      }[] = [];

      const rotX = rotXRef.current;
      const rotY = rotYRef.current;
      const s = scatterRef.current;

      for (let i = 0; i < basePoints.length; i++) {
        const bp = basePoints[i];
        const p = { x: bp.x * s, y: bp.y * s, z: bp.z * s };
        const proj = project(p, rotX, rotY, RADIUS * 0.85);
        pts.push({ ...proj });

        for (const j of neighbors[i]) {
          const bp2 = basePoints[j];
          const p2 = { x: bp2.x * s, y: bp2.y * s, z: bp2.z * s };
          const proj2 = project(p2, rotX, rotY, RADIUS * 0.85);
          lines.push({
            x1: proj.x,
            y1: proj.y,
            x2: proj2.x,
            y2: proj2.y,
            depth: (proj.depth + proj2.depth) / 2,
          });
        }
      }

      setProjected({ pts, lines });
      const id = requestAnimationFrame(loop);
      setFrameId(id);
    };

    const id = requestAnimationFrame(loop);
    setFrameId(id);

    return () => {
      if (frameId != null) cancelAnimationFrame(frameId);
      cancelAnimationFrame(id);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [basePoints, neighbors, RADIUS]);

  // gestures
  const touchStartTimeRef = useRef(0);
  const lastXRef = useRef(0);
  const lastYRef = useRef(0);
  const totalMoveRef = useRef(0);
  const isDraggingRef = useRef(false);

  const panResponder = useRef(
    PanResponder.create({
      onStartShouldSetPanResponder: () => true,
      onMoveShouldSetPanResponder: () => true,
      onPanResponderGrant: (e: GestureResponderEvent) => {
        const { pageX, pageY } = e.nativeEvent;
        touchStartTimeRef.current = Date.now();
        lastXRef.current = pageX;
        lastYRef.current = pageY;
        totalMoveRef.current = 0;
        isDraggingRef.current = false;
        autoRotateRef.current = false;
      },
      onPanResponderMove: (e: GestureResponderEvent) => {
        const { pageX, pageY } = e.nativeEvent;
        const dx = pageX - lastXRef.current;
        const dy = pageY - lastYRef.current;
        lastXRef.current = pageX;
        lastYRef.current = pageY;

        totalMoveRef.current += Math.sqrt(dx * dx + dy * dy);
        if (!isDraggingRef.current && totalMoveRef.current > 4) {
          isDraggingRef.current = true;
        }

        if (isDraggingRef.current) {
          const rotateSpeed = 0.005;
          rotYRef.current += dx * rotateSpeed;
          rotXRef.current += dy * rotateSpeed;
        }
      },
      onPanResponderRelease: () => {
        const pressDuration = Date.now() - touchStartTimeRef.current;
        const isTap =
          pressDuration < 200 &&
          totalMoveRef.current < 5 &&
          !isDraggingRef.current;

        if (isTap) {
          scatterRef.current = scatterRef.current === 1 ? 2.4 : 1;
        }

        autoRotateRef.current = true;
        isDraggingRef.current = false;
      },
      onPanResponderTerminate: () => {
        autoRotateRef.current = true;
        isDraggingRef.current = false;
      },
    }),
  ).current;

  // chat send with per-user persistence
  const handleSend = useCallback(async () => {
    const trimmed = input.trim();
    if (!trimmed || isSending || !uid) return;

    const messagesRef = collection(
      db,
      "users",
      uid,
      "aiChats",
      "hackGlobe",
      "messages",
    );

    const userMessage: HackGlobeMessage = {
      id: Date.now().toString(),
      role: "user",
      content: trimmed,
    };

    const newHistory = [userMessage, ...messages];
    setMessages(newHistory);
    setInput("");
    setIsSending(true);

    try {
      await addDoc(messagesRef, {
        role: "user",
        content: trimmed,
        createdAt: serverTimestamp(),
      });
    } catch {
      // ignore
    }

    try {
      const answer = await askHackGlobe({ messages: newHistory });
      const assistantMessage: HackGlobeMessage = {
        id: Date.now().toString() + "-ai",
        role: "assistant",
        content: answer,
      };
      setMessages((prev) => [assistantMessage, ...prev]);

      try {
        await addDoc(messagesRef, {
          role: "assistant",
          content: answer,
          createdAt: serverTimestamp(),
        });
      } catch {
        // ignore
      }

      if (answer.trim().startsWith("{") && onLabConfig) {
        try {
          const cfg = JSON.parse(answer) as HackLabConfig;
          if (Array.isArray(cfg.nodes) && Array.isArray(cfg.tasks)) {
            onLabConfig(cfg);
          }
        } catch {
          // not valid JSON
        }
      }
    } catch {
      const errContent =
        "I ran into an error responding. Try again in a moment.";
      const errMsg: HackGlobeMessage = {
        id: Date.now().toString() + "-err",
        role: "assistant",
        content: errContent,
      };
      setMessages((prev) => [errMsg, ...prev]);

      try {
        await addDoc(messagesRef, {
          role: "assistant",
          content: errContent,
          createdAt: serverTimestamp(),
        });
      } catch {
        // ignore
      }
    } finally {
      setIsSending(false);
    }
  }, [input, isSending, messages, uid, onLabConfig]);

  return (
    <View style={styles.screen}>
      <View style={styles.globeContainer}>
        <View
          {...panResponder.panHandlers}
          style={{ borderRadius: RADIUS, overflow: "hidden" }}
        >
          <Canvas
            style={[
              styles.canvas,
              { width: SIZE, height: SIZE, borderRadius: RADIUS },
            ]}
          >
            <Group
              transform={[
                { translateX: RADIUS },
                { translateY: RADIUS },
                { scale: breath },
              ]}
            >
              <Circle cx={0} cy={0} r={RADIUS * 0.9} color="#020617" />
              <Circle
                cx={0}
                cy={0}
                r={RADIUS * 0.9}
                color="rgba(0,255,255,0.08)"
              />

              {projected.lines.map((l, idx) => (
                <Line
                  key={`line-${idx}`}
                  p1={vec(l.x1, l.y1)}
                  p2={vec(l.x2, l.y2)}
                  color="rgba(56,189,248,0.38)"
                  strokeWidth={0.7}
                />
              ))}

              {projected.pts.map((p, idx) => {
                const depthNorm = Math.max(-1, Math.min(1, p.depth));
                const alpha = 0.25 + 0.4 * ((depthNorm + 1) / 2);
                const size = 1.5 + 1.2 * ((depthNorm + 1) / 2);
                return (
                  <Circle
                    key={`pt-${idx}`}
                    cx={p.x}
                    cy={p.y}
                    r={size}
                    color={`rgba(45,212,191,${alpha.toFixed(2)})`}
                  />
                );
              })}
            </Group>
          </Canvas>
        </View>
      </View>

      <View style={styles.chatContainer}>
        <FlatList
          data={messages}
          keyExtractor={(item) => item.id}
          inverted
          style={styles.messageList}
          contentContainerStyle={{ paddingBottom: 4 }}
          renderItem={({ item }) => (
            <View
              style={[
                styles.messageBubble,
                item.role === "assistant" && styles.messageBubbleAssistant,
              ]}
            >
              <Text style={styles.messageText}>{item.content}</Text>
            </View>
          )}
        />

        <View style={styles.chatInner}>
          <TextInput
            style={styles.input}
            placeholder="Ask the HackGlobe, e.g. “create a beginner lab”..."
            placeholderTextColor="#777"
            value={input}
            onChangeText={setInput}
            autoCorrect={false}
          />
          <TouchableOpacity
            style={[styles.sendButton, isSending && { opacity: 0.4 }]}
            onPress={handleSend}
            disabled={isSending || !uid}
          >
            <Text style={styles.sendText}>{isSending ? "..." : "Send"}</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: "#02040A",
  },
  globeContainer: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
  },
  canvas: {
    backgroundColor: "transparent",
  },
  chatContainer: {
    paddingHorizontal: 16,
    paddingBottom: 18,
    paddingTop: 8,
    backgroundColor: "rgba(3, 6, 18, 0.95)",
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: "rgba(255,255,255,0.06)",
  },
  messageList: {
    maxHeight: 120,
    marginBottom: 8,
  },
  messageBubble: {
    alignSelf: "flex-end",
    backgroundColor: "rgba(0, 255, 255, 0.15)",
    borderRadius: 16,
    paddingHorizontal: 10,
    paddingVertical: 6,
    marginBottom: 4,
    maxWidth: "90%",
  },
  messageBubbleAssistant: {
    alignSelf: "flex-start",
    backgroundColor: "rgba(120, 120, 255, 0.18)",
  },
  messageText: {
    color: "#fff",
    fontSize: 13,
  },
  chatInner: {
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "rgba(10, 14, 30, 1)",
    borderRadius: 999,
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderWidth: 1,
    borderColor: "rgba(0, 255, 255, 0.22)",
  },
  input: {
    flex: 1,
    color: "#fff",
    fontSize: 14,
    paddingVertical: 4,
    paddingHorizontal: 4,
  },
  sendButton: {
    marginLeft: 8,
    paddingHorizontal: 14,
    paddingVertical: 6,
    borderRadius: 999,
    backgroundColor: "#00ffff",
  },
  sendText: {
    color: "#02040A",
    fontWeight: "600",
    fontSize: 13,
  },
});
