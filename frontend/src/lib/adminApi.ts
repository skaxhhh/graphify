import { apiGet } from "@/lib/apiClient";
import type { AdminAgentStats, AdminPeriod, UserUsageData } from "@/types/admin";

export async function fetchAdminAgentStats(period: AdminPeriod) {
  return apiGet<AdminAgentStats>(`/api/v1/admin/agent/stats?period=${period}`);
}

export async function fetchAdminUserUsage() {
  return apiGet<UserUsageData>("/api/v1/admin/users/usage");
}
