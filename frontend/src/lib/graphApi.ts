import { apiGet } from "@/lib/apiClient";
import type { CompanyGraph } from "@/types/graph";

export interface GraphQueryParams {
  depth: number;
  filter?: string;
}

export async function fetchCompanyGraph(companyId: number, params: GraphQueryParams) {
  const query = new URLSearchParams();
  query.set("depth", String(params.depth));
  if (params.filter) {
    query.set("filter", params.filter);
  }
  return apiGet<CompanyGraph>(`/api/v1/companies/${companyId}/graph?${query.toString()}`);
}
