import { apiGet, apiPost } from "@/lib/apiClient";
import type { PaperDashboardData, MonitorData, ReportData, PaperTradeHistoryItem } from "@/types/paper";

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

export async function startRule(id: number) {
  return apiPost<unknown, void>(`/api/v1/trading/paper/rules/${id}/start`, undefined);
}

export async function stopRule(id: number) {
  return apiPost<unknown, void>(`/api/v1/trading/paper/rules/${id}/stop`, undefined);
}
