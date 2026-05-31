export type AdminPeriod = "day" | "week";

export interface StatsPoint {
  date: string;
  runCount: number;
  tokenUsage: number;
  errorCount: number;
}

export interface AdminAlert {
  severity: string;
  message: string;
  detectedAt: string;
}

export interface AdminAgentStats {
  runCount: number;
  avgDurationMs: number;
  tokenUsage: number;
  errorRate: number;
  period: AdminPeriod;
  series: StatsPoint[];
  alerts: AdminAlert[];
}

export interface UserUsageRow {
  userId: number;
  name: string;
  email: string;
  requests: number;
  tokens: number;
  errors: number;
}

export interface UserUsageData {
  rows: UserUsageRow[];
}
