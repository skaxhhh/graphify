import { apiGet } from "@/lib/apiClient";
import type { HistoryDetail, HistoryListData, HistoryQueryParams } from "@/types/history";

export async function fetchMyHistory(params: HistoryQueryParams) {
  const query = new URLSearchParams();
  query.set("page", String(params.page ?? 0));
  if (params.size != null) {
    query.set("size", String(params.size));
  }
  if (params.q) {
    query.set("q", params.q);
  }
  if (params.from) {
    query.set("from", params.from);
  }
  if (params.to) {
    query.set("to", params.to);
  }
  return apiGet<HistoryListData>(`/api/v1/history/me?${query.toString()}`);
}

export async function fetchHistoryDetail(sessionId: string) {
  return apiGet<HistoryDetail>(`/api/v1/history/${encodeURIComponent(sessionId)}`);
}
