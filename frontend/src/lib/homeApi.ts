import { apiGet } from "@/lib/apiClient";
import type {
  BuzzCompany,
  MarketNewsItem,
  MarketSentiment,
  TrendingCompany,
} from "@/types/home";

export async function fetchTrendingCompanies(limit = 8) {
  return apiGet<TrendingCompany[]>(
    `/api/v1/home/trending-companies?limit=${limit}`
  );
}

export async function fetchMarketNews(limit = 12) {
  return apiGet<MarketNewsItem[]>(`/api/v1/home/market-news?limit=${limit}`);
}

export async function fetchBuzzCompanies(limit = 8) {
  return apiGet<BuzzCompany[]>(`/api/v1/home/buzz-companies?limit=${limit}`);
}

export async function fetchMarketSentiment() {
  return apiGet<MarketSentiment>("/api/v1/home/market-sentiment");
}
