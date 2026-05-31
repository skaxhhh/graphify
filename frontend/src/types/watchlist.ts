import type { InsightCard } from "@/types/company";

export interface WatchlistItem {
  companyId: number;
  name: string;
  industry: string | null;
  ticker: string | null;
  addedAt: string;
}

export interface WatchlistData {
  items: WatchlistItem[];
}

export type CompareBasis = "INVESTMENT" | "SUPPLY_CHAIN" | "PARTNERSHIP";

export interface CompareMetrics {
  insightCount: number;
  signalCount: number;
  relationCount: number;
}

export interface CompareCompany {
  companyId: number;
  name: string;
  industry: string | null;
  insightCards: InsightCard[];
  metrics: CompareMetrics;
}

export interface CompanyCompareData {
  basis: CompareBasis;
  companies: CompareCompany[];
}
