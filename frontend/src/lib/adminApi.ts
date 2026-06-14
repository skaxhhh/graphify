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
