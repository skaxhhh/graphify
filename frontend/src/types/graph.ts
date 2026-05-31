import type { Provenance } from "@/types/company";

export type RelationType = "SUPPLY_CHAIN" | "INVESTMENT" | "PARTNERSHIP" | "RISK";
export type DimMode = "dim" | "hide";

export interface GraphNode {
  id: string;
  label: string;
  type: string;
  summary: string | null;
  degree: number;
  clusterId: string | null;
}

export interface GraphEdge {
  id: number;
  source: string;
  target: string;
  relationType: RelationType;
  strength: number;
  evidence: string | null;
  updatedAt: string;
}

export interface CompanyGraph {
  nodes: GraphNode[];
  edges: GraphEdge[];
  sessionId: string;
  provenance: Provenance;
}

export interface AgentStreamEvent {
  stage: string;
  message: string;
  progress?: number;
}
