import { apiDelete, apiGet, apiPost } from "@/lib/apiClient";
import type { CompanyCompareData, CompareBasis, WatchlistData, WatchlistItem } from "@/types/watchlist";

export async function fetchMyWatchlist() {
  return apiGet<WatchlistData>("/api/v1/watchlist/me");
}

export async function addToWatchlist(companyId: number) {
  return apiPost<WatchlistItem, { companyId: number }>("/api/v1/watchlist/me", {
    companyId,
  });
}

export async function removeFromWatchlist(companyId: number) {
  return apiDelete<null>(`/api/v1/watchlist/me/${companyId}`);
}

export async function fetchCompanyCompare(ids: number[], basis: CompareBasis) {
  const query = new URLSearchParams();
  query.set("ids", ids.join(","));
  query.set("basis", basis);
  return apiGet<CompanyCompareData>(`/api/v1/companies/compare?${query.toString()}`);
}
