import { apiGet, apiPost } from "@/lib/apiClient";
import type {
  PaperDashboardData,
  MonitorData,
  ReportData,
  PaperTradeHistoryItem,
  RunSummary,
  RunDashboard,
} from "@/types/paper";

const BASE = "/api/v1/trading/paper";

export async function fetchPaperDashboard() {
  return apiGet<PaperDashboardData>(`${BASE}/dashboard`);
}

export async function fetchPaperMonitor() {
  return apiGet<MonitorData>(`${BASE}/monitor`);
}

export async function fetchPaperReport() {
  return apiGet<ReportData>(`${BASE}/report`);
}

export async function fetchPaperHistory() {
  return apiGet<PaperTradeHistoryItem[]>(`${BASE}/history`);
}

export async function promoteRule(id: number) {
  return apiPost<unknown, void>(`/api/v1/trading/paper/rules/${id}/promote`, undefined);
}

export async function pauseRule(id: number) {
  return apiPost<unknown, void>(`/api/v1/trading/paper/rules/${id}/pause`, undefined);
}

export async function resumeRule(id: number) {
  return apiPost<unknown, void>(`/api/v1/trading/paper/rules/${id}/resume`, undefined);
}

export async function copyRule(id: number) {
  return apiPost<unknown, void>(`/api/v1/trading/paper/rules/${id}/copy`, undefined);
}

// 2-axis lifecycle — Wave 3 (06.5-05)
export async function activateRule(id: number) {
  return apiPost<unknown, void>(`/api/v1/trading/paper/rules/${id}/activate`, undefined);
}

export async function deactivateRule(id: number) {
  return apiPost<unknown, void>(`/api/v1/trading/paper/rules/${id}/deactivate`, undefined);
}

export async function startRule(id: number, overrideSymbols?: string[]) {
  // v1.6.0: 빈 유니버스(ERR_LIFECYCLE_005) 폴백 시 직접 선택한 종목을 전달
  return apiPost<unknown, { overrideSymbols?: string[] }>(
    `/api/v1/trading/paper/rules/${id}/start`,
    overrideSymbols && overrideSymbols.length > 0 ? { overrideSymbols } : {}
  );
}

export async function stopRule(id: number) {
  return apiPost<unknown, void>(`/api/v1/trading/paper/rules/${id}/stop`, undefined);
}

// 6.9 Wave 4: run-scoped fetch functions (Wave 5 pages consume these)
export async function fetchPaperRuns() {
  return apiGet<RunSummary[]>(`${BASE}/runs`);
}

export async function fetchRunDashboard(runId: number) {
  return apiGet<RunDashboard>(`${BASE}/runs/${runId}/dashboard`);
}

export async function fetchRunHistory(
  runId: number,
  mode?: string,
  from?: string,
  to?: string
) {
  const params = new URLSearchParams();
  if (mode) params.set("mode", mode);
  if (from) params.set("from", from);
  if (to) params.set("to", to);
  const qs = params.toString();
  return apiGet<PaperTradeHistoryItem[]>(
    `${BASE}/runs/${runId}/history${qs ? `?${qs}` : ""}`
  );
}

export async function fetchRunReport(
  runId: number,
  mode?: string,
  from?: string,
  to?: string
) {
  const params = new URLSearchParams();
  if (mode) params.set("mode", mode);
  if (from) params.set("from", from);
  if (to) params.set("to", to);
  const qs = params.toString();
  return apiGet<ReportData>(
    `${BASE}/runs/${runId}/report${qs ? `?${qs}` : ""}`
  );
}
