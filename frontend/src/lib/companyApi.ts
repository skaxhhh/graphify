import { apiGet, apiPost } from "@/lib/apiClient";
import type { AgentInsight, CompanyDetail, CompanyInsights } from "@/types/company";
import type { CompanyMarketTechnical } from "@/types/marketTechnical";

export async function fetchCompanyDetail(companyId: number) {
  return apiGet<CompanyDetail>(`/api/v1/companies/${companyId}`);
}

export async function fetchCompanyInsights(companyId: number) {
  return apiGet<CompanyInsights>(`/api/v1/companies/${companyId}/insights`);
}

export async function syncCompanyFromDart(companyId: number) {
  return apiPost<CompanyDetail, Record<string, never>>(
    `/api/v1/companies/${companyId}/sync`,
    {}
  );
}

export async function generateCompanyAgentInsight(companyId: number) {
  return apiPost<AgentInsight, Record<string, never>>(
    `/api/v1/companies/${companyId}/insights/generate`,
    {}
  );
}

export async function fetchCompanyMarketTechnical(companyId: number) {
  return apiGet<CompanyMarketTechnical>(`/api/v1/companies/${companyId}/market-technical`);
}
