export interface TrendingCompany {
  rank: number;
  companyId: number;
  name: string;
  ticker: string | null;
  industry: string | null;
  viewCount: number;
}

export interface MarketNewsItem {
  id: number;
  title: string;
  summary: string;
  sourceName: string;
  sourceUrl: string | null;
  ticker: string | null;
  companyName: string | null;
  publishedAt: string;
}

export interface BuzzCompany {
  rank: number;
  companyId: number | null;
  name: string;
  ticker: string | null;
  industry: string | null;
  price: number | null;
  priceDirection: string | null;
  sourceLabel: string;
}

export type SentimentZone =
  | "EXTREME_FEAR"
  | "FEAR"
  | "NEUTRAL"
  | "GREED"
  | "EXTREME_GREED";

export interface SentimentIndicator {
  id: string;
  name: string;
  description: string;
  score: number;
  signal: string;
}

export interface MarketSentimentSnapshot {
  score: number;
  zone: SentimentZone;
  zoneLabel: string;
  market: string;
  indicators: SentimentIndicator[];
  quoteTime: string;
  dataSource: string;
  vix: number | null;
  vixMa50: number | null;
}

export interface MarketSentiment {
  kospi: MarketSentimentSnapshot | null;
  nasdaq: MarketSentimentSnapshot | null;
  asOf: string;
}
