export type InsightType = "SUPPLY_CHAIN" | "INVESTMENT" | "PARTNERSHIP" | "RISK";
export type InsightConfidence = "HIGH" | "MEDIUM" | "LOW";
export type SignalKind = "RISK" | "OPPORTUNITY";

export interface FinancialSummary {
  periodLabel: string;
  revenue: string;
  operatingMargin: string;
  netIncome: string;
}

export interface Provenance {
  sources: string[];
  lastUpdated: string;
  mcpToolsUsed: string[];
}

export interface DisclosureSummary {
  receiptNo: string | null;
  receiptDate: string | null;
  reportName: string;
  submitter: string | null;
}

export interface FinancialStatementLine {
  bsnsYear: string;
  reprtCode: string | null;
  reportLabel: string;
  accountName: string;
  currentAmount: string | null;
  previousAmount: string | null;
  currency: string | null;
}

export interface CompanyNewsItem {
  title: string;
  summary: string;
  sourceName: string;
  sourceUrl: string;
  publishedAt: string;
}

export interface CompanyDartProfile {
  corpName: string | null;
  stockCode: string | null;
  ceoName: string | null;
  corpClassLabel: string | null;
  address: string | null;
  homepage: string | null;
  industryCode: string | null;
  estDate: string | null;
  accMonth: string | null;
  bizrNo: string | null;
  recentDisclosures: DisclosureSummary[];
  financialStatements: FinancialStatementLine[];
  relatedNews: CompanyNewsItem[];
  collectedAt: string | null;
}

export interface CompanyDetail {
  id: number;
  name: string;
  ticker: string | null;
  industry: string | null;
  market: string | null;
  dataStatus: string;
  summary: string;
  financials: FinancialSummary | null;
  lastUpdated: string;
  coverageByRelationType: Record<string, number>;
  provenance: Provenance;
  needsSync?: boolean;
  syncStatus?: string;
  dartProfile?: CompanyDartProfile | null;
}

export interface AgentInsight {
  content: string;
  modelLabel: string | null;
  status: string;
  generatedAt: string;
}

export interface InsightCard {
  id: number;
  type: InsightType;
  title: string;
  summary: string;
  confidence: InsightConfidence;
  evidence: string | null;
  highlightNodeIds: string[];
}

export interface CompanySignal {
  label: string;
  kind: SignalKind;
  relatedNodeIds: string[];
  sources: string[];
}

export interface CompanyInsights {
  cards: InsightCard[];
  signals: CompanySignal[];
  agentInsight: AgentInsight | null;
}
