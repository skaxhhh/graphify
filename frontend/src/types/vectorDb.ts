export type ReindexScope = "ALL" | "SELECTED";

export type VectorEntityType = "COMPANY" | "INSIGHT" | "RELATION";

export type EmbeddingJobStatus = "PENDING" | "RUNNING" | "SUCCESS" | "FAILED";

export interface VectorDbJobSummary {
  id: number;
  jobType: string;
  scope: string;
  status: string;
  progress: number;
  message: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface VectorDbStats {
  totalVectors: number;
  byType: Record<string, number>;
  indexSizeBytes: number;
  avgLatencyMs: number;
  avgSimilarity: number;
  requestCount24h: number;
  latencySeries: number[];
  similaritySeries: number[];
  requestSeries: number[];
  lastJobs: VectorDbJobSummary[];
  updatedAt: string;
}

export interface ReindexPayload {
  scope: ReindexScope;
  targetIds?: number[];
}

export interface ReindexResult {
  jobId: number;
}

export interface EmbeddingJob {
  jobId: number;
  jobType: string;
  scope: string;
  status: EmbeddingJobStatus;
  progress: number;
  message: string | null;
  createdAt: string;
  completedAt: string | null;
}

export interface CleanupPayload {
  olderThanDays: number;
  types: VectorEntityType[];
}

export interface CleanupPreview {
  previewCount: number;
}

export interface CleanupResult {
  deletedCount: number;
}

export const VECTOR_TYPE_OPTIONS: { value: VectorEntityType; label: string }[] = [
  { value: "COMPANY", label: "기업" },
  { value: "INSIGHT", label: "인사이트" },
  { value: "RELATION", label: "관계" },
];
