// src/member/work/hack/HackTypes.ts
import type { NetworkNode } from "./HackNetworkScreen";

export type HackTask = {
  id: string;
  description: string;
  targetIds: string[];
  requiredAction?: "exploit" | "bruteforce" | "scan";
};

export type HackLabConfig = {
  title: string;
  description: string;
  nodes: NetworkNode[];
  tasks: HackTask[];
};
