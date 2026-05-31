import { apiDeleteWithBody, apiGet, apiPost } from "@/lib/apiClient";
import type {
  CleanupPayload,
  CleanupPreview,
  CleanupResult,
  EmbeddingJob,
  ReindexPayload,
  ReindexResult,
  VectorDbStats,
  VectorEntityType,
} from "@/types/vectorDb";

export async function fetchVectorDbStats() {
  return apiGet<VectorDbStats>("/api/v1/admin/vectordb/stats");
}

export async function startVectorReindex(payload: ReindexPayload) {
  return apiPost<ReindexResult, ReindexPayload>(
    "/api/v1/admin/vectordb/reindex",
    payload
  );
}

export async function fetchEmbeddingJob(jobId: number) {
  return apiGet<EmbeddingJob>(`/api/v1/admin/vectordb/jobs/${jobId}`);
}

export async function fetchCleanupPreview(
  olderThanDays: number,
  types: VectorEntityType[]
) {
  const query = new URLSearchParams({
    olderThanDays: String(olderThanDays),
    types: types.join(","),
  });
  return apiGet<CleanupPreview>(
    `/api/v1/admin/vectordb/cleanup/preview?${query.toString()}`
  );
}

export async function runVectorCleanup(payload: CleanupPayload) {
  return apiDeleteWithBody<CleanupResult, CleanupPayload>(
    "/api/v1/admin/vectordb/cleanup",
    payload
  );
}
