import { apiGet, apiPost, apiPut } from "@/lib/apiClient";
import type { AdminAgentStats, AdminPeriod, AdminUser, UserUsageData } from "@/types/admin";

export async function fetchAdminAgentStats(period: AdminPeriod) {
  return apiGet<AdminAgentStats>(`/api/v1/admin/agent/stats?period=${period}`);
}

export async function fetchAdminUserUsage() {
  return apiGet<UserUsageData>("/api/v1/admin/users/usage");
}

export async function fetchAdminUsers() {
  return apiGet<AdminUser[]>("/api/v1/admin/users");
}

export async function updateTradingAccess(userId: number, tradingEnabled: boolean) {
  return apiPut<AdminUser, { tradingEnabled: boolean }>(
    `/api/v1/admin/users/${userId}/trading-access`,
    { tradingEnabled }
  );
}

export async function createAdminUser(data: {
  email: string;
  displayName: string;
  password: string;
  role: string;
}) {
  return apiPost<AdminUser, typeof data>("/api/v1/admin/users", data);
}

// v1.6.0: KOSPI200 마스터 시드 / 일봉 적재 (ROLE_ADMIN, 멱등)
// 반환은 카운트 맵 (예: { inserted, updated, flagged } / { ingested })
export type Kospi200Counts = Record<string, number>;

export async function seedKospi200() {
  return apiPost<Kospi200Counts, void>(
    "/api/v1/admin/market/seed-kospi200",
    undefined
  );
}

// 일봉 적재는 99종목 직렬 외부호출이라 백그라운드(fire-and-forget)로 실행된다.
// 트리거는 즉시 202(STARTED) / 409(ALREADY_RUNNING)를 반환하고, 진행은 status로 폴링한다.
export type IngestTriggerResult = { status: "STARTED" | "ALREADY_RUNNING" };

export type IngestJobState = "IDLE" | "RUNNING" | "DONE" | "FAILED";
export type IngestJobStatus = {
  state: IngestJobState;
  symbols: number | null;
  startedAt: string | null;
  finishedAt: string | null;
  error: string | null;
};

export async function ingestKospi200() {
  return apiPost<IngestTriggerResult, void>(
    "/api/v1/admin/market/ingest-kospi200",
    undefined
  );
}

export async function fetchIngestKospi200Status() {
  return apiGet<IngestJobStatus>(
    "/api/v1/admin/market/ingest-kospi200/status"
  );
}
