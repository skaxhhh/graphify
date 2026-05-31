import type { CompanySignal, InsightCard } from "@/types/company";
import type { GraphEdge, GraphNode } from "@/types/graph";

export type HistoryStatus = "COMPLETED" | "FAILED" | "RUNNING";

export interface HistoryItem {
  sessionId: string;
  companyId: number;
  companyName: string;
  analyzedAt: string;
  status: HistoryStatus;
  summaryLine: string | null;
}

export interface HistoryListData {
  items: HistoryItem[];
}

export interface HistoryQueryParams {
  page?: number;
  size?: number;
  q?: string;
  from?: string;
  to?: string;
}

export interface HistoryCompany {
  id: number;
  name: string;
}

export interface TimelineEvent {
  t: string;
  eventType: string;
  label: string;
  payload: Record<string, unknown>;
}

export interface HistoryGraphSnapshot {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

export interface HistoryDiffSummary {
  text: string;
  generatedAt: string;
}

export interface HistoryDetail {
  sessionId: string;
  company: HistoryCompany;
  analyzedAt: string;
  status: HistoryStatus;
  summaryLine: string | null;
  timeline: TimelineEvent[];
  graphSnapshot: HistoryGraphSnapshot;
  insights: InsightCard[];
  signals: CompanySignal[];
  diffSummary: HistoryDiffSummary | null;
}
