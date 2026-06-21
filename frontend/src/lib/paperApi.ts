import { apiGet, apiPost } from "@/lib/apiClient";
import type { PaperDashboardData, MonitorData, ReportData } from "@/types/paper";

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
