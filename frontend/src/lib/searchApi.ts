import { apiGet, apiPost } from "@/lib/apiClient";
import type {
  AutocompleteItem,
  CompanySearchData,
  CompanyResolveResult,
  CompanySearchParams,
  TermsLatest,
} from "@/types/search";

export async function fetchAutocomplete(q: string) {
  const encoded = encodeURIComponent(q.trim());
  return apiGet<AutocompleteItem[]>(`/api/v1/search/autocomplete?q=${encoded}`);
}

export async function fetchTermsLatest() {
  return apiGet<TermsLatest>("/api/v1/terms/latest");
}

export async function fetchCompanySearch(params: CompanySearchParams) {
  const query = new URLSearchParams();
  query.set("q", params.q.trim());
  query.set("sort", params.sort ?? "name");
  query.set("page", String(params.page ?? 0));
  if (params.size != null) {
    query.set("size", String(params.size));
  }
  if (params.industry) {
    query.set("industry", params.industry);
  }
  if (params.market) {
    query.set("market", params.market);
  }
  if (params.dataStatus) {
    query.set("dataStatus", params.dataStatus);
  }
  if (params.enrich !== false) {
    query.set("enrich", "true");
  }
  if (params.enrichThreshold != null) {
    query.set("enrichThreshold", String(params.enrichThreshold));
  }
  return apiGet<CompanySearchData>(`/api/v1/companies/search?${query.toString()}`);
}

export async function resolveCompany(body: {
  query?: string;
  ticker?: string;
  externalSource?: string;
  externalId?: string;
}) {
  return apiPost<CompanyResolveResult, typeof body>("/api/v1/companies/resolve", body);
}
